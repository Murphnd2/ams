package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.BpoRegistration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PSP-side service — pushes sourced tasks to BPO vendor deployments.
 * All failures are non-fatal: errors are logged but never propagate.
 * Must be called AFTER the transaction that created the ToDo records has committed.
 */
public class BpoTaskPushService {

    /**
     * Push all sourced, incomplete ToDos in a checklist to their BPO vendors.
     * Groups by BpoRegistration and sends one HTTP call per vendor.
     */
    public static void pushDelegatedTasks(EntityManager em, CheckList checklist) {
        if (!AppConfig.isPsp()) return;
        if (checklist == null || checklist.getToDoList() == null) return;

        try {
            // Collect sourced, incomplete ToDos
            List<ToDo> sourcedToDos = checklist.getToDoList().stream()
                    .filter(t -> t.getTask() != null && t.getTask().isSourced())
                    .filter(t -> !t.isComplete() && !t.isBpoCompleted())
                    .collect(Collectors.toList());

            if (sourcedToDos.isEmpty()) return;

            // Group by BpoRegistration
            Map<Long, List<ToDo>> byVendor = new LinkedHashMap<>();
            Map<Long, BpoRegistration> regMap = new HashMap<>();

            for (ToDo todo : sourcedToDos) {
                BpoRegistration reg = todo.getTask().getBpoRegistration();
                if (reg == null || !reg.isAvailable()) continue;
                if (reg.getPartnerUrl() == null || reg.getApiTokenOutbound() == null) continue;

                byVendor.computeIfAbsent(reg.getId(), k -> new ArrayList<>()).add(todo);
                regMap.put(reg.getId(), reg);
            }

            // Push to each vendor
            for (Map.Entry<Long, List<ToDo>> entry : byVendor.entrySet()) {
                BpoRegistration reg = regMap.get(entry.getKey());
                List<ToDo> todos = entry.getValue();
                pushToVendor(reg, todos, checklist);
            }

        } catch (Exception e) {
            System.out.println("[BPO-API] pushDelegatedTasks error (non-fatal): " + e.getMessage());
        }
    }

    private static void pushToVendor(BpoRegistration reg, List<ToDo> todos, CheckList checklist) {
        try {
            String url = reg.getPartnerUrl() + "/api/v1/tasks";
            String token = reg.getApiTokenOutbound();

            // Build activity context
            String activityType = "CHECKLIST";
            String activityName = checklist.getFullName();
            String employerName = null;

            if (checklist.getRenewal() != null) {
                activityType = "RENEWAL";
                activityName = checklist.getRenewal().getFullName() + " Renewal";
                if (checklist.getRenewal().getEmployer() != null) {
                    employerName = checklist.getRenewal().getEmployer().getContactName();
                }
            } else if (checklist.getSetup() != null) {
                activityType = "SETUP";
                activityName = checklist.getSetup().getFullName() + " Setup";
            } else if (checklist.getTicket() != null) {
                activityType = "TICKET";
                activityName = checklist.getTicket().getFullName() + " Ticket";
            }

            // Build task array
            List<Map<String, String>> taskList = new ArrayList<>();
            for (ToDo todo : todos) {
                Map<String, String> taskMap = new LinkedHashMap<>();
                taskMap.put("todoGuid", todo.getTodoGuid());
                taskMap.put("taskName", todo.getTask().getPlainDescription());
                taskMap.put("taskDescription", todo.getTask().getDescription());
                taskMap.put("activityType", activityType);
                taskMap.put("activityName", activityName);

                if (employerName != null) {
                    taskMap.put("employerName", employerName);
                }

                if (checklist.getDueDate() != null) {
                    taskMap.put("dueDate", checklist.getDueDate().toString());
                }

                if (todo.getTask().hasGoTo() && todo.getTask().getGoToLink() != null) {
                    taskMap.put("gotoLink", todo.getTask().getGoToLink().getLinkPath());
                }

                if (todo.getTask().hasInfo() && todo.getTask().getInfoLink() != null) {
                    taskMap.put("infoLink", todo.getTask().getInfoLink().getLinkPath());
                }

                taskList.add(taskMap);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("tasks", taskList);

            ApiClient.ApiResponse resp = ApiClient.postJsonObject(url, payload, token);

            if (resp.isSuccess()) {
                System.out.println("[BPO-API] pushToVendor: Pushed " + taskList.size() +
                        " tasks to " + reg.getBpoName() + " (" + resp.statusCode + ")");
            } else {
                System.out.println("[BPO-API] pushToVendor: Push to " + reg.getBpoName() +
                        " returned " + resp.statusCode + ": " + resp.body);
            }

        } catch (Exception e) {
            System.out.println("[BPO-API] pushToVendor: Failed push to " +
                    reg.getBpoName() + " (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Push a single ToDo to its BPO vendor.
     * Used when a task becomes sourced or a new ToDo is added individually.
     */
    public static void pushSingleTask(EntityManager em, ToDo todo) {
        if (!AppConfig.isPsp()) return;
        if (todo == null || todo.getTask() == null || !todo.getTask().isSourced()) return;
        if (todo.isComplete() || todo.isBpoCompleted()) return;

        try {
            BpoRegistration reg = todo.getTask().getBpoRegistration();
            if (reg == null || !reg.isAvailable()) return;
            if (reg.getPartnerUrl() == null || reg.getApiTokenOutbound() == null) return;

            CheckList checklist = todo.getCheckList();
            if (checklist == null) return;

            pushToVendor(reg, List.of(todo), checklist);
        } catch (Exception e) {
            System.out.println("[BPO-API] pushSingleTask error (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Notify BPO that a task's metadata has changed (name, description, links, due date).
     * Sends UPDATE command to the BPO's /api/v1/tasks/update endpoint for each affected ToDo.
     */
    public static void pushTaskUpdate(EntityManager em, Task task) {
        if (!AppConfig.isPsp()) return;
        if (task == null || !task.isSourced()) return;

        BpoRegistration reg = task.getBpoRegistration();
        if (reg == null || !reg.isAvailable()) return;
        if (reg.getPartnerUrl() == null || reg.getApiTokenOutbound() == null) return;

        try {
            // Find all incomplete ToDos for this task
            List<ToDo> todos = em.createQuery(
                            "SELECT t FROM ToDo t WHERE t.task.id = :taskId AND t.isComplete = false",
                            ToDo.class)
                    .setParameter("taskId", task.getId())
                    .getResultList();

            String url = reg.getPartnerUrl() + "/api/v1/tasks/update";
            String token = reg.getApiTokenOutbound();

            for (ToDo todo : todos) {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("action", "UPDATE");
                payload.put("todoGuid", todo.getTodoGuid());
                payload.put("taskName", task.getPlainDescription());
                payload.put("taskDescription", task.getDescription());

                if (task.hasGoTo() && task.getGoToLink() != null) {
                    payload.put("gotoLink", task.getGoToLink().getLinkPath());
                }
                if (task.hasInfo() && task.getInfoLink() != null) {
                    payload.put("infoLink", task.getInfoLink().getLinkPath());
                }

                ApiClient.ApiResponse resp = ApiClient.postJson(url, payload, token);
                System.out.println("[BPO-API] pushTaskUpdate: todoGuid=" + todo.getTodoGuid() +
                        " to " + reg.getBpoName() + " (" + resp.statusCode + ")");
            }
        } catch (Exception e) {
            System.out.println("[BPO-API] pushTaskUpdate error (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Recall all incomplete ToDos for a task from a BPO vendor.
     * Sends RECALL command to the BPO's /api/v1/tasks/update endpoint.
     */
    public static void recallTask(EntityManager em, Task task, BpoRegistration reg) {
        if (!AppConfig.isPsp()) return;
        if (task == null || reg == null) return;
        if (reg.getPartnerUrl() == null || reg.getApiTokenOutbound() == null) return;

        try {
            List<ToDo> todos = em.createQuery(
                            "SELECT t FROM ToDo t WHERE t.task.id = :taskId AND t.isComplete = false",
                            ToDo.class)
                    .setParameter("taskId", task.getId())
                    .getResultList();

            String url = reg.getPartnerUrl() + "/api/v1/tasks/update";
            String token = reg.getApiTokenOutbound();

            for (ToDo todo : todos) {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("action", "RECALL");
                payload.put("todoGuid", todo.getTodoGuid());

                ApiClient.ApiResponse resp = ApiClient.postJson(url, payload, token);
                System.out.println("[BPO-API] recallTask: todoGuid=" + todo.getTodoGuid() +
                        " from " + reg.getBpoName() + " (" + resp.statusCode + ")");
            }
        } catch (Exception e) {
            System.out.println("[BPO-API] recallTask error (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Push all incomplete ToDos for a task to its BPO vendor.
     * Used when a task transitions from Internal to Sourced.
     */
    public static void pushTaskTodos(EntityManager em, Task task) {
        if (!AppConfig.isPsp()) return;
        if (task == null || !task.isSourced()) return;

        BpoRegistration reg = task.getBpoRegistration();
        if (reg == null || !reg.isAvailable()) return;
        if (reg.getPartnerUrl() == null || reg.getApiTokenOutbound() == null) return;

        try {
            List<ToDo> todos = em.createQuery(
                            "SELECT t FROM ToDo t WHERE t.task.id = :taskId AND t.isComplete = false AND t.bpoCompleted = false",
                            ToDo.class)
                    .setParameter("taskId", task.getId())
                    .getResultList();

            if (todos.isEmpty()) return;

            // Group by checklist and push
            Map<Long, List<ToDo>> byChecklist = new LinkedHashMap<>();
            for (ToDo todo : todos) {
                if (todo.getCheckList() != null) {
                    byChecklist.computeIfAbsent(todo.getCheckList().getId(), k -> new ArrayList<>()).add(todo);
                }
            }

            for (Map.Entry<Long, List<ToDo>> entry : byChecklist.entrySet()) {
                CheckList cl = entry.getValue().get(0).getCheckList();
                pushToVendor(reg, entry.getValue(), cl);
            }
        } catch (Exception e) {
            System.out.println("[BPO-API] pushTaskTodos error (non-fatal): " + e.getMessage());
        }
    }
}

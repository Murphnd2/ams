package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.activity.checklist.CheckList;
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
            System.err.println("BpoTaskPushService error (non-fatal): " + e.getMessage());
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
                System.out.println("BpoTaskPushService: Pushed " + taskList.size() +
                        " tasks to " + reg.getBpoName() + " (" + resp.statusCode + ")");
            } else {
                System.err.println("BpoTaskPushService: Push to " + reg.getBpoName() +
                        " returned " + resp.statusCode + ": " + resp.body);
            }

        } catch (Exception e) {
            System.err.println("BpoTaskPushService: Failed push to " +
                    reg.getBpoName() + " (non-fatal): " + e.getMessage());
        }
    }
}

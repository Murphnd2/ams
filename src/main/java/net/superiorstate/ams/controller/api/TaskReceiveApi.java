package net.superiorstate.ams.controller.api;

import com.google.gson.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.model.activity.checklist.tasks.DelegatedToDo;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.PspClient;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

/**
 * BPO endpoint — receives delegated tasks from PSP deployments.
 * POST /api/v1/tasks
 */
@WebServlet("/api/v1/tasks")
public class TaskReceiveApi extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        if (!AppConfig.isBpo()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Not found\"}");
            return;
        }

        PspClient pspClient = (PspClient) request.getAttribute("authenticatedPartner");
        if (pspClient == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return;
        }

        String body;
        try {
            body = new String(request.getInputStream().readAllBytes());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Invalid request body\"}");
            return;
        }

        JsonObject json;
        JsonArray tasksArray;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
            tasksArray = json.getAsJsonArray("tasks");
            if (tasksArray == null || tasksArray.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"tasks array is required and must not be empty\"}");
                return;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Invalid JSON format\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            int created = 0;
            int skipped = 0;

            em.getTransaction().begin();

            for (JsonElement el : tasksArray) {
                JsonObject t = el.getAsJsonObject();
                String todoGuid = getJsonString(t, "todoGuid");

                if (todoGuid == null || todoGuid.isBlank()) {
                    skipped++;
                    continue;
                }

                // Skip duplicates
                List<DelegatedToDo> existing = em.createQuery(
                                "SELECT d FROM DelegatedToDo d WHERE d.todoGuid = :guid", DelegatedToDo.class)
                        .setParameter("guid", todoGuid)
                        .getResultList();

                if (!existing.isEmpty()) {
                    skipped++;
                    continue;
                }

                DelegatedToDo dt = new DelegatedToDo();
                dt.setTodoGuid(todoGuid);
                dt.setPspClient(pspClient);
                dt.setTaskName(getJsonString(t, "taskName", "Untitled Task"));
                dt.setTaskDescription(getJsonString(t, "taskDescription"));
                dt.setActivityType(getJsonString(t, "activityType"));
                dt.setActivityName(getJsonString(t, "activityName"));
                dt.setEmployerName(getJsonString(t, "employerName"));
                dt.setGotoLink(getJsonString(t, "gotoLink"));
                dt.setInfoLink(getJsonString(t, "infoLink"));
                String sortOrderStr = getJsonString(t, "sortOrder");
                if (sortOrderStr != null) {
                    try { dt.setSortOrder(Integer.parseInt(sortOrderStr)); } catch (NumberFormatException ignored) {}
                }

                String recurringSeriesId = getJsonString(t, "recurringSeriesId");
                if (recurringSeriesId != null && !recurringSeriesId.isBlank()) {
                    dt.setRecurringSeriesId(recurringSeriesId);
                }

                String sourceTaskId = getJsonString(t, "sourceTaskId");
                if (sourceTaskId != null && !sourceTaskId.isBlank()) {
                    dt.setSourceTaskId(sourceTaskId);
                }

                // Determine status: auto-accept if toggle is ON, or if this is a
                // recurring task that was previously accepted for this PSP,
                // or if the same source task was previously accepted (required-sequence
                // tasks from tickets, setups, renewals — same task doesn't need re-approval)
                if (pspClient.isAutoAcceptTasks()) {
                    dt.setStatus("ACTIVE");
                } else if (recurringSeriesId != null && !recurringSeriesId.isBlank()) {
                    long priorAccepted = (Long) em.createQuery(
                            "SELECT COUNT(d) FROM DelegatedToDo d " +
                            "WHERE d.recurringSeriesId = :seriesId " +
                            "AND d.pspClient.id = :clientId " +
                            "AND d.status <> 'PENDING'")
                        .setParameter("seriesId", recurringSeriesId)
                        .setParameter("clientId", pspClient.getId())
                        .getSingleResult();
                    dt.setStatus(priorAccepted > 0 ? "ACTIVE" : "PENDING");
                } else if (sourceTaskId != null && !sourceTaskId.isBlank()) {
                    // Required-sequence task: once approved for one activity, auto-accept
                    // all future occurrences of the same source task from this PSP
                    long priorAccepted = (Long) em.createQuery(
                            "SELECT COUNT(d) FROM DelegatedToDo d " +
                            "WHERE d.sourceTaskId = :taskId " +
                            "AND d.pspClient.id = :clientId " +
                            "AND d.status <> 'PENDING'")
                        .setParameter("taskId", sourceTaskId)
                        .setParameter("clientId", pspClient.getId())
                        .getSingleResult();
                    dt.setStatus(priorAccepted > 0 ? "ACTIVE" : "PENDING");
                } else {
                    dt.setStatus("PENDING");
                }

                // Auto-assignment: when task is auto-accepted, try to assign it
                if ("ACTIVE".equals(dt.getStatus())) {
                    Person assignee = resolveAutoAssignee(em, pspClient, recurringSeriesId, sourceTaskId);
                    if (assignee != null) {
                        dt.setAssignedTo(assignee);
                    }
                }

                String dueDateStr = getJsonString(t, "dueDate");
                if (dueDateStr != null) {
                    try {
                        dt.setDueDate(Date.valueOf(dueDateStr));
                    } catch (IllegalArgumentException ignored) {}
                }

                em.persist(dt);
                created++;
            }

            em.getTransaction().commit();

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"status\": \"OK\", \"created\": " + created + ", \"skipped\": " + skipped + "}");
            System.out.println("[BPO-API] TaskReceiveApi: received tasks, created=" + created + " skipped=" + skipped);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.out.println("[BPO-API] TaskReceiveApi error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Resolve who should be auto-assigned to an incoming task.
     * Priority:
     *   1. Prior instance of same recurring series or source task → copy that assignee (if active)
     *   2. PspClient default assignee (if set and active)
     *   3. null (leave unassigned)
     */
    private Person resolveAutoAssignee(EntityManager em, PspClient pspClient,
                                       String recurringSeriesId, String sourceTaskId) {
        // 1. Try prior instance assignment (recurring series first, then source task)
        Person priorAssignee = findPriorAssignee(em, pspClient, "recurringSeriesId", recurringSeriesId);
        if (priorAssignee == null) {
            priorAssignee = findPriorAssignee(em, pspClient, "sourceTaskId", sourceTaskId);
        }
        if (priorAssignee != null) return priorAssignee;

        // 2. Fall back to PSP client default assignee
        Person defaultAssignee = pspClient.getDefaultAssignee();
        if (defaultAssignee != null && isActiveBpoUser(em, defaultAssignee.getId())) {
            return defaultAssignee;
        }

        return null;
    }

    private Person findPriorAssignee(EntityManager em, PspClient pspClient,
                                     String fieldName, String fieldValue) {
        if (fieldValue == null || fieldValue.isBlank()) return null;

        // Get prior instances ordered by most recent first, that had an assignee
        String jpql = "SELECT d FROM DelegatedToDo d " +
                "WHERE d." + fieldName + " = :val " +
                "AND d.pspClient.id = :clientId " +
                "AND d.assignedTo IS NOT NULL " +
                "ORDER BY d.dateReceived DESC";
        List<DelegatedToDo> priors = em.createQuery(jpql, DelegatedToDo.class)
                .setParameter("val", fieldValue)
                .setParameter("clientId", pspClient.getId())
                .setMaxResults(5)
                .getResultList();

        for (DelegatedToDo prior : priors) {
            Person candidate = prior.getAssignedTo();
            if (candidate != null && isActiveBpoUser(em, candidate.getId())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isActiveBpoUser(EntityManager em, Long personId) {
        // Check if person has an active user account (role doesn't matter for BPO —
        // if they were previously assigned, they were valid)
        String jpql = "SELECT COUNT(u) FROM User u " +
                "WHERE u.person.id = :pid AND u.isActive = true";
        long count = (Long) em.createQuery(jpql)
                .setParameter("pid", personId)
                .getSingleResult();
        return count > 0;
    }

    private String getJsonString(JsonObject obj, String key) {
        return getJsonString(obj, key, null);
    }

    private String getJsonString(JsonObject obj, String key, String defaultValue) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return defaultValue;
        return el.getAsString();
    }
}

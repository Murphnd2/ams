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

                // Determine status: auto-accept if toggle is ON, or if this is a
                // recurring task that was previously accepted for this PSP
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
                } else {
                    dt.setStatus("PENDING");
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

    private String getJsonString(JsonObject obj, String key) {
        return getJsonString(obj, key, null);
    }

    private String getJsonString(JsonObject obj, String key, String defaultValue) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return defaultValue;
        return el.getAsString();
    }
}

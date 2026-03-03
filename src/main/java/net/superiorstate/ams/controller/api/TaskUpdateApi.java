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

import java.io.IOException;
import java.sql.Date;
import java.util.List;

/**
 * BPO endpoint — receives task update commands from PSP deployments.
 * POST /api/v1/tasks/update
 *
 * Actions: REVERT (undo completion), RECALL (remove task), UPDATE (modify fields)
 */
@WebServlet("/api/v1/tasks/update")
public class TaskUpdateApi extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        if (!AppConfig.isBpo()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Not found\"}");
            return;
        }

        if (request.getAttribute("authenticatedPartner") == null) {
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
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Invalid JSON format\"}");
            return;
        }

        String action = getJsonString(json, "action");
        String todoGuid = getJsonString(json, "todoGuid");

        if (action == null || todoGuid == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"action and todoGuid are required\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<DelegatedToDo> matches = em.createQuery(
                            "SELECT d FROM DelegatedToDo d WHERE d.todoGuid = :guid", DelegatedToDo.class)
                    .setParameter("guid", todoGuid)
                    .getResultList();

            if (matches.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Task not found\"}");
                return;
            }

            DelegatedToDo dt = matches.get(0);

            em.getTransaction().begin();

            switch (action.toUpperCase()) {
                case "REVERT":
                    dt.setCompleted(false);
                    dt.setCompletedBy(null);
                    dt.setCompletedDate(null);
                    dt.setReverted(true);
                    dt.setStatus("ACTIVE");
                    break;

                case "RECALL":
                    dt.setStatus("RECALLED");
                    break;

                case "UPDATE":
                    String taskName = getJsonString(json, "taskName");
                    if (taskName != null) dt.setTaskName(taskName);

                    String taskDescription = getJsonString(json, "taskDescription");
                    if (taskDescription != null) dt.setTaskDescription(taskDescription);

                    String dueDateStr = getJsonString(json, "dueDate");
                    if (dueDateStr != null) {
                        try {
                            dt.setDueDate(Date.valueOf(dueDateStr));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    String gotoLink = getJsonString(json, "gotoLink");
                    if (gotoLink != null) dt.setGotoLink(gotoLink);

                    String infoLink = getJsonString(json, "infoLink");
                    if (infoLink != null) dt.setInfoLink(infoLink);
                    break;

                default:
                    em.getTransaction().rollback();
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Unknown action: " + action + "\"}");
                    return;
            }

            em.merge(dt);
            em.getTransaction().commit();

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\": \"OK\", \"action\": \"" + action.toUpperCase() + "\"}");
            System.out.println("[BPO-API] TaskUpdateApi: action=" + action.toUpperCase() + " todoGuid=" + todoGuid);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.out.println("[BPO-API] TaskUpdateApi error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private String getJsonString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return null;
        return el.getAsString();
    }
}

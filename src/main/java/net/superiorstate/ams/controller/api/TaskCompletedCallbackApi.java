package net.superiorstate.ams.controller.api;

import com.google.gson.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * PSP endpoint — receives task completion callbacks from BPO deployments.
 * POST /api/v1/callback/task-completed
 */
@WebServlet("/api/v1/callback/task-completed")
public class TaskCompletedCallbackApi extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        if (!AppConfig.isPsp()) {
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

        String todoGuid = getJsonString(json, "todoGuid");
        String completedByName = getJsonString(json, "completedByName");

        if (todoGuid == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"todoGuid is required\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<ToDo> matches = em.createQuery(
                            "SELECT t FROM ToDo t WHERE t.todoGuid = :guid", ToDo.class)
                    .setParameter("guid", todoGuid)
                    .getResultList();

            if (matches.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Task not found\"}");
                return;
            }

            ToDo todo = matches.get(0);

            em.getTransaction().begin();

            todo.setBpoCompleted(true);
            todo.setBpoCompletedDate(Date.valueOf(LocalDate.now()));
            // bpoCompletedBy stays null since the BPO person doesn't exist locally

            // Check if Vendor Only (sourced + !allowNonOwner) → auto-complete
            Task task = todo.getTask();
            boolean vendorOnly = task != null && task.isSourced() && !task.allowNonOwner();
            String noteText;
            if (vendorOnly) {
                todo.setComplete(true);
                todo.setDateCompleted(Date.valueOf(LocalDate.now()));
                noteText = "Task completed by BPO (auto-closed — vendor only).";
            } else {
                noteText = "Task marked complete by BPO — awaiting PSP verification.";
            }
            em.merge(todo);

            // Auto-add a completion note
            ToDoNote note = new ToDoNote();
            note.setToDo(todo);
            note.setTodoGuid(todoGuid);
            note.setNoteText(noteText);
            note.setSourceType("BPO");
            note.setAuthorName(completedByName);
            em.persist(note);

            em.getTransaction().commit();

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\": \"OK\", \"message\": \"Task completion recorded.\"}");
            System.out.println("[BPO-API] TaskCompletedCallbackApi: completion recorded for todoGuid=" + todoGuid
                    + (vendorOnly ? " (vendor-only, auto-closed)" : " (awaiting PSP verification)"));

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.out.println("[BPO-API] TaskCompletedCallbackApi error: " + e.getMessage());
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

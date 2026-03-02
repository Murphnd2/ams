package net.superiorstate.ams.controller.api;

import com.google.gson.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;

import java.io.IOException;
import java.util.List;

/**
 * PSP endpoint — receives note-added callbacks from BPO deployments.
 * POST /api/v1/callback/note-added
 */
@WebServlet("/api/v1/callback/note-added")
public class NoteAddedCallbackApi extends HttpServlet {

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
        String noteText = getJsonString(json, "noteText");
        String authorName = getJsonString(json, "authorName");

        if (todoGuid == null || noteText == null || noteText.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"todoGuid and noteText are required\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Find the local ToDo by GUID
            List<ToDo> matches = em.createQuery(
                            "SELECT t FROM ToDo t WHERE t.todoGuid = :guid", ToDo.class)
                    .setParameter("guid", todoGuid)
                    .getResultList();

            ToDo localTodo = matches.isEmpty() ? null : matches.get(0);

            em.getTransaction().begin();

            ToDoNote note = new ToDoNote();
            note.setToDo(localTodo);
            note.setTodoGuid(todoGuid);
            note.setNoteText(noteText.trim());
            note.setSourceType("BPO");
            note.setAuthorName(authorName);
            // createdBy is null — BPO person doesn't exist on PSP side
            em.persist(note);

            em.getTransaction().commit();

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"status\": \"OK\"}");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.err.println("NoteAddedCallbackApi error: " + e.getMessage());
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

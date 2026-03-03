package net.superiorstate.ams.controller.api;

import com.google.gson.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.WebLink;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;

/**
 * BPO endpoint — serves and receives notes for delegated tasks.
 * GET  /api/v1/tasks/notes?todoGuid=xxx  — returns notes for a task
 * POST /api/v1/tasks/notes               — receives a note from PSP
 */
@WebServlet("/api/v1/tasks/notes")
public class TaskNotesApi extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

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

        String todoGuid = request.getParameter("todoGuid");
        if (todoGuid == null || todoGuid.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"todoGuid is required\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<ToDoNote> notes = em.createQuery(
                            "SELECT n FROM ToDoNote n LEFT JOIN FETCH n.webLinkList WHERE n.todoGuid = :guid ORDER BY n.createdDate DESC",
                            ToDoNote.class)
                    .setParameter("guid", todoGuid)
                    .getResultList();

            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            String pspSlug = global.getPsp().getFullName();

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < notes.size(); i++) {
                ToDoNote n = notes.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"source\":\"").append(escapeJson(n.getSourceType())).append("\",");
                json.append("\"author\":\"").append(escapeJson(n.getDisplayAuthor())).append("\",");
                json.append("\"date\":\"").append(sdf.format(n.getCreatedDate())).append("\",");
                json.append("\"text\":\"").append(escapeJson(n.getNoteText())).append("\"");

                // Attachments (7-day pre-signed URLs for cross-system access)
                json.append(",\"attachments\":[");
                List<WebLink> links = n.getWebLinkList();
                if (links != null) {
                    int ai = 0;
                    for (WebLink w : links) {
                        if (w.getLinkType() != null && w.getLinkType().getId() == 1 && w.isActive()) {
                            if (ai > 0) json.append(",");
                            String downloadUrl = StorageDAO.getDownloadUrl(null, pspSlug, w.getLinkPath(), Duration.ofDays(7));
                            json.append("{");
                            json.append("\"name\":\"").append(escapeJson(w.getPlainText())).append("\",");
                            json.append("\"url\":\"").append(escapeJson(downloadUrl)).append("\"");
                            json.append("}");
                            ai++;
                        }
                        if (w.getLinkType() != null && w.getLinkType().getId() == 2 && w.isActive()) {
                            if (ai > 0) json.append(",");
                            json.append("{");
                            json.append("\"name\":\"").append(escapeJson(w.getPlainText())).append("\",");
                            json.append("\"url\":\"").append(escapeJson(w.getLinkPath())).append("\"");
                            json.append("}");
                            ai++;
                        }
                    }
                }
                json.append("]");

                json.append("}");
            }
            json.append("]");

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(json.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.out.println("[BPO-API] TaskNotesApi GET error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }

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
            em.getTransaction().begin();

            ToDoNote note = new ToDoNote();
            note.setTodoGuid(todoGuid);
            note.setNoteText(noteText.trim());
            note.setSourceType("PSP");
            note.setAuthorName(authorName);
            // toDo and createdBy are null for cross-system notes from PSP
            em.persist(note);

            // Store attachment metadata from PSP (external URLs — linkType 2)
            JsonArray attachments = json.has("attachments") ? json.getAsJsonArray("attachments") : null;
            if (attachments != null && attachments.size() > 0) {
                LinkType externalLinkType = SequenceDAO.getLinkTypeById(em, 2); // type 2 = external link
                for (JsonElement el : attachments) {
                    JsonObject att = el.getAsJsonObject();
                    String displayName = getJsonString(att, "displayName");
                    String downloadUrl = getJsonString(att, "downloadUrl");
                    if (displayName != null && downloadUrl != null) {
                        WebLink w = new WebLink();
                        w.setPlainText(displayName);
                        w.setLinkPath(downloadUrl);
                        w.setLinkType(externalLinkType);
                        w.setActive(true);
                        w.setToDoNote(note);
                        em.persist(w);
                    }
                }
            }

            em.getTransaction().commit();

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"status\": \"OK\"}");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.out.println("[BPO-API] TaskNotesApi POST error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private String getJsonString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return null;
        return el.getAsString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

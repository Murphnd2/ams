package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.general.WebLink;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;

@WebServlet(name = "BpoGetNotes", value = "/BpoGetNotes")
public class BpoGetNotes extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String todoIdParam = request.getParameter("todoId");
        String todoGuidParam = request.getParameter("todoGuid");

        if (todoIdParam == null && todoGuidParam == null) {
            out.print("[]");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            List<ToDoNote> notes;

            if (todoGuidParam != null && !todoGuidParam.isBlank()) {
                // Cross-system mode: query by todoGuid
                TypedQuery<ToDoNote> q = em.createQuery(
                        "SELECT n FROM ToDoNote n LEFT JOIN FETCH n.webLinkList WHERE n.todoGuid = :guid ORDER BY n.createdDate DESC",
                        ToDoNote.class);
                q.setParameter("guid", todoGuidParam);
                notes = q.getResultList();
            } else {
                // Co-located mode: query by toDo.id
                long todoId = Long.parseLong(todoIdParam);
                TypedQuery<ToDoNote> q = em.createQuery(
                        "SELECT n FROM ToDoNote n LEFT JOIN FETCH n.webLinkList WHERE n.toDo.id = :todoId ORDER BY n.createdDate DESC",
                        ToDoNote.class);
                q.setParameter("todoId", todoId);
                notes = q.getResultList();
            }

            // Get PSP name for pre-signed URL generation
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

                // Attachments
                json.append(",\"attachments\":[");
                List<WebLink> links = n.getWebLinkList();
                if (links != null) {
                    int ai = 0;
                    for (WebLink w : links) {
                        if (w.getLinkType() != null && w.getLinkType().getId() == 1 && w.isActive()) {
                            if (ai > 0) json.append(",");
                            String downloadUrl = StorageDAO.getDownloadUrl(null, pspSlug, w.getLinkPath(), Duration.ofHours(1));
                            json.append("{");
                            json.append("\"name\":\"").append(escapeJson(w.getPlainText())).append("\",");
                            json.append("\"url\":\"").append(escapeJson(downloadUrl)).append("\"");
                            json.append("}");
                            ai++;
                        }
                        // linkType 2 = external URL (cross-system attachment from other side)
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
            out.print(json);
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        } finally {
            em.close();
        }
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

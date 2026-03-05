package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.checklist.tasks.DelegatedToDo;

import java.io.IOException;
import java.util.List;

/**
 * BPO-side AJAX endpoint — returns past completed DelegatedToDo records
 * for a given recurring_series_id + psp_client_id combination.
 * Returns JSON array with dueDate, completedByName, todoGuid, and noteCount.
 */
@WebServlet("/BpoRecurringHistory")
public class BpoRecurringHistory extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || !local.isAuthenticated()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String recurringSeriesId = request.getParameter("recurringSeriesId");
        String pspClientIdParam = request.getParameter("pspClientId");

        if (recurringSeriesId == null || recurringSeriesId.isBlank()
                || pspClientIdParam == null || pspClientIdParam.isBlank()) {
            response.setContentType("application/json");
            response.getWriter().write("[]");
            return;
        }

        long pspClientId;
        try {
            pspClientId = Long.parseLong(pspClientIdParam);
        } catch (NumberFormatException e) {
            response.setContentType("application/json");
            response.getWriter().write("[]");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Fetch up to 6 most-recent completed delegated todos for this series + client
            List<DelegatedToDo> past = em.createQuery(
                "SELECT d FROM DelegatedToDo d " +
                "WHERE d.recurringSeriesId = :seriesId " +
                "AND d.pspClient.id = :clientId " +
                "AND d.isCompleted = true " +
                "ORDER BY d.dueDate DESC",
                DelegatedToDo.class)
                .setParameter("seriesId", recurringSeriesId)
                .setParameter("clientId", pspClientId)
                .setMaxResults(6)
                .getResultList();

            response.setContentType("application/json");
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < past.size(); i++) {
                DelegatedToDo dt = past.get(i);
                if (i > 0) json.append(",");

                // Count notes by todoGuid
                Long noteCount = 0L;
                try {
                    noteCount = em.createQuery(
                        "SELECT COUNT(n) FROM ToDoNote n WHERE n.todoGuid = :guid", Long.class)
                        .setParameter("guid", dt.getTodoGuid())
                        .getSingleResult();
                } catch (Exception ignored) {}

                json.append("{");
                json.append("\"dueDate\":\"").append(dt.getDueDate() != null ? dt.getDueDate() : "").append("\",");
                json.append("\"completedByName\":\"").append(dt.getCompletedBy() != null ? escapeJson(dt.getCompletedBy().getFullName()) : "").append("\",");
                json.append("\"todoGuid\":\"").append(dt.getTodoGuid()).append("\",");
                json.append("\"noteCount\":").append(noteCount);
                json.append("}");
            }
            json.append("]");
            response.getWriter().write(json.toString());

        } finally {
            em.close();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

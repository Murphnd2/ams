package net.superiorstate.ams.controller.api.outlook;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

/**
 * GET /api/v1/outlook/activities?q={searchTerm}
 *
 * Returns up to 20 open activities whose employer/full-name matches the
 * search term, scoped to the PSP of the authenticated (token-linked) person.
 *
 * Response 200: JSON array of activity objects
 *   [{"activityId":1234,"label":"Acme Corp — Renewal (Due: 2026-05-01)",
 *     "employerName":"Acme Corp","activityType":"Renewal"}]
 * Response 401: {"error":"Invalid or missing Outlook API token"}
 * Response 400: {"error":"Missing or too-short search term (min 2 chars)"}
 */
@WebServlet(name = "OutlookActivitiesApi", urlPatterns = "/api/v1/outlook/activities")
public class OutlookActivitiesApi extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Person caller = OutlookApiHelper.validateOutlookToken(em, request);
            if (caller == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or missing Outlook API token");
                return;
            }

            String q = request.getParameter("q");
            if (q == null || q.trim().length() < 2) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Missing or too-short search term (min 2 chars)");
                return;
            }

            Long pspId = caller.getPsp() != null ? caller.getPsp().getId() : null;
            if (pspId == null) {
                OutlookApiHelper.sendJson(response, HttpServletResponse.SC_OK, "[]");
                return;
            }

            // Activity25 is a view-backed DTO (a25_activity_list_open) already
            // scoped to open activities. We join through activity.loggedBy.psp
            // to scope to the caller's PSP, and fall back to matching activities
            // whose loggedBy is null (legacy rows).
            List<Activity25> matches = em.createQuery(
                            "SELECT a25 FROM Activity25 a25 " +
                            "WHERE LOWER(a25.name) LIKE :q " +
                            "AND (a25.activity.loggedBy.psp.id = :pspId OR a25.activity.loggedBy IS NULL) " +
                            "ORDER BY a25.dueDate ASC",
                            Activity25.class)
                    .setParameter("q", "%" + q.trim().toLowerCase() + "%")
                    .setParameter("pspId", pspId)
                    .setMaxResults(20)
                    .getResultList();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Activity25 a25 : matches) {
                if (a25.getActivity() == null) continue;
                if (!first) json.append(",");
                first = false;

                String employerName = a25.getName() != null ? a25.getName() : "";
                String activityType = a25.getDtype() != null ? a25.getDtype() : "Activity";
                Date dueDate = a25.getDueDate();
                String label = employerName
                        + " — " + activityType
                        + (dueDate != null ? " (Due: " + dueDate + ")" : "");

                json.append("{")
                        .append("\"activityId\":").append(a25.getActivity().getId()).append(",")
                        .append("\"label\":\"").append(OutlookApiHelper.escapeJson(label)).append("\",")
                        .append("\"employerName\":\"").append(OutlookApiHelper.escapeJson(employerName)).append("\",")
                        .append("\"activityType\":\"").append(OutlookApiHelper.escapeJson(activityType)).append("\"")
                        .append("}");
            }
            json.append("]");
            OutlookApiHelper.sendJson(response, HttpServletResponse.SC_OK, json.toString());
        } finally {
            if (em.isOpen()) em.close();
        }
    }
}

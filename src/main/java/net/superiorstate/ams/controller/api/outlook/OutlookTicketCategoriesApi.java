package net.superiorstate.ams.controller.api.outlook;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.dao.TicketQueryDAO;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.util.List;

/**
 * GET /api/v1/outlook/ticket-categories
 *
 * Returns the active ticket ServiceItems grouped by TicketCategory for the
 * Outlook add-in's "Create Ticket" dropdown. Uses the same query as the
 * main AMS ticket creation form (TicketQueryDAO.getActiveTicketServiceItems).
 *
 * Response JSON:
 *   [
 *     { "id": 123, "description": "Claim not paid", "categoryId": 11, "category": "Claims" },
 *     ...
 *   ]
 */
@WebServlet(name = "OutlookTicketCategoriesApi", urlPatterns = "/api/v1/outlook/ticket-categories")
public class OutlookTicketCategoriesApi extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Person caller = OutlookApiHelper.validateOutlookToken(em, request);
            if (caller == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or missing Outlook API token");
                return;
            }

            List<ServiceItem> items = TicketQueryDAO.getActiveTicketServiceItems(em);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                ServiceItem si = items.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"id\":").append(si.getId()).append(",");
                json.append("\"description\":\"").append(OutlookApiHelper.escapeJson(si.getDescription())).append("\",");
                json.append("\"categoryId\":").append(si.getTicketCategory().getId()).append(",");
                json.append("\"category\":\"").append(OutlookApiHelper.escapeJson(si.getTicketCategory().getDescription())).append("\"");
                json.append("}");
            }
            json.append("]");

            OutlookApiHelper.sendJson(response, HttpServletResponse.SC_OK, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to load ticket categories: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }
}

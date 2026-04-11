package net.superiorstate.ams.controller.api.outlook;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.model.general.OutlookUserLink;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * POST /api/v1/outlook/authenticate
 *
 * Accepts a Microsoft 365 email address and, if a matching active
 * outlook_user_link row exists, returns the stored api_token and the linked
 * person's display name. The add-in caches the token in localStorage and
 * sends it on every subsequent request as `Authorization: Bearer {token}`.
 *
 * Request body (JSON): {"m365Email":"kevin@superiorstate.biz"}
 * Response 200:        {"token":"...","userName":"Kevin Murphy","personId":42}
 * Response 404:        {"error":"No AMS account linked to this email. Contact your administrator."}
 */
@WebServlet(name = "OutlookAuthApi", urlPatterns = "/api/v1/outlook/authenticate")
public class OutlookAuthApi extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String m365Email = extractEmail(request);
        if (m365Email == null || m365Email.isBlank()) {
            OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing m365Email in request body.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            OutlookUserLink link;
            try {
                link = em.createQuery(
                                "SELECT l FROM OutlookUserLink l JOIN FETCH l.person " +
                                "WHERE LOWER(l.m365Email) = :email AND l.isActive = true",
                                OutlookUserLink.class)
                        .setParameter("email", m365Email.trim().toLowerCase())
                        .getSingleResult();
            } catch (Exception e) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_NOT_FOUND,
                        "No AMS account linked to this email. Contact your administrator.");
                return;
            }

            String userName = link.getPerson().getFullName() != null
                    ? link.getPerson().getFullName()
                    : (link.getPerson().getFirstName() + " " + link.getPerson().getLastName()).trim();

            String json = "{"
                    + "\"token\":\"" + OutlookApiHelper.escapeJson(link.getApiToken()) + "\","
                    + "\"userName\":\"" + OutlookApiHelper.escapeJson(userName) + "\","
                    + "\"personId\":" + link.getPerson().getId()
                    + "}";
            OutlookApiHelper.sendJson(response, HttpServletResponse.SC_OK, json);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Extracts the m365Email field from a simple JSON body.
     * Avoids a full JSON library dependency for this single field.
     */
    private String extractEmail(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        String raw = body.toString();
        int keyIdx = raw.indexOf("\"m365Email\"");
        if (keyIdx < 0) return null;
        int colon = raw.indexOf(':', keyIdx);
        if (colon < 0) return null;
        int firstQuote = raw.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        int secondQuote = raw.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return raw.substring(firstQuote + 1, secondQuote);
    }
}

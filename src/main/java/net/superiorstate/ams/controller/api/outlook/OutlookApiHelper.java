package net.superiorstate.ams.controller.api.outlook;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.model.general.OutlookUserLink;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;

/**
 * Shared helpers for the Outlook add-in API endpoints.
 *
 * Outlook endpoints do NOT use the existing session cookie or ApiTokenFilter.
 * Instead, each request carries `Authorization: Bearer {token}` where the
 * token is the `api_token` stored on an `outlook_user_link` row. The add-in
 * obtains this token once via the authenticate endpoint and persists it in
 * the browser's localStorage for subsequent requests.
 */
public final class OutlookApiHelper {

    private OutlookApiHelper() {}

    /**
     * Validates an Outlook API token and returns the linked Person.
     *
     * @return the Person linked to the token, or null if the token is missing,
     *         malformed, unknown, or inactive.
     */
    public static Person validateOutlookToken(EntityManager em, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) return null;

        try {
            OutlookUserLink link = em.createQuery(
                    "SELECT l FROM OutlookUserLink l JOIN FETCH l.person " +
                    "WHERE l.apiToken = :token AND l.isActive = true",
                    OutlookUserLink.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return link.getPerson();
        } catch (Exception e) {
            return null;
        }
    }

    /** Writes a JSON error response with the given status code. */
    public static void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + escapeJson(message) + "\"}");
    }

    /** Writes a JSON success body with the given status code. */
    public static void sendJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    /** Escapes a string for safe inclusion in a JSON string literal. */
    public static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

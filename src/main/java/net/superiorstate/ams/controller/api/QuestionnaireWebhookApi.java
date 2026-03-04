package net.superiorstate.ams.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireInstance;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Public webhook endpoint for Jotform submission callbacks.
 * Receives multipart/form-data POST from Jotform when a form is submitted.
 * Extracts the instance GUID from the rawRequest JSON, looks up the
 * QuestionnaireInstance, and auto-marks it SUBMITTED.
 *
 * Bypasses ApiTokenFilter — authenticated by GUID knowledge (same pattern as /q/{guid}).
 *
 * Jotform setup:
 * 1. Add hidden field with default value = {ref} (captures GUID from URL query param)
 * 2. Add webhook integration: https://superiorstate.biz/api/v1/questionnaire/webhook
 */
@WebServlet("/api/v1/questionnaire/webhook")
@MultipartConfig
public class QuestionnaireWebhookApi extends HttpServlet {

    private static final Gson gson = new Gson();
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Service unavailable\"}");
            return;
        }

        // 1. Extract instance GUID from the Jotform payload
        String guid = extractGuid(request);
        if (guid == null) {
            System.out.println("[QWebhook] No GUID found in payload");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\": \"ignored\", \"reason\": \"no ref GUID found\"}");
            return;
        }

        System.out.println("[QWebhook] Received callback for GUID: " + guid);

        // 2. Look up instance and mark SUBMITTED
        EntityManager em = emf.createEntityManager();
        try {
            QuestionnaireInstance instance = QuestionnaireService.getInstanceByGuid(em, guid);
            if (instance == null) {
                System.out.println("[QWebhook] No instance found for GUID: " + guid);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"status\": \"ignored\", \"reason\": \"instance not found\"}");
                return;
            }

            // Only process if not already submitted/reviewed
            String status = instance.getStatus();
            if ("SUBMITTED".equals(status) || "REVIEWED".equals(status)) {
                System.out.println("[QWebhook] Instance already " + status + ", skipping");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"status\": \"already_" + status.toLowerCase() + "\"}");
                return;
            }

            // Extract submitter info from rawRequest if available
            String submitterName = extractSubmitterName(request);
            String submitterEmail = extractSubmitterEmail(request);

            // Mark as SUBMITTED via QuestionnaireService (creates Note on activity)
            em.getTransaction().begin();
            QuestionnaireService.submitInstance(
                    instance.getId(), submitterName, submitterEmail, em, null);
            em.getTransaction().commit();

            System.out.println("[QWebhook] Instance " + instance.getId() + " marked SUBMITTED"
                    + (submitterName != null ? " by " + submitterName : ""));

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\": \"submitted\", \"instanceId\": " + instance.getId() + "}");

        } catch (Exception e) {
            System.out.println("[QWebhook] Error: " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } finally {
            em.close();
        }
    }

    /**
     * Extracts the instance GUID from the Jotform webhook payload.
     * Strategy: parse rawRequest JSON and search all values for a UUID pattern.
     * Falls back to scanning all request parameters.
     */
    private String extractGuid(HttpServletRequest request) {
        // Strategy 1: Parse rawRequest JSON (Jotform's primary data field)
        String rawRequest = request.getParameter("rawRequest");
        if (rawRequest != null && !rawRequest.isBlank()) {
            try {
                JsonObject json = gson.fromJson(rawRequest, JsonObject.class);
                String guid = findGuidInJson(json);
                if (guid != null) return guid;
            } catch (Exception e) {
                System.out.println("[QWebhook] rawRequest parse error: " + e.getMessage());
            }
        }

        // Strategy 2: Check all request parameters for a UUID value
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            for (String val : entry.getValue()) {
                if (val != null && UUID_PATTERN.matcher(val.trim()).matches()) {
                    return val.trim();
                }
            }
        }

        return null;
    }

    /**
     * Recursively searches a JSON object for a value matching UUID pattern.
     * Checks keys containing "ref" first for priority matching.
     */
    private String findGuidInJson(JsonObject json) {
        // Priority pass: look for keys containing "ref" (our hidden field name)
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonElement val = entry.getValue();
            if (key.contains("ref") && val.isJsonPrimitive()) {
                String s = val.getAsString().trim();
                if (UUID_PATTERN.matcher(s).matches()) return s;
            }
        }

        // General pass: search all string values for a UUID
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement val = entry.getValue();
            if (val.isJsonPrimitive()) {
                String s = val.getAsString().trim();
                if (UUID_PATTERN.matcher(s).matches()) return s;
            } else if (val.isJsonObject()) {
                String found = findGuidInJson(val.getAsJsonObject());
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Attempts to extract submitter name from Jotform payload.
     * Looks for common Jotform name field patterns in rawRequest.
     */
    private String extractSubmitterName(HttpServletRequest request) {
        String rawRequest = request.getParameter("rawRequest");
        if (rawRequest == null) return null;
        try {
            JsonObject json = gson.fromJson(rawRequest, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey().toLowerCase();
                JsonElement val = entry.getValue();
                // Jotform name fields are typically objects with first/last
                if (key.contains("name") && val.isJsonObject()) {
                    JsonObject nameObj = val.getAsJsonObject();
                    String first = getJsonString(nameObj, "first");
                    String last = getJsonString(nameObj, "last");
                    if (first != null || last != null) {
                        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                    }
                }
                // Simple text name field
                if (key.contains("name") && val.isJsonPrimitive()) {
                    String s = val.getAsString().trim();
                    if (!s.isEmpty() && !UUID_PATTERN.matcher(s).matches()) return s;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Attempts to extract submitter email from Jotform payload.
     */
    private String extractSubmitterEmail(HttpServletRequest request) {
        String rawRequest = request.getParameter("rawRequest");
        if (rawRequest == null) return null;
        try {
            JsonObject json = gson.fromJson(rawRequest, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey().toLowerCase();
                JsonElement val = entry.getValue();
                if (key.contains("email") && val.isJsonPrimitive()) {
                    String s = val.getAsString().trim();
                    if (s.contains("@")) return s;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
            String s = obj.get(key).getAsString().trim();
            return s.isEmpty() ? null : s;
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Health check / test endpoint
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"service\": \"questionnaire-webhook\", \"status\": \"active\"}");
    }
}

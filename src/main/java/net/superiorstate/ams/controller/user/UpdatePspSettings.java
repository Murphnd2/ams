package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.service.ClaudeApiService;
import net.superiorstate.ams.model.Constant;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

/**
 * PSP Settings — Admin modal for viewing/updating SMTP and feature configuration.
 *
 * GET  → Returns current settings as JSON (AJAX, called when modal opens)
 * POST → Updates constants in DB, reloads global cache, redirects home
 *        Also handles AJAX action=saveLandingHtml for custom landing page content.
 */
@WebServlet(name = "UpdatePspSettings", value = "/UpdatePspSettings")
public class UpdatePspSettings extends HttpServlet {

    private static final String[] SMTP_KEYS = {
            "SMTP_SERVER", "SMTP_PORT", "SMTP_USER", "SMTP_PASSWORD", "SMTP_FROM", "EMAIL_FOOTER_TEXT"
    };

    private static final String[] FEATURE_KEYS = {
            "USE_TIMECLOCK", "USE_FRIENDLY_NAMES", "USE_CUSTOM_LANDING", "CHATBOT_ALL_USERS",
            "NOTES_AGENT_VISIBLE_DEFAULT"
    };

    // HTML sanitization patterns — strips scripts, event handlers, javascript: protocols.
    // Intentionally preserves <style> blocks for rich landing page HTML.
    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("<script[\\s\\S]*?>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_HANDLER_PATTERN =
            Pattern.compile("(?i)\\s*on[a-z]+\\s*=\\s*\"[^\"]*\"");
    private static final Pattern EVENT_HANDLER_SINGLE_PATTERN =
            Pattern.compile("(?i)\\s*on[a-z]+\\s*=\\s*'[^']*'");
    private static final Pattern JS_PROTOCOL_PATTERN =
            Pattern.compile("(?i)(href|src)\\s*=\\s*\"\\s*javascript:[^\"]*\"");
    private static final Pattern JS_PROTOCOL_SINGLE_PATTERN =
            Pattern.compile("(?i)(href|src)\\s*=\\s*'\\s*javascript:[^']*'");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            response.sendError(403, "PSP Admin required");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            StringBuilder json = new StringBuilder("{");

            // SMTP settings
            for (int i = 0; i < SMTP_KEYS.length; i++) {
                String val = AppConstantDAO.getConstantValue(em, SMTP_KEYS[i]);
                if (val == null) val = "";
                json.append("\"").append(SMTP_KEYS[i]).append("\":\"")
                    .append(escapeJson(val)).append("\"");
                json.append(",");
            }

            // Feature settings
            for (int i = 0; i < FEATURE_KEYS.length; i++) {
                String val = AppConstantDAO.getConstantValue(em, FEATURE_KEYS[i]);
                if (val == null) {
                    val = ("USE_CUSTOM_LANDING".equals(FEATURE_KEYS[i]) || "CHATBOT_ALL_USERS".equals(FEATURE_KEYS[i])) ? "false" : "true";
                }
                json.append("\"").append(FEATURE_KEYS[i]).append("\":\"")
                    .append(escapeJson(val)).append("\"");
                json.append(",");
            }

            // Numeric settings
            String dsw = AppConstantDAO.getConstantValue(em, "DAYS_SINCE_WARNING");
            if (dsw == null) dsw = "7";
            json.append("\"DAYS_SINCE_WARNING\":\"").append(escapeJson(dsw)).append("\",");

            // Landing page color settings
            String lhc = AppConstantDAO.getConstantValue(em, "LANDING_HEADER_COLOR");
            if (lhc == null || lhc.isBlank()) lhc = "#0d5681";
            json.append("\"LANDING_HEADER_COLOR\":\"").append(escapeJson(lhc)).append("\",");

            String lhtc = AppConstantDAO.getConstantValue(em, "LANDING_HEADER_TEXT_COLOR");
            if (lhtc == null || lhtc.isBlank()) lhtc = "#ffffff";
            json.append("\"LANDING_HEADER_TEXT_COLOR\":\"").append(escapeJson(lhtc)).append("\",");

            // Landing page HTML content (from text_value column)
            Constant clc = AppConstantDAO.getConstant(em, "CUSTOM_LANDING_HTML");
            String landingHtml = (clc != null && clc.getTextValue() != null) ? clc.getTextValue() : "";
            json.append("\"CUSTOM_LANDING_HTML\":\"").append(escapeJson(landingHtml)).append("\",");

            // AI API key status (never send full key)
            String dbApiKey = AppConstantDAO.getConstantValue(em, "ANTHROPIC_API_KEY");
            String keySource = "none";
            String keyHint = "";
            if (dbApiKey != null && !dbApiKey.isBlank()) {
                keySource = "database";
                keyHint = dbApiKey.length() > 4 ? "..." + dbApiKey.substring(dbApiKey.length() - 4) : "****";
            } else {
                String propsKey = AppConfig.get("ANTHROPIC_API_KEY");
                if (propsKey != null && !propsKey.isBlank() && !"FILL_ME_IN".equals(propsKey)) {
                    keySource = "properties";
                    keyHint = propsKey.length() > 4 ? "..." + propsKey.substring(propsKey.length() - 4) : "****";
                }
            }
            json.append("\"AI_KEY_SOURCE\":\"").append(escapeJson(keySource)).append("\",");
            json.append("\"AI_KEY_HINT\":\"").append(escapeJson(keyHint)).append("\"");

            json.append("}");
            out.print(json);
            out.flush();
        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            response.sendRedirect("ViewHome25");
            return;
        }

        // Check for AJAX actions
        String action = request.getParameter("action");
        if ("saveLandingHtml".equals(action)) {
            saveLandingHtml(request, response);
            return;
        }
        if ("saveApiKey".equals(action)) {
            saveApiKey(request, response);
            return;
        }
        if ("removeApiKey".equals(action)) {
            removeApiKey(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            // SMTP settings
            upsertConstant(em, "SMTP_SERVER", request.getParameter("smtpServer"));
            upsertConstant(em, "SMTP_PORT", request.getParameter("smtpPort"));
            upsertConstant(em, "SMTP_USER", request.getParameter("smtpUser"));
            upsertConstant(em, "SMTP_PASSWORD", request.getParameter("smtpPassword"));
            String from = request.getParameter("smtpFrom");
            upsertConstant(em, "SMTP_FROM", from != null ? from.trim() : "");
            String footer = request.getParameter("emailFooterText");
            upsertConstant(em, "EMAIL_FOOTER_TEXT", footer != null ? footer.trim() : "");

            // Feature settings
            String useTimeclock = request.getParameter("useTimeclock");
            upsertConstant(em, "USE_TIMECLOCK", "on".equals(useTimeclock) ? "true" : "false");
            String useFriendlyNames = request.getParameter("useFriendlyNames");
            upsertConstant(em, "USE_FRIENDLY_NAMES", "on".equals(useFriendlyNames) ? "true" : "false");
            String useCustomLanding = request.getParameter("useCustomLanding");
            upsertConstant(em, "USE_CUSTOM_LANDING", "on".equals(useCustomLanding) ? "true" : "false");
            String chatbotAllUsers = request.getParameter("chatbotAllUsers");
            upsertConstant(em, "CHATBOT_ALL_USERS", "on".equals(chatbotAllUsers) ? "true" : "false");
            String notesAgentVisibleDefault = request.getParameter("notesAgentVisibleDefault");
            upsertConstant(em, "NOTES_AGENT_VISIBLE_DEFAULT", "on".equals(notesAgentVisibleDefault) ? "true" : "false");

            // Landing page color settings
            String headerColor = request.getParameter("landingHeaderColor");
            upsertConstant(em, "LANDING_HEADER_COLOR", headerColor != null && !headerColor.isBlank() ? headerColor.trim() : "#0d5681");
            String headerTextColor = request.getParameter("landingHeaderTextColor");
            upsertConstant(em, "LANDING_HEADER_TEXT_COLOR", headerTextColor != null && !headerTextColor.isBlank() ? headerTextColor.trim() : "#ffffff");

            // Numeric settings
            String dswParam = request.getParameter("daysSinceWarning");
            int dsw = 7;
            try { dsw = Integer.parseInt(dswParam); } catch (Exception ignored) {}
            if (dsw < 0) dsw = 0;
            if (dsw > 99) dsw = 99;
            upsertConstant(em, "DAYS_SINCE_WARNING", String.valueOf(dsw));

            em.getTransaction().commit();

            // Reload global data so cached flags update immediately
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            if (global != null) {
                global.initializeGlobalData(em);
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            response.sendRedirect("ViewHome25?error=settings");
            return;
        } finally {
            em.close();
        }

        response.sendRedirect("ViewHome25");
    }

    /**
     * AJAX handler: saves custom landing page HTML into the CUSTOM_LANDING_HTML constant's text_value column.
     */
    private void saveLandingHtml(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            String htmlContent = request.getParameter("landingHtml");
            String sanitized = sanitizeHtml(htmlContent);

            em.getTransaction().begin();
            Constant c = AppConstantDAO.getConstant(em, "CUSTOM_LANDING_HTML");
            if (c != null) {
                c.setTextValue(sanitized);
                em.merge(c);
            } else {
                c = new Constant();
                c.setName("CUSTOM_LANDING_HTML");
                c.setValue("");
                c.setTextValue(sanitized);
                em.persist(c);
            }
            em.getTransaction().commit();

            // Reload global cache
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            if (global != null) {
                global.initializeGlobalData(em);
            }

            response.setContentType("application/json");
            response.getWriter().print("{\"status\":\"ok\"}");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().print("{\"status\":\"error\"}");
        } finally {
            em.close();
        }
    }

    /**
     * AJAX handler: validates an Anthropic API key, then saves it to the DB constants table.
     */
    private void saveApiKey(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            String apiKey = request.getParameter("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                sendJsonResponse(response, "error", "No API key provided");
                return;
            }
            apiKey = apiKey.trim();

            // Validate against Anthropic API
            String error = ClaudeApiService.validateApiKey(apiKey);
            if (error != null) {
                sendJsonResponse(response, "error", error);
                return;
            }

            // Save to DB
            em.getTransaction().begin();
            upsertConstant(em, "ANTHROPIC_API_KEY", apiKey);
            em.getTransaction().commit();

            // Update cached key and reload global
            AppConfig.setAnthropicApiKey(apiKey);
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            if (global != null) {
                global.initializeGlobalData(em);
            }

            String hint = apiKey.length() > 4 ? "..." + apiKey.substring(apiKey.length() - 4) : "****";
            response.setContentType("application/json");
            response.getWriter().print("{\"status\":\"ok\",\"hint\":\"" + escapeJson(hint) + "\"}");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            sendJsonResponse(response, "error", "Save failed");
        } finally {
            em.close();
        }
    }

    /**
     * AJAX handler: removes the API key by setting the DB constant to empty string.
     * This explicitly overrides any ssa.properties fallback.
     */
    private void removeApiKey(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            upsertConstant(em, "ANTHROPIC_API_KEY", "");
            em.getTransaction().commit();

            AppConfig.setAnthropicApiKey("");
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            if (global != null) {
                global.initializeGlobalData(em);
            }

            sendJsonResponse(response, "ok", "API key removed");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            sendJsonResponse(response, "error", "Remove failed");
        } finally {
            em.close();
        }
    }

    private void sendJsonResponse(HttpServletResponse response, String status, String message) throws IOException {
        response.setContentType("application/json");
        response.getWriter().print("{\"status\":\"" + escapeJson(status) + "\",\"message\":\"" + escapeJson(message) + "\"}");
    }

    /**
     * Create-or-update a Constant row. Handles constants that don't exist yet
     * (e.g., USE_TIMECLOCK on first save for existing installations).
     */
    private void upsertConstant(EntityManager em, String name, String value) {
        Constant c = AppConstantDAO.getConstant(em, name);
        if (c != null) {
            c.setValue(value != null ? value.trim() : "");
            em.merge(c);
        } else {
            c = new Constant();
            c.setName(name);
            c.setValue(value != null ? value.trim() : "");
            em.persist(c);
        }
    }

    private boolean isPspAdmin(HttpServletRequest request) {
        Boolean admin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return admin != null && admin;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * HTML sanitization for trusted PSP admin users.
     * Strips: <script> tags, on* event handlers, javascript: protocols.
     * Preserves: <style> blocks, inline styles, CSS variables.
     */
    static String sanitizeHtml(String html) {
        if (html == null) return "";
        String result = html;
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = EVENT_HANDLER_PATTERN.matcher(result).replaceAll("");
        result = EVENT_HANDLER_SINGLE_PATTERN.matcher(result).replaceAll("");
        result = JS_PROTOCOL_PATTERN.matcher(result).replaceAll("$1=\"\"");
        result = JS_PROTOCOL_SINGLE_PATTERN.matcher(result).replaceAll("$1=''");
        return result;
    }
}

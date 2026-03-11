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

/**
 * BPO Settings — Admin modal for managing AI configuration.
 *
 * GET  → Returns current AI key status + BPO chatbot toggle as JSON
 * POST → Handles AJAX actions: saveApiKey, removeApiKey, saveSettings
 */
@WebServlet(name = "UpdateBpoSettings", value = "/UpdateBpoSettings")
public class UpdateBpoSettings extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isBpoAdmin(request)) {
            response.sendRedirect("BpoHome");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            PrintWriter out = response.getWriter();
            StringBuilder json = new StringBuilder("{");

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
            json.append("\"AI_KEY_HINT\":\"").append(escapeJson(keyHint)).append("\",");

            // Chatbot toggle for BPO users
            String chatbotAllBpo = AppConstantDAO.getConstantValue(em, "CHATBOT_ALL_BPO_USERS");
            json.append("\"CHATBOT_ALL_BPO_USERS\":").append("true".equalsIgnoreCase(chatbotAllBpo));

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

        if (!isBpoAdmin(request)) {
            response.sendRedirect("BpoHome");
            return;
        }

        String action = request.getParameter("action");
        if ("saveApiKey".equals(action)) {
            saveApiKey(request, response);
            return;
        }
        if ("removeApiKey".equals(action)) {
            removeApiKey(request, response);
            return;
        }
        if ("saveSettings".equals(action)) {
            saveSettings(request, response);
            return;
        }

        // Unknown action — redirect home
        response.sendRedirect("BpoHome");
    }

    /**
     * AJAX: validates an Anthropic API key, then saves it to the DB constants table.
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
            reloadGlobal(em);

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
     * AJAX: removes the API key by setting the DB constant to empty string.
     */
    private void removeApiKey(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            upsertConstant(em, "ANTHROPIC_API_KEY", "");
            em.getTransaction().commit();

            AppConfig.setAnthropicApiKey("");
            reloadGlobal(em);

            sendJsonResponse(response, "ok", "API key removed");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            sendJsonResponse(response, "error", "Remove failed");
        } finally {
            em.close();
        }
    }

    /**
     * AJAX: saves the chatbot toggle for BPO users.
     */
    private void saveSettings(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            String chatbotAllBpo = request.getParameter("chatbotAllBpoUsers");
            upsertConstant(em, "CHATBOT_ALL_BPO_USERS", "true".equals(chatbotAllBpo) ? "true" : "false");
            em.getTransaction().commit();

            reloadGlobal(em);

            sendJsonResponse(response, "ok", "Settings saved");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            sendJsonResponse(response, "error", "Save failed");
        } finally {
            em.close();
        }
    }

    // ── Helpers ──

    private void reloadGlobal(EntityManager em) {
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global != null) {
            global.initializeGlobalData(em);
        }
    }

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

    private boolean isBpoAdmin(HttpServletRequest request) {
        Boolean admin = (Boolean) request.getSession().getAttribute("isBpoAdmin");
        return admin != null && admin;
    }

    private void sendJsonResponse(HttpServletResponse response, String status, String message) throws IOException {
        response.setContentType("application/json");
        response.getWriter().print("{\"status\":\"" + escapeJson(status) + "\",\"message\":\"" + escapeJson(message) + "\"}");
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

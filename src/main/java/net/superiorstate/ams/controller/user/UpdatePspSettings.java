package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.model.Constant;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * PSP Settings — Admin modal for viewing/updating SMTP and feature configuration.
 *
 * GET  → Returns current settings as JSON (AJAX, called when modal opens)
 * POST → Updates constants in DB, reloads global cache, redirects home
 */
@WebServlet(name = "UpdatePspSettings", value = "/UpdatePspSettings")
public class UpdatePspSettings extends HttpServlet {

    private static final String[] SMTP_KEYS = {
            "SMTP_SERVER", "SMTP_PORT", "SMTP_USER", "SMTP_PASSWORD", "SMTP_FROM", "EMAIL_FOOTER_TEXT"
    };

    private static final String[] FEATURE_KEYS = {
            "USE_TIMECLOCK", "USE_FRIENDLY_NAMES"
    };

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
                if (val == null) val = "true";
                json.append("\"").append(FEATURE_KEYS[i]).append("\":\"")
                    .append(escapeJson(val)).append("\"");
                json.append(",");
            }

            // Numeric settings
            String dsw = AppConstantDAO.getConstantValue(em, "DAYS_SINCE_WARNING");
            if (dsw == null) dsw = "7";
            json.append("\"DAYS_SINCE_WARNING\":\"").append(escapeJson(dsw)).append("\"");

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
}

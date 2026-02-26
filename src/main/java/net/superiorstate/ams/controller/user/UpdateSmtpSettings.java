package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.model.Constant;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * SMTP Settings — PSP Admin modal for viewing/updating SMTP configuration.
 *
 * GET  → Returns current SMTP values as JSON (AJAX, called when modal opens)
 * POST → Updates SMTP constants in DB, redirects back to referring page
 */
@WebServlet(name = "UpdateSmtpSettings", value = "/UpdateSmtpSettings")
public class UpdateSmtpSettings extends HttpServlet {

    private static final String[] SMTP_KEYS = {
            "SMTP_SERVER", "SMTP_PORT", "SMTP_USER", "SMTP_PASSWORD", "SMTP_FROM", "EMAIL_FOOTER_TEXT"
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
            for (int i = 0; i < SMTP_KEYS.length; i++) {
                String val = AppConstantDAO.getConstantValue(em, SMTP_KEYS[i]);
                if (val == null) val = "";
                json.append("\"").append(SMTP_KEYS[i]).append("\":\"")
                    .append(escapeJson(val)).append("\"");
                if (i < SMTP_KEYS.length - 1) json.append(",");
            }
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

            updateConstant(em, "SMTP_SERVER", request.getParameter("smtpServer"));
            updateConstant(em, "SMTP_PORT", request.getParameter("smtpPort"));
            updateConstant(em, "SMTP_USER", request.getParameter("smtpUser"));
            updateConstant(em, "SMTP_PASSWORD", request.getParameter("smtpPassword"));

            // SMTP_FROM is optional — save blank if not provided
            String from = request.getParameter("smtpFrom");
            updateConstant(em, "SMTP_FROM", from != null ? from.trim() : "");

            // EMAIL_FOOTER_TEXT — custom footer for outbound emails
            String footer = request.getParameter("emailFooterText");
            updateConstant(em, "EMAIL_FOOTER_TEXT", footer != null ? footer.trim() : "");

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            response.sendRedirect("ViewHome25?error=smtp");
            return;
        } finally {
            em.close();
        }

        // Redirect back to referring page (or home)
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect("ViewHome25");
        }
    }

    private void updateConstant(EntityManager em, String name, String value) {
        Constant c = AppConstantDAO.getConstant(em, name);
        if (c != null) {
            c.setValue(value != null ? value.trim() : "");
            em.merge(c);
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

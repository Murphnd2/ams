package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.util.EmailTemplate;
import net.superiorstate.ams.model.general.User;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@WebServlet(name = "HelpUserLogin", value = "/HelpUserLogin")
public class HelpUserLogin extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = findUser(request);
        if (user == null) {
            response.sendRedirect("NeedsHelp?status=notfound");
            return;
        }

        try {
            sendRequestEmail(user, request);
            response.sendRedirect("NeedsHelp?status=sent");
        } catch (Exception e) {
            System.err.println("❌ Failed to send login help email: " + e.getMessage());
            response.sendRedirect("NeedsHelp?status=notfound");
        }
    }

    private void sendRequestEmail(User user, HttpServletRequest request) throws MessagingException {
        int helpMethod = Integer.parseInt(request.getParameter("submitButton"));
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            String guid = UUID.randomUUID().toString();
            String subject;
            String body;

            // Resolve WEB_PATH for link generation
            String webPath = resolveWebPath(em, user);
            String link = webPath + "/OneTimeUserLogin?guid=" + guid;

            // Get PSP name for branding
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            String pspName = (global != null && global.getPsp() != null)
                    ? global.getPsp().getFullName() : "Activity Management System";

            if (helpMethod == 0) {
                // One-Time Login — 10 minute expiry, no password reset
                updateUserData(em, user, false, guid, 10);
                subject = "Your One-Time Login Link";
                body = buildOneTimeLoginEmail(link, user.getPerson().getFirstName(), pspName);
            } else {
                // Password Reset — 7 day expiry, allow password set
                updateUserData(em, user, true, guid, 7 * 24 * 60);
                subject = "Password Reset Request";
                body = buildPasswordResetEmail(link, user.getPerson().getFirstName(), pspName);
            }

            // Wrap in PSP-branded template
            String wrappedBody = EmailTemplate.wrapBodyOnly(body, pspName, em);

            // V069: system/security mail always sends as noreply@superiorstate.net (never white-labeled), no Reply-To.
            EmailDAO.sendEmail(net.superiorstate.ams.data.util.EmailIdentityResolver.systemIdentity(),
                    java.util.List.of(user.getEmail()),
                    java.util.Collections.emptyList(), java.util.Collections.emptyList(),
                    subject, wrappedBody, em);
            System.out.println("✅ Login help email sent to " + user.getEmail() + " (method=" + helpMethod + ")");
        } finally {
            em.close();
        }
    }

    private String buildPasswordResetEmail(String link, String firstName, String pspName) {
        return "<p>Hello " + firstName + ",</p>"
            + "<p>We received a request to reset your password for your <strong>" + pspName + "</strong> account.</p>"
            + "<p>Click the button below to set a new password:</p>"
            + "<p style=\"text-align:center; margin:24px 0;\">"
            + "<a href=\"" + link + "\" style=\"background:#0d5681; color:white; "
            + "padding:12px 28px; text-decoration:none; border-radius:6px; font-weight:bold;\">"
            + "Reset My Password</a></p>"
            + "<p style=\"font-size:0.85rem; color:#6c757d;\">This link expires in 7 days. "
            + "If you didn't request this, you can safely ignore this email.</p>";
    }

    private String buildOneTimeLoginEmail(String link, String firstName, String pspName) {
        return "<p>Hello " + firstName + ",</p>"
            + "<p>You requested a one-time login link for your <strong>" + pspName + "</strong> account.</p>"
            + "<p>Click the button below to log in immediately:</p>"
            + "<p style=\"text-align:center; margin:24px 0;\">"
            + "<a href=\"" + link + "\" style=\"background:#0d5681; color:white; "
            + "padding:12px 28px; text-decoration:none; border-radius:6px; font-weight:bold;\">"
            + "Log In Now</a></p>"
            + "<p style=\"font-size:0.85rem; color:#6c757d;\">This link expires in 10 minutes and can only be used once. "
            + "If you didn't request this, you can safely ignore this email.</p>";
    }

    /**
     * Resolve the base URL for email links.
     * Priority: WEB_PATH constant → derive from user's email domain.
     */
    private String resolveWebPath(EntityManager em, User user) {
        String webPath = AppConstantDAO.getConstantValue(em, "WEB_PATH");
        if (webPath == null || webPath.isBlank()) {
            String emailDomain = user.getEmail().substring(user.getEmail().indexOf("@") + 1);
            webPath = "https://" + emailDomain;
        }
        if (webPath.endsWith("/")) webPath = webPath.substring(0, webPath.length() - 1);
        return webPath;
    }

    /**
     * Update user's GUID, expiration, and password-reset flag.
     * @param expirationMinutes how many minutes until the GUID expires
     */
    public static void updateUserData(EntityManager em, User user, boolean allowSetPassword, String guid, int expirationMinutes) {
        em.getTransaction().begin();
        Query q = em.createQuery("SELECT u FROM User u WHERE u.userName = :username");
        q.setParameter("username", user.getUserName());
        User thisUser = (User) q.getSingleResult();
        thisUser.setTempGuid(guid);
        thisUser.setGuidUsed(false);
        thisUser.setAllowSetPassword(allowSetPassword);

        LocalDateTime expirationDateTime = LocalDateTime.now().plus(Duration.of(expirationMinutes, ChronoUnit.MINUTES));
        java.util.Date expirationDate = java.util.Date.from(expirationDateTime.atZone(ZoneId.systemDefault()).toInstant());
        thisUser.setGuidExpiration(expirationDate);

        em.persist(thisUser);
        em.getTransaction().commit();
    }

    /**
     * Legacy overload — defaults to 10 minute expiration.
     * Used by older code that calls updateUserData without specifying minutes.
     */
    public static void updateUserData(EntityManager em, User user, boolean allowSetPassword, String guid) {
        updateUserData(em, user, allowSetPassword, guid, 10);
    }

    private User findUser(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            String userNameOrEmail = request.getParameter("userName");
            return AuthDAO.getUserByUserName(em, userNameOrEmail);
        } finally {
            em.close();
        }
    }
}

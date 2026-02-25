package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.dao.TimeTrackingDAO;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.TimeLog;
import net.superiorstate.ams.model.general.User;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Processes GUID-based login links (welcome emails, password resets, one-time logins).
 * <p>
 * If user.allowSetPassword = true → forward to set-password form.
 * If user.allowSetPassword = false → one-time auto-login, redirect to home.
 * <p>
 * No instance variables — all state is request-scoped (servlet singleton safe).
 */
@WebServlet(name = "OneTimeUserLogin", value = "/OneTimeUserLogin")
public class OneTimeUserLogin extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = request.getParameter("guid");
        if (guid == null || guid.isBlank()) {
            forwardToError(request, response, "Invalid or missing login link.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Find user by GUID
            User user = findUserByGuid(em, guid);
            if (user == null) {
                forwardToError(request, response, "This login link is not valid.");
                return;
            }

            // Check expiration
            Date now = Date.from(Instant.now());
            if (user.getGuidExpiration() == null || user.getGuidExpiration().before(now)) {
                forwardToError(request, response, "This link has expired. Please request a new one from the login page.");
                return;
            }

            // Check if already used
            if (user.isGuidUsed()) {
                forwardToError(request, response, "This link has already been used. Please request a new one from the login page.");
                return;
            }

            // Mark GUID as used
            markGuidUsed(em, guid);

            if (user.isAllowSetPassword()) {
                // Password reset / welcome email flow → show set-password form
                request.getSession().setAttribute("tempUser", user);
                request.getRequestDispatcher("/WEB-INF/view/authentication/setPassword.jsp")
                        .forward(request, response);
            } else {
                // One-time login flow → full session setup and redirect to home
                authenticateAndRedirect(request, response, em, user);
            }

        } catch (Exception e) {
            e.printStackTrace();
            forwardToError(request, response, "An unexpected error occurred. Please try again.");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("index.jsp");
    }

    private User findUserByGuid(EntityManager em, String guid) {
        try {
            Query q = em.createQuery("SELECT u FROM User u WHERE u.tempGuid = :guid");
            q.setParameter("guid", guid);
            return (User) q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private void markGuidUsed(EntityManager em, String guid) {
        try {
            Query q = em.createQuery("SELECT u FROM User u WHERE u.tempGuid = :guid");
            q.setParameter("guid", guid);
            User user = (User) q.getSingleResult();
            em.getTransaction().begin();
            user.setGuidUsed(true);
            em.persist(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Full session initialization — mirrors AuthenticateUser.loadSessionData25().
     * Sets up AmsDataLocal, roles, timeclock, then redirects based on role.
     */
    private void authenticateAndRedirect(HttpServletRequest request, HttpServletResponse response,
                                          EntityManager em, User user) throws IOException {
        // 1. Load Person and put on session (needed by AmsDataLocal.intializeLocalData)
        Person person = AuthDAO.getPersonByUser(em, user);
        request.getSession().setAttribute("currentPerson", person);

        // 2. Create and initialize AmsDataLocal — same as AuthenticateUser
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        AmsDataLocal local = new AmsDataLocal(emf);
        local.setAuthenticated(true);
        local.intializeLocalData(em, request);

        // 3. Assign role flags to session (isPspAdmin, isAgent, etc.)
        AuthDAO.assignUserRoles(request, user);
        local.setPspAdmin((boolean) request.getSession().getAttribute("isPspAdmin"));

        // 4. Initialize timeclock state
        try {
            TimeLog lastPunch = TimeTrackingDAO.getMyLastPunch(em, user);
            local.setUserIsIn(lastPunch.isIn());
        } catch (Exception e) {
            local.setUserIsIn(false);
        }
        local.setMyTimeHistory(TimeTrackingDAO.getTodaysTimeHistory(em, person));

        // 5. Put local on session — this is what LoginFilter checks
        request.getSession().setAttribute("local", local);

        // 6. Role-based redirect — same as AuthenticateUser.goToPage()
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
        boolean isBpo = Boolean.TRUE.equals(request.getSession().getAttribute("isBpo"));
        boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));
        boolean isBpoUser = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoUser"));

        if (isBpo || isBpoAdmin || isBpoUser) {
            response.sendRedirect("BpoHome");
        } else if (isAgent || isAgencyAdmin) {
            response.sendRedirect("AgentHome");
        } else {
            response.sendRedirect("ViewHome25");
        }
    }

    private void forwardToError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("linkError", message);
        request.getRequestDispatcher("/WEB-INF/view/authentication/setPassword.jsp")
                .forward(request, response);
    }
}

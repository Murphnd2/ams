package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.model.general.User;

import java.io.IOException;

/**
 * Handles password reset form submission.
 * Expects tempUser in session (set by OneTimeUserLogin).
 * On success → redirect to login page.
 * On failure → forward back to set-password form with error.
 */
@WebServlet(name = "ResetLogin", value = "/ResetLogin")
public class ResetLogin extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pass1 = request.getParameter("newPassword1");
        String pass2 = request.getParameter("newPassword2");
        User tempUser = (User) request.getSession().getAttribute("tempUser");

        if (tempUser == null) {
            request.setAttribute("linkError", "Session expired. Please use your login link again.");
            request.getRequestDispatcher("/WEB-INF/view/authentication/setPassword.jsp")
                    .forward(request, response);
            return;
        }

        if (pass1 == null || pass1.isBlank()) {
            request.setAttribute("formError", "Password cannot be blank.");
            request.getRequestDispatcher("/WEB-INF/view/authentication/setPassword.jsp")
                    .forward(request, response);
            return;
        }

        if (!pass1.equals(pass2)) {
            request.setAttribute("formError", "Passwords do not match. Please try again.");
            request.getRequestDispatcher("/WEB-INF/view/authentication/setPassword.jsp")
                    .forward(request, response);
            return;
        }

        if (pass1.length() < 8) {
            request.setAttribute("formError", "Password must be at least 8 characters.");
            request.getRequestDispatcher("/WEB-INF/view/authentication/setPassword.jsp")
                    .forward(request, response);
            return;
        }

        // Update password using shared EMF
        boolean updated = updatePassword(tempUser, pass1);
        if (updated) {
            request.getSession().removeAttribute("tempUser");
            request.getSession().setAttribute("passwordResetSuccess", true);
            response.sendRedirect("index.jsp");
        } else {
            request.setAttribute("formError", "Unable to update password. Please contact your administrator.");
            request.getRequestDispatcher("/WEB-INF/view/authentication/setPassword.jsp")
                    .forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("index.jsp");
    }

    private boolean updatePassword(User user, String newPassword) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            String salt = AuthDAO.generateSalt();
            String hash = AuthDAO.generatePasswordHash(newPassword, salt);
            Query q = em.createQuery("SELECT u FROM User u WHERE u.userName = :username");
            q.setParameter("username", user.getUserName());
            User currentUser = (User) q.getSingleResult();
            em.getTransaction().begin();
            currentUser.setGuidUsed(true);
            currentUser.setAllowSetPassword(false);
            currentUser.setSalt(salt);
            currentUser.setPasswordHash(hash);
            em.persist(currentUser);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return false;
        } finally {
            em.close();
        }
    }
}

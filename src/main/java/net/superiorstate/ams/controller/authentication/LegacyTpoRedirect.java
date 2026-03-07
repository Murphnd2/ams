package net.superiorstate.ams.controller.authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Catches all legacy /tpo/* URLs from the old superiorstate.net IIS site.
 * Displays a friendly notice that the site has been updated and directs
 * visitors to contact their agent or Superior State directly.
 *
 * Public — bypasses LoginFilter via ALLOWED path prefix check.
 */
@WebServlet(name = "LegacyTpoRedirect", urlPatterns = {"/tpo", "/tpo/*"})
public class LegacyTpoRedirect extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/authentication/legacyTpo.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}

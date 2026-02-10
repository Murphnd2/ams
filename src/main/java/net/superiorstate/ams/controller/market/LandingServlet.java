package net.superiorstate.ams.controller.market; // <-- change to your package

import java.io.IOException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "LandingServlet", urlPatterns = {"/market/landing"})
public class LandingServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/view/market/landing.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Optional: if you want canonical no-cache during development:
        // response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        RequestDispatcher rd = request.getRequestDispatcher(VIEW);
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}

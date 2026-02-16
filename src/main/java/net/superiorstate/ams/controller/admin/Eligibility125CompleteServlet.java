package net.superiorstate.ams.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/Eligibility125Complete")
public class Eligibility125CompleteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // TODO: in the future you can:
        //  - read query params (guid, etc.)
        //  - write to your DB
        //  - send internal emails / logs

        // For now, just send them to the success JSP
        String context = request.getContextPath(); // "" or "/beta", etc.
        response.sendRedirect(context + "/125eligibilitySuccess.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        // If this ever gets a POST, just treat it like a GET
        doGet(request, response);
    }
}


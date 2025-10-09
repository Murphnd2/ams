package net.superiorstate.ams.previous.controller.activity.renewal;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;

@WebServlet(name = "ResetRenewal", value = "/ResetRenewal")
public class ResetRenewal extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        resetBasics(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        resetBasics(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RenewalHome");
        dispatcher.forward(request,response);
    }

    private void resetBasics(HttpServletRequest request){
        request.getSession().setAttribute("currentEmployer",new Employer());
        request.getSession().setAttribute("currentRenewal", new Renewal());
        request.getSession().setAttribute("renewalView",1001);
    }
}

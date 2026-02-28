package net.superiorstate.ams.controller.monthly;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;

@WebServlet(name = "BillBack", value = "/BillBack")
public class BillBack extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoBillingHome");
        dispatcher.forward(request, response);
    }

    private void changeView(HttpServletRequest request) {
        request.getSession().setAttribute("billingView", 999);
        request.getSession().setAttribute("currentBillingEmployer", new Employer());
        request.getSession().setAttribute("currentBillingEmployee", new Employee());
    }
}

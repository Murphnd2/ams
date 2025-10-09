package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.util.Objects;

@WebServlet(name = "ShowTicketActivities", value = "/ShowTicketActivities")
public class ShowTicketActivities extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        request.getSession().setAttribute("aFilter","Ticket");
        String vT = (String) request.getSession().getAttribute("vT");
        request.getSession().setAttribute("vT","ON");
        if(Objects.equals(vT, "ON"))
            request.getSession().setAttribute("vT","OFF");
        request.getSession().setAttribute("followUp",0);
        request.getSession().setAttribute("onUs",0);

    }
}

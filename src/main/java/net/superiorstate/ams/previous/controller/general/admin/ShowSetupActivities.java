package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.util.Objects;

@WebServlet(name = "ShowSetupActivities", value = "/ShowSetupActivities")
public class ShowSetupActivities extends HttpServlet {
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
        request.getSession().setAttribute("aFilter","Setup");
        String vS = (String) request.getSession().getAttribute("vS");
        request.getSession().setAttribute("vS","ON");
        if(Objects.equals(vS, "ON"))
            request.getSession().setAttribute("vS","OFF");

        request.getSession().setAttribute("rFlag","");
        request.getSession().setAttribute("followUp",0);
        request.getSession().setAttribute("onUs",0);
    }
}

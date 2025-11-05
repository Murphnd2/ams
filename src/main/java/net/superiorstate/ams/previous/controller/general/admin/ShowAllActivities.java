package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "ShowAllActivities", value = "/ShowAllActivities")
public class ShowAllActivities extends HttpServlet {
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
        request.getSession().setAttribute("aFilter","Activity");
        String vA = (String) request.getSession().getAttribute("vA");
        request.getSession().setAttribute("vA", "ALL");
        if(vA.equals("ALL"))
            request.getSession().setAttribute("vA","MINE");

        request.getSession().setAttribute("rFlag","");
        request.getSession().setAttribute("followUp",0);
        request.getSession().setAttribute("onUs",0);
    }
}

package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "ShowFollowUps", value = "/ShowFollowUps")
public class ShowFollowUps extends HttpServlet {
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
        request.getSession().setAttribute("aFilter","FollowUp");
        int followUp = (Integer) request.getSession().getAttribute("followUp");
        if(followUp==1){
            request.getSession().setAttribute("followUp",0);
        } else {
            request.getSession().setAttribute("followUp",1);
            request.getSession().setAttribute("onUs",0);
        }
    }
}

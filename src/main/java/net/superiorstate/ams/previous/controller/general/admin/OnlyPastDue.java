package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "OnlyPastDue", value = "/OnlyPastDue")
public class OnlyPastDue extends HttpServlet {
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

        String onlyPast;
        try{
            onlyPast = (String) request.getSession().getAttribute("onlyPast");
        } catch (Exception e){
            onlyPast = "N";
        }
        if(onlyPast.equals("Y"))
            request.getSession().setAttribute("onlyPast","N");
        else
            request.getSession().setAttribute("onlyPast","Y");
        System.out.println(request.getSession().getAttribute("onlyPast").toString());
    }
}

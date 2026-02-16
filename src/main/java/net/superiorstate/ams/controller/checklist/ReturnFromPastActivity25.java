package net.superiorstate.ams.controller.checklist;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "ReturnFromPastActivity25", value = "/ReturnFromPastActivity25")
public class ReturnFromPastActivity25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoActivityDetail25");
        dispatcher.forward(request,response);
    }
    private void changeView(HttpServletRequest request){
        Long activityId = (Long) request.getSession().getAttribute("originalActivityId");
        String pastActivityId = activityId.toString();
        request.getSession().setAttribute("vp","1");
        request.getSession().setAttribute("pastActivityId",pastActivityId);

    }
}

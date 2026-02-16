package net.superiorstate.ams.controller.activity;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;

import java.io.IOException;

@WebServlet(name = "ViewPastActivity25", value = "/ViewPastActivity25")
public class ViewPastActivity25 extends HttpServlet {
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
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        request.getSession().setAttribute("originalActivityId",local.getCurrentActivity().getActivity().getId());
        String pastActivityId = request.getParameter("pastActivityId");
        request.getSession().setAttribute("vp","1");
        request.getSession().setAttribute("pastActivityId",pastActivityId);

    }
}

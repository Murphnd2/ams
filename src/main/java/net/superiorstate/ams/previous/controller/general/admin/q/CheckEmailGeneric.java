package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.service.ReadInboundEmailService;

import java.io.IOException;

@WebServlet(name = "CheckEmailGeneric", value = "/CheckEmailGeneric")
public class CheckEmailGeneric extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/adminHome.jsp");
        dispatcher.forward(request,response);
    }

    private void doThisFirst(HttpServletRequest request){
        ReadInboundEmailService readInboundEmailService = new ReadInboundEmailService();
        readInboundEmailService.check("kevin@superiorstate.net","Adelight@");
    }
}

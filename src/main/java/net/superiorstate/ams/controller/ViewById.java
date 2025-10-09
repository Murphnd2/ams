package net.superiorstate.ams.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "ViewById", value = "/ViewById")
public class ViewById extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    private void processData(HttpServletRequest request) {

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        request.setAttribute("btnViewActivity",id);
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoActivityDetail25");
        dispatcher.forward(request,response);
    }
}

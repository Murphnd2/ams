package net.superiorstate.ams.controller.monthly.initial;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "UpdateInitialTables", value = "/UpdateInitialTables")
public class UpdateInitialTables extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        InitialHelper.markTaskAndForward(request,response,35L,"UpdateTables25");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}

// File: src/main/java/net/superiorstate/ams/controller/ReOpenToDo25.java
package net.superiorstate.ams.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;

import java.io.IOException;

@WebServlet(name = "ReOpenToDo25", value = "/ReOpenToDo25")
public class ReOpenToDo25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String toDoIdParam = request.getParameter("btnToDo");
        if (toDoIdParam == null) {
            response.setStatus(400);
            return;
        }

        long toDoId = Long.parseLong(toDoIdParam);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null) {
            response.setStatus(400);
            return;
        }

        ToDoOut25 t = local.retrieveToDoOutFromList(local.getCurrentActivity().getToDoList(), toDoId);
        if (t != null) {
            // IN-MEMORY REOPEN
            t.setComplete(false);
            t.getToDo().setComplete(false);
            t.getToDo().setCompletedBy(null);
            t.getToDo().setDateCompleted(null);
        }

        local.respondToActivityUpdate(null, "TODO_TOGGLE", toDoId);

        // Return JSON
        response.setContentType("application/json");
        response.getWriter().write("{\"success\": true}");
    }
}

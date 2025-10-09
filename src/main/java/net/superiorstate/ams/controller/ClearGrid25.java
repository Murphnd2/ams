package net.superiorstate.ams.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.GenSeq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ClearGrid25", value = "/ClearGrid25")
public class ClearGrid25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GenSeq g = new GenSeq();
        g.setDescription("");

        List<GenSeq> endList = new ArrayList<>();
        endList.add(g);

        request.getSession().setAttribute("listBuilder", endList);

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/a/general/sequenceBuilder/sequenceBuilderForm.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // POST not used for this servlet
    }
}


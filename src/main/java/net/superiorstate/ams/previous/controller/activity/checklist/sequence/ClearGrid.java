package net.superiorstate.ams.previous.controller.activity.checklist.sequence;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.GenSeq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ClearGrid", value = "/ClearGrid")
public class ClearGrid extends HttpServlet {
    private RequestDispatcher dispatcher;
    public RequestDispatcher getDispatcher() {
        return dispatcher;
    }
    public void setDispatcher(RequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setDispatcher(request.getRequestDispatcher("/WEB-INF/view/checklist/checklistBuilder.jsp"));
        GenSeq g = new GenSeq();
        g.setDescription("");
        List<GenSeq> endList = new ArrayList<>();
        endList.add(g);
        request.getSession().setAttribute("listBuilder",endList);
        getDispatcher().forward(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}

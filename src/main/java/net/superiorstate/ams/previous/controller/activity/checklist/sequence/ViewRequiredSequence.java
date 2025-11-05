package net.superiorstate.ams.previous.controller.activity.checklist.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.general.PSP;

import java.io.IOException;

@WebServlet(name = "ViewRequiredSequence", value = "/ViewRequiredSequence")
public class ViewRequiredSequence extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewSequence(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewSequence(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RequiredSequenceBuilder");
        dispatcher.forward(request,response);
    }

    private void viewSequence(HttpServletRequest request){
        System.out.println("1");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        Long seqId = Long.parseLong(request.getParameter("reqListSelection"));
        request.getSession().setAttribute("currentReqList", dM.getReqListById(em,seqId));
        request.getSession().setAttribute("currentRecList", new RecurringTaskList());
        request.getSession().setAttribute("sequenceView",1);
    }
}

package net.superiorstate.ams.previous.controller.activity.checklist.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.general.PSP;

import java.io.IOException;

@WebServlet(name = "AddRequiredSequence", value = "/AddRequiredSequence")
public class AddRequiredSequence extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addSequence(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addSequence(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RequiredSequenceBuilder");
        dispatcher.forward(request,response);
    }

    private void addSequence(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        int tpId = Integer.parseInt(request.getParameter("templatePurposeList"));
        TemplatePurpose tp = dM.getTemplatePurposeById(em,tpId);
        String name = "(" +tp.getTemplateGroup().getDescription() +") " + tp.getDescription();
        request.getSession().setAttribute("currentReqList", dC.createReqList(em,name,psp,tp));
        request.getSession().setAttribute("currentRecList", new RecurringTaskList());
        request.getSession().setAttribute("sequenceView",1);
        em.close();
    }
}

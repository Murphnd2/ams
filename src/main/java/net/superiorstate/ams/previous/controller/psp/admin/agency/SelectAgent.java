package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;

import java.io.IOException;

@WebServlet(name = "SelectAgent", value = "/SelectAgent")
public class SelectAgent extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillAgentData(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillAgentData(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }
    private void fillAgentData(HttpServletRequest request,HttpServletResponse response){
        long agentId = Long.parseLong(request.getParameter("btnAgentSelect"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Person currentAgent = dM.getPersonById(em, agentId);
        request.getSession().setAttribute("hasCurrentAgent",true);
        request.getSession().setAttribute("currentAgent",currentAgent);
        request.getSession().setAttribute("formDisable2",true);
        Prospect prospect = new Prospect();
        request.getSession().setAttribute("hasCurrentProspect",false);
        request.getSession().setAttribute("currentProspect",prospect);

        dG.setAgencyAccordion(request,2);
    }
}

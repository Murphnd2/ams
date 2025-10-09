package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;

import java.io.IOException;

@WebServlet(name = "ProposalView", value = "/ProposalView")
public class ProposalView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillProspectData(request);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillProspectData(request);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }
    private void fillProspectData(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        long prospectId = Long.parseLong(request.getParameter("prospectSelectButton"));
        Prospect prospect = dM.getProspectById(em, prospectId);
        request.getSession().setAttribute("hasCurrentProspect",true);
        request.getSession().setAttribute("currentProspect",prospect);
        dG.setAgencyAccordion(request,3);
        request.getSession().setAttribute("formDisable3",true);
        em.close();
    }
}

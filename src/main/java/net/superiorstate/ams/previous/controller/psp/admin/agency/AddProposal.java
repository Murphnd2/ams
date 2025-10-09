package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.misc.dPSP;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.offering.LOS;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AddProposal", value = "/AddProposal")
public class AddProposal extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addProposal(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addProposal(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }

    private void addProposal(HttpServletRequest request,HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        Prospect currentProspect = (Prospect) request.getSession().getAttribute("currentProspect");
        long rateId = Long.parseLong(request.getParameter("ddRateList"));
        Rate rate = dM.getRateById(em,rateId);

        Proposal proposal = new Proposal();
        proposal.setProspect(currentProspect);
        proposal.setRate(rate);
        proposal.setInactive(false);
        proposal.setApplicationGUID(UUID.randomUUID().toString());

        em.getTransaction().begin();
        em.persist(proposal);
        em.getTransaction().commit();

        em.getTransaction().begin();
        List<LOS> pspLosList = dPSP.getLOS(psp);
        String buttonName;
        for(int i=0;i<pspLosList.size();i++){
            buttonName = "chkLos" + pspLosList.get(i).getId().toString();
            boolean isChecked = (request.getParameter(buttonName) != null);
            if(isChecked)
                proposal.addLos(pspLosList.get(i));
        }
        em.persist(proposal);
        em.getTransaction().commit();
        dG.setAgencyAccordion(request,4);
        em.close();

        request.getSession().setAttribute("hasCurrentRate",false);
    }
}

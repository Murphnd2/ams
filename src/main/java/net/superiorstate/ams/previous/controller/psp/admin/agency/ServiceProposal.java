package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;
import net.superiorstate.ams.previous.model.sales.agency.RateTable;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ServiceProposal", value = {"/serviceProposal"})
public class ServiceProposal extends HttpServlet {
    private Proposal activeProposal;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(locatedProposal(request)){
            fillProposalData(request,response);
        } else {
            goToNotFoundPage(request,response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //getProposalData(request,response);
    }

    public Proposal getActiveProposal() {
        return activeProposal;
    }

    public void setActiveProposal(Proposal activeProposal) {
        this.activeProposal = activeProposal;
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/agency/proposalMain.jsp");
        dispatcher.forward(request,response);
    }
    private void goToNotFoundPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/agency/noProposalFound.jsp");
        dispatcher.forward(request,response);
    }

    private void fillProposalData(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        EntityManager em = emf.createEntityManager();
        setValidSessionAttributes(request,getActiveProposal(),em);
        goToPage(request,response);
        em.close();
        emf.close();
    }

    private boolean locatedProposal(HttpServletRequest request){
        boolean isLocated = false;
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        EntityManager em = emf.createEntityManager();
        Proposal proposal = new Proposal();
        proposal.setId(0L);
        try{
            String proposalGuid = request.getParameter("guid");
            proposal = dG.getProposalByGuid(em,proposalGuid);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(proposal.getId()>0)
                isLocated = true;
            request.getSession().setAttribute("proposal",proposal);
            setActiveProposal(proposal);
            em.close();
            emf.close();
            return isLocated;
        }
    }
    private void setValidSessionAttributes(HttpServletRequest request,Proposal proposal, EntityManager em){
        Person agent = proposal.getProspect().getAgent();
        Prospect prospect = proposal.getProspect();
        Agency agency = proposal.getProspect().getAgent().getListOfAgenciesWithThisAgent().get(0);
        PSP psp = proposal.getProspect().getAgent().getPsp();
        List<ServiceModule> serviceModuleList = dG.getDistinctListOfServiceModulesForThisProposal(proposal);
        List<RateTable> rateTableList = dG.getPricing(em,proposal);
        request.getSession().setAttribute("propAgent",agent);
        request.getSession().setAttribute("propProspect",prospect);
        request.getSession().setAttribute("propPsp",psp);
        request.getSession().setAttribute("propAgency",agency);
        request.getSession().setAttribute("propModules",serviceModuleList);
        request.getSession().setAttribute("propPricing",rateTableList);
        request.getSession().setAttribute("hrefString",dG.getJotFormParameterString(em,proposal));
    }



}

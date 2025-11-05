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
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.offering.LOS;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "PspAgencyHome", value = "/PspAgencyHome")
public class PspAgencyHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillAgencyData(response,request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillAgencyData(response,request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/psp/admin/adminAgencyHome.jsp");
        dispatcher.forward(request,response);
    }

    private void fillAgencyData(HttpServletResponse response, HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        PSP psp = (PSP) request.getSession().getAttribute("psp");
        List<Agency> agencyList = dG.getAgencyList(em,Integer.parseInt(psp.getId().toString()));
        List<LOS> losList = dPSP.getLOS(psp);
        List<Person> pendingAgentList = dG.getPendingAgents(em,psp);
        List<Rate> rateList = dG.getRateList(em,Integer.parseInt(psp.getId().toString()));
        request.getSession().setAttribute("losList",losList);
        request.getSession().setAttribute("agencyList",agencyList);
        request.getSession().setAttribute("pendingAgentList",pendingAgentList);
        request.getSession().setAttribute("rateList",rateList);

        Boolean hasCurrentAgency = false;
        try{
            hasCurrentAgency = (boolean) request.getSession().getAttribute("hasCurrentAgency");
        } catch (Exception e){
            e.printStackTrace();
        }
        if(hasCurrentAgency)
            fillAgencyDetail(em,request);

        em.close();
    }

    private void fillAgencyDetail(EntityManager em,HttpServletRequest request){
        Agency currentAgency = (Agency) request.getSession().getAttribute("currentAgency");
        Agency agency = dG.getAgencyFull(em,currentAgency.getId());
        request.getSession().setAttribute("currentAgency",agency);
        List<Person> agentList = dG.getAgencyAgents(em,agency.getId());
        request.getSession().setAttribute("agencyAgentList",agentList);
        List<Rate> agencyRateList = dG.getRatesByAgency(em,currentAgency);
        Collections.sort(agencyRateList);
        request.getSession().setAttribute("agencyRateList",agencyRateList);

        Boolean hasCurrentAgent = false;
        try{
            hasCurrentAgent= (boolean) request.getSession().getAttribute("hasCurrentAgent");
        } catch (Exception e){
            e.printStackTrace();
            hasCurrentAgent = false;
        }
        if(hasCurrentAgent) {
            fillAgentData(em, request);
        } else {
            List<Prospect> prospectList = dG.getAgencyProspects(em,agency);
            request.getSession().setAttribute("prospectList",prospectList);
        }

        Boolean hasCurrentProspect = false;
        try{
            hasCurrentProspect = (boolean) request.getSession().getAttribute("hasCurrentProspect");
        } catch (Exception e){
            e.printStackTrace();
            hasCurrentProspect = false;
        }
        if(hasCurrentProspect)
            fillProspectData(em,request);
    }

    private void fillAgentData(EntityManager em,HttpServletRequest request){
        Person currentAgent = (Person) request.getSession().getAttribute("currentAgent");
        Person agent = dG.getAgentWithAddress(em,currentAgent.getId());
        request.getSession().setAttribute("currentAgent",agent);
        List<Prospect> prospectList = dG.getAgentProspects(em,agent);
        request.getSession().setAttribute("prospectList",prospectList);
    }

    private void fillProspectData(EntityManager em, HttpServletRequest request){
        Prospect currentProspect = (Prospect) request.getSession().getAttribute("currentProspect");
        Prospect prospect = dM.getProspectById(em,currentProspect.getId());
        List<Proposal> proposalList = dG.getProposalList(em,prospect);
        List<Proposal> proposalListFull = dG.getProposalListFull(em,prospect);
        request.getSession().setAttribute("currentProspect",prospect);
        request.getSession().setAttribute("proposalListFull",proposalListFull);
        request.getSession().setAttribute("proposalList",proposalList);
    }
}

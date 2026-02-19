package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@WebServlet(name = "ProposalBuilder", value = "/ProposalBuilder")
public class ProposalBuilder extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        EntityManager em = getEntityManager(request);
        em.clear();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        int pspId = local.getCurrentPerson().getPsp().getId().intValue();

        try {
            // Load agencies for this PSP
            List<Agency> agencyList = SalesDAO.getAgencyList(em, pspId);
            request.setAttribute("agencyList", agencyList);

            // Load all rates for this PSP (for PSP inside-sales users)
            List<Rate> allRates = SalesDAO.getRateList(em, pspId);
            request.setAttribute("allRates", allRates);

            // Load all LOS for this PSP
            List<LOS> losList = em.createNamedQuery("LOS.getByPsp", LOS.class)
                    .setParameter("psp_id", (long) pspId)
                    .getResultList();
            request.setAttribute("losList", losList);

            // Load existing prospects for this PSP
            List<Prospect> prospectList = SalesDAO.getProspectsByPsp(em, pspId);
            request.setAttribute("prospectList", prospectList);
            request.setAttribute("selectedProspect", request.getParameter("selectedProspect"));

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/proposalBuilder.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManager em = getEntityManager(request);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        try {
            String action = request.getParameter("action");

            if ("createProposal".equals(action)) {
                Proposal proposal = createProposal(request, em, local);
                response.sendRedirect("ProposalDetail?id=" + proposal.getId());
                return;
            }

        } finally {
            em.close();
        }

        // Redirect back to builder to show updated state

    }

    private Proposal createProposal(HttpServletRequest request, EntityManager em, AmsDataLocal local) {
        // Get prospect
        long prospectId = Long.parseLong(request.getParameter("prospectId"));
        Prospect prospect = em.find(Prospect.class, prospectId);

        // Get rate
        long rateId = Long.parseLong(request.getParameter("rateId"));
        Rate rate = em.find(Rate.class, rateId);

        // Get current user as creator
        Person createdBy = local.getCurrentPerson();

        // Generate GUID
        String guid = UUID.randomUUID().toString();

        // Create proposal
        em.getTransaction().begin();
        Proposal proposal = new Proposal();
        proposal.setProspect(prospect);
        proposal.setRate(rate);
        proposal.setApplicationGUID(guid);
        proposal.setStatus("CREATED");
        proposal.setCreatedBy(createdBy);
        proposal.setInactive(false);
        proposal.setLosList(new ArrayList<>());
        em.persist(proposal);
        em.getTransaction().commit();

        // Add selected LOSs
        String[] losIds = request.getParameterValues("losIds");
        if (losIds != null) {
            for (String losIdStr : losIds) {
                long losId = Long.parseLong(losIdStr);
                LOS los = SalesDAO.getLosFull(em, losId);
                em.getTransaction().begin();
                proposal.getLosList().add(los);
                los.getListOfProposalsThatIncludeThisLOS().add(proposal);
                em.persist(proposal);
                em.persist(los);
                em.getTransaction().commit();
            }
        }

        System.out.println("Proposal created: #" + proposal.getId() + " GUID=" + guid);
        return proposal;
    }

    private EntityManager getEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }
}

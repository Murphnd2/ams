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
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "ProposalBuilder", value = "/ProposalBuilder")
public class ProposalBuilder extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        EntityManager em = getEntityManager(request);
        em.clear();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        int pspId = local.getCurrentPerson().getPsp().getId().intValue();
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
        try {
            // Load agencies for this PSP
            List<Agency> agencyList = SalesDAO.getAgencyList(em, pspId);
            request.setAttribute("agencyList", agencyList);

            // Load rates - filter by agency for agent users
            List<Rate> allRates;
            if (isAgent || isAgencyAdmin) {
                Agency agency = findAgencyForUser(em, local.getCurrentPerson());
                if (agency != null) {
                    Agency fullAgency = SalesDAO.getAgencyFull(em, agency.getId());
                    allRates = fullAgency.getAgencyRateList() != null
                            ? new ArrayList<>(fullAgency.getAgencyRateList())
                            : new ArrayList<>();
                    allRates.removeIf(Rate::isSuppressed);
                } else {
                    allRates = new ArrayList<>();
                }
            } else {
                allRates = SalesDAO.getRateList(em, pspId);
                allRates.removeIf(Rate::isSuppressed);
            }
            request.setAttribute("allRates", allRates);

            // If only one rate, auto-select it
            if (allRates.size() == 1) {
                request.setAttribute("autoSelectedRateId", allRates.get(0).getId());
            }

            // Load all LOS for this PSP — filter out suppressed
            List<LOS> losList = em.createNamedQuery("LOS.getByPsp", LOS.class)
                    .setParameter("psp_id", (long) pspId)
                    .getResultList();
            losList.removeIf(LOS::isSuppressed);
            request.setAttribute("losList", losList);

            // Build rate → LOS availability map
            // A LOS is "available" for a rate if any of its ServiceModules appear in that rate's RateTable
            Map<Long, Set<Long>> rateLosMap = new HashMap<>();
            for (Rate rate : allRates) {
                List<RateTable> rtRows = SalesDAO.getRateTableList(em, rate.getId());
                Set<Long> availableLosIds = new HashSet<>();
                for (RateTable rt : rtRows) {
                    ServiceModule mod = rt.getModule();
                    // Check direct LOS FK on module
                    if (mod.getLos() != null) {
                        availableLosIds.add(mod.getLos().getId());
                    }
                    // Also check M:N relationship (losmodules table)
                    if (mod.getListOfLosWithThisModule() != null) {
                        for (LOS los : mod.getListOfLosWithThisModule()) {
                            availableLosIds.add(los.getId());
                        }
                    }
                }
                rateLosMap.put(rate.getId(), availableLosIds);
            }

            // Serialize to JSON string for the JSP
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<Long, Set<Long>> entry : rateLosMap.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":[");
                json.append(entry.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")));
                json.append("]");
                first = false;
            }
            json.append("}");
            request.setAttribute("rateLosMapJson", json.toString());

            // Load prospects - filter by agent/agency for non-PSP users
            List<Prospect> prospectList;


            if (isAgent && !isAgencyAdmin) {
                // Agent sees only their own prospects
                prospectList = em.createQuery(
                                "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id = :agentId ORDER BY p.name",
                                Prospect.class)
                        .setParameter("pspId", (long) pspId)
                        .setParameter("agentId", local.getCurrentPerson().getId())
                        .getResultList();
            } else if (isAgencyAdmin) {
                // Agency Manager sees all agency prospects
                Agency agency = findAgencyForUser(em, local.getCurrentPerson());
                if (agency != null && agency.getAgentList() != null) {
                    List<Long> agentIds = agency.getAgentList().stream()
                            .map(Person::getId).collect(java.util.stream.Collectors.toList());
                    prospectList = em.createQuery(
                                    "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id IN :agentIds ORDER BY p.name",
                                    Prospect.class)
                            .setParameter("pspId", (long) pspId)
                            .setParameter("agentIds", agentIds)
                            .getResultList();
                } else {
                    prospectList = new ArrayList<>();
                }
            } else {
                // PSP admin sees all
                prospectList = SalesDAO.getProspectsByPsp(em, pspId);
            }
            request.setAttribute("prospectList", prospectList);

// Support both parameter names for pre-selection
            String selectedProspect = request.getParameter("prospectId");
            if (selectedProspect == null) selectedProspect = request.getParameter("selectedProspect");
            request.setAttribute("selectedProspect", selectedProspect);

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
    private Agency findAgencyForUser(EntityManager em, Person person) {
        try {
            return em.createQuery("SELECT a FROM Agency a WHERE a.manager.id = :pid", Agency.class)
                    .setParameter("pid", person.getId())
                    .getSingleResult();
        } catch (Exception e) {
            try {
                return em.createQuery("SELECT a FROM Agency a JOIN a.agentList al WHERE al.id = :pid", Agency.class)
                        .setParameter("pid", person.getId())
                        .getSingleResult();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}

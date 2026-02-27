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
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));

        try {
            // ── Pass role flags to JSP ──
            request.setAttribute("isPspAdmin", isPspAdmin);
            request.setAttribute("isAgent", isAgent);
            request.setAttribute("isAgencyAdmin", isAgencyAdmin);

            // ── Load agencies for this PSP (needed for PSP admin new-prospect modal) ──
            List<Agency> agencyList = SalesDAO.getAgencyList(em, pspId);
            request.setAttribute("agencyList", agencyList);

            // ── Resolve the current user's agency (if they belong to one) ──
            Agency userAgency = null;
            if (isAgent || isAgencyAdmin) {
                userAgency = findAgencyForUser(em, local.getCurrentPerson());
            }
            request.setAttribute("userAgency", userAgency);

            // ── Load rates — filter by agency for agent/agencyAdmin roles ──
            List<Rate> allRates;
            if (isPspAdmin) {
                // PSP Admin sees all non-suppressed rates
                allRates = SalesDAO.getRateList(em, pspId);
                allRates.removeIf(Rate::isSuppressed);
            } else if (userAgency != null) {
                // Agent or Agency Manager sees only their agency's assigned rates
                Agency fullAgency = SalesDAO.getAgencyFull(em, userAgency.getId());
                allRates = fullAgency.getAgencyRateList() != null
                        ? new ArrayList<>(fullAgency.getAgencyRateList())
                        : new ArrayList<>();
                allRates.removeIf(Rate::isSuppressed);
            } else {
                allRates = new ArrayList<>();
            }
            request.setAttribute("allRates", allRates);

            // If only one rate, auto-select it
            if (allRates.size() == 1) {
                request.setAttribute("autoSelectedRateId", allRates.get(0).getId());
            }

            // ── Load all LOS for this PSP — filter out suppressed ──
            List<LOS> losList = em.createNamedQuery("LOS.getByPsp", LOS.class)
                    .setParameter("psp_id", (long) pspId)
                    .getResultList();
            losList.removeIf(LOS::isSuppressed);
            request.setAttribute("losList", losList);

            // ── Build rate → LOS availability map ──
            Map<Long, Set<Long>> rateLosMap = new HashMap<>();
            for (Rate rate : allRates) {
                List<RateTable> rtRows = SalesDAO.getRateTableList(em, rate.getId());
                Set<Long> availableLosIds = new HashSet<>();
                for (RateTable rt : rtRows) {
                    ServiceModule mod = rt.getModule();
                    if (mod.getLos() != null) {
                        availableLosIds.add(mod.getLos().getId());
                    }
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

            // ── Load prospects — role-based scoping ──
            List<Prospect> prospectList;
            boolean canExpand = false;  // whether the user can toggle to see more prospects

            if (isPspAdmin) {
                // PSP Admin default: all prospects from the PSP admin's own agency (if they have one)
                // with a button to expand to ALL prospects across all agencies
                Agency pspUserAgency = findAgencyForUser(em, local.getCurrentPerson());
                if (pspUserAgency != null && pspUserAgency.getAgentList() != null) {
                    List<Long> agentIds = pspUserAgency.getAgentList().stream()
                            .map(Person::getId).collect(Collectors.toList());
                    prospectList = em.createQuery(
                                    "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id IN :agentIds ORDER BY p.name",
                                    Prospect.class)
                            .setParameter("pspId", (long) pspId)
                            .setParameter("agentIds", agentIds)
                            .getResultList();
                    request.setAttribute("defaultAgencyId", pspUserAgency.getId());
                } else {
                    // PSP admin not in any agency — just show all by default
                    prospectList = SalesDAO.getProspectsByPsp(em, pspId);
                }
                // Always load the full list for the "Show All" expansion
                List<Prospect> allProspects = SalesDAO.getProspectsByPsp(em, pspId);
                request.setAttribute("allProspects", allProspects);
                canExpand = true;

            } else if (isAgencyAdmin) {
                // Agency Manager default: their own prospects
                prospectList = em.createQuery(
                                "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id = :agentId ORDER BY p.name",
                                Prospect.class)
                        .setParameter("pspId", (long) pspId)
                        .setParameter("agentId", local.getCurrentPerson().getId())
                        .getResultList();

                // Load all agency prospects for "Show All Agency" expansion
                if (userAgency != null && userAgency.getAgentList() != null) {
                    List<Long> agentIds = userAgency.getAgentList().stream()
                            .map(Person::getId).collect(Collectors.toList());
                    List<Prospect> agencyProspects = em.createQuery(
                                    "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id IN :agentIds ORDER BY p.name",
                                    Prospect.class)
                            .setParameter("pspId", (long) pspId)
                            .setParameter("agentIds", agentIds)
                            .getResultList();
                    request.setAttribute("allProspects", agencyProspects);
                    canExpand = agencyProspects.size() > prospectList.size();
                }

            } else if (isAgent) {
                // Agent sees only their own prospects — no expansion
                prospectList = em.createQuery(
                                "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id = :agentId ORDER BY p.name",
                                Prospect.class)
                        .setParameter("pspId", (long) pspId)
                        .setParameter("agentId", local.getCurrentPerson().getId())
                        .getResultList();

            } else {
                prospectList = new ArrayList<>();
            }

            request.setAttribute("prospectList", prospectList);
            request.setAttribute("canExpand", canExpand);

            // ── Load agent list for New Prospect modal ──
            // PSP Admin: needs agency dropdown + agent sub-dropdown (agents loaded per agency via JS, but seed with first agency)
            // Agency Manager: agent list from their agency
            // Agent: no dropdown needed (auto-assigned to self)
            if (isPspAdmin) {
                // Pass all agencies (already set above as agencyList)
                // Build a JSON map of agencyId → [{id, name}, ...] for JS-driven agent sub-dropdown
                StringBuilder agentMapJson = new StringBuilder("{");
                boolean agFirst = true;
                for (Agency agency : agencyList) {
                    if (!agFirst) agentMapJson.append(",");
                    agentMapJson.append("\"").append(agency.getId()).append("\":[");
                    Agency fullAg = SalesDAO.getAgencyFull(em, agency.getId());
                    if (fullAg.getAgentList() != null) {
                        boolean pFirst = true;
                        for (Person agent : fullAg.getAgentList()) {
                            if (!pFirst) agentMapJson.append(",");
                            agentMapJson.append("{\"id\":").append(agent.getId())
                                    .append(",\"name\":\"")
                                    .append(agent.getFirstName().replace("\"", "\\\""))
                                    .append(" ")
                                    .append(agent.getLastName().replace("\"", "\\\""))
                                    .append("\"}");
                            pFirst = false;
                        }
                    }
                    agentMapJson.append("]");
                    agFirst = false;
                }
                agentMapJson.append("}");
                request.setAttribute("agentMapJson", agentMapJson.toString());

            } else if (isAgencyAdmin && userAgency != null) {
                // Agency Manager: pass agent list for their agency
                Agency fullAgency = SalesDAO.getAgencyFull(em, userAgency.getId());
                List<Person> agencyAgents = fullAgency.getAgentList() != null
                        ? new ArrayList<>(fullAgency.getAgentList())
                        : new ArrayList<>();
                Collections.sort(agencyAgents);
                request.setAttribute("agencyAgents", agencyAgents);
            }
            // Agent: no agent list needed — CreateProspect will use the current user

            // Pass current user ID for default selections
            request.setAttribute("currentUserId", local.getCurrentPerson().getId());

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

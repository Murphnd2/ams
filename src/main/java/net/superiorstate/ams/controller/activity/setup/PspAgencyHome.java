package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "PspAgencyHome", value = "/PspAgencyHome")
public class PspAgencyHome extends HttpServlet {

    // Status hierarchy: higher number = further along in pipeline
    private static final Map<String, Integer> STATUS_RANK = Map.of(
            "CREATED", 1,
            "SENT", 2,
            "VIEWED", 3,
            "APPLIED", 4,
            "DENIED", 5,
            "APPROVED", 6
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        int pspId = local.getCurrentPerson().getPsp().getId().intValue();

        try {
            // Load agency list — filter suppressed unless toggled
            boolean showSuppressed = "true".equals(request.getParameter("showSuppressed"));
            request.setAttribute("showSuppressed", showSuppressed);

            List<Agency> agencyList;
            if (showSuppressed) {
                agencyList = SalesDAO.getAgencyList(em, pspId);
            } else {
                agencyList = SalesDAO.getActiveAgencyList(em, pspId);
            }
            Collections.sort(agencyList);
            request.setAttribute("agencyList", agencyList);

            // Always load all rates (for assignment checkboxes)
            List<Rate> allRates = SalesDAO.getRateList(em, pspId);
            Collections.sort(allRates);
            request.setAttribute("allRates", allRates);

            // Load pending agents (users with agent role not yet assigned to an agency)
            List<Person> pendingAgents = SalesDAO.getPendingAgents(em, local.getCurrentPerson().getPsp());
            request.setAttribute("pendingAgents", pendingAgents);

            // If an agency is selected, load its details
            String agencyIdParam = request.getParameter("agencyId");
            if (agencyIdParam != null && !agencyIdParam.isEmpty()) {
                long agencyId = Long.parseLong(agencyIdParam);

                // Load agency with rates fetched
                Agency selectedAgency = SalesDAO.getAgencyFull(em, agencyId);
                request.setAttribute("selectedAgency", selectedAgency);

                // Sub-agency picker constraint (V070): if this agency is downstream of a
                // parent GA, restrict the assignable-rate checklist to the GA's own rate
                // set (overwriting the PSP-wide allRates built above). Top-level agencies
                // keep the full PSP list. em is still open, so getAgencyFull is safe.
                if (selectedAgency.getParentAgency() != null) {
                    Agency parentGa = SalesDAO.getAgencyFull(em, selectedAgency.getParentAgency().getId());
                    List<Rate> gaRates = parentGa.getAgencyRateList() != null
                            ? new ArrayList<>(parentGa.getAgencyRateList())
                            : new ArrayList<>();
                    Collections.sort(gaRates);
                    request.setAttribute("allRates", gaRates);
                }

                // Increment 3: parent-GA selector data. Eligible parents = top-level agencies
                // (no parent of their own) under this PSP, excluding self. hasChildren gates the
                // selector off entirely for an agency that is itself a GA (strict two levels).
                final Long selId = selectedAgency.getId();
                List<Agency> eligibleParents = new ArrayList<>();
                boolean selectedAgencyHasChildren = false;
                for (Agency a : agencyList) {
                    if (a.getParentAgency() == null && !a.getId().equals(selId)) {
                        eligibleParents.add(a);
                    }
                    if (a.getParentAgency() != null && a.getParentAgency().getId().equals(selId)) {
                        selectedAgencyHasChildren = true;
                    }
                }
                request.setAttribute("eligibleParents", eligibleParents);
                request.setAttribute("selectedAgencyHasChildren", selectedAgencyHasChildren);

                // Quote-link base host (V071): the host a RequestQuote?k=<token> link for this
                // agency should ride. Precedence: the agency's own white-label host, else its
                // parent GA's host, else the primary PSP host. em is open; parentAgency is
                // EAGER and landingHost is a scalar, so both are safe to read here.
                String quoteLinkBase = selectedAgency.getLandingHost();
                if ((quoteLinkBase == null || quoteLinkBase.isBlank())
                        && selectedAgency.getParentAgency() != null) {
                    quoteLinkBase = selectedAgency.getParentAgency().getLandingHost();
                }
                if (quoteLinkBase == null || quoteLinkBase.isBlank()) {
                    AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
                    if (global != null) quoteLinkBase = global.getPrimaryPspHost();
                }
                request.setAttribute("quoteLinkBase", quoteLinkBase);

                // Load agents for this agency
                List<Person> agents;
                try {
                    agents = SalesDAO.getAgencyAgents(em, agencyId);
                } catch (Exception e) {
                    agents = List.of();
                }
                request.setAttribute("agentList", agents);

                // Manager-eligibility: which of these agents have an ACTIVE user account.
                java.util.Set<Long> activeAgentIds = new java.util.HashSet<>();
                if (!agents.isEmpty()) {
                    java.util.List<Long> agentIds = new java.util.ArrayList<>();
                    for (Person p : agents) agentIds.add(p.getId());
                    try {
                        java.util.List<Long> found = em.createQuery(
                                "SELECT u.person.id FROM User u WHERE u.isActive = true AND u.person.id IN :ids", Long.class)
                                .setParameter("ids", agentIds)
                                .getResultList();
                        activeAgentIds.addAll(found);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                request.setAttribute("activeAgentIds", activeAgentIds);

                // Load proposals for this agency (with losList fetch-joined)
                List<Proposal> proposalList = SalesDAO.getProposalsByAgency(em, agencyId);
                request.setAttribute("proposalList", proposalList);

                // Load rate table data for all assigned rates (for popover)
                Map<Long, List<RateTable>> rateTableMap = new LinkedHashMap<>();
                if (selectedAgency.getAgencyRateList() != null) {
                    for (Rate rate : selectedAgency.getAgencyRateList()) {
                        List<RateTable> rtList = SalesDAO.getRateTableList(em, rate.getId());
                        rateTableMap.put(rate.getId(), rtList);
                    }
                }
                request.setAttribute("rateTableMap", rateTableMap);

                // Build prospect summary list with furthest status
                // Group proposals by prospect ID
                Map<Long, List<Proposal>> proposalsByProspect = proposalList.stream()
                        .collect(Collectors.groupingBy(p -> p.getProspect().getId()));

                // Build prospect summary: each entry = [prospectId, prospectName, agentName, agentId, furthestStatus]
                List<Map<String, String>> prospectSummaryList = new ArrayList<>();
                for (Map.Entry<Long, List<Proposal>> entry : proposalsByProspect.entrySet()) {
                    List<Proposal> prospects = entry.getValue();
                    Proposal first = prospects.get(0);

                    // Compute furthest status across all proposals for this prospect
                    String furthestStatus = "CREATED";
                    int highestRank = 0;
                    for (Proposal p : prospects) {
                        int rank = p.getStatus() != null ? STATUS_RANK.getOrDefault(p.getStatus(), 0) : 0;
                        if (rank > highestRank) {
                            highestRank = rank;
                            furthestStatus = p.getStatus();
                        }
                    }

                    String agentName = "";
                    String agentId = "";
                    if (first.getProspect().getAgent() != null) {
                        Person agent = first.getProspect().getAgent();
                        agentName = agent.getFirstName() + " " + agent.getLastName();
                        agentId = agent.getId().toString();
                    }

                    Map<String, String> summary = new LinkedHashMap<>();
                    summary.put("prospectId", entry.getKey().toString());
                    summary.put("prospectName", first.getProspect().getName() != null ? first.getProspect().getName() : "(unnamed)");
                    summary.put("agentName", agentName);
                    summary.put("agentId", agentId);
                    summary.put("furthestStatus", furthestStatus);
                    summary.put("proposalCount", String.valueOf(prospects.size()));
                    prospectSummaryList.add(summary);
                }

                // Sort by prospect name (default)
                prospectSummaryList.sort(Comparator.comparing(m -> m.getOrDefault("prospectName", "").toLowerCase()));
                request.setAttribute("prospectSummaryList", prospectSummaryList);
            }

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/agencyManager25.jsp");
        dispatcher.forward(request, response);
    }
}
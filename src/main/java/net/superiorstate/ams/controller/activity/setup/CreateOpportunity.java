package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.data.resolver.AgencyScope;
import net.superiorstate.ams.data.resolver.AgencyScopeResolver;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "CreateOpportunity", value = "/CreateOpportunity")
public class CreateOpportunity extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("AgentHome");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorizedForAgency(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Opportunity opp = createOpportunity(request);

        String returnTo = request.getParameter("returnTo");
        if ("home".equals(returnTo)) {
            if (opp != null) {
                updateGlobalState(request, opp);
            }
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
            dispatcher.forward(request, response);
        } else if (opp != null && opp.getProspect() != null
                && Boolean.parseBoolean(request.getParameter("createProposalNow"))) {
            // Bug B hand-off: reuse the proven ProposalBuilder deep-link (same format as
            // agentHome25.jsp:826 and detailOpportunity25.jsp:126) to pre-fill the prospect
            // and link the new proposal back to this opportunity.
            response.sendRedirect("ProposalBuilder?prospectId=" + opp.getProspect().getId()
                    + "&sourceActivityId=" + opp.getId());
        } else {
            response.sendRedirect("AgentHome");
        }
    }

    /**
     * PHASE 2 (closing AGENCY_STRUCTURE_AUDIT.md §2.2 #5), PHASE 2b: agencyId was
     * previously an unchecked request param. Gate via AgencyScopeResolver.canSeeDetail(),
     * never the raw detailAgencyIds set (the pspWide trap). No carve-out needed here
     * since Phase 2b — a Plain Agent's detailAgencyIds now includes their own real
     * agency memberships, so canSeeDetail() alone covers their legitimate own-agency
     * "New Opportunity" submissions.
     */
    private boolean isAuthorizedForAgency(HttpServletRequest request) {
        String agencyIdParam = request.getParameter("agencyId");
        if (agencyIdParam == null || agencyIdParam.isBlank()) return false;

        long agencyId;
        try {
            agencyId = Long.parseLong(agencyIdParam.trim());
        } catch (NumberFormatException e) {
            return false;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            AgencyScope scope = AgencyScopeResolver.resolve(em, request);
            return AgencyScopeResolver.canSeeDetail(scope, agencyId);
        } finally {
            em.close();
        }
    }

    private Opportunity createOpportunity(HttpServletRequest request) {
        System.out.println("CreateOpportunity: returnTo=" + request.getParameter("returnTo")
            + " prospectMode=" + request.getParameter("prospectMode")
            + " agencyId=" + request.getParameter("agencyId")
            + " agentId=" + request.getParameter("agentId"));

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();

            long agencyId = Long.parseLong(request.getParameter("agencyId"));
            Agency agency = EntityLookup.getAgencyById(em, agencyId);
            if (agency == null) return null;

            // Determine prospect — existing or new
            Prospect prospect;
            String prospectMode = request.getParameter("prospectMode");

            if ("new".equals(prospectMode)) {
                prospect = createNewProspect(em, request, agency, currentUser);
            } else {
                long prospectId = Long.parseLong(request.getParameter("prospectId"));
                prospect = EntityLookup.getProspectById(em, prospectId);
            }

            if (prospect == null) return null;

            // Determine the agent for this opportunity
            Person agent;
            if ("new".equals(prospectMode)) {
                agent = resolveAgent(em, request, agency, currentUser);
            } else {
                // Existing prospect — use the prospect's agent if available
                agent = (prospect.getAgent() != null) ? prospect.getAgent() : currentUser;
            }

            // Create Opportunity
            em.getTransaction().begin();
            Opportunity opp = new Opportunity();
            opp.setProspect(prospect);
            opp.setAgency(agency);
            opp.setStage("NEW");
            opp.setAssignedTo(agent);                          // Agent owns the work
            opp.setLoggedBy(currentUser);                      // PSP admin created it
            // managedBy = creator if different from agent (PSP admin oversight), otherwise agent self-manages
            opp.setManagedBy(agent.getId().equals(currentUser.getId()) ? agent : currentUser);
            opp.setPrimaryContact(prospect.getContact());
            opp.setFullName(prospect.getName().trim().toUpperCase());
            opp.setDueDate(Date.valueOf(LocalDate.now().plusDays(30)));
            opp.setComplete(false);

            // Optional pipeline fields
            String eeParam = request.getParameter("estimatedEmployees");
            if (eeParam != null && !eeParam.isBlank()) {
                try { opp.setEstimatedEmployees(Integer.parseInt(eeParam.trim())); } catch (NumberFormatException ignored) {}
            }
            String valParam = request.getParameter("estimatedValue");
            if (valParam != null && !valParam.isBlank()) {
                try { opp.setEstimatedValue(Double.parseDouble(valParam.trim())); } catch (NumberFormatException ignored) {}
            }
            String closeDateParam = request.getParameter("expectedCloseDate");
            if (closeDateParam != null && !closeDateParam.isBlank()) {
                try { opp.setExpectedCloseDate(Date.valueOf(closeDateParam.trim())); } catch (IllegalArgumentException ignored) {}
            }

            em.persist(opp);
            em.getTransaction().commit();

            // Create CheckList
            em.getTransaction().begin();
            CheckList c = new CheckList();
            c.setAssignedTo(opp);
            c.setComplete(false);
            c.setFullName(prospect.getName().trim().toUpperCase() + " Checklist");
            c.setDueDate(opp.getDueDate());
            c.setLoggedBy(currentUser);
            em.persist(c);
            em.getTransaction().commit();

            // Add default ToDo (task 153 - pre-completed placeholder)
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(EntityLookup.getTaskById(em, 153L));
            toDo.setSortOrder(1000);
            toDo.setCheckList(c);
            toDo.setComplete(true);
            em.persist(toDo);
            em.getTransaction().commit();

            // Link CheckList back to Opportunity
            CheckList checkList = EntityLookup.getCheckListById(em, c.getId());
            em.getTransaction().begin();
            opp.setCheckList(checkList);
            em.persist(opp);
            em.getTransaction().commit();

            // Create linked Proposal with rate and LOS selections
            String rateIdParam = request.getParameter("rateId");
            String losIdsParam = request.getParameter("losIds");
            if (rateIdParam != null && !rateIdParam.isEmpty()) {
                long rateId = Long.parseLong(rateIdParam);
                Rate rate = em.find(Rate.class, rateId);

                em.getTransaction().begin();
                Proposal proposal = new Proposal();
                proposal.setProspect(prospect);
                proposal.setRate(rate);
                proposal.setApplicationGUID(UUID.randomUUID().toString());
                proposal.setStatus("CREATED");
                proposal.setCreatedBy(currentUser);
                proposal.setInactive(false);
                proposal.setLosList(new ArrayList<>());
                proposal.setSourceActivity(opp);
                em.persist(proposal);
                em.getTransaction().commit();

                // Add selected LOSs to proposal
                if (losIdsParam != null && !losIdsParam.isEmpty()) {
                    for (String losIdStr : losIdsParam.split(",")) {
                        long losId = Long.parseLong(losIdStr.trim());
                        LOS los = SalesDAO.getLosFull(em, losId);
                        em.getTransaction().begin();
                        proposal.getLosList().add(los);
                        los.getListOfProposalsThatIncludeThisLOS().add(proposal);
                        em.persist(proposal);
                        em.persist(los);
                        em.getTransaction().commit();
                    }
                }

                System.out.println("Proposal created for opportunity: #" + proposal.getId());
            }

            // Update session
            em.refresh(opp);
            local.respondToActivityUpdate(em, "ADD_TICKET", opp);
            request.getSession().setAttribute("local", local);

            return opp;

        } catch (Exception e) {
            System.out.println("CreateOpportunity FAILED: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    /** Update global activity list and prospect cache when creating from PSP home */
    private void updateGlobalState(HttpServletRequest request, Opportunity opp) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");

            Activity25u newActivity = local.getActivity25u(em, opp);
            List<Activity25u> allActivities = new ArrayList<>(global.getActivitiesAllOpen());
            allActivities.add(newActivity);

            global.setActivitiesAllOpen(allActivities);
            local.setActivitiesAllOpen(allActivities);

            // Refresh full sales data cache (prospect + agent/agency data)
            global.refreshSalesData(em);

            local.getCurrentActivity().setActivity(opp);
            local.getCurrentActivity().setReFilterOnExit(true);

            request.getSession().setAttribute("local", local);
            request.getServletContext().setAttribute("global", global);
        } finally {
            em.close();
        }
    }

    /**
     * Resolve the agent for the given agency.
     * Priority: explicit agentId param > current user if in agency > first agent in agency > fallback to currentUser
     *
     * PHASE 2: a valid agency plus a foreign agentId is still an IDOR — a caller
     * authorized for agencyId could otherwise attribute the opportunity to any
     * arbitrary Person system-wide. The submitted agentId is now discarded (falling
     * through to the existing membership-based resolution below, same as if no
     * agentId had been sent at all) unless it actually belongs to this agency's
     * agentList.
     */
    private Person resolveAgent(EntityManager em, HttpServletRequest request, Agency agency, Person currentUser) {
        Person agent = null;
        String agentIdParam = request.getParameter("agentId");
        if (agentIdParam != null && !agentIdParam.isEmpty()) {
            try {
                Person candidate = EntityLookup.getPersonById(em, Long.parseLong(agentIdParam));
                if (candidate != null && agency.getAgentList() != null
                        && agency.getAgentList().stream().anyMatch(p -> p.getId().equals(candidate.getId()))) {
                    agent = candidate;
                }
            } catch (NumberFormatException ignored) {}
        }
        if (agent == null && agency.getAgentList() != null && !agency.getAgentList().isEmpty()) {
            // Check if current user is an agent of this agency
            for (Person p : agency.getAgentList()) {
                if (p.getId().equals(currentUser.getId())) {
                    agent = currentUser;
                    break;
                }
            }
            // Fallback: first agent in the agency
            if (agent == null) {
                agent = agency.getAgentList().get(0);
            }
        }
        // Last resort fallback
        if (agent == null) {
            agent = currentUser;
        }
        return agent;
    }

    private Prospect createNewProspect(EntityManager em, HttpServletRequest request, Agency agency, Person currentUser) {
        String companyName = request.getParameter("companyName");
        String contactFirst = request.getParameter("contactFirst");
        String contactLast = request.getParameter("contactLast");
        String contactEmail = request.getParameter("contactEmail");

        if (companyName == null || companyName.trim().isEmpty()) return null;

        // Create contact Person
        em.getTransaction().begin();
        Person contact = new Person();
        contact.setFirstName(contactFirst != null ? contactFirst.trim() : "");
        contact.setLastName(contactLast != null ? contactLast.trim() : "");
        contact.setFullName((contact.getFirstName() + " " + contact.getLastName()).trim());
        contact.setEmail(contactEmail != null ? contactEmail.trim() : "");
        contact.setPsp(currentUser.getPsp());
        em.persist(contact);
        em.getTransaction().commit();

        // Resolve agent for the prospect
        Person agent = resolveAgent(em, request, agency, currentUser);

        // Create Prospect
        em.getTransaction().begin();
        Prospect prospect = new Prospect();
        prospect.setName(companyName.trim());
        prospect.setContact(contact);
        prospect.setAgent(agent);
        em.persist(prospect);
        em.getTransaction().commit();

        return prospect;
    }
}

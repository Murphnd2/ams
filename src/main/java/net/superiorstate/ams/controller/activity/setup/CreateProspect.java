package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.AgencyScope;
import net.superiorstate.ams.data.resolver.AgencyScopeResolver;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;

import java.io.IOException;

@WebServlet(name = "CreateProspect", value = "/CreateProspect")
public class CreateProspect extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Prospect prospect = null;
        try {
            String prospectName = request.getParameter("prospectName");
            String contactFirst = request.getParameter("contactFirst");
            String contactLast = request.getParameter("contactLast");
            String contactEmail = request.getParameter("contactEmail");
            String contactPhone = request.getParameter("contactPhone");
            long agencyId = Long.parseLong(request.getParameter("agencyId"));

            // PHASE 2 (closing AGENCY_STRUCTURE_AUDIT.md §2.2 #5), PHASE 2b: agencyId
            // was previously an unchecked request param — any authenticated user could
            // attribute a new prospect to an arbitrary agency. Gate via
            // AgencyScopeResolver.canSeeDetail(), never the raw detailAgencyIds set
            // (the pspWide trap). No carve-out needed here since Phase 2b — a Plain
            // Agent's detailAgencyIds now includes their own real agency memberships,
            // so canSeeDetail() alone covers their legitimate own-agency submissions.
            AgencyScope scope = AgencyScopeResolver.resolve(em, request);
            if (!AgencyScopeResolver.canSeeDetail(scope, agencyId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Agency agency = em.find(Agency.class, agencyId);

            // Determine agent: explicit agentId param > first agent in agency > null
            Person agent = null;
            String agentIdParam = request.getParameter("agentId");
            if (agentIdParam != null && !agentIdParam.isEmpty()) {
                try {
                    // PHASE 2b (Fix 3): a valid agency plus a foreign agentId is still
                    // an IDOR — mirrors CreateOpportunity.resolveAgent()'s membership
                    // check exactly. An agentId that doesn't belong to this agency's
                    // agentList is discarded and falls through to the default below,
                    // same as if no agentId had been submitted at all.
                    Person candidate = EntityLookup.getPersonById(em, Long.parseLong(agentIdParam));
                    if (candidate != null && agency.getAgentList() != null
                            && agency.getAgentList().stream().anyMatch(p -> p.getId().equals(candidate.getId()))) {
                        agent = candidate;
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (agent == null && agency.getAgentList() != null && !agency.getAgentList().isEmpty()) {
                // For plain agents (no agentId sent, or an invalid one discarded above),
                // default to the current user if they're in this agency
                boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
                if (!isPspAdmin) {
                    // Non-admin: assign to current user
                    agent = local.getCurrentPerson();
                } else {
                    // PSP Admin fallback: first agent in agency
                    agent = agency.getAgentList().get(0);
                }
            }

            // Create contact person
            em.getTransaction().begin();
            Person contact = new Person();
            contact.setFirstName(contactFirst);
            contact.setLastName(contactLast);
            contact.setEmail(contactEmail);
            contact.setPhone(contactPhone);
            contact.setPsp(local.getCurrentPerson().getPsp());
            em.persist(contact);
            em.getTransaction().commit();

            // Create prospect
            em.getTransaction().begin();
            prospect = new Prospect();
            prospect.setName(prospectName);
            prospect.setContact(contact);
            prospect.setAgent(agent);
            em.persist(prospect);
            em.getTransaction().commit();

            System.out.println("Prospect created: " + prospectName + " (ID=" + prospect.getId() + ") agent=" + (agent != null ? agent.getFullName() : "none"));

        } finally {
            em.close();
        }

        // Refresh global sales data cache (new prospect appears in dropdowns)
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global != null) {
            EntityManager em2 = emf.createEntityManager();
            try {
                global.refreshSalesData(em2);
                getServletContext().setAttribute("global", global);
            } finally {
                em2.close();
            }
        }

        response.sendRedirect("ProposalBuilder" + (prospect != null ? "?selectedProspect=" + prospect.getId() : ""));
    }
}

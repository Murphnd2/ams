package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Proposal;

import net.superiorstate.ams.model.sales.agency.Agency;

import java.io.IOException;
import java.util.*;

@WebServlet(name = "ApplicationsHome", value = "/ApplicationsHome")
public class ApplicationsHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Boolean isPspUser = Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"));
        Boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspUser && !isPspAdmin) {
            response.sendRedirect("ViewHome25");
            return;
        }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        long pspId = local.getCurrentPerson().getPsp().getId();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // In-progress applications: prospect started but not yet submitted
            List<Proposal> inProgressList = em.createQuery(
                    "SELECT p FROM Proposal p " +
                    "JOIN FETCH p.prospect pr " +
                    "JOIN FETCH pr.contact c " +
                    "LEFT JOIN FETCH pr.agent ag " +
                    "LEFT JOIN FETCH p.application a " +
                    "LEFT JOIN FETCH p.sourceActivity sa " +
                    "WHERE pr.contact.psp.id = :pspId " +
                    "AND a.status = 'IN_PROGRESS' " +
                    "AND p.isInactive = false " +
                    "ORDER BY a.dateStarted DESC",
                    Proposal.class)
                .setParameter("pspId", pspId)
                .getResultList();

            // Submitted but not yet approved/denied
            List<Proposal> pendingReviewList = em.createQuery(
                    "SELECT p FROM Proposal p " +
                    "JOIN FETCH p.prospect pr " +
                    "JOIN FETCH pr.contact c " +
                    "LEFT JOIN FETCH pr.agent ag " +
                    "LEFT JOIN FETCH p.application a " +
                    "WHERE pr.contact.psp.id = :pspId " +
                    "AND a.status = 'SUBMITTED' " +
                    "AND p.status NOT IN ('APPROVED', 'DENIED') " +
                    "AND p.isInactive = false " +
                    "ORDER BY a.dateSubmitted DESC",
                    Proposal.class)
                .setParameter("pspId", pspId)
                .getResultList();

            // Build agency name map (Person → first agency via ManyToMany)
            Map<Long, String> agencyNameMap = new HashMap<>();
            for (Proposal p : inProgressList) {
                resolveAgencyName(p, agencyNameMap);
            }
            for (Proposal p : pendingReviewList) {
                resolveAgencyName(p, agencyNameMap);
            }

            // Build set of proposal IDs eligible for "Take Over" (sourceActivity is Opportunity with null managedBy)
            Set<Long> takeOverEligible = new HashSet<>();
            if (Boolean.TRUE.equals(isPspAdmin)) {
                for (Proposal p : inProgressList) {
                    if (p.getSourceActivity() != null) {
                        try {
                            Opportunity opp = em.find(Opportunity.class, p.getSourceActivity().getId());
                            if (opp != null && opp.getManagedBy() == null) {
                                takeOverEligible.add(p.getId());
                            }
                        } catch (Exception ignored) {
                            // sourceActivity may not be an Opportunity
                        }
                    }
                }
            }

            request.setAttribute("inProgressList", inProgressList);
            request.setAttribute("pendingReviewList", pendingReviewList);
            request.setAttribute("isPspAdmin", isPspAdmin);
            request.setAttribute("takeOverEligible", takeOverEligible);
            request.setAttribute("agencyNameMap", agencyNameMap);

            request.setAttribute("pageTitle", "Applications");
            request.setAttribute("pageIcon", "bi-file-earmark-check");
            request.getRequestDispatcher("/WEB-INF/view/sales/applicationsHome25.jsp")
                .forward(request, response);

        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if ("takeOver".equals(action)) {
            long proposalId = Long.parseLong(request.getParameter("proposalId"));
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentPerson = local.getCurrentPerson();

            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                Proposal proposal = em.find(Proposal.class, proposalId);
                if (proposal != null && proposal.getSourceActivity() != null) {
                    Opportunity opp = em.find(Opportunity.class, proposal.getSourceActivity().getId());
                    if (opp != null && opp.getManagedBy() == null) {
                        em.getTransaction().begin();
                        opp.setManagedBy(currentPerson);
                        em.merge(opp);
                        em.getTransaction().commit();
                    }
                }
            } finally {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                em.close();
            }
        }

        response.sendRedirect("ApplicationsHome");
    }

    private void resolveAgencyName(Proposal p, Map<Long, String> map) {
        if (map.containsKey(p.getId())) return;
        Person agent = p.getProspect() != null ? p.getProspect().getAgent() : null;
        if (agent != null) {
            List<Agency> agencies = agent.getListOfAgenciesWithThisAgent();
            if (agencies != null && !agencies.isEmpty()) {
                map.put(p.getId(), agencies.get(0).getName());
                return;
            }
        }
        map.put(p.getId(), null);
    }
}

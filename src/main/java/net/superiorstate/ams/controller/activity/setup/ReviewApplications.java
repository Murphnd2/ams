package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.AgencyScope;
import net.superiorstate.ams.data.resolver.AgencyScopeResolver;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.application.Application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet(name = "ReviewApplications", value = "/ReviewApplications")
public class ReviewApplications extends HttpServlet {

    private static final List<String> VALID_STATUSES =
            List.of("SUBMITTED", "UNDER_REVIEW", "MORE_INFO", "APPROVED", "DENIED");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        if (currentPerson == null) {
            response.sendRedirect("Login");
            return;
        }

        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            String statusParam = request.getParameter("status");

            // Default to SUBMITTED only
            List<String> selectedStatuses;
            if (statusParam == null || statusParam.isBlank()) {
                selectedStatuses = List.of("SUBMITTED");
            } else if ("ALL".equals(statusParam)) {
                selectedStatuses = VALID_STATUSES;
            } else {
                selectedStatuses = Arrays.stream(statusParam.split(","))
                        .map(String::trim)
                        .filter(VALID_STATUSES::contains)
                        .toList();
                if (selectedStatuses.isEmpty()) {
                    selectedStatuses = List.of("SUBMITTED");
                }
            }

            // PHASE 2 (closing AGENCY_STRUCTURE_AUDIT.md §2.2 #3): resolver-driven
            // scoping instead of the old agentOnly boolean, which had no Agency Admin
            // carve-out at all (they fell through to fully unscoped) and ignored
            // isPspSales when combined with isAgent. See PHASE2_NOTES.md.
            AgencyScope scope = AgencyScopeResolver.resolve(em, request);
            List<Application> applications;
            if (scope.pspWide()) {
                applications = queryApplications(em, selectedStatuses, null, null);
            } else if (isAgencyAdmin) {
                List<Long> agencyIds = new ArrayList<>(scope.detailAgencyIds());
                applications = agencyIds.isEmpty()
                        ? new ArrayList<>()
                        : queryApplications(em, selectedStatuses, agencyIds, null);
            } else if (isAgent) {
                applications = queryApplications(em, selectedStatuses, null, currentPerson.getId());
            } else {
                applications = new ArrayList<>();
            }

            request.setAttribute("applications", applications);
            request.setAttribute("selectedStatuses", selectedStatuses);
            request.setAttribute("isAll", selectedStatuses.size() == VALID_STATUSES.size());

            // Flash messages from redirect
            String msg = request.getParameter("msg");
            if (msg != null) request.setAttribute("msg", msg);
            String err = request.getParameter("err");
            if (err != null) request.setAttribute("err", err);

            request.getRequestDispatcher("/WEB-INF/view/sales/reviewApplications.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error loading applications");
        } finally {
            em.close();
        }
    }

    /**
     * Builds and runs the Application query for one of three predicate shapes:
     * agencyIds != null -> restrict to applications whose proposal's prospect's
     * agent belongs to one of those agencies (Agency Admin bucket, same 3-hop
     * relationship as SalesDAO.getProposalsByAgency, generalized to a set of
     * agencies instead of one). agentId != null -> self-only (Plain Agent bucket,
     * unchanged from before). Neither set -> no predicate beyond status (pspWide).
     */
    private List<Application> queryApplications(EntityManager em, List<String> statuses,
                                                  List<Long> agencyIds, Long agentId) {
        String jpql = "SELECT a FROM Application a " +
                "JOIN FETCH a.proposal p " +
                "JOIN FETCH p.prospect pr " +
                "JOIN FETCH pr.contact " +
                "LEFT JOIN FETCH p.losList " +
                "WHERE a.status IN :statuses ";
        if (agencyIds != null) {
            jpql += "AND pr.agent.id IN (SELECT agt.id FROM Agency agy JOIN agy.agentList agt WHERE agy.id IN :agencyIds) ";
        } else if (agentId != null) {
            jpql += "AND pr.agent.id = :agentId ";
        }
        jpql += "ORDER BY a.dateSubmitted DESC";

        Query q = em.createQuery(jpql);
        q.setParameter("statuses", statuses);
        if (agencyIds != null) q.setParameter("agencyIds", agencyIds);
        if (agentId != null) q.setParameter("agentId", agentId);
        return q.getResultList();
    }
}
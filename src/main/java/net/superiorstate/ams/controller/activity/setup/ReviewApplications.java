package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.application.Application;

import java.io.IOException;
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

        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        boolean isPspUser = Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean agentOnly = isAgent && !isPspAdmin && !isPspUser;

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

            // Agent-only users see only their own prospects' applications
            String jpql = "SELECT a FROM Application a " +
                    "JOIN FETCH a.proposal p " +
                    "JOIN FETCH p.prospect pr " +
                    "JOIN FETCH pr.contact " +
                    "LEFT JOIN FETCH p.losList " +
                    "WHERE a.status IN :statuses " +
                    (agentOnly ? "AND pr.agent.id = :agentId " : "") +
                    "ORDER BY a.dateSubmitted DESC";
            Query q = em.createQuery(jpql);
            q.setParameter("statuses", selectedStatuses);
            if (agentOnly) {
                q.setParameter("agentId", currentPerson.getId());
            }
            List<Application> applications = q.getResultList();

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
}
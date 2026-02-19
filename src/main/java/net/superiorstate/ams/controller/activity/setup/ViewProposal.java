package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.RateTable;
import net.superiorstate.ams.model.sales.offering.Feature;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@WebServlet(name = "ViewProposal", value = "/proposal/*")
public class ViewProposal extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Extract GUID from path: /proposal/{guid}
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String guid = pathInfo.substring(1); // strip leading "/"

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Load proposal with LOSs
            Query q = em.createQuery("SELECT DISTINCT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.applicationGUID = :guid");
            q.setParameter("guid", guid);
            List<Proposal> results = q.getResultList();
            if (results.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Proposal proposal = results.get(0);

            // Check if inactive
            if (proposal.isInactive()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Update status to VIEWED on first access (only if currently SENT)
            if ("SENT".equals(proposal.getStatus()) && proposal.getDateViewed() == null) {
                em.getTransaction().begin();
                proposal.setStatus("VIEWED");
                proposal.setDateViewed(Timestamp.from(Instant.now()));
                em.persist(proposal);
                em.getTransaction().commit();
            }

            // Load pricing
            List<RateTable> pricing = SalesDAO.getPricing(em, proposal);

            // Load features for all modules in this proposal
            List<Long> moduleIds = SalesDAO.getDistinctListOfServiceModulesForThisProposal(proposal)
                    .stream().map(m -> m.getId()).toList();

            List<Feature> features = List.of();
            if (!moduleIds.isEmpty()) {
                Query fq = em.createQuery("SELECT f FROM Feature f WHERE f.serviceModule.id IN :moduleIds ORDER BY f.serviceModule.sortOrder, f.sortOrder");
                fq.setParameter("moduleIds", moduleIds);
                features = fq.getResultList();
            }

            // Get PSP branding colors
            String primaryColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY");
            String accentColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT");
            if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#2B5F8A";
            if (accentColor == null || accentColor.isEmpty()) accentColor = "#7AB648";

            // Set attributes for JSP
            request.setAttribute("proposal", proposal);
            request.setAttribute("pricing", pricing);
            request.setAttribute("features", features);
            request.setAttribute("primaryColor", primaryColor);
            request.setAttribute("accentColor", accentColor);
            request.setAttribute("pspName", proposal.getProspect().getContact().getPsp() != null
                    ? proposal.getProspect().getContact().getPsp().getFullName() : "");

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/viewProposal.jsp");
        dispatcher.forward(request, response);
    }
}

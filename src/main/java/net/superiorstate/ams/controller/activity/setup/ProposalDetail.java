package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.RateTable;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ProposalDetail", value = "/ProposalDetail")
public class ProposalDetail extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long proposalId = Long.parseLong(request.getParameter("id"));

            // Load proposal with LOSs
            Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.id = :id");
            q.setParameter("id", proposalId);
            Proposal proposal = (Proposal) q.getSingleResult();

            // Get pricing
            List<RateTable> pricing = SalesDAO.getPricing(em, proposal);

            request.setAttribute("proposal", proposal);
            request.setAttribute("pricing", pricing);

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/proposalDetail.jsp");
        dispatcher.forward(request, response);
    }
}
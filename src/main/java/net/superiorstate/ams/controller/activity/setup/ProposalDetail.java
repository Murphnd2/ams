package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.util.EmailTemplate;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.RateTable;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@WebServlet(name = "ProposalDetail", value = "/ProposalDetail")
public class ProposalDetail extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long proposalId = Long.parseLong(request.getParameter("id"));

            Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.id = :id");
            q.setParameter("id", proposalId);
            Proposal proposal = (Proposal) q.getSingleResult();

            List<RateTable> pricing = SalesDAO.getPricing(em, proposal);

            request.setAttribute("proposal", proposal);
            request.setAttribute("pricing", pricing);

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/proposalDetail.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        long proposalId = Long.parseLong(request.getParameter("id"));

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        try {
            if ("sendToProspect".equals(action)) {
                Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.id = :id");
                q.setParameter("id", proposalId);
                Proposal proposal = (Proposal) q.getSingleResult();

                Person contact = proposal.getProspect().getContact();
                Person sender = local.getCurrentPerson();
                String toEmail = contact.getEmail();
                String fromEmail = sender.getEmail();

                String proposalLink = "https://superiorstate.biz/proposal/" + proposal.getApplicationGUID();
                String prospectName = contact.getFirstName() != null ? contact.getFirstName() : "there";

                String bodyHtml = "<p>Hi " + prospectName + ",</p>"
                        + "<p>We've prepared a benefits proposal for <strong>" + proposal.getProspect().getName() + "</strong>.</p>"
                        + "<p>Please click the link below to view your customized proposal, including pricing and plan details:</p>"
                        + "<p><a href=\"" + proposalLink + "\" style=\"display:inline-block;padding:12px 24px;background-color:#2B5F8A;color:#ffffff;text-decoration:none;border-radius:4px;font-weight:bold;\">View Your Proposal</a></p>"
                        + "<p>If you have any questions, simply reply to this email.</p>";

                String wrappedBody = EmailTemplate.wrap(bodyHtml, sender, null, em);
                String subject = "Your Benefits Proposal — " + proposal.getProspect().getName();

                EmailDAO.sendEmail(fromEmail, toEmail, subject, wrappedBody, em);

                // Update status
                em.getTransaction().begin();
                proposal.setStatus("SENT");
                proposal.setDateSent(Timestamp.from(Instant.now()));
                em.persist(proposal);
                em.getTransaction().commit();

                System.out.println("Proposal #" + proposalId + " sent to " + toEmail);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to send proposal #" + proposalId + ": " + e.getMessage());
        } finally {
            em.close();
        }

        response.sendRedirect("ProposalDetail?id=" + proposalId);
    }
}
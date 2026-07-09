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
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.RateTable;
import net.superiorstate.ams.data.resolver.EntityLookup;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "SendProposal", value = "/SendProposal")
public class SendProposal extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        try {
            long proposalId = Long.parseLong(request.getParameter("id"));

            Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.id = :id");
            q.setParameter("id", proposalId);
            Proposal proposal = (Proposal) q.getSingleResult();

            List<RateTable> pricing = SalesDAO.getPricing(em, proposal);
            Person sender = local.getCurrentPerson();
            Person contact = proposal.getProspect().getContact();

            // Pre-populate defaults
            request.setAttribute("proposal", proposal);
            request.setAttribute("pricing", pricing);
            request.setAttribute("toEmail", contact != null ? contact.getEmail() : "");
            request.setAttribute("senderEmail", sender.getEmail());
            request.setAttribute("prospectName", contact != null && contact.getFirstName() != null ? contact.getFirstName() : "there");

            String baseUrl = request.getScheme() + "://" + request.getServerName();
            int port = request.getServerPort();
            if (port != 80 && port != 443) baseUrl += ":" + port;
            baseUrl += request.getContextPath() + "/";
            String proposalLink = baseUrl + "proposal/" + proposal.getApplicationGUID();

            String agencyName = resolveSenderAgencyName(em, sender);
            String senderCompany = agencyName != null ? agencyName
                    : (sender.getPsp() != null ? sender.getPsp().getFullName() : "");

            String defaultBody = "<p>Hi " + request.getAttribute("prospectName") + ",</p>"
                    + "<p>We've prepared a benefits proposal for <strong>" + proposal.getProspect().getName() + "</strong>.</p>"
                    + "<p>Please click the link below to view your customized proposal, including pricing and plan details:</p>"
                    + "<p><a href=\"" + proposalLink + "\" style=\"display:inline-block;padding:12px 24px;background-color:#2B5F8A;color:#ffffff;text-decoration:none;border-radius:4px;font-weight:bold;\">View Your Proposal</a></p>"
                    + "<p>If you have any questions, simply reply to this email.</p>"
                    + "<p style=\"margin-top:24px;\">" + sender.getFirstName() + " " + sender.getLastName() + "<br/>"
                    + "<span style=\"color:#666666;\">" + (sender.getEmail() != null ? sender.getEmail() : "") + "</span><br/>"
                    + "<span style=\"color:#7AB648;font-weight:bold;\">" + senderCompany + "</span></p>";

            request.setAttribute("defaultBody", defaultBody);
            request.setAttribute("proposalLink", proposalLink);

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/sendProposal.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long proposalId = Long.parseLong(request.getParameter("id"));
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        try {
            Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.id = :id");
            q.setParameter("id", proposalId);
            Proposal proposal = (Proposal) q.getSingleResult();

            Person sender = local.getCurrentPerson();
            String fromEmail = sender.getEmail();
            String toEmail = request.getParameter("toEmail");
            String ccEmail = request.getParameter("ccEmail");
            String subject = request.getParameter("subject");
            String body = request.getParameter("body");
            boolean copyMe = "on".equals(request.getParameter("copyMe"));

            // Build recipient lists
            List<String> toList = new ArrayList<>();
            if (toEmail != null && !toEmail.isBlank()) toList.add(toEmail.trim());

            List<String> ccList = new ArrayList<>();
            if (ccEmail != null && !ccEmail.isBlank()) {
                for (String cc : ccEmail.split("[,;]")) {
                    if (!cc.trim().isBlank()) ccList.add(cc.trim());
                }
            }
            if (copyMe && fromEmail != null) ccList.add(fromEmail);

            // Wrap and send
            String pspName = sender.getPsp() != null ? sender.getPsp().getFullName() : "";
            String wrappedBody = EmailTemplate.wrapBodyOnly(body, pspName, em);
            EmailDAO.sendEmail(fromEmail, toList, ccList, Collections.emptyList(), subject, wrappedBody, em);

            // Update proposal status
            em.getTransaction().begin();
            if ("CREATED".equals(proposal.getStatus())) {
                proposal.setStatus("SENT");
                proposal.setDateSent(Timestamp.from(Instant.now()));
            }
            em.persist(proposal);
            em.getTransaction().commit();

            // Log to source activity if linked
            if (proposal.getSourceActivity() != null) {
                logEmailToActivity(em, proposal, sender, subject, wrappedBody);
            }

            System.out.println("Proposal #" + proposalId + " sent to " + toEmail);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to send proposal #" + proposalId + ": " + e.getMessage());
        } finally {
            em.close();
        }

        response.sendRedirect("ProposalDetail?id=" + proposalId);
    }

    /** Returns the sender's first agency name, or null if they belong to none. */
    private String resolveSenderAgencyName(EntityManager em, Person sender) {
        try {
            if (sender.getPsp() == null) return null;
            List<Agency> agencies = em.createQuery(
                    "SELECT a FROM Agency a JOIN a.agentList al WHERE al.id = :agentId AND a.psp.id = :pspId",
                    Agency.class)
                    .setParameter("agentId", sender.getId())
                    .setParameter("pspId", (long) sender.getPsp().getId())
                    .getResultList();
            return agencies.isEmpty() ? null : agencies.get(0).getName();
        } catch (Exception e) {
            return null;
        }
    }

    private void logEmailToActivity(EntityManager em, Proposal proposal, Person sender, String subject, String wrappedBody) {
        try {
            Activity activity = EntityLookup.getActivityById(em, proposal.getSourceActivity().getId());
            if (activity != null) {
                em.getTransaction().begin();
                Email email = new Email();
                email.setActivity(activity);
                email.setSubject(subject);
                email.setDateGenerated(Date.valueOf(LocalDate.now()));
                email.setStatus(EntityLookup.getActivityStatusById(em, 1));
                email.setReasonCreated(EntityLookup.getReasonById(em, 7));
                email.setCreatedBy(sender);
                email.setDetail(wrappedBody);
                em.persist(email);
                activity.addNote(email);
                em.persist(activity);
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not log email to activity: " + e.getMessage());
        }
    }
}

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
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.data.util.EmailIdentity;
import net.superiorstate.ams.data.util.EmailIdentityResolver;
import net.superiorstate.ams.data.util.EmailTemplate;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.PriceItem;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.ProposalPriceLine;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

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

            Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList LEFT JOIN FETCH p.application WHERE p.id = :id");
            q.setParameter("id", proposalId);
            Proposal proposal = (Proposal) q.getSingleResult();

            List<ProposalPriceLine> pricing = SalesDAO.getPricingWithAdjustments(em, proposal);

            // Build proposal link dynamically from request (adapts to any host/port/context)
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            int port = request.getServerPort();
            if (port != 80 && port != 443) baseUrl += ":" + port;
            baseUrl += request.getContextPath() + "/";
            String proposalLink = baseUrl + "proposal/" + proposal.getApplicationGUID();

            boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
            boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
            boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));

            // V067: markup is off by default for every agency (and for the no-agency /
            // PSP-direct case — no admin exception) until explicitly enabled on the agency.
            Agency originatingAgency = OriginatingAgencyResolver.resolve(proposal);
            boolean agencyMarkupEnabled = originatingAgency != null && originatingAgency.isMarkupEnabled();

            request.setAttribute("proposal", proposal);
            request.setAttribute("pricing", pricing);
            request.setAttribute("proposalLink", proposalLink);
            request.setAttribute("canEditMarkup", (isPspAdmin || isAgent || isAgencyAdmin) && agencyMarkupEnabled);

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
                Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList LEFT JOIN FETCH p.application WHERE p.id = :id");
                q.setParameter("id", proposalId);
                Proposal proposal = (Proposal) q.getSingleResult();

                Person contact = proposal.getProspect().getContact();
                Person sender = local.getCurrentPerson();
                String toEmail = contact.getEmail();

                String baseUrl = request.getScheme() + "://" + request.getServerName();
                int port = request.getServerPort();
                if (port != 80 && port != 443) baseUrl += ":" + port;
                baseUrl += request.getContextPath() + "/";
                String proposalLink = baseUrl + "proposal/" + proposal.getApplicationGUID();
                String prospectName = contact.getFirstName() != null ? contact.getFirstName() : "there";

                String bodyHtml = "<p>Hi " + prospectName + ",</p>"
                        + "<p>We've prepared a benefits proposal for <strong>" + proposal.getProspect().getName() + "</strong>.</p>"
                        + "<p>Please click the link below to view your customized proposal, including pricing and plan details:</p>"
                        + "<p><a href=\"" + proposalLink + "\" style=\"display:inline-block;padding:12px 24px;background-color:#2B5F8A;color:#ffffff;text-decoration:none;border-radius:4px;font-weight:bold;\">View Your Proposal</a></p>"
                        + "<p>If you have any questions, simply reply to this email.</p>";

                // V069: white-label sender identity + agency-signed body (matches the From).
                Agency agency = OriginatingAgencyResolver.resolve(proposal);
                String wrappedBody = EmailTemplate.wrap(bodyHtml, sender, agency, null, em);
                String subject = "Your Benefits Proposal — " + proposal.getProspect().getName();

                EmailIdentity identity = EmailIdentityResolver.resolve(sender, agency, sender.getPsp(), em);
                EmailDAO.sendEmail(identity, java.util.List.of(toEmail),
                        java.util.Collections.emptyList(), java.util.Collections.emptyList(),
                        subject, wrappedBody, em);

                // Update status
                em.getTransaction().begin();
                proposal.setStatus("SENT");
                proposal.setDateSent(Timestamp.from(Instant.now()));
                em.persist(proposal);
                em.getTransaction().commit();

                System.out.println("Proposal #" + proposalId + " sent to " + toEmail);
            } else if ("saveMarkup".equals(action)) {
                boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
                boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
                boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
                if (!isPspAdmin && !isAgent && !isAgencyAdmin) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                Proposal proposal = em.find(Proposal.class, proposalId);
                if (proposal == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // V067: authoritative gate — a hand-crafted POST must be rejected the same
                // as the UI hides the control, in case the agency isn't markup-enabled (or
                // no agency resolves at all, e.g. a PSP-direct proposal).
                Agency originatingAgency = OriginatingAgencyResolver.resolve(proposal);
                boolean agencyMarkupEnabled = originatingAgency != null && originatingAgency.isMarkupEnabled();
                if (!agencyMarkupEnabled) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                String[] moduleIds = request.getParameterValues("moduleId");
                String[] priceItemIds = request.getParameterValues("priceItemId");
                String[] markups = request.getParameterValues("markup");

                if (moduleIds != null && priceItemIds != null && markups != null
                        && moduleIds.length == priceItemIds.length && moduleIds.length == markups.length) {
                    Person currentUser = local.getCurrentPerson();
                    for (int i = 0; i < moduleIds.length; i++) {
                        double markupAmount;
                        try {
                            markupAmount = Double.parseDouble(markups[i]);
                        } catch (NumberFormatException e) {
                            continue;
                        }
                        if (markupAmount < 0) continue; // upward-only — silently skip invalid negative input

                        ServiceModule module = em.find(ServiceModule.class, Long.parseLong(moduleIds[i]));
                        PriceItem priceItem = em.find(PriceItem.class, Long.parseLong(priceItemIds[i]));
                        if (module == null || priceItem == null) continue;

                        SalesDAO.saveProposalPriceAdjustment(em, proposal, module, priceItem, markupAmount, currentUser);
                    }
                }

                System.out.println("Proposal #" + proposalId + " markup updated by " + local.getCurrentPerson().getId());
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
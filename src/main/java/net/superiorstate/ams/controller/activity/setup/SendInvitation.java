package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "SendInvitation", value = "/SendInvitation")
public class SendInvitation extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");

        String agencyIdParam = null;

        try {
            String mode = request.getParameter("inviteMode"); // "new" or "existing"
            String role = request.getParameter("role");       // "AGENCY_MANAGER" or "AGENT"
            String firstName = request.getParameter("firstName").trim();
            String lastName = request.getParameter("lastName").trim();
            String email = request.getParameter("email").trim();

            Agency agency;

            if ("new".equals(mode)) {
                // Create new agency
                String agencyName = request.getParameter("agencyName").trim();

                em.getTransaction().begin();
                Address address = new Address();
                em.persist(address);

                agency = new Agency();
                agency.setName(agencyName);
                agency.setPsp(local.getCurrentPerson().getPsp());
                agency.setAddress(address);
                agency.setAgencyRateList(new ArrayList<>());
                agency.setAgentList(new ArrayList<>());
                em.persist(agency);
                em.getTransaction().commit();

                // Assign selected rates
                String[] rateIds = request.getParameterValues("rateIds");
                if (rateIds != null && rateIds.length > 0) {
                    for (String rateIdStr : rateIds) {
                        Rate rate = EntityLookup.getRateById(em, Long.parseLong(rateIdStr));
                        if (rate != null) {
                            agency.addRate(rate);
                        }
                    }
                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }

            } else {
                // Existing agency
                long existingId = Long.parseLong(request.getParameter("existingAgencyId"));
                agency = em.find(Agency.class, existingId);

                // Also assign rates if provided
                String[] rateIds = request.getParameterValues("rateIds");
                if (rateIds != null && rateIds.length > 0) {
                    for (String rateIdStr : rateIds) {
                        long rateId = Long.parseLong(rateIdStr);
                        boolean alreadyAssigned = false;
                        if (agency.getAgencyRateList() != null) {
                            for (Rate r : agency.getAgencyRateList()) {
                                if (r.getId() == rateId) { alreadyAssigned = true; break; }
                            }
                        }
                        if (!alreadyAssigned) {
                            Rate rate = EntityLookup.getRateById(em, rateId);
                            if (rate != null) agency.addRate(rate);
                        }
                    }
                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }
            }

            agencyIdParam = agency.getId().toString();

            // Create person for the contact
            em.getTransaction().begin();
            Person person = new Person();
            person.setFirstName(firstName);
            person.setLastName(lastName);
            person.setFullName(firstName + " " + lastName);
            person.setEmail(email);
            person.setPsp(local.getCurrentPerson().getPsp());
            em.persist(person);
            em.getTransaction().commit();

            // Set as primary contact if agency doesn't have one
            if (agency.getPrimaryContact() == null) {
                em.getTransaction().begin();
                agency.setPrimaryContact(person);
                em.merge(agency);
                em.getTransaction().commit();
            }
            // Link person to invitation for reliable lookup later

            // Create invitation record
            String guid = UUID.randomUUID().toString();
            Timestamp expires = Timestamp.valueOf(LocalDateTime.now().plusDays(30));

            em.getTransaction().begin();
            Invitation invitation = new Invitation();
            invitation.setGuid(guid);
            invitation.setEmail(email);
            invitation.setFirstName(firstName);
            invitation.setLastName(lastName);
            invitation.setAgency(agency);
            invitation.setRole(role);
            invitation.setInvitedBy(local.getCurrentPerson());
            invitation.setPerson(person);
            invitation.setDateExpires(expires);
            invitation.setIsUsed(false);
            em.persist(invitation);
            em.getTransaction().commit();

            // Send invitation email
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            int port = request.getServerPort();
            if (port != 80 && port != 443) baseUrl += ":" + port;
            baseUrl += request.getContextPath();

            String inviteLink = baseUrl + "/AcceptInvite?guid=" + guid;
            String roleLabel = "AGENCY_MANAGER".equals(role) ? "Agency Manager" : "Agent";

            String subject = "You've been invited to join " + agency.getName();
            String body = "<p>Hello " + firstName + ",</p>"
                + "<p>You have been invited to join <strong>" + agency.getName() + "</strong> as an <strong>" + roleLabel + "</strong>.</p>"
                + "<p>Click the link below to complete your registration:</p>"
                + "<p><a href=\"" + inviteLink + "\">" + inviteLink + "</a></p>"
                + "<p>This invitation expires in 30 days.</p>"
                + "<p>Thank you,<br>" + global.getPsp().getFullName() + "</p>";

            try {
                String fromEmail = local.getCurrentPerson().getEmail();
                List<String> toList = new ArrayList<>();
                toList.add(email);
                EmailDAO.sendEmail(fromEmail, toList, new ArrayList<>(), new ArrayList<>(), subject, body, em);
                System.out.println("✅ Invitation email sent to " + email + " (guid: " + guid + ")");
            } catch (Exception emailEx) {
                System.err.println("❌ Failed to send invitation email to " + email + ": " + emailEx.getMessage());
            }

        } finally {
            em.close();
        }

        // Refresh global sales data cache (new agency/agent/rate assignments)
        EntityManagerFactory emf2 = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em2 = emf2.createEntityManager();
        try {
            global.refreshSalesData(em2);
            getServletContext().setAttribute("global", global);
        } finally {
            em2.close();
        }

        String redirectUrl = "PspAgencyHome";
        if (agencyIdParam != null) redirectUrl += "?agencyId=" + agencyIdParam;
        response.sendRedirect(redirectUrl);
    }
}

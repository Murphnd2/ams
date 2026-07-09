package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.service.DatabaseInitializer;
import net.superiorstate.ams.data.util.LandingSafe;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@WebServlet(name = "AgencyAction", value = "/AgencyAction")
public class AgencyAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // V067 hardening: Agency Manager (agencyManager25.jsp) is nav-gated to PSP admins
        // only, but this servlet itself had no server-side check — every action here
        // (including the new markup_enabled toggle) is a capability grant, so enforce it
        // here directly rather than relying solely on the nav link being hidden.
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");

        // V068: AJAX save of the agency landing-page HTML (mirrors UpdatePspSettings.saveLandingHtml).
        // Sanitized on save via LandingSafe; returns JSON and does not fall through to the redirect.
        if ("saveAgencyLandingHtml".equals(action)) {
            saveAgencyLandingHtml(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        String agencyIdParam = request.getParameter("agencyId");

        try {
            switch (action) {

                case "createAgency" -> {
                    String name = request.getParameter("agencyName");
                    String phone = request.getParameter("phone");
                    String taxId = request.getParameter("taxId");

                    String contactFirst = request.getParameter("contactFirst");
                    String contactLast = request.getParameter("contactLast");
                    String contactEmail = request.getParameter("contactEmail");

                    em.getTransaction().begin();

                    Address address = new Address();
                    em.persist(address);

                    Agency agency = new Agency();
                    agency.setName(name.trim());
                    String trimmedPhone = phone != null ? phone.trim() : null;
                    agency.setPhone(trimmedPhone);
                    agency.setTaxId(taxId != null ? taxId.trim() : null);
                    agency.setPsp(psp);
                    agency.setAddress(address);
                    agency.setAgencyRateList(new ArrayList<>());
                    agency.setAgentList(new ArrayList<>());

                    // Create primary contact / agency manager
                    if (contactFirst != null && !contactFirst.trim().isEmpty()) {
                        Person contact = new Person();
                        contact.setFirstName(contactFirst.trim());
                        contact.setLastName(contactLast != null ? contactLast.trim() : null);
                        contact.setEmail(contactEmail != null ? contactEmail.trim() : null);
                        contact.setPhone(trimmedPhone);  // Use agency phone for contact too
                        contact.setFullName(contactFirst.trim() + " " + (contactLast != null ? contactLast.trim() : ""));
                        contact.setPsp(psp);
                        contact.setAddress(address);
                        em.persist(contact);
                        agency.setPrimaryContact(contact);
                        agency.setManager(contact);
                    }

                    em.persist(agency);

                    em.getTransaction().commit();
                    agencyIdParam = agency.getId().toString();

                    // Create user account for the agency manager (if email provided)
                    Person contact = agency.getPrimaryContact();
                    if (contact != null && contactEmail != null && !contactEmail.trim().isEmpty()) {
                        String tempPw = UUID.randomUUID().toString();
                        User managerUser = DatabaseInitializer.createUser(em, contact, contactEmail.trim(), tempPw);

                        // Assign Agency Admin (8) and Agent (2) roles
                        UserRole agencyAdminRole = AuthDAO.getUserRoleById(em, 8);
                        UserRole agentRole = AuthDAO.getUserRoleById(em, 2);
                        em.getTransaction().begin();
                        managerUser.addUserToRole(agencyAdminRole);
                        managerUser.addUserToRole(agentRole);
                        em.persist(managerUser);
                        em.getTransaction().commit();

                        // Add manager to agency's agent list
                        em.getTransaction().begin();
                        agency.getAgentList().add(contact);
                        em.merge(agency);
                        em.getTransaction().commit();

                        System.out.println("Created user for agency manager: " + contactEmail.trim()
                                + " (Agency: " + agency.getName() + ")");
                    }
                }

                case "editAgency" -> {
                    long agencyId = Long.parseLong(agencyIdParam);

                    // V068: validate the optional landing host BEFORE any mutation. A blank
                    // host clears it; a bad/duplicate/PSP host is rejected with a friendly
                    // redirect (not a bubbled 500 from the unique index).
                    String landingHost = AmsDataGlobal.normalizeHost(request.getParameter("landingHost"));
                    if (!landingHost.isEmpty()) {
                        AmsDataGlobal g = (AmsDataGlobal) getServletContext().getAttribute("global");
                        String hostError = validateLandingHost(em, landingHost, agencyId, g);
                        if (hostError != null) {
                            response.sendRedirect("PspAgencyHome?agencyId=" + agencyId + "&landingError=" + hostError);
                            return; // outer finally closes the EntityManager
                        }
                    }

                    // V069: validate the optional email sending domain BEFORE any mutation.
                    String emailDomain = AmsDataGlobal.normalizeHost(request.getParameter("emailDomain"));
                    if (!emailDomain.isEmpty()) {
                        String domainError = validateEmailDomain(em, emailDomain, agencyId);
                        if (domainError != null) {
                            response.sendRedirect("PspAgencyHome?agencyId=" + agencyId + "&emailError=" + domainError);
                            return; // outer finally closes the EntityManager
                        }
                    }
                    // email_verified can only be true when a domain is present.
                    boolean emailVerified = !emailDomain.isEmpty() && "on".equals(request.getParameter("emailVerified"));

                    Agency agency = em.find(Agency.class, agencyId);

                    agency.setName(request.getParameter("agencyName").trim());
                    String phone = request.getParameter("phone");
                    agency.setPhone(phone != null ? phone.trim() : null);
                    String taxId = request.getParameter("taxId");
                    agency.setTaxId(taxId != null ? taxId.trim() : null);
                    agency.setMarkupEnabled("on".equals(request.getParameter("markupEnabled")));
                    agency.setLandingHost(landingHost.isEmpty() ? null : landingHost);
                    agency.setEmailDomain(emailDomain.isEmpty() ? null : emailDomain);
                    agency.setEmailVerified(emailVerified);

                    // Update address
                    Address address = agency.getAddress();
                    if (address == null) {
                        address = new Address();
                        em.getTransaction().begin();
                        em.persist(address);
                        em.getTransaction().commit();
                        agency.setAddress(address);
                    }
                    em.getTransaction().begin();
                    address.setAddress1(request.getParameter("address1"));
                    address.setAddress2(request.getParameter("address2"));
                    address.setCity(request.getParameter("city"));
                    address.setState(request.getParameter("state"));
                    address.setZipCode(request.getParameter("zipCode"));
                    em.merge(address);
                    em.getTransaction().commit();

                    // Update or create primary contact
                    String contactFirst = request.getParameter("contactFirst");
                    String contactLast = request.getParameter("contactLast");
                    String contactEmail = request.getParameter("contactEmail");
                    String contactPhone = request.getParameter("contactPhone");

                    Person contact = agency.getPrimaryContact();
                    if (contact == null && contactFirst != null && !contactFirst.trim().isEmpty()) {
                        contact = new Person();
                        contact.setPsp(psp);
                        contact.setAddress(agency.getAddress());
                        em.getTransaction().begin();
                        em.persist(contact);
                        em.getTransaction().commit();
                        agency.setPrimaryContact(contact);
                    }
                    if (contact != null) {
                        em.getTransaction().begin();
                        contact.setFirstName(contactFirst != null ? contactFirst.trim() : null);
                        contact.setLastName(contactLast != null ? contactLast.trim() : null);
                        contact.setEmail(contactEmail != null ? contactEmail.trim() : null);
                        contact.setPhone(contactPhone != null ? contactPhone.trim() : null);
                        contact.setFullName((contact.getFirstName() != null ? contact.getFirstName() : "") + " " + (contact.getLastName() != null ? contact.getLastName() : ""));
                        em.merge(contact);
                        em.getTransaction().commit();
                    }

                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }

                case "updateRates" -> {
                    long agencyId = Long.parseLong(agencyIdParam);
                    Agency agency = SalesDAO.getAgencyFull(em, agencyId);

                    // Get checked rate IDs from form
                    String[] rateIds = request.getParameterValues("rateIds");
                    List<Long> selectedRateIds = new ArrayList<>();
                    if (rateIds != null) {
                        for (String id : rateIds) {
                            selectedRateIds.add(Long.parseLong(id));
                        }
                    }

                    // Remove rates no longer checked
                    List<Rate> toRemove = new ArrayList<>();
                    for (Rate r : agency.getAgencyRateList()) {
                        if (!selectedRateIds.contains(r.getId())) {
                            toRemove.add(r);
                        }
                    }
                    for (Rate r : toRemove) {
                        agency.removeRate(r);
                    }

                    // Add newly checked rates
                    List<Long> currentRateIds = new ArrayList<>();
                    for (Rate r : agency.getAgencyRateList()) {
                        currentRateIds.add(r.getId());
                    }
                    for (Long rateId : selectedRateIds) {
                        if (!currentRateIds.contains(rateId)) {
                            Rate rate = EntityLookup.getRateById(em, rateId);
                            agency.addRate(rate);
                        }
                    }

                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }

                case "assignAgent" -> {
                    long agencyId = Long.parseLong(agencyIdParam);
                    long agentId = Long.parseLong(request.getParameter("agentId"));

                    Agency agency = SalesDAO.getAgencyFull(em, agencyId);
                    // Also need agent list loaded
                    try {
                        SalesDAO.getAgencyAgents(em, agencyId);
                    } catch (Exception ignored) {}
                    agency = em.find(Agency.class, agencyId);

                    Person agent = EntityLookup.getPersonById(em, agentId);

                    // Check not already assigned
                    boolean alreadyAssigned = false;
                    if (agency.getAgentList() != null) {
                        for (Person p : agency.getAgentList()) {
                            if (p.getId().equals(agent.getId())) {
                                alreadyAssigned = true;
                                break;
                            }
                        }
                    }

                    if (!alreadyAssigned) {
                        agency.addAgent(agent);
                        em.getTransaction().begin();
                        em.merge(agency);
                        em.getTransaction().commit();
                    }
                }

                case "removeAgent" -> {
                    long agencyId = Long.parseLong(agencyIdParam);
                    long agentId = Long.parseLong(request.getParameter("agentId"));

                    // Need full agency with both rates and agents loaded
                    Agency agency = em.find(Agency.class, agencyId);
                    Person agent = EntityLookup.getPersonById(em, agentId);

                    agency.removeAgent(agent);
                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }

                case "suppressAgency" -> {
                    long agencyId = Long.parseLong(agencyIdParam);
                    Agency agency = SalesDAO.getAgencyFull(em, agencyId);

                    // Remove all rate assignments
                    if (agency.getAgencyRateList() != null) {
                        List<Rate> toRemove = new ArrayList<>(agency.getAgencyRateList());
                        for (Rate r : toRemove) {
                            agency.removeRate(r);
                        }
                    }

                    agency.setSuppressed(true);
                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }

                case "unsuppressAgency" -> {
                    long agencyId = Long.parseLong(agencyIdParam);
                    Agency agency = em.find(Agency.class, agencyId);

                    agency.setSuppressed(false);
                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }
            }

        } finally {
            em.close();
        }

        // Refresh global sales data cache
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

        String redirectUrl = "PspAgencyHome";
        if (agencyIdParam != null && !agencyIdParam.isEmpty()) {
            redirectUrl += "?agencyId=" + agencyIdParam;
        }
        response.sendRedirect(redirectUrl);
    }

    // ═══ V068: agency landing-page HTML + host validation ═══

    /** Hostname format (already normalized lowercase): dotted labels, no scheme/path/space/*. */
    private static final Pattern HOST_PATTERN =
            Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$");

    /**
     * AJAX handler: sanitize (LandingSafe) and store the agency's custom landing HTML in
     * agency.landing_html, then rebuild the global caches so it serves immediately.
     * Mirrors {@code UpdatePspSettings.saveLandingHtml}. Returns JSON.
     */
    private void saveAgencyLandingHtml(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long agencyId = Long.parseLong(request.getParameter("agencyId"));
            String sanitized = LandingSafe.clean(request.getParameter("landingHtml"));

            em.getTransaction().begin();
            Agency agency = em.find(Agency.class, agencyId);
            if (agency == null) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                writeJson(response, "{\"status\":\"error\",\"message\":\"Agency not found\"}");
                return;
            }
            agency.setLandingHtml(sanitized);
            em.merge(agency);
            em.getTransaction().commit();

            // Rebuild caches (incl. the host → agency map) so the new HTML serves immediately.
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
            writeJson(response, "{\"status\":\"ok\"}");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            writeJson(response, "{\"status\":\"error\"}");
        } finally {
            em.close();
        }
    }

    /**
     * V069: validate a normalized-lowercase email sending domain. Returns an error code
     * ("format" | "duplicate") for a friendly redirect, or null if valid and free.
     */
    private String validateEmailDomain(EntityManager em, String domain, long agencyId) {
        if (domain.length() > 255 || !HOST_PATTERN.matcher(domain).matches()) return "format";
        Long count = em.createQuery(
                        "SELECT COUNT(a) FROM Agency a WHERE a.emailDomain = :d AND a.id <> :id", Long.class)
                .setParameter("d", domain)
                .setParameter("id", agencyId)
                .getSingleResult();
        if (count != null && count > 0) return "duplicate";
        return null;
    }

    /**
     * Validate a normalized-lowercase landing host. Returns an error code
     * ("format" | "psp" | "duplicate") for a friendly redirect, or null if valid and free.
     */
    private String validateLandingHost(EntityManager em, String host, long agencyId, AmsDataGlobal global) {
        if (host.length() > 255 || !HOST_PATTERN.matcher(host).matches()) return "format";
        if (global != null && global.isPspHost(host)) return "psp";
        Long count = em.createQuery(
                        "SELECT COUNT(a) FROM Agency a WHERE a.landingHost = :h AND a.id <> :id", Long.class)
                .setParameter("h", host)
                .setParameter("id", agencyId)
                .getSingleResult();
        if (count != null && count > 0) return "duplicate";
        return null;
    }

    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(json);
    }
}
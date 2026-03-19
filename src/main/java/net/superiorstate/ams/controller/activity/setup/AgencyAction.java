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
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AgencyAction", value = "/AgencyAction")
public class AgencyAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        String action = request.getParameter("action");
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
                    Agency agency = em.find(Agency.class, agencyId);

                    agency.setName(request.getParameter("agencyName").trim());
                    String phone = request.getParameter("phone");
                    agency.setPhone(phone != null ? phone.trim() : null);
                    String taxId = request.getParameter("taxId");
                    agency.setTaxId(taxId != null ? taxId.trim() : null);

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
}
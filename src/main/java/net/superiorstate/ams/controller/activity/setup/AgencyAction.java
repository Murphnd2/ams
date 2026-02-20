package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

                    em.getTransaction().begin();

                    Address address = new Address();
                    em.persist(address);

                    Agency agency = new Agency();
                    agency.setName(name.trim());
                    agency.setPhone(phone != null ? phone.trim() : null);
                    agency.setTaxId(taxId != null ? taxId.trim() : null);
                    agency.setPsp(psp);
                    agency.setAddress(address);
                    agency.setAgencyRateList(new ArrayList<>());
                    agency.setAgentList(new ArrayList<>());
                    em.persist(agency);

                    em.getTransaction().commit();
                    agencyIdParam = agency.getId().toString();
                }

                case "editAgency" -> {
                    long agencyId = Long.parseLong(agencyIdParam);
                    Agency agency = em.find(Agency.class, agencyId);

                    agency.setName(request.getParameter("agencyName").trim());
                    String phone = request.getParameter("phone");
                    agency.setPhone(phone != null ? phone.trim() : null);
                    String taxId = request.getParameter("taxId");
                    agency.setTaxId(taxId != null ? taxId.trim() : null);

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
            }

        } finally {
            em.close();
        }

        String redirectUrl = "PspAgencyHome";
        if (agencyIdParam != null && !agencyIdParam.isEmpty()) {
            redirectUrl += "?agencyId=" + agencyIdParam;
        }
        response.sendRedirect(redirectUrl);
    }
}
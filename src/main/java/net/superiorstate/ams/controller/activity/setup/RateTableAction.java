package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.util.List;
import java.util.List;

@WebServlet(name = "RateTableAction", value = "/RateTableAction")
public class RateTableAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        String action = request.getParameter("action");
        String rateIdParam = request.getParameter("rateId");

        try {
            switch (action) {

                case "createRate" -> {
                    String description = request.getParameter("description");
                    Rate rate = new Rate();
                    rate.setDescription(description.trim());
                    rate.setPsp(psp);
                    rate.setSuppressed(false);
                    em.getTransaction().begin();
                    em.persist(rate);
                    em.getTransaction().commit();
                    rateIdParam = rate.getId().toString();
                }

                case "editRate" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    Rate rate = EntityLookup.getRateById(em, rateId);
                    rate.setDescription(request.getParameter("description").trim());
                    rate.setSuppressed(request.getParameter("suppressed") != null);
                    em.getTransaction().begin();
                    em.merge(rate);
                    em.getTransaction().commit();
                }

                case "addRateTableRow" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    long moduleId = Long.parseLong(request.getParameter("moduleId"));
                    long priceItemId = Long.parseLong(request.getParameter("priceItemId"));
                    double price = Double.parseDouble(request.getParameter("price"));

                    Rate rate = EntityLookup.getRateById(em, rateId);
                    ServiceModule module = EntityLookup.getServiceModuleById(em, (int) moduleId);
                    PriceItem priceItem = EntityLookup.getPriceItemById(em, priceItemId);

                    RateTable rt = new RateTable();
                    rt.setRate(rate);
                    rt.setModule(module);
                    rt.setPriceItem(priceItem);
                    rt.setPrice(price);

                    em.getTransaction().begin();
                    em.persist(rt);
                    em.getTransaction().commit();
                }

                case "deleteRow" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    long priceItemId = Long.parseLong(request.getParameter("priceItemId"));
                    long moduleId = Long.parseLong(request.getParameter("moduleId"));

                    RateTableID rtId = new RateTableID();
                    rtId.setRateId(rateId);
                    rtId.setPriceItemId(priceItemId);
                    rtId.setModuleId(moduleId);

                    RateTable rt = em.find(RateTable.class, rtId);
                    if (rt != null) {
                        em.getTransaction().begin();
                        em.remove(rt);
                        em.getTransaction().commit();
                    }
                }

                case "assignAgencyToRate" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    long agencyId = Long.parseLong(request.getParameter("agencyId"));

                    Agency agency = SalesDAO.getAgencyFull(em, agencyId);
                    Rate rate = EntityLookup.getRateById(em, rateId);

                    // Check if already assigned
                    boolean alreadyAssigned = false;
                    for (Rate r : agency.getAgencyRateList()) {
                        if (r.getId().equals(rate.getId())) {
                            alreadyAssigned = true;
                            break;
                        }
                    }

                    if (!alreadyAssigned) {
                        agency.addRate(rate);
                        em.getTransaction().begin();
                        em.merge(agency);
                        em.getTransaction().commit();
                    }
                }

                case "removeAgencyFromRate" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    long agencyId = Long.parseLong(request.getParameter("agencyId"));

                    Agency agency = SalesDAO.getAgencyFull(em, agencyId);
                    Rate rate = EntityLookup.getRateById(em, rateId);

                    agency.removeRate(rate);
                    em.getTransaction().begin();
                    em.merge(agency);
                    em.getTransaction().commit();
                }

                case "createPriceItem" -> {
                    String description = request.getParameter("description");

                    // Auto-assign sort order: max existing + 100
                    List<PriceItem> existingItems = SalesDAO.getPriceItemList(em, psp.getId().intValue());
                    int maxSort = 0;
                    for (PriceItem existing : existingItems) {
                        if (existing.getSortOrder() > maxSort) maxSort = existing.getSortOrder();
                    }

                    PriceItem pi = new PriceItem();
                    pi.setDescription(description.trim());
                    pi.setSortOrder(maxSort + 100);
                    pi.setPsp(psp);
                    pi.setSuppressed(false);

                    em.getTransaction().begin();
                    em.persist(pi);
                    em.getTransaction().commit();
                }

                case "cloneRate" -> {
                    long oldRateId = Long.parseLong(rateIdParam);
                    Rate oldRate = EntityLookup.getRateById(em, oldRateId);

                    // 1. Create new rate with same name
                    Rate newRate = new Rate();
                    newRate.setDescription(oldRate.getDescription());
                    newRate.setPsp(psp);
                    newRate.setSuppressed(false);
                    em.getTransaction().begin();
                    em.persist(newRate);
                    em.getTransaction().commit();

                    // 2. Copy all rate table rows
                    List<RateTable> oldRows = SalesDAO.getRateTableList(em, oldRateId);
                    for (RateTable oldRow : oldRows) {
                        RateTable newRow = new RateTable();
                        newRow.setRate(newRate);
                        newRow.setModule(oldRow.getModule());
                        newRow.setPriceItem(oldRow.getPriceItem());
                        newRow.setPrice(oldRow.getPrice());
                        em.getTransaction().begin();
                        em.persist(newRow);
                        em.getTransaction().commit();
                    }

                    // 3. Copy agency assignments — snapshot IDs first to avoid concurrent modification
                    try {
                        List<Agency> assignedAgencies = SalesDAO.getAgenciesAssignedToRate(em, oldRateId);
                        java.util.List<Long> agencyIds = new java.util.ArrayList<>();
                        for (Agency a : assignedAgencies) {
                            agencyIds.add(a.getId());
                        }
                        for (Long agId : agencyIds) {
                            Agency fullAgency = SalesDAO.getAgencyFull(em, agId);
                            fullAgency.addRate(newRate);
                            fullAgency.removeRate(oldRate);
                            em.getTransaction().begin();
                            em.merge(fullAgency);
                            em.getTransaction().commit();
                        }
                    } catch (Exception e) {
                        // No agencies assigned — that's fine
                    }

                    // 4. Suppress the old rate
                    em.refresh(oldRate);
                    oldRate.setSuppressed(true);
                    em.getTransaction().begin();
                    em.merge(oldRate);
                    em.getTransaction().commit();

                    // Redirect to the new rate
                    rateIdParam = newRate.getId().toString();
                }
            }

        } finally {
            em.close();
        }

        // Redirect back to rate manager, preserving selected rate
        String redirectUrl = "PspAdminHome";
        if (rateIdParam != null && !rateIdParam.isEmpty()) {
            redirectUrl += "?rateId=" + rateIdParam;
        }
        response.sendRedirect(redirectUrl);
    }
}
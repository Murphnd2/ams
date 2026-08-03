package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RateTableAction", value = "/RateTableAction")
public class RateTableAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // T124 hardening: Rate Manager (rateManager25.jsp) is nav-gated to PSP admins only, but
        // this servlet itself had no server-side check — and every action here mutates pricing
        // (updatePrice, addRateTableRow, deleteRow) or rate-table assignment (assignAgencyToRate,
        // removeAgencyFromRate), so an unguarded POST could change what a client is billed.
        // Placed before the action dispatch so it covers the whole switch, and before EMF
        // acquisition so a rejected request never opens an EntityManager.
        // Same shape as AgencyAction.doPost's V067 guard — deliberately identical, not improved.
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

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

                case "suppressRate" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    Rate rate = EntityLookup.getRateById(em, rateId);
                    em.getTransaction().begin();
                    rate.setSuppressed(!rate.isSuppressed());
                    em.merge(rate);
                    em.getTransaction().commit();
                    // Clear selection after suppress so we don't try to show a hidden rate
                    if (rate.isSuppressed()) rateIdParam = null;
                }

                case "addRateTableRow" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    long priceItemId = Long.parseLong(request.getParameter("priceItemId"));
                    double price = Double.parseDouble(request.getParameter("price"));

                    Rate rate = EntityLookup.getRateById(em, rateId);
                    PriceItem priceItem = EntityLookup.getPriceItemById(em, priceItemId);

                    // Determine the ServiceModule — find existing or create new
                    ServiceModule module = null;
                    String losIdParam = request.getParameter("losId");
                    String enhIdParam = request.getParameter("enhId");

                    if (losIdParam != null && !losIdParam.isEmpty()) {
                        long losId = Long.parseLong(losIdParam);
                        LOS los = EntityLookup.getLosById(em, losId);
                        module = findModuleByLos(em, losId);
                        if (module == null) {
                            // Create a new ServiceModule for this LOS
                            module = new ServiceModule();
                            module.setDescription(los.getDescription());
                            module.setShortText(los.getShortText());
                            module.setSortOrder(los.getSortOrder());
                            module.setPsp(psp);
                            module.setLos(los);
                            module.setSuppressed(false);
                            em.getTransaction().begin();
                            em.persist(module);
                            em.getTransaction().commit();
                        }
                    } else if (enhIdParam != null && !enhIdParam.isEmpty()) {
                        long enhId = Long.parseLong(enhIdParam);
                        Enhancement enh = em.find(Enhancement.class, enhId);
                        module = findModuleByEnhancement(em, enhId);
                        if (module == null) {
                            // Create a new ServiceModule for this Enhancement
                            module = new ServiceModule();
                            module.setDescription(enh.getDescription());
                            module.setShortText(enh.getShortText());
                            module.setSortOrder(enh.getSortOrder());
                            module.setPsp(psp);
                            module.setEnhancement(enh);
                            module.setSuppressed(false);
                            em.getTransaction().begin();
                            em.persist(module);
                            em.getTransaction().commit();
                        }
                    } else {
                        // Fallback: direct moduleId (legacy support)
                        String moduleIdParam = request.getParameter("moduleId");
                        if (moduleIdParam != null && !moduleIdParam.isEmpty()) {
                            module = EntityLookup.getServiceModuleById(em, Integer.parseInt(moduleIdParam));
                        }
                    }

                    if (module != null) {
                        // Determine sort_order for this row:
                        // If this module already has rows in this rate, match their sortOrder.
                        // Otherwise, assign max existing sortOrder + 100.
                        List<RateTable> existingRows = SalesDAO.getRateTableList(em, rateId);
                        int sortOrder = -1;
                        int maxSort = 0;
                        for (RateTable existing : existingRows) {
                            if (existing.getSortOrder() > maxSort) maxSort = existing.getSortOrder();
                            if (existing.getModule().getId().equals(module.getId())) {
                                sortOrder = existing.getSortOrder();
                            }
                        }
                        if (sortOrder < 0) sortOrder = maxSort + 100;

                        RateTable rt = new RateTable();
                        rt.setRate(rate);
                        rt.setModule(module);
                        rt.setPriceItem(priceItem);
                        rt.setPrice(price);
                        rt.setSortOrder(sortOrder);
                        em.getTransaction().begin();
                        em.persist(rt);
                        em.getTransaction().commit();
                    }
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

                case "copyRate" -> {
                    long sourceRateId = Long.parseLong(rateIdParam);
                    String newName = request.getParameter("description").trim();

                    // Create new rate
                    Rate newRate = new Rate();
                    newRate.setDescription(newName);
                    newRate.setPsp(psp);
                    newRate.setSuppressed(false);
                    em.getTransaction().begin();
                    em.persist(newRate);
                    em.getTransaction().commit();

                    // Copy all rate table rows
                    List<RateTable> sourceRows = SalesDAO.getRateTableList(em, sourceRateId);
                    for (RateTable oldRow : sourceRows) {
                        RateTable newRow = new RateTable();
                        newRow.setRate(newRate);
                        newRow.setModule(oldRow.getModule());
                        newRow.setPriceItem(oldRow.getPriceItem());
                        newRow.setPrice(oldRow.getPrice());
                        newRow.setSortOrder(oldRow.getSortOrder());
                        em.getTransaction().begin();
                        em.persist(newRow);
                        em.getTransaction().commit();
                    }

                    // Redirect to the new rate
                    rateIdParam = newRate.getId().toString();
                }

                case "updatePrice" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    long priceItemId = Long.parseLong(request.getParameter("priceItemId"));
                    long moduleId = Long.parseLong(request.getParameter("moduleId"));
                    double newPrice = Double.parseDouble(request.getParameter("price"));

                    RateTableID rtId = new RateTableID();
                    rtId.setRateId(rateId);
                    rtId.setPriceItemId(priceItemId);
                    rtId.setModuleId(moduleId);

                    RateTable rt = em.find(RateTable.class, rtId);
                    if (rt != null) {
                        rt.setPrice(newPrice);
                        em.getTransaction().begin();
                        em.merge(rt);
                        em.getTransaction().commit();
                    }
                }

                case "reorderModules" -> {
                    long rateId = Long.parseLong(rateIdParam);
                    String order = request.getParameter("order");
                    if (order != null && !order.isEmpty()) {
                        String[] moduleIds = order.split(",");
                        List<RateTable> allRows = SalesDAO.getRateTableList(em, rateId);
                        int sortVal = 100;
                        for (String modIdStr : moduleIds) {
                            long modId = Long.parseLong(modIdStr.trim());
                            for (RateTable rt : allRows) {
                                if (rt.getModule().getId() == modId) {
                                    rt.setSortOrder(sortVal);
                                    em.getTransaction().begin();
                                    em.merge(rt);
                                    em.getTransaction().commit();
                                }
                            }
                            sortVal += 100;
                        }
                    }
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
                        newRow.setSortOrder(oldRow.getSortOrder());
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

            // Refresh cached rate/LOS data so setup modal picks up changes
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            global.refreshSalesData(em);

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

    /**
     * Find the ServiceModule linked to a given LOS (by los_id FK).
     * Returns null if none exists yet.
     */
    private ServiceModule findModuleByLos(EntityManager em, long losId) {
        try {
            return em.createQuery(
                    "SELECT sm FROM ServiceModule sm WHERE sm.los.id = :losId", ServiceModule.class)
                    .setParameter("losId", losId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find the ServiceModule linked to a given Enhancement (by enhancement_id FK).
     * Returns null if none exists yet.
     */
    private ServiceModule findModuleByEnhancement(EntityManager em, long enhId) {
        try {
            return em.createQuery(
                    "SELECT sm FROM ServiceModule sm WHERE sm.enhancement.id = :enhId", ServiceModule.class)
                    .setParameter("enhId", enhId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}

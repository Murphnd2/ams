package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.data.service.PackageLoader;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.*;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ServiceManagerAction", value = "/ServiceManagerAction")
public class ServiceManagerAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // T123 hardening: Service Manager (serviceManager25.jsp) is nav-gated to PSP admins
        // only, but this servlet itself had no server-side check — every action here mutates
        // catalog data (LOS, Enhancement, ServiceItem, ServiceModule, ApplicationSection),
        // so enforce it here directly rather than relying solely on the nav link being hidden.
        // Placed before the action dispatch so it covers the whole switch, not one branch.
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
        String losIdParam = request.getParameter("losId");
        String enhIdParam = request.getParameter("enhId");
        String sectionIdParam = request.getParameter("sectionId");

        try {
            switch (action) {

                // ── LOS CRUD ────────────────────────────────────────────

                case "createLos" -> {
                    String description = request.getParameter("description");
                    String shortText = request.getParameter("shortText");
                    LOS los = new LOS();
                    los.setDescription(description.trim());
                    los.setShortText(shortText.trim());
                    los.setSortOrder(9999);
                    los.setSuppressed(false);
                    los.setPsp(psp);
                    em.getTransaction().begin();
                    em.persist(los);
                    em.getTransaction().commit();

                    // Auto-create linked ServiceItem (group 2 = Setup)
                    ActivityCategory setupCategory = em.find(ActivityCategory.class, 2);
                    ServiceItem si = new ServiceItem();
                    si.setDescription(shortText.trim());
                    si.setActivityCategory(setupCategory);
                    si.setPsp(psp);
                    si.setSourceType("MANUAL");
                    si.setSuppressed(false);
                    si.setHasRequiredTasks(true);
                    si.setSortOrder(los.getSortOrder());
                    em.getTransaction().begin();
                    em.persist(si);
                    los.setServiceItem(si);
                    em.merge(los);
                    em.getTransaction().commit();

                    // Auto-create linked ServiceModule (required for features)
                    ServiceModule sm = new ServiceModule();
                    sm.setDescription(description.trim());
                    sm.setShortText(shortText.trim());
                    sm.setSortOrder(los.getSortOrder());
                    sm.setPsp(psp);
                    sm.setLos(los);
                    em.getTransaction().begin();
                    em.persist(sm);
                    em.getTransaction().commit();

                    // Auto-link all ALL-scoped sections to this new LOS
                    List<ApplicationSection> allScopedSections = em.createQuery(
                                    "SELECT s FROM ApplicationSection s WHERE s.psp.id = :pspId AND s.scope = 'ALL' AND s.suppressed = false",
                                    ApplicationSection.class)
                            .setParameter("pspId", psp.getId().longValue())
                            .getResultList();
                    if (!allScopedSections.isEmpty()) {
                        em.getTransaction().begin();
                        for (ApplicationSection section : allScopedSections) {
                            if (!section.getLosList().contains(los)) {
                                section.getLosList().add(los);
                                em.merge(section);
                            }
                        }
                        em.getTransaction().commit();
                    }

                    losIdParam = los.getId().toString();
                }

                case "editLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    LOS los = EntityLookup.getLosById(em, losId);
                    em.getTransaction().begin();
                    los.setDescription(request.getParameter("description").trim());
                    los.setShortText(request.getParameter("shortText").trim());
                    // V086: plus-tier classification. Unconditional assignment, mirroring
                    // AgencyAction's ichraEnabled/markupEnabled handling exactly — an unchecked
                    // HTML checkbox submits no parameter at all, so this must set false rather
                    // than skip, or the flag could be set but never cleared. The edit modal
                    // always posts the whole form, so absence here genuinely means unchecked.
                    los.setPlusTier("on".equals(request.getParameter("plusTier")));
                    em.merge(los);
                    em.getTransaction().commit();
                }

                case "suppressLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    LOS los = EntityLookup.getLosById(em, losId);
                    em.getTransaction().begin();
                    los.setSuppressed(!los.isSuppressed());
                    em.merge(los);
                    em.getTransaction().commit();
                }

                // ── Enhancement CRUD ────────────────────────────────────

                case "createEnhancement" -> {
                    String desc = request.getParameter("description");
                    String shortText = request.getParameter("shortText");
                    Enhancement enh = new Enhancement();
                    enh.setDescription(desc.trim());
                    enh.setShortText(shortText.trim());
                    enh.setSortOrder(9999);
                    enh.setSuppressed(false);
                    enh.setPsp(psp);
                    em.getTransaction().begin();
                    em.persist(enh);
                    em.getTransaction().commit();

                    // Auto-create linked ServiceItem (group 2 = Setup)
                    ActivityCategory setupCategory = em.find(ActivityCategory.class, 2);
                    ServiceItem si = new ServiceItem();
                    si.setDescription(shortText.trim());
                    si.setActivityCategory(setupCategory);
                    si.setPsp(psp);
                    si.setSourceType("MANUAL");
                    si.setSuppressed(false);
                    si.setHasRequiredTasks(true);
                    si.setSortOrder(enh.getSortOrder());
                    em.getTransaction().begin();
                    em.persist(si);
                    enh.setServiceItem(si);
                    em.merge(enh);
                    em.getTransaction().commit();

                    // Auto-create linked ServiceModule (required for features)
                    ServiceModule sm = new ServiceModule();
                    sm.setDescription(desc.trim());
                    sm.setShortText(shortText.trim());
                    sm.setSortOrder(enh.getSortOrder());
                    sm.setPsp(psp);
                    sm.setEnhancement(enh);
                    em.getTransaction().begin();
                    em.persist(sm);
                    em.getTransaction().commit();

                    // Auto-link all ALL-scoped sections to this new Enhancement
                    List<ApplicationSection> allScopedSections = em.createQuery(
                                    "SELECT s FROM ApplicationSection s WHERE s.psp.id = :pspId AND s.scope = 'ALL' AND s.suppressed = false",
                                    ApplicationSection.class)
                            .setParameter("pspId", psp.getId().longValue())
                            .getResultList();
                    if (!allScopedSections.isEmpty()) {
                        em.getTransaction().begin();
                        for (ApplicationSection section : allScopedSections) {
                            if (!section.getEnhancementList().contains(enh)) {
                                section.getEnhancementList().add(enh);
                                em.merge(section);
                            }
                        }
                        em.getTransaction().commit();
                    }

                    enhIdParam = enh.getId().toString();
                }

                case "editEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    em.getTransaction().begin();
                    enh.setDescription(request.getParameter("description").trim());
                    enh.setShortText(request.getParameter("shortText").trim());
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                case "suppressEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    em.getTransaction().begin();
                    enh.setSuppressed(!enh.isSuppressed());
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                // ── LOS ↔ Enhancement Associations ──────────────────────

                case "assignLosToEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long losId = Long.parseLong(request.getParameter("losId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    LOS los = EntityLookup.getLosById(em, losId);
                    if (!enh.getLosList().contains(los)) {
                        em.getTransaction().begin();
                        enh.getLosList().add(los);
                        em.merge(enh);
                        em.getTransaction().commit();
                    }
                }

                case "removeLosFromEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long losId = Long.parseLong(request.getParameter("losId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    LOS los = EntityLookup.getLosById(em, losId);
                    em.getTransaction().begin();
                    enh.getLosList().remove(los);
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                case "assignEnhancementToLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long enhId = Long.parseLong(request.getParameter("enhId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    if (!enh.getLosList().contains(los)) {
                        em.getTransaction().begin();
                        enh.getLosList().add(los);
                        em.merge(enh);
                        em.getTransaction().commit();
                    }
                }

                case "removeEnhancementFromLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long enhId = Long.parseLong(request.getParameter("enhId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    em.getTransaction().begin();
                    enh.getLosList().remove(los);
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                // ── ApplicationSection ↔ LOS Associations ───────────────

                case "assignAppSectionToLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long sId = Long.parseLong(request.getParameter("sectionId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    if (!section.getLosList().contains(los)) {
                        em.getTransaction().begin();
                        section.getLosList().add(los);
                        em.merge(section);
                        em.getTransaction().commit();
                    }
                }

                case "removeAppSectionFromLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long sId = Long.parseLong(request.getParameter("sectionId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    em.getTransaction().begin();
                    section.getLosList().remove(los);
                    em.merge(section);
                    em.getTransaction().commit();
                }

                // ── ApplicationSection ↔ Enhancement Associations ───────

                case "assignAppSectionToEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long sId = Long.parseLong(request.getParameter("sectionId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    if (!section.getEnhancementList().contains(enh)) {
                        em.getTransaction().begin();
                        section.getEnhancementList().add(enh);
                        em.merge(section);
                        em.getTransaction().commit();
                    }
                }

                case "removeAppSectionFromEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long sId = Long.parseLong(request.getParameter("sectionId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    em.getTransaction().begin();
                    section.getEnhancementList().remove(enh);
                    em.merge(section);
                    em.getTransaction().commit();
                }

                // ── ApplicationSection CRUD ─────────────────────────────

                case "createAppSection" -> {
                    String name = request.getParameter("name");
                    String desc = request.getParameter("description");
                    String scope = request.getParameter("scope");
                    ApplicationSection section = new ApplicationSection();
                    section.setName(name.trim());
                    section.setDescription(desc != null ? desc.trim() : "");
                    section.setScope(scope != null ? scope.trim() : "LOS");
                    section.setSortOrder(9999);
                    section.setSuppressed(false);
                    section.setPsp(psp);
                    em.getTransaction().begin();
                    em.persist(section);
                    em.getTransaction().commit();

                    // If ALL-scoped, auto-link to all active LOSs and Enhancements
                    if ("ALL".equals(section.getScope())) {
                        List<LOS> allLos = em.createQuery(
                                        "SELECT l FROM LOS l WHERE l.psp.id = :pspId AND l.suppressed = false", LOS.class)
                                .setParameter("pspId", psp.getId().longValue())
                                .getResultList();
                        List<Enhancement> allEnh = em.createQuery(
                                        "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId AND e.suppressed = false", Enhancement.class)
                                .setParameter("pspId", psp.getId().longValue())
                                .getResultList();
                        em.getTransaction().begin();
                        for (LOS l : allLos) {
                            if (!section.getLosList().contains(l)) {
                                section.getLosList().add(l);
                            }
                        }
                        for (Enhancement e : allEnh) {
                            if (!section.getEnhancementList().contains(e)) {
                                section.getEnhancementList().add(e);
                            }
                        }
                        em.merge(section);
                        em.getTransaction().commit();
                    }

                    sectionIdParam = section.getId().toString();
                }

                case "editAppSection" -> {
                    long sId = Long.parseLong(sectionIdParam);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    String newScope = request.getParameter("scope");
                    em.getTransaction().begin();
                    section.setName(request.getParameter("name").trim());
                    String desc = request.getParameter("description");
                    section.setDescription(desc != null ? desc.trim() : "");
                    section.setScope(newScope != null ? newScope.trim() : section.getScope());
                    em.merge(section);
                    em.getTransaction().commit();

                    emf.getCache().evict(ApplicationSection.class, sId);

                    // If scope changed TO "ALL", additively link missing LOSs and Enhancements
                    if ("ALL".equals(section.getScope())) {
                        List<LOS> allLos = em.createQuery(
                                        "SELECT l FROM LOS l WHERE l.psp.id = :pspId AND l.suppressed = false", LOS.class)
                                .setParameter("pspId", psp.getId().longValue())
                                .getResultList();
                        List<Enhancement> allEnh = em.createQuery(
                                        "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId AND e.suppressed = false", Enhancement.class)
                                .setParameter("pspId", psp.getId().longValue())
                                .getResultList();
                        em.getTransaction().begin();
                        for (LOS l : allLos) {
                            if (!section.getLosList().contains(l)) {
                                section.getLosList().add(l);
                            }
                        }
                        for (Enhancement e : allEnh) {
                            if (!section.getEnhancementList().contains(e)) {
                                section.getEnhancementList().add(e);
                            }
                        }
                        em.merge(section);
                        em.getTransaction().commit();
                    }
                }

                case "suppressAppSection" -> {
                    long sId = Long.parseLong(sectionIdParam);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    em.getTransaction().begin();
                    section.setSuppressed(!section.isSuppressed());
                    em.merge(section);
                    em.getTransaction().commit();
                    emf.getCache().evict(ApplicationSection.class, sId);
                }

                // ── ApplicationField CRUD ────────────────────────────────

                case "createAppField" -> {
                    long sId = Long.parseLong(sectionIdParam);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    ApplicationField field = new ApplicationField();
                    field.setLabel(request.getParameter("label").trim());
                    field.setFieldKey(request.getParameter("fieldKey").trim());
                    field.setFieldType(request.getParameter("fieldType").trim());
                    String opts = request.getParameter("selectOptions");
                    field.setSelectOptions(opts != null ? opts.trim() : "");
                    String help = request.getParameter("helpText");
                    field.setHelpText(help != null ? help.trim() : "");
                    field.setRequired("on".equals(request.getParameter("isRequired")));
                    field.setSuppressed(false);
                    field.setSortOrder(9999);
                    field.setApplicationSection(section);
                    em.getTransaction().begin();
                    em.persist(field);
                    em.getTransaction().commit();
                    emf.getCache().evict(ApplicationSection.class, sId);
                }

                case "editAppField" -> {
                    String fieldKey = request.getParameter("fieldKey");
                    ApplicationField field = em.find(ApplicationField.class, fieldKey);
                    em.getTransaction().begin();
                    field.setLabel(request.getParameter("label").trim());
                    String opts = request.getParameter("selectOptions");
                    field.setSelectOptions(opts != null ? opts.trim() : "");
                    String help = request.getParameter("helpText");
                    field.setHelpText(help != null ? help.trim() : "");
                    field.setRequired("on".equals(request.getParameter("isRequired")));
                    em.merge(field);
                    em.getTransaction().commit();
                    long sId = Long.parseLong(sectionIdParam);
                    emf.getCache().evict(ApplicationSection.class, sId);
                }

                case "suppressAppField" -> {
                    String fieldKey = request.getParameter("fieldKey");
                    ApplicationField field = em.find(ApplicationField.class, fieldKey);
                    em.getTransaction().begin();
                    field.setSuppressed(!field.isSuppressed());
                    em.merge(field);
                    em.getTransaction().commit();
                    long sId = Long.parseLong(sectionIdParam);
                    emf.getCache().evict(ApplicationSection.class, sId);
                }

                // ── Starter Packages ──────────────────────────────────────

                case "loadStarterPackage" -> {
                    String packageId = request.getParameter("packageId");
                    PackageLoader.PackageLoadResult result = PackageLoader.loadPackage(em, packageId, psp);

                    // Evict all ApplicationSection from L2 cache so fields are visible immediately
                    emf.getCache().evict(ApplicationSection.class);

                    if (result.sectionsLoaded() > 0) {
                        request.getSession().setAttribute("flashMessage",
                                result.sectionsLoaded() + " section(s) loaded successfully." +
                                        (result.sectionsSkipped() > 0 ? " " + result.sectionsSkipped() + " already existed and were skipped." : ""));
                    } else {
                        request.getSession().setAttribute("flashMessage",
                                "All sections from this package are already loaded.");
                    }
                }

                case "resetSectionToDefault" -> {
                    long sId = Long.parseLong(sectionIdParam);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    if (section != null && section.getTemplateKey() != null) {
                        int fieldsProcessed = PackageLoader.resetSectionToDefault(em, section);
                        request.getSession().setAttribute("flashMessage",
                                "Section '" + section.getName() + "' reset to default (" + fieldsProcessed + " fields restored).");
                        emf.getCache().evict(ApplicationSection.class, sId);
                    } else {
                        request.getSession().setAttribute("flashMessage",
                                "This section was not loaded from a starter package and cannot be reset.");
                    }
                }

                // ── Feature CRUD (under ServiceModule) ──────────────────

                case "createFeature" -> {
                    long moduleId = Long.parseLong(request.getParameter("moduleId"));
                    ServiceModule module = em.find(ServiceModule.class, moduleId);
                    Feature feature = new Feature();
                    feature.setDescription(request.getParameter("description").trim());
                    String headline = request.getParameter("headline");
                    if (headline != null && !headline.trim().isEmpty()) {
                        feature.setHeadline(headline.trim());
                    }
                    feature.setSortOrder(9999);
                    feature.setServiceModule(module);
                    feature.setPsp(psp);

                    String resIdParam = request.getParameter("libraryResourceId");
                    if (resIdParam != null && !resIdParam.isEmpty()) {
                        MarketingMaterial resource = em.find(MarketingMaterial.class, Long.parseLong(resIdParam));
                        feature.setLibraryResource(resource);
                    }

                    em.getTransaction().begin();
                    em.persist(feature);
                    em.getTransaction().commit();
                }

                case "editFeature" -> {
                    long featureId = Long.parseLong(request.getParameter("featureId"));
                    Feature feature = em.find(Feature.class, featureId);
                    em.getTransaction().begin();
                    feature.setDescription(request.getParameter("description").trim());
                    String headline = request.getParameter("headline");
                    feature.setHeadline(headline != null && !headline.trim().isEmpty() ? headline.trim() : null);

                    // Update library resource link
                    String resIdParam = request.getParameter("libraryResourceId");
                    if (resIdParam != null && !resIdParam.isEmpty()) {
                        MarketingMaterial resource = em.find(MarketingMaterial.class, Long.parseLong(resIdParam));
                        feature.setLibraryResource(resource);
                    } else {
                        feature.setLibraryResource(null);
                    }

                    em.merge(feature);
                    em.getTransaction().commit();
                }

                case "deleteFeature" -> {
                    long featureId = Long.parseLong(request.getParameter("featureId"));
                    Feature feature = em.find(Feature.class, featureId);
                    if (feature != null) {
                        em.getTransaction().begin();
                        em.remove(feature);
                        em.getTransaction().commit();
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("ServiceManagerAction error (" + action + "): " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            if (em.isOpen()) em.close();
        }

        // Refresh global sales data cache after any service manager change
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

        // Redirect back preserving selection and tab
        String redirect = "ServiceManagerHome";
        if ("loadStarterPackage".equals(action)) {
            redirect += "?tab=section";
        } else if (sectionIdParam != null && !sectionIdParam.isEmpty()
                && (action.startsWith("createApp") || action.startsWith("editApp") || action.startsWith("suppressApp") || action.startsWith("resetSection"))) {
            redirect += "?sectionId=" + sectionIdParam + "&tab=section";
        } else if (losIdParam != null && !losIdParam.isEmpty()) {
            redirect += "?losId=" + losIdParam;
        } else if (enhIdParam != null && !enhIdParam.isEmpty()) {
            redirect += "?enhId=" + enhIdParam + "&tab=enhancement";
        }
        response.sendRedirect(redirect);
    }
}

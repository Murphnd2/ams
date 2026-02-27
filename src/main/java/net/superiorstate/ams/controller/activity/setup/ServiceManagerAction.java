package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.*;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ServiceManagerAction", value = "/ServiceManagerAction")
public class ServiceManagerAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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

                case "addLosToEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long losId = Long.parseLong(request.getParameter("losId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    LOS los = EntityLookup.getLosById(em, losId);
                    em.getTransaction().begin();
                    enh.getLosList().add(los);
                    em.merge(enh);
                    em.getTransaction().commit();
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

                case "addEnhancementToLos" -> {
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
                    section.setScope(scope != null ? scope.trim() : "ALL");
                    section.setSortOrder(9999);
                    section.setSuppressed(false);
                    section.setPsp(psp);
                    em.getTransaction().begin();
                    em.persist(section);
                    em.getTransaction().commit();

                    // Auto-link ALL-scoped sections to every active LOS and Enhancement
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
                        section.setLosList(new java.util.ArrayList<>(allLos));
                        section.setEnhancementList(new java.util.ArrayList<>(allEnh));
                        em.merge(section);
                        em.getTransaction().commit();
                    }

                    sectionIdParam = section.getId().toString();
                }

                case "editAppSection" -> {
                    long sId = Long.parseLong(sectionIdParam);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    String oldScope = section.getScope();
                    em.getTransaction().begin();
                    section.setName(request.getParameter("name").trim());
                    String desc = request.getParameter("description");
                    section.setDescription(desc != null ? desc.trim() : "");
                    String scope = request.getParameter("scope");
                    section.setScope(scope != null ? scope.trim() : section.getScope());
                    em.merge(section);
                    em.getTransaction().commit();

                    // If scope changed TO "ALL", auto-link to all active LOSs and Enhancements
                    if ("ALL".equals(section.getScope()) && !"ALL".equals(oldScope)) {
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
                }

                // ── ApplicationField CRUD ───────────────────────────────

                case "createAppField" -> {
                    long sId = Long.parseLong(sectionIdParam);
                    ApplicationSection section = em.find(ApplicationSection.class, sId);
                    String fieldKey = request.getParameter("fieldKey").trim();
                    ApplicationField field = new ApplicationField();
                    field.setFieldKey(fieldKey);
                    field.setLabel(request.getParameter("label").trim());
                    field.setFieldType(request.getParameter("fieldType"));
                    field.setApplicationSection(section);
                    field.setSortOrder(9999);
                    field.setSuppressed(false);
                    String helpText = request.getParameter("helpText");
                    field.setHelpText(helpText != null ? helpText.trim() : "");
                    field.setRequired("on".equals(request.getParameter("isRequired")));
                    String opts = request.getParameter("selectOptions");
                    field.setSelectOptions(opts != null ? opts.trim() : "");
                    em.getTransaction().begin();
                    em.persist(field);
                    em.getTransaction().commit();
                }

                case "editAppField" -> {
                    String fieldKey = request.getParameter("fieldKey");
                    ApplicationField field = em.find(ApplicationField.class, fieldKey);
                    em.getTransaction().begin();
                    field.setLabel(request.getParameter("label").trim());
                    String helpText = request.getParameter("helpText");
                    field.setHelpText(helpText != null ? helpText.trim() : "");
                    field.setRequired("on".equals(request.getParameter("isRequired")));
                    String opts = request.getParameter("selectOptions");
                    field.setSelectOptions(opts != null ? opts.trim() : "");
                    em.merge(field);
                    em.getTransaction().commit();
                }

                case "suppressAppField" -> {
                    String fieldKey = request.getParameter("fieldKey");
                    ApplicationField field = em.find(ApplicationField.class, fieldKey);
                    em.getTransaction().begin();
                    field.setSuppressed(!field.isSuppressed());
                    em.merge(field);
                    em.getTransaction().commit();
                }

                // ── Feature CRUD ────────────────────────────────────────

                case "createFeature" -> {
                    long moduleId = Long.parseLong(request.getParameter("moduleId"));
                    ServiceModule module = em.find(ServiceModule.class, moduleId);
                    Feature feature = new Feature();
                    feature.setDescription(request.getParameter("description").trim());
                    feature.setServiceModule(module);
                    feature.setSortOrder(9999);
                    feature.setPsp(psp);

                    // Optional library resource link
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

        } finally {
            em.close();
        }

        // Redirect back preserving selection and tab
        String redirect = "ServiceManagerHome";
        if (sectionIdParam != null && !sectionIdParam.isEmpty()
                && (action.startsWith("createApp") || action.startsWith("editApp") || action.startsWith("suppressApp"))) {
            redirect += "?sectionId=" + sectionIdParam + "&tab=section";
        } else if (losIdParam != null && !losIdParam.isEmpty()) {
            redirect += "?losId=" + losIdParam;
        } else if (enhIdParam != null && !enhIdParam.isEmpty()) {
            redirect += "?enhId=" + enhIdParam + "&tab=enhancement";
        }
        response.sendRedirect(redirect);
    }
}

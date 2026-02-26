package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.*;

import java.io.IOException;
import java.util.*;

@WebServlet(name = "ServiceManagerHome", value = "/ServiceManagerHome")
public class ServiceManagerHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        int pspId = local.getCurrentPerson().getPsp().getId().intValue();

        try {
            // Load ALL LOS for this PSP (including suppressed, for toggle)
            List<LOS> losList = em.createQuery(
                    "SELECT los FROM LOS los WHERE los.psp.id = :psp_id ORDER BY los.sortOrder", LOS.class)
                    .setParameter("psp_id", (long) pspId)
                    .getResultList();
            request.setAttribute("losList", losList);

            // Load ALL Enhancements for this PSP (including suppressed, for toggle)
            List<Enhancement> enhancementList = getEnhancementList(em, pspId);
            request.setAttribute("enhancementList", enhancementList);

            // Load all ApplicationSections with fields eagerly fetched (for preview + section tab)
            List<ApplicationSection> appSectionList = getAppSectionListWithFields(em, pspId);
            request.setAttribute("appSectionList", appSectionList);

            // Load library resources for feature→resource dropdown
            List<MarketingMaterial> libraryResources = em.createQuery(
                    "SELECT m FROM MarketingMaterial m WHERE m.psp.id = :pspId ORDER BY m.title", MarketingMaterial.class)
                    .setParameter("pspId", (long) pspId)
                    .getResultList();
            request.setAttribute("libraryResources", libraryResources);

            // Determine which tab is active: "los" (default), "enhancement", or "section"
            String tab = request.getParameter("tab");
            if (tab == null)
                tab = "los";
            request.setAttribute("activeTab", tab);

            // If a LOS is selected
            String losIdParam = request.getParameter("losId");
            if (losIdParam != null && !losIdParam.isEmpty()) {
                long losId = Long.parseLong(losIdParam);
                LOS selectedLos = findById(losList, losId);
                if (selectedLos != null) {
                    request.setAttribute("selectedLos", selectedLos);
                    request.setAttribute("activeTab", "los");

                    List<Enhancement> losEnhancements = getEnhancementsForLos(em, losId);
                    request.setAttribute("losEnhancements", losEnhancements);

                    List<ApplicationSection> losAppSections = getAppSectionsForLos(em, losId);
                    request.setAttribute("losAppSections", losAppSections);

                    // Load features via ServiceModule linked to this LOS
                    ServiceModule losModule = findModuleByLos(em, losId);
                    if (losModule != null) {
                        request.setAttribute("selectedModule", losModule);
                        List<Feature> features = getFeaturesForModule(em, losModule.getId());
                        request.setAttribute("featureList", features);
                    }
                }
            }

            // If an Enhancement is selected
            String enhIdParam = request.getParameter("enhId");
            if (enhIdParam != null && !enhIdParam.isEmpty()) {
                long enhId = Long.parseLong(enhIdParam);
                Enhancement selectedEnh = findEnhById(enhancementList, enhId);
                if (selectedEnh != null) {
                    request.setAttribute("selectedEnhancement", selectedEnh);
                    request.setAttribute("activeTab", "enhancement");

                    List<LOS> enhLosItems = getLosForEnhancement(em, enhId);
                    request.setAttribute("enhLosItems", enhLosItems);

                    List<ApplicationSection> enhAppSections = getAppSectionsForEnhancement(em, enhId);
                    request.setAttribute("enhAppSections", enhAppSections);

                    // Load features via ServiceModule linked to this Enhancement
                    ServiceModule enhModule = findModuleByEnhancement(em, enhId);
                    if (enhModule != null) {
                        request.setAttribute("selectedModule", enhModule);
                        List<Feature> features = getFeaturesForModule(em, enhModule.getId());
                        request.setAttribute("featureList", features);
                    }
                }
            }

            // If an ApplicationSection is selected (section tab)
            String sectionIdParam = request.getParameter("sectionId");
            if (sectionIdParam != null && !sectionIdParam.isEmpty()) {
                long sectionId = Long.parseLong(sectionIdParam);
                // Fresh query to reliably load fields (avoids EclipseLink DISTINCT+JOIN FETCH cache issues)
                ApplicationSection selectedSection = getAppSectionWithFields(em, sectionId);
                if (selectedSection != null) {
                    request.setAttribute("selectedSection", selectedSection);
                    request.setAttribute("activeTab", "section");

                    // Load LOS associations for this section
                    List<LOS> sectionLosItems = getLosForSection(em, sectionId);
                    request.setAttribute("sectionLosItems", sectionLosItems);

                    // Load Enhancement associations for this section
                    List<Enhancement> sectionEnhItems = getEnhancementsForSection(em, sectionId);
                    request.setAttribute("sectionEnhItems", sectionEnhItems);
                }
            }

        } finally {
            em.close();
        }

        request.setAttribute("adminCurrentPage", "serviceManager");
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/serviceManager25.jsp");
        dispatcher.forward(request, response);
    }

    // ── Query helpers ──────────────────────────────────────────────────────────

    private List<Enhancement> getEnhancementList(EntityManager em, int pspId) {
        Query q = em.createQuery("SELECT e FROM Enhancement e WHERE e.psp.id = :pspId ORDER BY e.sortOrder");
        q.setParameter("pspId", pspId);
        try {
            return (List<Enhancement>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<ApplicationSection> getAppSectionListWithFields(EntityManager em, int pspId) {
        Query q = em.createQuery(
                "SELECT DISTINCT s FROM ApplicationSection s LEFT JOIN FETCH s.fieldList WHERE s.psp.id = :pspId ORDER BY s.sortOrder");
        q.setParameter("pspId", pspId);
        try {
            List<ApplicationSection> sections = (List<ApplicationSection>) q.getResultList();
            for (ApplicationSection s : sections) {
                if (s.getFieldList() != null) {
                    s.getFieldList().sort(java.util.Comparator.comparingInt(f -> f.getSortOrder()));
                }
            }
            return sections;
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<Enhancement> getEnhancementsForLos(EntityManager em, long losId) {
        Query q = em.createQuery("SELECT e FROM Enhancement e JOIN e.losList l WHERE l.id = :losId ORDER BY e.sortOrder");
        q.setParameter("losId", losId);
        try {
            return (List<Enhancement>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<ApplicationSection> getAppSectionsForLos(EntityManager em, long losId) {
        Query q = em.createQuery(
                "SELECT DISTINCT s FROM ApplicationSection s LEFT JOIN FETCH s.fieldList JOIN s.losList l WHERE l.id = :losId ORDER BY s.sortOrder");
        q.setParameter("losId", losId);
        try {
            List<ApplicationSection> sections = (List<ApplicationSection>) q.getResultList();
            for (ApplicationSection s : sections) {
                if (s.getFieldList() != null) {
                    s.getFieldList().sort(java.util.Comparator.comparingInt(f -> f.getSortOrder()));
                }
            }
            return sections;
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<LOS> getLosForEnhancement(EntityManager em, long enhId) {
        Query q = em.createQuery("SELECT l FROM LOS l JOIN l.enhancementList e WHERE e.id = :enhId ORDER BY l.sortOrder");
        q.setParameter("enhId", enhId);
        try {
            return (List<LOS>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<ApplicationSection> getAppSectionsForEnhancement(EntityManager em, long enhId) {
        Query q = em.createQuery(
                "SELECT DISTINCT s FROM ApplicationSection s LEFT JOIN FETCH s.fieldList JOIN s.enhancementList e WHERE e.id = :enhId ORDER BY s.sortOrder");
        q.setParameter("enhId", enhId);
        try {
            List<ApplicationSection> sections = (List<ApplicationSection>) q.getResultList();
            for (ApplicationSection s : sections) {
                if (s.getFieldList() != null) {
                    s.getFieldList().sort(java.util.Comparator.comparingInt(f -> f.getSortOrder()));
                }
            }
            return sections;
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<LOS> getLosForSection(EntityManager em, long sectionId) {
        Query q = em.createQuery("SELECT l FROM ApplicationSection s JOIN s.losList l WHERE s.id = :sectionId ORDER BY l.sortOrder");
        q.setParameter("sectionId", sectionId);
        try {
            return (List<LOS>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<Enhancement> getEnhancementsForSection(EntityManager em, long sectionId) {
        Query q = em.createQuery("SELECT e FROM ApplicationSection s JOIN s.enhancementList e WHERE s.id = :sectionId ORDER BY e.sortOrder");
        q.setParameter("sectionId", sectionId);
        try {
            return (List<Enhancement>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

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

    private List<Feature> getFeaturesForModule(EntityManager em, long moduleId) {
        return em.createQuery(
                "SELECT f FROM Feature f LEFT JOIN FETCH f.libraryResource WHERE f.serviceModule.id = :moduleId ORDER BY f.sortOrder",
                Feature.class)
                .setParameter("moduleId", moduleId)
                .getResultList();
    }

    // ── Single-section detail query (bypasses DISTINCT+JOIN FETCH issues) ─────

    private ApplicationSection getAppSectionWithFields(EntityManager em, long sectionId) {
        try {
            ApplicationSection section = em.find(ApplicationSection.class, sectionId);
            if (section != null && section.getFieldList() != null) {
                section.getFieldList().size(); // force lazy init
                section.getFieldList().sort(java.util.Comparator.comparingInt(f -> f.getSortOrder()));
            }
            return section;
        } catch (Exception e) {
            return null;
        }
    }

    // ── List search helpers ────────────────────────────────────────────────────

    private LOS findById(List<LOS> list, long id) {
        for (LOS l : list) {
            if (l.getId() == id) return l;
        }
        return null;
    }

    private Enhancement findEnhById(List<Enhancement> list, long id) {
        for (Enhancement e : list) {
            if (e.getId() == id) return e;
        }
        return null;
    }

    private ApplicationSection findSectionById(List<ApplicationSection> list, long id) {
        for (ApplicationSection s : list) {
            if (s.getId() == id) return s;
        }
        return null;
    }
}

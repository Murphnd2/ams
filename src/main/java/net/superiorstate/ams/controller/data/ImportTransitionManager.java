package net.superiorstate.ams.controller.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import net.superiorstate.ams.data.resolver.ImportIdResolver;
import net.superiorstate.ams.model.imports.ImportIdMapping;
import net.superiorstate.ams.model.imports.ImportProvider;
import net.superiorstate.ams.model.summit.archive.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Admin UI for browsing and managing import cross-reference mappings.
 * Supports: browsing by provider/entity, searching AMS records,
 * linking/unlinking external IDs, toggling is_primary, bulk CSV linking,
 * and transferring primary status between providers.
 */
@WebServlet(name = "ImportTransitionManager", value = "/ImportTransitionManager")
@MultipartConfig(maxFileSize = 1024 * 1024 * 5)
public class ImportTransitionManager extends HttpServlet {

    private static final String JSP = "/WEB-INF/view/a/general/importTransition.jsp";

    // ── GET: render page ──────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            loadProviders(em, req);

            String pidStr = req.getParameter("providerId");
            String entityType = req.getParameter("entityType");

            if (pidStr != null && entityType != null && !pidStr.isEmpty() && !entityType.isEmpty()) {
                int pid = Integer.parseInt(pidStr);
                req.setAttribute("selectedProviderId", pid);
                req.setAttribute("selectedEntityType", entityType);
                loadMappings(em, pid, entityType, req);
            }

            // Search
            String q = req.getParameter("q");
            if (q != null && !q.isBlank() && entityType != null) {
                req.setAttribute("searchResults", searchEntities(em, entityType, q.trim()));
                req.setAttribute("searchQuery", q);
            }

            // Flash message from POST actions
            String flash = (String) req.getSession().getAttribute("flash");
            if (flash != null) {
                req.setAttribute("flash", flash);
                req.getSession().removeAttribute("flash");
            }

            req.setAttribute("pageTitle", "Import Transition Manager");
            req.setAttribute("pageIcon", "bi-arrow-left-right");
            req.getRequestDispatcher(JSP).forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            em.close();
        }
    }

    // ── POST: handle actions ──────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        String action = req.getParameter("action");
        if (action == null) { resp.sendRedirect("ImportTransitionManager"); return; }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            String pidStr = req.getParameter("providerId");
            String entityType = req.getParameter("entityType");
            int pid = pidStr != null && !pidStr.isEmpty() ? Integer.parseInt(pidStr) : 0;

            switch (action) {
                case "link"            -> handleLink(em, req, pid, entityType);
                case "unlink"          -> handleUnlink(em, req);
                case "togglePrimary"   -> handleTogglePrimary(em, req);
                case "transferPrimary" -> handleTransferPrimary(em, req, entityType);
                case "bulkLink"        -> handleBulkLink(em, req, pid, entityType);
            }

            // Redirect back with current filters
            String redirect = "ImportTransitionManager";
            if (pid > 0 && entityType != null && !entityType.isEmpty()) {
                redirect += "?providerId=" + pid + "&entityType=" + entityType;
            }
            resp.sendRedirect(redirect);
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    // ── Action Handlers ───────────────────────────────────────────────

    private void handleLink(EntityManager em, HttpServletRequest req, int pid, String entityType) {
        String externalId = req.getParameter("externalId");
        String internalIdStr = req.getParameter("internalId");
        if (externalId == null || internalIdStr == null || entityType == null || pid == 0) return;

        int internalId = Integer.parseInt(internalIdStr);
        boolean isPrimary = "true".equals(req.getParameter("isPrimary"));

        ImportProvider provider = em.find(ImportProvider.class, pid);
        if (provider == null) return;

        em.getTransaction().begin();
        ImportIdResolver.recordMapping(em, provider, entityType, externalId.trim(), internalId, isPrimary);
        em.getTransaction().commit();

        req.getSession().setAttribute("flash",
                "Linked " + entityType + " external " + externalId + " → internal " + internalId);
    }

    private void handleUnlink(EntityManager em, HttpServletRequest req) {
        String mappingIdStr = req.getParameter("mappingId");
        if (mappingIdStr == null) return;

        ImportIdMapping mapping = em.find(ImportIdMapping.class, Integer.parseInt(mappingIdStr));
        if (mapping == null) return;

        em.getTransaction().begin();
        em.remove(mapping);
        em.getTransaction().commit();

        req.getSession().setAttribute("flash", "Mapping #" + mappingIdStr + " removed.");
    }

    private void handleTogglePrimary(EntityManager em, HttpServletRequest req) {
        String mappingIdStr = req.getParameter("mappingId");
        if (mappingIdStr == null) return;

        ImportIdMapping mapping = em.find(ImportIdMapping.class, Integer.parseInt(mappingIdStr));
        if (mapping == null) return;

        em.getTransaction().begin();
        mapping.setPrimary(!mapping.isPrimary());
        em.merge(mapping);
        em.getTransaction().commit();

        req.getSession().setAttribute("flash",
                "Mapping #" + mappingIdStr + " is_primary → " + mapping.isPrimary());
    }

    private void handleTransferPrimary(EntityManager em, HttpServletRequest req, String entityType) {
        String internalIdStr = req.getParameter("internalId");
        String newProviderIdStr = req.getParameter("newProviderId");
        if (internalIdStr == null || newProviderIdStr == null || entityType == null) return;

        int internalId = Integer.parseInt(internalIdStr);
        int newProviderId = Integer.parseInt(newProviderIdStr);

        em.getTransaction().begin();
        ImportIdResolver.transferPrimary(em, entityType, internalId, newProviderId);
        em.getTransaction().commit();

        req.getSession().setAttribute("flash",
                "Primary transferred to provider " + newProviderId + " for " + entityType + " #" + internalId);
    }

    private void handleBulkLink(EntityManager em, HttpServletRequest req, int pid, String entityType)
            throws Exception {
        Part csvPart = req.getPart("bulkFile");
        if (csvPart == null || csvPart.getSize() == 0 || entityType == null || pid == 0) return;

        ImportProvider provider = em.find(ImportProvider.class, pid);
        if (provider == null) return;

        int linked = 0, errors = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvPart.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().contains("external") || line.toLowerCase().contains("internal"))
                        continue; // skip header
                }

                String[] parts = line.split("[,\t]", -1);
                if (parts.length < 2) { errors++; continue; }

                String externalId = parts[0].trim();
                String internalIdStr = parts[1].trim();
                boolean isPrimary = parts.length > 2 && "true".equalsIgnoreCase(parts[2].trim());

                if (externalId.isEmpty() || internalIdStr.isEmpty()) { errors++; continue; }

                try {
                    int internalId = Integer.parseInt(internalIdStr);
                    em.getTransaction().begin();
                    ImportIdResolver.recordMapping(em, provider, entityType, externalId, internalId, isPrimary);
                    em.getTransaction().commit();
                    linked++;
                } catch (Exception e) {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                    errors++;
                }
            }
        }

        req.getSession().setAttribute("flash",
                "Bulk link complete: " + linked + " linked, " + errors + " errors.");
    }

    // ── Data Loading ──────────────────────────────────────────────────

    private void loadProviders(EntityManager em, HttpServletRequest req) {
        List<ImportProvider> providers = em.createQuery(
                "SELECT p FROM ImportProvider p ORDER BY p.providerName", ImportProvider.class)
                .getResultList();
        req.setAttribute("providers", providers);
    }

    private void loadMappings(EntityManager em, int pid, String entityType, HttpServletRequest req) {
        List<ImportIdMapping> mappings = ImportIdResolver.findMappingsByProvider(em, pid, entityType);

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (ImportIdMapping m : mappings) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mapping", m);
            row.put("entityName", resolveEntityName(em, entityType, m.getInternalId()));
            enriched.add(row);
        }
        req.setAttribute("enrichedMappings", enriched);
        req.setAttribute("mappingCount", mappings.size());
    }

    // ── Search ────────────────────────────────────────────────────────

    private List<Map<String, Object>> searchEntities(EntityManager em, String entityType, String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        int maxResults = 25;

        Integer numericId = null;
        try { numericId = Integer.parseInt(query); } catch (NumberFormatException ignored) {}

        switch (entityType) {
            case "PLAN_TYPE" -> {
                if (numericId != null) {
                    PlanType pt = em.find(PlanType.class, numericId);
                    if (pt != null) results.add(entityRow(pt.getPlanTypeId(), pt.getPlanTypeName(),
                            "Code: " + (pt.getCode() != null ? pt.getCode() : "—")));
                }
                if (results.isEmpty()) {
                    em.createQuery("SELECT pt FROM PlanType pt WHERE LOWER(pt.planTypeName) LIKE LOWER(:q) ORDER BY pt.planTypeName",
                                    PlanType.class)
                            .setParameter("q", "%" + query + "%").setMaxResults(maxResults).getResultList()
                            .forEach(pt -> results.add(entityRow(pt.getPlanTypeId(), pt.getPlanTypeName(),
                                    "Code: " + (pt.getCode() != null ? pt.getCode() : "—"))));
                }
            }
            case "EMPLOYER" -> {
                if (numericId != null) {
                    Employer er = em.find(Employer.class, numericId);
                    if (er != null) results.add(entityRow(er.getId(), er.getEmployerName(),
                            "Contact: " + (er.getContactName() != null ? er.getContactName() : "—")));
                }
                if (results.isEmpty()) {
                    em.createQuery("SELECT e FROM Employer e WHERE LOWER(e.employerName) LIKE LOWER(:q) ORDER BY e.employerName",
                                    Employer.class)
                            .setParameter("q", "%" + query + "%").setMaxResults(maxResults).getResultList()
                            .forEach(er -> results.add(entityRow(er.getId(), er.getEmployerName(),
                                    "Contact: " + (er.getContactName() != null ? er.getContactName() : "—"))));
                }
            }
            case "EMPLOYEE" -> {
                if (numericId != null) {
                    Employee ee = em.find(Employee.class, numericId);
                    if (ee != null) results.add(entityRow(ee.getId(),
                            ee.getFirstName() + " " + ee.getLastName(),
                            "Employer: " + (ee.getEmployer() != null ? ee.getEmployer().getEmployerName() : "—")));
                }
                if (results.isEmpty()) {
                    em.createQuery("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(:q) OR LOWER(e.lastName) LIKE LOWER(:q) ORDER BY e.lastName",
                                    Employee.class)
                            .setParameter("q", "%" + query + "%").setMaxResults(maxResults).getResultList()
                            .forEach(ee -> results.add(entityRow(ee.getId(),
                                    ee.getFirstName() + " " + ee.getLastName(),
                                    "Employer: " + (ee.getEmployer() != null ? ee.getEmployer().getEmployerName() : "—"))));
                }
            }
            case "BENEFIT" -> {
                if (numericId != null) {
                    Benefit b = em.find(Benefit.class, numericId);
                    if (b != null) results.add(entityRow(b.getId(), b.getPlanName(),
                            "Source: " + b.getSourceType() + " | Employer: " +
                                    (b.getEmployer() != null ? b.getEmployer().getEmployerName() : "—")));
                }
                if (results.isEmpty()) {
                    em.createQuery("SELECT b FROM Benefit b WHERE LOWER(b.planName) LIKE LOWER(:q) ORDER BY b.planName",
                                    Benefit.class)
                            .setParameter("q", "%" + query + "%").setMaxResults(maxResults).getResultList()
                            .forEach(b -> results.add(entityRow(b.getId(), b.getPlanName(),
                                    "Source: " + b.getSourceType() + " | Employer: " +
                                            (b.getEmployer() != null ? b.getEmployer().getEmployerName() : "—"))));
                }
            }
        }
        return results;
    }

    private Map<String, Object> entityRow(int id, String name, String detail) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("name", name);
        r.put("detail", detail);
        return r;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private String resolveEntityName(EntityManager em, String entityType, int internalId) {
        try {
            return switch (entityType) {
                case "PLAN_TYPE" -> {
                    PlanType pt = em.find(PlanType.class, internalId);
                    yield pt != null ? pt.getPlanTypeName() : "(not found)";
                }
                case "EMPLOYER" -> {
                    Employer er = em.find(Employer.class, internalId);
                    yield er != null ? er.getEmployerName() : "(not found)";
                }
                case "EMPLOYEE" -> {
                    Employee ee = em.find(Employee.class, internalId);
                    yield ee != null ? (ee.getFirstName() + " " + ee.getLastName()) : "(not found)";
                }
                case "BENEFIT" -> {
                    Benefit b = em.find(Benefit.class, internalId);
                    yield b != null ? b.getPlanName() : "(not found)";
                }
                default -> "(unknown)";
            };
        } catch (Exception e) {
            return "(error)";
        }
    }

    private boolean isAdmin(HttpServletRequest req) {
        Boolean isPspAdmin = (Boolean) req.getSession().getAttribute("isPspAdmin");
        if (isPspAdmin != null && isPspAdmin) return true;
        Boolean isBpoAdmin = (Boolean) req.getSession().getAttribute("isBpoAdmin");
        return isBpoAdmin != null && isBpoAdmin;
    }
}

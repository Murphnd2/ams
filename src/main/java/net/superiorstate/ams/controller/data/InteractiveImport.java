package net.superiorstate.ams.controller.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.service.InteractiveImportSession;
import net.superiorstate.ams.data.service.InteractiveImportSession.EntityImportState;
import net.superiorstate.ams.model.imports.ImportFileType;
import net.superiorstate.ams.model.imports.ImportProvider;

import java.io.IOException;
import java.util.List;

/**
 * Interactive Import Wizard — entity-by-entity import with cross-reference resolution.
 *
 * Flow: Select Provider → for each entity type (PlanType → Employer → Benefit → Employee):
 *   Upload file → auto-resolve cross-references → user confirms/matches/creates → commit → next entity
 * → Results summary.
 *
 * Security: PSP Admin (role 5) or BPO Admin (role 102) only.
 */
@WebServlet(name = "InteractiveImport", value = "/InteractiveImport")
@MultipartConfig(
        maxFileSize = 1024 * 1024 * 20,    // 20 MB per file
        maxRequestSize = 1024 * 1024 * 50   // 50 MB total
)
public class InteractiveImport extends HttpServlet {

    private static final String SESSION_KEY = "ii_session";

    // ═══════════════════════════════════════════════════════════════
    //  GET — render the current step
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        String step = request.getParameter("step");
        if (step == null) step = "1";

        InteractiveImportSession session = getSession(request);

        switch (step) {
            case "1" -> {
                // Provider selection
                EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
                EntityManager em = emf.createEntityManager();
                try {
                    long pspId = getPspId(request);
                    List<ImportProvider> providers = em.createQuery(
                                    "SELECT p FROM ImportProvider p WHERE p.isActive = true AND p.pspId = :pid ORDER BY p.providerName",
                                    ImportProvider.class)
                            .setParameter("pid", pspId)
                            .getResultList();
                    request.setAttribute("providers", providers);
                } finally {
                    em.close();
                }
                forwardTo(request, response, "selectProvider");
            }
            case "entity" -> {
                // Entity step — upload + resolution page
                if (session == null || "SELECT_PROVIDER".equals(session.getCurrentEntityStep())) {
                    response.sendRedirect("InteractiveImport");
                    return;
                }
                if ("RESULTS".equals(session.getCurrentEntityStep())) {
                    response.sendRedirect("InteractiveImport?step=results");
                    return;
                }
                String entityType = session.getCurrentEntityStep();
                EntityImportState state = session.getEntityStates().get(entityType);

                request.setAttribute("iiSession", session);
                request.setAttribute("entityState", state);
                request.setAttribute("entityType", entityType);

                // Load the file type for this entity (for display: label, mode, status)
                EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
                EntityManager em = emf.createEntityManager();
                try {
                    if (state != null && state.getFileTypeId() > 0) {
                        ImportFileType ft = em.find(ImportFileType.class, state.getFileTypeId());
                        request.setAttribute("fileType", ft);
                    }
                } finally {
                    em.close();
                }
                forwardTo(request, response, "entityStep");
            }
            case "results" -> {
                // Results summary
                if (session == null) {
                    response.sendRedirect("InteractiveImport");
                    return;
                }
                request.setAttribute("iiSession", session);
                forwardTo(request, response, "results");
            }
            default -> response.sendRedirect("InteractiveImport");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  POST — handle actions
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "";

        switch (action) {
            case "selectProvider" -> handleSelectProvider(request, response);
            case "skipEntity" -> handleSkipEntity(request, response);
            case "reset" -> handleReset(request, response);
            default -> response.sendRedirect("InteractiveImport");
        }
    }

    // ── selectProvider ────────────────────────────────────────────

    private void handleSelectProvider(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String providerIdStr = request.getParameter("providerId");
        if (providerIdStr == null || providerIdStr.isBlank()) {
            response.sendRedirect("InteractiveImport");
            return;
        }

        int providerId = Integer.parseInt(providerIdStr);

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            ImportProvider provider = em.find(ImportProvider.class, providerId);
            if (provider == null) {
                response.sendRedirect("InteractiveImport");
                return;
            }

            // Load file types for this provider (only READY ones, or UPDATE_ONLY with PK)
            List<ImportFileType> fileTypes = em.createQuery(
                            "SELECT ft FROM ImportFileType ft WHERE ft.provider.id = :pid ORDER BY ft.sortOrder",
                            ImportFileType.class)
                    .setParameter("pid", providerId)
                    .getResultList();

            // Initialize session
            InteractiveImportSession iiSession = new InteractiveImportSession();
            iiSession.setProviderId(providerId);
            iiSession.setProviderName(provider.getProviderName());

            // Create EntityImportState for each entity type that has at least one file type defined
            for (ImportFileType ft : fileTypes) {
                String entityType = ft.getTargetEntity();
                if (!iiSession.getEntityStates().containsKey(entityType)) {
                    EntityImportState state = new EntityImportState();
                    state.setEntityType(entityType);
                    state.setFileTypeId(ft.getId());
                    state.setUpdateMode(ft.getUpdateMode());
                    iiSession.getEntityStates().put(entityType, state);
                }
            }

            // Set first entity step
            List<String> available = iiSession.getAvailableEntityTypes();
            if (available.isEmpty()) {
                response.sendRedirect("InteractiveImport");
                return;
            }
            iiSession.setCurrentEntityStep(available.get(0));

            // Store in HTTP session
            request.getSession().setAttribute(SESSION_KEY, iiSession);

        } finally {
            em.close();
        }

        response.sendRedirect("InteractiveImport?step=entity");
    }

    // ── skipEntity ────────────────────────────────────────────────

    private void handleSkipEntity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        InteractiveImportSession iiSession = getSession(request);
        if (iiSession == null) {
            response.sendRedirect("InteractiveImport");
            return;
        }

        String entityType = iiSession.getCurrentEntityStep();
        EntityImportState state = iiSession.getEntityStates().get(entityType);
        if (state != null) {
            state.setSkipped(true);
        }
        iiSession.getCompletedEntities().add(entityType);
        iiSession.advanceToNextEntity();

        if ("RESULTS".equals(iiSession.getCurrentEntityStep())) {
            response.sendRedirect("InteractiveImport?step=results");
        } else {
            response.sendRedirect("InteractiveImport?step=entity");
        }
    }

    // ── reset ─────────────────────────────────────────────────────

    private void handleReset(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.getSession().removeAttribute(SESSION_KEY);
        response.sendRedirect("InteractiveImport");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private InteractiveImportSession getSession(HttpServletRequest request) {
        return (InteractiveImportSession) request.getSession().getAttribute(SESSION_KEY);
    }

    private boolean isAdmin(HttpServletRequest request) {
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        if (isPspAdmin != null && isPspAdmin) return true;
        Boolean isBpoAdmin = (Boolean) request.getSession().getAttribute("isBpoAdmin");
        return isBpoAdmin != null && isBpoAdmin;
    }

    private long getPspId(HttpServletRequest request) {
        Object pspId = request.getSession().getAttribute("pspId");
        if (pspId instanceof Long) return (Long) pspId;
        if (pspId instanceof Number) return ((Number) pspId).longValue();
        return 4L;
    }

    private void forwardTo(HttpServletRequest request, HttpServletResponse response, String page)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/a/general/interactiveImport/" + page + ".jsp")
                .forward(request, response);
    }
}

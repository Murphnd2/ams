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
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.service.ImportCommitService;
import net.superiorstate.ams.data.service.ImportResolutionService;
import net.superiorstate.ams.data.service.SummitProviderSeeder;
import net.superiorstate.ams.data.service.InteractiveImportSession;
import net.superiorstate.ams.data.service.InteractiveImportSession.EntityImportState;
import net.superiorstate.ams.data.service.InteractiveImportSession.ImportRow;
import net.superiorstate.ams.data.service.InteractiveImportSession.MatchCandidate;
import net.superiorstate.ams.data.service.UniversalImportService;
import net.superiorstate.ams.data.service.UniversalImportService.FieldMappingInfo;
import net.superiorstate.ams.data.service.UniversalImportService.ImportResult;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.model.imports.ImportFileType;
import net.superiorstate.ams.model.imports.ImportProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

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

        request.setAttribute("pageTitle", "Interactive Import");
        request.setAttribute("pageIcon", "bi-diagram-3");

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

                    // Auto-ensure Summit provider exists for this PSP (idempotent)
                    SummitProviderSeeder.seed(em, pspId);

                    List<ImportProvider> providers = em.createQuery(
                                    "SELECT p FROM ImportProvider p WHERE p.active = true AND p.pspId = :pid ORDER BY p.providerName",
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
            case "ajax" -> {
                // AJAX search endpoint
                if (session == null) {
                    sendJson(response, "{\"error\":\"no session\"}");
                    return;
                }
                handleAjaxGet(request, response, session);
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
            case "uploadEntity" -> handleUploadEntity(request, response);
            case "resolveRow" -> handleResolveRow(request, response);
            case "commitEntity" -> handleCommitEntity(request, response);
            case "skipEntity" -> handleSkipEntity(request, response);
            case "resetUpload" -> handleResetUpload(request, response);
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

            // Summit provider uses its own dedicated wizard
            if ("SUMMIT".equalsIgnoreCase(provider.getProviderCode())) {
                response.sendRedirect("SummitImport");
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

    // ── uploadEntity ────────────────────────────────────────────

    private void handleUploadEntity(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        InteractiveImportSession iiSession = getSession(request);
        if (iiSession == null) {
            response.sendRedirect("InteractiveImport");
            return;
        }

        String entityType = iiSession.getCurrentEntityStep();
        EntityImportState state = iiSession.getEntityStates().get(entityType);
        if (state == null) {
            response.sendRedirect("InteractiveImport?step=entity");
            return;
        }

        // Extract uploaded file
        Part filePart = request.getPart("entityFile");
        if (filePart == null || filePart.getSize() == 0) {
            response.sendRedirect("InteractiveImport?step=entity");
            return;
        }

        String fileName = extractFileName(filePart);

        // Save to temp directory — use servlet context temp dir (guaranteed writable by container)
        File servletTmp = (File) getServletContext().getAttribute("jakarta.servlet.context.tempdir");
        Path tempBase = servletTmp != null ? servletTmp.toPath() : Path.of(System.getProperty("user.home"), "tmp");
        Files.createDirectories(tempBase);
        Path tempDir = Files.createTempDirectory(tempBase, "ii_" + entityType.toLowerCase() + "_");
        Path tempFile = tempDir.resolve(fileName);
        try (InputStream is = filePart.getInputStream()) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        state.setFilePath(tempFile.toString());
        state.setFileName(fileName);

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Load provider
            ImportProvider provider = em.find(ImportProvider.class, iiSession.getProviderId());
            if (provider == null) {
                response.sendRedirect("InteractiveImport");
                return;
            }

            // Load file type for format info
            ImportFileType fileType = em.find(ImportFileType.class, state.getFileTypeId());
            String format = fileType != null ? fileType.getFileFormat() : detectFormat(fileName);

            // Parse file
            File file = tempFile.toFile();
            List<Map<String, String>> parsedRows = UniversalImportService.parseFile(file, format);
            state.setTotalRows(parsedRows.size());

            // Load field mappings
            Map<String, FieldMappingInfo> mappings = UniversalImportService.loadFieldMappings(
                    em, state.getFileTypeId());

            // Load FK mappings
            Map<String, String> fkMappings = ImportResolutionService.loadFkMappings(
                    em, state.getFileTypeId());

            // Resolve rows
            List<ImportRow> rows = ImportResolutionService.resolveRows(
                    em, provider, parsedRows, mappings, entityType,
                    state.getUpdateMode(), fkMappings);

            state.setRows(rows);
            state.recalculateCounts();

        } catch (Exception e) {
            System.out.println("InteractiveImport: Upload error for " + entityType + ": " + e.getMessage());
            e.printStackTrace();
            // Clear the file path so the upload form shows again
            state.setFilePath(null);
            state.setFileName(null);
        } finally {
            em.close();
        }

        response.sendRedirect("InteractiveImport?step=entity");
    }

    private static String extractFileName(Part part) {
        String header = part.getHeader("content-disposition");
        if (header != null) {
            for (String token : header.split(";")) {
                if (token.trim().startsWith("filename")) {
                    String name = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                    // Handle full path from IE/Edge
                    int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                    if (lastSlash >= 0) name = name.substring(lastSlash + 1);
                    return name;
                }
            }
        }
        return "upload_" + System.currentTimeMillis();
    }

    private static String detectFormat(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "EXCEL";
        if (lower.endsWith(".tsv")) return "TSV";
        return "CSV";
    }

    // ── commitEntity ──────────────────────────────────────────────

    private void handleCommitEntity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        InteractiveImportSession iiSession = getSession(request);
        if (iiSession == null) {
            response.sendRedirect("InteractiveImport");
            return;
        }

        String entityType = iiSession.getCurrentEntityStep();
        EntityImportState state = iiSession.getEntityStates().get(entityType);
        if (state == null || state.getRows().isEmpty()) {
            response.sendRedirect("InteractiveImport?step=entity");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            ImportProvider provider = em.find(ImportProvider.class, iiSession.getProviderId());
            if (provider == null) {
                response.sendRedirect("InteractiveImport");
                return;
            }

            ImportResult result = ImportCommitService.commitEntity(
                    em, provider, state, iiSession.getRenewalMonthsMap());

            state.setResult(result);
            state.setResolutionComplete(true);

            // Refresh employee cache so Create Ticket modal picks up new/updated employees
            if ("EMPLOYEE".equals(entityType)) {
                AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
                if (global != null) global.resetEmployeeCache();
            }
        } catch (Exception e) {
            System.out.println("InteractiveImport: Commit error for " + entityType + ": " + e.getMessage());
            e.printStackTrace();
            ImportResult errorResult = new ImportResult();
            errorResult.addError("Commit failed: " + e.getMessage());
            state.setResult(errorResult);
        } finally {
            em.close();
        }

        // Mark entity complete and advance
        iiSession.getCompletedEntities().add(entityType);
        iiSession.advanceToNextEntity();

        if ("RESULTS".equals(iiSession.getCurrentEntityStep())) {
            response.sendRedirect("InteractiveImport?step=results");
        } else {
            response.sendRedirect("InteractiveImport?step=entity");
        }
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

    // ── resetUpload ─────────────────────────────────────────────────

    private void handleResetUpload(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        InteractiveImportSession iiSession = getSession(request);
        if (iiSession == null) {
            response.sendRedirect("InteractiveImport");
            return;
        }

        String entityType = iiSession.getCurrentEntityStep();
        EntityImportState state = iiSession.getEntityStates().get(entityType);
        if (state != null) {
            state.setFilePath(null);
            state.setFileName(null);
            state.setRows(new java.util.ArrayList<>());
            state.setTotalRows(0);
            state.recalculateCounts();
        }

        response.sendRedirect("InteractiveImport?step=entity");
    }

    // ── reset ─────────────────────────────────────────────────────

    private void handleReset(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.getSession().removeAttribute(SESSION_KEY);
        response.sendRedirect("InteractiveImport");
    }

    // ═══════════════════════════════════════════════════════════════
    //  AJAX — search AMS records
    // ═══════════════════════════════════════════════════════════════

    private void handleAjaxGet(HttpServletRequest request, HttpServletResponse response,
                                InteractiveImportSession iiSession) throws IOException {
        String ajaxAction = request.getParameter("action");
        if (!"searchAms".equals(ajaxAction)) {
            sendJson(response, "{\"error\":\"unknown ajax action\"}");
            return;
        }

        String entityType = iiSession.getCurrentEntityStep();
        String query = request.getParameter("query");
        if (query == null || query.isBlank()) {
            sendJson(response, "[]");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<MatchCandidate> results = ImportResolutionService.searchAmsRecords(
                    em, entityType, query, 10);
            sendJson(response, candidatesToJson(results));
        } finally {
            em.close();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  AJAX — resolve a single row (confirm, manual link, new, skip)
    // ═══════════════════════════════════════════════════════════════

    private void handleResolveRow(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        InteractiveImportSession iiSession = getSession(request);
        if (iiSession == null) {
            sendJson(response, "{\"error\":\"no session\"}");
            return;
        }

        String entityType = iiSession.getCurrentEntityStep();
        EntityImportState state = iiSession.getEntityStates().get(entityType);
        if (state == null) {
            sendJson(response, "{\"error\":\"no entity state\"}");
            return;
        }

        int rowIndex;
        try {
            rowIndex = Integer.parseInt(request.getParameter("rowIndex"));
        } catch (Exception e) {
            sendJson(response, "{\"error\":\"invalid rowIndex\"}");
            return;
        }

        // Find the row
        ImportRow row = null;
        for (ImportRow r : state.getRows()) {
            if (r.getRowIndex() == rowIndex) {
                row = r;
                break;
            }
        }
        if (row == null) {
            sendJson(response, "{\"error\":\"row not found\"}");
            return;
        }

        String resolution = request.getParameter("resolution");
        if (resolution == null) resolution = "";

        switch (resolution) {
            case "confirm" -> {
                // Confirm the current suggestion
                if (row.getAmsInternalId() != null) {
                    row.setStatus("CONFIRMED");
                    row.setMatchMethod("confirmed");
                }
            }
            case "manual" -> {
                // Manual link to a specific AMS record
                String internalIdStr = request.getParameter("internalId");
                String label = request.getParameter("label");
                if (internalIdStr != null && !internalIdStr.isBlank()) {
                    row.setStatus("MANUAL");
                    row.setAmsInternalId(Integer.parseInt(internalIdStr));
                    row.setAmsDisplayLabel(label != null ? label : "ID: " + internalIdStr);
                    row.setMatchMethod("manual");
                    row.setMatchConfidence(1.0);
                }
            }
            case "new" -> {
                // Mark as new / create on commit
                row.setStatus("UNMATCHED");
                row.setAmsInternalId(null);
                row.setAmsDisplayLabel(null);
                row.setMatchMethod(null);
                row.setMatchConfidence(0);
                row.setCandidates(null);
            }
            case "skip" -> {
                row.setStatus("SKIPPED");
            }
            default -> {
                sendJson(response, "{\"error\":\"unknown resolution: " + resolution + "\"}");
                return;
            }
        }

        // Recalculate counts
        state.recalculateCounts();

        // Return updated row + counts as JSON
        sendJson(response, rowToJson(row, state));
    }

    // ── JSON helpers ──────────────────────────────────────────────

    private static String candidatesToJson(List<MatchCandidate> candidates) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) sb.append(",");
            MatchCandidate c = candidates.get(i);
            sb.append("{\"id\":").append(c.getInternalId())
              .append(",\"label\":\"").append(escJson(c.getDisplayLabel())).append("\"")
              .append(",\"method\":\"").append(escJson(c.getMatchMethod())).append("\"")
              .append(",\"confidence\":").append(String.format("%.2f", c.getConfidence()))
              .append(",\"detail\":\"").append(escJson(c.getDetail())).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String rowToJson(ImportRow row, EntityImportState state) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"rowIndex\":").append(row.getRowIndex());
        sb.append(",\"status\":\"").append(row.getStatus()).append("\"");
        sb.append(",\"amsInternalId\":").append(row.getAmsInternalId() != null ? row.getAmsInternalId() : "null");
        sb.append(",\"amsDisplayLabel\":\"").append(escJson(row.getAmsDisplayLabel())).append("\"");
        sb.append(",\"matchMethod\":\"").append(escJson(row.getMatchMethod())).append("\"");
        sb.append(",\"matchedCount\":").append(state.getMatchedCount());
        sb.append(",\"suggestedCount\":").append(state.getSuggestedCount());
        sb.append(",\"unmatchedCount\":").append(state.getUnmatchedCount());
        sb.append(",\"errorCount\":").append(state.getErrorCount());
        sb.append("}");
        return sb.toString();
    }

    private static String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void sendJson(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
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

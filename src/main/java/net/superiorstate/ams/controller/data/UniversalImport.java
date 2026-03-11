package net.superiorstate.ams.controller.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.service.UniversalImportService;
import net.superiorstate.ams.data.service.UniversalImportService.ImportResult;
import net.superiorstate.ams.model.imports.ImportFileType;
import net.superiorstate.ams.model.imports.ImportProvider;
import net.superiorstate.ams.model.imports.ImportRunLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Multi-step wizard for provider-agnostic data import.
 *
 * Step 1 (SELECT):    Choose import provider
 * Step 2 (UPLOAD):    Upload files defined by the provider's file types
 * Step 3 (CONFIGURE): Review uploads, set renewal months
 * Step 4 (RESULTS):   Display import results
 *
 * Security: PSP Admin (role 5) or BPO Admin (role 102).
 */
@WebServlet(name = "UniversalImport", value = "/UniversalImport")
@MultipartConfig(
        maxFileSize = 1024 * 1024 * 20,    // 20 MB per file
        maxRequestSize = 1024 * 1024 * 50   // 50 MB total
)
public class UniversalImport extends HttpServlet {

    // Session attribute keys
    private static final String UI_PROVIDER_ID = "ui_providerId";
    private static final String UI_TEMP_DIR = "ui_tempDir";
    private static final String UI_UPLOADED_FILES = "ui_uploadedFiles";   // Map<Integer, String> fileTypeId → file path
    private static final String UI_UPLOAD_SUMMARY = "ui_uploadSummary";   // Map<String, Map<String,Object>>
    private static final String UI_RESULTS = "ui_results";                // Map<String, ImportResult>

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

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long pspId = getPspId(request);

            switch (step) {
                case "2" -> {
                    // Load file types for the selected provider
                    Integer providerId = (Integer) request.getSession().getAttribute(UI_PROVIDER_ID);
                    if (providerId == null) {
                        response.sendRedirect("UniversalImport");
                        return;
                    }
                    ImportProvider provider = em.find(ImportProvider.class, providerId);
                    List<ImportFileType> fileTypes = em.createQuery(
                                    "SELECT ft FROM ImportFileType ft WHERE ft.provider.id = :pid ORDER BY ft.sortOrder",
                                    ImportFileType.class)
                            .setParameter("pid", providerId)
                            .getResultList();
                    request.setAttribute("provider", provider);
                    request.setAttribute("fileTypes", fileTypes);
                    forwardTo(request, response, "step2Upload");
                }
                case "3" -> forwardTo(request, response, "step3Configure");
                case "4" -> forwardTo(request, response, "step4Results");
                default -> {
                    // Step 1: show provider selection
                    List<ImportProvider> providers = em.createQuery(
                                    "SELECT p FROM ImportProvider p WHERE p.pspId = :pspId AND p.active = true ORDER BY p.providerName",
                                    ImportProvider.class)
                            .setParameter("pspId", pspId)
                            .getResultList();
                    request.setAttribute("providers", providers);
                    forwardTo(request, response, "step1Provider");
                }
            }
        } finally {
            em.close();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  POST — handle form submissions
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "selectProvider";

        switch (action) {
            case "selectProvider" -> handleSelectProvider(request, response);
            case "upload" -> handleUpload(request, response);
            case "import" -> handleImport(request, response);
            case "reset" -> handleReset(request, response);
            default -> response.sendRedirect("UniversalImport");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 1 → 2: Select provider
    // ═══════════════════════════════════════════════════════════════

    private void handleSelectProvider(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String providerIdStr = request.getParameter("providerId");
        if (providerIdStr == null || providerIdStr.isBlank()) {
            response.sendRedirect("UniversalImport");
            return;
        }

        // Summit has a dedicated import wizard — redirect there
        if ("SUMMIT_REDIRECT".equals(providerIdStr)) {
            response.sendRedirect("SummitImport");
            return;
        }

        clearSessionAttributes(request);
        request.getSession().setAttribute(UI_PROVIDER_ID, Integer.parseInt(providerIdStr));
        response.sendRedirect("UniversalImport?step=2");
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 2 → 3: Upload files, validate, save to temp dir
    // ═══════════════════════════════════════════════════════════════

    private void handleUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer providerId = (Integer) request.getSession().getAttribute(UI_PROVIDER_ID);
        if (providerId == null) {
            response.sendRedirect("UniversalImport");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            ImportProvider provider = em.find(ImportProvider.class, providerId);
            List<ImportFileType> fileTypes = em.createQuery(
                            "SELECT ft FROM ImportFileType ft WHERE ft.provider.id = :pid ORDER BY ft.sortOrder",
                            ImportFileType.class)
                    .setParameter("pid", providerId)
                    .getResultList();

            // Create temp directory
            Path baseDir = Path.of(AppConstantDAO.getSavePath(), "universal_import");
            Files.createDirectories(baseDir);
            Path tempDir = Files.createTempDirectory(baseDir, "session_");
            request.getSession().setAttribute(UI_TEMP_DIR, tempDir.toString());

            Map<Integer, String> uploadedFiles = new LinkedHashMap<>();
            Map<String, Map<String, Object>> uploadSummary = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();

            // Process each file type's upload
            for (ImportFileType ft : fileTypes) {
                String partName = "file_" + ft.getId();
                Part part = request.getPart(partName);
                if (part == null || part.getSize() == 0) {
                    if (ft.isRequired()) {
                        errors.add(ft.getFileLabel() + " is required but was not uploaded.");
                    }
                    continue;
                }

                String fileName = getSubmittedFileName(part);
                if (fileName == null || fileName.isBlank()) continue;

                // Save to temp
                Path saved = tempDir.resolve(fileName);
                try (InputStream is = part.getInputStream()) {
                    Files.copy(is, saved, StandardCopyOption.REPLACE_EXISTING);
                }
                uploadedFiles.put(ft.getId(), saved.toString());

                // Read headers and row count for summary
                try {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("fileName", fileName);
                    info.put("fileLabel", ft.getFileLabel());
                    info.put("targetEntity", ft.getTargetEntity());

                    List<String> headers = UniversalImportService.getFileHeaders(saved.toFile(), ft.getFileFormat());
                    int rowCount;
                    if ("EXCEL".equals(ft.getFileFormat())) {
                        rowCount = net.superiorstate.ams.data.service.SummitImportService.countExcelRows(saved.toFile());
                    } else {
                        rowCount = net.superiorstate.ams.data.service.SummitImportService.countCsvRows(saved.toFile());
                    }
                    info.put("headers", headers);
                    info.put("rowCount", rowCount);
                    uploadSummary.put(ft.getFileLabel(), info);
                } catch (Exception e) {
                    errors.add(ft.getFileLabel() + ": Error reading file — " + e.getMessage());
                }
            }

            if (uploadedFiles.isEmpty() && errors.isEmpty()) {
                errors.add("No files were uploaded. Please select at least one file.");
            }

            request.getSession().setAttribute(UI_UPLOADED_FILES, uploadedFiles);
            request.getSession().setAttribute(UI_UPLOAD_SUMMARY, uploadSummary);
            request.setAttribute("provider", provider);
            request.setAttribute("uploadSummary", uploadSummary);
            request.setAttribute("uploadErrors", errors);
            forwardTo(request, response, "step3Configure");

        } finally {
            em.close();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 3 → 4: Execute imports
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void handleImport(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Integer providerId = (Integer) request.getSession().getAttribute(UI_PROVIDER_ID);
        Map<Integer, String> uploadedFiles = (Map<Integer, String>) request.getSession().getAttribute(UI_UPLOADED_FILES);

        if (providerId == null || uploadedFiles == null || uploadedFiles.isEmpty()) {
            response.sendRedirect("UniversalImport");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Map<String, ImportResult> results = new LinkedHashMap<>();
        ImportRunLog runLog = null;

        try {
            ImportProvider provider = em.find(ImportProvider.class, providerId);
            Map<Integer, Integer> renewalMonthsMap = parseRenewalMonthsConfig(request);

            // Get person ID for run log
            long personId = getPersonId(request);

            // Create run log
            em.getTransaction().begin();
            runLog = new ImportRunLog();
            runLog.setProvider(provider);
            runLog.setRunBy(personId);
            em.persist(runLog);
            em.getTransaction().commit();

            // Group uploaded files by target entity
            List<ImportFileType> allFileTypes = em.createQuery(
                            "SELECT ft FROM ImportFileType ft WHERE ft.provider.id = :pid ORDER BY ft.sortOrder",
                            ImportFileType.class)
                    .setParameter("pid", providerId)
                    .getResultList();

            Map<String, List<File>> filesByEntity = new LinkedHashMap<>();
            for (ImportFileType ft : allFileTypes) {
                String filePath = uploadedFiles.get(ft.getId());
                if (filePath != null) {
                    filesByEntity.computeIfAbsent(ft.getTargetEntity(), k -> new ArrayList<>())
                            .add(new File(filePath));
                }
            }

            // Execute imports in order: Plan Types → Employers → Employees → Benefits
            if (filesByEntity.containsKey("PLAN_TYPE")) {
                ImportResult ptResult = UniversalImportService.importPlanTypes(
                        em, filesByEntity.get("PLAN_TYPE"), provider, renewalMonthsMap);
                results.put("Plan Types", ptResult);
            }

            if (filesByEntity.containsKey("EMPLOYER")) {
                ImportResult erResult = UniversalImportService.importEmployers(
                        em, filesByEntity.get("EMPLOYER"), provider);
                results.put("Employers", erResult);
            }

            if (filesByEntity.containsKey("EMPLOYEE")) {
                ImportResult eeResult = UniversalImportService.importEmployees(
                        em, filesByEntity.get("EMPLOYEE"), provider);
                results.put("Employees", eeResult);
            }

            if (filesByEntity.containsKey("BENEFIT")) {
                ImportResult bResult = UniversalImportService.importBenefits(
                        em, filesByEntity.get("BENEFIT"), provider, renewalMonthsMap);
                results.put("Benefits", bResult);
            }

            // Update run log with results
            em.getTransaction().begin();
            runLog = em.find(ImportRunLog.class, runLog.getId());
            populateRunLog(runLog, results);
            runLog.setStatus("COMPLETED");
            runLog.setCompletedOn(Timestamp.from(Instant.now()));
            em.merge(runLog);
            em.getTransaction().commit();

            // Reload global state
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            if (global != null) {
                global.initializeGlobalData(em);
                if (filesByEntity.containsKey("EMPLOYEE")) {
                    global.resetEmployeeCache();
                }
            }

        } catch (Exception e) {
            ImportResult errorResult = new ImportResult();
            errorResult.addError("Import failed: " + e.getMessage());
            results.put("ERROR", errorResult);
            e.printStackTrace();

            // Update run log as failed
            if (runLog != null) {
                try {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                    em.getTransaction().begin();
                    runLog = em.find(ImportRunLog.class, runLog.getId());
                    runLog.setStatus("FAILED");
                    runLog.setErrors(e.getMessage());
                    runLog.setCompletedOn(Timestamp.from(Instant.now()));
                    em.merge(runLog);
                    em.getTransaction().commit();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (em.isOpen()) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                em.close();
            }
        }

        request.getSession().setAttribute(UI_RESULTS, results);
        System.out.println("[UniversalImport] Import complete. Results: " + results.size() + " sections.");
        for (var e : results.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue().summary());
        }

        // Clean up temp files
        cleanupTempDir(request);

        response.sendRedirect("UniversalImport?step=4");
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESET — clear session state and start over
    // ═══════════════════════════════════════════════════════════════

    private void handleReset(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        cleanupTempDir(request);
        clearSessionAttributes(request);
        response.sendRedirect("UniversalImport");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

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

    private long getPersonId(HttpServletRequest request) {
        Object personId = request.getSession().getAttribute("personId");
        if (personId instanceof Long) return (Long) personId;
        if (personId instanceof Number) return ((Number) personId).longValue();
        return 104L;
    }

    private void forwardTo(HttpServletRequest request, HttpServletResponse response, String page)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/a/general/universalImport/" + page + ".jsp")
                .forward(request, response);
    }

    private String getSubmittedFileName(Part part) {
        String s = part.getSubmittedFileName();
        if (s != null && !s.isBlank()) {
            return java.nio.file.Paths.get(s).getFileName().toString();
        }
        return null;
    }

    private Map<Integer, Integer> parseRenewalMonthsConfig(HttpServletRequest request) {
        Map<Integer, Integer> map = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String name = params.nextElement();
            if (name.startsWith("renewalMonths_")) {
                try {
                    int ptId = Integer.parseInt(name.substring("renewalMonths_".length()));
                    int months = Integer.parseInt(request.getParameter(name));
                    if (months > 0) map.put(ptId, months);
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    private void populateRunLog(ImportRunLog log, Map<String, ImportResult> results) {
        ImportResult pt = results.get("Plan Types");
        if (pt != null) {
            log.setPlanTypesInserted(pt.getInserted());
            log.setPlanTypesUpdated(pt.getUpdated());
            log.setPlanTypesSkipped(pt.getSkipped());
            log.setServiceItemsCreated(pt.getServiceItemsCreated());
        }
        ImportResult er = results.get("Employers");
        if (er != null) {
            log.setEmployersInserted(er.getInserted());
            log.setEmployersUpdated(er.getUpdated());
            log.setEmployersSkipped(er.getSkipped());
        }
        ImportResult ee = results.get("Employees");
        if (ee != null) {
            log.setEmployeesInserted(ee.getInserted());
            log.setEmployeesUpdated(ee.getUpdated());
            log.setEmployeesSkipped(ee.getSkipped());
        }
        ImportResult bn = results.get("Benefits");
        if (bn != null) {
            log.setBenefitsInserted(bn.getInserted());
            log.setBenefitsUpdated(bn.getUpdated());
            log.setBenefitsSkipped(bn.getSkipped());
        }

        // Aggregate warnings and errors
        List<String> allWarnings = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();
        for (var entry : results.entrySet()) {
            for (String w : entry.getValue().getWarnings()) {
                allWarnings.add(entry.getKey() + ": " + w);
            }
        }
        if (!allWarnings.isEmpty()) log.setWarnings(String.join("\n", allWarnings));
        if (!allErrors.isEmpty()) log.setErrors(String.join("\n", allErrors));
    }

    private void cleanupTempDir(HttpServletRequest request) {
        String tempDir = (String) request.getSession().getAttribute(UI_TEMP_DIR);
        if (tempDir != null) {
            try {
                Path dir = Path.of(tempDir);
                if (Files.exists(dir)) {
                    Files.walk(dir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                            });
                }
            } catch (IOException ignored) {}
        }
    }

    private void clearSessionAttributes(HttpServletRequest request) {
        request.getSession().removeAttribute(UI_PROVIDER_ID);
        request.getSession().removeAttribute(UI_TEMP_DIR);
        request.getSession().removeAttribute(UI_UPLOADED_FILES);
        request.getSession().removeAttribute(UI_UPLOAD_SUMMARY);
        request.getSession().removeAttribute(UI_RESULTS);
    }
}

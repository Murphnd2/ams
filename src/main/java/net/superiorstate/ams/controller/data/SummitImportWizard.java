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
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.service.SummitImportService;
import net.superiorstate.ams.data.service.SummitImportService.ImportResult;
import net.superiorstate.ams.data.AmsDataGlobal;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Multi-step wizard for importing Summit data into AMS.
 *
 * Step 1 (UPLOAD):    Upload CSV/Excel files, validate headers, show row counts
 * Step 2 (CONFIGURE): Review plan types, set renewal months per type
 * Step 3 (IMPORT):    Execute imports in order (Plan Types → Employers → Employees → Benefits)
 * Step 4 (RESULTS):   Display import results summary
 *
 * Security: PSP Admin session required.
 * Files are saved to a temp directory and cleaned up after import.
 */
@WebServlet(name = "SummitImportWizard", value = "/SummitImport")
@MultipartConfig(
        maxFileSize = 1024 * 1024 * 20,    // 20 MB per file
        maxRequestSize = 1024 * 1024 * 50   // 50 MB total
)
public class SummitImportWizard extends HttpServlet {

    // Session attribute keys
    private static final String SI_TEMP_DIR = "si_tempDir";
    private static final String SI_PLAN_TYPE_FILE = "si_planTypeFile";
    private static final String SI_EMPLOYER_FILE = "si_employerFile";
    private static final String SI_EMPLOYEE_J2_FILE = "si_employeeJ2File";
    private static final String SI_EMPLOYEE_J3_FILE = "si_employeeJ3File";
    private static final String SI_BENEFIT_FILE = "si_benefitFile";
    private static final String SI_BENEFIT_COBRA_FILE = "si_benefitCobraFile";
    private static final String SI_BENEFIT_YEAR_FILE = "si_benefitYearFile";
    private static final String SI_RESULTS = "si_results";

    // ═══════════════════════════════════════════════════════════════
    //  GET — render the current step
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "PSP Admin access required");
            return;
        }

        String step = request.getParameter("step");
        if (step == null) step = "1";

        switch (step) {
            case "2" -> forwardTo(request, response, "step2Configure");
            case "4" -> forwardTo(request, response, "step4Results");
            default -> forwardTo(request, response, "step1Upload");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  POST — handle form submissions
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "PSP Admin access required");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "upload";

        switch (action) {
            case "upload" -> handleUpload(request, response);
            case "import" -> handleImport(request, response);
            case "reset" -> handleReset(request, response);
            default -> response.sendRedirect("SummitImport");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 1 → 2: Upload files, validate, save to temp dir
    // ═══════════════════════════════════════════════════════════════

    private void handleUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Clear any leftover file paths from a previous import run
        clearSessionAttributes(request);

        // Create temp directory under SAVE_PATH (writable on all environments)
        Path baseDir = Path.of(AppConstantDAO.getSavePath(), "summit_import");
        Files.createDirectories(baseDir);
        Path tempDir = Files.createTempDirectory(baseDir, "session_");
        request.getSession().setAttribute(SI_TEMP_DIR, tempDir.toString());

        List<String> errors = new ArrayList<>();
        Map<String, Object> uploadSummary = new LinkedHashMap<>();

        // Process each file upload
        processUpload(request, "planTypeFile", tempDir, SI_PLAN_TYPE_FILE, "Plan Types", uploadSummary, errors, true);
        processUpload(request, "employerFile", tempDir, SI_EMPLOYER_FILE, "Employers", uploadSummary, errors, false);
        processUpload(request, "employeeJ2File", tempDir, SI_EMPLOYEE_J2_FILE, "Employees (Contact)", uploadSummary, errors, false);
        processUpload(request, "employeeJ3File", tempDir, SI_EMPLOYEE_J3_FILE, "Employees (Status)", uploadSummary, errors, false);
        processUpload(request, "benefitFile", tempDir, SI_BENEFIT_FILE, "Benefits (CDH)", uploadSummary, errors, false);
        processUpload(request, "benefitCobraFile", tempDir, SI_BENEFIT_COBRA_FILE, "Benefits (COBRA)", uploadSummary, errors, false);
        processUpload(request, "benefitYearFile", tempDir, SI_BENEFIT_YEAR_FILE, "Benefit Plan Years (J5)", uploadSummary, errors, false);

        // At least one file should be uploaded
        if (request.getSession().getAttribute(SI_PLAN_TYPE_FILE) == null
                && request.getSession().getAttribute(SI_EMPLOYER_FILE) == null
                && request.getSession().getAttribute(SI_EMPLOYEE_J2_FILE) == null
                && request.getSession().getAttribute(SI_BENEFIT_FILE) == null) {
            errors.add("No files were uploaded. Please select at least one file.");
        }

        // Validate: J5 (benefit plan years) requires J4 (CDH benefits) or existing CDH benefits
        boolean hasBenefitYearFile = request.getSession().getAttribute(SI_BENEFIT_YEAR_FILE) != null;
        boolean hasCdhBenefitFile = request.getSession().getAttribute(SI_BENEFIT_FILE) != null;
        if (hasBenefitYearFile && !hasCdhBenefitFile) {
            // Check if CDH benefits already exist in the database
            EntityManagerFactory emfCheck = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager emCheck = emfCheck.createEntityManager();
            try {
                Long cdhCount = emCheck.createQuery(
                        "SELECT COUNT(b) FROM Benefit b WHERE b.sourceType = 'CDH'", Long.class).getSingleResult();
                if (cdhCount == 0) {
                    errors.add("Benefit Plan Years (J5) requires CDH benefit data. Upload a Benefits (CDH) file or import CDH benefits first.");
                    request.getSession().removeAttribute(SI_BENEFIT_YEAR_FILE);
                    uploadSummary.remove("Benefit Plan Years (J5)");
                }
            } finally {
                emCheck.close();
            }
        }

        // Validate: employee and benefit files require employer data
        boolean hasEmployerFile = request.getSession().getAttribute(SI_EMPLOYER_FILE) != null;
        boolean hasEmployeeFiles = request.getSession().getAttribute(SI_EMPLOYEE_J2_FILE) != null
                || request.getSession().getAttribute(SI_EMPLOYEE_J3_FILE) != null;
        boolean hasBenefitFile = request.getSession().getAttribute(SI_BENEFIT_FILE) != null
                || request.getSession().getAttribute(SI_BENEFIT_COBRA_FILE) != null;

        if ((hasEmployeeFiles || hasBenefitFile) && !hasEmployerFile) {
            // Check if employers already exist in the database
            boolean employersExist = false;
            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                Long count = em.createQuery("SELECT COUNT(e) FROM Employer e", Long.class).getSingleResult();
                employersExist = count > 0;
            } finally {
                em.close();
            }

            if (!employersExist) {
                if (hasEmployeeFiles) {
                    errors.add("Employee files require employer data. Upload an Employer file first, or import employers in a separate run before employees.");
                    request.getSession().removeAttribute(SI_EMPLOYEE_J2_FILE);
                    request.getSession().removeAttribute(SI_EMPLOYEE_J3_FILE);
                    uploadSummary.remove("Employees (Contact)");
                    uploadSummary.remove("Employees (Status)");
                }
                if (hasBenefitFile) {
                    errors.add("Benefit files require employer data. Upload an Employer file first, or import employers in a separate run before benefits.");
                    request.getSession().removeAttribute(SI_BENEFIT_FILE);
                    request.getSession().removeAttribute(SI_BENEFIT_COBRA_FILE);
                    uploadSummary.remove("Benefits (CDH)");
                    uploadSummary.remove("Benefits (COBRA)");
                }
            }
        }

        request.setAttribute("uploadSummary", uploadSummary);
        request.setAttribute("uploadErrors", errors);
        forwardTo(request, response, "step2Configure");
    }

    private void processUpload(HttpServletRequest request, String partName, Path tempDir,
                                String sessionKey, String label,
                                Map<String, Object> summary, List<String> errors,
                                boolean isExcel) throws IOException, ServletException {
        Part part = request.getPart(partName);
        if (part == null || part.getSize() == 0) return;

        String fileName = getSubmittedFileName(part);
        if (fileName == null || fileName.isBlank()) return;

        // Save to temp
        Path saved = tempDir.resolve(fileName);
        try (InputStream is = part.getInputStream()) {
            Files.copy(is, saved, StandardCopyOption.REPLACE_EXISTING);
        }
        request.getSession().setAttribute(sessionKey, saved.toString());

        // Validate and get info
        try {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("fileName", fileName);

            if (isExcel) {
                List<String> headers = SummitImportService.getExcelHeaders(saved.toFile());
                int rowCount = SummitImportService.countExcelRows(saved.toFile());
                info.put("headers", headers);
                info.put("rowCount", rowCount);
            } else {
                List<String> headers = SummitImportService.getCsvHeaders(saved.toFile());
                int rowCount = SummitImportService.countCsvRows(saved.toFile());
                info.put("headers", headers);
                info.put("rowCount", rowCount);
            }
            summary.put(label, info);
        } catch (Exception e) {
            errors.add(label + ": Error reading file — " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 2 → 3/4: Execute imports
    // ═══════════════════════════════════════════════════════════════

    private void handleImport(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Map<String, ImportResult> results = new LinkedHashMap<>();

        // Parse renewal months configuration from form
        Map<Integer, Integer> renewalMonthsMap = parseRenewalMonthsConfig(request);

        try {
            // 1. Plan Types (must be first — benefits reference them)
            String planTypeFile = (String) request.getSession().getAttribute(SI_PLAN_TYPE_FILE);
            if (planTypeFile != null) {
                ImportResult ptResult = SummitImportService.importPlanTypes(em, new File(planTypeFile), renewalMonthsMap);
                results.put("Plan Types", ptResult);
            }

            // 2. Employers (must be before employees and benefits)
            String employerFile = (String) request.getSession().getAttribute(SI_EMPLOYER_FILE);
            if (employerFile != null) {
                ImportResult erResult = SummitImportService.importEmployers(em, new File(employerFile));
                results.put("Employers", erResult);
            }

            // 3. Employees (need employers to exist)
            String j2File = (String) request.getSession().getAttribute(SI_EMPLOYEE_J2_FILE);
            String j3File = (String) request.getSession().getAttribute(SI_EMPLOYEE_J3_FILE);
            if (j2File != null || j3File != null) {
                File j2 = j2File != null ? new File(j2File) : null;
                File j3 = j3File != null ? new File(j3File) : null;

                // If only one file provided, create an empty temp file for the other
                if (j2 == null) {
                    j2 = createEmptyCsv(request);
                }
                if (j3 == null) {
                    j3 = createEmptyCsv(request);
                }

                ImportResult eeResult = SummitImportService.importEmployees(em, j2, j3);
                results.put("Employees", eeResult);
            }

            // 4. Benefits — CDH (need employers and plan types)
            String benefitFile = (String) request.getSession().getAttribute(SI_BENEFIT_FILE);
            if (benefitFile != null) {
                ImportResult bResult = SummitImportService.importBenefits(em, new File(benefitFile), renewalMonthsMap);
                results.put("Benefits (CDH)", bResult);
            }

            // 5. Benefits — COBRA/PB (need employers and plan types)
            String benefitCobraFile = (String) request.getSession().getAttribute(SI_BENEFIT_COBRA_FILE);
            if (benefitCobraFile != null) {
                ImportResult bCobraResult = SummitImportService.importBenefitsCobra(em, new File(benefitCobraFile), renewalMonthsMap);
                results.put("Benefits (COBRA)", bCobraResult);
            }

            // 6. Benefit Plan Years — J5 (need CDH benefits to exist)
            String benefitYearFile = (String) request.getSession().getAttribute(SI_BENEFIT_YEAR_FILE);
            if (benefitYearFile != null) {
                ImportResult byResult = SummitImportService.importBenefitYears(em, new File(benefitYearFile));
                results.put("Benefit Plan Years", byResult);
            }

            // Reload global state (employers, service items, etc.)
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            if (global != null) {
                global.initializeGlobalData(em);
                // Reset lazy-loaded employee cache so next access reloads from DB
                if (j2File != null || j3File != null) {
                    global.resetEmployeeCache();
                }
            }

        } catch (Exception e) {
            ImportResult errorResult = new ImportResult();
            errorResult.addError("Import failed: " + e.getMessage());
            results.put("ERROR", errorResult);
            e.printStackTrace();
        } finally {
            if (em.isOpen()) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                em.close();
            }
        }

        request.getSession().setAttribute(SI_RESULTS, results);
        System.out.println("[SummitImport] Import complete. Results: " + results.size() + " sections.");
        for (var e : results.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue().summary());
        }

        // Clean up temp files
        cleanupTempDir(request);

        response.sendRedirect("SummitImport?step=4");
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESET — clear session state and start over
    // ═══════════════════════════════════════════════════════════════

    private void handleReset(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        cleanupTempDir(request);
        clearSessionAttributes(request);
        response.sendRedirect("SummitImport");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private boolean isAdmin(HttpServletRequest request) {
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return isPspAdmin != null && isPspAdmin;
    }

    private void forwardTo(HttpServletRequest request, HttpServletResponse response, String page)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/a/general/summitImport/" + page + ".jsp")
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

    private File createEmptyCsv(HttpServletRequest request) throws IOException {
        String tempDir = (String) request.getSession().getAttribute(SI_TEMP_DIR);
        Path emptyFile = Path.of(tempDir, "empty_" + System.currentTimeMillis() + ".csv");
        Files.writeString(emptyFile, "Participant_ID\n"); // header only
        return emptyFile.toFile();
    }

    private void cleanupTempDir(HttpServletRequest request) {
        String tempDir = (String) request.getSession().getAttribute(SI_TEMP_DIR);
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
        request.getSession().removeAttribute(SI_TEMP_DIR);
        request.getSession().removeAttribute(SI_PLAN_TYPE_FILE);
        request.getSession().removeAttribute(SI_EMPLOYER_FILE);
        request.getSession().removeAttribute(SI_EMPLOYEE_J2_FILE);
        request.getSession().removeAttribute(SI_EMPLOYEE_J3_FILE);
        request.getSession().removeAttribute(SI_BENEFIT_FILE);
        request.getSession().removeAttribute(SI_BENEFIT_COBRA_FILE);
        request.getSession().removeAttribute(SI_BENEFIT_YEAR_FILE);
        request.getSession().removeAttribute(SI_RESULTS);
    }
}

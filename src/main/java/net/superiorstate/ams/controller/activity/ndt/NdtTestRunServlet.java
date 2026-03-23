package net.superiorstate.ams.controller.activity.ndt;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.NdtTestRunDAO;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.ndt.NdtAccessLog;
import net.superiorstate.ams.model.activity.ndt.NdtDocumentUpload;
import net.superiorstate.ams.model.activity.ndt.NdtTestRun;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@WebServlet(name = "NdtTestRunServlet", value = "/NdtTestRun")
@MultipartConfig(
        maxFileSize = 1024 * 1024 * 20,     // 20 MB per file
        maxRequestSize = 1024 * 1024 * 50    // 50 MB total
)
public class NdtTestRunServlet extends HttpServlet {

    private static final String JSP_PATH = "/WEB-INF/view/ndt/ndtTestDashboard.jsp";
    private static final String DEFAULT_UPLOAD_DIR = "/var/lib/tomcat10/ndt-uploads";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "csv", "xlsx", "xls", "pdf", "txt", "tsv", "doc", "docx"
    );
    private static final Set<String> VALID_DOC_TYPES = Set.of(
            NdtDocumentUpload.TYPE_CENSUS, NdtDocumentUpload.TYPE_PAYROLL,
            NdtDocumentUpload.TYPE_OWNERSHIP, NdtDocumentUpload.TYPE_BILLING,
            NdtDocumentUpload.TYPE_ENROLLMENT, NdtDocumentUpload.TYPE_OTHER
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || local.getCurrentPerson() == null) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }

        // PSP Admin only (role 5)
        if (!local.isPspAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Activity activity = local.getCurrentActivity() != null
                    ? local.getCurrentActivity().getActivity() : null;

            if (activity == null) {
                request.setAttribute("error", "No activity selected. Open a Renewal activity first.");
                request.getRequestDispatcher(JSP_PATH).forward(request, response);
                return;
            }

            // Load existing test run for this activity
            NdtTestRun testRun = NdtTestRunDAO.findByActivity(em, activity.getId());

            if (testRun != null) {
                // Load documents separately (avoid nested fetch issues)
                List<NdtDocumentUpload> documents = NdtTestRunDAO.getDocuments(em, testRun.getId());
                request.setAttribute("testRun", testRun);
                request.setAttribute("documents", documents);

                // Log view access
                NdtTestRunDAO.logAccess(em, testRun, local.getCurrentPerson(),
                        NdtAccessLog.ACTION_VIEW, null, request.getRemoteAddr());
            }

            // Pass activity info for the creation form
            request.setAttribute("activity", activity);
            if (activity instanceof Renewal renewal && renewal.getEmployer() != null) {
                request.setAttribute("employerName", renewal.getEmployer().getEmployerName());
            }

            // Also fix the handleCreate method's employer name lookup
            request.setAttribute("pageTitle", "NDT Census Testing");
            request.setAttribute("pageIcon", "bi-shield-check");
            request.getRequestDispatcher(JSP_PATH).forward(request, response);

        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || local.getCurrentPerson() == null) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }
        if (!local.isPspAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "";

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            switch (action) {
                case "create" -> handleCreate(request, response, em, local);
                case "upload" -> handleUpload(request, response, em, local);
                case "deleteUpload" -> handleDeleteUpload(request, response, em, local);
                default -> response.sendRedirect(request.getContextPath() + "/NdtTestRun");
            }
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    // =========================================================================
    // Create a new test run
    // =========================================================================
    private void handleCreate(HttpServletRequest request, HttpServletResponse response,
                              EntityManager em, AmsDataLocal local) throws IOException {

        Activity activity = local.getCurrentActivity().getActivity();
        Person person = local.getCurrentPerson();

        // Check if one already exists
        NdtTestRun existing = NdtTestRunDAO.findByActivity(em, activity.getId());
        if (existing != null) {
            response.sendRedirect(request.getContextPath() + "/NdtTestRun");
            return;
        }

        String planYearEndStr = request.getParameter("planYearEnd");
        if (planYearEndStr == null || planYearEndStr.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/NdtTestRun?error=Plan+year+end+date+is+required");
            return;
        }

        // Get employer name from Renewal
        String employerName = "Unknown Employer";
        if (activity instanceof Renewal renewal && renewal.getEmployer() != null) {
            employerName = renewal.getEmployer().getEmployerName();
        }

        em.getTransaction().begin();
        try {
            NdtTestRun testRun = new NdtTestRun();
            testRun.setActivity(activity);
            testRun.setPsp(person.getPsp());
            testRun.setEmployerName(employerName);
            testRun.setPlanYearEnd(Date.valueOf(planYearEndStr));
            testRun.setCreatedBy(person);
            testRun.setPlanData("{}");
            testRun.setCensusData("[]");

            em.persist(testRun);
            em.flush();

            // Log creation
            NdtAccessLog log = new NdtAccessLog(testRun, person,
                    NdtAccessLog.ACTION_CREATE, "Created NDT test run", request.getRemoteAddr());
            em.persist(log);

            em.getTransaction().commit();
            System.out.println("[NDT] Created test run " + testRun.getId() + " for activity " + activity.getId());

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("[NDT] Error creating test run: " + e.getMessage());
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/NdtTestRun");
    }

    // =========================================================================
    // Upload a document
    // =========================================================================
    private void handleUpload(HttpServletRequest request, HttpServletResponse response,
                              EntityManager em, AmsDataLocal local) throws IOException, ServletException {

        Activity activity = local.getCurrentActivity().getActivity();
        NdtTestRun testRun = NdtTestRunDAO.findByActivity(em, activity.getId());

        if (testRun == null) {
            sendJsonResponse(response, 400, "{\"error\":\"No test run found\"}");
            return;
        }

        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            sendJsonResponse(response, 400, "{\"error\":\"No file provided\"}");
            return;
        }

        String originalFilename = filePart.getSubmittedFileName();
        if (originalFilename == null || originalFilename.isBlank()) {
            sendJsonResponse(response, 400, "{\"error\":\"Invalid filename\"}");
            return;
        }

        // Validate extension
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            sendJsonResponse(response, 400, "{\"error\":\"File type not allowed: " + extension + "\"}");
            return;
        }

        // Validate document type
        String documentType = request.getParameter("documentType");
        if (documentType == null || !VALID_DOC_TYPES.contains(documentType)) {
            documentType = NdtDocumentUpload.TYPE_OTHER;
        }

        // Create storage directory
        String uploadBaseDir = getUploadDir();
        Path testRunDir = Paths.get(uploadBaseDir, String.valueOf(testRun.getId()));
        Files.createDirectories(testRunDir);

        // Generate stored filename
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path storedPath = testRunDir.resolve(storedFilename);

        // Save file to disk
        try (InputStream is = filePart.getInputStream()) {
            Files.copy(is, storedPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Create DB record
        em.getTransaction().begin();
        try {
            NdtDocumentUpload upload = new NdtDocumentUpload();
            upload.setTestRun(testRun);
            upload.setDocumentType(documentType);
            upload.setOriginalFilename(originalFilename);
            upload.setStoredFilename(storedFilename);
            upload.setFileSizeBytes(filePart.getSize());
            upload.setMimeType(filePart.getContentType());
            upload.setUploadedBy(local.getCurrentPerson());

            em.persist(upload);

            // Update test run counts
            testRun.setDocumentCount(testRun.getDocumentCount() + 1);
            em.merge(testRun);

            // Log upload
            NdtAccessLog log = new NdtAccessLog(testRun, local.getCurrentPerson(),
                    NdtAccessLog.ACTION_UPLOAD,
                    documentType + ": " + originalFilename + " (" + filePart.getSize() + " bytes)",
                    request.getRemoteAddr());
            em.persist(log);

            em.getTransaction().commit();

            System.out.println("[NDT] Uploaded " + originalFilename + " -> " + storedFilename
                    + " for test run " + testRun.getId());

            sendJsonResponse(response, 200,
                    "{\"success\":true,\"uploadId\":" + upload.getId()
                            + ",\"filename\":\"" + escapeJson(originalFilename) + "\""
                            + ",\"size\":\"" + upload.getFileSizeFormatted() + "\""
                            + ",\"type\":\"" + upload.getDocumentTypeLabel() + "\"}");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            // Clean up file on failure
            Files.deleteIfExists(storedPath);
            System.out.println("[NDT] Upload error: " + e.getMessage());
            sendJsonResponse(response, 500, "{\"error\":\"Upload failed\"}");
        }
    }

    // =========================================================================
    // Delete an uploaded document
    // =========================================================================
    private void handleDeleteUpload(HttpServletRequest request, HttpServletResponse response,
                                    EntityManager em, AmsDataLocal local) throws IOException {

        String uploadIdStr = request.getParameter("uploadId");
        if (uploadIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/NdtTestRun");
            return;
        }

        Long uploadId = Long.parseLong(uploadIdStr);
        NdtDocumentUpload upload = NdtTestRunDAO.findUploadById(em, uploadId);

        if (upload == null) {
            response.sendRedirect(request.getContextPath() + "/NdtTestRun");
            return;
        }

        NdtTestRun testRun = upload.getTestRun();

        // Delete physical file
        String uploadBaseDir = getUploadDir();
        Path filePath = Paths.get(uploadBaseDir, String.valueOf(testRun.getId()), upload.getStoredFilename());
        Files.deleteIfExists(filePath);

        em.getTransaction().begin();
        try {
            // Log before delete
            NdtAccessLog log = new NdtAccessLog(testRun, local.getCurrentPerson(),
                    NdtAccessLog.ACTION_DELETE_UPLOAD,
                    "Deleted: " + upload.getOriginalFilename(),
                    request.getRemoteAddr());
            em.persist(log);

            // Update count
            testRun.setDocumentCount(Math.max(0, testRun.getDocumentCount() - 1));
            em.merge(testRun);

            // Delete record
            em.remove(em.contains(upload) ? upload : em.merge(upload));

            em.getTransaction().commit();
            System.out.println("[NDT] Deleted upload " + uploadId + " (" + upload.getOriginalFilename() + ")");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("[NDT] Delete error: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/NdtTestRun");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String getUploadDir() {
        String dir = getServletContext().getInitParameter("ndt.upload.dir");
        if (dir == null || dir.isBlank()) {
            // Check system property
            dir = System.getProperty("ndt.upload.dir");
        }
        if (dir == null || dir.isBlank()) {
            dir = DEFAULT_UPLOAD_DIR;
        }
        return dir;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    private void sendJsonResponse(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

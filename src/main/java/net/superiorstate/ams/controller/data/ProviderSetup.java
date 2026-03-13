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
import net.superiorstate.ams.data.service.SummitProviderSeeder;
import net.superiorstate.ams.data.service.UniversalImportService;
import net.superiorstate.ams.model.imports.ImportFieldMapping;
import net.superiorstate.ams.model.imports.ImportFileType;
import net.superiorstate.ams.model.imports.ImportPlanTypeMapping;
import net.superiorstate.ams.model.imports.ImportProvider;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provider configuration admin page.
 * Manages TPA platform providers, file type definitions, column mappings, and plan type mappings.
 * PSP Admin (role 5) or BPO Admin (role 102) only.
 */
@WebServlet(name = "ProviderSetup", value = "/ProviderSetup")
@MultipartConfig(maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 20)
public class ProviderSetup extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "list";

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long pspId = getPspId(request);

            switch (action) {
                case "edit" -> {
                    String idStr = request.getParameter("id");
                    if (idStr != null) {
                        ImportProvider provider = em.find(ImportProvider.class, Integer.parseInt(idStr));
                        request.setAttribute("provider", provider);
                    }
                    forwardTo(request, response, "providerEdit");
                }
                case "files" -> {
                    int providerId = Integer.parseInt(request.getParameter("id"));
                    ImportProvider provider = em.find(ImportProvider.class, providerId);
                    List<ImportFileType> fileTypes = em.createQuery(
                            "SELECT ft FROM ImportFileType ft WHERE ft.provider.id = :pid ORDER BY ft.sortOrder",
                            ImportFileType.class)
                            .setParameter("pid", providerId)
                            .getResultList();
                    request.setAttribute("provider", provider);
                    request.setAttribute("fileTypes", fileTypes);
                    forwardTo(request, response, "fileTypeList");
                }
                case "mappings" -> {
                    int fileTypeId = Integer.parseInt(request.getParameter("fileTypeId"));
                    ImportFileType fileType = em.find(ImportFileType.class, fileTypeId);
                    List<ImportFieldMapping> mappings = em.createQuery(
                            "SELECT m FROM ImportFieldMapping m WHERE m.fileType.id = :ftId ORDER BY m.id",
                            ImportFieldMapping.class)
                            .setParameter("ftId", fileTypeId)
                            .getResultList();

                    // Load sample file headers if a sample file exists
                    List<String> sampleHeaders = loadSampleHeaders(fileType);

                    // Determine which sample headers are unmapped
                    Set<String> mappedSources = new LinkedHashSet<>();
                    for (ImportFieldMapping m : mappings) {
                        mappedSources.add(m.getSourceColumn().toLowerCase());
                    }
                    List<String> unmappedHeaders = new ArrayList<>();
                    if (sampleHeaders != null) {
                        for (String h : sampleHeaders) {
                            if (!mappedSources.contains(h.toLowerCase())) {
                                unmappedHeaders.add(h);
                            }
                        }
                    }

                    request.setAttribute("fileType", fileType);
                    request.setAttribute("mappings", mappings);
                    request.setAttribute("provider", fileType.getProvider());
                    request.setAttribute("sampleHeaders", sampleHeaders);
                    request.setAttribute("unmappedHeaders", unmappedHeaders);
                    forwardTo(request, response, "fieldMappingEdit");
                }
                case "planTypes" -> {
                    int providerId = Integer.parseInt(request.getParameter("id"));
                    ImportProvider provider = em.find(ImportProvider.class, providerId);
                    List<ImportPlanTypeMapping> ptMappings = em.createQuery(
                            "SELECT m FROM ImportPlanTypeMapping m WHERE m.provider.id = :pid ORDER BY m.sourcePlanCode",
                            ImportPlanTypeMapping.class)
                            .setParameter("pid", providerId)
                            .getResultList();
                    List<ImportPlanTypeMapping> systemDefaults = em.createQuery(
                            "SELECT m FROM ImportPlanTypeMapping m WHERE m.provider IS NULL AND m.systemDefault = true ORDER BY m.sourcePlanCode",
                            ImportPlanTypeMapping.class)
                            .getResultList();
                    List<PlanType> allPlanTypes = em.createQuery(
                            "SELECT pt FROM PlanType pt ORDER BY pt.planTypeName", PlanType.class)
                            .getResultList();
                    request.setAttribute("provider", provider);
                    request.setAttribute("ptMappings", ptMappings);
                    request.setAttribute("systemDefaults", systemDefaults);
                    request.setAttribute("allPlanTypes", allPlanTypes);
                    forwardTo(request, response, "planTypeMappingEdit");
                }
                default -> {
                    // Auto-ensure Summit provider exists (idempotent)
                    SummitProviderSeeder.seed(em, pspId);

                    List<ImportProvider> providers = em.createQuery(
                            "SELECT p FROM ImportProvider p WHERE p.pspId = :pspId ORDER BY p.providerName",
                            ImportProvider.class)
                            .setParameter("pspId", pspId)
                            .getResultList();
                    request.setAttribute("providers", providers);
                    forwardTo(request, response, "providerList");
                }
            }
        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        String action = request.getParameter("action");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long pspId = getPspId(request);

            switch (action) {
                case "saveProvider" -> {
                    String idStr = request.getParameter("providerId");
                    String name = request.getParameter("providerName");
                    String code = request.getParameter("providerCode");
                    String desc = request.getParameter("description");

                    em.getTransaction().begin();
                    if (idStr != null && !idStr.isEmpty()) {
                        ImportProvider p = em.find(ImportProvider.class, Integer.parseInt(idStr));
                        p.setProviderName(name);
                        p.setProviderCode(code.toUpperCase());
                        p.setDescription(desc);
                        em.merge(p);
                    } else {
                        ImportProvider p = new ImportProvider();
                        p.setProviderName(name);
                        p.setProviderCode(code.toUpperCase());
                        p.setDescription(desc);
                        p.setPspId(pspId);
                        em.persist(p);
                    }
                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup");
                }
                case "deleteProvider" -> {
                    int providerId = Integer.parseInt(request.getParameter("providerId"));
                    em.getTransaction().begin();
                    ImportProvider p = em.find(ImportProvider.class, providerId);
                    if (p != null) {
                        p.setActive(false);
                        em.merge(p);
                    }
                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup");
                }
                case "saveFileType" -> {
                    int providerId = Integer.parseInt(request.getParameter("providerId"));
                    String ftIdStr = request.getParameter("fileTypeId");
                    String label = request.getParameter("fileLabel");
                    String targetEntity = request.getParameter("targetEntity");
                    String fileFormat = request.getParameter("fileFormat");
                    int sortOrder = parseInt(request.getParameter("sortOrder"), 0);
                    boolean required = "on".equals(request.getParameter("isRequired"));
                    String desc = request.getParameter("description");
                    String updateMode = request.getParameter("updateMode");
                    if (updateMode == null || updateMode.isBlank()) updateMode = "CREATE_AND_UPDATE";

                    ImportProvider provider = em.find(ImportProvider.class, providerId);
                    em.getTransaction().begin();
                    if (ftIdStr != null && !ftIdStr.isEmpty()) {
                        ImportFileType ft = em.find(ImportFileType.class, Integer.parseInt(ftIdStr));
                        ft.setFileLabel(label);
                        ft.setTargetEntity(targetEntity);
                        ft.setFileFormat(fileFormat);
                        ft.setSortOrder(sortOrder);
                        ft.setRequired(required);
                        ft.setDescription(desc);
                        ft.setUpdateMode(updateMode);
                        em.merge(ft);
                    } else {
                        ImportFileType ft = new ImportFileType();
                        ft.setProvider(provider);
                        ft.setFileLabel(label);
                        ft.setTargetEntity(targetEntity);
                        ft.setFileFormat(fileFormat);
                        ft.setSortOrder(sortOrder);
                        ft.setRequired(required);
                        ft.setDescription(desc);
                        ft.setUpdateMode(updateMode);
                        em.persist(ft);
                    }
                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup?action=files&id=" + providerId);
                }
                case "deleteFileType" -> {
                    int ftId = Integer.parseInt(request.getParameter("fileTypeId"));
                    ImportFileType ft = em.find(ImportFileType.class, ftId);
                    int providerId = ft.getProvider().getId();
                    em.getTransaction().begin();
                    // Delete mappings first
                    em.createQuery("DELETE FROM ImportFieldMapping m WHERE m.fileType.id = :ftId")
                            .setParameter("ftId", ftId)
                            .executeUpdate();
                    em.remove(ft);
                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup?action=files&id=" + providerId);
                }
                case "autoDetect" -> {
                    int fileTypeId = Integer.parseInt(request.getParameter("fileTypeId"));
                    ImportFileType fileType = em.find(ImportFileType.class, fileTypeId);
                    Part filePart = request.getPart("sampleFile");

                    if (filePart != null && filePart.getSize() > 0) {
                        // Save sample file to persistent path
                        File sampleFile = saveSampleFile(filePart, fileType);

                        List<String> headers = UniversalImportService.getFileHeaders(sampleFile, fileType.getFileFormat());
                        List<ImportFieldMapping> suggestions = UniversalImportService.autoDetectMappings(
                                headers, fileType.getTargetEntity());

                        // Clear existing mappings, save auto-detected, reset status to PENDING
                        em.getTransaction().begin();
                        em.createQuery("DELETE FROM ImportFieldMapping m WHERE m.fileType.id = :ftId")
                                .setParameter("ftId", fileTypeId)
                                .executeUpdate();

                        for (ImportFieldMapping mapping : suggestions) {
                            mapping.setFileType(fileType);
                            em.persist(mapping);
                        }

                        // Also create unmapped rows for headers that auto-detect didn't match
                        Set<String> mappedSources = new LinkedHashSet<>();
                        for (ImportFieldMapping s : suggestions) {
                            mappedSources.add(s.getSourceColumn().toLowerCase());
                        }
                        for (String header : headers) {
                            if (!mappedSources.contains(header.toLowerCase())) {
                                ImportFieldMapping unmapped = new ImportFieldMapping();
                                unmapped.setFileType(fileType);
                                unmapped.setSourceColumn(header);
                                unmapped.setCanonicalField("");  // empty = unmapped
                                em.persist(unmapped);
                            }
                        }

                        // Reset status — user must re-confirm PK/FK
                        fileType.setMappingStatus("PENDING");
                        em.merge(fileType);

                        em.getTransaction().commit();
                    }

                    response.sendRedirect("ProviderSetup?action=mappings&fileTypeId=" + fileTypeId);
                }
                case "saveMappingsBulk" -> {
                    int fileTypeId = Integer.parseInt(request.getParameter("fileTypeId"));
                    ImportFileType fileType = em.find(ImportFileType.class, fileTypeId);

                    String[] sourceColumns = request.getParameterValues("sourceColumn");
                    String[] canonicalFields = request.getParameterValues("canonicalField");
                    String[] isKeyValues = request.getParameterValues("isKeyFlag");
                    String[] isFkValues = request.getParameterValues("isFkFlag");
                    String[] fkEntityTypes = request.getParameterValues("fkEntityType");
                    String[] isRequiredValues = request.getParameterValues("isRequiredFlag");
                    String[] transformRules = request.getParameterValues("transformRule");

                    em.getTransaction().begin();

                    // Delete all existing mappings
                    em.createQuery("DELETE FROM ImportFieldMapping m WHERE m.fileType.id = :ftId")
                            .setParameter("ftId", fileTypeId)
                            .executeUpdate();

                    // The PK value sent is the index of the radio-selected row
                    String pkIndex = request.getParameter("pkColumnIndex");
                    int pkIdx = (pkIndex != null && !pkIndex.isEmpty()) ? Integer.parseInt(pkIndex) : -1;

                    if (sourceColumns != null) {
                        for (int i = 0; i < sourceColumns.length; i++) {
                            String source = sourceColumns[i];
                            String canonical = (canonicalFields != null && i < canonicalFields.length) ? canonicalFields[i] : "";
                            boolean isKey = (i == pkIdx);
                            boolean isFk = (isFkValues != null && i < isFkValues.length && "true".equals(isFkValues[i]));
                            String fkEt = (fkEntityTypes != null && i < fkEntityTypes.length) ? fkEntityTypes[i] : null;
                            boolean isReq = (isRequiredValues != null && i < isRequiredValues.length && "true".equals(isRequiredValues[i]));
                            String transform = (transformRules != null && i < transformRules.length) ? transformRules[i] : null;

                            ImportFieldMapping m = new ImportFieldMapping();
                            m.setFileType(fileType);
                            m.setSourceColumn(source);
                            m.setCanonicalField(canonical != null && !canonical.isBlank() ? canonical : "");
                            m.setKey(isKey);
                            m.setFk(isFk);
                            m.setFkEntityType(isFk && fkEt != null && !fkEt.isBlank() ? fkEt : null);
                            m.setRequired(isReq || isKey); // PK is always required
                            m.setTransformRule(transform != null && !transform.isBlank() ? transform : null);
                            em.persist(m);
                        }
                    }

                    // Validate and update status
                    validateAndUpdateStatus(em, fileType);
                    em.merge(fileType);

                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup?action=mappings&fileTypeId=" + fileTypeId);
                }
                case "saveMapping" -> {
                    int fileTypeId = Integer.parseInt(request.getParameter("fileTypeId"));
                    ImportFileType fileType = em.find(ImportFileType.class, fileTypeId);

                    String mappingIdStr = request.getParameter("mappingId");
                    String sourceCol = request.getParameter("sourceColumn");
                    String canonicalField = request.getParameter("canonicalField");
                    boolean isRequired = "on".equals(request.getParameter("isRequired"));
                    boolean isKey = "on".equals(request.getParameter("isKey"));
                    String transformRule = request.getParameter("transformRule");

                    em.getTransaction().begin();
                    if (mappingIdStr != null && !mappingIdStr.isEmpty()) {
                        ImportFieldMapping m = em.find(ImportFieldMapping.class, Integer.parseInt(mappingIdStr));
                        m.setSourceColumn(sourceCol);
                        m.setCanonicalField(canonicalField);
                        m.setRequired(isRequired);
                        m.setKey(isKey);
                        m.setTransformRule(transformRule != null && !transformRule.isBlank() ? transformRule : null);
                        em.merge(m);
                    } else {
                        ImportFieldMapping m = new ImportFieldMapping();
                        m.setFileType(fileType);
                        m.setSourceColumn(sourceCol);
                        m.setCanonicalField(canonicalField);
                        m.setRequired(isRequired);
                        m.setKey(isKey);
                        m.setTransformRule(transformRule != null && !transformRule.isBlank() ? transformRule : null);
                        em.persist(m);
                    }

                    // Re-validate status after individual mapping change
                    validateAndUpdateStatus(em, fileType);
                    em.merge(fileType);

                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup?action=mappings&fileTypeId=" + fileTypeId);
                }
                case "deleteMapping" -> {
                    int mappingId = Integer.parseInt(request.getParameter("mappingId"));
                    int fileTypeId = Integer.parseInt(request.getParameter("fileTypeId"));
                    ImportFileType fileType = em.find(ImportFileType.class, fileTypeId);
                    em.getTransaction().begin();
                    ImportFieldMapping m = em.find(ImportFieldMapping.class, mappingId);
                    if (m != null) em.remove(m);

                    // Re-validate status
                    validateAndUpdateStatus(em, fileType);
                    em.merge(fileType);

                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup?action=mappings&fileTypeId=" + fileTypeId);
                }
                case "savePlanTypeMapping" -> {
                    int providerId = Integer.parseInt(request.getParameter("providerId"));
                    ImportProvider provider = em.find(ImportProvider.class, providerId);

                    String ptmIdStr = request.getParameter("ptmId");
                    String sourcePlanCode = request.getParameter("sourcePlanCode");
                    String sourcePlanName = request.getParameter("sourcePlanName");
                    String targetPtIdStr = request.getParameter("targetPlanTypeId");
                    PlanType targetPt = (targetPtIdStr != null && !targetPtIdStr.isEmpty())
                            ? em.find(PlanType.class, Integer.parseInt(targetPtIdStr)) : null;

                    em.getTransaction().begin();
                    if (ptmIdStr != null && !ptmIdStr.isEmpty()) {
                        ImportPlanTypeMapping ptm = em.find(ImportPlanTypeMapping.class, Integer.parseInt(ptmIdStr));
                        ptm.setSourcePlanCode(sourcePlanCode.toUpperCase());
                        ptm.setSourcePlanName(sourcePlanName);
                        ptm.setTargetPlanType(targetPt);
                        em.merge(ptm);
                    } else {
                        ImportPlanTypeMapping ptm = new ImportPlanTypeMapping();
                        ptm.setProvider(provider);
                        ptm.setSourcePlanCode(sourcePlanCode.toUpperCase());
                        ptm.setSourcePlanName(sourcePlanName);
                        ptm.setTargetPlanType(targetPt);
                        em.persist(ptm);
                    }
                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup?action=planTypes&id=" + providerId);
                }
                case "deletePlanTypeMapping" -> {
                    int ptmId = Integer.parseInt(request.getParameter("ptmId"));
                    int providerId = Integer.parseInt(request.getParameter("providerId"));
                    em.getTransaction().begin();
                    ImportPlanTypeMapping ptm = em.find(ImportPlanTypeMapping.class, ptmId);
                    if (ptm != null) em.remove(ptm);
                    em.getTransaction().commit();
                    response.sendRedirect("ProviderSetup?action=planTypes&id=" + providerId);
                }
                default -> response.sendRedirect("ProviderSetup");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Check if a file type's mappings meet the minimum requirements for import readiness.
     * Updates the fileType's mappingStatus to READY or PENDING.
     * Must be called within an active transaction.
     */
    private void validateAndUpdateStatus(EntityManager em, ImportFileType fileType) {
        List<ImportFieldMapping> mappings = em.createQuery(
                "SELECT m FROM ImportFieldMapping m WHERE m.fileType.id = :ftId",
                ImportFieldMapping.class)
                .setParameter("ftId", fileType.getId())
                .getResultList();

        boolean hasPk = false;
        boolean hasEmployerFk = false;
        boolean hasPlanTypeFk = false;

        for (ImportFieldMapping m : mappings) {
            if (m.isKey()) hasPk = true;
            if (m.isFk() && "EMPLOYER".equals(m.getFkEntityType())) hasEmployerFk = true;
            if (m.isFk() && "PLAN_TYPE".equals(m.getFkEntityType())) hasPlanTypeFk = true;
        }

        boolean ready = hasPk; // PK is always required

        // UPDATE_ONLY mode only needs PK — no FK requirements (supplemental data refresh)
        boolean updateOnly = "UPDATE_ONLY".equals(fileType.getUpdateMode());

        if (!updateOnly) {
            String target = fileType.getTargetEntity();
            if ("BENEFIT".equals(target)) {
                ready = ready && hasEmployerFk && hasPlanTypeFk;
            } else if ("EMPLOYEE".equals(target)) {
                ready = ready && hasEmployerFk;
            }
        }

        fileType.setMappingStatus(ready ? "READY" : "PENDING");
    }

    // ── Sample file management ────────────────────────────────────────────

    /**
     * Save an uploaded sample file to a persistent location under SAVE_PATH.
     * Returns the saved File.
     */
    private File saveSampleFile(Part filePart, ImportFileType fileType) throws IOException {
        String ext = switch (fileType.getFileFormat()) {
            case "EXCEL" -> ".xlsx";
            case "TSV" -> ".tsv";
            default -> ".csv";
        };

        Path sampleDir = Path.of(AppConstantDAO.getSavePath(),
                "universal_import", "samples",
                String.valueOf(fileType.getProvider().getId()),
                String.valueOf(fileType.getId()));
        Files.createDirectories(sampleDir);

        File sampleFile = sampleDir.resolve("sample" + ext).toFile();
        try (InputStream is = filePart.getInputStream()) {
            Files.copy(is, sampleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return sampleFile;
    }

    /**
     * Load headers from a previously saved sample file.
     * Returns null if no sample file exists.
     */
    private List<String> loadSampleHeaders(ImportFileType fileType) {
        String ext = switch (fileType.getFileFormat()) {
            case "EXCEL" -> ".xlsx";
            case "TSV" -> ".tsv";
            default -> ".csv";
        };

        Path samplePath = Path.of(AppConstantDAO.getSavePath(),
                "universal_import", "samples",
                String.valueOf(fileType.getProvider().getId()),
                String.valueOf(fileType.getId()),
                "sample" + ext);

        File sampleFile = samplePath.toFile();
        if (!sampleFile.exists()) return null;

        try {
            return UniversalImportService.getFileHeaders(sampleFile, fileType.getFileFormat());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────

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
        return 4L; // default PSP
    }

    private void forwardTo(HttpServletRequest request, HttpServletResponse response, String page)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/a/general/providerSetup/" + page + ".jsp")
                .forward(request, response);
    }

    private static int parseInt(String s, int defaultVal) {
        if (s == null || s.isBlank()) return defaultVal;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}

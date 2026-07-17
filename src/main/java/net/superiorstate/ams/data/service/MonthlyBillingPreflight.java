package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.data.util.PathUtil;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Read-only pre-flight verification for the Monthly Billing Launcher. Given the
 * monthly upload folder, reports which required import files are present/missing and
 * whether the benefit files reference any plan type not yet in the live plantype
 * table (which would otherwise be silently dropped at promotion — see
 * Updater.processNewBenefitI4Fast/I7Fast). Performs no writes and moves no files.
 */
public abstract class MonthlyBillingPreflight {

    private static final Set<String> RELEVANT_EXTENSIONS = Set.of(".csv", ".xlsx", ".xls");
    private static final String TABLE_PLAN_TYPE = "importplantype";
    private static final String TABLE_CDH = "import4benefitcdh";
    private static final String TABLE_PB = "import7benefitpb";

    public static PreflightResult check(EntityManagerFactory emf) throws IOException {
        Path uploadDir = PathUtil.resolveAndEnsureDir(
                null, "AMS_UPLOAD_DIR", "AMS_UPLOAD_DIR", "ams.upload.dir", "work/ams-uploads");

        List<Path> folderFiles = listRelevantFiles(uploadDir);

        // Step 2: match files to mappings — first match wins per mapping.
        Map<String, String> tableNameToFileName = new LinkedHashMap<>();
        List<String> unrecognizedFiles = new ArrayList<>();

        for (Path file : folderFiles) {
            Importer.TableMapping mapping;
            try {
                mapping = Importer.findMatchingTableMapping(file);
            } catch (IOException e) {
                mapping = null;
            }

            if (mapping == null) {
                unrecognizedFiles.add(file.getFileName().toString());
                continue;
            }

            tableNameToFileName.putIfAbsent(mapping.tableName(), file.getFileName().toString());
        }

        // Step 3: build FileStatus list, one per TABLE_MAPPINGS entry, in order.
        List<FileStatus> files = new ArrayList<>();
        for (Importer.TableMapping mapping : Importer.TABLE_MAPPINGS) {
            String tableName = mapping.tableName();
            String matchedFileName = tableNameToFileName.get(tableName);

            FileStatus fs = new FileStatus();
            fs.label = mapping.filePrefix();
            fs.tableName = tableName;
            fs.required = !TABLE_PLAN_TYPE.equals(tableName);
            fs.present = matchedFileName != null;
            fs.matchedFileName = matchedFileName;
            files.add(fs);
        }

        // Step 4: plan-type supplied?
        boolean planTypeSupplied = tableNameToFileName.containsKey(TABLE_PLAN_TYPE);

        // Step 5: plan-type gap check — only when no Plan Type file was supplied.
        List<Integer> newPlanTypeIds = new ArrayList<>();
        if (!planTypeSupplied) {
            Set<Integer> referencedPlanTypeIds = new TreeSet<>();

            String cdhFileName = tableNameToFileName.get(TABLE_CDH);
            if (cdhFileName != null) {
                referencedPlanTypeIds.addAll(readPlanTypeIds(new File(uploadDir.toFile(), cdhFileName)));
            }

            String pbFileName = tableNameToFileName.get(TABLE_PB);
            if (pbFileName != null) {
                referencedPlanTypeIds.addAll(readPlanTypeIds(new File(uploadDir.toFile(), pbFileName)));
            }

            if (!referencedPlanTypeIds.isEmpty()) {
                Set<Integer> livePlanTypeIds = loadLivePlanTypeIds(emf);
                referencedPlanTypeIds.removeAll(livePlanTypeIds);
                newPlanTypeIds.addAll(referencedPlanTypeIds);
                Collections.sort(newPlanTypeIds);
            }
        }
        boolean planTypeGapDetected = !newPlanTypeIds.isEmpty();

        // Step 6: canLaunch.
        boolean allRequiredPresent = files.stream().filter(f -> f.required).allMatch(f -> f.present);
        boolean canLaunch = allRequiredPresent && (planTypeSupplied || !planTypeGapDetected);

        // Step 7: errors (blocking).
        List<String> errors = new ArrayList<>();
        for (FileStatus fs : files) {
            if (fs.required && !fs.present) {
                errors.add("Missing required file: " + fs.label);
            }
        }
        if (planTypeGapDetected) {
            errors.add("New plan type(s) referenced (" + newPlanTypeIds
                    + ") — run the Plan Type export in Summit and add it before launching.");
        }

        // Step 8: notes (informational).
        List<String> notes = new ArrayList<>();
        if (planTypeSupplied) {
            notes.add("Plan Type file provided.");
        } else if (!planTypeGapDetected) {
            notes.add("No new plan types detected — Plan Type file not needed.");
        }

        PreflightResult result = new PreflightResult();
        result.files = files;
        result.planTypeSupplied = planTypeSupplied;
        result.planTypeGapDetected = planTypeGapDetected;
        result.newPlanTypeIds = newPlanTypeIds;
        result.unrecognizedFiles = unrecognizedFiles;
        result.canLaunch = canLaunch;
        result.errors = errors;
        result.notes = notes;
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static List<Path> listRelevantFiles(Path uploadDir) throws IOException {
        List<Path> result = new ArrayList<>();
        try (var stream = Files.list(uploadDir)) {
            for (Path p : stream.toList()) {
                if (!Files.isRegularFile(p)) continue;
                String lower = p.getFileName().toString().toLowerCase();
                for (String ext : RELEVANT_EXTENSIONS) {
                    if (lower.endsWith(ext)) {
                        result.add(p);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static Set<Integer> readPlanTypeIds(File file) throws IOException {
        Set<Integer> ids = new TreeSet<>();
        List<Map<String, String>> rows = SummitImportService.parseCsv(file);
        for (Map<String, String> row : rows) {
            String raw = row.get("plantypeid");
            if (raw == null) continue;
            raw = raw.trim();
            if (raw.isEmpty()) continue;
            try {
                ids.add(Integer.parseInt(raw));
            } catch (NumberFormatException ignored) {
                // non-numeric plantypeid value — skip
            }
        }
        return ids;
    }

    private static Set<Integer> loadLivePlanTypeIds(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            List<Integer> ids = em.createQuery(
                    "SELECT p.planTypeId FROM PlanType p", Integer.class)
                    .getResultList();
            return new HashSet<>(ids);
        } finally {
            em.close();
        }
    }

    // ── Result POJOs ─────────────────────────────────────────────────

    public static class FileStatus {
        private String label;
        private String tableName;
        private boolean required;
        private boolean present;
        private String matchedFileName;

        public String getLabel() { return label; }
        public String getTableName() { return tableName; }
        public boolean isRequired() { return required; }
        public boolean isPresent() { return present; }
        public String getMatchedFileName() { return matchedFileName; }
    }

    public static class PreflightResult {
        private List<FileStatus> files;
        private boolean planTypeSupplied;
        private boolean planTypeGapDetected;
        private List<Integer> newPlanTypeIds;
        private List<String> unrecognizedFiles;
        private boolean canLaunch;
        private List<String> errors;
        private List<String> notes;

        public List<FileStatus> getFiles() { return files; }
        public boolean isPlanTypeSupplied() { return planTypeSupplied; }
        public boolean isPlanTypeGapDetected() { return planTypeGapDetected; }
        public List<Integer> getNewPlanTypeIds() { return newPlanTypeIds; }
        public List<String> getUnrecognizedFiles() { return unrecognizedFiles; }
        public boolean isCanLaunch() { return canLaunch; }
        public List<String> getErrors() { return errors; }
        public List<String> getNotes() { return notes; }
    }
}

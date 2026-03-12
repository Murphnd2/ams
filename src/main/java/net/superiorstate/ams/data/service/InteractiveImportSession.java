package net.superiorstate.ams.data.service;

import java.io.Serializable;
import java.util.*;

/**
 * Session-scoped state for the Interactive Import Wizard.
 * Tracks the user's progress through entity-by-entity import with cross-reference resolution.
 * Stored in HttpSession under key "ii_session".
 */
public class InteractiveImportSession implements Serializable {

    private int providerId;
    private String providerName;
    private String tempDirPath;
    private int runLogId;

    /**
     * Current wizard step: "SELECT_PROVIDER", "PLAN_TYPE", "EMPLOYER", "BENEFIT", "EMPLOYEE", "RESULTS"
     */
    private String currentEntityStep = "SELECT_PROVIDER";

    /**
     * Per-entity-type state, keyed by entity type string.
     * Only populated for entities the provider has file types defined for.
     */
    private Map<String, EntityImportState> entityStates = new LinkedHashMap<>();

    /** Entity types that have been committed (in processing order). */
    private List<String> completedEntities = new ArrayList<>();

    /** Default renewal months and per-plan-type overrides. */
    private Map<Integer, Integer> renewalMonthsMap = new HashMap<>();

    /** Entity processing order. */
    public static final List<String> ENTITY_ORDER = List.of(
            "PLAN_TYPE", "EMPLOYER", "BENEFIT", "EMPLOYEE"
    );

    // ── Getters/Setters ───────────────────────────────────────────

    public int getProviderId() { return providerId; }
    public void setProviderId(int providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getTempDirPath() { return tempDirPath; }
    public void setTempDirPath(String tempDirPath) { this.tempDirPath = tempDirPath; }

    public int getRunLogId() { return runLogId; }
    public void setRunLogId(int runLogId) { this.runLogId = runLogId; }

    public String getCurrentEntityStep() { return currentEntityStep; }
    public void setCurrentEntityStep(String currentEntityStep) { this.currentEntityStep = currentEntityStep; }

    public Map<String, EntityImportState> getEntityStates() { return entityStates; }
    public List<String> getCompletedEntities() { return completedEntities; }

    public Map<Integer, Integer> getRenewalMonthsMap() { return renewalMonthsMap; }
    public void setRenewalMonthsMap(Map<Integer, Integer> renewalMonthsMap) { this.renewalMonthsMap = renewalMonthsMap; }

    /**
     * Returns the ordered list of entity types this provider has defined (READY or UPDATE_ONLY eligible).
     */
    public List<String> getAvailableEntityTypes() {
        List<String> result = new ArrayList<>();
        for (String et : ENTITY_ORDER) {
            if (entityStates.containsKey(et)) {
                result.add(et);
            }
        }
        return result;
    }

    /**
     * Advance to the next entity type in processing order, or "RESULTS" if all done.
     */
    public void advanceToNextEntity() {
        List<String> available = getAvailableEntityTypes();
        int idx = available.indexOf(currentEntityStep);
        if (idx >= 0 && idx < available.size() - 1) {
            currentEntityStep = available.get(idx + 1);
        } else {
            currentEntityStep = "RESULTS";
        }
    }

    /**
     * Get the 1-based step number for display (1=Provider, 2..N+1=entities, N+2=Results).
     */
    public int getStepNumber() {
        if ("SELECT_PROVIDER".equals(currentEntityStep)) return 1;
        if ("RESULTS".equals(currentEntityStep)) return getAvailableEntityTypes().size() + 2;
        List<String> available = getAvailableEntityTypes();
        int idx = available.indexOf(currentEntityStep);
        return idx >= 0 ? idx + 2 : 1;
    }

    /**
     * Get total number of steps (1 provider + N entities + 1 results).
     */
    public int getTotalSteps() {
        return getAvailableEntityTypes().size() + 2;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Inner class: EntityImportState
    // ═══════════════════════════════════════════════════════════════

    public static class EntityImportState implements Serializable {
        private String entityType;
        private int fileTypeId;
        private String updateMode;       // CREATE_AND_UPDATE, CREATE_ONLY, UPDATE_ONLY
        private String filePath;
        private String fileName;
        private int totalRows;

        private List<ImportRow> rows = new ArrayList<>();

        // Cached counts
        private int matchedCount;
        private int suggestedCount;
        private int unmatchedCount;
        private int errorCount;

        private boolean resolutionComplete;
        private boolean skipped;

        /** Import result after commit (from UniversalImportService.ImportResult). */
        private UniversalImportService.ImportResult result;

        // ── Getters/Setters ───────────────────────────────

        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }

        public int getFileTypeId() { return fileTypeId; }
        public void setFileTypeId(int fileTypeId) { this.fileTypeId = fileTypeId; }

        public String getUpdateMode() { return updateMode; }
        public void setUpdateMode(String updateMode) { this.updateMode = updateMode; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

        public List<ImportRow> getRows() { return rows; }
        public void setRows(List<ImportRow> rows) { this.rows = rows; }

        public int getMatchedCount() { return matchedCount; }
        public int getSuggestedCount() { return suggestedCount; }
        public int getUnmatchedCount() { return unmatchedCount; }
        public int getErrorCount() { return errorCount; }

        public boolean isResolutionComplete() { return resolutionComplete; }
        public void setResolutionComplete(boolean resolutionComplete) { this.resolutionComplete = resolutionComplete; }

        public boolean isSkipped() { return skipped; }
        public void setSkipped(boolean skipped) { this.skipped = skipped; }

        public UniversalImportService.ImportResult getResult() { return result; }
        public void setResult(UniversalImportService.ImportResult result) { this.result = result; }

        /** Recalculate status counts from the row list. */
        public void recalculateCounts() {
            matchedCount = 0; suggestedCount = 0; unmatchedCount = 0; errorCount = 0;
            for (ImportRow row : rows) {
                switch (row.getStatus()) {
                    case "MATCHED", "CONFIRMED", "MANUAL" -> matchedCount++;
                    case "SUGGESTED" -> suggestedCount++;
                    case "UNMATCHED" -> unmatchedCount++;
                    case "ERROR" -> errorCount++;
                    case "SKIPPED" -> {} // not counted in the main categories
                }
            }
        }

        /** Friendly label for this entity type. */
        public String getEntityLabel() {
            return switch (entityType) {
                case "PLAN_TYPE" -> "Plan Types";
                case "EMPLOYER" -> "Employers";
                case "BENEFIT" -> "Benefits";
                case "EMPLOYEE" -> "Employees";
                default -> entityType;
            };
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Inner class: ImportRow
    // ═══════════════════════════════════════════════════════════════

    public static class ImportRow implements Serializable {
        private int rowIndex;
        private String externalId;
        private Map<String, String> canonicalValues;   // canonical_field → value
        private String displayLabel;

        /**
         * Resolution status:
         *  MATCHED   — xref mapping already existed
         *  SUGGESTED — fuzzy/name match found, awaiting user confirmation
         *  UNMATCHED — no match found, will create new on commit
         *  CONFIRMED — user confirmed a suggestion
         *  MANUAL    — user manually linked to an AMS record
         *  SKIPPED   — user chose to skip this row
         *  ERROR     — validation error (missing FK, etc.)
         */
        private String status = "UNMATCHED";

        private Integer amsInternalId;
        private String amsDisplayLabel;
        private String matchMethod;
        private double matchConfidence;
        private String errorMessage;

        private List<MatchCandidate> candidates;

        // ── Getters/Setters ───────────────────────────────

        public int getRowIndex() { return rowIndex; }
        public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }

        public Map<String, String> getCanonicalValues() { return canonicalValues; }
        public void setCanonicalValues(Map<String, String> canonicalValues) { this.canonicalValues = canonicalValues; }

        public String getDisplayLabel() { return displayLabel; }
        public void setDisplayLabel(String displayLabel) { this.displayLabel = displayLabel; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getAmsInternalId() { return amsInternalId; }
        public void setAmsInternalId(Integer amsInternalId) { this.amsInternalId = amsInternalId; }

        public String getAmsDisplayLabel() { return amsDisplayLabel; }
        public void setAmsDisplayLabel(String amsDisplayLabel) { this.amsDisplayLabel = amsDisplayLabel; }

        public String getMatchMethod() { return matchMethod; }
        public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }

        public double getMatchConfidence() { return matchConfidence; }
        public void setMatchConfidence(double matchConfidence) { this.matchConfidence = matchConfidence; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public List<MatchCandidate> getCandidates() { return candidates; }
        public void setCandidates(List<MatchCandidate> candidates) { this.candidates = candidates; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Inner class: MatchCandidate
    // ═══════════════════════════════════════════════════════════════

    public static class MatchCandidate implements Serializable {
        private int internalId;
        private String displayLabel;
        private String matchMethod;
        private double confidence;
        private String detail;

        public MatchCandidate() {}

        public MatchCandidate(int internalId, String displayLabel, String matchMethod, double confidence, String detail) {
            this.internalId = internalId;
            this.displayLabel = displayLabel;
            this.matchMethod = matchMethod;
            this.confidence = confidence;
            this.detail = detail;
        }

        public int getInternalId() { return internalId; }
        public void setInternalId(int internalId) { this.internalId = internalId; }

        public String getDisplayLabel() { return displayLabel; }
        public void setDisplayLabel(String displayLabel) { this.displayLabel = displayLabel; }

        public String getMatchMethod() { return matchMethod; }
        public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
    }
}

package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.resolver.ImportIdResolver;
import net.superiorstate.ams.data.service.InteractiveImportSession.ImportRow;
import net.superiorstate.ams.data.service.InteractiveImportSession.MatchCandidate;
import net.superiorstate.ams.data.service.UniversalImportService.FieldMappingInfo;
import net.superiorstate.ams.model.imports.ImportFieldMapping;
import net.superiorstate.ams.model.imports.ImportProvider;
import net.superiorstate.ams.model.summit.archive.*;

import java.util.*;

/**
 * Auto-resolution engine for Interactive Import.
 * Parses uploaded file rows and categorizes each as MATCHED / SUGGESTED / UNMATCHED
 * against existing AMS data, using cross-reference mappings, exact matches, and fuzzy matching.
 */
public abstract class ImportResolutionService {

    // Fuzzy match thresholds
    private static final double MATCH_THRESHOLD = 0.90;    // auto-match
    private static final double SUGGEST_THRESHOLD = 0.65;   // show as suggestion

    // Corporate suffixes to strip before fuzzy comparison
    private static final String[] CORP_SUFFIXES = {
            " inc", " inc.", " llc", " llc.", " ltd", " ltd.", " corp", " corp.",
            " co", " co.", " company", " corporation", " incorporated",
            " group", " holdings", " enterprises", " services", " solutions"
    };

    // ═══════════════════════════════════════════════════════════════
    //  MAIN ENTRY POINT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resolve all parsed rows for a given entity type.
     * Categorizes each row as MATCHED, SUGGESTED, UNMATCHED, or ERROR.
     *
     * @param em         EntityManager (read-only, no transactions)
     * @param provider   the ImportProvider for this import
     * @param parsedRows raw parsed rows from CSV/Excel (lowercased column keys)
     * @param mappings   canonical field mappings from UniversalImportService.loadFieldMappings()
     * @param entityType PLAN_TYPE, EMPLOYER, BENEFIT, or EMPLOYEE
     * @param updateMode CREATE_AND_UPDATE, CREATE_ONLY, or UPDATE_ONLY
     * @param fkMappings FK field mappings for resolving foreign keys (from loadFkMappings)
     * @return list of ImportRow with resolution status set
     */
    public static List<ImportRow> resolveRows(EntityManager em, ImportProvider provider,
                                               List<Map<String, String>> parsedRows,
                                               Map<String, FieldMappingInfo> mappings,
                                               String entityType, String updateMode,
                                               Map<String, String> fkMappings) {
        List<ImportRow> results = new ArrayList<>();

        // Find the PK field (is_key = true)
        String pkField = findPkField(mappings);

        for (int i = 0; i < parsedRows.size(); i++) {
            Map<String, String> rawRow = parsedRows.get(i);

            ImportRow row = new ImportRow();
            row.setRowIndex(i);

            // Extract canonical values
            Map<String, String> canonical = new LinkedHashMap<>();
            for (Map.Entry<String, FieldMappingInfo> entry : mappings.entrySet()) {
                String value = UniversalImportService.getCanonicalValue(rawRow, mappings, entry.getKey());
                if (!value.isEmpty()) {
                    canonical.put(entry.getKey(), value);
                }
            }
            row.setCanonicalValues(canonical);

            // Extract external ID from the PK field
            String externalId = pkField != null ? canonical.getOrDefault(pkField, "") : "";
            if (externalId.isEmpty() && pkField != null) {
                // Try the raw row directly with source column name
                FieldMappingInfo pkInfo = mappings.get(pkField);
                if (pkInfo != null) {
                    externalId = rawRow.getOrDefault(pkInfo.getSourceColumn(), "").trim();
                }
            }
            row.setExternalId(externalId);

            // Generate display label
            row.setDisplayLabel(generateDisplayLabel(canonical, entityType));

            // Skip empty rows
            if (externalId.isEmpty() && row.getDisplayLabel().isEmpty()) {
                continue; // skip truly empty rows
            }

            // Resolve by entity type
            switch (entityType) {
                case "PLAN_TYPE" -> resolvePlanTypeRow(em, provider, row, canonical);
                case "EMPLOYER" -> resolveEmployerRow(em, provider, row, canonical);
                case "BENEFIT" -> resolveBenefitRow(em, provider, row, canonical, fkMappings);
                case "EMPLOYEE" -> resolveEmployeeRow(em, provider, row, canonical, fkMappings);
            }

            results.add(row);
        }

        return results;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PER-ENTITY RESOLVERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Plan Type resolution cascade:
     * 1. Cross-reference (import_id_mapping) → MATCHED
     * 2. Code match (import_plan_type_mapping) → MATCHED
     * 3. Exact name match → MATCHED
     * 4. Fuzzy name match → SUGGESTED (with candidates)
     * 5. No match → UNMATCHED
     */
    private static void resolvePlanTypeRow(EntityManager em, ImportProvider provider,
                                            ImportRow row, Map<String, String> canonical) {
        String externalId = row.getExternalId();
        String code = canonical.getOrDefault("code", "");
        String name = canonical.getOrDefault("name", "");

        // 1) Cross-reference lookup
        if (!externalId.isEmpty()) {
            Integer internalId = ImportIdResolver.resolveInternalId(
                    em, provider.getId(), ImportIdResolver.PLAN_TYPE, externalId);
            if (internalId != null) {
                PlanType pt = em.find(PlanType.class, internalId);
                if (pt != null) {
                    setMatched(row, pt.getPlanTypeId(), formatPlanTypeLabel(pt), "xref", 1.0);
                    return;
                }
            }
        }

        // 2) Use ImportIdResolver's cascading plan type match (code → exact name)
        String[] matchInfo = new String[1];
        PlanType resolved = ImportIdResolver.resolvePlanType(em, provider, externalId, code, name, matchInfo);
        if (resolved != null) {
            String method = matchInfo[0]; // "identity", "code", or "name"
            setMatched(row, resolved.getPlanTypeId(), formatPlanTypeLabel(resolved), method, 1.0);
            return;
        }

        // 3) Fuzzy name match against all plan types
        if (!name.isEmpty()) {
            List<PlanType> allPlanTypes = em.createQuery(
                    "SELECT pt FROM PlanType pt", PlanType.class).getResultList();
            List<MatchCandidate> candidates = new ArrayList<>();

            for (PlanType pt : allPlanTypes) {
                if (pt.getPlanTypeName() == null) continue;
                double score = fuzzyScore(name, pt.getPlanTypeName());
                if (score >= SUGGEST_THRESHOLD) {
                    candidates.add(new MatchCandidate(
                            pt.getPlanTypeId(),
                            formatPlanTypeLabel(pt),
                            "fuzzy",
                            score,
                            "Code: " + (pt.getCode() != null ? pt.getCode() : "—")
                    ));
                }
            }

            if (!candidates.isEmpty()) {
                candidates.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

                // If best match is above auto-match threshold, auto-match
                MatchCandidate best = candidates.get(0);
                if (best.getConfidence() >= MATCH_THRESHOLD) {
                    setMatched(row, best.getInternalId(), best.getDisplayLabel(), "fuzzy", best.getConfidence());
                    return;
                }

                // Otherwise, suggest top candidates
                row.setStatus("SUGGESTED");
                row.setAmsInternalId(best.getInternalId());
                row.setAmsDisplayLabel(best.getDisplayLabel());
                row.setMatchMethod("fuzzy");
                row.setMatchConfidence(best.getConfidence());
                row.setCandidates(candidates.size() > 5 ? candidates.subList(0, 5) : candidates);
                return;
            }
        }

        // 4) No match
        row.setStatus("UNMATCHED");
    }

    /**
     * Employer resolution cascade:
     * 1. Cross-reference → MATCHED
     * 2. Exact name match → MATCHED
     * 3. Fuzzy name match → SUGGESTED (with candidates)
     * 4. No match → UNMATCHED
     */
    private static void resolveEmployerRow(EntityManager em, ImportProvider provider,
                                            ImportRow row, Map<String, String> canonical) {
        String externalId = row.getExternalId();
        String employerName = canonical.getOrDefault("employer_name", "");

        // 1) Cross-reference
        if (!externalId.isEmpty()) {
            Integer internalId = ImportIdResolver.resolveInternalId(
                    em, provider.getId(), ImportIdResolver.EMPLOYER, externalId);
            if (internalId != null) {
                Employer er = em.find(Employer.class, internalId);
                if (er != null) {
                    setMatched(row, er.getId(), er.getEmployerName(), "xref", 1.0);
                    return;
                }
            }
        }

        // 2) Exact name match
        if (!employerName.isEmpty()) {
            List<Employer> exactMatches = em.createQuery(
                    "SELECT e FROM Employer e WHERE LOWER(e.employerName) = LOWER(:name)", Employer.class)
                    .setParameter("name", employerName)
                    .getResultList();
            if (exactMatches.size() == 1) {
                Employer match = exactMatches.get(0);
                setMatched(row, match.getId(), match.getEmployerName(), "exact_name", 1.0);
                return;
            }
        }

        // 3) Fuzzy name match
        if (!employerName.isEmpty()) {
            List<Employer> allEmployers = em.createQuery(
                    "SELECT e FROM Employer e", Employer.class).getResultList();
            List<MatchCandidate> candidates = new ArrayList<>();

            for (Employer er : allEmployers) {
                if (er.getEmployerName() == null) continue;
                double score = fuzzyScore(employerName, er.getEmployerName());
                if (score >= SUGGEST_THRESHOLD) {
                    candidates.add(new MatchCandidate(
                            er.getId(),
                            er.getEmployerName(),
                            "fuzzy",
                            score,
                            "ID: " + er.getId()
                    ));
                }
            }

            if (!candidates.isEmpty()) {
                candidates.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

                MatchCandidate best = candidates.get(0);
                if (best.getConfidence() >= MATCH_THRESHOLD) {
                    setMatched(row, best.getInternalId(), best.getDisplayLabel(), "fuzzy", best.getConfidence());
                    return;
                }

                row.setStatus("SUGGESTED");
                row.setAmsInternalId(best.getInternalId());
                row.setAmsDisplayLabel(best.getDisplayLabel());
                row.setMatchMethod("fuzzy");
                row.setMatchConfidence(best.getConfidence());
                row.setCandidates(candidates.size() > 5 ? candidates.subList(0, 5) : candidates);
                return;
            }
        }

        // 4) No match
        row.setStatus("UNMATCHED");
    }

    /**
     * Benefit resolution cascade:
     * 1. Cross-reference → MATCHED
     * 2. Employer + PlanType combo lookup → MATCHED/SUGGESTED
     * 3. If employer FK unresolvable → ERROR
     * 4. No match → UNMATCHED
     */
    private static void resolveBenefitRow(EntityManager em, ImportProvider provider,
                                           ImportRow row, Map<String, String> canonical,
                                           Map<String, String> fkMappings) {
        String externalId = row.getExternalId();

        // 1) Cross-reference
        if (!externalId.isEmpty()) {
            Integer internalId = ImportIdResolver.resolveInternalId(
                    em, provider.getId(), ImportIdResolver.BENEFIT, externalId);
            if (internalId != null) {
                Benefit ben = em.find(Benefit.class, internalId);
                if (ben != null) {
                    setMatched(row, ben.getId(), formatBenefitLabel(ben), "xref", 1.0);
                    return;
                }
            }
        }

        // Resolve FK: employer_id
        String employerExtId = canonical.getOrDefault("employer_id", "");
        Integer employerInternalId = null;
        if (!employerExtId.isEmpty()) {
            employerInternalId = ImportIdResolver.resolveInternalId(
                    em, provider.getId(), ImportIdResolver.EMPLOYER, employerExtId);
            if (employerInternalId == null) {
                // Try direct PK
                int directId = parseIntSafe(employerExtId);
                if (directId > 0 && ImportIdResolver.internalIdExists(em, ImportIdResolver.EMPLOYER, directId)) {
                    employerInternalId = directId;
                }
            }
        }

        // Resolve FK: plan_type_id
        String planTypeExtId = canonical.getOrDefault("plan_type_id", "");
        Integer planTypeInternalId = null;
        if (!planTypeExtId.isEmpty()) {
            planTypeInternalId = ImportIdResolver.resolveInternalId(
                    em, provider.getId(), ImportIdResolver.PLAN_TYPE, planTypeExtId);
            if (planTypeInternalId == null) {
                int directId = parseIntSafe(planTypeExtId);
                if (directId > 0) {
                    PlanType directPt = em.find(PlanType.class, directId);
                    if (directPt != null) planTypeInternalId = directId;
                }
            }
        }

        // 2) Search by employer + plan type combo
        if (employerInternalId != null && planTypeInternalId != null) {
            List<Benefit> matches = em.createQuery(
                    "SELECT b FROM Benefit b WHERE b.employer.id = :eid AND b.planType.planTypeId = :ptid",
                    Benefit.class)
                    .setParameter("eid", employerInternalId)
                    .setParameter("ptid", planTypeInternalId)
                    .getResultList();
            if (matches.size() == 1) {
                Benefit match = matches.get(0);
                setMatched(row, match.getId(), formatBenefitLabel(match), "employer_plantype", 1.0);
                return;
            }
            if (matches.size() > 1) {
                // Multiple benefits for same employer+planType — suggest all
                List<MatchCandidate> candidates = new ArrayList<>();
                for (Benefit b : matches) {
                    candidates.add(new MatchCandidate(
                            b.getId(), formatBenefitLabel(b), "employer_plantype", 0.85,
                            "Effective: " + (b.getEffectiveDate() != null ? b.getEffectiveDate().toString() : "—")));
                }
                row.setStatus("SUGGESTED");
                MatchCandidate best = candidates.get(0);
                row.setAmsInternalId(best.getInternalId());
                row.setAmsDisplayLabel(best.getDisplayLabel());
                row.setMatchMethod("employer_plantype");
                row.setMatchConfidence(0.85);
                row.setCandidates(candidates);
                return;
            }
        }

        // 3) If employer FK is required but unresolvable
        if (employerInternalId == null && !employerExtId.isEmpty()) {
            row.setStatus("ERROR");
            row.setErrorMessage("Employer '" + employerExtId + "' not resolved. Import employers first.");
            return;
        }

        // If employer resolved, try just by employer (with plan_name match)
        if (employerInternalId != null) {
            String planName = canonical.getOrDefault("plan_name", "");
            if (!planName.isEmpty()) {
                List<Benefit> erBenefits = em.createQuery(
                        "SELECT b FROM Benefit b WHERE b.employer.id = :eid", Benefit.class)
                        .setParameter("eid", employerInternalId)
                        .getResultList();
                List<MatchCandidate> candidates = new ArrayList<>();
                for (Benefit b : erBenefits) {
                    if (b.getPlanName() == null) continue;
                    double score = fuzzyScore(planName, b.getPlanName());
                    if (score >= SUGGEST_THRESHOLD) {
                        candidates.add(new MatchCandidate(
                                b.getId(), formatBenefitLabel(b), "fuzzy_plan_name", score,
                                "Plan: " + b.getPlanName()));
                    }
                }
                if (!candidates.isEmpty()) {
                    candidates.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
                    MatchCandidate best = candidates.get(0);
                    if (best.getConfidence() >= MATCH_THRESHOLD) {
                        setMatched(row, best.getInternalId(), best.getDisplayLabel(), "fuzzy_plan_name", best.getConfidence());
                        return;
                    }
                    row.setStatus("SUGGESTED");
                    row.setAmsInternalId(best.getInternalId());
                    row.setAmsDisplayLabel(best.getDisplayLabel());
                    row.setMatchMethod("fuzzy_plan_name");
                    row.setMatchConfidence(best.getConfidence());
                    row.setCandidates(candidates.size() > 5 ? candidates.subList(0, 5) : candidates);
                    return;
                }
            }
        }

        // 4) No match
        row.setStatus("UNMATCHED");
    }

    /**
     * Employee resolution cascade:
     * 1. Cross-reference → MATCHED
     * 2. First+Last name within employer scope → MATCHED (unique) / SUGGESTED (multiple)
     * 3. If employer FK unresolvable → ERROR
     * 4. No match → UNMATCHED
     */
    private static void resolveEmployeeRow(EntityManager em, ImportProvider provider,
                                            ImportRow row, Map<String, String> canonical,
                                            Map<String, String> fkMappings) {
        String externalId = row.getExternalId();

        // 1) Cross-reference
        if (!externalId.isEmpty()) {
            Integer internalId = ImportIdResolver.resolveInternalId(
                    em, provider.getId(), ImportIdResolver.EMPLOYEE, externalId);
            if (internalId != null) {
                Employee ee = em.find(Employee.class, internalId);
                if (ee != null) {
                    setMatched(row, ee.getId(), formatEmployeeLabel(ee), "xref", 1.0);
                    return;
                }
            }
        }

        // Resolve FK: employer_id
        String employerExtId = canonical.getOrDefault("employer_id", "");
        Integer employerInternalId = null;
        if (!employerExtId.isEmpty()) {
            employerInternalId = ImportIdResolver.resolveInternalId(
                    em, provider.getId(), ImportIdResolver.EMPLOYER, employerExtId);
            if (employerInternalId == null) {
                int directId = parseIntSafe(employerExtId);
                if (directId > 0 && ImportIdResolver.internalIdExists(em, ImportIdResolver.EMPLOYER, directId)) {
                    employerInternalId = directId;
                }
            }
        }

        String firstName = canonical.getOrDefault("first_name", "");
        String lastName = canonical.getOrDefault("last_name", "");

        // 2) Name match within employer scope
        if (employerInternalId != null && !firstName.isEmpty() && !lastName.isEmpty()) {
            List<Employee> nameMatches = em.createQuery(
                    "SELECT e FROM Employee e WHERE e.employer.id = :eid " +
                    "AND LOWER(e.firstName) = LOWER(:fn) AND LOWER(e.lastName) = LOWER(:ln)",
                    Employee.class)
                    .setParameter("eid", employerInternalId)
                    .setParameter("fn", firstName)
                    .setParameter("ln", lastName)
                    .getResultList();
            if (nameMatches.size() == 1) {
                Employee match = nameMatches.get(0);
                setMatched(row, match.getId(), formatEmployeeLabel(match), "exact_name", 1.0);
                return;
            }
            if (nameMatches.size() > 1) {
                List<MatchCandidate> candidates = new ArrayList<>();
                for (Employee e : nameMatches) {
                    candidates.add(new MatchCandidate(
                            e.getId(), formatEmployeeLabel(e), "exact_name", 0.90,
                            "Email: " + (e.getEmail() != null ? e.getEmail() : "—")));
                }
                row.setStatus("SUGGESTED");
                MatchCandidate best = candidates.get(0);
                row.setAmsInternalId(best.getInternalId());
                row.setAmsDisplayLabel(best.getDisplayLabel());
                row.setMatchMethod("name_multi");
                row.setMatchConfidence(0.90);
                row.setCandidates(candidates);
                return;
            }
        }

        // 3) Fuzzy name match within employer scope (if employer resolved)
        if (employerInternalId != null && !lastName.isEmpty()) {
            List<Employee> erEmployees = em.createQuery(
                    "SELECT e FROM Employee e WHERE e.employer.id = :eid", Employee.class)
                    .setParameter("eid", employerInternalId)
                    .getResultList();
            List<MatchCandidate> candidates = new ArrayList<>();
            String fullName = (firstName + " " + lastName).trim();
            for (Employee e : erEmployees) {
                String amsName = ((e.getFirstName() != null ? e.getFirstName() : "") + " " +
                        (e.getLastName() != null ? e.getLastName() : "")).trim();
                double score = fuzzyScore(fullName, amsName);
                if (score >= SUGGEST_THRESHOLD) {
                    candidates.add(new MatchCandidate(
                            e.getId(), formatEmployeeLabel(e), "fuzzy", score,
                            "Email: " + (e.getEmail() != null ? e.getEmail() : "—")));
                }
            }
            if (!candidates.isEmpty()) {
                candidates.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
                MatchCandidate best = candidates.get(0);
                if (best.getConfidence() >= MATCH_THRESHOLD) {
                    setMatched(row, best.getInternalId(), best.getDisplayLabel(), "fuzzy", best.getConfidence());
                    return;
                }
                row.setStatus("SUGGESTED");
                row.setAmsInternalId(best.getInternalId());
                row.setAmsDisplayLabel(best.getDisplayLabel());
                row.setMatchMethod("fuzzy");
                row.setMatchConfidence(best.getConfidence());
                row.setCandidates(candidates.size() > 5 ? candidates.subList(0, 5) : candidates);
                return;
            }
        }

        // 4) Employer not resolvable
        if (employerInternalId == null && !employerExtId.isEmpty()) {
            row.setStatus("ERROR");
            row.setErrorMessage("Employer '" + employerExtId + "' not resolved. Import employers first.");
            return;
        }

        // 5) No match
        row.setStatus("UNMATCHED");
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEARCH FOR MANUAL MATCHING (used in B3 AJAX)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Search AMS records by entity type and query string for manual linking.
     * Returns up to `limit` matching candidates.
     */
    public static List<MatchCandidate> searchAmsRecords(EntityManager em, String entityType,
                                                         String query, int limit) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        String q = "%" + query.trim().toLowerCase() + "%";
        List<MatchCandidate> results = new ArrayList<>();

        switch (entityType) {
            case "PLAN_TYPE" -> {
                List<PlanType> pts = em.createQuery(
                        "SELECT pt FROM PlanType pt WHERE LOWER(pt.planTypeName) LIKE :q OR LOWER(pt.code) LIKE :q",
                        PlanType.class)
                        .setParameter("q", q)
                        .setMaxResults(limit)
                        .getResultList();
                for (PlanType pt : pts) {
                    results.add(new MatchCandidate(pt.getPlanTypeId(), formatPlanTypeLabel(pt),
                            "search", 0, "Code: " + (pt.getCode() != null ? pt.getCode() : "—")));
                }
            }
            case "EMPLOYER" -> {
                List<Employer> ers = em.createQuery(
                        "SELECT e FROM Employer e WHERE LOWER(e.employerName) LIKE :q",
                        Employer.class)
                        .setParameter("q", q)
                        .setMaxResults(limit)
                        .getResultList();
                for (Employer er : ers) {
                    results.add(new MatchCandidate(er.getId(), er.getEmployerName(),
                            "search", 0, "ID: " + er.getId()));
                }
            }
            case "BENEFIT" -> {
                List<Benefit> bens = em.createQuery(
                        "SELECT b FROM Benefit b WHERE LOWER(b.planName) LIKE :q",
                        Benefit.class)
                        .setParameter("q", q)
                        .setMaxResults(limit)
                        .getResultList();
                for (Benefit b : bens) {
                    results.add(new MatchCandidate(b.getId(), formatBenefitLabel(b),
                            "search", 0, "Plan: " + (b.getPlanName() != null ? b.getPlanName() : "—")));
                }
            }
            case "EMPLOYEE" -> {
                List<Employee> ees = em.createQuery(
                        "SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE :q OR LOWER(e.lastName) LIKE :q",
                        Employee.class)
                        .setParameter("q", q)
                        .setMaxResults(limit)
                        .getResultList();
                for (Employee e : ees) {
                    results.add(new MatchCandidate(e.getId(), formatEmployeeLabel(e),
                            "search", 0, "Email: " + (e.getEmail() != null ? e.getEmail() : "—")));
                }
            }
        }

        return results;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FK MAPPING LOADER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Load FK field mappings for a file type.
     * Returns map of fk_entity_type → canonical_field (e.g., "EMPLOYER" → "employer_id").
     */
    public static Map<String, String> loadFkMappings(EntityManager em, int fileTypeId) {
        List<ImportFieldMapping> fkFields = em.createQuery(
                "SELECT m FROM ImportFieldMapping m WHERE m.fileType.id = :ftId AND m.fk = true",
                ImportFieldMapping.class)
                .setParameter("ftId", fileTypeId)
                .getResultList();
        Map<String, String> result = new LinkedHashMap<>();
        for (ImportFieldMapping m : fkFields) {
            if (m.getFkEntityType() != null) {
                result.put(m.getFkEntityType(), m.getCanonicalField());
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FUZZY MATCHING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Compute Levenshtein-based similarity score between two strings.
     * Strips corporate suffixes before comparison. Returns 0.0 to 1.0.
     */
    public static double fuzzyScore(String a, String b) {
        if (a == null || b == null) return 0.0;

        String normA = stripCorpSuffix(a.trim().toLowerCase());
        String normB = stripCorpSuffix(b.trim().toLowerCase());

        if (normA.equals(normB)) return 1.0;
        if (normA.isEmpty() || normB.isEmpty()) return 0.0;

        int distance = levenshteinDistance(normA, normB);
        int maxLen = Math.max(normA.length(), normB.length());
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Standard Levenshtein distance.
     */
    private static int levenshteinDistance(String s, String t) {
        int m = s.length(), n = t.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int j = 0; j <= n; j++) prev[j] = j;

        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = s.charAt(i - 1) == t.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[n];
    }

    /**
     * Strip common corporate suffixes for better name comparison.
     */
    private static String stripCorpSuffix(String name) {
        String lower = name.toLowerCase();
        for (String suffix : CORP_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length()).trim();
            }
        }
        return name;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static void setMatched(ImportRow row, int internalId, String label,
                                    String method, double confidence) {
        row.setStatus("MATCHED");
        row.setAmsInternalId(internalId);
        row.setAmsDisplayLabel(label);
        row.setMatchMethod(method);
        row.setMatchConfidence(confidence);
    }

    private static String findPkField(Map<String, FieldMappingInfo> mappings) {
        for (Map.Entry<String, FieldMappingInfo> entry : mappings.entrySet()) {
            if (entry.getValue().isKey()) return entry.getKey();
        }
        // Fallback: common PK field names
        for (String candidate : List.of("plan_type_id", "employer_id", "employee_id", "benefit_id")) {
            if (mappings.containsKey(candidate)) return candidate;
        }
        return null;
    }

    private static String generateDisplayLabel(Map<String, String> canonical, String entityType) {
        return switch (entityType) {
            case "PLAN_TYPE" -> {
                String code = canonical.getOrDefault("code", "");
                String name = canonical.getOrDefault("name", "");
                yield code.isEmpty() ? name : code + " — " + name;
            }
            case "EMPLOYER" -> canonical.getOrDefault("employer_name", "");
            case "BENEFIT" -> {
                String planName = canonical.getOrDefault("plan_name", "");
                String erName = canonical.getOrDefault("employer_name", "");
                // If no employer_name in benefit file, show employer_id
                if (erName.isEmpty()) {
                    String erId = canonical.getOrDefault("employer_id", "");
                    erName = erId.isEmpty() ? "" : "ER:" + erId;
                }
                yield erName.isEmpty() ? planName : planName + " (" + erName + ")";
            }
            case "EMPLOYEE" -> {
                String first = canonical.getOrDefault("first_name", "");
                String last = canonical.getOrDefault("last_name", "");
                yield (first + " " + last).trim();
            }
            default -> "";
        };
    }

    private static String formatPlanTypeLabel(PlanType pt) {
        String code = pt.getCode() != null ? pt.getCode() : "";
        String name = pt.getPlanTypeName() != null ? pt.getPlanTypeName() : "";
        return code.isEmpty() ? name : code + " — " + name;
    }

    private static String formatBenefitLabel(Benefit b) {
        String planName = b.getPlanName() != null ? b.getPlanName() : "Benefit #" + b.getId();
        String erName = b.getEmployer() != null && b.getEmployer().getEmployerName() != null
                ? b.getEmployer().getEmployerName() : "";
        return erName.isEmpty() ? planName : planName + " (" + erName + ")";
    }

    private static String formatEmployeeLabel(Employee e) {
        String first = e.getFirstName() != null ? e.getFirstName() : "";
        String last = e.getLastName() != null ? e.getLastName() : "";
        return (first + " " + last).trim();
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.imports.ImportFieldMapping;
import net.superiorstate.ams.model.imports.ImportFileType;
import net.superiorstate.ams.model.imports.ImportProvider;
import net.superiorstate.ams.data.resolver.ImportIdResolver;
import net.superiorstate.ams.model.summit.archive.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Provider-agnostic import engine for AMS.
 * Reads CSV/Excel files using stored column mappings (import_field_mapping)
 * and upserts into the existing archive tables (employer, employee, benefit, plantype).
 *
 * Uses the same upsert pattern as SummitImportService: INSERT if new, UPDATE if changed, SKIP if unchanged.
 */
public class UniversalImportService {

    // ═══════════════════════════════════════════════════════════════
    //  FIELD MAPPING INFO — in-memory representation of a mapping row
    // ═══════════════════════════════════════════════════════════════

    public static class FieldMappingInfo {
        private final String sourceColumn;
        private final String canonicalField;
        private final boolean required;
        private final boolean key;
        private final String transformRule;

        public FieldMappingInfo(String sourceColumn, String canonicalField,
                                boolean required, boolean key, String transformRule) {
            this.sourceColumn = sourceColumn;
            this.canonicalField = canonicalField;
            this.required = required;
            this.key = key;
            this.transformRule = transformRule;
        }

        public String getSourceColumn() { return sourceColumn; }
        public String getCanonicalField() { return canonicalField; }
        public boolean isRequired() { return required; }
        public boolean isKey() { return key; }
        public String getTransformRule() { return transformRule; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  IMPORT RESULT — same pattern as SummitImportService
    // ═══════════════════════════════════════════════════════════════

    public static class ImportResult {
        private int inserted = 0;
        private int updated = 0;
        private int skipped = 0;
        private int errors = 0;
        private int serviceItemsCreated = 0;
        private List<String> warnings = new ArrayList<>();

        public int getInserted() { return inserted; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public int getErrors() { return errors; }
        public int getServiceItemsCreated() { return serviceItemsCreated; }
        public List<String> getWarnings() { return warnings; }

        public void addInserted() { inserted++; }
        public void addUpdated() { updated++; }
        public void addSkipped() { skipped++; }
        public void addError() { errors++; }
        public void addError(String warning) { errors++; warnings.add(warning); }
        public void addWarning(String warning) { warnings.add(warning); }
        public void addServiceItemCreated() { serviceItemsCreated++; }

        public int total() { return inserted + updated + skipped + errors; }

        public String summary() {
            return String.format("%d inserted, %d updated, %d unchanged, %d errors",
                    inserted, updated, skipped, errors);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FILE PARSING
    // ═══════════════════════════════════════════════════════════════

    /** Parse a CSV file into List<Map<String,String>> where keys are lowercased column names. */
    public static List<Map<String, String>> parseCsvFile(File file) throws IOException {
        return SummitImportService.parseCsv(file);
    }

    /** Parse an Excel file into List<Map<String,String>> with lowercased column names. */
    public static List<Map<String, String>> parseExcelFile(File file) throws Exception {
        List<Map<String, String>> results = new ArrayList<>();
        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(file)) {
            workbook = file.getName().toLowerCase().endsWith(".xls")
                    ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
        }
        try {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return results;

            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                headers[i] = cell != null ? getCellString(cell).trim().toLowerCase() : "col_" + i;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = row.getCell(c);
                    map.put(headers[c], cell != null ? getCellString(cell).trim() : "");
                }
                results.add(map);
            }
        } finally {
            workbook.close();
        }
        return results;
    }

    /** Parse a file based on format (CSV, EXCEL, TSV). */
    public static List<Map<String, String>> parseFile(File file, String format) throws Exception {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return parseExcelFile(file);
        }
        // CSV and TSV both handled by CSV parser (TSV is tab-delimited but parseCsv handles it)
        return parseCsvFile(file);
    }

    /** Get file headers for display/mapping. */
    public static List<String> getFileHeaders(File file, String format) throws Exception {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return SummitImportService.getExcelHeaders(file);
        }
        return SummitImportService.getCsvHeaders(file);
    }

    /** Count data rows in a file. */
    public static int countFileRows(File file, String format) throws Exception {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return SummitImportService.countExcelRows(file);
        }
        return SummitImportService.countCsvRows(file);
    }

    // ═══════════════════════════════════════════════════════════════
    //  COLUMN MAPPING RESOLUTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Load field mappings for a given file type from the database.
     * Returns a map of canonical_field → FieldMappingInfo.
     */
    public static Map<String, FieldMappingInfo> loadFieldMappings(EntityManager em, int fileTypeId) {
        List<ImportFieldMapping> mappings = em.createQuery(
                "SELECT m FROM ImportFieldMapping m WHERE m.fileType.id = :ftId", ImportFieldMapping.class)
                .setParameter("ftId", fileTypeId)
                .getResultList();

        Map<String, FieldMappingInfo> result = new LinkedHashMap<>();
        for (ImportFieldMapping m : mappings) {
            result.put(m.getCanonicalField(), new FieldMappingInfo(
                    m.getSourceColumn().toLowerCase(),
                    m.getCanonicalField(),
                    m.isRequired(),
                    m.isKey(),
                    m.getTransformRule()));
        }
        return result;
    }

    /**
     * Extract a canonical field value from a CSV row using the stored mapping.
     * Applies transform_rule if present.
     * Returns empty string if unmapped or missing.
     */
    public static String getCanonicalValue(Map<String, String> row,
                                            Map<String, FieldMappingInfo> mappings,
                                            String canonicalField) {
        FieldMappingInfo info = mappings.get(canonicalField);
        if (info == null) return "";

        String raw = row.getOrDefault(info.getSourceColumn(), "").trim();
        return applyTransform(raw, info.getTransformRule());
    }

    /**
     * Apply a transform rule to a raw value.
     */
    private static String applyTransform(String raw, String rule) {
        if (rule == null || rule.isEmpty() || raw.isEmpty()) return raw;

        if ("UPPERCASE".equals(rule)) return raw.toUpperCase();
        if ("LOWERCASE".equals(rule)) return raw.toLowerCase();
        if ("INT".equals(rule)) return String.valueOf(parseIntSafe(raw));

        if (rule.startsWith("DATE:")) {
            String pattern = rule.substring(5);
            try {
                LocalDate ld = LocalDate.parse(raw, DateTimeFormatter.ofPattern(pattern));
                return ld.toString(); // ISO format for internal use
            } catch (DateTimeParseException e) {
                return raw; // can't parse, return as-is
            }
        }

        if (rule.startsWith("MAP:")) {
            String mappingStr = rule.substring(4);
            for (String pair : mappingStr.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(raw)) {
                    return kv[1].trim();
                }
            }
            return raw; // no match, return as-is
        }

        if (rule.startsWith("BOOLEAN:")) {
            String truthyList = rule.substring(8);
            for (String truthy : truthyList.split(",")) {
                if (truthy.trim().equalsIgnoreCase(raw)) return "true";
            }
            return "false";
        }

        return raw;
    }

    // ═══════════════════════════════════════════════════════════════
    //  AUTO-DETECT COLUMN MAPPINGS
    // ═══════════════════════════════════════════════════════════════

    /** Known aliases for auto-detecting canonical field names from CSV headers. */
    private static final Map<String, Map<String, List<String>>> ALIASES = new HashMap<>();
    static {
        // EMPLOYER aliases
        Map<String, List<String>> employer = new LinkedHashMap<>();
        employer.put("employer_id", List.of("organizationid", "organization_id", "employer_id", "org_id", "company_id", "id"));
        employer.put("employer_name", List.of("employername", "employer", "company", "organization", "org_name", "name"));
        employer.put("contact_name", List.of("primarycontact", "contact", "contact_name", "contactname"));
        employer.put("email", List.of("email", "emailaddress", "email_address", "contact_email"));
        employer.put("phone", List.of("phone", "phonenumber", "phone_number", "telephone"));
        employer.put("alt_id", List.of("employer_id", "employerorganizationid", "alt_id", "altid"));
        employer.put("er_key", List.of("er_key", "erkey", "customid", "custom_id"));
        employer.put("status", List.of("status", "organizationstatus", "active", "is_active"));
        ALIASES.put("EMPLOYER", employer);

        // EMPLOYEE aliases
        Map<String, List<String>> employee = new LinkedHashMap<>();
        employee.put("employee_id", List.of("participant_id", "participantid", "employee_id", "employeeid", "member_id", "id"));
        employee.put("employer_id", List.of("organization_id", "organizationid", "employer_id", "company_id"));
        employee.put("first_name", List.of("firstname", "first_name", "fname", "first"));
        employee.put("last_name", List.of("lastname", "last_name", "lname", "last"));
        employee.put("email", List.of("email", "emailaddress", "email_address", "participant_email"));
        employee.put("hr_email", List.of("hr_email", "hremail", "hr_email_address"));
        employee.put("address1", List.of("address1", "address", "street", "street_address"));
        employee.put("address2", List.of("address2", "apt", "suite", "unit"));
        employee.put("city", List.of("city", "town"));
        employee.put("state", List.of("state", "province", "region"));
        employee.put("zip", List.of("zipcode", "zip", "zip_code", "postal", "postalcode"));
        employee.put("custom_id", List.of("participantcustomid", "custom_id", "customid"));
        employee.put("user_id", List.of("user_id", "userid", "username"));
        employee.put("status", List.of("status", "participantstatus", "active", "is_active"));
        ALIASES.put("EMPLOYEE", employee);

        // BENEFIT aliases
        Map<String, List<String>> benefit = new LinkedHashMap<>();
        benefit.put("benefit_id", List.of("employerplan_id", "benefit_id", "plan_id", "benefitid"));
        benefit.put("employer_id", List.of("organizationid", "organization_id", "employer_id"));
        benefit.put("plan_type_id", List.of("plantypeid", "plan_type_id", "plantype_id", "type_id", "plantypecode"));
        benefit.put("plan_name", List.of("planname", "plan_name", "benefitname", "benefit_name", "name"));
        benefit.put("plan_description", List.of("plandescription", "plan_description", "description"));
        benefit.put("effective_date", List.of("effectivedate", "effective_date", "start_date", "startdate"));
        benefit.put("termination_date", List.of("terminationdate", "termination_date", "end_date", "enddate"));
        benefit.put("status", List.of("planstatus", "status", "active", "is_active"));
        ALIASES.put("BENEFIT", benefit);

        // PLAN_TYPE aliases
        Map<String, List<String>> planType = new LinkedHashMap<>();
        planType.put("plan_type_id", List.of("plantypeid", "plan_type_id", "plantype_id", "id", "plan type id"));
        planType.put("code", List.of("code", "plantypecode", "plan_type_code", "plan type code"));
        planType.put("name", List.of("plantypename", "plan_type_name", "name", "plan type name"));
        planType.put("level", List.of("level", "planlevel"));
        planType.put("line_of_service", List.of("los", "line_of_service", "lineofservice"));
        ALIASES.put("PLAN_TYPE", planType);
    }

    /**
     * Auto-detect column mappings by comparing CSV headers against known aliases.
     * Returns suggested ImportFieldMapping objects (not persisted).
     */
    public static List<ImportFieldMapping> autoDetectMappings(List<String> csvHeaders, String targetEntity) {
        List<ImportFieldMapping> suggestions = new ArrayList<>();

        Map<String, List<String>> entityAliases = ALIASES.get(targetEntity);
        if (entityAliases == null) return suggestions;

        // Normalize headers: lowercase, strip spaces and underscores for fuzzy matching
        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        for (String h : csvHeaders) {
            String normalized = h.toLowerCase().replaceAll("[\\s_]", "");
            normalizedToOriginal.put(normalized, h);
        }

        for (Map.Entry<String, List<String>> entry : entityAliases.entrySet()) {
            String canonical = entry.getKey();
            List<String> aliases = entry.getValue();

            // Try each alias against normalized headers
            for (String alias : aliases) {
                String normalizedAlias = alias.replaceAll("[\\s_]", "");
                if (normalizedToOriginal.containsKey(normalizedAlias)) {
                    ImportFieldMapping mapping = new ImportFieldMapping();
                    mapping.setSourceColumn(normalizedToOriginal.get(normalizedAlias));
                    mapping.setCanonicalField(canonical);
                    // Mark key fields
                    if (canonical.equals("employer_id") && "EMPLOYER".equals(targetEntity)) mapping.setKey(true);
                    if (canonical.equals("employee_id") && "EMPLOYEE".equals(targetEntity)) mapping.setKey(true);
                    if (canonical.equals("benefit_id") && "BENEFIT".equals(targetEntity)) mapping.setKey(true);
                    if (canonical.equals("plan_type_id") && "PLAN_TYPE".equals(targetEntity)) mapping.setKey(true);
                    // Mark required fields
                    if (mapping.isKey()) mapping.setRequired(true);
                    if (canonical.equals("employer_name")) mapping.setRequired(true);
                    if (canonical.equals("first_name") || canonical.equals("last_name")) mapping.setRequired(true);
                    if (canonical.equals("plan_name")) mapping.setRequired(true);
                    if (canonical.equals("code") || canonical.equals("name")) mapping.setRequired(true);
                    suggestions.add(mapping);
                    break; // first match wins
                }
            }
        }

        return suggestions;
    }

    // ═══════════════════════════════════════════════════════════════
    //  IMPORT: PLAN TYPES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Import plan types from uploaded file(s) using stored column mappings.
     * Creates ServiceItems for new plan types (same pattern as SummitImportService).
     */
    public static ImportResult importPlanTypes(EntityManager em, List<File> files,
                                                ImportProvider provider,
                                                Map<Integer, Integer> renewalMonthsMap) throws Exception {
        ImportResult result = new ImportResult();

        // Load file types for this provider targeting PLAN_TYPE
        List<ImportFileType> fileTypes = getFileTypesForEntity(em, provider, "PLAN_TYPE");
        if (fileTypes.isEmpty()) {
            result.addError("No PLAN_TYPE file types configured for provider " + provider.getProviderName());
            return result;
        }

        // Reference entities
        BillingGroup defaultBg = getOrCreateDefaultBillingGroup(em);
        ActivityCategory renewalCategory = em.find(ActivityCategory.class, 1);
        PSP psp = em.find(PSP.class, provider.getPspId());

        if (renewalCategory == null) {
            result.addError("ActivityCategory ID 1 (Renewal) not found — cannot create ServiceItems.");
            return result;
        }

        for (int fi = 0; fi < files.size() && fi < fileTypes.size(); fi++) {
            File file = files.get(fi);
            ImportFileType fileType = fileTypes.get(fi);
            Map<String, FieldMappingInfo> mappings = loadFieldMappings(em, fileType.getId());

            if (!mappings.containsKey("plan_type_id") || !mappings.containsKey("name")) {
                result.addWarning("File '" + fileType.getFileLabel() + "': missing required mappings (plan_type_id, name). Skipping.");
                continue;
            }

            List<Map<String, String>> rows = parseFile(file, fileType.getFileFormat());

            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);

                int ptId = parseIntSafe(getCanonicalValue(row, mappings, "plan_type_id"));
                if (ptId == 0) {
                    result.addError("Row " + (i + 2) + ": invalid plan_type_id, skipping.");
                    continue;
                }

                String name = getCanonicalValue(row, mappings, "name");
                String code = getCanonicalValue(row, mappings, "code");
                String level = blankToNull(getCanonicalValue(row, mappings, "level"));
                String los = blankToNull(getCanonicalValue(row, mappings, "line_of_service"));

                if (name.isEmpty()) {
                    result.addError("Row " + (i + 2) + ": empty plan type name for ID " + ptId + ", skipping.");
                    continue;
                }
                if (code.isEmpty()) code = name; // fallback

                String externalPtId = String.valueOf(ptId);

                // Cascading plan type resolution: identity → code → name → none
                String[] matchInfo = new String[1];
                PlanType existing = ImportIdResolver.resolvePlanType(em, provider, externalPtId, code, name, matchInfo);

                // Fall back to direct PK lookup for backward compatibility
                if (existing == null && "none".equals(matchInfo[0])) {
                    existing = em.find(PlanType.class, ptId);
                    if (existing != null) matchInfo[0] = "direct-pk";
                }

                if (existing != null) {
                    boolean changed = false;
                    if (!name.equals(existing.getPlanTypeName())) { existing.setPlanTypeName(name); changed = true; }
                    if (!code.equals(existing.getCode())) { existing.setCode(code); changed = true; }
                    if (!Objects.equals(level, existing.getLevel())) { existing.setLevel(level); changed = true; }
                    if (!Objects.equals(los, existing.getLos())) { existing.setLos(los); changed = true; }

                    if (changed) {
                        em.getTransaction().begin();
                        em.merge(existing);
                        ImportIdResolver.recordMapping(em, provider, ImportIdResolver.PLAN_TYPE,
                                externalPtId, existing.getPlanTypeId(), true);
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        result.addSkipped();
                    }

                    // Ensure ServiceItem exists
                    if (existing.getServiceItem() == null) {
                        int renewalMonths = getRenewalMonths(renewalMonthsMap, existing.getPlanTypeId());
                        ServiceItem si = createRenewalServiceItem(em, name, code, existing.getPlanTypeId(), renewalMonths,
                                renewalCategory, psp, provider.getProviderCode());
                        em.getTransaction().begin();
                        existing.setServiceItem(si);
                        em.merge(existing);
                        em.getTransaction().commit();
                        result.addServiceItemCreated();
                    }
                } else {
                    // No match — check if external ID conflicts with existing internal PK
                    int internalPtId = ptId;
                    if (ImportIdResolver.internalIdExists(em, ImportIdResolver.PLAN_TYPE, ptId)) {
                        internalPtId = ImportIdResolver.allocateInternalId(em, ImportIdResolver.PLAN_TYPE);
                        result.addWarning("PlanType ID " + ptId + " conflicts, allocated " + internalPtId);
                    }

                    int renewalMonths = getRenewalMonths(renewalMonthsMap, internalPtId);
                    em.getTransaction().begin();
                    ServiceItem si = createRenewalServiceItem(em, name, code, internalPtId, renewalMonths,
                            renewalCategory, psp, provider.getProviderCode());
                    result.addServiceItemCreated();

                    PlanType pt = new PlanType();
                    pt.setPlanTypeId(internalPtId);
                    pt.setCode(code);
                    pt.setPlanTypeName(name);
                    pt.setLevel(level);
                    pt.setLos(los);
                    pt.setBillingGroup(defaultBg);
                    pt.setServiceItem(si);
                    em.persist(pt);
                    ImportIdResolver.recordMapping(em, provider, ImportIdResolver.PLAN_TYPE,
                            externalPtId, internalPtId, true);
                    em.getTransaction().commit();
                    result.addInserted();
                }

                if (result.total() % 50 == 0) {
                    em.clear();
                    defaultBg = getOrCreateDefaultBillingGroup(em);
                    renewalCategory = em.find(ActivityCategory.class, 1);
                    psp = em.find(PSP.class, provider.getPspId());
                }
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  IMPORT: EMPLOYERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Import employers from uploaded file(s) using stored column mappings.
     * Upsert by external employer_id → employer.organization_id.
     */
    public static ImportResult importEmployers(EntityManager em, List<File> files,
                                                ImportProvider provider) throws Exception {
        ImportResult result = new ImportResult();

        List<ImportFileType> fileTypes = getFileTypesForEntity(em, provider, "EMPLOYER");
        if (fileTypes.isEmpty()) {
            result.addError("No EMPLOYER file types configured for provider " + provider.getProviderName());
            return result;
        }

        for (int fi = 0; fi < files.size() && fi < fileTypes.size(); fi++) {
            File file = files.get(fi);
            ImportFileType fileType = fileTypes.get(fi);
            Map<String, FieldMappingInfo> mappings = loadFieldMappings(em, fileType.getId());

            if (!mappings.containsKey("employer_id") || !mappings.containsKey("employer_name")) {
                result.addWarning("File '" + fileType.getFileLabel() + "': missing required mappings. Skipping.");
                continue;
            }

            List<Map<String, String>> rows = parseFile(file, fileType.getFileFormat());

            for (Map<String, String> row : rows) {
                int orgId = parseIntSafe(getCanonicalValue(row, mappings, "employer_id"));
                if (orgId == 0) { result.addError(); continue; }

                String employerName = getCanonicalValue(row, mappings, "employer_name");
                if (employerName.isEmpty()) {
                    result.addError("Empty employer name for ID " + orgId + ", skipping.");
                    continue;
                }

                // Status filter
                String status = getCanonicalValue(row, mappings, "status");
                if (status.equalsIgnoreCase("Inactive") || status.equalsIgnoreCase("Closed")
                        || status.equalsIgnoreCase("false")) {
                    result.addSkipped();
                    continue;
                }

                String contactName = getCanonicalValue(row, mappings, "contact_name");
                String email = getCanonicalValue(row, mappings, "email");
                String phone = getCanonicalValue(row, mappings, "phone");
                int altId = parseIntSafe(getCanonicalValue(row, mappings, "alt_id"));
                int erKey = parseIntSafe(getCanonicalValue(row, mappings, "er_key"));

                String externalOrgId = String.valueOf(orgId);

                // Resolve via cross-reference, then fall back to direct PK
                Employer existing = ImportIdResolver.resolveEntity(em, Employer.class,
                        provider.getId(), ImportIdResolver.EMPLOYER, externalOrgId);
                if (existing == null) existing = em.find(Employer.class, orgId);

                if (existing != null) {
                    boolean changed = false;
                    if (!employerName.equals(existing.getEmployerName())) { existing.setEmployerName(employerName); changed = true; }
                    if (!Objects.equals(blankToNull(email), blankToNull(existing.getEmail()))) { existing.setEmail(email); changed = true; }
                    if (!Objects.equals(blankToNull(phone), blankToNull(existing.getPhone()))) { existing.setPhone(phone); changed = true; }
                    if (!Objects.equals(blankToNull(contactName), blankToNull(existing.getContactName()))) { existing.setContactName(contactName); changed = true; }

                    if (changed) {
                        em.getTransaction().begin();
                        em.merge(existing);
                        ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYER,
                                externalOrgId, existing.getId(), true);
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        result.addSkipped();
                    }
                } else {
                    // No existing record — check PK conflict
                    int internalOrgId = orgId;
                    if (ImportIdResolver.internalIdExists(em, ImportIdResolver.EMPLOYER, orgId)) {
                        internalOrgId = ImportIdResolver.allocateInternalId(em, ImportIdResolver.EMPLOYER);
                        result.addWarning("Employer ID " + orgId + " conflicts, allocated " + internalOrgId);
                    }

                    em.getTransaction().begin();
                    Employer er = new Employer();
                    er.setId(internalOrgId);
                    er.setEmployerName(employerName);
                    er.setAltId(altId);
                    er.setErKey(erKey);
                    er.setEmail(email);
                    er.setPhone(phone);
                    er.setContactName(contactName);
                    er.setActive(true);
                    em.persist(er);
                    ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYER,
                            externalOrgId, internalOrgId, true);
                    em.getTransaction().commit();
                    result.addInserted();
                }

                if (result.total() % 50 == 0) em.clear();
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  IMPORT: EMPLOYEES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Import employees from uploaded file(s) using stored column mappings.
     * If multiple files map to EMPLOYEE, merge by employee_id.
     * Validates employer FK exists.
     */
    public static ImportResult importEmployees(EntityManager em, List<File> files,
                                                ImportProvider provider) throws Exception {
        ImportResult result = new ImportResult();

        List<ImportFileType> fileTypes = getFileTypesForEntity(em, provider, "EMPLOYEE");
        if (fileTypes.isEmpty()) {
            result.addError("No EMPLOYEE file types configured for provider " + provider.getProviderName());
            return result;
        }

        // Parse all employee files and merge rows by key field (employee_id)
        Map<Integer, Map<String, String>> mergedByKey = new LinkedHashMap<>();

        for (int fi = 0; fi < files.size() && fi < fileTypes.size(); fi++) {
            File file = files.get(fi);
            ImportFileType fileType = fileTypes.get(fi);
            Map<String, FieldMappingInfo> mappings = loadFieldMappings(em, fileType.getId());

            if (!mappings.containsKey("employee_id")) {
                result.addWarning("File '" + fileType.getFileLabel() + "': missing employee_id mapping. Skipping.");
                continue;
            }

            List<Map<String, String>> rows = parseFile(file, fileType.getFileFormat());

            for (Map<String, String> row : rows) {
                // Resolve canonical values for this row
                Map<String, String> canonicalRow = new LinkedHashMap<>();
                for (String canonicalField : mappings.keySet()) {
                    String value = getCanonicalValue(row, mappings, canonicalField);
                    if (!value.isEmpty()) {
                        canonicalRow.put(canonicalField, value);
                    }
                }

                int empId = parseIntSafe(canonicalRow.getOrDefault("employee_id", "0"));
                if (empId == 0) continue;

                // Merge: later files fill in fields that the first file left empty
                Map<String, String> existing = mergedByKey.get(empId);
                if (existing == null) {
                    mergedByKey.put(empId, canonicalRow);
                } else {
                    for (Map.Entry<String, String> e : canonicalRow.entrySet()) {
                        existing.putIfAbsent(e.getKey(), e.getValue());
                    }
                }
            }
        }

        // Now upsert the merged employee records
        for (Map.Entry<Integer, Map<String, String>> entry : mergedByKey.entrySet()) {
            int empId = entry.getKey();
            Map<String, String> data = entry.getValue();

            int orgId = parseIntSafe(data.getOrDefault("employer_id", "0"));
            if (orgId == 0) {
                result.addError("Employee " + empId + ": no employer_id, skipping.");
                continue;
            }

            // Resolve employer via cross-reference, then fall back to direct PK
            Employer employer = ImportIdResolver.resolveEntity(em, Employer.class,
                    provider.getId(), ImportIdResolver.EMPLOYER, String.valueOf(orgId));
            if (employer == null) employer = em.find(Employer.class, orgId);
            if (employer == null) {
                result.addError("Employee " + empId + ": employer " + orgId + " not found, skipping.");
                continue;
            }

            String firstName = data.getOrDefault("first_name", "");
            String lastName = data.getOrDefault("last_name", "");
            String email = data.getOrDefault("email", "");
            String hrEmail = data.getOrDefault("hr_email", "");
            String address1 = data.getOrDefault("address1", "");
            String address2 = data.getOrDefault("address2", "");
            String city = data.getOrDefault("city", "");
            String state = data.getOrDefault("state", "");
            String zip = data.getOrDefault("zip", "");
            String customId = data.getOrDefault("custom_id", "");
            String userId = data.getOrDefault("user_id", "");
            String statusStr = data.getOrDefault("status", "");

            boolean isActive = !statusStr.equalsIgnoreCase("Inactive")
                    && !statusStr.equalsIgnoreCase("false")
                    && !statusStr.equals("2"); // Summit convention: 2=Inactive

            String externalEmpId = String.valueOf(empId);

            // Resolve via cross-reference, then fall back to direct PK
            Employee existingEe = ImportIdResolver.resolveEntity(em, Employee.class,
                    provider.getId(), ImportIdResolver.EMPLOYEE, externalEmpId);
            if (existingEe == null) existingEe = em.find(Employee.class, empId);

            if (existingEe != null) {
                boolean changed = false;
                if (!firstName.isEmpty() && !firstName.equals(existingEe.getFirstName())) { existingEe.setFirstName(firstName); changed = true; }
                if (!lastName.isEmpty() && !lastName.equals(existingEe.getLastName())) { existingEe.setLastName(lastName); changed = true; }
                if (!email.isEmpty() && !Objects.equals(email, existingEe.getEmail())) { existingEe.setEmail(email); changed = true; }
                if (!hrEmail.isEmpty() && !Objects.equals(hrEmail, existingEe.getHrEmail())) { existingEe.setHrEmail(hrEmail); changed = true; }
                if (!address1.isEmpty() && !Objects.equals(address1, existingEe.getAddress1())) { existingEe.setAddress1(address1); changed = true; }
                if (!address2.isEmpty() && !Objects.equals(address2, existingEe.getAddress2())) { existingEe.setAddress2(address2); changed = true; }
                if (!city.isEmpty() && !Objects.equals(city, existingEe.getCity())) { existingEe.setCity(city); changed = true; }
                if (!state.isEmpty() && !Objects.equals(state, existingEe.getState())) { existingEe.setState(state); changed = true; }
                if (!zip.isEmpty() && !Objects.equals(zip, existingEe.getZipCode())) { existingEe.setZipCode(zip); changed = true; }
                if (existingEe.isActive() != isActive) { existingEe.setActive(isActive); changed = true; }

                if (changed) {
                    em.getTransaction().begin();
                    em.merge(existingEe);
                    ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYEE,
                            externalEmpId, existingEe.getId(), true);
                    em.getTransaction().commit();
                    result.addUpdated();
                } else {
                    result.addSkipped();
                }
            } else {
                // No existing record — check PK conflict
                int internalEmpId = empId;
                if (ImportIdResolver.internalIdExists(em, ImportIdResolver.EMPLOYEE, empId)) {
                    internalEmpId = ImportIdResolver.allocateInternalId(em, ImportIdResolver.EMPLOYEE);
                    result.addWarning("Employee ID " + empId + " conflicts, allocated " + internalEmpId);
                }

                em.getTransaction().begin();
                Employee ee = new Employee();
                ee.setId(internalEmpId);
                ee.setEmployer(employer);
                ee.setFirstName(firstName.isEmpty() ? "Unknown" : firstName);
                ee.setLastName(lastName.isEmpty() ? "Unknown" : lastName);
                ee.setEmail(email);
                ee.setHrEmail(hrEmail);
                ee.setAddress1(address1);
                ee.setAddress2(address2);
                ee.setCity(city);
                ee.setState(state);
                ee.setZipCode(zip);
                ee.setCustomId(customId);
                ee.setUserId(userId);
                ee.setActive(isActive);
                em.persist(ee);
                ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYEE,
                        externalEmpId, internalEmpId, true);
                em.getTransaction().commit();
                result.addInserted();
            }

            if (result.total() % 50 == 0) em.clear();
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  IMPORT: BENEFITS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Import benefits from uploaded file(s) using stored column mappings.
     * Creates Benefit records with source_type = provider.providerCode.
     * Validates employer FK and plan_type FK.
     * Sets renewal dates based on renewalMonthsMap config.
     */
    public static ImportResult importBenefits(EntityManager em, List<File> files,
                                               ImportProvider provider,
                                               Map<Integer, Integer> renewalMonthsMap) throws Exception {
        ImportResult result = new ImportResult();

        List<ImportFileType> fileTypes = getFileTypesForEntity(em, provider, "BENEFIT");
        if (fileTypes.isEmpty()) {
            result.addError("No BENEFIT file types configured for provider " + provider.getProviderName());
            return result;
        }

        String sourceType = provider.getProviderCode();

        for (int fi = 0; fi < files.size() && fi < fileTypes.size(); fi++) {
            File file = files.get(fi);
            ImportFileType fileType = fileTypes.get(fi);
            Map<String, FieldMappingInfo> mappings = loadFieldMappings(em, fileType.getId());

            if (!mappings.containsKey("benefit_id") || !mappings.containsKey("employer_id") || !mappings.containsKey("plan_type_id")) {
                result.addWarning("File '" + fileType.getFileLabel() + "': missing required mappings. Skipping.");
                continue;
            }

            List<Map<String, String>> rows = parseFile(file, fileType.getFileFormat());

            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);

                int benefitId = parseIntSafe(getCanonicalValue(row, mappings, "benefit_id"));
                if (benefitId == 0) { result.addError(); continue; }

                int orgId = parseIntSafe(getCanonicalValue(row, mappings, "employer_id"));
                int planTypeId = parseIntSafe(getCanonicalValue(row, mappings, "plan_type_id"));

                // Status filter
                String status = getCanonicalValue(row, mappings, "status");
                if (status.equalsIgnoreCase("Inactive") || status.equalsIgnoreCase("false")) {
                    Benefit existingInactive = findBenefitBySummitKey(em, sourceType, benefitId);
                    if (existingInactive != null && existingInactive.isActive()) {
                        em.getTransaction().begin();
                        existingInactive.setActive(false);
                        em.merge(existingInactive);
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        result.addSkipped();
                    }
                    continue;
                }

                // Validate FKs — resolve via cross-reference, then fall back to direct PK
                Employer employer = ImportIdResolver.resolveEntity(em, Employer.class,
                        provider.getId(), ImportIdResolver.EMPLOYER, String.valueOf(orgId));
                if (employer == null) employer = em.find(Employer.class, orgId);
                if (employer == null) {
                    result.addError("Benefit " + benefitId + ": employer " + orgId + " not found, skipping.");
                    continue;
                }
                PlanType planType = ImportIdResolver.resolveEntity(em, PlanType.class,
                        provider.getId(), ImportIdResolver.PLAN_TYPE, String.valueOf(planTypeId));
                if (planType == null) planType = em.find(PlanType.class, planTypeId);
                if (planType == null) {
                    result.addError("Benefit " + benefitId + ": PlanType " + planTypeId + " not found, skipping.");
                    continue;
                }

                String planName = getCanonicalValue(row, mappings, "plan_name");
                String planDescription = getCanonicalValue(row, mappings, "plan_description");

                // Cross-populate: if one is provided but the other is not, use it for both
                if (!planName.isEmpty() && planDescription.isEmpty()) planDescription = planName;
                else if (!planDescription.isEmpty() && planName.isEmpty()) planName = planDescription;

                String effectiveDateStr = getCanonicalValue(row, mappings, "effective_date");
                String terminationDateStr = getCanonicalValue(row, mappings, "termination_date");

                Date effectiveDate = parseDate(effectiveDateStr);
                Date terminationDate = parseDate(terminationDateStr);
                int renewalMonths = getRenewalMonths(renewalMonthsMap, planTypeId);

                Benefit existing = findBenefitBySummitKey(em, sourceType, benefitId);

                if (existing != null) {
                    boolean changed = false;
                    if (!planName.isEmpty() && !planName.equals(existing.getPlanName())) { existing.setPlanName(planName); changed = true; }
                    if (!Objects.equals(blankToNull(planDescription), blankToNull(existing.getPlanDescription()))) { existing.setPlanDescription(planDescription); changed = true; }
                    // Cross-populate null plan fields after import updates are applied
                    String pn = existing.getPlanName(), pd = existing.getPlanDescription();
                    if (pn != null && !pn.isEmpty() && (pd == null || pd.isEmpty())) { existing.setPlanDescription(pn); changed = true; }
                    else if (pd != null && !pd.isEmpty() && (pn == null || pn.isEmpty())) { existing.setPlanName(pd); changed = true; }
                    if (!existing.isActive()) { existing.setActive(true); changed = true; }

                    if (changed) {
                        em.getTransaction().begin();
                        em.merge(existing);
                        ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT,
                                String.valueOf(benefitId), existing.getId(), true);
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        result.addSkipped();
                    }
                } else {
                    em.getTransaction().begin();
                    Benefit b = new Benefit();
                    b.setSummitId(benefitId);
                    b.setSourceType(sourceType);
                    b.setEmployer(employer);
                    b.setPlanType(planType);
                    b.setPlanName(planName);
                    b.setPlanDescription(planDescription);
                    b.setEffectiveDate(effectiveDate);
                    b.setTerminationDate(terminationDate);
                    b.setRenewalMonths(renewalMonths);
                    b.setActive(true);

                    // Calculate next renewal due
                    if (effectiveDate != null) {
                        LocalDate eff = effectiveDate.toLocalDate();
                        LocalDate nextDue = eff.plusMonths(renewalMonths);
                        while (nextDue.isBefore(LocalDate.now())) {
                            nextDue = nextDue.plusMonths(renewalMonths);
                        }
                        b.setNextRenewalDue(Date.valueOf(nextDue));
                    }

                    em.persist(b);
                    em.flush(); // ensure auto-generated benefit_id is assigned
                    ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT,
                            String.valueOf(benefitId), b.getId(), true);
                    em.getTransaction().commit();
                    result.addInserted();
                }

                if (result.total() % 50 == 0) em.clear();
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    /** Get file types for a provider targeting a specific entity. Ordered by sort_order. */
    private static List<ImportFileType> getFileTypesForEntity(EntityManager em, ImportProvider provider, String targetEntity) {
        return em.createQuery(
                "SELECT ft FROM ImportFileType ft WHERE ft.provider.id = :pid AND ft.targetEntity = :te ORDER BY ft.sortOrder",
                ImportFileType.class)
                .setParameter("pid", provider.getId())
                .setParameter("te", targetEntity)
                .getResultList();
    }

    /** Create a ServiceItem for a new PlanType (Renewal group). */
    private static ServiceItem createRenewalServiceItem(EntityManager em, String name, String code,
                                                         int planTypeId, int renewalMonths,
                                                         ActivityCategory renewalCategory, PSP psp,
                                                         String providerCode) {
        ServiceItem si = new ServiceItem();
        si.setDescription(name);
        si.setCode(code);
        si.setSortOrder(planTypeId);
        si.setActivityCategory(renewalCategory);
        si.setPsp(psp);
        si.setSourceType(providerCode);
        si.setProviderRef(String.valueOf(planTypeId));
        si.setDefaultRenewalMonths(renewalMonths);
        si.setHasRequiredTasks(true);
        em.persist(si);
        return si;
    }

    private static BillingGroup getOrCreateDefaultBillingGroup(EntityManager em) {
        BillingGroup bg = em.find(BillingGroup.class, 99);
        if (bg == null) {
            em.getTransaction().begin();
            bg = new BillingGroup();
            bg.setId(99);
            bg.setDescription("Other");
            em.persist(bg);
            em.getTransaction().commit();
        }
        return bg;
    }

    private static int getRenewalMonths(Map<Integer, Integer> map, int planTypeId) {
        if (map != null && map.containsKey(planTypeId)) {
            return map.get(planTypeId);
        }
        return 12;
    }

    /** Lookup Benefit by source-discriminated key. Returns null if not found. */
    private static Benefit findBenefitBySummitKey(EntityManager em, String sourceType, int summitId) {
        try {
            return em.createQuery(
                    "SELECT b FROM Benefit b WHERE b.sourceType = :src AND b.summitId = :sid", Benefit.class)
                    .setParameter("src", sourceType)
                    .setParameter("sid", summitId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(s.trim());
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /**
     * Parses a date string in common formats: M/d/yyyy, yyyy-MM-dd, ISO.
     * Returns null if blank or unparseable.
     */
    private static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();

        // Try M/d/yyyy
        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy"));
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        // Try yyyy-MM-dd (ISO)
        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        // Try MM/dd/yyyy
        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        return null;
    }
}

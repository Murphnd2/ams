package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.ImportIdResolver;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.imports.ImportProvider;
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
 * Core import/sync logic for Summit data.
 * Handles Plan Types (Excel), Employers (CSV), Employees (CSV x2), and Benefits (CSV).
 *
 * Uses upsert pattern: INSERT if new, UPDATE if changed, SKIP if unchanged.
 * Summit IDs are natural keys — no auto-generation.
 */
public class SummitImportService {

    // ═══════════════════════════════════════════════════════════════
    //  IMPORT RESULT — returned from each import method
    // ═══════════════════════════════════════════════════════════════

    public static class ImportResult {
        private int inserted = 0;
        private int updated = 0;
        private int skipped = 0;
        private int errors = 0;
        private List<String> warnings = new ArrayList<>();

        public int getInserted() { return inserted; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public int getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public void addInserted() { inserted++; }
        public void addUpdated() { updated++; }
        public void addSkipped() { skipped++; }
        public void addError() { errors++; }
        public void addError(String warning) { errors++; warnings.add(warning); }
        public void addWarning(String warning) { warnings.add(warning); }

        public int total() { return inserted + updated + skipped + errors; }

        public String summary() {
            return String.format("%d inserted, %d updated, %d unchanged, %d errors",
                    inserted, updated, skipped, errors);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PLAN TYPE IMPORT (Excel)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imports Plan Types from a Summit Excel export.
     * For each Plan Type:
     * - INSERT if not found by PlanType_ID
     * - UPDATE if found but name/code/level/los changed
     * - SKIP if unchanged
     * Also auto-creates ServiceItems (Renewal group) for new Plan Types
     * that don't already have one linked.
     *
     * @param em           Active EntityManager (caller manages transaction)
     * @param excelFile    The uploaded Excel file
     * @param renewalMonthsMap  Map of PlanType_ID → renewal months (from wizard Step 2 config)
     *                          If null or missing entry, defaults to 12.
     * @return ImportResult with counts
     */
    public static ImportResult importPlanTypes(EntityManager em, File excelFile,
                                               Map<Integer, Integer> renewalMonthsMap,
                                               ImportProvider provider) throws Exception {
        ImportResult result = new ImportResult();

        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(excelFile)) {
            workbook = excelFile.getName().toLowerCase().endsWith(".xls")
                    ? new HSSFWorkbook(fis)
                    : new XSSFWorkbook(fis);
        }

        try {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.addWarning("Excel file has no header row.");
                return result;
            }

            // Build column index from headers (case-insensitive)
            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                String name = getCellString(cell).trim().toLowerCase();
                colIndex.put(name, cell.getColumnIndex());
            }

            // Validate required headers
            List<String> required = List.of("plan type id", "plan type code", "plan type name");
            for (String req : required) {
                if (!colIndex.containsKey(req)) {
                    result.addWarning("Missing required column: " + req);
                    result.addError();
                    return result;
                }
            }

            // Optional headers (new V025 columns)
            Integer levelIdx = colIndex.get("level");
            Integer losIdx = colIndex.get("los");
            Integer employerNameIdx = colIndex.get("employer name");

            // Lookup reference entities
            BillingGroup defaultBg = getOrCreateDefaultBillingGroup(em);
            ActivityCategory renewalCategory = em.find(ActivityCategory.class, 1); // Renewal
            ActivityCategory setupCategory = em.find(ActivityCategory.class, 2);   // Setup
            PSP psp = em.find(PSP.class, 4L);

            if (renewalCategory == null) {
                result.addWarning("ActivityCategory ID 1 (Renewal) not found — cannot create ServiceItems.");
                return result;
            }

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell idCell = row.getCell(colIndex.get("plan type id"));
                if (idCell == null) continue;

                int ptId;
                try {
                    ptId = (int) idCell.getNumericCellValue();
                } catch (Exception e) {
                    result.addWarning("Row " + (i + 1) + ": invalid Plan Type ID, skipping.");
                    result.addError();
                    continue;
                }

                String code = getCellString(row.getCell(colIndex.get("plan type code"))).trim();
                String name = getCellString(row.getCell(colIndex.get("plan type name"))).trim();
                String level = levelIdx != null ? getCellString(row.getCell(levelIdx)).trim() : null;
                String los = losIdx != null ? getCellString(row.getCell(losIdx)).trim() : null;
                String employerName = employerNameIdx != null ? getCellString(row.getCell(employerNameIdx)).trim() : null;

                if (name.isEmpty()) {
                    result.addWarning("Row " + (i + 1) + ": empty Plan Type Name for ID " + ptId + ", skipping.");
                    result.addError();
                    continue;
                }

                // Blank strings → null for optional fields
                if (level != null && level.isEmpty()) level = null;
                if (los != null && los.isEmpty()) los = null;
                if (employerName != null && employerName.isEmpty()) employerName = null;

                // Lookup existing
                PlanType existing = em.find(PlanType.class, ptId);

                if (existing != null) {
                    // Check for changes
                    boolean changed = false;
                    if (!name.equals(existing.getPlanTypeName())) { existing.setPlanTypeName(name); changed = true; }
                    if (!code.equals(existing.getCode())) { existing.setCode(code); changed = true; }
                    if (!Objects.equals(level, existing.getLevel())) { existing.setLevel(level); changed = true; }
                    if (!Objects.equals(los, existing.getLos())) { existing.setLos(los); changed = true; }
                    if (!Objects.equals(employerName, existing.getEmployerName())) { existing.setEmployerName(employerName); changed = true; }

                    if (changed) {
                        em.getTransaction().begin();
                        em.merge(existing);
                        if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.PLAN_TYPE, String.valueOf(ptId), ptId, true);
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        result.addSkipped();
                    }

                    // Ensure ServiceItem exists for existing PlanType
                    if (existing.getServiceItem() == null) {
                        int renewalMonths = getRenewalMonths(renewalMonthsMap, ptId);
                        ServiceItem si = createRenewalServiceItem(em, name, code, ptId, renewalMonths, renewalCategory, psp);
                        em.getTransaction().begin();
                        existing.setServiceItem(si);
                        em.merge(existing);
                        em.getTransaction().commit();
                    }
                } else {
                    // New Plan Type — create with ServiceItem
                    int renewalMonths = getRenewalMonths(renewalMonthsMap, ptId);

                    em.getTransaction().begin();

                    ServiceItem si = createRenewalServiceItem(em, name, code, ptId, renewalMonths, renewalCategory, psp);

                    PlanType pt = new PlanType();
                    pt.setPlanTypeId(ptId);
                    pt.setCode(code);
                    pt.setPlanTypeName(name);
                    pt.setLevel(level);
                    pt.setLos(los);
                    pt.setEmployerName(employerName);
                    pt.setBillingGroup(defaultBg);
                    pt.setServiceItem(si);
                    em.persist(pt);
                    if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.PLAN_TYPE, String.valueOf(ptId), ptId, true);

                    em.getTransaction().commit();
                    result.addInserted();
                }

                // Clear persistence context periodically to free memory
                if (result.total() % 50 == 0) {
                    em.clear();
                    // Re-lookup reference entities after clear
                    defaultBg = getOrCreateDefaultBillingGroup(em);
                    renewalCategory = em.find(ActivityCategory.class, 1);
                    psp = em.find(PSP.class, 4L);
                }
            }
        } finally {
            workbook.close();
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  EMPLOYER IMPORT (CSV)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imports Employers from a Summit J1 CSV export.
     * Upsert by OrganizationID.
     */
    public static ImportResult importEmployers(EntityManager em, File csvFile,
                                                ImportProvider provider) throws Exception {
        ImportResult result = new ImportResult();

        List<Map<String, String>> rows = parseCsv(csvFile);
        if (rows.isEmpty()) {
            result.addWarning("CSV file is empty or has no data rows.");
            return result;
        }

        // Validate required columns
        Map<String, String> sample = rows.get(0);
        for (String req : List.of("organizationid", "employername")) {
            if (!sample.containsKey(req)) {
                result.addWarning("Missing required column: " + req);
                result.addError();
                return result;
            }
        }

        for (Map<String, String> row : rows) {
            String orgIdStr = row.getOrDefault("organizationid", "").trim();
            if (orgIdStr.isEmpty()) {
                result.addError();
                continue;
            }

            int orgId;
            try {
                orgId = Integer.parseInt(orgIdStr);
            } catch (NumberFormatException e) {
                result.addWarning("Invalid OrganizationID: " + orgIdStr);
                result.addError();
                continue;
            }

            String employerName = row.getOrDefault("employername", "").trim();
            if (employerName.isEmpty()) {
                result.addWarning("Empty EmployerName for OrganizationID " + orgId + ", skipping.");
                result.addError();
                continue;
            }

            // Status filter — skip inactive
            String status = row.getOrDefault("status", "").trim();
            if (status.equalsIgnoreCase("Inactive") || status.equalsIgnoreCase("Closed")) {
                result.addSkipped();
                continue;
            }

            String customId = row.getOrDefault("customid", "").trim();
            String email = row.getOrDefault("email", "").trim();
            String phone = coalesce(row.get("phonenumber"), row.get("phone"));
            String primaryContact = row.getOrDefault("primarycontact", "").trim();
            String taxId = row.getOrDefault("taxid", "").trim();
            int employerId = parseIntSafe(row.getOrDefault("employer_id",
                    row.getOrDefault("employerorganizationid", "0")));

            Employer existing = em.find(Employer.class, orgId);

            if (existing != null) {
                boolean changed = false;
                if (!employerName.equals(existing.getEmployerName())) { existing.setEmployerName(employerName); changed = true; }
                if (!Objects.equals(email, blankToNull(existing.getEmail()))) { existing.setEmail(email); changed = true; }
                if (!Objects.equals(phone, blankToNull(existing.getPhone()))) { existing.setPhone(phone); changed = true; }
                if (!Objects.equals(primaryContact, blankToNull(existing.getContactName()))) { existing.setContactName(primaryContact); changed = true; }

                if (changed) {
                    em.getTransaction().begin();
                    em.merge(existing);
                    if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYER, String.valueOf(orgId), orgId, true);
                    em.getTransaction().commit();
                    result.addUpdated();
                } else {
                    result.addSkipped();
                }
            } else {
                em.getTransaction().begin();
                Employer er = new Employer();
                er.setId(orgId);
                er.setEmployerName(employerName);
                er.setAltId(employerId);
                er.setErKey(parseIntSafe(customId));
                er.setEmail(email);
                er.setPhone(phone);
                er.setContactName(primaryContact);
                er.setActive(true);
                em.persist(er);
                if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYER, String.valueOf(orgId), orgId, true);
                em.getTransaction().commit();
                result.addInserted();
            }

            if (result.total() % 50 == 0) {
                em.clear();
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  EMPLOYEE IMPORT (CSV x2 — J2 Contact + J3 Status)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imports Employees from two Summit CSV exports.
     * J2 (contact info) is the primary source; J3 (status/dates) supplements.
     * Both are joined on Participant_ID.
     *
     * @param em         Active EntityManager
     * @param j2File     J2 — Participant Listing Simple (contact info)
     * @param j3File     J3 — Participant Listing Report with Division Option (status + dates)
     * @return ImportResult
     */
    public static ImportResult importEmployees(EntityManager em, File j2File, File j3File,
                                                ImportProvider provider) throws Exception {
        ImportResult result = new ImportResult();

        // Parse both files
        List<Map<String, String>> j2Rows = parseCsv(j2File);
        List<Map<String, String>> j3Rows = parseCsv(j3File);

        // Index J2 by Participant_ID
        Map<Integer, Map<String, String>> j2ByPid = new LinkedHashMap<>();
        for (Map<String, String> row : j2Rows) {
            int pid = parseIntSafe(row.getOrDefault("participant_id", "0"));
            if (pid > 0) j2ByPid.put(pid, row);
        }

        // Index J3 by Participant_ID
        Map<Integer, Map<String, String>> j3ByPid = new LinkedHashMap<>();
        for (Map<String, String> row : j3Rows) {
            int pid = parseIntSafe(row.getOrDefault("participant_id", "0"));
            if (pid > 0) j3ByPid.put(pid, row);
        }

        // Union of all participant IDs from both files
        Set<Integer> allPids = new LinkedHashSet<>();
        allPids.addAll(j2ByPid.keySet());
        allPids.addAll(j3ByPid.keySet());

        for (int pid : allPids) {
            Map<String, String> j2 = j2ByPid.get(pid);
            Map<String, String> j3 = j3ByPid.get(pid);

            // Determine employer FK
            int orgId = 0;
            if (j2 != null) orgId = parseIntSafe(j2.getOrDefault("organization_id", "0"));
            if (orgId == 0 && j3 != null) orgId = parseIntSafe(j3.getOrDefault("organization_id", "0"));

            if (orgId == 0) {
                result.addWarning("Participant " + pid + ": no Organization_ID, skipping.");
                result.addError();
                continue;
            }

            // Verify employer exists
            Employer employer = em.find(Employer.class, orgId);
            if (employer == null) {
                result.addWarning("Participant " + pid + ": employer " + orgId + " not found, skipping.");
                result.addError();
                continue;
            }

            // Extract fields — prefer J2 for contact, J3 for status
            String firstName = j2 != null ? j2.getOrDefault("firstname", "").trim() : "";
            String lastName = j2 != null ? j2.getOrDefault("lastname", "").trim() : "";
            if (firstName.isEmpty() && j3 != null) firstName = j3.getOrDefault("participant first name", "").trim();
            if (lastName.isEmpty() && j3 != null) lastName = j3.getOrDefault("participant last name", "").trim();

            String email = j2 != null ? j2.getOrDefault("email", "").trim() : "";
            String address1 = j2 != null ? j2.getOrDefault("address1", "").trim() : "";
            String address2 = j2 != null ? j2.getOrDefault("address2", "").trim() : "";
            String city = j2 != null ? j2.getOrDefault("city", "").trim() : "";
            String state = j2 != null ? j2.getOrDefault("state", "").trim() : "";
            String zip = j2 != null ? j2.getOrDefault("zipcode", "").trim() : "";
            String customId = j2 != null ? j2.getOrDefault("participantcustomid", "").trim() : "";
            String userId = j2 != null ? j2.getOrDefault("user_id", "").trim() : "";

            // Status from J3
            int eeStatusId = j3 != null ? parseIntSafe(j3.getOrDefault("participantstatusid", "0")) : 0;
            int systemStatusId = j3 != null ? parseIntSafe(j3.getOrDefault("userstatusid", "0")) : 0;

            // Determine active — systemStatusId 1 = active in Summit (may need tuning)
            boolean isActive = systemStatusId != 2; // 2 = Inactive in Summit

            Employee existing = em.find(Employee.class, pid);

            if (existing != null) {
                boolean changed = false;
                if (!firstName.isEmpty() && !firstName.equals(existing.getFirstName())) { existing.setFirstName(firstName); changed = true; }
                if (!lastName.isEmpty() && !lastName.equals(existing.getLastName())) { existing.setLastName(lastName); changed = true; }
                if (!email.isEmpty() && !Objects.equals(email, existing.getEmail())) { existing.setEmail(email); changed = true; }
                if (!address1.isEmpty() && !Objects.equals(address1, existing.getAddress1())) { existing.setAddress1(address1); changed = true; }
                if (!address2.isEmpty() && !Objects.equals(address2, existing.getAddress2())) { existing.setAddress2(address2); changed = true; }
                if (!city.isEmpty() && !Objects.equals(city, existing.getCity())) { existing.setCity(city); changed = true; }
                if (!state.isEmpty() && !Objects.equals(state, existing.getState())) { existing.setState(state); changed = true; }
                if (!zip.isEmpty() && !Objects.equals(zip, existing.getZipCode())) { existing.setZipCode(zip); changed = true; }
                if (eeStatusId != 0 && !Objects.equals(eeStatusId, existing.getEeStatusId())) { existing.setEeStatusId(eeStatusId); changed = true; }
                if (systemStatusId != 0 && !Objects.equals(systemStatusId, existing.getSystemStatusId())) { existing.setSystemStatusId(systemStatusId); changed = true; }
                if (existing.isActive() != isActive) { existing.setActive(isActive); changed = true; }

                if (changed) {
                    em.getTransaction().begin();
                    em.merge(existing);
                    if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYEE, String.valueOf(pid), pid, true);
                    em.getTransaction().commit();
                    result.addUpdated();
                } else {
                    result.addSkipped();
                }
            } else {
                em.getTransaction().begin();
                Employee ee = new Employee();
                ee.setId(pid);
                ee.setEmployer(employer);
                ee.setFirstName(firstName.isEmpty() ? "Unknown" : firstName);
                ee.setLastName(lastName.isEmpty() ? "Unknown" : lastName);
                ee.setEmail(email);
                ee.setAddress1(address1);
                ee.setAddress2(address2);
                ee.setCity(city);
                ee.setState(state);
                ee.setZipCode(zip);
                ee.setCustomId(customId);
                ee.setUserId(userId);
                ee.setEeStatusId(eeStatusId != 0 ? eeStatusId : null);
                ee.setSystemStatusId(systemStatusId != 0 ? systemStatusId : null);
                ee.setActive(isActive);
                em.persist(ee);
                if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYEE, String.valueOf(pid), pid, true);
                em.getTransaction().commit();
                result.addInserted();
            }

            if (result.total() % 50 == 0) {
                em.clear();
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BENEFIT IMPORT (CSV)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imports Benefits from a Summit J4 CSV export.
     * Upsert by EmployerPlan_ID. Links to Employer and PlanType.
     * Sets renewal fields based on configured renewal months.
     *
     * @param em               Active EntityManager
     * @param csvFile          J4 — Employer Benefit Plans CSV
     * @param renewalMonthsMap Map of PlanType_ID → renewal months (from wizard config)
     * @return ImportResult
     */
    public static ImportResult importBenefits(EntityManager em, File csvFile,
                                              Map<Integer, Integer> renewalMonthsMap,
                                              ImportProvider provider) throws Exception {
        ImportResult result = new ImportResult();

        List<Map<String, String>> rows = parseCsv(csvFile);
        if (rows.isEmpty()) {
            result.addWarning("Benefits CSV is empty or has no data rows.");
            return result;
        }

        // Validate required columns
        Map<String, String> sample = rows.get(0);
        for (String req : List.of("employerplan_id", "organizationid", "plantypeid")) {
            if (!sample.containsKey(req)) {
                result.addWarning("Missing required column: " + req);
                result.addError();
                return result;
            }
        }

        for (Map<String, String> row : rows) {
            int benefitId = parseIntSafe(row.getOrDefault("employerplan_id", "0"));
            if (benefitId == 0) {
                result.addError();
                continue;
            }

            int orgId = parseIntSafe(row.getOrDefault("organizationid", "0"));
            int planTypeId = parseIntSafe(row.getOrDefault("plantypeid", "0"));

            // Status filter
            String planStatus = row.getOrDefault("planstatus", "").trim();
            if (planStatus.equalsIgnoreCase("Inactive")) {
                // If exists and was active, mark inactive
                Benefit existing = findBenefitBySummitKey(em, "CDH", benefitId);
                if (existing != null && existing.isActive()) {
                    em.getTransaction().begin();
                    existing.setActive(false);
                    em.merge(existing);
                    em.getTransaction().commit();
                    result.addUpdated();
                } else {
                    result.addSkipped();
                }
                continue;
            }

            // Validate FKs
            Employer employer = em.find(Employer.class, orgId);
            if (employer == null) {
                result.addWarning("Benefit " + benefitId + ": employer " + orgId + " not found, skipping.");
                result.addError();
                continue;
            }

            PlanType planType = em.find(PlanType.class, planTypeId);
            if (planType == null) {
                result.addWarning("Benefit " + benefitId + ": PlanType " + planTypeId + " not found, skipping.");
                result.addError();
                continue;
            }

            String planName = row.getOrDefault("planname", "").trim();
            String planDescription = row.getOrDefault("plandescription", "").trim();
            String effectiveDateStr = row.getOrDefault("effectivedate", "").trim();
            String terminationDateStr = row.getOrDefault("terminationdate", "").trim();
            boolean cardEnabled = "True".equalsIgnoreCase(row.getOrDefault("cardenabled", "").trim());

            Date effectiveDate = parseDate(effectiveDateStr);
            Date terminationDate = parseDate(terminationDateStr);

            int renewalMonths = getRenewalMonths(renewalMonthsMap, planTypeId);

            Benefit existing = findBenefitBySummitKey(em, "CDH", benefitId);

            if (existing != null) {
                boolean changed = false;
                if (!planName.equals(existing.getPlanName())) { existing.setPlanName(planName); changed = true; }
                if (!Objects.equals(planDescription, existing.getPlanDescription())) { existing.setPlanDescription(planDescription); changed = true; }
                if (!existing.isActive()) { existing.setActive(true); changed = true; } // reactivate if re-imported as active

                if (changed) {
                    em.getTransaction().begin();
                    em.merge(existing);
                    if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT, String.valueOf(benefitId), existing.getId(), true);
                    em.getTransaction().commit();
                    result.addUpdated();
                } else {
                    result.addSkipped();
                }
            } else {
                em.getTransaction().begin();
                Benefit b = new Benefit();
                b.setSummitId(benefitId);
                b.setSourceType("CDH");
                b.setEmployer(employer);
                b.setPlanType(planType);
                b.setPlanName(planName);
                b.setPlanDescription(planDescription);
                b.setEffectiveDate(effectiveDate);
                b.setTerminationDate(terminationDate);
                b.setHasCards(cardEnabled);
                b.setRenewalMonths(renewalMonths);
                b.setActive(true);

                // Calculate next renewal due
                if (effectiveDate != null) {
                    LocalDate eff = effectiveDate.toLocalDate();
                    LocalDate nextDue = eff.plusMonths(renewalMonths);
                    // If next due is in the past, roll forward until it's in the future
                    while (nextDue.isBefore(LocalDate.now())) {
                        nextDue = nextDue.plusMonths(renewalMonths);
                    }
                    b.setNextRenewalDue(Date.valueOf(nextDue));
                }

                em.persist(b);
                em.flush(); // flush to get auto-generated benefit_id
                if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT, String.valueOf(benefitId), b.getId(), true);
                em.getTransaction().commit();
                result.addInserted();
            }

            if (result.total() % 50 == 0) {
                em.clear();
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BENEFIT IMPORT — COBRA/PB (J7 CSV)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imports COBRA/PB Benefits from a Summit J7 CSV export.
     * Upsert by (source_type='COBRA', summit_id=BenefitID).
     * Also sets pbBenId from the PBBenefitID column.
     *
     * @param em               Active EntityManager
     * @param csvFile          J7 — PB Employer Benefit Detail Report CSV
     * @param renewalMonthsMap Map of PlanType_ID → renewal months
     * @return ImportResult
     */
    public static ImportResult importBenefitsCobra(EntityManager em, File csvFile,
                                                    Map<Integer, Integer> renewalMonthsMap,
                                                    ImportProvider provider) throws Exception {
        ImportResult result = new ImportResult();

        List<Map<String, String>> rows = parseCsv(csvFile);
        if (rows.isEmpty()) {
            result.addWarning("COBRA Benefits CSV is empty or has no data rows.");
            return result;
        }

        // Validate required columns
        Map<String, String> sample = rows.get(0);
        for (String req : List.of("benefitid", "organizationid", "plantypeid")) {
            if (!sample.containsKey(req)) {
                result.addWarning("Missing required column: " + req);
                result.addError();
                return result;
            }
        }

        // J7 has multiple rows per benefit (one per plan year / tier).
        // We only need to create/update the benefit once — track which we've processed.
        Set<Integer> processedSummitIds = new HashSet<>();

        // Pre-compute latest plan year dates per benefit (J7 has multiple rows per benefit).
        // The latest plan year end date + 1 day = the assumed renewal anchor.
        Map<Integer, Date> latestPlanYearEnd = new HashMap<>();
        Map<Integer, Date> latestPlanYearStart = new HashMap<>();
        for (Map<String, String> row : rows) {
            int bid = parseIntSafe(row.getOrDefault("benefitid", "0"));
            if (bid == 0) continue;
            Date endDate = parseDate(row.getOrDefault("enddate", "").trim());
            Date startDate = parseDate(row.getOrDefault("startdate", "").trim());
            if (endDate != null) {
                Date current = latestPlanYearEnd.get(bid);
                if (current == null || endDate.after(current)) {
                    latestPlanYearEnd.put(bid, endDate);
                    if (startDate != null) latestPlanYearStart.put(bid, startDate);
                }
            }
        }

        for (Map<String, String> row : rows) {
            int summitBenefitId = parseIntSafe(row.getOrDefault("benefitid", "0"));
            if (summitBenefitId == 0) {
                result.addError();
                continue;
            }

            // Skip if already processed this benefit (J7 has multiple rows per benefit)
            if (processedSummitIds.contains(summitBenefitId)) continue;
            processedSummitIds.add(summitBenefitId);

            int orgId = parseIntSafe(row.getOrDefault("organizationid", "0"));
            int planTypeId = parseIntSafe(row.getOrDefault("plantypeid", "0"));
            int pbBenefitId = parseIntSafe(row.getOrDefault("pbbenefitid", "0"));

            // Validate FKs
            Employer employer = em.find(Employer.class, orgId);
            if (employer == null) {
                result.addWarning("COBRA Benefit " + summitBenefitId + ": employer " + orgId + " not found, skipping.");
                result.addError();
                continue;
            }

            PlanType planType = em.find(PlanType.class, planTypeId);
            if (planType == null) {
                result.addWarning("COBRA Benefit " + summitBenefitId + ": PlanType " + planTypeId + " not found, skipping.");
                result.addError();
                continue;
            }

            String benefitName = row.getOrDefault("benefitname", "").trim();
            String effectiveDateStr = row.getOrDefault("effectivedate", "").trim();
            String startDateStr = row.getOrDefault("startdate", "").trim();
            String endDateStr = row.getOrDefault("enddate", "").trim();

            Date effectiveDate = parseDate(effectiveDateStr);
            Date terminationDate = parseDate(endDateStr);
            if (effectiveDate == null) effectiveDate = parseDate(startDateStr);

            int renewalMonths = getRenewalMonths(renewalMonthsMap, planTypeId);

            Benefit existing = findBenefitBySummitKey(em, "COBRA", summitBenefitId);

            if (existing != null) {
                boolean changed = false;
                if (!benefitName.isEmpty() && !benefitName.equals(existing.getPlanName())) { existing.setPlanName(benefitName); changed = true; }
                if (pbBenefitId != 0 && pbBenefitId != existing.getPbBenId()) { existing.setPbBenId(pbBenefitId); changed = true; }

                // Update plan year dates from latest J7 row data — renewal date
                // changes are driven by the user via the Benefit Audit page
                Date pyStart = latestPlanYearStart.get(summitBenefitId);
                Date pyEnd = latestPlanYearEnd.get(summitBenefitId);
                if (pyStart != null && !pyStart.equals(existing.getPlanYearStart())) { existing.setPlanYearStart(pyStart); changed = true; }
                if (pyEnd != null && !pyEnd.equals(existing.getPlanYearEnd())) { existing.setPlanYearEnd(pyEnd); changed = true; }

                if (changed) {
                    em.getTransaction().begin();
                    em.merge(existing);
                    if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT, "COBRA-" + summitBenefitId, existing.getId(), true);
                    em.getTransaction().commit();
                    result.addUpdated();
                } else {
                    result.addSkipped();
                }
            } else {
                em.getTransaction().begin();
                Benefit b = new Benefit();
                b.setSummitId(summitBenefitId);
                b.setSourceType("COBRA");
                b.setPbBenId(pbBenefitId);
                b.setEmployer(employer);
                b.setPlanType(planType);
                b.setPlanName(benefitName);
                b.setEffectiveDate(effectiveDate);
                b.setTerminationDate(terminationDate);
                b.setRenewalMonths(renewalMonths);
                b.setActive(true);

                // Store latest plan year dates from J7 multi-row data
                Date pyStart = latestPlanYearStart.get(summitBenefitId);
                Date pyEnd = latestPlanYearEnd.get(summitBenefitId);
                if (pyStart != null) b.setPlanYearStart(pyStart);
                if (pyEnd != null) b.setPlanYearEnd(pyEnd);

                // Use plan year end + 1 day as renewal anchor; fall back to effective date
                LocalDate renewalAnchor = null;
                if (pyEnd != null) {
                    renewalAnchor = pyEnd.toLocalDate().plusDays(1);
                } else if (effectiveDate != null) {
                    renewalAnchor = effectiveDate.toLocalDate();
                }

                if (renewalAnchor != null) {
                    LocalDate nextDue = renewalAnchor;
                    while (nextDue.isBefore(LocalDate.now())) {
                        nextDue = nextDue.plusMonths(renewalMonths);
                    }
                    b.setNextRenewalDue(Date.valueOf(nextDue));
                }

                em.persist(b);
                em.flush(); // flush to get auto-generated benefit_id
                if (provider != null) ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT, "COBRA-" + summitBenefitId, b.getId(), true);
                em.getTransaction().commit();
                result.addInserted();
            }

            if (result.total() % 50 == 0) {
                em.clear();
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BENEFIT PLAN YEARS (J5) — CDH plan year data for renewal correction
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imports J5 (Benefit Plan Years) CSV to correct renewal dates on CDH benefits.
     * Each J5 row is a plan year entry for a CDH benefit (EmployerPlan_ID).
     * Multiple rows per benefit represent different plan years.
     *
     * Logic:
     * - Groups rows by EmployerPlan_ID
     * - For each benefit, finds the latest plan year end date
     * - Sets planYearStart/planYearEnd on the Benefit
     * - Recalculates nextRenewalDue using planYearEnd + 1 day as anchor
     * - Flags benefits where end date month/day changes year-to-year (short plan year)
     */
    public static ImportResult importBenefitYears(EntityManager em, File csvFile) throws Exception {
        ImportResult result = new ImportResult();

        List<Map<String, String>> rows = parseCsv(csvFile);
        if (rows.isEmpty()) {
            result.addWarning("Benefit Plan Years CSV is empty or has no data rows.");
            return result;
        }

        // Validate required columns
        Map<String, String> sample = rows.get(0);
        for (String req : List.of("employerplan_id", "planyear")) {
            if (!sample.containsKey(req)) {
                result.addWarning("Missing required column: " + req);
                result.addError();
                return result;
            }
        }

        // Group plan year rows by EmployerPlan_ID
        Map<Integer, List<LocalDate[]>> planYearsByBenefit = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            int employerPlanId = parseIntSafe(row.getOrDefault("employerplan_id", "0"));
            if (employerPlanId == 0) continue;

            String planYearStr = row.getOrDefault("planyear", "").trim();
            if (planYearStr.length() < 21) continue;

            try {
                // PlanYear format: "MM/DD/YYYY-MM/DD/YYYY"
                LocalDate start = parsePlanYearDate(planYearStr.substring(0, 10));
                LocalDate end = parsePlanYearDate(planYearStr.substring(11, 21));
                planYearsByBenefit.computeIfAbsent(employerPlanId, k -> new ArrayList<>())
                        .add(new LocalDate[]{start, end});
            } catch (Exception e) {
                result.addWarning("Benefit " + employerPlanId + ": could not parse PlanYear '" + planYearStr + "'");
            }
        }

        // Process each benefit
        for (Map.Entry<Integer, List<LocalDate[]>> entry : planYearsByBenefit.entrySet()) {
            int employerPlanId = entry.getKey();
            List<LocalDate[]> planYears = entry.getValue();

            // Find the latest plan year (by end date)
            LocalDate latestStart = null;
            LocalDate latestEnd = null;
            for (LocalDate[] py : planYears) {
                if (latestEnd == null || py[1].isAfter(latestEnd)) {
                    latestStart = py[0];
                    latestEnd = py[1];
                }
            }

            // Look up the CDH benefit
            Benefit benefit = findBenefitBySummitKey(em, "CDH", employerPlanId);
            if (benefit == null) {
                result.addWarning("Benefit Plan Year: no CDH benefit found for EmployerPlan_ID " + employerPlanId + ", skipping.");
                result.addSkipped();
                continue;
            }

            // Check for year-to-year end date changes (short plan year detection)
            if (planYears.size() > 1) {
                Set<String> endMonthDays = new HashSet<>();
                for (LocalDate[] py : planYears) {
                    endMonthDays.add(String.format("%02d/%02d", py[1].getMonthValue(), py[1].getDayOfMonth()));
                }
                if (endMonthDays.size() > 1) {
                    result.addWarning("Benefit " + employerPlanId + " (" + benefit.getPlanName()
                            + "): short plan year detected — end dates vary across years: " + endMonthDays);
                }
            }

            // Update benefit with latest plan year data
            em.getTransaction().begin();
            boolean isFirstPlanYear = benefit.getPlanYearEnd() == null;
            benefit.setPlanYearStart(Date.valueOf(latestStart));
            benefit.setPlanYearEnd(Date.valueOf(latestEnd));

            // Seed nextRenewalDue from planYearEnd + 1 only on first import
            // (when benefit had no plan year data yet). On re-import, the audit
            // page drives renewal date changes.
            if (isFirstPlanYear) {
                LocalDate renewalAnchor = latestEnd.plusDays(1);
                int rm = benefit.getRenewalMonths();
                LocalDate nextDue = renewalAnchor;
                while (nextDue.isBefore(LocalDate.now())) {
                    nextDue = nextDue.plusMonths(rm);
                }
                benefit.setNextRenewalDue(Date.valueOf(nextDue));
            }

            em.merge(benefit);
            em.getTransaction().commit();
            result.addUpdated();
        }

        return result;
    }

    /** Parse a plan year date in MM/DD/YYYY format. */
    private static LocalDate parsePlanYearDate(String dateStr) {
        String trimmed = dateStr.trim();
        int month = Integer.parseInt(trimmed.substring(0, 2));
        int day = Integer.parseInt(trimmed.substring(3, 5));
        int year = Integer.parseInt(trimmed.substring(6, 10));
        return LocalDate.of(year, month, day);
    }

    // ═══════════════════════════════════════════════════════════════
    //  BENEFIT LOOKUP HELPER
    // ═══════════════════════════════════════════════════════════════

    /** Lookup Benefit by source-discriminated Summit key. Returns null if not found. */
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

    // ═══════════════════════════════════════════════════════════════
    //  CSV PARSING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parses a CSV file into a list of maps (header → value).
     * All header keys are lowercased and trimmed.
     * Handles quoted fields with commas inside.
     */
    public static List<Map<String, String>> parseCsv(File file) throws IOException {
        List<Map<String, String>> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) return results;

            // Remove BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = splitCsvLine(headerLine);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().toLowerCase();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = splitCsvLine(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i], values[i].trim());
                }
                results.add(row);
            }
        }

        return results;
    }

    /**
     * Splits a CSV line respecting quoted fields.
     * Handles commas inside double quotes.
     */
    private static String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());

        return tokens.toArray(new String[0]);
    }

    /**
     * Returns the list of headers from a CSV file (lowercased, trimmed).
     */
    public static List<String> getCsvHeaders(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return List.of();
            if (headerLine.startsWith("\uFEFF")) headerLine = headerLine.substring(1);
            String[] headers = splitCsvLine(headerLine);
            List<String> result = new ArrayList<>();
            for (String h : headers) result.add(h.trim().toLowerCase());
            return result;
        }
    }

    /**
     * Counts the data rows in a CSV file (excludes header).
     */
    public static int countCsvRows(File file) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            reader.readLine(); // skip header
            while (reader.readLine() != null) count++;
        }
        return count;
    }

    /**
     * Returns the list of headers from an Excel file (lowercased, trimmed).
     */
    public static List<String> getExcelHeaders(File file) throws Exception {
        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(file)) {
            workbook = file.getName().toLowerCase().endsWith(".xls")
                    ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
        }
        try {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return List.of();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellString(cell).trim().toLowerCase());
            }
            return headers;
        } finally {
            workbook.close();
        }
    }

    /**
     * Counts data rows in an Excel file (excludes header).
     */
    public static int countExcelRows(File file) throws Exception {
        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(file)) {
            workbook = file.getName().toLowerCase().endsWith(".xls")
                    ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
        }
        try {
            Sheet sheet = workbook.getSheetAt(0);
            return sheet.getLastRowNum(); // 0-based, so lastRowNum = row count excluding header
        } finally {
            workbook.close();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static ServiceItem createRenewalServiceItem(EntityManager em, String name, String code,
                                                         int planTypeId, int renewalMonths,
                                                         ActivityCategory renewalCategory, PSP psp) {
        ServiceItem si = new ServiceItem();
        si.setDescription(name);
        si.setCode(code);
        si.setSortOrder(planTypeId);
        si.setActivityCategory(renewalCategory);
        si.setPsp(psp);
        si.setSourceType("SUMMIT");
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
        return 12; // default
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
            // Try parsing as double first (e.g., "123.0" from Excel)
            try {
                return (int) Double.parseDouble(s.trim());
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return "";
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /**
     * Parses a date string in common Summit formats: M/d/yyyy, MM/dd/yyyy, yyyy-MM-dd.
     * Returns null if blank or unparseable.
     */
    private static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();

        // Try M/d/yyyy (Summit's typical format)
        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy"));
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        // Try yyyy-MM-dd (ISO)
        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        return null;
    }
}

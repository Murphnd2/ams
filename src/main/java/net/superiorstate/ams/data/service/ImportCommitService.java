package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.resolver.ImportIdResolver;
import net.superiorstate.ams.data.service.InteractiveImportSession.EntityImportState;
import net.superiorstate.ams.data.service.InteractiveImportSession.ImportRow;
import net.superiorstate.ams.data.service.UniversalImportService.ImportResult;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.imports.ImportProvider;
import net.superiorstate.ams.model.summit.archive.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Commit service for Interactive Import — applies user-resolved ImportRow decisions to the database.
 *
 * For each row:
 *   MATCHED / CONFIRMED / MANUAL → update existing record fields, record xref mapping
 *   UNMATCHED → create new record, allocate PK if needed, record xref mapping
 *   SKIPPED / ERROR → skip (no DB changes)
 *
 * Follows the same upsert patterns as UniversalImportService but uses the pre-resolved
 * ImportRow decisions (amsInternalId, status) rather than re-resolving each row.
 */
public abstract class ImportCommitService {

    // ═══════════════════════════════════════════════════════════════
    //  MAIN ENTRY POINT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Commit all resolved rows for a given entity type.
     *
     * @param em             EntityManager (must be available for transactions)
     * @param provider       the ImportProvider
     * @param state          the EntityImportState containing resolved rows
     * @param renewalMonthsMap optional per-planType renewal months overrides
     * @return ImportResult with insert/update/skip/error counts
     */
    public static ImportResult commitEntity(EntityManager em, ImportProvider provider,
                                             EntityImportState state,
                                             Map<Integer, Integer> renewalMonthsMap) {
        // Pre-commit: treat any remaining SUGGESTED rows as UNMATCHED (create new)
        for (ImportRow row : state.getRows()) {
            if ("SUGGESTED".equals(row.getStatus())) {
                row.setStatus("UNMATCHED");
                row.setAmsInternalId(null);
                row.setAmsDisplayLabel(null);
            }
        }

        return switch (state.getEntityType()) {
            case "PLAN_TYPE" -> commitPlanTypes(em, provider, state, renewalMonthsMap);
            case "EMPLOYER" -> commitEmployers(em, provider, state);
            case "BENEFIT" -> commitBenefits(em, provider, state, renewalMonthsMap);
            case "EMPLOYEE" -> commitEmployees(em, provider, state);
            default -> {
                ImportResult r = new ImportResult();
                r.addError("Unknown entity type: " + state.getEntityType());
                yield r;
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  PLAN TYPE COMMIT
    // ═══════════════════════════════════════════════════════════════

    private static ImportResult commitPlanTypes(EntityManager em, ImportProvider provider,
                                                 EntityImportState state,
                                                 Map<Integer, Integer> renewalMonthsMap) {
        ImportResult result = new ImportResult();

        BillingGroup defaultBg = getOrCreateDefaultBillingGroup(em);
        ActivityCategory renewalCategory = em.find(ActivityCategory.class, 1);
        PSP psp = em.find(PSP.class, provider.getPspId());

        if (renewalCategory == null) {
            result.addError("ActivityCategory ID 1 (Renewal) not found — cannot create ServiceItems.");
            return result;
        }

        for (ImportRow row : state.getRows()) {
            String status = row.getStatus();

            // Skip non-actionable rows
            if ("SKIPPED".equals(status) || "ERROR".equals(status)) {
                result.addSkipped();
                continue;
            }

            Map<String, String> data = row.getCanonicalValues();
            String externalId = row.getExternalId();
            String name = data.getOrDefault("name", "");
            String code = data.getOrDefault("code", "");
            String level = blankToNull(data.getOrDefault("level", ""));
            String los = blankToNull(data.getOrDefault("line_of_service", ""));

            if (name.isEmpty()) {
                result.addError("Row " + row.getRowIndex() + ": empty plan type name, skipping.");
                continue;
            }
            if (code.isEmpty()) code = name;

            try {
                if ("MATCHED".equals(status) || "CONFIRMED".equals(status) || "MANUAL".equals(status)) {
                    // Update existing
                    PlanType existing = em.find(PlanType.class, row.getAmsInternalId());
                    if (existing == null) {
                        result.addError("Row " + row.getRowIndex() + ": PlanType " + row.getAmsInternalId() + " not found.");
                        continue;
                    }

                    boolean changed = false;
                    if (!name.equals(existing.getPlanTypeName())) { existing.setPlanTypeName(name); changed = true; }
                    if (!code.equals(existing.getCode())) { existing.setCode(code); changed = true; }
                    if (!Objects.equals(level, existing.getLevel())) { existing.setLevel(level); changed = true; }
                    if (!Objects.equals(los, existing.getLos())) { existing.setLos(los); changed = true; }

                    if (changed) {
                        em.getTransaction().begin();
                        em.merge(existing);
                        if (!externalId.isEmpty()) {
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.PLAN_TYPE,
                                    externalId, existing.getPlanTypeId(), true);
                        }
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        // Still record the xref even if no fields changed
                        if (!externalId.isEmpty()) {
                            em.getTransaction().begin();
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.PLAN_TYPE,
                                    externalId, existing.getPlanTypeId(), true);
                            em.getTransaction().commit();
                        }
                        result.addSkipped();
                    }

                    // Ensure ServiceItem exists
                    if (existing.getServiceItem() == null) {
                        int renewalMonths = getRenewalMonths(renewalMonthsMap, existing.getPlanTypeId());
                        em.getTransaction().begin();
                        ServiceItem si = createRenewalServiceItem(em, name, code, existing.getPlanTypeId(),
                                renewalMonths, renewalCategory, psp, provider.getProviderCode());
                        existing.setServiceItem(si);
                        em.merge(existing);
                        em.getTransaction().commit();
                        result.addServiceItemCreated();
                    }

                } else if ("UNMATCHED".equals(status)) {
                    // Create new
                    int externalPtId = parseIntSafe(externalId);
                    int internalPtId = externalPtId > 0 ? externalPtId : 0;

                    // Allocate PK if external ID conflicts or is 0
                    if (internalPtId == 0 || ImportIdResolver.internalIdExists(em, ImportIdResolver.PLAN_TYPE, internalPtId)) {
                        internalPtId = ImportIdResolver.allocateInternalId(em, ImportIdResolver.PLAN_TYPE);
                        if (externalPtId > 0) {
                            result.addWarning("PlanType ID " + externalPtId + " conflicts, allocated " + internalPtId);
                        }
                    }

                    int renewalMonths = getRenewalMonths(renewalMonthsMap, internalPtId);

                    em.getTransaction().begin();
                    ServiceItem si = createRenewalServiceItem(em, name, code, internalPtId,
                            renewalMonths, renewalCategory, psp, provider.getProviderCode());
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

                    if (!externalId.isEmpty()) {
                        ImportIdResolver.recordMapping(em, provider, ImportIdResolver.PLAN_TYPE,
                                externalId, internalPtId, true);
                    }
                    em.getTransaction().commit();
                    result.addInserted();
                }
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                result.addError("Row " + row.getRowIndex() + ": " + e.getMessage());
            }

            if (result.total() % 50 == 0) {
                em.clear();
                defaultBg = getOrCreateDefaultBillingGroup(em);
                renewalCategory = em.find(ActivityCategory.class, 1);
                psp = em.find(PSP.class, provider.getPspId());
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  EMPLOYER COMMIT
    // ═══════════════════════════════════════════════════════════════

    private static ImportResult commitEmployers(EntityManager em, ImportProvider provider,
                                                 EntityImportState state) {
        ImportResult result = new ImportResult();

        for (ImportRow row : state.getRows()) {
            String status = row.getStatus();

            if ("SKIPPED".equals(status) || "ERROR".equals(status)) {
                result.addSkipped();
                continue;
            }

            Map<String, String> data = row.getCanonicalValues();
            String externalId = row.getExternalId();
            String employerName = data.getOrDefault("employer_name", "");

            if (employerName.isEmpty()) {
                result.addError("Row " + row.getRowIndex() + ": empty employer name, skipping.");
                continue;
            }

            String contactName = data.getOrDefault("contact_name", "");
            String email = data.getOrDefault("email", "");
            String phone = data.getOrDefault("phone", "");
            int altId = parseIntSafe(data.getOrDefault("alt_id", "0"));
            int erKey = parseIntSafe(data.getOrDefault("er_key", "0"));

            try {
                if ("MATCHED".equals(status) || "CONFIRMED".equals(status) || "MANUAL".equals(status)) {
                    Employer existing = em.find(Employer.class, row.getAmsInternalId());
                    if (existing == null) {
                        result.addError("Row " + row.getRowIndex() + ": Employer " + row.getAmsInternalId() + " not found.");
                        continue;
                    }

                    boolean changed = false;
                    if (!employerName.equals(existing.getEmployerName())) { existing.setEmployerName(employerName); changed = true; }
                    if (!Objects.equals(blankToNull(email), blankToNull(existing.getEmail()))) { existing.setEmail(email); changed = true; }
                    if (!Objects.equals(blankToNull(phone), blankToNull(existing.getPhone()))) { existing.setPhone(phone); changed = true; }
                    if (!Objects.equals(blankToNull(contactName), blankToNull(existing.getContactName()))) { existing.setContactName(contactName); changed = true; }

                    if (changed) {
                        em.getTransaction().begin();
                        em.merge(existing);
                        if (!externalId.isEmpty()) {
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYER,
                                    externalId, existing.getId(), true);
                        }
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        if (!externalId.isEmpty()) {
                            em.getTransaction().begin();
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYER,
                                    externalId, existing.getId(), true);
                            em.getTransaction().commit();
                        }
                        result.addSkipped();
                    }

                } else if ("UNMATCHED".equals(status)) {
                    int externalOrgId = parseIntSafe(externalId);
                    int internalOrgId = externalOrgId > 0 ? externalOrgId : 0;

                    if (internalOrgId == 0 || ImportIdResolver.internalIdExists(em, ImportIdResolver.EMPLOYER, internalOrgId)) {
                        internalOrgId = ImportIdResolver.allocateInternalId(em, ImportIdResolver.EMPLOYER);
                        if (externalOrgId > 0) {
                            result.addWarning("Employer ID " + externalOrgId + " conflicts, allocated " + internalOrgId);
                        }
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

                    if (!externalId.isEmpty()) {
                        ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYER,
                                externalId, internalOrgId, true);
                    }
                    em.getTransaction().commit();
                    result.addInserted();
                }
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                result.addError("Row " + row.getRowIndex() + ": " + e.getMessage());
            }

            if (result.total() % 50 == 0) em.clear();
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BENEFIT COMMIT
    // ═══════════════════════════════════════════════════════════════

    private static ImportResult commitBenefits(EntityManager em, ImportProvider provider,
                                                EntityImportState state,
                                                Map<Integer, Integer> renewalMonthsMap) {
        ImportResult result = new ImportResult();
        String sourceType = provider.getProviderCode();

        for (ImportRow row : state.getRows()) {
            String status = row.getStatus();

            if ("SKIPPED".equals(status) || "ERROR".equals(status)) {
                result.addSkipped();
                continue;
            }

            Map<String, String> data = row.getCanonicalValues();
            String externalId = row.getExternalId();

            // Resolve FK: employer
            String employerExtId = data.getOrDefault("employer_id", "");
            Employer employer = resolveEmployerFk(em, provider, employerExtId);
            if (employer == null) {
                result.addError("Row " + row.getRowIndex() + ": employer '" + employerExtId + "' not found, skipping.");
                continue;
            }

            // Resolve FK: plan type
            String planTypeExtId = data.getOrDefault("plan_type_id", "");
            PlanType planType = resolvePlanTypeFk(em, provider, planTypeExtId);
            if (planType == null) {
                result.addError("Row " + row.getRowIndex() + ": PlanType '" + planTypeExtId + "' not found, skipping.");
                continue;
            }

            String planName = data.getOrDefault("plan_name", "");
            String planDescription = data.getOrDefault("plan_description", "");

            // Cross-populate: if one is provided but the other is not, use it for both
            if (!planName.isEmpty() && planDescription.isEmpty()) planDescription = planName;
            else if (!planDescription.isEmpty() && planName.isEmpty()) planName = planDescription;

            String effectiveDateStr = data.getOrDefault("effective_date", "");
            String terminationDateStr = data.getOrDefault("termination_date", "");

            Date effectiveDate = parseDate(effectiveDateStr);
            Date terminationDate = parseDate(terminationDateStr);
            int renewalMonths = getRenewalMonths(renewalMonthsMap, planType.getPlanTypeId());

            try {
                if ("MATCHED".equals(status) || "CONFIRMED".equals(status) || "MANUAL".equals(status)) {
                    Benefit existing = em.find(Benefit.class, row.getAmsInternalId());
                    if (existing == null) {
                        result.addError("Row " + row.getRowIndex() + ": Benefit " + row.getAmsInternalId() + " not found.");
                        continue;
                    }

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
                        if (!externalId.isEmpty()) {
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT,
                                    externalId, existing.getId(), true);
                        }
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        if (!externalId.isEmpty()) {
                            em.getTransaction().begin();
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT,
                                    externalId, existing.getId(), true);
                            em.getTransaction().commit();
                        }
                        result.addSkipped();
                    }

                } else if ("UNMATCHED".equals(status)) {
                    int summitId = parseIntSafe(externalId);

                    em.getTransaction().begin();
                    Benefit b = new Benefit();
                    b.setSummitId(summitId);
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

                    if (!externalId.isEmpty()) {
                        ImportIdResolver.recordMapping(em, provider, ImportIdResolver.BENEFIT,
                                externalId, b.getId(), true);
                    }
                    em.getTransaction().commit();
                    result.addInserted();
                }
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                result.addError("Row " + row.getRowIndex() + ": " + e.getMessage());
            }

            if (result.total() % 50 == 0) em.clear();
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  EMPLOYEE COMMIT
    // ═══════════════════════════════════════════════════════════════

    private static ImportResult commitEmployees(EntityManager em, ImportProvider provider,
                                                 EntityImportState state) {
        ImportResult result = new ImportResult();
        PSP psp = em.find(PSP.class, provider.getPspId());

        for (ImportRow row : state.getRows()) {
            String status = row.getStatus();

            if ("SKIPPED".equals(status) || "ERROR".equals(status)) {
                result.addSkipped();
                continue;
            }

            Map<String, String> data = row.getCanonicalValues();
            String externalId = row.getExternalId();

            // Resolve FK: employer
            String employerExtId = data.getOrDefault("employer_id", "");
            Employer employer = resolveEmployerFk(em, provider, employerExtId);
            if (employer == null) {
                result.addError("Row " + row.getRowIndex() + ": employer '" + employerExtId + "' not found, skipping.");
                continue;
            }

            String firstName = data.getOrDefault("first_name", "");
            String lastName = data.getOrDefault("last_name", "");
            String email = data.getOrDefault("email", "");
            String hrEmail = data.getOrDefault("hr_email", "");
            String address1 = data.getOrDefault("address1", "");
            String address2 = data.getOrDefault("address2", "");
            String city = data.getOrDefault("city", "");
            String stateVal = data.getOrDefault("state", "");
            String zip = data.getOrDefault("zip", "");
            String customId = data.getOrDefault("custom_id", "");
            String userId = data.getOrDefault("user_id", "");
            String statusStr = data.getOrDefault("status", "");

            boolean isActive = !statusStr.equalsIgnoreCase("Inactive")
                    && !statusStr.equalsIgnoreCase("false")
                    && !statusStr.equals("2");

            try {
                if ("MATCHED".equals(status) || "CONFIRMED".equals(status) || "MANUAL".equals(status)) {
                    Employee existing = em.find(Employee.class, row.getAmsInternalId());
                    if (existing == null) {
                        result.addError("Row " + row.getRowIndex() + ": Employee " + row.getAmsInternalId() + " not found.");
                        continue;
                    }

                    boolean changed = false;
                    if (!firstName.isEmpty() && !firstName.equals(existing.getFirstName())) { existing.setFirstName(firstName); changed = true; }
                    if (!lastName.isEmpty() && !lastName.equals(existing.getLastName())) { existing.setLastName(lastName); changed = true; }
                    if (!email.isEmpty() && !Objects.equals(email, existing.getEmail())) { existing.setEmail(email); changed = true; }
                    if (!hrEmail.isEmpty() && !Objects.equals(hrEmail, existing.getHrEmail())) { existing.setHrEmail(hrEmail); changed = true; }
                    if (!address1.isEmpty() && !Objects.equals(address1, existing.getAddress1())) { existing.setAddress1(address1); changed = true; }
                    if (!address2.isEmpty() && !Objects.equals(address2, existing.getAddress2())) { existing.setAddress2(address2); changed = true; }
                    if (!city.isEmpty() && !Objects.equals(city, existing.getCity())) { existing.setCity(city); changed = true; }
                    if (!stateVal.isEmpty() && !Objects.equals(stateVal, existing.getState())) { existing.setState(stateVal); changed = true; }
                    if (!zip.isEmpty() && !Objects.equals(zip, existing.getZipCode())) { existing.setZipCode(zip); changed = true; }
                    if (existing.isActive() != isActive) { existing.setActive(isActive); changed = true; }

                    if (changed) {
                        em.getTransaction().begin();
                        em.merge(existing);
                        if (!externalId.isEmpty()) {
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYEE,
                                    externalId, existing.getId(), true);
                        }
                        syncPersonForEmployee(em, existing, psp);
                        em.getTransaction().commit();
                        result.addUpdated();
                    } else {
                        em.getTransaction().begin();
                        if (!externalId.isEmpty()) {
                            ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYEE,
                                    externalId, existing.getId(), true);
                        }
                        syncPersonForEmployee(em, existing, psp);
                        em.getTransaction().commit();
                        result.addSkipped();
                    }

                } else if ("UNMATCHED".equals(status)) {
                    int externalEmpId = parseIntSafe(externalId);
                    int internalEmpId = externalEmpId > 0 ? externalEmpId : 0;

                    if (internalEmpId == 0 || ImportIdResolver.internalIdExists(em, ImportIdResolver.EMPLOYEE, internalEmpId)) {
                        internalEmpId = ImportIdResolver.allocateInternalId(em, ImportIdResolver.EMPLOYEE);
                        if (externalEmpId > 0) {
                            result.addWarning("Employee ID " + externalEmpId + " conflicts, allocated " + internalEmpId);
                        }
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
                    ee.setState(stateVal);
                    ee.setZipCode(zip);
                    ee.setCustomId(customId);
                    ee.setUserId(userId);
                    ee.setActive(isActive);
                    em.persist(ee);

                    if (!externalId.isEmpty()) {
                        ImportIdResolver.recordMapping(em, provider, ImportIdResolver.EMPLOYEE,
                                externalId, internalEmpId, true);
                    }
                    syncPersonForEmployee(em, ee, psp);
                    em.getTransaction().commit();
                    result.addInserted();
                }
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                result.addError("Row " + row.getRowIndex() + ": " + e.getMessage());
            }

            if (result.total() % 50 == 0) {
                em.clear();
                psp = em.find(PSP.class, provider.getPspId());
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PERSON / ASSIGNEE SYNC
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find or create a Person linked to this Employee.
     * Match order: existing link → email → first+last name → create new.
     * Must be called within an active transaction.
     */
    private static void syncPersonForEmployee(EntityManager em, Employee employee, PSP psp) {
        // 1. Already linked?
        List<Person> linked = em.createQuery(
                "SELECT p FROM Person p WHERE p.employee.id = :id", Person.class)
                .setParameter("id", employee.getId())
                .setMaxResults(1)
                .getResultList();

        Person person;
        if (!linked.isEmpty()) {
            person = linked.get(0);
        } else {
            person = null;

            // 2. Try email match
            String email = employee.getEmail();
            if (email != null && !email.isEmpty()) {
                List<Person> byEmail = em.createQuery(
                        "SELECT p FROM Person p WHERE p.email = :email AND p.employee IS NULL",
                        Person.class)
                        .setParameter("email", email)
                        .setMaxResults(1)
                        .getResultList();
                if (!byEmail.isEmpty()) {
                    person = byEmail.get(0);
                    person.setEmployee(employee);
                }
            }

            // 3. Try name match within same PSP
            if (person == null && employee.getFirstName() != null && employee.getLastName() != null
                    && !employee.getFirstName().isEmpty() && !employee.getLastName().isEmpty()) {
                List<Person> byName = em.createQuery(
                        "SELECT p FROM Person p WHERE p.firstName = :fn AND p.lastName = :ln " +
                                "AND p.employee IS NULL AND p.psp.id = :pspId", Person.class)
                        .setParameter("fn", employee.getFirstName())
                        .setParameter("ln", employee.getLastName())
                        .setParameter("pspId", psp.getId())
                        .setMaxResults(1)
                        .getResultList();
                if (!byName.isEmpty()) {
                    person = byName.get(0);
                    person.setEmployee(employee);
                }
            }

            // 4. Create new Person + Address
            if (person == null) {
                Address a = new Address();
                a.setAddress1(employee.getAddress1());
                a.setAddress2(employee.getAddress2());
                a.setCity(employee.getCity());
                String st = employee.getState();
                a.setState(st != null && st.length() >= 2 ? st.substring(0, 2) : st);
                a.setZipCode(employee.getZipCode());
                em.persist(a);

                person = new Person();
                person.setEmployee(employee);
                person.setAddress(a);
                person.setPsp(psp);
            }
        }

        // Update Person fields from Employee
        if (employee.getFirstName() != null && !employee.getFirstName().isEmpty())
            person.setFirstName(employee.getFirstName());
        if (employee.getLastName() != null && !employee.getLastName().isEmpty())
            person.setLastName(employee.getLastName());
        if (employee.getEmail() != null && !employee.getEmail().isEmpty())
            person.setEmail(employee.getEmail());

        // Update address fields if Person already has one
        if (person.getAddress() != null) {
            Address addr = person.getAddress();
            if (employee.getAddress1() != null && !employee.getAddress1().isEmpty()) addr.setAddress1(employee.getAddress1());
            if (employee.getAddress2() != null) addr.setAddress2(employee.getAddress2());
            if (employee.getCity() != null && !employee.getCity().isEmpty()) addr.setCity(employee.getCity());
            String st = employee.getState();
            if (st != null && !st.isEmpty()) addr.setState(st.length() >= 2 ? st.substring(0, 2) : st);
            if (employee.getZipCode() != null && !employee.getZipCode().isEmpty()) addr.setZipCode(employee.getZipCode());
        }

        if (person.getId() == null || person.getId() == 0) {
            em.persist(person);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FK RESOLUTION HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resolve employer FK: xref first, then direct PK fallback.
     */
    private static Employer resolveEmployerFk(EntityManager em, ImportProvider provider, String employerExtId) {
        if (employerExtId == null || employerExtId.isEmpty()) return null;

        // Try cross-reference
        Employer er = ImportIdResolver.resolveEntity(em, Employer.class,
                provider.getId(), ImportIdResolver.EMPLOYER, employerExtId);
        if (er != null) return er;

        // Fallback: direct PK
        int directId = parseIntSafe(employerExtId);
        if (directId > 0) return em.find(Employer.class, directId);

        return null;
    }

    /**
     * Resolve plan type FK: xref first, then direct PK fallback.
     */
    private static PlanType resolvePlanTypeFk(EntityManager em, ImportProvider provider, String planTypeExtId) {
        if (planTypeExtId == null || planTypeExtId.isEmpty()) return null;

        // Try cross-reference
        PlanType pt = ImportIdResolver.resolveEntity(em, PlanType.class,
                provider.getId(), ImportIdResolver.PLAN_TYPE, planTypeExtId);
        if (pt != null) return pt;

        // Fallback: direct PK
        int directId = parseIntSafe(planTypeExtId);
        if (directId > 0) return em.find(PlanType.class, directId);

        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SHARED HELPERS (match UniversalImportService patterns)
    // ═══════════════════════════════════════════════════════════════

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

    private static int getRenewalMonths(Map<Integer, Integer> map, int planTypeId) {
        if (map != null && map.containsKey(planTypeId)) {
            return map.get(planTypeId);
        }
        return 12;
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

    private static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();

        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy"));
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            return Date.valueOf(ld);
        } catch (DateTimeParseException ignored) {}

        return null;
    }
}

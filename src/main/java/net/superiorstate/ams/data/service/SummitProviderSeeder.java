package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.imports.ImportFieldMapping;
import net.superiorstate.ams.model.imports.ImportFileType;
import net.superiorstate.ams.model.imports.ImportProvider;

/**
 * Seeds a "DataPath Summit" provider configuration for the Universal Import system.
 * Creates the provider, file type definitions, and column mappings that match
 * the existing Summit CSV/Excel export format.
 *
 * This proves the universal import system can handle Summit's format and provides
 * a ready-to-use configuration for existing Summit users.
 *
 * Idempotent: checks for existing SUMMIT provider before creating.
 */
public class SummitProviderSeeder {

    /**
     * Seed the DataPath Summit provider configuration.
     * @param em Active EntityManager (caller manages transaction boundaries)
     * @param pspId The PSP ID to associate the provider with
     */
    public static void seed(EntityManager em, long pspId) {
        // Idempotency check
        Long existing = em.createQuery(
                "SELECT COUNT(p) FROM ImportProvider p WHERE p.providerCode = 'SUMMIT' AND p.pspId = :pspId",
                Long.class)
                .setParameter("pspId", pspId)
                .getSingleResult();
        if (existing > 0) {
            System.out.println("[SummitProviderSeeder] SUMMIT provider already exists for PSP " + pspId + " — skipping.");
            return;
        }

        em.getTransaction().begin();

        // ── Provider ──
        ImportProvider provider = new ImportProvider();
        provider.setProviderName("DataPath Summit");
        provider.setProviderCode("SUMMIT");
        provider.setDescription("DataPath's Summit benefits administration platform. Imports plan types (Excel), employers (J1 CSV), employees (J2/J3 CSV), benefits (J4 CSV), plan years (J5 CSV), and COBRA (J7 CSV).");
        provider.setPspId(pspId);
        em.persist(provider);
        em.flush(); // ensure provider ID is assigned

        // ── File Types ──
        ImportFileType ftPlanTypes = createFileType(em, provider, "Plan Types", "PLAN_TYPE", "EXCEL", 1, true,
                "Summit plan type export (Excel). Categorizes benefits: FSA, HRA, COBRA, etc.");

        ImportFileType ftEmployers = createFileType(em, provider, "Employer Listing (J1)", "EMPLOYER", "CSV", 2, true,
                "J1 — Employer Listing CSV from Summit.");

        ImportFileType ftEmployeesJ2 = createFileType(em, provider, "Participant Contact (J2)", "EMPLOYEE", "CSV", 3, false,
                "J2 — Participant Listing Simple: names, email, addresses.");

        ImportFileType ftEmployeesJ3 = createFileType(em, provider, "Participant Status (J3)", "EMPLOYEE", "CSV", 4, false,
                "J3 — Participant Listing Report: status IDs, hire/term dates.");

        ImportFileType ftBenefitsCDH = createFileType(em, provider, "Benefits CDH (J4)", "BENEFIT", "CSV", 5, true,
                "J4 — CDH Employer Benefit Plans: FSA, HRA, etc. Drives renewal pipeline.");

        ImportFileType ftBenefitPlanYears = createFileType(em, provider, "Benefit Plan Years (J5)", "BENEFIT", "CSV", 6, false,
                "J5 — CDH plan year data. Updates plan year start/end and recalculates renewal dates on J4 benefits.");

        ImportFileType ftBenefitsCobra = createFileType(em, provider, "Benefits COBRA (J7)", "BENEFIT", "CSV", 7, false,
                "J7 — COBRA / Post-Benefit Employer Plans.");

        // ── Plan Type Mappings (Excel) ──
        addMapping(em, ftPlanTypes, "plan type id", "plan_type_id", true, true, null);
        addMapping(em, ftPlanTypes, "plan type code", "code", true, false, null);
        addMapping(em, ftPlanTypes, "plan type name", "name", true, false, null);
        addMapping(em, ftPlanTypes, "level", "level", false, false, null);
        addMapping(em, ftPlanTypes, "los", "line_of_service", false, false, null);

        // ── Employer Mappings (J1 CSV) ──
        addMapping(em, ftEmployers, "organizationid", "employer_id", true, true, null);
        addMapping(em, ftEmployers, "employername", "employer_name", true, false, null);
        addMapping(em, ftEmployers, "primarycontact", "contact_name", false, false, null);
        addMapping(em, ftEmployers, "email", "email", false, false, null);
        addMapping(em, ftEmployers, "phonenumber", "phone", false, false, null);
        addMapping(em, ftEmployers, "customid", "alt_id", false, false, null);
        addMapping(em, ftEmployers, "taxid", "er_key", false, false, null);
        addMapping(em, ftEmployers, "status", "status", false, false, null);

        // ── Employee J2 Mappings (Contact Info CSV) ──
        addMapping(em, ftEmployeesJ2, "participant_id", "employee_id", true, true, null);
        addMapping(em, ftEmployeesJ2, "organization_id", "employer_id", true, false, null);
        addMapping(em, ftEmployeesJ2, "firstname", "first_name", true, false, null);
        addMapping(em, ftEmployeesJ2, "lastname", "last_name", true, false, null);
        addMapping(em, ftEmployeesJ2, "email", "email", false, false, null);
        addMapping(em, ftEmployeesJ2, "address1", "address1", false, false, null);
        addMapping(em, ftEmployeesJ2, "address2", "address2", false, false, null);
        addMapping(em, ftEmployeesJ2, "city", "city", false, false, null);
        addMapping(em, ftEmployeesJ2, "state", "state", false, false, null);
        addMapping(em, ftEmployeesJ2, "zipcode", "zip", false, false, null);
        addMapping(em, ftEmployeesJ2, "participantcustomid", "custom_id", false, false, null);
        addMapping(em, ftEmployeesJ2, "user_id", "user_id", false, false, null);

        // ── Employee J3 Mappings (Status CSV) ──
        addMapping(em, ftEmployeesJ3, "participant_id", "employee_id", true, true, null);
        addMapping(em, ftEmployeesJ3, "organization_id", "employer_id", true, false, null);
        addMapping(em, ftEmployeesJ3, "participant first name", "first_name", true, false, null);
        addMapping(em, ftEmployeesJ3, "participant last name", "last_name", true, false, null);
        addMapping(em, ftEmployeesJ3, "participantstatusid", "status", false, false, null);

        // ── Benefits CDH J4 Mappings ──
        addMapping(em, ftBenefitsCDH, "employerplan_id", "benefit_id", true, true, null);
        addMapping(em, ftBenefitsCDH, "organizationid", "employer_id", true, false, null);
        addMapping(em, ftBenefitsCDH, "plantypeid", "plan_type_id", true, false, null);
        addMapping(em, ftBenefitsCDH, "planname", "plan_name", false, false, null);
        addMapping(em, ftBenefitsCDH, "plandescription", "plan_description", false, false, null);
        addMapping(em, ftBenefitsCDH, "effectivedate", "effective_date", false, false, null);
        addMapping(em, ftBenefitsCDH, "terminationdate", "termination_date", false, false, null);
        addMapping(em, ftBenefitsCDH, "planstatus", "status", false, false, null);

        // ── Benefit Plan Years J5 Mappings ──
        addMapping(em, ftBenefitPlanYears, "employerplan_id", "benefit_id", true, true, null);
        addMapping(em, ftBenefitPlanYears, "planyear", "plan_year_range", true, false, null);

        // ── Benefits COBRA J7 Mappings ──
        addMapping(em, ftBenefitsCobra, "benefitid", "benefit_id", true, true, null);
        addMapping(em, ftBenefitsCobra, "organizationid", "employer_id", true, false, null);
        addMapping(em, ftBenefitsCobra, "plantypeid", "plan_type_id", true, false, null);
        addMapping(em, ftBenefitsCobra, "benefitname", "plan_name", false, false, null);
        addMapping(em, ftBenefitsCobra, "effectivedate", "effective_date", false, false, null);
        addMapping(em, ftBenefitsCobra, "enddate", "termination_date", false, false, null);

        em.getTransaction().commit();

        System.out.println("[SummitProviderSeeder] DataPath Summit provider seeded for PSP " + pspId
                + " with 7 file types and column mappings.");
    }

    private static ImportFileType createFileType(EntityManager em, ImportProvider provider,
                                                   String label, String targetEntity, String format,
                                                   int sortOrder, boolean required, String description) {
        ImportFileType ft = new ImportFileType();
        ft.setProvider(provider);
        ft.setFileLabel(label);
        ft.setTargetEntity(targetEntity);
        ft.setFileFormat(format);
        ft.setSortOrder(sortOrder);
        ft.setRequired(required);
        ft.setDescription(description);
        em.persist(ft);
        em.flush(); // ensure ID is assigned for FK references
        return ft;
    }

    private static void addMapping(EntityManager em, ImportFileType fileType,
                                    String sourceColumn, String canonicalField,
                                    boolean required, boolean isKey, String transformRule) {
        ImportFieldMapping m = new ImportFieldMapping();
        m.setFileType(fileType);
        m.setSourceColumn(sourceColumn);
        m.setCanonicalField(canonicalField);
        m.setRequired(required);
        m.setKey(isKey);
        m.setTransformRule(transformRule);
        em.persist(m);
    }
}

package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.util.BillingHelper;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.HsaBillingHelper;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.BenefitTier;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;
import net.superiorstate.ams.previous.model.summit.imports.HsaAccount;
import net.superiorstate.ams.previous.model.summit.imports.HsaEe;
import net.superiorstate.ams.previous.model.summit.imports.HsaEr;
import net.superiorstate.ams.previous.model.summit.imports.order.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Updater {

    private static final Map<String, Integer> COBRA_STATUS_PRIORITY = Map.of(
            "COBRA", 2,
            "Qualified Beneficiary", 1,
            "Terminated", 0
    );


    public static void processEmployerImportI1FastAddsActiveOnly(EntityManager em) {
        System.out.println("Firing: I1 Fast");
        List<ImportEmployer> newActiveOrgs = em.createQuery(
                "SELECT i FROM ImportEmployer i " +
                        "WHERE LOWER(i.status) = 'active' " +
                        "AND i.organizationId NOT IN (SELECT e.id FROM Employer e)",
                ImportEmployer.class
        ).getResultList();

        for (ImportEmployer row : newActiveOrgs) {
            try {
                em.getTransaction().begin();

                Employer employer = new Employer();
                employer.setId(row.getOrganizationId());
                employer.setEmployerName(row.getEmployerName().trim().toUpperCase());
                employer.setAltId(row.getEmployerId());
                employer.setEmail(row.getEmail());
                employer.setContactName(row.getPrimaryContact());
                employer.setPhone(row.getPhone());
                employer.setActive(true); // always true because we filtered by status

                em.persist(employer);
                em.getTransaction().commit();

                //System.out.println("Added new ACTIVE Employer: " + employer.getEmployerName());

            } catch (Exception ex) {
                em.getTransaction().rollback();
                System.err.println("Failed to insert Employer " + row.getOrganizationId() + ": " + ex.getMessage());
            }
        }
    }
    public static void processEmployerImportI1SelectiveUpdates(EntityManager em) {
        System.out.println("Firing: I1 Selective");
        List<ImportEmployer> toUpdate = em.createQuery(
                "SELECT i FROM ImportEmployer i " +
                        "JOIN Employer e ON i.organizationId = e.id " +
                        "WHERE " +
                        "  (LOWER(TRIM(i.employerName)) <> LOWER(TRIM(e.employerName)) " +
                        "   OR " +
                        "   (LOWER(TRIM(i.status)) = 'active' AND e.isActive = false) " +
                        "   OR " +
                        "   (LOWER(TRIM(i.status)) <> 'active' AND e.isActive = true))",
                ImportEmployer.class
        ).getResultList();

        for (ImportEmployer row : toUpdate) {
            try {
                em.getTransaction().begin();

                Employer employer = em.find(Employer.class, row.getOrganizationId());
                if (employer != null) {
                    boolean updated = false;

                    String trimmedImportName = row.getEmployerName().trim();
                    if (!employer.getEmployerName().trim().equalsIgnoreCase(trimmedImportName)) {
                        employer.setEmployerName(trimmedImportName);
                        updated = true;
                    }

                    boolean isActiveInImport = "active".equalsIgnoreCase(row.getStatus());
                    if (employer.isActive() != isActiveInImport) {
                        employer.setActive(isActiveInImport);
                        updated = true;
                    }

                    if (updated) {
                        em.merge(employer);
                        System.out.println("Updated Employer ID=" + employer.getId());
                    }
                }

                em.getTransaction().commit();
            } catch (Exception ex) {
                em.getTransaction().rollback();
                System.err.println("Error updating Employer " + row.getOrganizationId() + ": " + ex.getMessage());
            }
        }
    }
    public static void processEmployeeAddsFromI2I3(EntityManager em) {
        System.out.println("Firing: I2I3 Adds");
        final int batchSize = 200;
        int count = 0;

        Set<Integer> importIds = new HashSet<>();
        Map<Integer, ImportEmployee> i2Map = new HashMap<>();
        Map<Integer, ImportEmployeeAlt> i3Map = new HashMap<>();

        // Step 1: Load import rows
        for (ImportEmployee row : em.createQuery("SELECT i FROM ImportEmployee i", ImportEmployee.class).getResultList()) {
            int id = row.getId();
            importIds.add(id);
            i2Map.putIfAbsent(id, row);
        }

        for (ImportEmployeeAlt row : em.createQuery("SELECT i FROM ImportEmployeeAlt i", ImportEmployeeAlt.class).getResultList()) {
            int id = row.getParticipantId();
            if (!importIds.contains(id)) {
                importIds.add(id);
                i3Map.put(id, row);
            }
        }

        if (importIds.isEmpty()) {
            System.out.println("No employee IDs found in I2/I3 imports.");
            return;
        }

        // Step 2: Filter out already existing Employee records
        List<Integer> existingEmployeeIds = em.createQuery(
                        "SELECT e.id FROM Employee e WHERE e.id IN :ids", Integer.class)
                .setParameter("ids", importIds)
                .getResultList();
        importIds.removeAll(existingEmployeeIds);

        // Step 3: Batch insert new Employees
        em.getTransaction().begin();
        List<Employee> newEmployees = new ArrayList<>();

        for (Integer newId : importIds) {
            try {
                ImportEmployee i2row = i2Map.get(newId);
                ImportEmployeeAlt i3row = i3Map.get(newId);

                Employee emp = new Employee();
                emp.setId(newId);

                if (i2row != null) {
                    emp.setFirstName(BillingHelper.trimForDb(i2row.getFirstName(), 50));
                    emp.setLastName(BillingHelper.trimForDb(i2row.getLastName(), 50));
                    emp.setEmail(i2row.getEmail());
                    emp.setAddress1(i2row.getAddress1());
                    emp.setCity(i2row.getCity());
                    emp.setState(i2row.getState());
                    emp.setZipCode(i2row.getZipCode());
                    emp.setUserId(i2row.getUserId());
                    emp.setActive("Active".equalsIgnoreCase(i2row.getUserStatus()));

                    if (i2row.getImportEmployer() != null) {
                        emp.setEmployer(em.find(Employer.class, i2row.getImportEmployer().getOrganizationId()));
                    }
                } else if (i3row != null) {
                    emp.setFirstName(BillingHelper.trimForDb(i3row.getParticipantFirst(), 50));
                    emp.setLastName(BillingHelper.trimForDb(i3row.getParticipantLast(), 50));
                    emp.setUserId(i3row.getUserId());
                    emp.setActive("Active".equalsIgnoreCase(i3row.getUserStatus()));
                    emp.setEeStatusId(i3row.getParticipantStatusId());
                    emp.setSystemStatusId(i3row.getUserStatusId());
                    emp.setCobraStatusId(i3row.getEmploymentStatusId());

                    if (i3row.getImportEmployer() != null) {
                        emp.setEmployer(em.find(Employer.class, i3row.getImportEmployer().getOrganizationId()));
                    }
                }

                em.persist(emp);
                newEmployees.add(emp);

                if (++count % batchSize == 0) {
                    em.flush();
                    em.clear();
                }

            } catch (Exception ex) {
                System.err.println("Error creating Employee ID=" + newId + ": " + ex.getMessage());
            }
        }

        em.getTransaction().commit();

        // Step 4: Batch link to Person entities
        batchLinkPersonsToEmployees(em, newEmployees);
    }
    public static void processEmployeeUpdatesFromI2Efficient(EntityManager em) {
        System.out.println("Firing: I2 Updates");
        // Step 1: Find mismatches only
        List<ImportEmployee> mismatches = em.createQuery(
                "SELECT i FROM ImportEmployee i " +
                        "JOIN Employee e ON i.id = e.id " +
                        "WHERE " +
                        "  TRIM(LOWER(i.firstName)) <> TRIM(LOWER(e.firstName)) OR " +
                        "  TRIM(LOWER(i.lastName)) <> TRIM(LOWER(e.lastName)) OR " +
                        "  (i.email IS NOT NULL AND e.email IS NULL) OR " +
                        "  (i.email IS NULL AND e.email IS NOT NULL) OR " +
                        "  TRIM(LOWER(i.email)) <> TRIM(LOWER(e.email))",
                ImportEmployee.class
        ).getResultList();

        // Step 2: Update only those that need it
        for (ImportEmployee row : mismatches) {
            int id = row.getId();
            try {
                em.getTransaction().begin();

                Employee emp = em.find(Employee.class, id);
                if (emp != null) {
                    boolean updated = false;

                    if (!Objects.equals(trimIgnoreCase(emp.getFirstName()), trimIgnoreCase(row.getFirstName()))) {
                        emp.setFirstName(row.getFirstName());
                        updated = true;
                    }

                    if (!Objects.equals(trimIgnoreCase(emp.getLastName()), trimIgnoreCase(row.getLastName()))) {
                        emp.setLastName(row.getLastName());
                        updated = true;
                    }

                    if (!Objects.equals(trimIgnoreCase(emp.getEmail()), trimIgnoreCase(row.getEmail()))) {
                        emp.setEmail(row.getEmail());
                        updated = true;
                    }

                    if (updated) {
                        em.merge(emp);
                        System.out.println("Updated Employee ID=" + id);
                    }
                }

                em.getTransaction().commit();
            } catch (Exception ex) {
                em.getTransaction().rollback();
                System.err.println("Failed to update Employee ID=" + id + ": " + ex.getMessage());
            }
        }
    }
    public static void reAssociateOrphanEmployees(EntityManager em) {
        System.out.println("Firing: Orphans");
        List<Employee> orphans = em.createQuery(
                "SELECT e FROM Employee e WHERE e.employer IS NULL", Employee.class).getResultList();

        if (orphans.isEmpty()) {
            System.out.println("No orphan employees found.");
            return;
        }

        for (Employee emp : orphans) {
            int id = emp.getId();
            boolean updated = false;

            try {
                em.getTransaction().begin();

                // Try I2 (ImportEmployee)
                ImportEmployee i2 = em.find(ImportEmployee.class, id);
                if (i2 != null && i2.getImportEmployer() != null) {
                    Employer employer = em.find(Employer.class, i2.getImportEmployer().getOrganizationId());
                    if (employer != null) {
                        emp.setEmployer(employer);
                        updated = true;
                    }
                } else {
                    // Try I3 (ImportEmployeeAlt)
                    ImportEmployeeAlt i3 = em.find(ImportEmployeeAlt.class, id);
                    if (i3 != null && i3.getImportEmployer() != null) {
                        Employer employer = em.find(Employer.class, i3.getImportEmployer().getOrganizationId());
                        if (employer != null) {
                            emp.setEmployer(employer);
                            updated = true;
                        }
                    }
                }

                if (updated) {
                    em.merge(emp);
                    em.getTransaction().commit();
                    System.out.println("Updated orphan Employee ID=" + id + " with employer.");
                } else {
                    em.getTransaction().rollback();
                    System.out.println("No employer found for orphan Employee ID=" + id);
                }

            } catch (Exception ex) {
                em.getTransaction().rollback();
                System.err.println("Error processing orphan Employee ID=" + id + ": " + ex.getMessage());
            }
        }
    }
    public static void updateEmployeeActiveStatusQueryDriven(EntityManager em) {
        System.out.println("Firing:  EE Active Status");
        List<Employee> mismatched = em.createQuery(
                "SELECT e FROM Employee e " +
                        "JOIN ImportEmployeeAlt s ON e.id = s.participantId " +
                        "WHERE " +
                        "  e.systemStatusId <> s.participantStatusId " +
                        "  OR e.eeStatusId <> s.employmentStatusId " +
                        "  OR (e.isActive = true AND (s.participantStatusId <> 1 OR s.employmentStatusId NOT IN (1,2,4,6,7))) " +
                        "  OR (e.isActive = false AND (s.participantStatusId = 1 AND s.employmentStatusId IN (1,2,4,6,7)))",
                Employee.class
        ).getResultList();

        if (mismatched.isEmpty()) {
            System.out.println("No employee status mismatches found.");
            return;
        }

        em.getTransaction().begin();

        for (Employee e : mismatched) {
            ImportEmployeeAlt s = em.find(ImportEmployeeAlt.class, e.getId());
            if (s == null) continue;

            boolean shouldBeActive = s.getParticipantStatusId() == 1 &&
                    Set.of(1,2,4,6,7).contains(s.getEmploymentStatusId());

            e.setSystemStatusId(s.getParticipantStatusId());
            e.setEeStatusId(s.getEmploymentStatusId());
            e.setActive(shouldBeActive);

            em.merge(e);
        }

        em.getTransaction().commit();
        System.out.println("Updated " + mismatched.size() + " mismatched employees.");
    }
    public static void makeEmployeesInactiveIfNotInI2OrI3(EntityManager em) {
        System.out.println("Firing: Inactives");

        List<Employee> toDeactivate = em.createQuery(
                "SELECT e FROM Employee e " +
                        "WHERE e.isActive = true " +
                        "AND e.id > 0 " + // ✅ Exclude negative ID employees
                        "AND e.id NOT IN (" +
                        "    SELECT i.id FROM ImportEmployee i" +
                        ") " +
                        "AND e.id NOT IN (" +
                        "    SELECT a.participantId FROM ImportEmployeeAlt a" +
                        ")",
                Employee.class
        ).getResultList();

        if (toDeactivate.isEmpty()) {
            System.out.println("No employees need to be marked inactive.");
            return;
        }

        em.getTransaction().begin();

        for (Employee e : toDeactivate) {
            e.setActive(false);
            em.merge(e);
        }

        em.getTransaction().commit();

        System.out.println("✅ Deactivated " + toDeactivate.size() + " employees not found in I2 or I3 imports.");
    }
    public static void updateEmployeeStatusFromCobraList(EntityManager em) {
        System.out.println("Firing: EE Status Cobra List");
        List<ImportCobraQb> cobraRows = fetchCobraList(em);
        if (cobraRows == null || cobraRows.isEmpty()) {
            System.out.println("No COBRA data found.");
            return;
        }

        // Step 1: Map participantSystemId -> highest status value (2 = COBRA, 1 = QB, 0 = Terminated)
        Map<Integer, Integer> cobraStatusMap = new HashMap<>();

        for (ImportCobraQb row : cobraRows) {
            int participantId = row.getParticipantSystemId();
            String status = row.getCoverageStatus();
            int priority = COBRA_STATUS_PRIORITY.getOrDefault(status.trim(), -1);

            cobraStatusMap.merge(participantId, priority, Math::max);
        }

        if (cobraStatusMap.isEmpty()) {
            System.out.println("No valid COBRA status entries to apply.");
            return;
        }

        // Step 2: Fetch all matching employees
        List<Employee> employees = em.createQuery(
                        "SELECT e FROM Employee e WHERE e.id IN :ids", Employee.class)
                .setParameter("ids", cobraStatusMap.keySet())
                .getResultList();

        int updateCount = 0;
        em.getTransaction().begin();

        for (Employee e : employees) {
            int cobraStatus = cobraStatusMap.getOrDefault(e.getId(), -1);
            if (cobraStatus >= 0 && !Objects.equals(e.getCobraStatusId(), cobraStatus)) {
                e.setCobraStatusId(cobraStatus);
                em.merge(e);
                updateCount++;

                // Optional: batch commit every 200 records
                if (updateCount % 200 == 0) {
                    em.getTransaction().commit();
                    em.getTransaction().begin();
                }
            }
        }

        em.getTransaction().commit();
        System.out.println("Updated COBRA status for " + updateCount + " employees.");
    }
    public static void updateEeCobraStatus(EntityManager em) {
        System.out.println("Firing: EE Cobra Status");
        Map<Integer, Integer> cobraStatusMap = new HashMap<>();

        // Step 1: From ImportCobTerm (COBRA Termed = 1)
        List<ImportCobTerm> cobTerms = em.createQuery("SELECT t FROM ImportCobTerm t", ImportCobTerm.class).getResultList();
        for (ImportCobTerm t : cobTerms) {
            int employeeId = BillingHelper.matchByCustomId(em, t);
            if (employeeId == 0) employeeId = BillingHelper.matchBySsn(em, t);
            if (employeeId == 0) employeeId = BillingHelper.matchByName(em, t);
            if (employeeId != 0) {
                cobraStatusMap.merge(employeeId, 1, Math::max); // 1 = Termed
            }
        }

        // Step 2: From ImportCobraQb (Qualified Beneficiary = 2)
        List<ImportCobraQb> qbs = em.createQuery("SELECT qb FROM ImportCobraQb qb", ImportCobraQb.class).getResultList();
        for (ImportCobraQb qb : qbs) {
            int employeeId = qb.getParticipantSystemId();
            cobraStatusMap.merge(employeeId, 2, Math::max); // 2 = Qualified Beneficiary
        }

        // Step 3: From ImportCobPart (Active COBRA = 3)
        List<ImportCobPart> participants = em.createQuery("SELECT p FROM ImportCobPart p", ImportCobPart.class).getResultList();
        for (ImportCobPart p : participants) {
            int employeeId = p.getParticipantId();
            cobraStatusMap.merge(employeeId, 3, Math::max); // 3 = Active COBRA
        }

        if (cobraStatusMap.isEmpty()) {
            System.out.println("No COBRA statuses to apply.");
            return;
        }

        // Step 4: Fetch matching employees
        List<Employee> employees = em.createQuery(
                        "SELECT e FROM Employee e WHERE e.id IN :ids", Employee.class)
                .setParameter("ids", cobraStatusMap.keySet())
                .getResultList();

        // Step 5: Apply highest-status updates
        int updatedCount = 0;
        em.getTransaction().begin();

        for (Employee e : employees) {
            int newStatus = cobraStatusMap.get(e.getId());
            if (!Objects.equals(e.getCobraStatusId(), newStatus)) {
                e.setCobraStatusId(newStatus);
                em.merge(e);
                updatedCount++;

                // Optional: commit every 200
                if (updatedCount % 200 == 0) {
                    em.getTransaction().commit();
                    em.getTransaction().begin();
                }
            }
        }

        em.getTransaction().commit();
        System.out.println("Updated COBRA status for " + updatedCount + " employees.");
    }
    public static void ensurePrimaryContactEmployee(EntityManager em) {
        System.out.println("🔁 Firing: Ensure Primary Contact Employees");

        // 1. Fetch employers with a valid contact email
        List<Employer> employers = em.createQuery(
                        "SELECT e FROM Employer e WHERE e.email IS NOT NULL AND TRIM(e.email) <> ''", Employer.class)
                .getResultList();

        if (employers.isEmpty()) {
            System.out.println("❌ No employers with contact emails found.");
            return;
        }

        // 2. Normalize emails
        Map<Integer, String> employerEmailMap = employers.stream()
                .collect(Collectors.toMap(
                        Employer::getId,
                        e -> e.getEmail().trim().toLowerCase()
                ));

        // 3. Fetch all employees (positive and negative) for those employers
        List<Employee> allEmployees = em.createQuery(
                        "SELECT e FROM Employee e WHERE e.employer.id IN :ids", Employee.class)
                .setParameter("ids", employerEmailMap.keySet())
                .getResultList();

        // 4. Group employees by employer
        Map<Integer, List<Employee>> employeesByEmployer = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEmployer().getId(),
                        Collectors.toCollection(ArrayList::new) // ensures mutability
                ));

        // 5. Track the lowest existing negative ID
        Integer minId = (Integer) em.createQuery("SELECT MIN(e.id) FROM Employee e").getSingleResult();
        int nextId = (minId != null && minId < 0) ? minId - 1 : -1;

        int created = 0;
        em.getTransaction().begin();

        for (Employer employer : employers) {
            Integer employerId = employer.getId();
            String contactEmail = employer.getEmail().trim().toLowerCase();
            String contactName = employer.getContactName() != null ? employer.getContactName().trim() : "";

            // Parse contact name into first/last
            String firstName = "Primary";
            String lastName = "Contact";
            if (!contactName.isEmpty()) {
                String[] parts = contactName.split("\\s+", 2);
                firstName = parts[0];
                if (parts.length > 1) lastName = parts[1];
            }

            String normFirst = Validator.normalizeName(firstName);
            String normLast = Validator.normalizeName(lastName);

            // Use mutable list or create new one if not present
            List<Employee> scopedEmployees = employeesByEmployer.computeIfAbsent(employerId, k -> new ArrayList<>());

            // Match check: email or normalized name
            boolean matchFound = scopedEmployees.stream().anyMatch(e ->
                    (e.getEmail() != null && e.getEmail().trim().equalsIgnoreCase(contactEmail)) ||
                            (Validator.normalizeName(e.getFirstName()).equals(normFirst) &&
                                    Validator.normalizeName(e.getLastName()).equals(normLast))
            );

            if (matchFound) {
                System.out.println("⚠️ Skipping: Match found for Employer ID=" + employerId);
                continue;
            }

            // Create and persist new Employee
            Employee contact = new Employee();
            contact.setId(nextId--);
            contact.setEmployer(employer);
            contact.setEmail(contactEmail);
            contact.setFirstName(BillingHelper.trimForDb(firstName, 50));
            contact.setLastName(BillingHelper.trimForDb(lastName, 50));
            contact.setActive(true);

            em.persist(contact);
            scopedEmployees.add(contact); // Safe now

            System.out.println("✅ Created contact for Employer ID=" + employerId +
                    ": " + contactEmail + " (" + firstName + " " + lastName + ")");

            if (++created % 100 == 0) {
                em.flush();
                em.clear();
            }
        }

        em.getTransaction().commit();
        System.out.println("✅ Batch complete. " + created + " primary contacts created.");
    }

    public static void mergeNegativeToPositiveEmployees(EntityManager em) {
        System.out.println("Firing: Merge Negatives");

        List<Employee> negatives = em.createQuery(
                        "SELECT e FROM Employee e WHERE e.id < 0", Employee.class)
                .getResultList();

        if (negatives.isEmpty()) {
            System.out.println("No negative employees to process.");
            return;
        }

        // 1. Collect all employers + emails to use in a single preload
        Set<Integer> employerIds = negatives.stream()
                .map(e -> e.getEmployer().getId())
                .collect(Collectors.toSet());

        Set<String> emails = negatives.stream()
                .map(e -> e.getEmail() != null ? e.getEmail().trim().toLowerCase() : "")
                .filter(e -> !e.isBlank())
                .collect(Collectors.toSet());

        // 2. Load all positive employees scoped to these employers
        List<Employee> positiveEmployees = em.createQuery(
                        "SELECT e FROM Employee e WHERE e.id > 0 AND e.employer.id IN :employerIds", Employee.class)
                .setParameter("employerIds", employerIds)
                .getResultList();

        // Group by employer for quick lookup
        Map<Integer, List<Employee>> positiveByEmployer = positiveEmployees.stream()
                .collect(Collectors.groupingBy(e -> e.getEmployer().getId()));

        em.getTransaction().begin();
        int merged = 0;

        for (Employee neg : negatives) {
            Integer employerId = neg.getEmployer().getId();
            String normEmail = neg.getEmail() != null ? neg.getEmail().trim().toLowerCase() : null;
            String normFirst = Validator.normalizeName(neg.getFirstName());
            String normLast = Validator.normalizeName(neg.getLastName());

            List<Employee> candidates = positiveByEmployer.getOrDefault(employerId, List.of());

            // Try email match first
            Optional<Employee> match = Optional.empty();
            if (normEmail != null) {
                match = candidates.stream()
                        .filter(e -> e.getEmail() != null && e.getEmail().trim().equalsIgnoreCase(normEmail))
                        .findFirst();
            }

            // Fallback: name match
            if (match.isEmpty()) {
                match = candidates.stream()
                        .filter(e ->
                                Validator.normalizeName(e.getFirstName()).equals(normFirst) &&
                                        Validator.normalizeName(e.getLastName()).equals(normLast))
                        .findFirst();
            }

            if (match.isEmpty()) {
                System.out.println("❌ No match found for neg ID " + neg.getId() + " — skipping.");
                continue;
            }

            Employee pos = match.get();

            if (pos.getId() < 0 || pos.getId() == neg.getId()) {
                System.out.println("🚫 Invalid match: match has negative or same ID (" + pos.getId() + ") — skipping.");
                continue;
            }

            try {
                // Move HsaEe
                List<HsaEe> hsaEes = em.createQuery(
                                "SELECT h FROM HsaEe h WHERE h.employee = :neg", HsaEe.class)
                        .setParameter("neg", neg)
                        .getResultList();
                for (HsaEe h : hsaEes) {
                    h.setEmployee(pos);
                    em.merge(h);
                }

                // Move Person
                List<Person> people = em.createQuery(
                                "SELECT p FROM Person p WHERE p.employee = :neg", Person.class)
                        .setParameter("neg", neg)
                        .getResultList();
                for (Person p : people) {
                    p.setEmployee(pos);
                    em.merge(p);
                }

                em.remove(em.contains(neg) ? neg : em.merge(neg));

                System.out.println("✅ Merged negative " + neg.getId() + " → positive " + pos.getId());

                if (++merged % 25 == 0) {
                    em.flush();
                    em.clear();
                }

            } catch (Exception ex) {
                System.err.println("❌ Error merging Employee " + neg.getId() + ": " + ex.getMessage());
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                em.getTransaction().begin(); // continue with the next
            }
        }

        em.getTransaction().commit();
        System.out.println("✅ Merge complete. " + merged + " negative employees merged.");
    }
    public static void processNewBenefitI4Fast(EntityManager em, String savePath) {
        System.out.println("Firing: I4 Fast");
        // Load valid PlanType IDs
        Set<Integer> validPlanTypeIds = em.createQuery("SELECT p.planTypeId FROM PlanType p", Integer.class)
                .getResultStream()
                .collect(Collectors.toSet());

        List<ImportBenefitCdh> imports = em.createQuery(
                "SELECT i FROM ImportBenefitCdh i LEFT JOIN Benefit b ON i.benefitId = b.id WHERE b.id IS NULL",
                ImportBenefitCdh.class
        ).getResultList();

        if (imports.isEmpty()) {
            System.out.println("No new CDH benefits to insert.");
            return;
        }

        int inserted = 0;
        int skipped = 0;
        List<ImportBenefitCdh> skippedRecords = new ArrayList<>();

        for (ImportBenefitCdh i : imports) {
            Integer planTypeId = i.getPlanType() != null ? i.getPlanType().getPlanTypeId() : null;

            if (planTypeId == null || !validPlanTypeIds.contains(planTypeId)) {
                skippedRecords.add(i);
                skipped++;
                continue;
            }

            try {
                em.getTransaction().begin();

                Employer employer = em.find(Employer.class, i.getImportEmployer().getOrganizationId());
                Date effDate = i.getPlanEffectiveDate();
                Date renDue = BillingHelper.getNextRenewalDate(effDate);

                Benefit b = new Benefit();
                b.setId(i.getBenefitId());
                b.setPlanType(i.getPlanType());
                b.setEmployer(employer);
                b.setPlanName(i.getPlanName());
                b.setPlanDescription(i.getPlanDescription());
                b.setActive("Active".equalsIgnoreCase(i.getPlanStatus()));
                b.setHasCards(i.getCardEnabledBoolean());
                b.setEffectiveDate(effDate);
                b.setTerminationDate(i.getPlanTerminationDate());
                b.setNextRenewalDue(renDue);

                em.persist(b);
                em.getTransaction().commit();
                inserted++;

            } catch (Exception ex) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();

                System.err.println("❌ Failed to insert Benefit ID=" + i.getBenefitId() + ": " + ex.getMessage());
            }
        }

        BillingHelper.writeSkippedToCsv(skippedRecords, savePath);

        System.out.println("✅ Inserted " + inserted + " new CDH benefits.");
        System.out.println("⏭️ Skipped " + skipped + " benefits due to invalid PlanType. See CSV at: " + savePath);
    }
    public static void processNewBenefitI7Fast(EntityManager em, String saveFolderPath) {
        System.out.println("Firing: I7 Fast");
        Set<Integer> validPlanTypeIds = em.createQuery("SELECT p.planTypeId FROM PlanType p", Integer.class)
                .getResultStream()
                .collect(Collectors.toSet());

        List<ImportBenefitPb> imports = em.createQuery(
                "SELECT i FROM ImportBenefitPb i LEFT JOIN Benefit b ON b.id = -i.benefitId WHERE b.id IS NULL",
                ImportBenefitPb.class
        ).getResultList();

        if (imports.isEmpty()) {
            System.out.println("No new PB benefits to insert.");
            return;
        }

        int inserted = 0;
        int skipped = 0;
        List<ImportBenefitPb> skippedRecords = new ArrayList<>();

        for (ImportBenefitPb i : imports) {
            Integer planTypeId = i.getPlanType() != null ? i.getPlanType().getPlanTypeId() : null;

            if (planTypeId == null || !validPlanTypeIds.contains(planTypeId)) {
                skippedRecords.add(i);
                skipped++;
                continue;
            }

            try {
                em.getTransaction().begin();

                Employer employer = em.find(Employer.class, i.getImportEmployer().getOrganizationId());
                Date effDate = i.getEffectiveDate();
                Date renDue = BillingHelper.getNextRenewalDate(effDate);
                Date termDate = i.getEndDate();

                Benefit b = new Benefit();
                b.setId(-i.getBenefitId());  // Store with negative ID
                b.setEmployer(employer);
                b.setPlanType(i.getPlanType());
                b.setPlanName(i.getBenefitName());
                b.setPlanDescription(i.getBenefitName());
                b.setEffectiveDate(effDate);
                b.setTerminationDate(termDate);
                b.setActive(false);
                b.setHasCards(false);
                b.setNextRenewalDue(renDue);
                b.setPbBenId(i.getPbBenefitId());

                em.persist(b);
                em.getTransaction().commit();
                inserted++;

            } catch (Exception ex) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();

                System.err.println("❌ Failed to insert PB benefit ID=" + i.getBenefitId() + ": " + ex.getMessage());
            }
        }

        if (!skippedRecords.isEmpty()) {
            String fullPath = BillingHelper.writeSkippedPbToCsv(skippedRecords, saveFolderPath);
            System.out.println("⏭️ Skipped " + skipped + " PB benefits due to invalid PlanType. See CSV at: " + fullPath);
        }

        System.out.println("✅ Inserted " + inserted + " new PB benefits.");
    }
    public static void syncBenefit(EntityManager em) {
        System.out.println("🔥 Starting syncBenefit");

        Date cutoff = BillingHelper.getMonthFor(); // Start of current month
        Date today = Date.valueOf(LocalDate.now());
        LocalDate lastDayPrevMonth = LocalDate.now().withDayOfMonth(1).minusDays(1);
        Date i7TermDate = java.sql.Date.valueOf(lastDayPrevMonth);

        Set<Benefit> toDeactivate = new HashSet<>();
        Set<Benefit> toReactivate = new HashSet<>();

        // --- I4: CDH DEACTIVATION ---
        try {
            System.out.println("→ Running CDH deactivation query...");
            List<Benefit> list = em.createQuery(
                    "SELECT b FROM Benefit b " +
                            "JOIN ImportBenefitCdh sb ON sb.benefitId = b.id " +
                            "WHERE b.isActive = true AND (LOWER(sb.planStatus) = 'inactive' OR b.terminationDate < :cutoff)",
                    Benefit.class
            ).setParameter("cutoff", cutoff).getResultList();

            toDeactivate.addAll(list);
            System.out.println("✔ CDH deactivation candidates: " + list.size());
        } catch (Exception ex) {
            System.out.println("❌ Error in CDH deactivation query: " + ex.getMessage());
            ex.printStackTrace();
        }

        // --- I4: CDH REACTIVATION ---
        try {
            System.out.println("→ Running CDH reactivation query...");
            List<Benefit> list = em.createQuery(
                    "SELECT b FROM Benefit b " +
                            "JOIN ImportBenefitCdh sb ON sb.benefitId = b.id " +
                            "WHERE b.isActive = false AND LOWER(sb.planStatus) <> 'inactive'",
                    Benefit.class
            ).getResultList();

            toReactivate.addAll(list);
            System.out.println("✔ CDH reactivation candidates: " + list.size());
        } catch (Exception ex) {
            System.out.println("❌ Error in CDH reactivation query: " + ex.getMessage());
            ex.printStackTrace();
        }

        // --- I7: PB DEACTIVATION ---
        try {
            System.out.println("→ Running PB deactivation query...");
            List<Benefit> list = em.createQuery(
                    "SELECT b FROM Benefit b " +
                            "JOIN ImportBenefitPb p ON b.id = -p.benefitId " +
                            "WHERE b.isActive = true AND (" +
                            "  :today < FUNCTION('STR_TO_DATE', p.startDate, '%m/%d/%Y') " +
                            "  OR :today > FUNCTION('STR_TO_DATE', p.endDate, '%m/%d/%Y')" +
                            ")",
                    Benefit.class
            ).setParameter("today", today).getResultList();

            toDeactivate.addAll(list);
            System.out.println("✔ PB deactivation candidates: " + list.size());
        } catch (Exception ex) {
            System.out.println("❌ Error in PB deactivation query: " + ex.getMessage());
            ex.printStackTrace();
        }

        // --- I7: PB REACTIVATION ---
        try {
            System.out.println("→ Running PB reactivation query...");
            List<Benefit> list = em.createQuery(
                    "SELECT b FROM Benefit b " +
                            "JOIN ImportBenefitPb p ON b.id = -p.benefitId " +
                            "WHERE b.isActive = false AND " +
                            "  :today BETWEEN FUNCTION('STR_TO_DATE', p.startDate, '%m/%d/%Y') " +
                            "  AND FUNCTION('STR_TO_DATE', p.endDate, '%m/%d/%Y')",
                    Benefit.class
            ).setParameter("today", today).getResultList();

            toReactivate.addAll(list);
            System.out.println("✔ PB reactivation candidates: " + list.size());
        } catch (Exception ex) {
            System.out.println("❌ Error in PB reactivation query: " + ex.getMessage());
            ex.printStackTrace();
        }

        // --- Process Combined Update Set ---
        Set<Benefit> toUpdate = new HashSet<>();
        toUpdate.addAll(toDeactivate);
        toUpdate.addAll(toReactivate);

        if (toUpdate.isEmpty()) {
            System.out.println("✅ No benefit status changes needed.");
            return;
        }

        // --- Batch update all ---
        System.out.println("→ Updating benefit records...");
        em.getTransaction().begin();
        int count = 0;

        for (Benefit b : toUpdate) {
            boolean shouldBeActive = toReactivate.contains(b);

            if (b.isActive() != shouldBeActive) {
                b.setActive(shouldBeActive);

                // If deactivating an I7 benefit, set termination date
                if (!shouldBeActive && b.getId() < 0) {
                    b.setTerminationDate(i7TermDate);
                }

                em.merge(b);
                count++;

                if (count % 200 == 0) {
                    em.getTransaction().commit();
                    em.getTransaction().begin();
                }
            }
        }

        em.getTransaction().commit();
        System.out.println("✅ Updated active status for " + count + " benefits (CDH + PB).");
    }
    public static void processBenefitTiersFromI7Import(EntityManager em) {
        System.out.println("Firing: Ben Tiers");
        List<ImportBenefitTier> importTiers = em.createQuery(
                "SELECT sbt FROM ImportBenefitTier sbt", ImportBenefitTier.class
        ).getResultList();

        if (importTiers.isEmpty()) {
            System.out.println("No PB benefit tiers found to process.");
            return;
        }

        em.getTransaction().begin();
        int insertCount = 0;
        int skippedNoBenefit = 0;
        int skippedExists = 0;

        for (ImportBenefitTier sbt : importTiers) {
            String tierId = Importer.getId(sbt);

            // Check for existing BenefitTier
            boolean exists = !em.createQuery("SELECT bt.id FROM BenefitTier bt WHERE bt.id = :id")
                    .setParameter("id", tierId)
                    .getResultList().isEmpty();
            if (exists) {
                skippedExists++;
                continue;
            }

            // Lookup matching Benefit using pbBenId
            Benefit benefit = Importer.getBenefit(em, sbt);
            if (benefit == null) {
                skippedNoBenefit++;
                continue;
            }

            // Create new BenefitTier
            BenefitTier bt = new BenefitTier();
            bt.setId(tierId);
            bt.setBenefit(benefit);
            bt.setTierName(sbt.getTierId());
            bt.setTierDescription(sbt.getTierName());
            bt.setStartDate(sbt.getPlanStartDate());
            bt.setEndDate(sbt.getPlanEndDate());

            em.persist(bt);
            insertCount++;

            if (insertCount % 200 == 0) {
                em.getTransaction().commit();
                em.getTransaction().begin();
            }
        }

        em.getTransaction().commit();

        System.out.printf(
                "Inserted %d PB benefit tiers. Skipped %d (exists), %d (no benefit).%n",
                insertCount, skippedExists, skippedNoBenefit
        );
    }
    public static void processHsaErFromAccounts(EntityManager em) {
        System.out.println("Firing: HSA Er");

        List<HsaAccount> accounts = em.createQuery("SELECT h FROM HsaAccount h", HsaAccount.class).getResultList();
        if (accounts.isEmpty()) return;

        Set<String> processedEmployers = new HashSet<>();
        Map<String, HsaEr> existingErMap = new HashMap<>();

        // Preload existing HsaEr by name
        for (HsaEr er : em.createQuery("SELECT h FROM HsaEr h", HsaEr.class).getResultList()) {
            existingErMap.put(er.getName().trim().toLowerCase(), er);
        }

        em.getTransaction().begin();
        int created = 0;

        for (HsaAccount account : accounts) {
            String rawEmployer = account.getEmployer();
            if (rawEmployer == null || rawEmployer.isBlank()) continue;

            String normalized = Validator.normalizeEmployerName(rawEmployer);
            if (processedEmployers.contains(normalized)) continue;

            if (!existingErMap.containsKey(rawEmployer.trim().toLowerCase())) {
                Employer employer = findOrCreateEmployer(em, rawEmployer);

                HsaEr hsaEr = new HsaEr();
                hsaEr.setName(rawEmployer);
                hsaEr.setEmployer(employer);
                hsaEr.setBilledDirect(false);

                em.persist(hsaEr);
                existingErMap.put(rawEmployer.trim().toLowerCase(), hsaEr);
                created++;
            }

            processedEmployers.add(normalized);
        }

        em.getTransaction().commit();
        System.out.printf("✅ Created %d new HsaEr records.%n", created);
    }
    public static void processHsaEeFromAccounts(EntityManager em) {
        System.out.println("Firing: HSA Ee");

        List<HsaAccount> accounts = em.createQuery(
                        "SELECT h FROM HsaAccount h WHERE h.active = true ORDER BY h.hsaId", HsaAccount.class)
                .getResultList();

        if (accounts.isEmpty()) {
            System.out.println("No active HSA accounts found.");
            return;
        }

        Map<Integer, HsaEe> existingEeMap = em.createQuery("SELECT h FROM HsaEe h", HsaEe.class)
                .getResultStream()
                .collect(Collectors.toMap(HsaEe::getHsaId, h -> h));

        Map<String, HsaEr> hsaErCache = new HashMap<>();
        Map<Long, List<Employee>> employerEmployeeMap = new HashMap<>();

        int batchSize = 200;
        int count = 0;
        int updatedEr = 0;

        em.getTransaction().begin();

        for (HsaAccount h : accounts) {
            HsaEe hsaEe = existingEeMap.get(h.getHsaId());

            // Always resolve current correct HsaEr
            HsaEr correctHsaEr = hsaErCache.computeIfAbsent(h.getEmployer(),
                    name -> HsaBillingHelper.getHsaErByAccount(em, h));

            boolean isNew = (hsaEe == null);
            if (isNew) {
                hsaEe = new HsaEe();
                hsaEe.setHsaId(h.getHsaId());
            }

            // CRITICAL: Always sync HsaEr — this fixes your bug
            if (!Objects.equals(hsaEe.getHsaEr(), correctHsaEr)) {
                hsaEe.setHsaEr(correctHsaEr);
                if (!isNew) updatedEr++;
            }

            // ... rest of your existing logic (name, address, employee matching, etc.)
            hsaEe.setFirstName(h.getFirstName());
            hsaEe.setLastName(h.getLastName());
            // ... etc

            if (isNew) {
                em.persist(hsaEe);
            } else {
                em.merge(hsaEe);
            }

            count++;
            if (count % batchSize == 0) {
                em.flush();
                em.clear();
                em.getTransaction().commit();
                em.getTransaction().begin();
            }
        }

        em.getTransaction().commit();
        System.out.printf("Processed %d HSA accounts. Updated HsaEr on %d existing records.%n", count, updatedEr);
    }

    private static List<ImportCobraQb> fetchCobraList(EntityManager em){
        Query q = em.createQuery("SELECT c FROM ImportCobraQb c order by c.participantSystemId, c.coverageStatus");
        List<ImportCobraQb> ImportCobraQbs;
        try{
            ImportCobraQbs = (List<ImportCobraQb>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(ImportCobraQbs.size()==0)
            return null;
        return ImportCobraQbs;
    }
    private static String trimIgnoreCase(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
    private static Employer findOrCreateEmployer(EntityManager em, String rawName) {
        String normalizedInput = Validator.normalizeEmployerName(rawName);

        List<Employer> employers = em.createQuery("SELECT e FROM Employer e", Employer.class).getResultList();
        for (Employer e : employers) {
            String normalizedExisting = Validator.normalizeEmployerName(e.getEmployerName());
            if (normalizedInput.equals(normalizedExisting)) {
                return e;
            }
        }

        // No match found — create new Employer with a safe negative ID
        Integer minId = (Integer) em.createQuery("SELECT MIN(e.id) FROM Employer e").getSingleResult();
        int newId = (minId != null && minId < 0) ? minId - 1 : -1;

        Employer newEmployer = new Employer();
        newEmployer.setId(newId); // explicitly set negative ID
        newEmployer.setEmployerName(rawName.trim());
        newEmployer.setBillable(true);
        newEmployer.setHasPop(false);
        newEmployer.setHasCdh(true);
        newEmployer.setHasPb(false);
        newEmployer.setActive(true);
        newEmployer.setAgency(false);
        em.persist(newEmployer);

        return newEmployer;
    }
    public static void batchLinkPersonsToEmployees(EntityManager em, List<Employee> employees) {
        // Pre-fetch all unmatched persons
        List<Person> unmatchedPeople = em.createQuery(
                        "SELECT p FROM Person p WHERE p.employee IS NULL", Person.class)
                .getResultList();

        Map<String, List<Person>> peopleByEmail = new HashMap<>();
        Map<String, List<Person>> peopleByName = new HashMap<>();

        for (Person p : unmatchedPeople) {
            if (p.getEmail() != null && !p.getEmail().isBlank()) {
                peopleByEmail.computeIfAbsent(p.getEmail().toLowerCase(), k -> new ArrayList<>()).add(p);
            }
            String nameKey = (p.getFirstName() + "||" + p.getLastName()).toLowerCase();
            peopleByName.computeIfAbsent(nameKey, k -> new ArrayList<>()).add(p);
        }

        PSP psp = EntityLookup.getPspById(em, 4L);

        int count = 0;
        int batchSize = 200;

        em.getTransaction().begin();

        for (Employee emp : employees) {
            String email = Validator.isValidEmail(emp.getEmail()) ? emp.getEmail().toLowerCase() : null;
            String first = emp.getFirstName();
            String last = emp.getLastName();
            if (first == null || last == null) continue;

            Person person = null;
            String nameKey = (first + "||" + last).toLowerCase();

            // 1. Match by email
            if (email != null) {
                List<Person> emailMatches = peopleByEmail.getOrDefault(email, List.of());

                if (emailMatches.size() == 1) {
                    person = emailMatches.get(0);
                } else if (emailMatches.size() > 1) {
                    for (Person p : emailMatches) {
                        if (first.equalsIgnoreCase(p.getFirstName()) && last.equalsIgnoreCase(p.getLastName())) {
                            person = p;
                            break;
                        }
                    }
                    if (person == null) {
                        person = emailMatches.get(0);
                        person.setFirstName(first);
                        person.setLastName(last);
                        person.setFullName(first + " " + last);
                    }
                }
            }

            // 2. Fallback: match by name (null-email people)
            if (person == null) {
                List<Person> nameMatches = peopleByName.getOrDefault(nameKey, List.of());
                if (!nameMatches.isEmpty()) {
                    for (Person p : nameMatches) {
                        if (p.getEmail() != null && !p.getEmail().isBlank()) {
                            person = p;
                            break;
                        }
                    }
                    if (person == null) {
                        person = nameMatches.get(0);
                    }
                }
            }

            // 3. Create new person
            if (person == null) {
                person = new Person();
                person.setFirstName(first.toUpperCase());
                person.setLastName(last.toUpperCase());
                person.setEmail(email);
                person.setFullName(first.toUpperCase() + " " + last.toUpperCase());
                person.setPsp(psp);
                em.persist(person);
            }

            person.setEmployee(emp);
            em.merge(person);

            if (++count % batchSize == 0) {
                em.flush();
                em.clear();
            }
        }

        em.getTransaction().commit();
    }
    private static Person createPersonFromEmployee(Employee ee, PSP psp){
        Person person = new Person();
        person.setFirstName(ee.getFirstName().toUpperCase());
        person.setLastName(ee.getLastName().toUpperCase());
        person.setEmail(ee.getEmail().toLowerCase());
        person.setFullName(ee.getFirstName().toUpperCase()+ " "+ ee.getLastName().toUpperCase());
        person.setPsp(psp);
        return person;
    }


}


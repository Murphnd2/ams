package net.superiorstate.ams.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;
import net.superiorstate.ams.previous.model.summit.imports.order.ImportBenefitCdh;
import net.superiorstate.ams.previous.model.summit.imports.order.ImportBenefitPb;
import net.superiorstate.ams.previous.model.summit.imports.order.ImportCobTerm;
import net.superiorstate.ams.previous.model.summit.imports.order.ImportEmployeeAlt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public abstract class Helper {
    public static final String CSV_DIR = "C:/ams/uploads";
    public static final String PROCESSED_DIR = "C:/ams/uploads/processed";
    public static final String SKIPPED_DIR = "C:/ams/uploads/failures";

    public static final String UNKNOWN_DIR = "C:/ams/uploads/unrecognized";

    /**
     * Resolves the billing monthId from the BillingMonth entity
     * based on the current date from getMonthFor().
     *
     * @param em the EntityManager to use for the lookup
     * @return the resolved monthId, or -1 if not found
     */
    public static int resolveMonthId(EntityManager em) {
        Date monthFor = getMonthFor();
        try {
            return em.createQuery(
                            "SELECT bm.monthId FROM BillingMonth bm WHERE bm.fullDate = :fullDate", Integer.class)
                    .setParameter("fullDate", monthFor)
                    .getSingleResult();
        } catch (NoResultException e) {
            return -1;
        }
    }

    public static Date getMonthFor(){
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        LocalDate billingDate = LocalDate.of(currentDate.getYear(),currentDate.getMonthValue(),1);
        return Date.valueOf(billingDate);
    }
    public static int matchByName(EntityManager em, ImportCobTerm ict) {
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.employer.id = :id");
        q.setParameter("id", ict.getOrganizationId());
        List<Employee> employees;
        try {
            employees = (List<Employee>) q.getResultList();
        } catch (NoResultException e) {
            return 0;
        }
        if (employees.size() == 0)
            return 0;
        String ictName = ict.getParticipantName().toUpperCase().trim();
        String ictNameStripped = ictName.replace(" ", "");
        for (Employee ee : employees) {
            String checkNameOne = ee.getLastName().toUpperCase().trim() + "," + ee.getFirstName().toUpperCase().trim();
            String checkNameTwo = ee.getLastName().toUpperCase().trim() + ", " + ee.getFirstName().toUpperCase().trim();
            if (checkNameOne.equals(ictNameStripped) || checkNameTwo.equals(ictName))
                return ee.getId();
        }
        return 0;
    }
    public static int matchBySsn(EntityManager em, ImportCobTerm ict) {
        Query q = em.createQuery("SELECT e FROM ImportEmployeeAlt e WHERE e.ssn = :ssn AND e.importEmployer.organizationId = :eid1");
        q.setParameter("ssn", ict.getSsn());
        q.setParameter("eid1", ict.getOrganizationId());
        List<ImportEmployeeAlt> employees;
        try {
            employees = (List<ImportEmployeeAlt>) q.getResultList();
        } catch (NoResultException e) {
            return 0;
        }
        if (employees.size() == 1)
            return employees.get(0).getParticipantId();
        else if (employees.size() > 1 && employees.get(0).getParticipantId() == 0)
            return employees.get(1).getParticipantId();
        else return 0;
    }
    public static int matchByCustomId(EntityManager em, ImportCobTerm ict) {
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.customId = :cid AND e.employer.id = :eid");
        q.setParameter("cid", ict.getId());
        q.setParameter("eid", ict.getOrganizationId());
        List<Employee> employees;
        try {
            employees = (List<Employee>) q.getResultList();
        } catch (NoResultException e) {
            return 0;
        }

        if (employees.size() == 1)
            return employees.get(0).getId();
        else if (employees.size() > 1 && employees.get(0).getId() == 0)
            return employees.get(1).getId();
        else return 0;

    }
    public static Date getNextRenewalDate(Date effDate) {
        if (effDate == null) return null;

        LocalDate eff = effDate.toLocalDate();
        Month effMonth = eff.getMonth();
        LocalDate today = LocalDate.now();

        // Determine the next future occurrence of that month
        int targetYear = (effMonth.getValue() > today.getMonthValue()) ||
                (effMonth.getValue() == today.getMonthValue() && today.getDayOfMonth() < 1)
                ? today.getYear()
                : today.getYear() + 1;

        LocalDate renewalDate = LocalDate.of(targetYear, effMonth, 1);
        return Date.valueOf(renewalDate);
    }
    public static String writeSkippedToCsv(List<ImportBenefitCdh> skipped, String folderPath) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String fileName = "I4_Update_Failure_" + timestamp + ".csv";
        String fullPath = folderPath.endsWith("/") || folderPath.endsWith("\\")
                ? folderPath + fileName
                : folderPath + File.separator + fileName;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath, StandardCharsets.UTF_8))) {
            writer.write("BenefitId,PlanName,PlanType,PlanStatus,EmployerName,EffectiveDate,TerminationDate\n");

            for (ImportBenefitCdh i : skipped) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                        sanitizeCsv(i.getBenefitId()),
                        sanitizeCsv(i.getPlanName()),
                        sanitizeCsv(i.getPlanType() != null ? i.getPlanType().getPlanTypeName() : "null"),
                        sanitizeCsv(i.getPlanStatus()),
                        sanitizeCsv(i.getImportEmployer() != null ? i.getImportEmployer().getEmployerName() : "null"),
                        sanitizeCsv(i.getPlanEffectiveDate()),
                        sanitizeCsv(i.getPlanTerminationDate())
                ));
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to write skipped records to CSV: " + e.getMessage());
        }

        return fullPath;
    }
    public static String writeSkippedPbToCsv(List<ImportBenefitPb> skipped, String folderPath) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String fileName = "I7_Update_Failure_" + timestamp + ".csv";
        String fullPath = folderPath.endsWith("/") || folderPath.endsWith("\\")
                ? folderPath + fileName
                : folderPath + File.separator + fileName;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath, StandardCharsets.UTF_8))) {
            writer.write("BenefitId,BenefitName,PlanType,EmployerName,EffectiveDate,EndDate,PbBenefitId\n");

            for (ImportBenefitPb i : skipped) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                        sanitizeCsv(i.getBenefitId()),
                        sanitizeCsv(i.getBenefitName()),
                        sanitizeCsv(i.getPlanType() != null ? i.getPlanType().getPlanTypeName() : "null"),
                        sanitizeCsv(i.getImportEmployer() != null ? i.getImportEmployer().getEmployerName() : "null"),
                        sanitizeCsv(i.getEffectiveDate()),
                        sanitizeCsv(i.getEndDate()),
                        sanitizeCsv(i.getPbBenefitId())
                ));
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to write skipped PB records to CSV: " + e.getMessage());
        }

        return fullPath;
    }

    public static String sanitizeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString().replace("\"", "\"\"");
        return "\"" + str + "\"";
    }

    public static String trimForDb(String input, int maxLength) {
        return input == null ? null : input.trim().length() > maxLength
                ? input.trim().substring(0, maxLength)
                : input.trim();
    }
    public static int[] parseDateParts(String dateString) {
        try {
            String[] parts = dateString.split("/");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2].substring(0, 4));
            return new int[] { month, day, year };
        } catch (Exception e) {
            LocalDate now = LocalDate.now();
            return new int[] { now.getMonthValue(), 1, now.getYear() };
        }
    }
    public class EmployeeFinder {

        /**
         * Finds a matching positive-ID employee by:
         * 1. Email match with same employer (priority)
         * 2. Name match with same employer (fallback)
         * Returns null if no match is found.
         */
        public static Employee findMatchingEmployee(EntityManager em, String email, String firstName, String lastName, Employer employer) {
            String normEmail = (email != null) ? email.trim().toLowerCase() : null;
            String normFirst = V.normalizeName(firstName);
            String normLast = V.normalizeName(lastName);

            // 1. Email match with same employer, positive ID only
            if (normEmail != null && !normEmail.isBlank()) {
                List<Employee> emailMatches = em.createQuery(
                                "SELECT e FROM Employee e WHERE e.id > 0 AND LOWER(e.email) = :email", Employee.class)
                        .setParameter("email", normEmail)
                        .getResultList();

                Optional<Employee> emailMatch = emailMatches.stream()
                        .filter(e -> e.getEmployer().equals(employer))
                        .findFirst();

                if (emailMatch.isPresent()) return emailMatch.get();
            }

            // 2. Name match with same employer, positive ID only
            List<Employee> employerEmployees = em.createQuery(
                            "SELECT e FROM Employee e WHERE e.id > 0 AND e.employer = :employer", Employee.class)
                    .setParameter("employer", employer)
                    .getResultList();

            for (Employee e : employerEmployees) {
                if (V.normalizeName(e.getFirstName()).equals(normFirst) &&
                        V.normalizeName(e.getLastName()).equals(normLast)) {
                    return e;
                }
            }

            return null;
        }
    }




}

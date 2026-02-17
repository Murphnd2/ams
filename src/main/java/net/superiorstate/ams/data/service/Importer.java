package net.superiorstate.ams.data.service;


import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.util.BillingHelper;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplateGroup;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.billing.BillingGroup;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.PlanType;
import net.superiorstate.ams.previous.model.summit.imports.HsaAccount;
import net.superiorstate.ams.previous.model.summit.imports.order.ImportBenefitTier;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Importer {
    public static final List<TableMapping> TABLE_MAPPINGS = List.of(
            new TableMapping("I1_Employer", "import1employer", List.of(
                    "OrganizationID", "CreatedByUser", "CreatedDate",
                    "CreatedUser", "CustomID", "Email", "Employer_ID", "EmployerName", "ImplementationLead",
                    "ImplementationLeadUserID", "IsSetUpCompleted", "OrganizationStatusID", "Phone", "PhoneNumber",
                    "PrimaryContact", "SetUpComplete", "SetUpCompletionDate", "Setup", "Status", "TaxID"
            ), true, false, Map.of(
                    "Employer_ID","EmployerOrganizationID",
                    "IsSetUpCompleted","IsSetupCompleted"
            )),

            new TableMapping("I2_Employee", "import2employee", List.of(
                    "Participant_ID", "Address1", "Address2", "City", "ParticipantCustomID", "Email",
                    "EmployerCustomID", "Employer_ID", "EmployerName", "FailedLoginCount", "FirstName",
                    "IsRegisterdToPortal", "LastLoginDate", "LastName", "SetupCompletionDate", "State",
                    "User_ID", "UserStatus", "ZipCode", "Organization_ID"
            ), true),

            new TableMapping("I3_Employee_Alt", "import3employeealt", List.of(
                    "Participant_ID", "EffectiveDate", "TerminationDate", "UserId", "UserStatus", "CreatedDate",
                    "HireDate", "Organization_ID", "ParticipantStatusId", "ERName", "ParticipantName",
                    "Participant_Last", "Participant_First", "SSN", "userStatusID", "DOB",
                    "ReimbursementMethod", "EmploymentStatus", "EmploymentStatusID", "No_of_participants", "DivisionName",
                    "Bankname", "RoutingNo", "AccountType", "AccountNumber", "UserBank_ID", "ReimbursementMethod_ID", "ByDivision"
            ), true, false, Map.of(
                    // 💡 mapping DB field ➜ import header
                    "UserStatus", "User Status Description",
                    "Participant_Last", "Participant Last Name",
                    "Participant_First", "Participant First name",
                    "No_of_participants", "No: of participants"
            )),

            new TableMapping("I4_Benefits", "import4benefitcdh", List.of(
                    "EmployerPlan_ID", "CardEnabled", "EffectiveDate", "Employer_ID", "EmployerName", "ImportPlanID",
                    "LinkedtoDefaultPlan", "PlanDescription", "PlanName", "PlanStatus", "PlanType",
                    "TerminationDate", "PlanTypeID", "OrganizationID"
            ), true),

            new TableMapping("I5_BenefitYears", "import5benefityear", List.of(
                    "EmployerPlanDetailForPlanYear_ID", "ContributionSchedule", "ContributionScheduleTemplate_ID",
                    "Employer", "Organization_ID", "PlanDescription", "PlanName", "PlanStatus", "PlanYear",
                    "PlanYear_ID", "EmployerPlan_ID"
            ), true),

            new TableMapping("I6_Enrollments", "import6enrollment", List.of(
                    "ParticipantPlan_ID", "AccountBalance", "ActiveParticipantCount", "AvailableBalance", "CarryoverAmount",
                    "CoverageEndDate", "CustomID", "DisbursableBalance", "DivisionCustomID", "DivisionOrganizationID",
                    "DivisionOrganizationName", "ElectionAmount", "EmployerID", "EmployerName", "EmployerPlan_ID",
                    "EmployerYTDContribution", "FirstName", "IsByDivision", "IsCarryOverEnable", "LastName",
                    "OrganizationID", "ParticipantCount", "ParticipantYTDContribution", "PendingCardTransaction",
                    "PlanDescription", "PlanName", "PlanStatus", "PlanType", "PlanYear", "SSN", "Eff_TermDate",
                    "YTDClaim", "YtdDCReturn", "YTDPayments", "YtdReturn", "EmployerPlanDetailForPlanYearID", "Participant_ID"
            ), true),

            new TableMapping("I7_Cobra_Benefits", "import7benefitpb", List.of(
                    "TPA", "OrganizationID", "EmployerID", "Employer", "BenefitName", "PBBenefitID", "Type",
                    "RemitTo", "PlanTypeID", "PBType", "PBTypeID", "BenefitID", "ImportPlanID", "EffectiveDate",
                    "Carrier", "LastDayofCoverage", "Fee", "PlanYearID", "StartDate", "EndDate", "TierName",
                    "TierID", "TierAge", "Gender", "Smoker", "Amount"
            ), true),

            new TableMapping("I8_Alt2_QB", "import8cobraqb", List.of(
                    "EmployerSystemId", "EmployerCustomId", "EmployerName", "EmployerPlanSystemID", "ImportPlanID",
                    "EmployerPlanName", "EmployerDivisionID", "EmployerDivision", "IsIndividuallyRated",
                    "EmployerPlanTierName", "UserId", "Relationship", "ParticipantSystemId", "ParticipantCustomId",
                    "ParticipantName", "ParticipantFirstName", "ParticipantLastName", "ParticipantMiddleName", "IsDependent",
                    "CoveredMemberName", "CoveredMemberFirstName", "CoveredMemberLastName", "CoveredMemberMiddleName",
                    "ParticipantAddress1", "ParticipantAddress2", "ParticipantCity", "ParticipantState",
                    "ParticipantZipCode", "ParticipantEmail", "ParticipantPhone", "SSN", "DependentSystemID",
                    "DependentCustomID", "DependentFirstName", "DependentLastName", "DependentMiddleName",
                    "DependentRelationship", "DependentParticipantSystemID", "QualifyingEventReason",
                    "QualifyingEventDate", "CoverageStatus", "CoverageStatusEffectiveDate", "LastDayOfCoverage",
                    "LastDayToAccept", "CobraAccepted", "AcceptedEntered", "CobraAcceptedDate", "CobraStartDate",
                    "CobraTermed", "TermedEntered", "Subsidy", "TransactionStartDate", "TransactionEndDate",
                    "IsByDivision", "CoveredMembersNames", "PBBillingFrequency"
            ), true, false, Map.of(
                    "CoverageStatusEffectiveDate", "coveragestatuseffecivedate",
                    "DependentMiddleName", "dependentmiddilename"
            ))
            ,

            new TableMapping("I9_PbTermed", "importbcobraterm", List.of(
                    "TransactionStartDate", "TransactionEndDate", "ParticipantName", "ID", "SSN", "EmployerName",
                    "BenefitName", "Tier", "TerminationReason", "TermedDate", "TermedOn", "PBBillingFrequency", "EmployerOrganizationID"
            ), true),

            new TableMapping("IA_Cobra_Cov", "importacoverage", List.of(
                    "PBCoverageHeaderID", "BenefitID", "BenefitName", "CoverageStatus", "EffectiveDate",
                    "EmployerCustomID", "EmployerDivision", "EmployerDivisionID", "EmployerID", "EmployerName",
                    "ImportPlanID", "ParticipantCustomID", "ParticipantFirstName", "ParticipantLastName",
                    "PlanType", "SSN", "TierName", "TransactionEndDate", "TransactionStartDate",
                    "Participant_ID", "ProcessedDate", "IsByDivision", "PBBillingFrequency"
            ), true),

            new TableMapping("IB_PbCobraActive", "import9cobrapart", List.of(
                    "User_ID", "Participant_id", "AcceptedDate", "DisplayPremium", "EmployerName", "ExpirationDate",
                    "PaidThroughDate", "ParticipantName", "CoveredMemberName", "PBTypeName", "QualifyingEvent",
                    "QualifyingEventDate", "StartDate", "TermedDate", "BenefitName", "TransactionStartDate",
                    "TransactionEndDate", "PBCoverageHeaderId", "Tier", "CoveredMembersNames",
                    "IncludeDependentsCoveredUnderEachPlan", "IsDependent", "SSN", "DateOfBirth",
                    "ParticipantAddress1", "ParticipantAddress2", "ParticipantCity", "ParticipantState", "ParticipantZip",
                    "Carrier", "Gender", "CustomID", "Relationship", "PBBillingFrequency", "DivisionName",
                    "DivisionCustomID", "DivisionID", "IsByDivision", "EmployerOrganizationID"
            ), true),

            // ✅ Custom handler files
            new TableMapping("Life Count Detail", "hsaaccount", List.of(), false, true),
            new TableMapping("Plan Type", "importplantype", List.of("plan type id", "plan type code", "plan type name"), true, true)
    );


    public record TableMapping(
            String filePrefix,
            String tableName,
            List<String> columns,
            boolean requiresHeaders,
            boolean customHandler,
            Map<String, String> headerOverrides
    ) {
        // Convenience constructors
        public TableMapping(String filePrefix, String tableName, List<String> columns, boolean requiresHeaders) {
            this(filePrefix, tableName, columns, requiresHeaders, false, Map.of());
        }

        public TableMapping(String filePrefix, String tableName, List<String> columns, boolean requiresHeaders, boolean customHandler) {
            this(filePrefix, tableName, columns, requiresHeaders, customHandler, Map.of());
        }

        // Optional: a convenience method to access headerOverrides safely
        public Map<String, String> headerOverrides() {
            return headerOverrides == null ? Map.of() : headerOverrides;
        }
    }



    public static List<String> readCsvHeader(File file) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line != null) {
                return Arrays.asList(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String buildInsertSql(String tableName, List<String> resolvedColumnNames) {
        String columnsPart = resolvedColumnNames.stream()
                .map(col -> "`" + col + "`")  // ✅ Escape column names
                .collect(Collectors.joining(", "));

        String placeholders = resolvedColumnNames.stream()
                .map(col -> "?")
                .collect(Collectors.joining(", "));

        return "INSERT INTO `" + tableName + "` (" + columnsPart + ") VALUES (" + placeholders + ")";
    }

    public static String getId(ImportBenefitTier bt) {
        return -bt.getBenefitId() + "-" + bt.getPlanYearId() + "-" + bt.getTierId();
    }
    private static Benefit getBenefit(EntityManager em, int id) {
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.pbBenId = :id");
        q.setParameter("id", id);
        Benefit b;
        try {
            b = (Benefit) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        return b;
    }
    public static Benefit getBenefit(EntityManager em, ImportBenefitTier sbt) {
        return getBenefit(em, sbt.getBenefitId());
    }
    public static void processHsaAccountFile(EntityManager em, String sourceDir, String processedDir) {
        if (!em.getTransaction().isActive()) {
            throw new IllegalStateException("processHsaAccountFile requires an active transaction.");
        }

        File lifeFile = findFileWithPrefix(sourceDir, "Life");
        if (lifeFile == null) {
            System.out.println("No Life*.csv file found for hsaaccount import.");
            return;
        }

        System.out.println("Processing Life file: " + lifeFile.getName());

        int lineNumber = 0;
        int inserted = 0, skipped = 0, duplicates = 0, malformed = 0;

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(lifeFile))
                .withCSVParser(new CSVParserBuilder().withSeparator(',').withQuoteChar('"').build())
                .build()) {

            String[] row;
            while ((row = reader.readNext()) != null) {
                lineNumber++;
                //System.out.println("Parsed Row " + lineNumber + ": " + Arrays.toString(row));

                if (row.length < 16) {
                    malformed++;
                    continue;
                }

                String acctNum = row[9].trim();
                String hsaId = row[12].trim();
                String fullName = row[3].trim();
                String employer = row[1].trim();

                if (acctNum.isEmpty() || hsaId.isEmpty() || fullName.isEmpty() || employer.isEmpty()) {
                    malformed++;
                    continue;
                }

                if (!hsaId.matches("\\d+")) {
                    malformed++;
                    continue;
                }

                int hsaIdInt = Integer.parseInt(hsaId);

                long commaCount = fullName.chars().filter(ch -> ch == ',').count();
                String lastName = "", firstName = "";
                if (commaCount >= 1) {
                    int firstCommaIndex = fullName.indexOf(',');
                    lastName = fullName.substring(0, firstCommaIndex).trim().toUpperCase();
                    firstName = fullName.substring(firstCommaIndex + 1).replace(",", "").trim().toUpperCase();
                } else {
                    malformed++;
                    continue;
                }

                Long count = em.createQuery(
                                "SELECT COUNT(h) FROM HsaAccount h WHERE h.hsaId = :hsaId AND h.accountNum = :acctNum", Long.class)
                        .setParameter("hsaId", hsaIdInt)
                        .setParameter("acctNum", acctNum)
                        .getSingleResult();

                if (count > 0) {
                    duplicates++;
                    continue;
                }

                try {
                    HsaAccount h = new HsaAccount();
                    h.setHsaId(hsaIdInt);
                    h.setAccountNum(acctNum);
                    h.setLastName(lastName);
                    h.setFirstName(firstName);
                    h.setEmployer(cleanEmployer(employer));
                    h.setAddress(row[5].replace(",", "").toUpperCase());
                    h.setCity(row[6].replace(",", "").toUpperCase());
                    h.setState(mapState(row[7]));
                    h.setZip(formatZip(row[8]));
                    h.setPhone(formatPhone(row[11]));
                    h.setActive("Active".equalsIgnoreCase(row[14].trim()));

                    em.persist(h);
                    inserted++;
                } catch (Exception ex) {
                    malformed++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Path targetPath = Paths.get(processedDir, lifeFile.getName());
            Files.createDirectories(Paths.get(processedDir));
            boolean moved = false;
            for (int i = 0; i < 3 && !moved; i++) {
                try {
                    Files.move(lifeFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    moved = true;
                } catch (IOException e) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
            if (!moved) {
                System.err.println("Giving up: could not move Life file after 3 attempts.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("✅ HSA Import Complete: inserted=%d, skipped=%d, duplicates=%d, malformed=%d%n",
                inserted, skipped, duplicates, malformed);

    }
    public static String cleanEmployer(String raw) {
        return raw == null ? "" : raw.replaceAll("[(),]|XFER|TERM|TERMED", "").replace(",", "").trim();
    }
    public static String mapState(String abbrev) {
        if (abbrev == null) return "";
        return switch (abbrev.trim().toUpperCase()) {
            case "AL" -> "Alabama"; case "AK" -> "Alaska"; case "AZ" -> "Arizona"; case "AR" -> "Arkansas";
            case "CA" -> "California"; case "CO" -> "Colorado"; case "CT" -> "Connecticut"; case "DE" -> "Delaware";
            case "FL" -> "Florida"; case "GA" -> "Georgia"; case "HI" -> "Hawaii"; case "ID" -> "Idaho";
            case "IL" -> "Illinois"; case "IN" -> "Indiana"; case "IA" -> "Iowa"; case "KS" -> "Kansas";
            case "KY" -> "Kentucky"; case "LA" -> "Louisiana"; case "ME" -> "Maine"; case "MD" -> "Maryland";
            case "MA" -> "Massachusetts"; case "MI" -> "Michigan"; case "MN" -> "Minnesota"; case "MS" -> "Mississippi";
            case "MO" -> "Missouri"; case "MT" -> "Montana"; case "NE" -> "Nebraska"; case "NV" -> "Nevada";
            case "NH" -> "New Hampshire"; case "NJ" -> "New Jersey"; case "NM" -> "New Mexico"; case "NY" -> "New York";
            case "NC" -> "North Carolina"; case "ND" -> "North Dakota"; case "OH" -> "Ohio"; case "OK" -> "Oklahoma";
            case "OR" -> "Oregon"; case "PA" -> "Pennsylvania"; case "RI" -> "Rhode Island"; case "SC" -> "South Carolina";
            case "SD" -> "South Dakota"; case "TN" -> "Tennessee"; case "TX" -> "Texas"; case "UT" -> "Utah";
            case "VT" -> "Vermont"; case "VA" -> "Virginia"; case "WA" -> "Washington"; case "WV" -> "West Virginia";
            case "WI" -> "Wisconsin"; case "WY" -> "Wyoming"; default -> abbrev.toUpperCase();
        };
    }
    public static String formatZip(String zip) {
        if (zip == null) return "";
        zip = zip.trim();
        if (zip.matches("^\\d{4}$")) return "0" + zip;
        if (zip.matches("^\\d{4}-\\d+$")) return "0" + zip;
        return zip;
    }
    public static String formatPhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() == 10
                ? digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6)
                : digits;
    }
    public static File findFileWithPrefix(String directoryPath, String prefix) {
        File dir = new File(directoryPath);
        File[] matching = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".csv") && name.startsWith(prefix)
        );
        return (matching != null && matching.length == 1) ? matching[0] : null;
    }
    public static void convertFileToUTF8(File file) throws IOException {
        List<String[]> validLines = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(file), Charset.forName("Windows-1252"))
        ).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                validLines.add(line);
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                ',', '"', '\\', "\n"
        )) {
            for (String[] row : validLines) {
                writer.writeNext(row, false);
            }
        }
    }
    public static void fallbackCsvInsert(EntityManager em, File file, TableMapping mapping, File processedDir) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File debugLog = new File(processedDir, "fallback_insert_debug_log_" + timestamp + ".csv");

        try (
                CSVReader reader = new CSVReaderBuilder(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)).withSkipLines(1).build();
                BufferedWriter logWriter = new BufferedWriter(new FileWriter(debugLog))
        ) {
            List<String> headers = Importer.readCsvHeader(file);
            if (headers == null || headers.isEmpty()) return;

            // Normalize helper
            Function<String, String> normalize = s -> s == null ? "" : s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");

            // Build column mapping
            Map<Integer, Integer> columnMapping = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String importHeaderNorm = normalize.apply(headers.get(i));

                for (int j = 0; j < mapping.columns().size(); j++) {
                    String tableField = mapping.columns().get(j);
                    String expectedCsvHeader = mapping.headerOverrides().getOrDefault(tableField, tableField);
                    if (importHeaderNorm.equals(normalize.apply(expectedCsvHeader))) {
                        columnMapping.put(i, j);
                        break;
                    }
                }
            }

            String insertSQL = Importer.buildInsertSql(mapping.tableName(), mapping.columns());
            Connection conn = em.unwrap(Connection.class);
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);

            int batchSize = 0, inserted = 0, skipped = 0, rowIndex = 0;
            String[] row;

            logWriter.write("RowIndex,Reason,RawRow\n");

            while ((row = reader.readNext()) != null) {
                rowIndex++;
                try {
                    String[] values = new String[mapping.columns().size()];
                    Arrays.fill(values, null);

                    for (Map.Entry<Integer, Integer> entry : columnMapping.entrySet()) {
                        int fileIndex = entry.getKey();
                        int dbIndex = entry.getValue();
                        if (fileIndex < row.length) {
                            values[dbIndex] = row[fileIndex];
                        }
                    }

                    for (int i = 0; i < values.length; i++) {
                        pstmt.setString(i + 1, values[i]);
                    }

                    pstmt.addBatch();
                    batchSize++;
                    inserted++;

                    if (batchSize >= 100) {
                        try {
                            pstmt.executeBatch();
                        } catch (Exception e) {
                            logWriter.write(rowIndex + ",Batch error: " + e.getMessage() + ",\"" + String.join("|", row) + "\"\n");
                        }
                        batchSize = 0;
                    }

                } catch (Exception e) {
                    logWriter.write(rowIndex + ",Row error: " + e.getMessage() + ",\"" + String.join("|", row) + "\"\n");
                    skipped++;
                }
            }

            try {
                pstmt.executeBatch();
            } catch (Exception e) {
                logWriter.write("Final batch error: " + e.getMessage() + ",\n");
            }

            pstmt.close();
            logWriter.flush();

            System.out.printf("✅ Inserted %d into %s, skipped %d malformed rows.\n", inserted, mapping.tableName(), skipped);
            System.out.println("📝 See debug log at: " + debugLog.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Fallback insert failed for: " + mapping.tableName());
            e.printStackTrace();
        }
    }


    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public static void importMissingPlanTypes(EntityManager em, String sourcePath, String archivePath) {
        File sourceFolder = new File(sourcePath);
        File archiveFolder = new File(archivePath);

        File[] files = sourceFolder.listFiles((dir, name) ->
                (name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".csv")));

        if (files == null || files.length == 0) {
            System.out.println("No matching files found in: " + sourcePath);
            return;
        }

        for (File file : files) {
            try {
                TableMapping mapping = Importer.findMatchingTableMapping(file.toPath());

                if (mapping == null || !mapping.tableName().equalsIgnoreCase("importplantype")) {
                    System.out.println("⚠️ Skipping file (no PlanType match): " + file.getName());
                    continue;
                }

                if (file.getName().toLowerCase().endsWith(".csv")) {
                    processCsvFile(em, file, archiveFolder); // still valid if implemented
                } else {
                    processExcelFile(em, file, archiveFolder, mapping); // ✅ updated
                }

            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                System.err.println("❌ Error processing: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    private static void processExcelFile(EntityManager em, File file, File archiveFolder, TableMapping mapping) throws Exception {
        if (!em.getTransaction().isActive()) {
            throw new IllegalStateException("No active transaction. Transaction must be started by the caller.");
        }

        System.out.println("📘 Processing Excel file: " + file.getName());

        Workbook workbook;
        try (FileInputStream fis = new FileInputStream(file)) {
            workbook = file.getName().toLowerCase().endsWith(".xls")
                    ? new HSSFWorkbook(fis)
                    : new XSSFWorkbook(fis);
        }

        try {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return;

            Map<String, Integer> colIndex = new HashMap<>();
            Set<String> actualHeaders = new HashSet<>();

            System.out.println("🔍 Found headers:");
            for (Cell cell : headerRow) {
                String name = switch (cell.getCellType()) {
                    case STRING -> cell.getStringCellValue();
                    case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                    default -> "";
                };
                name = name.trim().toLowerCase();
                actualHeaders.add(name);
                colIndex.put(name, cell.getColumnIndex());
                System.out.println(" - [" + name + "]");
            }

            List<String> expectedHeaders = mapping.columns();
            if (!actualHeaders.containsAll(expectedHeaders)) {
                System.out.println("⚠️ Skipping: missing expected headers in " + file.getName());
                return;
            }

            int added = 0;
            BillingGroup bg = em.find(BillingGroup.class, 99);
            if (bg == null) {
                bg = new BillingGroup();
                bg.setId(99);
                bg.setDescription("Other");
                em.persist(bg);
            }

            TemplateGroup defaultGroup = em.find(TemplateGroup.class, 2);
            TemplateGroup secondaryGroup = em.find(TemplateGroup.class, 1);
            if (defaultGroup == null) {
                throw new IllegalStateException("TemplateGroup with ID 2 not found.");
            } else if (secondaryGroup == null) {
                throw new IllegalStateException("TemplateGroup with ID 1 not found.");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell idCell = row.getCell(colIndex.get("plan type id"));
                if (idCell == null || idCell.getCellType() != CellType.NUMERIC) continue;

                int id = (int) idCell.getNumericCellValue();
                if (em.find(PlanType.class, id) != null) continue;

                PlanType pt = new PlanType();
                pt.setPlanTypeId(id);
                pt.setCode(getString(row, colIndex.get("plan type code")));
                String name = getString(row, colIndex.get("plan type name"));
                pt.setPlanTypeName(name);
                pt.setBillingGroup(bg);

                TemplatePurpose tp = new TemplatePurpose();
                tp.setDescription(name);
                tp.setSortOrder(id);
                tp.setTemplateGroup(defaultGroup);
                em.persist(tp);

                TemplatePurpose tp2 = new TemplatePurpose();
                tp2.setDescription(name);
                tp2.setSortOrder(id);
                tp2.setTemplateGroup(secondaryGroup);
                em.persist(tp2);

                pt.setTemplatePurpose(tp2);
                em.persist(pt);

                if (++added % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }

            System.out.println("✅ Imported " + added + " entries from: " + file.getName());

        } finally {
            workbook.close(); // <-- this ensures the file is no longer in use
        }

        // Now safe to move file
        moveToArchive(file, archiveFolder);
    }

    private static void processCsvFile(EntityManager em, File file, File archiveFolder) throws Exception {
        System.out.println("📄 Processing CSV file: " + file.getName());

        if (!em.getTransaction().isActive()) {
            throw new IllegalStateException("No active transaction.");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.toLowerCase().split(",");
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIndex.put(headers[i].trim(), i);
            }

            List<String> expectedHeaders = Arrays.asList("plan type id", "plan type code", "plan type name");
            if (!colIndex.keySet().containsAll(expectedHeaders)) {
                System.out.println("⚠️ Skipping: missing expected headers in " + file.getName());
                return;
            }

            int added = 0;

            BillingGroup bg = em.find(BillingGroup.class, 99);
            if (bg == null) {
                bg = new BillingGroup();
                bg.setId(99);
                bg.setDescription("Other");
                em.persist(bg);
            }

            TemplateGroup defaultGroup = em.find(TemplateGroup.class, 2);
            if (defaultGroup == null)
                throw new IllegalStateException("TemplateGroup with ID 2 not found.");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[colIndex.get("plan type id")].trim());
                if (em.find(PlanType.class, id) != null) continue;

                String code = parts[colIndex.get("plan type code")].trim();
                String name = parts[colIndex.get("plan type name")].trim();

                PlanType pt = new PlanType();
                pt.setPlanTypeId(id);
                pt.setCode(code);
                pt.setPlanTypeName(name);
                pt.setBillingGroup(bg);

                TemplatePurpose tp = new TemplatePurpose();
                tp.setDescription(name);
                tp.setSortOrder(id);
                tp.setTemplateGroup(defaultGroup);
                em.persist(tp);

                pt.setTemplatePurpose(tp);
                em.persist(pt);

                if (++added % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }

            System.out.println("✅ Imported " + added + " entries from: " + file.getName());
        }

        moveToArchive(file, archiveFolder);
    }

    private static void moveToArchive(File file, File archiveFolder) throws IOException {
        if (!archiveFolder.exists()) archiveFolder.mkdirs();
        Path target = archiveFolder.toPath().resolve(file.getName());
        Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("📁 Moved file to: " + target);
    }

    private static String getString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula(); // or evaluate it if needed
            default -> "";
        };
    }
    public static List<String> extractHeaders(Path filePath) throws IOException {
        String name = filePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".csv")) {
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String firstLine = reader.readLine();
                if (firstLine == null) return Collections.emptyList();
                return Arrays.stream(firstLine.split(","))
                        .map(h -> h.trim().toLowerCase())
                        .collect(Collectors.toList());
            }
        } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            try (InputStream in = Files.newInputStream(filePath)) {
                Workbook workbook = name.endsWith(".xlsx") ? new XSSFWorkbook(in) : new HSSFWorkbook(in);
                Sheet sheet = workbook.getSheetAt(0);
                Row row = sheet.getRow(0);
                if (row == null) return Collections.emptyList();

                List<String> headers = new ArrayList<>();
                for (Cell cell : row) {
                    headers.add(cell.getStringCellValue().trim().toLowerCase());
                }
                return headers;
            } catch (Exception e) {
                throw new IOException("Failed to read Excel headers: " + e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    public static TableMapping findMatchingTableMapping(Path path) throws IOException {
        String filename = path.getFileName().toString().toLowerCase();
        List<String> headers = extractHeaders(path);
        List<String> normalizedHeaders = normalize(headers);

        for (TableMapping mapping : TABLE_MAPPINGS) {
            if (mapping.requiresHeaders()) {
                List<String> expected = normalize(mapping.columns());
                if (normalizedHeaders.containsAll(expected)) {
                    return mapping;
                }
            } else if (mapping.customHandler()) {
                // fallback: match custom handler by loose filename rule
                if (filename.contains(mapping.filePrefix().toLowerCase())) {
                    return mapping;
                }
            } else {
                if (filename.startsWith(mapping.filePrefix().toLowerCase())) {
                    return mapping;
                }
            }
        }

        return null;
    }


    public static void moveWithRetry(Path source, Path target, int maxAttempts, long waitMillis) throws IOException {
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (FileSystemException e) {
                if (i == maxAttempts) throw e;
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry", ie);
                }
            }
        }
    }



    public static void importAllMatchingFilesInMappingOrder(EntityManager em, File uploadDir, File processedDir) throws Exception {
        if (!processedDir.exists()) processedDir.mkdirs();
        File unknownDir = new File(BillingHelper.UNKNOWN_DIR);
        if (!unknownDir.exists()) unknownDir.mkdirs();

        List<File> availableFiles = Arrays.stream(Objects.requireNonNull(uploadDir.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".csv") ||
                                name.toLowerCase().endsWith(".xlsx") ||
                                name.toLowerCase().endsWith(".xls"))))
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        int processedCount = 0;
        int skippedMappings = 0;
        List<String> skippedPrefixes = new ArrayList<>();
        DateTimeFormatter tsFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        Set<File> matchedFiles = new HashSet<>();

        for (TableMapping mapping : TABLE_MAPPINGS) {
            boolean processedFirst = false;
            Iterator<File> iterator = availableFiles.iterator();

            while (iterator.hasNext()) {
                File file = iterator.next();
                String fileNameLower = file.getName().toLowerCase();
                boolean isMatch = false;

                if (mapping.requiresHeaders()) {
                    Set<String> fileHeaders = extractHeaders(file).stream()
                            .map(h -> h.trim().toLowerCase().replaceAll("[^a-z0-9]", ""))
                            .collect(Collectors.toSet());

                    Set<String> expectedHeaders = mapping.columns().stream()
                            .map(col -> mapping.headerOverrides().getOrDefault(col, col))
                            .map(h -> h.trim().toLowerCase().replaceAll("[^a-z0-9]", ""))
                            .collect(Collectors.toSet());

                    Set<String> missing = new HashSet<>(expectedHeaders);
                    missing.removeAll(fileHeaders);

                    if (!missing.isEmpty()) {
                        System.out.println("🚫 Skipping file due to missing headers for mapping: " + mapping.filePrefix());
                        System.out.println("🗂️ File: " + file.getName());
                        System.out.println("🔍 Headers in file: " + fileHeaders);
                        System.out.println("📋 Expected headers: " + expectedHeaders);
                        System.out.println("❌ Missing: " + missing);
                    }

                    if (fileHeaders.containsAll(expectedHeaders)) {
                        System.out.println("✅ Header match for: " + mapping.filePrefix() + " → " + file.getName());
                        isMatch = true;
                    }

                } else {
                    if (fileNameLower.startsWith(mapping.filePrefix().toLowerCase())) {
                        System.out.println("✅ Prefix match for: " + mapping.filePrefix() + " → " + file.getName());
                        isMatch = true;
                    }
                }

                if (isMatch) {
                    try {
                        String ext = getFileExtension(file.getName());
                        String timestamp = LocalDateTime.now().format(tsFormat);
                        Path targetFile;

                        if (!processedFirst) {
                            System.out.println("📥 Processing " + file.getName() + " for mapping: " + mapping.filePrefix());

                            if (mapping.customHandler()) {
                                handleCustomImport(em, file, uploadDir, processedDir, mapping);
                            } else {
                                if (file.getName().toLowerCase().endsWith(".csv")) {
                                    convertFileToUTF8(file);
                                }
                                fallbackCsvInsert(em, file, mapping, processedDir);
                            }

                            targetFile = processedDir.toPath().resolve(mapping.filePrefix() + "_processed_" + timestamp + ext);
                            moveSafely(file, targetFile);
                            processedCount++;
                            processedFirst = true;
                        } else {
                            targetFile = processedDir.toPath().resolve(mapping.filePrefix() + "_skipped_" + timestamp + ext);
                            moveSafely(file, targetFile);
                            System.out.println("⏭️ Skipped extra match: " + file.getName() + " → " + targetFile.getFileName());
                        }

                        iterator.remove();
                        matchedFiles.add(file);

                    } catch (Exception ex) {
                        System.err.println("❌ Failed to process " + file.getName() + ": " + ex.getMessage());
                        ex.printStackTrace();
                        break;
                    }
                }
            }

            if (!processedFirst) {
                skippedMappings++;
                skippedPrefixes.add(mapping.filePrefix());
            }
        }

        // Move unmatched files to unknownDir
        for (File file : availableFiles) {
            if (!matchedFiles.contains(file)) {
                try {
                    Path dest = unknownDir.toPath().resolve(file.getName());
                    moveSafely(file, dest);
                    System.out.println("❓ Moved unmatched file to unknown: " + file.getName());
                } catch (Exception ex) {
                    System.err.println("❌ Failed to move unmatched file: " + file.getName() + " → " + ex.getMessage());
                }
            }
        }

        // Summary
        System.out.println("\n------------------------");
        System.out.println("🏁 Ordered import complete.");
        System.out.println("✅ Files processed: " + processedCount);
        System.out.println("⚠️ Mappings skipped: " + skippedMappings);
        if (!skippedPrefixes.isEmpty()) {
            System.out.println("⚠️ Skipped mappings: " + String.join(", ", skippedPrefixes));
        }
        System.out.println("📂 Files remaining in upload folder: " + availableFiles.size());
        if (!availableFiles.isEmpty()) {
            for (File f : availableFiles) {
                System.out.println("   • " + f.getName());
            }
        }
    }


    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot);
    }

    // With retries and delay
    private static void moveSafely(File sourceFile, Path dest, int attempts, int delayMillis) throws IOException, InterruptedException {
        for (int i = 0; i < attempts; i++) {
            try {
                Files.move(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException ex) {
                if (i == attempts - 1) throw ex;
                Thread.sleep(delayMillis);
            }
        }
    }

    // Simple one-time move with graceful handling
    private static void moveSafely(File sourceFile, Path dest) throws IOException {
        try {
            Files.move(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchFileException nfex) {
            System.out.println("⚠️ File already handled (probably moved earlier): " + sourceFile.getName());
        }
    }



    public static Set<String> extractHeaders(File file) {
        String name = file.getName().toLowerCase();

        try {
            if (name.endsWith(".csv")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String headerLine = reader.readLine();
                    if (headerLine != null) {
                        return Arrays.stream(headerLine.split(","))
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet());
                    }
                }
            } else if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    Workbook workbook = name.endsWith(".xls") ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
                    Sheet sheet = workbook.getSheetAt(0);
                    Row headerRow = sheet.getRow(0);
                    if (headerRow != null) {
                        Set<String> headers = new HashSet<>();
                        for (Cell cell : headerRow) {
                            headers.add(cell.getStringCellValue().trim().toLowerCase());
                        }
                        return headers;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("?? Failed to extract headers from " + file.getName() + ": " + e.getMessage());
        }

        return Collections.emptySet();
    }
    public static TableMapping getMappingByPrefix(String prefix) {
        return TABLE_MAPPINGS.stream()
                .filter(m -> m.filePrefix().equalsIgnoreCase(prefix))
                .findFirst()
                .orElse(null);
    }
    private static List<String> normalize(List<String> headers) {
        return headers.stream()
                .map(h -> h.trim().toLowerCase())
                .collect(Collectors.toList());
    }
    public static void handleCustomImport(EntityManager em, File file, File uploadDir, File processedDir, TableMapping mapping) {
        String table = mapping.tableName();

        switch (table) {
            case "hsaaccount" -> {
                System.out.println("📄 Routing to HSA import: " + file.getName());
                processHsaAccountFile(em, uploadDir.getAbsolutePath(), processedDir.getAbsolutePath());
            }
            case "importplantype" -> {
                System.out.println("📘 Routing to PlanType import: " + file.getName());
                try {
                    processExcelFile(em, file, processedDir, mapping);
                } catch (Exception e) {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                    System.err.println("❌ Error processing: " + file.getName());
                    e.printStackTrace();
                }
            }
            default -> System.out.println("⚠️ No custom handler implemented for: " + table);
        }
    }





}


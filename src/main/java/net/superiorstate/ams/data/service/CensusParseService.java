package net.superiorstate.ams.data.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses and validates an employer's census spreadsheet. <b>Pure parsing — no persistence, no
 * HTTP, no disk.</b> Callers hand it a stream and a filename and get back either rows or errors.
 * <p>
 * <b>Column order in the employer's file is irrelevant.</b> Every field is located by matching
 * its header against a synonym set, so the employer's columns may appear in any order with any
 * number of unrecognised columns interleaved. There is deliberately no positional fallback: a
 * file whose headers cannot be matched fails by name rather than being read by position and
 * silently mis-mapped.
 * <p>
 * <b>SSN, date of birth and compensation are never collected</b> (LA-35). They are simply
 * unrecognised headers and are dropped with every other unrecognised column — the exclusion
 * lives here, at the parser, not only at the schema, so the values never enter the process.
 * <p>
 * <b>All or nothing.</b> If any data row fails validation, {@link Result#isSuccess()} is false,
 * {@link Result#getRows()} is empty and every failure is reported with its row number. Nothing
 * partial is ever returned.
 */
public abstract class CensusParseService {

    /** Beyond this many data rows the file is refused outright. */
    public static final int MAX_DATA_ROWS = 5_000;

    private static final Pattern POSTAL_CODE = Pattern.compile("\\d{5}(-\\d{4})?");
    /** Exactly four digits — the shape Excel leaves when it strips a ZIP's leading zero. */
    private static final Pattern FOUR_DIGITS = Pattern.compile("\\d{4}");
    private static final Pattern STATE_CODE = Pattern.compile("[A-Z]{2}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    // Canonical field names. These are the parser's own vocabulary and match the column names
    // in employer_participant (V094); they are not the employer's header text.
    public static final String F_FIRST_NAME = "first_name";
    public static final String F_LAST_NAME = "last_name";
    public static final String F_ADDRESS_LINE1 = "address_line1";
    public static final String F_ADDRESS_LINE2 = "address_line2";
    public static final String F_CITY = "city";
    public static final String F_STATE = "state";
    public static final String F_POSTAL_CODE = "postal_code";
    public static final String F_EMAIL = "email";

    private static final List<String> REQUIRED_FIELDS = List.of(
            F_FIRST_NAME, F_LAST_NAME, F_ADDRESS_LINE1, F_CITY, F_STATE, F_POSTAL_CODE);

    /**
     * Every field in the order an operator reads them, with a plain-language label. Used only by
     * the mapping report — the canonical field names above stay the parser's vocabulary, and this
     * list has no effect on matching.
     */
    private static final List<String> DISPLAY_ORDER = List.of(
            F_FIRST_NAME, F_LAST_NAME, F_ADDRESS_LINE1, F_ADDRESS_LINE2,
            F_CITY, F_STATE, F_POSTAL_CODE, F_EMAIL);

    private static final Map<String, String> FIELD_LABELS = Map.of(
            F_FIRST_NAME, "First name",
            F_LAST_NAME, "Last name",
            F_ADDRESS_LINE1, "Address line 1",
            F_ADDRESS_LINE2, "Address line 2",
            F_CITY, "City",
            F_STATE, "State",
            F_POSTAL_CODE, "Zip",
            F_EMAIL, "Email");

    /**
     * Normalised header text to canonical field. Built once. A header is normalised to lowercase
     * with every non-alphanumeric character removed before lookup, so "First Name", "first_name"
     * and "FIRST-NAME" all collapse to {@code firstname}.
     * <p>
     * This is a <b>lookup, not a layout</b> — the order of entries here says nothing about the
     * order of columns in any file.
     */
    private static final Map<String, String> HEADER_SYNONYMS = buildHeaderSynonyms();

    private static Map<String, String> buildHeaderSynonyms() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String h : List.of("firstname", "first", "fname", "employeefirstname", "givenname"))
            m.put(h, F_FIRST_NAME);
        for (String h : List.of("lastname", "last", "lname", "employeelastname", "surname", "familyname"))
            m.put(h, F_LAST_NAME);
        for (String h : List.of("address", "address1", "addressline1", "street", "streetaddress",
                "homeaddress", "mailingaddress"))
            m.put(h, F_ADDRESS_LINE1);
        for (String h : List.of("address2", "addressline2", "apt", "unit", "suite", "apartment"))
            m.put(h, F_ADDRESS_LINE2);
        for (String h : List.of("city", "town", "mailingcity"))
            m.put(h, F_CITY);
        for (String h : List.of("state", "st", "statecode", "mailingstate"))
            m.put(h, F_STATE);
        for (String h : List.of("zip", "zipcode", "postalcode", "postal", "mailingzip"))
            m.put(h, F_POSTAL_CODE);
        for (String h : List.of("email", "emailaddress", "workemail", "personalemail", "employeeemail"))
            m.put(h, F_EMAIL);
        return Map.copyOf(m);
    }

    // ── Result types ─────────────────────────────────────────────────────

    /** One parsed census row. Field values only — no entity, no id, no persistence concern. */
    public static class CensusRow {
        private final int rowNumber;
        private final String firstName;
        private final String lastName;
        private final String addressLine1;
        private final String addressLine2;
        private final String city;
        private final String state;
        private final String postalCode;
        private final String email;

        public CensusRow(int rowNumber, String firstName, String lastName, String addressLine1,
                         String addressLine2, String city, String state, String postalCode,
                         String email) {
            this.rowNumber = rowNumber;
            this.firstName = firstName;
            this.lastName = lastName;
            this.addressLine1 = addressLine1;
            this.addressLine2 = addressLine2;
            this.city = city;
            this.state = state;
            this.postalCode = postalCode;
            this.email = email;
        }

        public int getRowNumber() { return rowNumber; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getAddressLine1() { return addressLine1; }
        public String getAddressLine2() { return addressLine2; }
        public String getCity() { return city; }
        public String getState() { return state; }
        public String getPostalCode() { return postalCode; }
        public String getEmail() { return email; }
    }

    /**
     * One validation failure. {@code rowNumber} is the 1-based line in the source file as the
     * employer sees it, so a person can find it in their spreadsheet. A file-level failure
     * (missing header, unreadable file, too many rows) reports row 0.
     */
    public static class RowError {
        private final int rowNumber;
        private final String column;
        private final String reason;

        public RowError(int rowNumber, String column, String reason) {
            this.rowNumber = rowNumber;
            this.column = column;
            this.reason = reason;
        }

        public int getRowNumber() { return rowNumber; }
        public String getColumn() { return column; }
        public String getReason() { return reason; }
    }

    /**
     * One field and the employer's header that supplied it. {@code header} is null when no column
     * in the file matched — an absent optional field is reported, not omitted, because absence is
     * information an operator needs before sending the data to Summit.
     */
    public static class FieldMatch {
        private final String field;
        private final String label;
        private final String header;
        private final boolean required;

        FieldMatch(String field, String label, String header, boolean required) {
            this.field = field;
            this.label = label;
            this.header = header;
            this.required = required;
        }

        public String getField() { return field; }
        public String getLabel() { return label; }
        /** The employer's header text exactly as it appeared, or null if nothing matched. */
        public String getHeader() { return header; }
        public boolean isRequired() { return required; }
        public boolean isMatched() { return header != null; }
    }

    /**
     * What the parser actually read from the file: which header supplied each field, which
     * columns it ignored, and how many blank rows it skipped.
     * <p>
     * <b>Reporting only.</b> Nothing here influences matching or validation — it is the same
     * header map the parser already built, exposed rather than discarded, so an operator can
     * check that "it parsed" and "it read the columns I think it read" are the same claim.
     * <p>
     * Populated on failure paths too. A missing-header failure is exactly when someone most
     * needs to see what <em>did</em> match.
     */
    public static class MappingReport {
        private final List<FieldMatch> fields;
        private final List<String> unmatchedRequiredFields;
        private final List<String> ignoredColumns;
        private final int skippedBlankRows;
        private final int dataRowCount;

        MappingReport(List<FieldMatch> fields, List<String> unmatchedRequiredFields,
                      List<String> ignoredColumns, int skippedBlankRows, int dataRowCount) {
            this.fields = fields;
            this.unmatchedRequiredFields = unmatchedRequiredFields;
            this.ignoredColumns = ignoredColumns;
            this.skippedBlankRows = skippedBlankRows;
            this.dataRowCount = dataRowCount;
        }

        /** All eight fields in reading order, matched or not. Never null, never partial. */
        public List<FieldMatch> getFields() { return fields; }
        /** Required fields with no column — empty unless the file failed on a missing header. */
        public List<String> getUnmatchedRequiredFields() { return unmatchedRequiredFields; }
        /**
         * Headers present in the file that matched no field, in file order, as written.
         * <b>Names only — no value from an ignored column is ever carried here.</b>
         */
        public List<String> getIgnoredColumns() { return ignoredColumns; }
        public int getSkippedBlankRows() { return skippedBlankRows; }
        public int getDataRowCount() { return dataRowCount; }

        /** Used before a header row is even located, so the report is never null downstream. */
        static MappingReport empty() {
            return new MappingReport(List.of(), List.of(), List.of(), 0, 0);
        }
    }

    /** Either rows or errors, never both populated. */
    public static class Result {
        private final List<CensusRow> rows;
        private final List<RowError> errors;
        private MappingReport mapping = MappingReport.empty();

        private Result(List<CensusRow> rows, List<RowError> errors) {
            this.rows = rows;
            this.errors = errors;
        }

        static Result ok(List<CensusRow> rows) { return new Result(rows, List.of()); }
        static Result failed(List<RowError> errors) { return new Result(List.of(), errors); }
        static Result failedWith(int rowNumber, String column, String reason) {
            return failed(List.of(new RowError(rowNumber, column, reason)));
        }

        /** Attaches the mapping report. Fluent so every return site in interpret() is one line. */
        Result with(MappingReport mapping) {
            this.mapping = mapping == null ? MappingReport.empty() : mapping;
            return this;
        }

        public boolean isSuccess() { return errors.isEmpty(); }
        public List<CensusRow> getRows() { return rows; }
        public List<RowError> getErrors() { return errors; }
        /** Never null — an empty report when the file could not be read far enough to build one. */
        public MappingReport getMapping() { return mapping; }
    }

    // ── Entry point ──────────────────────────────────────────────────────

    /**
     * Parses a census from a stream. The stream is read once and never written anywhere — the
     * caller is expected to hand in a multipart {@code Part}'s input stream directly.
     *
     * @param in       the file's bytes
     * @param filename used only to choose a reader by extension; never used as a path
     */
    public static Result parse(InputStream in, String filename) {
        if (in == null) {
            return Result.failedWith(0, null, "No file was received.");
        }
        String ext = extensionOf(filename);
        try {
            List<List<Cellv>> grid;
            if ("csv".equals(ext) || "txt".equals(ext)) {
                grid = readDelimited(in);
            } else if ("xlsx".equals(ext) || "xls".equals(ext)) {
                grid = readSpreadsheet(in, "xls".equals(ext));
            } else {
                return Result.failedWith(0, null,
                        "Unsupported file type '" + (ext == null ? "(none)" : ext)
                                + "'. Upload a .csv, .txt, .xlsx or .xls file.");
            }
            return interpret(grid);
        } catch (Exception e) {
            return Result.failedWith(0, null, "Could not read the file: " + e.getMessage());
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null) return null;
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) return null;
        return filename.substring(i + 1).toLowerCase();
    }

    // ── Readers — both produce the same neutral grid ─────────────────────

    /**
     * One source cell: its <b>displayed</b> text, plus whether the source cell was numerically
     * typed. The type flag exists for exactly one purpose — the leading-zero ZIP inference in
     * {@link #validateRow} — and is always false for delimited input, which has no cell types.
     */
    private static final class Cellv {
        final String text;
        final boolean numeric;

        Cellv(String text, boolean numeric) {
            this.text = text == null ? "" : text;
            this.numeric = numeric;
        }

        static final Cellv EMPTY = new Cellv("", false);
    }

    private static List<List<Cellv>> readDelimited(InputStream in) throws Exception {
        List<List<Cellv>> grid = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(in, StandardCharsets.UTF_8)).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                List<Cellv> cells = new ArrayList<>(line.length);
                // Delimited input carries no cell type, so nothing here is ever "numeric" and the
                // leading-zero inference can never fire on a CSV. A four-digit ZIP in a CSV is
                // what the employer typed, and it fails validation.
                for (String cell : line) cells.add(new Cellv(cell, false));
                grid.add(cells);
            }
        }
        return grid;
    }

    /** First sheet only, deliberately — a census is one sheet, and guessing among several is worse than refusing. */
    private static List<List<Cellv>> readSpreadsheet(InputStream in, boolean legacy) throws Exception {
        List<List<Cellv>> grid = new ArrayList<>();
        // DataFormatter renders every cell as the string Excel displays, which is what stops a
        // General-formatted numeric ZIP reading as "54143.0". It is applied to every column, not
        // just ZIP — the same failure shape applies to any numeric-looking field.
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = legacy ? new HSSFWorkbook(in) : new XSSFWorkbook(in)) {
            if (workbook.getNumberOfSheets() == 0) return grid;
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                int last = row.getLastCellNum();
                List<Cellv> cells = new ArrayList<>(Math.max(last, 0));
                for (int c = 0; c < last; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) {
                        cells.add(Cellv.EMPTY);
                    } else {
                        cells.add(new Cellv(formatter.formatCellValue(cell), isNumericCell(cell)));
                    }
                }
                grid.add(cells);
            }
        }
        return grid;
    }

    /** True when the cell holds a number, including a formula whose cached result is numeric. */
    private static boolean isNumericCell(Cell cell) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return type == CellType.NUMERIC;
    }

    // ── Interpretation ───────────────────────────────────────────────────

    private static Result interpret(List<List<Cellv>> grid) {
        // The first non-empty row is the header row. Rows above it (a title banner, a blank
        // spacer) are skipped rather than treated as data.
        int headerIndex = -1;
        for (int i = 0; i < grid.size(); i++) {
            if (!isBlankRow(grid.get(i))) { headerIndex = i; break; }
        }
        if (headerIndex < 0) {
            return Result.failedWith(0, null, "The file is empty.");
        }

        List<Cellv> header = grid.get(headerIndex);

        // Ignored columns are collected in a separate read-only pass so the list is complete
        // even when the matching loop below bails early on a duplicate. This pass writes
        // nothing the matching loop reads, so it cannot affect what matches.
        List<String> ignoredColumns = new ArrayList<>();
        for (Cellv cell : header) {
            String raw = cell.text;
            String normalised = normaliseHeader(raw);
            if (normalised.isEmpty()) continue;
            if (HEADER_SYNONYMS.get(normalised) == null) ignoredColumns.add(raw.trim());
        }

        // Header text to column index. Every column is located here, by name.
        Map<String, Integer> fieldColumns = new LinkedHashMap<>();
        // The header text as the employer wrote it, per field, so a duplicate can name both.
        Map<String, String> fieldHeaderText = new LinkedHashMap<>();
        for (int c = 0; c < header.size(); c++) {
            String raw = header.get(c).text;
            String normalised = normaliseHeader(raw);
            if (normalised.isEmpty()) continue;
            String field = HEADER_SYNONYMS.get(normalised);
            // Unrecognised columns are ignored silently — employer files carry payroll,
            // department, salary, SSN and DOB columns we neither need nor want.
            if (field == null) continue;
            // Two columns mapping to one field is refused, not resolved. Picking one quietly
            // would silently discard whichever column we did not choose, and the two may hold
            // different data — the employer has to say which one they mean.
            if (fieldColumns.containsKey(field)) {
                return Result.failedWith(headerIndex + 1, field,
                        "Two columns both map to '" + field + "': '"
                                + fieldHeaderText.get(field).trim() + "' and '" + raw.trim()
                                + "'. Remove or rename one so only a single column supplies it.")
                        .with(report(fieldHeaderText, ignoredColumns, 0, 0));
            }
            fieldColumns.put(field, c);
            fieldHeaderText.put(field, raw);
        }

        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_FIELDS) {
            if (!fieldColumns.containsKey(required)) missing.add(required);
        }
        if (!missing.isEmpty()) {
            return Result.failedWith(headerIndex + 1, String.join(", ", missing),
                    "The file has no column for: " + String.join(", ", missing)
                            + ". Every required column is located by its heading, so the heading"
                            + " must be present — column order does not matter.")
                    .with(report(fieldHeaderText, ignoredColumns, 0, 0));
        }

        List<CensusRow> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        int dataRows = 0;
        int skippedBlankRows = 0;

        for (int i = headerIndex + 1; i < grid.size(); i++) {
            List<Cellv> raw = grid.get(i);
            if (isBlankRow(raw)) {           // a fully blank row is skipped, not an error
                skippedBlankRows++;
                continue;
            }
            dataRows++;
            if (dataRows > MAX_DATA_ROWS) {
                return Result.failedWith(0, null,
                        "The file has more than " + MAX_DATA_ROWS + " employees. Split it and"
                                + " upload the parts separately.")
                        .with(report(fieldHeaderText, ignoredColumns, skippedBlankRows, dataRows));
            }
            validateRow(i + 1, raw, fieldColumns, rows, errors);
        }

        MappingReport mapping =
                report(fieldHeaderText, ignoredColumns, skippedBlankRows, dataRows);

        if (!errors.isEmpty()) return Result.failed(errors).with(mapping);
        if (rows.isEmpty()) {
            return Result.failedWith(0, null, "The file has a header row but no employees.")
                    .with(mapping);
        }
        return Result.ok(rows).with(mapping);
    }

    /**
     * Builds the mapping report from the header map the parser already produced. Pure
     * projection — it reads {@code fieldHeaderText} and writes nothing back.
     */
    private static MappingReport report(Map<String, String> fieldHeaderText,
                                        List<String> ignoredColumns,
                                        int skippedBlankRows, int dataRowCount) {
        List<FieldMatch> fields = new ArrayList<>(DISPLAY_ORDER.size());
        List<String> unmatchedRequired = new ArrayList<>();
        for (String field : DISPLAY_ORDER) {
            String rawHeader = fieldHeaderText.get(field);
            boolean required = REQUIRED_FIELDS.contains(field);
            // Header kept verbatim apart from surrounding whitespace — the operator compares it
            // against their own spreadsheet, so the normalised form would be useless.
            fields.add(new FieldMatch(field, FIELD_LABELS.get(field),
                    rawHeader == null ? null : rawHeader.trim(), required));
            if (required && rawHeader == null) unmatchedRequired.add(field);
        }
        return new MappingReport(List.copyOf(fields), List.copyOf(unmatchedRequired),
                List.copyOf(ignoredColumns), skippedBlankRows, dataRowCount);
    }

    private static void validateRow(int rowNumber, List<Cellv> raw, Map<String, Integer> cols,
                                    List<CensusRow> rows, List<RowError> errors) {
        int before = errors.size();

        String firstName = required(rowNumber, raw, cols, F_FIRST_NAME, errors);
        String lastName = required(rowNumber, raw, cols, F_LAST_NAME, errors);
        String addressLine1 = required(rowNumber, raw, cols, F_ADDRESS_LINE1, errors);
        String city = required(rowNumber, raw, cols, F_CITY, errors);
        String rawState = required(rowNumber, raw, cols, F_STATE, errors);
        String rawPostal = required(rowNumber, raw, cols, F_POSTAL_CODE, errors);

        String addressLine2 = blankToNull(cellAt(raw, cols.get(F_ADDRESS_LINE2)));
        String email = blankToNull(cellAt(raw, cols.get(F_EMAIL)));

        String state = null;
        if (rawState != null) {
            state = rawState.trim().toUpperCase();
            if (!STATE_CODE.matcher(state).matches()) {
                errors.add(new RowError(rowNumber, F_STATE,
                        "'" + rawState.trim() + "' is not a two-letter state code."));
            }
        }

        String postalCode = null;
        if (rawPostal != null) {
            postalCode = rawPostal.replaceAll("\\s", "");
            // Leading-zero recovery, deliberately narrow. Excel stores an unformatted ZIP as a
            // number, so 07001 round-trips as 7001 — a four-digit US ZIP does not otherwise
            // exist, and the spreadsheet is the known cause. The inference fires ONLY when the
            // source cell was numerically typed: a four-digit value in a text cell (or any CSV,
            // which has no cell types at all) is what the employer typed, and it still fails.
            // Applied to postal_code only; no other field is ever padded.
            if (isNumericSourceCell(raw, cols.get(F_POSTAL_CODE))
                    && FOUR_DIGITS.matcher(postalCode).matches()) {
                postalCode = "0" + postalCode;
            }
            if (!POSTAL_CODE.matcher(postalCode).matches()) {
                errors.add(new RowError(rowNumber, F_POSTAL_CODE,
                        "'" + rawPostal.trim() + "' is not a 5-digit or ZIP+4 postal code."));
            }
        }

        // Email is validated only when present — absent and blank are both fine.
        if (email != null && !isPlausibleEmail(email)) {
            errors.add(new RowError(rowNumber, F_EMAIL, "'" + email + "' is not a valid email address."));
        }

        if (errors.size() == before) {
            rows.add(new CensusRow(rowNumber, firstName.trim(), lastName.trim(),
                    addressLine1.trim(), addressLine2 == null ? null : addressLine2.trim(),
                    city.trim(), state, postalCode, email));
        }
    }

    /** Records an error and returns null when a required value is absent or blank. */
    private static String required(int rowNumber, List<Cellv> raw, Map<String, Integer> cols,
                                   String field, List<RowError> errors) {
        String value = cellAt(raw, cols.get(field));
        if (value == null || value.trim().isEmpty()) {
            errors.add(new RowError(rowNumber, field, "Required value is missing."));
            return null;
        }
        return value;
    }

    /** A single {@code @} with a {@code .} somewhere after it. Deliberately not RFC 5322. */
    private static boolean isPlausibleEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1) return false;
        int dot = email.indexOf('.', at);
        return dot > at + 1 && dot < email.length() - 1;
    }

    private static String cellAt(List<Cellv> row, Integer index) {
        if (index == null || index < 0 || index >= row.size()) return null;
        return row.get(index).text;
    }

    /** Whether the source cell at this column was numerically typed. Always false for CSV. */
    private static boolean isNumericSourceCell(List<Cellv> row, Integer index) {
        if (index == null || index < 0 || index >= row.size()) return false;
        return row.get(index).numeric;
    }

    private static String blankToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }

    private static boolean isBlankRow(List<Cellv> row) {
        if (row == null || row.isEmpty()) return true;
        for (Cellv cell : row) {
            if (cell != null && !cell.text.trim().isEmpty()) return false;
        }
        return true;
    }

    private static String normaliseHeader(String header) {
        if (header == null) return "";
        return NON_ALPHANUMERIC.matcher(header.toLowerCase()).replaceAll("");
    }
}

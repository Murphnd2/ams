package net.superiorstate.ams.data.service.audit;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.service.SummitSftpService;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T237 second check: contribution-funded purses (PremiumPath allowance/premium purses, health FSA)
 * that received a contribution in the evaluation month but paid nothing out. For PremiumPath a
 * month with money in and nothing out means the premium was never attempted — a coverage-lapse
 * signal that card-decline monitoring cannot see, because a payment that was never tried produces
 * no declined transaction.
 * <p>
 * Reads the newest matching Participant Plan History export in the {@code ExportFiles} directory
 * sibling to {@code SUMMIT_SFTP_IMPORT_DIR}, over the same {@code SummitSftpService} transport and
 * with the same newest-file / staleness / byte-cap rules as {@link IchraUncodedParticipantsCheck}.
 * Columns are resolved <b>by header name, never by position</b>.
 * <p>
 * <b>Employer scope is Summit-side, deliberately.</b> The export template
 * ({@code ZZ_PARTICIPANT_HISTORY_AUDIT}, export type Participant Account History with Division
 * Option) has a required Employer selector; adding a group to that export is part of plan setup.
 * This check has no AMS-side employer list — one existed briefly and was removed because it
 * duplicated the template's selector, creating a second place to maintain and a second way for a
 * group to be silently excluded. This matches {@link IchraUncodedParticipantsCheck}, whose
 * employer scope is likewise Summit-side (TA-56). The plan-type filter below is <b>retained as a
 * guard</b> in case the template's scope is ever widened to plan types this check should not
 * evaluate.
 * <p>
 * <b>Predicate.</b> Rows are kept for configured plan types ({@code PlanTypeID}) whose
 * {@code SystemDate} falls in the evaluation month, then grouped by
 * {@code (Participant_ID, ParticipantPlan_ID)}. Per group,
 * contribution = sum of |{@code Transactionamt}| over rows whose {@code TransactionType} is in the
 * configured contribution list, disbursement = sum of |{@code Transactionamt}| over the configured
 * disbursement list. A finding is a group with contribution &gt; 0 and disbursement == 0.
 * Direction comes only from the two configured lists — the sign of {@code Transactionamt} is
 * deliberately ignored. A row whose type is in neither list is ignored; the count of distinct
 * ignored types is reported in the summary so unmapped types are visible, not silent.
 * <p>
 * <b>Total failure is {@code ERROR}, never {@code OK}.</b> If no row survives the scope filter,
 * or none of the scoped rows parses a date, or none of the date-parsed rows matches either type
 * list, or none of the typed rows falls in the evaluation month, the check evaluated nothing and
 * says so (see {@code totalFailureOf}) — an "all clear"
 * after parsing nothing is a false negative that would be trusted. Partial failures are counted
 * in the summary line only and never change the status; there is no tolerance knob.
 * <p>
 * <b>Not built here (out of scope):</b> expected-amount matching, partial-payment detection.
 * <p>
 * <b>Configuration</b> — all via {@code AppConfig.get} ({@code ssa.properties}); no employer,
 * plan-type or transaction-type value is literal in this class:
 * <ul>
 *   <li>{@code SUMMIT_AUDIT_PLAN_HISTORY_EXPORT_PREFIX} — filename prefix (Summit template name)
 *       of the Participant Plan History export. <b>Required.</b></li>
 *   <li>{@code SUMMIT_AUDIT_FUNDED_PLAN_TYPE_IDS} — comma-separated {@code PlanTypeID} values.
 *       <b>Required.</b> Matched on the numeric id, not {@code PlanName}: ids survive a rename in
 *       Summit; names do not. (No employer key — see "Employer scope is Summit-side" above.)</li>
 *   <li>{@code SUMMIT_AUDIT_FUNDED_CONTRIBUTION_TYPES} — comma-separated {@code TransactionType}
 *       values that mean money in. <b>Required.</b></li>
 *   <li>{@code SUMMIT_AUDIT_FUNDED_DISBURSEMENT_TYPES} — comma-separated {@code TransactionType}
 *       values that mean money out. <b>Required.</b></li>
 *   <li>{@code SUMMIT_AUDIT_FUNDED_MONTH_OFFSET} — closed months back to evaluate; default 1
 *       (the most recent fully-closed calendar month).</li>
 *   <li>{@code SUMMIT_AUDIT_PLAN_HISTORY_MAX_AGE_HOURS} — staleness threshold; default 36.
 *       Separate from {@code SUMMIT_AUDIT_EXPORT_MAX_AGE_HOURS} because this export may be
 *       scheduled on a different cadence from the participant list.</li>
 *   <li>{@code SUMMIT_AUDIT_EXPORT_MAX_BYTES} — reused from the first check; default 16 MiB.</li>
 * </ul>
 * ⚠️ <b>Counts only persist.</b> {@link #evaluate} never returns row content; {@link #readLive}
 * exposes the finding rows for the detail page's one in-request render and nothing else. This
 * check joins to nothing to acquire a name or SSN (LA-40).
 * <p>
 * ⚠️ <b>{@code PTName} is the participant's name</b> ("lastname, firstname"), <b>not</b> the plan
 * type name — {@code PlanName} is the plan label. The export's field set cannot be filtered in
 * Summit, so the file landing in {@code ExportFiles} always carries participant names in that
 * column. This check does not read it: it is not a required header, not in {@link HistoryRow},
 * not on the detail page. The file's contents are an LA-40 handling consideration for the export
 * itself, recorded here so the next reader does not re-add the column thinking it is a plan
 * attribute.
 * <p>
 * <b>Month basis is {@code SystemDate}, unconditionally.</b> {@code EventDate} was once a
 * configurable alternative and was removed after profiling a real 365-row export (2026-09-14):
 * <pre>
 *   TransactionType                                             rows  blank EventDate  blank SystemDate
 *   Debit Card                                                    87        87               0
 *   Claim -Participant Portal/Mobile                              22        22               0
 *   Participant Scheduled Contribution                           200         0               0
 *   Participant Portal/Mobile ClaimsPayment ACH                   16         0               0
 *   Participant Portal/Mobile ClaimsPayment Check                  5         0               0
 *   Election                                                      22         0               0
 *   Participant Single Fund Amount or Annual Election Amount      12         0               0
 *   Credit                                                         1         0               0
 * </pre>
 * {@code EventDate} is blank on every card and claim row. With it as the basis, every disbursement
 * fails date parsing while contributions parse normally — contribution totals with zero
 * disbursements, turning <b>every participant into a false finding</b>. Do not re-add the option.
 * <p>
 * <b>Confirmed by the same export, do not change:</b> the date format is
 * {@code M/d/yyyy h:mm:ss AM} (e.g. {@code 4/1/2026 12:00:00 AM}) — the leading-token
 * {@code M/d/yyyy} parse handles it, zero unparsable {@code SystemDate} values, zero unparsable
 * amounts. All amounts were positive for every type (no signs, parentheses, or currency symbols);
 * {@code abs()} is harmless and direction correctly comes only from the configured lists. The
 * filename shape is {@code {template}_Export_{17-digit timestamp}_CSV.csv} — a token sits between
 * the timestamp and the extension, hence {@link #TIMESTAMPED_NAME} differs from the first check's.
 * The double-count hazard is confirmed: every {@code ClaimsPayment} row matched a
 * {@code Claim -Participant Portal/Mobile} row on {@code ClaimKeyCheckNumber} with an identical
 * amount, and no {@code Debit Card} key appeared as a payment row — so counting
 * {@code {Debit Card, ClaimsPayment ACH, ClaimsPayment Check}} counts each disbursement exactly
 * once, and adding the claim record would double-count every portal reimbursement.
 * {@code ClaimKeyCheckNumber} on a Check payment takes the form {@code 120544/ 20005} (claim key,
 * slash, check number); parse the leading token if ever joining on it — nothing here does.
 */
public class FundedPurseNoDisbursementCheck implements AuditCheck {

    public static final String KEY = "funded_purse_no_disbursement";

    static final String CFG_PREFIX = "SUMMIT_AUDIT_PLAN_HISTORY_EXPORT_PREFIX";
    static final String CFG_PLAN_TYPE_IDS = "SUMMIT_AUDIT_FUNDED_PLAN_TYPE_IDS";
    static final String CFG_CONTRIBUTION_TYPES = "SUMMIT_AUDIT_FUNDED_CONTRIBUTION_TYPES";
    static final String CFG_DISBURSEMENT_TYPES = "SUMMIT_AUDIT_FUNDED_DISBURSEMENT_TYPES";
    static final String CFG_MONTH_OFFSET = "SUMMIT_AUDIT_FUNDED_MONTH_OFFSET";
    static final String CFG_MAX_AGE_HOURS = "SUMMIT_AUDIT_PLAN_HISTORY_MAX_AGE_HOURS";
    /** Reused from {@link IchraUncodedParticipantsCheck} — same landing directory, same cap. */
    static final String CFG_MAX_BYTES = "SUMMIT_AUDIT_EXPORT_MAX_BYTES";

    private static final long DEFAULT_MAX_AGE_HOURS = 36;
    private static final long DEFAULT_MAX_BYTES = 16_777_216L;
    private static final int DEFAULT_MONTH_OFFSET = 1;

    /** This export's ExportFiles naming pattern, observed 2026-09-14:
     *  {@code {template name}_Export_{17-digit yyyyMMddHHmmssSSS}_CSV.csv} — zero or more
     *  underscore-delimited tokens may sit between the timestamp and the extension. This
     *  deliberately differs from {@code IchraUncodedParticipantsCheck}'s pattern (which requires
     *  the timestamp immediately before the extension); the captured 17-digit group is still the
     *  newest-file sort key. Each check owns its own constant — nothing is shared. */
    static final Pattern TIMESTAMPED_NAME =
            Pattern.compile("^.+_(\\d{17})(?:_[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    // Header names — the verified Participant Plan History header row is the contract.
    // PTName is deliberately absent: it is the participant's name (see the class note).
    static final String H_PARTICIPANT_ID = "Participant_ID";
    static final String H_PARTICIPANT_PLAN_ID = "ParticipantPlan_ID";
    static final String H_PLAN_NAME = "PlanName";
    static final String H_PLAN_TYPE_ID = "PlanTypeID";
    static final String H_TRANSACTION_TYPE = "TransactionType";
    static final String H_SYSTEM_DATE = "SystemDate";
    static final String H_TRANSACTION_AMT = "Transactionamt";
    /** Optional, not required: read only for the summary line's distinct-employer count. It is
     *  not a filter — employer scope is Summit-side. Absent header → blank → count omitted. */
    static final String H_EMPLOYER_ID = "Employer_ID";

    private static final List<String> REQUIRED_HEADERS = List.of(
            H_PARTICIPANT_ID, H_PARTICIPANT_PLAN_ID, H_PLAN_NAME,
            H_PLAN_TYPE_ID, H_TRANSACTION_TYPE, H_SYSTEM_DATE, H_TRANSACTION_AMT);

    /** Date shapes seen in Summit exports ({@code SummitImportService.parseDate} plus the
     *  {@code M/d/yyyy h:mm:ss a} form in the J4 sample). Only the leading date token is read. */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M-d-yyyy"));

    @Override
    public String key() { return KEY; }

    @Override
    public String label() { return "Funded purses with no disbursement"; }

    @Override
    public String detailPath() { return "/AuditFundedPurse"; }

    @Override
    public AuditResult evaluate(EntityManager em, Long pspId) {
        try {
            Config config = Config.load();
            if (config.notConfigured != null) return config.notConfigured;

            Loaded loaded = loadLatest(config);
            if (loaded.errorResult != null) return loaded.errorResult;

            Evaluation evaluation = evaluate(loaded.rows, config);
            AuditResult totalFailure = totalFailureOf(evaluation, config, loaded.fileName);
            if (totalFailure != null) return totalFailure;

            String summary = summaryOf(evaluation, config, loaded.fileName);
            return evaluation.findings.isEmpty()
                    ? AuditResult.ok(summary)
                    : AuditResult.action(evaluation.findings.size(), summary);
        } catch (Exception e) {
            return AuditResult.error(scrub(e));
        }
    }

    /**
     * For the detail page — the same read path as {@link #evaluate}, never stored. The returned
     * rows are exactly the fields the detail page renders.
     */
    public Snapshot readLive() {
        try {
            Config config = Config.load();
            if (config.notConfigured != null) {
                return Snapshot.failed(config.notConfigured.error());
            }
            Loaded loaded = loadLatest(config);
            if (loaded.errorResult != null) {
                return Snapshot.failed(loaded.errorResult.error());
            }
            Evaluation evaluation = evaluate(loaded.rows, config);
            AuditResult totalFailure = totalFailureOf(evaluation, config, loaded.fileName);
            if (totalFailure != null) {
                return Snapshot.failed(totalFailure.error());
            }
            List<Row> rows = new ArrayList<>(evaluation.findings.size());
            for (Purse p : evaluation.findings) {
                rows.add(new Row(p.participantId, p.participantPlanId, p.planName,
                        p.contribution.toPlainString(), p.disbursement.toPlainString()));
            }
            return new Snapshot(loaded.fileName, loaded.fileTimestamp, config.evaluationMonth,
                    rows, evaluation.ignoredTypes.size(),
                    new ArrayList<>(evaluation.ignoredTypes.values()), null);
        } catch (Exception e) {
            return Snapshot.failed(scrub(e));
        }
    }

    // ── Config ────────────────────────────────────────────────────────

    /** Everything read from {@code AppConfig}, resolved once per evaluation. {@code notConfigured}
     *  is set (and nothing else is trusted) when a required key is absent, blank, or
     *  contradictory. */
    private record Config(AuditResult notConfigured, String prefix,
                          Set<String> planTypeIds, Set<String> contributionTypes,
                          Set<String> disbursementTypes, YearMonth evaluationMonth,
                          long maxAgeHours, long maxBytes) {

        static Config load() {
            String prefix = AppConfig.get(CFG_PREFIX);
            Set<String> planTypeIds = csvSet(AppConfig.get(CFG_PLAN_TYPE_IDS), false);
            Set<String> contributionTypes = csvSet(AppConfig.get(CFG_CONTRIBUTION_TYPES), true);
            Set<String> disbursementTypes = csvSet(AppConfig.get(CFG_DISBURSEMENT_TYPES), true);

            List<String> missing = new ArrayList<>();
            if (prefix == null || prefix.isBlank()) missing.add(CFG_PREFIX);
            if (planTypeIds.isEmpty()) missing.add(CFG_PLAN_TYPE_IDS);
            if (contributionTypes.isEmpty()) missing.add(CFG_CONTRIBUTION_TYPES);
            if (disbursementTypes.isEmpty()) missing.add(CFG_DISBURSEMENT_TYPES);
            if (!missing.isEmpty()) {
                return failed("Config key(s) not set: " + String.join(", ", missing) + ".");
            }

            Set<String> overlap = new LinkedHashSet<>(contributionTypes);
            overlap.retainAll(disbursementTypes);
            if (!overlap.isEmpty()) {
                return failed(overlap.size() + " TransactionType value(s) appear in both "
                        + CFG_CONTRIBUTION_TYPES + " and " + CFG_DISBURSEMENT_TYPES + ".");
            }

            long offset = parseLongOrDefault(AppConfig.get(CFG_MONTH_OFFSET), DEFAULT_MONTH_OFFSET);
            if (offset < 0) offset = DEFAULT_MONTH_OFFSET;
            YearMonth evaluationMonth = YearMonth.now().minusMonths(offset);

            long maxAgeHours = parseLongOrDefault(AppConfig.get(CFG_MAX_AGE_HOURS), DEFAULT_MAX_AGE_HOURS);
            long maxBytes = parseLongOrDefault(AppConfig.get(CFG_MAX_BYTES), DEFAULT_MAX_BYTES);

            return new Config(null, prefix.trim(), planTypeIds, contributionTypes,
                    disbursementTypes, evaluationMonth, maxAgeHours, maxBytes);
        }

        private static Config failed(String message) {
            return new Config(AuditResult.notConfigured(message), null, Set.of(), Set.of(),
                    Set.of(), null, 0, 0);
        }
    }

    /** Splits a comma-separated value into trimmed, non-blank tokens; lower-cased when
     *  {@code caseInsensitive} so {@code TransactionType} comparison ignores case. */
    private static Set<String> csvSet(String raw, boolean caseInsensitive) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null) return out;
        for (String token : raw.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            out.add(caseInsensitive ? t.toLowerCase(Locale.ROOT) : t);
        }
        return out;
    }

    private static long parseLongOrDefault(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ── Load ──────────────────────────────────────────────────────────

    private Loaded loadLatest(Config config) {
        String importDir = AppConfig.get("SUMMIT_SFTP_IMPORT_DIR");
        String exportDir = exportDirFor(importDir);
        if (exportDir == null) {
            return Loaded.failed(AuditResult.error(
                    "Cannot derive an ExportFiles directory from SUMMIT_SFTP_IMPORT_DIR ('"
                            + importDir + "')."));
        }

        SummitSftpService sftp = new SummitSftpService();

        List<SummitSftpService.SftpEntry> entries;
        try {
            entries = sftp.list(exportDir);
        } catch (SummitSftpService.SftpTransportException e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        String newestName = null;
        long newestTimestamp = -1;
        for (SummitSftpService.SftpEntry entry : entries) {
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (name == null || !name.startsWith(config.prefix)) continue;
            Matcher m = TIMESTAMPED_NAME.matcher(name);
            if (!m.matches()) continue;
            long ts;
            try {
                ts = Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (ts > newestTimestamp) {
                newestTimestamp = ts;
                newestName = name;
            }
        }

        if (newestName == null) {
            return Loaded.failed(AuditResult.error(
                    "No export matching prefix '" + config.prefix + "' found in " + exportDir + "."));
        }

        LocalDateTime fileTimestamp;
        try {
            fileTimestamp = LocalDateTime.parse(String.valueOf(newestTimestamp), TIMESTAMP_FMT);
        } catch (Exception e) {
            return Loaded.failed(AuditResult.error(
                    "Export filename '" + newestName + "' carries an unparsable timestamp."));
        }

        long ageHours = Duration.between(fileTimestamp, LocalDateTime.now()).toHours();
        if (ageHours > config.maxAgeHours) {
            return Loaded.failed(AuditResult.error(
                    "Export is " + ageHours + " hours old (max " + config.maxAgeHours + ")."));
        }

        byte[] bytes;
        try {
            bytes = sftp.read(exportDir, newestName, (int) Math.min(config.maxBytes, Integer.MAX_VALUE));
        } catch (SummitSftpService.SftpTransportException e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        String content = new String(bytes, StandardCharsets.UTF_8);
        // Same BOM-strip pattern as SummitImportService.
        if (content.startsWith("﻿")) {
            content = content.substring(1);
        }

        List<HistoryRow> rows;
        try {
            rows = parse(content);
        } catch (MissingHeaderException e) {
            return Loaded.failed(AuditResult.error(e.getMessage()));
        } catch (Exception e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        return Loaded.ok(newestName, fileTimestamp, rows);
    }

    /** Sibling {@code ExportFiles} directory to {@code importDir} — identical rule to the first
     *  check: the final segment must be exactly {@code ImportFiles}, otherwise {@code null}. */
    private static String exportDirFor(String importDir) {
        if (importDir == null) return null;
        String trimmed = importDir.trim();
        if (trimmed.isEmpty()) return null;
        String normalized = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        int lastSlash = normalized.lastIndexOf('/');
        String finalSegment = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        if (!"ImportFiles".equals(finalSegment)) return null;
        String parent = lastSlash >= 0 ? normalized.substring(0, lastSlash + 1) : "";
        return parent + "ExportFiles";
    }

    // ── Parse ─────────────────────────────────────────────────────────

    private static final class MissingHeaderException extends RuntimeException {
        MissingHeaderException(String message) { super(message); }
    }

    /** opencsv {@code CSVReaderBuilder}, header indexed by trimmed name, each row narrowed at
     *  once to a {@link HistoryRow} holding only the columns this check needs. */
    private List<HistoryRow> parse(String content) throws Exception {
        List<String[]> lines = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8)).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            return List.of();
        }

        String[] header = lines.get(0);
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            index.put(header[i] == null ? "" : header[i].trim(), i);
        }

        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_HEADERS) {
            if (!index.containsKey(required)) missing.add(required);
        }
        if (!missing.isEmpty()) {
            throw new MissingHeaderException("Export is missing required header(s): " + String.join(", ", missing));
        }

        List<HistoryRow> rows = new ArrayList<>();
        for (int r = 1; r < lines.size(); r++) {
            String[] raw = lines.get(r);
            rows.add(new HistoryRow(
                    cellOf(raw, index, H_PARTICIPANT_ID),
                    cellOf(raw, index, H_PARTICIPANT_PLAN_ID),
                    cellOf(raw, index, H_EMPLOYER_ID),
                    cellOf(raw, index, H_PLAN_NAME),
                    cellOf(raw, index, H_PLAN_TYPE_ID),
                    cellOf(raw, index, H_TRANSACTION_TYPE),
                    cellOf(raw, index, H_SYSTEM_DATE),
                    cellOf(raw, index, H_TRANSACTION_AMT)));
        }
        return rows;
    }

    private static String cellOf(String[] raw, Map<String, Integer> index, String header) {
        Integer i = index.get(header);
        if (i == null || i >= raw.length || raw[i] == null) return "";
        return raw[i].trim();
    }

    // ── Evaluate ──────────────────────────────────────────────────────

    /** Filters to configured plan types (employer scope is Summit-side — no employer filter
     *  here), groups the evaluation month's rows by purse
     *  and applies the predicate. Rows whose {@code TransactionType} is in neither list are
     *  ignored and their distinct type values counted; rows whose {@code SystemDate} or amount cannot be
     *  parsed are counted separately and ignored.
     *  <p>
     *  Scope, date-parse and type-match are counted over the <b>whole file</b>, not just the
     *  evaluation month, so {@link #totalFailureOf} can tell "the export/config is wrong" apart
     *  from "this month genuinely has no rows". Every cell was trimmed by {@code cellOf} at parse
     *  time; the configured lists were trimmed (and, for types, case-folded) by {@code csvSet}. */
    private static Evaluation evaluate(List<HistoryRow> rows, Config config) {
        Map<String, Purse> purses = new LinkedHashMap<>();
        // Keyed by the lower-cased, case-folded value used for matching (dedupe key); the value
        // is the first-seen ORIGINAL-CASE text from the file, so it can be pasted straight into
        // ssa.properties — csvSet lower-cases the configured lists, so a re-typed lower-cased
        // value would defeat that purpose.
        Map<String, String> ignoredTypes = new LinkedHashMap<>();
        int scopedRows = 0;
        int dateParsedRows = 0;
        int typeMatchedRows = 0;
        int inMonthRows = 0;
        int unparsableRows = 0;

        for (HistoryRow row : rows) {
            if (!config.planTypeIds.contains(row.planTypeId)) continue;
            scopedRows++;

            YearMonth rowMonth = monthOf(row.systemDate);
            if (rowMonth == null) {
                unparsableRows++;
                continue;
            }
            dateParsedRows++;

            String type = row.transactionType.toLowerCase(Locale.ROOT);
            boolean isContribution = config.contributionTypes.contains(type);
            boolean isDisbursement = config.disbursementTypes.contains(type);
            if (!isContribution && !isDisbursement) {
                ignoredTypes.putIfAbsent(type, row.transactionType);
                continue;
            }
            typeMatchedRows++;

            if (!rowMonth.equals(config.evaluationMonth)) continue;
            inMonthRows++;

            BigDecimal amount = parseAmount(row.transactionAmt);
            if (amount == null) {
                unparsableRows++;
                continue;
            }
            amount = amount.abs();

            String purseKey = row.participantId + "|" + row.participantPlanId;
            Purse purse = purses.computeIfAbsent(purseKey, k ->
                    new Purse(row.participantId, row.participantPlanId, row.employerId, row.planName));
            if (isContribution) {
                purse.contribution = purse.contribution.add(amount);
            } else {
                purse.disbursement = purse.disbursement.add(amount);
            }
        }

        List<Purse> findings = new ArrayList<>();
        for (Purse purse : purses.values()) {
            if (purse.contribution.signum() > 0 && purse.disbursement.signum() == 0) {
                findings.add(purse);
            }
        }
        return new Evaluation(findings, ignoredTypes, rows.size(), scopedRows, dateParsedRows,
                typeMatchedRows, inMonthRows, unparsableRows);
    }

    /**
     * Total-failure guard: a result that reads "all clear" after evaluating nothing is a false
     * negative that will be trusted, so it is {@code ERROR}, never {@code OK}. Exactly four
     * conditions, checked in order, each absolute — partial failures stay in the summary line and
     * never change the status. Returns {@code null} when the evaluation actually evaluated
     * something.
     * <p>
     * The fourth condition (rows scoped, dated and typed, but none in the evaluation month) exists
     * because an export configured month-to-date while this check evaluates the prior closed month
     * would otherwise report {@code OK} on every run, permanently, with the zero count visible only
     * in a summary line nobody reads on a check that says it is fine. A genuinely dormant or
     * terminated group producing this error is cheap — it is dropped from the employer list. A
     * misaligned window producing {@code OK} is a premium monitor that never fires and is trusted.
     */
    private static AuditResult totalFailureOf(Evaluation evaluation, Config config, String fileName) {
        if (evaluation.scopedRows == 0) {
            return AuditResult.error("Nothing evaluated: 0 of " + evaluation.totalRows
                    + " row(s) in " + fileName + " matched the configured plan-type scope ("
                    + config.planTypeIds.size() + " " + H_PLAN_TYPE_ID + " value(s)). Likely cause: "
                    + CFG_PREFIX + " names the wrong export template, or " + CFG_PLAN_TYPE_IDS
                    + " carries " + H_PLAN_TYPE_ID + " values not present in the file.");
        }
        if (evaluation.dateParsedRows == 0) {
            return AuditResult.error("Nothing evaluated: " + evaluation.scopedRows
                    + " row(s) matched scope but none carried a parsable " + H_SYSTEM_DATE
                    + " value. Likely cause: unrecognized date format in the " + H_SYSTEM_DATE
                    + " column of " + fileName + ".");
        }
        if (evaluation.typeMatchedRows == 0) {
            return AuditResult.error("Nothing evaluated: " + evaluation.dateParsedRows
                    + " row(s) matched scope with a parsable " + H_SYSTEM_DATE
                    + ", but none carried a " + H_TRANSACTION_TYPE + " in either configured list ("
                    + evaluation.ignoredTypes.size() + " distinct value(s) seen"
                    + namedIgnoredTypesForMessage(evaluation) + "). Likely cause: "
                    + CFG_CONTRIBUTION_TYPES + " / " + CFG_DISBURSEMENT_TYPES
                    + " do not match the file's vocabulary.");
        }
        if (evaluation.inMonthRows == 0) {
            return AuditResult.error("Nothing evaluated: " + evaluation.typeMatchedRows
                    + " row(s) matched scope, date and type, but 0 fall in " + config.evaluationMonth
                    + " (" + H_SYSTEM_DATE + "). Likely cause: the Summit export's date window does"
                    + " not cover the evaluation month (e.g. a month-to-date export), or "
                    + CFG_MONTH_OFFSET + " is set wrong.");
        }
        return null;
    }

    /** Maximum distinct {@code TransactionType} values named inline in the type-list total-failure
     *  message — capped (unlike the detail page's fuller list) so {@code audit_run.error} stays
     *  well inside its 500-character truncation regardless of how many distinct values a file
     *  carries. */
    private static final int MAX_NAMED_TYPES_IN_MESSAGE = 5;

    /** {@code ": TypeA, TypeB"} (original case, first {@link #MAX_NAMED_TYPES_IN_MESSAGE} values
     *  in first-seen order) for the type-list total-failure message, plus a remainder count when
     *  there are more — {@code ", and N more"}. Empty string when nothing was ignored. */
    private static String namedIgnoredTypesForMessage(Evaluation evaluation) {
        if (evaluation.ignoredTypes.isEmpty()) return "";
        List<String> values = new ArrayList<>(evaluation.ignoredTypes.values());
        int shown = Math.min(values.size(), MAX_NAMED_TYPES_IN_MESSAGE);
        StringBuilder sb = new StringBuilder(": ");
        for (int i = 0; i < shown; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values.get(i));
        }
        int remaining = values.size() - shown;
        if (remaining > 0) sb.append(", and ").append(remaining).append(" more");
        return sb.toString();
    }

    /** Reads only the leading date token (e.g. {@code 8/1/2026} from {@code 8/1/2026 12:00:00 AM},
     *  or {@code 2026-08-01} from {@code 2026-08-01 00:00:00.000} / {@code 2026-08-01T00:00:00}). */
    static YearMonth monthOf(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String token = raw.trim();
        int cut = token.indexOf(' ');
        if (cut < 0) cut = token.indexOf('T');
        if (cut > 0) token = token.substring(0, cut);
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return YearMonth.from(LocalDate.parse(token, fmt));
            } catch (DateTimeParseException ignored) {
                // try the next shape
            }
        }
        return null;
    }

    /** Accepts {@code 123.45}, {@code -123.45}, {@code $1,234.56}, {@code (123.45)}. Sign is
     *  discarded by the caller — direction comes only from the configured type lists. */
    static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().replace("$", "").replace(",", "");
        if (s.startsWith("(") && s.endsWith(")")) {
            s = "-" + s.substring(1, s.length() - 1);
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String summaryOf(Evaluation evaluation, Config config, String fileName) {
        // Distinct-employer count is informational only (the export may carry several groups);
        // Employer_ID is an optional header, so blank values are simply not counted.
        Set<String> employers = new LinkedHashSet<>();
        for (Purse purse : evaluation.findings) {
            if (!purse.employerId.isBlank()) employers.add(purse.employerId);
        }
        StringBuilder sb = new StringBuilder()
                .append(evaluation.findings.size()).append(" purse(s)");
        if (!employers.isEmpty()) {
            sb.append(" at ").append(employers.size()).append(" employer(s)");
        }
        sb.append(" for ").append(config.evaluationMonth).append(" (")
                .append(evaluation.inMonthRows).append(" row(s) in month)");
        sb.append(" — ").append(evaluation.ignoredTypes.size()).append(" unmapped TransactionType value(s) ignored");
        if (evaluation.unparsableRows > 0) {
            sb.append(", ").append(evaluation.unparsableRows).append(" row(s) with unparsable date/amount skipped");
        }
        sb.append(" — export ").append(fileName);
        return sb.toString();
    }

    private static String scrub(Exception e) {
        String message = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
        return message.length() > 400 ? message.substring(0, 400) : message;
    }

    // ── Value types ──────────────────────────────────────────────────

    /** Internal, one export row narrowed to the columns this check reads — never returned
     *  outside this class. */
    private record HistoryRow(String participantId, String participantPlanId, String employerId,
                              String planName, String planTypeId, String transactionType,
                              String systemDate, String transactionAmt) {
    }

    /** Internal accumulator, one per {@code (Participant_ID, ParticipantPlan_ID)}. */
    private static final class Purse {
        final String participantId;
        final String participantPlanId;
        final String employerId;
        final String planName;
        BigDecimal contribution = BigDecimal.ZERO;
        BigDecimal disbursement = BigDecimal.ZERO;

        Purse(String participantId, String participantPlanId, String employerId, String planName) {
            this.participantId = participantId;
            this.participantPlanId = participantPlanId;
            this.employerId = employerId;
            this.planName = planName;
        }
    }

    /** Findings plus the whole-file funnel counts {@link #totalFailureOf} reads:
     *  {@code totalRows} ⊇ {@code scopedRows} ⊇ {@code dateParsedRows} ⊇ {@code typeMatchedRows}
     *  ⊇ {@code inMonthRows}. {@code ignoredTypes} maps the case-folded matching key to the
     *  first-seen original-case text — see the field's construction site in {@link #evaluate}. */
    private record Evaluation(List<Purse> findings, Map<String, String> ignoredTypes, int totalRows,
                              int scopedRows, int dateParsedRows, int typeMatchedRows, int inMonthRows,
                              int unparsableRows) {
    }

    private record Loaded(AuditResult errorResult, String fileName, LocalDateTime fileTimestamp,
                          List<HistoryRow> rows) {
        static Loaded failed(AuditResult errorResult) {
            return new Loaded(errorResult, null, null, List.of());
        }

        static Loaded ok(String fileName, LocalDateTime fileTimestamp, List<HistoryRow> rows) {
            return new Loaded(null, fileName, fileTimestamp, rows);
        }
    }

    /** One finding, narrowed to exactly what the detail page renders. Amounts are plain
     *  decimal strings so the JSP never formats a {@code BigDecimal}. */
    public record Row(String participantId, String participantPlanId, String planName,
                      String contributionTotal, String disbursementTotal) {
    }

    /** The detail page's whole view model. {@code error} is set (with empty {@code findings})
     *  when the check is not configured or the export could not be loaded.
     *  {@code ignoredTypeValues} is the full (unlimited, original-case, first-seen order) list
     *  behind {@code ignoredTypeCount} — the JSP truncates it for display. */
    public record Snapshot(String fileName, LocalDateTime fileTimestamp, YearMonth evaluationMonth,
                           List<Row> findings, int ignoredTypeCount, List<String> ignoredTypeValues,
                           String error) {
        static Snapshot failed(String error) {
            return new Snapshot(null, null, null, List.of(), 0, List.of(), error);
        }
    }
}

package net.superiorstate.ams.data.service.audit;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.AuditDeclineEmployerDAO;
import net.superiorstate.ams.data.dao.AuditFindingAckDAO;
import net.superiorstate.ams.data.service.SummitSftpService;
import net.superiorstate.ams.model.market.AuditFindingAck;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T237 third check: card declines in a rolling recent window, one finding per distinct
 * {@code Participant System ID}. This is the complement to {@link FundedPurseNoDisbursementCheck}
 * — that check catches the <i>silent</i> failure (money funded, nothing ever attempted); this one
 * catches the <i>loud</i> one (an attempt that Summit rejected). Neither sees the other's cases.
 * On a PremiumPath card, a block or a funds shortfall means a premium draft failed and coverage
 * may lapse; this check surfaces that daily rather than waiting for the silent-purse check's next
 * monthly cycle.
 * <p>
 * Reads the newest matching Summit <b>Transaction</b> export in the {@code ExportFiles} directory
 * sibling to {@code SUMMIT_SFTP_IMPORT_DIR}, over the same {@code SummitSftpService} transport and
 * the same staleness / byte-cap rules as the other two checks. Columns are resolved <b>by header
 * name, never by position</b>; every cell is trimmed before use — the sibling Plan History export
 * carries trailing whitespace on {@code TransactionType}, so this export's {@code Decline Reason}
 * is assumed capable of the same and is compared trimmed throughout.
 * <p>
 * <b>{@code Decline Reason} is load-bearing on the Summit side, not just in this class.</b> Without
 * that column in the export template's field set, Summit emits no decline rows at all — the export
 * would look identical to one with a clean card program. This check cannot detect that
 * misconfiguration; only a human comparing the export to the template catches it.
 * <p>
 * <b>Two constraints this export creates:</b>
 * <ol>
 *   <li><b>No plan-type scoping is possible — employer designation is the only available
 *       narrowing.</b> {@code Plan Type Code} is blank on every decline row observed (0 of 958 in
 *       the verification sample), so declines cannot be scoped by purse and a premium decline is
 *       indistinguishable from an FSA one in the file — unlike {@link FundedPurseNoDisbursementCheck},
 *       this check has no plan-type filter to fall back on. The employer is the only dimension
 *       the export and AMS agree on, so <b>this check filters to designated employers</b>: a
 *       decline row is kept only when its {@code Employer SystemID} matches the {@code altId} of
 *       an employer in {@code audit_decline_employer} (V114) for the check's PSP, maintained
 *       through {@code AuditDeclineEmployerAdmin}. This is <i>data, not configuration</i> — there
 *       is no config key for it — and it is opt-in per employer, set during plan setup. <b>Zero
 *       designated employers is {@code NOT_CONFIGURED}, not {@code OK}</b>: an opt-in feature with
 *       nothing opted in has not been configured, and {@code OK} would claim all-clear on an
 *       unmonitored book. (This supersedes the Summit-side-only scoping the check shipped with;
 *       the export template's own Employer selector still applies upstream, but the check no
 *       longer relies on it.)
 *       <p>
 *       <b>Identity, settled 2026-09-15 by comparing real exports — treat as fact:</b>
 *       {@code Employer SystemID} (this export) = {@code Employer_ID} (Plan History) = Summit
 *       {@code EmployerID} = {@code Employer.altId} ({@code employer.employer_id}), <b>not</b>
 *       {@code Employer.id} ({@code organization_id}). Evidence: the Plan History sample carries
 *       {@code Employer_ID} 1100 and {@code Organization_ID} 1102 as different values; this
 *       export's {@code Employer SystemID} set contains 1100 and not 1102. Matched as trimmed
 *       strings against the {@code altId} values rendered as strings — the export column is
 *       text.</li>
 *   <li><b>MCC is the only merchant signal.</b> {@code Merchant Name} is also blank on every
 *       decline (0 of 958), so the MCC summary table is by code only — there is no merchant name to
 *       show alongside it.</li>
 * </ol>
 * The other five columns confirmed always blank on a decline row —
 * {@code Claim Denied Reason}, {@code Card Transaction Status}, {@code Amount Paid},
 * {@code Denied Amount}, plus {@code Plan Type Code} and {@code Merchant Name} above — are
 * <b>deliberately not read</b>: not required headers, not in {@link DeclineRow}, not on the detail
 * page. Do not add a plan-type filter or a merchant-name column later; the data to support either
 * is not there.
 * <p>
 * <b>Window, not a closed month.</b> {@link FundedPurseNoDisbursementCheck}'s calendar-month logic
 * does not transfer: a decline is an operational alert, not an accounting-period reconciliation. A
 * row is kept when its {@code Transaction Date} falls within the last {@code windowDays} days,
 * inclusive of today.
 * <p>
 * <b>Findings can be acknowledged — {@code audit_finding_ack} (V115), generic across every check
 * but wired here only.</b> {@code HANDLED} suppresses a participant's current finding only while
 * its decline count and most-recent-decline date have not moved past what was recorded at
 * acknowledgment time ({@link #splitByAcknowledgment}) — new activity past that point re-surfaces
 * it, deliberately: a {@code HANDLED} acknowledgment that swallowed a fresh decline on a
 * premium-bearing card would be exactly the silent-failure shape this check exists to prevent.
 * {@code IGNORED} suppresses unconditionally until the row is removed. {@code findingCount} counts
 * only surfaced findings — an acknowledgment is meant to move the hub count and the navbar badge,
 * or it does nothing visible. <b>Out of scope, deliberately: acknowledging {@code ERROR} or
 * {@code NOT_CONFIGURED}.</b> Those mean the check could not evaluate at all (missing export,
 * unmapped vocabulary, no designated employers); acknowledgment only ever applies to a resolved
 * per-participant finding, never to the check being unable to run, so neither
 * {@link #evaluate(EntityManager, Long)} nor {@link #readLive} consults
 * {@code AuditFindingAckDAO} on any path that returns before a successful parse.
 * <p>
 * <b>The MCC summary is computed over every decline row that passed the designation and window
 * filters, before acknowledgment is even loaded — acknowledged participants are not excluded from
 * it.</b> It is a card-configuration diagnostic (which MCC to consider enabling), not a
 * per-participant finding; suppressing a participant who happens to have an unqualified-merchant
 * decline must not distort what the diagnostic reports about that MCC.
 * <p>
 * <b>Finding unit is the participant, not the row.</b> A card program can produce thousands of
 * decline rows in a window; {@code findingCount} is the number of distinct
 * {@code Participant System ID} values with at least one decline in the window, not the row count
 * (the verification sample: 958 decline rows, 59+63+57 participants across three reason
 * categories). The row count, distinct employer count, and distinct {@code Decline Reason} count
 * are carried in the summary line instead, so they are visible without inflating the badge.
 * <p>
 * <b>{@code OK} on zero declines is deliberate — this is NOT the same rule as
 * {@link FundedPurseNoDisbursementCheck}'s fourth total-failure condition.</b> There, an empty
 * evaluation month after rows were scoped, dated and typed means the export window is misaligned
 * with the evaluation period — a configuration failure. Here, the window is always "now," so an
 * empty window after the export loads and parses cleanly means the card program produced zero
 * declines, which is the good outcome. Do not "fix" this into a fourth condition that fires
 * {@code ERROR} on an empty window; that would page someone every day a card program is healthy.
 * {@code ERROR} is reserved for the export itself being unusable: not found, stale, empty of data
 * rows, or every row's {@code Transaction Date} failing to parse. A missing {@code Decline Reason}
 * column is already caught by required-header validation — the real configuration error this check
 * is exposed to.
 * <p>
 * <b>Configuration</b> — all via {@code AppConfig.get} ({@code ssa.properties}); no decline-reason
 * string is hardcoded beyond the Merchant-Not-Qualified default below, because Summit's reason
 * vocabulary is free text:
 * <ul>
 *   <li>{@code SUMMIT_AUDIT_DECLINE_EXPORT_PREFIX} — filename prefix (Summit template name) of the
 *       Transaction export. <b>Required.</b></li>
 *   <li>{@code SUMMIT_AUDIT_DECLINE_WINDOW_DAYS} — rolling window size; default 14.</li>
 *   <li>{@code SUMMIT_AUDIT_DECLINE_MNQ_REASON} — the {@code Decline Reason} value driving the MCC
 *       summary table; default {@code Merchant Not Qualified}. Compared trimmed, case-insensitive.</li>
 *   <li>{@code SUMMIT_AUDIT_DECLINE_MAX_AGE_HOURS} — staleness threshold; default 36.</li>
 *   <li>{@code SUMMIT_AUDIT_EXPORT_MAX_BYTES} — reused from the other checks; default 16 MiB.</li>
 * </ul>
 * ⚠️ <b>Counts only persist.</b> {@link #evaluate} never returns row content; {@link #readLive}
 * exposes the finding rows for the detail page's one in-request render and nothing else. The
 * export carries no participant name or SSN. {@link #readLive} — and only {@code readLive} —
 * resolves names for the detail page from AMS rows already in hand, in-request, and persists
 * nothing (LA-40, LA-43): the employer name comes off the designated-employer join above, and the
 * participant name from {@code Employee.firstName}/{@code lastName} keyed
 * {@code Employee.id} = {@code Participant System ID}. No SSN is read from anywhere.
 * <p>
 * <b>Participant identity, settled 2026-09-15 by comparing real exports — treat as fact:</b>
 * {@code Participant System ID} (this export) = {@code Participant_ID} (Plan History) =
 * {@code Employee.id} ({@code employee.employee_id}). Nine participants overlap between the two
 * exports on those columns, while {@code Participant Custom ID} has zero overlap with
 * {@code Participant_ID} — separate namespaces. {@code Participant Custom ID} is therefore
 * <b>not</b> a join key and is never used for name resolution.
 * <p>
 * <b>A missing name degrades to the bare id — a transient gap, not an error.</b> {@code Employee}
 * rows are kept current by the J2 refresh (a daily service mirroring {@code SummitRefreshService}'s
 * J1 handling; planned, and this check is built against that expectation without depending on
 * it — today only the manual {@code SummitImportWizard} J2/J3 path populates the table). The bare-id
 * fallback exists for the window between a participant appearing in Summit and the next J2
 * refresh, and for a newly designated employer before its first refresh — not as a steady state.
 * {@code employer_participant} is deliberately not consulted: it carries no Summit id and would
 * need {@code Participant Custom ID} derivation for marginal coverage.
 * <p>
 * <b>Filename matching is exact, unlike the other two checks.</b>
 * {@code IchraUncodedParticipantsCheck} and {@link FundedPurseNoDisbursementCheck} both use
 * {@code startsWith(prefix)}, which lets a test template beat a production one when both share a
 * stem (e.g. {@code ZZ_PARTICIPANT_HISTORY_AUDIT} vs. {@code ZZ_PARTICIPANT_HISTORY_AUDIT_DEL}).
 * This check requires the literal token {@code prefix + "_Export_"} immediately after the prefix —
 * see {@link #newestMatchingPattern}. The other two checks' patterns are intentionally left alone;
 * nothing is shared between any of the three.
 */
public class CardDeclineCheck implements AuditCheck {

    public static final String KEY = "card_declines";

    static final String CFG_PREFIX = "SUMMIT_AUDIT_DECLINE_EXPORT_PREFIX";
    static final String CFG_WINDOW_DAYS = "SUMMIT_AUDIT_DECLINE_WINDOW_DAYS";
    static final String CFG_MNQ_REASON = "SUMMIT_AUDIT_DECLINE_MNQ_REASON";
    static final String CFG_MAX_AGE_HOURS = "SUMMIT_AUDIT_DECLINE_MAX_AGE_HOURS";
    /** Reused from the other checks — same landing directory, same cap. */
    static final String CFG_MAX_BYTES = "SUMMIT_AUDIT_EXPORT_MAX_BYTES";

    private static final int DEFAULT_WINDOW_DAYS = 14;
    private static final String DEFAULT_MNQ_REASON = "Merchant Not Qualified";
    private static final long DEFAULT_MAX_AGE_HOURS = 36;
    private static final long DEFAULT_MAX_BYTES = 16_777_216L;

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** For the Acknowledged section's "who and when" — same shape as the detail servlet's own
     *  export-timestamp display, kept local here since it's this class that builds {@link AckRow}. */
    private static final DateTimeFormatter ACK_TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Header names, verbatim from the real Transaction export header row (the contract).
    static final String H_EMPLOYER_CUSTOM_ID = "Employer Custom ID";
    static final String H_EMPLOYER_SYSTEM_ID = "Employer SystemID";
    static final String H_MCC = "MCC";
    static final String H_PARTICIPANT_CUSTOM_ID = "Participant Custom ID";
    static final String H_PARTICIPANT_SYSTEM_ID = "Participant System ID";
    static final String H_TOTAL_TRANSACTION_AMOUNT = "Total Transaction Amount";
    static final String H_TRANSACTION_DATE = "Transaction Date";
    static final String H_DECLINE_REASON = "Decline Reason";
    // Deliberately not read: Claim Denied Reason, Plan Type Code, Card Transaction Status,
    // Amount Paid, Merchant Name, Denied Amount — confirmed 0/958 populated on a decline row
    // (see the class Javadoc). Do not add them to REQUIRED_HEADERS or DeclineRow.

    private static final List<String> REQUIRED_HEADERS = List.of(
            H_EMPLOYER_SYSTEM_ID, H_MCC, H_PARTICIPANT_SYSTEM_ID,
            H_TOTAL_TRANSACTION_AMOUNT, H_TRANSACTION_DATE, H_DECLINE_REASON);

    /** Same leading-token date shapes as {@link FundedPurseNoDisbursementCheck#monthOf} —
     *  {@code M/d/yyyy h:mm:ss a} observed ({@code 9/14/2026 3:49:15 AM}); only the date token
     *  before the first space is parsed. */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M-d-yyyy"));

    @Override
    public String key() { return KEY; }

    @Override
    public String label() { return "Card declines"; }

    @Override
    public String detailPath() { return "/AuditCardDeclines"; }

    @Override
    public AuditResult evaluate(EntityManager em, Long pspId) {
        try {
            Config config = Config.load();
            if (config.notConfigured != null) return config.notConfigured;

            // Designated employers are read before the export is fetched: with nothing opted in
            // there is nothing to evaluate, and no reason to touch SFTP.
            Map<Integer, String> designated = AuditDeclineEmployerDAO.findDesignatedEmployersByAltId(em, pspId);
            if (designated.isEmpty()) return noDesignatedEmployers();

            Loaded loaded = loadLatest(config);
            if (loaded.errorResult != null) return loaded.errorResult;
            if (loaded.rows.isEmpty()) {
                return AuditResult.error("Export " + loaded.fileName + " contains no data rows.");
            }

            Evaluation evaluation = evaluate(loaded.rows, config, designatedAltIdStrings(designated));
            if (evaluation.dateParsedRows == 0) {
                return AuditResult.error("Every row in " + loaded.fileName + " (" + loaded.rows.size()
                        + " row(s)) has an unparsable " + H_TRANSACTION_DATE + " value.");
            }

            AckSplit split = splitByAcknowledgment(evaluation.findings,
                    ackMap(AuditFindingAckDAO.findAllByPspAndCheck(em, pspId, KEY)));

            String summary = summaryOf(evaluation, config, designated.size(), split, loaded.fileName);
            return split.surfaced.isEmpty()
                    ? AuditResult.ok(summary)
                    : AuditResult.action(split.surfaced.size(), summary);
        } catch (Exception e) {
            return AuditResult.error(scrub(e));
        }
    }

    /**
     * For the detail page — the same read path as {@link #evaluate}, never stored. The returned
     * rows are exactly the fields the detail page renders, including the employer and participant
     * names resolved here, in-request, from the supplied {@code em}.
     * <p>
     * Takes the caller's {@code EntityManager} and PSP id (the detail servlet supplies both, the
     * same way {@code AuditService} supplies them to {@link #evaluate}) rather than opening its own:
     * the designated-employer set and the {@code Employee} lookups are both DB reads, and the check
     * never owns a persistence context of its own.
     */
    public Snapshot readLive(EntityManager em, Long pspId) {
        try {
            Config config = Config.load();
            if (config.notConfigured != null) {
                return Snapshot.failed(config.notConfigured.error());
            }
            Map<Integer, String> designated = AuditDeclineEmployerDAO.findDesignatedEmployersByAltId(em, pspId);
            if (designated.isEmpty()) {
                return Snapshot.failed(noDesignatedEmployers().error());
            }
            Loaded loaded = loadLatest(config);
            if (loaded.errorResult != null) {
                return Snapshot.failed(loaded.errorResult.error());
            }
            if (loaded.rows.isEmpty()) {
                return Snapshot.failed("Export " + loaded.fileName + " contains no data rows.");
            }

            Evaluation evaluation = evaluate(loaded.rows, config, designatedAltIdStrings(designated));
            if (evaluation.dateParsedRows == 0) {
                return Snapshot.failed("Every row in " + loaded.fileName + " (" + loaded.rows.size()
                        + " row(s)) has an unparsable " + H_TRANSACTION_DATE + " value.");
            }

            AckSplit split = splitByAcknowledgment(evaluation.findings,
                    ackMap(AuditFindingAckDAO.findAllByPspAndCheck(em, pspId, KEY)));

            List<Row> rows = toRows(split.surfaced, designated, em);
            List<AckRow> acknowledged = toAckRows(split.suppressed, designated, em);

            // Unaffected by acknowledgment — see the class note. mnqByMcc was built from every
            // decline row that passed designation/window, before any participant-level ack split.
            List<McRow> mccSummary = new ArrayList<>();
            for (Map.Entry<String, McccAccumulator> e : evaluation.mnqByMcc.entrySet()) {
                mccSummary.add(new McRow(e.getKey(), e.getValue().count, e.getValue().participants.size()));
            }
            mccSummary.sort(Comparator.comparingInt(McRow::declineCount).reversed());

            String windowRangeDisplay = displayDate(config.windowStart) + " – " + displayDate(config.windowEnd);

            return new Snapshot(loaded.fileName, loaded.fileTimestamp, config.windowDays, windowRangeDisplay,
                    designated.size(), rows, split.suppressed.size(), acknowledged, mccSummary,
                    evaluation.totalDeclineRowsInWindow, evaluation.distinctEmployers.size(),
                    evaluation.distinctReasons.size(), null);
        } catch (Exception e) {
            return Snapshot.failed(scrub(e));
        }
    }

    /** The {@code NOT_CONFIGURED} result for a PSP with nothing designated — shared by both paths. */
    private static AuditResult noDesignatedEmployers() {
        return AuditResult.notConfigured("No employers are designated for card-decline monitoring."
                + " Designate at least one on the Card-Decline Employers page (AuditDeclineEmployerAdmin).");
    }

    /** The designated {@code altId}s as the trimmed strings the export column is compared against. */
    private static Set<String> designatedAltIdStrings(Map<Integer, String> designated) {
        Set<String> out = new LinkedHashSet<>();
        for (Integer altId : designated.keySet()) {
            out.add(String.valueOf(altId));
        }
        return out;
    }

    /**
     * Detail-page rows for a list of current findings: one per participant, names resolved
     * in-request, ordered by decline count descending. Takes the already-surfaced subset (see
     * {@link #splitByAcknowledgment}), not the raw evaluation — package-private so the synthetic
     * test can drive it with a stub {@code EntityManager}; nothing outside this class calls it
     * otherwise.
     */
    static List<Row> toRows(List<ParticipantDeclines> findings, Map<Integer, String> designated, EntityManager em) {
        List<Row> rows = new ArrayList<>(findings.size());
        for (ParticipantDeclines p : findings) {
            rows.add(new Row(p.participantSystemId, resolveParticipantName(em, p.participantSystemId),
                    p.participantCustomId, p.employerSystemId, resolveEmployerName(designated, p.employerSystemId),
                    p.declineCount(), reasonBreakdownOf(p), displayDate(p.mostRecentDeclineDate),
                    p.maxAmount == null ? "—" : p.maxAmount.toPlainString(), mccListOf(p)));
        }
        rows.sort(Comparator.comparingInt(Row::declineCount).reversed());
        return rows;
    }

    /**
     * Detail-page rows for the Acknowledged section: one per suppressed finding, pairing its
     * <b>current</b> activity (name, decline count, most-recent date — so it is visible how close a
     * {@code HANDLED} row is to re-surfacing) with what was recorded at acknowledgment time. Order
     * follows {@code suppressed}'s own order (most recently acknowledged first — see
     * {@code AuditFindingAckDAO#findAllByPspAndCheck}).
     */
    static List<AckRow> toAckRows(List<Acknowledged> suppressed, Map<Integer, String> designated, EntityManager em) {
        List<AckRow> rows = new ArrayList<>(suppressed.size());
        for (Acknowledged a : suppressed) {
            ParticipantDeclines p = a.finding;
            AuditFindingAck ack = a.ack;
            rows.add(new AckRow(ack.getId(), p.participantSystemId, resolveParticipantName(em, p.participantSystemId),
                    ack.getAckState(), ack.getNote() == null ? "" : ack.getNote(),
                    mostRecentActorDisplay(ack), mostRecentActionAtDisplay(ack),
                    ack.getObservedCount() == null ? "—" : String.valueOf(ack.getObservedCount()),
                    ack.getObservedThrough() == null ? "—" : displayDate(ack.getObservedThrough()),
                    p.declineCount(), displayDate(p.mostRecentDeclineDate)));
        }
        return rows;
    }

    /** Whoever most recently acted on the row — the updater if it has been touched since insert,
     *  otherwise whoever created it. */
    private static String mostRecentActorDisplay(AuditFindingAck ack) {
        String actor = ack.getUpdatedAt() != null ? ack.getUpdatedBy() : ack.getCreatedBy();
        return actor == null ? "—" : actor;
    }

    private static String mostRecentActionAtDisplay(AuditFindingAck ack) {
        LocalDateTime when = ack.getUpdatedAt() != null ? ack.getUpdatedAt() : ack.getCreatedAt();
        return when == null ? "—" : when.format(ACK_TIMESTAMP_FMT);
    }

    /**
     * Splits current findings into surfaced and suppressed against the PSP's acknowledgments for
     * this check, keyed by {@code Participant System ID}. {@code IGNORED} always suppresses.
     * {@code HANDLED} suppresses only while the finding has not moved past what was observed at
     * acknowledgment: current {@code declineCount() <= observedCount} <b>and</b> current
     * {@code mostRecentDeclineDate <= observedThrough}. A {@code HANDLED} row missing either
     * observed value (should not happen — the save path always sets both together) fails open to
     * "surfaced," never silently suppresses on incomplete data. A finding with no acknowledgment
     * at all always surfaces.
     */
    static AckSplit splitByAcknowledgment(List<ParticipantDeclines> findings, Map<String, AuditFindingAck> acks) {
        List<ParticipantDeclines> surfaced = new ArrayList<>();
        List<Acknowledged> suppressed = new ArrayList<>();
        for (ParticipantDeclines finding : findings) {
            AuditFindingAck ack = acks.get(finding.participantSystemId);
            if (ack == null) {
                surfaced.add(finding);
                continue;
            }
            if (AuditFindingAck.STATE_IGNORED.equals(ack.getAckState())) {
                suppressed.add(new Acknowledged(finding, ack));
                continue;
            }
            boolean countStillCovered = ack.getObservedCount() != null
                    && finding.declineCount() <= ack.getObservedCount();
            boolean dateStillCovered = ack.getObservedThrough() != null && finding.mostRecentDeclineDate != null
                    && !finding.mostRecentDeclineDate.isAfter(ack.getObservedThrough());
            if (countStillCovered && dateStillCovered) {
                suppressed.add(new Acknowledged(finding, ack));
            } else {
                surfaced.add(finding);
            }
        }
        return new AckSplit(surfaced, suppressed);
    }

    /** {@code acks}, keyed by {@code findingKey} (the unique constraint means at most one row per
     *  key already; this is a lookup convenience, not a dedupe). */
    private static Map<String, AuditFindingAck> ackMap(List<AuditFindingAck> acks) {
        Map<String, AuditFindingAck> byFindingKey = new LinkedHashMap<>();
        for (AuditFindingAck ack : acks) {
            byFindingKey.put(ack.getFindingKey(), ack);
        }
        return byFindingKey;
    }

    /** Employer name off the designated-employer join; {@code ""} when the id does not parse or is
     *  not designated (cannot happen for a row that survived the filter, but stays defensive). */
    private static String resolveEmployerName(Map<Integer, String> designated, String employerSystemId) {
        try {
            String name = designated.get(Integer.parseInt(employerSystemId.trim()));
            return name == null ? "" : name;
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /**
     * {@code Employee.firstName + " " + lastName} for {@code Employee.id} = {@code Participant
     * System ID}, or {@code ""} when no row exists yet (the J2-refresh window — see the class note)
     * or the id does not parse. Never throws; a lookup failure is a blank name, not an error.
     */
    private static String resolveParticipantName(EntityManager em, String participantSystemId) {
        try {
            Employee employee = em.find(Employee.class, Integer.parseInt(participantSystemId.trim()));
            if (employee == null) return "";
            String first = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
            String last = employee.getLastName() == null ? "" : employee.getLastName().trim();
            return (first + " " + last).trim();
        } catch (RuntimeException e) {
            return "";
        }
    }

    // ── Config ────────────────────────────────────────────────────────

    /** Everything read from {@code AppConfig}, resolved once per evaluation. {@code notConfigured}
     *  is set (and nothing else is trusted) when the required key is absent or blank. */
    private record Config(AuditResult notConfigured, String prefix, int windowDays, LocalDate windowStart,
                          LocalDate windowEnd, String mnqReason, long maxAgeHours, long maxBytes) {

        static Config load() {
            String prefix = AppConfig.get(CFG_PREFIX);
            if (prefix == null || prefix.isBlank()) {
                return new Config(AuditResult.notConfigured("Config key " + CFG_PREFIX + " is not set."),
                        null, 0, null, null, null, 0, 0);
            }

            int windowDays = (int) parseLongOrDefault(AppConfig.get(CFG_WINDOW_DAYS), DEFAULT_WINDOW_DAYS);
            if (windowDays < 1) windowDays = DEFAULT_WINDOW_DAYS;

            String mnqRaw = AppConfig.get(CFG_MNQ_REASON);
            String mnqReason = (mnqRaw == null || mnqRaw.isBlank()) ? DEFAULT_MNQ_REASON : mnqRaw.trim();

            long maxAgeHours = parseLongOrDefault(AppConfig.get(CFG_MAX_AGE_HOURS), DEFAULT_MAX_AGE_HOURS);
            long maxBytes = parseLongOrDefault(AppConfig.get(CFG_MAX_BYTES), DEFAULT_MAX_BYTES);

            LocalDate today = LocalDate.now();
            LocalDate windowStart = today.minusDays(windowDays - 1L);

            return new Config(null, prefix.trim(), windowDays, windowStart, today, mnqReason, maxAgeHours, maxBytes);
        }
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

        Pattern namePattern = newestMatchingPattern(config.prefix);

        String newestName = null;
        long newestTimestamp = -1;
        for (SummitSftpService.SftpEntry entry : entries) {
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (name == null) continue;
            Matcher m = namePattern.matcher(name);
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
                    "No export exactly matching template '" + config.prefix + "' found in " + exportDir + "."));
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

        List<DeclineRow> rows;
        try {
            rows = parse(content);
        } catch (MissingHeaderException e) {
            return Loaded.failed(AuditResult.error(e.getMessage()));
        } catch (Exception e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        return Loaded.ok(newestName, fileTimestamp, rows);
    }

    /** Exact-match filename pattern: {@code {prefix}_Export_{17-digit timestamp}} followed by zero
     *  or more underscore-delimited tokens, then any-case extension. Unlike the other two checks'
     *  {@code startsWith(prefix)}, this requires the literal {@code "_Export_"} token immediately
     *  after the prefix — {@code prefix + "_DEL_Export_..."} does not match, so a test template
     *  sharing a stem with a production template cannot silently win the newest-file race. Built
     *  per call (the prefix is runtime config, not a shared constant like the other checks'
     *  {@code TIMESTAMPED_NAME}). */
    static Pattern newestMatchingPattern(String prefix) {
        return Pattern.compile("^" + Pattern.quote(prefix) + "_Export_(\\d{17})(?:_[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$");
    }

    /** Sibling {@code ExportFiles} directory to {@code importDir} — identical rule to the other
     *  checks: the final segment must be exactly {@code ImportFiles}, otherwise {@code null}. */
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

    /** opencsv {@code CSVReaderBuilder}, header indexed by trimmed name, each row narrowed at once
     *  to a {@link DeclineRow} holding only the columns this check reads — the six confirmed-blank
     *  columns are never touched (see the class Javadoc). */
    private List<DeclineRow> parse(String content) throws Exception {
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

        List<DeclineRow> rows = new ArrayList<>();
        for (int r = 1; r < lines.size(); r++) {
            String[] raw = lines.get(r);
            rows.add(new DeclineRow(
                    cellOf(raw, index, H_EMPLOYER_CUSTOM_ID),
                    cellOf(raw, index, H_EMPLOYER_SYSTEM_ID),
                    cellOf(raw, index, H_MCC),
                    cellOf(raw, index, H_PARTICIPANT_CUSTOM_ID),
                    cellOf(raw, index, H_PARTICIPANT_SYSTEM_ID),
                    cellOf(raw, index, H_TOTAL_TRANSACTION_AMOUNT),
                    cellOf(raw, index, H_TRANSACTION_DATE),
                    cellOf(raw, index, H_DECLINE_REASON)));
        }
        return rows;
    }

    private static String cellOf(String[] raw, Map<String, Integer> index, String header) {
        Integer i = index.get(header);
        if (i == null || i >= raw.length || raw[i] == null) return "";
        return raw[i].trim();
    }

    // ── Evaluate ──────────────────────────────────────────────────────

    /** Groups declines within the window by {@code Participant System ID}. A row with a blank
     *  {@code Decline Reason} is not a decline and is excluded from every count except
     *  {@code totalRows}/{@code dateParsedRows}, which are computed over <b>every</b> row (decline
     *  or not) so {@link #evaluate(EntityManager, Long)} can tell "the export's dates are
     *  unparsable" apart from "there happen to be no declines."
     *  <p>
     *  {@code designatedAltIds} is the designated employers' {@code altId}s as strings; a decline
     *  row whose trimmed {@code Employer SystemID} is not in it is dropped before the window and
     *  counted in {@code undesignatedDeclineRows} so the summary line shows how much of the file
     *  the designation excluded. Every cell was already trimmed by {@code cellOf}. */
    static Evaluation evaluate(List<DeclineRow> rows, Config config, Set<String> designatedAltIds) {
        Map<String, ParticipantDeclines> participants = new LinkedHashMap<>();
        Map<String, McccAccumulator> mnqByMcc = new TreeMap<>();
        Set<String> distinctEmployers = new LinkedHashSet<>();
        Set<String> distinctReasons = new LinkedHashSet<>();
        int dateParsedRows = 0;
        int totalDeclineRowsInWindow = 0;
        int undesignatedDeclineRows = 0;

        for (DeclineRow row : rows) {
            LocalDate date = dateOf(row.transactionDate);
            if (date == null) continue;
            dateParsedRows++;

            if (row.declineReason.isEmpty()) continue; // not a decline row
            if (!designatedAltIds.contains(row.employerSystemId.trim())) {
                undesignatedDeclineRows++;
                continue;
            }
            if (date.isBefore(config.windowStart) || date.isAfter(config.windowEnd)) continue;

            totalDeclineRowsInWindow++;
            if (!row.employerSystemId.isEmpty()) distinctEmployers.add(row.employerSystemId);
            distinctReasons.add(row.declineReason);

            ParticipantDeclines p = participants.computeIfAbsent(row.participantSystemId,
                    k -> new ParticipantDeclines(row.participantSystemId));
            p.record(row, date);

            if (config.mnqReason.equalsIgnoreCase(row.declineReason) && !row.mcc.isEmpty()) {
                McccAccumulator acc = mnqByMcc.computeIfAbsent(row.mcc, k -> new McccAccumulator());
                acc.count++;
                acc.participants.add(row.participantSystemId);
            }
        }

        List<ParticipantDeclines> findings = new ArrayList<>(participants.values());
        return new Evaluation(findings, mnqByMcc, distinctEmployers, distinctReasons,
                dateParsedRows, totalDeclineRowsInWindow, undesignatedDeclineRows);
    }

    /** Reads only the leading date token (e.g. {@code 9/14/2026} from
     *  {@code 9/14/2026 3:49:15 AM}). */
    static LocalDate dateOf(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String token = raw.trim();
        int cut = token.indexOf(' ');
        if (cut < 0) cut = token.indexOf('T');
        if (cut > 0) token = token.substring(0, cut);
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(token, fmt);
            } catch (DateTimeParseException ignored) {
                // try the next shape
            }
        }
        return null;
    }

    /** Accepts {@code 123.45}, {@code -123.45}, {@code $1,234.56}, {@code (123.45)}. */
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

    private static String summaryOf(Evaluation evaluation, Config config, int designatedCount,
                                    AckSplit split, String fileName) {
        int handledCount = 0;
        int ignoredCount = 0;
        for (Acknowledged a : split.suppressed) {
            if (AuditFindingAck.STATE_IGNORED.equals(a.ack.getAckState())) ignoredCount++; else handledCount++;
        }
        return split.surfaced.size() + " participant(s) declined in the last " + config.windowDays
                + " day(s) (" + displayDate(config.windowStart) + " – " + displayDate(config.windowEnd) + ") — "
                + evaluation.totalDeclineRowsInWindow + " decline row(s) at " + evaluation.distinctEmployers.size()
                + " of " + designatedCount + " designated employer(s), " + evaluation.distinctReasons.size()
                + " distinct " + H_DECLINE_REASON + " value(s); " + evaluation.undesignatedDeclineRows
                + " decline row(s) at undesignated employers ignored; " + split.suppressed.size()
                + " finding(s) acknowledged (" + handledCount + " handled, " + ignoredCount
                + " ignored) — export " + fileName;
    }

    private static String reasonBreakdownOf(ParticipantDeclines p) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(p.reasonCounts.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : entries) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append(" ×").append(e.getValue());
        }
        return sb.toString();
    }

    private static String mccListOf(ParticipantDeclines p) {
        List<String> mccs = new ArrayList<>(p.mccs);
        mccs.sort(Comparator.naturalOrder());
        return String.join(", ", mccs);
    }

    private static String displayDate(LocalDate date) {
        return date == null ? "—" : date.toString();
    }

    private static String scrub(Exception e) {
        String message = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
        return message.length() > 400 ? message.substring(0, 400) : message;
    }

    // ── Value types ──────────────────────────────────────────────────

    /** Internal, one export row narrowed to the columns this check reads — never returned outside
     *  this class. */
    record DeclineRow(String employerCustomId, String employerSystemId, String mcc,
                              String participantCustomId, String participantSystemId,
                              String totalTransactionAmount, String transactionDate, String declineReason) {
    }

    /** Internal accumulator, one per distinct {@code Participant System ID}. */
    private static final class ParticipantDeclines {
        final String participantSystemId;
        String participantCustomId = "";
        String employerSystemId = "";
        int declineCount;
        final Map<String, Integer> reasonCounts = new LinkedHashMap<>();
        LocalDate mostRecentDeclineDate;
        BigDecimal maxAmount;
        final Set<String> mccs = new LinkedHashSet<>();

        ParticipantDeclines(String participantSystemId) {
            this.participantSystemId = participantSystemId;
        }

        int declineCount() { return declineCount; }

        void record(DeclineRow row, LocalDate date) {
            declineCount++;
            if (participantCustomId.isEmpty() && !row.participantCustomId.isEmpty()) {
                participantCustomId = row.participantCustomId;
            }
            if (employerSystemId.isEmpty() && !row.employerSystemId.isEmpty()) {
                employerSystemId = row.employerSystemId;
            }
            reasonCounts.merge(row.declineReason, 1, Integer::sum);
            if (mostRecentDeclineDate == null || date.isAfter(mostRecentDeclineDate)) {
                mostRecentDeclineDate = date;
            }
            BigDecimal amount = parseAmount(row.totalTransactionAmount);
            if (amount != null && (maxAmount == null || amount.compareTo(maxAmount) > 0)) {
                maxAmount = amount;
            }
            if (!row.mcc.isEmpty()) mccs.add(row.mcc);
        }
    }

    /** Internal accumulator for the MCC/unqualified-merchant summary. */
    private static final class McccAccumulator {
        int count;
        final Set<String> participants = new LinkedHashSet<>();
    }

    /** One current finding paired with the acknowledgment suppressing it. */
    record Acknowledged(ParticipantDeclines finding, AuditFindingAck ack) {
    }

    /** {@link #splitByAcknowledgment}'s result. */
    record AckSplit(List<ParticipantDeclines> surfaced, List<Acknowledged> suppressed) {
    }

    /** Findings plus the whole-file counts the status decision and summary line read. Package-private
     *  so the synthetic test can pass one to {@link #toRows}. */
    record Evaluation(List<ParticipantDeclines> findings, Map<String, McccAccumulator> mnqByMcc,
                      Set<String> distinctEmployers, Set<String> distinctReasons,
                      int dateParsedRows, int totalDeclineRowsInWindow, int undesignatedDeclineRows) {
    }

    private record Loaded(AuditResult errorResult, String fileName, LocalDateTime fileTimestamp,
                          List<DeclineRow> rows) {
        static Loaded failed(AuditResult errorResult) {
            return new Loaded(errorResult, null, null, List.of());
        }

        static Loaded ok(String fileName, LocalDateTime fileTimestamp, List<DeclineRow> rows) {
            return new Loaded(null, fileName, fileTimestamp, rows);
        }
    }

    /** One finding, narrowed to exactly what the detail page renders. {@code reasonBreakdown} and
     *  {@code mccList} are pre-formatted strings so the JSP never assembles them. */
    public record Row(String participantSystemId, String participantName, String participantCustomId,
                      String employerSystemId, String employerName, int declineCount, String reasonBreakdown,
                      String mostRecentDeclineDate, String maxAmount, String mccList) {
    }

    /** One row of the MCC / unqualified-merchant summary table. */
    public record McRow(String mcc, int declineCount, int participantCount) {
    }

    /** One row of the Acknowledged section: a suppressed finding's current activity alongside what
     *  was recorded when it was acknowledged. {@code observedCountDisplay}/{@code
     *  observedThroughDisplay} read {@code "—"} for {@code IGNORED} (neither is stored for that
     *  state). {@code ackedBy}/{@code ackedAtDisplay} name whoever most recently acted on the row. */
    public record AckRow(Long ackId, String participantSystemId, String participantName, String ackState,
                         String note, String ackedBy, String ackedAtDisplay, String observedCountDisplay,
                         String observedThroughDisplay, int currentDeclineCount, String currentMostRecentDeclineDisplay) {
    }

    /** The detail page's whole view model. {@code error} is set (with empty lists) when the check
     *  is not configured or the export could not be loaded or parsed. {@code findings} is the
     *  surfaced set; {@code acknowledged} the suppressed set with acknowledgment detail —
     *  {@code suppressedCount} is {@code acknowledged.size()}, carried separately so the header
     *  block never calls {@code fn:length} in the JSP for a number the check already has. */
    public record Snapshot(String fileName, LocalDateTime fileTimestamp, int windowDays,
                           String windowRangeDisplay, int designatedEmployerCount, List<Row> findings,
                           int suppressedCount, List<AckRow> acknowledged, List<McRow> mccSummary,
                           int totalDeclineRows, int distinctEmployerCount, int distinctReasonCount,
                           String error) {
        static Snapshot failed(String error) {
            return new Snapshot(null, null, 0, null, 0, List.of(), 0, List.of(), List.of(), 0, 0, 0, error);
        }
    }
}

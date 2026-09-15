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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T237 fourth check: card status problems off the Summit <b>Debit Card Participants</b> export.
 * This is the complement to {@link CardDeclineCheck} — that check sees a payment that already
 * failed; this one sees one that is about to. A card that is lost, on hold, permanently inactive,
 * or expired cannot pay a premium, and on a PremiumPath card that is a coverage lapse arriving on
 * a known date. <b>{@code Hold} status here is the same condition {@link CardDeclineCheck} reports
 * as a blocked-card decline</b> — cross-reference the two when triaging.
 * <p>
 * Reads the newest matching export in the {@code ExportFiles} directory sibling to
 * {@code SUMMIT_SFTP_IMPORT_DIR}, over the same {@code SummitSftpService} transport, staleness and
 * byte-cap rules as the other checks. Columns are resolved <b>by header name, never by
 * position</b>; every cell is trimmed before use.
 * <p>
 * <b>Employer designation is reused from {@link CardDeclineCheck}, deliberately — {@code
 * audit_decline_employer} (V114) is not re-scoped per check.</b> Designating a PremiumPath employer
 * enables both card audits at once rather than requiring two setup steps. Consequence: the table
 * name ({@code audit_decline_employer}) is narrower than its use, and there is no way to designate
 * an employer for one card check but not the other — a {@code check_key} column would be needed for
 * that, and is a follow-up, not this build. <b>Zero designated employers is {@code NOT_CONFIGURED}</b>,
 * matching {@link CardDeclineCheck} — an opt-in feature with nothing opted in has not been
 * configured, and {@code OK} would claim all-clear on an unmonitored book.
 * <p>
 * <b>Acknowledgment is reused from {@code audit_finding_ack} (V115),</b> already generic on
 * {@code check_key} + {@code finding_key} — wiring a second check to it costs no schema.
 * <p>
 * <b>⚠️ {@code Name} is not a name — never read it.</b> It carries the export's status-legend
 * constant string on every row ({@code "Issued ,Mailed,Active,Hold,..."}), a Summit export defect.
 * The header's presence invites reading it as the participant's name; it is not.
 * <p>
 * <b>⚠️ {@code IsExpired} is wrong — it is {@code 0} on every row, including rows whose {@code
 * ExpirationDate} has already passed.</b> The same is true of {@code IsCancelled}, {@code
 * IsReplaced} and {@code IsMissingEmail} — all {@code 0} on every row, unused. <b>Expiry is
 * computed from {@code ExpirationDate} alone, never from any {@code Is*} flag.</b> The {@code Is*}
 * columns are otherwise a one-hot encoding of {@code Status} carrying no extra information, and are
 * never read.
 * <p>
 * <b>{@code ParticipantCardStatusID} is the matching key</b> — numeric, immune to the casing and
 * trailing-space problems {@code Status} has (observed with trailing whitespace, e.g.
 * {@code "Issued "}). {@code Status} is display-only.
 * <p>
 * <b>{@code Requested} (id 7) is a dead end, not a pending state, and is out of scope here</b> —
 * excluded by the default active-status list so it can never drive an expiry finding (a past-dated
 * {@code ExpirationDate} on a card that was never issued is not a real expiry). It may still be
 * added to a problem-status list if ever wanted, but is not by default. Genuine fulfilment tracking
 * for newly requested/issued cards is a separate, later check (see the class-level "out of scope"
 * note below) — not built here.
 * <p>
 * <b>Employer key is {@code EmployerID} = Summit {@code EmployerID} = {@code Employer.altId}</b>,
 * the same identity {@link CardDeclineCheck} matches as {@code Employer SystemID}.
 * {@code EmployerOrganizationID} and {@code Organization_ID} are both <b>never read</b>:
 * {@code Organization_ID} is {@code 0} on every row, and {@code EmployerOrganizationID} is not a
 * fixed offset from {@code EmployerID} (observed offsets vary) so it cannot substitute for it and
 * nothing in this check needs it.
 * <p>
 * <b>Participant key is {@code ParticipantID} → {@code Employee.id}</b>, as established for
 * {@link CardDeclineCheck}. <b>Blank on every dependent-card row</b> ({@code UserTypeID = 7}), which
 * carry {@code DependentID} and a populated {@code UserID} instead — those rows are still counted
 * and rendered (dependent id + user id, no name), never dropped. Dependent names are not
 * resolvable; {@code Employee} holds participants only.
 * <p>
 * <b>What the check flags — two independent conditions, computed per card row:</b>
 * <ol>
 *   <li><b>Problem status</b> — {@code ParticipantCardStatusID} in {@code
 *       SUMMIT_AUDIT_CARD_PROBLEM_STATUS_IDS} (default {@code 5,4,6}: Lost/Stolen, Hold,
 *       Permanently Inactive).</li>
 *   <li><b>Expired or expiring</b> — {@code ExpirationDate} is on or before {@code today +
 *       SUMMIT_AUDIT_CARD_EXPIRY_WARN_DAYS}, <b>and</b> {@code ParticipantCardStatusID} is in
 *       {@code SUMMIT_AUDIT_CARD_ACTIVE_STATUS_IDS} (default {@code 3,2,1,4,11}: Active, Mailed,
 *       Issued, Hold, Reissued) — the gate that excludes {@code Requested}, whose {@code
 *       ExpirationDate} is frequently already past despite no card ever being issued.</li>
 * </ol>
 * <b>A row can satisfy both</b> (e.g. a {@code Hold} card that is also past its expiration date) —
 * it is one finding (one acknowledgment), reported in both detail-page sections. {@code LastFour}
 * is populated on 100% of card-bearing statuses and is the stronger card-exists signal, per the
 * later fulfilment-profiling pass over this same export — <b>deliberately not used as a second
 * expiry gate here</b>; the active-status list above is already consistent with it. A future
 * fulfilment-monitor build should reuse that fact rather than re-deriving it.
 * <p>
 * <b>Supersession — a flagged row is dropped, not just acknowledged, when it is superseded.</b> The
 * first real run (2026-09-15) flagged 29 problem-status cards, most from 2023–2024, several users
 * holding two or three flagged records apiece — the shape of a lost card that got replaced while
 * Summit kept the old record at {@code Lost/Stolen} forever. A row an admin can do nothing about is
 * noise that trains people to stop reading the hub, so a flagged row is excluded entirely — from
 * {@code findings}, {@code findingCount}, the badge, and the acknowledgment split — when the same
 * {@code UserID} holds at least one other <b>card-bearing</b> row (see {@link #cardBearingStatusIds}
 * — the active list <i>union</i> the problem list, so a newer {@code Hold} or {@code Lost/Stolen}
 * counts too, not only a newer usable card) with a strictly later card date. The second real run
 * verified all 16 usable-card supersessions row by row against the export, then found the same
 * pattern one step removed: 4 of the remaining 13 surfaced rows were an old {@code Lost/Stolen}
 * alongside a <i>newer</i> {@code Hold} for the same holder — the older record is exactly as dead as
 * the ones already excluded, it just hadn't been replaced by something usable yet. <b>The newest
 * card-bearing row for a holder is never itself superseded — nothing is later than it — so widening
 * the replacement set can never hide a real, current problem: it only stops re-reporting a holder's
 * history once their newest card has its own say.</b> See {@link #evaluate(List, Config, Set)} for
 * the full rule (including the usable-preferred replacement choice), {@link #usableStatusIds} and
 * {@link #cardBearingStatusIds} for the two derived status sets, and {@link #cardDateOf} for the
 * {@code IssuedDate}/{@code RequestedDate} fallback. Superseded rows are rendered, never silently
 * discarded — they are the evidence the hypothesis holds — in their own detail-page section,
 * collapsed by default.
 * <p>
 * <b>{@code OK} on zero findings is deliberate, the same rule {@link CardDeclineCheck} follows —
 * not {@link FundedPurseNoDisbursementCheck}'s fourth total-failure condition.</b> A forward-looking
 * expiry check that returns zero on a healthy card book is working, not broken. {@code ERROR} is
 * reserved for the export itself being unusable: not found, stale, empty of data rows, or every
 * row's {@code ExpirationDate} failing to parse (checked over <b>every</b> row in the file, not
 * only rows at designated employers — mirrors {@link CardDeclineCheck}'s {@code dateParsedRows}).
 * <p>
 * <b>Finding key: {@code {UserID}:{LastFour}:{ParticipantCardStatusID}}</b>, tolerating a blank
 * {@code LastFour} — {@code UserID} alone is not unique (9,452 distinct values across 9,558 rows,
 * so a user can hold more than one card record). <b>State the composition anywhere this key is
 * read back; a key whose shape changes later orphans acknowledgments.</b>
 * <p>
 * <b>Acknowledgment semantics reuse {@code observed_count}/{@code observed_through} for a status id
 * and a date, not a count and a "through" cutoff — read those two columns as the finding's observed
 * {@code ParticipantCardStatusID} and observed {@code ExpirationDate}, nothing more.</b> {@code
 * IGNORED} suppresses unconditionally until removed. {@code HANDLED} suppresses only while the
 * current expiration date exactly matches what was recorded at acknowledgment time
 * ({@link #splitByAcknowledgment}, via {@link Objects#equals} so a {@code null} — unparsable/blank
 * on a problem-status-only row that never reached the expiry test — compares as "unchanged" against
 * another {@code null}, and any transition into or out of {@code null} re-surfaces): a corrected
 * (earlier, or later) {@code ExpirationDate} re-surfaces, because it is a fact, not a cumulative
 * count. <b>A status change re-surfaces too, but structurally, not through this comparison</b> — the
 * status id is already embedded in {@link #findingKey}'s own composition, so a card moving from
 * {@code Hold} (4) to {@code Lost/Stolen} (5) produces a <i>different</i> finding key; the old
 * acknowledgment simply no longer matches anything and the new key surfaces unacknowledged, the same
 * "no acknowledgment for this key" path any brand-new finding takes. {@code observed_count} is
 * therefore stored for the Acknowledged section's own display (and compared defensively, though it
 * can never actually mismatch a found row — see {@link #splitByAcknowledgment}), not because the
 * status-change re-surfacing depends on it.
 * <p>
 * <b>Configuration</b> — all via {@code AppConfig.get} ({@code ssa.properties}):
 * <ul>
 *   <li>{@code SUMMIT_AUDIT_CARD_STATUS_EXPORT_PREFIX} — filename prefix (Summit template name) of
 *       the Debit Card Participants export. <b>Required.</b></li>
 *   <li>{@code SUMMIT_AUDIT_CARD_PROBLEM_STATUS_IDS} — comma-separated {@code
 *       ParticipantCardStatusID} values considered a problem status; default {@code 5,4,6}
 *       (5=Lost/Stolen, 4=Hold, 6=Permanently Inactive).</li>
 *   <li>{@code SUMMIT_AUDIT_CARD_ACTIVE_STATUS_IDS} — comma-separated {@code
 *       ParticipantCardStatusID} values where a card actually exists, gating the expiry test;
 *       default {@code 3,2,1,4,11} (3=Active, 2=Mailed, 1=Issued, 4=Hold, 11=Reissued).</li>
 *   <li>{@code SUMMIT_AUDIT_CARD_EXPIRY_WARN_DAYS} — expiring-soon horizon in days; default 60.</li>
 *   <li>{@code SUMMIT_AUDIT_CARD_MAX_AGE_HOURS} — staleness threshold; default 36.</li>
 *   <li>{@code SUMMIT_AUDIT_EXPORT_MAX_BYTES} — reused from the other checks; default 16 MiB.</li>
 * </ul>
 * Status id → label, for readable configuration (Summit's vocabulary, may gain values): 1=Issued,
 * 2=Mailed, 3=Active, 4=Hold, 5=Lost/Stolen, 6=Permanently Inactive, 7=Requested, 11=Reissued,
 * 13=Requested (Queued).
 * <p>
 * <b>Filename matching is exact</b>, the same {@code {prefix}_Export_{17-digit timestamp}} shape
 * {@link CardDeclineCheck} requires — <b>not</b> {@link FundedPurseNoDisbursementCheck}'s {@code
 * startsWith(prefix)}, which lets a test template beat a production one when both share a stem.
 * <p>
 * ⚠️ <b>Counts only persist.</b> {@link #evaluate} never returns row content; {@link #readLive}
 * exposes finding rows for the detail page's one in-request render and nothing else, resolving
 * names from AMS rows already in hand (LA-40, LA-43). <b>No SSN is read from anywhere.</b>
 * <p>
 * <b>Out of scope — the card fulfilment monitor</b> (initial-card mailing latency: {@code Issued}
 * rows with a blank {@code MailedDate}, aging {@code Requested} rows, etc.) is a different
 * operational owner and cadence from remediation and is <b>not built here</b>; see the audit
 * check's originating prompt for the profiling facts, recorded there so they are not rediscovered.
 */
public class CardStatusCheck implements AuditCheck {

    public static final String KEY = "card_status";

    static final String CFG_PREFIX = "SUMMIT_AUDIT_CARD_STATUS_EXPORT_PREFIX";
    static final String CFG_PROBLEM_STATUS_IDS = "SUMMIT_AUDIT_CARD_PROBLEM_STATUS_IDS";
    static final String CFG_ACTIVE_STATUS_IDS = "SUMMIT_AUDIT_CARD_ACTIVE_STATUS_IDS";
    static final String CFG_WARN_DAYS = "SUMMIT_AUDIT_CARD_EXPIRY_WARN_DAYS";
    static final String CFG_MAX_AGE_HOURS = "SUMMIT_AUDIT_CARD_MAX_AGE_HOURS";
    /** Reused from the other checks — same landing directory, same cap. */
    static final String CFG_MAX_BYTES = "SUMMIT_AUDIT_EXPORT_MAX_BYTES";

    private static final Set<Integer> DEFAULT_PROBLEM_STATUS_IDS = Set.of(5, 4, 6);
    private static final Set<Integer> DEFAULT_ACTIVE_STATUS_IDS = Set.of(3, 2, 1, 4, 11);
    private static final int DEFAULT_WARN_DAYS = 60;
    private static final long DEFAULT_MAX_AGE_HOURS = 36;
    private static final long DEFAULT_MAX_BYTES = 16_777_216L;

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** For the Acknowledged section's "who and when". */
    private static final DateTimeFormatter ACK_TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Header names, verbatim from the real Debit Card Participants export header row (the contract).
    static final String H_EMPLOYER_ID = "EmployerID";
    static final String H_EMPLOYER_NAME = "EmployerName";
    static final String H_USER_ID = "UserID";
    static final String H_PARTICIPANT_ID = "ParticipantID";
    static final String H_DEPENDENT_ID = "DependentID";
    static final String H_USER_TYPE_ID = "UserTypeID";
    static final String H_LAST_FOUR = "LastFour";
    static final String H_REQUESTED_DATE = "RequestedDate";
    static final String H_ISSUED_DATE = "IssuedDate";
    static final String H_MAILED_DATE = "MailedDate";
    static final String H_EXPIRATION_DATE = "ExpirationDate";
    static final String H_STATUS = "Status";
    static final String H_PARTICIPANT_CARD_STATUS_ID = "ParticipantCardStatusID";
    // Deliberately not read: Name, EmployerOrganizationID, Organization_ID, every Is* column,
    // IsByDivision, DivisionID, DivisionName, BIN, CardType — see the class Javadoc. Do not add
    // them to REQUIRED_HEADERS or CardRow.

    private static final List<String> REQUIRED_HEADERS = List.of(
            H_EMPLOYER_ID, H_USER_ID, H_PARTICIPANT_CARD_STATUS_ID, H_STATUS, H_EXPIRATION_DATE);

    /** Same leading-token date shapes the other checks use for a Summit export column. */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M-d-yyyy"));

    @Override
    public String key() { return KEY; }

    @Override
    public String label() { return "Card status"; }

    @Override
    public String detailPath() { return "/AuditCardStatus"; }

    @Override
    public AuditResult evaluate(EntityManager em, Long pspId) {
        try {
            Config config = Config.load();
            if (config.notConfigured != null) return config.notConfigured;

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
                        + " row(s)) has an unparsable " + H_EXPIRATION_DATE + " value.");
            }

            AckSplit split = splitByAcknowledgment(evaluation.findings,
                    ackMap(AuditFindingAckDAO.findAllByPspAndCheck(em, pspId, KEY)));

            String summary = summaryOf(evaluation, designated.size(), split, loaded.fileName);
            return split.surfaced.isEmpty()
                    ? AuditResult.ok(summary)
                    : AuditResult.action(split.surfaced.size(), summary);
        } catch (Exception e) {
            return AuditResult.error(scrub(e));
        }
    }

    /**
     * For the detail page — the same read path as {@link #evaluate}, never stored. See
     * {@link CardDeclineCheck#readLive} for why this takes the caller's {@code EntityManager} and
     * PSP id rather than opening its own.
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
                        + " row(s)) has an unparsable " + H_EXPIRATION_DATE + " value.");
            }

            AckSplit split = splitByAcknowledgment(evaluation.findings,
                    ackMap(AuditFindingAckDAO.findAllByPspAndCheck(em, pspId, KEY)));

            List<Row> problemRows = toRows(split.surfaced, designated, em, true);
            List<Row> expiryRows = toRows(split.surfaced, designated, em, false);
            List<AckRow> acknowledged = toAckRows(split.suppressed, designated, em);
            List<SupersededRow> supersededRows = toSupersededRows(evaluation.superseded, designated, em);
            int supersededByUsable = 0;
            int supersededByProblem = 0;
            for (SupersededFinding sf : evaluation.superseded) {
                if (sf.replacementIsUsable()) supersededByUsable++; else supersededByProblem++;
            }

            return new Snapshot(loaded.fileName, loaded.fileTimestamp, config.warnDays, designated.size(),
                    problemRows, expiryRows, split.suppressed.size(), acknowledged, supersededRows,
                    supersededByUsable, supersededByProblem, evaluation.matchedRows,
                    evaluation.distinctEmployers.size(), evaluation.undesignatedRows, null);
        } catch (Exception e) {
            return Snapshot.failed(scrub(e));
        }
    }

    /** The {@code NOT_CONFIGURED} result for a PSP with nothing designated — shared by both paths. */
    private static AuditResult noDesignatedEmployers() {
        return AuditResult.notConfigured("No employers are designated for card monitoring."
                + " Designate at least one on the Card-Decline Employers page (AuditDeclineEmployerAdmin) —"
                + " the same list drives both card audits.");
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
     * Detail-page rows for one section — {@code problemSection = true} for Problem status,
     * {@code false} for Expired or expiring. Takes the already-surfaced subset (see
     * {@link #splitByAcknowledgment}), not the raw evaluation. Package-private so the synthetic
     * test can drive it directly.
     */
    static List<Row> toRows(List<CardFinding> findings, Map<Integer, String> designated, EntityManager em,
                            boolean problemSection) {
        List<Row> rows = new ArrayList<>();
        for (CardFinding f : findings) {
            boolean include = problemSection ? f.problemStatus() : f.expiringOrExpired();
            if (!include) continue;
            rows.add(new Row(f.userId(), f.participantId(), resolveParticipantName(em, f.participantId()),
                    f.dependentId(), f.employerId(), resolveEmployerName(designated, f.employerId()),
                    f.lastFour(), f.status(), String.valueOf(f.participantCardStatusId()),
                    displayDate(f.expirationDate()), f.issuedDate(), f.mailedDate(), f.findingKey()));
        }
        if (problemSection) {
            rows.sort(Comparator.comparing(Row::employerName, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Row::userId));
        } else {
            // expiringOrExpired implies a non-null expirationDate, so displayDate() here is never
            // "-" for a row in this section — a plain string sort of the ISO yyyy-MM-dd shape is
            // also a correct chronological sort (soonest/most-overdue expiry first).
            rows.sort(Comparator.comparing(Row::expirationDateDisplay));
        }
        return rows;
    }

    /** Detail-page rows for the Superseded section — same columns as {@link #toRows}, minus
     *  acknowledge controls (a superseded row is not a finding and cannot be acknowledged), plus the
     *  replacement evidence. Every row shown here is "why this flagged-looking card is not actually a
     *  problem" — the whole point of the section — so nothing is filtered or split further.
     *  Package-private so the synthetic test can drive it directly. */
    static List<SupersededRow> toSupersededRows(List<SupersededFinding> superseded, Map<Integer, String> designated,
                                                EntityManager em) {
        List<SupersededRow> rows = new ArrayList<>(superseded.size());
        for (SupersededFinding f : superseded) {
            rows.add(new SupersededRow(f.userId(), f.participantId(), resolveParticipantName(em, f.participantId()),
                    f.dependentId(), f.employerId(), resolveEmployerName(designated, f.employerId()), f.lastFour(),
                    f.status(), String.valueOf(f.participantCardStatusId()), displayDate(f.expirationDate()),
                    f.issuedDate(), f.mailedDate(), f.replacementLastFour(), f.replacementStatus(),
                    displayDate(f.replacementCardDate())));
        }
        rows.sort(Comparator.comparing(SupersededRow::employerName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SupersededRow::userId));
        return rows;
    }

    /**
     * Detail-page rows for the Acknowledged section — one per suppressed finding, regardless of
     * which condition(s) it satisfies (see {@link #conditionsOf}). Order follows {@code
     * suppressed}'s own order (most recently acknowledged first — see
     * {@code AuditFindingAckDAO#findAllByPspAndCheck}).
     */
    static List<AckRow> toAckRows(List<Acknowledged> suppressed, Map<Integer, String> designated, EntityManager em) {
        List<AckRow> rows = new ArrayList<>(suppressed.size());
        for (Acknowledged a : suppressed) {
            CardFinding f = a.finding();
            AuditFindingAck ack = a.ack();
            rows.add(new AckRow(ack.getId(), f.userId(), resolveParticipantName(em, f.participantId()),
                    f.participantId(), f.dependentId(), f.employerId(), resolveEmployerName(designated, f.employerId()),
                    ack.getAckState(), ack.getNote() == null ? "" : ack.getNote(),
                    mostRecentActorDisplay(ack), mostRecentActionAtDisplay(ack),
                    ack.getObservedCount() == null ? "—" : String.valueOf(ack.getObservedCount()),
                    ack.getObservedThrough() == null ? "—" : displayDate(ack.getObservedThrough()),
                    String.valueOf(f.participantCardStatusId()), displayDate(f.expirationDate()),
                    conditionsOf(f)));
        }
        return rows;
    }

    /** Which section(s) a finding belongs to, for the Acknowledged section's own display — a row
     *  there is not re-split by section (see the class Javadoc: one finding, possibly both). */
    private static String conditionsOf(CardFinding f) {
        if (f.problemStatus() && f.expiringOrExpired()) return "Problem status, Expired/expiring";
        return f.problemStatus() ? "Problem status" : "Expired/expiring";
    }

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
     * this check, keyed by {@link CardFinding#findingKey}. A finding with no acknowledgment at all
     * always surfaces — this is also how a status change re-surfaces a {@code HANDLED} row, since
     * {@code findingKey} embeds {@code ParticipantCardStatusID} (see the class Javadoc): the lookup
     * itself misses once the status differs, before either comparison below runs. {@code IGNORED}
     * always suppresses. {@code HANDLED} suppresses only while the current {@code ExpirationDate}
     * still equals what was observed at acknowledgment time ({@code Objects.equals}, so {@code
     * null} vs {@code null} is "unchanged") — see the class Javadoc for why this is an equality
     * check in both directions, unlike {@link CardDeclineCheck}'s monotonic one. The status-id
     * comparison alongside it can never actually fail for a row the lookup found (same reason), and
     * is kept only as a defensive fail-open guard against a missing {@code observedCount}.
     */
    static AckSplit splitByAcknowledgment(List<CardFinding> findings, Map<String, AuditFindingAck> acks) {
        List<CardFinding> surfaced = new ArrayList<>();
        List<Acknowledged> suppressed = new ArrayList<>();
        for (CardFinding finding : findings) {
            AuditFindingAck ack = acks.get(finding.findingKey());
            if (ack == null) {
                surfaced.add(finding);
                continue;
            }
            if (AuditFindingAck.STATE_IGNORED.equals(ack.getAckState())) {
                suppressed.add(new Acknowledged(finding, ack));
                continue;
            }
            boolean statusUnchanged = ack.getObservedCount() != null
                    && ack.getObservedCount() == finding.participantCardStatusId();
            boolean expiryUnchanged = Objects.equals(ack.getObservedThrough(), finding.expirationDate());
            if (statusUnchanged && expiryUnchanged) {
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
    private static String resolveEmployerName(Map<Integer, String> designated, String employerId) {
        try {
            String name = designated.get(Integer.parseInt(employerId.trim()));
            return name == null ? "" : name;
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /**
     * {@code Employee.firstName + " " + lastName} for {@code Employee.id} = {@code ParticipantID},
     * or {@code ""} when blank (every dependent row — see the class Javadoc), no row exists yet, or
     * the id does not parse. Never throws; a lookup failure is a blank name, not an error.
     */
    private static String resolveParticipantName(EntityManager em, String participantId) {
        try {
            Employee employee = em.find(Employee.class, Integer.parseInt(participantId.trim()));
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
     *  is set (and nothing else is trusted) when the required key is absent or blank. Package-private
     *  (not {@code private}, unlike {@link CardDeclineCheck}'s copy of the same shape) so the
     *  synthetic test can construct one directly — {@code AppConfig} has no reset/injection seam
     *  (it is loaded once, JVM-wide, from a resolved {@code ssa.properties} path) and is on this
     *  build's do-not-touch list, so driving {@link #evaluate(List, Config, Set)} under test means
     *  bypassing {@link Config#load()}, not it. */
    record Config(AuditResult notConfigured, String prefix, Set<Integer> problemStatusIds,
                  Set<Integer> activeStatusIds, int warnDays, LocalDate warnThroughDate,
                  long maxAgeHours, long maxBytes) {

        static Config load() {
            String prefix = AppConfig.get(CFG_PREFIX);
            if (prefix == null || prefix.isBlank()) {
                return new Config(AuditResult.notConfigured("Config key " + CFG_PREFIX + " is not set."),
                        null, Set.of(), Set.of(), 0, null, 0, 0);
            }

            Set<Integer> problemStatusIds = parseIdSetOrDefault(AppConfig.get(CFG_PROBLEM_STATUS_IDS),
                    DEFAULT_PROBLEM_STATUS_IDS);
            Set<Integer> activeStatusIds = parseIdSetOrDefault(AppConfig.get(CFG_ACTIVE_STATUS_IDS),
                    DEFAULT_ACTIVE_STATUS_IDS);

            int warnDays = (int) parseLongOrDefault(AppConfig.get(CFG_WARN_DAYS), DEFAULT_WARN_DAYS);
            if (warnDays < 0) warnDays = DEFAULT_WARN_DAYS;

            long maxAgeHours = parseLongOrDefault(AppConfig.get(CFG_MAX_AGE_HOURS), DEFAULT_MAX_AGE_HOURS);
            long maxBytes = parseLongOrDefault(AppConfig.get(CFG_MAX_BYTES), DEFAULT_MAX_BYTES);

            LocalDate warnThroughDate = LocalDate.now().plusDays(warnDays);

            return new Config(null, prefix.trim(), problemStatusIds, activeStatusIds, warnDays,
                    warnThroughDate, maxAgeHours, maxBytes);
        }
    }

    private static Set<Integer> parseIdSetOrDefault(String raw, Set<Integer> fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        Set<Integer> out = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                // skip malformed token
            }
        }
        return out.isEmpty() ? fallback : out;
    }

    private static long parseLongOrDefault(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
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

        List<CardRow> rows;
        try {
            rows = parse(content);
        } catch (MissingHeaderException e) {
            return Loaded.failed(AuditResult.error(e.getMessage()));
        } catch (Exception e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        return Loaded.ok(newestName, fileTimestamp, rows);
    }

    /** Exact-match filename pattern — see {@link CardDeclineCheck#newestMatchingPattern}; built per
     *  call since the prefix is runtime config. */
    static Pattern newestMatchingPattern(String prefix) {
        return Pattern.compile("^" + Pattern.quote(prefix) + "_Export_(\\d{17})(?:_[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$");
    }

    /** Sibling {@code ExportFiles} directory to {@code importDir} — identical rule to the other
     *  checks. */
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
     *  to a {@link CardRow} holding only the columns this check reads. Package-private (not
     *  {@code private}, unlike {@link CardDeclineCheck}'s copy of the same shape) so the synthetic
     *  test can drive CSV parsing and required-header validation directly. */
    List<CardRow> parse(String content) throws Exception {
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

        List<CardRow> rows = new ArrayList<>();
        for (int r = 1; r < lines.size(); r++) {
            String[] raw = lines.get(r);
            rows.add(new CardRow(
                    cellOf(raw, index, H_EMPLOYER_ID),
                    cellOf(raw, index, H_EMPLOYER_NAME),
                    cellOf(raw, index, H_USER_ID),
                    cellOf(raw, index, H_PARTICIPANT_ID),
                    cellOf(raw, index, H_DEPENDENT_ID),
                    cellOf(raw, index, H_USER_TYPE_ID),
                    cellOf(raw, index, H_LAST_FOUR),
                    cellOf(raw, index, H_STATUS),
                    cellOf(raw, index, H_PARTICIPANT_CARD_STATUS_ID),
                    cellOf(raw, index, H_EXPIRATION_DATE),
                    cellOf(raw, index, H_ISSUED_DATE),
                    cellOf(raw, index, H_MAILED_DATE),
                    cellOf(raw, index, H_REQUESTED_DATE)));
        }
        return rows;
    }

    private static String cellOf(String[] raw, Map<String, Integer> index, String header) {
        Integer i = index.get(header);
        if (i == null || i >= raw.length || raw[i] == null) return "";
        return raw[i].trim();
    }

    // ── Evaluate ──────────────────────────────────────────────────────

    /**
     * One pass over every row: {@code dateParsedRows} is computed over <b>every</b> row (matched or
     * not) so {@link #evaluate(EntityManager, Long)} can tell "the export's dates are unparsable"
     * apart from "there happen to be no findings." A row at an undesignated employer is dropped
     * (counted in {@code undesignatedRows}) before the two conditions are even tested. Every cell
     * was already trimmed by {@code cellOf}.
     * <p>
     * <b>Supersession is evaluated before the finding key or the acknowledgment split even exist for
     * a row.</b> A flagged row (problem-status or expiring-or-expired) is superseded — and becomes a
     * {@link SupersededFinding} instead of a {@link CardFinding} — when the same {@code UserID} has
     * at least one <i>other</i> card row (grouped over <b>every</b> row at a designated employer,
     * not only flagged ones — a replacement can be, and often is, itself a flagged row: see the
     * "widened" paragraph below) whose {@code ParticipantCardStatusID} is <i>card-bearing</i> (in
     * {@link #cardBearingStatusIds} — the active list <b>union</b> the problem list, which excludes
     * only {@code Requested} and {@code Requested (Queued)}, the two statuses that never produced an
     * actual card) and whose {@link #cardDateOf card date} is <b>strictly</b> later than the flagged
     * row's own card date. A superseded row is not a finding at all: it contributes to neither
     * {@code findingCount} nor the acknowledgment split.
     * <p>
     * <b>Widened on purpose, and why it is safe (the never-hide invariant):</b> the replacement is
     * not required to be usable — a newer {@code Hold} or {@code Lost/Stolen} for the same holder
     * also supersedes an older problem row, because the older record is exactly as dead as one
     * replaced by a usable card; it just hasn't been replaced by something usable <i>yet</i>. This
     * cannot hide a real, current problem: <b>the newest card-bearing row for a holder is never
     * itself superseded</b>, because nothing in the group is later than it, so if that newest row is
     * itself in a problem status or expired, it always surfaces on its own. Supersession only ever
     * removes strictly older history, never the most current state.
     * <p>
     * <b>Replacement selection, when a row does supersede</b> (see {@link #cardDateOf}'s companion
     * logic inline below): among every qualifying (card-bearing, strictly-later) candidate, the
     * <i>newest usable one</i> (see {@link #usableStatusIds}) is preferred and shown as the
     * replacement, even if a chronologically newer problem-status candidate also qualifies — a usable
     * replacement is the stronger evidence the account is fine now. Only when <b>no</b> qualifying
     * candidate is usable does the newest qualifying problem-status candidate get shown instead, and
     * its own status (e.g. {@code Hold}) is what the Superseded section's replacement column then
     * displays — visibly a problem card, not a clean one, which is exactly what happened.
     * <p>
     * This is <b>fail-open in both directions</b>: if the flagged row's own card date does not parse,
     * supersession is not even attempted (it stays surfaced); a candidate replacement whose own card
     * date does not parse is simply not usable evidence (skipped, not treated as "later"). Because
     * supersession is recomputed on every run from the current export, it is inherently
     * self-correcting: if a replacement card is later put on {@code Hold} or goes {@code Lost/Stolen}
     * itself, that does not un-supersede anything (a problem-status replacement was already eligible),
     * but if it is instead reissued (a genuinely newer usable card), the old row's shown replacement
     * updates to reflect that on the next run — and the replacement, now itself flagged, is evaluated
     * for supersession the same way everything else is (it may in turn be superseded by a
     * still-newer card, per the never-hide invariant above).
     */
    static Evaluation evaluate(List<CardRow> rows, Config config, Set<String> designatedAltIds) {
        List<CardFinding> findings = new ArrayList<>();
        List<SupersededFinding> superseded = new ArrayList<>();
        Set<String> distinctEmployers = new LinkedHashSet<>();
        int dateParsedRows = 0;
        int matchedRows = 0;
        int undesignatedRows = 0;

        Set<Integer> usableStatusIds = usableStatusIds(config);
        Set<Integer> cardBearingStatusIds = cardBearingStatusIds(config);

        // Grouped over every row at a designated employer -- not only flagged ones, since a
        // replacement card is often itself unflagged (or, since the widening, may itself be
        // flagged — a newer Hold row, say) and would otherwise never appear in any group. A
        // dependent's UserID is its own, distinct from the primary cardholder's, so dependents
        // group (and supersede) correctly with no special-casing here.
        Map<String, List<CardRow>> byUserId = new LinkedHashMap<>();
        for (CardRow row : rows) {
            if (!designatedAltIds.contains(row.employerId())) continue;
            byUserId.computeIfAbsent(row.userId(), k -> new ArrayList<>()).add(row);
        }

        for (CardRow row : rows) {
            LocalDate expirationDate = dateOf(row.expirationDateRaw());
            if (expirationDate != null) dateParsedRows++;

            String employerId = row.employerId();
            if (!designatedAltIds.contains(employerId)) {
                undesignatedRows++;
                continue;
            }
            matchedRows++;
            if (!employerId.isEmpty()) distinctEmployers.add(employerId);

            Integer statusId = parseIntOrNull(row.participantCardStatusId());
            if (statusId == null) continue; // ParticipantCardStatusID present but unparsable — can't classify

            boolean problem = config.problemStatusIds.contains(statusId);
            boolean cardExists = config.activeStatusIds.contains(statusId);
            boolean expiring = cardExists && expirationDate != null && !expirationDate.isAfter(config.warnThroughDate);

            if (!problem && !expiring) continue;

            // Supersession check — before the finding key is even built. See the method Javadoc.
            // Two candidates are tracked in one pass: the newest USABLE qualifying candidate
            // (preferred), and the newest qualifying candidate of ANY card-bearing status (the
            // fallback, used only when no usable one qualifies — in which case it is necessarily a
            // problem-status candidate, since usable and problem never overlap).
            LocalDate flaggedCardDate = cardDateOf(row);
            CardRow usableReplacement = null;
            LocalDate usableReplacementDate = null;
            CardRow anyReplacement = null;
            LocalDate anyReplacementDate = null;
            if (flaggedCardDate != null) {
                for (CardRow candidate : byUserId.getOrDefault(row.userId(), List.of())) {
                    if (candidate == row) continue; // "at least one OTHER card row"
                    Integer candidateStatusId = parseIntOrNull(candidate.participantCardStatusId());
                    if (candidateStatusId == null || !cardBearingStatusIds.contains(candidateStatusId)) continue;
                    LocalDate candidateCardDate = cardDateOf(candidate);
                    if (candidateCardDate == null || !candidateCardDate.isAfter(flaggedCardDate)) continue;

                    if (anyReplacementDate == null || candidateCardDate.isAfter(anyReplacementDate)) {
                        anyReplacement = candidate;
                        anyReplacementDate = candidateCardDate;
                    }
                    if (usableStatusIds.contains(candidateStatusId)
                            && (usableReplacementDate == null || candidateCardDate.isAfter(usableReplacementDate))) {
                        usableReplacement = candidate;
                        usableReplacementDate = candidateCardDate;
                    }
                }
            }
            CardRow replacement = usableReplacement != null ? usableReplacement : anyReplacement;
            LocalDate replacementCardDate = usableReplacement != null ? usableReplacementDate : anyReplacementDate;

            if (replacement != null) {
                superseded.add(new SupersededFinding(employerId, row.userId(), row.participantId(), row.dependentId(),
                        row.userTypeId(), row.lastFour(), row.status(), statusId, expirationDate, row.issuedDate(),
                        row.mailedDate(), problem, expiring,
                        replacement.lastFour(), replacement.status(), replacementCardDate,
                        usableReplacement != null));
                continue; // not a finding at all — see the method Javadoc
            }

            String findingKey = row.userId() + ":" + row.lastFour() + ":" + row.participantCardStatusId();

            findings.add(new CardFinding(employerId, row.userId(), row.participantId(), row.dependentId(),
                    row.userTypeId(), row.lastFour(), row.status(), statusId, expirationDate, row.issuedDate(),
                    row.mailedDate(), problem, expiring, findingKey));
        }

        return new Evaluation(findings, superseded, distinctEmployers, matchedRows, undesignatedRows, dateParsedRows);
    }

    /** The usable-status set: the active list minus the problem list — with the defaults
     *  ({@code 3,2,1,4,11} minus {@code 5,4,6}) that is {@code 3,2,1,11} (Active, Mailed, Issued,
     *  Reissued). Derived from the two existing config keys every call; deliberately <b>not</b> a
     *  new config key. Used two ways: as the gate for the expiry test ({@link #evaluate}'s {@code
     *  cardExists}), and as the <i>preferred</i> replacement tier in the supersession rule — a usable
     *  replacement is shown over a merely card-bearing one when both qualify (see {@link
     *  #evaluate(List, Config, Set)}). {@code Hold} (4) sits in both this and the problem list, so it
     *  is always removed here and is therefore never a <i>usable</i> replacement — though since the
     *  widening it can still be a card-bearing one; see {@link #cardBearingStatusIds}. */
    static Set<Integer> usableStatusIds(Config config) {
        Set<Integer> usable = new LinkedHashSet<>(config.activeStatusIds);
        usable.removeAll(config.problemStatusIds);
        return usable;
    }

    /** The card-bearing status set for the supersession rule's replacement eligibility (not the
     *  expiry test — that still gates on {@link #usableStatusIds} alone): the active list
     *  <b>union</b> the problem list — with the defaults ({@code 3,2,1,4,11} union {@code 5,4,6})
     *  that is {@code 1,2,3,4,5,6,11}. Derived from the same two existing config keys every call;
     *  deliberately <b>not</b> a new config key. This excludes only {@code Requested} (7) and
     *  {@code Requested (Queued)} (13) — the two statuses that never produced a card at all — so a
     *  newer {@code Hold} or {@code Lost/Stolen} row still counts as evidence the older record is
     *  superseded history, not just a newer {@code Active}/{@code Mailed}/{@code Issued}/{@code
     *  Reissued} one. See the class Javadoc's "never-hide invariant" for why widening the replacement
     *  set this way cannot suppress a genuinely current problem. */
    static Set<Integer> cardBearingStatusIds(Config config) {
        Set<Integer> cardBearing = new LinkedHashSet<>(config.activeStatusIds);
        cardBearing.addAll(config.problemStatusIds);
        return cardBearing;
    }

    /**
     * The supersession rule's notion of "when was this card issued" — {@code IssuedDate}, falling
     * back to {@code RequestedDate} only when {@code IssuedDate} is <b>blank</b> (not merely
     * unparsable — see below). The fallback exists because {@code IssuedDate} is unreliable on this
     * export: blank on 8 of 41 {@code Issued} rows and 14 of 526 {@code Active} rows in the
     * verification sample, which would otherwise make a genuinely-replaced card's newer record
     * invisible to the supersession rule for no reason but a missing timestamp Summit itself never
     * back-filled. <b>Fail-open, deliberately asymmetric:</b> a blank {@code IssuedDate} tries {@code
     * RequestedDate}; a <i>present but unparsable</i> {@code IssuedDate} does not — it returns
     * {@code null} rather than guessing from a different column entirely, and {@link #evaluate}
     * treats a {@code null} card date as "no evidence," never as "treat as always latest" or "always
     * earliest." Reads only the leading date token via {@link #dateOf}, same as every other date
     * column this check parses.
     */
    static LocalDate cardDateOf(CardRow row) {
        String issuedRaw = row.issuedDate();
        if (issuedRaw != null && !issuedRaw.isBlank()) {
            return dateOf(issuedRaw);
        }
        return dateOf(row.requestedDate());
    }

    /** Reads only the leading date token, same as the other checks' Summit date columns. */
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

    /** Package-private (not {@code private}) so the synthetic test can assert on the exact summary
     *  text, the same reasoning as {@link #parse} and {@link Config}. */
    static String summaryOf(Evaluation evaluation, int designatedCount, AckSplit split, String fileName) {
        int problemCount = 0;
        int expiryCount = 0;
        for (CardFinding f : split.surfaced) {
            if (f.problemStatus()) problemCount++;
            if (f.expiringOrExpired()) expiryCount++;
        }
        int handledCount = 0;
        int ignoredCount = 0;
        for (Acknowledged a : split.suppressed) {
            if (AuditFindingAck.STATE_IGNORED.equals(a.ack().getAckState())) ignoredCount++; else handledCount++;
        }
        int supersededByUsable = 0;
        int supersededByProblem = 0;
        for (SupersededFinding sf : evaluation.superseded) {
            if (sf.replacementIsUsable()) supersededByUsable++; else supersededByProblem++;
        }
        return split.surfaced.size() + " card record(s) flagged (" + problemCount + " problem-status, "
                + expiryCount + " expired/expiring) at " + evaluation.distinctEmployers.size() + " of "
                + designatedCount + " designated employer(s); " + evaluation.superseded.size()
                + " superseded by a newer card (" + supersededByUsable + " by a usable card, "
                + supersededByProblem + " by a newer problem card); " + evaluation.undesignatedRows
                + " row(s) at undesignated employers ignored; " + split.suppressed.size()
                + " finding(s) acknowledged (" + handledCount + " handled, " + ignoredCount
                + " ignored) — export " + fileName;
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
    record CardRow(String employerId, String employerName, String userId, String participantId,
                   String dependentId, String userTypeId, String lastFour, String status,
                   String participantCardStatusId, String expirationDateRaw, String issuedDate,
                   String mailedDate, String requestedDate) {
    }

    /** One qualifying card record — a row that satisfied at least one of the two conditions.
     *  {@code problemStatus}/{@code expiringOrExpired} record which; both may be true (see the
     *  class Javadoc). {@code expirationDate} may be {@code null} when unparsable/blank on a
     *  problem-status-only row that never reached the expiry test. */
    record CardFinding(String employerId, String userId, String participantId, String dependentId,
                       String userTypeId, String lastFour, String status, int participantCardStatusId,
                       LocalDate expirationDate, String issuedDate, String mailedDate, boolean problemStatus,
                       boolean expiringOrExpired, String findingKey) {
    }

    /** A row that would have qualified as a {@link CardFinding} but was superseded by a newer
     *  card-bearing row for the same {@code UserID} — see {@link #evaluate(List, Config, Set)}'s
     *  Javadoc. Carries the same fields a {@link CardFinding} would, plus the replacement evidence
     *  ({@code replacementLastFour}/{@code replacementStatus}/{@code replacementCardDate}) so the
     *  Superseded section can be eyeballed against the row it explains away, and {@code
     *  replacementIsUsable} (the usable-preferred tier that was actually shown — {@code false} means
     *  the shown replacement is itself a problem-status card, e.g. {@code Hold}) for the summary
     *  line's and the header row's usable/problem split. Never acknowledged — there is no {@code
     *  findingKey} here on purpose, since a superseded row is not a finding. */
    record SupersededFinding(String employerId, String userId, String participantId, String dependentId,
                             String userTypeId, String lastFour, String status, int participantCardStatusId,
                             LocalDate expirationDate, String issuedDate, String mailedDate, boolean problemStatus,
                             boolean expiringOrExpired, String replacementLastFour, String replacementStatus,
                             LocalDate replacementCardDate, boolean replacementIsUsable) {
    }

    /** One current finding paired with the acknowledgment suppressing it. */
    record Acknowledged(CardFinding finding, AuditFindingAck ack) {
    }

    /** {@link #splitByAcknowledgment}'s result. */
    record AckSplit(List<CardFinding> surfaced, List<Acknowledged> suppressed) {
    }

    /** Findings plus the whole-file counts the status decision and summary line read.
     *  {@code superseded} findings are excluded from {@code findings} entirely (see {@link
     *  #evaluate(List, Config, Set)}) — they never reach {@link #splitByAcknowledgment}. Package-private
     *  so the synthetic test can pass one to {@link #toRows}. */
    record Evaluation(List<CardFinding> findings, List<SupersededFinding> superseded,
                      Set<String> distinctEmployers, int matchedRows, int undesignatedRows, int dateParsedRows) {
    }

    private record Loaded(AuditResult errorResult, String fileName, LocalDateTime fileTimestamp,
                          List<CardRow> rows) {
        static Loaded failed(AuditResult errorResult) {
            return new Loaded(errorResult, null, null, List.of());
        }

        static Loaded ok(String fileName, LocalDateTime fileTimestamp, List<CardRow> rows) {
            return new Loaded(null, fileName, fileTimestamp, rows);
        }
    }

    /** One finding, narrowed to exactly what the detail page renders in either section.
     *  {@code findingKey} rides along so the JSP's acknowledge form can post it directly, without
     *  the servlet having to reconstruct it. */
    public record Row(String userId, String participantId, String participantName, String dependentId,
                      String employerId, String employerName, String lastFour, String status,
                      String participantCardStatusId, String expirationDateDisplay, String issuedDate,
                      String mailedDate, String findingKey) {
    }

    /** One superseded row for the Superseded section — the same columns {@link Row} carries, minus
     *  {@code findingKey} (nothing here is acknowledgeable), plus the newest qualifying replacement's
     *  {@code LastFour}, {@code Status} and card date, so the row can be eyeballed against the
     *  evidence that superseded it. */
    public record SupersededRow(String userId, String participantId, String participantName, String dependentId,
                                String employerId, String employerName, String lastFour, String status,
                                String participantCardStatusId, String expirationDateDisplay, String issuedDate,
                                String mailedDate, String replacementLastFour, String replacementStatus,
                                String replacementCardDateDisplay) {
    }

    /** One row of the Acknowledged section: a suppressed finding's current activity alongside what
     *  was recorded when it was acknowledged, and which section(s) it belongs to. */
    public record AckRow(Long ackId, String userId, String participantName, String participantId,
                         String dependentId, String employerId, String employerName, String ackState,
                         String note, String ackedBy, String ackedAtDisplay, String observedStatusDisplay,
                         String observedThroughDisplay, String currentStatusDisplay,
                         String currentExpirationDisplay, String conditions) {
    }

    /** The detail page's whole view model. {@code error} is set (with empty lists) when the check
     *  is not configured or the export could not be loaded or parsed. {@code problemRows}/{@code
     *  expiryRows} are the two surfaced sections — a finding satisfying both conditions appears in
     *  both lists (see the class Javadoc); {@code acknowledged} is the single suppressed-findings
     *  section, not split by condition. {@code supersededRows}/{@code supersededCount} are the
     *  evidence for the supersession rule (see {@link #evaluate(List, Config, Set)}) — rows here
     *  never appear in {@code problemRows}, {@code expiryRows}, or {@code acknowledged}, and are
     *  never counted in {@code findingCount}. {@code supersededByUsable}/{@code supersededByProblem}
     *  split {@code supersededCount} by which replacement tier was actually shown for each row (see
     *  {@code SupersededFinding.replacementIsUsable}), mirroring the summary line's own split. */
    public record Snapshot(String fileName, LocalDateTime fileTimestamp, int warnDays,
                           int designatedEmployerCount, List<Row> problemRows, List<Row> expiryRows,
                           int suppressedCount, List<AckRow> acknowledged, List<SupersededRow> supersededRows,
                           int supersededByUsable, int supersededByProblem, int matchedRows,
                           int distinctEmployerCount, int undesignatedRows, String error) {
        static Snapshot failed(String error) {
            return new Snapshot(null, null, 0, 0, List.of(), List.of(), 0, List.of(), List.of(), 0, 0, 0, 0, 0, error);
        }

        public int supersededCount() {
            return supersededRows.size();
        }
    }
}

package net.superiorstate.ams.data.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.controller.market.SummitExportServlet;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.data.dao.SummitSetupStepDAO;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.market.SummitFileExport;
import net.superiorstate.ams.model.market.SummitSetupStep;
import net.superiorstate.ams.model.sales.agency.Prospect;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S62-P2 -- read-only export-based verification for the Demographics push (file 4). Compares the
 * participant-list export's {@code ParticipantCustomID} column against AMS's own
 * {@code employer_participant} roster for one setup.
 * <p>
 * <b>Why Demographics first</b> (S62-P1 Q4): it is the only unproven push with an observed echo --
 * {@code docs/business/summit_data_exchange.md:1007-1011} records that {@code ParticipantCustomID}
 * is populated for every AMS-loaded participant and blank for one added in the Summit UI, and
 * {@code SummitResponseService}'s "Check response" already renders every line {@code UNKNOWN} for
 * this step (its classifier reads {@code fields[0]}, which on this response shape is the
 * participant key, not a status), so the panel has no working signal here today.
 * <p>
 * <b>S62-P4 -- the compare alone cannot distinguish "never pushed" from "pushed and missing."</b>
 * A proposal with no recorded delivery attempt reports every expected participant {@code MISSING}
 * with no way to tell that reading apart from a genuine push failure. {@link Verdict} adds that
 * distinction using {@code SummitSetupStepDAO}'s existing read methods (see the DAO Javadoc on
 * {@link #verify} below) -- the compare logic itself (the per-participant loop) is unchanged.
 * <p>
 * ⚠️ <b>TA-56 -- the export itself is filtered Summit-side to employers on PremiumPath or a regular
 * ICHRA.</b> The filter is employer-level, not participant-level, so the per-participant compare
 * stays sound for an in-scope employer -- but {@link Verdict#EMPLOYER_KEY_ABSENT} now has two
 * live causes this class cannot tell apart: the employer is out of scope for this export, or the
 * composed key genuinely does not match. Both are true "zero rows" outcomes; neither is asserted
 * over the other. See TA-56 in {@code docs/analysis/technical_assumptions.md}.
 * <p>
 * <b>S62-P5 -- {@code pspId} comes from the caller (the servlet's session), not an inference.</b>
 * S62-P4 derived it from {@code proposal.getRate().getPsp()} and documented that as an unproven
 * invariant. {@code SummitDemographicsVerifyServlet} now resolves it the same way
 * {@code SummitSetupStatusServlet.java:123-129} does and passes it in; this class no longer loads
 * a {@code Proposal} or reasons about {@code Rate} at all.
 * <p>
 * <b>Persists nothing (LA-40).</b> Every row this class reads lives only in the returned
 * {@link Result} for one request's render; no {@code EntityManager} write happens anywhere in this
 * class, no {@code audit_run} row, no cache. Every {@link EntityManager} call is a read
 * ({@code em.find}, {@link EmployerParticipantDAO#findByProspectId},
 * {@link SummitSetupStepDAO#findByProposalAndStep}, {@link SummitSetupStepDAO#findLatestDeliveryAttempt}
 * -- all three confirmed by inspection to contain no {@code persist}/{@code merge}/transaction call).
 */
public final class SummitDemographicsVerifyService {

    private static final String CFG_PREFIX = "SUMMIT_AUDIT_PARTICIPANT_EXPORT_PREFIX";
    private static final String CFG_IMPORT_DIR = "SUMMIT_SFTP_IMPORT_DIR";
    private static final String CFG_MAX_BYTES = "SUMMIT_AUDIT_EXPORT_MAX_BYTES";
    private static final long DEFAULT_MAX_BYTES = 16_777_216L;

    private static final String H_PARTICIPANT_CUSTOM_ID = "ParticipantCustomID";
    private static final String H_EMPLOYER_CUSTOM_ID = "EmployerCustomID";
    private static final String H_USER_STATUS = "UserStatus";

    /** The {@code step}/{@code fileType} token for Demographics -- identical string in both
     *  {@code SummitSetupStatusServlet.STEP_FILE_TYPES} and {@code SummitResponseServlet.STEP_FILE_TYPES}
     *  (both map {@code "demographics"} to itself), so one constant serves both DAO parameters. */
    private static final String STEP_DEMOGRAPHICS = "demographics";

    /**
     * ⚠️ <b>All three headers are required here</b>, unlike {@code IchraUncodedParticipantsCheck}
     * (which keys on {@code Employer_ID} and treats {@code EmployerCustomID} as optional). This
     * class correlates on {@code EmployerCustomID} directly -- there is no other employer key on
     * this export that maps back to AMS's own composed {@code Employer TPA Custom ID} -- so its
     * absence means the compare cannot run at all, not merely that one optional field is missing.
     */
    private static final List<String> REQUIRED_HEADERS =
            List.of(H_PARTICIPANT_CUSTOM_ID, H_EMPLOYER_CUSTOM_ID, H_USER_STATUS);

    private SummitDemographicsVerifyService() {}

    public enum Outcome {
        /** The compare ran; see the per-participant list and counts. */
        OK,
        /** No export matched the configured prefix (or none had a parseable timestamp). */
        NO_EXPORT_FOUND,
        /** The export was read but is missing one or more of {@link #REQUIRED_HEADERS}. */
        MISSING_HEADERS,
        /** {@code SUMMIT_TPA_ID_PREFIX} is absent, blank, or non-alphanumeric -- no employer or
         *  participant key can be composed, so no compare can run. */
        PREFIX_NOT_CONFIGURED,
        /** {@code prospectId} does not resolve to a {@link Prospect}. */
        PROSPECT_NOT_FOUND,
        /** {@code SUMMIT_SFTP_IMPORT_DIR} does not resolve to an {@code ExportFiles} sibling. */
        EXPORT_DIR_NOT_CONFIGURED,
        /** The SFTP transport itself failed. {@link Result#errorMessage} carries a scrubbed message. */
        TRANSPORT_ERROR
    }

    public enum ParticipantState { FOUND, FOUND_WRONG_EMPLOYER, MISSING }

    /**
     * S62-P4 -- the single banner-worthy reading of an {@link Outcome#OK} result. Populated only
     * when {@code outcome == OK}; null for every other {@link Outcome}, since those short-circuit
     * before a compare ever runs (there is nothing to distinguish "never pushed" from "pushed and
     * missing" when there is no export, no headers, or no configuration to run the compare at all).
     * <p>
     * <b>Evaluated in this order inside {@link #verify}; first match wins</b> ({@code NO_EXPORT_FOUND}
     * and {@code HEADER_MISSING} are steps 1-2 of that ordering conceptually, but they are
     * {@link Outcome} values reached before this enum is ever consulted -- listed here only so the
     * full priority is legible in one place):
     * <ol>
     *   <li>{@code NO_EXPORT_FOUND} -- {@link Outcome#NO_EXPORT_FOUND}, returned before this enum runs.</li>
     *   <li>{@code HEADER_MISSING} -- {@link Outcome#MISSING_HEADERS}, returned before this enum runs.</li>
     *   <li>{@link #NO_PUSH_RECORDED} -- checked first among the values on this enum. A proposal
     *       with no delivery attempt must never reach {@link #EMPLOYER_KEY_ABSENT},
     *       {@link #ALL_CONFIRMED} or {@link #PARTICIPANTS_UNCONFIRMED}: with nothing pushed, the
     *       compare's {@code MISSING} rows are an expectation, not a finding.</li>
     *   <li>{@code EXPORT_PREDATES_PUSH} -- <b>not implemented, deliberately.</b> See {@link #verify}'s
     *       comment on timestamp zones (D-104's documented several-hour clock skew between Summit's
     *       export-filename clock and this server's {@code LocalDateTime.now()}). A wrong staleness
     *       verdict is worse than none, so this step is skipped entirely rather than guessed.</li>
     *   <li>{@link #EMPLOYER_KEY_ABSENT} -- checked before the per-participant tally: zero export
     *       rows carry this employer's key at all, which is an employer-leg or key-composition
     *       problem, not evidence about any specific participant.</li>
     *   <li>{@link #ALL_CONFIRMED} / {@link #PARTICIPANTS_UNCONFIRMED} -- reached only once a push
     *       is recorded and the employer's key is present in the export; the only two verdicts that
     *       are genuinely about individual participants.</li>
     * </ol>
     */
    public enum Verdict { NO_PUSH_RECORDED, EXPORT_PREDATES_PUSH, EMPLOYER_KEY_ABSENT, ALL_CONFIRMED, PARTICIPANTS_UNCONFIRMED }

    /** One expected participant's outcome. {@code userStatus} is null when {@code state} is
     *  {@link ParticipantState#MISSING} -- there is no export row to read it from. */
    public record ParticipantCheck(Long employerParticipantId, String firstName, String lastName,
                                    String expectedKey, ParticipantState state, String userStatus) {
    }

    /**
     * The compare's full outcome. {@code participants} and every count are empty/zero unless
     * {@code outcome} is {@link Outcome#OK}. {@code missingHeaderNames} is populated only for
     * {@link Outcome#MISSING_HEADERS}; {@code errorMessage} only for {@link Outcome#TRANSPORT_ERROR}.
     * {@code verdict}, {@code expectedEmployerKey}, {@code totalDataRows},
     * {@code employerMatchingRowCount}, {@code lastPushTimestamp} and {@code manualMarkDoneOnly} are
     * populated only for {@link Outcome#OK} -- see {@link Verdict}'s own Javadoc.
     */
    public record Result(Outcome outcome, String fileName, LocalDateTime fileTimestamp,
                          List<String> missingHeaderNames, String errorMessage,
                          Verdict verdict, String expectedEmployerKey,
                          int totalDataRows, int employerMatchingRowCount,
                          LocalDateTime lastPushTimestamp, boolean manualMarkDoneOnly,
                          int expectedCount, int foundCount, int wrongEmployerCount,
                          int missingCount, int unkeyedCount, List<ParticipantCheck> participants) {

        static Result notFound() {
            return new Result(Outcome.NO_EXPORT_FOUND, null, null, List.of(), null,
                    null, null, 0, 0, null, false,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result missingHeaders(String fileName, LocalDateTime fileTimestamp, List<String> missing) {
            return new Result(Outcome.MISSING_HEADERS, fileName, fileTimestamp, missing, null,
                    null, null, 0, 0, null, false,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result prefixNotConfigured() {
            return new Result(Outcome.PREFIX_NOT_CONFIGURED, null, null, List.of(), null,
                    null, null, 0, 0, null, false,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result prospectNotFound() {
            return new Result(Outcome.PROSPECT_NOT_FOUND, null, null, List.of(), null,
                    null, null, 0, 0, null, false,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result exportDirNotConfigured() {
            return new Result(Outcome.EXPORT_DIR_NOT_CONFIGURED, null, null, List.of(), null,
                    null, null, 0, 0, null, false,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result transportError(String message) {
            return new Result(Outcome.TRANSPORT_ERROR, null, null, List.of(), message,
                    null, null, 0, 0, null, false,
                    0, 0, 0, 0, 0, List.of());
        }
    }

    /**
     * Runs the compare for one setup. Reads only -- see the class Javadoc. Never throws; every
     * failure path (missing config, missing prospect, transport failure, missing headers) returns
     * a populated {@link Result} rather than propagating an exception.
     * <p>
     * S62-P4 added {@code proposalId}: it feeds {@code SummitSetupStepDAO.findByProposalAndStep}
     * (proposal + step, {@code SummitSetupStatusServlet.java:93}) and
     * {@code SummitSetupStepDAO.findLatestDeliveryAttempt} (PSP + proposal + file type,
     * {@code SummitSetupStatusServlet.java:103}) -- both confirmed by inspection to run one
     * {@code em.createQuery(...).getResultList()} each, no write, no transaction.
     * <p>
     * S62-P5 added {@code pspId}, supplied by the caller -- see the class Javadoc. May be null
     * (an unresolvable session); {@code findLatestDeliveryAttempt} returns null for a null
     * {@code pspId} exactly as it does for "no delivery attempt," so a null session PSP falls
     * through to {@link Verdict#NO_PUSH_RECORDED} rather than a special case.
     */
    public static Result verify(EntityManager em, long proposalId, Long pspId, long prospectId) {
        Prospect prospect = em.find(Prospect.class, prospectId);
        if (prospect == null) {
            return Result.prospectNotFound();
        }

        // Employer key composition is not duplicated here -- SummitExportServlet.resolveEmployerTpaCustomId
        // is public static exactly so callers do not have to reinvent its validation.
        String employerTpaCustomId = SummitExportServlet.resolveEmployerTpaCustomId(prospect);
        if (employerTpaCustomId == null) {
            return Result.prefixNotConfigured();
        }

        // The participant key ({prefix}-P-{id}) has no public shared composer anywhere in the tree
        // (SummitExportServlet.summitTpaIdPrefix() is private). Reading and validating the same
        // config key again here, rather than string-parsing it back out of employerTpaCustomId, is
        // the same trim+alphanumeric-only rule resolveEmployerTpaCustomId already applied -- and
        // since employerTpaCustomId resolved successfully above, this key is known-valid, matching
        // the exact safety argument SummitExportServlet.summitTpaIdPrefix()'s own Javadoc makes for
        // calling it unvalidated once employerTpaCustomId has already resolved.
        String participantPrefix = SummitExportFetch.config("SUMMIT_TPA_ID_PREFIX").trim();

        String importDir = SummitExportFetch.config(CFG_IMPORT_DIR);
        String exportDir = SummitExportFetch.exportDirFor(importDir);
        if (exportDir == null) {
            return Result.exportDirNotConfigured();
        }

        String prefix = SummitExportFetch.config(CFG_PREFIX);
        if (prefix == null || prefix.isBlank()) {
            return Result.notFound();
        }
        long maxBytes = parseLongOrDefault(SummitExportFetch.config(CFG_MAX_BYTES), DEFAULT_MAX_BYTES);

        SummitExportFetch.Result fetched;
        try {
            fetched = SummitExportFetch.fetch(new SummitSftpService(), exportDir, prefix, maxBytes);
        } catch (SummitSftpService.SftpTransportException e) {
            return Result.transportError(scrub(e));
        }

        if (!fetched.found()) {
            return Result.notFound();
        }

        List<String[]> lines;
        try {
            lines = parseCsv(fetched.content());
        } catch (Exception e) {
            return Result.transportError(scrub(e));
        }
        if (lines.isEmpty()) {
            return Result.missingHeaders(fetched.fileName(), fetched.fileTimestamp(), REQUIRED_HEADERS);
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
            return Result.missingHeaders(fetched.fileName(), fetched.fileTimestamp(), missing);
        }

        // Index export rows by ParticipantCustomID. summit_data_exchange.md's own warning: this
        // value is meant to be globally unique across every employer, so a direct key lookup is the
        // correct join -- a duplicate under a different employer would itself be the T-number-worthy
        // finding the warning describes, not a reason to key differently here.
        Map<String, String[]> byParticipantCustomId = new LinkedHashMap<>();
        int unkeyedCount = 0;
        // S62-P4 -- totalDataRows/employerMatchingRowCount feed Verdict.EMPLOYER_KEY_ABSENT and the
        // JSP's "Source export" block. employerMatchingRowCount counts every row under this
        // employer, keyed or not (unlike unkeyedCount, which counts only the blank-ParticipantCustomID
        // subset) -- a nonzero unkeyedCount does not by itself prove the employer's key appears at
        // all if every one of those rows happened to be unkeyed, so this is tracked independently.
        int totalDataRows = 0;
        int employerMatchingRowCount = 0;
        for (int r = 1; r < lines.size(); r++) {
            String[] row = lines.get(r);
            totalDataRows++;
            String participantCustomId = cellOf(row, index, H_PARTICIPANT_CUSTOM_ID);
            String rowEmployerCustomId = cellOf(row, index, H_EMPLOYER_CUSTOM_ID);
            if (employerTpaCustomId.equals(rowEmployerCustomId)) {
                employerMatchingRowCount++;
            }
            if (participantCustomId.isEmpty()) {
                if (employerTpaCustomId.equals(rowEmployerCustomId)) {
                    unkeyedCount++;
                }
                continue;
            }
            byParticipantCustomId.putIfAbsent(participantCustomId, row);
        }

        List<EmployerParticipant> roster = EmployerParticipantDAO.findByProspectId(em, prospectId);
        List<ParticipantCheck> checks = new ArrayList<>(roster.size());
        int foundCount = 0;
        int wrongEmployerCount = 0;
        int missingCount = 0;
        for (EmployerParticipant participant : roster) {
            String expectedKey = participantPrefix + "-P-" + participant.getId();
            String[] row = byParticipantCustomId.get(expectedKey);
            ParticipantState state;
            String userStatus = null;
            if (row == null) {
                state = ParticipantState.MISSING;
                missingCount++;
            } else {
                String rowEmployerCustomId = cellOf(row, index, H_EMPLOYER_CUSTOM_ID);
                userStatus = cellOf(row, index, H_USER_STATUS);
                // A blank EmployerCustomID cell on a matched row is not "a different" employer --
                // it is an absent one, so it is treated as FOUND rather than FOUND_WRONG_EMPLOYER.
                // See the class-level note; this is a judgment call, deliberately visible here
                // rather than silently folded into either state.
                if (rowEmployerCustomId.isEmpty() || employerTpaCustomId.equals(rowEmployerCustomId)) {
                    state = ParticipantState.FOUND;
                    foundCount++;
                } else {
                    state = ParticipantState.FOUND_WRONG_EMPLOYER;
                    wrongEmployerCount++;
                }
            }
            checks.add(new ParticipantCheck(participant.getId(), participant.getFirstName(),
                    participant.getLastName(), expectedKey, state, userStatus));
        }

        // S62-P4 -- delivery-attempt / step-state lookup, both read-only DAO calls (see the class
        // Javadoc and this method's own Javadoc for the file:line each was found at).
        //
        // pspId is the caller-supplied parameter (S62-P5) -- see this method's own Javadoc.
        // SummitSetupStepDAO.findByProposalAndStep does not filter by pspId at all (its own class
        // Javadoc: "Deliberately not filtered by pspId"), so a null pspId is safe there regardless.
        // findLatestDeliveryAttempt DOES filter by pspId and returns null immediately when it is
        // null -- a null (unresolvable session) pspId therefore falls through to
        // Verdict.NO_PUSH_RECORDED below, exactly as this method's own Javadoc states.
        SummitSetupStep stepState = SummitSetupStepDAO.findByProposalAndStep(em, pspId, proposalId, STEP_DEMOGRAPHICS);
        SummitFileExport latestAttempt =
                SummitSetupStepDAO.findLatestDeliveryAttempt(em, pspId, proposalId, STEP_DEMOGRAPHICS);
        LocalDateTime lastPushTimestamp = latestAttempt == null ? null : latestAttempt.getDeliveredAt();
        boolean manualMarkDoneOnly = latestAttempt == null
                && stepState != null && "DONE".equals(stepState.getState()) && "MANUAL".equals(stepState.getBasis());

        // EXPORT_PREDATES_PUSH (step 4 of Verdict's ordering) is deliberately not implemented here.
        // It would compare fetched.fileTimestamp() -- parsed from the export filename's digits via
        // DateTimeFormatter.ofPattern("yyyyMMddHHmmss"), i.e. Summit's own SFTP-side clock, whatever
        // zone that is -- against lastPushTimestamp above, which is LocalDateTime.now() taken on
        // this AMS application server's JVM default zone (SummitExportServlet.java:2301). Neither
        // value carries zone information (both are bare LocalDateTime, not Instant/ZonedDateTime),
        // and D-104 (docs/deployment_backlog.md) already documents an unresolved multi-hour clock
        // skew between these two systems on this exact export family. Comparing them as if they
        // shared a zone could produce a wrong "predates"/"postdates" verdict in either direction,
        // and per this build's own instruction a wrong staleness verdict is worse than none -- so
        // this step is skipped entirely and a proposal with a push always continues on to
        // EMPLOYER_KEY_ABSENT / ALL_CONFIRMED / PARTICIPANTS_UNCONFIRMED below, regardless of how
        // old the selected export is.

        // Verdict, first match wins -- see Verdict's own Javadoc for the full seven-step ordering
        // (steps 1-2, NO_EXPORT_FOUND/HEADER_MISSING, already returned above as Outcome values;
        // step 4, EXPORT_PREDATES_PUSH, is the paragraph immediately above).
        Verdict verdict;
        if (latestAttempt == null) {
            verdict = Verdict.NO_PUSH_RECORDED;
        } else if (employerMatchingRowCount == 0) {
            verdict = Verdict.EMPLOYER_KEY_ABSENT;
        } else if (missingCount == 0 && wrongEmployerCount == 0) {
            verdict = Verdict.ALL_CONFIRMED;
        } else {
            verdict = Verdict.PARTICIPANTS_UNCONFIRMED;
        }

        return new Result(Outcome.OK, fetched.fileName(), fetched.fileTimestamp(), List.of(), null,
                verdict, employerTpaCustomId, totalDataRows, employerMatchingRowCount,
                lastPushTimestamp, manualMarkDoneOnly,
                roster.size(), foundCount, wrongEmployerCount, missingCount, unkeyedCount, checks);
    }

    private static List<String[]> parseCsv(String content) throws Exception {
        List<String[]> lines = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8)).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String cellOf(String[] raw, Map<String, Integer> index, String header) {
        Integer i = index.get(header);
        if (i == null || i >= raw.length || raw[i] == null) return "";
        return raw[i].trim();
    }

    private static long parseLongOrDefault(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Matches {@code SummitRefreshService.scrub} / {@code IchraUncodedParticipantsCheck.scrub}: a
     *  short, exception-class-plus-message string, never the full stack trace. */
    private static String scrub(Throwable t) {
        String message = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
        return message.length() > 400 ? message.substring(0, 400) : message;
    }
}

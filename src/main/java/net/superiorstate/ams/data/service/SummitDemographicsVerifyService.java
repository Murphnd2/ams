package net.superiorstate.ams.data.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.controller.market.SummitExportServlet;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.model.market.EmployerParticipant;
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
 * <b>Persists nothing (LA-40).</b> Every row this class reads lives only in the returned
 * {@link Result} for one request's render; no {@code EntityManager} write happens anywhere in this
 * class, no {@code audit_run} row, no cache. The one {@link EntityManager} parameter is read-only
 * ({@code em.find(Prospect.class, ...)} and {@link EmployerParticipantDAO#findByProspectId}).
 */
public final class SummitDemographicsVerifyService {

    private static final String CFG_PREFIX = "SUMMIT_AUDIT_PARTICIPANT_EXPORT_PREFIX";
    private static final String CFG_IMPORT_DIR = "SUMMIT_SFTP_IMPORT_DIR";
    private static final String CFG_MAX_BYTES = "SUMMIT_AUDIT_EXPORT_MAX_BYTES";
    private static final long DEFAULT_MAX_BYTES = 16_777_216L;

    private static final String H_PARTICIPANT_CUSTOM_ID = "ParticipantCustomID";
    private static final String H_EMPLOYER_CUSTOM_ID = "EmployerCustomID";
    private static final String H_USER_STATUS = "UserStatus";

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

    /** One expected participant's outcome. {@code userStatus} is null when {@code state} is
     *  {@link ParticipantState#MISSING} -- there is no export row to read it from. */
    public record ParticipantCheck(Long employerParticipantId, String firstName, String lastName,
                                    String expectedKey, ParticipantState state, String userStatus) {
    }

    /**
     * The compare's full outcome. {@code participants} and every count are empty/zero unless
     * {@code outcome} is {@link Outcome#OK}. {@code missingHeaderNames} is populated only for
     * {@link Outcome#MISSING_HEADERS}; {@code errorMessage} only for {@link Outcome#TRANSPORT_ERROR}.
     */
    public record Result(Outcome outcome, String fileName, LocalDateTime fileTimestamp,
                          List<String> missingHeaderNames, String errorMessage,
                          int expectedCount, int foundCount, int wrongEmployerCount,
                          int missingCount, int unkeyedCount, List<ParticipantCheck> participants) {

        static Result notFound() {
            return new Result(Outcome.NO_EXPORT_FOUND, null, null, List.of(), null,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result missingHeaders(String fileName, LocalDateTime fileTimestamp, List<String> missing) {
            return new Result(Outcome.MISSING_HEADERS, fileName, fileTimestamp, missing, null,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result prefixNotConfigured() {
            return new Result(Outcome.PREFIX_NOT_CONFIGURED, null, null, List.of(), null,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result prospectNotFound() {
            return new Result(Outcome.PROSPECT_NOT_FOUND, null, null, List.of(), null,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result exportDirNotConfigured() {
            return new Result(Outcome.EXPORT_DIR_NOT_CONFIGURED, null, null, List.of(), null,
                    0, 0, 0, 0, 0, List.of());
        }

        static Result transportError(String message) {
            return new Result(Outcome.TRANSPORT_ERROR, null, null, List.of(), message,
                    0, 0, 0, 0, 0, List.of());
        }
    }

    /**
     * Runs the compare for one setup. Reads only -- see the class Javadoc. Never throws; every
     * failure path (missing config, missing prospect, transport failure, missing headers) returns
     * a populated {@link Result} rather than propagating an exception.
     */
    public static Result verify(EntityManager em, long prospectId) {
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
        for (int r = 1; r < lines.size(); r++) {
            String[] row = lines.get(r);
            String participantCustomId = cellOf(row, index, H_PARTICIPANT_CUSTOM_ID);
            String rowEmployerCustomId = cellOf(row, index, H_EMPLOYER_CUSTOM_ID);
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

        return new Result(Outcome.OK, fetched.fileName(), fetched.fileTimestamp(), List.of(), null,
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

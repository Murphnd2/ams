package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.CensusRequestDAO;
import net.superiorstate.ams.data.dao.CensusSubmissionDAO;
import net.superiorstate.ams.model.market.CensusRequest;
import net.superiorstate.ams.model.market.CensusSubmission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * S47-C — the census-intake rules behind the request link, the public drop page and the setup
 * panel's step-3 status (T231 build 1, D45 decisions a–d). Build 2 (review, Load, Reject) adds to
 * this class; nothing here anticipates it beyond the states V100 already defines.
 * <p>
 * <b>The client's file is never stored.</b> {@link #accept} parses it from the stream with
 * {@link CensusParseService#parseLenient} and stages only the whitelisted fields as JSON
 * ({@code rows_json}); header names and field matches go to {@code mapping_json}. Neither ever
 * carries a value from an unrecognised column, so SSN, DOB and pay cannot be staged (LA-35, LA-41).
 * <p>
 * JSON is serialised with Gson — the library the rest of the codebase already uses.
 */
public final class CensusIntakeService {

    private static final Logger log = LogManager.getLogger(CensusIntakeService.class);

    /** A request link is usable for this many days after its last send. */
    public static final int EXPIRY_DAYS = 30;

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private CensusIntakeService() {}

    // ── Request lifecycle ───────────────────────────────────────────────

    /**
     * Returns the request to send for this proposal, creating or renewing as D45 decision c
     * requires: an OPEN, unexpired request is renewed in place (same token, expiry reset to now +
     * {@link #EXPIRY_DAYS}, sender fields updated); an OPEN but expired one is REVOKED and replaced;
     * no request, or a LOADED/REVOKED latest, gets a fresh one with a new UUID.
     */
    public static CensusRequest openOrRenew(EntityManager em, Long proposalId, String sentTo, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        CensusRequest latest = CensusRequestDAO.findLatestByProposalId(em, proposalId);

        if (latest != null && latest.isActive()) {
            latest.setExpiresAt(now.plusDays(EXPIRY_DAYS));
            latest.setSentTo(trimToNull(sentTo, 320));
            latest.setRequestedBy(userId);
            latest.setRequestedAt(now);
            CensusRequestDAO.update(em, latest);
            log.info("[CENSUS-INTAKE] Renewed request #{} for proposal {}", latest.getId(), proposalId);
            return latest;
        }

        if (latest != null && latest.isExpired()) {
            latest.setState(CensusRequest.STATE_REVOKED);
            latest.setClosedAt(now);
            latest.setClosedBy(userId);
            CensusRequestDAO.update(em, latest);
            log.info("[CENSUS-INTAKE] Expired request #{} for proposal {} revoked before reissue",
                    latest.getId(), proposalId);
        }

        CensusRequest fresh = new CensusRequest();
        fresh.setProposalId(proposalId);
        fresh.setToken(UUID.randomUUID().toString());
        fresh.setState(CensusRequest.STATE_OPEN);
        fresh.setSentTo(trimToNull(sentTo, 320));
        fresh.setRequestedBy(userId);
        fresh.setRequestedAt(now);
        fresh.setExpiresAt(now.plusDays(EXPIRY_DAYS));
        CensusRequestDAO.insert(em, fresh);
        log.info("[CENSUS-INTAKE] Opened request #{} for proposal {}", fresh.getId(), proposalId);
        return fresh;
    }

    /** Revokes a request and supersedes every open submission under it (NULLing their rows). */
    public static void revoke(EntityManager em, Long requestId, Long userId) {
        CensusRequest request = em.find(CensusRequest.class, requestId);
        if (request == null) return;
        request.setState(CensusRequest.STATE_REVOKED);
        request.setClosedAt(LocalDateTime.now());
        request.setClosedBy(userId);
        CensusRequestDAO.update(em, request);
        int superseded = CensusSubmissionDAO.supersedeOpen(em, requestId);
        log.info("[CENSUS-INTAKE] Revoked request #{} ({} open submission(s) superseded)", requestId, superseded);
    }

    /**
     * The request behind a token, only if it is OPEN and unexpired. Unknown, expired, revoked and
     * loaded tokens all come back empty — the public page shows one inactive state for all four.
     */
    public static Optional<CensusRequest> resolveActive(EntityManager em, String token) {
        CensusRequest request = CensusRequestDAO.findByToken(em, token);
        if (request == null || !request.isActive()) return Optional.empty();
        return Optional.of(request);
    }

    // ── Upload ──────────────────────────────────────────────────────────

    /**
     * Accepts one client upload: supersedes anything still open under the request, parses the
     * stream leniently, and inserts a submission — PENDING with rows when readable, UNREADABLE with
     * the mapping report only when a required column is missing or the file could not be read.
     * <b>Nothing from the stream is written anywhere but {@code rows_json}/{@code mapping_json}.</b>
     *
     * @param filename the client's file name; only its last path segment is kept, and only for display
     */
    public static CensusSubmission accept(EntityManager em, CensusRequest request, String filename, InputStream in) {
        CensusSubmissionDAO.supersedeOpen(em, request.getId());

        CensusParseService.LenientResult result = CensusParseService.parseLenient(in, filename);

        CensusSubmission submission = new CensusSubmission();
        submission.setRequestId(request.getId());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setOriginalFilename(baseName(filename));
        submission.setMappingJson(GSON.toJson(mappingView(result)));
        if (result.isReadable()) {
            submission.setState(CensusSubmission.STATE_PENDING);
            submission.setRowCount(result.getRows().size());
            submission.setIssueCount(result.getIssueCount());
            submission.setRowsJson(GSON.toJson(rowsView(result.getRows())));
        } else {
            submission.setState(CensusSubmission.STATE_UNREADABLE);
            submission.setRowCount(0);
            submission.setIssueCount(0);
            submission.setRowsJson(null);
        }
        CensusSubmissionDAO.insert(em, submission);
        log.info("[CENSUS-INTAKE] Request #{}: submission #{} {} ({} rows, {} issues)",
                request.getId(), submission.getId(), submission.getState(),
                submission.getRowCount(), submission.getIssueCount());
        return submission;
    }

    /** Header names and field matches only — no values. Shape is stable for build 2 to read back. */
    private static Map<String, Object> mappingView(CensusParseService.LenientResult result) {
        CensusParseService.MappingReport mapping = result.getMapping();
        Map<String, Object> view = new LinkedHashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        for (CensusParseService.FieldMatch fm : mapping.getFields()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("field", fm.getField());
            f.put("label", fm.getLabel());
            f.put("header", fm.getHeader());
            f.put("required", fm.isRequired());
            fields.add(f);
        }
        view.put("fields", fields);
        view.put("unmatchedRequiredFields", mapping.getUnmatchedRequiredFields());
        view.put("ignoredColumns", mapping.getIgnoredColumns());
        view.put("skippedBlankRows", mapping.getSkippedBlankRows());
        view.put("dataRowCount", mapping.getDataRowCount());
        if (result.getFileError() != null) view.put("fileError", result.getFileError());
        return view;
    }

    /** Whitelisted fields per row, plus row number and issues. */
    private static List<Map<String, Object>> rowsView(List<CensusParseService.StagedRow> rows) {
        List<Map<String, Object>> view = new ArrayList<>(rows.size());
        for (CensusParseService.StagedRow row : rows) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("rowNumber", row.getRowNumber());
            r.put("values", row.getValues());
            r.put("issues", row.getIssues());
            view.add(r);
        }
        return view;
    }

    // ── Status ──────────────────────────────────────────────────────────

    /** What the setup panel's step-3 line and the request page show. Read-only view. */
    public static final class Status {
        private final CensusRequest request;
        private final CensusSubmission latestSubmission;

        Status(CensusRequest request, CensusSubmission latestSubmission) {
            this.request = request;
            this.latestSubmission = latestSubmission;
        }

        public boolean hasRequest() { return request != null; }
        public CensusRequest getRequest() { return request; }
        /** OPEN | LOADED | REVOKED, or null when no request exists. */
        public String getRequestState() { return request == null ? null : request.getState(); }
        public boolean isOpen() { return request != null && CensusRequest.STATE_OPEN.equals(request.getState()); }
        public boolean isActive() { return request != null && request.isActive(); }
        public boolean isExpired() { return request != null && request.isExpired(); }
        public LocalDateTime getRequestedAt() { return request == null ? null : request.getRequestedAt(); }
        public LocalDateTime getExpiresAt() { return request == null ? null : request.getExpiresAt(); }
        public LocalDateTime getClosedAt() { return request == null ? null : request.getClosedAt(); }
        /** The latest non-superseded submission, or null. */
        public CensusSubmission getLatestSubmission() { return latestSubmission; }
        public boolean hasSubmission() { return latestSubmission != null; }
    }

    /**
     * The latest request for a proposal and its latest non-superseded submission. Never throws on
     * a missing row — both parts are nullable and the fragment renders accordingly.
     */
    public static Status statusFor(EntityManager em, Long proposalId) {
        CensusRequest request = CensusRequestDAO.findLatestByProposalId(em, proposalId);
        CensusSubmission latest = null;
        if (request != null) {
            for (CensusSubmission s : CensusSubmissionDAO.findByRequestId(em, request.getId())) {
                if (!CensusSubmission.STATE_SUPERSEDED.equals(s.getState())) { latest = s; break; }
            }
        }
        return new Status(request, latest);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** Last path segment of a client-supplied file name, capped to the column width. Never a path. */
    static String baseName(String filename) {
        if (filename == null) return null;
        String name = filename;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.trim();
        if (name.isEmpty()) return null;
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private static String trimToNull(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}

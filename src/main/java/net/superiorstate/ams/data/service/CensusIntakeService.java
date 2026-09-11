package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.dao.CensusRequestDAO;
import net.superiorstate.ams.data.dao.CensusSubmissionDAO;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.data.dao.SummitSetupStepDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.market.CensusRequest;
import net.superiorstate.ams.model.market.CensusSubmission;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.sales.agency.Proposal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
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

    /**
     * S47-F — {@code ActivityStatus} and {@code ReasonCreated} ids. Both are system reference rows
     * seeded identically on every installation by {@code DatabaseInitializer} — {@code ActivityStatus}
     * at :276-278 (Waiting on Them / No Change / Waiting on Us), {@code ReasonCreated} at :322-329
     * (Internal Note / … / Sent Email Message) — not PSP-scoped rows an admin creates, so rule 4's
     * cross-installation risk does not apply here the way it does to a {@code ServiceItem} or
     * {@code PlanType} id. {@code SendProposal} and {@code AddNoteToActivity25} already depend on
     * these same ids; this is the same practice, named as constants in one place instead of scattered
     * literals. Reversal: swap these for a by-description lookup.
     */
    public static final int STATUS_WAITING_ON_THEM = 1;
    public static final int STATUS_NO_CHANGE = 2;
    public static final int STATUS_WAITING_ON_US = 3;
    public static final int REASON_INTERNAL_NOTE = 1;
    public static final int REASON_RECEIVED_EMAIL = 4;

    /**
     * S47-F — the demographics step key {@code SummitResponseServlet} uses ({@code :67},
     * {@code STEP_DEMOGRAPHICS}). That constant is {@code private}, so this is the same literal
     * string rather than a cross-class reference; the two must be kept in agreement by hand.
     */
    private static final String STEP_DEMOGRAPHICS = "demographics";

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

    // ── Review, Load, Reject (S47-F, T231 build 2, D45 e–f) ──────────────

    /** The reviewable submission for a proposal — its latest request plus that request's latest PENDING/UNREADABLE submission — or empty when nothing is awaiting review. */
    public static Optional<CensusSubmission> reviewable(EntityManager em, Long proposalId) {
        CensusRequest request = CensusRequestDAO.findLatestByProposalId(em, proposalId);
        if (request == null) return Optional.empty();
        CensusSubmission submission = CensusSubmissionDAO.findLatestReviewable(em, request.getId());
        return Optional.ofNullable(submission);
    }

    /**
     * True when Demographics has been pushed to Summit or marked done for this proposal — the D45 f
     * guard, shared by {@link #load} (replacement) and {@code CensusUploadServlet.handleClear}
     * (Step 8). Either signal is sufficient: a {@code DONE} step row (Mark done, reviewed or manual)
     * or a {@code summit_file_export} row of type {@code demographics} with {@code delivery_status
     * = PUSHED} — see s47e Q8. {@code findLatestPushed} requires a non-null {@code pspId}, so a
     * session that could not resolve one is treated as "push unknown" rather than "not pushed";
     * the step-state check does not depend on {@code pspId} and still applies.
     */
    public static boolean demographicsSettled(EntityManager em, Long pspId, Long proposalId) {
        var step = SummitSetupStepDAO.findByProposalAndStep(em, pspId, proposalId, STEP_DEMOGRAPHICS);
        if (step != null && "DONE".equals(step.getState())) return true;
        return SummitSetupStepDAO.findLatestPushed(em, pspId, proposalId, STEP_DEMOGRAPHICS) != null;
    }

    /** The outcome of {@link #load}. */
    public static final class LoadResult {
        private final boolean ok;
        private final String message;
        private final int loadedCount;

        private LoadResult(boolean ok, String message, int loadedCount) {
            this.ok = ok;
            this.message = message;
            this.loadedCount = loadedCount;
        }

        static LoadResult refused(String message) { return new LoadResult(false, message, 0); }
        static LoadResult ok(int loadedCount) { return new LoadResult(true, null, loadedCount); }

        public boolean isOk() { return ok; }
        /** Set only when {@link #isOk()} is false, or on the partial-failure case described in step 5. */
        public String getMessage() { return message; }
        public int getLoadedCount() { return loadedCount; }
    }

    /**
     * Loads a submission's staged rows into {@code employer_participant}, following D45 e–f.
     * <ol>
     *   <li>The submission must be {@code PENDING}, belong to the proposal's latest request, and
     *       carry zero issues — a row with issues is never silently dropped from the load.</li>
     *   <li>If a roster already exists: refuse when {@link #demographicsSettled} is true (D45 f);
     *       otherwise require {@code replaceConfirmed}; otherwise clear it.</li>
     *   <li>Build {@link EmployerParticipant} rows exactly as {@code CensusUploadServlet:252-270}
     *       does, from the staged {@code values} map (the {@code F_*} keys), and insert.</li>
     *   <li>Only after a successful insert: close the submission {@code LOADED}, close the request
     *       {@code LOADED}, and log an internal note (Waiting on Us stays as it was — this is
     *       {@code STATUS_NO_CHANGE} in {@code Note} terms because the PSP admin, not the client,
     *       is acting).</li>
     *   <li>If insert fails after a clear, the submission is left {@code PENDING} with its rows
     *       intact so Load can be retried — the clear already happened and is not undone.</li>
     * </ol>
     */
    public static LoadResult load(EntityManager em, Long proposalId, Long submissionId,
                                  LocalDate effectiveDate, boolean replaceConfirmed,
                                  Person reviewer, String createdByName, Long pspId) {
        CensusRequest request = CensusRequestDAO.findLatestByProposalId(em, proposalId);
        CensusSubmission submission = CensusSubmissionDAO.findById(em, submissionId);
        if (request == null || submission == null || !request.getId().equals(submission.getRequestId())) {
            return LoadResult.refused("That upload is no longer available for review.");
        }
        if (!CensusSubmission.STATE_PENDING.equals(submission.getState())) {
            return LoadResult.refused("That upload is " + submission.getState().toLowerCase()
                    + " and can no longer be loaded.");
        }
        if (submission.getIssueCount() > 0) {
            return LoadResult.refused("Fix or reject: " + submission.getIssueCount() + " rows have issues.");
        }

        long existing = EmployerParticipantDAO.countByProspectId(em, prospectIdFor(em, proposalId));
        if (existing > 0) {
            if (demographicsSettled(em, pspId, proposalId)) {
                return LoadResult.refused("The roster can't be replaced: Demographics has been"
                        + " pushed to Summit or marked done for this setup. Participant ids are"
                        + " Summit identities from that point.");
            }
            if (!replaceConfirmed) {
                return LoadResult.refused("Confirm replacing the current " + existing + "-participant roster.");
            }
            EmployerParticipantDAO.deleteByProspectId(em, prospectIdFor(em, proposalId));
        }

        List<StagedRow> rows = readStagedRows(submission.getRowsJson());
        Long prospectId = prospectIdFor(em, proposalId);
        LocalDateTime now = LocalDateTime.now();
        List<EmployerParticipant> participants = new ArrayList<>(rows.size());
        for (StagedRow row : rows) {
            EmployerParticipant p = new EmployerParticipant();
            p.setProspectId(prospectId);
            p.setFirstName(row.values.get(CensusParseService.F_FIRST_NAME));
            p.setLastName(row.values.get(CensusParseService.F_LAST_NAME));
            p.setAddressLine1(row.values.get(CensusParseService.F_ADDRESS_LINE1));
            p.setAddressLine2(row.values.get(CensusParseService.F_ADDRESS_LINE2));
            p.setCity(row.values.get(CensusParseService.F_CITY));
            p.setState(row.values.get(CensusParseService.F_STATE));
            p.setPostalCode(row.values.get(CensusParseService.F_POSTAL_CODE));
            p.setEmail(row.values.get(CensusParseService.F_EMAIL));
            p.setEffectiveDate(effectiveDate);
            p.setCreatedAt(now);
            p.setCreatedBy(createdByName);
            participants.add(p);
        }

        try {
            EmployerParticipantDAO.insertAll(em, participants);
        } catch (RuntimeException e) {
            // The clear (if any) already happened and is not undone -- the submission is left
            // PENDING with its rows intact so Load can be retried without another client upload.
            log.error("[CENSUS-INTAKE] Load failed for proposal {} submission {}: {}",
                    proposalId, submissionId, e.getMessage());
            return LoadResult.refused("The roster was cleared but the load failed and nothing was"
                    + " inserted: " + e.getMessage() + ". Run Load again.");
        }

        Long reviewerId = reviewer == null ? null : reviewer.getId();
        CensusSubmissionDAO.close(em, submission.getId(), CensusSubmission.STATE_LOADED, reviewerId, null);
        request.setState(CensusRequest.STATE_LOADED);
        request.setClosedAt(LocalDateTime.now());
        request.setClosedBy(reviewerId);
        CensusRequestDAO.update(em, request);
        logToSetup(em, proposalId, reviewer, STATUS_NO_CHANGE, REASON_INTERNAL_NOTE,
                "Census loaded from client upload: " + participants.size() + " participants.");

        log.info("[CENSUS-INTAKE] Loaded {} participants for proposal {} from submission {}",
                participants.size(), proposalId, submissionId);
        return LoadResult.ok(participants.size());
    }

    /**
     * Rejects a submission ({@code PENDING} or {@code UNREADABLE}), keeping the request's token but
     * extending its expiry so the client can upload a corrected file at the same link.
     */
    public static void reject(EntityManager em, Long proposalId, Long submissionId, String note, Person reviewer) {
        CensusRequest request = CensusRequestDAO.findLatestByProposalId(em, proposalId);
        CensusSubmission submission = CensusSubmissionDAO.findById(em, submissionId);
        if (request == null || submission == null || !request.getId().equals(submission.getRequestId())) return;

        Long reviewerId = reviewer == null ? null : reviewer.getId();
        CensusSubmissionDAO.close(em, submission.getId(), CensusSubmission.STATE_REJECTED, reviewerId, note);

        request.setExpiresAt(LocalDateTime.now().plusDays(EXPIRY_DAYS));
        CensusRequestDAO.update(em, request);

        String noteText = (note == null || note.isBlank()) ? "(no note)" : note;
        logToSetup(em, proposalId, reviewer, STATUS_WAITING_ON_THEM, REASON_INTERNAL_NOTE,
                "Client census upload rejected: " + noteText);

        log.info("[CENSUS-INTAKE] Rejected submission {} for proposal {}", submissionId, proposalId);
    }

    /**
     * S47-F — writes a plain {@code Note} to the proposal's Setup activity (the same activity
     * {@code CensusRequestServlet.logEmailToActivity} logs the request email to), or its proposal's
     * source activity when no Setup is found. Never throws — a logging failure must not fail the
     * upload, the load or the rejection it accompanies. {@code statusId} must resolve to a real
     * {@code ActivityStatus} row (see class note on {@link #STATUS_WAITING_ON_THEM} etc.) or nothing
     * is written, because {@code Activity.getOnUs()} dereferences a note's status unguarded.
     */
    public static void logToSetup(EntityManager em, Long proposalId, Person author,
                                  int statusId, int reasonId, String detail) {
        try {
            Activity activity = findSetup(em, proposalId);
            if (activity == null) {
                Proposal proposal = em.find(Proposal.class, proposalId);
                if (proposal != null && proposal.getSourceActivity() != null) {
                    activity = EntityLookup.getActivityById(em, proposal.getSourceActivity().getId());
                }
            }
            if (activity == null) return;

            var status = EntityLookup.getActivityStatusById(em, statusId);
            if (status == null) {
                log.warn("[CENSUS-INTAKE] ActivityStatus {} not found; note not written for proposal {}",
                        statusId, proposalId);
                return;
            }

            em.getTransaction().begin();
            Note note = new Note();
            note.setActivity(activity);
            note.setDateGenerated(Date.valueOf(LocalDate.now()));
            note.setStatus(status);
            note.setReasonCreated(EntityLookup.getReasonById(em, reasonId));
            note.setCreatedBy(author);
            note.setDetail(detail);
            em.persist(note);
            activity.addNote(note);
            em.persist(activity);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            log.warn("[CENSUS-INTAKE] Could not log to setup activity for proposal {}: {}",
                    proposalId, e.getMessage());
        }
    }

    /** Same query {@code CensusRequestServlet.findSetup} uses: the Setup whose application's proposal is {@code proposalId}. */
    private static Activity findSetup(EntityManager em, Long proposalId) {
        try {
            Query q = em.createQuery("SELECT s FROM Setup s WHERE s.application.proposal.id = :pid");
            q.setParameter("pid", proposalId);
            q.setMaxResults(1);
            @SuppressWarnings("unchecked")
            List<Setup> setups = (List<Setup>) q.getResultList();
            return setups.isEmpty() ? null : setups.get(0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Long prospectIdFor(EntityManager em, Long proposalId) {
        Proposal proposal = em.find(Proposal.class, proposalId);
        return proposal == null || proposal.getProspect() == null ? null : proposal.getProspect().getId();
    }

    /** One staged row read back from {@code rows_json}: field values only, keyed by the {@code F_*} constants. */
    private static final class StagedRow {
        final Map<String, String> values;
        StagedRow(Map<String, String> values) { this.values = values; }
    }

    /** Reads {@code rows_json} back into {@link StagedRow}s. Malformed JSON yields an empty list rather than throwing. */
    private static List<StagedRow> readStagedRows(String rowsJson) {
        List<StagedRow> rows = new ArrayList<>();
        if (rowsJson == null) return rows;
        try {
            for (JsonElement e : JsonParser.parseString(rowsJson).getAsJsonArray()) {
                JsonObject obj = e.getAsJsonObject();
                Map<String, String> values = new LinkedHashMap<>();
                if (obj.has("values")) {
                    JsonObject v = obj.getAsJsonObject("values");
                    for (String key : v.keySet()) {
                        if (!v.get(key).isJsonNull()) values.put(key, v.get(key).getAsString());
                    }
                }
                rows.add(new StagedRow(values));
            }
        } catch (RuntimeException e) {
            log.warn("[CENSUS-INTAKE] Could not parse staged rows: {}", e.getMessage());
        }
        return rows;
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

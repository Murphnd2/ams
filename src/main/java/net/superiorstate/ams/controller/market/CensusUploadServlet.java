package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.service.CensusIntakeService;
import net.superiorstate.ams.data.service.CensusParseService;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * S26-C — loads an employer's census onto the AMS-owned participant roster
 * ({@code employer_participant}, V094). <b>Emits no Summit file.</b> Files 4 (Demographics)
 * and 5 (HRA Enrollment) read this roster and are a separate build item.
 * <p>
 * PSP-admin-only and ICHRA-gated, reachable only from the Setup screen's Census Upload button
 * — no nav entry, no menu link. Gating mirrors {@code SummitExportServlet:63-67} (session
 * {@code isPspAdmin}) and {@code SummitExportServlet:89-93}
 * ({@code IchraAccessResolver.isAvailable}), in that order, so the two surfaces of this flow
 * can never disagree about who may reach them.
 * <p>
 * <b>The uploaded file is never written to disk</b> — not to {@code AMS_UPLOAD_DIR}, not to a
 * temp path, not as a {@code .part} file. It is parsed straight from the multipart
 * {@code Part}'s stream and discarded. This is names and home addresses; nothing about the raw
 * file needs to survive the request. That is a deliberate divergence from
 * {@code UploadCsvServlet}, which stages billing files on the filesystem.
 * <p>
 * <b>A loaded roster refuses replacement rather than merging</b> (LA-34). Re-uploading would
 * assign new participant ids, orphaning any Summit record already keyed on the old ones — a
 * refusal is recoverable, a silent re-key is not.
 */
@WebServlet(name = "CensusUploadServlet", value = "/CensusUpload")
@MultipartConfig(
        maxFileSize = 5 * 1024 * 1024,      // 5 MB — a 5,000-row census is far below this
        maxRequestSize = 10 * 1024 * 1024   // 10 MB total
)
public class CensusUploadServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(CensusUploadServlet.class);

    private static final String VIEW = "/WEB-INF/view/market/censusUpload25.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Resolved resolved = resolve(request, response, em);
            if (resolved == null) return;   // resolve() has already answered

            renderForm(request, response, em, resolved, null, null);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Resolved resolved = resolve(request, response, em);
            if (resolved == null) return;

            String action = request.getParameter("action");
            if ("clear".equals(action)) {
                handleClear(request, response, em, resolved);
            } else {
                handleUpload(request, response, em, resolved);
            }
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    // ── Gating and resolution ────────────────────────────────────────────

    /** Mirrors {@code SummitExportServlet:63} exactly — the session flag, not a role lookup. */
    private static boolean isPspAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
    }

    /** A proposal that resolved all the way to a prospect. Nothing partial is ever carried. */
    private static class Resolved {
        final long proposalId;
        final Prospect prospect;
        Resolved(long proposalId, Prospect prospect) {
            this.proposalId = proposalId;
            this.prospect = prospect;
        }
    }

    /**
     * Resolves {@code proposalId → Proposal → Prospect}, mirroring
     * {@code SummitExportServlet:70-101}. Refuses by name at every step rather than guessing:
     * 400 on a missing or non-numeric id, 404 on an unknown proposal, 400 on a proposal that
     * reaches no prospect.
     *
     * @return null when the response has already been written, in which case the caller returns.
     */
    private Resolved resolve(HttpServletRequest request, HttpServletResponse response,
                             EntityManager em) throws IOException {
        String proposalIdParam = request.getParameter("proposalId");
        if (proposalIdParam == null || proposalIdParam.isBlank()) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "proposalId is required.");
            return null;
        }
        long proposalId;
        try {
            proposalId = Long.parseLong(proposalIdParam.trim());
        } catch (NumberFormatException e) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid proposalId.");
            return null;
        }

        Proposal proposal = em.find(Proposal.class, proposalId);
        if (proposal == null) {
            writePlainError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Proposal " + proposalId + " not found.");
            return null;
        }
        Prospect prospect = proposal.getProspect();
        if (prospect == null || prospect.getId() == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Proposal " + proposalId + " has no employer (prospect) to load a census"
                            + " against. The census is keyed to the employer, so there is nothing"
                            + " to attach these participants to.");
            return null;
        }
        return new Resolved(proposalId, prospect);
    }

    // ── Actions ──────────────────────────────────────────────────────────

    private void handleUpload(HttpServletRequest request, HttpServletResponse response,
                              EntityManager em, Resolved resolved)
            throws ServletException, IOException {

        // Replacement is refused, not merged (LA-34) — but the refusal is decided AFTER parsing,
        // below. Refusing first meant the operator had to destroy a good roster before they could
        // learn whether the replacement file was even parseable. The multipart limits on the
        // class annotation still bound what is read, so parsing first does not widen the upload.
        long existing = EmployerParticipantDAO.countByProspectId(em, resolved.prospect.getId());

        // The effective date is only ever consumed by an insert. On a replacement attempt the
        // insert is refused regardless, so demanding a date before reporting on the file would be
        // friction for no gain.
        LocalDate effectiveDate = null;
        if (existing == 0) {
            String effectiveDateParam = request.getParameter("effectiveDate");
            if (effectiveDateParam != null && !effectiveDateParam.isBlank()) {
                try {
                    effectiveDate = LocalDate.parse(effectiveDateParam.trim());
                } catch (DateTimeParseException e) {
                    effectiveDate = null;
                }
            }
            if (effectiveDate == null) {
                renderForm(request, response, em, resolved,
                        List.of(new CensusParseService.RowError(0, "effectiveDate",
                                "An effective date is required, and applies to every participant"
                                        + " in the file.")),
                        null);
                return;
            }
        }

        Part filePart = request.getPart("censusFile");
        if (filePart == null || filePart.getSize() == 0
                || filePart.getSubmittedFileName() == null
                || filePart.getSubmittedFileName().isBlank()) {
            renderForm(request, response, em, resolved,
                    List.of(new CensusParseService.RowError(0, "censusFile",
                            "Choose a census file to upload.")),
                    null);
            return;
        }

        // Parsed straight from the stream. The bytes are never written anywhere.
        CensusParseService.Result result;
        try (InputStream in = filePart.getInputStream()) {
            result = CensusParseService.parse(in, filePart.getSubmittedFileName());
        } finally {
            try { filePart.delete(); } catch (Exception ignore) { /* container temp, best effort */ }
        }

        if (!result.isSuccess()) {
            // Nothing is inserted on any failure — all or nothing. The mapping report still goes
            // to the page: a missing-header failure is exactly when someone needs to see what did
            // match.
            request.setAttribute("mapping", result.getMapping());
            renderForm(request, response, em, resolved, result.getErrors(), null);
            return;
        }
        request.setAttribute("mapping", result.getMapping());

        // The file is good. If a roster is already loaded, refuse the insert here — after the
        // operator has been shown what the submitted file contained, so they can decide whether
        // clearing the current roster is worth it. insertAll is deliberately not reached.
        if (existing > 0) {
            request.setAttribute("refusedExisting", existing);
            request.setAttribute("refusedSubmitted", result.getRows().size());
            log.info("[CENSUS-UPLOAD] Refused replacement for prospect {} (proposal {}): {} loaded,"
                            + " {} submitted and parsed cleanly; nothing inserted",
                    resolved.prospect.getId(), resolved.proposalId, existing,
                    result.getRows().size());
            renderForm(request, response, em, resolved, null, null);
            return;
        }

        String createdBy = resolveCreatedBy(request);
        LocalDateTime now = LocalDateTime.now();
        List<EmployerParticipant> participants = new ArrayList<>(result.getRows().size());
        for (CensusParseService.CensusRow row : result.getRows()) {
            EmployerParticipant p = new EmployerParticipant();
            p.setProspectId(resolved.prospect.getId());
            p.setFirstName(row.getFirstName());
            p.setLastName(row.getLastName());
            p.setAddressLine1(row.getAddressLine1());
            p.setAddressLine2(row.getAddressLine2());
            p.setCity(row.getCity());
            p.setState(row.getState());
            p.setPostalCode(row.getPostalCode());
            p.setEmail(row.getEmail());
            p.setEffectiveDate(effectiveDate);
            p.setCreatedAt(now);
            p.setCreatedBy(createdBy);
            participants.add(p);
        }

        try {
            EmployerParticipantDAO.insertAll(em, participants);
        } catch (RuntimeException e) {
            log.error("[CENSUS-UPLOAD] Insert failed for prospect {}: {}",
                    resolved.prospect.getId(), e.getMessage());
            renderForm(request, response, em, resolved,
                    List.of(new CensusParseService.RowError(0, null,
                            "The roster could not be saved and nothing was inserted: "
                                    + e.getMessage())),
                    null);
            return;
        }

        log.info("[CENSUS-UPLOAD] Loaded {} participants for prospect {} (proposal {})",
                participants.size(), resolved.prospect.getId(), resolved.proposalId);
        renderForm(request, response, em, resolved, null,
                "Loaded " + participants.size() + " participants.");
    }

    private void handleClear(HttpServletRequest request, HttpServletResponse response,
                             EntityManager em, Resolved resolved)
            throws ServletException, IOException {

        // An explicit confirmation parameter, not a bare POST — clearing reassigns ids on any
        // re-upload and orphans Summit records keyed on the old ones (LA-33/LA-34).
        if (!"yes".equals(request.getParameter("confirmClear"))) {
            renderForm(request, response, em, resolved,
                    List.of(new CensusParseService.RowError(0, null,
                            "Clearing the roster was not confirmed. Nothing was removed.")),
                    null);
            return;
        }

        // S47-F, D45 f -- the roster can be replaced until Demographics is pushed to Summit or
        // marked done for this setup; past that point participant ids are Summit identities and
        // clearing them would orphan those records (LA-33/LA-34). Same guard CensusIntakeService.load
        // applies to a review-page replacement.
        Long pspId = resolveCurrentPspId(request);
        if (CensusIntakeService.demographicsSettled(em, pspId, resolved.proposalId)) {
            renderForm(request, response, em, resolved,
                    List.of(new CensusParseService.RowError(0, null,
                            "The roster can't be cleared: Demographics has been pushed to Summit"
                                    + " or marked done for this setup. Participant ids are Summit"
                                    + " identities from that point.")),
                    null);
            return;
        }

        int removed;
        try {
            removed = EmployerParticipantDAO.deleteByProspectId(em, resolved.prospect.getId());
        } catch (RuntimeException e) {
            log.error("[CENSUS-UPLOAD] Clear failed for prospect {}: {}",
                    resolved.prospect.getId(), e.getMessage());
            renderForm(request, response, em, resolved,
                    List.of(new CensusParseService.RowError(0, null,
                            "The roster could not be cleared: " + e.getMessage())),
                    null);
            return;
        }

        log.info("[CENSUS-UPLOAD] Cleared {} participants for prospect {} (proposal {})",
                removed, resolved.prospect.getId(), resolved.proposalId);
        renderForm(request, response, em, resolved, null,
                "Cleared " + removed + " participants.");
    }

    // ── Rendering ────────────────────────────────────────────────────────

    private void renderForm(HttpServletRequest request, HttpServletResponse response,
                            EntityManager em, Resolved resolved,
                            List<CensusParseService.RowError> errors, String notice)
            throws ServletException, IOException {

        request.setAttribute("proposalId", resolved.proposalId);
        request.setAttribute("employerName", resolved.prospect.getName());
        request.setAttribute("participants",
                EmployerParticipantDAO.findByProspectId(em, resolved.prospect.getId()));
        request.setAttribute("errors", errors);
        request.setAttribute("notice", notice);

        // EM stays open through the forward — the JSP walks the participant list.
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /**
     * S47-F — same pattern as {@code SummitResponseServlet.resolveCurrentPspId}, copied here for
     * the D45 f Clear guard ({@code CensusIntakeService.demographicsSettled} needs a {@code pspId}
     * to check a pushed export).
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** Best-effort attribution for {@code created_by}; never fails the upload. */
    private static String resolveCreatedBy(HttpServletRequest request) {
        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (local != null && local.getCurrentPerson() != null) {
                String name = local.getCurrentPerson().getFullName();
                if (name != null && !name.isBlank()) {
                    return name.length() > 100 ? name.substring(0, 100) : name;
                }
            }
        } catch (Exception ignore) { /* attribution is not worth failing an insert over */ }
        return null;
    }

    private void writePlainError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
    }
}

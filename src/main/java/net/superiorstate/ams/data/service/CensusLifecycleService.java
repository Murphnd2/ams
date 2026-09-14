package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.model.market.CensusRequest;
import net.superiorstate.ams.model.market.CensusSubmission;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * S59-P6rev -- read-only: which of the merged Census row's states applies to one setup? Backs the
 * Summit setup panel's single `Census` row (`detailSummitSetup25.jsp`), via
 * {@code CensusLifecycleServlet}. Replaces the three separate rows -- Request census, Census,
 * Demographics -- with one row whose available actions this service's state token gates.
 * <p>
 * Consumes existing accessors only, never re-queries {@code census_request} / {@code
 * census_submission} / {@code employer_participant} / {@code summit_setup_step} directly:
 * {@link CensusIntakeService#statusFor} for the request/submission state, {@link
 * EmployerParticipantDAO#countByProspectId} for roster presence (the same route {@code
 * CensusUploadServlet} uses), and {@link CensusIntakeService#demographicsSettled} for the terminal
 * gate.
 * <p>
 * <b>Roster presence stands in for a missing request.</b> A PSP-side upload through {@code
 * /CensusUpload} writes no {@code census_request} row and no {@code census_submission} row -- without
 * a roster check, a roster loaded by hand would sit at {@link State#NOT_REQUESTED} forever. So {@link
 * State#ROSTER_LOADED} is reached either by the request itself reaching {@code LOADED} or by roster
 * rows existing regardless of request state.
 * <p>
 * Precedence, highest first: {@link State#TERMINAL} &gt; {@link State#ROSTER_LOADED} &gt; {@link
 * State#AWAITING_REVIEW} &gt; {@link State#REVOKED} &gt; {@link State#LINK_EXPIRED} &gt; {@link
 * State#AWAITING_CLIENT} &gt; {@link State#NOT_REQUESTED}.
 * <p>
 * {@link State#INDETERMINATE} on any failure -- no PSP, no proposal, no prospect, or any exception.
 * Callers must fail open: render every action the merged row can ever offer, never hide a control
 * because a lookup failed. Read-only: no write, no flush, no transaction started here.
 */
public final class CensusLifecycleService {

    private static final Logger log = LogManager.getLogger(CensusLifecycleService.class);

    public enum State {
        /** No {@code census_request} row, and no roster rows. */
        NOT_REQUESTED,
        /** Request {@code OPEN} and {@link CensusRequest#isActive()}. */
        AWAITING_CLIENT,
        /** Request {@code OPEN} and {@link CensusRequest#isExpired()}. */
        LINK_EXPIRED,
        /** Request state {@code REVOKED}. */
        REVOKED,
        /** Latest non-superseded submission {@code PENDING} or {@code UNREADABLE}. */
        AWAITING_REVIEW,
        /** Request {@code LOADED}, or roster rows exist regardless of request state. */
        ROSTER_LOADED,
        /** {@link CensusIntakeService#demographicsSettled} true -- Done (reviewed/manual) or Pushed. */
        TERMINAL,
        /** No PSP, no proposal, no prospect, or any failure resolving any of the above. Callers
         *  render the full legacy action set -- never hide a control on an indeterminate read. */
        INDETERMINATE
    }

    private CensusLifecycleService() {}

    /**
     * @param em         an open {@code EntityManager}
     * @param pspId      the acting PSP, or {@code null} if unresolved -- {@code null} is
     *                   {@link State#INDETERMINATE}, not an error, matching the terminal gate's own
     *                   PSP scoping
     * @param proposalId the setup's proposal id
     * @return never {@code null}; any exception anywhere in resolution is caught and reported as
     * {@link State#INDETERMINATE} rather than propagated
     */
    public static State resolve(EntityManager em, Long pspId, long proposalId) {
        try {
            if (em == null || pspId == null) {
                return State.INDETERMINATE;
            }

            Proposal proposal = em.find(Proposal.class, proposalId);
            if (proposal == null) {
                return State.INDETERMINATE;
            }
            Prospect prospect = proposal.getProspect();
            if (prospect == null || prospect.getId() == null) {
                return State.INDETERMINATE;
            }

            // Terminal -- highest precedence, checked first so a settled Demographics file always
            // wins regardless of what the census_request/roster side still shows.
            if (CensusIntakeService.demographicsSettled(em, pspId, proposalId)) {
                return State.TERMINAL;
            }

            CensusIntakeService.Status status = CensusIntakeService.statusFor(em, proposalId);
            long rosterCount = EmployerParticipantDAO.countByProspectId(em, prospect.getId());

            // Roster loaded -- request LOADED, or roster rows present regardless of request state
            // (the PSP-side-upload-with-no-request case this class exists to catch).
            if (CensusRequest.STATE_LOADED.equals(status.getRequestState()) || rosterCount > 0) {
                return State.ROSTER_LOADED;
            }

            // Awaiting review -- latest non-superseded submission still PENDING or UNREADABLE.
            if (status.hasSubmission()) {
                String submissionState = status.getLatestSubmission().getState();
                if (CensusSubmission.STATE_PENDING.equals(submissionState)
                        || CensusSubmission.STATE_UNREADABLE.equals(submissionState)) {
                    return State.AWAITING_REVIEW;
                }
            }

            // Revoked.
            if (CensusRequest.STATE_REVOKED.equals(status.getRequestState())) {
                return State.REVOKED;
            }

            // Link expired -- open request past its expiry.
            if (status.isOpen() && status.isExpired()) {
                return State.LINK_EXPIRED;
            }

            // Awaiting client -- open request still active.
            if (status.isOpen() && status.isActive()) {
                return State.AWAITING_CLIENT;
            }

            // Not requested -- no request row (status.getRequestState() is null here by
            // elimination) and no roster rows (already ruled out above).
            return State.NOT_REQUESTED;
        } catch (Exception e) {
            log.warn("[CENSUS-LIFECYCLE] resolution failed for proposalId={}, pspId={}: {}",
                    proposalId, pspId, e.getMessage());
            return State.INDETERMINATE;
        }
    }
}

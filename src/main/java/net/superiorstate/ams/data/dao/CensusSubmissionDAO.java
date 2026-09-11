package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.CensusSubmission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO for {@link CensusSubmission} (V100). Static methods, abstract class, same transaction
 * pattern as {@link SummitSetupStepDAO}. <b>Not an authorization boundary</b> — callers establish
 * access before calling; the public drop page reaches this only through
 * {@code CensusIntakeService.accept}, after {@code resolveActive} has admitted the token.
 */
public abstract class CensusSubmissionDAO {

    private static final String JPQL_BY_REQUEST =
            "SELECT s FROM CensusSubmission s WHERE s.requestId = :requestId " +
            "ORDER BY s.submittedAt DESC, s.id DESC";

    /**
     * Open rows for a request. Only PENDING and UNREADABLE are "open": SUPERSEDED, LOADED and
     * REJECTED are terminal and are never touched again.
     */
    private static final String JPQL_OPEN_BY_REQUEST =
            "SELECT s FROM CensusSubmission s WHERE s.requestId = :requestId " +
            "AND s.state IN ('" + CensusSubmission.STATE_PENDING + "', '" + CensusSubmission.STATE_UNREADABLE + "')";

    /** Every submission for a request, newest first. */
    public static List<CensusSubmission> findByRequestId(EntityManager em, Long requestId) {
        if (requestId == null) return List.of();
        return em.createQuery(JPQL_BY_REQUEST, CensusSubmission.class)
                .setParameter("requestId", requestId)
                .getResultList();
    }

    /** The newest submission for a request in any state, or {@code null}. */
    public static CensusSubmission findLatestByRequestId(EntityManager em, Long requestId) {
        if (requestId == null) return null;
        List<CensusSubmission> results = em.createQuery(JPQL_BY_REQUEST, CensusSubmission.class)
                .setParameter("requestId", requestId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /** One submission by id, or {@code null}. */
    public static CensusSubmission findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(CensusSubmission.class, id);
    }

    /**
     * S47-F — the newest submission for a request that is still awaiting a PSP-admin decision:
     * {@code PENDING} (readable, ready to Load) or {@code UNREADABLE} (ready to Reject only). Same
     * "open" definition {@link #JPQL_OPEN_BY_REQUEST} uses for {@link #supersedeOpen}, but ordered
     * newest-first and capped to one row, since only the latest open submission is ever actionable
     * — {@code supersedeOpen} guarantees at most one open row exists per request at a time.
     */
    public static CensusSubmission findLatestReviewable(EntityManager em, Long requestId) {
        if (requestId == null) return null;
        List<CensusSubmission> results = em.createQuery(
                        JPQL_OPEN_BY_REQUEST + " ORDER BY s.submittedAt DESC, s.id DESC",
                        CensusSubmission.class)
                .setParameter("requestId", requestId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Persists a new submission in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void insert(EntityManager em, CensusSubmission submission) {
        try {
            em.getTransaction().begin();
            em.persist(submission);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to record census submission: " + e.getMessage(), e);
        }
    }

    /**
     * Sets every PENDING / UNREADABLE submission for the request to SUPERSEDED and <b>NULLs
     * {@code rows_json}</b> — staged personal data does not outlive its pending review (LA-41).
     * Mutates managed entities inside one transaction rather than a bulk JPQL UPDATE so
     * EclipseLink's L2 cache stays coherent (the same reason {@link SummitSetupStepDAO#markDone}
     * finds-then-mutates). A no-op when nothing is open.
     *
     * @return how many rows were superseded
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static int supersedeOpen(EntityManager em, Long requestId) {
        if (requestId == null) return 0;
        try {
            em.getTransaction().begin();
            List<CensusSubmission> open = em.createQuery(JPQL_OPEN_BY_REQUEST, CensusSubmission.class)
                    .setParameter("requestId", requestId)
                    .getResultList();
            for (CensusSubmission s : open) {
                s.setState(CensusSubmission.STATE_SUPERSEDED);
                s.setRowsJson(null);
            }
            em.getTransaction().commit();
            return open.size();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to supersede census submissions: " + e.getMessage(), e);
        }
    }

    /**
     * S47-F — closes a submission with a PSP-admin decision: {@code state} is
     * {@link CensusSubmission#STATE_LOADED} or {@link CensusSubmission#STATE_REJECTED}. NULLs
     * {@code rows_json} the same way {@link #supersedeOpen} does — the decision is recorded, the
     * staged personal data is not kept past it (LA-41) — and stamps {@code reviewed_by}/
     * {@code reviewed_at}/{@code review_note}. Same find-then-mutate transaction pattern as
     * {@link SummitSetupStepDAO#markDone}.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void close(EntityManager em, Long id, String state, Long reviewerId, String note) {
        try {
            em.getTransaction().begin();
            CensusSubmission submission = em.find(CensusSubmission.class, id);
            if (submission != null) {
                submission.setState(state);
                submission.setRowsJson(null);
                submission.setReviewedBy(reviewerId);
                submission.setReviewedAt(LocalDateTime.now());
                submission.setReviewNote(note);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to close census submission: " + e.getMessage(), e);
        }
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.CensusRequest;

import java.util.List;

/**
 * DAO for {@link CensusRequest} (V100). All methods are static; the class is abstract (not
 * instantiable), following {@link SummitSetupStepDAO}'s shape and its transaction pattern (begin,
 * mutate, commit; rollback and rethrow on failure).
 * <p>
 * <b>Not an authorization boundary.</b> {@link #findByToken} is the public drop page's only lookup
 * and returns whatever row carries the token, in any state — {@code CensusIntakeService.resolveActive}
 * decides whether that row is usable. Authenticated callers establish PSP-admin and ICHRA access
 * before calling, exactly as {@code CensusUploadServlet} does.
 */
public abstract class CensusRequestDAO {

    private static final String JPQL_BY_TOKEN =
            "SELECT r FROM CensusRequest r WHERE r.token = :token";

    private static final String JPQL_LATEST_BY_PROPOSAL =
            "SELECT r FROM CensusRequest r WHERE r.proposalId = :proposalId " +
            "ORDER BY r.requestedAt DESC, r.id DESC";

    /** The request carrying this token in any state, or {@code null}. */
    public static CensusRequest findByToken(EntityManager em, String token) {
        if (token == null || token.isBlank()) return null;
        List<CensusRequest> results = em.createQuery(JPQL_BY_TOKEN, CensusRequest.class)
                .setParameter("token", token.trim())
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /** The most recently issued request for this proposal in any state, or {@code null}. */
    public static CensusRequest findLatestByProposalId(EntityManager em, Long proposalId) {
        if (proposalId == null) return null;
        List<CensusRequest> results = em.createQuery(JPQL_LATEST_BY_PROPOSAL, CensusRequest.class)
                .setParameter("proposalId", proposalId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Persists a new request in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void insert(EntityManager em, CensusRequest request) {
        try {
            em.getTransaction().begin();
            em.persist(request);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to record census request: " + e.getMessage(), e);
        }
    }

    /**
     * Flushes changes already made to a managed (or detached) request, in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void update(EntityManager em, CensusRequest request) {
        try {
            em.getTransaction().begin();
            em.merge(request);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to update census request: " + e.getMessage(), e);
        }
    }
}

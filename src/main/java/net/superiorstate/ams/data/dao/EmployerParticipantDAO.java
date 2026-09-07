package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.EmployerParticipant;

import java.util.List;

/**
 * DAO for {@link EmployerParticipant}. All methods are static; the class is abstract (not
 * instantiable) following the convention of {@link IllustrationLogDAO}, RateCacheDAO and
 * ChatbotSkillDAO.
 * <p>
 * <b>Not an authorization boundary.</b> Every method answers "what is on this prospect's
 * roster", nothing more — none of them knows who is asking. Callers must establish that the
 * caller may see the proposal that resolves to this prospect <em>before</em> calling, exactly
 * as {@code CensusUploadServlet} does with its PSP-admin and {@code IchraAccessResolver} gates.
 */
public abstract class EmployerParticipantDAO {

    private static final String JPQL_BY_PROSPECT =
            "SELECT p FROM EmployerParticipant p " +
            "WHERE p.prospectId = :prospectId " +
            "ORDER BY p.lastName, p.firstName, p.id";

    private static final String JPQL_COUNT_BY_PROSPECT =
            "SELECT COUNT(p) FROM EmployerParticipant p WHERE p.prospectId = :prospectId";

    private static final String JPQL_DELETE_BY_PROSPECT =
            "DELETE FROM EmployerParticipant p WHERE p.prospectId = :prospectId";

    /**
     * The roster for one prospect, ordered by last name, then first name, then id — id last so
     * two employees sharing a name still order deterministically, which the table deliberately
     * permits (V094 has no name uniqueness constraint).
     *
     * @return the matching rows, or an empty list when {@code prospectId} is null or nothing is
     * loaded against it. Never null.
     */
    public static List<EmployerParticipant> findByProspectId(EntityManager em, Long prospectId) {
        if (prospectId == null) return List.of();
        return em.createQuery(JPQL_BY_PROSPECT, EmployerParticipant.class)
                .setParameter("prospectId", prospectId)
                .getResultList();
    }

    /**
     * How many participants are loaded against one prospect. Used as the replacement guard —
     * a non-zero count refuses an upload rather than merging into it (LA-34).
     *
     * @return the count, or 0 when {@code prospectId} is null.
     */
    public static long countByProspectId(EntityManager em, Long prospectId) {
        if (prospectId == null) return 0L;
        return em.createQuery(JPQL_COUNT_BY_PROSPECT, Long.class)
                .setParameter("prospectId", prospectId)
                .getSingleResult();
    }

    /**
     * Persists a whole roster in <b>one transaction, all or nothing</b>. A census is a single
     * artifact — a partially loaded roster would emit a partial Demographics file to Summit and
     * look complete, so any failure rolls the whole list back and rethrows.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back. Callers report the
     * failure and insert nothing.
     */
    public static void insertAll(EntityManager em, List<EmployerParticipant> participants) {
        if (participants == null || participants.isEmpty()) return;
        try {
            em.getTransaction().begin();
            for (EmployerParticipant participant : participants) {
                em.persist(participant);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to insert participant roster: " + e.getMessage(), e);
        }
    }

    /**
     * Removes every participant loaded against one prospect, in its own transaction.
     * <p>
     * <b>Destructive and not reversible in-app.</b> Rows deleted here had AMS-generated ids that
     * a re-upload will not reissue, so any Summit participant already keyed on an old id is
     * orphaned (LA-33/LA-34). The caller is responsible for the explicit confirmation.
     *
     * @return the number of rows removed.
     */
    public static int deleteByProspectId(EntityManager em, Long prospectId) {
        if (prospectId == null) return 0;
        try {
            em.getTransaction().begin();
            int removed = em.createQuery(JPQL_DELETE_BY_PROSPECT)
                    .setParameter("prospectId", prospectId)
                    .executeUpdate();
            em.getTransaction().commit();
            // A bulk JPQL DELETE bypasses both the persistence context and EclipseLink's L2
            // cache, so a findByProspectId in the same request would otherwise return rows that
            // no longer exist. Evict the class, following ServiceManagerAction:482.
            em.clear();
            em.getEntityManagerFactory().getCache().evict(EmployerParticipant.class);
            return removed;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to clear participant roster: " + e.getMessage(), e);
        }
    }
}

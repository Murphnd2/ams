package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.EnrollmentMatrixParticipant;

import java.util.List;

/**
 * DAO for {@link EnrollmentMatrixParticipant} (V106). All methods are static; the class is
 * abstract (not instantiable), following {@link EnrollmentMatrixDAO}'s convention.
 * <p>
 * Nothing calls this yet — the matrix UI, the push mechanism, and any report are all later,
 * separate builds.
 */
public abstract class EnrollmentMatrixParticipantDAO {

    private static final String JPQL_BY_MATRIX =
            "SELECT p FROM EnrollmentMatrixParticipant p WHERE p.matrixId = :matrixId";

    private static final String JPQL_BY_MATRIX_AND_PARTICIPANT =
            "SELECT p FROM EnrollmentMatrixParticipant p " +
            "WHERE p.matrixId = :matrixId AND p.participantId = :participantId";

    /**
     * s52k — the newest non-blank {@code custom_schedule_name} in this matrix, for the "OTHER_CUSTOM"
     * prefill: when an admin picks OTHER_CUSTOM on an empty field, the page offers the most recently
     * entered schedule name from another participant row in the same matrix, always overridable.
     * Ordered by {@code id DESC} as a proxy for recency — this table has no {@code updated_at}, so an
     * edit to an older row cannot be distinguished from its original entry; newest-inserted is the
     * best available signal.
     */
    private static final String JPQL_MOST_RECENT_CUSTOM_SCHEDULE_NAME =
            "SELECT p.customScheduleName FROM EnrollmentMatrixParticipant p " +
            "WHERE p.matrixId = :matrixId AND p.customScheduleName IS NOT NULL AND p.customScheduleName <> '' " +
            "ORDER BY p.id DESC";

    /** One header row by primary key, or null. */
    public static EnrollmentMatrixParticipant findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(EnrollmentMatrixParticipant.class, id);
    }

    /** Every header row for one matrix. Never null; empty when {@code matrixId} is null. */
    public static List<EnrollmentMatrixParticipant> findByMatrixId(EntityManager em, Long matrixId) {
        if (matrixId == null) return List.of();
        return em.createQuery(JPQL_BY_MATRIX, EnrollmentMatrixParticipant.class)
                .setParameter("matrixId", matrixId)
                .getResultList();
    }

    /**
     * The header row already occupying a (matrix, participant) pair, or null — the duplicate
     * pre-check a future writer will need, following {@link SummitPlanTemplateMapDAO}'s and
     * {@link SummitServiceItemFlagsDAO}'s own {@code findByPsp*} precedent.
     */
    public static EnrollmentMatrixParticipant findByMatrixAndParticipant(EntityManager em, Long matrixId,
                                                                          Long participantId) {
        if (matrixId == null || participantId == null) return null;
        List<EnrollmentMatrixParticipant> found = em.createQuery(JPQL_BY_MATRIX_AND_PARTICIPANT, EnrollmentMatrixParticipant.class)
                .setParameter("matrixId", matrixId)
                .setParameter("participantId", participantId)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * The newest non-blank {@code custom_schedule_name} anywhere in this matrix, or null when
     * none exists yet. Never stored anywhere as a matrix-level default — recomputed on read.
     */
    public static String findMostRecentCustomScheduleName(EntityManager em, Long matrixId) {
        if (matrixId == null) return null;
        List<String> found = em.createQuery(JPQL_MOST_RECENT_CUSTOM_SCHEDULE_NAME, String.class)
                .setParameter("matrixId", matrixId)
                .setMaxResults(1)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Persists a new header row in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back — same contract
     * {@link EnrollmentMatrixDAO#insert} documents.
     */
    public static void insert(EntityManager em, EnrollmentMatrixParticipant participant) {
        try {
            em.getTransaction().begin();
            em.persist(participant);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to add enrollment matrix participant: " + e.getMessage(), e);
        }
    }

    /** Merges an edited header row in its own transaction. Same failure contract as {@link #insert}. */
    public static void update(EntityManager em, EnrollmentMatrixParticipant participant) {
        try {
            em.getTransaction().begin();
            em.merge(participant);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to save enrollment matrix participant: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one header row, in its own transaction.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Long id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            EnrollmentMatrixParticipant participant = em.find(EnrollmentMatrixParticipant.class, id);
            if (participant != null) em.remove(participant);
            em.getTransaction().commit();
            return participant != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove enrollment matrix participant: " + e.getMessage(), e);
        }
    }
}

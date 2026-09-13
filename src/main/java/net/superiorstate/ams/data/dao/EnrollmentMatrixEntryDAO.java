package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.EnrollmentMatrixEntry;
import net.superiorstate.ams.model.market.EnrollmentMatrixParticipant;

import java.util.List;

/**
 * DAO for {@link EnrollmentMatrixEntry} (V106). All methods are static; the class is abstract
 * (not instantiable), following {@link EnrollmentMatrixDAO}'s and
 * {@link EnrollmentMatrixParticipantDAO}'s convention.
 * <p>
 * Nothing calls this yet — the matrix UI, the push mechanism, and any report are all later,
 * separate builds.
 */
public abstract class EnrollmentMatrixEntryDAO {

    private static final String JPQL_BY_PARTICIPANT =
            "SELECT e FROM EnrollmentMatrixEntry e WHERE e.matrixParticipantId = :matrixParticipantId";

    private static final String JPQL_BY_PARTICIPANT_AND_LEG =
            "SELECT e FROM EnrollmentMatrixEntry e " +
            "WHERE e.matrixParticipantId = :matrixParticipantId AND e.planTemplateMapId = :planTemplateMapId";

    /**
     * S55-B — every detail row belonging to any header row of one matrix, in one query. This
     * entity carries no {@code matrix_id} column of its own (only {@code matrix_participant_id}),
     * so the join to the matrix is expressed as a subquery over
     * {@link EnrollmentMatrixParticipant#getMatrixId()} rather than a direct equality — added for
     * the enrollment-matrix exporter (build s55b), which needs every entry in a matrix at once
     * rather than one {@link #findByMatrixParticipantId} call per header row.
     */
    private static final String JPQL_BY_MATRIX =
            "SELECT e FROM EnrollmentMatrixEntry e WHERE e.matrixParticipantId IN (" +
            "SELECT p.id FROM EnrollmentMatrixParticipant p WHERE p.matrixId = :matrixId)";

    /** One detail row by primary key, or null. */
    public static EnrollmentMatrixEntry findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(EnrollmentMatrixEntry.class, id);
    }

    /** Every detail row for one participant header row. Never null; empty when the id is null. */
    public static List<EnrollmentMatrixEntry> findByMatrixParticipantId(EntityManager em, Long matrixParticipantId) {
        if (matrixParticipantId == null) return List.of();
        return em.createQuery(JPQL_BY_PARTICIPANT, EnrollmentMatrixEntry.class)
                .setParameter("matrixParticipantId", matrixParticipantId)
                .getResultList();
    }

    /**
     * S55-B — every detail row in one matrix, across every header row, in a single query. Never
     * null; empty when {@code matrixId} is null or the matrix has no entries yet. Declined rows
     * are included — filtering those out is the caller's job, the same way this DAO leaves every
     * other selection decision to its callers.
     */
    public static List<EnrollmentMatrixEntry> findByMatrixId(EntityManager em, Long matrixId) {
        if (matrixId == null) return List.of();
        return em.createQuery(JPQL_BY_MATRIX, EnrollmentMatrixEntry.class)
                .setParameter("matrixId", matrixId)
                .getResultList();
    }

    /**
     * The detail row already occupying a (participant row, leg) pair, or null — the duplicate
     * pre-check a future writer will need, following {@link EnrollmentMatrixParticipantDAO}'s
     * own {@code findByMatrixAndParticipant} precedent.
     */
    public static EnrollmentMatrixEntry findByParticipantAndLeg(EntityManager em, Long matrixParticipantId,
                                                                 Long planTemplateMapId) {
        if (matrixParticipantId == null || planTemplateMapId == null) return null;
        List<EnrollmentMatrixEntry> found = em.createQuery(JPQL_BY_PARTICIPANT_AND_LEG, EnrollmentMatrixEntry.class)
                .setParameter("matrixParticipantId", matrixParticipantId)
                .setParameter("planTemplateMapId", planTemplateMapId)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Persists a new detail row in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back — same contract
     * {@link EnrollmentMatrixDAO#insert} documents.
     */
    public static void insert(EntityManager em, EnrollmentMatrixEntry entry) {
        try {
            em.getTransaction().begin();
            em.persist(entry);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to add enrollment matrix entry: " + e.getMessage(), e);
        }
    }

    /** Merges an edited detail row in its own transaction. Same failure contract as {@link #insert}. */
    public static void update(EntityManager em, EnrollmentMatrixEntry entry) {
        try {
            em.getTransaction().begin();
            em.merge(entry);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to save enrollment matrix entry: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one detail row, in its own transaction.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Long id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            EnrollmentMatrixEntry entry = em.find(EnrollmentMatrixEntry.class, id);
            if (entry != null) em.remove(entry);
            em.getTransaction().commit();
            return entry != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove enrollment matrix entry: " + e.getMessage(), e);
        }
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.EnrollmentMatrix;

/**
 * DAO for {@link EnrollmentMatrix} (V106). All methods are static; the class is abstract (not
 * instantiable), following {@link SummitPlanTemplateMapDAO}'s and
 * {@link SummitServiceItemFlagsDAO}'s convention.
 * <p>
 * Nothing calls this yet — the matrix UI, the push mechanism, and any report are all later,
 * separate builds.
 */
public abstract class EnrollmentMatrixDAO {

    private static final String JPQL_BY_SETUP =
            "SELECT m FROM EnrollmentMatrix m WHERE m.setupId = :setupId";

    /** One matrix by primary key, or null. */
    public static EnrollmentMatrix findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(EnrollmentMatrix.class, id);
    }

    /**
     * The matrix for a given setup, or null — the query every later build (UI, push, report)
     * will start from: one matrix per setup, {@code setup_id} unique at the database.
     */
    public static EnrollmentMatrix findBySetupId(EntityManager em, Long setupId) {
        if (setupId == null) return null;
        java.util.List<EnrollmentMatrix> found = em.createQuery(JPQL_BY_SETUP, EnrollmentMatrix.class)
                .setParameter("setupId", setupId)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Persists a new matrix in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back — same contract
     * {@link SummitServiceItemFlagsDAO#insert} documents. A unique-constraint violation arrives
     * here when two callers race the same setup.
     */
    public static void insert(EntityManager em, EnrollmentMatrix matrix) {
        try {
            em.getTransaction().begin();
            em.persist(matrix);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to create enrollment matrix: " + e.getMessage(), e);
        }
    }

    /** Merges an edited matrix in its own transaction. Same failure contract as {@link #insert}. */
    public static void update(EntityManager em, EnrollmentMatrix matrix) {
        try {
            em.getTransaction().begin();
            em.merge(matrix);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to save enrollment matrix: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one matrix, in its own transaction.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Long id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            EnrollmentMatrix matrix = em.find(EnrollmentMatrix.class, id);
            if (matrix != null) em.remove(matrix);
            em.getTransaction().commit();
            return matrix != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove enrollment matrix: " + e.getMessage(), e);
        }
    }
}

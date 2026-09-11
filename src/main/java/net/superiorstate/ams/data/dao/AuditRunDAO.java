package net.superiorstate.ams.data.dao;

import net.superiorstate.ams.model.audit.AuditRun;
import jakarta.persistence.EntityManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for {@link AuditRun} (V099). All methods are static; the class is abstract (not
 * instantiable), following {@code SummitSetupStepDAO}'s own shape.
 * <p>
 * <b>Not an authorization boundary</b> — same note as {@code SummitFileExportDAO} and
 * {@code SummitSetupStepDAO} make for their own reads. Callers (here, only
 * {@code AuditService}) establish PSP scope before calling.
 */
public abstract class AuditRunDAO {

    private static final String JPQL_ALL_FOR_PSP =
            "SELECT r FROM AuditRun r WHERE r.pspId = :pspId ORDER BY r.runAt DESC, r.id DESC";

    /**
     * Inserts one run row, in its own transaction. Same transaction pattern as
     * {@code SummitSetupStepDAO.markDone} / {@code SummitFileExportDAO.insert}: begin, persist,
     * commit; rollback and rethrow on failure.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void insert(EntityManager em, AuditRun run) {
        try {
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to insert audit run: " + e.getMessage(), e);
        }
    }

    /**
     * The most recent run for each registered check key, for one PSP. A check with no run yet
     * simply has no entry in the returned map — callers render that as "never run", not as an
     * error.
     */
    public static Map<String, AuditRun> findLatestPerCheck(EntityManager em, Long pspId) {
        Map<String, AuditRun> latest = new LinkedHashMap<>();
        if (pspId == null) return latest;
        List<AuditRun> all = em.createQuery(JPQL_ALL_FOR_PSP, AuditRun.class)
                .setParameter("pspId", pspId)
                .getResultList();
        for (AuditRun run : all) {
            latest.putIfAbsent(run.getCheckKey(), run);
        }
        return latest;
    }
}

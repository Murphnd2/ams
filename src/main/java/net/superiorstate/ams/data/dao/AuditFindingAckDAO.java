package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.AuditFindingAck;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO for {@link AuditFindingAck} (V115). All methods are static; the class is abstract (not
 * instantiable), following {@link AuditDeclineEmployerDAO}'s convention.
 * <p>
 * <b>Not an authorization boundary.</b> The reads answer "what has this PSP acknowledged for this
 * check", nothing more — callers establish that the caller may act for that PSP before calling,
 * exactly as {@link AuditDeclineEmployerDAO} documents for its own reads.
 */
public abstract class AuditFindingAckDAO {

    /** Every acknowledgment for one PSP and check, most recently created first — the order the
     *  detail page's Acknowledged section lists them in. */
    private static final String JPQL_ALL_BY_PSP_AND_CHECK =
            "SELECT a FROM AuditFindingAck a WHERE a.pspId = :pspId AND a.checkKey = :checkKey " +
            "ORDER BY a.createdAt DESC, a.id DESC";

    private static final String JPQL_ONE =
            "SELECT a FROM AuditFindingAck a " +
            "WHERE a.pspId = :pspId AND a.checkKey = :checkKey AND a.findingKey = :findingKey";

    /**
     * Every acknowledgment for one PSP and check — the owning check's own read, to split its
     * current findings into surfaced and suppressed. Never null; empty when {@code pspId} or
     * {@code checkKey} is null, or nothing has been acknowledged.
     */
    public static List<AuditFindingAck> findAllByPspAndCheck(EntityManager em, Long pspId, String checkKey) {
        if (pspId == null || checkKey == null) return List.of();
        return em.createQuery(JPQL_ALL_BY_PSP_AND_CHECK, AuditFindingAck.class)
                .setParameter("pspId", pspId)
                .setParameter("checkKey", checkKey)
                .getResultList();
    }

    /** The acknowledgment for one (PSP, check, finding) triple, or null. The admin save path's
     *  own duplicate/update lookup, and {@link #upsert}'s. */
    public static AuditFindingAck findOne(EntityManager em, Long pspId, String checkKey, String findingKey) {
        if (pspId == null || checkKey == null || findingKey == null) return null;
        List<AuditFindingAck> found = em.createQuery(JPQL_ONE, AuditFindingAck.class)
                .setParameter("pspId", pspId)
                .setParameter("checkKey", checkKey)
                .setParameter("findingKey", findingKey)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /** One acknowledgment by primary key, or null — the detail page's ownership check before
     *  un-acknowledging, the same role {@link AuditDeclineEmployerDAO#findById} plays for its
     *  own table. */
    public static AuditFindingAck findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(AuditFindingAck.class, id);
    }

    /**
     * Inserts a new acknowledgment or updates the existing one for the same (PSP, check, finding)
     * triple — the unique key is what makes this an upsert rather than a risk of a duplicate.
     * {@code ack} carries {@code pspId}/{@code checkKey}/{@code findingKey}/{@code ackState}/
     * {@code observedCount}/{@code observedThrough}/{@code note}; its {@code id}/{@code createdAt}/
     * {@code createdBy}/{@code updatedAt}/{@code updatedBy} are ignored on input and set here —
     * {@code createdAt}/{@code createdBy} only on first insert, {@code updatedAt}/{@code updatedBy}
     * only when an existing row is being changed, the same split {@code SummitEmployerFlagAdmin
     * .save} makes between its own insert and update branches.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void upsert(EntityManager em, AuditFindingAck ack, String actorName) {
        try {
            em.getTransaction().begin();
            AuditFindingAck existing = findOne(em, ack.getPspId(), ack.getCheckKey(), ack.getFindingKey());
            if (existing == null) {
                ack.setCreatedAt(LocalDateTime.now());
                ack.setCreatedBy(actorName);
                em.persist(ack);
            } else {
                existing.setAckState(ack.getAckState());
                existing.setObservedCount(ack.getObservedCount());
                existing.setObservedThrough(ack.getObservedThrough());
                existing.setNote(ack.getNote());
                existing.setUpdatedAt(LocalDateTime.now());
                existing.setUpdatedBy(actorName);
                em.merge(existing);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to save acknowledgment: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one acknowledgment, in its own transaction. There is no artifact anywhere else keyed
     * on this row — un-acknowledging is pure AMS-side data, so deletion is unconditional, the same
     * reasoning {@link AuditDeclineEmployerDAO#delete} gives for its own table.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Long id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            AuditFindingAck ack = em.find(AuditFindingAck.class, id);
            if (ack != null) em.remove(ack);
            em.getTransaction().commit();
            return ack != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove acknowledgment: " + e.getMessage(), e);
        }
    }
}

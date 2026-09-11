package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.SummitServiceItemFlags;

import java.util.Collection;
import java.util.List;

/**
 * DAO for {@link SummitServiceItemFlags} (V101). All methods are static; the class is abstract
 * (not instantiable), following {@link SummitPlanTemplateMapDAO}'s convention.
 * <p>
 * <b>Not an authorization boundary.</b> The reads answer "what Summit flags has this PSP mapped",
 * nothing more — callers establish that the caller may act for that PSP before calling, exactly as
 * {@link SummitPlanTemplateMapDAO} documents for its own reads.
 */
public abstract class SummitServiceItemFlagsDAO {

    /**
     * Every mapping for one PSP, ordered by the ServiceItem's description (the admin screen's own
     * listing order — {@link SummitPlanTemplateMapDAO#findAllByPspId} orders by {@code sortOrder}
     * instead, but this table carries no emit order of its own). The join stays simple — one
     * {@code ServiceItem} lookup per row via a correlated subquery-free join — because both sides
     * are PSP-scoped and small.
     */
    private static final String JPQL_ALL_BY_PSP =
            "SELECT f FROM SummitServiceItemFlags f, ServiceItem si " +
            "WHERE f.pspId = :pspId AND si.id = f.serviceItemId " +
            "ORDER BY si.description, si.id";

    /** Falls back to id order when the join above can't be used (e.g. a stale service item id). */
    private static final String JPQL_ALL_BY_PSP_FALLBACK =
            "SELECT f FROM SummitServiceItemFlags f WHERE f.pspId = :pspId ORDER BY f.id";

    private static final String JPQL_BY_PSP_AND_SERVICE_ITEMS =
            "SELECT f FROM SummitServiceItemFlags f " +
            "WHERE f.pspId = :pspId AND f.serviceItemId IN :serviceItemIds";

    private static final String JPQL_BY_PSP_AND_SERVICE_ITEM =
            "SELECT f FROM SummitServiceItemFlags f " +
            "WHERE f.pspId = :pspId AND f.serviceItemId = :serviceItemId";

    /**
     * Every flag mapping for one PSP, ordered by the ServiceItem's description (falling back to
     * id if a service item id no longer resolves — e.g. the item was deleted after the mapping was
     * made). Never null; empty when {@code pspId} is null or the PSP has mapped nothing.
     */
    public static List<SummitServiceItemFlags> findAllByPspId(EntityManager em, Long pspId) {
        if (pspId == null) return List.of();
        try {
            return em.createQuery(JPQL_ALL_BY_PSP, SummitServiceItemFlags.class)
                    .setParameter("pspId", pspId)
                    .getResultList();
        } catch (RuntimeException e) {
            return em.createQuery(JPQL_ALL_BY_PSP_FALLBACK, SummitServiceItemFlags.class)
                    .setParameter("pspId", pspId)
                    .getResultList();
        }
    }

    /**
     * The flag mappings for whichever of the given ServiceItem ids this PSP has mapped — the
     * resolver's own read. Never null; empty when {@code pspId} is null or {@code serviceItemIds}
     * is null or empty (an empty {@code IN} clause is invalid JPQL, so this is checked rather than
     * left to the query to reject).
     */
    public static List<SummitServiceItemFlags> findByPspAndServiceItems(EntityManager em, Long pspId,
                                                                        Collection<Integer> serviceItemIds) {
        if (pspId == null || serviceItemIds == null || serviceItemIds.isEmpty()) return List.of();
        return em.createQuery(JPQL_BY_PSP_AND_SERVICE_ITEMS, SummitServiceItemFlags.class)
                .setParameter("pspId", pspId)
                .setParameter("serviceItemIds", serviceItemIds)
                .getResultList();
    }

    /** One mapping by primary key, or null. */
    public static SummitServiceItemFlags findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(SummitServiceItemFlags.class, id);
    }

    /**
     * The row already occupying a (PSP, ServiceItem) pair, or null — the admin screen's duplicate
     * pre-check, the same role {@link SummitPlanTemplateMapDAO#findByPspAndServiceItem} plays for
     * its own table.
     */
    public static SummitServiceItemFlags findByPspAndServiceItem(EntityManager em, Long pspId,
                                                                  Integer serviceItemId) {
        if (pspId == null || serviceItemId == null) return null;
        List<SummitServiceItemFlags> found = em.createQuery(JPQL_BY_PSP_AND_SERVICE_ITEM, SummitServiceItemFlags.class)
                .setParameter("pspId", pspId)
                .setParameter("serviceItemId", serviceItemId)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Persists a new mapping in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back. A unique-constraint
     * violation arrives here when two admins race the same (PSP, ServiceItem) pair; the caller
     * reports it rather than swallowing it, the same contract {@link SummitPlanTemplateMapDAO#insert}
     * documents.
     */
    public static void insert(EntityManager em, SummitServiceItemFlags flags) {
        try {
            em.getTransaction().begin();
            em.persist(flags);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to add employer flag mapping: " + e.getMessage(), e);
        }
    }

    /** Merges an edited mapping in its own transaction. Same failure contract as {@link #insert}. */
    public static void update(EntityManager em, SummitServiceItemFlags flags) {
        try {
            em.getTransaction().begin();
            em.merge(flags);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to save employer flag mapping: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one mapping, in its own transaction. Unlike {@link SummitPlanTemplateMapDAO#delete},
     * there is no Summit-side artifact keyed on this row to worry about orphaning — a flag mapping
     * is pure AMS-side configuration, so deletion is unconditional.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Long id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            SummitServiceItemFlags flags = em.find(SummitServiceItemFlags.class, id);
            if (flags != null) em.remove(flags);
            em.getTransaction().commit();
            return flags != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove employer flag mapping: " + e.getMessage(), e);
        }
    }
}

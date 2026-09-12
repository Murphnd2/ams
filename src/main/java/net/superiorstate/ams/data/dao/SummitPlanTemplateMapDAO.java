package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;

import java.util.List;

/**
 * DAO for {@link SummitPlanTemplateMap} (V095). All methods are static; the class is abstract
 * (not instantiable), following the convention of {@link EmployerParticipantDAO} and its siblings.
 * <p>
 * <b>Not an authorization boundary.</b> The single read answers "what Summit plan templates has
 * this PSP mapped", nothing more — it does not know who is asking. Callers establish that the
 * caller may act for that PSP before calling, exactly as {@code SummitExportServlet} does with its
 * PSP-admin and {@code IchraAccessResolver} gates.
 */
public abstract class SummitPlanTemplateMapDAO {

    /**
     * Active mappings in emit order. {@code id} breaks a {@code sort_order} tie so two rows sharing
     * an order still emit deterministically — the property path's config order is total, and this
     * keeps the table path's order total too rather than leaving it to the database.
     * V103 (W3) adds {@code seq} between them: for a service item with several rows, the resolver's
     * first-wins dedupe now picks the lowest {@code seq} within a shared {@code sort_order} rather
     * than whichever row was inserted first. A single-row service item is unaffected.
     */
    private static final String JPQL_ACTIVE_BY_PSP =
            "SELECT m FROM SummitPlanTemplateMap m " +
            "WHERE m.pspId = :pspId AND m.active = true " +
            "ORDER BY m.sortOrder, m.seq, m.id";

    /** S31-F — the admin screen's listing: active and inactive, same ordering as the emit read. */
    private static final String JPQL_ALL_BY_PSP =
            "SELECT m FROM SummitPlanTemplateMap m " +
            "WHERE m.pspId = :pspId " +
            "ORDER BY m.sortOrder, m.seq, m.id";

    /**
     * S31-F — the constraint pre-check. Deliberately ignores is_active, because the constraint does.
     * V103 (W3): the constraint is now per {@code seq}, so this may return several rows; ordered by
     * {@code seq} so the caller's {@code get(0)} is the {@code seq 0} row. The admin screen still
     * treats any hit as "already mapped" — widening that is W5.
     */
    private static final String JPQL_BY_PSP_AND_SERVICE_ITEM =
            "SELECT m FROM SummitPlanTemplateMap m " +
            "WHERE m.pspId = :pspId AND m.serviceItemId = :serviceItemId " +
            "ORDER BY m.seq, m.id";

    /**
     * The active plan template mappings for one PSP, in emit order.
     *
     * @return the matching rows, or an empty list when {@code pspId} is null or the PSP has mapped
     * nothing. Never null. <b>An empty list is the signal the resolver falls back to
     * {@code SUMMIT_PLAN_TEMPLATES} on</b>, so it is a supported state rather than an error — an
     * installation that has taken V095 but entered no rows returns empty here on every request.
     */
    public static List<SummitPlanTemplateMap> findActiveByPspId(EntityManager em, Long pspId) {
        if (pspId == null) return List.of();
        return em.createQuery(JPQL_ACTIVE_BY_PSP, SummitPlanTemplateMap.class)
                .setParameter("pspId", pspId)
                .getResultList();
    }

    /**
     * S31-F — <b>every</b> mapping for one PSP, active and inactive alike, in the same order
     * {@link #findActiveByPspId} uses.
     * <p>
     * ⚠️ <b>Inactive rows must be visible on the admin screen.</b> The unique constraint
     * {@code uq_summit_plan_template_map_psp_service} does not consider {@code is_active}, so an
     * inactive row still occupies its (PSP, ServiceItem) pair and blocks a new one. A screen that
     * listed only active rows would show an admin nothing while their "add" kept failing — the
     * trap T202 was filed to avoid.
     *
     * @return the matching rows, or an empty list when {@code pspId} is null. Never null.
     */
    public static List<SummitPlanTemplateMap> findAllByPspId(EntityManager em, Long pspId) {
        if (pspId == null) return List.of();
        return em.createQuery(JPQL_ALL_BY_PSP, SummitPlanTemplateMap.class)
                .setParameter("pspId", pspId)
                .getResultList();
    }

    /** One mapping by primary key, or null. */
    public static SummitPlanTemplateMap findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(SummitPlanTemplateMap.class, id);
    }

    /**
     * S31-F — the row already occupying a (PSP, ServiceItem) pair, or null.
     * <p>
     * Exists so the admin screen can explain a rejected insert in words — naming the existing row
     * and whether it is active — rather than surfacing a constraint violation. It reads
     * <b>regardless of {@code is_active}</b>, because the constraint does.
     */
    public static SummitPlanTemplateMap findByPspAndServiceItem(EntityManager em, Long pspId,
                                                                Integer serviceItemId) {
        if (pspId == null || serviceItemId == null) return null;
        List<SummitPlanTemplateMap> found = em.createQuery(JPQL_BY_PSP_AND_SERVICE_ITEM, SummitPlanTemplateMap.class)
                .setParameter("pspId", pspId)
                .setParameter("serviceItemId", serviceItemId)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Persists a new mapping in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back. ⚠️ <b>A unique-constraint
     * violation arrives here</b> when two admins race the same (PSP, ServiceItem) pair; the caller
     * reports it rather than swallowing it. The screen's pre-check catches the ordinary case and
     * produces a better message, but it cannot close the race, so this path must stay loud.
     */
    public static void insert(EntityManager em, SummitPlanTemplateMap mapping) {
        try {
            em.getTransaction().begin();
            em.persist(mapping);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to add plan template mapping: " + e.getMessage(), e);
        }
    }

    /** Merges an edited mapping in its own transaction. Same failure contract as {@link #insert}. */
    public static void update(EntityManager em, SummitPlanTemplateMap mapping) {
        try {
            em.getTransaction().begin();
            em.merge(mapping);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to save plan template mapping: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one mapping, in its own transaction. Follows {@code ProviderSetup}'s handling of its
     * own mapping rows, which deletes rather than deactivating.
     * <p>
     * ⚠️ Deleting a mapping does <b>not</b> touch anything already created in Summit. A plan built
     * from this row keeps its {@code Import Plan ID}; removing the row only stops future exports
     * emitting it, and re-adding it later with a different {@code key_segment} would emit a
     * <b>different</b> plan rather than updating the old one.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Long id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            SummitPlanTemplateMap mapping = em.find(SummitPlanTemplateMap.class, id);
            if (mapping != null) em.remove(mapping);
            em.getTransaction().commit();
            return mapping != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove plan template mapping: " + e.getMessage(), e);
        }
    }
}

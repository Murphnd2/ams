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
     */
    private static final String JPQL_ACTIVE_BY_PSP =
            "SELECT m FROM SummitPlanTemplateMap m " +
            "WHERE m.pspId = :pspId AND m.active = true " +
            "ORDER BY m.sortOrder, m.id";

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
}

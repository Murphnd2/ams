package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.IllustrationLog;

import java.util.List;

/**
 * DAO for {@link IllustrationLog}. All methods are static; the class is
 * abstract (not instantiable) following the convention of {@link RateCacheDAO},
 * EmployerInventoryDAO and ChatbotSkillDAO.
 * <p>
 * Read-only by design. {@code illustration_log} rows are written directly by
 * {@code IllustrationServlet} and {@code GroupConversionServlet} inside their own
 * telemetry transactions — deliberately not routed through here, so a DAO method can
 * never become a second, divergent way to write a log row.
 */
public abstract class IllustrationLogDAO {

    /**
     * Cap on rows returned by {@link #findByOpportunityId}. The opportunity drawer shows
     * a short recent history, not an audit trail — an opportunity with hundreds of runs
     * should not drag hundreds of rows into a drawer that opens on every card click.
     */
    private static final int MAX_ROWS_BY_OPPORTUNITY = 25;

    private static final String JPQL_BY_OPPORTUNITY =
            "SELECT l FROM IllustrationLog l " +
            "WHERE l.opportunityId = :opportunityId " +
            "ORDER BY l.createdAt DESC, l.id DESC";

    /**
     * Illustration and conversion-analysis rows logged against one opportunity, newest
     * first, capped at {@value #MAX_ROWS_BY_OPPORTUNITY}.
     * <p>
     * <b>Not an authorization boundary.</b> This method answers "what was logged against
     * this id", nothing more — it does not know who is asking. Every caller must check
     * that the caller may see the opportunity <em>before</em> calling it
     * ({@code OpportunityAuthz.canAccessOpportunity}), exactly as
     * {@code IchraOpportunityAnalyses} does.
     *
     * @return the matching rows, or an empty list when {@code opportunityId} is null or
     * nothing is logged against it. Never null.
     */
    public static List<IllustrationLog> findByOpportunityId(EntityManager em, Long opportunityId) {
        if (opportunityId == null) {
            return List.of();
        }
        return em.createQuery(JPQL_BY_OPPORTUNITY, IllustrationLog.class)
                .setParameter("opportunityId", opportunityId)
                .setMaxResults(MAX_ROWS_BY_OPPORTUNITY)
                .getResultList();
    }
}

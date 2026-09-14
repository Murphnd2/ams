package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.dao.SummitPlanTemplateMapDAO;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * S59-P3rev -- read-only: does this setup have an elected service item mapped to a
 * card-issuer-flagged Summit plan template? Backs the card-issuer row's visibility guard on the
 * Summit setup panel ({@code detailSummitSetup25.jsp}), via {@code CardIssuerAvailabilityServlet}.
 * <p>
 * <b>Deliberately does not call {@code SummitPlanTemplateResolver}.</b> That resolver's
 * table-vs-property fallback is opaque by design -- its own javadoc: "Callers cannot tell which
 * source answered, and must not learn" -- so the exported file stays byte-identical regardless of
 * how a PSP's mapping is expressed, and this class must not compromise that. Instead it asks a
 * narrower, row-level question directly of {@link SummitPlanTemplateMapDAO}: does the PSP have any
 * active mapping rows, and if so is one of them, restricted to this setup's elected service items,
 * flagged card issuer. That is not a question the resolver's contract was ever meant to answer.
 * <p>
 * ⚠️ <b>Known, accepted gap</b> (registered as a {@code TA-NN} assumption in the S59-P3rev report):
 * a PSP whose active mapping rows all fail {@code SummitPlanTemplateResolver}'s private
 * key-segment validation would look row-backed here -- this class reads
 * {@link SummitPlanTemplateMapDAO} directly, with no key-segment check of its own -- while the
 * resolver itself would fall through to the property fallback for the actual export. In that
 * narrow case this service could return {@link Result#NOT_AVAILABLE} (hiding the row) in a state
 * the export path would treat as property-fallback indeterminate. Closing this would mean either
 * duplicating the resolver's private validation here or breaking its source-opacity contract --
 * both rejected. Accepted as a narrow, one-condition gap rather than closed.
 * <p>
 * Three-valued, never a bare boolean: {@link Result#NOT_AVAILABLE} is the <b>only</b> outcome that
 * hides the row. No active mapping rows for the PSP (property-fallback territory, including every
 * fresh installation), a null PSP, or any exception all resolve to {@link Result#INDETERMINATE},
 * which the caller must render as "show the row" -- hiding a working control because a lookup
 * failed or because a PSP still runs on the legacy property is a worse outcome than showing a row
 * that turns out not to apply.
 * <p>
 * Read-only: no write, no flush, no transaction started here.
 */
public final class CardIssuerAvailabilityService {

    private static final Logger log = LogManager.getLogger(CardIssuerAvailabilityService.class);

    public enum Result {
        /**
         * At least one active {@code summit_plan_template_map} row for the PSP, and one of them
         * -- restricted to this setup's elected service items -- carries {@code is_card_issuer}.
         */
        AVAILABLE,
        /**
         * At least one active {@code summit_plan_template_map} row for the PSP, and none of them
         * -- restricted to this setup's elected service items -- carries {@code is_card_issuer}.
         * The only outcome that hides the row.
         */
        NOT_AVAILABLE,
        /**
         * No active mapping rows for the PSP (the state under which
         * {@code SummitPlanTemplateResolver} would fall back to the {@code SUMMIT_PLAN_TEMPLATES}
         * property), a null/unresolvable PSP, or any failure resolving either. Callers must treat
         * this exactly like {@link #AVAILABLE} -- fail open, never hide on an indeterminate read.
         */
        INDETERMINATE
    }

    private CardIssuerAvailabilityService() {}

    /**
     * @param em         an open {@code EntityManager}
     * @param pspId      the acting PSP, or {@code null} if unresolved -- {@code null} is
     *                   {@link Result#INDETERMINATE}, not an error
     * @param proposalId the setup's proposal id, queried exactly the way
     *                   {@code SummitExportServlet.loadElectedServiceItems} queries it
     * @return never {@code null}; any exception anywhere in resolution is caught and reported as
     * {@link Result#INDETERMINATE} rather than propagated
     */
    public static Result resolve(EntityManager em, Long pspId, long proposalId) {
        try {
            if (em == null || pspId == null) {
                return Result.INDETERMINATE;
            }

            List<SummitPlanTemplateMap> activeMappings = SummitPlanTemplateMapDAO.findActiveByPspId(em, pspId);
            if (activeMappings.isEmpty()) {
                // Property-fallback territory (or a PSP that has taken V095 but entered no rows
                // yet). SummitPlanTemplateResolver is never asked and this class does not learn,
                // or need to learn, which source the export path would actually use -- it only
                // observes that this PSP has no mapping rows of its own to ask about.
                return Result.INDETERMINATE;
            }

            Set<Integer> electedServiceItemIds = loadElectedServiceItemIds(em, proposalId);
            for (SummitPlanTemplateMap mapping : activeMappings) {
                if (mapping.isCardIssuer() && electedServiceItemIds.contains(mapping.getServiceItemId())) {
                    return Result.AVAILABLE;
                }
            }
            return Result.NOT_AVAILABLE;
        } catch (Exception e) {
            log.warn("[CARD-ISSUER-AVAILABILITY] resolution failed for proposalId={}, pspId={}: {}",
                    proposalId, pspId, e.getMessage());
            return Result.INDETERMINATE;
        }
    }

    /**
     * Same query shape {@code SummitExportServlet.loadElectedServiceItems} uses -- that method is
     * private to its own class, so this is a read of the same entities via the same JPQL, not a
     * shared call. Ids only; this service has no use for the description text that method also
     * collects.
     */
    private static Set<Integer> loadElectedServiceItemIds(EntityManager em, long proposalId) {
        Query q = em.createQuery(
                "SELECT am FROM ApplicationModule am " +
                "WHERE am.application.proposal.id = :pid");
        q.setParameter("pid", proposalId);
        @SuppressWarnings("unchecked")
        List<ApplicationModule> modules = (List<ApplicationModule>) q.getResultList();
        Set<Integer> ids = new HashSet<>();
        for (ApplicationModule module : modules) {
            ServiceItem serviceItem = module.getServiceItem();
            if (serviceItem != null) ids.add(serviceItem.getId());
        }
        return ids;
    }
}

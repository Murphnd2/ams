package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * S18-C/S18-D — answers whether a {@code SCOPED} proposal section tied to a given
 * enhancement should be considered a match for a given proposal, when that enhancement
 * is flagged {@code system_managed} (V089).
 * <p>
 * <b>Bit-identity guarantee.</b> An unflagged enhancement ({@code system_managed = 0} —
 * every row in existence the moment V089 applies) returns {@code true} immediately, before
 * touching {@code em}, {@code proposal}, or any query. {@link
 * net.superiorstate.ams.controller.activity.setup.ViewProposal}'s scope filter composes
 * this with its existing membership test as {@code proposalEnhIds.contains(...) &&
 * isSectionEnabled(...)}, which for an unflagged enhancement reduces to
 * {@code proposalEnhIds.contains(...)} exactly — today's condition, unchanged.
 * <p>
 * <b>The flagged path is a stub.</b> The per-proposal signal this method needs for a
 * flagged enhancement is a later build — the ICHRA JSON payload has not landed. Until
 * then, a flagged enhancement's section is withheld (see the {@code TODO} below). Do not
 * invent the payload's shape here.
 * <p>
 * <b>Fails closed.</b> Any exception returns {@code false}, matching {@code ViewProposal
 * .isPlusTierScoped}'s stated uncertainty discipline — never render on uncertainty. Never
 * throws, so a proposal cannot fail to render because this check failed.
 * <p>
 * <b>No enhancement ID literal may ever appear in this class.</b> {@code system_managed}
 * is the only discriminator. {@code enhancement.psp_id} makes the flag PSP-scoped by
 * construction; a hardcoded ID would not survive a second installation. See
 * {@code V089__enhancement_system_managed.sql} and
 * {@code docs/analysis/S18C_proposal_detail_render_spec.md} §2.2.
 */
public final class FlaggedEnhancementResolver {

    private static final Logger log = LogManager.getLogger(FlaggedEnhancementResolver.class);

    private FlaggedEnhancementResolver() {}

    public static boolean isSectionEnabled(EntityManager em, Proposal proposal, Enhancement enhancement) {
        try {
            if (enhancement == null) {
                return true;
            }
            if (!enhancement.isSystemManaged()) {
                return true;
            }

            // TODO: per-proposal signal not implemented yet. See
            // docs/analysis/S18C_proposal_detail_render_spec.md §2.2 step 3 — the ICHRA
            // JSON payload that will carry this signal is a later build. Until it lands,
            // a system_managed enhancement's section is withheld.
            return false;
        } catch (Exception e) {
            log.warn("FlaggedEnhancementResolver check failed for enhancement #{} on proposal #{} — " +
                            "treating as not enabled and omitting the section: {}",
                    enhancement != null ? enhancement.getId() : "null",
                    proposal != null ? proposal.getId() : "null",
                    e.getMessage());
            return false;
        }
    }
}

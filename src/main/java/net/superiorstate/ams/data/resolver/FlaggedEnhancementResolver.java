package net.superiorstate.ams.data.resolver;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.ProposalIchraSnapshotDAO;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.ProposalIchraSnapshot;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * <b>The flagged path reads {@code payload_json}.</b> S20-B/V091 — a flagged enhancement's
 * section renders only when the proposal's {@link ProposalIchraSnapshot#getPayloadJson()}
 * carries a {@code sections} block (schemaVersion 2+) whose entry for this enhancement's
 * {@link Enhancement#getSystemSectionKey()} is both {@code selected} and {@code complete}.
 * See {@code docs/analysis/S20A_ichra_sections_spec.md} §4/§5. No payload, a v1 payload (no
 * {@code sections}), an unselected or incomplete section, or an unclassified enhancement
 * (blank {@code system_section_key}) all withhold — same as the pre-V091 stub's behaviour
 * for every one of those cases.
 * <p>
 * <b>Fails closed.</b> Any exception returns {@code false}, matching {@code ViewProposal
 * .isPlusTierScoped}'s stated uncertainty discipline — never render on uncertainty. Never
 * throws, so a proposal cannot fail to render because this check failed.
 * <p>
 * <b>No enhancement ID literal may ever appear in this class.</b> {@code system_managed}
 * gates whether a section is resolver-controlled at all; {@code system_section_key}
 * (V091) is the discriminator for *which* section. Both are columns on the PSP-scoped
 * {@code enhancement} row, never a hardcoded id or a global {@code constant} row — a
 * hardcoded ID would not survive a second installation. See
 * {@code V089__enhancement_system_managed.sql}, {@code V091__ichra_section_selection.sql}
 * and {@code docs/analysis/S18C_proposal_detail_render_spec.md} §2.2.
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

            // S20-B/V091 — see docs/analysis/S20A_ichra_sections_spec.md §5.3.
            String key = enhancement.getSystemSectionKey();
            if (key == null || key.isBlank()) {
                return false; // flagged but unclassified — half-configured is not configured
            }

            ProposalIchraSnapshot snapshot = ProposalIchraSnapshotDAO.findByProposalId(em, proposal.getId());
            if (snapshot == null || snapshot.getPayloadJson() == null) {
                return false; // no ICHRA hand-off for this proposal at all
            }

            JsonObject payload = new Gson().fromJson(snapshot.getPayloadJson(), JsonObject.class);
            if (payload == null || !payload.has("sections") || !payload.get("sections").isJsonObject()) {
                return false; // v1 payload (predates the sections block), or unparseable
            }

            JsonObject sections = payload.getAsJsonObject("sections");
            String k = key.trim();
            if (!sections.has(k) || !sections.get(k).isJsonObject()) {
                return false;
            }

            JsonObject sec = sections.getAsJsonObject(k);
            return sec.has("selected") && !sec.get("selected").isJsonNull() && sec.get("selected").getAsBoolean()
                && sec.has("complete") && !sec.get("complete").isJsonNull() && sec.get("complete").getAsBoolean();
        } catch (Exception e) {
            log.warn("FlaggedEnhancementResolver check failed for enhancement #{} on proposal #{} — " +
                            "treating as not enabled and omitting the section: {}",
                    enhancement != null ? enhancement.getId() : "null",
                    proposal != null ? proposal.getId() : "null",
                    e.getMessage());
            return false;
        }
    }

    /**
     * S20-B/V091 — the membership half {@code isSectionEnabled} alone cannot answer.
     * {@code ViewProposal}'s {@code proposalEnhIds} set (the scope filter's enhancement
     * membership test) is derived from priced {@code RateTable} rows
     * ({@code SalesDAO.getPricing}), so a {@code system_managed} enhancement — which by
     * design drives no pricing — never enters it and its section is unreachable regardless
     * of what {@link #isSectionEnabled} would answer. This widens membership directly from
     * the proposal's LOS list, the same association {@code getPricing}'s own enhancement
     * branch already keys on, without requiring a fabricated price line. See
     * {@code docs/analysis/S20A_ichra_sections_spec.md} §1.4b/§5.2.
     * <p>
     * Flat single-level query — no nested {@code JOIN FETCH} (this codebase's own
     * EclipseLink gotcha). {@code suppressed = false} is included deliberately: a suppressed
     * enhancement is one the PSP has retired, and this is a new, additive path — the safe
     * direction is to withhold, not to reveal a retired enhancement's section.
     * <p>
     * Returns an empty set for a null/empty LOS list or any exception — fails closed, never
     * throws, so a proposal cannot fail to render because this lookup failed.
     */
    public static Set<Long> systemManagedIdsForProposal(EntityManager em, Proposal proposal) {
        try {
            List<LOS> losList = proposal != null ? proposal.getLosList() : null;
            if (losList == null || losList.isEmpty()) {
                return new HashSet<>();
            }
            Set<Long> losIds = new HashSet<>();
            for (LOS los : losList) {
                if (los != null && los.getId() != null) {
                    losIds.add(los.getId());
                }
            }
            if (losIds.isEmpty()) {
                return new HashSet<>();
            }
            List<Long> ids = em.createQuery(
                            "SELECT e.id FROM Enhancement e JOIN e.losList el " +
                                    "WHERE e.systemManaged = true AND e.suppressed = false AND el.id IN :losIds",
                            Long.class)
                    .setParameter("losIds", losIds)
                    .getResultList();
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("FlaggedEnhancementResolver.systemManagedIdsForProposal failed for proposal #{} — " +
                            "treating as no system-managed membership: {}",
                    proposal != null ? proposal.getId() : "null",
                    e.getMessage());
            return new HashSet<>();
        }
    }
}

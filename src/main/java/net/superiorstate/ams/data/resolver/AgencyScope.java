package net.superiorstate.ams.data.resolver;

import java.util.Set;

/**
 * Immutable result of {@link AgencyScopeResolver} — answers "what agencies can this
 * user see?" for the current session, split into two independent sets:
 *
 *   - rollupAgencyIds: agencies whose opportunities/prospects may appear in list and
 *     kanban views (name, stage, value, agency badge only).
 *   - detailAgencyIds: agencies whose records may be OPENED (contact, census, proposal,
 *     application).
 *
 * PHASE 1 (this class's introduction): rollupAgencyIds and detailAgencyIds are always
 * identical. The split exists so a later phase can widen rollup (e.g. a General Agent
 * seeing its downline's opportunities as cards) without touching detail (which record
 * pages may actually be opened) — do not collapse the two fields back into one, and do
 * not assume they'll stay equal once hierarchy-widening lands.
 */
public record AgencyScope(
        boolean pspWide,
        Long pspId,
        Long primaryAgencyId,
        Set<Long> rollupAgencyIds,
        Set<Long> detailAgencyIds
) {
    public AgencyScope {
        rollupAgencyIds = rollupAgencyIds == null ? Set.of() : Set.copyOf(rollupAgencyIds);
        detailAgencyIds = detailAgencyIds == null ? Set.of() : Set.copyOf(detailAgencyIds);
    }

    public static AgencyScope empty() {
        return new AgencyScope(false, null, null, Set.of(), Set.of());
    }
}

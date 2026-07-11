package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;

import java.util.List;
import java.util.Set;

/**
 * Single source of truth for "what agency/agencies can the current user see?" —
 * replaces four independently-written, mutually-inconsistent "find my agency" queries
 * (AgentHome, CreateUser25, ProposalBuilder each had their own findAgencyForUser();
 * OpportunityAuthz.belongsToAgency() was a fifth variant expressed as a membership
 * predicate). See AGENCY_STRUCTURE_AUDIT.md §2.1 for the pre-refactor inventory and
 * PHASE1_NOTES.md for the call-by-call behavior-change analysis.
 *
 * PHASE 1 scope: this resolver is behavior-neutral scaffolding. rollupAgencyIds and
 * detailAgencyIds are always identical here — the split exists for a later phase
 * (a General Agent rolling up its downline's opportunities without being able to open
 * their records) and is intentionally unused beyond storage in Phase 1. No hierarchy
 * walking (parent_agency_id) happens in this class yet.
 */
public final class AgencyScopeResolver {

    private AgencyScopeResolver() {}

    /**
     * Resolves scope for the current session — reads role flags and the cached
     * current Person off {@code request}'s session (same session attributes AuthDAO
     * sets at login: isPspAdmin/isPspUser/isPspSales/isAgencyAdmin/isAgent, and the
     * "local" AmsDataLocal holding currentPerson).
     */
    public static AgencyScope resolve(EntityManager em, HttpServletRequest request) {
        Object localObj = request.getSession().getAttribute("local");
        Person currentUser = (localObj instanceof AmsDataLocal local) ? local.getCurrentPerson() : null;

        return resolve(em, currentUser,
                flag(request, "isPspAdmin"),
                flag(request, "isPspUser"),
                flag(request, "isPspSales"),
                flag(request, "isAgencyAdmin"),
                flag(request, "isAgent"));
    }

    /**
     * Resolves scope from explicit inputs — for callers that already have the role
     * flags and Person in hand and don't want a second session lookup.
     *
     * RESOLUTION RULES (Phase 1), evaluated in this priority order (a session can in
     * principle carry more than one role flag at once; PSP staff takes precedence):
     *   1. PSP staff (isPspAdmin || isPspUser || isPspSales) -> pspWide = true.
     *      primaryAgencyId is still resolved via the tie-break below (several existing
     *      call sites — e.g. ProposalBuilder's PSP-admin branch — use it to ask "does
     *      this PSP staffer *also* happen to belong to an agency" for UI defaults; that
     *      question is independent of their PSP-wide authorization). rollup/detail are
     *      left empty since pspWide already implies "everything in the PSP."
     *   2. Agency Admin (role 8) -> primaryAgencyId = resolved agency;
     *      rollup = detail = { primaryAgencyId } (empty set if unresolved).
     *   3. Plain Agent (role 2, not role 8) -> primaryAgencyId = resolved agency;
     *      rollup = detail = EMPTY SET (agents scope by agent_id, not agency_id —
     *      unchanged from today).
     *   4. Anyone else (no currentUser, or none of the above flags) -> AgencyScope.empty().
     */
    public static AgencyScope resolve(EntityManager em, Person currentUser,
                                       boolean isPspAdmin, boolean isPspUser, boolean isPspSales,
                                       boolean isAgencyAdmin, boolean isAgent) {
        if (currentUser == null || currentUser.getId() == null) {
            return AgencyScope.empty();
        }

        boolean pspWide = isPspAdmin || isPspUser || isPspSales;

        if (pspWide) {
            Long primaryAgencyId = resolvePrimaryAgencyId(em, currentUser.getId());
            Long pspId = currentUser.getPsp() != null ? currentUser.getPsp().getId() : null;
            return new AgencyScope(true, pspId, primaryAgencyId, Set.of(), Set.of());
        }

        if (isAgencyAdmin) {
            Long primaryAgencyId = resolvePrimaryAgencyId(em, currentUser.getId());
            Set<Long> ids = primaryAgencyId != null ? Set.of(primaryAgencyId) : Set.of();
            return new AgencyScope(false, null, primaryAgencyId, ids, ids);
        }

        if (isAgent) {
            Long primaryAgencyId = resolvePrimaryAgencyId(em, currentUser.getId());
            return new AgencyScope(false, null, primaryAgencyId, Set.of(), Set.of());
        }

        return AgencyScope.empty();
    }

    /**
     * Deterministic "my agency" tie-break for a person who may belong to more than one
     * Agency (Person<->Agency is genuinely many-to-many via the agents join table).
     * Never uses getSingleResult() on a query that can return more than one row.
     *
     *   1. Any agency where agency.manager_id = personId — lowest agency_id if, somehow,
     *      more than one agency names this person manager (shouldn't happen; manager
     *      is meant to be 1:1, but this keeps resolution deterministic regardless).
     *   2. Otherwise the lowest agency_id among agentList memberships.
     *   3. Otherwise null.
     */
    private static Long resolvePrimaryAgencyId(EntityManager em, Long personId) {
        if (personId == null) return null;

        List<Long> managerMatches = em.createQuery(
                        "SELECT a.id FROM Agency a WHERE a.manager.id = :pid ORDER BY a.id ASC", Long.class)
                .setParameter("pid", personId)
                .getResultList();
        if (!managerMatches.isEmpty()) {
            return managerMatches.get(0);
        }

        List<Long> memberMatches = em.createQuery(
                        "SELECT a.id FROM Agency a JOIN a.agentList ag WHERE ag.id = :pid ORDER BY a.id ASC", Long.class)
                .setParameter("pid", personId)
                .getResultList();
        if (!memberMatches.isEmpty()) {
            return memberMatches.get(0);
        }

        return null;
    }

    /**
     * Convenience for call sites that need the full Agency entity (branding/display),
     * not just the id — several existing pages set request attribute "agency" to the
     * whole entity and must keep working unchanged.
     */
    public static Agency primaryAgencyEntity(EntityManager em, AgencyScope scope) {
        if (scope == null || scope.primaryAgencyId() == null) return null;
        return em.find(Agency.class, scope.primaryAgencyId());
    }

    private static boolean flag(HttpServletRequest request, String attr) {
        return Boolean.TRUE.equals(request.getSession().getAttribute(attr));
    }
}

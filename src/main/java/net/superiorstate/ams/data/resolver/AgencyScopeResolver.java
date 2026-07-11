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
 *
 * PHASE 1b correction: primaryAgencyId (singular, tie-broken — "which agency do I
 * display/default to") and detail/rollup (set-valued — "which agencies may I see") are
 * two distinct concepts. The initial Phase 1 cut wrongly derived the Agency Admin
 * bucket's detail/rollup sets from the primaryAgencyId singleton, which silently
 * narrowed OpportunityAuthz.belongsToAgency() versus its pre-Phase-1 predicate for any
 * admin who manages one agency but is a plain member of another (Person<->Agency is
 * genuinely many-to-many). Fixed: detail/rollup are now their own membership query,
 * independent of primaryAgencyId. See PHASE1_NOTES.md "Phase 1b" for the verification.
 *
 * PHASE 2b correction: the Plain Agent bucket's "rollup = detail = EMPTY SET" rule from
 * Phase 1 was itself a spec error — it forced write-path callers (CreateProspect,
 * CreateOpportunity) to bolt on an {@code || agencyId == primaryAgencyId} carve-out just
 * so a plain agent could act on their own agency at all. Plain Agent now gets the same
 * real membership-based detail/rollup set as Agency Admin (see {@link
 * #resolveAgencyMembershipIds}). This is an AUTHORIZATION change only ("is this agency
 * in my world" — may I write a prospect/opportunity here, is this proposal's owning
 * agency one I'm affiliated with). It must NOT be read as a READ-path row-filtering
 * change: no caller derives Plain-Agent row filtering from these sets. AgentHome's
 * opportunity query stays assigned_to_id-based, ReviewApplications' Plain Agent branch
 * stays pr.agent.id-based, and ProposalDetail's broad "any agency in my detail scope"
 * visibility check is Agency-Admin-role-gated specifically (not merely "detail set is
 * non-empty") so a Plain Agent still can't open a colleague's proposal through it — see
 * PHASE2B_NOTES.md for the full verification of all three read paths.
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
     *   2. Agency Admin (role 8) -> primaryAgencyId = resolved agency (tie-break,
     *      singular); rollup = detail = EVERY agency where this person is the manager
     *      OR a plain agentList member (set-valued, independent of primaryAgencyId —
     *      see the PHASE 1b class-level note).
     *   3. Plain Agent (role 2, not role 8) -> primaryAgencyId = resolved agency
     *      (tie-break, singular); rollup = detail = EVERY agency where this person is
     *      the manager OR a plain agentList member — same query as the Agency Admin
     *      bucket (PHASE 2b — see the class-level note; Phase 1's "empty set" rule
     *      here was a spec error). This governs AUTHORIZATION only. Read-path row
     *      filtering for a Plain Agent stays agent_id-based everywhere it already
     *      was — this bucket change does not by itself alter what any read query
     *      returns.
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
            Set<Long> ids = resolveAgencyMembershipIds(em, currentUser.getId());
            return new AgencyScope(false, null, primaryAgencyId, ids, ids);
        }

        if (isAgent) {
            Long primaryAgencyId = resolvePrimaryAgencyId(em, currentUser.getId());
            Set<Long> ids = resolveAgencyMembershipIds(em, currentUser.getId());
            return new AgencyScope(false, null, primaryAgencyId, ids, ids);
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
     * All agencies this person is authorized to see — every Agency where they are the
     * designated manager OR a plain agentList member. Used by both the Agency Admin
     * and (since Phase 2b) Plain Agent buckets. This is the SET-valued counterpart to
     * {@link #resolvePrimaryAgencyId} and is intentionally NOT derived from it:
     * Person<->Agency is genuinely many-to-many, and a person can manage one agency
     * while also being a plain member of another. This query reproduces
     * OpportunityAuthz's pre-Phase-1 predicate exactly ({@code a.manager.id =
     * :personId OR ag.id = :personId}) for a given target agency, just expressed as
     * "give me the whole set" instead of "check one id" — see PHASE1_NOTES.md
     * "Phase 1b" for the original equivalence check.
     */
    private static Set<Long> resolveAgencyMembershipIds(EntityManager em, Long personId) {
        if (personId == null) return Set.of();

        List<Long> ids = em.createQuery(
                        "SELECT DISTINCT a.id FROM Agency a LEFT JOIN a.agentList ag " +
                                "WHERE a.manager.id = :pid OR ag.id = :pid", Long.class)
                .setParameter("pid", personId)
                .getResultList();
        return ids.isEmpty() ? Set.of() : Set.copyOf(ids);
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

    /**
     * Authorization gate for opening/mutating a specific agency's records (contact,
     * census, proposal, application). Call sites MUST use this — never read
     * {@code scope.detailAgencyIds()} directly — because PSP staff have
     * {@code pspWide = true} with EMPTY scope sets; checking the raw set alone would
     * incorrectly deny PSP staff access to agencies in their own tenant.
     *
     * This answers "is agencyId in my world" for AUTHORIZATION purposes only (may I
     * write/see-that-it-exists). It is not a substitute for role-specific READ-path
     * row filtering — e.g. ProposalDetail still gates "may I open any proposal
     * belonging to this agency, regardless of which agent within it created it" on
     * the Agency Admin role specifically, since Phase 2b gave Plain Agent the same
     * non-empty detail set for authorization purposes without granting that broader
     * per-record read visibility.
     */
    public static boolean canSeeDetail(AgencyScope scope, Long agencyId) {
        if (scope == null || agencyId == null) return false;
        return scope.pspWide() || scope.detailAgencyIds().contains(agencyId);
    }

    /**
     * Authorization gate for whether a specific agency's opportunities/prospects may
     * appear in list/kanban rollup views. Call sites MUST use this — never read
     * {@code scope.rollupAgencyIds()} directly — for the same pspWide-trap reason as
     * {@link #canSeeDetail}.
     */
    public static boolean canSeeRollup(AgencyScope scope, Long agencyId) {
        if (scope == null || agencyId == null) return false;
        return scope.pspWide() || scope.rollupAgencyIds().contains(agencyId);
    }

    private static boolean flag(HttpServletRequest request, String attr) {
        return Boolean.TRUE.equals(request.getSession().getAttribute(attr));
    }
}

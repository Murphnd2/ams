package net.superiorstate.ams.data.util;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.general.Person;

import java.util.Objects;

/**
 * Central server-side authorization for Opportunity actions (view / note / email / stage).
 *
 * Single source of truth so every entry point (GoActivityDetail25, UpdateOpportunityStage,
 * AddNoteToActivity25, SendEmail25) enforces the same rule instead of relying on
 * presentational-only gates. Closes the cross-tenant IDOR on the ungated write endpoints
 * (previously an outside agent could POST an arbitrary oppId and mutate another agency's
 * opportunity).
 *
 * Access is granted when the current user is ANY of:
 *   - PSP staff (PSP Admin, PSP User, or PSP Sales)  — existing PSP-wide access, preserved
 *   - the owning agent (opportunity.assignedTo)        — P1
 *   - the designated manager (opportunity.managedBy)   — P2
 *   - an Agency Admin over the opportunity's agency    — parity with getOpportunitiesByAgency
 *
 * Reassigning the managed-by owner is a stricter action — PSP Admin only (canReassignManager).
 */
public abstract class OpportunityAuthz {

    /**
     * Convenience overload for endpoints that only have an opportunity id (e.g. the raw
     * oppId POST to UpdateOpportunityStage). Loads the opportunity via the supplied EM.
     */
    public static boolean canAccessOpportunity(EntityManager em, HttpServletRequest request, long oppId) {
        Opportunity opp = em.find(Opportunity.class, oppId);
        return canAccessOpportunity(em, request, opp);
    }

    /**
     * Core predicate. Returns true if the current session user may view / note / email /
     * change the stage of this opportunity.
     */
    public static boolean canAccessOpportunity(EntityManager em, HttpServletRequest request, Opportunity opp) {
        if (opp == null) return false;

        Person currentPerson = currentPerson(request);
        if (currentPerson == null) return false;
        Long meId = currentPerson.getId();

        // PSP staff retain existing unrestricted access.
        if (flag(request, "isPspAdmin") || flag(request, "isPspUser") || flag(request, "isPspSales")) {
            return true;
        }

        // Owning agent (P1).
        if (opp.getAssignedTo() != null && Objects.equals(opp.getAssignedTo().getId(), meId)) {
            return true;
        }

        // Designated manager (P2).
        if (opp.getManagedBy() != null && Objects.equals(opp.getManagedBy().getId(), meId)) {
            return true;
        }

        // Agency Admin over the opportunity's agency.
        if (flag(request, "isAgencyAdmin") && opp.getAgency() != null
                && belongsToAgency(em, meId, opp.getAgency().getId())) {
            return true;
        }

        return false;
    }

    /**
     * Reassigning the managed-by owner of an opportunity is PSP-Admin only.
     * Kept separate from canAccessOpportunity so the stage-vs-managedBy write paths in
     * UpdateOpportunityStage can be gated at different privilege levels.
     */
    public static boolean canReassignManager(HttpServletRequest request) {
        return flag(request, "isPspAdmin");
    }

    // ────────────────────────────────────────────────────────────────
    // helpers
    // ────────────────────────────────────────────────────────────────

    /**
     * True when the person is the manager of the agency OR a member of its agent list.
     * Mirrors AgentHome.findAgencyForUser resolution as a single COUNT query.
     */
    private static boolean belongsToAgency(EntityManager em, Long personId, Long agencyId) {
        if (personId == null || agencyId == null) return false;
        Long count = em.createQuery(
                        "SELECT COUNT(a) FROM Agency a LEFT JOIN a.agentList ag " +
                                "WHERE a.id = :agencyId " +
                                "AND (a.manager.id = :personId OR ag.id = :personId)",
                        Long.class)
                .setParameter("agencyId", agencyId)
                .setParameter("personId", personId)
                .getSingleResult();
        return count != null && count > 0;
    }

    private static Person currentPerson(HttpServletRequest request) {
        Object localObj = request.getSession().getAttribute("local");
        if (!(localObj instanceof AmsDataLocal local)) return null;
        return local.getCurrentPerson();
    }

    private static boolean flag(HttpServletRequest request, String attr) {
        return Boolean.TRUE.equals(request.getSession().getAttribute(attr));
    }
}

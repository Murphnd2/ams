package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.application.Application;

import java.util.Objects;
import java.util.Set;

/**
 * S58-P3 (D1), widened S58-P4 (Part 3) — answers one question: may the current requester
 * <i>view</i> this setup's enrollment matrix? Read access only; nothing here grants a write.
 * <p>
 * Allow if any holds, evaluated in this order:
 * <ol>
 *   <li><b>PSP staff of the setup's own PSP</b> — the session carries {@code isPspAdmin},
 *       {@code isPspUser} or {@code isPspSales} (S58-P5), and {@link AgencyScopeResolver}'s
 *       {@code pspId} equals {@link #resolveSetupPspId(Setup)}. A PSP staffer of a
 *       <i>different</i> PSP is never admitted here; a setup whose PSP cannot be established
 *       admits no staffer at all (the PSP admin still has the {@code ?setupId=} page);</li>
 *   <li>the requester's {@code currentPerson} is the originating agent
 *       ({@link OriginatingAgencyResolver#resolveAgent(Setup)});</li>
 *   <li>the requester is a member of the originating agency
 *       ({@link OriginatingAgencyResolver#resolve(Setup)} — its existing priority is adopted
 *       as-is, no second answer to "which agency sold this" is derived here);</li>
 *   <li>the requester is a member of that agency's parent agency — one hop up
 *       {@code parent_agency_id}, never further.</li>
 * </ol>
 * Otherwise deny. Membership comes from {@link AgencyScopeResolver#resolve} —
 * {@code detailAgencyIds()} is the set of agencies the requester manages or belongs to — so
 * agency admins get exactly the membership check, the same as a plain agent; there is
 * deliberately no "agency admin is never blocked" shortcut here.
 * <p>
 * <b>Not ICHRA-scoped.</b> This capability does not consult {@code agency.ichra_enabled} and
 * has no relationship to the ICHRA entitlement.
 * <p>
 * The setup's own preconditions (open, matrix not locked) are the caller's to check; this
 * class only answers the relationship question.
 */
public final class MatrixAccessResolver {

    private MatrixAccessResolver() {}

    public static boolean canView(EntityManager em, HttpServletRequest request, Setup setup) {
        if (em == null || request == null || setup == null) return false;

        HttpSession session = request.getSession(false);
        if (session == null) return false;

        Object localObj = session.getAttribute("local");
        Person currentPerson = (localObj instanceof AmsDataLocal local) ? local.getCurrentPerson() : null;
        if (currentPerson == null || currentPerson.getId() == null) return false;

        AgencyScope scope = AgencyScopeResolver.resolve(em, request);

        // 1. PSP staff, scoped to the setup's own PSP. S58-P5: isPspSales included, the same
        //    three flags AgencyScopeResolver treats as pspWide.
        boolean isPspStaff = Boolean.TRUE.equals(session.getAttribute("isPspAdmin"))
                || Boolean.TRUE.equals(session.getAttribute("isPspUser"))
                || Boolean.TRUE.equals(session.getAttribute("isPspSales"));
        if (isPspStaff) {
            Long setupPspId = resolveSetupPspId(setup);
            if (setupPspId != null && setupPspId.equals(scope.pspId())) return true;
            // Not this PSP's setup (or PSP unknown): fall through. pspWide scope carries empty
            // membership sets, so the agency rules below will not admit a staffer either.
        }

        // 2. The originating agent.
        Person originatingAgent = OriginatingAgencyResolver.resolveAgent(setup);
        if (originatingAgent != null && Objects.equals(originatingAgent.getId(), currentPerson.getId())) {
            return true;
        }

        // 3 / 4. Membership of the originating agency, or of its parent (one hop).
        Agency originatingAgency = OriginatingAgencyResolver.resolve(setup);
        if (originatingAgency == null || originatingAgency.getId() == null) return false;

        Set<Long> membership = scope.detailAgencyIds();
        if (membership.isEmpty()) return false;

        if (membership.contains(originatingAgency.getId())) return true;

        Agency parent = originatingAgency.getParentAgency();
        return parent != null && parent.getId() != null && membership.contains(parent.getId());
    }

    /**
     * S58-P4 — the PSP a setup belongs to. {@code Setup}/{@code Activity}/{@code Assignee} carry
     * no PSP column, so it is read off the sale: {@code proposal.rate.psp} first —
     * {@code proposal.rate_id} is {@code NOT NULL} and every {@code Rate} is created under a
     * PSP ({@code RateTableAction}), so this is the deterministic answer — then the originating
     * agency's {@code psp_id} as the fallback. Null when neither resolves; callers treat null
     * as "cannot establish", never as a match.
     */
    public static Long resolveSetupPspId(Setup setup) {
        if (setup == null) return null;

        Application application = setup.getApplication();
        Proposal proposal = application == null ? null : application.getProposal();
        if (proposal != null) {
            Rate rate = proposal.getRate();
            PSP psp = rate == null ? null : rate.getPsp();
            if (psp != null && psp.getId() != null) return psp.getId();
        }

        Agency originatingAgency = OriginatingAgencyResolver.resolve(setup);
        if (originatingAgency != null && originatingAgency.getPsp() != null) {
            return originatingAgency.getPsp().getId();
        }
        return null;
    }
}

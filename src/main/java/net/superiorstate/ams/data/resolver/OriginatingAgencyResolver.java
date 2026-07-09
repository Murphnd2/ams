package net.superiorstate.ams.data.resolver;

import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Assignee;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;

import java.util.Collections;
import java.util.List;

/**
 * Resolves the Agency that originated a sale — the agency whose agents may be
 * delegated specific ToDos on a Setup's checklist ({@link #resolve(Setup)}),
 * or whose agency-level settings (e.g. proposal markup enablement) apply to a
 * Proposal directly, before a Setup necessarily exists ({@link #resolve(Proposal)}).
 *
 * Resolution order (V061 agent-delegation feature; reused as-is by the V067
 * per-agency markup gate):
 *   1. proposal.sourceActivity.assignedTo  (Opportunity's agent of record — strongest signal)
 *   2. proposal.prospect.agent             (prospect's agent of record — outside agent)
 *   3. proposal.createdBy                  (fallback: whoever built the proposal)
 *
 * The Rate → agencyrates join is intentionally NOT consulted (many agencies
 * can share a rate, which wouldn't identify the originator).
 *
 * `createdBy` is the weakest signal because when a PSP user builds a proposal
 * on behalf of an outside agency, `createdBy` resolves to the PSP user (and
 * therefore the PSP's own "agency," not the outside one). `prospect.agent`
 * and the Opportunity's `assignedTo` are the reliable outside-agent signals.
 * This ordering is intentional — a real selling agent on the prospect must
 * win over whoever happened to click "new proposal" — do not reorder it.
 */
public class OriginatingAgencyResolver {

    private OriginatingAgencyResolver() {}

    public static Agency resolve(Setup setup) {
        Person p = resolveAgent(setup);
        return agencyOf(p);
    }

    /**
     * Returns the Person whose agency is treated as the originating one
     * (i.e., the "originating agent"). Same walk as {@link #resolve(Setup)};
     * useful when the UI wants to display the agent's name in addition to
     * the agency.
     */
    public static Person resolveAgent(Setup setup) {
        if (setup == null) return null;
        Application app = setup.getApplication();
        if (app == null) return null;
        Proposal proposal = app.getProposal();
        if (proposal == null) return null;

        return resolveAgent(proposal);
    }

    /**
     * Same resolution as {@link #resolve(Setup)}, but starting directly from
     * a Proposal — for callers (e.g. proposal pricing/markup) that need the
     * originating agency before a Setup necessarily exists.
     */
    public static Agency resolve(Proposal proposal) {
        Person p = resolveAgent(proposal);
        return agencyOf(p);
    }

    /**
     * Returns the Person whose agency is treated as the originating one for
     * this Proposal. Same three-step chain as {@link #resolveAgent(Setup)},
     * starting directly from the Proposal (no Setup/Application unwrap).
     */
    public static Person resolveAgent(Proposal proposal) {
        if (proposal == null) return null;

        // 1. Source Opportunity's assigned agent
        Activity source = proposal.getSourceActivity();
        if (source != null) {
            Person fromSource = personOrNull(source.getAssignedTo());
            if (agencyOf(fromSource) != null) return fromSource;
        }

        // 2. Prospect's agent of record
        Prospect prospect = proposal.getProspect();
        if (prospect != null) {
            Person fromProspectAgent = prospect.getAgent();
            if (agencyOf(fromProspectAgent) != null) return fromProspectAgent;
        }

        // 3. Fallback: proposal's creator
        Person creator = proposal.getCreatedBy();
        if (agencyOf(creator) != null) return creator;

        return null;
    }

    /**
     * Agents of the originating agency (role=2 in practice — caller should
     * trust data integrity rather than filter by role here).
     * Returns empty list when no agency can be resolved.
     */
    public static List<Person> eligibleAgents(Setup setup) {
        Agency a = resolve(setup);
        if (a == null || a.getAgentList() == null) return Collections.emptyList();
        return a.getAgentList();
    }

    private static Person personOrNull(Assignee a) {
        return (a instanceof Person) ? (Person) a : null;
    }

    private static Agency agencyOf(Person p) {
        if (p == null) return null;
        List<Agency> list = p.getListOfAgenciesWithThisAgent();
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }
}

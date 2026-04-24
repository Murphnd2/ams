package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationFieldValue;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads the application-detail data needed by the agent-flavored Setup page.
 *
 * Writes these request attributes (all optional — safe to render the page
 * without them if the Setup has no linked Application):
 *   - {@code agentAppSections}  (List&lt;ApplicationSection&gt;) — suppressed fields removed, sorted
 *   - {@code agentAppValueMap}  (Map&lt;String,String&gt;) — fieldKey → user value
 *   - {@code agentAppLosNames}  (List&lt;String&gt;) — selected Lines of Service
 *   - {@code agentAppEnhNames}  (List&lt;String&gt;) — selected Enhancements
 *
 * Modeled after ReviewApplication servlet's doGet data prep, scoped by the
 * selections on the Application (or falling back to the proposal's LOS list).
 *
 * JSON benefit-plan fields (key {@code bill_benefit_plans}) are intentionally
 * left out of the rendered field list — agents don't need that blob in this
 * view. Document attachments are also hidden at the JSP level.
 */
public final class AgentSetupSnapshotLoader {

    private AgentSetupSnapshotLoader() {}

    public static void load(EntityManager em, Setup setup, HttpServletRequest request) {
        if (setup == null || setup.getApplication() == null) return;
        Application application = setup.getApplication();
        if (application.getProposal() == null) return;
        long proposalId = application.getProposal().getId();

        // Field values → fieldKey/value map
        Query fvq = em.createQuery(
                "SELECT fv FROM ApplicationFieldValue fv " +
                "JOIN FETCH fv.applicationField af " +
                "JOIN FETCH af.applicationSection " +
                "WHERE fv.application.proposal.id = :pid");
        fvq.setParameter("pid", proposalId);
        @SuppressWarnings("unchecked")
        List<ApplicationFieldValue> fieldValues = (List<ApplicationFieldValue>) fvq.getResultList();
        Map<String, String> valueMap = new LinkedHashMap<>();
        for (ApplicationFieldValue fv : fieldValues) {
            valueMap.put(fv.getApplicationField().getFieldKey(), fv.getFieldValue());
        }

        // Scoped section load — same rule as ReviewApplication
        List<Long> losIds;
        List<Long> enhIds;
        if (application.hasServiceSelections()) {
            losIds = application.getSelectedLosIdList();
            enhIds = application.getSelectedEnhancementIdList();
        } else {
            losIds = application.getProposal().getLosList() == null
                    ? new ArrayList<>()
                    : application.getProposal().getLosList().stream().map(LOS::getId).collect(Collectors.toList());
            enhIds = new ArrayList<>();
        }
        List<Long> safeLosIds = (losIds == null || losIds.isEmpty()) ? List.of(-1L) : losIds;
        List<Long> safeEnhIds = (enhIds == null || enhIds.isEmpty()) ? List.of(-1L) : enhIds;

        Query sq = em.createQuery(
                "SELECT DISTINCT s FROM ApplicationSection s " +
                "LEFT JOIN FETCH s.fieldList f " +
                "LEFT JOIN s.losList los " +
                "LEFT JOIN s.enhancementList enh " +
                "WHERE s.suppressed = false AND (s.scope = 'ALL' OR los.id IN :losIds OR enh.id IN :enhIds) " +
                "ORDER BY s.sortOrder");
        sq.setParameter("losIds", safeLosIds);
        sq.setParameter("enhIds", safeEnhIds);
        @SuppressWarnings("unchecked")
        List<ApplicationSection> sections = (List<ApplicationSection>) sq.getResultList();

        // EclipseLink DISTINCT + JOIN FETCH can scramble @OrderBy; re-sort manually
        for (ApplicationSection sec : sections) {
            if (sec.getFieldList() != null) {
                sec.getFieldList().removeIf(ApplicationField::isSuppressed);
                sec.getFieldList().sort(Comparator.comparingInt(ApplicationField::getSortOrder));
            }
        }

        // Selected service names for the header chips
        List<String> losNames = new ArrayList<>();
        List<String> enhNames = new ArrayList<>();
        if (application.hasServiceSelections()) {
            if (application.getProposal().getLosList() != null) {
                for (LOS los : application.getProposal().getLosList()) {
                    if (application.getSelectedLosIdList().contains(los.getId())) {
                        losNames.add(los.getDescription());
                    }
                }
            }
            List<Long> selEnhIds = application.getSelectedEnhancementIdList();
            if (selEnhIds != null && !selEnhIds.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Enhancement> selected = (List<Enhancement>) em.createQuery(
                        "SELECT e FROM Enhancement e WHERE e.id IN :ids ORDER BY e.sortOrder")
                        .setParameter("ids", selEnhIds)
                        .getResultList();
                for (Enhancement e : selected) enhNames.add(e.getDescription());
            }
        } else if (application.getProposal().getLosList() != null) {
            for (LOS los : application.getProposal().getLosList()) {
                losNames.add(los.getDescription());
            }
        }

        request.setAttribute("agentAppSections", sections);
        request.setAttribute("agentAppValueMap", valueMap);
        request.setAttribute("agentAppLosNames", losNames);
        request.setAttribute("agentAppEnhNames", enhNames);
    }
}

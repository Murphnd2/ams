package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.service.SummitDemographicsVerifyService;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S62-P2 -- the Summit setup panel's Demographics "Verify against Summit export" control
 * (T277 -- {@code SummitResponseService}'s "Check response" classifies every Demographics response
 * line {@code UNKNOWN}, so this is a separate, working signal for that one step).
 * <p>
 * Gate: PSP-admin session attribute, then {@link IchraAccessResolver#isAvailable} -- the identical
 * two-step gate {@link SummitResponseServlet} uses (copied from
 * {@code SummitResponseServlet.java:101-105} and {@code :129-132}), so this surface can never
 * disagree with the check-response page or any other ICHRA surface about who may act.
 * <p>
 * ⚠️ <b>No proposal-to-PSP ownership check</b> -- matching {@code SummitResponseServlet}'s own
 * documented precedent ({@code SummitResponseServlet.java:52-58}): neither {@code SummitExportServlet}
 * nor {@code SummitResponseServlet} performs one, so none is introduced here either. This run does
 * not add a new authorization model for Summit surfaces.
 * <p>
 * GET only -- this is a read-only compare (LA-40); there is nothing to POST.
 */
@WebServlet(name = "SummitDemographicsVerifyServlet", value = "/SummitVerifyDemographics")
public class SummitDemographicsVerifyServlet extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Copied from SummitResponseServlet.java:101-105.
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String proposalIdParam = request.getParameter("proposalId");
        if (proposalIdParam == null || proposalIdParam.isBlank()) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "proposalId is required.");
            return;
        }

        long proposalId;
        try {
            proposalId = Long.parseLong(proposalIdParam.trim());
        } catch (NumberFormatException e) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid proposalId.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Copied from SummitResponseServlet.java:129-132 -- same order, same resolver.
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Resolves Proposal -> Prospect the same way SummitEmployerLinkServlet.java:74-76 does.
            Proposal proposal = em.find(Proposal.class, proposalId);
            Prospect prospect = proposal != null ? proposal.getProspect() : null;
            if (prospect == null) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "proposalId " + proposalId + " does not resolve to a Proposal with a Prospect.");
                return;
            }

            // S62-P5 -- pspId from the session, matching SummitSetupStatusServlet.java:123-129
            // exactly, rather than S62-P4's proposal.getRate().getPsp() inference (removed).
            Long pspId = resolveCurrentPspId(request);

            SummitDemographicsVerifyService.Result result =
                    SummitDemographicsVerifyService.verify(em, proposalId, pspId, prospect.getId());

            // The JSP never touches SummitDemographicsVerifyService's records directly -- matching
            // SummitResponseServlet.java:174-177's own reasoning verbatim: EL property resolution
            // is JavaBean-getter based, and a record's accessors carry no "get" prefix. Everything
            // the page needs is flattened here into request attributes and a plain
            // List<Map<String,Object>> for the per-participant table, which EL resolves natively.
            request.setAttribute("proposalId", proposalId);
            request.setAttribute("outcome", result.outcome().name());
            request.setAttribute("fileName", result.fileName());
            request.setAttribute("fileTimestampDisplay",
                    result.fileTimestamp() == null ? null : DISPLAY_FORMAT.format(result.fileTimestamp()));
            request.setAttribute("missingHeaderNames", result.missingHeaderNames());
            request.setAttribute("errorMessage", result.errorMessage());
            // S62-P4 -- verdict is null except for Outcome.OK; .name() would NPE, so this stays a
            // plain conditional rather than the ternary style used for fileTimestampDisplay above.
            request.setAttribute("verdict", result.verdict() == null ? null : result.verdict().name());
            request.setAttribute("expectedEmployerKey", result.expectedEmployerKey());
            request.setAttribute("totalDataRows", result.totalDataRows());
            request.setAttribute("employerMatchingRowCount", result.employerMatchingRowCount());
            request.setAttribute("lastPushTimestampDisplay",
                    result.lastPushTimestamp() == null ? null : DISPLAY_FORMAT.format(result.lastPushTimestamp()));
            request.setAttribute("manualMarkDoneOnly", result.manualMarkDoneOnly());
            request.setAttribute("expectedCount", result.expectedCount());
            request.setAttribute("foundCount", result.foundCount());
            request.setAttribute("wrongEmployerCount", result.wrongEmployerCount());
            request.setAttribute("missingCount", result.missingCount());
            request.setAttribute("unkeyedCount", result.unkeyedCount());

            List<Map<String, Object>> participantRows = new ArrayList<>();
            for (SummitDemographicsVerifyService.ParticipantCheck p : result.participants()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("firstName", p.firstName());
                row.put("lastName", p.lastName());
                row.put("expectedKey", p.expectedKey());
                row.put("state", p.state().name());
                row.put("userStatus", p.userStatus());
                participantRows.add(row);
            }
            request.setAttribute("participants", participantRows);

            // Same rule as SummitResponseServlet.java:160 -- only true, and only then shown, when
            // the session's current activity really is this proposal's Setup.
            request.setAttribute("backToSetup", sessionActivityIsProposal(request, proposalId));

            request.getRequestDispatcher("/WEB-INF/view/market/summitVerifyDemographics25.jsp")
                    .forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Duplicated from {@code SummitResponseServlet.java:412-428} rather than shared -- that method
     * is {@code private}, and this codebase's own established precedent (T254, three fragment
     * servlets each copying {@code resolveCurrentPspId} rather than sharing it) is to duplicate a
     * small helper like this rather than widen an existing class's visibility for one new caller.
     * Walks the identical chain {@code detailSummitSetup25.jsp}'s proposalId expression walks;
     * null-safe at every hop, never throws.
     */
    private static boolean sessionActivityIsProposal(HttpServletRequest request, Long proposalId) {
        if (proposalId == null) return false;
        try {
            Object attribute = request.getSession().getAttribute("local");
            if (!(attribute instanceof AmsDataLocal local)) return false;
            if (local.getCurrentActivity() == null) return false;
            Activity activity = local.getCurrentActivity().getActivity();
            if (!(activity instanceof Setup setup)) return false;
            Application application = setup.getApplication();
            if (application == null) return false;
            Proposal setupProposal = application.getProposal();
            if (setupProposal == null || setupProposal.getId() == null) return false;
            return setupProposal.getId().equals(proposalId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * S62-P5 -- copied from {@code SummitSetupStatusServlet.java:123-129} rather than shared,
     * matching this codebase's own T254 precedent (three fragment servlets each independently
     * copying this exact method). Session-based, not proposal-derived -- replaces S62-P4's
     * {@code proposal.getRate().getPsp()} inference, which this run removed as an unproven
     * invariant now that the session is available here.
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
    }
}

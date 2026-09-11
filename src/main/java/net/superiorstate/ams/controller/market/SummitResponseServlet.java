package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SummitFileExportDAO;
import net.superiorstate.ams.data.dao.SummitSetupStepDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.service.SummitResponseService;
import net.superiorstate.ams.data.service.SummitSftpService;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.SummitFileExport;
import net.superiorstate.ams.model.market.SummitSetupStep;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.application.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * S45-B -- T230 phase 1. Setup-panel "Check response" and "Mark done" for the three pushable
 * steps ({@code employer}, {@code cdhplan}, {@code demographics}) plus the hand-built
 * {@code schedules} checkpoint. {@code enrollment} is priority 2 and is not in the whitelist here.
 * <p>
 * Gated exactly as {@link SummitExportServlet} is -- PSP-admin session attribute, then
 * {@link IchraAccessResolver#isAvailable}, same order, same resolver -- so this surface can never
 * disagree with the export screen or any other ICHRA surface about who may act.
 * <p>
 * ⚠️ <b>No response content is stored.</b> {@link #doGet} runs {@link SummitResponseService#check}
 * fresh on every request and forwards the result to the JSP for this one render; nothing from a
 * response file is written to {@code summit_setup_step} or anywhere else. Only the fact and basis
 * of a Mark done decision persist.
 * <p>
 * Unlike {@code SummitExportServlet}, this servlet never loads a {@code Proposal} or
 * {@code Prospect} entity -- every read and write here operates on the {@code proposalId} scalar
 * plus the session-resolved {@code pspId}, matching how {@code SummitSetupStepDAO} and the
 * {@code SummitFileExport} finders it adds are themselves scalar-keyed. {@code SummitExportServlet}
 * itself performs no proposal-to-PSP ownership check (confirmed by inspection, S45a Phase A); none
 * is added here either, for the same reason that servlet has none -- this run does not introduce a
 * new authorization model for Summit surfaces.
 */
@WebServlet(name = "SummitResponseServlet", value = "/SummitResponse")
public class SummitResponseServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitResponseServlet.class);

    private static final String STEP_EMPLOYER = "employer";
    private static final String STEP_CDHPLAN = "cdhplan";
    private static final String STEP_SCHEDULES = "schedules";
    private static final String STEP_DEMOGRAPHICS = "demographics";

    /** S45c -- matches SummitSetupStatusServlet.DISPLAY_FORMAT; raw LocalDateTime renders as
     *  an ISO instant (e.g. 2026-09-10T11:12:46) on the check page, so every LocalDateTime this
     *  servlet forwards is pre-formatted here, not left to the JSP. */
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> STEP_LABELS = Map.of(
            STEP_EMPLOYER, "Employer (file 1)",
            STEP_CDHPLAN, "Plans — Employer CDH Plan (file 2)",
            STEP_SCHEDULES, "Contribution schedules (hand-built in Summit)",
            STEP_DEMOGRAPHICS, "Demographics — participants"
    );

    /** File type per step. {@code schedules} is deliberately absent -- it has no pushed file. */
    private static final Map<String, String> STEP_FILE_TYPES = Map.of(
            STEP_EMPLOYER, "employer",
            STEP_CDHPLAN, "cdhplan",
            STEP_DEMOGRAPHICS, "demographics"
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String proposalIdParam = request.getParameter("proposalId");
        String step = request.getParameter("step");
        if (proposalIdParam == null || proposalIdParam.isBlank()
                || step == null || !STEP_LABELS.containsKey(step)) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "proposalId and step (employer|cdhplan|schedules|demographics) are required.");
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
            // PSP admin, then ICHRA entitlement -- same order and same resolver SummitExportServlet
            // uses, so this gate can never disagree with the rest of the feature.
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Long pspId = resolveCurrentPspId(request);
            String fileType = STEP_FILE_TYPES.get(step);

            SummitSetupStep stepState = SummitSetupStepDAO.findByProposalAndStep(em, pspId, proposalId, step);
            SummitFileExport latestPushed = fileType == null ? null
                    : SummitSetupStepDAO.findLatestPushed(em, pspId, proposalId, fileType);
            SummitFileExport latestAttempt = fileType == null ? null
                    : SummitSetupStepDAO.findLatestDeliveryAttempt(em, pspId, proposalId, fileType);

            SummitResponseService.CheckResult checkResult = null;
            if (latestPushed != null) {
                checkResult = SummitResponseService.check(new SummitSftpService(), latestPushed);
            }

            request.setAttribute("proposalId", proposalId);
            request.setAttribute("step", step);
            request.setAttribute("stepLabel", STEP_LABELS.get(step));
            request.setAttribute("stepState", stepState);
            request.setAttribute("latestPushed", latestPushed);
            request.setAttribute("latestAttempt", latestAttempt);

            // S45d -- Back to setup link. Only true, and only then shown, when the session's
            // current activity really is this proposal's Setup -- ViewActivity25 renders whatever
            // activity is current with no parameters (S45e: not GoActivityDetail25, which 500s on
            // a bare GET), so a link there would be wrong (opening a different activity) whenever
            // the session has since moved on.
            request.setAttribute("backToSetup", sessionActivityIsProposal(request, proposalId));

            // S45c -- pre-formatted display strings for the three LocalDateTime values the JSP
            // shows. summitFileExportAdmin25.jsp's ${e.generatedAt} was checked as a model (R3)
            // and does not format at all -- it renders LocalDateTime's raw ISO toString(), the
            // same "2026-09-10T11:12:46" defect being fixed here. Formatting happens in Java, not
            // EL, matching the checkResult/ResponseCheck flattening already done below.
            request.setAttribute("stepUpdatedDisplay",
                    formatDisplay(stepState == null ? null : stepState.getUpdatedAt()));
            request.setAttribute("deliveredDisplay",
                    formatDisplay(latestPushed == null ? null : latestPushed.getDeliveredAt()));
            request.setAttribute("latestAttemptDisplay",
                    formatDisplay(latestAttempt == null ? null : latestAttempt.getDeliveredAt()));

            // The JSP never touches SummitResponseService's records directly -- Java's EL property
            // resolution is JavaBean-getter based, and a record's accessors carry no "get" prefix.
            // Everything the page needs is flattened here into request attributes and a plain
            // List<Map<String,Object>> for the per-line table, which EL resolves natively.
            if (checkResult != null) {
                request.setAttribute("checkFound", checkResult.found());
                request.setAttribute("checkResponseDir", checkResult.responseDir());
                request.setAttribute("checkResponseName", checkResult.responseName());
                request.setAttribute("checkSizeBytes", checkResult.sizeBytes());
                request.setAttribute("checkError", checkResult.error());

                SummitResponseService.ResponseCheck rc = checkResult.responseCheck();
                if (rc != null) {
                    request.setAttribute("rcSentRows", rc.sentRows());
                    request.setAttribute("rcLineCount", rc.lineCount());
                    request.setAttribute("rcOkCount", rc.okCount());
                    request.setAttribute("rcFailedCount", rc.failedCount());
                    request.setAttribute("rcUnknownCount", rc.unknownCount());
                    request.setAttribute("rcCountMismatch", rc.countMismatch());
                    request.setAttribute("rcAllOk", rc.allOk());

                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (SummitResponseService.ResponseLine line : rc.lines()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("lineNo", line.lineNo());
                        row.put("status", line.status());
                        row.put("classification", line.classification());
                        List<String> fields = line.fields();
                        row.put("middle", fields.size() > 2
                                ? String.join(" | ", fields.subList(1, fields.size() - 1))
                                : "");
                        row.put("comment", line.comment());
                        rows.add(row);
                    }
                    request.setAttribute("rcRows", rows);
                }
            }

            request.getRequestDispatcher("/WEB-INF/view/market/summitResponse25.jsp")
                    .forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String proposalIdParam = request.getParameter("proposalId");
        String step = request.getParameter("step");
        String action = request.getParameter("action");
        if (proposalIdParam == null || proposalIdParam.isBlank()
                || step == null || !STEP_LABELS.containsKey(step)
                || action == null || !(action.equals("markdone") || action.equals("reopen"))) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "proposalId, step (employer|cdhplan|schedules|demographics) and action"
                            + " (markdone|reopen) are required.");
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
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Long pspId = resolveCurrentPspId(request);
            String user = resolveCurrentUserName(request);

            if (action.equals("markdone")) {
                Long exportId = null;
                String exportIdParam = request.getParameter("exportId");
                if (exportIdParam != null && !exportIdParam.isBlank()) {
                    try {
                        exportId = Long.valueOf(exportIdParam.trim());
                    } catch (NumberFormatException e) {
                        writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid exportId.");
                        return;
                    }
                }

                if (exportId != null) {
                    // Reviewed basis -- the export must be this proposal's, this step's file
                    // type, actually PUSHED, and (to the extent the session resolved one) this
                    // PSP's. SummitFileExportDAO.findById does not scope by PSP itself (its own
                    // javadoc says so), so that comparison is made here, exactly as its javadoc
                    // requires of every caller.
                    SummitFileExport export = SummitFileExportDAO.findById(em, exportId);
                    String fileType = STEP_FILE_TYPES.get(step);
                    boolean valid = export != null
                            && Objects.equals(export.getPspId(), pspId)
                            && Objects.equals(export.getProposalId(), proposalId)
                            && Objects.equals(export.getFileType(), fileType)
                            && "PUSHED".equals(export.getDeliveryStatus());
                    if (!valid) {
                        writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                                "exportId does not match a PUSHED export for this proposal and step.");
                        return;
                    }
                    SummitSetupStepDAO.markDone(em, pspId, proposalId, step, "REVIEWED", exportId, user);
                } else {
                    SummitSetupStepDAO.markDone(em, pspId, proposalId, step, "MANUAL", null, user);
                }
            } else {
                SummitSetupStepDAO.reopen(em, pspId, proposalId, step, user);
            }
        } finally {
            if (em.isOpen()) em.close();
        }

        redirectAfterPost(request, response, proposalId, step);
    }

    /**
     * Redirects back to where the POST came from when that is safe, else to this servlet's own
     * check page. "Safe" means the {@code Referer} parses as a URI (which also rejects a raw
     * CR/LF in the header -- {@link URI}'s parser has no valid production for bare control
     * characters, so this is already the CR/LF guard, not a separate check) and its host is
     * absent or matches this request's server name.
     * <p>
     * ⚠️ S45c -- the panel that submits this form is reached at {@code /GoActivityDetail25}, not
     * {@code /ViewActivity25} directly (R1/Kevin's runtime walk, 2026-09-10). ⚠️ S45d -- Mark
     * done/Reopen submitted from the check page itself needs its own rule, because landing back on
     * the check page leaves no way back to the activity. ⚠️ S45e -- runtime-verified 2026-09-10: a
     * bare {@code GET /GoActivityDetail25} (no parameters) throws
     * {@code NumberFormatException: Cannot parse null string} at
     * {@code GoActivityDetail25.viewActivity} -- it declaring its own {@code doGet} (S45c R1) does
     * <b>not</b> mean a parameterless GET is valid, and S45c's premise there was wrong. A bare
     * {@code GET /ViewActivity25} (no parameters), by contrast, does render the session's current
     * activity correctly (also runtime-verified 2026-09-10) -- so every redirect this method issues
     * now targets {@code /ViewActivity25}, never {@code /GoActivityDetail25}, even though
     * {@code /GoActivityDetail25} is still recognized as a Referer (the panel is reached there).
     * Four rules, in order --
     * <ol>
     *   <li>{@code /ViewActivity25} or {@code /ViewChecklist25} -- redirect to the Referer verbatim,
     *       as before this run.</li>
     *   <li>{@code /SummitResponse} itself (S45d) -- redirect to {@code /ViewActivity25} when
     *       {@link #sessionActivityIsProposal} says the session's current activity is this
     *       proposal's Setup; otherwise redirect back to the check page (the Referer), unchanged
     *       from before this run. {@code ViewActivity25} renders whatever activity is current in
     *       the session with no parameters, so it is only safe to send the user there when that
     *       activity really is this proposal.</li>
     *   <li>{@code /GoActivityDetail25} (S45e) -- recognized as a Referer because the panel is
     *       reached there, but the redirect target is {@code /ViewActivity25}, guarded by the same
     *       {@link #sessionActivityIsProposal} check as rule 2 -- a bare {@code GoActivityDetail25}
     *       is never a valid target (see above). When the guard is false, fall through to rule 4's
     *       fixed check-page fallback rather than the Referer, since redirecting back to a bare
     *       {@code GoActivityDetail25} would 500.</li>
     *   <li>Anything else -- fall through to this servlet's own check page, as before.</li>
     * </ol>
     */
    private void redirectAfterPost(HttpServletRequest request, HttpServletResponse response,
                                    long proposalId, String step) throws IOException {
        String contextPath = request.getContextPath();
        String fallback = contextPath + "/SummitResponse?proposalId=" + proposalId + "&step=" + step;
        String target = fallback;

        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = new URI(referer);
                String host = uri.getHost();
                boolean hostOk = host == null || host.equalsIgnoreCase(request.getServerName());
                String path = uri.getPath();

                if (hostOk && path != null) {
                    if (path.startsWith(contextPath + "/ViewActivity25")
                            || path.startsWith(contextPath + "/ViewChecklist25")) {
                        target = referer;
                    } else if (path.startsWith(contextPath + "/SummitResponse")) {
                        // S45d -- Mark done/Reopen submitted from the check page itself. Landing
                        // back on the check page (the old behaviour) leaves no way back to the
                        // activity, so prefer ViewActivity25 -- but only when the session's current
                        // activity really is this proposal's Setup (sessionActivityIsProposal),
                        // since ViewActivity25 renders whatever activity is current with no
                        // parameters and would otherwise silently show a different one. S45e --
                        // target changed from /GoActivityDetail25 to /ViewActivity25; a bare
                        // GoActivityDetail25 500s (runtime-verified), a bare ViewActivity25 does not.
                        target = sessionActivityIsProposal(request, proposalId)
                                ? contextPath + "/ViewActivity25"
                                : referer;
                    } else if (path.startsWith(contextPath + "/GoActivityDetail25")) {
                        // S45e -- recognized as a Referer (the panel is reached here), but never a
                        // redirect target: a bare GET here throws NumberFormatException (Kevin's
                        // runtime walk, 2026-09-10). Redirect to ViewActivity25 instead, guarded the
                        // same way rule 2 is; when the guard fails, leave target as the fixed
                        // check-page fallback already assigned above rather than the Referer, since
                        // redirecting back to a bare GoActivityDetail25 would itself 500.
                        if (sessionActivityIsProposal(request, proposalId)) {
                            target = contextPath + "/ViewActivity25";
                        }
                    }
                    // else: rule 4 -- keep the fixed fallback already set above.
                }
            } catch (URISyntaxException ignored) {
                // Also catches a Referer carrying raw CR/LF; fall through to the fixed fallback.
            }
        }
        response.sendRedirect(target);
    }

    /**
     * S45d -- true only when the session's current activity (as {@code detailSummitSetup25.jsp}'s
     * own gate reads it) is a Setup whose application's proposal is {@code proposalId}. This is the
     * guard that makes redirecting to {@code /ViewActivity25} (which renders whatever activity is
     * current in the session, with no parameters -- runtime-verified S45e; {@code GoActivityDetail25}
     * looked like the same thing but 500s on a bare GET) safe: it is only the right target when that
     * session activity actually is this proposal's Setup.
     * <p>
     * Walks the identical chain {@code detailSummitSetup25.jsp}'s proposalId expression walks --
     * {@code local.getCurrentActivity().getActivity().getApplication().getProposal().id} (verified
     * current at {@code detailSummitSetup25.jsp:33/37/39} in this run; s45b's own edits shifted it
     * off the s45d prompt's cited line 36) -- except {@code Activity} itself declares no
     * {@code getApplication()} (only {@link Setup} does), so this method's Java equivalent needs an
     * {@code instanceof} where the JSP's EL resolves the same call dynamically against the runtime
     * object.
     * <p>
     * Null-safe at every hop and never throws -- a {@link RuntimeException} anywhere in the chain
     * (session serialization oddities included) is caught and treated as "no match", never as a
     * reason to fail the redirect this guards.
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
            Proposal proposal = application.getProposal();
            if (proposal == null || proposal.getId() == null) return false;
            return proposal.getId().equals(proposalId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Reads {@code local.getCurrentPerson().getPsp()}, the same pattern
     * {@code SummitExportServlet.resolveCurrentPspId} uses.
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /**
     * Display-only acting-user name, the same pattern {@code SummitExportServlet.resolveCurrentUserName}
     * uses. Null when the session cannot name anyone -- a supported state, not an error.
     */
    private static String resolveCurrentUserName(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        return local.getCurrentPerson().getFullName();
    }

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
    }

    /** S45c -- {@code yyyy-MM-dd HH:mm}, matching {@code SummitSetupStatusServlet}'s fragment. Null-safe. */
    private static String formatDisplay(LocalDateTime dateTime) {
        return dateTime == null ? null : DISPLAY_FORMAT.format(dateTime);
    }
}

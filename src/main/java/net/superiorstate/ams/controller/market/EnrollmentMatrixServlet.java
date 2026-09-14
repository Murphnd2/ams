package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixEntryDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixParticipantDAO;
import net.superiorstate.ams.data.dao.CoverageTierDAO;
import net.superiorstate.ams.data.dao.PayrollFrequencyDAO;
import net.superiorstate.ams.data.dao.PaycycleFrequencyAliasDAO;
import net.superiorstate.ams.data.dao.SummitPlanTemplateMapDAO;
import net.superiorstate.ams.data.resolver.MatrixAccessResolver;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.data.service.MatrixCompletenessService;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.market.EnrollmentMatrix;
import net.superiorstate.ams.model.market.EnrollmentMatrixEntry;
import net.superiorstate.ams.model.market.EnrollmentMatrixParticipant;
import net.superiorstate.ams.model.market.CoverageTier;
import net.superiorstate.ams.model.market.PayrollFrequency;
import net.superiorstate.ams.model.market.PaycycleFrequencyAlias;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationModule;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * s52k — the enrollment matrix screen: reads V105's {@code enrollment_amount_mode} and writes
 * V106's three tables. {@code /EnrollmentMatrix?setupId=N}. No exporter, no reports this build.
 * <p>
 * <b>Gate: PSP admin only.</b> {@link #isAuthorized(HttpSession)} is copied verbatim from
 * {@code SummitPlanTemplateAdmin}, itself copied verbatim from {@code RateCacheAdmin}. <b>No nav
 * entry is added here</b> — {@code navbar25.jsp} is untouched; this screen is reachable by URL
 * only, deliberately, for this build.
 * <p>
 * <b>Setup → legs.</b> {@code setup.getApplication().getProposal().getId()} is the proposal id;
 * {@link #loadElectedServiceItemIds} mirrors {@code SummitExportServlet.loadElectedServiceItems}'s
 * JPQL exactly (that method is private and lives in a file this build may not touch, so the query
 * is duplicated here rather than shared) to get the set of elected {@code ServiceItem} ids. Legs
 * are {@link SummitPlanTemplateMapDAO#findActiveByPspId} filtered to those ids and to
 * {@code enrollmentAmountMode != "NONE"}, preserving that method's own {@code sortOrder, seq, id}
 * emit order — no new query method was added to that DAO (out of scope this run).
 * <p>
 * <b>Setup → participants.</b> {@code setup.getApplication().getProposal().getProspect().getId()}
 * is the prospect id; {@link EmployerParticipantDAO#findByProspectId} already orders
 * {@code lastName, firstName, id} — exactly "alphabetised last, first" — so no new query was
 * needed for the roster either.
 * <p>
 * <b>Matrix row is created lazily on first open</b> — {@link #findOrCreateMatrix}, the way
 * {@code Application} rows are created lazily, never on setup creation.
 * <p>
 * <b>Participant header and entry rows are created lazily on first SAVE, not on open.</b> The
 * prompt only specifies lazy creation for the top-level matrix row; extending the same principle
 * downward avoids inserting hundreds of empty header/entry rows for a roster nobody has touched
 * yet just from opening the page. A GET with no saved data yet renders every input blank.
 * <p>
 * <b>One save for the whole matrix</b> — a single POST carries every unlocked participant's
 * fields. Picked over per-row saves because this is a plain JSP with no JS framework, and a
 * matrix-shaped screen is naturally edited a row at a time before one submit, the same way
 * {@code SummitPlanTemplateAdmin} commits one mapping at a time from one form. Unlocking a
 * locked row is a separate, smaller POST ({@code action=unlock}) with its own confirm, since a
 * locked row's inputs are disabled and so never appear in the whole-form save.
 * <p>
 * ⚠️ <b>No agreement is enforced between a leg's {@code enrollmentAmountMode} and what gets
 * saved.</b> An {@code amount} typed against a {@code TIER} leg, or a {@code tierName} typed
 * against a {@code MONTHLY_PREMIUM} leg, is stored as submitted. Validation is a later decision
 * (per this build's instructions) and guessing it now would be schema-shaped.
 * <p>
 * <b>s53d — the payroll-frequency select is now built from {@link PayrollFrequencyDAO}</b>
 * ({@code findEnrollmentApproved()}, s53c/V107), plus any stored value already on a row in this
 * matrix that is no longer enrollment-approved (so un-approving a frequency never silently blanks
 * a row that already holds it), plus the two sentinels {@code OTHER_CUSTOM} and
 * {@code OTHER_NOT_IMPORTABLE}. A hand-crafted POST with any other string is still stored
 * verbatim; the column is code-validated later, not here.
 * <p>
 * <b>S57-P4 — no render-time payroll-frequency default.</b> The V107 column this used to
 * default from was retired (TA-16) in favor of {@code PaycycleFrequencyAliasDAO} (V110); a
 * row with no stored value now renders with no preselection until the TA-15 matrix filter
 * ships.
 * <p>
 * <b>Push lock.</b> If {@link EnrollmentMatrix#isPushed()}, the whole table renders read-only and
 * {@link #save} refuses with a flash error before touching any row. No exporter sets this column
 * yet, so this path is unreachable by normal use today; it is built and gated the same way the
 * entry lock is, and was verified by code inspection only — no live row with {@code is_pushed=1}
 * was created to exercise it, since creating test data is out of scope for this build.
 * <p>
 * <b>S58-P3 — GUID entry path, {@code /matrix/{guid}}, read only.</b> Routed the way
 * {@code ViewProposal} routes {@code /proposal/{guid}} (path info, not a parameter). The GUID
 * is {@code enrollment_matrix.access_guid} (V112) — an identifier, not a credential: the
 * request is authenticated by {@code LoginFilter} and authorised by
 * {@link MatrixAccessResolver} on the requester's relationship to the setup. Preconditions
 * checked on every request: the setup is open ({@code Activity.is_complete} false) and the
 * matrix is not locked ({@code is_pushed} false — nothing sets it today, so that check is inert
 * until freeze-on-push lands). Unknown GUID, closed setup, locked matrix and failed
 * authorisation all take the one {@link #refuse} path and are indistinguishable to the caller.
 * The GUID path looks the matrix up by GUID and never calls {@link #findOrCreateMatrix} — a
 * viewer cannot cause an INSERT.
 * <p>
 * <b>S58-P4 — the GUID path renders its own standalone page</b>, {@link #AGENT_VIEW}
 * ({@code matrixAgentView25.jsp}): display markup only, no form, no inputs, no navbar, no
 * {@code <base>} tag. The {@code ?setupId=} path renders {@link #VIEW}
 * ({@code enrollmentMatrix25.jsp}) exactly as before, PSP-admin only. Both go through
 * {@link #renderMatrix}, which is parameterised by the view it forwards to. Mutations:
 * {@code save} (incl. the lock checkbox) and {@code unlock} stay PSP-admin server-side;
 * {@code issueLink} is any PSP user or admin, matching {@link MatrixAccessResolver}'s
 * PSP-scoped widening.
 * <p>
 * {@code action=issueLink} (POST) lazily generates the GUID and answers JSON
 * {@code {"url": ...}} for the "Copy matrix link" / "Open agent view" controls on the PSP
 * setup screen.
 */
@WebServlet(name = "EnrollmentMatrixServlet", value = {"/EnrollmentMatrix", "/matrix/*"})
public class EnrollmentMatrixServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/view/market/enrollmentMatrix25.jsp";

    /** S58-P4 — the standalone read-only page the {@code /matrix/{guid}} path forwards to. */
    private static final String AGENT_VIEW = "/WEB-INF/view/market/matrixAgentView25.jsp";

    /** S58-P4 — the application answers the agent page's header shows when present (same keys {@code SummitExportServlet} reads). */
    private static final String FIELD_PLAN_YEAR_START = "plan_year_start";
    private static final String FIELD_PLAN_YEAR_END = "plan_year_end";

    /**
     * S58-P5 — the session attribute carrying a logged-out visitor's {@code /matrix/{guid}}
     * destination across login. Set only by {@link #doGetByGuid}, consumed (and always removed,
     * single use) by {@code AuthenticateUser.goToPage}. Deliberately one shape of destination
     * for one feature — not a general "return to any internal path" facility.
     */
    public static final String MATRIX_RETURN_ATTR = "matrixReturnPath";

    /**
     * S58-P5 — the only destination shape that is ever captured or consumed: {@code /matrix/}
     * followed by one canonical UUID (the exact shape {@link #issueLink} emits). Anchored at both
     * ends, fixed length (44 chars), no query string, no fragment, no host. Because the stored
     * value is <i>matched</i> against this rather than sanitised, there is no open-redirect
     * surface; widening this pattern would reintroduce one.
     */
    public static final Pattern MATRIX_RETURN_PATTERN = Pattern.compile(
            "^/matrix/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** S58-P5 — hard cap checked before the pattern; the pattern itself is 44 characters. */
    private static final int MATRIX_RETURN_MAX_LENGTH = 64;

    private static final String FLASH_MESSAGE = "enrollmentMatrixMessage";
    private static final String FLASH_ERROR = "enrollmentMatrixError";

    /** S58-P7 — the agent view's own flash keys, kept apart from the PSP page's. */
    private static final String AGENT_FLASH_MESSAGE = "matrixAgentMessage";
    private static final String AGENT_FLASH_ERROR = "matrixAgentError";
    /** S58-P7 — which participant the last {@code agentSave} landed on, so the page can say so. */
    private static final String AGENT_FLASH_PARTICIPANT = "matrixAgentSavedParticipantId";

    /** S58-P7 — V113 column width for {@code agent_schedule_note}. */
    private static final int AGENT_NOTE_MAX_LENGTH = 500;

    /**
     * s53d — the {@code ApplicationField} key whose answer defaults the matrix dropdown at
     * render time (s53b). Referenced by name, not retyped at each call site.
     */
    private static final String FIELD_PAYCYCLE_FREQUENCY = "paycycle_frequency";

    /** S57-P6/TA-15 — the other two answers the schedule-suggestion filter reads. */
    private static final String FIELD_PAYCYCLE_FIRST_PAYDATE = "paycycle_first_paydate";
    private static final String FIELD_PAYCYCLE_OTHER_HAVE = "paycycle_other_have";

    /** Referenced from {@link PayrollFrequency}, never retyped as a literal (s53d). */
    private static final String OTHER_CUSTOM = PayrollFrequency.OTHER_CUSTOM;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // S58-P3 -- /matrix/{guid} carries path info; /EnrollmentMatrix?setupId= never does.
        // The GUID path has its own gate (MatrixAccessResolver) and never reaches the
        // PSP-admin check below.
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            doGetByGuid(request, response, pathInfo);
            return;
        }

        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        Long setupId = parseLongOrNull(request.getParameter("setupId"));
        if (setupId == null) {
            // s52m -- this and the three branches below were silent redirects (T-none, s52l
            // diagnosis session). A PSP admin who is allowed to be here needs to see why the
            // page refused; the gate branch above stays a redirect because a non-PSP-admin must
            // not learn the page exists at all.
            request.setAttribute("guardMessage", "No setup id was given.");
            forward(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = resolveCurrentPspId(request);
            if (pspId == null) {
                request.setAttribute("guardMessage", "Could not determine your PSP from this session.");
                forward(request, response);
                return;
            }

            Setup setup = em.find(Setup.class, setupId);
            if (setup == null) {
                request.setAttribute("guardMessage", "No setup found for id " + setupId + ".");
                forward(request, response);
                return;
            }
            Application application = setup.getApplication();
            if (application == null || application.getProposal() == null) {
                request.setAttribute("guardMessage", "Setup " + setupId + " has no linked application.");
                forward(request, response);
                return;
            }
            long proposalId = application.getProposal().getId();
            Prospect prospect = application.getProposal().getProspect();

            EnrollmentMatrix matrix = findOrCreateMatrix(em, setupId, resolveCurrentUserName(request));

            renderMatrix(request, response, em, setupId, pspId, proposalId, prospect, matrix, VIEW);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * S58-P3 — {@code GET /matrix/{guid}}. Find-only lookup, preconditions, then
     * {@link MatrixAccessResolver}, then the same render the {@code ?setupId=} path uses.
     * Every failure — no such GUID, closed setup, locked matrix, not authorised, or a matrix
     * whose setup no longer resolves to a sale — is {@link #refuse}, identically.
     */
    private void doGetByGuid(HttpServletRequest request, HttpServletResponse response, String pathInfo)
            throws ServletException, IOException {
        // S58-P5 -- /matrix/ is exempt from LoginFilter, so this servlet owns its own
        // authentication. The check is LoginFilter's own test (session present and
        // AmsDataLocal.isAuthenticated()), and it runs BEFORE the GUID is looked up or any
        // database read happens: a logged-out visitor gets the same login redirect for a
        // fabricated GUID as for a real one, so the response never reveals which GUIDs exist.
        if (!isAuthenticatedSession(request)) {
            captureMatrixReturnPath(request);
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String guid = pathInfo.length() < 2 ? null : pathInfo.substring(1);

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Find only -- EnrollmentMatrixDAO.findByAccessGuid never inserts, and a null/blank
            // guid returns null without a query. findOrCreateMatrix is not on this path.
            EnrollmentMatrix matrix = EnrollmentMatrixDAO.findByAccessGuid(em, guid);
            if (matrix == null) { refuse(response); return; }

            Setup setup = em.find(Setup.class, matrix.getSetupId());
            if (setup == null) { refuse(response); return; }

            // D2 -- checked on every request, not only when the link was issued.
            if (setup.isComplete()) { refuse(response); return; }   // open = Activity.is_complete false
            if (matrix.isPushed()) { refuse(response); return; }    // locked = enrollment_matrix.is_pushed

            // D1 -- the relationship question.
            if (!MatrixAccessResolver.canView(em, request, setup)) { refuse(response); return; }

            Application application = setup.getApplication();
            if (application == null || application.getProposal() == null) { refuse(response); return; }
            long proposalId = application.getProposal().getId();
            Prospect prospect = application.getProposal().getProspect();

            // The legs are the sale's PSP's plan-template rows -- the same PSP the access
            // resolver scoped PSP staff against (proposal.rate.psp, else the originating
            // agency's PSP). The session PSP is a last resort for an agent whose Person row
            // names one; an unresolvable PSP is refused like every other failure.
            Long pspId = MatrixAccessResolver.resolveSetupPspId(setup);
            if (pspId == null) pspId = resolveCurrentPspId(request);
            if (pspId == null) { refuse(response); return; }

            // S58-P4 -- header facts for the standalone page. Absent answers render nothing.
            request.setAttribute("planYearStart", resolveApplicationAnswer(em, proposalId, FIELD_PLAN_YEAR_START));
            request.setAttribute("planYearEnd", resolveApplicationAnswer(em, proposalId, FIELD_PLAN_YEAR_END));
            Agency originatingAgency = OriginatingAgencyResolver.resolve(setup);
            request.setAttribute("originatingAgencyName", originatingAgency == null ? null : originatingAgency.getName());
            // S58-P7 -- editing surface: which participant to open (?p= after an agentSave, else
            // none), whether this viewer may edit (page-level hint only; agentSave re-checks),
            // and the agent flashes (consumed here, single use).
            request.setAttribute("focusParticipantId", parseLongOrNull(request.getParameter("p")));
            request.setAttribute("matrixGuid", matrix.getAccessGuid());
            request.setAttribute("agentFlashMessage", request.getSession().getAttribute(AGENT_FLASH_MESSAGE));
            request.setAttribute("agentFlashError", request.getSession().getAttribute(AGENT_FLASH_ERROR));
            request.setAttribute("agentSavedParticipantId", request.getSession().getAttribute(AGENT_FLASH_PARTICIPANT));
            request.getSession().removeAttribute(AGENT_FLASH_MESSAGE);
            request.getSession().removeAttribute(AGENT_FLASH_ERROR);
            request.getSession().removeAttribute(AGENT_FLASH_PARTICIPANT);

            renderMatrix(request, response, em, setup.getId(), pspId, proposalId, prospect, matrix, AGENT_VIEW);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * S58-P3 — the one refusal every GUID-path failure takes. Unknown GUID, closed setup,
     * locked matrix and failed authorisation are deliberately indistinguishable: same status,
     * same body, no message naming the condition.
     */
    private static void refuse(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * S58-P5 — exactly {@code LoginFilter}'s notion of "logged in": a session exists and its
     * {@code "local"} attribute is an {@code AmsDataLocal} reporting {@code isAuthenticated()}.
     * Not a weaker test (no role flag, no {@code currentPerson} shortcut).
     */
    private static boolean isAuthenticatedSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object localObj = session.getAttribute("local");
        return localObj instanceof AmsDataLocal local && local.isAuthenticated();
    }

    /**
     * S58-P5 — stores the request's path in the session for {@code AuthenticateUser.goToPage},
     * but only when it is exactly the shape this feature emits ({@link #MATRIX_RETURN_PATTERN}).
     * Path only: {@code servletPath + pathInfo}, never the query string, fragment, host or
     * scheme. Length-capped, and anything carrying CR, LF, a backslash or a leading {@code //}
     * is rejected outright (the anchored pattern already excludes all of those; the explicit
     * checks make the rule legible). Anything that does not match is not captured at all — the
     * visitor simply goes to {@code /login} with no destination.
     */
    private static void captureMatrixReturnPath(HttpServletRequest request) {
        String servletPath = request.getServletPath() == null ? "" : request.getServletPath();
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        String path = servletPath + pathInfo;

        if (path.length() > MATRIX_RETURN_MAX_LENGTH) return;
        if (path.indexOf('\r') >= 0 || path.indexOf('\n') >= 0 || path.indexOf('\\') >= 0) return;
        if (path.startsWith("//")) return;
        if (!MATRIX_RETURN_PATTERN.matcher(path).matches()) return;

        request.getSession(true).setAttribute(MATRIX_RETURN_ATTR, path);
    }

    /**
     * The render shared by both entry paths — everything from the roster query to the forward,
     * unchanged from the pre-S58-P3 {@code doGet} body except that the view it forwards to is
     * a parameter ({@link #VIEW} for the PSP page, {@link #AGENT_VIEW} for the GUID page). The
     * option maps ({@code payrollFrequencyOptions}, {@code coverageTierOptions}) double as the
     * agent page's code → label lookups.
     */
    private void renderMatrix(HttpServletRequest request, HttpServletResponse response, EntityManager em,
                              Long setupId, Long pspId, long proposalId, Prospect prospect,
                              EnrollmentMatrix matrix, String view)
            throws ServletException, IOException {
            List<EmployerParticipant> roster = prospect == null
                    ? List.of() : EmployerParticipantDAO.findByProspectId(em, prospect.getId());

            Set<Integer> electedServiceItemIds = loadElectedServiceItemIds(em, proposalId);
            List<SummitPlanTemplateMap> legs = new ArrayList<>();
            for (SummitPlanTemplateMap candidate : SummitPlanTemplateMapDAO.findActiveByPspId(em, pspId)) {
                if (electedServiceItemIds.contains(candidate.getServiceItemId())
                        && !"NONE".equals(candidate.getEnrollmentAmountMode())) {
                    legs.add(candidate);
                }
            }

            // Existing header rows, keyed by participant id -- a participant with no row yet
            // renders blank; nothing is created on GET (see class-level note).
            Map<Long, EnrollmentMatrixParticipant> headersByParticipant = new LinkedHashMap<>();
            // Existing entry rows, keyed by "<matrixParticipantId>_<legId>" -- only reachable
            // when a header row already exists for that participant.
            Map<String, EnrollmentMatrixEntry> entriesByParticipantAndLeg = new LinkedHashMap<>();
            for (EmployerParticipant participant : roster) {
                EnrollmentMatrixParticipant header =
                        EnrollmentMatrixParticipantDAO.findByMatrixAndParticipant(em, matrix.getId(), participant.getId());
                if (header == null) continue;
                headersByParticipant.put(participant.getId(), header);
                for (SummitPlanTemplateMap leg : legs) {
                    EnrollmentMatrixEntry entry =
                            EnrollmentMatrixEntryDAO.findByParticipantAndLeg(em, header.getId(), leg.getId());
                    if (entry != null) {
                        entriesByParticipantAndLeg.put(header.getId() + "_" + leg.getId(), entry);
                    }
                }
            }

            request.setAttribute("setupId", setupId);
            request.setAttribute("prospectName", prospect == null ? null : prospect.getName());
            request.setAttribute("matrix", matrix);
            request.setAttribute("roster", roster);
            request.setAttribute("legs", legs);
            request.setAttribute("headersByParticipant", headersByParticipant);
            request.setAttribute("entriesByParticipantAndLeg", entriesByParticipantAndLeg);
            request.setAttribute("payrollFrequencyOptions",
                    buildPayrollFrequencyOptions(em, headersByParticipant));
            request.setAttribute("otherCustom", OTHER_CUSTOM);
            // S58-P9 -- the shared option map above is matrix-wide (an inactive stored code is
            // injected once for every participant's select; the PSP page relies on that). The
            // agent page scopes those entries to the participant whose stored value they are,
            // and the completeness service flags them; both need to know which codes they are.
            Set<String> approvedPayrollCodes = approvedPayrollCodes(em);
            request.setAttribute("approvedPayrollCodes", approvedPayrollCodes);

            // S57-P6/TA-15 -- schedule suggestion from the setup's application answers. Empty
            // map (any unrecognised/missing/unparseable answer) renders no optgroup at all --
            // see filterSuggestedSchedules.
            String paycycleFrequencyAnswer = resolveApplicationAnswer(em, proposalId, FIELD_PAYCYCLE_FREQUENCY);
            String paycycleFirstPaydateAnswer = resolveApplicationAnswer(em, proposalId, FIELD_PAYCYCLE_FIRST_PAYDATE);
            String paycycleOtherHaveAnswer = resolveApplicationAnswer(em, proposalId, FIELD_PAYCYCLE_OTHER_HAVE);
            request.setAttribute("suggestedPayrollFrequencies",
                    buildSuggestedPayrollFrequencies(em, paycycleFrequencyAnswer, paycycleFirstPaydateAnswer, paycycleOtherHaveAnswer));
            request.setAttribute("suggestedPayrollFrequency",
                    resolveSuggestedPayrollFrequency(filterSuggestedSchedules(
                            em, paycycleFrequencyAnswer, paycycleFirstPaydateAnswer, paycycleOtherHaveAnswer)));
            request.setAttribute("coverageTierOptions",
                    buildCoverageTierOptions(em, entriesByParticipantAndLeg));
            request.setAttribute("mostRecentCustomScheduleName",
                    EnrollmentMatrixParticipantDAO.findMostRecentCustomScheduleName(em, matrix.getId()));
            // S58-P7 -- per-participant status and named gaps, from the same four collections
            // this method already built. Both views read it: the agent page renders it in full,
            // the PSP page shows the needs-PSP-confirmation marker and the agent note.
            request.setAttribute("assessment",
                    MatrixCompletenessService.assess(roster, legs, headersByParticipant, entriesByParticipantAndLeg,
                            approvedPayrollCodes));
            forward(request, response, view);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        String action = request.getParameter("action");

        // S58-P7 -- agentSave is gated by MatrixAccessResolver inside agentSave itself (the same
        // question the GUID page asks), after LoginFilter has already required a login for
        // /EnrollmentMatrix. It is branched before the PSP gates and before the session-PSP
        // check, neither of which applies to an outside agent.
        if ("agentSave".equals(action)) {
            Long agentSetupId = parseLongOrNull(request.getParameter("setupId"));
            if (agentSetupId == null) { refuse(response); return; }
            EntityManagerFactory agentEmf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager agentEm = agentEmf.createEntityManager();
            try {
                agentSave(request, response, session, agentEm, agentSetupId);
            } finally {
                if (agentEm.isOpen()) agentEm.close();
            }
            return;
        }

        // S58-P4 -- two gates, decided by action before anything else runs:
        //   issueLink            : any PSP user or admin (isPspStaff), matching MatrixAccessResolver;
        //   save (incl. lock), unlock, anything else : PSP admin only (isAuthorized), unchanged.
        boolean allowed = "issueLink".equals(action) ? isPspStaff(session) : isAuthorized(session);
        if (!allowed) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        Long setupId = parseLongOrNull(request.getParameter("setupId"));
        if (setupId == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = resolveCurrentPspId(request);
            if (pspId == null) {
                session.setAttribute(FLASH_ERROR, "Could not determine your PSP from this session."
                        + " Nothing was saved.");
                response.sendRedirect(request.getContextPath() + "/EnrollmentMatrix?setupId=" + setupId);
                return;
            }

            if ("issueLink".equals(action)) {
                // S58-P3/P4 -- answers JSON to the setup screen's link controls, no redirect.
                // Gate: isPspStaff (see the top of this method).
                issueLink(request, response, em, setupId, pspId);
                return;
            }
            // Everything below is PSP-admin only -- guaranteed by the gate at the top of this
            // method (isAuthorized for every action other than issueLink), re-asserted here so
            // the invariant survives any later edit to that gate.
            if (!isAuthorized(session)) {
                response.sendRedirect(request.getContextPath() + "/");
                return;
            }
            if ("unlock".equals(action)) {
                unlock(request, session, em);
            } else if ("save".equals(action)) {
                save(request, session, em, setupId, pspId);
            }
        } catch (RuntimeException e) {
            session.setAttribute(FLASH_ERROR, "The change was not saved: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/EnrollmentMatrix?setupId=" + setupId);
    }

    // ── Actions ───────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpSession session, EntityManager em, Long setupId, Long pspId) {
        Setup setup = em.find(Setup.class, setupId);
        if (setup == null || setup.getApplication() == null || setup.getApplication().getProposal() == null) {
            session.setAttribute(FLASH_ERROR, "Setup " + setupId + " no longer resolves to a sale."
                    + " Nothing was saved.");
            return;
        }

        EnrollmentMatrix matrix = findOrCreateMatrix(em, setupId, resolveCurrentUserName(request));
        if (matrix.isPushed()) {
            session.setAttribute(FLASH_ERROR, "This matrix has been pushed and is read-only."
                    + " Nothing was saved.");
            return;
        }

        long proposalId = setup.getApplication().getProposal().getId();
        Set<Integer> electedServiceItemIds = loadElectedServiceItemIds(em, proposalId);
        List<SummitPlanTemplateMap> legs = new ArrayList<>();
        for (SummitPlanTemplateMap candidate : SummitPlanTemplateMapDAO.findActiveByPspId(em, pspId)) {
            if (electedServiceItemIds.contains(candidate.getServiceItemId())
                    && !"NONE".equals(candidate.getEnrollmentAmountMode())) {
                legs.add(candidate);
            }
        }

        String participantIdsRaw = request.getParameter("participantIds");
        if (participantIdsRaw == null || participantIdsRaw.isBlank()) {
            session.setAttribute(FLASH_ERROR, "No participant rows were submitted. Nothing was saved.");
            return;
        }

        String currentUser = resolveCurrentUserName(request);
        int savedCount = 0;
        List<String> rowErrors = new ArrayList<>();

        for (String rawId : participantIdsRaw.split(",")) {
            Long participantId = parseLongOrNull(rawId);
            if (participantId == null) continue;

            EnrollmentMatrixParticipant header =
                    EnrollmentMatrixParticipantDAO.findByMatrixAndParticipant(em, matrix.getId(), participantId);
            // An already-locked row's inputs were disabled in the form and submitted nothing --
            // it is never touched here, matching the "locked row's inputs are disabled" rule.
            if (header != null && header.isEntryLocked()) continue;

            boolean isNewHeader = header == null;
            if (isNewHeader) {
                header = new EnrollmentMatrixParticipant();
                header.setMatrixId(matrix.getId());
                header.setParticipantId(participantId);
                header.setCreatedAt(LocalDateTime.now());
                header.setCreatedBy(currentUser);
            }

            header.setPayrollFrequency(trimToNull(request.getParameter("payrollFrequency_" + participantId)));
            header.setCustomScheduleName(trimToNull(request.getParameter("customScheduleName_" + participantId)));

            boolean requestedLock = request.getParameter("lock_" + participantId) != null;
            if (requestedLock && !header.isEntryLocked()) {
                header.setEntryLocked(true);
                header.setLockedBy(currentUser);
                header.setLockedAt(LocalDateTime.now());
            }

            if (isNewHeader) {
                EnrollmentMatrixParticipantDAO.insert(em, header);
            } else {
                EnrollmentMatrixParticipantDAO.update(em, header);
            }
            savedCount++;

            saveEntriesForParticipant(request, em, header, participantId, legs, currentUser, rowErrors);
        }

        StringBuilder message = new StringBuilder("Saved " + savedCount + " participant row"
                + (savedCount == 1 ? "" : "s") + ".");
        if (!rowErrors.isEmpty()) {
            message.append(" ").append(rowErrors.size()).append(" field(s) had a problem: ")
                    .append(String.join(" ", rowErrors));
        }
        session.setAttribute(FLASH_MESSAGE, message.toString());
    }

    private void unlock(HttpServletRequest request, HttpSession session, EntityManager em) {
        Long headerId = parseLongOrNull(request.getParameter("matrixParticipantId"));
        if (headerId == null) {
            session.setAttribute(FLASH_ERROR, "No participant row was identified to unlock.");
            return;
        }
        EnrollmentMatrixParticipant header = EnrollmentMatrixParticipantDAO.findById(em, headerId);
        if (header == null) {
            session.setAttribute(FLASH_ERROR, "That participant row no longer exists.");
            return;
        }
        header.setEntryLocked(false);
        header.setLockedBy(null);
        header.setLockedAt(null);
        EnrollmentMatrixParticipantDAO.update(em, header);
        session.setAttribute(FLASH_MESSAGE, "Row " + headerId + " unlocked.");
    }

    /**
     * S58-P7 — the per-leg entry writes for one participant, <b>moved verbatim out of
     * {@link #save}'s inner loop</b> (no behaviour change) so the PSP {@code save} and the agent
     * {@link #agentSave} share one copy of the Summit-bound value logic rather than two that
     * drift. Reads {@code amount_<pid>_<legId>}, {@code tier_<pid>_<legId>} and
     * {@code declined_<pid>_<legId>} exactly as before; a non-numeric amount is reported into
     * {@code rowErrors} and the stored amount left unchanged, exactly as before.
     */
    private void saveEntriesForParticipant(HttpServletRequest request, EntityManager em,
                                           EnrollmentMatrixParticipant header, Long participantId,
                                           List<SummitPlanTemplateMap> legs, String currentUser,
                                           List<String> rowErrors) {
        for (SummitPlanTemplateMap leg : legs) {
            String suffix = participantId + "_" + leg.getId();
            EnrollmentMatrixEntry entry =
                    EnrollmentMatrixEntryDAO.findByParticipantAndLeg(em, header.getId(), leg.getId());
            boolean isNewEntry = entry == null;
            if (isNewEntry) {
                entry = new EnrollmentMatrixEntry();
                entry.setMatrixParticipantId(header.getId());
                entry.setPlanTemplateMapId(leg.getId());
                entry.setCreatedAt(LocalDateTime.now());
                entry.setCreatedBy(currentUser);
            }

            String amountRaw = trimToNull(request.getParameter("amount_" + suffix));
            if (amountRaw == null) {
                entry.setAmount(null);
            } else {
                try {
                    entry.setAmount(new BigDecimal(amountRaw));
                } catch (NumberFormatException e) {
                    rowErrors.add("Participant " + participantId + ", leg " + leg.getId()
                            + ": amount '" + amountRaw + "' is not a number -- left unchanged.");
                }
            }
            entry.setTierName(trimToNull(request.getParameter("tier_" + suffix)));

            boolean declined = request.getParameter("declined_" + suffix) != null;
            if (declined && !entry.isDeclined()) {
                entry.setDeclinedAt(LocalDateTime.now());
                entry.setRecordedBy(currentUser);
            }
            entry.setDeclined(declined);

            if (isNewEntry) {
                EnrollmentMatrixEntryDAO.insert(em, entry);
            } else {
                EnrollmentMatrixEntryDAO.update(em, entry);
            }
        }
    }

    /**
     * S58-P7 — {@code action=agentSave}: one participant per request, from the agent view.
     * <p>
     * <b>Gate is {@link MatrixAccessResolver#canView}</b> — the same question the GUID page asks;
     * no new gate. Every refusal that would also refuse the page (no matrix, no setup, closed
     * setup, pushed matrix, not authorised, participant not on this employer's roster) takes
     * {@link #refuse} — the identical 404 — because a caller who can't view can't be told why.
     * A <i>locked</i> participant is the one refusal a legitimate viewer can hit (PSP locked the
     * row while the agent had the form open), so it answers with a flash error and a redirect
     * back to the page.
     * <p>
     * <b>Writes only the agent-permitted fields</b>: {@code payrollFrequency_<pid>} (the same
     * global list the PSP page offers, sentinels included), {@code agentScheduleNote_<pid>}
     * (V113, trimmed, capped at the column width), and the per-leg amount / tier / declined via
     * the shared {@link #saveEntriesForParticipant}. <b>{@code customScheduleName_<pid>} is
     * never read</b> — it is the Summit-bound schedule name and stays PSP-only; a posted value is
     * ignored, not rejected. <b>No lock state is read or written.</b> Nothing is pushed or
     * exported.
     * <p>
     * <b>Never creates a matrix row</b> — {@link EnrollmentMatrixDAO#findBySetupId} only; a
     * missing matrix is refused. The participant header and entry rows are created lazily,
     * exactly as the PSP {@code save} creates them.
     */
    private void agentSave(HttpServletRequest request, HttpServletResponse response,
                           HttpSession session, EntityManager em, Long setupId) throws IOException {
        Long participantId = parseLongOrNull(request.getParameter("participantId"));
        if (participantId == null) { refuse(response); return; }

        EnrollmentMatrix matrix = EnrollmentMatrixDAO.findBySetupId(em, setupId);   // find only
        if (matrix == null) { refuse(response); return; }

        Setup setup = em.find(Setup.class, setupId);
        if (setup == null || setup.isComplete()) { refuse(response); return; }
        if (matrix.isPushed()) { refuse(response); return; }
        if (!MatrixAccessResolver.canView(em, request, setup)) { refuse(response); return; }

        Application application = setup.getApplication();
        if (application == null || application.getProposal() == null) { refuse(response); return; }
        Prospect prospect = application.getProposal().getProspect();
        if (prospect == null) { refuse(response); return; }

        // The participant must be on this employer's roster -- a participant id from another
        // prospect must not be attachable to this matrix.
        boolean onRoster = false;
        for (EmployerParticipant p : EmployerParticipantDAO.findByProspectId(em, prospect.getId())) {
            if (participantId.equals(p.getId())) { onRoster = true; break; }
        }
        if (!onRoster) { refuse(response); return; }

        String backTo = request.getContextPath()
                + (trimToNull(matrix.getAccessGuid()) == null ? "/" : "/matrix/" + matrix.getAccessGuid()
                + "?p=" + participantId);

        EnrollmentMatrixParticipant header =
                EnrollmentMatrixParticipantDAO.findByMatrixAndParticipant(em, matrix.getId(), participantId);
        if (header != null && header.isEntryLocked()) {
            session.setAttribute(AGENT_FLASH_ERROR, "This participant was locked by the administrator"
                    + " and can no longer be edited. Nothing was saved.");
            response.sendRedirect(backTo);
            return;
        }

        Long pspId = MatrixAccessResolver.resolveSetupPspId(setup);
        if (pspId == null) pspId = resolveCurrentPspId(request);
        if (pspId == null) { refuse(response); return; }

        long proposalId = application.getProposal().getId();
        Set<Integer> electedServiceItemIds = loadElectedServiceItemIds(em, proposalId);
        List<SummitPlanTemplateMap> legs = new ArrayList<>();
        for (SummitPlanTemplateMap candidate : SummitPlanTemplateMapDAO.findActiveByPspId(em, pspId)) {
            if (electedServiceItemIds.contains(candidate.getServiceItemId())
                    && !"NONE".equals(candidate.getEnrollmentAmountMode())) {
                legs.add(candidate);
            }
        }

        String currentUser = resolveCurrentUserName(request);
        List<String> rowErrors = new ArrayList<>();
        try {
            boolean isNewHeader = header == null;
            if (isNewHeader) {
                header = new EnrollmentMatrixParticipant();
                header.setMatrixId(matrix.getId());
                header.setParticipantId(participantId);
                header.setCreatedAt(LocalDateTime.now());
                header.setCreatedBy(currentUser);
            }
            // Agent-permitted header fields only. customScheduleName_<pid> is deliberately not
            // read here (Part 2); lock_<pid> is deliberately not read here.
            // S58-P9 -- a non-enrollment-approved code is accepted only when it is this
            // participant's own current stored value (the passthrough that keeps a stored
            // inactive schedule from being silently rewritten). Any other non-approved code can
            // only arrive by a crafted POST, and is refused whole: nothing is saved.
            String postedFrequency = trimToNull(request.getParameter("payrollFrequency_" + participantId));
            if (postedFrequency != null
                    && !MatrixCompletenessService.isSentinelPayrollFrequency(postedFrequency)
                    && !approvedPayrollCodes(em).contains(postedFrequency)
                    && !postedFrequency.equals(isNewHeader ? null : header.getPayrollFrequency())) {
                session.setAttribute(AGENT_FLASH_ERROR, "That payroll schedule isn't available to choose."
                        + " Please pick one from the list. Nothing was saved.");
                response.sendRedirect(backTo);
                return;
            }
            header.setPayrollFrequency(postedFrequency);
            String note = trimToNull(request.getParameter("agentScheduleNote_" + participantId));
            if (note != null && note.length() > AGENT_NOTE_MAX_LENGTH) note = note.substring(0, AGENT_NOTE_MAX_LENGTH);
            header.setAgentScheduleNote(note);

            if (isNewHeader) {
                EnrollmentMatrixParticipantDAO.insert(em, header);
            } else {
                EnrollmentMatrixParticipantDAO.update(em, header);
            }

            saveEntriesForParticipant(request, em, header, participantId, legs, currentUser, rowErrors);
        } catch (RuntimeException e) {
            // S58-P8: agent-facing -- the exception text carries internal vocabulary (DAO and
            // table names), so it goes to the log, not the page.
            e.printStackTrace();
            session.setAttribute(AGENT_FLASH_ERROR, "Sorry — that didn't save. Please try again; if it keeps"
                    + " happening, contact your plan administrator.");
            response.sendRedirect(backTo);
            return;
        }

        // S58-P8: the shared entry loop phrases row errors for the PSP page ("leg N"); the agent
        // gets a plain count instead of the raw text.
        StringBuilder message = new StringBuilder("Saved.");
        if (!rowErrors.isEmpty()) {
            message.append(" ").append(rowErrors.size()).append(" amount").append(rowErrors.size() == 1 ? "" : "s")
                    .append(" couldn't be read as a number and ").append(rowErrors.size() == 1 ? "was" : "were")
                    .append(" left unchanged — please check and save again.");
        }
        session.setAttribute(AGENT_FLASH_MESSAGE, message.toString());
        session.setAttribute(AGENT_FLASH_PARTICIPANT, participantId);
        response.sendRedirect(backTo);
    }

    /**
     * S58-P3 (D4) — {@code action=issueLink}: lazily generates {@code access_guid} the first time
     * a PSP admin asks for the link, persists it, and answers {@code {"url": "..."}}. Idempotent
     * — a matrix that already has a GUID gets the same URL back; nothing is regenerated. The
     * matrix row itself is created here if absent, exactly as the PSP-admin GET does
     * ({@link #findOrCreateMatrix}); this is the one PSP-admin action, not the viewer path.
     * The URL is built from the request the way {@code ProposalDetail} builds its proposal
     * link, so it matches whatever host the admin is on. Errors answer JSON too, never a
     * redirect, because the caller is a {@code fetch}.
     */
    private void issueLink(HttpServletRequest request, HttpServletResponse response,
                           EntityManager em, Long setupId, Long sessionPspId) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        try {
            Setup setup = em.find(Setup.class, setupId);
            // S58-P4 -- PSP-scoped like MatrixAccessResolver: a staff member may only issue a
            // link for a setup of their own PSP. A setup whose PSP cannot be established is
            // treated as not theirs. Same answer for "no such setup" so neither is revealed.
            Long setupPspId = setup == null ? null : MatrixAccessResolver.resolveSetupPspId(setup);
            if (setup == null || setupPspId == null || !setupPspId.equals(sessionPspId)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\":\"No setup found for id " + setupId + ".\"}");
                return;
            }
            EnrollmentMatrix matrix = findOrCreateMatrix(em, setupId, resolveCurrentUserName(request));
            if (trimToNull(matrix.getAccessGuid()) == null) {
                matrix.setAccessGuid(UUID.randomUUID().toString());
                EnrollmentMatrixDAO.update(em, matrix);
            }

            String baseUrl = request.getScheme() + "://" + request.getServerName();
            int port = request.getServerPort();
            if (port != 80 && port != 443) baseUrl += ":" + port;
            String url = baseUrl + request.getContextPath() + "/matrix/" + matrix.getAccessGuid();

            out.print("{\"url\":\"" + jsonEscape(url) + "\"}");
        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + jsonEscape("The link could not be issued: " + e.getMessage()) + "\"}");
        } finally {
            out.flush();
        }
    }

    /** Minimal JSON string escaping for the one URL/message {@link #issueLink} emits. */
    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    // ── Reads ─────────────────────────────────────────────────────────

    /** Find-or-create, in its own transaction on create -- the matrix row is lazy, never seeded. */
    private EnrollmentMatrix findOrCreateMatrix(EntityManager em, Long setupId, String currentUser) {
        EnrollmentMatrix matrix = EnrollmentMatrixDAO.findBySetupId(em, setupId);
        if (matrix != null) return matrix;
        matrix = new EnrollmentMatrix();
        matrix.setSetupId(setupId);
        matrix.setCreatedAt(LocalDateTime.now());
        matrix.setCreatedBy(currentUser);
        EnrollmentMatrixDAO.insert(em, matrix);
        return matrix;
    }

    /**
     * s52k — duplicates {@code SummitExportServlet.loadElectedServiceItems}'s JPQL (that method is
     * private, in a file this build may not touch): the services this proposal's sale actually
     * elected, matching {@code ApplicationModule}, the join AMS records the sale on. Returns only
     * the id set; this screen has no use for the description that method also carries.
     */
    private Set<Integer> loadElectedServiceItemIds(EntityManager em, long proposalId) {
        Query q = em.createQuery(
                "SELECT am FROM ApplicationModule am " +
                "WHERE am.application.proposal.id = :pid");
        q.setParameter("pid", proposalId);
        @SuppressWarnings("unchecked")
        List<ApplicationModule> modules = (List<ApplicationModule>) q.getResultList();
        Set<Integer> ids = new LinkedHashSet<>();
        for (ApplicationModule module : modules) {
            ServiceItem serviceItem = module.getServiceItem();
            if (serviceItem != null) ids.add(serviceItem.getId());
        }
        return ids;
    }

    /**
     * S57-P2 — the tier select's option map, keyed on {@link CoverageTier#getSummitTierId()},
     * not on {@code code} and never on the primary key: the map key is the exact string that
     * {@link EnrollmentMatrixEntry#setTierName} stores and {@code SummitExportServlet} later
     * emits verbatim into column E of the HRA Enrollment file (V109, TA-14). The value is a
     * display-only label, in order: (1) every {@link CoverageTierDAO#findActive} row, (2) any
     * {@code tier_name} already stored on an entry in this matrix that is not in the active list
     * (so deactivating a tier never silently blanks a cell that already holds it). No sentinels
     * — {@code OTHER_CUSTOM}/{@code OTHER_NOT_IMPORTABLE} are payroll-frequency concepts with no
     * tier equivalent. The save path does not validate against this table, so a hand-crafted
     * POST is still stored verbatim, same as payroll frequency.
     */
    private LinkedHashMap<String, String> buildCoverageTierOptions(
            EntityManager em, Map<String, EnrollmentMatrixEntry> entries) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (CoverageTier tier : CoverageTierDAO.findActive(em)) {
            options.put(tier.getSummitTierId(), tier.getLabel());
        }
        for (EnrollmentMatrixEntry entry : entries.values()) {
            String stored = trimToNull(entry.getTierName());
            if (stored == null) continue;
            if (options.containsKey(stored)) continue;
            options.put(stored, stored + " (inactive)");
        }
        return options;
    }

    /**
     * S57-P6/TA-15 — the day-of-week / recurrence / bi-weekly-parity filter behind both the
     * "Suggested from application" optgroup ({@link #buildSuggestedPayrollFrequencies}) and the
     * single-preferred-survivor caption ({@link #resolveSuggestedPayrollFrequency}). Returns an
     * empty list — never a partial or guessed filter — when {@code freqAnswer} is null or
     * blank, when no active {@link PaycycleFrequencyAlias} matches it
     * ({@link PaycycleFrequencyAliasDAO#findByNormalizedKey}), or when {@code paydateAnswer}
     * fails strict ISO parsing ({@link #parseStrictIsoDate}).
     * <p>
     * Rows come from {@link PayrollFrequencyDAO#findEnrollmentApproved} only, so sentinels and
     * inactive/unapproved rows never reach this filter. Order: (1) day rule, always — a row
     * with a non-null {@code payDow} not matching the parsed paydate's day of week is dropped;
     * (2) if {@code otherHaveAnswer} is {@code Yes} (trimmed, case-insensitive), stop here — the
     * day rule is the whole filter; (3) otherwise the alias's recurrence token narrows further
     * (a {@code WEEKLY} answer keeps same-day {@code BIWEEKLY} rows too; {@code SEMIMONTHLY}
     * requires a matching {@code semimonthlyVariant}); (4) any surviving {@code BIWEEKLY} row is
     * kept only if its {@code anchorDate} is non-null and
     * {@code Math.floorMod(daysBetween(paydate, anchorDate), 14) == 0} — {@code floorMod}, not
     * {@code %}, since the first paydate preceding the anchor is the normal case (every V110
     * anchor is September 2026) and {@code %} would silently match nothing on a negative
     * difference.
     */
    private List<PayrollFrequency> filterSuggestedSchedules(
            EntityManager em, String freqAnswer, String paydateAnswer, String otherHaveAnswer) {
        if (freqAnswer == null || freqAnswer.isBlank()) return List.of();

        PaycycleFrequencyAlias alias = PaycycleFrequencyAliasDAO.findByNormalizedKey(em, freqAnswer);
        if (alias == null) return List.of();

        LocalDate paydate = parseStrictIsoDate(paydateAnswer);
        if (paydate == null) return List.of();

        List<PayrollFrequency> dayFiltered = new ArrayList<>();
        for (PayrollFrequency candidate : PayrollFrequencyDAO.findEnrollmentApproved(em)) {
            String payDow = candidate.getPayDow();
            if (payDow != null && !payDow.equals(paydate.getDayOfWeek().name())) continue;
            dayFiltered.add(candidate);
        }

        if ("YES".equalsIgnoreCase(trimToNull(otherHaveAnswer))) {
            return dayFiltered;
        }

        String recurrence = alias.getRecurrence();
        List<PayrollFrequency> recurrenceFiltered = new ArrayList<>();
        for (PayrollFrequency candidate : dayFiltered) {
            String candidateRecurrence = candidate.getRecurrence();
            if ("BIWEEKLY".equals(recurrence)) {
                if ("BIWEEKLY".equals(candidateRecurrence)) recurrenceFiltered.add(candidate);
            } else if ("WEEKLY".equals(recurrence)) {
                if ("WEEKLY".equals(candidateRecurrence) || "BIWEEKLY".equals(candidateRecurrence)) {
                    recurrenceFiltered.add(candidate);
                }
            } else if ("SEMIMONTHLY".equals(recurrence)) {
                if ("SEMIMONTHLY".equals(candidateRecurrence)
                        && java.util.Objects.equals(candidate.getSemimonthlyVariant(), alias.getSemimonthlyVariant())) {
                    recurrenceFiltered.add(candidate);
                }
            } else if ("MONTHLY".equals(recurrence)) {
                if ("MONTHLY".equals(candidateRecurrence)) recurrenceFiltered.add(candidate);
            }
        }

        List<PayrollFrequency> result = new ArrayList<>();
        for (PayrollFrequency candidate : recurrenceFiltered) {
            if ("BIWEEKLY".equals(candidate.getRecurrence())) {
                LocalDate anchor = candidate.getAnchorDate();
                if (anchor == null) continue;
                if (Math.floorMod(ChronoUnit.DAYS.between(paydate, anchor), 14) != 0) continue;
            }
            result.add(candidate);
        }
        return result;
    }

    /**
     * S57-P6/TA-15 — the "Suggested from application" optgroup's option map, code → display
     * label, built from {@link #filterSuggestedSchedules}. <b>Empty map ⇒ flat list, never
     * partial</b> — the JSP renders no {@code <optgroup>} at all when this is empty.
     */
    private LinkedHashMap<String, String> buildSuggestedPayrollFrequencies(
            EntityManager em, String freqAnswer, String paydateAnswer, String otherHaveAnswer) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (PayrollFrequency suggested : filterSuggestedSchedules(em, freqAnswer, paydateAnswer, otherHaveAnswer)) {
            options.put(suggested.getCode(), suggested.getLabel());
        }
        return options;
    }

    /**
     * S57-P6/TA-15 — the one schedule named in the caption, never preselected (a hidden
     * participant panel's {@code <select>} still submits on Save, so a {@code selected}
     * suggestion would silently persist onto every unlocked row the operator never opened): the
     * single survivor with {@code preferred = true} when exactly one exists among
     * {@code survivors}, else null.
     */
    private String resolveSuggestedPayrollFrequency(List<PayrollFrequency> survivors) {
        String preselect = null;
        int preferredCount = 0;
        for (PayrollFrequency candidate : survivors) {
            if (candidate.isPreferred()) {
                preferredCount++;
                preselect = candidate.getCode();
            }
        }
        return preferredCount == 1 ? preselect : null;
    }

    /**
     * Copied from {@code SummitExportServlet.parseAnswerDate} (private to that file, which this
     * build may not touch) — an HTML {@code <input type="date">} submits ISO {@code yyyy-MM-dd}.
     * Returns null if the value is absent, blank, or unparseable; callers refuse to filter
     * rather than guess.
     */
    private static LocalDate parseStrictIsoDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * s53d — the payroll-frequency select's option map, code → display label, in order:
     * (1) every {@link PayrollFrequencyDAO#findEnrollmentApproved} row, (2) any code already
     * stored on a header row in this matrix that is not enrollment-approved (so un-approving a
     * frequency never silently blanks a row that already holds it), (3)
     * {@link PayrollFrequency#OTHER_CUSTOM}, (4) {@link PayrollFrequency#OTHER_NOT_IMPORTABLE}.
     * A {@code LinkedHashMap}, not a record or POJO list — the JSP's EL resolves {@code getCode()}
     * and {@code getLabel()}, which a record does not generate, and the map keeps both the JSP
     * change and the ordering explicit.
     */
    /**
     * S58-P9 — the codes an agent may choose fresh: every
     * {@link PayrollFrequencyDAO#findEnrollmentApproved} row's code. The two {@code OTHER_*}
     * sentinels are not in this set; callers test them separately. Anything stored that is not
     * here is an "inactive" schedule — offered on the agent page only to the participant already
     * holding it, refused by {@link #agentSave} otherwise, and flagged by
     * {@code MatrixCompletenessService} as needing the administrator's attention.
     */
    private static Set<String> approvedPayrollCodes(EntityManager em) {
        Set<String> codes = new LinkedHashSet<>();
        for (PayrollFrequency approved : PayrollFrequencyDAO.findEnrollmentApproved(em)) {
            codes.add(approved.getCode());
        }
        return codes;
    }

    private LinkedHashMap<String, String> buildPayrollFrequencyOptions(
            EntityManager em, Map<Long, EnrollmentMatrixParticipant> headersByParticipant) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (PayrollFrequency approved : PayrollFrequencyDAO.findEnrollmentApproved(em)) {
            options.put(approved.getCode(), approved.getLabel());
        }
        for (EnrollmentMatrixParticipant header : headersByParticipant.values()) {
            String stored = header.getPayrollFrequency();
            if (stored == null || stored.isBlank()) continue;
            if (options.containsKey(stored)) continue;
            if (PayrollFrequency.OTHER_CUSTOM.equals(stored)
                    || PayrollFrequency.OTHER_NOT_IMPORTABLE.equals(stored)) continue;
            options.put(stored, stored + " (inactive)");
        }
        options.put(PayrollFrequency.OTHER_CUSTOM, "Other — custom Summit schedule");
        options.put(PayrollFrequency.OTHER_NOT_IMPORTABLE, "Other — not importable");
        return options;
    }

    /**
     * ⚠️ <b>TA-2 shape.</b> A third, narrow copy of the "{@code ApplicationFieldValue} by
     * {@code fieldKey}" query pattern — {@code SummitExportServlet.loadApplicationAnswers}
     * (private to that file) and {@code AgentSetupSnapshotLoader.load} (a different loader with
     * a different job, and it loads every field rather than one) both already carry the same
     * shape. No shared {@code ApplicationFieldValueDAO} exists to call instead (s53d Step 2
     * finding); registered here rather than adding a fourth divergent copy of the same query.
     * Generalised (S57-P6) from a single {@code paycycle_frequency}-only lookup to any field key,
     * so the TA-15 suggestion filter's three answers share one query shape.
     *
     * @return the application's answer for {@code fieldKey}, or null when the application has no
     * saved answer for that key — the ordinary state until the field is filled in.
     */
    private String resolveApplicationAnswer(EntityManager em, long proposalId, String fieldKey) {
        Query q = em.createQuery(
                "SELECT fv.fieldValue FROM ApplicationFieldValue fv " +
                "WHERE fv.application.proposal.id = :pid AND fv.applicationField.fieldKey = :fieldKey");
        q.setParameter("pid", proposalId);
        q.setParameter("fieldKey", fieldKey);
        @SuppressWarnings("unchecked")
        List<String> found = (List<String>) q.getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Verbatim from {@code SummitPlanTemplateAdmin.isAuthorized}, itself verbatim from {@code RateCacheAdmin}. */
    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    /**
     * S58-P4/P5 — any PSP admin, user or sales: the gate for {@code action=issueLink} only, the
     * same three flags {@link MatrixAccessResolver} admits (scoped there to the setup's own PSP;
     * {@link #issueLink} applies the same scope against the session's PSP).
     */
    private static boolean isPspStaff(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isPspAdmin"))
                || Boolean.TRUE.equals(session.getAttribute("isPspUser"))
                || Boolean.TRUE.equals(session.getAttribute("isPspSales"));
    }

    /** Same walk {@code SummitPlanTemplateAdmin.resolveCurrentPspId} uses. */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** Display-only, for {@code created_by}/{@code locked_by}/{@code recorded_by}. Null when the session cannot name anyone. */
    private static String resolveCurrentUserName(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        return local.getCurrentPerson().getFullName();
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        forward(request, response, VIEW);
    }

    /** S58-P4 — same attributes, caller-chosen view ({@link #VIEW} or {@link #AGENT_VIEW}). */
    private void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Enrollment Matrix");
        request.setAttribute("pageIcon", "bi-grid-3x3-gap");
        request.getRequestDispatcher(view).forward(request, response);
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trimToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

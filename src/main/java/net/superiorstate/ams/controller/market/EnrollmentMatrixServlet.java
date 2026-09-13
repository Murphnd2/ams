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
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationModule;

import java.io.IOException;
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
 */
@WebServlet(name = "EnrollmentMatrixServlet", value = "/EnrollmentMatrix")
public class EnrollmentMatrixServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/view/market/enrollmentMatrix25.jsp";

    private static final String FLASH_MESSAGE = "enrollmentMatrixMessage";
    private static final String FLASH_ERROR = "enrollmentMatrixError";

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

            forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
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

            String action = request.getParameter("action");
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
        request.setAttribute("pageTitle", "Enrollment Matrix");
        request.setAttribute("pageIcon", "bi-grid-3x3-gap");
        request.getRequestDispatcher(VIEW).forward(request, response);
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

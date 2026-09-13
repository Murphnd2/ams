package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixEntryDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixParticipantDAO;
import net.superiorstate.ams.data.dao.PayrollFrequencyDAO;
import net.superiorstate.ams.data.dao.SummitFileExportDAO;
import net.superiorstate.ams.data.dao.SummitPlanTemplateMapDAO;
import net.superiorstate.ams.data.dao.SummitSetupStepDAO;
import net.superiorstate.ams.data.resolver.EmployerDisplayNameResolver;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.SummitCdhElementResolver;
import net.superiorstate.ams.data.resolver.SummitEmployerElementResolver;
import net.superiorstate.ams.data.resolver.SummitEmployerFlagResolver;
import net.superiorstate.ams.data.resolver.SummitImportTemplateResolver;
import net.superiorstate.ams.data.resolver.SummitPlanDateRuleResolver;
import net.superiorstate.ams.data.resolver.SummitPlanTemplateResolver;
import net.superiorstate.ams.data.resolver.SummitPlanTemplateResolver.PlanTemplate;
import net.superiorstate.ams.data.service.SummitSftpService;
import net.superiorstate.ams.data.service.SummitSftpService.SftpTransportException;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.market.EnrollmentMatrix;
import net.superiorstate.ams.model.market.EnrollmentMatrixEntry;
import net.superiorstate.ams.model.market.EnrollmentMatrixParticipant;
import net.superiorstate.ams.model.market.PayrollFrequency;
import net.superiorstate.ams.model.market.SummitFileExport;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.ApplicationFieldValue;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * S24-E, stage 1 — generates the two Summit import files AMS can source today from a
 * proposal's own data: Employer Demographic and Employer CDH Plan. Participants and
 * enrollment are out of scope; they need an employee roster AMS does not have. See
 * {@code docs/business/summit_data_exchange.md} for the file spec this implements.
 * <p>
 * PSP-admin-only, reachable only by URL — no nav entry, no menu link. A user clicks the
 * link Kevin gives them, a file downloads, and the file is uploaded to Summit by hand.
 * No FTP, no automation, no scheduling.
 */
@WebServlet(name = "SummitExportServlet", value = "/SummitExport")
public class SummitExportServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitExportServlet.class);

    private static final DateTimeFormatter SUMMIT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    // S29-D -- the timestamp appended to a template-named filename. Second resolution, unlike
    // SUMMIT_DATE's day resolution, so two downloads of the same file on the same day stay
    // distinct in the operator's downloads folder. Used only on the configured path; the legacy
    // filenames keep SUMMIT_DATE exactly as they had it.
    private static final DateTimeFormatter SUMMIT_FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // S42-B -- request attribute doPost sets before delegating to doGet, so the shared dispatch
    // can tell a push from a download. Never set by a GET.
    private static final String PUSH_ATTR = "summitExport.push";

    // S29-D -- the values the `type` request parameter may take. Named so the request
    // contract, the dispatch and the SUMMIT_IMPORT_TEMPLATES lookup key can never drift apart.
    private static final String TYPE_EMPLOYER = "employer";
    private static final String TYPE_CDH_PLAN = "cdhplan";
    private static final String TYPE_DEMOGRAPHICS = "demographics";
    // S30-A -- HRA Enrollment, template ZZ_TEST_HRA_ENROLL. A fourth discriminator rather than a
    // mode of an existing one: it is a separate Summit import template with its own filename
    // prefix, so it must be separately addressable in SUMMIT_IMPORT_TEMPLATES.
    // S56-C -- matrix-sourced: one row per non-declined enrollment_matrix_entry (V106) whose leg
    // carries import_file_type = 'enrollment' (V108). The pre-S56-C flat-answer body is gone.
    private static final String TYPE_ENROLLMENT = "enrollment";
    // V104 -- the $1 card-issuer seed election, a `125 PI Elections` file. A fifth discriminator,
    // not a mode of writeHraEnrollment: different Summit import template (125 PI Elections, not
    // HRA Enrollment), different layout, different plan (the card-issuer row, not the ICHRA row).
    private static final String TYPE_CARD_SEED = "cardseed";
    // S56-C -- 125 PI Elections, template ZZ_TEST_125_ELECTIONS, matrix-sourced: one row per
    // non-declined enrollment_matrix_entry whose leg carries import_file_type = 'elections'.
    // Same eleven-column layout writeCardSeedElection emits, real amounts instead of $1.00.
    // The interim sixth type "enrollmatrix" (S55-B/S55-C) is deleted; its loader is now the
    // shared engine behind this type and TYPE_ENROLLMENT (see exportMatrixFile).
    private static final String TYPE_ELECTIONS = "elections";

    // Application-answer field keys this export reads (S25-B). Defined in both
    // DatabaseInitializer's baseline sections and the package JSONs under
    // src/main/resources/packages/ -- see docs/analysis/legal_assumptions.md LA-31.
    private static final String FIELD_ADDRESS_STREET1 = "address_street1";
    private static final String FIELD_ADDRESS_CITY = "address_city";
    private static final String FIELD_ADDRESS_STATE = "address_state";
    private static final String FIELD_ADDRESS_ZIP = "address_zip";
    // T238 part 2 / N1 (s48) -- Employer Name source. Lives in the "general" package (attached to
    // every application, unlike the LOS-scoped fields below), so it is present on every setup.
    private static final String FIELD_COMPANY_LEGAL_NAME = "company_legal_name";
    // plan_year_start / plan_year_end ship in the s125_fsa package's LOS-scoped
    // plan_year_eligibility section, so they are only present when that section is attached
    // to the LOS being sold -- a refusal below is expected behaviour on an installation where
    // it is not, not a defect.
    private static final String FIELD_PLAN_YEAR_START = "plan_year_start";
    private static final String FIELD_PLAN_YEAR_END = "plan_year_end";
    // S30-A -- the flat "Annual Amount per Employee" answer. Ships in the hra package's LOS-scoped
    // hra_benefit_allocation section ("105 Benefit Allocation") as a REQUIRED field, so it is
    // present exactly when that section is attached to the LOS being sold -- the same
    // installation-configuration dependency, and the same expected refusal, as the plan-year
    // fields above. S56-C: read only by file 2's allowance plan-name composition now; the
    // enrollment files take their amounts from the matrix, not from this answer.
    private static final String FIELD_HRA_ANNUAL_EE = "hra_annual_ee";

    // S28-B/S28-D -- the key segment and label the legacy single-row fallback synthesises when
    // SUMMIT_PLAN_TEMPLATES is unset. Both were literals in the pre-S28-B row builder; the
    // constant exists so the fallback's byte-for-byte equivalence to v0.94.00 is visible in one
    // place rather than inferred from separate string literals.
    private static final String LEGACY_ICHRA_SEGMENT = "ICHRA";
    // The synthetic fallback template's service item id. Never matched against anything by
    // construction: the legacy branch runs only when the configured list is empty, so no
    // elected-service comparison is ever performed on this value.
    private static final int LEGACY_UNMATCHED_SERVICE_ITEM_ID = 0;

    // S29-I -- the Demographics template's mandatory final column (L). It exists only to guarantee
    // the last field is never empty, which a trailing optional value cannot promise; see
    // writeDemographics. Overridable per installation via SUMMIT_BRANCH_CODE, but never absent --
    // an unset key falls back to this literal rather than emitting an empty mandatory column.
    private static final String DEFAULT_BRANCH_CODE = "AMS";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String proposalIdParam = request.getParameter("proposalId");
        String type = request.getParameter("type");
        if (proposalIdParam == null || proposalIdParam.isBlank()
                || type == null || !(type.equals(TYPE_EMPLOYER) || type.equals(TYPE_CDH_PLAN)
                        || type.equals(TYPE_DEMOGRAPHICS) || type.equals(TYPE_ENROLLMENT)
                        || type.equals(TYPE_CARD_SEED) || type.equals(TYPE_ELECTIONS))) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "proposalId and type (employer|cdhplan|demographics|enrollment|cardseed|elections) are required.");
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
            // PSP admin, then ICHRA entitlement — same order and same resolver every other
            // ICHRA surface uses, so this gate can never disagree with the rest of the feature.
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Proposal proposal = em.find(Proposal.class, proposalId);
            Prospect prospect = proposal != null ? proposal.getProspect() : null;
            if (prospect == null) {
                writePlainError(response, HttpServletResponse.SC_NOT_FOUND, "Proposal not found.");
                return;
            }

            // S25-B -- employer address and plan year both come from the application's own
            // answers, not Prospect.address or proposal_ichra_intake (LA-31). A proposal that
            // has never been applied against has no rows here; refuse rather than guess.
            Map<String, String> answers = loadApplicationAnswers(em, proposalId);
            if (answers.isEmpty()) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Proposal " + proposalId + " has no submitted application; the Summit"
                                + " export reads employer details from application answers.");
                return;
            }

            // S25-C -- Employer TPA Custom ID is Summit's upsert key. A bare Prospect.id is
            // only unique within this AMS installation; a configured prefix (LA-29) keeps it
            // unique across installations that could ever feed the same Summit TPA account.
            // Built once, here, so both files can never emit a different value for the same
            // employer.
            String employerTpaCustomId = resolveEmployerTpaCustomId(prospect);
            if (employerTpaCustomId == null) {
                log.error("[SUMMIT-EXPORT] SUMMIT_TPA_ID_PREFIX is absent or invalid on this installation");
                writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Cannot generate Summit export: this installation has no valid"
                                + " SUMMIT_TPA_ID_PREFIX configured. Set it in ssa.properties to"
                                + " this installation's Summit TPA prefix (e.g. SSA) before"
                                + " generating either file. The value must not be blank and must"
                                + " contain only letters and digits — Summit rejects an Employer"
                                + " TPA Custom ID carrying any other character, so a prefix like"
                                + " SSA-158 would produce IDs it refuses.");
                return;
            }

            // S31-D -- the PSP whose plan template mapping applies, resolved once here because
            // the session is reachable from doGet and from nowhere further in. Null is a
            // supported answer, not an error: SummitPlanTemplateResolver skips the V095 table
            // and uses the SUMMIT_PLAN_TEMPLATES property, which is exactly the pre-V095
            // behaviour. The two plan-emitting writers take it directly; the employer writer
            // (T238 part 2) now also takes it, to tell whether the sale elected ICHRA, and it
            // separately consults the flag config -- the Demographics writer alone still consults
            // no plan mapping.
            Long pspId = resolveCurrentPspId(request);

            // S42-B -- push mode checks. Run only when doPost has set PUSH_ATTR; a GET never
            // reaches here with it set. Every refusal returns before anything is generated,
            // the same shape the confirm guards in the dispatch below use. S56-C -- the T201
            // "enrollment cannot be pushed" refusal that used to sit here is gone: AMS now stores
            // election state (the enrollment matrix, V106), so both matrix-sourced files push
            // through the same checks as every other type.
            String pushDir = null;
            Long ackDuplicate = null;
            if (Boolean.TRUE.equals(request.getAttribute(PUSH_ATTR))) {
                if (!"true".equalsIgnoreCase(AppConfig.get("SUMMIT_PUSH_ENABLED"))) {
                    writePlainError(response, HttpServletResponse.SC_FORBIDDEN,
                            "Not pushed: push to DataPath is disabled on this installation"
                                    + " (SUMMIT_PUSH_ENABLED).");
                    return;
                }
                if (!(type.equals(TYPE_EMPLOYER) || type.equals(TYPE_CDH_PLAN)
                        || type.equals(TYPE_DEMOGRAPHICS) || type.equals(TYPE_CARD_SEED)
                        || type.equals(TYPE_ENROLLMENT) || type.equals(TYPE_ELECTIONS))) {
                    writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Not pushed: type is not pushable.");
                    return;
                }

                // The resolved directory's final path segment must be exactly "ImportFiles",
                // mirroring SummitSftpTestServlet's inverse guard -- checked but never echoed.
                String importDir = AppConfig.get("SUMMIT_SFTP_IMPORT_DIR");
                boolean importDirValid = importDir != null && !importDir.isBlank()
                        && importDir.indexOf('\r') < 0 && importDir.indexOf('\n') < 0;
                if (importDirValid) {
                    String normalized = importDir.endsWith("/")
                            ? importDir.substring(0, importDir.length() - 1) : importDir;
                    int lastSlash = normalized.lastIndexOf('/');
                    String finalSegment = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
                    importDirValid = "ImportFiles".equals(finalSegment);
                }
                if (!importDirValid) {
                    writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Not pushed: SUMMIT_SFTP_IMPORT_DIR is not configured to an ImportFiles"
                                    + " directory.");
                    return;
                }

                if (SummitImportTemplateResolver.templateNameFor(type).isEmpty()) {
                    writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Not pushed: no Summit import template is configured for this type, so"
                                    + " Summit would ignore the file.");
                    return;
                }

                try {
                    ackDuplicate = Long.valueOf(request.getParameter("ackDuplicate"));
                } catch (NumberFormatException e) {
                    ackDuplicate = null;
                }

                pushDir = importDir;
            }

            // S32-A -- the identity of this export, assembled once here because doGet is the only
            // place all of it is in scope: proposalId is parsed here, pspId is resolved here, and
            // the acting user is reachable only from the session, which no writer receives. It is
            // then threaded through every writer to the single shared write path, so recording
            // happens at one point rather than at four call sites that could diverge.
            // S42-B -- pushDir/ackDuplicate are null for a GET (download); the T229 push path
            // sets both above.
            ExportRecord record = new ExportRecord(em, type, pspId, proposalId, prospect.getId(),
                    resolveCurrentUserName(request), pushDir, ackDuplicate);

            if (type.equals(TYPE_EMPLOYER)) {
                writeEmployerDemographic(response, prospect, answers, employerTpaCustomId, record,
                        em, proposalId, pspId);
            } else if (type.equals(TYPE_CDH_PLAN)) {
                writeEmployerCdhPlan(response, em, proposalId, prospect, answers, employerTpaCustomId, pspId, record);
            } else if (type.equals(TYPE_DEMOGRAPHICS)) {
                writeDemographics(response, em, prospect, employerTpaCustomId, record);
            } else if (type.equals(TYPE_CARD_SEED)) {
                // V104 -- confirm gate (T201 shape) plus an operator-editable effective date. TA-e
                // (docs/business/summit_data_exchange.md) records run-date + 1 as the default, but
                // card issuance timing is untestable until SSA issues cards (T245), so the field is
                // editable rather than fixed -- reversible without a deploy. Runs before any
                // response write, mirroring the enrollment guard below exactly.
                String expectedConfirm = "CARDSEED-P" + proposalId;
                LocalDate cardSeedToday = LocalDate.now();
                LocalDate defaultEffectiveDate = cardSeedToday.plusDays(1);
                LocalDate submittedEffectiveDate = parseAnswerDate(request.getParameter("effectiveDate"));
                boolean confirmMatches = expectedConfirm.equals(request.getParameter("confirm"));
                String dateError = null;
                if (confirmMatches) {
                    if (submittedEffectiveDate == null) {
                        dateError = "The effective date could not be read. Enter a date and try again.";
                    } else if (submittedEffectiveDate.isBefore(cardSeedToday)) {
                        dateError = "The effective date " + submittedEffectiveDate + " is in the"
                                + " past. A past effective date on an election file back-posts every"
                                + " elapsed contribution run date immediately -- choose today or later.";
                    }
                }
                if (!confirmMatches || dateError != null) {
                    writeCardSeedConfirmPage(response, em, prospect, proposalId,
                            record.pushDir() != null, defaultEffectiveDate, dateError);
                    return;
                }

                // V104 -- prior-push guard. AMS records nothing per participant (Elections is
                // neither additive nor a replacement -- a duplicate row fails per row, "Plan
                // Already Enrolled"), so a second run re-emits every roster row. Refuse unless the
                // caller acknowledges the specific prior push.
                Long ackPrior;
                try {
                    ackPrior = Long.valueOf(request.getParameter("ackPrior"));
                } catch (NumberFormatException e) {
                    ackPrior = null;
                }
                SummitFileExport priorCardSeedPush =
                        SummitSetupStepDAO.findLatestPushed(em, pspId, proposalId, TYPE_CARD_SEED);
                if (priorCardSeedPush != null && !Objects.equals(priorCardSeedPush.getId(), ackPrior)) {
                    writeCardSeedPriorPushRefusal(response, record, priorCardSeedPush, submittedEffectiveDate);
                    return;
                }

                writeCardSeedElection(response, em, proposalId, prospect, answers,
                        employerTpaCustomId, pspId, record, submittedEffectiveDate);
            } else if (type.equals(TYPE_ENROLLMENT) || type.equals(TYPE_ELECTIONS)) {
                // S56-C -- both matrix-sourced enrollment files share one front door: setup
                // resolution, the whole-matrix completeness gate, the unassigned-route refusal,
                // the confirm token (ENROLL-ALL-P / ELECT-ALL-P, keyed on proposalId like every
                // sibling) and then the parameterised writer. See exportMatrixFile.
                exportMatrixFile(request, response, em, proposalId, prospect, answers,
                        employerTpaCustomId, record, type);
            } else {
                // Unreachable: the type whitelist above admits nothing else. Kept as a plain
                // refusal rather than a fall-through into any writer.
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "Unsupported type.");
            }
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * S42-B — the push mode of the export flow (T229). Sets {@link #PUSH_ATTR} and delegates to
     * {@link #doGet}, so every check and the existing type dispatch are shared with the download
     * path; a GET can never push. See the push checks near {@code ExportRecord}'s construction.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute(PUSH_ATTR, Boolean.TRUE);
        doGet(request, response);
    }

    /**
     * Resolves this installation's Summit TPA prefix from config and combines it with
     * {@code Prospect.id} into the {@code Employer TPA Custom ID} both files share. The prefix
     * is read at request time, never hardcoded — a second installation uses a different one.
     * Returns null if the prefix is absent, blank, or carries any non-alphanumeric character; the
     * caller refuses to emit rather than fall back to a bare id, which would create a duplicate
     * employer in Summit under a different key.
     * <p>
     * ⚠️ <b>S29-G2 — the separator is {@code E}, not a hyphen.</b> Established by import
     * 2026-09-08: Summit rejected both {@code 158-140952} and {@code 158_140952} with
     * {@code Invalid data for Employer TPA Custom ID.} <b>before field binding</b> — the key echo
     * came back empty — while {@code 158140952} was accepted. That makes it character validation,
     * not a business rule. {@code E} is alphanumeric and keeps the key self-describing rather than
     * an undelimited digit run.
     * <p>
     * <b>The constraint does not generalise — do not "fix" the other two identifiers to match.</b>
     * The same import round proved {@code Import Plan ID} accepts {@code 158140952-PROBEA-2026},
     * and {@code Participant TPA Custom ID} accepts {@code 158-P-9001} — the latter then enrolled
     * successfully, clearing the stage where a bad participant key surfaces as
     * {@code Employer ID Conflict}. Three identifiers, three different validations.
     * <p>
     * ⚠️ <b>S50 — that {@code Import Plan ID} observation is per-file-type, not universal.</b>
     * {@code 158140952-PROBEA-2026} was accepted by {@code Employer CDH Plan} and
     * {@code HRA Enrollment}. It is rejected by {@code 125 PI Contributions}, which returns
     * {@code Invalid data for Import Plan ID} for any non-alphanumeric character in that field —
     * proven 2026-09-12 by {@code Validate Import Format}. The rule is per-field and per-file-type;
     * AMS now composes {@code Import Plan ID} strictly alphanumeric everywhere so one shipped form
     * satisfies every file type.
     * <p>
     * The prefix check is widened to match the field it feeds: a prefix carrying a hyphen or
     * underscore ({@code SSA-158}) would compose an ID Summit refuses, and that failure would
     * surface in a results file rather than at configuration time. Rejecting every
     * non-alphanumeric subsumes the pipe and whitespace checks it replaces.
     */
    public static String resolveEmployerTpaCustomId(Prospect prospect) {
        String rawPrefix = AppConfig.get("SUMMIT_TPA_ID_PREFIX");
        if (rawPrefix == null) return null;
        String prefix = rawPrefix.trim();
        if (prefix.isEmpty()) return null;
        if (!prefix.matches("[A-Za-z0-9]+")) return null;
        return prefix + "E" + prospect.getId();
    }

    /**
     * S31-D — the current session's PSP id, or null.
     * <p>
     * Reads {@code local.getCurrentPerson().getPsp()}, the pattern every other PSP-scoped surface
     * uses ({@code AgencyAction:52-53}, {@code PspDashboardHome:76}). {@code Prospect} carries no
     * PSP reference, so the session is the only route.
     * <p>
     * ⚠️ <b>Every step is null-tolerant and null is a supported return.</b> A caller that cannot
     * resolve a PSP gets the {@code SUMMIT_PLAN_TEMPLATES} property path — the pre-V095 behaviour —
     * rather than an error page. This method must never be the reason an export fails.
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /**
     * S32-A -- display name of the acting user, for {@code summit_file_export.generated_by}.
     * Display-only, exactly as {@code SummitPlanTemplateAdmin.resolveCurrentUserName} is: null when
     * the session cannot name anyone, which the recording treats as a supported state rather than a
     * reason to fail.
     */
    private static String resolveCurrentUserName(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        return local.getCurrentPerson().getFullName();
    }

    /**
     * Loads this proposal's application answers keyed by {@code fieldKey}. Follows
     * {@code AgentSetupSnapshotLoader}'s pattern -- proposal id straight to values, no
     * {@code Application} handle needed. Empty when the proposal has no application yet
     * (rows are created lazily, only once someone opens or saves the application form), or
     * when it has one with no saved answers.
     */
    private Map<String, String> loadApplicationAnswers(EntityManager em, long proposalId) {
        Query q = em.createQuery(
                "SELECT fv FROM ApplicationFieldValue fv " +
                "JOIN FETCH fv.applicationField af " +
                "WHERE fv.application.proposal.id = :pid");
        q.setParameter("pid", proposalId);
        @SuppressWarnings("unchecked")
        List<ApplicationFieldValue> fieldValues = (List<ApplicationFieldValue>) q.getResultList();
        Map<String, String> valueMap = new LinkedHashMap<>();
        for (ApplicationFieldValue fv : fieldValues) {
            valueMap.put(fv.getApplicationField().getFieldKey(), fv.getFieldValue());
        }
        return valueMap;
    }

    /**
     * S28-B/S28-D — the services this proposal's employer actually elected, as an ordered map
     * of {@code ServiceItem.id} to {@code ServiceItem.description}.
     * <p>
     * {@code ApplicationModule} is the join AMS records the sale on: its composite PK is
     * application × {@code ServiceItem}, and both {@code ApplyForProposal} and
     * {@code CreateSetup25} write it by collapsing each elected {@code LOS} and
     * {@code Enhancement} onto that entity's own {@code serviceItem}. So this one query covers
     * all three ways a service reaches a sale without caring which one it came through — the
     * same keying S27-D established for the setup checklist.
     * <p>
     * ⚠️ <b>Matching is on the id, never on {@code ServiceItem.code}</b> — that column is
     * unreachable from the Service Manager UI (S28-C: {@code ServiceManagerAction} never calls
     * {@code setCode} and the JSP has no such field), so it is null on every real installation
     * and cannot key anything. The <b>description</b> is carried alongside purely so the
     * mismatch error page can name each service in a way a human recognises; it is never
     * matched on.
     * <p>
     * Returns the entity rather than projecting a scalar, matching the two live precedents for
     * this filter — {@code ApplicationTaskDAO.getModulesForApplication} and
     * {@code ActivityDAO.moduleExists}. Rows with no {@code ServiceItem} are skipped; a null
     * description becomes an empty string rather than dropping the row, since the row still
     * matters for matching. Empty when the proposal has no modules.
     */
    private Map<Integer, String> loadElectedServiceItems(EntityManager em, long proposalId) {
        Query q = em.createQuery(
                "SELECT am FROM ApplicationModule am " +
                "WHERE am.application.proposal.id = :pid");
        q.setParameter("pid", proposalId);
        @SuppressWarnings("unchecked")
        List<ApplicationModule> modules = (List<ApplicationModule>) q.getResultList();
        Map<Integer, String> elected = new LinkedHashMap<>();
        for (ApplicationModule module : modules) {
            ServiceItem serviceItem = module.getServiceItem();
            if (serviceItem == null) continue;
            String description = serviceItem.getDescription();
            elected.put(serviceItem.getId(), description == null ? "" : description);
        }
        return elected;
    }

    /**
     * Employer Demographic — eleven columns (T238 part 2 / D46, s48). {@code Employer TPA Custom
     * ID} stays the configured installation prefix plus {@code Prospect.id} (S25-C, LA-29).
     * <p>
     * <b>N1′ (Kevin, s48b3, supersedes N1's no-fallback rule) — Employer Name prefers the
     * {@code company_legal_name} answer; a blank answer falls back to {@code Prospect.name} with
     * a WARN, never a refusal, unless both are blank.</b> The filename still stays on
     * {@code Prospect.name}.
     * <p>
     * Columns 8–11 are the four Summit administration flags, unioned across elected
     * {@code ServiceItem}s by {@link SummitEmployerFlagResolver} and emitted as explicit
     * {@code true}/{@code false} via {@link SummitCdhElementResolver#bool}; refused when none is
     * true. Column 7, Employer Plan Name, is populated when exactly one elected plan's key segment
     * is in {@code SUMMIT_ALLOWANCE_KEY_SEGMENTS} (comma-separated, default {@code ICHRA} — N7′,
     * s48b2) and the COBRA flag is false (N5) — see {@link #allowanceSegment} and
     * {@link #employerPlanName}. It may be empty; that is legal only because the four
     * always-populated flag columns follow it.
     */
    private void writeEmployerDemographic(HttpServletResponse response, Prospect prospect,
                                           Map<String, String> answers, String employerTpaCustomId,
                                           ExportRecord record, EntityManager em, long proposalId,
                                           Long pspId)
            throws IOException {
        // N1' (Kevin, s48b3) -- prefer the application's legal name; a blank answer falls back to
        // the prospect name with a WARN, not a refusal. Only both being blank refuses.
        String employerName = EmployerDisplayNameResolver.resolve(answers, prospect);
        if (employerName == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer Demographic file: prospect " + prospect.getId()
                            + " has neither a 'company_legal_name' answer nor a prospect name.");
            return;
        }

        String address1 = answers.get(FIELD_ADDRESS_STREET1);
        String city = answers.get(FIELD_ADDRESS_CITY);
        String state = answers.get(FIELD_ADDRESS_STATE);
        String zip = answers.get(FIELD_ADDRESS_ZIP);

        String missingField = firstBlank(
                FIELD_ADDRESS_STREET1, address1,
                FIELD_ADDRESS_CITY, city,
                FIELD_ADDRESS_STATE, state,
                FIELD_ADDRESS_ZIP, zip);
        if (missingField != null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer Demographic file: prospect " + prospect.getId()
                            + "'s application is missing answer '" + missingField + "'. All four"
                            + " address fields are hard requirements in Summit despite being"
                            + " labelled Optional.");
            return;
        }

        // T238 part 2 / N2 -- the four Summit administration flags, unioned across elected
        // ServiceItems. Three distinguishable refusals: no resolvable PSP, no elected item mapped
        // at all, or mapped rows that are all false. Each one lists every elected item as
        // "id = description", the same shape writeEmployerCdhPlan's own refusal uses.
        Map<Integer, String> electedServices = loadElectedServiceItems(em, proposalId);
        if (pspId == null) {
            log.error("[SUMMIT-EXPORT] Employer Demographic for proposal {}: no PSP resolved for"
                            + " the current session, so no Summit administration flags can be"
                            + " looked up", proposalId);
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate Employer Demographic file: no PSP is resolved for the current"
                            + " session, so the Summit administration flags (Enable CDH/COBRA/"
                            + "Retiree Billing/Direct Bill Administration) cannot be looked up.");
            return;
        }

        SummitEmployerFlagResolver.EmployerFlags flags =
                SummitEmployerFlagResolver.flagsFor(em, pspId, electedServices.keySet());
        boolean anyFlagTrue = flags.isCdh() || flags.isCobra() || flags.isRetireeBilling()
                || flags.isDirectBill();
        if (!anyFlagTrue) {
            StringBuilder electedList = new StringBuilder();
            for (Map.Entry<Integer, String> service : electedServices.entrySet()) {
                if (electedList.length() > 0) electedList.append(", ");
                electedList.append(service.getKey()).append("=").append(service.getValue());
                if (flags.getUnmappedServiceItemIds().contains(service.getKey())) {
                    electedList.append(" (unmapped)");
                }
            }
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer Demographic file: no Summit administration flag is"
                            + " true for prospect " + prospect.getId() + "'s elected services, so no"
                            + " file would be written that turns on any administration in Summit."
                            + " Elected services (ServiceItem id = description): "
                            + (electedServices.isEmpty() ? "(none)" : electedList.toString())
                            + (flags.getMappedCount() == 0
                                    ? ". None of them has a Summit flag mapping."
                                    : ". Every mapped item has all four flags false.")
                            + " Configure at least one flag for at least one elected item on"
                            + " /SummitEmployerFlagAdmin, then retry.");
            return;
        }
        if (!flags.getUnmappedServiceItemIds().isEmpty()) {
            // N3 -- an elected item with no flag mapping (e.g. a line-of-service item like FSA,
            // which maps to nothing by design) does not refuse and does not warn; a single INFO
            // line is enough to explain a missing flag contribution without becoming noise on
            // every setup.
            StringBuilder unmappedList = new StringBuilder();
            for (Integer id : flags.getUnmappedServiceItemIds()) {
                if (unmappedList.length() > 0) unmappedList.append(", ");
                unmappedList.append(id).append("=").append(electedServices.get(id));
            }
            log.info("[SUMMIT-EXPORT] Employer Demographic for proposal {}: elected ServiceItem(s)"
                            + " with no Summit flag mapping, contributing no flags (ServiceItem id ="
                            + " description): {}",
                    proposalId, unmappedList);
        }

        String allowanceSegment;
        try {
            allowanceSegment = allowanceSegment(em, pspId, electedServices, prospect.getId());
        } catch (SummitPlanTemplateResolver.KeySegmentRejectedException e) {
            // ⚠️ S50 -- refuse rather than leave Employer Plan Name unmatched. A key segment bad
            // enough to break file 2's Import Plan ID composition is bad configuration for this PSP
            // full stop, not a problem scoped to the file that composes an id from it.
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate Employer Demographic file: " + e.getMessage()
                            + ". Fix the Summit Plan Templates mapping (SummitPlanTemplateAdmin) or"
                            + " the SUMMIT_PLAN_TEMPLATES property, then retry.");
            return;
        }
        String employerPlanName = employerPlanName(answers, flags, allowanceSegment, prospect.getId());

        // W2 -- the optional element block, appended after the eleven fixed columns, mirroring
        // file 2's SUMMIT_CDH_OPTIONAL_ELEMENTS. Unset means an empty list, which appends nothing
        // and leaves this file byte-identical to what it emitted before W2. That is the default and
        // it must stay the default: a column count that does not match the installation's template
        // rejects the whole file.
        SummitEmployerElementResolver.Parsed optional = SummitEmployerElementResolver.configured();
        if (optional.isRejected()) {
            log.error("[SUMMIT-EXPORT] {}", optional.getRejection());
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate Employer Demographic file: " + optional.getRejection());
            return;
        }
        Map<SummitEmployerElementResolver.Element, String> optionalValues =
                SummitEmployerElementResolver.values(optional.getElements(), answers, prospect.getId());

        List<String> columns = new ArrayList<>(List.of(
                sanitize(employerName),
                employerTpaCustomId,
                sanitize(address1),
                sanitize(city),
                sanitize(state),
                sanitize(zip),
                sanitize(employerPlanName),
                SummitCdhElementResolver.bool(flags.isCdh()),
                SummitCdhElementResolver.bool(flags.isCobra()),
                SummitCdhElementResolver.bool(flags.isRetireeBilling()),
                SummitCdhElementResolver.bool(flags.isDirectBill())));
        // Every configured element resolves to a value (possibly empty) rather than being omitted:
        // Summit binds optional elements positionally, so a missing one would shift every element
        // after it.
        for (SummitEmployerElementResolver.Element element : optional.getElements()) {
            String value = optionalValues.get(element);
            columns.add(sanitize(value == null ? "" : value));
        }
        String line = String.join("|", columns);

        String filename = resolveFilename(TYPE_EMPLOYER,
                "employer-demographic-" + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, line, record);
    }

    // T238 part 2 / N4 (amended s48b2, N7') -- the Employer Plan Name template. One place, so the
    // observed hand-typed shape ("ICHRA allowance: $50.00/month ($600.00/year)") is visible rather
    // than assembled from scattered literals. The label is now the matched key segment, not always
    // "ICHRA" -- see allowanceSegment.
    private static final String EMPLOYER_PLAN_NAME_FORMAT = "%s allowance: $%s/month ($%s/year)";

    /**
     * T238 part 2 / N7′ (s48b2, supersedes N7) — the single Summit plan key segment this sale's
     * Employer Plan Name should be labelled with, or {@code null} for zero or more than one match.
     * Configured segments come from {@code SUMMIT_ALLOWANCE_KEY_SEGMENTS} (comma-separated,
     * trimmed, case-insensitive), defaulting to {@code LEGACY_ICHRA_SEGMENT} alone when unset or
     * blank. Mirrors {@link #writeEmployerCdhPlan}'s own plan resolution: when
     * {@link SummitPlanTemplateResolver#configuredOrThrow} has no rows for this PSP, the legacy
     * single-plan fallback applies, and its synthetic template always carries the
     * {@code ICHRA} key segment ({@code LEGACY_ICHRA_SEGMENT}) — so the legacy path matches only
     * when {@code ICHRA} is configured. Otherwise, every configured template that both matches an
     * elected ServiceItem and carries a configured key segment is a candidate, de-duplicated by
     * upper-cased segment; more than one candidate is ambiguous — there is one Employer Plan Name
     * field and one {@code hra_annual_ee} — so it logs a WARN naming the prospect and the matched
     * segments, and returns {@code null}.
     */
    private static String allowanceSegment(EntityManager em, Long pspId,
                                            Map<Integer, String> electedServices, long prospectId) {
        Set<String> configuredSegments = new HashSet<>();
        String raw = AppConfig.get("SUMMIT_ALLOWANCE_KEY_SEGMENTS", LEGACY_ICHRA_SEGMENT);
        if (raw != null) {
            for (String entry : raw.split(",")) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    configuredSegments.add(trimmed.toUpperCase());
                }
            }
        }
        if (configuredSegments.isEmpty()) {
            configuredSegments.add(LEGACY_ICHRA_SEGMENT.toUpperCase());
        }

        // upper-cased segment -> the segment's own case, as configured on the template. LinkedHashMap
        // so a >1 WARN lists matches in a stable, encounter order rather than hash order.
        Map<String, String> matched = new LinkedHashMap<>();
        // ⚠️ S50 -- throws SummitPlanTemplateResolver.KeySegmentRejectedException (unchecked) on a
        // rejected key segment, propagating up to writeEmployerDemographic's catch, so a
        // misconfigured mapping refuses this file too rather than only leaving this label unmatched.
        List<PlanTemplate> configured = SummitPlanTemplateResolver.configuredOrThrow(em, pspId);
        if (configured.isEmpty()) {
            if (configuredSegments.contains(LEGACY_ICHRA_SEGMENT.toUpperCase())) {
                matched.put(LEGACY_ICHRA_SEGMENT.toUpperCase(), LEGACY_ICHRA_SEGMENT);
            }
        } else {
            for (PlanTemplate template : configured) {
                if (!electedServices.containsKey(template.getServiceItemId())) continue;
                String segment = template.getKeySegment();
                if (segment == null) continue;
                String upper = segment.toUpperCase();
                if (configuredSegments.contains(upper)) {
                    matched.putIfAbsent(upper, segment);
                }
            }
        }

        if (matched.isEmpty()) {
            return null;
        }
        if (matched.size() > 1) {
            log.warn("[SUMMIT-EXPORT] Employer Demographic for prospect {}: Employer Plan Name"
                            + " ambiguous -- {} configured allowance key segments matched elected"
                            + " services ({}), and there is one Employer Plan Name field and one"
                            + " hra_annual_ee, so no single label can be chosen. Column left empty.",
                    prospectId, matched.size(), String.join(", ", matched.values()));
            return null;
        }
        return matched.values().iterator().next();
    }

    /**
     * T238 part 2 / N4–N6 (s48), segment param amended N7′ (s48b2) — column 7, Employer Plan Name.
     * Non-empty only when all three hold: {@code allowanceSegment} is non-null (exactly one
     * configured key segment matched), the COBRA flag is false (N5 — the default COBRA general
     * notice merges this same column as "the Plan" name, so populating it on a COBRA-administered
     * employer would leak allowance text into COBRA notices), and {@code hra_annual_ee} parses via
     * the existing {@link #parseAnnualElectionAmount}. A missing or unparseable amount is not a
     * refusal (N6) — Plan Name is optional, and a sale with no matching segment must still produce
     * this file — it logs at WARN and the column comes back empty.
     */
    private static String employerPlanName(Map<String, String> answers,
                                            SummitEmployerFlagResolver.EmployerFlags flags,
                                            String allowanceSegment, long prospectId) {
        if (allowanceSegment == null) {
            return "";
        }
        if (flags.isCobra()) {
            log.info("[SUMMIT-EXPORT] Employer Demographic for prospect {}: Employer Plan Name"
                    + " suppressed -- COBRA flag is true and the default COBRA general notice"
                    + " merges this same column as the plan name", prospectId);
            return "";
        }
        BigDecimal annual = parseAnnualElectionAmount(answers.get(FIELD_HRA_ANNUAL_EE));
        if (annual == null) {
            log.warn("[SUMMIT-EXPORT] Employer Demographic for prospect {}: '{}' is absent or"
                            + " unparseable, so Employer Plan Name is left empty", prospectId,
                    FIELD_HRA_ANNUAL_EE);
            return "";
        }
        BigDecimal monthly = annual.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        return String.format(EMPLOYER_PLAN_NAME_FORMAT,
                allowanceSegment,
                monthly.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                annual.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /**
     * Employer CDH Plan — eight columns. Plan year comes from the application's own
     * {@code plan_year_start}/{@code plan_year_end} answers, not
     * {@code proposal_ichra_intake.plan_year} (S25-B, LA-31 — the calendar-year assumption in
     * LA-30 is no longer load-bearing here since these are real dates). {@code Employer TPA
     * Custom ID} carries the same configured installation prefix as the Employer Demographic
     * file (S25-C, LA-29) — it is the join between the two files, so a mismatch would break
     * the import. The plan template ID comes from config, resolved here at request time so it
     * is never baked into the WAR.
     * <p>
     * <b>S28-B/S28-D — one file, one row per elected plan.</b> {@code SUMMIT_PLAN_TEMPLATES}
     * maps {@code ServiceItem.id} to a Summit plan template
     * ({@link SummitPlanTemplateResolver}); rows are emitted in config order, for whichever of
     * those ids the employer actually elected. The id — not {@code ServiceItem.code}, which is
     * unreachable from the Service Manager UI and null on real installations (S28-C) — is the
     * key. <b>Plan year is shared by every row</b> — it
     * comes from the two application answers below, and {@code ApplicationFieldValue} is keyed
     * on (application, fieldKey) with no plan or LOS dimension, so one sale structurally cannot
     * carry two plan years.
     * <p>
     * <b>When {@code SUMMIT_PLAN_TEMPLATES} is unset the legacy path runs</b>: one ICHRA row
     * from {@code SUMMIT_ICHRA_PLAN_TEMPLATE_ID}, byte-identical to what production emitted at
     * {@code v0.94.00}. That is the deliberate fallback, not a degraded mode — an installation
     * that has not configured the new key keeps exactly the behaviour it had.
     * <p>
     * ⚠️ <b>The plan year stays inside {@code Import Plan ID}</b>, which is an upsert key
     * (T185). Whether Summit models one plan across successive years or one plan per year is
     * unproven, and nothing has been imported yet, so the reversal cost is zero today and
     * non-zero the moment a file lands. Keeping the year is the recoverable error — a spare
     * plan to delete; dropping it is the destructive one — a renewal overwriting the prior
     * year's plan. ⚠️ <b>The plan name still carries the year</b> (T186), but now derives from
     * the configured {@code label}, so it follows whatever T185 settles rather than
     * pre-committing it.
     */
    private void writeEmployerCdhPlan(HttpServletResponse response, EntityManager em, long proposalId,
                                       Prospect prospect, Map<String, String> answers,
                                       String employerTpaCustomId, Long pspId, ExportRecord record)
            throws IOException {
        LocalDate planYearStart = parseAnswerDate(answers.get(FIELD_PLAN_YEAR_START));
        if (planYearStart == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer CDH Plan file: prospect " + prospect.getId()
                            + "'s application has no usable answer for '" + FIELD_PLAN_YEAR_START
                            + "'.");
            return;
        }
        LocalDate planYearEnd = parseAnswerDate(answers.get(FIELD_PLAN_YEAR_END));
        if (planYearEnd == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer CDH Plan file: prospect " + prospect.getId()
                            + "'s application has no usable answer for '" + FIELD_PLAN_YEAR_END
                            + "'.");
            return;
        }

        // S31-J -- why each candidate plan did not reach the file, one human-readable line each.
        // Read only by the zero-row guard below, which refuses rather than writing an empty file.
        List<String> dropReasons = new ArrayList<>();
        // Carryover warnings are collected rather than logged inline: the sentence an operator needs
        // -- how many OTHER plans survived -- is not knowable until the loop has finished.
        List<String> carryoverWarnings = new ArrayList<>();

        List<PlanTemplate> configured;
        try {
            configured = SummitPlanTemplateResolver.configuredOrThrow(em, pspId);
        } catch (SummitPlanTemplateResolver.KeySegmentRejectedException e) {
            // ⚠️ S50 -- refuse rather than skip the plan. A skipped plan is a plan silently missing
            // from this employer's Summit setup, discovered later when someone cannot enroll into
            // it -- the accept-now-fail-later shape this project keeps getting caught by.
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate Employer CDH Plan file: " + e.getMessage()
                            + ". Fix the Summit Plan Templates mapping (SummitPlanTemplateAdmin) or"
                            + " the SUMMIT_PLAN_TEMPLATES property, then retry.");
            return;
        }
        List<PlanTemplate> emit;
        if (configured.isEmpty()) {
            // LEGACY PATH -- byte-identical to what production emitted at v0.94.00.
            Integer templateId = parsePositiveInt(AppConfig.get("SUMMIT_ICHRA_PLAN_TEMPLATE_ID"));
            if (templateId == null) {
                log.error("[SUMMIT-EXPORT] SUMMIT_ICHRA_PLAN_TEMPLATE_ID is absent or non-numeric on this installation");
                writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Cannot generate Employer CDH Plan file: this installation has no valid"
                                + " SUMMIT_ICHRA_PLAN_TEMPLATE_ID configured. Set it in ssa.properties"
                                + " to the Summit-assigned Plan Template ID before generating this file.");
                return;
            }
            log.warn("[SUMMIT-EXPORT] {} is unset, emitting a single ICHRA row from"
                            + " SUMMIT_ICHRA_PLAN_TEMPLATE_ID ({}); only the ICHRA plan will be"
                            + " created in Summit",
                    SummitPlanTemplateResolver.CONFIG_KEY, templateId);
            emit = java.util.Collections.singletonList(new PlanTemplate(
                    LEGACY_UNMATCHED_SERVICE_ITEM_ID, templateId,
                    LEGACY_ICHRA_SEGMENT, LEGACY_ICHRA_SEGMENT));
        } else {
            Map<Integer, String> electedServices = loadElectedServiceItems(em, proposalId);
            emit = new ArrayList<>();
            for (PlanTemplate template : configured) {
                if (electedServices.containsKey(template.getServiceItemId())) {
                    emit.add(template);
                }
            }
            if (emit.isEmpty()) {
                // This error page is the ONLY way an operator can discover a ServiceItem id
                // without running SQL, which this project does not ask of anyone -- so it names
                // every elected service as "id = description", not just the ids. Configuring
                // SUMMIT_PLAN_TEMPLATES for a new installation starts by reading this page.
                StringBuilder electedList = new StringBuilder();
                for (Map.Entry<Integer, String> service : electedServices.entrySet()) {
                    if (electedList.length() > 0) electedList.append(", ");
                    electedList.append(service.getKey()).append("=").append(service.getValue());
                }
                StringBuilder configuredIds = new StringBuilder();
                for (PlanTemplate template : configured) {
                    if (configuredIds.length() > 0) configuredIds.append(", ");
                    configuredIds.append(template.getServiceItemId());
                }
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Cannot generate Employer CDH Plan file: none of the services elected on"
                                + " prospect " + prospect.getId() + "'s application map to a"
                                + " configured Summit plan template. Elected services (ServiceItem"
                                + " id = description): "
                                + (electedServices.isEmpty() ? "(none)" : electedList.toString())
                                + ". Ids configured in " + SummitPlanTemplateResolver.CONFIG_KEY
                                + ": " + configuredIds + ". Configure "
                                + SummitPlanTemplateResolver.CONFIG_KEY + " in ssa.properties"
                                + " using the ids above, then restart Tomcat.");
                return;
            }
            // S31-D -- record what was dropped. An elected service with no configured template is
            // still skipped silently in the emitted file, which is correct and unchanged; what was
            // missing was any trace of it. The filter above tests config against elections and
            // never looks the other way, so this is the only place the difference exists.
            // Deliberately placed AFTER the empty-emit refusal: when nothing matched, the error
            // page already names every elected service, and warning as well would double-report.
            Set<Integer> mappedServiceItemIds = new HashSet<>();
            for (PlanTemplate template : configured) {
                mappedServiceItemIds.add(template.getServiceItemId());
            }
            StringBuilder unmapped = new StringBuilder();
            for (Map.Entry<Integer, String> service : electedServices.entrySet()) {
                if (mappedServiceItemIds.contains(service.getKey())) continue;
                if (unmapped.length() > 0) unmapped.append(", ");
                unmapped.append(service.getKey()).append("=").append(service.getValue());
                dropReasons.add("elected service " + service.getKey() + " (" + service.getValue()
                        + ") -- no Summit plan template is mapped to it, so no plan row could be built."
                        + " Map it on the Summit Plan Templates admin screen, or in "
                        + SummitPlanTemplateResolver.CONFIG_KEY + ".");
            }
            if (unmapped.length() > 0) {
                log.warn("[SUMMIT-EXPORT] Employer CDH Plan for proposal {}: elected service(s) with"
                                + " no Summit plan template mapped, omitted from the file"
                                + " (ServiceItem id = description): {}",
                        proposalId, unmapped);
            }

            List<String> matchedTemplateIds = new ArrayList<>();
            for (PlanTemplate template : emit) {
                matchedTemplateIds.add(template.getServiceItemId() + "=" + template.getTemplateId());
            }
            log.info("[SUMMIT-EXPORT] proposal {} elected ServiceItem ids {} matched plan templates"
                            + " (serviceItemId=templateId) {}",
                    proposalId, electedServices.keySet(), matchedTemplateIds);
        }

        // W4 (V103) -- columns E/G/H are now per row, evaluated by SummitPlanDateRuleResolver from
        // the sale's plan year and the mapping row's rule/offsets. A seq 0 / PLAN_YEAR_START / 0 / 0
        // row -- every pre-V103 row, every property entry, the legacy synthetic -- resolves to
        // exactly the pair of answers that used to be formatted here, so its line is unchanged.
        // `today` is taken once per export so every row in one file sees the same calendar date.
        LocalDate today = LocalDate.now();
        // S31-J -- planYear is deliberately no longer computed here. A plan's identity and its
        // names carry no year; the year travels in Plan Year Begin / Plan Year End alone.

        // S31-H -- the optional element block, appended after the eight mandatory columns. Unset
        // means an empty list, which appends nothing and leaves this file byte-identical to what it
        // emitted before S31-H. That is the default and it must stay the default.
        SummitCdhElementResolver.Parsed optional = SummitCdhElementResolver.configured();
        if (optional.isRejected()) {
            log.error("[SUMMIT-EXPORT] {}", optional.getRejection());
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate Employer CDH Plan file: " + optional.getRejection());
            return;
        }
        Map<String, String> graceFields = SummitCdhElementResolver.graceFields();

        List<String> lines = new ArrayList<>();
        // W4 -- file 2's results correlate on Plan Name (no row number), so two rows sharing a
        // label produce unattributable result lines. Refused, naming the duplicate. Keyed on the
        // exact emitted label; first occurrence remembered for the message.
        Map<String, PlanTemplate> labelsSeen = new LinkedHashMap<>();
        for (PlanTemplate template : emit) {
            SummitPlanDateRuleResolver.Resolved resolved = SummitPlanDateRuleResolver.resolve(
                    planYearStart, planYearEnd, template.getEffectiveDateRule(),
                    template.getOffsetMonths(), template.getPlanYearOffsetYears(), today,
                    "plan '" + template.getLabel() + "' (ServiceItem " + template.getServiceItemId()
                            + ", template " + template.getTemplateId() + ", seq " + template.getSeq() + ")");
            if (resolved.isRejected()) {
                log.error("[SUMMIT-EXPORT] {}", resolved.getRejection());
                writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Cannot generate Employer CDH Plan file: " + resolved.getRejection());
                return;
            }
            PlanTemplate sameLabel = labelsSeen.putIfAbsent(template.getLabel(), template);
            if (sameLabel != null) {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Cannot generate Employer CDH Plan file: two plan rows would carry the same"
                                + " Plan Name '" + template.getLabel() + "' -- ServiceItem "
                                + sameLabel.getServiceItemId() + " seq " + sameLabel.getSeq()
                                + " (template " + sameLabel.getTemplateId() + ") and ServiceItem "
                                + template.getServiceItemId() + " seq " + template.getSeq()
                                + " (template " + template.getTemplateId() + "). Summit's results"
                                + " file for this template correlates on Plan Name and carries no"
                                + " row number, so the two rows' outcomes could not be told apart."
                                + " Give each mapping row a distinct Label on the Summit Plan"
                                + " Templates admin screen, then retry.");
                return;
            }
            // ⚠️ Grace is per sale and per plan. A key segment absent from SUMMIT_CDH_GRACE_FIELDS
            // means the plan has no grace concept -- ICHRA's path -- and its grace columns emit
            // empty. A key segment that IS listed must produce a readable answer.
            // Null-safe and short-circuited: with SUMMIT_CDH_GRACE_FIELDS unset the map is empty,
            // the lookup never runs, and every plan takes the NOT_APPLICABLE path -- so the default
            // configuration adds no behaviour here at all, not merely no visible difference.
            // getKeySegment() is non-null on all three supply paths (property, V095 table, legacy
            // ICHRA fallback), but an NPE here would break file 2 for every employer, so it is
            // guarded rather than assumed.
            String graceFieldKey = null;
            if (!graceFields.isEmpty() && template.getKeySegment() != null) {
                graceFieldKey = graceFields.get(template.getKeySegment().toUpperCase());
            }
            SummitCdhElementResolver.GraceChoice grace =
                    SummitCdhElementResolver.GraceChoice.NOT_APPLICABLE;
            if (graceFieldKey != null) {
                grace = SummitCdhElementResolver.classifyGrace(answers.get(graceFieldKey));
            }

            if (grace == SummitCdhElementResolver.GraceChoice.CARRYOVER) {
                // ⚠️ Carryover has NO element anywhere in the Employer CDH Plan template -- verified
                // against the live element list 2026-09-08 -- so this plan cannot be imported at
                // all and must be built by hand in Summit. Omit it and name it loudly. The same
                // shape as the unmapped-elected-item skip above: behaviour correct, visibility
                // added. ⚠️ There is no carryover AMOUNT to name -- S31-H searched every application
                // package and none collects one.
                // ⚠️ S31-J -- the warning is DEFERRED to after the loop. It used to end "Every other
                // plan was emitted normally", which was simply false in the case that prompted this
                // fix: on proposal 140956 there were no other plans, and the file came out empty.
                // How many others survived cannot be known here, so the sentence is written once the
                // loop has finished and the count is real.
                carryoverWarnings.add("plan '" + template.getLabel() + "' (ServiceItem "
                        + template.getServiceItemId() + ", template " + template.getTemplateId()
                        + ") OMITTED from the file -- its '" + graceFieldKey + "' answer is Carryover,"
                        + " and carryover has no element in the Employer CDH Plan template, so this"
                        + " plan cannot be imported and must be built by hand in Summit. AMS collects"
                        + " no carryover amount, so the amount must come from the employer.");
                dropReasons.add("plan '" + template.getLabel() + "' (ServiceItem "
                        + template.getServiceItemId() + ") -- answered Carryover on '" + graceFieldKey
                        + "'. Carryover has no element in the Employer CDH Plan template, so this plan"
                        + " cannot be imported at all and must be built by hand in Summit.");
                continue;
            }

            if (grace == SummitCdhElementResolver.GraceChoice.UNRECOGNISED) {
                String raw = answers.get(graceFieldKey);
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Cannot generate Employer CDH Plan file: plan '" + template.getLabel()
                                + "' is mapped to the application answer '" + graceFieldKey
                                + "' for its end-of-year feature, and prospect " + prospect.getId()
                                + "'s application"
                                + ((raw == null || raw.isBlank())
                                        ? " has no answer for it."
                                        : " answers it '" + raw + "', which this export does not"
                                          + " recognise.")
                                + " Accepted answers are "
                                + SummitCdhElementResolver.acceptedGraceAnswers()
                                + ". This is refused rather than defaulted: an unanswered grace"
                                + " question emitted as 'no grace' is a wrong plan setting that"
                                + " Summit imports cleanly and nobody notices. If the plan genuinely"
                                + " has no grace concept, remove its key segment from "
                                + SummitCdhElementResolver.GRACE_FIELDS_KEY + " instead.");
                return;
            }

            // With no optional elements configured, no values are built and none are appended --
            // the row is the eight mandatory columns, byte for byte as before S31-H.
            // W4 -- the grace date derives from THIS row's plan-year end, not the sale's: a
            // prior-year row's grace period belongs to the prior plan year.
            SummitPlanDateRuleResolver.PlanDates dates = resolved.getDates();
            Map<SummitCdhElementResolver.Element, String> optionalValues =
                    optional.getElements().isEmpty()
                            ? java.util.Collections.emptyMap()
                            : buildOptionalValues(grace, dates.getPlanYearEnd());
            lines.add(buildCdhPlanRow(template, employerTpaCustomId,
                    dates.getEffectiveDate().format(SUMMIT_DATE),
                    dates.getPlanYearBegin().format(SUMMIT_DATE),
                    dates.getPlanYearEnd().format(SUMMIT_DATE),
                    optional.getElements(), optionalValues));
        }

        // S31-J -- now the count is real, so the carryover warnings can say something true about it.
        for (String warning : carryoverWarnings) {
            log.warn("[SUMMIT-EXPORT] Employer CDH Plan for proposal {}: {} {}",
                    proposalId, warning,
                    lines.isEmpty()
                            ? "NO other plans were emitted -- this file would have been empty, and the"
                              + " export was refused."
                            : lines.size() + " other plan(s) were emitted normally.");
        }

        // ⚠️ S31-J -- a zero-row file 2 is a defect, not an empty result.
        // Runtime-verified 2026-09-08 on proposal 140956: two elected services were unmapped, the one
        // mapped plan was omitted for carryover, and a 0-byte file downloaded with nothing anywhere
        // saying why. A zero-row file uploaded to Summit does nothing at all, so an operator's next
        // move is to investigate a silent no-op rather than to fix the two real problems named below.
        // ⚠️ This deliberately does NOT reuse the all-unmapped page above by moving or rewording it:
        // that page's exact output is the operator's only route to a ServiceItem id without SQL
        // (D-90), so it keeps its wording, its position and its case. This is a second refusal, at the
        // same 400, covering the case that page cannot see -- plans that mapped and were then dropped.
        if (lines.isEmpty()) {
            StringBuilder reasons = new StringBuilder();
            for (String reason : dropReasons) {
                reasons.append("\n  - ").append(reason);
            }
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer CDH Plan file: no plan rows survived for prospect "
                            + prospect.getId() + ", so the file would have been empty. An empty file"
                            + " imports as nothing at all, so it is refused rather than downloaded."
                            + (dropReasons.isEmpty()
                                    ? " No candidate plans were found at all -- check that the"
                                      + " application elects at least one service."
                                    : " Every candidate plan was dropped, for these reasons:"
                                      + reasons)
                            + "\nFix the reasons above, then generate the file again.");
            return;
        }

        String filename = resolveFilename(TYPE_CDH_PLAN,
                "employer-cdh-plan-" + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, lines, record);
    }

    /**
     * One Employer CDH Plan row — the eight columns in spec order
     * (docs/business/summit_data_exchange.md §2). Field order and {@code sanitize()} usage are
     * unchanged from the pre-S28-B single-row builder; the only difference is that the template
     * id, plan name, import plan id and description now come from the configured
     * {@link PlanTemplate} instead of being hardcoded to ICHRA.
     * <p>
     * W4 (V103): {@code Effective Date} (column E) and {@code Plan Year Begin} (column G) are
     * <b>no longer necessarily equal</b>. Each arrives pre-computed by
     * {@link SummitPlanDateRuleResolver} from the mapping row's rule and offsets; under
     * {@code PLAN_YEAR_START} with zero offsets they are the same value they always were. ⭐ An
     * effective date outside its own plan year is valid — Summit stores it verbatim (2026-09-12) —
     * so this builder applies no guard between them.
     */
    private String buildCdhPlanRow(PlanTemplate t, String employerTpaCustomId,
                                   String effectiveDate, String planYearBegin, String planYearEnd,
                                   List<SummitCdhElementResolver.Element> optionalElements,
                                   Map<SummitCdhElementResolver.Element, String> optionalValues) {
        List<String> columns = new ArrayList<>(List.of(
                String.valueOf(t.getTemplateId()),
                // S31-J -- Plan Name and Plan Description are the mapping row's label, and nothing
                // else. The year is already carried by Plan Year Begin / End, and the employer name
                // restates what the row's own Employer TPA Custom ID column already says. The label
                // is editable per mapping on the Summit Plan Templates admin screen, so renaming a
                // plan needs no code change and no config change.
                sanitize(t.getLabel()),
                // ⚠️ S31-J -- THE UPSERT KEY CARRIES NO PLAN YEAR. A Summit benefit plan persists
                // across plan years and accumulates them: a renewal imports into the SAME plan and
                // attaches another plan year to it, with elections separating by plan year. With the
                // year in this key, next year's export would create a SECOND plan rather than
                // renewing -- every year, indefinitely, with elections split across duplicates that
                // are indistinguishable in the UI except by a key nobody reads. The superseded form
                // was {employerKey}-{keySegment}-{planYear}; T185's do-not-touch protected that
                // shape and is retired, because its premise was wrong.
                // ⚠️ S50 -- THE HYPHEN IS ALSO GONE. `125 PI Contributions` rejects any
                // non-alphanumeric character in Import Plan ID, proven by Validate Import Format on
                // 2026-09-12: `ZZSDX27A-INS125A` and `ZZSDX27A_INS125A` failed, while
                // `ZZSDX27AINS125A`, `INS125A`, `1394` and `ABC123` all passed -- `ABC123` passing
                // proves this is a character check, not a lookup. The shipped form is now
                // {employerTpaCustomId}{keySegment}, strictly alphanumeric. This site and the HRA
                // Enrollment writer's composition (S31-J's note above still applies) must always
                // agree.
                sanitize(employerTpaCustomId + t.getKeySegment()),
                sanitize(t.getLabel()),
                effectiveDate,                              // E  (W4: per row)
                employerTpaCustomId,
                planYearBegin,                              // G  (W4: per row)
                planYearEnd));                              // H  (W4: per row)

        // S31-H -- the optional block, appended in configured order. With no elements configured
        // this loop runs zero times and the joined result is the eight mandatory columns exactly as
        // before. Every configured element resolves to a value (possibly empty) rather than being
        // omitted: Summit binds optional elements positionally, so a missing one would shift every
        // element after it.
        for (SummitCdhElementResolver.Element element : optionalElements) {
            String value = optionalValues.get(element);
            columns.add(sanitize(value == null ? "" : value));
        }

        return String.join("|", columns);
    }

    /**
     * S31-H — the optional element values for one plan, keyed by element, ready to append after the
     * eight mandatory columns in the order {@code SUMMIT_CDH_OPTIONAL_ELEMENTS} lists.
     * <p>
     * Every element in {@link Element} gets an entry, so a configured element always finds a value
     * and never emits a stray null. An element AMS has no source for emits <b>empty</b> rather than
     * being omitted — omitting it would shift every column after it, which is the failure the
     * resolver's refusal-on-unknown-token exists to prevent, arriving by a different door.
     * <p>
     * ⚠️ {@code GRACE_DAYS} is <b>always empty</b> — the grace period is expressed as a date
     * ({@code GRACE_DATE}), because a day count cannot encode Treas. Reg. §1.125-1(e). The token
     * survives only so a template mapping that column still binds positionally.
     * <p>
     * ⚠️ {@code OPEN_ENROLL_START} and {@code OPEN_ENROLL_END} are <b>always empty</b>: S31-H
     * searched every application package and AMS collects no open-enrollment dates anywhere. The
     * tokens are supported so a template that maps those columns still binds positionally; they
     * carry no data and will not until something collects it.
     */
    private Map<SummitCdhElementResolver.Element, String> buildOptionalValues(
            SummitCdhElementResolver.GraceChoice grace, LocalDate planYearEnd) {
        Map<SummitCdhElementResolver.Element, String> values =
                new java.util.EnumMap<>(SummitCdhElementResolver.Element.class);

        // Fixed on every CDH plan (settled 2026-09-08), each config-overridable.
        values.put(SummitCdhElementResolver.Element.RUNOUT_ENABLED, SummitCdhElementResolver.bool(true));
        values.put(SummitCdhElementResolver.Element.RUNOUT_BY_DATE, SummitCdhElementResolver.bool(false));
        values.put(SummitCdhElementResolver.Element.RUNOUT_DAYS, SummitCdhElementResolver.runoutDays());
        values.put(SummitCdhElementResolver.Element.TERM_RUNOUT_TYPE, SummitCdhElementResolver.termRunoutType());
        values.put(SummitCdhElementResolver.Element.TERM_RUNOUT_DAYS, SummitCdhElementResolver.termRunoutDays());

        // No source in AMS -- see the javadoc above.
        values.put(SummitCdhElementResolver.Element.OPEN_ENROLL_START, "");
        values.put(SummitCdhElementResolver.Element.OPEN_ENROLL_END, "");

        // Per sale. A plan whose key segment is not listed in SUMMIT_CDH_GRACE_FIELDS reaches here
        // as NOT_APPLICABLE and emits all three empty -- ICHRA's path, and correct: one layout for
        // every row, blank where the concept does not apply.
        switch (grace) {
            case GRACE -> {
                // ⚠️ BY DATE, not by day count. Treas. Reg. 1.125-1(e) caps a grace period at the
                // fifteenth day of the third calendar month after the plan year ends -- a calendar
                // rule whose length varies with the year-end month, so no fixed count expresses it.
                // S31-H emitted 75 days, which for a 31 December year end resolves to 16 March: one
                // day past the statutory maximum. Do not reintroduce a day count here.
                LocalDate graceEnd = SummitCdhElementResolver.graceDate(planYearEnd);
                values.put(SummitCdhElementResolver.Element.GRACE_ENABLED, SummitCdhElementResolver.bool(true));
                values.put(SummitCdhElementResolver.Element.GRACE_BY_DATE, SummitCdhElementResolver.bool(true));
                values.put(SummitCdhElementResolver.Element.GRACE_DATE,
                        graceEnd == null ? "" : graceEnd.format(SUMMIT_DATE));
                // Redundant once a date is supplied, and a second source of truth for the same fact.
                values.put(SummitCdhElementResolver.Element.GRACE_DAYS, "");
            }
            case NONE -> {
                values.put(SummitCdhElementResolver.Element.GRACE_ENABLED, SummitCdhElementResolver.bool(false));
                values.put(SummitCdhElementResolver.Element.GRACE_BY_DATE, "");
                values.put(SummitCdhElementResolver.Element.GRACE_DATE, "");
                values.put(SummitCdhElementResolver.Element.GRACE_DAYS, "");
            }
            default -> {
                // Not applicable to this plan: the key segment was never listed. CARRYOVER and
                // UNRECOGNISED never reach here -- the writer skips or refuses before building a row.
                values.put(SummitCdhElementResolver.Element.GRACE_ENABLED, "");
                values.put(SummitCdhElementResolver.Element.GRACE_BY_DATE, "");
                values.put(SummitCdhElementResolver.Element.GRACE_DATE, "");
                values.put(SummitCdhElementResolver.Element.GRACE_DAYS, "");
            }
        }
        return values;
    }

    /**
     * Demographics (file 4) — <b>twelve columns in the Summit template's own A–L order</b>, one row
     * per participant on this prospect's roster ({@code employer_participant}, V094).
     * <p>
     * ⚠️ <b>S29-I — the order is dictated by the Summit import template and is not negotiable.</b>
     * Summit cannot reorder mandatory elements: optional elements can only be appended after the
     * mandatory block, which forces {@code E-mail Address} to J and {@code Mailing Address Line 2}
     * to K regardless of where they belong logically. The eleven-column order this replaced put
     * {@code Mailing Address Line 2} at position 6 and {@code Effective Date} last; bound
     * positionally against the template, <b>City would have landed in a state field</b>.
     * <p>
     * ⚠️ <b>The final column exists to guarantee a non-empty last field.</b> A trailing empty
     * optional value breaks Summit's parse, and {@code Mailing Address Line 2} at K is blank on most
     * rosters. {@code Branch Code} is mandatory, always populated, carries no meaning to SSA and is
     * part of no identity — so it is the cheapest possible sentinel, reversible by a template edit
     * rather than by orphaning records. <b>Nothing may be appended after it</b>; a new optional
     * field goes before it and {@code Branch Code} stays last.
     * <p>
     * ⭐ This layout is import-proven: a hand-built file in this order was accepted 2026-09-08,
     * including rows with an empty column K. The one row that failed did so on a field-length limit
     * ({@code Mailing Address Line 1} caps at 50), not on order or on the sentinel.
     * <p>
     * {@code Mailing Address Line 2} and {@code Email} are nullable on the roster and emit empty
     * rather than being dropped or defaulted. {@code Effective Date} comes from
     * {@code EmployerParticipant.effectiveDate} itself, not file 2's plan-year-answer derivation
     * — the entity's own field is documented as applied uniformly from a single form field for
     * exactly this column, never read from a spreadsheet. {@code Participant TPA Custom ID}
     * follows the same rule as {@code Employer TPA Custom ID} (S25-C, LA-29): derived here from
     * the AMS-owned, immutable primary key plus the configured prefix, and never persisted
     * (LA-33).
     * <p>
     * ⚠️ <b>An empty roster is refused, not emitted</b> (T209) — see the guard below.
     */
    private void writeDemographics(HttpServletResponse response, EntityManager em,
                                    Prospect prospect, String employerTpaCustomId,
                                    ExportRecord record)
            throws IOException {
        List<EmployerParticipant> roster = EmployerParticipantDAO.findByProspectId(em, prospect.getId());

        String prefix = summitTpaIdPrefix();
        // Resolved once per export, not per row: it is a constant for the whole file, and a
        // configured value that sanitizes away to nothing would defeat the very column it fills,
        // so the default takes over rather than emitting an empty mandatory field.
        String branchCode = sanitize(summitBranchCode());
        if (branchCode.isEmpty()) branchCode = DEFAULT_BRANCH_CODE;

        List<String> lines = new java.util.ArrayList<>();
        for (EmployerParticipant participant : roster) {
            String participantTpaCustomId = prefix + "-P-" + participant.getId();
            lines.add(String.join("|",
                    employerTpaCustomId,                                    // A
                    participantTpaCustomId,                                 // B
                    sanitize(participant.getFirstName()),                   // C
                    sanitize(participant.getLastName()),                    // D
                    sanitize(participant.getAddressLine1()),                // E
                    sanitize(participant.getCity()),                        // F
                    sanitize(participant.getState()),                       // G
                    sanitize(participant.getPostalCode()),                  // H
                    participant.getEffectiveDate().format(SUMMIT_DATE),     // I
                    sanitize(participant.getEmail()),                       // J  optional
                    sanitize(participant.getAddressLine2()),                // K  optional
                    branchCode));                                           // L  mandatory sentinel
        }

        // ⚠️ T209 -- a zero-row file is a defect, not an empty result. Same guard, same
        // position and same refusal as file 2's (S31-J): an empty file uploads to Summit cleanly and
        // creates no participants at all, so the operator's next move is to investigate a silent
        // no-op rather than to fix the one real problem. The likely trigger is ordinary -- generating
        // this file before the census has been uploaded. There is no drop logic on this path, so
        // unlike file 2 there is exactly one reason and it needs no enumeration.
        if (lines.isEmpty()) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Demographics file: prospect " + prospect.getId() + " has no"
                            + " participants on its roster, so the file would have been empty. An"
                            + " empty file imports as nothing at all, so it is refused rather than"
                            + " downloaded. Upload the census on the Setup screen, then generate the"
                            + " file again.");
            return;
        }

        String filename = resolveFilename(TYPE_DEMOGRAPHICS,
                "demographics-" + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, lines, record);
    }

    /**
     * S55-B — the Setup whose application's proposal is {@code proposalId}, or null. Same chain
     * {@code GoActivityDetail25} and {@code CensusRequestServlet.findSetup} /
     * {@code CensusIntakeService} both already walk; duplicated here rather than shared because
     * none of those classes is in this build's scope fence.
     */
    private static Setup findSetupByProposalId(EntityManager em, long proposalId) {
        Query q = em.createQuery("SELECT s FROM Setup s WHERE s.application.proposal.id = :pid");
        q.setParameter("pid", proposalId);
        q.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<Setup> setups = (List<Setup>) q.getResultList();
        return setups.isEmpty() ? null : setups.get(0);
    }

    /**
     * S55-B / S56-C — one {@code enrollment_matrix_entry} (V106) joined out to its participant and
     * leg. A carrier only. Since S56-C the loader returns <b>every</b> entry row, declined
     * included, because the completeness gate has to see a declined cell as satisfied — the
     * emit-time filters ({@link #isEmittable}) run on top of this list, never inside the loader.
     */
    private record EnrollmentMatrixExportRow(EmployerParticipant participant, EnrollmentMatrixParticipant header,
                                              SummitPlanTemplateMap leg, EnrollmentMatrixEntry entry) {}

    /**
     * S55-B / S56-C — every entry row in one setup's Enrollment Matrix, in participant-then-leg
     * order, for every participant <b>except</b> those on the {@code OTHER_NOT_IMPORTABLE}
     * sentinel. Never null; empty when no matrix exists yet for {@code setupId}, when it exists but
     * has no header rows, or when every header is excluded.
     * <p>
     * <b>Participant order</b> is {@link EmployerParticipantDAO#findByProspectId}'s own roster
     * order (last name, first name, id) — the same order {@code writeDemographics} emits in.
     * <b>Leg order</b> within a participant is {@link SummitPlanTemplateMap#getSortOrder()}, then
     * {@code plan_template_map_id} as a tiebreaker (V095/V103 carry no second ordering column).
     * <p>
     * ⚠️ <b>{@code OTHER_NOT_IMPORTABLE} excludes the whole participant</b> — V106's own migration
     * comment records the meaning ("none of that participant's entries go in the FTP export") and
     * it is the ONLY participant-level exclusion anywhere on this path. Enforced at the header
     * level, before entries are looked at.
     * <p>
     * ⚠️ <b>Declined entries are returned, not dropped</b> (S56-C). The completeness gate needs
     * them: a declined cell is the waiver that satisfies the gate. Row selection for the file is
     * {@link #isEmittable}'s job.
     * <p>
     * ⚠️ <b>The leg is resolved with {@code computeIfAbsent} in the emit loop, not only inside the
     * sort comparator</b> (S55-C). {@code List.sort} invokes its comparator zero times for a list
     * shorter than two elements, so a participant with exactly one entry left the leg cache
     * unpopulated and a plain {@code get()} silently dropped a real row. Do not revert this to a
     * comparator-populated cache. A leg id that no longer exists at all is logged and the row is
     * dropped — fabricating an Import Plan ID for a leg AMS can no longer identify would be worse.
     */
    private List<EnrollmentMatrixExportRow> loadEnrollmentMatrixExportRows(EntityManager em, Long setupId,
                                                                            Prospect prospect) {
        EnrollmentMatrix matrix = EnrollmentMatrixDAO.findBySetupId(em, setupId);
        if (matrix == null) return List.of();

        Map<Long, EnrollmentMatrixParticipant> headersById = new LinkedHashMap<>();
        for (EnrollmentMatrixParticipant header : EnrollmentMatrixParticipantDAO.findByMatrixId(em, matrix.getId())) {
            if (!PayrollFrequency.OTHER_NOT_IMPORTABLE.equals(header.getPayrollFrequency())) {
                headersById.put(header.getId(), header);
            }
        }
        if (headersById.isEmpty()) return List.of();

        Map<Long, EnrollmentMatrixParticipant> headerByParticipantId = new LinkedHashMap<>();
        for (EnrollmentMatrixParticipant header : headersById.values()) {
            headerByParticipantId.put(header.getParticipantId(), header);
        }

        List<EmployerParticipant> roster = prospect == null
                ? List.of() : EmployerParticipantDAO.findByProspectId(em, prospect.getId());

        Map<Long, List<EnrollmentMatrixEntry>> entriesByHeaderId = new LinkedHashMap<>();
        for (EnrollmentMatrixEntry entry : EnrollmentMatrixEntryDAO.findByMatrixId(em, matrix.getId())) {
            if (!headersById.containsKey(entry.getMatrixParticipantId())) continue; // OTHER_NOT_IMPORTABLE
            entriesByHeaderId.computeIfAbsent(entry.getMatrixParticipantId(), k -> new ArrayList<>()).add(entry);
        }

        Map<Long, SummitPlanTemplateMap> legsById = new LinkedHashMap<>();
        List<EnrollmentMatrixExportRow> rows = new ArrayList<>();
        for (EmployerParticipant participant : roster) {
            EnrollmentMatrixParticipant header = headerByParticipantId.get(participant.getId());
            if (header == null) continue;
            List<EnrollmentMatrixEntry> entries = entriesByHeaderId.get(header.getId());
            if (entries == null || entries.isEmpty()) continue;

            entries.sort((a, b) -> {
                SummitPlanTemplateMap legA = legsById.computeIfAbsent(a.getPlanTemplateMapId(),
                        id -> SummitPlanTemplateMapDAO.findById(em, id));
                SummitPlanTemplateMap legB = legsById.computeIfAbsent(b.getPlanTemplateMapId(),
                        id -> SummitPlanTemplateMapDAO.findById(em, id));
                int sortA = legA == null ? Integer.MAX_VALUE : legA.getSortOrder();
                int sortB = legB == null ? Integer.MAX_VALUE : legB.getSortOrder();
                if (sortA != sortB) return Integer.compare(sortA, sortB);
                return Long.compare(a.getPlanTemplateMapId(), b.getPlanTemplateMapId());
            });

            for (EnrollmentMatrixEntry entry : entries) {
                // S55-C -- computeIfAbsent here too, not just in the comparator above; see the
                // javadoc. This is what keeps a single-entry participant in the file.
                SummitPlanTemplateMap leg = legsById.computeIfAbsent(entry.getPlanTemplateMapId(),
                        id -> SummitPlanTemplateMapDAO.findById(em, id));
                if (leg == null) {
                    log.error("[SUMMIT-EXPORT] enrollment matrix entry {} for setup {} references"
                                    + " plan_template_map_id {} which no longer exists -- row skipped",
                            entry.getId(), setupId, entry.getPlanTemplateMapId());
                    continue;
                }
                rows.add(new EnrollmentMatrixExportRow(participant, header, leg, entry));
            }
        }
        return rows;
    }

    /**
     * S56-C — whether an entry carries its leg's mode value: {@code TIER} → {@code tier_name}
     * non-blank; {@code ANNUAL_ELECTION} or {@code MONTHLY_PREMIUM} → {@code amount} non-null.
     * Any other mode (there should be none — {@code NONE} legs never reach the matrix,
     * {@code EnrollmentMatrixServlet:171}) is treated as not populated, never as satisfied.
     * This is the one predicate both the completeness gate and {@link #isEmittable} use.
     */
    private static boolean isModeValuePopulated(SummitPlanTemplateMap leg, EnrollmentMatrixEntry entry) {
        String mode = leg.getEnrollmentAmountMode();
        if ("TIER".equals(mode)) return trimToNull(entry.getTierName()) != null;
        if ("ANNUAL_ELECTION".equals(mode) || "MONTHLY_PREMIUM".equals(mode)) return entry.getAmount() != null;
        return false;
    }

    /**
     * S56-C — row selection for a file: not declined, mode value populated (defensive — redundant
     * once the gate has passed, and it stays), and the leg routed to {@code targetFileType}. The
     * participant-level {@code OTHER_NOT_IMPORTABLE} exclusion already happened in the loader.
     * A NULL {@code import_file_type} never matches any target — the unassigned-route refusal in
     * {@link #exportMatrixFile} has named it before this is ever asked.
     */
    private static boolean isEmittable(EnrollmentMatrixExportRow row, String targetFileType) {
        if (row.entry().isDeclined()) return false;
        if (!isModeValuePopulated(row.leg(), row.entry())) return false;
        return targetFileType.equals(row.leg().getImportFileType());
    }

    /** S56-C — "Last, First — leg label", for the refusal pages. */
    private static String matrixCellLabel(EnrollmentMatrixExportRow row) {
        String legLabel = trimToNull(row.leg().getLabel());
        if (legLabel == null) legLabel = row.leg().getKeySegment();
        return row.participant().getLastName() + ", " + row.participant().getFirstName()
                + " (participant " + row.participant().getId() + ") — " + legLabel;
    }

    /** S56-C — leg label for the unassigned-route refusal: "label (key segment KEYSEG, row N)". */
    private static String legLabel(SummitPlanTemplateMap leg) {
        String label = trimToNull(leg.getLabel());
        return (label == null ? leg.getKeySegment() : label)
                + " (key segment " + leg.getKeySegment() + ", mapping row " + leg.getId() + ")";
    }

    /**
     * S55-B / S56-C — the contribution schedule name for one participant, or null.
     * {@code custom_schedule_name} (the {@code OTHER_CUSTOM} override) wins over the curated
     * {@code payroll_frequency.summit_schedule_name} for the stored code; a stored code that no
     * longer resolves to a row, or resolves to one whose schedule name is NULL, yields null —
     * which every caller emits as an <b>empty column, never a dropped row</b> (TA-13). The
     * column it lands in differs by template: F (Employer) on HRA Enrollment, I (Participant) on
     * 125 PI Elections.
     */
    private static String resolveScheduleName(EntityManager em, EnrollmentMatrixParticipant header) {
        String schedule = trimToNull(header.getCustomScheduleName());
        if (schedule != null) return schedule;
        String storedCode = header.getPayrollFrequency();
        if (storedCode == null || PayrollFrequency.OTHER_CUSTOM.equals(storedCode)
                || PayrollFrequency.OTHER_NOT_IMPORTABLE.equals(storedCode)) {
            return null;
        }
        PayrollFrequency frequency = PayrollFrequencyDAO.findByCode(em, storedCode);
        return frequency == null ? null : trimToNull(frequency.getSummitScheduleName());
    }

    /**
     * S56-C — the shared front door for both matrix-sourced enrollment files ({@link #TYPE_ENROLLMENT}
     * and {@link #TYPE_ELECTIONS}). Runs, in this order, before any byte is generated:
     * <ol>
     *   <li><b>Setup resolution</b> — {@code enrollment_matrix} keys off {@code setup_id} (V106), so
     *       the Setup behind {@code proposalId} is looked up first. Same request parameter every
     *       sibling form sends; no new one.</li>
     *   <li><b>Completeness gate — whole matrix, both files or neither.</b> Every entry row of every
     *       non-{@code OTHER_NOT_IMPORTABLE} participant must be declined (the waiver) or carry its
     *       leg's mode value ({@link #isModeValuePopulated}). Requesting the HRA file with the 125
     *       legs half-filled is refused. The refusal names every offending cell (participant + leg)
     *       and links to the matrix. Enforced here at export only — saving a partly-filled matrix
     *       stays legal. Fires before the confirm token, for download and push alike.</li>
     *   <li><b>Unassigned-route refusal</b> — separately: any leg with at least one emittable entry
     *       and a NULL {@code import_file_type} (V108) is named and the request refused. Nothing is
     *       routed by default and nothing is skipped silently.</li>
     *   <li><b>Mode/template mismatch refusal</b> — a {@code TIER}-mode leg routed to
     *       {@link #TYPE_ELECTIONS}: the 125 PI Elections template has no Tier column and TA-12
     *       defines column G only for the two amount modes, so there is no honest value to emit.
     *       Named and refused rather than sent with a blank amount.</li>
     *   <li><b>Confirm token</b> — {@code ENROLL-ALL-P{proposalId}} / {@code ELECT-ALL-P{proposalId}},
     *       keyed on the proposal like every sibling. The refusal states the row count this file
     *       would carry, computed from the same rows the writer then receives.</li>
     * </ol>
     * The writer refuses again on zero rows (T209 shape) — a backstop, not a duplicate: the matrix
     * can change between the refusal being shown and the request being replayed.
     */
    private void exportMatrixFile(HttpServletRequest request, HttpServletResponse response, EntityManager em,
                                  long proposalId, Prospect prospect, Map<String, String> answers,
                                  String employerTpaCustomId, ExportRecord record, String targetFileType)
            throws IOException {
        boolean isElections = TYPE_ELECTIONS.equals(targetFileType);
        String fileLabel = isElections ? "125 PI Elections" : "HRA Enrollment";

        Setup setup = findSetupByProposalId(em, proposalId);
        if (setup == null) {
            writePlainError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Cannot generate " + fileLabel + ": no Setup activity is linked to proposal "
                            + proposalId + ". The enrollment matrix keys off the setup, not the"
                            + " proposal, and this proposal has no linked setup.");
            return;
        }
        String matrixUrl = "/EnrollmentMatrix?setupId=" + setup.getId();

        List<EnrollmentMatrixExportRow> rows = loadEnrollmentMatrixExportRows(em, setup.getId(), prospect);

        // 2. Completeness gate -- whole matrix.
        List<String> incomplete = new ArrayList<>();
        for (EnrollmentMatrixExportRow row : rows) {
            if (row.entry().isDeclined()) continue;
            if (isModeValuePopulated(row.leg(), row.entry())) continue;
            incomplete.add(matrixCellLabel(row) + " -- needs "
                    + ("TIER".equals(row.leg().getEnrollmentAmountMode()) ? "a tier" : "an amount")
                    + " or Declined");
        }
        if (!incomplete.isEmpty()) {
            StringBuilder body = new StringBuilder();
            body.append(fileLabel).append(" not generated: the Enrollment Matrix for setup ")
                .append(setup.getId()).append(" is incomplete.\n\n")
                .append("Every cell must hold an election or a waiver before either enrollment file")
                .append(" can be produced -- one matrix, one state, both files or neither. ")
                .append(incomplete.size()).append(" cell").append(incomplete.size() == 1 ? "" : "s")
                .append(" still in limbo:\n");
            for (String cell : incomplete) body.append("  - ").append(cell).append("\n");
            body.append("\nComplete them at ").append(matrixUrl).append(" then generate the file again.");
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, body.toString());
            return;
        }

        // 3. Unassigned-route refusal -- any leg with an emittable entry and no import_file_type.
        Map<Long, SummitPlanTemplateMap> unassignedLegs = new LinkedHashMap<>();
        // 4. TIER-mode leg routed to the 125 template -- no honest column G exists.
        Map<Long, SummitPlanTemplateMap> tierOn125 = new LinkedHashMap<>();
        for (EnrollmentMatrixExportRow row : rows) {
            if (row.entry().isDeclined() || !isModeValuePopulated(row.leg(), row.entry())) continue;
            SummitPlanTemplateMap leg = row.leg();
            if (trimToNull(leg.getImportFileType()) == null) {
                unassignedLegs.putIfAbsent(leg.getId(), leg);
            } else if (TYPE_ELECTIONS.equals(leg.getImportFileType())
                    && "TIER".equals(leg.getEnrollmentAmountMode())) {
                tierOn125.putIfAbsent(leg.getId(), leg);
            }
        }
        if (!unassignedLegs.isEmpty()) {
            StringBuilder body = new StringBuilder();
            body.append(fileLabel).append(" not generated: ").append(unassignedLegs.size())
                .append(" plan").append(unassignedLegs.size() == 1 ? "" : "s")
                .append(" with recorded elections ").append(unassignedLegs.size() == 1 ? "has" : "have")
                .append(" no Summit import file assigned, so AMS cannot tell which file")
                .append(" those rows belong in and will not guess:\n");
            for (SummitPlanTemplateMap leg : unassignedLegs.values()) body.append("  - ").append(legLabel(leg)).append("\n");
            body.append("\nSet 'Enrollment import file' on each of these rows at /SummitPlanTemplateAdmin")
                .append(" (HRA Enrollment for ICHRA/HRA/MERP, 125 PI Elections for PremiumPath/FSA/DCA),")
                .append(" then generate the file again.");
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, body.toString());
            return;
        }
        if (!tierOn125.isEmpty()) {
            StringBuilder body = new StringBuilder();
            body.append(fileLabel).append(" not generated: ").append(tierOn125.size())
                .append(" plan").append(tierOn125.size() == 1 ? "" : "s")
                .append(" routed to 125 PI Elections ").append(tierOn125.size() == 1 ? "uses" : "use")
                .append(" the TIER amount mode. That template carries no Tier column and its Annual")
                .append(" Election Amount is defined only for ANNUAL_ELECTION and MONTHLY_PREMIUM,")
                .append(" so there is no honest value to emit:\n");
            for (SummitPlanTemplateMap leg : tierOn125.values()) body.append("  - ").append(legLabel(leg)).append("\n");
            body.append("\nChange the amount mode or the import file on each row at /SummitPlanTemplateAdmin,")
                .append(" then generate the file again.");
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, body.toString());
            return;
        }

        // 5. Confirm token -- keyed on proposalId, matching every sibling.
        int emittable = 0;
        for (EnrollmentMatrixExportRow row : rows) if (isEmittable(row, targetFileType)) emittable++;
        String expectedConfirm = (isElections ? "ELECT-ALL-P" : "ENROLL-ALL-P") + proposalId;
        if (!expectedConfirm.equals(request.getParameter("confirm"))) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    fileLabel + " file not generated.\n\n"
                  + "This file sends every non-declined " + fileLabel + " entry recorded in the"
                  + " Enrollment Matrix for setup " + setup.getId() + " (" + emittable + " row"
                  + (emittable == 1 ? "" : "s") + ") to Summit. Review the matrix at " + matrixUrl
                  + " before sending it.\n\n"
                  + "To generate it anyway, repeat this request with &confirm=" + expectedConfirm);
            return;
        }

        if (isElections) {
            writeElections(response, em, prospect, answers, employerTpaCustomId, setup.getId(), record, rows);
        } else {
            writeHraEnrollment(response, em, prospect, answers, employerTpaCustomId, setup.getId(), record, rows);
        }
    }

    /**
     * HRA Enrollment ({@link #TYPE_ENROLLMENT}) — <b>matrix-sourced since S56-C.</b> Eight columns,
     * one row per non-declined {@code enrollment_matrix_entry} whose leg is routed
     * {@code import_file_type = 'enrollment'}, template {@code ZZ_TEST_HRA_ENROLL}
     * ({@code docs/analysis/summit_import_templates_reference.md} §6):
     * <pre>
     * Employer TPA Custom ID|Participant TPA Custom ID|Import Plan ID|Effective Date|Tier ID|
     * Employer Contribution Schedule|Participant Contribution Schedule|Filler
     * 158E140952|158-P-77|158E140952ICHRA|20260101|EE0|||X
     * </pre>
     * The pre-S56-C body (five columns, one flat {@code hra_annual_ee} answer for every roster
     * participant, template 1030) is gone — Kevin confirmed 2026-09-12 neither legacy download was
     * functional, so this type was rewired rather than preserved. The name is kept so the sibling
     * writers' composition notes still point somewhere true: the Import Plan ID composition is
     * unchanged, {@code sanitize(employerTpaCustomId + keySegment)}.
     */
    private void writeHraEnrollment(HttpServletResponse response, EntityManager em, Prospect prospect,
                                    Map<String, String> answers, String employerTpaCustomId,
                                    Long setupId, ExportRecord record, List<EnrollmentMatrixExportRow> rows)
            throws IOException {
        writeMatrixEnrollmentFile(response, em, prospect, answers, employerTpaCustomId, setupId, record,
                rows, TYPE_ENROLLMENT);
    }

    /**
     * 125 PI Elections ({@link #TYPE_ELECTIONS}) — matrix-sourced (S56-C). Eleven columns, the
     * shape {@link #writeCardSeedElection} emits, one row per non-declined
     * {@code enrollment_matrix_entry} whose leg is routed {@code import_file_type = 'elections'};
     * column G is the recorded amount (annualised for {@code MONTHLY_PREMIUM}, TA-12) and column I
     * the participant's resolved contribution schedule (TA-13). Layout detail on
     * {@link #writeMatrixEnrollmentFile}.
     */
    private void writeElections(HttpServletResponse response, EntityManager em, Prospect prospect,
                                Map<String, String> answers, String employerTpaCustomId,
                                Long setupId, ExportRecord record, List<EnrollmentMatrixExportRow> rows)
            throws IOException {
        writeMatrixEnrollmentFile(response, em, prospect, answers, employerTpaCustomId, setupId, record,
                rows, TYPE_ELECTIONS);
    }

    /**
     * S56-C — the shared engine behind {@link #writeHraEnrollment} and {@link #TYPE_ELECTIONS},
     * parameterised by target file type. One row per {@link #isEmittable} entry, in the loader's
     * order. Layout by template:
     * <ul>
     *   <li>{@link #TYPE_ENROLLMENT}, 8 columns: A Employer TPA Custom ID, B Participant TPA Custom
     *       ID, C Import Plan ID, D Effective Date, E Tier ID ({@code tier_name} verbatim), F
     *       <b>Employer</b> Contribution Schedule (resolved name), G Participant Contribution Schedule
     *       (always empty), H {@code X}.</li>
     *   <li>{@link #TYPE_ELECTIONS}, 11 columns, byte-for-byte the shape {@link #writeCardSeedElection}
     *       emits: A–D as above, E Plan Start Date (empty), F Coverage End Date (empty), G Annual
     *       Election Amount (TA-12: {@code ANNUAL_ELECTION} → {@code amount}; {@code MONTHLY_PREMIUM}
     *       → {@code amount × 12}; scale 2, HALF_UP, {@code BigDecimal} only), H Per Contribution
     *       Amount (empty), I <b>Participant</b> Contribution Schedule (resolved name), J Employer
     *       Contribution Schedule (empty), K {@code X}.</li>
     * </ul>
     * TA-13: the schedule name lands in F on HRA (employer-funded) and I on 125 (employee salary
     * reduction). Resolution order is unchanged from S55-B ({@link #resolveScheduleName}); a
     * missing name is an empty column, never a dropped row. {@code Effective Date} is the
     * {@code plan_year_start} answer for both, as file 2 emits it. Column B is
     * {@code prefix + "-P-" + participant.getId()}, unchanged since S30-A.
     */
    private void writeMatrixEnrollmentFile(HttpServletResponse response, EntityManager em, Prospect prospect,
                                           Map<String, String> answers, String employerTpaCustomId,
                                           Long setupId, ExportRecord record,
                                           List<EnrollmentMatrixExportRow> rows, String targetFileType)
            throws IOException {
        boolean isElections = TYPE_ELECTIONS.equals(targetFileType);
        String fileLabel = isElections ? "125 PI Elections" : "HRA Enrollment";

        LocalDate planYearStart = parseAnswerDate(answers.get(FIELD_PLAN_YEAR_START));
        if (planYearStart == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate " + fileLabel + ": prospect " + prospect.getId()
                            + "'s application has no usable answer for '" + FIELD_PLAN_YEAR_START
                            + "'. Effective Date is the plan year start, the same value file 2"
                            + " emits, so it cannot be derived without it.");
            return;
        }
        String effectiveDate = planYearStart.format(SUMMIT_DATE);
        String prefix = summitTpaIdPrefix();

        List<String> lines = new ArrayList<>();
        for (EnrollmentMatrixExportRow row : rows) {
            if (!isEmittable(row, targetFileType)) continue;
            SummitPlanTemplateMap leg = row.leg();

            // ⚠️ S31-J -- the SAME Import Plan ID composition writeEmployerCdhPlan and
            // writeCardSeedElection use, deliberately duplicated rather than extracted.
            String importPlanId = sanitize(employerTpaCustomId + leg.getKeySegment());
            String participantId = prefix + "-P-" + row.participant().getId();
            String schedule = sanitize(resolveScheduleName(em, row.header()));

            if (isElections) {
                // TA-12 -- column G by mode. The gate guarantees amount is non-null here and the
                // TIER-on-125 refusal guarantees the mode is one of the two below.
                BigDecimal amount = row.entry().getAmount();
                if ("MONTHLY_PREMIUM".equals(leg.getEnrollmentAmountMode())) {
                    amount = amount.multiply(BigDecimal.valueOf(12));
                }
                String annualElection = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
                lines.add(String.join("|",
                        employerTpaCustomId,     // A
                        participantId,           // B
                        importPlanId,            // C
                        effectiveDate,           // D
                        "",                      // E Plan Start Date -- never
                        "",                      // F Coverage End Date -- never
                        annualElection,          // G Annual Election Amount (TA-12)
                        "",                      // H Per Contribution Amount -- never
                        schedule,                // I Participant Contribution Schedule (TA-13)
                        "",                      // J Employer Contribution Schedule -- never
                        "X"));                   // K Filler -- mandatory sentinel
            } else {
                lines.add(String.join("|",
                        employerTpaCustomId,                              // A
                        participantId,                                    // B
                        importPlanId,                                     // C
                        effectiveDate,                                    // D
                        sanitize(row.entry().getTierName()),              // E Tier ID
                        schedule,                                         // F Employer Contribution Schedule (TA-13)
                        "",                                               // G Participant Contribution Schedule -- always empty
                        "X"));                                            // H Filler -- mandatory
            }
        }

        log.info("[SUMMIT-EXPORT] setup {} {} (matrix-sourced): {} row(s) effective {}",
                setupId, fileLabel, lines.size(), effectiveDate);

        // T209 shape -- an empty file imports as nothing at all, so it is refused rather than
        // downloaded. Backstop behind exportMatrixFile's own count, not a duplicate of it.
        if (lines.isEmpty()) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate " + fileLabel + ": setup " + setupId + " has no non-declined"
                            + " entry routed to this file in its Enrollment Matrix, so the file"
                            + " would have been empty. Record elections at /EnrollmentMatrix?setupId="
                            + setupId + " (and check each plan's 'Enrollment import file' on"
                            + " /SummitPlanTemplateAdmin), then generate the file again.");
            return;
        }

        String filename = resolveFilename(targetFileType,
                (isElections ? "pi-elections-" : "hra-enrollment-") + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, lines, record);
    }

    /**
     * V104 — the confirm-gate screen for {@link #TYPE_CARD_SEED}, shown whenever the request has
     * not yet supplied both a matching {@code confirm} token and a non-past {@code effectiveDate}.
     * Same shape as the plain-text T201 guard above, widened to an HTML form because the effective
     * date must be operator-editable (TA-e's run-date+1 is a default, not a fixed rule — card
     * issuance timing is untestable until SSA issues cards, T245). The form re-submits {@code
     * proposalId}, {@code type} and the (already-satisfied) {@code confirm} token as hidden fields,
     * so only the date itself is ever something the operator changes.
     */
    private void writeCardSeedConfirmPage(HttpServletResponse response, EntityManager em,
                                          Prospect prospect, long proposalId, boolean push,
                                          LocalDate defaultEffectiveDate, String dateError)
            throws IOException {
        List<EmployerParticipant> guardRoster =
                EmployerParticipantDAO.findByProspectId(em, prospect.getId());
        int rosterCount = (guardRoster == null) ? 0 : guardRoster.size();
        String expectedConfirm = "CARDSEED-P" + proposalId;

        StringBuilder body = new StringBuilder();
        body.append("<h3>$1 card-issuer seed election</h3>");
        body.append("<p>This file enrols EVERY participant on the roster for prospect ")
                .append(prospect.getId()).append(" (").append(rosterCount).append(" participant")
                .append(rosterCount == 1 ? "" : "s").append(") into the card-issuer placeholder"
                        + " plan at a flat $1.00 annual election. Files 1 (Employer), 2 (Plans) and"
                        + " 4 (Demographics) must already be processed in Summit, or this file"
                        + " fails or produces nothing meaningful. Summit rejects a participant"
                        + " already enrolled in this plan.</p>");
        if (dateError != null) {
            body.append("<p style=\"color:#b00020;\"><strong>").append(html(dateError)).append("</strong></p>");
        }
        body.append("<form method=\"").append(push ? "post" : "get")
                .append("\" action=\"SummitExport\"").append(push ? " target=\"_blank\"" : "").append(">");
        body.append("<input type=\"hidden\" name=\"proposalId\" value=\"").append(proposalId).append("\">");
        body.append("<input type=\"hidden\" name=\"type\" value=\"").append(TYPE_CARD_SEED).append("\">");
        body.append("<input type=\"hidden\" name=\"confirm\" value=\"").append(html(expectedConfirm)).append("\">");
        body.append("<label>Effective date: <input type=\"date\" name=\"effectiveDate\" value=\"")
                .append(defaultEffectiveDate).append("\" required></label> ");
        body.append("<button type=\"submit\" onclick=\"return confirm('")
                .append(push ? "Push" : "Generate")
                .append(" the $1 card-issuer seed election for every participant on the roster?"
                        + " Files 1, 2 and 4 must already be processed in Summit. Summit rejects a"
                        + " participant already enrolled in this plan. There is no undo.');\">")
                .append(push ? "Push" : "Generate").append("</button>");
        body.append("</form>");
        writeHtml(response, HttpServletResponse.SC_BAD_REQUEST, body.toString());
    }

    /**
     * V104 — the T227-shaped prior-push refusal for {@link #TYPE_CARD_SEED}: content-hash dedupe
     * ({@code pushFile}'s own duplicate check) only catches a re-push on the same calendar day,
     * because column D changes daily by default — and the operator-editable date (above) means it
     * can differ even sooner. This is the proposal-level backstop: any prior successful push at all
     * is a reason to stop and ask, because AMS records nothing per participant and a second run
     * re-emits every roster row. Only the exact prior push's id, supplied as {@code ackPrior},
     * unlocks it. A download (GET) gets the same refusal as plain text, no form — matching the
     * spec's distinction between the two request kinds.
     *
     * @param effectiveDate the already-validated (non-past) date the operator just confirmed —
     *                       carried through into the "Push anyway" form's hidden field rather than
     *                       a freshly computed default, so acknowledging does not silently change
     *                       the date the operator chose.
     */
    private void writeCardSeedPriorPushRefusal(HttpServletResponse response, ExportRecord record,
                                                SummitFileExport priorPush, LocalDate effectiveDate)
            throws IOException {
        if (record.pushDir() == null) {
            writePlainError(response, HttpServletResponse.SC_CONFLICT,
                    "$1 card-issuer seed election not generated: a prior push already exists for"
                            + " this proposal (push #" + priorPush.getId() + ", "
                            + priorPush.getFileName() + ", delivered "
                            + formatPushTimestamp(priorPush.getDeliveredAt()) + "). AMS records"
                            + " nothing per participant, so re-emitting re-attempts every"
                            + " participant, including those Summit already enrolled. To generate"
                            + " anyway, repeat this request with &ackPrior=" + priorPush.getId() + ".");
            return;
        }
        StringBuilder body = new StringBuilder();
        body.append("<h3>Not pushed — a prior seed election exists</h3>");
        body.append("<p>Push #").append(priorPush.getId()).append(", file ")
                .append(html(priorPush.getFileName())).append(", delivered ")
                .append(html(formatPushTimestamp(priorPush.getDeliveredAt())))
                .append(", already pushed the $1 card-issuer seed election for this proposal.</p>");
        body.append("<p>AMS records nothing per participant, so re-emitting re-attempts every"
                + " participant — Summit rejects the already-enrolled ones per row, harmlessly, and"
                + " enrols anyone added to the roster since. Push again only for that reason.</p>");
        body.append("<form method=\"post\" action=\"SummitExport\" target=\"_blank\">");
        body.append("<input type=\"hidden\" name=\"proposalId\" value=\"")
                .append(record.proposalId()).append("\">");
        body.append("<input type=\"hidden\" name=\"type\" value=\"")
                .append(html(record.fileType())).append("\">");
        body.append("<input type=\"hidden\" name=\"confirm\" value=\"CARDSEED-P")
                .append(record.proposalId()).append("\">");
        body.append("<input type=\"hidden\" name=\"effectiveDate\" value=\"")
                .append(effectiveDate).append("\">");
        body.append("<input type=\"hidden\" name=\"ackPrior\" value=\"").append(priorPush.getId()).append("\">");
        body.append("<button type=\"submit\" onclick=\"return confirm('Push anyway? This re-attempts"
                + " every participant on the roster.');\">Push anyway</button>");
        body.append("</form>");
        writeHtml(response, HttpServletResponse.SC_CONFLICT, body.toString());
    }

    /**
     * V104 — the $1 card-issuer seed election, a {@code 125 PI Elections} file (spec
     * {@code docs/analysis/spec_card_issuer_seed_election.md} §2). One row per census participant,
     * each a flat {@code $1.00} annual election against the one {@code summit_plan_template_map}
     * row flagged {@code is_card_issuer} among this sale's elected services.
     * <p>
     * Deliberately not built on {@link #writeHraEnrollment} or {@link #writeEmployerCdhPlan} by
     * extraction — both are import-proven and this method is not; see the same deliberate-duplicate
     * reasoning at {@link #writeHraEnrollment}'s own composition comment.
     * <p>
     * <b>Column layout, eleven fields, always:</b> A Employer TPA Custom ID, B Participant TPA
     * Custom ID, C Import Plan ID, D Effective Date, E Plan Start Date (never — a cross-check, not a
     * selector; a wrong value returns {@code Plan Not Found}), F Coverage End Date (never —
     * termination only), G Participant Annual Election Amount ({@code 1.00}, always), H Per
     * Contribution Amount (never — if both G and H are supplied, H is ignored and the record shows
     * two numbers), I Participant Contribution Schedule (never in this build — omitting it produces
     * the proven expectation-only election: annual recorded, {@code $0.00} posted, {@code $0.00}
     * disbursable), J Employer Contribution Schedule (never — structurally meaningless on
     * {@code Ins125+}), K Filler (mandatory trailing sentinel, constant {@code X}).
     *
     * @param effectiveDate the operator-submitted, already-validated (non-past) effective date —
     *                       never computed here. Column D and nothing else depends on it.
     */
    private void writeCardSeedElection(HttpServletResponse response, EntityManager em, long proposalId,
                                        Prospect prospect, Map<String, String> answers,
                                        String employerTpaCustomId, Long pspId, ExportRecord record,
                                        LocalDate effectiveDate)
            throws IOException {
        LocalDate planYearStart = parseAnswerDate(answers.get(FIELD_PLAN_YEAR_START));
        if (planYearStart == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate $1 card-issuer seed election: prospect " + prospect.getId()
                            + "'s application has no usable answer for '" + FIELD_PLAN_YEAR_START
                            + "'. The Card Issuer plan's own plan year is derived from it and the"
                            + " effective date must fall inside that plan year.");
            return;
        }
        LocalDate planYearEnd = parseAnswerDate(answers.get(FIELD_PLAN_YEAR_END));
        if (planYearEnd == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate $1 card-issuer seed election: prospect " + prospect.getId()
                            + "'s application has no usable answer for '" + FIELD_PLAN_YEAR_END + "'.");
            return;
        }

        // Deliberately duplicated from writeEmployerCdhPlan / writeHraEnrollment rather than
        // extracted into a shared helper -- see the javadoc above.
        List<PlanTemplate> configured;
        try {
            configured = SummitPlanTemplateResolver.configuredOrThrow(em, pspId);
        } catch (SummitPlanTemplateResolver.KeySegmentRejectedException e) {
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate $1 card-issuer seed election: " + e.getMessage()
                            + ". Fix the Summit Plan Templates mapping (SummitPlanTemplateAdmin) or"
                            + " the SUMMIT_PLAN_TEMPLATES property, then retry.");
            return;
        }
        // The legacy SUMMIT_PLAN_TEMPLATES property has no slot for the card-issuer flag and never
        // will (rule 4 -- it is a per-row marker, not a per-installation setting); an empty
        // configured list is therefore always a refusal here, unlike the sibling writers' legacy
        // single-ICHRA fallback.
        if (configured.isEmpty()) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate $1 card-issuer seed election: no Summit Plan Templates mapping"
                            + " rows are configured for your PSP. The legacy SUMMIT_PLAN_TEMPLATES"
                            + " property carries no Card Issuer flag, so this file needs at least"
                            + " one summit_plan_template_map row with Card Issuer checked. Configure"
                            + " it on the Summit Plan Templates admin screen.");
            return;
        }

        Map<Integer, String> electedServices = loadElectedServiceItems(em, proposalId);
        List<PlanTemplate> candidates = new ArrayList<>();
        for (PlanTemplate template : configured) {
            if (electedServices.containsKey(template.getServiceItemId())) {
                candidates.add(template);
            }
        }
        List<PlanTemplate> matches = new ArrayList<>();
        for (PlanTemplate template : candidates) {
            if (template.isCardIssuer()) matches.add(template);
        }
        if (matches.size() != 1) {
            StringBuilder candidateList = new StringBuilder();
            for (PlanTemplate template : candidates) {
                if (candidateList.length() > 0) candidateList.append(", ");
                candidateList.append(template.getKeySegment()).append("=").append(template.getTemplateId())
                        .append(template.isCardIssuer() ? " [Card Issuer]" : "");
            }
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate $1 card-issuer seed election: exactly one plan in prospect "
                            + prospect.getId() + "'s file 2 row-set must be flagged Card Issuer among"
                            + " the elected services. Found " + matches.size() + ". Plans file 2"
                            + " would emit (keySegment=templateId): "
                            + (candidates.isEmpty() ? "(none)" : candidateList.toString())
                            + ". Flag exactly one row Card Issuer on the Summit Plan Templates admin"
                            + " screen, then retry.");
            return;
        }
        PlanTemplate cardIssuer = matches.get(0);

        // The Card Issuer row's own plan year, same call file 2 makes (SummitPlanDateRuleResolver),
        // used here only to bound the operator-submitted effective date -- not to compute one.
        SummitPlanDateRuleResolver.Resolved resolved = SummitPlanDateRuleResolver.resolve(
                planYearStart, planYearEnd, cardIssuer.getEffectiveDateRule(),
                cardIssuer.getOffsetMonths(), cardIssuer.getPlanYearOffsetYears(), LocalDate.now(),
                "plan '" + cardIssuer.getLabel() + "' (ServiceItem " + cardIssuer.getServiceItemId()
                        + ", template " + cardIssuer.getTemplateId() + ", seq " + cardIssuer.getSeq() + ")");
        if (resolved.isRejected()) {
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate $1 card-issuer seed election: " + resolved.getRejection());
            return;
        }
        SummitPlanDateRuleResolver.PlanDates dates = resolved.getDates();
        if (effectiveDate.isBefore(dates.getPlanYearBegin()) || effectiveDate.isAfter(dates.getPlanYearEnd())) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate $1 card-issuer seed election: the effective date "
                            + effectiveDate + " falls outside the Card Issuer plan's own plan year ("
                            + dates.getPlanYearBegin() + " to " + dates.getPlanYearEnd() + "). Summit"
                            + " returns 'Plan Not Found' for an election dated outside the plan it"
                            + " enrols into. Choose a date inside that range.");
            return;
        }

        // S31-J/S50 composition, byte-identical to writeEmployerCdhPlan and writeHraEnrollment's own
        // Import Plan ID: sanitize(employerTpaCustomId + keySegment), strictly alphanumeric, no
        // plan year in the key.
        String importPlanId = sanitize(employerTpaCustomId + cardIssuer.getKeySegment());
        String effectiveDateStr = effectiveDate.format(SUMMIT_DATE);

        List<EmployerParticipant> roster = EmployerParticipantDAO.findByProspectId(em, prospect.getId());
        // ⚠️ T209 shape -- a zero-row file is a defect, not an empty result. Same guard, same
        // position and same refusal as Demographics' and HRA Enrollment's own.
        if (roster.isEmpty()) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate $1 card-issuer seed election: prospect " + prospect.getId()
                            + " has no participants on its roster, so the file would have been"
                            + " empty. An empty file imports as nothing at all, so it is refused"
                            + " rather than downloaded. Upload the census on the Setup screen, then"
                            + " generate the file again.");
            return;
        }

        String prefix = summitTpaIdPrefix();
        List<String> lines = new ArrayList<>();
        for (EmployerParticipant participant : roster) {
            lines.add(String.join("|",
                    employerTpaCustomId,                        // A
                    prefix + "-P-" + participant.getId(),       // B
                    importPlanId,                               // C
                    effectiveDateStr,                           // D
                    "",                                         // E Plan Start Date -- never
                    "",                                         // F Coverage End Date -- never
                    "1.00",                                     // G Annual Election Amount
                    "",                                         // H Per Contribution Amount -- never
                    "",                                         // I Contribution Schedule -- never in this build
                    "",                                         // J Employer Contribution Schedule -- never
                    "X"));                                       // K Filler -- mandatory sentinel
        }

        log.info("[SUMMIT-EXPORT] proposal {} $1 card-issuer seed election: {} participant row(s)"
                        + " into plan {} effective {} at $1.00 each",
                proposalId, lines.size(), importPlanId, effectiveDateStr);

        String filename = resolveFilename(TYPE_CARD_SEED,
                "card-issuer-seed-" + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, lines, record);
    }

    /**
     * The raw configured Summit TPA prefix, trimmed — matching
     * {@link #resolveEmployerTpaCustomId(Prospect)}. Safe to call unvalidated wherever
     * {@code employerTpaCustomId} was already successfully resolved in the same request, since
     * that could not have happened unless this same config key held a valid value.
     */
    private static String summitTpaIdPrefix() {
        return AppConfig.get("SUMMIT_TPA_ID_PREFIX").trim();
    }

    /**
     * S29-I — the value emitted in Demographics column L ({@code Branch Code}), read the same way
     * this file already reads {@code SUMMIT_TPA_ID_PREFIX} and
     * {@code SUMMIT_ICHRA_PLAN_TEMPLATE_ID}: {@code AppConfig} at request time, never hardcoded,
     * because the Summit-side template is per-installation configuration.
     * <p>
     * ⚠️ <b>Absence is not a reason to omit the column.</b> It is mandatory in the template and its
     * whole purpose is to be non-empty, so an unset or blank key falls back to
     * {@link #DEFAULT_BRANCH_CODE} rather than producing the trailing-empty-field case the column
     * exists to prevent. There is deliberately no branch that emits nothing here.
     */
    private static String summitBranchCode() {
        String raw = AppConfig.get("SUMMIT_BRANCH_CODE");
        return (raw == null || raw.isBlank()) ? DEFAULT_BRANCH_CODE : raw.trim();
    }

    /**
     * Parses a DATE-typed application answer (an HTML {@code <input type="date">} submits ISO
     * {@code yyyy-MM-dd}). Returns null if the value is absent, blank, or unparseable — callers
     * refuse to emit rather than guess.
     */
    private static LocalDate parseAnswerDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * S30-A — parses the {@code hra_annual_ee} application answer into the
     * {@code Participant Annual Election Amount} column.
     * <p>
     * That field is {@code TEXT} on the application form, so what arrives is whatever a human typed.
     * <b>Accepted: an optional leading {@code $}, then digits, then at most two decimal places.</b>
     * Everything else returns null and the caller refuses to emit.
     * <p>
     * ⚠️ <b>A thousands separator is refused, not stripped.</b> Stripping commas reads {@code 7,200}
     * correctly and {@code 7.200,00} as seven-point-two — and Summit accepts a wrong amount
     * silently, funding the benefit from it, so there is no later stage at which such a misread
     * would surface. A refusal costs one corrected answer on the application; a misread costs a
     * wrongly funded benefit. Only a leading {@code $} is tolerated, because it cannot change the
     * numeric value.
     * <p>
     * Non-positive returns null for the same reason {@link #parsePositiveInt} rejects it: an
     * enrollment of {@code 0.00} enrols a participant into a benefit funded with nothing, which is
     * far more likely a placeholder answer than an intent.
     */
    private static BigDecimal parseAnnualElectionAmount(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.startsWith("$")) value = value.substring(1).trim();
        if (!value.matches("\\d{1,13}(\\.\\d{1,2})?")) return null;
        BigDecimal amount = new BigDecimal(value);
        return amount.signum() > 0 ? amount : null;
    }

    /**
     * S29-D — the emitted download filename for one export file.
     * <p>
     * Summit binds a retrieved file to an import template <b>by filename prefix</b>, so when this
     * installation has named its templates in {@code SUMMIT_IMPORT_TEMPLATES} the file must be
     * called {@code {templateName}_{yyyyMMddHHmmss}.txt} and nothing else. The timestamp keeps
     * successive downloads of the same file distinct.
     * <p>
     * ⚠️ <b>When the key holds no entry for this file, {@code legacyFilename} is returned
     * unchanged</b> — not adjusted, not normalised, not "improved". An installation that takes
     * this commit without editing {@code ssa.properties} keeps the descriptive filenames it had
     * before, byte for byte. That is the same legacy-path protection
     * {@link SummitImportTemplateResolver} and {@code SummitPlanTemplateResolver} both carry, and
     * it is why the caller builds the legacy name eagerly rather than behind a branch.
     *
     * @param type            the {@code type} discriminator this file was requested under
     * @param legacyFilename  the descriptive filename emitted before this method existed
     */
    private static String resolveFilename(String type, String legacyFilename) {
        return SummitImportTemplateResolver.templateNameFor(type)
                .map(name -> name + "_" + LocalDateTime.now().format(SUMMIT_FILE_STAMP) + ".txt")
                .orElse(legacyFilename);
    }

    private void writeFile(HttpServletResponse response, String filename, String line,
                           ExportRecord record) throws IOException {
        writeFile(response, filename, java.util.Collections.singletonList(line), record);
    }

    /**
     * File 2 (Employer CDH Plan) is one file with one row per plan — this overload is the
     * multi-row sink that lets a single response carry more than one plan's row. It is also the
     * <b>single point at which any Summit export file's bytes reach the response</b>: all four
     * types funnel here, the single-row overload above included.
     * <p>
     * ⚠️ <b>S32-A — the emitted bytes are unchanged by the recording.</b> The per-line
     * {@code print(line); print(newline)} loop this method used is replaced by one
     * {@code StringBuilder} appending exactly the same line-then-newline sequence, printed once.
     * Concatenation is associative, so the character sequence written to the response is
     * identical; it is now materialised in a variable so the record can be taken <b>from the very
     * bytes the response received</b> rather than from a regeneration that could differ. There is
     * one {@code content} String and it is both printed and recorded.
     * <p>
     * ⚠️ <b>A recording failure must not fail the export.</b> The response is written and flushed
     * <i>before</i> {@link #recordExport} is called, and that method swallows and logs rather than
     * throwing. The reverse choice — failing the download when the record cannot be written — was
     * considered and <b>rejected</b>: someone waiting on a file they need in order to load an
     * employer into Summit should not be blocked by a bookkeeping failure, and the record's two
     * consumers (response-file matching and the T194 same-hash warning) degrade to "no information
     * about this export" rather than to a wrong answer. The ERROR log names the file, so a missing
     * record is diagnosable after the fact.
     */
    private void writeFile(HttpServletResponse response, String filename, List<String> lines,
                           ExportRecord record) throws IOException {
        StringBuilder body = new StringBuilder();
        if (lines != null) {
            for (String line : lines) {
                body.append(line);
                body.append("\n");
            }
        }
        String content = body.toString();

        // S42-B -- push mode diverts here, before any response header or write. pushFile owns
        // the entire response from this point; the download path below never runs for a push.
        if (record.pushDir() != null) { pushFile(response, content, lines == null ? 0 : lines.size(), record); return; }

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        PrintWriter out = response.getWriter();
        out.print(content);
        out.flush();

        recordExport(record, filename, content, lines == null ? 0 : lines.size());
    }

    /**
     * S32-A — the identity of one export, assembled in {@code doGet} and carried to the single
     * write path. A carrier, not a behaviour: it holds no logic and decides nothing.
     * <p>
     * The {@code EntityManager} is the request's own, still open — {@code doGet}'s {@code finally}
     * closes it after the writer returns, and the recording runs inside that window. Every field
     * but {@code fileType} may legitimately be null; see {@link SummitFileExport} for why
     * nullability is the correct shape for an audit row.
     * <p>
     * S42-B — {@code pushDir} and {@code ackDuplicate} are added for T229. {@code pushDir} is
     * null for a download and the resolved {@code ImportFiles} directory for a push; that is the
     * single flag {@code writeFile} and {@code recordExport} branch on. {@code ackDuplicate} is
     * the id of a prior pushed row the caller has acknowledged is a byte-identical duplicate; null
     * unless the caller supplied one.
     */
    private record ExportRecord(EntityManager em, String fileType, Long pspId, Long proposalId,
                                Long prospectId, String userName, String pushDir, Long ackDuplicate) {}

    /**
     * S32-A — writes one {@code summit_file_export} row for a file that has already been sent.
     * <p>
     * ⚠️ <b>Never throws.</b> Every failure path logs at ERROR naming the file and the reason and
     * returns; see the invariant on {@link #writeFile}. The file has already reached the client by
     * the time this runs, so there is nothing left to fail.
     * <p>
     * <b>Every generated file is recorded, zero-row files included.</b> S31-J's empty-file guard
     * covers {@code cdhplan} only — {@code demographics} and {@code enrollment} iterate the
     * participant roster with no empty guard, so a prospect with an empty roster still produces a
     * zero-row file that reaches this method. That send is exactly the kind worth a record, so it
     * is written rather than skipped.
     * <p>
     * S42-B — <b>for a push ({@code record.pushDir() != null}), this runs before transport, not
     * after</b>: {@code pushFile} calls this to insert the row first, and only uploads once the
     * row exists (decision 5 — no byte reaches {@code ImportFiles} unless its row was inserted
     * first). Push rows are additionally stamped {@code PUSHING}/{@code deliveredAt}/
     * {@code deliveryDir} here, at insert time. The return value lets {@code pushFile} recover the
     * inserted row's id; the download call site ignores it.
     *
     * @return the inserted {@link SummitFileExport}, or {@code null} on failure or a null record.
     */
    private SummitFileExport recordExport(ExportRecord record, String filename, String content, int rowCount) {
        if (record == null) return null;
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            SummitFileExport row = new SummitFileExport();
            row.setPspId(record.pspId());
            row.setFileType(record.fileType());
            row.setProposalId(record.proposalId());
            row.setProspectId(record.prospectId());
            row.setFileName(filename);
            row.setGeneratedAt(LocalDateTime.now());
            row.setGeneratedBy(record.userName());
            row.setRowCount(rowCount);
            row.setByteCount(bytes.length);
            row.setContentSha256(sha256Hex(bytes));
            row.setContent(content);
            if (record.pushDir() != null) {
                row.setDeliveryStatus("PUSHING");
                row.setDeliveredAt(LocalDateTime.now());
                row.setDeliveryDir(record.pushDir());
            }
            SummitFileExportDAO.insert(record.em(), row);
            return row;
        } catch (Exception e) {
            log.error("[SUMMIT-EXPORT] file '{}' was delivered but could NOT be recorded in"
                    + " summit_file_export: {}. The file is correct and was sent; only the record"
                    + " is missing, so response-file matching and the T194 same-hash warning have"
                    + " no entry for this export.", filename, e.getMessage(), e);
            return null;
        }
    }

    /** Lowercase hex SHA-256 — the T194 dedupe key. SHA-256 is mandatory on every JVM. */
    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
    }

    /**
     * S42-B — T229: pushes {@code content} to Summit's {@code ImportFiles} directory over SFTP,
     * after recording it in {@code summit_file_export}. Reached only from {@link #writeFile} when
     * {@code record.pushDir() != null}; owns the entire HTTP response from that point on — the
     * download path in {@code writeFile} never runs for a push.
     * <p>
     * Order is load-bearing (S42-B decision 5): the row is inserted <b>before</b> the upload is
     * attempted, so no byte ever reaches {@code ImportFiles} without a record of it existing
     * first. The upload itself is unconditional {@code OVERWRITE} (see
     * {@link SummitSftpService#upload}) — the uniqueness that keeps two pushes from colliding is
     * the filename built in step 1 below, not the transport.
     */
    private void pushFile(HttpServletResponse response, String content, int rowCount, ExportRecord record)
            throws IOException {
        // Step 1 -- push filename. {template}_{yyyyMMddHHmmss}.txt -- the SAME shape and the
        // SAME formatter (SUMMIT_FILE_STAMP) downloads already use. Summit binds by
        // template-name prefix, and TWO shapes are observed to match the ZZ_TEST_ER template
        // over SFTP: this one (push #8, processed 2026-09-10) and `{template}_P{id}_{17-digit}`
        // (push #5, held as a content duplicate, not rejected). The earlier apparent non-pickup
        // of #5 was caused by Summit's Schedule Import checkbox being disabled (SDX-22), not by
        // its filename -- see S42's close-out for the correction. Uniqueness comes from the
        // pushed-filename refusal in step 2 below, not from the name. doGet's push checks already
        // refused the request if no template is configured for this type, so .get() here can
        // never throw.
        String pushFilename = SummitImportTemplateResolver.templateNameFor(record.fileType()).get()
                + "_" + LocalDateTime.now().format(SUMMIT_FILE_STAMP) + ".txt";

        // Step 2 -- filename collision refusal (S42-D). Same shape as a download's filename now
        // means two pushes within the same second for the same type CAN collide, and Summit
        // rejects a repeated filename outright (SDX-17) -- so this, not the name itself, is what
        // keeps a push unique. No ack option: unlike the content duplicate below, there is no
        // legitimate reason to push the identical name again, only a reason to wait a second and
        // retry. Nothing is recorded or uploaded when this refuses.
        if (SummitFileExportDAO.existsPushedFileName(record.em(), pushFilename)) {
            log.info("[SUMMIT-EXPORT] push refused for proposal {} type {}: filename {} already"
                    + " pushed", record.proposalId(), record.fileType(), pushFilename);
            writeHtml(response, HttpServletResponse.SC_CONFLICT,
                    "<h3>Not pushed</h3><p>A file named " + html(pushFilename) + " has already"
                            + " been pushed. Summit rejects a repeated filename. Wait one second"
                            + " and push again.</p>");
            return;
        }

        // Step 3 -- hash. The same UTF-8 bytes the upload in step 6 sends, so the duplicate check
        // below compares exactly what would be sent, not a re-derived approximation.
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String sha = sha256Hex(bytes);

        // Step 4 -- duplicate-content check. findByContentHash already scopes by fileType; the
        // pspId filter narrows further to this PSP's own history. The query orders newest-first,
        // so the first PUSHED/PUSHING match found is also the most recent one.
        List<SummitFileExport> matches =
                SummitFileExportDAO.findByContentHash(record.em(), sha, record.fileType());
        SummitFileExport pushedMatch = null;
        SummitFileExport downloadMatch = null;
        for (SummitFileExport candidate : matches) {
            if (!Objects.equals(candidate.getPspId(), record.pspId())) continue;
            String status = candidate.getDeliveryStatus();
            if (pushedMatch == null && ("PUSHED".equals(status) || "PUSHING".equals(status))) {
                pushedMatch = candidate;
            }
            if (downloadMatch == null && status == null) {
                downloadMatch = candidate;
            }
        }

        if (pushedMatch != null && !Objects.equals(pushedMatch.getId(), record.ackDuplicate())) {
            log.info("[SUMMIT-EXPORT] push refused for proposal {} type {}: content matches prior"
                    + " push #{}, not acknowledged", record.proposalId(), record.fileType(),
                    pushedMatch.getId());
            writeDuplicateWarningHtml(response, record, pushedMatch);
            return;
        }

        // Step 5 -- record. No byte reaches ImportFiles unless this succeeds.
        SummitFileExport row = recordExport(record, pushFilename, content, rowCount);
        if (row == null || row.getId() == null) {
            log.error("[SUMMIT-EXPORT] push for proposal {} type {} NOT sent: AMS could not"
                    + " record the file", record.proposalId(), record.fileType());
            writeHtml(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "<h3>Not pushed</h3><p>AMS could not record the file, so it was not sent.</p>");
            return;
        }

        // Step 6 -- upload. Constructed the way SummitSftpTestServlet constructs it.
        SummitSftpService service = new SummitSftpService();
        try {
            service.upload(record.pushDir(), pushFilename, content.getBytes(StandardCharsets.UTF_8));
        } catch (SftpTransportException e) {
            try {
                SummitFileExportDAO.updateDelivery(record.em(), row.getId(), "PUSH_FAILED", e.getMessage());
            } catch (Exception updateFailure) {
                log.error("[SUMMIT-EXPORT] push #{} FAILED and its PUSH_FAILED status could not be"
                        + " recorded: {}", row.getId(), updateFailure.getMessage(), updateFailure);
            }
            log.error("[SUMMIT-EXPORT] push #{} ({}) for proposal {} type {} FAILED: {}",
                    row.getId(), pushFilename, record.proposalId(), record.fileType(), e.getMessage());
            writeHtml(response, HttpServletResponse.SC_BAD_GATEWAY,
                    "<h3>Not pushed</h3><p>The upload to DataPath failed: " + html(e.getMessage())
                            + "</p><p>Record #" + row.getId() + ", filename " + html(pushFilename)
                            + ".</p>");
            return;
        }

        // Step 7 -- mark pushed.
        String pushingNote = "";
        try {
            SummitFileExportDAO.updateDelivery(record.em(), row.getId(), "PUSHED", null);
        } catch (Exception e) {
            log.error("[SUMMIT-EXPORT] push #{} succeeded but could not be marked PUSHED: {}",
                    row.getId(), e.getMessage(), e);
            pushingNote = "<p><strong>The file was sent, but its row is still marked PUSHING.</strong></p>";
        }

        log.info("[SUMMIT-EXPORT] push #{} ({}) for proposal {} type {} succeeded, {} row(s), dir {}",
                row.getId(), pushFilename, record.proposalId(), record.fileType(), rowCount, record.pushDir());

        // Step 8 -- success page.
        StringBuilder body = new StringBuilder();
        body.append("<h3>Pushed ").append(html(pushFilename)).append(" to DataPath.</h3>");
        body.append("<p>Record #").append(row.getId()).append(", delivered ")
                .append(html(formatPushTimestamp(row.getDeliveredAt()))).append(" to ")
                .append(html(record.pushDir())).append(".</p>");
        body.append("<p>Summit polls ImportFiles about every 15 minutes and processes the file"
                + " automatically. Its response will be named Response_").append(html(pushFilename))
                .append(" in ResponseFiles. If no response appears, check Summit's File History"
                + " — a file held as a duplicate produces no response.</p>");
        if (downloadMatch != null) {
            body.append("<p>Note: this content also matches download #").append(downloadMatch.getId())
                    .append(" (").append(html(downloadMatch.getFileName())).append("). If that"
                    + " downloaded file was uploaded to Summit by hand, this push will be held"
                    + " pending TPA approval.</p>");
        }
        body.append(pushingNote);
        writeHtml(response, HttpServletResponse.SC_OK, body.toString());
    }

    /**
     * S42-B — the T227/SDX-17 duplicate refusal (HTTP 409): content byte-identical to a prior
     * push that has not been acknowledged for this specific request. Nothing is recorded or
     * uploaded when this runs.
     * <p>
     * The form's action is a bare relative path, not an absolute one built from
     * {@code request.getContextPath()} — {@code pushFile}'s mandated signature carries only
     * {@code ExportRecord}, not the request. This page is rendered as the direct response to a
     * POST to {@code {contextPath}/SummitExport}, so the browser's document base URI already
     * carries the context path; a relative {@code action="SummitExport"} resolves against that
     * base to the same URL an absolute one built from {@code getContextPath()} would.
     */
    private void writeDuplicateWarningHtml(HttpServletResponse response, ExportRecord record,
                                            SummitFileExport pushedMatch) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append("<h3>Not pushed — duplicate content</h3>");
        body.append("<p>This content is byte-identical to push #").append(pushedMatch.getId())
                .append(", file ").append(html(pushedMatch.getFileName())).append(", delivered ")
                .append(html(formatPushTimestamp(pushedMatch.getDeliveredAt()))).append(".</p>");
        body.append("<p>Summit will hold a repeat of this content pending TPA approval in File"
                + " History, rather than processing it.</p>");
        body.append("<form method=\"post\" action=\"SummitExport\" target=\"_blank\">");
        body.append("<input type=\"hidden\" name=\"proposalId\" value=\"")
                .append(record.proposalId()).append("\">");
        body.append("<input type=\"hidden\" name=\"type\" value=\"")
                .append(html(record.fileType())).append("\">");
        body.append("<input type=\"hidden\" name=\"ackDuplicate\" value=\"")
                .append(pushedMatch.getId()).append("\">");
        body.append("<button type=\"submit\" onclick=\"return confirm('Push anyway? Summit will"
                + " hold this as a duplicate pending TPA approval.');\">Push anyway</button>");
        body.append("</form>");
        writeHtml(response, HttpServletResponse.SC_CONFLICT, body.toString());
    }

    /**
     * S42-B — writes a minimal, self-contained HTML push-outcome response. No includes; no
     * {@code AppConfig} value appears in any body passed here except {@code record.pushDir()},
     * which callers above include only on the success page.
     */
    private void writeHtml(HttpServletResponse response, int status, String bodyHtml) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>"
                + bodyHtml + "</body></html>");
    }

    /** Escapes {@code & < > " '} for safe inclusion in the push HTML responses above. */
    private static String html(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** S42-D — timestamp format for a push page's rendered delivery times. */
    private static final DateTimeFormatter PUSH_PAGE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Formats {@code timestamp} for push-page HTML, or {@code ""} when null. */
    private static String formatPushTimestamp(LocalDateTime timestamp) {
        return timestamp == null ? "" : timestamp.format(PUSH_PAGE_TIMESTAMP);
    }

    /**
     * Pipe is the file delimiter, so no emitted value may carry one — and none may carry an
     * embedded newline or carriage return either, since that would silently fabricate an
     * extra record. One shared helper for every free-text field rather than a per-field rule.
     */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("|", "").replace("\r", "").replace("\n", "");
    }

    private static String sanitizeFilename(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }

    /** S55-B — null/blank-safe trim, following {@code EnrollmentMatrixServlet}'s own helper of the same name. */
    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Returns the label of the first null-or-blank value in the label/value pairs, or null if none. */
    private static String firstBlank(String... labelsAndValues) {
        for (int i = 0; i < labelsAndValues.length; i += 2) {
            String label = labelsAndValues[i];
            String value = labelsAndValues[i + 1];
            if (value == null || value.isBlank()) {
                return label;
            }
        }
        return null;
    }
}

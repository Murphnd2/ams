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
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.SummitImportTemplateResolver;
import net.superiorstate.ams.data.resolver.SummitPlanTemplateResolver;
import net.superiorstate.ams.data.resolver.SummitPlanTemplateResolver.PlanTemplate;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.ApplicationFieldValue;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // S29-D -- the three values the `type` request parameter may take. Named so the request
    // contract, the dispatch and the SUMMIT_IMPORT_TEMPLATES lookup key can never drift apart.
    private static final String TYPE_EMPLOYER = "employer";
    private static final String TYPE_CDH_PLAN = "cdhplan";
    private static final String TYPE_DEMOGRAPHICS = "demographics";

    // Application-answer field keys this export reads (S25-B). Defined in both
    // DatabaseInitializer's baseline sections and the package JSONs under
    // src/main/resources/packages/ -- see docs/analysis/legal_assumptions.md LA-31.
    private static final String FIELD_ADDRESS_STREET1 = "address_street1";
    private static final String FIELD_ADDRESS_CITY = "address_city";
    private static final String FIELD_ADDRESS_STATE = "address_state";
    private static final String FIELD_ADDRESS_ZIP = "address_zip";
    // plan_year_start / plan_year_end ship in the s125_fsa package's LOS-scoped
    // plan_year_eligibility section, so they are only present when that section is attached
    // to the LOS being sold -- a refusal below is expected behaviour on an installation where
    // it is not, not a defect.
    private static final String FIELD_PLAN_YEAR_START = "plan_year_start";
    private static final String FIELD_PLAN_YEAR_END = "plan_year_end";

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
                        || type.equals(TYPE_DEMOGRAPHICS))) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "proposalId and type (employer|cdhplan|demographics) are required.");
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

            if (type.equals(TYPE_EMPLOYER)) {
                writeEmployerDemographic(response, prospect, answers, employerTpaCustomId);
            } else if (type.equals(TYPE_CDH_PLAN)) {
                writeEmployerCdhPlan(response, em, proposalId, prospect, answers, employerTpaCustomId);
            } else {
                writeDemographics(response, em, prospect, employerTpaCustomId);
            }
        } finally {
            if (em.isOpen()) em.close();
        }
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
     * The prefix check is widened to match the field it feeds: a prefix carrying a hyphen or
     * underscore ({@code SSA-158}) would compose an ID Summit refuses, and that failure would
     * surface in a results file rather than at configuration time. Rejecting every
     * non-alphanumeric subsumes the pipe and whitespace checks it replaces.
     */
    private static String resolveEmployerTpaCustomId(Prospect prospect) {
        String rawPrefix = AppConfig.get("SUMMIT_TPA_ID_PREFIX");
        if (rawPrefix == null) return null;
        String prefix = rawPrefix.trim();
        if (prefix.isEmpty()) return null;
        if (!prefix.matches("[A-Za-z0-9]+")) return null;
        return prefix + "E" + prospect.getId();
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
     * Employer Demographic — six columns, {@code Employer TPA Custom ID} = the configured
     * installation prefix plus {@code Prospect.id} (S25-C, LA-29). The upsert key must never
     * change for a given employer, so it is built from the AMS-owned, immutable primary key
     * plus a config-driven prefix rather than any Summit- or user-editable value.
     * <p>
     * Employer name stays {@code Prospect.name} — always populated — rather than the
     * {@code company_legal_name} answer, which may be blank. A fallback chain between the two
     * would reintroduce the silent-variance failure this whole change exists to remove.
     */
    private void writeEmployerDemographic(HttpServletResponse response, Prospect prospect,
                                           Map<String, String> answers, String employerTpaCustomId)
            throws IOException {
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

        String line = String.join("|",
                sanitize(prospect.getName()),
                employerTpaCustomId,
                sanitize(address1),
                sanitize(city),
                sanitize(state),
                sanitize(zip));

        String filename = resolveFilename(TYPE_EMPLOYER,
                "employer-demographic-" + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, line);
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
                                       String employerTpaCustomId)
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

        List<PlanTemplate> configured = SummitPlanTemplateResolver.configured();
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
            List<String> matchedTemplateIds = new ArrayList<>();
            for (PlanTemplate template : emit) {
                matchedTemplateIds.add(template.getServiceItemId() + "=" + template.getTemplateId());
            }
            log.info("[SUMMIT-EXPORT] proposal {} elected ServiceItem ids {} matched plan templates"
                            + " (serviceItemId=templateId) {}",
                    proposalId, electedServices.keySet(), matchedTemplateIds);
        }

        String planYearBegin = planYearStart.format(SUMMIT_DATE);
        String planYearEndStr = planYearEnd.format(SUMMIT_DATE);
        int planYear = planYearStart.getYear();

        List<String> lines = new ArrayList<>();
        for (PlanTemplate template : emit) {
            lines.add(buildCdhPlanRow(template, employerTpaCustomId, prospect.getName(),
                    planYear, planYearBegin, planYearEndStr));
        }

        String filename = resolveFilename(TYPE_CDH_PLAN,
                "employer-cdh-plan-" + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, lines);
    }

    /**
     * One Employer CDH Plan row — the eight columns in spec order
     * (docs/business/summit_data_exchange.md §2). Field order and {@code sanitize()} usage are
     * unchanged from the pre-S28-B single-row builder; the only difference is that the template
     * id, plan name, import plan id and description now come from the configured
     * {@link PlanTemplate} instead of being hardcoded to ICHRA.
     * <p>
     * {@code Effective Date} (column 5) and {@code Plan Year Begin} (column 7) are deliberately
     * the same value, as they were before — the plan takes effect when its plan year opens.
     */
    private String buildCdhPlanRow(PlanTemplate t, String employerTpaCustomId, String prospectName,
                                   int planYear, String planYearBegin, String planYearEnd) {
        return String.join("|",
                String.valueOf(t.getTemplateId()),
                sanitize(t.getLabel() + " " + planYear),
                sanitize(employerTpaCustomId + "-" + t.getKeySegment() + "-" + planYear),
                sanitize(t.getLabel() + " Plan " + planYear + " for " + prospectName),
                planYearBegin,
                employerTpaCustomId,
                planYearBegin,
                planYearEnd);
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
     * (LA-33). An empty roster emits a zero-row file rather than refusing.
     */
    private void writeDemographics(HttpServletResponse response, EntityManager em,
                                    Prospect prospect, String employerTpaCustomId)
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

        String filename = resolveFilename(TYPE_DEMOGRAPHICS,
                "demographics-" + sanitizeFilename(prospect.getName())
                        + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt");
        writeFile(response, filename, lines);
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

    private void writeFile(HttpServletResponse response, String filename, String line) throws IOException {
        writeFile(response, filename, java.util.Collections.singletonList(line));
    }

    /**
     * File 2 (Employer CDH Plan) is one file with one row per plan — this overload is the
     * multi-row sink that lets a single response carry more than one plan's row.
     */
    private void writeFile(HttpServletResponse response, String filename, List<String> lines) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        PrintWriter out = response.getWriter();
        if (lines != null) {
            for (String line : lines) {
                out.print(line);
                out.print("\n");
            }
        }
        out.flush();
    }

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
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

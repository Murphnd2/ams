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
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.ApplicationFieldValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
                || type == null || !(type.equals("employer") || type.equals("cdhplan")
                        || type.equals("demographics"))) {
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
                                + " not contain a pipe or any whitespace.");
                return;
            }

            if (type.equals("employer")) {
                writeEmployerDemographic(response, prospect, answers, employerTpaCustomId);
            } else if (type.equals("cdhplan")) {
                writeEmployerCdhPlan(response, prospect, answers, employerTpaCustomId);
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
     * Returns null if the prefix is absent, blank, or contains a pipe or whitespace; the caller
     * refuses to emit rather than fall back to a bare id, which would create a duplicate
     * employer in Summit under a different key.
     */
    private static String resolveEmployerTpaCustomId(Prospect prospect) {
        String rawPrefix = AppConfig.get("SUMMIT_TPA_ID_PREFIX");
        if (rawPrefix == null) return null;
        String prefix = rawPrefix.trim();
        if (prefix.isEmpty()) return null;
        if (prefix.contains("|") || prefix.chars().anyMatch(Character::isWhitespace)) return null;
        return prefix + "-" + prospect.getId();
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

        String filename = "employer-demographic-" + sanitizeFilename(prospect.getName())
                + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt";
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
     */
    private void writeEmployerCdhPlan(HttpServletResponse response, Prospect prospect,
                                       Map<String, String> answers, String employerTpaCustomId)
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

        Integer templateId = parsePositiveInt(AppConfig.get("SUMMIT_ICHRA_PLAN_TEMPLATE_ID"));
        if (templateId == null) {
            log.error("[SUMMIT-EXPORT] SUMMIT_ICHRA_PLAN_TEMPLATE_ID is absent or non-numeric on this installation");
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate Employer CDH Plan file: this installation has no valid"
                            + " SUMMIT_ICHRA_PLAN_TEMPLATE_ID configured. Set it in ssa.properties"
                            + " to the Summit-assigned Plan Template ID before generating this file.");
            return;
        }

        String planYearBegin = planYearStart.format(SUMMIT_DATE);
        String planYearEndStr = planYearEnd.format(SUMMIT_DATE);
        int planYear = planYearStart.getYear();
        String importPlanId = employerTpaCustomId + "-ICHRA-" + planYear;

        String line = String.join("|",
                String.valueOf(templateId),
                sanitize("ICHRA " + planYear),
                sanitize(importPlanId),
                sanitize("ICHRA Plan " + planYear + " for " + prospect.getName()),
                planYearBegin,
                employerTpaCustomId,
                planYearBegin,
                planYearEndStr);

        String filename = "employer-cdh-plan-" + sanitizeFilename(prospect.getName())
                + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt";
        List<String> lines = java.util.Collections.singletonList(line);
        writeFile(response, filename, lines);
    }

    /**
     * Demographics (file 4) — eleven columns, one row per participant on this prospect's roster
     * ({@code employer_participant}, V094). {@code Mailing Address Line 2} and {@code Email} are
     * nullable on the roster and emit empty rather than being dropped, reordered, or defaulted —
     * a Summit importer defect mishandles a trailing empty value, so both nullable columns sit
     * ahead of the always-populated {@code Effective Date}
     * (docs/business/summit_data_exchange.md). {@code Effective Date} comes from
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
        List<String> lines = new java.util.ArrayList<>();
        for (EmployerParticipant participant : roster) {
            String participantTpaCustomId = prefix + "-P-" + participant.getId();
            lines.add(String.join("|",
                    employerTpaCustomId,
                    participantTpaCustomId,
                    sanitize(participant.getFirstName()),
                    sanitize(participant.getLastName()),
                    sanitize(participant.getAddressLine1()),
                    sanitize(participant.getAddressLine2()),
                    sanitize(participant.getCity()),
                    sanitize(participant.getState()),
                    sanitize(participant.getPostalCode()),
                    sanitize(participant.getEmail()),
                    participant.getEffectiveDate().format(SUMMIT_DATE)));
        }

        String filename = "demographics-" + sanitizeFilename(prospect.getName())
                + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt";
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

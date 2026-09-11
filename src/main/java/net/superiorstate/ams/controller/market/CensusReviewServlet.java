package net.superiorstate.ams.controller.market;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.service.CensusIntakeService;
import net.superiorstate.ams.data.service.CensusParseService;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.market.CensusSubmission;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * S47-F — the PSP review page for a client's staged census upload (T231 build 2, D45 e). Reachable
 * only from the Summit setup panel's step-3 status line (see {@code CensusRequestStatusServlet}).
 * <p>
 * <b>Gates mirror {@code CensusUploadServlet} exactly</b> — session {@code isPspAdmin}, then
 * {@code IchraAccessResolver.isAvailable} — and resolution is the same
 * {@code proposalId → Proposal → Prospect} chain, copied verbatim.
 * <p>
 * <b>Load and Reject are thin wrappers over {@link CensusIntakeService#load}/{@code reject}</b> —
 * this servlet resolves request-derived values (the reviewer, the acting PSP id, the effective
 * date, the confirmation checkbox) and reads {@code mapping_json}/{@code rows_json} back for
 * display, the same way {@code CensusDropServlet} reads them for its own page. PII shown here
 * (names, addresses) is visible to PSP admin only, the same audience as Census Upload's roster
 * table.
 */
@WebServlet(name = "CensusReviewServlet", value = "/CensusReview")
public class CensusReviewServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(CensusReviewServlet.class);

    private static final String VIEW = "/WEB-INF/view/market/censusReview25.jsp";
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Resolved resolved = resolve(request, response, em);
            if (resolved == null) return;

            render(request, response, em, resolved, null);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        long proposalId;
        try {
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Resolved resolved = resolve(request, response, em);
            if (resolved == null) return;
            proposalId = resolved.proposalId;

            String action = request.getParameter("action");
            if ("load".equals(action)) {
                String loadError = handleLoad(request, em, resolved);
                if (loadError != null) {
                    render(request, response, em, resolved, loadError);
                    return;
                }
            } else if ("reject".equals(action)) {
                handleReject(request, em, resolved);
            } else {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "action (load|reject) is required.");
                return;
            }
        } finally {
            if (em.isOpen()) em.close();
        }

        redirectAfterPost(request, response, proposalId);
    }

    // ── Gating and resolution (copied from CensusUploadServlet) ─────────

    private static boolean isPspAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
    }

    private static class Resolved {
        final long proposalId;
        final Prospect prospect;
        Resolved(long proposalId, Prospect prospect) {
            this.proposalId = proposalId;
            this.prospect = prospect;
        }
    }

    private Resolved resolve(HttpServletRequest request, HttpServletResponse response,
                             EntityManager em) throws IOException {
        String proposalIdParam = request.getParameter("proposalId");
        if (proposalIdParam == null || proposalIdParam.isBlank()) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "proposalId is required.");
            return null;
        }
        long proposalId;
        try {
            proposalId = Long.parseLong(proposalIdParam.trim());
        } catch (NumberFormatException e) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid proposalId.");
            return null;
        }

        Proposal proposal = em.find(Proposal.class, proposalId);
        if (proposal == null) {
            writePlainError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Proposal " + proposalId + " not found.");
            return null;
        }
        Prospect prospect = proposal.getProspect();
        if (prospect == null || prospect.getId() == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Proposal " + proposalId + " has no employer (prospect) to review a census"
                            + " against.");
            return null;
        }
        return new Resolved(proposalId, prospect);
    }

    // ── POST handlers ────────────────────────────────────────────────────

    /** @return null on success (caller redirects); an error message to re-render with otherwise. */
    private String handleLoad(HttpServletRequest request, EntityManager em, Resolved resolved) {
        Long submissionId = parseLongOrNull(request.getParameter("submissionId"));
        if (submissionId == null) {
            return "That upload is no longer available for review.";
        }

        LocalDate effectiveDate = null;
        String effectiveDateParam = request.getParameter("effectiveDate");
        if (effectiveDateParam != null && !effectiveDateParam.isBlank()) {
            try {
                effectiveDate = LocalDate.parse(effectiveDateParam.trim());
            } catch (DateTimeParseException e) {
                effectiveDate = null;
            }
        }
        if (effectiveDate == null) {
            return "An effective date is required, and applies to every participant in the file.";
        }

        boolean replaceConfirmed = "yes".equals(request.getParameter("replaceConfirmed"));
        Person reviewer = currentPerson(request);
        String createdByName = resolveCreatedBy(request);
        Long pspId = resolveCurrentPspId(request);

        CensusIntakeService.LoadResult result = CensusIntakeService.load(em, resolved.proposalId,
                submissionId, effectiveDate, replaceConfirmed, reviewer, createdByName, pspId);
        if (!result.isOk()) {
            return result.getMessage();
        }
        log.info("[CENSUS-REVIEW] Loaded {} participants for proposal {}",
                result.getLoadedCount(), resolved.proposalId);
        return null;
    }

    private void handleReject(HttpServletRequest request, EntityManager em, Resolved resolved) {
        Long submissionId = parseLongOrNull(request.getParameter("submissionId"));
        if (submissionId == null) return;
        String note = request.getParameter("note");
        Person reviewer = currentPerson(request);
        CensusIntakeService.reject(em, resolved.proposalId, submissionId, note, reviewer);
        log.info("[CENSUS-REVIEW] Rejected submission {} for proposal {}", submissionId, resolved.proposalId);
    }

    // ── Rendering ────────────────────────────────────────────────────────

    private void render(HttpServletRequest request, HttpServletResponse response, EntityManager em,
                        Resolved resolved, String formError)
            throws ServletException, IOException {

        request.setAttribute("proposalId", resolved.proposalId);
        request.setAttribute("employerName", resolved.prospect.getName());
        request.setAttribute("formError", formError);

        Optional<CensusSubmission> reviewable = CensusIntakeService.reviewable(em, resolved.proposalId);
        if (reviewable.isEmpty()) {
            request.setAttribute("reviewState", "NONE");
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }

        CensusSubmission submission = reviewable.get();
        request.setAttribute("submissionId", submission.getId());
        request.setAttribute("submittedAtDisplay", format(submission.getSubmittedAt()));
        request.setAttribute("originalFilename", submission.getOriginalFilename());

        JsonObject mapping = parseObject(submission.getMappingJson());

        if (CensusSubmission.STATE_UNREADABLE.equals(submission.getState())) {
            request.setAttribute("reviewState", "UNREADABLE");
            request.setAttribute("missingLabelsText", join(missingLabels(mapping)));
            request.setAttribute("foundHeadersText", join(foundHeaders(mapping)));
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }

        // PENDING
        request.setAttribute("reviewState", "PENDING");
        request.setAttribute("rowCount", submission.getRowCount());
        request.setAttribute("issueCount", submission.getIssueCount());

        request.setAttribute("mappingFields", mappingFieldRows(mapping));
        request.setAttribute("ignoredColumnsText", join(stringList(mapping, "ignoredColumns")));
        request.setAttribute("skippedBlankRows", mapping.has("skippedBlankRows") ? mapping.get("skippedBlankRows").getAsInt() : 0);

        List<Map<String, Object>> stagedRows = parseRows(submission.getRowsJson());
        List<Map<String, Object>> rowViews = new ArrayList<>(stagedRows.size());
        for (Map<String, Object> row : stagedRows) {
            rowViews.add(rowView(row));
        }
        request.setAttribute("rows", rowViews);

        List<EmployerParticipant> roster = EmployerParticipantDAO.findByProspectId(em, resolved.prospect.getId());
        request.setAttribute("rosterCount", roster.size());
        buildDiff(request, stagedRows, roster);

        Long pspId = resolveCurrentPspId(request);
        boolean settled = roster.isEmpty() ? false
                : CensusIntakeService.demographicsSettled(em, pspId, resolved.proposalId);
        request.setAttribute("demographicsSettled", settled);
        request.setAttribute("issuesBlockLoad", submission.getIssueCount() > 0);

        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** One row for the review table: canonical field values (may be blank) plus its labelled issues. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> rowView(Map<String, Object> row) {
        Map<String, Object> values = (Map<String, Object>) row.get("values");
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("rowNumber", row.get("rowNumber"));
        view.put("firstName", values.getOrDefault(CensusParseService.F_FIRST_NAME, ""));
        view.put("lastName", values.getOrDefault(CensusParseService.F_LAST_NAME, ""));
        view.put("addressLine1", values.getOrDefault(CensusParseService.F_ADDRESS_LINE1, ""));
        view.put("addressLine2", values.getOrDefault(CensusParseService.F_ADDRESS_LINE2, ""));
        view.put("city", values.getOrDefault(CensusParseService.F_CITY, ""));
        view.put("state", values.getOrDefault(CensusParseService.F_STATE, ""));
        view.put("postalCode", values.getOrDefault(CensusParseService.F_POSTAL_CODE, ""));
        view.put("email", values.getOrDefault(CensusParseService.F_EMAIL, ""));
        @SuppressWarnings("unchecked")
        List<String> rawIssues = (List<String>) row.getOrDefault("issues", List.of());
        List<String> labelled = new ArrayList<>(rawIssues.size());
        for (String issue : rawIssues) {
            labelled.add(labelIssue(issue));
        }
        view.put("issues", labelled);
        view.put("hasIssues", !labelled.isEmpty());
        return view;
    }

    /** {@code "field: reason"} -> {@code "Label: reason"}. Unknown prefixes and shapes pass through unchanged. */
    private static String labelIssue(String issue) {
        int sep = issue.indexOf(": ");
        if (sep <= 0) return issue;
        String field = issue.substring(0, sep);
        String reason = issue.substring(sep + 2);
        for (String known : ALL_FIELDS) {
            if (known.equals(field)) return CensusParseService.labelFor(field) + ": " + reason;
        }
        return issue;
    }

    private static final List<String> ALL_FIELDS = List.of(
            CensusParseService.F_FIRST_NAME, CensusParseService.F_LAST_NAME,
            CensusParseService.F_ADDRESS_LINE1, CensusParseService.F_ADDRESS_LINE2,
            CensusParseService.F_CITY, CensusParseService.F_STATE,
            CensusParseService.F_POSTAL_CODE, CensusParseService.F_EMAIL);

    /**
     * New / Matching / Missing against the current roster. Key is
     * {@code lower(trim(first)) | lower(trim(last)) | first five digits of postal code}, per s47f.
     */
    private void buildDiff(HttpServletRequest request, List<Map<String, Object>> stagedRows,
                           List<EmployerParticipant> roster) {
        Set<String> uploadKeys = new LinkedHashSet<>();
        Map<String, String> uploadNames = new LinkedHashMap<>();
        for (Map<String, Object> row : stagedRows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) row.get("values");
            String first = String.valueOf(values.getOrDefault(CensusParseService.F_FIRST_NAME, ""));
            String last = String.valueOf(values.getOrDefault(CensusParseService.F_LAST_NAME, ""));
            String postal = String.valueOf(values.getOrDefault(CensusParseService.F_POSTAL_CODE, ""));
            String key = diffKey(first, last, postal);
            if (key == null) continue;
            uploadKeys.add(key);
            uploadNames.put(key, first + " " + last);
        }

        Set<String> rosterKeys = new LinkedHashSet<>();
        Map<String, String> rosterNames = new LinkedHashMap<>();
        for (EmployerParticipant p : roster) {
            String key = diffKey(p.getFirstName(), p.getLastName(), p.getPostalCode());
            if (key == null) continue;
            rosterKeys.add(key);
            rosterNames.put(key, p.getFirstName() + " " + p.getLastName());
        }

        List<String> newNames = new ArrayList<>();
        List<String> matchingNames = new ArrayList<>();
        List<String> missingNames = new ArrayList<>();
        for (String key : uploadKeys) {
            if (rosterKeys.contains(key)) matchingNames.add(uploadNames.get(key));
            else newNames.add(uploadNames.get(key));
        }
        for (String key : rosterKeys) {
            if (!uploadKeys.contains(key)) missingNames.add(rosterNames.get(key));
        }

        request.setAttribute("diffNewCount", newNames.size());
        request.setAttribute("diffMatchingCount", matchingNames.size());
        request.setAttribute("diffMissingCount", missingNames.size());
        // S47-G, Kevin's walk: "Last, First" joined with ", " read as one run of comma-separated
        // words ("Testcase, Avery, Testcase, Blake…") with no way to tell a name-internal comma
        // from a list separator. Names are "First Last" (set above) and the list separator is
        // " · ", which appears nowhere inside a name.
        request.setAttribute("diffNewText", joinNames(newNames));
        request.setAttribute("diffMatchingText", joinNames(matchingNames));
        request.setAttribute("diffMissingText", joinNames(missingNames));
        request.setAttribute("rosterEmpty", roster.isEmpty());
    }

    private static String diffKey(String first, String last, String postal) {
        if (first == null || last == null || first.isBlank() || last.isBlank()) return null;
        String p = postal == null ? "" : postal.trim();
        String firstFive = p.length() >= 5 ? p.substring(0, 5) : p;
        return first.trim().toLowerCase() + "|" + last.trim().toLowerCase() + "|" + firstFive;
    }

    // ── JSON reading (mirrors CensusDropServlet's pattern) ──────────────

    private static JsonObject parseObject(String json) {
        if (json == null) return new JsonObject();
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    private static List<String> missingLabels(JsonObject mapping) {
        List<String> labels = new ArrayList<>();
        if (mapping.has("unmatchedRequiredFields")) {
            for (JsonElement e : mapping.getAsJsonArray("unmatchedRequiredFields")) {
                labels.add(CensusParseService.labelFor(e.getAsString()));
            }
        }
        return labels;
    }

    private static List<String> foundHeaders(JsonObject mapping) {
        List<String> headers = new ArrayList<>();
        if (mapping.has("fields")) {
            for (JsonElement e : mapping.getAsJsonArray("fields")) {
                JsonObject f = e.getAsJsonObject();
                if (f.has("header") && !f.get("header").isJsonNull()) {
                    headers.add(f.get("header").getAsString());
                }
            }
        }
        headers.addAll(stringList(mapping, "ignoredColumns"));
        return headers;
    }

    private static List<String> stringList(JsonObject obj, String key) {
        List<String> out = new ArrayList<>();
        if (obj.has(key)) {
            for (JsonElement e : obj.getAsJsonArray(key)) out.add(e.getAsString());
        }
        return out;
    }

    /** One row per {@code mapping.fields} entry, for the accordion — mirrors {@code censusUpload25.jsp:136-195}. */
    private static List<Map<String, Object>> mappingFieldRows(JsonObject mapping) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (!mapping.has("fields")) return rows;
        for (JsonElement e : mapping.getAsJsonArray("fields")) {
            JsonObject f = e.getAsJsonObject();
            Map<String, Object> row = new LinkedHashMap<>();
            String field = f.has("field") && !f.get("field").isJsonNull() ? f.get("field").getAsString() : null;
            row.put("label", field == null ? "" : CensusParseService.labelFor(field));
            boolean matched = f.has("header") && !f.get("header").isJsonNull();
            row.put("matched", matched);
            row.put("header", matched ? f.get("header").getAsString() : null);
            row.put("required", f.has("required") && f.get("required").getAsBoolean());
            rows.add(row);
        }
        return rows;
    }

    private static List<Map<String, Object>> parseRows(String rowsJson) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (rowsJson == null) return rows;
        try {
            for (JsonElement e : JsonParser.parseString(rowsJson).getAsJsonArray()) {
                JsonObject obj = e.getAsJsonObject();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rowNumber", obj.has("rowNumber") ? obj.get("rowNumber").getAsInt() : 0);
                Map<String, Object> values = new LinkedHashMap<>();
                if (obj.has("values")) {
                    JsonObject v = obj.getAsJsonObject("values");
                    for (String key : v.keySet()) {
                        if (!v.get(key).isJsonNull()) values.put(key, v.get(key).getAsString());
                    }
                }
                row.put("values", values);
                List<String> issues = new ArrayList<>();
                if (obj.has("issues")) {
                    for (JsonElement issue : obj.getAsJsonArray("issues")) issues.add(issue.getAsString());
                }
                row.put("issues", issues);
                rows.add(row);
            }
        } catch (RuntimeException e) {
            log.warn("[CENSUS-REVIEW] Could not parse staged rows: {}", e.getMessage());
        }
        return rows;
    }

    // ── Redirect (copied from CensusRequestServlet.redirectAfterPost, S45c/d/e) ──

    private void redirectAfterPost(HttpServletRequest request, HttpServletResponse response,
                                   long proposalId) throws IOException {
        String contextPath = request.getContextPath();
        String fallback = contextPath + "/CensusReview?proposalId=" + proposalId;
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
                    } else if (path.startsWith(contextPath + "/CensusReview")) {
                        target = sessionActivityIsProposal(request, proposalId)
                                ? contextPath + "/ViewActivity25"
                                : fallback;
                    } else if (path.startsWith(contextPath + "/GoActivityDetail25")) {
                        if (sessionActivityIsProposal(request, proposalId)) {
                            target = contextPath + "/ViewActivity25";
                        }
                    }
                }
            } catch (URISyntaxException ignored) {
                // Also catches a Referer carrying raw CR/LF; fall through to the fixed fallback.
            }
        }
        response.sendRedirect(target);
    }

    private static boolean sessionActivityIsProposal(HttpServletRequest request, Long proposalId) {
        if (proposalId == null) return false;
        try {
            Object attribute = request.getSession().getAttribute("local");
            if (!(attribute instanceof AmsDataLocal local)) return false;
            if (local.getCurrentActivity() == null) return false;
            net.superiorstate.ams.model.activity.Activity activity = local.getCurrentActivity().getActivity();
            if (!(activity instanceof net.superiorstate.ams.model.activity.ticket.setup.Setup setup)) return false;
            net.superiorstate.ams.model.sales.application.Application application = setup.getApplication();
            if (application == null) return false;
            Proposal proposal = application.getProposal();
            if (proposal == null || proposal.getId() == null) return false;
            return proposal.getId().equals(proposalId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** Same pattern as {@code SummitResponseServlet.resolveCurrentPspId}. */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** Same pattern as {@code CensusUploadServlet.resolveCreatedBy}. */
    private static String resolveCreatedBy(HttpServletRequest request) {
        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (local != null && local.getCurrentPerson() != null) {
                String name = local.getCurrentPerson().getFullName();
                if (name != null && !name.isBlank()) {
                    return name.length() > 100 ? name.substring(0, 100) : name;
                }
            }
        } catch (Exception ignore) { /* attribution is not worth failing an insert over */ }
        return null;
    }

    private static Person currentPerson(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        return local.getCurrentPerson();
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** S47-G — the diff name lists' own separator, distinct from {@link #join}'s comma. */
    private static String joinNames(List<String> items) {
        return String.join(" · ", items);
    }

    private static String join(List<String> items) {
        return String.join(", ", items);
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : DISPLAY_FORMAT.format(dateTime);
    }

    private void writePlainError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
    }
}

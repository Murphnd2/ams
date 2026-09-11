package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.data.service.CensusIntakeService;
import net.superiorstate.ams.data.service.CensusParseService;
import net.superiorstate.ams.data.util.EmailIdentity;
import net.superiorstate.ams.data.util.EmailIdentityResolver;
import net.superiorstate.ams.data.util.EmailTemplate;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.market.CensusRequest;
import net.superiorstate.ams.model.market.CensusSubmission;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * S47-C — the public, unauthenticated census drop page at {@code /census-drop/{token}} (T231
 * build 1, D29 option 2 / D45). {@code LoginFilter} lets {@code /census-drop/} through; nothing here
 * reads the session, renders the navbar, or links into the authenticated app.
 * <p>
 * <b>The surface stays dumb (D29).</b> The page shows the employer name, the column list and an
 * upload form; the token is resolved through {@code CensusIntakeService.resolveActive}, which
 * answers with one inactive state for unknown, expired, revoked and loaded tokens alike, and that
 * state shows no employer name and nothing else.
 * <p>
 * <b>The file is never stored.</b> It is read from the multipart {@code Part}'s stream and handed to
 * {@code CensusIntakeService.accept}, which parses it leniently and stages only whitelisted rows.
 * Multipart limits are {@code CensusUploadServlet}'s (5 MB file, 10 MB request). <b>No cell value
 * is ever echoed back</b> to the page — issues are reported as row number and reason only.
 * <p>
 * After a successful upload the requester is notified by email; a failed notification is logged
 * and never fails the upload.
 */
@WebServlet(name = "CensusDropServlet", value = "/census-drop/*")
@MultipartConfig(
        maxFileSize = 5 * 1024 * 1024,      // 5 MB — copied from CensusUploadServlet
        maxRequestSize = 10 * 1024 * 1024   // 10 MB total
)
public class CensusDropServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(CensusDropServlet.class);

    private static final String VIEW = "/WEB-INF/view/market/censusDrop25.jsp";
    private static final Set<String> ACCEPTED_EXTENSIONS = Set.of("csv", "txt", "xlsx", "xls");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' HH:mm");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Optional<CensusRequest> active = CensusIntakeService.resolveActive(em, tokenFrom(request));
            if (active.isEmpty()) {
                renderInactive(request, response, em);
                return;
            }
            render(request, response, em, active.get(), null, null);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Optional<CensusRequest> active = CensusIntakeService.resolveActive(em, tokenFrom(request));
            if (active.isEmpty()) {
                renderInactive(request, response, em);
                return;
            }
            CensusRequest censusRequest = active.get();

            Part filePart;
            try {
                filePart = request.getPart("censusFile");
            } catch (IllegalStateException tooLarge) {
                // The container refuses a part over the @MultipartConfig limits with this exception.
                render(request, response, em, censusRequest, null,
                        "That file is too large. The limit is 5 MB.");
                return;
            }
            if (filePart == null || filePart.getSize() == 0
                    || filePart.getSubmittedFileName() == null
                    || filePart.getSubmittedFileName().isBlank()) {
                render(request, response, em, censusRequest, null, "Choose a file to upload.");
                return;
            }
            String filename = filePart.getSubmittedFileName();
            String ext = extensionOf(filename);
            if (ext == null || !ACCEPTED_EXTENSIONS.contains(ext)) {
                render(request, response, em, censusRequest, null,
                        "Please upload a .csv, .txt, .xlsx or .xls file.");
                return;
            }

            CensusSubmission submission;
            try (InputStream in = filePart.getInputStream()) {
                submission = CensusIntakeService.accept(em, censusRequest, filename, in);
            } finally {
                try { filePart.delete(); } catch (Exception ignore) { /* container temp, best effort */ }
            }

            // The parse result for the page (issues by row number and reason only -- never a value).
            // The submission is re-read from its JSON so the page shows exactly what was staged.
            logUpload(em, censusRequest, submission);
            notifyRequester(em, censusRequest, submission);
            render(request, response, em, censusRequest, submission, null);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    // ── Rendering ───────────────────────────────────────────────────────

    /** One state for unknown, expired, revoked and loaded tokens. No employer name, nothing else. */
    private void renderInactive(HttpServletRequest request, HttpServletResponse response, EntityManager em)
            throws ServletException, IOException {
        request.setAttribute("inactive", Boolean.TRUE);
        applyBranding(request, em, null);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void render(HttpServletRequest request, HttpServletResponse response, EntityManager em,
                        CensusRequest censusRequest, CensusSubmission submission, String formError)
            throws ServletException, IOException {
        Proposal proposal = em.find(Proposal.class, censusRequest.getProposalId());
        Prospect prospect = proposal == null ? null : proposal.getProspect();
        String employerName = prospect == null ? "" : prospect.getName();

        applyBranding(request, em, proposal, censusRequest);

        request.setAttribute("inactive", Boolean.FALSE);
        request.setAttribute("token", censusRequest.getToken());
        request.setAttribute("employerName", employerName);
        request.setAttribute("requiredLabelsText", join(labels(CensusParseService.requiredFields())));
        request.setAttribute("optionalLabelsText", join(labels(CensusParseService.optionalFields())));
        request.setAttribute("formError", formError);

        // "Last upload received {date}" -- the latest non-superseded submission, if any.
        CensusIntakeService.Status status = CensusIntakeService.statusFor(em, censusRequest.getProposalId());
        if (status.hasSubmission()) {
            request.setAttribute("lastUploadDisplay", DISPLAY_FORMAT.format(status.getLatestSubmission().getSubmittedAt()));
        }

        if (submission != null) {
            request.setAttribute("resultState", submission.getState());
            request.setAttribute("resultRowCount", submission.getRowCount());
            request.setAttribute("resultIssueCount", submission.getIssueCount());
            if (CensusSubmission.STATE_UNREADABLE.equals(submission.getState())) {
                UnreadableView view = UnreadableView.from(submission.getMappingJson());
                request.setAttribute("missingLabelsText", join(view.missingLabels));
                request.setAttribute("foundHeadersText", join(view.foundHeaders));
                request.setAttribute("fileError", view.fileError);
            } else if (submission.getIssueCount() > 0) {
                request.setAttribute("issueLines", issueLines(submission.getRowsJson()));
            }
        }

        // EM stays open through the forward.
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** Branding attributes the public pages share (copied from {@code ApplyForProposal}): colours, PSP name, agency name. */
    private void applyBranding(HttpServletRequest request, EntityManager em, Proposal proposal) {
        applyBranding(request, em, proposal, null);
    }

    private void applyBranding(HttpServletRequest request, EntityManager em, Proposal proposal,
                               CensusRequest censusRequest) {
        String primaryColor = null;
        String accentColor = null;
        String pspName = "";
        String agencyName = null;
        if (em != null) {
            try {
                primaryColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY");
                accentColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT");
            } catch (RuntimeException e) {
                log.debug("[CENSUS-DROP] branding constants unavailable: {}", e.getMessage());
            }
            if (proposal != null) {
                try {
                    if (proposal.getProspect() != null && proposal.getProspect().getContact() != null
                            && proposal.getProspect().getContact().getPsp() != null) {
                        pspName = proposal.getProspect().getContact().getPsp().getFullName();
                    } else if (censusRequest != null && censusRequest.getRequestedBy() != null) {
                        Person requester = em.find(Person.class, censusRequest.getRequestedBy());
                        if (requester != null && requester.getPsp() != null) {
                            pspName = requester.getPsp().getFullName();
                        }
                    }
                    Agency agency = OriginatingAgencyResolver.resolve(proposal);
                    agencyName = agency == null ? null : agency.getName();
                } catch (RuntimeException e) {
                    log.debug("[CENSUS-DROP] branding names unavailable: {}", e.getMessage());
                }
            }
        }
        if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#2B5F8A";
        if (accentColor == null || accentColor.isEmpty()) accentColor = "#7AB648";
        request.setAttribute("primaryColor", primaryColor);
        request.setAttribute("accentColor", accentColor);
        request.setAttribute("pspName", pspName == null ? "" : pspName);
        request.setAttribute("agencyName", agencyName);
    }

    // ── Inbound activity entry (S47-F, T231 build 2) ─────────────────────

    /**
     * Logs the upload to the setup activity as an inbound note — Received Email (the nearest
     * inbound {@code ReasonCreated} that already exists; see {@code CensusIntakeService}'s
     * constants note). S47-G, Kevin's 2026-09-11 walk: the status follows the outcome rather than
     * always being Waiting on Us — a submission that can't be loaded as-is (issues, or unreadable)
     * leaves the ball with the client, so it's Waiting on Them; only a clean, zero-issue upload
     * puts it back on the PSP admin. The detail never includes a value or a filename.
     * {@code logToSetup} never throws, so an upload can't fail on logging.
     */
    private void logUpload(EntityManager em, CensusRequest censusRequest, CensusSubmission submission) {
        Person author = censusRequest.getRequestedBy() == null
                ? null : em.find(Person.class, censusRequest.getRequestedBy());

        int statusId;
        String detail;
        if (CensusSubmission.STATE_UNREADABLE.equals(submission.getState())) {
            statusId = CensusIntakeService.STATUS_WAITING_ON_THEM;
            detail = "Census upload received via secure link: unreadable — required columns missing."
                    + " Waiting on a corrected file.";
        } else if (submission.getIssueCount() > 0) {
            statusId = CensusIntakeService.STATUS_WAITING_ON_THEM;
            detail = "Census upload received via secure link: " + submission.getRowCount() + " rows, "
                    + submission.getIssueCount() + " issues. Waiting on a corrected file.";
        } else {
            statusId = CensusIntakeService.STATUS_WAITING_ON_US;
            detail = "Census upload received via secure link: " + submission.getRowCount() + " rows, "
                    + submission.getIssueCount() + " issues. Awaiting review.";
        }

        CensusIntakeService.logToSetup(em, censusRequest.getProposalId(), author,
                statusId, CensusIntakeService.REASON_RECEIVED_EMAIL, detail);
    }

    // ── Requester notification ──────────────────────────────────────────

    /**
     * Emails the requester ({@code requested_by}) that an upload arrived. Identity is resolved the
     * way {@code CensusRequestServlet} resolves it for the send — {@code EmailIdentityResolver.resolve}
     * with the requester as sender and the proposal's originating agency — which needs no session,
     * only the Person row. Never throws; a failure is logged and the upload stands.
     */
    private void notifyRequester(EntityManager em, CensusRequest censusRequest, CensusSubmission submission) {
        try {
            if (censusRequest.getRequestedBy() == null) {
                log.info("[CENSUS-DROP] Request #{} has no requester; upload notice skipped", censusRequest.getId());
                return;
            }
            Person requester = em.find(Person.class, censusRequest.getRequestedBy());
            if (requester == null) return;
            String to = requester.getEffectiveEmail();
            if (to == null || to.isBlank()) {
                log.info("[CENSUS-DROP] Requester {} has no email; upload notice skipped", requester.getId());
                return;
            }
            Proposal proposal = em.find(Proposal.class, censusRequest.getProposalId());
            String employerName = proposal != null && proposal.getProspect() != null
                    ? proposal.getProspect().getName() : "the employer";
            Agency agency = proposal == null ? null : OriginatingAgencyResolver.resolve(proposal);

            String subject = "Census upload received — " + employerName;
            StringBuilder body = new StringBuilder();
            body.append("<p>A census upload arrived for <strong>").append(escape(employerName)).append("</strong>.</p>");
            body.append("<p>State: <strong>").append(escape(submission.getState())).append("</strong>");
            if (!CensusSubmission.STATE_UNREADABLE.equals(submission.getState())) {
                body.append(" &middot; ").append(submission.getRowCount()).append(" row")
                        .append(submission.getRowCount() == 1 ? "" : "s")
                        .append(", ").append(submission.getIssueCount()).append(" issue")
                        .append(submission.getIssueCount() == 1 ? "" : "s");
            } else {
                body.append(" &middot; a required column was missing; header names were kept for review");
            }
            body.append(".</p>");
            body.append("<p>Open the Setup in AMS to review it.</p>");

            String pspName = requester.getPsp() != null ? requester.getPsp().getFullName() : "";
            String wrapped = EmailTemplate.wrapBodyOnly(body.toString(), pspName, em);
            EmailIdentity identity = EmailIdentityResolver.resolve(requester, agency, requester.getPsp(), em);
            List<String> toList = new ArrayList<>();
            toList.add(to.trim());
            EmailDAO.sendEmail(identity, toList, Collections.emptyList(), Collections.emptyList(), subject, wrapped, em);
        } catch (Exception e) {
            log.warn("[CENSUS-DROP] Upload notice for request #{} not sent: {}", censusRequest.getId(), e.getMessage());
        }
    }

    // ── Page-side views of the staged JSON (names and reasons only) ─────

    /** Missing required labels and the header names found, read back from {@code mapping_json}. */
    private static final class UnreadableView {
        final List<String> missingLabels = new ArrayList<>();
        final List<String> foundHeaders = new ArrayList<>();
        String fileError;

        static UnreadableView from(String mappingJson) {
            UnreadableView view = new UnreadableView();
            if (mappingJson == null) return view;
            try {
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(mappingJson).getAsJsonObject();
                if (root.has("unmatchedRequiredFields")) {
                    for (com.google.gson.JsonElement e : root.getAsJsonArray("unmatchedRequiredFields")) {
                        view.missingLabels.add(CensusParseService.labelFor(e.getAsString()));
                    }
                }
                if (root.has("fields")) {
                    for (com.google.gson.JsonElement e : root.getAsJsonArray("fields")) {
                        com.google.gson.JsonObject f = e.getAsJsonObject();
                        if (f.has("header") && !f.get("header").isJsonNull()) {
                            view.foundHeaders.add(f.get("header").getAsString());
                        }
                    }
                }
                if (root.has("ignoredColumns")) {
                    for (com.google.gson.JsonElement e : root.getAsJsonArray("ignoredColumns")) {
                        view.foundHeaders.add(e.getAsString());
                    }
                }
                if (root.has("fileError") && !root.get("fileError").isJsonNull()) {
                    view.fileError = root.get("fileError").getAsString();
                }
            } catch (RuntimeException ignored) {
                // A malformed report renders as "no columns found"; never a stack trace on a public page.
            }
            return view;
        }
    }

    /** {@code "Row N — issue"} lines from {@code rows_json}. Reads row numbers and issues only — never {@code values}. */
    private static List<String> issueLines(String rowsJson) {
        List<String> lines = new ArrayList<>();
        if (rowsJson == null) return lines;
        try {
            for (com.google.gson.JsonElement e : com.google.gson.JsonParser.parseString(rowsJson).getAsJsonArray()) {
                com.google.gson.JsonObject row = e.getAsJsonObject();
                if (!row.has("issues")) continue;
                int rowNumber = row.has("rowNumber") ? row.get("rowNumber").getAsInt() : 0;
                for (com.google.gson.JsonElement issue : row.getAsJsonArray("issues")) {
                    lines.add("Row " + rowNumber + " — " + labelIssue(issue.getAsString()));
                }
            }
        } catch (RuntimeException ignored) {
            // As above: a malformed staging row renders nothing rather than failing the page.
        }
        return lines;
    }

    /**
     * S47-F — {@code "field: reason"} (the shape staged by {@code CensusParseService.interpretLenient})
     * to {@code "Label: reason"}. Storage keeps field names; this is a render-time substitution
     * only. Falls back to the original string unchanged when there is no {@code ": "} separator or
     * the prefix is not one of the eight whitelisted fields — never guesses at an unknown shape.
     */
    private static String labelIssue(String issue) {
        int sep = issue.indexOf(": ");
        if (sep <= 0) return issue;
        String field = issue.substring(0, sep);
        String reason = issue.substring(sep + 2);
        for (String known : List.of(CensusParseService.F_FIRST_NAME, CensusParseService.F_LAST_NAME,
                CensusParseService.F_ADDRESS_LINE1, CensusParseService.F_ADDRESS_LINE2,
                CensusParseService.F_CITY, CensusParseService.F_STATE,
                CensusParseService.F_POSTAL_CODE, CensusParseService.F_EMAIL)) {
            if (known.equals(field)) return CensusParseService.labelFor(field) + ": " + reason;
        }
        return issue;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** The token is the path segment after {@code /census-drop/}; anything else is treated as no token. */
    private static String tokenFrom(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) return null;
        String token = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = token.indexOf('/');
        if (slash >= 0) token = token.substring(0, slash);
        token = token.trim();
        return token.isEmpty() || token.length() > 36 ? null : token;
    }

    private static String extensionOf(String filename) {
        if (filename == null) return null;
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) return null;
        return filename.substring(i + 1).toLowerCase();
    }

    private static List<String> labels(List<String> fields) {
        List<String> out = new ArrayList<>(fields.size());
        for (String f : fields) out.add(CensusParseService.labelFor(f));
        return out;
    }

    private static String join(List<String> items) {
        return String.join(", ", items);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.data.service.CensusIntakeService;
import net.superiorstate.ams.data.service.CensusParseService;
import net.superiorstate.ams.data.util.EmailIdentity;
import net.superiorstate.ams.data.util.EmailIdentityResolver;
import net.superiorstate.ams.data.util.EmailTemplate;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.market.CensusRequest;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * S47-C — the Summit setup panel's step 3, "Request census" (T231 build 1, D45 decision d). A
 * PSP admin composes and sends the employer's contact an email carrying a one-time upload link
 * ({@code /census-drop/{token}}), following {@code SendProposal}'s compose-then-send pattern:
 * editable To / Subject / CKEditor body, {@code EmailIdentityResolver} for the From identity,
 * {@code EmailDAO.sendEmail}, and a log entry on the setup activity.
 * <p>
 * <b>Gates mirror {@code CensusUploadServlet} exactly</b> — session {@code isPspAdmin}, then
 * {@code IchraAccessResolver.isAvailable} — so every surface of the census flow agrees about who
 * may reach it. Resolution is {@code proposalId → Proposal → Prospect}, refusing by name at each
 * step the same way.
 * <p>
 * <b>The {@code {link}} placeholder.</b> A request has no token until Send (a renewed request keeps
 * its token, a fresh one mints it inside {@code openOrRenew}), so the GET renders the default body
 * with a literal {@code {link}} where the URL goes and the POST substitutes the real link into
 * whatever body the admin edited. CKEditor may percent-encode the braces inside an {@code href},
 * so both spellings are replaced. That substitution is the only difference between the body shown
 * and the body sent.
 * <p>
 * After either POST the browser is sent back to the setup exactly the way
 * {@code SummitResponseServlet.redirectAfterPost} does it (S45c/d/e rules, copied verbatim with
 * this servlet's own page as the fixed fallback).
 */
@WebServlet(name = "CensusRequestServlet", value = "/CensusRequest")
public class CensusRequestServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(CensusRequestServlet.class);

    private static final String VIEW = "/WEB-INF/view/market/censusRequest25.jsp";
    private static final String LINK_PLACEHOLDER = "{link}";
    private static final String LINK_PLACEHOLDER_ENCODED = "%7Blink%7D";
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("MMMM d, yyyy");

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

            renderForm(request, response, em, resolved);
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
            if ("revoke".equals(action)) {
                handleRevoke(request, em, resolved);
            } else if ("send".equals(action)) {
                handleSend(request, em, resolved);
            } else {
                writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "action (send|revoke) is required.");
                return;
            }
        } finally {
            if (em.isOpen()) em.close();
        }

        redirectAfterPost(request, response, proposalId);
    }

    // ── Gating and resolution (copied from CensusUploadServlet) ─────────

    /** Mirrors {@code CensusUploadServlet.isPspAdmin} exactly — the session flag, not a role lookup. */
    private static boolean isPspAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
    }

    private static class Resolved {
        final long proposalId;
        final Proposal proposal;
        final Prospect prospect;
        Resolved(long proposalId, Proposal proposal, Prospect prospect) {
            this.proposalId = proposalId;
            this.proposal = proposal;
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
                    "Proposal " + proposalId + " has no employer (prospect) to request a census"
                            + " from.");
            return null;
        }
        return new Resolved(proposalId, proposal, prospect);
    }

    // ── GET: compose form ───────────────────────────────────────────────

    private void renderForm(HttpServletRequest request, HttpServletResponse response,
                            EntityManager em, Resolved resolved)
            throws ServletException, IOException {

        Person sender = currentPerson(request);
        Setup setup = findSetup(em, resolved.proposalId);
        String toEmail = defaultRecipient(setup, resolved.prospect);
        String employerName = resolved.prospect.getName();

        CensusIntakeService.Status status = CensusIntakeService.statusFor(em, resolved.proposalId);
        String existingLink = status.hasRequest()
                ? baseUrl(request) + "census-drop/" + status.getRequest().getToken() : null;

        // The expiry shown in the body is what the send will set: now + EXPIRY_DAYS.
        String expiryText = LocalDate.now().plusDays(CensusIntakeService.EXPIRY_DAYS).format(DATE_ONLY);

        request.setAttribute("proposalId", resolved.proposalId);
        request.setAttribute("employerName", employerName);
        request.setAttribute("toEmail", toEmail == null ? "" : toEmail);
        request.setAttribute("senderEmail", sender == null ? "" : nullToEmpty(sender.getEmail()));
        request.setAttribute("defaultSubject", "Census request — " + employerName);
        request.setAttribute("defaultBody", defaultBody(sender, resolved.proposal, employerName, expiryText));
        request.setAttribute("status", status);
        request.setAttribute("existingLink", existingLink);
        request.setAttribute("requestedAtDisplay", format(status.getRequestedAt()));
        request.setAttribute("expiresAtDisplay", format(status.getExpiresAt()));
        request.setAttribute("closedAtDisplay", format(status.getClosedAt()));
        request.setAttribute("submissionAtDisplay",
                status.hasSubmission() ? format(status.getLatestSubmission().getSubmittedAt()) : null);
        request.setAttribute("requiredLabelsText", join(labels(CensusParseService.requiredFields())));
        request.setAttribute("optionalLabelsText", join(labels(CensusParseService.optionalFields())));

        // EM stays open through the forward — the JSP reads the status object.
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /**
     * The default body. Column labels come from {@code CensusParseService}'s own field list
     * ({@code requiredFields()}/{@code optionalFields()}/{@code labelFor}), so the email and the
     * parser can never disagree about what is required. {@code {link}} is substituted on POST.
     */
    private static String defaultBody(Person sender, Proposal proposal, String employerName, String expiryText) {
        Agency originatingAgency = OriginatingAgencyResolver.resolve(proposal);
        String agencyName = originatingAgency != null ? originatingAgency.getName() : null;
        String senderCompany = agencyName != null ? agencyName
                : (sender != null && sender.getPsp() != null ? sender.getPsp().getFullName() : "");
        String senderName = sender == null ? "" : nullToEmpty(sender.getFirstName()) + " " + nullToEmpty(sender.getLastName());
        String senderEmail = sender == null ? "" : nullToEmpty(sender.getEmail());

        StringBuilder b = new StringBuilder();
        b.append("<p>Hello,</p>");
        b.append("<p>To set up benefits administration for <strong>").append(escape(employerName))
                .append("</strong>, we need a current employee census. Please upload it using the secure link below.</p>");
        b.append("<p><a href=\"").append(LINK_PLACEHOLDER).append("\">").append(LINK_PLACEHOLDER).append("</a></p>");
        b.append("<p>The link expires on <strong>").append(expiryText).append("</strong>.</p>");
        b.append("<p><strong>Required columns:</strong> ").append(join(labels(CensusParseService.requiredFields()))).append("<br/>");
        b.append("<strong>Optional columns:</strong> ").append(join(labels(CensusParseService.optionalFields()))).append("</p>");
        b.append("<p>Please leave out Social Security numbers, dates of birth, and pay — we don't need them.</p>");
        b.append("<p>You can upload a corrected file at any time before the link expires; the newest upload replaces earlier ones.</p>");
        b.append("<p>If you have any questions, simply reply to this email.</p>");
        b.append("<p style=\"margin-top:24px;\">").append(escape(senderName.trim())).append("<br/>")
                .append("<span style=\"color:#666666;\">").append(escape(senderEmail)).append("</span><br/>")
                .append("<span style=\"color:#7AB648;font-weight:bold;\">").append(escape(senderCompany)).append("</span></p>");
        return b.toString();
    }

    // ── POST: send / revoke ─────────────────────────────────────────────

    private void handleSend(HttpServletRequest request, EntityManager em, Resolved resolved) {
        Person sender = currentPerson(request);
        Long userId = sender == null ? null : sender.getId();
        String toEmail = request.getParameter("toEmail");
        String ccEmail = request.getParameter("ccEmail");
        String subject = request.getParameter("subject");
        String body = request.getParameter("body");
        boolean copyMe = "on".equals(request.getParameter("copyMe"));

        if (subject == null || subject.isBlank()) subject = "Census request — " + resolved.prospect.getName();
        if (body == null) body = "";

        // 1. Open or renew the request (the token is fixed here).
        CensusRequest censusRequest = CensusIntakeService.openOrRenew(em, resolved.proposalId,
                toEmail, userId);

        // 2. Substitute the link into the edited body.
        String link = baseUrl(request) + "census-drop/" + censusRequest.getToken();
        String linkedBody = body.replace(LINK_PLACEHOLDER, link).replace(LINK_PLACEHOLDER_ENCODED, link);

        // 3. Send, the way SendProposal does.
        try {
            List<String> toList = new ArrayList<>();
            if (toEmail != null && !toEmail.isBlank()) toList.add(toEmail.trim());
            List<String> ccList = new ArrayList<>();
            if (ccEmail != null && !ccEmail.isBlank()) {
                for (String cc : ccEmail.split("[,;]")) {
                    if (!cc.trim().isBlank()) ccList.add(cc.trim());
                }
            }
            String fromEmail = sender == null ? null : sender.getEmail();
            if (copyMe && fromEmail != null) ccList.add(fromEmail);

            String pspName = sender != null && sender.getPsp() != null ? sender.getPsp().getFullName() : "";
            String wrappedBody = EmailTemplate.wrapBodyOnly(linkedBody, pspName, em);

            Agency agency = OriginatingAgencyResolver.resolve(resolved.proposal);
            EmailIdentity identity = EmailIdentityResolver.resolve(sender, agency,
                    sender == null ? null : sender.getPsp(), em);
            EmailDAO.sendEmail(identity, toList, ccList, Collections.emptyList(), subject, wrappedBody, em);

            // 4. Log to the setup activity (fallback: the proposal's source activity).
            logEmailToActivity(em, resolved, sender, subject, wrappedBody);

            log.info("[CENSUS-REQUEST] Request #{} for proposal {} sent to {}",
                    censusRequest.getId(), resolved.proposalId, toEmail);
        } catch (Exception e) {
            log.error("[CENSUS-REQUEST] Send failed for proposal {}: {}", resolved.proposalId, e.getMessage());
        }
    }

    private void handleRevoke(HttpServletRequest request, EntityManager em, Resolved resolved) {
        Person sender = currentPerson(request);
        Long userId = sender == null ? null : sender.getId();
        CensusIntakeService.Status status = CensusIntakeService.statusFor(em, resolved.proposalId);
        if (status.isOpen()) {
            CensusIntakeService.revoke(em, status.getRequest().getId(), userId);
            log.info("[CENSUS-REQUEST] Request #{} for proposal {} revoked",
                    status.getRequest().getId(), resolved.proposalId);
        }
    }

    /** Same shape as {@code SendProposal.logEmailToActivity}; never fails the send. */
    private void logEmailToActivity(EntityManager em, Resolved resolved, Person sender,
                                    String subject, String wrappedBody) {
        try {
            Activity activity = findSetup(em, resolved.proposalId);
            if (activity == null && resolved.proposal.getSourceActivity() != null) {
                activity = EntityLookup.getActivityById(em, resolved.proposal.getSourceActivity().getId());
            }
            if (activity != null) {
                em.getTransaction().begin();
                Email email = new Email();
                email.setActivity(activity);
                email.setSubject(subject);
                email.setDateGenerated(Date.valueOf(LocalDate.now()));
                email.setStatus(EntityLookup.getActivityStatusById(em, 1));
                email.setReasonCreated(EntityLookup.getReasonById(em, 7));
                email.setCreatedBy(sender);
                email.setDetail(wrappedBody);
                em.persist(email);
                activity.addNote(email);
                em.persist(activity);
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            log.warn("[CENSUS-REQUEST] Could not log email to activity: {}", e.getMessage());
        }
    }

    // ── Redirect (copied from SummitResponseServlet.redirectAfterPost, S45c/d/e) ──

    private void redirectAfterPost(HttpServletRequest request, HttpServletResponse response,
                                   long proposalId) throws IOException {
        String contextPath = request.getContextPath();
        String fallback = contextPath + "/CensusRequest?proposalId=" + proposalId;
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
                    } else if (path.startsWith(contextPath + "/CensusRequest")) {
                        // Submitted from this servlet's own page: back to the activity when the
                        // session's current activity is this proposal's Setup, else stay here.
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

    // ── Helpers ─────────────────────────────────────────────────────────

    /** The Setup whose application's proposal is {@code proposalId}, or null. Same chain {@code GoActivityDetail25} walks. */
    private static Setup findSetup(EntityManager em, long proposalId) {
        try {
            Query q = em.createQuery("SELECT s FROM Setup s WHERE s.application.proposal.id = :pid");
            q.setParameter("pid", proposalId);
            q.setMaxResults(1);
            @SuppressWarnings("unchecked")
            List<Setup> setups = (List<Setup>) q.getResultList();
            return setups.isEmpty() ? null : setups.get(0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * The address the setup's detail page shows as Primary Contact
     * ({@code detailPrimaryContact25.jsp}: the activity's {@code primaryContact}, Employee email
     * preferred — which is what {@code Person.getEffectiveEmail()} encodes), falling back to the
     * prospect's contact ({@code SendProposal}'s choice). Null when neither has an address.
     */
    private static String defaultRecipient(Setup setup, Prospect prospect) {
        if (setup != null && setup.getPrimaryContact() != null) {
            String email = setup.getPrimaryContact().getEffectiveEmail();
            if (email != null && !email.isBlank()) return email.trim();
        }
        if (prospect.getContact() != null) {
            String email = prospect.getContact().getEffectiveEmail();
            if (email != null && !email.isBlank()) return email.trim();
        }
        return null;
    }

    /** Copied from {@code SendProposal.doGet}: scheme://host[:port]/context/ */
    private static String baseUrl(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (port != 80 && port != 443) baseUrl += ":" + port;
        baseUrl += request.getContextPath() + "/";
        return baseUrl;
    }

    private static Person currentPerson(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        return local.getCurrentPerson();
    }

    private static List<String> labels(List<String> fields) {
        List<String> out = new ArrayList<>(fields.size());
        for (String f : fields) out.add(CensusParseService.labelFor(f));
        return out;
    }

    private static String join(List<String> items) {
        return String.join(", ", items);
    }

    private static String format(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : DISPLAY_FORMAT.format(dateTime);
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void writePlainError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
    }
}

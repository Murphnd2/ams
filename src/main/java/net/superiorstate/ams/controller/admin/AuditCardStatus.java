package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AuditFindingAckDAO;
import net.superiorstate.ams.data.resolver.SummitParticipantLinkResolver;
import net.superiorstate.ams.data.service.audit.AuditCheck;
import net.superiorstate.ams.data.service.audit.AuditService;
import net.superiorstate.ams.data.service.audit.CardStatusCheck;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.AuditFindingAck;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T237 fourth check — detail page for {@link CardStatusCheck}. Reads the Debit Card Participants
 * export live, in-request, the same way {@link AuditCardDeclines} does for the Transaction export —
 * nothing here is stored, and nothing is logged beyond the count {@link AuditService} already
 * recorded (LA-40).
 * <p>
 * Gated on PSP admin plus the framework's own PSP match, exactly like {@link AuditCardDeclines}. No
 * {@code IchraAccessResolver} gate: this check covers every card-bearing plan type.
 * <p>
 * {@code doPost} handles only {@code action=ack|unack} for {@code audit_finding_ack} (V115), gated,
 * PSP-resolved, and flash-messaged exactly like {@link AuditCardDeclines}'s own {@code doPost}. The
 * hub's "Run audit now" button still posts to {@code AuditHub} separately.
 * <p>
 * <b>A successful acknowledgment re-runs {@link CardStatusCheck} alone, synchronously, before the
 * redirect</b> — see {@link AuditCardDeclines}'s class note for why (the hub and navbar badge read
 * the stored {@code audit_run} row, not a live recompute).
 */
@WebServlet(name = "AuditCardStatus", value = "/AuditCardStatus")
public class AuditCardStatus extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String FLASH_MESSAGE = "auditCardStatusAckMessage";
    private static final String FLASH_ERROR = "auditCardStatusAckError";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!Boolean.TRUE.equals(session.getAttribute("isPspAdmin"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        AuditService auditService = (AuditService) getServletContext().getAttribute("auditService");
        if (auditService == null) {
            writePlainError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The audit framework did not start on this installation. Check server logs.");
            return;
        }

        Long sessionPspId = resolveCurrentPspId(request);
        if (sessionPspId == null || !sessionPspId.equals(auditService.getPspId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        CardStatusCheck check = findCheck(auditService);
        if (check == null) {
            writePlainError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The card-status check is not registered on this installation.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        CardStatusCheck.Snapshot snapshot;
        try {
            snapshot = check.readLive(em, auditService.getPspId());
        } finally {
            if (em.isOpen()) em.close();
        }

        request.setAttribute("snapshotError", snapshot.error());
        request.setAttribute("exportFileName", snapshot.fileName());
        request.setAttribute("exportTimestampDisplay", formatDisplay(snapshot.fileTimestamp()));
        request.setAttribute("exportAgeHoursDisplay", ageHoursDisplay(snapshot.fileTimestamp()));
        request.setAttribute("warnDays", snapshot.warnDays());
        request.setAttribute("designatedEmployerCount", snapshot.designatedEmployerCount());
        request.setAttribute("matchedRows", snapshot.matchedRows());
        request.setAttribute("distinctEmployerCount", snapshot.distinctEmployerCount());
        request.setAttribute("undesignatedRows", snapshot.undesignatedRows());
        request.setAttribute("problemRows", snapshot.problemRows());
        request.setAttribute("expiryRows", snapshot.expiryRows());
        request.setAttribute("suppressedCount", snapshot.suppressedCount());
        request.setAttribute("acknowledged", snapshot.acknowledged());
        request.setAttribute("supersededRows", snapshot.supersededRows());
        request.setAttribute("supersededCount", snapshot.supersededCount());
        request.setAttribute("supersededByUsable", snapshot.supersededByUsable());
        request.setAttribute("supersededByProblem", snapshot.supersededByProblem());
        request.setAttribute("participantSummitUrls",
                participantSummitUrls(snapshot.problemRows(), snapshot.expiryRows(), snapshot.supersededRows()));

        request.setAttribute("pageTitle", "Card Status");
        request.setAttribute("pageIcon", "bi-credit-card");
        request.getRequestDispatcher("/WEB-INF/view/a/admin/auditCardStatus25.jsp").forward(request, response);
    }

    /** Same shape as {@link AuditCardDeclines#doPost} — see that class's note. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!Boolean.TRUE.equals(session.getAttribute("isPspAdmin"))) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        AuditService auditService = (AuditService) getServletContext().getAttribute("auditService");
        Long pspId = auditService == null ? null : resolveCurrentPspId(request);
        if (auditService == null || pspId == null || !pspId.equals(auditService.getPspId())) {
            session.setAttribute(FLASH_ERROR, "Could not determine your PSP from this session. Nothing was saved.");
            response.sendRedirect(request.getContextPath() + "/AuditCardStatus");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        boolean saved = false;
        try {
            String action = request.getParameter("action");
            if ("ack".equals(action)) {
                saved = ack(request, session, em, pspId);
            } else if ("unack".equals(action)) {
                saved = unack(request, session, em, pspId);
            }
        } catch (RuntimeException e) {
            session.setAttribute(FLASH_ERROR, "The change was not saved: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        if (saved) {
            reRunAfterAcknowledgment(auditService, session);
        }

        response.sendRedirect(request.getContextPath() + "/AuditCardStatus");
    }

    private void reRunAfterAcknowledgment(AuditService auditService, HttpSession session) {
        try {
            AuditService.SingleCheckRunResult result = auditService.runOneCheck(CardStatusCheck.KEY);
            switch (result) {
                case COMPLETED -> appendToFlash(session, FLASH_MESSAGE,
                        " Card-status audit re-run — the hub and badge are up to date.");
                case ALREADY_RUNNING -> appendToFlash(session, FLASH_MESSAGE,
                        " An audit run was already in progress, so the re-run was skipped — the hub"
                                + " count will update on that run's completion.");
                case NOT_REGISTERED -> { /* should not happen; nothing to tell the operator */ }
            }
        } catch (RuntimeException e) {
            appendToFlash(session, FLASH_MESSAGE, " The acknowledgment was saved, but the audit re-run"
                    + " failed (" + e.getMessage() + ") — the hub count will update on the next run.");
        }
    }

    private static void appendToFlash(HttpSession session, String attributeName, String suffix) {
        Object current = session.getAttribute(attributeName);
        session.setAttribute(attributeName, (current == null ? "" : current) + suffix);
    }

    // ── Acknowledgment actions ───────────────────────────────────────

    /** @return true if an acknowledgment was saved (a re-run should follow), false if validation
     *  failed and nothing changed (a flash error was already set). {@code observedThrough} is
     *  optional on the wire — a finding's expiration date may itself be null (see
     *  {@link CardStatusCheck} class Javadoc); {@code observedStatusId} is not, since it comes
     *  straight off {@code ParticipantCardStatusID}, which is always present on a row that reached
     *  the finding stage. */
    private boolean ack(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        String findingKey = request.getParameter("findingKey");
        String state = request.getParameter("state");
        if (findingKey == null || findingKey.isBlank()) {
            session.setAttribute(FLASH_ERROR, "No finding was identified.");
            return false;
        }
        if (!AuditFindingAck.STATE_HANDLED.equals(state) && !AuditFindingAck.STATE_IGNORED.equals(state)) {
            session.setAttribute(FLASH_ERROR, "Unrecognized acknowledgment state.");
            return false;
        }

        AuditFindingAck ack = new AuditFindingAck();
        ack.setPspId(pspId);
        ack.setCheckKey(CardStatusCheck.KEY);
        ack.setFindingKey(findingKey.trim());
        ack.setAckState(state);
        ack.setNote(blankToNull(request.getParameter("note")));

        // Set together for HANDLED, null for IGNORED regardless of what the form posted — the
        // invariant is enforced here, server-side, the only writer, matching AuditCardDeclines.
        if (AuditFindingAck.STATE_HANDLED.equals(state)) {
            Integer observedStatusId = parseIntOrNull(request.getParameter("observedStatusId"));
            if (observedStatusId == null) {
                session.setAttribute(FLASH_ERROR, "Handled requires the current card status.");
                return false;
            }
            ack.setObservedCount(observedStatusId);
            ack.setObservedThrough(parseDateOrNull(request.getParameter("observedThrough")));
        } else {
            ack.setObservedCount(null);
            ack.setObservedThrough(null);
        }

        AuditFindingAckDAO.upsert(em, ack, resolveCurrentUserName(request));
        session.setAttribute(FLASH_MESSAGE, "Finding " + findingKey
                + (AuditFindingAck.STATE_HANDLED.equals(state) ? " marked handled." : " marked ignored."));
        return true;
    }

    /** @return true if an acknowledgment was removed (a re-run should follow), false if
     *  validation failed and nothing changed (a flash error was already set). */
    private boolean unack(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        Long id = parseLongOrNull(request.getParameter("id"));
        if (id == null) {
            session.setAttribute(FLASH_ERROR, "No acknowledgment was identified to remove.");
            return false;
        }
        AuditFindingAck ack = AuditFindingAckDAO.findById(em, id);
        if (ack == null || !pspId.equals(ack.getPspId())) {
            session.setAttribute(FLASH_ERROR, "That acknowledgment no longer exists, or belongs to another"
                    + " PSP. Nothing was removed.");
            return false;
        }
        AuditFindingAckDAO.delete(em, id);
        session.setAttribute(FLASH_MESSAGE, "Acknowledgment removed — the finding will surface again if still active.");
        return true;
    }

    private CardStatusCheck findCheck(AuditService auditService) {
        for (AuditCheck check : auditService.getRegisteredChecks()) {
            if (check instanceof CardStatusCheck statusCheck) {
                return statusCheck;
            }
        }
        return null;
    }

    /** One Summit "Edit Participant" URL per distinct {@code ParticipantID} across all three
     *  sections (Problem status, Expired or expiring, and Superseded — the last one's {@code
     *  ParticipantID} column is a link exactly like the first two's) — same route
     *  {@link AuditCardDeclines} uses. Absent (never a broken link) for a blank {@code
     *  ParticipantID} (every dependent row) or when either config value is unset. */
    private Map<String, String> participantSummitUrls(List<CardStatusCheck.Row> problemRows,
                                                       List<CardStatusCheck.Row> expiryRows,
                                                       List<CardStatusCheck.SupersededRow> supersededRows) {
        Map<String, String> urls = new LinkedHashMap<>();
        for (CardStatusCheck.Row row : problemRows) {
            addParticipantUrl(urls, row.participantId());
        }
        for (CardStatusCheck.Row row : expiryRows) {
            addParticipantUrl(urls, row.participantId());
        }
        for (CardStatusCheck.SupersededRow row : supersededRows) {
            addParticipantUrl(urls, row.participantId());
        }
        return urls;
    }

    private void addParticipantUrl(Map<String, String> urls, String participantId) {
        if (participantId == null || participantId.isBlank() || urls.containsKey(participantId)) return;
        String url = SummitParticipantLinkResolver.buildEditParticipantUrl(getServletContext(), participantId);
        if (url != null) {
            urls.put(participantId, url);
        }
    }

    private static String formatDisplay(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_FORMAT);
    }

    private static String ageHoursDisplay(LocalDateTime fileTimestamp) {
        if (fileTimestamp == null) return "—";
        long hours = Duration.between(fileTimestamp, LocalDateTime.now()).toHours();
        return hours + "h";
    }

    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    private static String resolveCurrentUserName(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        return local.getCurrentPerson().getFullName();
    }

    private static String blankToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** ISO {@code yyyy-MM-dd}, or blank/"—" when the finding's expiration date is null — see the
     *  class Javadoc on {@link #ack}. */
    private static LocalDate parseDateOrNull(String raw) {
        if (raw == null || raw.isBlank() || "—".equals(raw.trim())) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}

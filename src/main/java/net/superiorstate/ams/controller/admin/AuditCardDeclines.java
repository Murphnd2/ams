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
import net.superiorstate.ams.data.service.audit.CardDeclineCheck;
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
 * T237 — detail page for {@link CardDeclineCheck}. Reads the Transaction export live,
 * in-request, the same way {@link AuditFundedPurse} does for the Plan History export — nothing
 * here is stored, and nothing is logged beyond the count {@link AuditService} already recorded
 * (LA-40).
 * <p>
 * Gated on PSP admin plus the framework's own PSP match, exactly like {@link AuditHub} and
 * {@link AuditFundedPurse}. It does <b>not</b> add {@link AuditIchraUncoded}'s extra
 * {@code IchraAccessResolver} gate: this check covers every card-bearing plan type, not just
 * ICHRA.
 * <p>
 * <b>{@code doPost} — acknowledgment, not "Run audit now."</b> This servlet had no {@code doPost}
 * before this build; the toolbar's "Run audit now" button posts to {@code AuditHub}
 * ({@code action=runnow}), a separate servlet, and still does — it is untouched here. This
 * {@code doPost} is new, handles only {@code action=ack|unack} for {@code audit_finding_ack}
 * (V115), and is gated, PSP-resolved, and flash-messaged exactly like {@code
 * AuditDeclineEmployerAdmin}'s {@code doPost}, including the ownership check that the
 * acknowledgment being removed belongs to the session PSP.
 * <p>
 * <b>A successful acknowledgment re-runs {@link CardDeclineCheck} alone, synchronously, before
 * the redirect.</b> {@code readLive()} recomputes per request, so this page is always current —
 * but the Audit Hub and the navbar badge read the <i>stored</i> {@code audit_run} row, which
 * changes only when a check actually runs. Without this, acknowledging a finding would leave the
 * hub showing a stale count until the next scheduled or manual run. See
 * {@link AuditService#runOneCheck}: it shares the framework's single-run guard, so a re-run here
 * can never overlap a scheduled tick or "Run now" — if one is already in progress, the
 * acknowledgment is still saved and the re-run is skipped, not retried and not lost, and the flash
 * message says so. A re-run that throws likewise never undoes the acknowledgment.
 */
@WebServlet(name = "AuditCardDeclines", value = "/AuditCardDeclines")
public class AuditCardDeclines extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Flash attribute names, following {@code AuditDeclineEmployerAdmin}'s naming convention. */
    private static final String FLASH_MESSAGE = "auditCardDeclineAckMessage";
    private static final String FLASH_ERROR = "auditCardDeclineAckError";

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

        CardDeclineCheck check = findCheck(auditService);
        if (check == null) {
            writePlainError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The card-decline check is not registered on this installation.");
            return;
        }

        // The check never opens its own persistence context: this servlet supplies the
        // EntityManager (for the designated-employer set and the in-request name lookups) and
        // the framework's PSP id, the same pair AuditService hands evaluate(). Closed before the
        // forward — the Snapshot holds only strings and counts, nothing lazy.
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        CardDeclineCheck.Snapshot snapshot;
        try {
            snapshot = check.readLive(em, auditService.getPspId());
        } finally {
            if (em.isOpen()) em.close();
        }

        request.setAttribute("snapshotError", snapshot.error());
        request.setAttribute("exportFileName", snapshot.fileName());
        request.setAttribute("exportTimestampDisplay", formatDisplay(snapshot.fileTimestamp()));
        request.setAttribute("exportAgeHoursDisplay", ageHoursDisplay(snapshot.fileTimestamp()));
        request.setAttribute("windowDays", snapshot.windowDays());
        request.setAttribute("windowRangeDisplay", snapshot.windowRangeDisplay());
        request.setAttribute("designatedEmployerCount", snapshot.designatedEmployerCount());
        request.setAttribute("totalDeclineRows", snapshot.totalDeclineRows());
        request.setAttribute("distinctEmployerCount", snapshot.distinctEmployerCount());
        request.setAttribute("distinctReasonCount", snapshot.distinctReasonCount());
        request.setAttribute("findings", snapshot.findings());
        request.setAttribute("suppressedCount", snapshot.suppressedCount());
        request.setAttribute("acknowledged", snapshot.acknowledged());
        request.setAttribute("mccSummary", snapshot.mccSummary());
        request.setAttribute("participantSummitUrls", participantSummitUrls(snapshot.findings()));

        request.setAttribute("pageTitle", "Card Declines");
        request.setAttribute("pageIcon", "bi-bell");
        request.getRequestDispatcher("/WEB-INF/view/a/admin/auditCardDeclines25.jsp").forward(request, response);
    }

    /**
     * {@code action=ack} saves a Handled or Ignored acknowledgment for one participant finding;
     * {@code action=unack} removes one. Same gate, PSP resolution, flash-message and
     * redirect-to-self shape as {@code AuditDeclineEmployerAdmin.doPost} — see the class note.
     */
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
            response.sendRedirect(request.getContextPath() + "/AuditCardDeclines");
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
            // Never swallowed, same contract AuditDeclineEmployerAdmin documents: a constraint
            // violation or other failure must reach the operator, not vanish silently.
            session.setAttribute(FLASH_ERROR, "The change was not saved: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        // Only when something actually changed — a validation failure above already left saved
        // false and set FLASH_ERROR, and there is nothing for a re-run to reflect.
        if (saved) {
            reRunAfterAcknowledgment(auditService, session);
        }

        response.sendRedirect(request.getContextPath() + "/AuditCardDeclines");
    }

    /**
     * Re-runs {@link CardDeclineCheck} alone and appends a note to the acknowledgment's own
     * success message — never overwrites it, and never turns a saved acknowledgment into an
     * error. See the class note for why this exists and the guard it shares with the framework's
     * other triggers. Any exception from {@link AuditService#runOneCheck} itself (as opposed to a
     * check-level failure, which {@code runOneCheck} already converts to a stored {@code ERROR}
     * row) is caught here too, belt and suspenders — the acknowledgment must survive regardless.
     */
    private void reRunAfterAcknowledgment(AuditService auditService, HttpSession session) {
        try {
            AuditService.SingleCheckRunResult result = auditService.runOneCheck(CardDeclineCheck.KEY);
            switch (result) {
                case COMPLETED -> appendToFlash(session, FLASH_MESSAGE,
                        " Card-decline audit re-run — the hub and badge are up to date.");
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
     *  failed and nothing changed (a flash error was already set). */
    private boolean ack(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        String participantId = request.getParameter("participantId");
        String state = request.getParameter("state");
        if (participantId == null || participantId.isBlank()) {
            session.setAttribute(FLASH_ERROR, "No participant was identified.");
            return false;
        }
        if (!AuditFindingAck.STATE_HANDLED.equals(state) && !AuditFindingAck.STATE_IGNORED.equals(state)) {
            session.setAttribute(FLASH_ERROR, "Unrecognized acknowledgment state.");
            return false;
        }

        AuditFindingAck ack = new AuditFindingAck();
        ack.setPspId(pspId);
        ack.setCheckKey(CardDeclineCheck.KEY);
        ack.setFindingKey(participantId.trim());
        ack.setAckState(state);
        ack.setNote(blankToNull(request.getParameter("note")));

        // Set together for HANDLED (what was actually seen when acknowledging), null for IGNORED
        // regardless of what the form posted — the invariant is enforced here, server-side, the
        // only writer, not trusted from the client. See V115's own note on why there is no DB
        // constraint for this.
        if (AuditFindingAck.STATE_HANDLED.equals(state)) {
            Integer observedCount = parseIntOrNull(request.getParameter("observedCount"));
            LocalDate observedThrough = parseDateOrNull(request.getParameter("observedThrough"));
            if (observedCount == null || observedThrough == null) {
                session.setAttribute(FLASH_ERROR, "Handled requires the current decline count and most-recent-decline date.");
                return false;
            }
            ack.setObservedCount(observedCount);
            ack.setObservedThrough(observedThrough);
        } else {
            ack.setObservedCount(null);
            ack.setObservedThrough(null);
        }

        AuditFindingAckDAO.upsert(em, ack, resolveCurrentUserName(request));
        session.setAttribute(FLASH_MESSAGE, "Participant " + participantId
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

    private CardDeclineCheck findCheck(AuditService auditService) {
        for (AuditCheck check : auditService.getRegisteredChecks()) {
            if (check instanceof CardDeclineCheck declineCheck) {
                return declineCheck;
            }
        }
        return null;
    }

    /**
     * One Summit "Edit Participant" URL per distinct {@code Participant System ID} on the page,
     * via {@link SummitParticipantLinkResolver} — the same {@code SUMMIT_PATH}/{@code
     * SUMMIT_TPA_GUID} route {@code SummitEmployerLinkResolver} uses for the employer link. Absent
     * from the map (never a broken link) when either constant is unset — the JSP falls back to
     * plain text for that id.
     */
    private Map<String, String> participantSummitUrls(List<CardDeclineCheck.Row> findings) {
        Map<String, String> urls = new LinkedHashMap<>();
        for (CardDeclineCheck.Row row : findings) {
            String url = SummitParticipantLinkResolver.buildEditParticipantUrl(
                    getServletContext(), row.participantSystemId());
            if (url != null) {
                urls.put(row.participantSystemId(), url);
            }
        }
        return urls;
    }

    private static String formatDisplay(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_FORMAT);
    }

    private static String ageHoursDisplay(LocalDateTime fileTimestamp) {
        if (fileTimestamp == null) return "—";
        long hours = Duration.between(fileTimestamp, LocalDateTime.now()).toHours();
        return hours + "h";
    }

    /** Same pattern as {@code AuditHub.resolveCurrentPspId}. */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** Display-only, for {@code created_by}/{@code updated_by} — same source
     *  {@code AuditDeclineEmployerAdmin.resolveCurrentUserName} uses. Null when the session cannot
     *  name anyone. */
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

    /** ISO {@code yyyy-MM-dd} — the shape {@code CardDeclineCheck}'s own {@code displayDate}
     *  ({@code LocalDate.toString()}) emits, which is what the form's hidden field carries back. */
    private static LocalDate parseDateOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
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

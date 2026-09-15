package net.superiorstate.ams.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.service.audit.AuditCheck;
import net.superiorstate.ams.data.service.audit.AuditService;
import net.superiorstate.ams.data.service.audit.FundedPurseNoDisbursementCheck;
import net.superiorstate.ams.model.general.PSP;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * T237 — detail page for {@link FundedPurseNoDisbursementCheck}. Reads the Participant Plan
 * History export live, in-request, the same way {@link AuditIchraUncoded} does for the
 * participant list — nothing here is stored, and nothing is logged beyond the count
 * {@link AuditService} already recorded (LA-40).
 * <p>
 * Gated on PSP admin plus the framework's own PSP match, exactly like {@link AuditHub}. It does
 * <b>not</b> add {@link AuditIchraUncoded}'s ICHRA gate: that gate is specific to the first
 * check's ICHRA-only content, and this check also covers health FSA purses.
 */
@WebServlet(name = "AuditFundedPurse", value = "/AuditFundedPurse")
public class AuditFundedPurse extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

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

        FundedPurseNoDisbursementCheck check = findCheck(auditService);
        if (check == null) {
            writePlainError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The funded-purse check is not registered on this installation.");
            return;
        }

        FundedPurseNoDisbursementCheck.Snapshot snapshot = check.readLive();

        request.setAttribute("snapshotError", snapshot.error());
        request.setAttribute("exportFileName", snapshot.fileName());
        request.setAttribute("exportTimestampDisplay", formatDisplay(snapshot.fileTimestamp()));
        request.setAttribute("exportAgeHoursDisplay", ageHoursDisplay(snapshot.fileTimestamp()));
        request.setAttribute("evaluationMonthDisplay", formatMonth(snapshot.evaluationMonth()));
        request.setAttribute("ignoredTypeCount", snapshot.ignoredTypeCount());
        request.setAttribute("ignoredTypeValues", snapshot.ignoredTypeValues());
        request.setAttribute("findings", snapshot.findings());

        request.setAttribute("pageTitle", "Funded Purses With No Disbursement");
        request.setAttribute("pageIcon", "bi-bell");
        request.getRequestDispatcher("/WEB-INF/view/a/admin/auditFundedPurse25.jsp").forward(request, response);
    }

    private FundedPurseNoDisbursementCheck findCheck(AuditService auditService) {
        for (AuditCheck check : auditService.getRegisteredChecks()) {
            if (check instanceof FundedPurseNoDisbursementCheck fundedCheck) {
                return fundedCheck;
            }
        }
        return null;
    }

    private static String formatDisplay(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_FORMAT);
    }

    private static String formatMonth(YearMonth value) {
        return value == null ? "—" : value.format(MONTH_FORMAT);
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

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}

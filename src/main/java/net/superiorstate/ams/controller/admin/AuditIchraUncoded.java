package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.service.audit.AuditCheck;
import net.superiorstate.ams.data.service.audit.AuditService;
import net.superiorstate.ams.data.service.audit.IchraUncodedParticipantsCheck;
import net.superiorstate.ams.model.general.PSP;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * T237 — detail page for {@link IchraUncodedParticipantsCheck}, the audit framework's first
 * check. Reads the export live, in-request, exactly the way {@code SummitResponseServlet}
 * fetches and renders a response file — nothing here is stored, and nothing is logged beyond
 * what {@link AuditService} already recorded as a count (LA-40).
 * <p>
 * ICHRA-gated on top of PSP admin because this page's content (the coded value to set, the
 * send-then-code instructions) only makes sense for ICHRA — unlike {@link AuditHub}, which is
 * AMS-wide.
 */
@WebServlet(name = "AuditIchraUncoded", value = "/AuditIchraUncoded")
public class AuditIchraUncoded extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** One rendered row — the check's {@code Row} plus the coded value to set, computed here
     *  (not in the check) because it depends on {@code SUMMIT_TPA_ID_PREFIX}, a display-time
     *  concern the check itself has no reason to know about. */
    public static final class DisplayRow {
        private final IchraUncodedParticipantsCheck.Row row;
        private final String codedValue;

        DisplayRow(IchraUncodedParticipantsCheck.Row row, String codedValue) {
            this.row = row;
            this.codedValue = codedValue;
        }

        public String getEmployerName() { return row.employerName(); }
        public String getEmployerKey() { return row.employerKey(); }
        public String getFirstName() { return row.firstName(); }
        public String getLastName() { return row.lastName(); }
        public String getParticipantId() { return row.participantId(); }
        public String getCodedValue() { return codedValue; }
    }

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

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } finally {
            if (em.isOpen()) em.close();
        }

        IchraUncodedParticipantsCheck check = findCheck(auditService);
        if (check == null) {
            writePlainError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The ICHRA uncoded-participants check is not registered on this installation.");
            return;
        }

        IchraUncodedParticipantsCheck.Snapshot snapshot = check.readLive();

        String prefix = AppConfig.get("SUMMIT_TPA_ID_PREFIX");
        boolean prefixConfigured = prefix != null && !prefix.isBlank();

        List<DisplayRow> displayRows = new ArrayList<>();
        if (snapshot.error() == null && prefixConfigured) {
            for (IchraUncodedParticipantsCheck.Row row : snapshot.findings()) {
                displayRows.add(new DisplayRow(row, prefix.trim() + "-S-" + row.participantId()));
            }
        }

        request.setAttribute("snapshotError", snapshot.error());
        request.setAttribute("prefixConfigured", prefixConfigured);
        request.setAttribute("exportFileName", snapshot.fileName());
        request.setAttribute("exportTimestampDisplay", formatDisplay(snapshot.fileTimestamp()));
        request.setAttribute("exportAgeHoursDisplay", ageHoursDisplay(snapshot.fileTimestamp()));
        request.setAttribute("displayRows", displayRows);

        request.setAttribute("pageTitle", "ICHRA Participants Without a Custom ID");
        request.setAttribute("pageIcon", "bi-bell");
        request.getRequestDispatcher("/WEB-INF/view/a/admin/auditIchraUncoded25.jsp").forward(request, response);
    }

    private IchraUncodedParticipantsCheck findCheck(AuditService auditService) {
        for (AuditCheck check : auditService.getRegisteredChecks()) {
            if (check instanceof IchraUncodedParticipantsCheck ichraCheck) {
                return ichraCheck;
            }
        }
        return null;
    }

    private static String formatDisplay(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_FORMAT);
    }

    private static String ageHoursDisplay(LocalDateTime fileTimestamp) {
        if (fileTimestamp == null) return "—";
        long hours = Duration.between(fileTimestamp, LocalDateTime.now()).toHours();
        return hours + "h";
    }

    /** Same pattern as {@code SummitResponseServlet.resolveCurrentPspId}. */
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

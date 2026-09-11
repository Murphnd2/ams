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
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.service.audit.AuditCheck;
import net.superiorstate.ams.data.service.audit.AuditService;
import net.superiorstate.ams.model.audit.AuditRun;
import net.superiorstate.ams.model.general.PSP;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * T237 — PSP Admin audit hub. Lists every registered {@link AuditCheck} with its latest stored
 * result and a "Run now" trigger. AMS-wide, not ICHRA-gated — the framework is meant to carry
 * checks beyond the ICHRA one, so this page gates on PSP admin only (mirrors
 * {@code RateCacheAdmin}'s own gate shape); {@link AuditIchraUncoded} adds the ICHRA gate on top
 * for its one check's detail page.
 */
@WebServlet(name = "AuditHub", value = "/AuditHub")
public class AuditHub extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Display string for JSP rendering — never format a java.time value in a JSP taglib. */
    private static String formatDisplay(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_FORMAT);
    }

    /** One row of the hub table — a check paired with its latest stored run, if any. */
    public static final class CheckRow {
        private final AuditCheck check;
        private final AuditRun latestRun;

        CheckRow(AuditCheck check, AuditRun latestRun) {
            this.check = check;
            this.latestRun = latestRun;
        }

        public AuditCheck getCheck() { return check; }
        public AuditRun getLatestRun() { return latestRun; }
        public boolean isEverRun() { return latestRun != null; }
        public String getLastRunDisplay() { return latestRun == null ? "—" : formatDisplay(latestRun.getRunAt()); }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isPspAdmin(session)) {
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
        String schedulerEnabledConstant;
        try {
            schedulerEnabledConstant = AppConstantDAO.getConstantValue(em, "AUDIT_SCHEDULER_ENABLED");
        } finally {
            if (em.isOpen()) em.close();
        }

        Map<String, AuditRun> latest = auditService.getLatestResults();
        List<CheckRow> rows = new ArrayList<>();
        for (AuditCheck check : auditService.getRegisteredChecks()) {
            rows.add(new CheckRow(check, latest.get(check.key())));
        }

        request.setAttribute("checkRows", rows);
        request.setAttribute("runInProgress", auditService.isRunInProgress());
        request.setAttribute("schedulerEnabled", "true".equalsIgnoreCase(schedulerEnabledConstant));

        request.setAttribute("pageTitle", "Audit Hub");
        request.setAttribute("pageIcon", "bi-bell");
        request.getRequestDispatcher("/WEB-INF/view/a/admin/auditHub25.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isPspAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        AuditService auditService = (AuditService) getServletContext().getAttribute("auditService");
        if (auditService != null) {
            Long sessionPspId = resolveCurrentPspId(request);
            if (sessionPspId == null || !sessionPspId.equals(auditService.getPspId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if ("runnow".equals(request.getParameter("action"))) {
                AuditService.TriggerResult result = auditService.triggerManual();
                session.setAttribute("auditMessage", result == AuditService.TriggerResult.STARTED
                        ? "Audit run started." : "An audit run is already in progress.");
            }
        }

        response.sendRedirect(request.getContextPath() + "/AuditHub");
    }

    private boolean isPspAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isPspAdmin"));
    }

    /** Same pattern as {@code SummitResponseServlet.resolveCurrentPspId}: reads
     *  {@code local.getCurrentPerson().getPsp()} off the session. */
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

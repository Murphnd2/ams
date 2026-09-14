package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.service.SummitRefreshService;
import net.superiorstate.ams.model.audit.AuditRun;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * S61-P6 -- status and "Run now" for {@link SummitRefreshService}, the scheduled J1 employer
 * refresh. Reachable by URL only: no nav entry, no menu link, no JSP -- a link visible to any agent
 * would be a defect. PSP-admin only, plus the same ICHRA availability check {@code SummitLinkServlet}
 * asks through {@link IchraAccessResolver}; no LOS, ServiceItem, PlanType, ServiceModule or
 * RateTable id is hardcoded here.
 * <p>
 * {@code GET} renders whether the scheduler is running, its interval, the configured prefix and
 * export directory, the expected filename shape, and the last recorded {@code audit_run} row.
 * {@code POST} triggers one refresh on the service's own executor (never on the request thread)
 * and redirects back here; two rapid clicks cannot overlap because the service's {@code running}
 * guard rejects the second with {@code ALREADY_RUNNING}. Every refusal is a visible HTML page.
 */
@WebServlet(name = "SummitRefreshServlet", value = "/SummitRefresh")
public class SummitRefreshServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitRefreshServlet.class);

    private static final String PAGE_CSS =
            "body{font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;" +
            "max-width:720px;margin:3rem auto;padding:0 1.5rem;color:#212529}" +
            "h1{font-size:1.15rem;margin-bottom:0.75rem}" +
            "p{line-height:1.5}" +
            "table{border-collapse:collapse;margin:1rem 0}" +
            "th,td{text-align:left;padding:0.25rem 0.75rem 0.25rem 0;vertical-align:top}" +
            "th{font-weight:600;white-space:nowrap}" +
            "code{background:#f1f3f5;padding:0.1rem 0.3rem;border-radius:3px}" +
            ".muted{color:#6c757d}" +
            ".notice{background:#e7f1ff;border:1px solid #b6d4fe;padding:0.5rem 0.75rem;border-radius:4px}" +
            "button{padding:0.4rem 0.9rem;border:1px solid #6c757d;background:#fff;border-radius:4px;cursor:pointer}";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            SummitRefreshService service = gate(request, response);
            if (service == null) return;

            StringBuilder body = new StringBuilder();
            body.append("<h1>Summit J1 employer refresh</h1>");

            String triggered = request.getParameter("triggered");
            if ("STARTED".equals(triggered)) {
                body.append("<p class=\"notice\">Refresh started on the background worker. Reload this page for the result.</p>");
            } else if ("ALREADY_RUNNING".equals(triggered)) {
                body.append("<p class=\"notice\">A refresh is already in progress; nothing new was started.</p>");
            }

            String prefix = service.getPrefix();
            String exportDir = service.getExportDir();

            body.append("<table>");
            row(body, "Scheduler", service.isScheduled()
                    ? "running — every " + service.getIntervalMinutes() + " min"
                    : "off — set the <code>SUMMIT_REFRESH_ENABLED</code> constant to <code>true</code> and restart to enable; Run now still works");
            row(body, "In progress", service.isRunInProgress() ? "yes" : "no");
            row(body, "Export directory", exportDir != null
                    ? "<code>" + escape(exportDir) + "</code>"
                    : "<span class=\"muted\">not derivable — <code>" + SummitRefreshService.CFG_IMPORT_DIR
                            + "</code> must end in <code>ImportFiles</code></span>");
            row(body, "Filename prefix", "<code>" + escape(prefix) + "</code> (<code>"
                    + SummitRefreshService.CFG_PREFIX + "</code>)");
            row(body, "Expected filename", "<code>" + escape(SummitRefreshService.expectedFilenameExample(prefix))
                    + "</code> — prefix, then Summit's own <code>_Export_</code> + 17-digit timestamp + extension");
            row(body, "Max age", service.getMaxAgeHours() + " h (<code>" + SummitRefreshService.CFG_MAX_AGE_HOURS + "</code>)");
            row(body, "Byte cap", service.getMaxBytes() + " (<code>" + SummitRefreshService.CFG_MAX_BYTES + "</code>)");
            body.append("</table>");

            body.append("<h1>Last recorded run</h1>");
            AuditRun last = service.getLatestRun();
            if (last == null) {
                body.append("<p class=\"muted\">Never run.</p>");
            } else {
                body.append("<table>");
                row(body, "At", escape(String.valueOf(last.getRunAt())));
                row(body, "Trigger", escape(last.getRunTrigger()));
                row(body, "Status", escape(last.getStatus()));
                row(body, "Summary", last.getSummary() != null ? escape(last.getSummary()) : "<span class=\"muted\">—</span>");
                row(body, "Error", last.getError() != null ? escape(last.getError()) : "<span class=\"muted\">—</span>");
                row(body, "Duration", last.getDurationMs() != null ? last.getDurationMs() + " ms" : "<span class=\"muted\">—</span>");
                body.append("</table>");
            }

            body.append("<form method=\"post\" action=\"").append(escape(request.getContextPath()))
                    .append("/SummitRefresh\"><button type=\"submit\">Run now</button></form>");
            body.append("<p class=\"muted\">J1 only. The importer skips Inactive rows and re-merges rows with blank "
                    + "email/phone/contact on every run, so a non-zero \"updated\" count is not evidence of change.</p>");

            renderPage(response, "Summit J1 employer refresh", body.toString());
        } catch (Exception e) {
            log.warn("[SUMMIT-REFRESH] status page failed: {}", e.getMessage());
            renderFailure(response, "Summit refresh unavailable",
                    "Something went wrong rendering this page. Please try again.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            SummitRefreshService service = gate(request, response);
            if (service == null) return;

            SummitRefreshService.TriggerResult result = service.triggerManual();
            response.sendRedirect(request.getContextPath() + "/SummitRefresh?triggered=" + result.name());
        } catch (Exception e) {
            log.warn("[SUMMIT-REFRESH] manual trigger failed: {}", e.getMessage());
            renderFailure(response, "Summit refresh unavailable",
                    "Something went wrong starting the refresh. Please try again.");
        }
    }

    /**
     * PSP-admin gate, then the ICHRA availability check through the single existing resolver, then
     * the service lookup. Returns {@code null} after rendering a visible refusal page.
     */
    private SummitRefreshService gate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();

        boolean isPspAdmin = Boolean.TRUE.equals(session.getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            renderFailure(response, "Access restricted", "This page is available to PSP admins only.");
            return null;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            renderFailure(response, "Summit refresh unavailable", "The application is not fully initialized.");
            return null;
        }
        EntityManager em = emf.createEntityManager();
        try {
            if (!IchraAccessResolver.isAvailable(em, request)) {
                renderFailure(response, "Access restricted", "ICHRA access is not available for your PSP.");
                return null;
            }
        } finally {
            if (em.isOpen()) em.close();
        }

        SummitRefreshService service = (SummitRefreshService) getServletContext().getAttribute("summitRefreshService");
        if (service == null) {
            renderFailure(response, "Summit refresh unavailable",
                    "The refresh service did not initialize on this installation. Check catalina.out for "
                            + "\"Summit refresh failed to initialize\".");
            return null;
        }
        return service;
    }

    // ── Rendering ───────────────────────────────────────────────────

    private static void row(StringBuilder sb, String label, String valueHtml) {
        sb.append("<tr><th>").append(escape(label)).append("</th><td>").append(valueHtml).append("</td></tr>");
    }

    private static void renderPage(HttpServletResponse response, String title, String bodyHtml) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
                "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + escape(title)
                        + "</title><style>" + PAGE_CSS + "</style></head><body>"
                        + bodyHtml + "</body></html>");
    }

    private static void renderFailure(HttpServletResponse response, String title, String message) throws IOException {
        String body = "<h1>" + escape(title) + "</h1><p>" + escape(message) + "</p>";
        renderPage(response, title, body);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

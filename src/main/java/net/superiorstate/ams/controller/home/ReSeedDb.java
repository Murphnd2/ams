package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.service.DatabaseResetUtil;
import net.superiorstate.ams.data.service.DatabaseResetUtil.SavedState;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Factory-reset servlet: captures initialization state, truncates all tables,
 * and re-initializes the database from saved values. Admin credentials are preserved.
 *
 * Security: PSP Admin session + deployment key (from ssa.properties).
 *
 * GET  → Confirmation page with deployment key input
 * POST → Executes the reset
 */
@WebServlet(name = "ReSeedDb", value = "/ReSeedDb")
public class ReSeedDb extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "PSP Admin access required");
            return;
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        renderConfirmationPage(out, getTitle(), getWarningMessage(), getServletPath());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "PSP Admin access required");
            return;
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        renderPageHeader(out, getTitle());

        // Validate deployment key
        String submittedKey = request.getParameter("deploymentKey");
        String expectedKey = AppConfig.get("DEPLOYMENT_KEY");

        if (expectedKey == null || expectedKey.isBlank()) {
            out.println("<div class='alert alert-danger'>No DEPLOYMENT_KEY configured in ssa.properties.</div>");
            renderPageFooter(out);
            return;
        }
        if (submittedKey == null || !expectedKey.equals(submittedKey.trim())) {
            out.println("<div class='alert alert-danger'>Invalid deployment key.</div>");
            out.println("<p><a href='" + getServletPath() + "' class='btn btn-outline-primary btn-sm'>Try Again</a></p>");
            renderPageFooter(out);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            em = executeReset(em, out);
            renderSuccessMessage(out);

            // Invalidate session so stale AmsDataLocal (cached checklists, activities,
            // filters) is discarded.  The success page is already rendered above;
            // when the user clicks "Go to Home", LoginFilter will require a fresh login
            // which re-initializes all session-scoped data.
            request.getSession().invalidate();
        } catch (Exception e) {
            if (em.isOpen() && em.getTransaction().isActive()) em.getTransaction().rollback();
            out.println("<div class='alert alert-danger'><strong>Error:</strong> "
                    + escapeHtml(e.getMessage()) + "</div>");
            e.printStackTrace();
        } finally {
            if (em.isOpen()) em.close();
        }

        renderPageFooter(out);
    }

    /**
     * Core reset logic — can be called by subclasses (ReSeedDemoData).
     * Returns the fresh EntityManager created after truncation (the original is closed).
     */
    protected EntityManager executeReset(EntityManager em, PrintWriter out) {
        // 1. Capture current initialization state
        SavedState state = DatabaseResetUtil.captureInitState(em, out);

        // 2. Clear all tables
        DatabaseResetUtil.clearAllTables(em, out);

        // Close the EM that performed native SQL truncation — EclipseLink's internal
        // identity maps get confused when the same EM is reused after bulk native deletes.
        em.close();

        // 3. Re-initialize from saved state using a fresh EntityManager
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        DatabaseResetUtil.evictEntityCaches(emf);  // Per-class L2 eviction (evictAll corrupts EclipseLink descriptors)
        EntityManager freshEm = emf.createEntityManager();

        DatabaseResetUtil.reinitialize(freshEm, state, out);

        // 4. Reload global application state
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        DatabaseResetUtil.reloadGlobals(freshEm, global, out);

        return freshEm;
    }

    // ═══════════════════════════════════════════════════════════════
    //  OVERRIDABLE LABELS (for ReSeedDemoData to customize)
    // ═══════════════════════════════════════════════════════════════

    protected String getTitle() {
        return "Reset Database";
    }

    protected String getWarningMessage() {
        return "This will <strong>delete all data</strong> (activities, employees, employers, benefits, billing) "
                + "and re-initialize the database with only the original setup values (PSP info, admin account, "
                + "reference data). Your admin login credentials will be preserved.";
    }

    protected String getServletPath() {
        return "ReSeedDb";
    }

    protected void renderSuccessMessage(PrintWriter out) {
        out.println("<hr>");
        out.println("<div class='alert alert-success'><strong>Database reset complete.</strong> "
                + "The database has been re-initialized with your original setup values.</div>");
        out.println("<p><a href='ViewHome25' class='btn btn-primary btn-sm'>Go to Home</a></p>");
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECURITY
    // ═══════════════════════════════════════════════════════════════

    protected boolean isAdmin(HttpServletRequest request) {
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return isPspAdmin != null && isPspAdmin;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HTML RENDERING
    // ═══════════════════════════════════════════════════════════════

    protected void renderConfirmationPage(PrintWriter out, String title, String warning, String action) {
        out.println("<html><head><title>" + title + "</title>"
                + "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>"
                + "</head><body class='p-4' style='max-width:700px; margin:auto;'>");
        out.println("<h2 style='color:#0d5681;'>" + title + "</h2><hr>");
        out.println("<div class='alert alert-warning'>" + warning + "</div>");
        out.println("<form method='POST' action='" + action + "'>");
        out.println("<div class='mb-3'>");
        out.println("<label for='deploymentKey' class='form-label'><strong>Deployment Key</strong></label>");
        out.println("<input type='password' class='form-control' id='deploymentKey' name='deploymentKey' "
                + "placeholder='Enter deployment key from ssa.properties' required style='max-width:400px;'>");
        out.println("</div>");
        out.println("<button type='submit' class='btn btn-danger'>" + title + "</button>");
        out.println("&nbsp;<a href='ViewHome25' class='btn btn-outline-secondary'>Cancel</a>");
        out.println("</form>");
        out.println("</body></html>");
    }

    protected void renderPageHeader(PrintWriter out, String title) {
        out.println("<html><head><title>" + title + "</title>"
                + "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>"
                + "</head><body class='p-4' style='max-width:900px; margin:auto;'>");
        out.println("<h2 style='color:#0d5681;'>" + title + "</h2><hr>");
    }

    protected void renderPageFooter(PrintWriter out) {
        out.println("</body></html>");
    }

    protected String escapeHtml(String text) {
        if (text == null) return "null";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.service.DatabaseResetUtil;
import net.superiorstate.ams.data.service.DemoDataSeeder;
import net.superiorstate.ams.model.Constant;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Conference Demo Data Seeder
 *
 * Creates purpose-built demo data for live demonstrations at demo.superiorstate.biz.
 * Delegates all seeding logic to {@link DemoDataSeeder#seedConferenceDemo(EntityManager, EntityManagerFactory)}.
 *
 * Idempotent: checks DEMO_DATA_SEEDED constant before running.
 * Access: requires authenticated PSP Admin session.
 */
@WebServlet(name = "SeedDemoData", value = "/SeedDemoData")
public class SeedDemoData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Admin guard — PSP Admin or BPO Admin
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        Boolean isBpoAdmin = (Boolean) request.getSession().getAttribute("isBpoAdmin");
        if (!Boolean.TRUE.equals(isPspAdmin) && !Boolean.TRUE.equals(isBpoAdmin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Demo Data Seeder</title>"
                + "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>"
                + "</head><body class='p-4' style='max-width:900px; margin:auto;'>");
        out.println("<h2 style='color:#0d5681;'>Conference Demo Data Seeder</h2><hr>");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Check if already seeded
            if (isAlreadySeeded(em)) {
                out.println("<div class='alert alert-warning'>Demo data has already been seeded. "
                        + "To re-seed, use <code>/ReSeedDemoData</code> for a full reset.</div>");
                out.println("<p><a href='ViewHome25' class='btn btn-outline-primary btn-sm'>Back to Home</a></p>");
                out.println("</body></html>");
                return;
            }

            // Sync SEQUENCE table past max ASSIGNEE ID and reset in-memory cache
            em.close();
            DatabaseResetUtil.syncAssigneeSequence(emf, out);
            em = emf.createEntityManager();

            seedAllDemoData(em, emf, out);

            // Reload global data so new users/service items appear in dropdowns
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            global.initializeGlobalData(em);

            out.println("<hr>");
            out.println("<div class='alert alert-success'>"
                    + "<strong>Demo data seeded successfully!</strong></div>");

            out.println("<p><a href='ViewHome25' class='btn btn-primary btn-sm'>Go to Home</a></p>");

            // Invalidate session so stale AmsDataLocal (cached activities, checklists)
            // is discarded.  LoginFilter will require a fresh login on next page load.
            request.getSession().invalidate();

        } catch (Exception e) {
            if (em.isOpen() && em.getTransaction().isActive()) em.getTransaction().rollback();
            out.println("<div class='alert alert-danger'><strong>Error:</strong> " + escapeHtml(e.getMessage()) + "</div>");
            e.printStackTrace();
        } finally {
            if (em.isOpen()) em.close();
        }

        out.println("</body></html>");
    }

    /**
     * Core seeding logic — callable from both the doGet flow and ReSeedDemoData.
     * Delegates to DemoDataSeeder for all demo entity creation.
     */
    public void seedAllDemoData(EntityManager em, EntityManagerFactory emf, PrintWriter out) {
        DemoDataSeeder.seedConferenceDemo(em, emf);
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEED GUARD
    // ═══════════════════════════════════════════════════════════════

    private boolean isAlreadySeeded(EntityManager em) {
        try {
            Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = 'DEMO_DATA_SEEDED'");
            return !q.getResultList().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════════════

    private String escapeHtml(String text) {
        if (text == null) return "null";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

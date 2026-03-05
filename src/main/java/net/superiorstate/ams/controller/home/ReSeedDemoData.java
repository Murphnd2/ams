package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import net.superiorstate.ams.data.AmsDataGlobal;

import java.io.PrintWriter;

/**
 * Factory-reset + demo data: performs a full database reset (via ReSeedDb),
 * then seeds conference demo data (via SeedDemoData).
 *
 * Security: PSP Admin session + deployment key (from ssa.properties).
 *
 * GET  → Confirmation page with deployment key input
 * POST → Resets database, then seeds demo data
 *
 * Demo credentials after reset:
 *   jmartinez@superiorstate.net / demo123  (PSP User)
 *   arivera@accelvantage.com / demo123     (BPO Admin)
 *   psharma@accelvantage.com / demo123     (BPO User)
 */
@WebServlet(name = "ReSeedDemoData", value = "/ReSeedDemoData")
public class ReSeedDemoData extends ReSeedDb {

    @Override
    protected String getTitle() {
        return "Reset + Seed Demo Data";
    }

    @Override
    protected String getWarningMessage() {
        return "This will <strong>delete all data</strong>, re-initialize the database with your original "
                + "setup values, and then seed conference demo data (LOS/Enhancement setup, rate manager, "
                + "resource library, sequences, employers, renewals, tickets, opportunities, and more). "
                + "Your admin login credentials will be preserved.";
    }

    @Override
    protected String getServletPath() {
        return "ReSeedDemoData";
    }

    @Override
    protected EntityManager executeReset(EntityManager em, PrintWriter out) {
        // First: full database reset (capture → clear → reinitialize)
        // Returns a fresh EM (the original is closed after truncation)
        EntityManager initEm = super.executeReset(em, out);
        initEm.close();

        // Create another fresh EM for demo seeding — the init EM has managed
        // entities (Person 104, CheckList 29, Ticket 99) whose cross-references
        // confuse EclipseLink when new Activity subclasses are persisted.
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager seedEm = emf.createEntityManager();

        out.println("<h5 class='mt-3' style='color:#0d5681;'>Seeding Demo Data</h5>");
        SeedDemoData seeder = new SeedDemoData();
        seeder.seedAllDemoData(seedEm, out);

        // Reload global data so new BPO users/service items appear in dropdowns
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        global.initializeGlobalData(seedEm);

        return seedEm;
    }

    @Override
    protected void renderSuccessMessage(PrintWriter out) {
        out.println("<hr>");
        out.println("<div class='alert alert-success'><strong>Database reset + demo data seeded!</strong></div>");
        out.println("<p><a href='ViewHome25' class='btn btn-primary btn-sm'>Go to Home</a></p>");
    }
}

package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.service.MonthlyBiller;
import net.superiorstate.ams.data.util.BillingHelper;
import net.superiorstate.ams.model.billing.BillingMonth;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;

/**
 * Diagnostic billing servlet — runs each MonthlyBiller step individually
 * with real-time progress streaming to the browser.
 *
 * URL: /CreateBillingDiag
 * Location: src/main/java/net/superiorstate/ams/controller/monthly/CreateBillingDiag.java
 *
 * Purpose: Identify which billing step is failing or producing zero rows.
 * Calls public step_XX() methods on MonthlyBiller to avoid protected access issues.
 */
@WebServlet(name = "CreateBillingDiag", value = "/CreateBillingDiag")
public class CreateBillingDiag extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        runBillingDiag(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        runBillingDiag(request, response);
    }

    private void runBillingDiag(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html><html><head>");
        out.println("<title>Billing Diagnostic</title>");
        out.println("<link href='https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;700&display=swap' rel='stylesheet'>");
        out.println("<style>");
        out.println("body { font-family: 'DM Sans', sans-serif; background: #f5f7fa; padding: 30px; color: #333; }");
        out.println(".container { max-width: 900px; margin: 0 auto; }");
        out.println("h2 { color: #0d5681; margin-bottom: 20px; }");
        out.println(".step { padding: 10px 16px; margin: 4px 0; border-radius: 6px; font-size: 14px; }");
        out.println(".ok { background: #d4edda; color: #155724; }");
        out.println(".fail { background: #f8d7da; color: #721c24; }");
        out.println(".info { background: #d1ecf1; color: #0c5460; }");
        out.println(".warn { background: #fff3cd; color: #856404; }");
        out.println(".count { font-weight: 700; }");
        out.println("pre { background: #2d2d2d; color: #f8f8f2; padding: 16px; border-radius: 6px; overflow-x: auto; font-size: 12px; max-height: 300px; }");
        out.println(".done { margin-top: 20px; padding: 16px; background: #0d5681; color: white; border-radius: 8px; text-align: center; }");
        out.println(".done a { color: #87a948; font-weight: 700; text-decoration: none; }");
        out.println("</style>");
        out.println("</head><body><div class='container'>");
        out.println("<h2>&#x1F4CA; Billing Diagnostic Run</h2>");
        out.flush();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        int stepNum = 0;
        int totalSteps = 14;
        boolean aborted = false;

        try {
            MonthlyBiller biller = new MonthlyBiller(em);
            Date monthFor = BillingHelper.getMonthFor();
            log(out, "info", "Billing month target: <span class='count'>" + monthFor + "</span>");

            // --- Pre-run counts ---
            long enrollmentsBefore = countTable(em, "ImportEnrollment", "TRIM(LOWER(ie.planStatus)) = 'active'", "ie");
            long employersBefore = countTable(em, "Employer", "er.id > 0", "er");
            long benefitsBefore = countTable(em, "Benefit", "b.isActive = true", "b");
            log(out, "info", "Pre-run: <span class='count'>" + enrollmentsBefore + "</span> active ImportEnrollments, "
                    + "<span class='count'>" + employersBefore + "</span> employers, "
                    + "<span class='count'>" + benefitsBefore + "</span> active benefits");
            out.flush();

            if (enrollmentsBefore == 0 && employersBefore == 0) {
                log(out, "warn", "&#x26A0; No import data found. Has a Summit data import been run? Billing needs ImportEnrollment + Employer data to produce results.");
            }

            // Step 1: setBillingFlags
            stepNum = 1;
            step(out, stepNum, totalSteps, "setBillingFlags", () -> biller.step_setBillingFlags());

            // Step 2: createBillingMonth
            stepNum = 2;
            step(out, stepNum, totalSteps, "createBillingMonth", () -> biller.step_createBillingMonth());
            BillingMonth bm = biller.step_getBillingMonthByDate(monthFor);
            log(out, bm.getMonthId() > 0 ? "ok" : "warn",
                    "BillingMonth ID: <span class='count'>" + bm.getMonthId() + "</span>");

            // Step 3: clearBillingEnrollmentTable
            stepNum = 3;
            step(out, stepNum, totalSteps, "clearBillingEnrollmentTable", () -> biller.step_clearBillingEnrollmentTable());

            // Step 4: fillBillingEnrollmentTable
            stepNum = 4;
            step(out, stepNum, totalSteps, "fillBillingEnrollmentTable", () -> biller.step_fillBillingEnrollmentTable());
            long enrollment2Count = countTable(em, "Enrollment2", null, null);
            log(out, enrollment2Count > 0 ? "ok" : "warn",
                    "Enrollment2 rows after fill: <span class='count'>" + enrollment2Count + "</span>");
            if (enrollment2Count == 0) {
                log(out, "warn", "&#x26A0; Zero Enrollment2 rows. Check: Are ImportEnrollment records present with planStatus='active'? Do they have valid ImportBenefitYear and benefit IDs?");
            }

            // Step 5: clearBillingCoverageTable
            stepNum = 5;
            step(out, stepNum, totalSteps, "clearBillingCoverageTable", () -> biller.step_clearBillingCoverageTable());

            // Step 6: clearCoverageStatusForMonth
            stepNum = 6;
            step(out, stepNum, totalSteps, "clearCoverageStatusForMonth", () -> biller.step_clearCoverageStatusForMonth());

            // Step 7: fillBillingCoverageTableAlt(0)
            stepNum = 7;
            step(out, stepNum, totalSteps, "fillBillingCoverageTableAlt(0)", () -> biller.step_fillBillingCoverageTableAlt());
            long coverageCount = countTable(em, "Coverage", "c.isBillable = true", "c");
            log(out, coverageCount > 0 ? "ok" : "warn",
                    "Coverage (billable) rows: <span class='count'>" + coverageCount + "</span>");

            // Step 8: logCoverageStatusForThisMonthCDH
            stepNum = 8;
            step(out, stepNum, totalSteps, "logCoverageStatusForThisMonthCDH", () -> biller.step_logCoverageStatusCDH());
            long csCountAfterCDH = countCoverageStatus(em, monthFor);
            log(out, "info", "CoverageStatus rows after CDH: <span class='count'>" + csCountAfterCDH + "</span>");

            // Step 9: logCoverageStatusForThisMonthPB(0)
            stepNum = 9;
            step(out, stepNum, totalSteps, "logCoverageStatusForThisMonthPB(0)", () -> biller.step_logCoverageStatusPB());
            long csCountAfterPB = countCoverageStatus(em, monthFor);
            log(out, "info", "CoverageStatus rows after PB: <span class='count'>" + csCountAfterPB + "</span>");

            if (csCountAfterPB == 0) {
                log(out, "warn", "&#x26A0; Zero CoverageStatus rows. The billing grid will be empty. Check Enrollment2 benefit lookups and employee matching.");
            }

            // Step 10: clearBillingGridForMonth
            stepNum = 10;
            step(out, stepNum, totalSteps, "clearBillingGridForMonth", () -> biller.step_clearBillingGridForMonth());

            // Step 11: fillBillingGrid(0)
            stepNum = 11;
            step(out, stepNum, totalSteps, "fillBillingGrid(0)", () -> biller.step_fillBillingGrid());
            long gridCount = countBillingGrid(em, monthFor);
            log(out, gridCount > 0 ? "ok" : "warn",
                    "BillingGrid rows: <span class='count'>" + gridCount + "</span>");

            // Step 12: fillHsaBillingGrid
            stepNum = 12;
            step(out, stepNum, totalSteps, "fillHsaBillingGrid", () -> biller.step_fillHsaBillingGrid());

            // Step 13: fillBillingLinks
            stepNum = 13;
            step(out, stepNum, totalSteps, "fillBillingLinks", () -> biller.step_fillBillingLinks());
            long linkCount = countTable(em, "BillingLink", null, null);
            log(out, "info", "Total BillingLink rows: <span class='count'>" + linkCount + "</span>");

            // Step 14: fillDualParticipantGrid
            stepNum = 14;
            step(out, stepNum, totalSteps, "fillDualParticipantGrid", () -> biller.step_fillDualParticipantGrid());

        } catch (Exception e) {
            aborted = true;
            log(out, "fail", "&#x274C; ABORTED at step " + stepNum + ": " + escapeHtml(e.getMessage()));
            out.println("<pre>" + escapeHtml(getStackTrace(e)) + "</pre>");
        } finally {
            if (em.isOpen()) {
                if (em.getTransaction().isActive()) {
                    try { em.getTransaction().rollback(); } catch (Exception ignored) {}
                }
                em.close();
            }
        }

        // Summary
        out.println("<div class='done'>");
        if (aborted) {
            out.println("&#x274C; Billing run aborted at step " + stepNum + " of " + totalSteps);
        } else {
            out.println("&#x2705; Billing run completed all " + totalSteps + " steps");
        }
        out.println("<br><br><a href='ResetBillingView'>&#x2192; Go to Billing Home</a>");
        out.println("</div>");
        out.println("</div></body></html>");
        out.flush();
    }

    private void step(PrintWriter out, int num, int total, String name, Runnable action) {
        out.flush();
        long start = System.currentTimeMillis();
        try {
            action.run();
            long elapsed = System.currentTimeMillis() - start;
            log(out, "ok", "[" + num + "/" + total + "] " + name + " — <span class='count'>" + elapsed + "ms</span>");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log(out, "fail", "[" + num + "/" + total + "] " + name + " — FAILED after " + elapsed + "ms: " + escapeHtml(e.getMessage()));
            out.println("<pre>" + escapeHtml(getStackTrace(e)) + "</pre>");
            throw new RuntimeException(e);
        }
        out.flush();
    }

    private void log(PrintWriter out, String cssClass, String message) {
        out.println("<div class='step " + cssClass + "'>" + message + "</div>");
        out.flush();
    }

    private long countTable(EntityManager em, String entity, String where, String alias) {
        try {
            String a = (alias != null) ? alias : "x";
            String jpql = "SELECT COUNT(" + a + ") FROM " + entity + " " + a;
            if (where != null && !where.isBlank()) {
                jpql += " WHERE " + where;
            }
            return em.createQuery(jpql, Long.class).getSingleResult();
        } catch (Exception e) {
            return -1;
        }
    }

    private long countCoverageStatus(EntityManager em, Date monthFor) {
        try {
            return em.createQuery(
                            "SELECT COUNT(cs) FROM CoverageStatus cs WHERE cs.monthFor = :mf", Long.class)
                    .setParameter("mf", monthFor)
                    .getSingleResult();
        } catch (Exception e) {
            return -1;
        }
    }

    private long countBillingGrid(EntityManager em, Date monthFor) {
        try {
            return em.createQuery(
                            "SELECT COUNT(bg) FROM BillingGrid bg WHERE bg.billingMonth.fullDate = :mf", Long.class)
                    .setParameter("mf", monthFor)
                    .getSingleResult();
        } catch (Exception e) {
            return -1;
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "(null)";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}

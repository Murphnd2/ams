package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.util.PathUtil;
import net.superiorstate.ams.model.billing.BillingRun;
import net.superiorstate.ams.model.billing.BillingRunStep;

import java.nio.file.Path;

/**
 * Background worker for the Monthly Billing Launcher. Runs WIPE -> IMPORT -> PROMOTE ->
 * CLEAR_BILLING -> CREATE_BILLING off-request (no HttpServletRequest/HttpSession on this
 * thread), recording per-step status to billing_run / billing_run_step via
 * BillingRunService so the launcher page can poll progress. In BILLING_ONLY mode, WIPE/
 * IMPORT/PROMOTE are recorded SKIPPED and only CLEAR_BILLING + CREATE_BILLING run.
 */
public class BillingPipelineRunner implements Runnable {

    private final EntityManagerFactory emf;
    private final AmsDataGlobal global;
    private final long runId;
    private final String mode;

    public BillingPipelineRunner(EntityManagerFactory emf, AmsDataGlobal global, long runId, String mode) {
        this.emf = emf;
        this.global = global;
        this.runId = runId;
        this.mode = mode;
    }

    @Override
    public void run() {
        boolean full = BillingRun.MODE_FULL.equals(mode);
        try {
            if (full) { if (!step(BillingRunStep.STEP_WIPE, this::doWipe)) return; }
            else BillingRunService.skipStep(emf, runId, BillingRunStep.STEP_WIPE);

            if (full) { if (!step(BillingRunStep.STEP_IMPORT, this::doImport)) return; }
            else BillingRunService.skipStep(emf, runId, BillingRunStep.STEP_IMPORT);

            if (full) { if (!step(BillingRunStep.STEP_PROMOTE, this::doPromote)) return; }
            else BillingRunService.skipStep(emf, runId, BillingRunStep.STEP_PROMOTE);

            if (!step(BillingRunStep.STEP_CLEAR_BILLING, this::doClearBilling)) return;
            if (!step(BillingRunStep.STEP_CREATE_BILLING, this::doCreateBilling)) return;

            BillingRunService.completeRun(emf, runId);
        } catch (Throwable t) {
            BillingRunService.failRun(emf, runId, describe(t));
        }
    }

    // ── Step runner ──────────────────────────────────────────────────

    private interface StepWork {
        String run() throws Exception;
    }

    private boolean step(String name, StepWork work) {
        BillingRunService.startStep(emf, runId, name);
        try {
            String detail = work.run();
            BillingRunService.completeStep(emf, runId, name, detail);
            return true;
        } catch (Throwable t) {
            BillingRunService.failStep(emf, runId, name, describe(t));
            return false;
        }
    }

    private static String describe(Throwable t) {
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    // ── Step bodies ──────────────────────────────────────────────────

    private String doWipe() {
        EntityManager em = emf.createEntityManager();
        try {
            new Cleaner(em) {}.wipeTables(Cleaner.MONTHLY_STAGING_TABLES);
            return null;
        } finally {
            em.close();
        }
    }

    private String doImport() throws Exception {
        Path uploadDir = PathUtil.resolveAndEnsureDir(
                null, "AMS_UPLOAD_DIR", "AMS_UPLOAD_DIR", "ams.upload.dir", "work/ams-uploads");
        Path processedDir = resolveProcessedDir(uploadDir);

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            try {
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            } catch (Exception ignore) {}

            Importer.importAllMatchingFilesInMappingOrder(em, uploadDir.toFile(), processedDir.toFile());

            try {
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            } catch (Exception ignore) {}
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }

        return "uploadDir=" + uploadDir;
    }

    /** Mirrors ImportCsvFiles25.resolveProcessedDir, minus the ServletContext context-param tier. */
    private Path resolveProcessedDir(Path uploadDir) {
        String v = System.getenv("AMS_PROCESSED_DIR");
        String configured = (v != null && !v.isBlank()) ? v : System.getProperty("ams.processed.dir");

        if (configured == null || configured.isBlank()) {
            return PathUtil.ensureDir(uploadDir.resolve("processed"));
        }

        Path p = PathUtil.resolveDir(
                null,
                "AMS_PROCESSED_DIR",
                "AMS_PROCESSED_DIR",
                "ams.processed.dir",
                "work/ams-uploads/processed" // not used because value is present, but harmless
        );
        return PathUtil.ensureDir(p);
    }

    private String doPromote() {
        EntityManager em = emf.createEntityManager();
        try {
            Updater.runMonthlyPromotion(em);
        } finally {
            em.close();
        }

        EntityManager em2 = emf.createEntityManager();
        try {
            global.miniUpdate(em2);
        } finally {
            em2.close();
        }

        return null;
    }

    private String doClearBilling() {
        EntityManager em = emf.createEntityManager();
        try {
            new MonthlyBiller(em).clearCurrentMonth();
            return null;
        } finally {
            em.close();
        }
    }

    private String doCreateBilling() {
        EntityManager em = emf.createEntityManager();
        try {
            MonthlyBiller biller = new MonthlyBiller(em);

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Set billing flags (1/14)");
            biller.step_setBillingFlags();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Create billing month (2/14)");
            biller.step_createBillingMonth();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear billing enrollment table (3/14)");
            biller.step_clearBillingEnrollmentTable();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing enrollment table (4/14)");
            biller.step_fillBillingEnrollmentTable();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear billing coverage table (5/14)");
            biller.step_clearBillingCoverageTable();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear coverage status for month (6/14)");
            biller.step_clearCoverageStatusForMonth();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing coverage table (7/14)");
            biller.step_fillBillingCoverageTableAlt();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Log coverage status CDH (8/14)");
            biller.step_logCoverageStatusCDH();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Log coverage status PB (9/14)");
            biller.step_logCoverageStatusPB();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear billing grid for month (10/14)");
            biller.step_clearBillingGridForMonth();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing grid (11/14)");
            biller.step_fillBillingGrid();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill HSA billing grid (12/14)");
            biller.step_fillHsaBillingGrid();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing links (13/14)");
            biller.step_fillBillingLinks();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill dual participant grid (14/14)");
            biller.step_fillDualParticipantGrid();

            return null;
        } finally {
            em.close();
        }
    }
}

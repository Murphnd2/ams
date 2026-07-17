package net.superiorstate.ams.data.service;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.util.PathUtil;
import net.superiorstate.ams.model.billing.BillingGrid;
import net.superiorstate.ams.model.billing.BillingItem;
import net.superiorstate.ams.model.billing.BillingLink;
import net.superiorstate.ams.model.billing.BillingRun;
import net.superiorstate.ams.model.billing.BillingRunStep;
import net.superiorstate.ams.model.summit.archive.CoverageStatus;
import net.superiorstate.ams.model.summit.temp.Coverage;
import net.superiorstate.ams.model.summit.temp.Enrollment2;

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
            // Clear any prior (e.g. failed) run's billing-entity residue from the shared
            // L2 cache before this run persists anything of its own — see evictBillingCaches().
            evictBillingCaches();

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

    // ── L2 cache ─────────────────────────────────────────────────────

    // SYNC-GUARD: EclipseLink caches every entity in the shared L2 cache by default (no
    // <shared-cache-mode>, no @Cacheable anywhere in model/ — opt-out default = ON). Nothing
    // in MonthlyBiller/Biller ever evicts it; em.clear() only clears the first-level (per-EM)
    // cache. Left unchecked, a CREATE_BILLING run accumulates thousands of Coverage/
    // Enrollment2/CoverageStatus/BillingGrid/BillingLink/BillingItem entities in shared
    // memory, and a prior FAILED run's entities stay resident into the next run — the
    // confirmed driver of the GC death-spiral OOM on a 1GB heap. MonthlyBiller/Biller/
    // CreateBilling25 (the legacy path) are intentionally left untouched — this evicts from
    // the worker side only, per-class (never evictAll() — corrupts EclipseLink descriptors).
    private void evictBillingCaches() {
        Cache cache = emf.getCache();
        if (cache == null) return;
        cache.evict(Coverage.class);
        cache.evict(Enrollment2.class);
        cache.evict(CoverageStatus.class);
        cache.evict(BillingGrid.class);
        cache.evict(BillingLink.class);
        cache.evict(BillingItem.class);
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

        evictBillingCaches();

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
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Create billing month (2/14)");
            biller.step_createBillingMonth();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear billing enrollment table (3/14)");
            biller.step_clearBillingEnrollmentTable();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing enrollment table (4/14)");
            biller.step_fillBillingEnrollmentTable();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear billing coverage table (5/14)");
            biller.step_clearBillingCoverageTable();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear coverage status for month (6/14)");
            biller.step_clearCoverageStatusForMonth();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing coverage table (7/14)");
            biller.step_fillBillingCoverageTableAlt();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Log coverage status CDH (8/14)");
            biller.step_logCoverageStatusCDH();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Log coverage status PB (9/14)");
            biller.step_logCoverageStatusPB();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Clear billing grid for month (10/14)");
            biller.step_clearBillingGridForMonth();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing grid (11/14)");
            biller.step_fillBillingGrid();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill HSA billing grid (12/14)");
            biller.step_fillHsaBillingGrid();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill billing links (13/14)");
            biller.step_fillBillingLinks();
            evictBillingCaches();

            BillingRunService.setCurrentStep(emf, runId, "CREATE_BILLING: Fill dual participant grid (14/14)");
            biller.step_fillDualParticipantGrid();
            evictBillingCaches();

            return null;
        } finally {
            em.close();
            evictBillingCaches();
        }
    }
}

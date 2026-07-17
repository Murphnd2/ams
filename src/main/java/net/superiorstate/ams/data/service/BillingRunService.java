package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.billing.BillingRun;
import net.superiorstate.ams.model.billing.BillingRunStep;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Status-tracking writes/reads for the Monthly Billing Launcher's billing_run /
 * billing_run_step tables. Every method opens its own short-lived EntityManager and
 * transaction, independent of whatever EntityManager the pipeline steps themselves are
 * using, so status updates are never blocked on or entangled with a pipeline step's
 * own long-running transaction.
 */
public abstract class BillingRunService {

    private static final List<String> STEP_ORDER = List.of(
            BillingRunStep.STEP_WIPE,
            BillingRunStep.STEP_IMPORT,
            BillingRunStep.STEP_PROMOTE,
            BillingRunStep.STEP_CLEAR_BILLING,
            BillingRunStep.STEP_CREATE_BILLING
    );

    public static boolean isRunActive(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(r) FROM BillingRun r WHERE r.status = :status", Long.class)
                    .setParameter("status", BillingRun.STATUS_RUNNING)
                    .getSingleResult();
            return count != null && count > 0;
        } finally {
            em.close();
        }
    }

    public static long createRun(EntityManagerFactory emf, String mode, Long launchedBy, boolean planTypeSupplied) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRun run = new BillingRun();
            run.setStatus(BillingRun.STATUS_RUNNING);
            run.setStartedAt(LocalDateTime.now());
            run.setMode(mode);
            run.setPlanTypeSupplied(planTypeSupplied);
            run.setRenewalsRefreshed(false);
            run.setLaunchedById(launchedBy);
            run.setCurrentStep(null);
            run.setCompletedAt(null);
            run.setErrorText(null);
            em.persist(run);
            em.flush(); // obtain the generated id

            for (String stepName : STEP_ORDER) {
                BillingRunStep step = new BillingRunStep();
                step.setRunId(run.getId());
                step.setStepName(stepName);
                step.setStatus(BillingRunStep.STATUS_PENDING);
                em.persist(step);
            }

            em.getTransaction().commit();
            evict(emf);
            return run.getId();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void startStep(EntityManagerFactory emf, long runId, String stepName) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRunStep step = findStep(em, runId, stepName);
            step.setStatus(BillingRunStep.STATUS_RUNNING);
            step.setStartedAt(LocalDateTime.now());
            em.merge(step);

            BillingRun run = em.find(BillingRun.class, runId);
            run.setCurrentStep(stepName);
            em.merge(run);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void completeStep(EntityManagerFactory emf, long runId, String stepName, String detail) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRunStep step = findStep(em, runId, stepName);
            step.setStatus(BillingRunStep.STATUS_COMPLETED);
            step.setCompletedAt(LocalDateTime.now());
            step.setDetail(detail);
            em.merge(step);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void skipStep(EntityManagerFactory emf, long runId, String stepName) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRunStep step = findStep(em, runId, stepName);
            step.setStatus(BillingRunStep.STATUS_SKIPPED);
            step.setCompletedAt(LocalDateTime.now());
            em.merge(step);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void failStep(EntityManagerFactory emf, long runId, String stepName, String errorText) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRunStep step = findStep(em, runId, stepName);
            step.setStatus(BillingRunStep.STATUS_FAILED);
            step.setCompletedAt(LocalDateTime.now());
            step.setErrorText(errorText);
            em.merge(step);

            BillingRun run = em.find(BillingRun.class, runId);
            run.setStatus(BillingRun.STATUS_FAILED);
            run.setCompletedAt(LocalDateTime.now());
            run.setErrorText(errorText);
            em.merge(run);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void failRun(EntityManagerFactory emf, long runId, String errorText) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRun run = em.find(BillingRun.class, runId);
            run.setStatus(BillingRun.STATUS_FAILED);
            run.setCompletedAt(LocalDateTime.now());
            run.setErrorText(errorText);
            em.merge(run);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void setCurrentStep(EntityManagerFactory emf, long runId, String label) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRun run = em.find(BillingRun.class, runId);
            run.setCurrentStep(label);
            em.merge(run);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void completeRun(EntityManagerFactory emf, long runId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRun run = em.find(BillingRun.class, runId);
            run.setStatus(BillingRun.STATUS_COMPLETED);
            run.setCompletedAt(LocalDateTime.now());
            em.merge(run);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // SYNC-GUARD: automated recovery for a worker that died hard (GC-starved / OOM / JVM
    // restart) — its billing_run row stays RUNNING forever because the top-level failRun
    // safety net in BillingPipelineRunner.run() can't fire from a dead thread. Left
    // unreaped, isRunActive() would return true on every future launch attempt, 409-ing
    // forever until someone hand-edits the row. 30 minutes is the threshold: billing runs
    // complete in minutes even when slow, so this cannot plausibly reap a legitimately-
    // running job, but reaps a truly-dead one promptly. Call from LaunchMonthlyBilling
    // before the isRunActive() guard.
    public static int reapStaleRuns(EntityManagerFactory emf, int thresholdMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(thresholdMinutes);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            int updated = em.createQuery(
                    "UPDATE BillingRun b SET b.status = :failed, b.completedAt = :now, b.errorText = :msg " +
                    "WHERE b.status = :running AND b.startedAt < :cutoff")
                    .setParameter("failed", BillingRun.STATUS_FAILED)
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("msg", "Reaped: run exceeded " + thresholdMinutes
                            + "-minute staleness threshold (worker presumed dead — e.g. JVM restart or OOM).")
                    .setParameter("running", BillingRun.STATUS_RUNNING)
                    .setParameter("cutoff", cutoff)
                    .executeUpdate();

            em.getTransaction().commit();
            evict(emf);
            return updated;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static BillingRun findRun(EntityManagerFactory emf, long runId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(BillingRun.class, runId);
        } finally {
            em.close();
        }
    }

    public static List<BillingRunStep> findSteps(EntityManagerFactory emf, long runId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT s FROM BillingRunStep s WHERE s.runId = :runId ORDER BY s.id ASC",
                    BillingRunStep.class)
                    .setParameter("runId", runId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public static BillingRun findLatestRun(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT r FROM BillingRun r ORDER BY r.startedAt DESC", BillingRun.class)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public static void markRenewalsRefreshed(EntityManagerFactory emf, long runId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            BillingRun run = em.find(BillingRun.class, runId);
            run.setRenewalsRefreshed(true);
            em.merge(run);

            em.getTransaction().commit();
            evict(emf);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static BillingRunStep findStep(EntityManager em, long runId, String stepName) {
        return em.createQuery(
                "SELECT s FROM BillingRunStep s WHERE s.runId = :runId AND s.stepName = :stepName",
                BillingRunStep.class)
                .setParameter("runId", runId)
                .setParameter("stepName", stepName)
                .getSingleResult();
    }

    private static void evict(EntityManagerFactory emf) {
        emf.getCache().evict(BillingRun.class);
        emf.getCache().evict(BillingRunStep.class);
    }
}

package net.superiorstate.ams.data.service.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.data.dao.AuditRunDAO;
import net.superiorstate.ams.model.audit.AuditRun;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * T237 — audit framework runner. Follows {@code RateCacheWarmService}'s own lifecycle shape:
 * single-thread scheduled executor, daemon thread, {@code scheduleAtFixedRate}, {@code
 * shutdownNow()} then {@code awaitTermination(10s)}, an {@code AtomicBoolean running} guard
 * shared between the scheduled tick and the manual trigger.
 * <p>
 * ⚠️ <b>Never evaluates on page render.</b> {@link #getActionCount()} / {@link #getErrorCount()}
 * are cheap reads of an in-memory map, rebuilt only when this service is constructed (from the
 * last stored {@code audit_run} rows) or when {@link #runAll} actually runs. The navbar badge
 * (EL, {@code applicationScope.auditService.actionCount}) never triggers a run.
 * <p>
 * <b>One check's failure never stops another.</b> {@link #runAll} wraps each check's
 * {@code evaluate} call in its own try/catch, on top of {@link AuditCheck#evaluate}'s own
 * contract to never throw — belt and suspenders, exactly because a check that breaks that
 * contract must not be allowed to take the rest of the run down with it.
 */
public class AuditService {

    private static final Logger log = LogManager.getLogger(AuditService.class);

    private static final long INTERVAL_HOURS = 24;
    private static final long INITIAL_DELAY_MINUTES = 10;

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";
    /** An acknowledgment-driven re-run of one check — see {@link #runOneCheck}. 9 characters fits
     *  {@code audit_run.run_trigger}'s existing {@code VARCHAR(10)} (V099) with room to spare; no
     *  schema change needed. Distinct from {@link #TRIGGER_MANUAL} so the run history stays honest
     *  about why a count changed outside the hub's own "Run now" or the daily schedule. */
    public static final String TRIGGER_ACK = "ACK_RERUN";

    private static final List<AuditCheck> CHECKS = List.of(
            new IchraUncodedParticipantsCheck(),
            new FundedPurseNoDisbursementCheck(),
            new CardDeclineCheck(),
            new CardStatusCheck());

    private final ScheduledExecutorService executor;
    private final EntityManagerFactory emf;
    private final Long pspId;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Map<String, AuditRun> latestResults = new ConcurrentHashMap<>();

    /**
     * Loads the latest stored result per check for {@code pspId}. Never throws — a database or
     * mapping problem at startup must not abort {@code contextInitialized}; it logs a warning and
     * starts with an empty map, which the hub renders as "never run".
     */
    public AuditService(EntityManagerFactory emf, Long pspId) {
        this.emf = emf;
        this.pspId = pspId;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "audit-service");
            t.setDaemon(true);
            return t;
        });

        try {
            EntityManager em = emf.createEntityManager();
            try {
                latestResults.putAll(AuditRunDAO.findLatestPerCheck(em, pspId));
            } finally {
                if (em.isOpen()) em.close();
            }
        } catch (Exception e) {
            log.warn("[AUDIT] Could not load prior audit runs — starting with no results", e);
        }
    }

    /** Starts the daily scheduled run. */
    public void start() {
        executor.scheduleAtFixedRate(this::scheduledTick, INITIAL_DELAY_MINUTES, INTERVAL_HOURS * 60, TimeUnit.MINUTES);
        log.info("[AUDIT] Started — running every {}h (first run in {}m)", INTERVAL_HOURS, INITIAL_DELAY_MINUTES);
    }

    /**
     * Shuts down the scheduler gracefully. Waits up to 10s for an in-flight run to finish so it
     * doesn't keep running against a closing/closed {@code EntityManagerFactory} after
     * {@code EmfListener.contextDestroyed()} proceeds to {@code emf.close()} — same fix as
     * {@code InstallationHealthScheduler} and {@code RateCacheWarmService}.
     */
    public void stop() {
        executor.shutdownNow();
        try {
            if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("[AUDIT] Stopped");
            } else {
                log.warn("[AUDIT] Stopped — task still running after 10s shutdown timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[AUDIT] Stop interrupted while awaiting termination");
        }
    }

    public boolean isRunInProgress() {
        return running.get();
    }

    public enum TriggerResult { STARTED, ALREADY_RUNNING }

    /**
     * Triggers a run on this service's own executor and returns immediately. Used by the hub's
     * "Run now" button, which can race a scheduled tick; both funnel through the same
     * {@code running} guard so at most one run executes at a time.
     */
    public TriggerResult triggerManual() {
        if (!running.compareAndSet(false, true)) {
            return TriggerResult.ALREADY_RUNNING;
        }
        executor.execute(() -> runAcquired(TRIGGER_MANUAL));
        return TriggerResult.STARTED;
    }

    /** {@link #runOneCheck}'s outcome. */
    public enum SingleCheckRunResult { COMPLETED, ALREADY_RUNNING, NOT_REGISTERED }

    /**
     * Runs exactly one registered check, by key, synchronously on the caller's thread, and
     * persists its result the same way {@link #runAll} does — for a rare, targeted re-run needed
     * right after a state change nothing else would surface (an audit-finding acknowledgment;
     * T237), not for the hub or the badge, which must never trigger a run on page render (see the
     * class note). Synchronous, unlike {@link #triggerManual}, because the caller (the
     * acknowledgment POST handler) needs to know the outcome before it redirects and builds its
     * own flash message.
     * <p>
     * Shares {@link #running} with {@link #triggerManual} and the scheduled tick, so this can
     * never race a full run in either direction: if one is already in flight,
     * {@link SingleCheckRunResult#ALREADY_RUNNING} is returned and nothing runs here — the
     * caller's own state change (e.g. the acknowledgment) is unaffected, since it was already
     * saved before this is called, and the next scheduled or manual run picks up the correct
     * count regardless.
     * <p>
     * Recorded with {@link #TRIGGER_ACK}, not {@link #TRIGGER_MANUAL} — this is not the hub's
     * "Run now".
     *
     * @return {@link SingleCheckRunResult#NOT_REGISTERED} if no check has this key (nothing run);
     * {@link SingleCheckRunResult#ALREADY_RUNNING} if a run was already in progress (nothing run);
     * otherwise {@link SingleCheckRunResult#COMPLETED} once the check has been evaluated and its
     * {@code audit_run} row written — a check that throws is still recorded, as {@code ERROR}, the
     * same contract {@link #runAll} documents for every check it runs.
     */
    public SingleCheckRunResult runOneCheck(String checkKey) {
        AuditCheck check = null;
        for (AuditCheck candidate : CHECKS) {
            if (candidate.key().equals(checkKey)) {
                check = candidate;
                break;
            }
        }
        if (check == null) return SingleCheckRunResult.NOT_REGISTERED;

        if (!running.compareAndSet(false, true)) {
            return SingleCheckRunResult.ALREADY_RUNNING;
        }
        try {
            runOneAcquired(check);
        } finally {
            running.set(false);
        }
        return SingleCheckRunResult.COMPLETED;
    }

    /**
     * Precondition: caller has already acquired {@code running} via compareAndSet(false, true).
     * Same per-check evaluate-then-persist shape as {@link #runAll}'s loop body — kept as its own
     * copy rather than shared with it, since {@code runAll} is left unmodified by this build.
     */
    private void runOneAcquired(AuditCheck check) {
        long start = System.currentTimeMillis();
        AuditResult result;
        EntityManager evalEm = emf.createEntityManager();
        try {
            result = check.evaluate(evalEm, pspId);
        } catch (Exception e) {
            log.error("[AUDIT] Check '{}' threw on acknowledgment re-run — this violates AuditCheck's contract",
                    check.key(), e);
            result = AuditResult.error(e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : ""));
        } finally {
            if (evalEm.isOpen()) evalEm.close();
        }
        int durationMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start);

        AuditRun run = new AuditRun();
        run.setPspId(pspId);
        run.setCheckKey(check.key());
        run.setRunAt(LocalDateTime.now());
        run.setRunTrigger(TRIGGER_ACK);
        run.setStatus(result.status());
        run.setFindingCount(result.findingCount());
        run.setSummary(truncate(result.summary(), 500));
        run.setError(truncate(result.error(), 500));
        run.setDurationMs(durationMs);

        EntityManager insertEm = emf.createEntityManager();
        try {
            AuditRunDAO.insert(insertEm, run);
            latestResults.put(check.key(), run);
        } catch (Exception e) {
            log.error("[AUDIT] Could not record acknowledgment re-run for check '{}'", check.key(), e);
        } finally {
            if (insertEm.isOpen()) insertEm.close();
        }
    }

    private void scheduledTick() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[AUDIT] Scheduled tick skipped — a run is already in progress");
            return;
        }
        runAcquired(TRIGGER_SCHEDULED);
    }

    /** Precondition: caller has already acquired {@code running} via compareAndSet(false, true). */
    private void runAcquired(String trigger) {
        try {
            runAll(trigger);
        } catch (Exception e) {
            log.error("[AUDIT] Run failed", e);
        } finally {
            running.set(false);
        }
    }

    /**
     * Runs every registered check for {@link #pspId}, in order. Each check gets its own
     * {@link EntityManager} and its own try/catch; a check that throws (breaking its own
     * contract) is recorded as {@code ERROR} with a scrubbed message rather than aborting the
     * remaining checks.
     */
    void runAll(String trigger) {
        for (AuditCheck check : CHECKS) {
            long start = System.currentTimeMillis();
            AuditResult result;
            EntityManager evalEm = emf.createEntityManager();
            try {
                result = check.evaluate(evalEm, pspId);
            } catch (Exception e) {
                log.error("[AUDIT] Check '{}' threw — this violates AuditCheck's contract", check.key(), e);
                result = AuditResult.error(e.getClass().getSimpleName()
                        + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            } finally {
                if (evalEm.isOpen()) evalEm.close();
            }
            int durationMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start);

            AuditRun run = new AuditRun();
            run.setPspId(pspId);
            run.setCheckKey(check.key());
            run.setRunAt(LocalDateTime.now());
            run.setRunTrigger(trigger);
            run.setStatus(result.status());
            run.setFindingCount(result.findingCount());
            run.setSummary(truncate(result.summary(), 500));
            run.setError(truncate(result.error(), 500));
            run.setDurationMs(durationMs);

            EntityManager insertEm = emf.createEntityManager();
            try {
                AuditRunDAO.insert(insertEm, run);
                latestResults.put(check.key(), run);
            } catch (Exception e) {
                log.error("[AUDIT] Could not record run for check '{}'", check.key(), e);
            } finally {
                if (insertEm.isOpen()) insertEm.close();
            }
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    // ── EL-facing getters ────────────────────────────────────────────

    public List<AuditCheck> getRegisteredChecks() {
        return CHECKS;
    }

    public Map<String, AuditRun> getLatestResults() {
        return latestResults;
    }

    public Long getPspId() {
        return pspId;
    }

    /** Sum of {@code findingCount} over every check whose latest result is {@code ACTION}. */
    public int getActionCount() {
        int total = 0;
        for (AuditRun run : latestResults.values()) {
            if (AuditResult.STATUS_ACTION.equals(run.getStatus())) {
                total += run.getFindingCount();
            }
        }
        return total;
    }

    /** Count of checks whose latest result is {@code ERROR}. */
    public int getErrorCount() {
        int total = 0;
        for (AuditRun run : latestResults.values()) {
            if (AuditResult.STATUS_ERROR.equals(run.getStatus())) {
                total++;
            }
        }
        return total;
    }
}

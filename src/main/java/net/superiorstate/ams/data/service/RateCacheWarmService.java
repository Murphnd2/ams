package net.superiorstate.ams.data.service;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.util.AgeCurve;
import net.superiorstate.ams.model.market.RatingAreaRateCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Warms {@link RatingAreaRateCache} for every configured county. Follows
 * InstallationHealthScheduler's lifecycle shape (single-thread scheduled executor,
 * daemon thread, scheduleAtFixedRate, shutdownNow() then awaitTermination(10s)) and
 * BillingPipelineRunner's per-class L2-eviction pattern.
 *
 * <p><b>Deviations from the original Phase B-1b design, both forced by the "no
 * changes to HealthSherpaService.java beyond Part 0" constraint and both flagged
 * in the Phase B-1b report — do not silently "fix" these back without re-reading
 * that report's reasoning:</b>
 * <ol>
 *   <li><b>Two sequential single-applicant calls, not one two-applicant call.</b>
 *       HealthSherpaService only exposes {@code quoteSingleApplicant} (one
 *       applicant per request, flat premium fields). It has no method taking a
 *       {primary, spouse} applicant pair, and {@link net.superiorstate.ams.data.service.HealthSherpaService.PlanSummary}
 *       does not parse a per-applicant premium breakdown. Building that would mean
 *       editing HealthSherpaService.java, which was out of scope. This class instead
 *       calls quoteSingleApplicant once at age 21 (the market-classification call)
 *       and once at age 45 (the canary-check call), matching plans between the two
 *       responses by hiosId. Two calls per county instead of one, but still far
 *       fewer than one per age (45).</li>
 *   <li><b>RATE_CACHE_COUNTIES is {@code zip:fips:state}, not {@code fips:state}.</b>
 *       quoteSingleApplicant requires a zipCode parameter; the county list as
 *       originally specified had no ZIP. Rather than guess a representative ZIP per
 *       county myself (a real correctness risk — the wrong ZIP could silently
 *       return the wrong rating area), the constant format carries one, supplied by
 *       whoever configures the constant. See D-83 in deployment_backlog.md.</li>
 * </ol>
 */
public class RateCacheWarmService {

    private static final Logger log = LogManager.getLogger(RateCacheWarmService.class);

    private static final long INTERVAL_HOURS = 24;
    private static final long INITIAL_DELAY_MINUTES = 5;
    private static final int CANARY_AGE = 45;
    private static final int BASE_AGE = 21;
    private static final BigDecimal CANARY_TOLERANCE = new BigDecimal("0.01");

    private static final String METAL_SILVER = "Silver";
    private static final String METAL_BRONZE = "Bronze";
    private static final String METAL_EXPANDED_BRONZE = "Expanded Bronze";

    private final ScheduledExecutorService executor;
    private final EntityManagerFactory emf;
    private final int planYear;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private static volatile String lastRunSummary;
    private static volatile LocalDateTime lastRunAt;

    public RateCacheWarmService(EntityManagerFactory emf, int planYear) {
        this.emf = emf;
        this.planYear = planYear;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-cache-warm-service");
            t.setDaemon(true);
            return t;
        });
    }

    /** Starts the periodic warm. Runs every INTERVAL_HOURS after an initial delay. */
    public void start() {
        executor.scheduleAtFixedRate(this::scheduledTick, INITIAL_DELAY_MINUTES, INTERVAL_HOURS * 60, TimeUnit.MINUTES);
        log.info("[RATE-CACHE] Started — warming every {}h (first run in {}m)", INTERVAL_HOURS, INITIAL_DELAY_MINUTES);
    }

    /**
     * Shuts down the scheduler gracefully. Waits up to 10s for an in-flight warm to
     * finish so it doesn't keep running against a closing/closed EntityManagerFactory
     * after EmfListener.contextDestroyed() proceeds to emf.close() — same fix applied
     * to InstallationHealthScheduler in Phase B-1a, needed here because this job
     * performs blocking network I/O.
     */
    public void stop() {
        executor.shutdownNow();
        try {
            if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("[RATE-CACHE] Stopped");
            } else {
                log.warn("[RATE-CACHE] Stopped — task still running after 10s shutdown timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[RATE-CACHE] Stop interrupted while awaiting termination");
        }
    }

    public boolean isRunInProgress() {
        return running.get();
    }

    public String getLastRunSummary() {
        return lastRunSummary;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public int getPlanYear() {
        return planYear;
    }

    /** STAGING if the configured base URL contains "ichra-staging", otherwise PRODUCTION. */
    public static String currentSourceEnv() {
        String baseUrl = AppConfig.getHealthSherpaBaseUrl();
        return baseUrl != null && baseUrl.contains("ichra-staging")
                ? RatingAreaRateCache.SOURCE_ENV_STAGING
                : RatingAreaRateCache.SOURCE_ENV_PRODUCTION;
    }

    public enum WarmTriggerResult { STARTED, ALREADY_RUNNING }

    /**
     * Triggers a warm run on this service's own executor and returns immediately —
     * does not block the caller for the duration of the run. Used by the admin
     * page's manual refresh button, which can race a scheduled tick; both funnel
     * through the same {@code running} guard so at most one warm executes at a time
     * and neither path queues a redundant run.
     */
    public WarmTriggerResult triggerManualWarm() {
        if (!running.compareAndSet(false, true)) {
            return WarmTriggerResult.ALREADY_RUNNING;
        }
        executor.execute(this::runAcquired);
        return WarmTriggerResult.STARTED;
    }

    private void scheduledTick() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[RATE-CACHE] Scheduled tick skipped — a warm run is already in progress");
            return;
        }
        runAcquired();
    }

    /** Precondition: caller has already acquired `running` via compareAndSet(false, true). */
    private void runAcquired() {
        try {
            warmAll();
        } catch (Exception e) {
            log.error("[RATE-CACHE] Warm run failed", e);
            lastRunSummary = "FAILED: " + e.getMessage();
            lastRunAt = LocalDateTime.now();
        } finally {
            running.set(false);
        }
    }

    private void warmAll() {
        List<CountyTarget> counties = readConfiguredCounties();
        if (counties.isEmpty()) {
            log.warn("[RATE-CACHE] No valid counties configured in RATE_CACHE_COUNTIES — nothing to warm");
            lastRunSummary = "0 counties configured";
            lastRunAt = LocalDateTime.now();
            return;
        }

        int succeeded = 0, skipped = 0, failed = 0;
        for (CountyTarget county : counties) {
            try {
                if (warmCounty(county)) {
                    succeeded++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.error("[RATE-CACHE] County {} failed: {}", county, e.getMessage(), e);
            }
        }

        lastRunSummary = succeeded + " warmed, " + skipped + " skipped, " + failed + " failed (of " + counties.size() + ")";
        lastRunAt = LocalDateTime.now();
        log.info("[RATE-CACHE] Run complete: {}", lastRunSummary);
    }

    /** @return true if the county was successfully warmed (rows written), false if skipped (e.g. zero plans). */
    private boolean warmCounty(CountyTarget county) {
        HealthSherpaService.HealthSherpaQuoteResponse baseResponse = HealthSherpaService.quoteSingleApplicant(
                county.zip, county.fips, county.state, planYear, BASE_AGE, false, true);

        if (!baseResponse.isSuccess()) {
            log.warn("[RATE-CACHE] County {} base (age {}) call failed: {}", county, BASE_AGE, baseResponse.getErrorMessage());
            return false;
        }
        List<HealthSherpaService.PlanSummary> basePlans = baseResponse.getPlans();
        if (basePlans.isEmpty()) {
            log.info("[RATE-CACHE] County {} returned zero plans — skipping", county);
            return false;
        }

        canaryCheck(county, basePlans);

        BigDecimal marketLow = null, marketHigh = null;
        BigDecimal lcsp = null, benchmarkSilver = null, lowestBronze = null;
        List<BigDecimal> silverPremiumsSorted = new ArrayList<>();
        java.util.Set<String> issuers = new java.util.HashSet<>();

        for (HealthSherpaService.PlanSummary plan : basePlans) {
            BigDecimal premium = basePremiumOf(plan);
            if (premium == null) continue;

            if (marketLow == null || premium.compareTo(marketLow) < 0) marketLow = premium;
            if (marketHigh == null || premium.compareTo(marketHigh) > 0) marketHigh = premium;

            String metal = plan.getMetalLevel();
            if (METAL_SILVER.equals(metal)) {
                silverPremiumsSorted.add(premium);
            } else if (METAL_BRONZE.equals(metal) || METAL_EXPANDED_BRONZE.equals(metal)) {
                if (lowestBronze == null || premium.compareTo(lowestBronze) < 0) lowestBronze = premium;
            }

            if (plan.getIssuerName() != null) {
                issuers.add(plan.getIssuerName());
            }
        }

        java.util.Collections.sort(silverPremiumsSorted);
        if (!silverPremiumsSorted.isEmpty()) {
            lcsp = silverPremiumsSorted.get(0);
        }
        if (silverPremiumsSorted.size() >= 2) {
            benchmarkSilver = silverPremiumsSorted.get(1);
        }

        int carrierCount = issuers.size();
        int planCount = basePlans.size();

        List<RatingAreaRateCache> rows = new ArrayList<>();
        LocalDateTime fetchedAt = LocalDateTime.now();
        String sourceEnv = currentSourceEnv();

        for (int age = AgeCurve.MIN_AGE; age <= AgeCurve.MAX_AGE; age++) {
            RatingAreaRateCache row = new RatingAreaRateCache();
            row.setPlanYear(planYear);
            row.setCountyFips(county.fips);
            row.setState(county.state);
            row.setAge(age);
            row.setUsesTobacco(false); // unused pending O19 — see V074 header
            row.setMarketLowPremium(scaleOrNull(marketLow, age));
            row.setMarketHighPremium(scaleOrNull(marketHigh, age));
            row.setLcspPremium(scaleOrNull(lcsp, age));
            row.setBenchmarkSilverPremium(scaleOrNull(benchmarkSilver, age));
            row.setLowestBronzePremium(scaleOrNull(lowestBronze, age));
            row.setCarrierCount(carrierCount);
            row.setPlanCount(planCount);
            row.setFetchedAt(fetchedAt);
            row.setSourceEnv(sourceEnv);
            rows.add(row);
        }

        EntityManager em = emf.createEntityManager();
        try {
            RateCacheDAO.replaceCountyRates(em, planYear, county.fips, rows);
        } finally {
            em.close();
        }

        evictRateCacheOnly();
        return true;
    }

    private BigDecimal scaleOrNull(BigDecimal baseAt21, int age) {
        return baseAt21 == null ? null : AgeCurve.scale(baseAt21, planYear, age);
    }

    /** Prefers grossPremium (the market-classification figure per Part 4 step 3), falls back to premium. */
    private BigDecimal basePremiumOf(HealthSherpaService.PlanSummary plan) {
        if (plan.getGrossPremium() != null) {
            return BigDecimal.valueOf(plan.getGrossPremium());
        }
        if (plan.getPremium() != null) {
            return BigDecimal.valueOf(plan.getPremium());
        }
        return null;
    }

    /**
     * Standing check that the age-factor table is still correct. Makes a second
     * quoteSingleApplicant call at CANARY_AGE, matches at least one plan present in
     * both responses by hiosId, and compares the observed ratio against
     * AgeCurve.factorFor. On any mismatch or failure, logs a clear warning naming
     * both values and returns — never aborts the county.
     */
    private void canaryCheck(CountyTarget county, List<HealthSherpaService.PlanSummary> basePlans) {
        HealthSherpaService.HealthSherpaQuoteResponse canaryResponse = HealthSherpaService.quoteSingleApplicant(
                county.zip, county.fips, county.state, planYear, CANARY_AGE, false, true);

        if (!canaryResponse.isSuccess()) {
            log.warn("[RATE-CACHE] County {} canary (age {}) call failed: {} — age curve unverified this run",
                    county, CANARY_AGE, canaryResponse.getErrorMessage());
            return;
        }

        Map<String, BigDecimal> canaryByHios = new LinkedHashMap<>();
        for (HealthSherpaService.PlanSummary plan : canaryResponse.getPlans()) {
            if (plan.getHiosId() == null) continue;
            BigDecimal premium = basePremiumOf(plan);
            if (premium != null) {
                canaryByHios.put(plan.getHiosId(), premium);
            }
        }

        for (HealthSherpaService.PlanSummary basePlan : basePlans) {
            if (basePlan.getHiosId() == null) continue;
            BigDecimal basePremium = basePremiumOf(basePlan);
            BigDecimal canaryPremium = canaryByHios.get(basePlan.getHiosId());
            if (basePremium == null || canaryPremium == null) continue;

            BigDecimal expectedCanary = AgeCurve.scale(basePremium, planYear, CANARY_AGE);
            BigDecimal diff = canaryPremium.subtract(expectedCanary).abs();
            if (diff.compareTo(CANARY_TOLERANCE) > 0) {
                log.warn("[RATE-CACHE] Canary mismatch for county {} plan {}: age-{} premium {} scaled to age-{} " +
                                "expects {}, HealthSherpa returned {} (diff {}) — AgeCurve may be stale for plan year {}",
                        county, basePlan.getHiosId(), BASE_AGE, basePremium, CANARY_AGE, expectedCanary,
                        canaryPremium, diff, planYear);
            } else {
                log.debug("[RATE-CACHE] Canary check passed for county {} plan {}", county, basePlan.getHiosId());
            }
            return; // one confirmed match is sufficient per Part 4 step 4
        }

        log.warn("[RATE-CACHE] County {} — no plan matched by hiosId between age-{} and age-{} responses; " +
                "canary check skipped this run", county, BASE_AGE, CANARY_AGE);
    }

    // ── L2 cache ─────────────────────────────────────────────────────

    /** Never evictAll() — documented in this codebase (BillingPipelineRunner) as corrupting EclipseLink descriptors. */
    private void evictRateCacheOnly() {
        Cache cache = emf.getCache();
        if (cache == null) return;
        cache.evict(RatingAreaRateCache.class);
    }

    // ── Config parsing ───────────────────────────────────────────────

    /**
     * Reads RATE_CACHE_COUNTIES as comma-separated {@code zip:fips:state} triples
     * (see class-level deviation note #2). Malformed entries are logged and
     * skipped, not fatal to the run.
     */
    private List<CountyTarget> readConfiguredCounties() {
        List<CountyTarget> result = new ArrayList<>();
        EntityManager em = emf.createEntityManager();
        String raw;
        try {
            raw = AppConstantDAO.getConstantValue(em, "RATE_CACHE_COUNTIES");
        } finally {
            em.close();
        }
        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split(":");
            if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
                log.warn("[RATE-CACHE] Skipping malformed RATE_CACHE_COUNTIES entry: '{}' (expected zip:fips:state)", trimmed);
                continue;
            }
            result.add(new CountyTarget(parts[0].trim(), parts[1].trim(), parts[2].trim().toUpperCase()));
        }
        return result;
    }

    private static class CountyTarget {
        final String zip;
        final String fips;
        final String state;

        CountyTarget(String zip, String fips, String state) {
            this.zip = zip;
            this.fips = fips;
            this.state = state;
        }

        @Override
        public String toString() {
            return fips + ":" + state + " (zip " + zip + ")";
        }
    }
}

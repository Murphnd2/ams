package net.superiorstate.ams.data.service;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.CountyReferenceDAO;
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.util.AgeCurve;
import net.superiorstate.ams.model.market.CountyReference;
import net.superiorstate.ams.model.market.RatingAreaRateCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Warms {@link RatingAreaRateCache} for every configured county and plan year.
 * Follows InstallationHealthScheduler's lifecycle shape (single-thread scheduled
 * executor, daemon thread, scheduleAtFixedRate, shutdownNow() then
 * awaitTermination(10s)) and BillingPipelineRunner's per-class L2-eviction pattern.
 *
 * <p><b>Plan years are configuration, never derived from the current date.</b> An
 * earlier version of this class used {@code Year.now()}. That is wrong during the
 * period it matters most: open enrollment for the next plan year begins November 1,
 * and from that date an agent illustrating a group with a January 1 effective date
 * needs the *next* year's rates while {@code Year.now()} still returns the current
 * year until January 1. The cache would have silently served the wrong plan year
 * — no error, no warning, just wrong numbers during the busiest quoting window of
 * the year. Plan years now come from the {@code RATE_CACHE_PLAN_YEARS} constant
 * (D-84), re-read at the start of every run (not cached at construction) so a
 * change takes effect on the next scheduled tick without a restart. A year with no
 * {@link AgeCurve} entry is skipped with a logged error, never guessed.
 *
 * <p><b>Deviations from the original Phase B-1b design, both forced by the "no
 * changes to HealthSherpaService.java" constraint and both flagged in the
 * Phase B-1b report — do not silently "fix" these back without re-reading that
 * report's reasoning:</b> (that constraint itself was released in T43, which does
 * edit HealthSherpaService.java — the two deviations below stand regardless.)
 * <ol>
 *   <li><b>Two sequential single-applicant calls per county per year, not one
 *       two-applicant call.</b> HealthSherpaService only exposes
 *       {@code quoteSingleApplicant}. This class calls it once at age 21 (market
 *       classification) and, for the canary check, once more at age 45 — but see
 *       point 3 below, the age-45 call is now made only once per plan year, not
 *       once per county.</li>
 *   <li><b>RATE_CACHE_COUNTIES is {@code zip:fips:state}, not {@code fips:state}.</b>
 *       quoteSingleApplicant requires a zipCode parameter the county list as
 *       originally specified didn't carry. As of V076, an entry may instead be a
 *       bare {@code county_fips} resolved through {@link CountyReferenceDAO},
 *       which supplies the zip and state from the county_reference row — the
 *       triple form still works unchanged and is not being phased out, this is
 *       an additional accepted format, not a replacement.</li>
 *   <li><b>Canary runs once per plan year, not once per county (Phase B-1b
 *       follow-up).</b> The uniform age rating curve is statutory and identical
 *       across every county in every state — validating it per county bought
 *       nothing and doubled the call count. It now runs once per plan year,
 *       against the first county processed for that year: {@code counties + 1}
 *       calls per plan year instead of {@code counties * 2}.</li>
 * </ol>
 */
public class RateCacheWarmService {

    private static final Logger log = LogManager.getLogger(RateCacheWarmService.class);

    private static final long INTERVAL_HOURS = 24;
    private static final long INITIAL_DELAY_MINUTES = 5;
    private static final int CANARY_AGE = 45;
    private static final int BASE_AGE = 21;
    private static final BigDecimal CANARY_TOLERANCE = new BigDecimal("0.01");

    /** ACA catastrophic plans are restricted to enrollees under 30, so they are
     *  excluded from the plan population for ages 30 and up. This is a plan-set
     *  restriction, not a premium adjustment — the statutory age curve governs
     *  premiums for plans that are available, but says nothing about which plans
     *  are available. Consequence: market_low_premium legitimately steps up
     *  between the age-29 and age-30 rows. That discontinuity is correct. Do not
     *  "fix" it. */
    private static final int CATASTROPHIC_MAX_AGE = 29;

    private static final String METAL_SILVER = "Silver";
    private static final String METAL_BRONZE = "Bronze";
    private static final String METAL_EXPANDED_BRONZE = "Expanded Bronze";

    private final ScheduledExecutorService executor;
    private final EntityManagerFactory emf;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private static volatile String lastRunSummary;
    private static volatile LocalDateTime lastRunAt;

    public RateCacheWarmService(EntityManagerFactory emf) {
        this.emf = emf;
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

    /** STAGING if the configured base URL contains "ichra-staging", PRODUCTION if configured
     *  and not staging, or null if the base URL is not configured at all. */
    public static String currentSourceEnv() {
        String baseUrl = AppConfig.getHealthSherpaBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        return baseUrl.contains("ichra-staging")
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
        EntityManager configEm = emf.createEntityManager();
        List<Integer> planYears;
        List<CountyTarget> counties;
        try {
            planYears = readConfiguredPlanYears(configEm);
            counties = readConfiguredCounties(configEm);
        } finally {
            configEm.close();
        }

        if (planYears.isEmpty()) {
            log.error("[RATE-CACHE] RATE_CACHE_PLAN_YEARS is absent, empty, or wholly unparseable — skipping run entirely");
            lastRunSummary = "SKIPPED: no valid plan years configured";
            lastRunAt = LocalDateTime.now();
            return;
        }
        if (counties.isEmpty()) {
            log.warn("[RATE-CACHE] No valid counties configured in RATE_CACHE_COUNTIES — nothing to warm");
            lastRunSummary = "SKIPPED: no counties configured";
            lastRunAt = LocalDateTime.now();
            return;
        }

        int yearsWarmed = 0, yearsSkipped = 0;
        int succeeded = 0, skipped = 0, failed = 0;

        for (int planYear : planYears) {
            if (!AgeCurve.hasCurveFor(planYear)) {
                log.error("[RATE-CACHE] No AgeCurve configured for plan year {} — skipping this year only, other configured years continue", planYear);
                yearsSkipped++;
                continue;
            }

            yearsWarmed++;
            boolean canaryDoneForYear = false;
            for (CountyTarget county : counties) {
                try {
                    if (warmCounty(planYear, county, !canaryDoneForYear)) {
                        succeeded++;
                    } else {
                        skipped++;
                    }
                    canaryDoneForYear = true; // attempted (or intentionally skipped) — never retried for this year
                } catch (Exception e) {
                    failed++;
                    canaryDoneForYear = true;
                    log.error("[RATE-CACHE] Plan year {} county {} failed: {}", planYear, county, e.getMessage(), e);
                }
            }
        }

        lastRunSummary = yearsWarmed + " plan year(s) warmed, " + yearsSkipped + " plan year(s) skipped (no AgeCurve); "
                + succeeded + " county-years warmed, " + skipped + " skipped, " + failed + " failed";
        lastRunAt = LocalDateTime.now();
        log.info("[RATE-CACHE] Run complete: {}", lastRunSummary);
    }

    /** @return true if the county was successfully warmed (rows written), false if skipped (e.g. zero plans). */
    private boolean warmCounty(int planYear, CountyTarget county, boolean runCanary) {
        HealthSherpaService.HealthSherpaQuoteResponse baseResponse = HealthSherpaService.quoteSingleApplicant(
                county.zip, county.fips, county.state, planYear, BASE_AGE, false, true);

        if (!baseResponse.isSuccess()) {
            log.warn("[RATE-CACHE] Plan year {} county {} base (age {}) call failed: {}",
                    planYear, county, BASE_AGE, baseResponse.getErrorMessage());
            return false;
        }
        List<HealthSherpaService.PlanSummary> basePlans = baseResponse.getPlans();
        if (basePlans.isEmpty()) {
            log.info("[RATE-CACHE] Plan year {} county {} returned zero plans — skipping", planYear, county);
            return false;
        }

        // Canary once per plan year, not once per county: the age curve is statutory and
        // county-invariant (identical for every county in every state that uses the federal
        // default), so re-validating it against every county bought nothing and doubled the
        // call count. See class Javadoc point 3.
        if (runCanary) {
            canaryCheck(planYear, county, basePlans);
        }

        List<RatingAreaRateCache> rows = new ArrayList<>();
        LocalDateTime fetchedAt = LocalDateTime.now();
        String sourceEnv = currentSourceEnv();

        for (int age = AgeCurve.MIN_AGE; age <= AgeCurve.MAX_AGE; age++) {
            List<HealthSherpaService.PlanSummary> plansForAge = (age <= CATASTROPHIC_MAX_AGE)
                    ? basePlans
                    : basePlans.stream()
                              .filter(p -> !"Catastrophic".equalsIgnoreCase(p.getMetalLevel()))
                              .collect(Collectors.toList());

            BigDecimal marketLow = null, marketHigh = null;
            BigDecimal lcsp = null, benchmarkSilver = null, lowestBronze = null;
            List<BigDecimal> silverPremiumsSorted = new ArrayList<>();
            Set<String> issuers = new LinkedHashSet<>();

            for (HealthSherpaService.PlanSummary plan : plansForAge) {
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
            int planCount = plansForAge.size();

            RatingAreaRateCache row = new RatingAreaRateCache();
            row.setPlanYear(planYear);
            row.setCountyFips(county.fips);
            row.setState(county.state);
            row.setAge(age);
            row.setUsesTobacco(false); // unused pending O19 — see V074 header
            row.setMarketLowPremium(scaleOrNull(marketLow, planYear, age));
            row.setMarketHighPremium(scaleOrNull(marketHigh, planYear, age));
            row.setLcspPremium(scaleOrNull(lcsp, planYear, age));
            row.setBenchmarkSilverPremium(scaleOrNull(benchmarkSilver, planYear, age));
            row.setLowestBronzePremium(scaleOrNull(lowestBronze, planYear, age));
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

    private BigDecimal scaleOrNull(BigDecimal baseAt21, int planYear, int age) {
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
     * Standing check that the age-factor table is still correct for this plan year.
     * Called once per plan year (against the first county processed for that year —
     * see the runCanary flag in warmCounty). Makes a second quoteSingleApplicant call
     * at CANARY_AGE, matches at least one plan present in both responses by hiosId,
     * and compares the observed ratio against AgeCurve.factorFor. On any mismatch or
     * failure, logs a clear warning naming both values and returns — never aborts
     * the county or the run.
     */
    private void canaryCheck(int planYear, CountyTarget county, List<HealthSherpaService.PlanSummary> basePlans) {
        HealthSherpaService.HealthSherpaQuoteResponse canaryResponse = HealthSherpaService.quoteSingleApplicant(
                county.zip, county.fips, county.state, planYear, CANARY_AGE, false, true);

        if (!canaryResponse.isSuccess()) {
            log.warn("[RATE-CACHE] Plan year {} canary (age {}, county {}) call failed: {} — age curve unverified this run",
                    planYear, CANARY_AGE, county, canaryResponse.getErrorMessage());
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
                log.warn("[RATE-CACHE] Canary mismatch for plan year {} county {} plan {}: age-{} premium {} scaled to age-{} " +
                                "expects {}, HealthSherpa returned {} (diff {}) — AgeCurve may be stale for plan year {}",
                        planYear, county, basePlan.getHiosId(), BASE_AGE, basePremium, CANARY_AGE, expectedCanary,
                        canaryPremium, diff, planYear);
            } else {
                log.debug("[RATE-CACHE] Canary check passed for plan year {} county {} plan {}", planYear, county, basePlan.getHiosId());
            }
            return; // one confirmed match is sufficient per Part 4 step 4
        }

        log.warn("[RATE-CACHE] Plan year {} county {} — no plan matched by hiosId between age-{} and age-{} responses; " +
                "canary check skipped this run", planYear, county, BASE_AGE, CANARY_AGE);
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
     * Reads RATE_CACHE_PLAN_YEARS as a comma-separated list of plan years (D-84).
     * Individual malformed tokens are logged and skipped; the list is only treated
     * as wholly unparseable (empty result, triggering a full-run skip in warmAll())
     * if zero valid years survive.
     */
    private List<Integer> readConfiguredPlanYears(EntityManager em) {
        List<Integer> result = new ArrayList<>();
        String raw = AppConstantDAO.getConstantValue(em, "RATE_CACHE_PLAN_YEARS");
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                log.warn("[RATE-CACHE] Skipping malformed RATE_CACHE_PLAN_YEARS entry: '{}'", trimmed);
            }
        }
        return result;
    }

    /**
     * Reads RATE_CACHE_COUNTIES as a comma-separated list of entries, each in one
     * of two forms (see class-level deviation note #2):
     * <ul>
     *   <li>Legacy triple {@code zip:fips:state} — used as-is, no database lookup.</li>
     *   <li>Bare {@code county_fips} — resolved via {@link CountyReferenceDAO#findByFips}
     *       to supply zip and state from the {@code county_reference} row (V076).</li>
     * </ul>
     * Malformed or unresolvable entries are logged and skipped, not fatal to the run.
     */
    private List<CountyTarget> readConfiguredCounties(EntityManager em) {
        List<CountyTarget> result = new ArrayList<>();
        String raw = AppConstantDAO.getConstantValue(em, "RATE_CACHE_COUNTIES");
        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split(":");

            if (parts.length == 3) {
                if (parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
                    log.warn("[RATE-CACHE] Skipping malformed RATE_CACHE_COUNTIES entry: '{}' (expected zip:fips:state or bare county_fips)", trimmed);
                    continue;
                }
                result.add(new CountyTarget(parts[0].trim(), parts[1].trim(), parts[2].trim().toUpperCase()));
            } else if (parts.length == 1) {
                CountyReference county = CountyReferenceDAO.findByFips(em, parts[0]);
                if (county == null) {
                    log.warn("[RATE-CACHE] Skipping RATE_CACHE_COUNTIES entry '{}' — no county_reference row for FIPS '{}'", trimmed, parts[0]);
                    continue;
                }
                result.add(new CountyTarget(county.getRepresentativeZip(), county.getCountyFips(), county.getState()));
            } else {
                log.warn("[RATE-CACHE] Skipping malformed RATE_CACHE_COUNTIES entry: '{}' (expected zip:fips:state or bare county_fips)", trimmed);
            }
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

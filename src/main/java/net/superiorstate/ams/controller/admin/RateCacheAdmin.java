package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.CountyReferenceDAO;
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.service.HealthSherpaService;
import net.superiorstate.ams.data.service.RateCacheWarmService;
import net.superiorstate.ams.model.market.CountyReference;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PSP Admin — A1 rate-cache status page. Displays whether the warm job is enabled
 * on this instance, per-county cache status, and a manual refresh trigger.
 * <p>
 * Also hosts a rate-cache diagnostic panel (build-plan item 8 support): two live
 * HealthSherpa quotes identical except {@code off_ex}, rendered side by side. The
 * diagnostic is <b>read-only</b> — it writes no cache row, no {@code illustration_log}
 * row, and no file. It exists because the T44 probe (does {@code off_ex: false}
 * return a distinct, plausible on-exchange silver set?) cannot run from a workstation
 * without a HealthSherpa credential; this panel lets it run wherever the credential
 * already is.
 */
@WebServlet(name = "RateCacheAdmin", value = "/RateCacheAdmin")
public class RateCacheAdmin extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String DIAGNOSTIC_ZIP_DEFAULT = "75482";
    private static final String DIAGNOSTIC_FIPS_DEFAULT = "48223";
    private static final String DIAGNOSTIC_AGE_DEFAULT = "40";
    private static final String DIAGNOSTIC_PLAN_YEAR_DEFAULT = "2026";
    private static final int MIN_AGE = 21;
    private static final int MAX_AGE = 64;
    private static final int MIN_PLAN_YEAR = 2000;
    private static final int MAX_PLAN_YEAR = 2100;

    /** Matches RateCacheWarmService's own metal-level literal — kept in sync manually, not shared, per this item's scope fence (RateCacheWarmService is not editable here). */
    private static final String METAL_SILVER = "Silver";

    /** Display string for JSP rendering — never format a java.time value in a JSP taglib. */
    private static String formatDisplay(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_FORMAT);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        RateCacheWarmService warmService = (RateCacheWarmService) getServletContext().getAttribute("rateCacheWarmService");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");

        EntityManager em = emf.createEntityManager();
        try {
            String warmEnabledConstant = AppConstantDAO.getConstantValue(em, "RATE_CACHE_WARM_ENABLED");
            String planYearsConstant = AppConstantDAO.getConstantValue(em, "RATE_CACHE_PLAN_YEARS");
            List<Integer> planYears = parsePlanYears(planYearsConstant);

            // Plan years are configuration (RATE_CACHE_PLAN_YEARS, D-84), never derived from
            // the current date — see RateCacheWarmService's class Javadoc for why. This page
            // displays whatever is actually configured, which may be zero, one, or several years
            // (e.g. "2026,2027" during open enrollment).
            Map<Integer, List<RateCacheDAO.CountySummary>> countySummariesByYear = new LinkedHashMap<>();
            for (Integer year : planYears) {
                countySummariesByYear.put(year, RateCacheDAO.getCountySummaries(em, year));
            }

            request.setAttribute("warmEnabled", warmService != null);
            request.setAttribute("warmEnabledConstant", warmEnabledConstant);
            request.setAttribute("sourceEnv", RateCacheWarmService.currentSourceEnv());
            request.setAttribute("planYearsConstant", planYearsConstant);
            request.setAttribute("configuredPlanYears", planYears);
            request.setAttribute("runInProgress", warmService != null && warmService.isRunInProgress());
            request.setAttribute("lastRunSummary", warmService != null ? warmService.getLastRunSummary() : null);
            LocalDateTime lastRunAt = warmService != null ? warmService.getLastRunAt() : null;
            request.setAttribute("lastRunAt", lastRunAt);
            request.setAttribute("lastRunAtDisplay", formatDisplay(lastRunAt));
            request.setAttribute("countySummariesByYear", countySummariesByYear);

            String zipParam = firstNonBlank(request.getParameter("zip"), DIAGNOSTIC_ZIP_DEFAULT);
            String fipsParam = firstNonBlank(request.getParameter("fips"), DIAGNOSTIC_FIPS_DEFAULT);
            String ageParam = firstNonBlank(request.getParameter("age"), DIAGNOSTIC_AGE_DEFAULT);
            String planYearParam = firstNonBlank(request.getParameter("planYear"), DIAGNOSTIC_PLAN_YEAR_DEFAULT);
            request.setAttribute("diagnosticZip", zipParam);
            request.setAttribute("diagnosticFips", fipsParam);
            request.setAttribute("diagnosticAge", ageParam);
            request.setAttribute("diagnosticPlanYear", planYearParam);

            if ("diagnostic".equals(request.getParameter("action"))) {
                runDiagnostic(em, request, zipParam, fipsParam, ageParam, planYearParam);
            }

            request.setAttribute("pageTitle", "Rate Cache");
            request.setAttribute("pageIcon", "bi-graph-up");
            request.getRequestDispatcher("/WEB-INF/view/a/admin/rateCacheAdmin25.jsp").forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /** Tolerant parse matching RateCacheWarmService's own RATE_CACHE_PLAN_YEARS handling — display only. */
    private List<Integer> parsePlanYears(String raw) {
        List<Integer> years = new ArrayList<>();
        if (raw == null || raw.isBlank()) return years;
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            try {
                years.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                // malformed entries are reported by RateCacheWarmService's own run logs; this
                // display-only parse just skips them
            }
        }
        return years;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String action = request.getParameter("action");
        if ("refresh".equals(action)) {
            RateCacheWarmService warmService = (RateCacheWarmService) getServletContext().getAttribute("rateCacheWarmService");
            if (warmService == null) {
                session.setAttribute("rateCacheError", "Rate-cache warming is not enabled on this instance.");
            } else {
                RateCacheWarmService.WarmTriggerResult result = warmService.triggerManualWarm();
                if (result == RateCacheWarmService.WarmTriggerResult.STARTED) {
                    session.setAttribute("rateCacheMessage", "Warm run started.");
                } else {
                    session.setAttribute("rateCacheMessage", "A warm run is already in progress.");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/RateCacheAdmin");
    }

    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    // ── Diagnostic ────────────────────────────────────────────────────

    /**
     * Runs the rate-cache diagnostic: two live HealthSherpa quotes identical except
     * {@code off_ex}, over the same GET request that already renders the page. Read-only
     * — no cache row is written, no {@code illustration_log} row is written, no file is
     * written. Validation failure sets {@code diagnosticError} and returns without
     * issuing either call; the caller's already-echoed field attributes let the form
     * re-render the submitted (possibly invalid) values rather than silently resetting.
     */
    private void runDiagnostic(EntityManager em, HttpServletRequest request,
                                String zipParam, String fipsParam, String ageParam, String planYearParam) {
        List<String> errors = new ArrayList<>();

        String zip = (zipParam != null && zipParam.matches("\\d{5}")) ? zipParam : null;
        if (zip == null) errors.add("ZIP must be exactly 5 digits.");

        String fips = (fipsParam != null && fipsParam.matches("\\d{5}")) ? fipsParam : null;
        if (fips == null) errors.add("County FIPS must be exactly 5 digits.");

        Integer age = parseBoundedInt(ageParam, MIN_AGE, MAX_AGE);
        if (age == null) errors.add("Age must be a whole number from " + MIN_AGE + " to " + MAX_AGE + ".");

        Integer planYear = parseBoundedInt(planYearParam, MIN_PLAN_YEAR, MAX_PLAN_YEAR);
        if (planYear == null) errors.add("Plan year must be a 4-digit year.");

        // quoteSingleApplicant requires a state, which this panel's inputs (ZIP/FIPS/age/
        // plan year only) don't collect directly — resolved from the county_reference row
        // already keyed by FIPS, same source RateCacheWarmService itself uses for the bare-
        // FIPS RATE_CACHE_COUNTIES form (V076).
        String state = null;
        if (fips != null) {
            CountyReference county = CountyReferenceDAO.findByFips(em, fips);
            if (county == null) {
                errors.add("No county_reference row for FIPS " + fips + " — cannot resolve state.");
            } else {
                state = county.getState();
            }
        }

        if (!errors.isEmpty()) {
            request.setAttribute("diagnosticError", String.join(" ", errors));
            return;
        }

        request.setAttribute("diagnosticOffExchange", runOneQuote(zip, fips, state, planYear, age, true));
        request.setAttribute("diagnosticOnExchange", runOneQuote(zip, fips, state, planYear, age, false));
    }

    /**
     * One side of the diagnostic. Mirrors RateCacheWarmService's own silver/lowest/
     * second-lowest derivation (lowest-premium Silver plan is LCSP, second-lowest is
     * benchmark) so a Kevin-eyeballed comparison against the cache's off-exchange values
     * is apples to apples. Never writes anything — this method only reads.
     */
    private RateCacheDiagnosticResult runOneQuote(String zip, String fips, String state,
                                                   int planYear, int age, boolean offExchange) {
        String baseUrl = AppConfig.getHealthSherpaBaseUrl();
        HealthSherpaService.HealthSherpaQuoteResponse response =
                HealthSherpaService.quoteSingleApplicant(zip, fips, state, planYear, age, false, offExchange);

        if (!response.isSuccess()) {
            return RateCacheDiagnosticResult.failure(offExchange, baseUrl, response.getErrorMessage());
        }

        List<HealthSherpaService.PlanSummary> plans = response.getPlans();
        Map<String, Integer> metalBreakdown = new LinkedHashMap<>();
        List<BigDecimal> silverPremiumsSorted = new ArrayList<>();
        Set<String> issuers = new LinkedHashSet<>();

        for (HealthSherpaService.PlanSummary plan : plans) {
            String metal = plan.getMetalLevel() != null ? plan.getMetalLevel() : "(none)";
            metalBreakdown.merge(metal, 1, Integer::sum);

            if (METAL_SILVER.equals(plan.getMetalLevel())) {
                BigDecimal premium = basePremiumOf(plan);
                if (premium != null) silverPremiumsSorted.add(premium);
            }
            if (plan.getIssuerName() != null) {
                issuers.add(plan.getIssuerName());
            }
        }

        Collections.sort(silverPremiumsSorted);
        BigDecimal lowestSilver = silverPremiumsSorted.isEmpty() ? null : silverPremiumsSorted.get(0);
        BigDecimal secondLowestSilver = silverPremiumsSorted.size() >= 2 ? silverPremiumsSorted.get(1) : null;

        return RateCacheDiagnosticResult.success(offExchange, baseUrl, response.getResultCount(),
                plans.size(), metalBreakdown, lowestSilver, secondLowestSilver, issuers.size());
    }

    /** Prefers grossPremium, falls back to premium — matches RateCacheWarmService.basePremiumOf exactly. */
    private BigDecimal basePremiumOf(HealthSherpaService.PlanSummary plan) {
        if (plan.getGrossPremium() != null) {
            return BigDecimal.valueOf(plan.getGrossPremium());
        }
        if (plan.getPremium() != null) {
            return BigDecimal.valueOf(plan.getPremium());
        }
        return null;
    }

    private Integer parseBoundedInt(String raw, int min, int max) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            return (value >= min && value <= max) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    /**
     * One side of the rate-cache diagnostic (one {@code off_ex} value) — a read-only
     * view model, never persisted. {@code baseUrl} is safe to display (it names which
     * environment answered, not a secret); the API key never appears on this object or
     * anywhere the JSP can reach.
     */
    public static final class RateCacheDiagnosticResult {
        private final boolean offExchange;
        private final String baseUrl;
        private final boolean success;
        private final String errorMessage;
        private final int accumulatedPlanCount;
        private final int resultCount;
        private final Map<String, Integer> metalBreakdown;
        private final BigDecimal lowestSilver;
        private final BigDecimal secondLowestSilver;
        private final int distinctIssuerCount;

        private RateCacheDiagnosticResult(boolean offExchange, String baseUrl, boolean success, String errorMessage,
                                           int accumulatedPlanCount, int resultCount, Map<String, Integer> metalBreakdown,
                                           BigDecimal lowestSilver, BigDecimal secondLowestSilver, int distinctIssuerCount) {
            this.offExchange = offExchange;
            this.baseUrl = baseUrl;
            this.success = success;
            this.errorMessage = errorMessage;
            this.accumulatedPlanCount = accumulatedPlanCount;
            this.resultCount = resultCount;
            this.metalBreakdown = metalBreakdown;
            this.lowestSilver = lowestSilver;
            this.secondLowestSilver = secondLowestSilver;
            this.distinctIssuerCount = distinctIssuerCount;
        }

        static RateCacheDiagnosticResult success(boolean offExchange, String baseUrl, int resultCount, int accumulatedPlanCount,
                                                  Map<String, Integer> metalBreakdown, BigDecimal lowestSilver,
                                                  BigDecimal secondLowestSilver, int distinctIssuerCount) {
            return new RateCacheDiagnosticResult(offExchange, baseUrl, true, null, accumulatedPlanCount, resultCount,
                    metalBreakdown, lowestSilver, secondLowestSilver, distinctIssuerCount);
        }

        static RateCacheDiagnosticResult failure(boolean offExchange, String baseUrl, String errorMessage) {
            return new RateCacheDiagnosticResult(offExchange, baseUrl, false, errorMessage, 0, 0,
                    Map.of(), null, null, 0);
        }

        public boolean isOffExchange() { return offExchange; }
        public String getBaseUrl() { return baseUrl; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public int getAccumulatedPlanCount() { return accumulatedPlanCount; }
        public int getResultCount() { return resultCount; }
        public Map<String, Integer> getMetalBreakdown() { return metalBreakdown; }
        public BigDecimal getLowestSilver() { return lowestSilver; }
        public BigDecimal getSecondLowestSilver() { return secondLowestSilver; }
        public int getDistinctIssuerCount() { return distinctIssuerCount; }
    }
}

package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.CountyReferenceDAO;
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.resolver.AgencyScope;
import net.superiorstate.ams.data.resolver.AgencyScopeResolver;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.ZipCountyResolver;
import net.superiorstate.ams.data.util.AffordabilityCalculator;
import net.superiorstate.ams.data.util.OpportunityAuthz;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.market.CountyReference;
import net.superiorstate.ams.model.market.IllustrationLog;
import net.superiorstate.ams.model.market.RatingAreaRateCache;
import net.superiorstate.ams.model.sales.agency.Agency;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Agent-facing ICHRA rating illustration. Two modes, selected by the {@code mode}
 * request parameter (default {@code RANGE} so every existing entry point behaves
 * exactly as before this class gained a second mode):
 * <ul>
 *     <li>{@code RANGE} (B-Phase-2a) — a bronze-floor range across three representative
 *     ages (21/40/64) times a flat headcount.</li>
 *     <li>{@code AGE_BAND} (B-Phase-2b, build-plan item 5) — repeating (age, count) rows
 *     plus an employer monthly contribution, rendering per-age-band net cost after
 *     contribution and a group monthly total. Optionally, with an
 *     {@code affordabilityBasis} selected, also renders the affordability threshold
 *     per row (build-plan item 9) — an employer/agent-facing ANALYSIS, never a
 *     determination and never advice to any employee (LA-12); see
 *     {@link net.superiorstate.ams.data.util.AffordabilityCalculator} and
 *     {@code illustration25.jsp} for the compliance boundaries this page observes. No
 *     subsidy or PTC dollar figure is ever computed or shown — only PTC eligibility as
 *     kept or lost.</li>
 * </ul>
 * <p>
 * GET only — an illustration is a query, not a state change, and GET makes results
 * linkable and renderable same-request (unlike {@code RateCacheAdmin}'s POST/redirect
 * flow, which cannot render results on the same request it receives them).
 */
@WebServlet(name = "IllustrationServlet", value = "/Illustration")
public class IllustrationServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(IllustrationServlet.class);

    private static final int[] REPRESENTATIVE_AGES = {21, 40, 64};
    private static final int MAX_HEADCOUNT = 10000;
    private static final int RESULT_SUMMARY_MAX_LENGTH = 255;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String MODE_RANGE = "RANGE";
    private static final String MODE_AGE_BAND = "AGE_BAND";
    private static final int AGE_BAND_ROWS = 6;
    private static final int MIN_AGE = 21;
    private static final int MAX_AGE = 64;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorized(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            request.setAttribute("pageTitle", "ICHRA Illustration");
            request.setAttribute("pageIcon", "bi-calculator");

            String mode = MODE_AGE_BAND.equals(request.getParameter("mode")) ? MODE_AGE_BAND : MODE_RANGE;
            request.setAttribute("mode", mode);

            // Build-plan item 13 — optional opportunity attribution. Resolved once here so
            // every forward path below (including the early returns) carries it, and read
            // back out of the request by logIllustration. Null whenever absent or not
            // permitted; never an error and never a message on screen.
            request.setAttribute("opportunityId", resolveOpportunityId(em, request));

            String planYearsConstant = AppConstantDAO.getConstantValue(em, "RATE_CACHE_PLAN_YEARS");
            List<Integer> configuredPlanYears = parsePlanYears(planYearsConstant);
            request.setAttribute("configuredPlanYears", configuredPlanYears);

            if (configuredPlanYears.isEmpty()) {
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }

            int planYear = resolvePlanYear(request, configuredPlanYears);
            request.setAttribute("selectedPlanYear", planYear);

            List<RateCacheDAO.CountySummary> summaries = RateCacheDAO.getCountySummaries(em, planYear);
            List<String> cachedFips = summaries.stream()
                    .map(RateCacheDAO.CountySummary::getCountyFips)
                    .collect(Collectors.toList());

            List<CountyReference> availableCounties = CountyReferenceDAO.findByFipsIn(em, cachedFips);
            int missingReferenceCount = cachedFips.size() - availableCounties.size();
            request.setAttribute("availableCounties", availableCounties);
            request.setAttribute("missingReferenceCount", missingReferenceCount);

            String countyFips = request.getParameter("countyFips");

            // ── T74 ZIP intake — precedence (R1) ──────────────────────────────────
            //
            // ⚠️ THIS REPLACES A RULE THAT SHIPPED WRONG. The original read "consulted
            // only when no county is present, so countyFips always wins", written to
            // protect the ?countyFips= URL contract. It does protect it — and "always
            // wins" also meant a STALE DROPDOWN SELECTION beat a freshly typed ZIP.
            // Observed on production 2026-08-01: Hopkins left selected from a previous
            // run, agent typed 75009 (Collin/Denton), and got Hopkins rates with no
            // warning. Wrong county, wrong rates, indistinguishable from right ones,
            // in front of a client. It is the exact failure the chooser exists to
            // prevent, arriving through a different door.
            //
            // The rule now: a present ZIP is always resolved, and a county that the
            // ZIP contradicts is never computed from. Not with a warning — not at all.
            //
            //   zip blank                          -> county wins (contract preserved)
            //   zip agrees with selected county    -> proceed on that county
            //   zip disagrees, resolves to one     -> the ZIP replaces the selection
            //   zip disagrees, resolves to several -> chooser; nothing computed
            //   zip resolves to nothing            -> no-match; NO fallback to county
            //
            String zipParam = request.getParameter("zip");
            if (zipParam != null && !zipParam.isBlank()) {
                request.setAttribute("submittedZip", zipParam.trim());
                ZipCountyResolver.Resolution resolution = ZipCountyResolver.resolve(em, zipParam);

                if (resolution.containsCounty(countyFips)) {
                    // They agree. Keep the explicit selection — this is how the chooser's
                    // own links land, carrying both zip and countyFips.
                    request.setAttribute("resolvedCountyFips", countyFips);
                } else if (resolution.isUnique()) {
                    // Either no county was selected, or the selected one is contradicted.
                    // Both resolve the same way: the ZIP the agent just typed governs.
                    countyFips = resolution.getUnique().getCountyFips();
                    request.setAttribute("resolvedCounty", resolution.getUnique());
                } else if (resolution.isAmbiguous()) {
                    // The common path — 34% of Texas ZIPs. The agent picks; nothing here
                    // selects, ranks by preference, or marks a likely answer. Note that
                    // submittedCountyFips is deliberately left unset by returning here, so
                    // a contradicted stale county does not stay selected in the dropdown
                    // underneath the chooser.
                    request.setAttribute("zipCandidates", resolution.getCandidates());
                    request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                    return;
                } else {
                    // Coverage gap, not a bad ZIP — the crosswalk is ZCTA-derived and
                    // Texas-only, so a real USPS ZIP can legitimately be absent.
                    // Deliberately NOT an inputError; this is not the agent's mistake.
                    // ⚠️ And deliberately NOT a fallback to whatever county happened to
                    // be selected: an unresolvable ZIP agrees with nothing.
                    request.setAttribute("zipNoMatch", true);
                    request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                    return;
                }
            }

            if (countyFips == null || countyFips.isBlank()) {
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }
            request.setAttribute("submittedCountyFips", countyFips);

            // `countyFips` is no longer effectively final — the ZIP branch above may have
            // assigned it — so the lambda captures a final copy instead.
            final String resolvedFips = countyFips;
            CountyReference selectedCounty = availableCounties.stream()
                    .filter(c -> resolvedFips.equals(c.getCountyFips()))
                    .findFirst()
                    .orElse(null);
            if (selectedCounty == null) {
                // `availableCounties` is counties that HAVE cached rates, so landing here
                // means the county is real but unwarmed. Outcome is unchanged from before
                // this run — still an error, still no rates, still T76's job to fix.
                // Only the wording differs, and only on the ZIP path: telling an agent who
                // typed a ZIP to "select a valid county from the list" describes neither
                // what they did nor what went wrong, and would collapse the unwarmed-county
                // case into the coverage-gap case that the block above reports separately.
                Object resolved = request.getAttribute("resolvedCounty");
                if (resolved instanceof ZipCountyResolver.Candidate) {
                    ZipCountyResolver.Candidate candidate = (ZipCountyResolver.Candidate) resolved;
                    request.setAttribute("inputError",
                            "That ZIP is in " + candidate.getCountyName() + ", " + candidate.getState()
                                    + ", which has no cached rates yet. Pick another county below.");
                } else {
                    request.setAttribute("inputError", "Select a valid county from the list.");
                }
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }
            request.setAttribute("selectedCounty", selectedCounty);

            if (MODE_AGE_BAND.equals(mode)) {
                handleAgeBandMode(em, request, response, planYear, countyFips, selectedCounty);
            } else {
                handleRangeMode(em, request, response, planYear, countyFips, selectedCounty);
            }
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /** RANGE mode — unchanged from B-Phase-2a other than being extracted into its own method and passing its mode literal into the shared log call. */
    private void handleRangeMode(EntityManager em, HttpServletRequest request, HttpServletResponse response,
                                  int planYear, String countyFips, CountyReference selectedCounty)
            throws ServletException, IOException {
        String headcountParam = request.getParameter("headcount");
        request.setAttribute("submittedHeadcount", headcountParam);
        Integer headcount = parseHeadcount(headcountParam);
        if (headcount == null) {
            request.setAttribute("inputError", "Enter a valid number of eligible employees (1-" + MAX_HEADCOUNT + ").");
            request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
            return;
        }

        List<RatingAreaRateCache> allRows = RateCacheDAO.getRatesForCounty(em, planYear, countyFips);
        List<RatingAreaRateCache> nonTobaccoRows = allRows.stream()
                .filter(r -> !r.isUsesTobacco())
                .collect(Collectors.toList());

        boolean hasRates = !nonTobaccoRows.isEmpty();
        request.setAttribute("hasRates", hasRates);

        String resultSummary;
        if (hasRates) {
            Map<Integer, RatingAreaRateCache> byAge = nonTobaccoRows.stream()
                    .collect(Collectors.toMap(RatingAreaRateCache::getAge, r -> r, (a, b) -> a));

            RatingAreaRateCache age21Row = byAge.get(REPRESENTATIVE_AGES[0]);
            RatingAreaRateCache age40Row = byAge.get(REPRESENTATIVE_AGES[1]);
            RatingAreaRateCache age64Row = byAge.get(REPRESENTATIVE_AGES[2]);
            request.setAttribute("age21Row", age21Row);
            request.setAttribute("age40Row", age40Row);
            request.setAttribute("age64Row", age64Row);

            BigDecimal groupMonthlyLow = groupPremium(age21Row, headcount);
            BigDecimal groupMonthlyHigh = groupPremium(age64Row, headcount);
            request.setAttribute("groupMonthlyLow", groupMonthlyLow);
            request.setAttribute("groupMonthlyHigh", groupMonthlyHigh);

            // Count row is deterministic (age 40, the reference age used elsewhere in this
            // servlet) because carrierCount/planCount are age-specific — catastrophic plans
            // are under-30 only, so age 21 can report a different plan count than age 40.
            RatingAreaRateCache countRow = age40Row != null ? age40Row : nonTobaccoRows.get(0);
            int countRowAge = countRow.getAge();
            request.setAttribute("carrierCount", countRow.getCarrierCount());
            request.setAttribute("planCount", countRow.getPlanCount());
            request.setAttribute("countRowAge", countRowAge);

            setProvenanceAttributes(request, nonTobaccoRows);

            resultSummary = buildResultSummary(headcount, selectedCounty, planYear, groupMonthlyLow, groupMonthlyHigh);
        } else {
            resultSummary = "RANGE: " + headcount + " lives, " + selectedCounty.getCountyName() + " "
                    + selectedCounty.getState() + ", PY" + planYear + ", no rate data";
        }

        logIllustration(em, request, selectedCounty, planYear, headcount, hasRates, resultSummary, MODE_RANGE);

        request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
    }

    /**
     * AGE_BAND mode (build-plan item 5) — repeating (age, count) rows plus an employer
     * monthly contribution. Mirrors RANGE mode's shape: parse and validate inputs,
     * read the cache (never compute a premium — every age 21-64 is already cached),
     * compute, log, forward. A missing cached age is a cache-completeness problem
     * reported on screen, never interpolated.
     */
    private void handleAgeBandMode(EntityManager em, HttpServletRequest request, HttpServletResponse response,
                                    int planYear, String countyFips, CountyReference selectedCounty)
            throws ServletException, IOException {
        String[] submittedAges = new String[AGE_BAND_ROWS];
        String[] submittedCounts = new String[AGE_BAND_ROWS];
        String[] submittedIncomes = new String[AGE_BAND_ROWS];
        for (int i = 0; i < AGE_BAND_ROWS; i++) {
            submittedAges[i] = request.getParameter("age" + (i + 1));
            submittedCounts[i] = request.getParameter("count" + (i + 1));
            submittedIncomes[i] = request.getParameter("income" + (i + 1));
        }
        request.setAttribute("submittedAges", submittedAges);
        request.setAttribute("submittedCounts", submittedCounts);
        request.setAttribute("submittedIncomes", submittedIncomes);

        String contributionParam = request.getParameter("contribution");
        request.setAttribute("submittedContribution", contributionParam);

        // Affordability (build item 9) is opt-in — a basis must be explicitly selected.
        // No selection means the affordability section stays hidden entirely; the
        // net-cost table below is unaffected either way.
        String affordabilityBasis = request.getParameter("affordabilityBasis");
        request.setAttribute("affordabilityBasis", affordabilityBasis);
        boolean incomeBasis = "INCOME".equals(affordabilityBasis);

        List<AgeBandRow> rows = new ArrayList<>();
        for (int i = 0; i < AGE_BAND_ROWS; i++) {
            String ageRaw = submittedAges[i];
            if (ageRaw == null || ageRaw.isBlank()) {
                continue; // blank age = ignored row
            }
            Integer age = parseAge(ageRaw);
            if (age == null) {
                request.setAttribute("inputError", "Row " + (i + 1) + ": age must be a whole number from " + MIN_AGE + " to " + MAX_AGE + ".");
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }
            Integer count = parseBandCount(submittedCounts[i]);
            if (count == null) {
                request.setAttribute("inputError", "Row " + (i + 1) + ": count must be a positive whole number.");
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }
            BigDecimal income = null;
            if (incomeBasis) {
                income = parsePositiveDecimal(submittedIncomes[i]);
                if (income == null) {
                    request.setAttribute("inputError", "Row " + (i + 1) + ": enter a valid annual household income (greater than 0) for the entered-income basis.");
                    request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                    return;
                }
            }
            rows.add(new AgeBandRow(age, count, income));
        }

        if (rows.isEmpty()) {
            request.setAttribute("inputError", "Enter at least one age.");
            request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
            return;
        }

        BigDecimal contribution = parseContribution(contributionParam);
        if (contribution == null) {
            request.setAttribute("inputError", "Enter a valid employer monthly contribution (0 or more).");
            request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
            return;
        }

        int totalLives = 0;
        for (AgeBandRow row : rows) {
            totalLives += row.getCount();
        }

        List<Integer> distinctAges = new ArrayList<>();
        for (AgeBandRow row : rows) {
            if (!distinctAges.contains(row.getAge())) {
                distinctAges.add(row.getAge());
            }
        }

        // Every age 21-64 is already cached — read the row, never compute the curve here.
        // A missing row (or a cached row with no bronze premium) is a cache-completeness
        // problem, not something this servlet interpolates around.
        Map<Integer, RatingAreaRateCache> cacheByAge = new LinkedHashMap<>();
        List<Integer> missingAges = new ArrayList<>();
        for (Integer age : distinctAges) {
            RatingAreaRateCache cacheRow = RateCacheDAO.getRate(em, planYear, countyFips, age, false);
            if (cacheRow == null || cacheRow.getLowestBronzePremium() == null) {
                missingAges.add(age);
            } else {
                cacheByAge.put(age, cacheRow);
            }
        }

        boolean hasRates = missingAges.isEmpty();
        request.setAttribute("hasRates", hasRates);

        String resultSummary;
        if (hasRates) {
            List<AgeBandResultRow> resultRows = new ArrayList<>();
            BigDecimal groupNetTotal = BigDecimal.ZERO;
            for (AgeBandRow row : rows) {
                RatingAreaRateCache cacheRow = cacheByAge.get(row.getAge());
                BigDecimal floorPremium = cacheRow.getLowestBronzePremium();
                // Clamp at zero — a contribution exceeding the floor premium means the
                // employee's cost is zero, not negative.
                BigDecimal netPerEmployee = floorPremium.subtract(contribution).max(BigDecimal.ZERO);
                BigDecimal bandNet = netPerEmployee.multiply(BigDecimal.valueOf(row.getCount()));
                resultRows.add(new AgeBandResultRow(row.getAge(), row.getCount(), floorPremium, netPerEmployee, bandNet));
                groupNetTotal = groupNetTotal.add(bandNet);
            }
            request.setAttribute("ageBandResultRows", resultRows);
            request.setAttribute("groupNetTotal", groupNetTotal);

            BigDecimal employerOutlay = contribution.multiply(BigDecimal.valueOf(totalLives));
            request.setAttribute("employerOutlay", employerOutlay);
            request.setAttribute("submittedTotalLives", totalLives);

            setProvenanceAttributes(request, new ArrayList<>(cacheByAge.values()));

            computeAffordability(em, request, planYear, affordabilityBasis, contribution, rows, cacheByAge);

            resultSummary = "AGE_BAND: " + totalLives + " lives, " + selectedCounty.getCountyName() + " "
                    + selectedCounty.getState() + ", PY" + planYear + ", group net floor " + money(groupNetTotal);
        } else {
            request.setAttribute("missingAges", missingAges);
            resultSummary = "AGE_BAND: " + totalLives + " lives, " + selectedCounty.getCountyName() + " "
                    + selectedCounty.getState() + ", PY" + planYear + ", missing cache data";
        }

        logIllustration(em, request, selectedCounty, planYear, totalLives, hasRates,
                truncate(resultSummary, RESULT_SUMMARY_MAX_LENGTH), MODE_AGE_BAND);

        request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
    }

    /**
     * Affordability threshold (build-plan item 9) — opt-in via {@code affordabilityBasis}
     * ({@code "FPL"} or {@code "INCOME"}; anything else leaves the section hidden). An
     * employer/agent-facing ANALYSIS, never a determination and never advice to any
     * employee (LA-12). Fails closed on missing configuration: no
     * {@code ICHRA_AFFORDABILITY_PCT_<planYear>} constant means no affordability output
     * at all for that plan year — never a default, never a prior year's value carried
     * forward. Reads {@link RatingAreaRateCache#getOnexLcspPremium()} exclusively —
     * never {@link RatingAreaRateCache#getLcspPremium()}, the off-exchange figure T44
     * found understates the true on-exchange LCSP by roughly 44% in the reference
     * county, the dangerous direction (a too-low LCSP makes an unaffordable offer look
     * affordable).
     */
    private void computeAffordability(EntityManager em, HttpServletRequest request, int planYear,
                                       String basis, BigDecimal contribution,
                                       List<AgeBandRow> rows, Map<Integer, RatingAreaRateCache> cacheByAge) {
        if (!"FPL".equals(basis) && !"INCOME".equals(basis)) {
            return;
        }

        // Name the missing constant, not just the concept. "Affordability is not
        // configured" tells the agent to give up; naming the row tells Kevin what to add,
        // and these two rows reach an existing installation only by hand — the
        // DatabaseInitializer seed runs on fresh installs only.
        BigDecimal applicablePct = parsePositiveDecimal(AppConstantDAO.getConstantValue(em, "ICHRA_AFFORDABILITY_PCT_" + planYear));
        if (applicablePct == null) {
            request.setAttribute("affordabilityUnavailableReason",
                    "Affordability is not configured for plan year " + planYear
                            + " — the constant ICHRA_AFFORDABILITY_PCT_" + planYear + " is missing or invalid.");
            return;
        }

        BigDecimal fplAnnual = null;
        if ("FPL".equals(basis)) {
            fplAnnual = parsePositiveDecimal(AppConstantDAO.getConstantValue(em, "FPL_ANNUAL_" + planYear));
            if (fplAnnual == null) {
                request.setAttribute("affordabilityUnavailableReason",
                        "FPL safe harbor is not configured for plan year " + planYear
                                + " — the constant FPL_ANNUAL_" + planYear + " is missing or invalid.");
                return;
            }
        }

        List<AffordabilityResultRow> affordabilityRows = new ArrayList<>();
        for (AgeBandRow row : rows) {
            RatingAreaRateCache cacheRow = cacheByAge.get(row.getAge());
            BigDecimal onexLcsp = cacheRow != null ? cacheRow.getOnexLcspPremium() : null;
            if (onexLcsp == null) {
                affordabilityRows.add(AffordabilityResultRow.unavailable(row.getAge(), row.getCount(),
                        "On-exchange rates not cached for this county; re-warm required."));
                continue;
            }

            // referenceIncome is never null here: the FPL basis already validated
            // fplAnnual above (or returned), and the INCOME basis already validated
            // every row's income during row parsing in handleAgeBandMode.
            BigDecimal referenceIncome = "FPL".equals(basis) ? fplAnnual : row.getIncome();
            BigDecimal flip = AffordabilityCalculator.flipContribution(onexLcsp, applicablePct, referenceIncome);
            boolean affordable = AffordabilityCalculator.isAffordable(contribution, flip);
            affordabilityRows.add(AffordabilityResultRow.available(row.getAge(), row.getCount(), onexLcsp, flip, affordable));
        }
        request.setAttribute("affordabilityRows", affordabilityRows);
    }

    private boolean isAuthorized(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            return IchraAccessResolver.isAvailable(em, request);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Build-plan item 13 — resolves the optional {@code opportunityId} request parameter
     * to an opportunity the caller may actually see, or null.
     * <p>
     * Scope validation is {@link OpportunityAuthz#canAccessOpportunity(EntityManager,
     * HttpServletRequest, long)} — the same predicate {@code UpdateOpportunityStage} and
     * {@code GoActivityDetail25} already use, which loads the row and delegates the
     * agency question to {@code AgencyScopeResolver.canSeeDetail}. No scoping logic is
     * written here; a second implementation of that rule is exactly what
     * {@code OpportunityAuthz} exists to prevent.
     * <p>
     * Fails quietly by design: absent, blank, unparseable, non-existent, or out of scope
     * all return null. An illustration is not an authorization surface — it works with or
     * without an opportunity, so a bad id must never error, redirect, or put a message on
     * screen. Returning null for out-of-scope also means an id belonging to another
     * agency cannot be written onto this agency's log row.
     */
    private Long resolveOpportunityId(EntityManager em, HttpServletRequest request) {
        String raw = request.getParameter("opportunityId");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long oppId = Long.parseLong(raw.trim());
            return OpportunityAuthz.canAccessOpportunity(em, request, oppId) ? oppId : null;
        } catch (NumberFormatException e) {
            return null;
        } catch (Exception e) {
            log.debug("[ILLUSTRATION] Opportunity attribution skipped; could not resolve id", e);
            return null;
        }
    }

    /** Tolerant parse matching RateCacheAdmin's own RATE_CACHE_PLAN_YEARS handling — display only. */
    private List<Integer> parsePlanYears(String raw) {
        List<Integer> years = new ArrayList<>();
        if (raw == null || raw.isBlank()) return years;
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            try {
                years.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                // malformed entries skipped, same tolerance as RateCacheWarmService's own parse
            }
        }
        return years;
    }

    /** Never accepts an arbitrary year — falls back to the first configured year. */
    private int resolvePlanYear(HttpServletRequest request, List<Integer> configuredPlanYears) {
        String param = request.getParameter("planYear");
        if (param != null) {
            try {
                int requested = Integer.parseInt(param.trim());
                if (configuredPlanYears.contains(requested)) {
                    return requested;
                }
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return configuredPlanYears.get(0);
    }

    /** @return the parsed headcount, or null if missing, non-numeric, or outside 1-MAX_HEADCOUNT. */
    private Integer parseHeadcount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1 || value > MAX_HEADCOUNT) return null;
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return the parsed age, or null if non-numeric or outside MIN_AGE-MAX_AGE. Caller handles blank (ignored row) before this is called. */
    private Integer parseAge(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < MIN_AGE || value > MAX_AGE) return null;
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return the parsed band count, defaulting to 1 when blank; null if non-numeric or not a positive integer up to MAX_HEADCOUNT. */
    private Integer parseBandCount(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try {
            int value = Integer.parseInt(raw.trim());
            return (value > 0 && value <= MAX_HEADCOUNT) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return the parsed employer monthly contribution, or null if missing, non-numeric, or negative. */
    private BigDecimal parseContribution(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.signum() >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @return the parsed positive decimal, or null if missing, non-numeric, or not
     * strictly positive. Used for both entered household income and the two
     * affordability configuration constants — all three must be a genuine positive
     * number, never zero or negative, and a missing/invalid value must fail closed
     * rather than default.
     */
    private BigDecimal parsePositiveDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.signum() > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Lowest-bronze premium at the given age times headcount, or null if the underlying value is null. */
    private BigDecimal groupPremium(RatingAreaRateCache ageRow, int headcount) {
        if (ageRow == null || ageRow.getLowestBronzePremium() == null) {
            return null;
        }
        return ageRow.getLowestBronzePremium().multiply(BigDecimal.valueOf(headcount));
    }

    /**
     * Sets fetchedAt/fetchedAtDisplay/sourceEnv request attributes from the rows actually
     * loaded for this run — shared by both modes so the staging-provenance banner in
     * illustration25.jsp behaves identically regardless of mode. Provenance is read from
     * the loaded rows themselves (whatever was stamped when they were cached), not from
     * RateCacheWarmService's current config, which can change after the rows were fetched.
     */
    private void setProvenanceAttributes(HttpServletRequest request, List<RatingAreaRateCache> rows) {
        LocalDateTime newestFetchedAt = rows.stream()
                .map(RatingAreaRateCache::getFetchedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        request.setAttribute("fetchedAt", newestFetchedAt);
        request.setAttribute("fetchedAtDisplay",
                newestFetchedAt != null ? newestFetchedAt.format(DISPLAY_FORMAT) : null);

        List<String> distinctSourceEnvs = rows.stream()
                .map(RatingAreaRateCache::getSourceEnv)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        String sourceEnv = distinctSourceEnvs.isEmpty() ? null : distinctSourceEnvs.get(0);
        request.setAttribute("sourceEnv", sourceEnv);
    }

    private String buildResultSummary(int headcount, CountyReference county, int planYear,
                                       BigDecimal groupMonthlyLow, BigDecimal groupMonthlyHigh) {
        StringBuilder sb = new StringBuilder("RANGE: ")
                .append(headcount).append(" lives, ")
                .append(county.getCountyName()).append(" ").append(county.getState())
                .append(", PY").append(planYear);

        if (groupMonthlyLow != null && groupMonthlyHigh != null) {
            sb.append(", bronze floor ").append(money(groupMonthlyLow)).append("-").append(money(groupMonthlyHigh));
        } else if (groupMonthlyLow != null) {
            sb.append(", bronze floor from ").append(money(groupMonthlyLow));
        } else if (groupMonthlyHigh != null) {
            sb.append(", bronze floor to ").append(money(groupMonthlyHigh));
        } else {
            sb.append(", bronze floor unavailable");
        }

        return truncate(sb.toString(), RESULT_SUMMARY_MAX_LENGTH);
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Writes the illustration_log row. A logging failure must never break the page — the
     * illustration is the product, the log is telemetry — so every exception is caught,
     * logged, and swallowed.
     * <p>
     * {@code illustration_log} carries no PII (V075): {@code headcount} is the same
     * aggregate-total field RANGE mode already populates (AGE_BAND passes the sum of its
     * row counts through it, not any individual row), and {@code resultSummary} names only
     * the county, plan year and an aggregate dollar total — never the individual ages,
     * per-row counts, or the contribution amount submitted for an AGE_BAND run.
     */
    private void logIllustration(EntityManager em, HttpServletRequest request, CountyReference county,
                                  int planYear, int headcount, boolean cacheHit, String resultSummary, String mode) {
        try {
            IllustrationLog logRow = new IllustrationLog();
            logRow.setCreatedAt(LocalDateTime.now());

            Object localObj = request.getSession().getAttribute("local");
            AmsDataLocal local = (localObj instanceof AmsDataLocal) ? (AmsDataLocal) localObj : null;
            Person currentPerson = local != null ? local.getCurrentPerson() : null;
            logRow.setAgentPersonId(currentPerson != null ? currentPerson.getId() : null);

            AgencyScope scope = AgencyScopeResolver.resolve(em, request);
            Long agencyId = scope.primaryAgencyId();
            logRow.setAgencyId(agencyId);

            Long parentAgencyId = null;
            if (agencyId != null) {
                Agency agency = em.find(Agency.class, agencyId);
                if (agency != null && agency.getParentAgency() != null) {
                    parentAgencyId = agency.getParentAgency().getId();
                }
            }
            logRow.setParentAgencyId(parentAgencyId);

            logRow.setZipCode(county.getRepresentativeZip());
            logRow.setCountyFips(county.getCountyFips());
            logRow.setState(county.getState());
            logRow.setPlanYear(planYear);
            logRow.setEligibleHeadcount(headcount);
            logRow.setMode(mode);
            logRow.setCacheHit(cacheHit);
            logRow.setResultSummary(resultSummary);

            // V081 — already resolved and scope-checked in doGet; null unless the caller
            // arrived from an opportunity they may see.
            Object oppIdAttr = request.getAttribute("opportunityId");
            logRow.setOpportunityId(oppIdAttr instanceof Long ? (Long) oppIdAttr : null);

            em.getTransaction().begin();
            try {
                em.persist(logRow);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        } catch (Exception e) {
            log.error("[ILLUSTRATION] Failed to write illustration_log row", e);
        }
    }

    /**
     * One validated (age, count, income) input row for AGE_BAND mode. Not JSP-visible.
     * {@code income} is the row's entered annual household income — null unless the
     * entered-income affordability basis is selected, in which case it is required
     * and validated before this row is constructed.
     */
    private static final class AgeBandRow {
        private final int age;
        private final int count;
        private final BigDecimal income;

        AgeBandRow(int age, int count, BigDecimal income) {
            this.age = age;
            this.count = count;
            this.income = income;
        }

        int getAge() { return age; }
        int getCount() { return count; }
        BigDecimal getIncome() { return income; }
    }

    /** One computed AGE_BAND result row, rendered by illustration25.jsp via c:forEach. */
    public static final class AgeBandResultRow {
        private final int age;
        private final int count;
        private final BigDecimal floorPremium;
        private final BigDecimal netPerEmployee;
        private final BigDecimal bandNet;

        AgeBandResultRow(int age, int count, BigDecimal floorPremium, BigDecimal netPerEmployee, BigDecimal bandNet) {
            this.age = age;
            this.count = count;
            this.floorPremium = floorPremium;
            this.netPerEmployee = netPerEmployee;
            this.bandNet = bandNet;
        }

        public int getAge() { return age; }
        public int getCount() { return count; }
        public BigDecimal getFloorPremium() { return floorPremium; }
        public BigDecimal getNetPerEmployee() { return netPerEmployee; }
        public BigDecimal getBandNet() { return bandNet; }
    }

    /**
     * One computed affordability result row (build-plan item 9), rendered by
     * illustration25.jsp via c:forEach. {@code available} is false when
     * {@code onex_lcsp_premium} is null for this row's age — every other field is then
     * null except {@code unavailableReason}, which explains why on screen.
     */
    public static final class AffordabilityResultRow {
        private final int age;
        private final int count;
        private final boolean available;
        private final BigDecimal onexLcspPremium;
        private final BigDecimal flipContribution;
        private final Boolean affordable;
        private final String unavailableReason;

        private AffordabilityResultRow(int age, int count, boolean available, BigDecimal onexLcspPremium,
                                        BigDecimal flipContribution, Boolean affordable, String unavailableReason) {
            this.age = age;
            this.count = count;
            this.available = available;
            this.onexLcspPremium = onexLcspPremium;
            this.flipContribution = flipContribution;
            this.affordable = affordable;
            this.unavailableReason = unavailableReason;
        }

        static AffordabilityResultRow available(int age, int count, BigDecimal onexLcspPremium,
                                                 BigDecimal flipContribution, boolean affordable) {
            return new AffordabilityResultRow(age, count, true, onexLcspPremium, flipContribution, affordable, null);
        }

        static AffordabilityResultRow unavailable(int age, int count, String reason) {
            return new AffordabilityResultRow(age, count, false, null, null, null, reason);
        }

        public int getAge() { return age; }
        public int getCount() { return count; }
        public boolean isAvailable() { return available; }
        public BigDecimal getOnexLcspPremium() { return onexLcspPremium; }
        public BigDecimal getFlipContribution() { return flipContribution; }
        public Boolean getAffordable() { return affordable; }
        public String getUnavailableReason() { return unavailableReason; }
    }
}

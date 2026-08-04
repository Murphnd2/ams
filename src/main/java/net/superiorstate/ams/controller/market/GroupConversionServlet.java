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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Group-to-ICHRA conversion analysis (build-plan item 11, A4a) — a thin increment over
 * {@link IllustrationServlet}'s AGE_BAND mode (build-plan item 9). The employer's current
 * group premium is the only genuinely new input: everything else — county/FIPS selection,
 * plan-year selection, the repeating (age, count) census rows, and the cached
 * {@code lowest_bronze_premium} lookup — is item 9's, reused unchanged in shape.
 * <p>
 * What this page answers: "what does the employer pay today, what would a flat ICHRA
 * contribution cost instead, and how many employees come out ahead?" It reports aggregate
 * market cost only.
 * <p>
 * ⚖️ <b>Compliance boundaries, deliberate and load-bearing:</b>
 * <ul>
 *     <li><b>D24 — the output goes to the agent, never to the employer.</b> There is no
 *     print, PDF, export, email, share-link, download, or proposal hand-off affordance on
 *     this page or its JSP, and none may be added.</li>
 *     <li><b>No plan or carrier identity.</b> No plan names, carrier names, plan list,
 *     ranking, curation, recommendation, badge, or default selection — the page reads one
 *     aggregate ({@code lowest_bronze_premium}) and nothing else.</li>
 *     <li><b>No affordability output.</b> Affordability is item 9's, on
 *     {@code /Illustration?mode=AGE_BAND}. Nothing here computes, displays, or links a
 *     per-employee affordability determination, and {@code onex_lcsp_premium} is never
 *     read.</li>
 *     <li><b>Nothing employee-facing.</b> No employee identifier is collected or stored.
 *     Ages, headcounts and an optional payroll-deduction figure are entered by the agent
 *     and live in the request only.</li>
 * </ul>
 * <p>
 * <b>Stateless.</b> No entity, DAO, table, or session storage is added for census data —
 * nothing entered on this page is persisted. The one write is the existing
 * {@code illustration_log} telemetry row (mode {@code CONVERSION}), which carries the same
 * aggregate, non-PII fields item 9 already writes and no census detail.
 * <p>
 * GET renders the empty form; POST computes and re-renders with results. This differs from
 * {@link IllustrationServlet}, which is GET-only so an illustration stays linkable — a
 * conversion analysis carries an employer's current premium and payroll deductions, which
 * have no business in a URL, a browser history entry, or an access log.
 */
@WebServlet(name = "GroupConversionServlet", value = "/GroupConversion")
public class GroupConversionServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(GroupConversionServlet.class);

    private static final int CENSUS_ROWS = 6;
    private static final int MIN_AGE = 21;
    private static final int MAX_AGE = 64;
    private static final int MAX_HEADCOUNT = 10000;
    private static final int RESULT_SUMMARY_MAX_LENGTH = 255;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String MODE_CONVERSION = "CONVERSION";
    private static final String VIEW = "/WEB-INF/view/market/groupConversion25.jsp";

    /** Empty form. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorized(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            setPageAttributes(request);
            request.setAttribute("opportunityId", resolveOpportunityId(em, request));
            List<Integer> configuredPlanYears = loadPlanYears(em, request);
            if (configuredPlanYears.isEmpty()) {
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }
            int planYear = resolvePlanYear(request, configuredPlanYears);
            request.setAttribute("selectedPlanYear", planYear);
            loadAvailableCounties(em, request, planYear);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /** Compute and re-render. Invalid input re-renders the form with a message — never throws. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorized(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            setPageAttributes(request);

            // Build-plan item 13 — optional opportunity attribution. Resolved once here so
            // every forward path below (including the validation early returns) carries it,
            // and read back out of the request by logIllustration.
            request.setAttribute("opportunityId", resolveOpportunityId(em, request));

            List<Integer> configuredPlanYears = loadPlanYears(em, request);
            if (configuredPlanYears.isEmpty()) {
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }

            int planYear = resolvePlanYear(request, configuredPlanYears);
            request.setAttribute("selectedPlanYear", planYear);

            List<CountyReference> availableCounties = loadAvailableCounties(em, request, planYear);

            // Echo every submitted value back before any validation can bail out, so a
            // re-rendered form never silently loses what the agent typed.
            String[] submittedAges = new String[CENSUS_ROWS];
            String[] submittedCounts = new String[CENSUS_ROWS];
            String[] submittedDeductions = new String[CENSUS_ROWS];
            for (int i = 0; i < CENSUS_ROWS; i++) {
                submittedAges[i] = request.getParameter("age" + (i + 1));
                submittedCounts[i] = request.getParameter("count" + (i + 1));
                submittedDeductions[i] = request.getParameter("deduction" + (i + 1));
            }
            request.setAttribute("submittedAges", submittedAges);
            request.setAttribute("submittedCounts", submittedCounts);
            request.setAttribute("submittedDeductions", submittedDeductions);

            String currentTotalParam = request.getParameter("currentTotalPremium");
            String employerShareParam = request.getParameter("currentEmployerShare");
            String contributionParam = request.getParameter("proposedContribution");
            request.setAttribute("submittedCurrentTotalPremium", currentTotalParam);
            request.setAttribute("submittedCurrentEmployerShare", employerShareParam);
            request.setAttribute("submittedProposedContribution", contributionParam);

            String countyFips = request.getParameter("countyFips");
            request.setAttribute("submittedCountyFips", countyFips);
            if (countyFips == null || countyFips.isBlank()) {
                request.setAttribute("inputError", "Select a county.");
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }

            CountyReference selectedCounty = availableCounties.stream()
                    .filter(c -> countyFips.equals(c.getCountyFips()))
                    .findFirst()
                    .orElse(null);
            if (selectedCounty == null) {
                request.setAttribute("inputError", "Select a valid county from the list.");
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }
            request.setAttribute("selectedCounty", selectedCounty);

            // --- census rows (item 9's shape: blank age = skipped row) ------------------
            List<CensusRow> rows = new ArrayList<>();
            for (int i = 0; i < CENSUS_ROWS; i++) {
                String ageRaw = submittedAges[i];
                if (ageRaw == null || ageRaw.isBlank()) {
                    continue;
                }
                Integer age = parseAge(ageRaw);
                if (age == null) {
                    request.setAttribute("inputError", "Row " + (i + 1) + ": age must be a whole number from " + MIN_AGE + " to " + MAX_AGE + ".");
                    request.getRequestDispatcher(VIEW).forward(request, response);
                    return;
                }
                Integer count = parseBandCount(submittedCounts[i]);
                if (count == null) {
                    request.setAttribute("inputError", "Row " + (i + 1) + ": count must be a positive whole number.");
                    request.getRequestDispatcher(VIEW).forward(request, response);
                    return;
                }
                // Optional. Blank means "no per-row figure supplied" — the derived average
                // employee share is used for that row instead. A supplied value must still
                // be a valid, non-negative amount.
                BigDecimal deduction = null;
                String deductionRaw = submittedDeductions[i];
                if (deductionRaw != null && !deductionRaw.isBlank()) {
                    deduction = parseNonNegativeDecimal(deductionRaw);
                    if (deduction == null) {
                        request.setAttribute("inputError", "Row " + (i + 1) + ": current payroll deduction must be 0 or more.");
                        request.getRequestDispatcher(VIEW).forward(request, response);
                        return;
                    }
                }
                rows.add(new CensusRow(age, count, deduction));
            }

            if (rows.isEmpty()) {
                request.setAttribute("inputError", "Enter at least one census row.");
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }

            // --- money inputs ----------------------------------------------------------
            BigDecimal currentTotalPremium = parseNonNegativeDecimal(currentTotalParam);
            if (currentTotalPremium == null) {
                request.setAttribute("inputError", "Enter the current total monthly group premium (0 or more).");
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }
            BigDecimal currentEmployerShare = parseNonNegativeDecimal(employerShareParam);
            if (currentEmployerShare == null) {
                request.setAttribute("inputError", "Enter the current monthly employer share (0 or more).");
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }
            if (currentEmployerShare.compareTo(currentTotalPremium) > 0) {
                request.setAttribute("inputError", "Employer share cannot exceed the total monthly group premium.");
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }
            BigDecimal proposedContribution = parseNonNegativeDecimal(contributionParam);
            if (proposedContribution == null) {
                request.setAttribute("inputError", "Enter the proposed monthly ICHRA contribution per employee (0 or more).");
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }

            int totalLives = 0;
            for (CensusRow row : rows) {
                totalLives += row.getCount();
            }
            request.setAttribute("submittedTotalLives", totalLives);

            // --- cached market figures (item 9's read, verbatim) ------------------------
            // Every age 21-64 is already cached — read the row, never compute the curve
            // here. A missing row is a cache-completeness problem reported on screen,
            // never interpolated.
            List<Integer> distinctAges = new ArrayList<>();
            for (CensusRow row : rows) {
                if (!distinctAges.contains(row.getAge())) {
                    distinctAges.add(row.getAge());
                }
            }
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
                resultSummary = computeConversion(request, selectedCounty, planYear, rows, totalLives,
                        currentTotalPremium, currentEmployerShare, proposedContribution, cacheByAge);
            } else {
                request.setAttribute("missingAges", missingAges);
                resultSummary = "CONVERSION: " + totalLives + " lives, " + selectedCounty.getCountyName() + " "
                        + selectedCounty.getState() + ", PY" + planYear + ", missing cache data";
            }

            logIllustration(em, request, selectedCounty, planYear, totalLives, hasRates,
                    truncate(resultSummary, RESULT_SUMMARY_MAX_LENGTH));

            request.getRequestDispatcher(VIEW).forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * The employer block, the per-employee table, and the plain-language summary. Pure
     * computation over already-validated inputs and already-loaded cache rows.
     *
     * @return the aggregate result summary for the telemetry row — county, plan year and
     * employer-level totals only, never a census row's age, count or deduction.
     */
    private String computeConversion(HttpServletRequest request, CountyReference selectedCounty, int planYear,
                                      List<CensusRow> rows, int totalLives,
                                      BigDecimal currentTotalPremium, BigDecimal currentEmployerShare,
                                      BigDecimal proposedContribution,
                                      Map<Integer, RatingAreaRateCache> cacheByAge) {

        // --- employer block --------------------------------------------------------
        BigDecimal proposedEmployerCost = proposedContribution.multiply(BigDecimal.valueOf(totalLives));
        BigDecimal monthlyDelta = proposedEmployerCost.subtract(currentEmployerShare);
        BigDecimal annualDelta = monthlyDelta.multiply(BigDecimal.valueOf(12));

        // Percentage change is undefined against a zero current employer cost — reported
        // as unavailable rather than as an invented figure.
        BigDecimal pctChange = null;
        if (currentEmployerShare.signum() > 0) {
            pctChange = monthlyDelta.multiply(BigDecimal.valueOf(100))
                    .divide(currentEmployerShare, 1, RoundingMode.HALF_UP);
        }

        request.setAttribute("currentEmployerCost", currentEmployerShare);
        request.setAttribute("proposedEmployerCost", proposedEmployerCost);
        request.setAttribute("monthlyDelta", monthlyDelta);
        request.setAttribute("annualDelta", annualDelta);
        request.setAttribute("pctChange", pctChange);

        // --- per-employee table ----------------------------------------------------
        BigDecimal totalEmployeeShare = currentTotalPremium.subtract(currentEmployerShare);
        BigDecimal derivedEmployeeCost = totalEmployeeShare.divide(BigDecimal.valueOf(totalLives), 2, RoundingMode.HALF_UP);
        request.setAttribute("totalEmployeeShare", totalEmployeeShare);
        request.setAttribute("derivedEmployeeCost", derivedEmployeeCost);

        List<ConversionResultRow> resultRows = new ArrayList<>();
        int aheadLives = 0;
        for (CensusRow row : rows) {
            RatingAreaRateCache cacheRow = cacheByAge.get(row.getAge());
            BigDecimal marketPremium = cacheRow.getLowestBronzePremium();
            // Clamp at zero — a contribution exceeding the market premium means the
            // employee's cost is zero, not negative.
            BigDecimal proposedEmployeeCost = marketPremium.subtract(proposedContribution).max(BigDecimal.ZERO);
            boolean deductionSupplied = row.getDeduction() != null;
            BigDecimal currentEmployeeCost = deductionSupplied ? row.getDeduction() : derivedEmployeeCost;
            BigDecimal netPosition = currentEmployeeCost.subtract(proposedEmployeeCost);
            if (netPosition.signum() > 0) {
                aheadLives += row.getCount();
            }
            resultRows.add(new ConversionResultRow(row.getAge(), row.getCount(), marketPremium,
                    proposedEmployeeCost, currentEmployeeCost, deductionSupplied, netPosition));
        }
        request.setAttribute("conversionResultRows", resultRows);
        request.setAttribute("aheadLives", aheadLives);

        setProvenanceAttributes(request, new ArrayList<>(cacheByAge.values()));

        // --- plain-language summary ------------------------------------------------
        StringBuilder summary = new StringBuilder()
                .append("Current group cost $").append(money(currentEmployerShare)).append("/mo. ")
                .append("ICHRA at $").append(money(proposedContribution)).append(" per employee is $")
                .append(money(proposedEmployerCost)).append("/mo, a change of ")
                .append(signedMoney(monthlyDelta));
        if (pctChange != null) {
            summary.append(" (").append(signedPercent(pctChange)).append(").");
        } else {
            summary.append(" (percentage change not available against a $0.00 current employer cost).");
        }
        summary.append(" ").append(aheadLives).append(" of ").append(totalLives)
                .append(totalLives == 1 ? " employee comes" : " employees come").append(" out ahead.");
        request.setAttribute("summaryLine", summary.toString());

        return "CONVERSION: " + totalLives + " lives, " + selectedCounty.getCountyName() + " "
                + selectedCounty.getState() + ", PY" + planYear
                + ", employer " + money(currentEmployerShare) + " -> " + money(proposedEmployerCost)
                + ", " + aheadLives + "/" + totalLives + " ahead";
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
     * to an opportunity the caller may actually see, or null. Identical in contract to
     * {@code IllustrationServlet}'s own resolver.
     * <p>
     * Scope validation is {@link OpportunityAuthz#canAccessOpportunity(EntityManager,
     * HttpServletRequest, long)} — the same predicate {@code UpdateOpportunityStage} and
     * {@code GoActivityDetail25} already use, which loads the row and delegates the
     * agency question to {@code AgencyScopeResolver.canSeeDetail}. No scoping logic is
     * written here.
     * <p>
     * Fails quietly by design: absent, blank, unparseable, non-existent, or out of scope
     * all return null. A conversion analysis works with or without an opportunity, so a
     * bad id must never error, redirect, or put a message on screen. Returning null for
     * out of scope also means an id belonging to another agency cannot be written onto
     * this agency's log row.
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
            log.debug("[ICHRA-CONVERSION] Opportunity attribution skipped; could not resolve id", e);
            return null;
        }
    }

    private void setPageAttributes(HttpServletRequest request) {
        request.setAttribute("pageTitle", "Group-to-ICHRA Conversion");
        request.setAttribute("pageIcon", "bi-arrow-left-right");
    }

    private List<Integer> loadPlanYears(EntityManager em, HttpServletRequest request) {
        String planYearsConstant = AppConstantDAO.getConstantValue(em, "RATE_CACHE_PLAN_YEARS");
        List<Integer> configuredPlanYears = parsePlanYears(planYearsConstant);
        request.setAttribute("configuredPlanYears", configuredPlanYears);
        return configuredPlanYears;
    }

    /** Counties that have cached rate data for this plan year, resolved to reference rows. */
    private List<CountyReference> loadAvailableCounties(EntityManager em, HttpServletRequest request, int planYear) {
        List<RateCacheDAO.CountySummary> summaries = RateCacheDAO.getCountySummaries(em, planYear);
        List<String> cachedFips = summaries.stream()
                .map(RateCacheDAO.CountySummary::getCountyFips)
                .collect(Collectors.toList());

        // T137 — provenance rides ALONGSIDE the list, never filters it. Every warmed county
        // stays selectable: filtering to production-sourced counties would empty the dropdown
        // today, since every warmed county is staging-sourced. The return type is deliberately
        // unchanged because the POST path validates a submitted county against it (:176-184);
        // reshaping it could reject a legitimate submission.
        // Fails toward labeling: anything not positively PRODUCTION is marked, so a county
        // whose rows are mixed, or whose source_env is null or unrecognised, is still marked.
        Set<String> stagingCountyFips = summaries.stream()
                .filter(s -> !RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(s.getSourceEnv()))
                .map(RateCacheDAO.CountySummary::getCountyFips)
                .collect(Collectors.toSet());
        request.setAttribute("stagingCountyFips", stagingCountyFips);

        List<CountyReference> availableCounties = CountyReferenceDAO.findByFipsIn(em, cachedFips);
        request.setAttribute("availableCounties", availableCounties);
        request.setAttribute("missingReferenceCount", cachedFips.size() - availableCounties.size());

        // T138 — pre-selection provenance banner. Same fail-toward-warning rule as
        // stagingCountyFips above, applied to the set actually offered in the dropdown
        // (not the raw cached set, which can include a fips with no CountyReference row
        // and therefore never appears as an option). Empty dropdown -> no match -> no
        // banner, which is correct: a provenance warning about zero rows is a false claim.
        // The results path (computeConversion -> setProvenanceAttributes) overwrites this
        // same attribute with the selected county's own value once a county is chosen.
        boolean dropdownHasStaging = availableCounties.stream()
                .map(CountyReference::getCountyFips)
                .anyMatch(stagingCountyFips::contains);
        request.setAttribute("sourceEnv", dropdownHasStaging ? RatingAreaRateCache.SOURCE_ENV_STAGING : null);

        return availableCounties;
    }

    /** Tolerant parse matching IllustrationServlet's own RATE_CACHE_PLAN_YEARS handling — display only. */
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

    /** @return the parsed age, or null if non-numeric or outside MIN_AGE-MAX_AGE. Caller handles blank (skipped row) before this is called. */
    private Integer parseAge(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < MIN_AGE || value > MAX_AGE) return null;
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return the parsed census count, defaulting to 1 when blank; null if non-numeric or not a positive integer up to MAX_HEADCOUNT. */
    private Integer parseBandCount(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try {
            int value = Integer.parseInt(raw.trim());
            return (value > 0 && value <= MAX_HEADCOUNT) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return the parsed amount, or null if missing, non-numeric, or negative. Every money field on this page must be zero or more. */
    private BigDecimal parseNonNegativeDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.signum() >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Sets fetchedAt/fetchedAtDisplay/sourceEnv request attributes from the rows actually
     * loaded for this run, so the staging-provenance banner behaves exactly as it does on
     * the illustration page. Provenance is read from the loaded rows themselves (whatever
     * was stamped when they were cached), not from RateCacheWarmService's current config,
     * which can change after the rows were fetched.
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
        request.setAttribute("sourceEnv", distinctSourceEnvs.isEmpty() ? null : distinctSourceEnvs.get(0));
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Explicit sign on the delta — "up or down" is the whole point of the figure. */
    private String signedMoney(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        return (scaled.signum() >= 0 ? "+$" : "-$") + scaled.abs().toPlainString();
    }

    private String signedPercent(BigDecimal value) {
        return (value.signum() >= 0 ? "+" : "-") + value.abs().toPlainString() + "%";
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Writes the {@code illustration_log} row with mode {@code CONVERSION}. Mirrors
     * IllustrationServlet's own telemetry write, including its rule that a logging failure
     * must never break the page — the analysis is the product, the log is telemetry — so
     * every exception is caught, logged, and swallowed.
     * <p>
     * No schema change: {@code illustration_log.mode} is an unconstrained
     * {@code VARCHAR(16)} (V075), so {@code CONVERSION} needs no new enum value or column.
     * <p>
     * The row carries no PII and no census detail: {@code eligible_headcount} is the
     * aggregate total already defined for that column, and {@code result_summary} names
     * only the county, plan year and employer-level totals — never an individual age, a
     * per-row count, a payroll deduction, or the entered group premium breakdown.
     */
    private void logIllustration(EntityManager em, HttpServletRequest request, CountyReference county,
                                  int planYear, int headcount, boolean cacheHit, String resultSummary) {
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
            logRow.setMode(MODE_CONVERSION);
            logRow.setCacheHit(cacheHit);
            logRow.setResultSummary(resultSummary);

            // V081 — already resolved and scope-checked in doPost; null unless the caller
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
            log.error("[ICHRA-CONVERSION] Failed to write illustration_log row", e);
        }
    }

    /**
     * One validated census input row. Not JSP-visible. {@code deduction} is the employee's
     * current monthly payroll deduction — optional, null when the agent left it blank, in
     * which case the derived average employee share is used for the row instead.
     */
    private static final class CensusRow {
        private final int age;
        private final int count;
        private final BigDecimal deduction;

        CensusRow(int age, int count, BigDecimal deduction) {
            this.age = age;
            this.count = count;
            this.deduction = deduction;
        }

        int getAge() { return age; }
        int getCount() { return count; }
        BigDecimal getDeduction() { return deduction; }
    }

    /** One computed conversion result row, rendered by groupConversion25.jsp via c:forEach. */
    public static final class ConversionResultRow {
        private final int age;
        private final int count;
        private final BigDecimal marketPremium;
        private final BigDecimal proposedEmployeeCost;
        private final BigDecimal currentEmployeeCost;
        private final boolean deductionSupplied;
        private final BigDecimal netPosition;

        ConversionResultRow(int age, int count, BigDecimal marketPremium, BigDecimal proposedEmployeeCost,
                            BigDecimal currentEmployeeCost, boolean deductionSupplied, BigDecimal netPosition) {
            this.age = age;
            this.count = count;
            this.marketPremium = marketPremium;
            this.proposedEmployeeCost = proposedEmployeeCost;
            this.currentEmployeeCost = currentEmployeeCost;
            this.deductionSupplied = deductionSupplied;
            this.netPosition = netPosition;
        }

        public int getAge() { return age; }
        public int getCount() { return count; }
        public BigDecimal getMarketPremium() { return marketPremium; }
        public BigDecimal getProposedEmployeeCost() { return proposedEmployeeCost; }
        public BigDecimal getCurrentEmployeeCost() { return currentEmployeeCost; }
        public boolean isDeductionSupplied() { return deductionSupplied; }
        public BigDecimal getNetPosition() { return netPosition; }
    }
}

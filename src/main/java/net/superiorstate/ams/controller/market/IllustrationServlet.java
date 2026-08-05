package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

            // ── One analysis surface, progressive by input fidelity ───────────────
            //
            // Steps 1/2/3 on the hub were never steps. All three are this servlet with a
            // different `mode`, and the difference between them is how much detail the
            // agent happens to have: ZIP + headcount, ZIP + age bands, or a census. That
            // is the fidelity-tier model in §4.1, and it is progressive, not sequential —
            // pre-sale this is done ONCE, at whatever detail is available.
            //
            // ⚠️ `mode` REMAINS A SUPPORTED URL PARAMETER and is honoured verbatim when
            // present. The hub cards, T59's affordability card, the proposal hand-off and
            // every link verified this session send it, and all of them must keep landing
            // exactly where they landed before. It is only DERIVED when absent — which is
            // what the form now does, so adding the first age band moves an agent from
            // tier 1 to tier 2 with no mode switch and no lost state.
            String modeParam = request.getParameter("mode");
            String mode;
            if (MODE_AGE_BAND.equals(modeParam)) {
                mode = MODE_AGE_BAND;
            } else if (MODE_RANGE.equals(modeParam)) {
                mode = MODE_RANGE;
            } else {
                mode = hasAnyAgeBand(request) ? MODE_AGE_BAND : MODE_RANGE;
            }
            request.setAttribute("mode", mode);
            // Whether the caller asked for a mode explicitly. The JSP uses this to decide
            // whether to open with a starter age row (the hub's age-band card must still
            // land on a usable row, including with JavaScript off).
            request.setAttribute("modeExplicit", MODE_AGE_BAND.equals(modeParam) || MODE_RANGE.equals(modeParam));

            // W7 — the repeater's cap, published rather than duplicated in the JSP so the
            // markup and the parse loop cannot drift apart.
            //
            // ⚠️ This limit is NOT free to raise. `proposalBuilder.jsp` echoes exactly six
            // age/count pairs into the proposal POST, and raising AGE_BAND_ROWS without
            // raising that too would silently drop rows 7+ from every proposal snapshot —
            // a wrong figure on a client-facing document, produced by a change that looks
            // purely additive here. Raise both together or neither.
            request.setAttribute("ageBandMaxRows", AGE_BAND_ROWS);

            // Build-plan item 13 — optional opportunity attribution. Resolved once here so
            // every forward path below (including the early returns) carries it, and read
            // back out of the request by logIllustration. Null whenever absent or not
            // permitted; never an error and never a message on screen.
            request.setAttribute("opportunityId", resolveOpportunityId(em, request));

            // T150 — the step-6 demo override. Resolved once here, alongside opportunityId
            // and for the identical reason: there are thirteen forward points below,
            // including several early returns, and a path that missed this attribute would
            // silently fail closed (button stays disabled) in a way that looks like a data
            // problem rather than a plumbing one. handleRangeMode and handleAgeBandMode are
            // both called from below this line, so every one of the thirteen is covered.
            //
            // ⚠️ BOTH conditions are required — the properties flag AND a PSP-admin session.
            // Never `||`. getSession(false) deliberately: a feature check must not create a
            // session. Absent flag, absent session, or a non-admin caller all yield false.
            //
            // Scope: this enables the hand-off BUTTON only. It suppresses no staging banner
            // (those read sourceEnv directly and are untouched), and it has no bearing on the
            // public /proposal/* render, which is gated session-free under LA-17.
            request.setAttribute("ichraDemoOverride", isIchraDemoOverride(request));

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
                    // Which candidates the illustration can actually price. A label, not an
                    // ordering — see pricedCountyFips.
                    request.setAttribute("pricedCountyFips", pricedCountyFips(em, planYear, availableCounties));
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
                request.setAttribute("inputError", describeUnavailableCounty(em, resolvedFips));
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
            // T103: a hub card (or any link) that asks for AGE_BAND explicitly but supplies
            // no ageN parameter at all is a landing, not a failed attempt to compute — the
            // starter row the JSP renders for modeExplicit is the whole answer, and an error
            // greeting the agent before they have done anything is wrong. A request that
            // carries age1 (even blank) is still trying to compute and still errors, as does
            // a derived mode (modeExplicit false) with nothing usable — unchanged both ways.
            boolean anyAgeParamPresent = false;
            for (String ageRaw : submittedAges) {
                if (ageRaw != null) {
                    anyAgeParamPresent = true;
                    break;
                }
            }
            boolean modeExplicit = Boolean.TRUE.equals(request.getAttribute("modeExplicit"));
            boolean isLanding = modeExplicit && !anyAgeParamPresent;
            if (isLanding) {
                // T103 second cut: suppressing the error is not enough — an empty `rows`
                // reaching the JSP with no other signal reads, to it, exactly like a
                // computed result that came back with nothing (hasRates never gets set
                // because this method returns before reaching it, and the JSP's own R3 fix
                // already treats an unset hasRates as "nothing cached" — see
                // illustration25.jsp around the illustrationResults guard). This flag is
                // the distinction the JSP has no other way to make: a landing, not a result.
                request.setAttribute("illustrationLanding", Boolean.TRUE);
            } else {
                request.setAttribute("inputError", "Enter at least one age.");
            }
            request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
            return;
        }

        // ── K3-c: the contribution is OPTIONAL ────────────────────────────────────
        //
        // "What does each employee pay at the bronze floor, by age" is a legitimate
        // question an agent asks BEFORE he has a contribution in mind, and requiring one
        // made Illustrate refuse to compute for a perfectly well-formed request. That
        // refusal is also what produced K3-a: the submit errored, mode had already flipped
        // to AGE_BAND, so the contribution and basis fields appeared — and the button
        // looked like it had revealed inputs instead of computing.
        //
        // Blank is now "not supplied" and yields a per-band premium table. A value that is
        // present but malformed is still an error — that is a typo, not an absence.
        boolean contributionSupplied = contributionParam != null && !contributionParam.isBlank();
        BigDecimal contribution = null;
        if (contributionSupplied) {
            contribution = parseContribution(contributionParam);
            if (contribution == null) {
                request.setAttribute("inputError", "Enter a valid employer monthly contribution (0 or more).");
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }
        }
        request.setAttribute("contributionSupplied", contributionSupplied);

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
                // Net cost is only meaningful against a contribution. Without one the row
                // carries the floor premium alone — the net columns are not rendered, so
                // they are left null rather than defaulted to the premium, which would
                // read as "the employee pays all of it" and is a different claim.
                // ⚠️ The clamp-at-zero arithmetic below is untouched; it simply does not
                // run when there is nothing to subtract.
                BigDecimal netPerEmployee = null;
                BigDecimal bandNet = null;
                if (contributionSupplied) {
                    netPerEmployee = floorPremium.subtract(contribution).max(BigDecimal.ZERO);
                    bandNet = netPerEmployee.multiply(BigDecimal.valueOf(row.getCount()));
                    groupNetTotal = groupNetTotal.add(bandNet);
                }
                resultRows.add(new AgeBandResultRow(row.getAge(), row.getCount(), floorPremium, netPerEmployee, bandNet));
            }
            request.setAttribute("ageBandResultRows", resultRows);
            request.setAttribute("submittedTotalLives", totalLives);

            if (contributionSupplied) {
                request.setAttribute("groupNetTotal", groupNetTotal);
                request.setAttribute("employerOutlay", contribution.multiply(BigDecimal.valueOf(totalLives)));
            }

            setProvenanceAttributes(request, new ArrayList<>(cacheByAge.values()));

            // Flip points do not depend on the contribution — only the verdict does — so a
            // basis chosen without one still yields the thresholds, with the verdict column
            // left empty rather than guessed.
            computeAffordability(em, request, planYear, affordabilityBasis, contribution, rows, cacheByAge);

            resultSummary = "AGE_BAND: " + totalLives + " lives, " + selectedCounty.getCountyName() + " "
                    + selectedCounty.getState() + ", PY" + planYear
                    + (contributionSupplied ? ", group net floor " + money(groupNetTotal) : ", premium by band");
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
            // K3-c: the threshold stands on its own — it is where the verdict WOULD change,
            // and that is a fact about the plan year and the employee's age, not about any
            // contribution. The verdict is the part that needs one, so without a
            // contribution it stays null and the column renders empty rather than guessing
            // a side. isAffordable is unchanged and simply is not called.
            Boolean affordable = (contribution == null)
                    ? null
                    : AffordabilityCalculator.isAffordable(contribution, flip);
            affordabilityRows.add(AffordabilityResultRow.available(row.getAge(), row.getCount(), onexLcsp, flip, affordable));
        }
        request.setAttribute("affordabilityRows", affordabilityRows);
    }

    /**
     * ⭐ <b>The single place a county the illustration cannot price is reported</b> — every
     * entry path lands here: the ZIP resolver, a chooser link, a hand-typed URL, and the
     * dropdown. One branch, one message per state, one method.
     * <p>
     * <b>It distinguishes two states that used to share a message, wrongly.</b> The county
     * dropdown is built from {@code rating_area_rate_cache} — the counties that have
     * <i>cached rates</i>, four of them on production — while the ZIP crosswalk knows all
     * 254 Texas counties (V085). So ZIP resolution can hand an agent a real county the
     * illustration has never been able to price, and the page answered
     * <i>"Select a valid county from the list"</i> — <b>blaming the agent for a coverage
     * gap that is ours.</b> Same category of error as the ZIP no-match copy, in a place
     * nobody had looked. ZIP intake did not create this gap; it exposed it.
     * <ul>
     *   <li><b>A real county with no cached rates</b> — say so, and name it.</li>
     *   <li><b>Not a county at all</b> (a typo, a truncated FIPS, a pasted placeholder) —
     *   the original message, which is correct for that input and unchanged.</li>
     * </ul>
     * <p>
     * ⭐ <b>This is where T76 (warm-on-miss) attaches.</b> The unwarmed branch below is the
     * one place that knows "a real county, no rates" — a warm trigger goes there and
     * nowhere else. <b>Deliberately no stub, no button and no TODO here:</b> a disabled
     * control implying a capability that does not exist is worse than its absence.
     * <p>
     * Fails toward the generic message: if {@code county_reference} cannot be read, the
     * agent gets the safe wording rather than an exception.
     */
    private String describeUnavailableCounty(EntityManager em, String countyFips) {
        try {
            CountyReference known = CountyReferenceDAO.findByFips(em, countyFips);
            if (known != null) {
                // W6 — the trailing advice used to read "Select another county from the
                // list." That is wrong advice whenever a ZIP resolves to counties that are
                // all uncached: there is no other county in the list that is right for this
                // employer, and telling an agent to pick one invites exactly the wrong
                // action — running a neighbouring county's rates for a client. State the
                // constraint instead of prescribing a move.
                return "We don't have rates for " + known.getCountyName() + ", " + known.getState()
                        + " yet. The county list holds only the counties we have rates for, so it will not contain this one.";
            }
        } catch (Exception e) {
            log.debug("[ILLUSTRATION] Could not classify unavailable county {}", countyFips, e);
        }
        return "Select a valid county from the list.";
    }

    /**
     * County FIPS codes with <b>production-sourced</b> cached rates for this plan year.
     * Handed to the JSP so the crossing-ZIP chooser can mark which of its candidates can
     * actually be priced.
     * <p>
     * ⚠️ <b>S12-B — corrected to check provenance, not merely presence</b>, the same fix
     * S11-G applied to {@code IchraZipLookup.pricedCountyFips}: a county with cached rows
     * that are all {@code STAGING} previously counted as "priced" here, even though
     * selecting it renders the red test-environment banner and disables the proposal
     * hand-off. Now asks {@link RateCacheDAO#check} per county and counts only
     * {@link RateCacheDAO.MarketDataAvailability#PRODUCTION_OK}.
     * <p>
     * ⚠️ <b>This method feeds the crossing-ZIP chooser's labels ONLY</b>
     * ({@code illustration25.jsp:615}) — it does not touch {@code availableCounties}, the
     * separate list that populates the main county {@code <select>}. That list must stay
     * provenance-blind: every county {@code RATE_CACHE_COUNTIES} currently warms is
     * staging-sourced, so filtering it by provenance would empty the dropdown entirely.
     * See {@code docs/session_s12b_closeout.md} for why {@code GroupConversionServlet}'s
     * analogous method was deliberately left untouched — it has no separate label to
     * correct; its county list <em>is</em> the dropdown.
     * <p>
     * ⚠️ <b>Descriptive only.</b> This drives a factual label, never an ordering and never
     * a recommendation: the candidates keep the resolver's land-area order, a county with
     * no rates is <b>not</b> demoted, and nothing is pre-selected. The no-steering boundary
     * applies to counties exactly as it does to plans.
     */
    private Set<String> pricedCountyFips(EntityManager em, int planYear, List<CountyReference> availableCounties) {
        Set<String> priced = new HashSet<>();
        for (CountyReference county : availableCounties) {
            String fips = county.getCountyFips();
            if (RateCacheDAO.check(em, planYear, fips) == RateCacheDAO.MarketDataAvailability.PRODUCTION_OK) {
                priced.add(fips);
            }
        }
        return priced;
    }

    /**
     * @return true if the request carries at least one non-blank {@code ageN} parameter.
     * <p>
     * This is the whole of the tier-1 → tier-2 transition: an agent who has entered an age
     * band is asking for per-band output, and one who has not is asking for a range. Read
     * only when no explicit {@code mode} was supplied, so it can never override a URL.
     * Bounded by {@link #AGE_BAND_ROWS} — the same limit the parse loop uses, so the two
     * cannot disagree about how many rows exist.
     */
    private boolean hasAnyAgeBand(HttpServletRequest request) {
        for (int i = 1; i <= AGE_BAND_ROWS; i++) {
            String age = request.getParameter("age" + i);
            if (age != null && !age.isBlank()) {
                return true;
            }
        }
        return false;
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
     * T150 — the step-6 demo override: {@code ICHRA_DEMO_ALLOW_STAGING_PROPOSAL=true} in
     * ssa.properties <b>AND</b> a PSP-admin session. Both, always; either alone is false.
     * <p>
     * Not an authorization check and not a substitute for {@link #isAuthorized}. It decides
     * one thing only: whether the "Use This in a Proposal" hand-off is offered on an
     * illustration built from staging-sourced rates. {@code ProposalBuilder} re-evaluates the
     * same two conditions independently before writing a snapshot, so enabling the button
     * cannot by itself produce a row.
     * <p>
     * {@code getSession(false)} deliberately — a feature check must never create a session.
     */
    private boolean isIchraDemoOverride(HttpServletRequest request) {
        if (!AppConfig.isIchraDemoStagingAllowed()) {
            return false;
        }
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("isPspAdmin"));
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

            // W2 — record a ZIP only when the agent actually supplied one.
            //
            // This previously wrote county.getRepresentativeZip() unconditionally, so a
            // run where the agent typed nothing and picked Hopkins from the dropdown
            // logged zip_code = '75437' — Hopkins's representative ZIP from
            // county_reference (V076). The column read like user input and was not:
            // anyone auditing the log would conclude an agent typed a ZIP they never
            // typed. county_fips already records the county, so the derived value added
            // no information and actively misled.
            //
            // Strictly a reduction in what is stored, which is the only direction this
            // column may ever move. No migration: the column is unchanged and nullable.
            Object submittedZipAttr = request.getAttribute("submittedZip");
            logRow.setZipCode(submittedZipAttr instanceof String ? (String) submittedZipAttr : null);
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

        /** {@code affordable} is nullable: a threshold without a contribution has no verdict (K3-c). */
        static AffordabilityResultRow available(int age, int count, BigDecimal onexLcspPremium,
                                                 BigDecimal flipContribution, Boolean affordable) {
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

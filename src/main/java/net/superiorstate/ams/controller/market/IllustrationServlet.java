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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Agent-facing ICHRA rating illustration, RANGE mode only (B-Phase-2a). AGE_BAND mode
 * (income input, affordability threshold) is deferred to B-Phase-2b — {@code mode} is
 * always written as the literal {@code "RANGE"} so the column is correct from the
 * first row.
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
            if (countyFips == null || countyFips.isBlank()) {
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }
            request.setAttribute("submittedCountyFips", countyFips);

            CountyReference selectedCounty = availableCounties.stream()
                    .filter(c -> countyFips.equals(c.getCountyFips()))
                    .findFirst()
                    .orElse(null);
            if (selectedCounty == null) {
                request.setAttribute("inputError", "Select a valid county from the list.");
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }

            String headcountParam = request.getParameter("headcount");
            request.setAttribute("submittedHeadcount", headcountParam);
            Integer headcount = parseHeadcount(headcountParam);
            if (headcount == null) {
                request.setAttribute("inputError", "Enter a valid number of eligible employees (1-" + MAX_HEADCOUNT + ").");
                request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
                return;
            }

            request.setAttribute("selectedCounty", selectedCounty);

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

                RatingAreaRateCache anyRow = nonTobaccoRows.get(0);
                request.setAttribute("carrierCount", anyRow.getCarrierCount());
                request.setAttribute("planCount", anyRow.getPlanCount());

                LocalDateTime newestFetchedAt = nonTobaccoRows.stream()
                        .map(RatingAreaRateCache::getFetchedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                request.setAttribute("fetchedAt", newestFetchedAt);

                resultSummary = buildResultSummary(headcount, selectedCounty, planYear, groupMonthlyLow, groupMonthlyHigh);
            } else {
                resultSummary = "RANGE: " + headcount + " lives, " + selectedCounty.getCountyName() + " "
                        + selectedCounty.getState() + ", PY" + planYear + ", no rate data";
            }

            logIllustration(em, request, selectedCounty, planYear, headcount, hasRates, resultSummary);

            request.getRequestDispatcher("/WEB-INF/view/market/illustration25.jsp").forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private boolean isAuthorized(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"))
                || Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"))
                || Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"))
                || Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"))
                || Boolean.TRUE.equals(request.getSession().getAttribute("isPspSales"));
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

    /** Lowest-bronze premium at the given age times headcount, or null if the underlying value is null. */
    private BigDecimal groupPremium(RatingAreaRateCache ageRow, int headcount) {
        if (ageRow == null || ageRow.getLowestBronzePremium() == null) {
            return null;
        }
        return ageRow.getLowestBronzePremium().multiply(BigDecimal.valueOf(headcount));
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
            logRow.setMode("RANGE");
            logRow.setCacheHit(cacheHit);
            logRow.setResultSummary(resultSummary);

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
}

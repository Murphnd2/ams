package net.superiorstate.ams.data.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ACA statutory uniform age rating curve. Carriers cannot deviate from it, which
 * means one quote at age 21 (factor 1.000) derives every other age's premium for
 * the same plan by simple multiplication — confirmed empirically against live
 * HealthSherpa staging data on 2026-07-30 across two carriers, five ages, and
 * three separate calls: a single base rate reproduced every observed premium to
 * the cent, and the age-64 to age-21 ratio was exactly 3.000. Consequence: the
 * rate-cache warm job makes one HealthSherpa API call per county per plan year,
 * not forty-five.
 *
 * <p><b>⚠ UNVERIFIED VALUES.</b> Only five points in the 2026 curve below are
 * confirmed against live data: age 21 = 1.000, 25 = 1.004, 40 = 1.278, 45 = 1.444,
 * 64 = 3.000. The remaining ages are transcribed from the CMS federal default
 * uniform age rating curve and have <b>not</b> been checked against a published
 * CMS source in this session. Verify the full table against the CMS-published
 * curve for the relevant plan year before relying on it for anything
 * production-facing.
 *
 * <p>This curve is <b>plan-year scoped</b> — CMS may revise it, which is why the
 * API below is keyed by plan year rather than exposing a single flat table. It is
 * also <b>state-specific</b>: Texas uses the federal default curve reproduced
 * here, but several states publish their own age curves. A state dimension will
 * be required if AMS's rate-cache book of business expands beyond states that use
 * the federal default.
 */
public final class AgeCurve {

    private AgeCurve() {}

    public static final int MIN_AGE = 21;
    public static final int MAX_AGE = 64;

    private static final Map<Integer, Map<Integer, BigDecimal>> CURVES_BY_PLAN_YEAR;

    static {
        Map<Integer, BigDecimal> curve2026 = new LinkedHashMap<>();
        curve2026.put(21, bd("1.000"));
        curve2026.put(22, bd("1.000"));
        curve2026.put(23, bd("1.000"));
        curve2026.put(24, bd("1.000"));
        curve2026.put(25, bd("1.004"));
        curve2026.put(26, bd("1.024"));
        curve2026.put(27, bd("1.048"));
        curve2026.put(28, bd("1.087"));
        curve2026.put(29, bd("1.119"));
        curve2026.put(30, bd("1.135"));
        curve2026.put(31, bd("1.159"));
        curve2026.put(32, bd("1.183"));
        curve2026.put(33, bd("1.198"));
        curve2026.put(34, bd("1.214"));
        curve2026.put(35, bd("1.222"));
        curve2026.put(36, bd("1.230"));
        curve2026.put(37, bd("1.238"));
        curve2026.put(38, bd("1.246"));
        curve2026.put(39, bd("1.262"));
        curve2026.put(40, bd("1.278"));
        curve2026.put(41, bd("1.302"));
        curve2026.put(42, bd("1.325"));
        curve2026.put(43, bd("1.357"));
        curve2026.put(44, bd("1.397"));
        curve2026.put(45, bd("1.444"));
        curve2026.put(46, bd("1.500"));
        curve2026.put(47, bd("1.563"));
        curve2026.put(48, bd("1.635"));
        curve2026.put(49, bd("1.706"));
        curve2026.put(50, bd("1.786"));
        curve2026.put(51, bd("1.865"));
        curve2026.put(52, bd("1.952"));
        curve2026.put(53, bd("2.040"));
        curve2026.put(54, bd("2.135"));
        curve2026.put(55, bd("2.230"));
        curve2026.put(56, bd("2.333"));
        curve2026.put(57, bd("2.437"));
        curve2026.put(58, bd("2.548"));
        curve2026.put(59, bd("2.603"));
        curve2026.put(60, bd("2.714"));
        curve2026.put(61, bd("2.810"));
        curve2026.put(62, bd("2.873"));
        curve2026.put(63, bd("2.952"));
        curve2026.put(64, bd("3.000"));

        Map<Integer, Map<Integer, BigDecimal>> byYear = new LinkedHashMap<>();
        byYear.put(2026, Collections.unmodifiableMap(curve2026));
        CURVES_BY_PLAN_YEAR = Collections.unmodifiableMap(byYear);
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    /**
     * Returns the age-rating factor for the given plan year and age. Ages below
     * {@value #MIN_AGE} clamp to {@value #MIN_AGE}; ages above {@value #MAX_AGE}
     * clamp to {@value #MAX_AGE}. Ages 0-20 are out of scope for this method (see
     * class Javadoc) — clamping to 21 rather than raising an error is the correct
     * behavior here, since a rating illustration is bounded to employees.
     *
     * @throws IllegalStateException if no curve is configured for the plan year
     */
    public static BigDecimal factorFor(int planYear, int age) {
        Map<Integer, BigDecimal> curve = CURVES_BY_PLAN_YEAR.get(planYear);
        if (curve == null) {
            throw new IllegalStateException("No age rating curve configured for plan year " + planYear);
        }
        int clamped = Math.max(MIN_AGE, Math.min(MAX_AGE, age));
        BigDecimal factor = curve.get(clamped);
        if (factor == null) {
            throw new IllegalStateException("No age rating factor for age " + clamped + " in plan year " + planYear + " curve");
        }
        return factor;
    }

    /**
     * Scales a base premium quoted at age 21 (factor 1.000) to the given age,
     * rounded to 2 decimal places, HALF_UP.
     */
    public static BigDecimal scale(BigDecimal basePremiumAt21, int planYear, int age) {
        BigDecimal factor = factorFor(planYear, age);
        return basePremiumAt21.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}

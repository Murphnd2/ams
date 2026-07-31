package net.superiorstate.ams.data.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ICHRA affordability threshold math (build-plan item 9). Pure computation — no I/O,
 * no persistence, no HTTP. The two IRS/HHS-indexed reference values (applicable
 * percentage, federal poverty line) are resolved by the caller from configuration and
 * passed in; this class never reads a constant and never guesses or defaults one.
 * <p>
 * This is an employer- and agent-facing ANALYSIS, never a determination and never
 * advice to any employee (LA-12) — see {@code illustration25.jsp} for the full set of
 * compliance boundaries this feature observes (no PTC dollar figure, no recommended
 * contribution, employer/agent audience only).
 * <p>
 * Callers must pass the on-exchange LCSP ({@code RatingAreaRateCache.onexLcspPremium}),
 * never the off-exchange {@code lcspPremium} — T44 found the off-exchange figure
 * understates the true on-exchange LCSP by roughly 44% in the reference county, which
 * is the dangerous direction for this computation (a too-low LCSP makes an
 * unaffordable offer look affordable).
 */
public final class AffordabilityCalculator {

    /** Internal division scale for income ÷ 12 — kept well beyond currency precision so rounding happens only at display, not here. */
    private static final int INTERNAL_SCALE = 10;

    private AffordabilityCalculator() {}

    /**
     * The employer contribution at or above which the offer becomes affordable and the
     * employee loses PTC eligibility:
     * <pre>flip_contribution = onexLcspPremium − applicablePct × (annualIncome ÷ 12)</pre>
     * Clamped to zero: a negative result means the offer is affordable at any
     * contribution, including none, and is returned as zero rather than a negative
     * figure.
     *
     * @param onexLcspPremium on-exchange LCSP at the employee's age — never the off-exchange figure
     * @param applicablePct   the IRS-indexed applicable percentage for the plan year, as a decimal (e.g. 0.0883 for 8.83%)
     * @param annualIncome    the reference annual income — entered household income, or the configured FPL for the safe-harbor basis
     * @return the flip contribution, clamped to zero or above
     */
    public static BigDecimal flipContribution(BigDecimal onexLcspPremium, BigDecimal applicablePct, BigDecimal annualIncome) {
        BigDecimal monthlyIncome = annualIncome.divide(BigDecimal.valueOf(12), INTERNAL_SCALE, RoundingMode.HALF_UP);
        BigDecimal monthlyApplicableAmount = applicablePct.multiply(monthlyIncome);
        return onexLcspPremium.subtract(monthlyApplicableAmount).max(BigDecimal.ZERO);
    }

    /**
     * True if the entered contribution is at or above the flip contribution — the
     * offer is affordable and the employee loses PTC eligibility. False means
     * unaffordable and the employee keeps the credit.
     */
    public static boolean isAffordable(BigDecimal contribution, BigDecimal flipContribution) {
        return contribution.compareTo(flipContribution) >= 0;
    }
}

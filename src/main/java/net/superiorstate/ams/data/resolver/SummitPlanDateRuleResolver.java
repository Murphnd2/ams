package net.superiorstate.ams.data.resolver;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;

/**
 * W4 — the per-mapping-row date rules V103 added to {@code summit_plan_template_map}
 * ({@code effective_date_rule}, {@code offset_months}, {@code plan_year_offset_years}), evaluated
 * into the three dates one Employer CDH Plan row carries: Effective Date (column E), Plan Year
 * Begin (G) and Plan Year End (H).
 * <p>
 * <b>Pure functions.</b> No database, no servlet, no {@code LocalDate.now()} — {@code today} is a
 * parameter so the one rule that reads the calendar can be reasoned about and exercised for any
 * date. Untestable date logic is how a class like this rots.
 * <p>
 * <b>Plan year, every rule:</b> begin = D plus {@code planYearOffsetYears} years; end = the sale's
 * own plan-year end plus the same number of years. ⚠️ End is <i>shifted</i>, not recomputed as
 * "begin + 1 year − 1 day": for a calendar plan year the two agree (LA-30), but the application
 * answer may describe a short first year, and today's file 2 emits that answer verbatim in column H.
 * Shifting keeps a zero-offset row byte-identical to what it emits today.
 * <p>
 * <b>Effective date, by rule:</b>
 * <ul>
 *   <li>{@link #RULE_PLAN_YEAR_START} — the row's own plan-year begin. ⚠️ {@code offsetMonths} is
 *       <b>not applied</b> under this rule; it is only meaningful to
 *       {@code MOST_RECENT_PAST_MONTHDAY}. A future reader will assume otherwise — it is stated here
 *       so they do not.</li>
 *   <li>{@link #RULE_MOST_RECENT_PAST_MONTHDAY} — take the month and day of (D plus
 *       {@code offsetMonths} months), then the most recent occurrence of that month/day that is
 *       <b>strictly before</b> {@code today}. Today 2026-09-21, D 2027-01-01, offset −3 → month/day
 *       10-01 → 2025-10-01. Today 2026-10-03, same inputs → 2026-10-01. Today exactly 2026-10-01 →
 *       2025-10-01 (not strictly past).</li>
 * </ul>
 * ⭐ <b>An effective date outside its own plan year is valid and expected.</b> Proven 2026-09-12:
 * Summit stored {@code 10/01/2025} against plan year 1/1/2026–12/31/2026 verbatim, uncoerced. This
 * class adds no guard that "corrects" it — that is the entire point of the generic card-enabled
 * plan (spec §7 step 1).
 * <p>
 * <b>Feb 29.</b> {@code LocalDate.plusMonths} already clamps to the last valid day of the target
 * month, so D plus offset never produces an invalid date. Where the resulting month/day is 02-29 and
 * the chosen year is not a leap year, the day <b>clamps to 02-28</b> — {@code java.time}'s own
 * convention ({@code YearMonth.atDay} guarded by {@code lengthOfMonth}). A 02-29 effective date
 * therefore lands on 02-28 in three of every four years; it never fails and never skips a year.
 * <p>
 * <b>Unknown rule:</b> returned as a rejection on {@link Resolved}, never thrown — the same shape
 * {@link SummitCdhElementResolver.Parsed} uses for an unknown token, so the servlet can log it and
 * put it on an error page naming the row and the supported values.
 */
public final class SummitPlanDateRuleResolver {

    public static final String RULE_PLAN_YEAR_START = "PLAN_YEAR_START";
    public static final String RULE_MOST_RECENT_PAST_MONTHDAY = "MOST_RECENT_PAST_MONTHDAY";

    /** The supported rule tokens, for error messages. */
    public static final String SUPPORTED_RULES =
            RULE_PLAN_YEAR_START + ", " + RULE_MOST_RECENT_PAST_MONTHDAY;

    private SummitPlanDateRuleResolver() {}

    /** The three dates one CDH row carries. Immutable. */
    public static final class PlanDates {
        private final LocalDate effectiveDate;
        private final LocalDate planYearBegin;
        private final LocalDate planYearEnd;

        PlanDates(LocalDate effectiveDate, LocalDate planYearBegin, LocalDate planYearEnd) {
            this.effectiveDate = effectiveDate;
            this.planYearBegin = planYearBegin;
            this.planYearEnd = planYearEnd;
        }

        /** Column E. May lie outside {@code [planYearBegin, planYearEnd]} by design. */
        public LocalDate getEffectiveDate() { return effectiveDate; }
        /** Column G. */
        public LocalDate getPlanYearBegin() { return planYearBegin; }
        /** Column H. */
        public LocalDate getPlanYearEnd() { return planYearEnd; }
    }

    /** The resolved dates, or the reason they could not be resolved. Never both, never neither. */
    public static final class Resolved {
        private final PlanDates dates;
        private final String rejection;

        private Resolved(PlanDates dates, String rejection) {
            this.dates = dates;
            this.rejection = rejection;
        }

        public PlanDates getDates() { return dates; }
        /** Null when resolved. Otherwise a message naming the bad rule, fit for an error page. */
        public String getRejection() { return rejection; }
        public boolean isRejected() { return rejection != null; }
    }

    /**
     * Evaluate one mapping row's rules against one sale.
     *
     * @param salePlanYearBegin   D — the sale's plan-year start (the {@code plan_year_start} answer)
     * @param salePlanYearEnd     the sale's plan-year end (the {@code plan_year_end} answer)
     * @param rule                {@code effective_date_rule}; compared case-insensitively, trimmed
     * @param offsetMonths        {@code offset_months}; applied to D only under MOST_RECENT_PAST_MONTHDAY
     * @param planYearOffsetYears {@code plan_year_offset_years}; applied to both plan-year dates
     * @param today               the calendar date the MOST_RECENT_PAST_MONTHDAY rule is relative to
     * @param rowRef              how the caller names the row in a rejection (e.g. "mapping row id 12")
     */
    public static Resolved resolve(LocalDate salePlanYearBegin, LocalDate salePlanYearEnd,
                                   String rule, int offsetMonths, int planYearOffsetYears,
                                   LocalDate today, String rowRef) {
        LocalDate planYearBegin = salePlanYearBegin.plusYears(planYearOffsetYears);
        LocalDate planYearEnd = salePlanYearEnd.plusYears(planYearOffsetYears);

        String normalised = rule == null ? "" : rule.trim().toUpperCase();
        if (RULE_PLAN_YEAR_START.equals(normalised)) {
            return new Resolved(new PlanDates(planYearBegin, planYearBegin, planYearEnd), null);
        }
        if (RULE_MOST_RECENT_PAST_MONTHDAY.equals(normalised)) {
            MonthDay monthDay = MonthDay.from(salePlanYearBegin.plusMonths(offsetMonths));
            LocalDate effective = mostRecentPast(monthDay, today);
            return new Resolved(new PlanDates(effective, planYearBegin, planYearEnd), null);
        }
        return new Resolved(null,
                rowRef + " has an unrecognised effective_date_rule '" + rule + "'. Supported values"
                        + " are: " + SUPPORTED_RULES + ". The export refuses rather than defaulting,"
                        + " because a plan created on the wrong effective date imports cleanly and"
                        + " nobody notices. Correct the rule on the Summit Plan Templates mapping,"
                        + " then retry.");
    }

    /**
     * The most recent occurrence of {@code monthDay} strictly before {@code today}. Feb 29 clamps
     * to Feb 28 in a non-leap year.
     */
    static LocalDate mostRecentPast(MonthDay monthDay, LocalDate today) {
        LocalDate candidate = atYear(monthDay, today.getYear());
        if (!candidate.isBefore(today)) {
            candidate = atYear(monthDay, today.getYear() - 1);
        }
        return candidate;
    }

    private static LocalDate atYear(MonthDay monthDay, int year) {
        YearMonth ym = YearMonth.of(year, monthDay.getMonth());
        return ym.atDay(Math.min(monthDay.getDayOfMonth(), ym.lengthOfMonth()));
    }
}

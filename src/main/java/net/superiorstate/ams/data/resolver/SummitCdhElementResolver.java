package net.superiorstate.ams.data.resolver;

import net.superiorstate.ams.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S31-H — the <b>optional</b> element block Summit file 2 (Employer CDH Plan) appends after its eight
 * mandatory columns.
 * <p>
 * <b>AMS conforms to the Summit template, never the reverse</b> (settled session 29). The template
 * decides which optional elements it maps and in what order; this class exists so that order can be
 * stated in configuration rather than compiled into a row builder. Nothing here decides what Summit
 * wants — it only lets an operator say what Summit was told.
 * <p>
 * ⚠️ <b>Unset or blank means emit nothing extra, and that is the default.</b> With
 * {@link #CONFIG_KEY} absent, {@link #configured()} returns an empty list and file 2's bytes are
 * identical to what it emitted before this class existed. An installation that takes this commit
 * without editing {@code ssa.properties} sees no change whatsoever — the same legacy-path protection
 * {@link SummitPlanTemplateResolver} and {@link SummitImportTemplateResolver} both carry.
 * <p>
 * <b>Config key: {@code SUMMIT_CDH_OPTIONAL_ELEMENTS}.</b> An ordered, comma-separated list of the
 * tokens in {@link Element}:
 * <pre>
 * SUMMIT_CDH_OPTIONAL_ELEMENTS=RUNOUT_ENABLED,RUNOUT_BY_DATE,RUNOUT_DAYS,TERM_RUNOUT_TYPE,TERM_RUNOUT_DAYS
 * </pre>
 * ⚠️ <b>The grace period is emitted as a DATE, not a day count</b> — see {@link #graceDate(LocalDate)}
 * for the statutory reason. {@code GRACE_DAYS} remains a supported token so a template that maps that
 * column still binds positionally, but <b>AMS has no source for it and it always emits empty</b>.
 * <b>Config order is emit order</b>, following the convention {@code SUMMIT_PLAN_TEMPLATES} and
 * {@code AmsDataGlobal.getPspHosts()} already establish on this installation: config order carries
 * operator intent and the reader preserves it rather than sorting.
 * <p>
 * ⚠️ <b>An unrecognised token is a REFUSAL, not a skip — and this is the one place this family of
 * resolvers deliberately breaks with its siblings.</b> {@code SummitPlanTemplateResolver} skips a
 * malformed entry with a {@code WARN} and serves the rest, because there one bad entry costs one
 * plan row. Here it would cost <b>column alignment</b>: silently dropping one element shifts every
 * element after it one position left, and Summit binds optional elements <b>positionally</b>. A
 * typo would then load a run-out day count into a Boolean field, and Summit accepts a wrong plan
 * setting without complaint. So a bad token stops the export and names itself.
 * <p>
 * <b>Parsing never throws.</b> A rejection is returned as data on {@link Parsed}, so the caller can
 * put it on an error page rather than a stack trace in a log nobody reads.
 */
public final class SummitCdhElementResolver {

    private static final Logger log = LogManager.getLogger(SummitCdhElementResolver.class);

    /** The {@code ssa.properties} key this resolver reads. Named so callers can cite it in errors. */
    public static final String CONFIG_KEY = "SUMMIT_CDH_OPTIONAL_ELEMENTS";

    /** Maps a mapping row's {@code key_segment} to the application field holding that plan's grace answer. */
    public static final String GRACE_FIELDS_KEY = "SUMMIT_CDH_GRACE_FIELDS";

    // ── Fixed-value keys, each defaulting to a real value rather than to absence, following
    //    SUMMIT_BRANCH_CODE's precedent (S29-I): a mandatory-once-mapped element must never emit
    //    empty just because nobody set a property.
    private static final String RUNOUT_DAYS_KEY = "SUMMIT_CDH_RUNOUT_DAYS";
    private static final String TERM_RUNOUT_DAYS_KEY = "SUMMIT_CDH_TERM_RUNOUT_DAYS";
    private static final String TERM_RUNOUT_TYPE_KEY = "SUMMIT_CDH_TERM_RUNOUT_TYPE";
    private static final String BOOL_TRUE_KEY = "SUMMIT_CDH_BOOL_TRUE";
    private static final String BOOL_FALSE_KEY = "SUMMIT_CDH_BOOL_FALSE";

    static final String DEFAULT_RUNOUT_DAYS = "90";
    static final String DEFAULT_TERM_RUNOUT_DAYS = "90";
    /** Summit's "days after termination" run-out type. */
    static final String DEFAULT_TERM_RUNOUT_TYPE = "1";
    static final String DEFAULT_BOOL_TRUE = "true";
    static final String DEFAULT_BOOL_FALSE = "false";

    private SummitCdhElementResolver() {}

    /** One optional element the {@code Employer CDH Plan} template may map. */
    public enum Element {
        GRACE_ENABLED, GRACE_BY_DATE, GRACE_DATE, GRACE_DAYS,
        RUNOUT_ENABLED, RUNOUT_BY_DATE, RUNOUT_DAYS,
        TERM_RUNOUT_TYPE, TERM_RUNOUT_DAYS,
        OPEN_ENROLL_START, OPEN_ENROLL_END
    }

    /**
     * What one plan's grace answer says. {@link #UNRECOGNISED} covers absent, blank and unknown
     * alike — every one of them means "this export cannot tell what the employer chose", and all
     * three take the same refusal.
     */
    public enum GraceChoice {
        /** The employer chose a grace period. */
        GRACE,
        /** The employer explicitly chose no end-of-year feature. */
        NONE,
        /** Carryover — <b>not importable through this template at all</b>; the plan is skipped (S31-H). */
        CARRYOVER,
        /**
         * This plan has no grace concept: its {@code key_segment} is not listed in
         * {@link #GRACE_FIELDS_KEY}. ICHRA takes this path. Grace elements emit empty; nothing is
         * wrong and nothing is refused.
         */
        NOT_APPLICABLE,
        /** Absent, blank or unknown — all three mean the export cannot tell, and all three refuse. */
        UNRECOGNISED
    }

    /**
     * The parsed element list, or the reason it could not be parsed. Never both, never neither.
     */
    public static final class Parsed {
        private final List<Element> elements;
        private final String rejection;

        private Parsed(List<Element> elements, String rejection) {
            this.elements = elements;
            this.rejection = rejection;
        }

        /** The elements in emit order. Empty when the key is unset — the default, and not an error. */
        public List<Element> getElements() {
            return elements;
        }

        /** Null when the list parsed. Otherwise a message naming the bad token, fit for an error page. */
        public String getRejection() {
            return rejection;
        }

        public boolean isRejected() {
            return rejection != null;
        }
    }

    /**
     * The configured optional elements, in emit order.
     *
     * @return a {@link Parsed} carrying an unmodifiable list — empty when {@link #CONFIG_KEY} is
     *         absent or blank — or a rejection naming the first unrecognised token. Never null.
     */
    public static Parsed configured() {
        String raw = AppConfig.get(CONFIG_KEY);
        if (raw == null || raw.isBlank()) {
            return new Parsed(Collections.emptyList(), null);
        }

        List<Element> elements = new ArrayList<>();
        for (String entry : raw.split(",")) {
            String token = entry.trim();
            // A stray or trailing comma is not a typo worth refusing over -- it cannot shift a
            // column, because it contributes no element. Only an unrecognised NAME can.
            if (token.isEmpty()) continue;

            Element element;
            try {
                element = Element.valueOf(token.toUpperCase());
            } catch (IllegalArgumentException e) {
                StringBuilder supported = new StringBuilder();
                for (Element known : Element.values()) {
                    if (supported.length() > 0) supported.append(", ");
                    supported.append(known.name());
                }
                return new Parsed(Collections.emptyList(),
                        CONFIG_KEY + " contains an unrecognised element token '" + token + "'."
                                + " Supported tokens are: " + supported + ". The export refuses rather"
                                + " than skipping the token, because Summit binds optional elements by"
                                + " position: dropping one would shift every element after it and load"
                                + " each value into the wrong field, which Summit accepts without"
                                + " complaint. Fix the token in ssa.properties, then restart Tomcat.");
            }
            if (elements.contains(element)) {
                return new Parsed(Collections.emptyList(),
                        CONFIG_KEY + " lists element '" + element.name() + "' more than once. Each"
                                + " element maps to exactly one column in the Summit template, so a"
                                + " repeat is a configuration error rather than two columns. Remove"
                                + " the duplicate in ssa.properties, then restart Tomcat.");
            }
            elements.add(element);
        }

        return new Parsed(Collections.unmodifiableList(elements), null);
    }

    /**
     * The {@code key_segment}-to-application-field map from {@link #GRACE_FIELDS_KEY}, e.g.
     * {@code FSA:hfsa_roll_or_grace,DCAP:dcap_grace}.
     * <p>
     * ⚠️ <b>A key segment absent from this map is a supported state, not an error</b> — it means
     * that plan has no grace concept and its grace elements emit empty. ICHRA takes exactly that
     * path. One layout for every row, blank where it does not apply.
     * <p>
     * A malformed entry is skipped with a {@code WARN}, matching the sibling resolvers. ⚠️ Note the
     * consequence, which is why the {@code WARN} names the entry: a skipped entry is
     * indistinguishable at emit time from a key segment that was never listed, so the plan quietly
     * takes the empty-grace path rather than refusing. Check {@code catalina.out} after changing
     * this key.
     *
     * @return an unmodifiable map keyed by the upper-cased key segment. Empty when unset. Never null.
     */
    public static Map<String, String> graceFields() {
        String raw = AppConfig.get(GRACE_FIELDS_KEY);
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (String entry : raw.split(",")) {
            String trimmedEntry = entry.trim();
            if (trimmedEntry.isEmpty()) continue;

            String[] parts = trimmedEntry.split(":");
            if (parts.length != 2) {
                warnSkip(trimmedEntry, "expected 2 colon-separated fields, found " + parts.length);
                continue;
            }
            String keySegment = parts[0].trim().toUpperCase();
            String fieldKey = parts[1].trim();
            if (keySegment.isEmpty()) {
                warnSkip(trimmedEntry, "blank key segment");
                continue;
            }
            if (fieldKey.isEmpty()) {
                warnSkip(trimmedEntry, "blank application field key");
                continue;
            }
            if (fields.containsKey(keySegment)) {
                log.warn("[SUMMIT-EXPORT] {} entry '{}' skipped: key segment '{}' is already mapped"
                                + " earlier in the list; the first entry wins",
                        GRACE_FIELDS_KEY, trimmedEntry, keySegment);
                continue;
            }
            fields.put(keySegment, fieldKey);
        }
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Classifies one plan's stored grace answer.
     * <p>
     * ⚠️ <b>The accepted strings are the application's own option labels, matched verbatim.</b>
     * S31-H established by tracing render → request parameter → persist that a {@code RADIO} stores
     * the option token itself: {@code applyForProposal.jsp} emits {@code value="${opt}"}, and both
     * {@code ApplyForProposal} and {@code SaveApplicationProgress} store {@code paramValue.trim()}.
     * The options are {@code None|Carryover|2-1/2 Month Grace Period} for {@code hfsa_roll_or_grace}
     * and {@code None|2-1/2 Month Grace Period} for {@code dcap_grace}.
     * <p>
     * ⚠️ <b>So the stored value is a display label, and a display label can be edited</b> in the
     * Service Manager. If someone rewords an option, answers saved afterwards will not match here
     * and this returns {@link GraceChoice#UNRECOGNISED} — which <b>refuses the export</b>. That is
     * the safe direction and it is deliberate: the alternative, treating an unknown answer as "no
     * grace", writes a wrong plan setting that imports cleanly and is never noticed. Trimmed and
     * case-insensitive comparison absorbs whitespace and casing drift, and nothing more.
     */
    public static GraceChoice classifyGrace(String storedAnswer) {
        if (storedAnswer == null) return GraceChoice.UNRECOGNISED;
        String value = storedAnswer.trim();
        if (value.isEmpty()) return GraceChoice.UNRECOGNISED;
        if (value.equalsIgnoreCase("None")) return GraceChoice.NONE;
        if (value.equalsIgnoreCase("Carryover")) return GraceChoice.CARRYOVER;
        if (value.equalsIgnoreCase("2-1/2 Month Grace Period")) return GraceChoice.GRACE;
        return GraceChoice.UNRECOGNISED;
    }

    /** The exact answers {@link #classifyGrace} recognises, for an operator-facing error message. */
    public static String acceptedGraceAnswers() {
        return "'None', 'Carryover' or '2-1/2 Month Grace Period'";
    }

    // ── Emitted representations ───────────────────────────────────────

    /**
     * ⚠️ <b>How Summit wants a Boolean in a delimited file is UNPROVEN</b> — its element list says
     * only "Boolean", and nothing has been imported to settle whether it wants {@code true}/
     * {@code false}, {@code 1}/{@code 0} or {@code Y}/{@code N}. Rather than guess in code, both
     * sides are config, defaulting to {@code true}/{@code false}. One import settles it and the fix
     * is an {@code ssa.properties} edit, not a build.
     */
    public static String bool(boolean value) {
        return value ? configuredOrDefault(BOOL_TRUE_KEY, DEFAULT_BOOL_TRUE)
                     : configuredOrDefault(BOOL_FALSE_KEY, DEFAULT_BOOL_FALSE);
    }

    public static String runoutDays() {
        return configuredOrDefault(RUNOUT_DAYS_KEY, DEFAULT_RUNOUT_DAYS);
    }

    public static String termRunoutDays() {
        return configuredOrDefault(TERM_RUNOUT_DAYS_KEY, DEFAULT_TERM_RUNOUT_DAYS);
    }

    public static String termRunoutType() {
        return configuredOrDefault(TERM_RUNOUT_TYPE_KEY, DEFAULT_TERM_RUNOUT_TYPE);
    }

    /**
     * S31-I — <b>the last day of the grace period</b>, as a calendar date rather than a duration.
     * <p>
     * ⚠️ <b>A grace period is a calendar rule, not a day count, and no fixed count can express it.</b>
     * <b>Treas. Reg. §1.125-1(e)</b> caps a grace period at <b>the fifteenth day of the third calendar
     * month after the end of the plan year</b>. The length that implies changes with the plan year's
     * end month:
     * <ul>
     *   <li>plan year ends 31 December → <b>15 March</b> — 74 days in a non-leap year</li>
     *   <li>plan year ends 30 June → <b>15 September</b> — 77 days</li>
     *   <li>plan year ends 31 January → <b>15 April</b> — 74 days</li>
     * </ul>
     * ⚠️ <b>S31-H got this wrong and it is worth recording why.</b> It emitted a fixed 75-day count.
     * <b>75 days after 31 December is 16 March</b> — one day <em>beyond</em> the statutory maximum,
     * every non-leap plan year — and it would have been wrong by a different amount for every other
     * year-end month. Summit supports the rule directly through
     * {@code Grace Period Calculation (by date)} + {@code Grace Period Date}, which is what this
     * method feeds. <b>Do not reintroduce a day count.</b>
     * <p>
     * The computation is deliberately month arithmetic, never day arithmetic: take the plan year end,
     * add three months, set the day to 15. {@code LocalDate.plusMonths} clamps a day-of-month that the
     * target month does not have (31 January + 3 months is 30 April), but that clamp is invisible here
     * because the day is overwritten immediately afterwards — the result depends only on the month.
     *
     * @param planYearEnd the plan year's last day, as the writer already parsed it. Null returns null.
     * @return the grace period's last day, or null when {@code planYearEnd} is null. <b>Formatting is
     *         the caller's job</b> — the export's own {@code yyyyMMdd} formatter lives in the servlet
     *         and is not restated here.
     */
    public static LocalDate graceDate(LocalDate planYearEnd) {
        if (planYearEnd == null) return null;
        return planYearEnd.plusMonths(3).withDayOfMonth(15);
    }

    /**
     * A configured value, or the default when the key is unset or blank. ⚠️ There is deliberately no
     * branch that emits nothing: every element reached here is one the Summit template has been told
     * to expect, and a mapped element emitting empty is the trailing-empty-field defect S29-I
     * already paid for once.
     */
    private static String configuredOrDefault(String key, String defaultValue) {
        String raw = AppConfig.get(key);
        return (raw == null || raw.isBlank()) ? defaultValue : raw.trim();
    }

    private static void warnSkip(String entry, String reason) {
        log.warn("[SUMMIT-EXPORT] {} entry '{}' skipped: {}", GRACE_FIELDS_KEY, entry, reason);
    }
}

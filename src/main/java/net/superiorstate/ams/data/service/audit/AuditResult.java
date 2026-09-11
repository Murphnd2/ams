package net.superiorstate.ams.data.service.audit;

/**
 * The outcome of one {@link AuditCheck#evaluate} call (T237).
 * <p>
 * ⚠️ <b>Counts only.</b> {@code summary} and {@code error} must never carry a person's name, an
 * id that identifies a specific person, or any content read from a source row. They are stored
 * verbatim in {@code audit_run} (V099) — see LA-40 in {@code docs/analysis/legal_assumptions.md}.
 *
 * @param status       {@code OK}, {@code ACTION}, {@code ERROR}, or {@code NOT_CONFIGURED}.
 * @param findingCount how many items the check found. Zero for {@code OK}, {@code ERROR} and
 *                     {@code NOT_CONFIGURED}.
 * @param summary      a one-line, counts-only description, e.g.
 *                     {@code "3 participant(s) at 2 employer(s) — export FOO_Export_....csv"}.
 * @param error        a scrubbed message when {@code status} is {@code ERROR} or
 *                     {@code NOT_CONFIGURED}; {@code null} otherwise.
 */
public record AuditResult(String status, int findingCount, String summary, String error) {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ACTION = "ACTION";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_NOT_CONFIGURED = "NOT_CONFIGURED";

    public static AuditResult ok(String summary) {
        return new AuditResult(STATUS_OK, 0, summary, null);
    }

    public static AuditResult action(int findingCount, String summary) {
        return new AuditResult(STATUS_ACTION, findingCount, summary, null);
    }

    public static AuditResult error(String message) {
        return new AuditResult(STATUS_ERROR, 0, null, message);
    }

    public static AuditResult notConfigured(String message) {
        return new AuditResult(STATUS_NOT_CONFIGURED, 0, null, message);
    }
}

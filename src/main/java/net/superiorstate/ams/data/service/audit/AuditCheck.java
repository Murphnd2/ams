package net.superiorstate.ams.data.service.audit;

import jakarta.persistence.EntityManager;

/**
 * One registered audit check (T237). Implementations are stateless and registered in code by
 * {@code AuditService} — nothing installation-specific lives here; per-installation config is
 * read by the implementation itself (typically via {@code AppConfig.get}).
 * <p>
 * {@link #evaluate} must never throw. A check that cannot complete (missing config, an
 * unreachable source, a malformed export) returns an {@link AuditResult} with status
 * {@code ERROR} or {@code NOT_CONFIGURED} and a scrubbed message — it does not propagate an
 * exception, because {@code AuditService.runAll} must be able to run every other check even if
 * this one fails outright. (It still wraps every call in its own try/catch as a second layer,
 * exactly so a check that breaks this contract cannot take another check down with it.)
 */
public interface AuditCheck {

    /** Stable identifier, e.g. {@code "ichra_uncoded_participants"}. Never changes once shipped
     *  — it is the join key against {@code audit_run.check_key}. */
    String key();

    /** Human-readable label for the hub page. */
    String label();

    /** Servlet path for this check's detail page, e.g. {@code "/AuditIchraUncoded"}. */
    String detailPath();

    /** Runs the check for one PSP. Never throws — see the class note. */
    AuditResult evaluate(EntityManager em, Long pspId);
}

package net.superiorstate.ams.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V099 — one row per (check, run) of the audit framework (T237). Counts only, never personal
 * data: {@link #summary} and {@link #error} are scrubbed, one-line strings written by the
 * check itself, never row content from the source the check read. See
 * {@code docs/analysis/audit_framework.md} and {@code docs/analysis/legal_assumptions.md} LA-40.
 * <p>
 * <b>Findings are self-clearing.</b> There is no acknowledge/dismiss state here — a finding
 * exists only while the next run's {@link #findingCount} is still nonzero for that
 * {@link #checkKey}. Nothing here is mutated after insert; each run is a new row.
 * <p>
 * Scalar {@link #pspId}, same reasoning as {@code SummitFileExport#pspId} /
 * {@code SummitSetupStep#pspId}: nullable so a recording failure never blocks a run from
 * completing.
 */
@Entity
@Table(name = "audit_run")
public class AuditRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** The installation PSP this run was scoped to. Null when it could not be resolved. */
    @Column(name = "psp_id")
    private Long pspId;

    /** Stable check identifier, e.g. {@code ichra_uncoded_participants}. */
    @Column(name = "check_key", nullable = false, length = 50)
    private String checkKey;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    /** SCHEDULED or MANUAL. */
    @Column(name = "run_trigger", nullable = false, length = 10)
    private String runTrigger;

    /** OK, ACTION, ERROR, or NOT_CONFIGURED. */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "finding_count", nullable = false)
    private int findingCount;

    /** Counts only — never a name, an id of a person, or file content. */
    @Column(name = "summary", length = 500)
    private String summary;

    /** A scrubbed message. Never the row content that caused the error. */
    @Column(name = "error", length = 500)
    private String error;

    @Column(name = "duration_ms")
    private Integer durationMs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }

    public String getCheckKey() { return checkKey; }
    public void setCheckKey(String checkKey) { this.checkKey = checkKey; }

    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime runAt) { this.runAt = runAt; }

    public String getRunTrigger() { return runTrigger; }
    public void setRunTrigger(String runTrigger) { this.runTrigger = runTrigger; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFindingCount() { return findingCount; }
    public void setFindingCount(int findingCount) { this.findingCount = findingCount; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
}

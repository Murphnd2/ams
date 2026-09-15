package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V115 — one row of the generic, PSP-scoped Handled/Ignored acknowledgment for one finding of one
 * audit check, keyed by {@code checkKey} ({@code AuditCheck.key()}, matching {@code audit_run
 * .check_key}) and a check-defined {@code findingKey}. Generic across every check in the T237
 * framework; wired to {@code CardDeclineCheck} only as of this build ({@code findingKey} =
 * {@code Participant System ID}) — see {@code V115__audit_finding_ack.sql}.
 * <p>
 * <b>Two states, evaluated differently — see {@link #STATE_HANDLED}, {@link #STATE_IGNORED}.</b>
 * {@code HANDLED} is state-scoped: {@code observedCount}/{@code observedThrough} record what was
 * true at acknowledgment time, and the owning check re-surfaces the finding once current activity
 * moves past them. {@code IGNORED} is unconditional and carries neither.
 * <p>
 * <b>Scalar foreign keys, not relations</b> — {@code pspId}, following {@link AuditDeclineEmployer}'s
 * and {@link SummitServiceItemFlags}'s convention in this package. {@code checkKey} and
 * {@code findingKey} name no table to relate to — see the migration's note on why neither carries
 * an FK.
 */
@Entity
@Table(name = "audit_finding_ack")
public class AuditFindingAck {

    public static final String STATE_HANDLED = "HANDLED";
    public static final String STATE_IGNORED = "IGNORED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "psp_id", nullable = false)
    private Long pspId;

    @Column(name = "check_key", nullable = false, length = 50)
    private String checkKey;

    @Column(name = "finding_key", nullable = false, length = 255)
    private String findingKey;

    /** {@link #STATE_HANDLED} or {@link #STATE_IGNORED}. */
    @Column(name = "ack_state", nullable = false, length = 16)
    private String ackState;

    /** Set for {@code HANDLED}, {@code null} for {@code IGNORED}. */
    @Column(name = "observed_count")
    private Integer observedCount;

    /** Set for {@code HANDLED}, {@code null} for {@code IGNORED}. */
    @Column(name = "observed_through")
    private LocalDate observedThrough;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public AuditFindingAck() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }

    public String getCheckKey() { return checkKey; }
    public void setCheckKey(String checkKey) { this.checkKey = checkKey; }

    public String getFindingKey() { return findingKey; }
    public void setFindingKey(String findingKey) { this.findingKey = findingKey; }

    public String getAckState() { return ackState; }
    public void setAckState(String ackState) { this.ackState = ackState; }

    public Integer getObservedCount() { return observedCount; }
    public void setObservedCount(Integer observedCount) { this.observedCount = observedCount; }

    public LocalDate getObservedThrough() { return observedThrough; }
    public void setObservedThrough(LocalDate observedThrough) { this.observedThrough = observedThrough; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

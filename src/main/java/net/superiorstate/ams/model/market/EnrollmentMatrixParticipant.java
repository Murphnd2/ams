package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V106 — the header row: one per participant per {@link EnrollmentMatrix}. Schema and entity
 * layer only this build; no UI, no exporter, no report reads or writes this yet.
 * <p>
 * <b>Scalar foreign keys, not relations</b> — {@code matrixId} and {@code participantId} are
 * plain columns, following {@link EnrollmentMatrix}'s and {@link SummitPlanTemplateMap}'s own
 * convention in this package. {@code participantId} points at {@code employer_participant(id)}
 * (V094), the AMS-owned pre-Summit census roster.
 * <p>
 * <b>One header row per participant per matrix, enforced at the database</b> —
 * {@code (matrixId, participantId)} carries a unique constraint.
 * <p>
 * <b>{@code payrollFrequency} is a plain string, deliberately.</b> Which global payroll-frequency
 * values are enrollment-approved is Kevin's to designate and is not decided this build — the
 * column is code-validated later, the same way {@link SummitPlanTemplateMap#getTaxTreatment()}
 * and {@link SummitPlanTemplateMap#getEnrollmentAmountMode()} are code-validated rather than a
 * database {@code ENUM}. No default, no members invented here. Two values the design already
 * names are stored the same way, as plain strings, not specially declared: {@code OTHER_CUSTOM}
 * ({@link #customScheduleName} carries a Summit-side schedule name the employer has set up) and
 * {@code OTHER_NOT_IMPORTABLE} (none of this participant's entries go in the FTP export).
 * <p>
 * <b>{@code isEntryLocked} is per participant and soft (confirm-to-unlock)</b> — different from
 * {@link EnrollmentMatrix#isPushed()}, which is the whole matrix. A participant can be locked
 * (entry finished) while the matrix as a whole is still open.
 */
@Entity
@Table(name = "enrollment_matrix_participant")
public class EnrollmentMatrixParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "matrix_id", nullable = false)
    private Long matrixId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "payroll_frequency")
    private String payrollFrequency;

    @Column(name = "custom_schedule_name")
    private String customScheduleName;

    @Column(name = "is_entry_locked", nullable = false)
    private boolean entryLocked = false;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public EnrollmentMatrixParticipant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMatrixId() { return matrixId; }
    public void setMatrixId(Long matrixId) { this.matrixId = matrixId; }

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public String getPayrollFrequency() { return payrollFrequency; }
    public void setPayrollFrequency(String payrollFrequency) { this.payrollFrequency = payrollFrequency; }

    public String getCustomScheduleName() { return customScheduleName; }
    public void setCustomScheduleName(String customScheduleName) { this.customScheduleName = customScheduleName; }

    public boolean isEntryLocked() { return entryLocked; }
    public void setEntryLocked(boolean entryLocked) { this.entryLocked = entryLocked; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

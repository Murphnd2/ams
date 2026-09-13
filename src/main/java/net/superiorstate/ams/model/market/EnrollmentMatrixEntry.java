package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V106 — the detail row: one per participant per enrollment leg. Schema and entity layer only
 * this build; no UI, no exporter, no report reads or writes this yet.
 * <p>
 * <b>Scalar foreign keys, not relations</b> — {@code matrixParticipantId} and
 * {@code planTemplateMapId} are plain columns, following {@link EnrollmentMatrix}'s and
 * {@link SummitPlanTemplateMap}'s own convention in this package. {@code planTemplateMapId}
 * points at {@link SummitPlanTemplateMap} — the leg — and V103's fan-out (several mapping rows
 * per service item) is exactly what lets one participant hold several entries under one matrix.
 * <p>
 * <b>One detail row per participant per leg, enforced at the database</b> —
 * {@code (matrixParticipantId, planTemplateMapId)} carries a unique constraint.
 * <p>
 * <b>One {@code amount} column, not two.</b> Monthly premium and annual election are mutually
 * exclusive and both money — which one it is comes from the leg's own
 * {@link SummitPlanTemplateMap#getEnrollmentAmountMode()} (V105), not from a second column here.
 * {@link #tierName} is separate because a tier is a selection, not a figure: under
 * {@code TIER} mode no amount is keyed at all — the HRA setup already carries the amount against
 * the tier, and the enrollment file resolves it by tier-name match.
 * <p>
 * <b>Decline tracking is independent of amount/tier</b> — a declined entry has neither.
 */
@Entity
@Table(name = "enrollment_matrix_entry")
public class EnrollmentMatrixEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "matrix_participant_id", nullable = false)
    private Long matrixParticipantId;

    @Column(name = "plan_template_map_id", nullable = false)
    private Long planTemplateMapId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "tier_name")
    private String tierName;

    @Column(name = "is_declined", nullable = false)
    private boolean declined = false;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public EnrollmentMatrixEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMatrixParticipantId() { return matrixParticipantId; }
    public void setMatrixParticipantId(Long matrixParticipantId) { this.matrixParticipantId = matrixParticipantId; }

    public Long getPlanTemplateMapId() { return planTemplateMapId; }
    public void setPlanTemplateMapId(Long planTemplateMapId) { this.planTemplateMapId = planTemplateMapId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public boolean isDeclined() { return declined; }
    public void setDeclined(boolean declined) { this.declined = declined; }

    public LocalDateTime getDeclinedAt() { return declinedAt; }
    public void setDeclinedAt(LocalDateTime declinedAt) { this.declinedAt = declinedAt; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

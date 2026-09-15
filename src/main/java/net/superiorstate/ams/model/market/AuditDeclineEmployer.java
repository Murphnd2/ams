package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V114 — one row of the PSP-scoped list of AMS employers whose card declines
 * {@code CardDeclineCheck} evaluates (T237 check #3). Card-decline monitoring is opt-in per
 * employer, set through {@code AuditDeclineEmployerAdmin}.
 * <p>
 * <b>Scalar foreign keys, not relations</b> — {@code pspId} and {@code employerId} are plain
 * columns, following {@link SummitServiceItemFlags}'s and {@link SummitPlanTemplateMap}'s
 * convention in this package rather than {@code @ManyToOne} graph edges. The DAO joins to
 * {@code Employer} explicitly when a name or the Summit id is wanted; nothing navigates from a
 * row to a {@code PSP} or an {@code Employer}.
 * <p>
 * ⚠️ {@code employerId} is the <b>AMS</b> employer primary key ({@code employer.organization_id},
 * {@code Employer.id}, an {@code int}) — <b>not</b> the Summit {@code EmployerID}
 * ({@code employer.employer_id}, {@code Employer.altId}) that the check matches against the
 * export's {@code Employer SystemID}. Storing the FK keeps referential integrity and resolves the
 * employer name for free; {@code altId} is read off the joined row. Identity settled 2026-09-15:
 * {@code Employer SystemID} = Plan History {@code Employer_ID} = Summit {@code EmployerID} =
 * {@code Employer.altId} (sample: {@code Employer_ID} 1100 vs {@code Organization_ID} 1102; the
 * Transaction export carries 1100, not 1102).
 * <p>
 * ⚠️ <b>A row's presence is the designation — there is no {@code isActive} column</b>, exactly
 * {@link SummitServiceItemFlags}'s reasoning: a designation creates nothing in Summit and is not an
 * upsert identity for anything, so an admin who no longer wants an employer monitored removes the
 * row. {@code CardDeclineCheck} reports {@code NOT_CONFIGURED} for a PSP with no rows here.
 */
@Entity
@Table(name = "audit_decline_employer")
public class AuditDeclineEmployer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "psp_id", nullable = false)
    private Long pspId;

    @Column(name = "employer_id", nullable = false)
    private Integer employerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public AuditDeclineEmployer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }

    public Integer getEmployerId() { return employerId; }
    public void setEmployerId(Integer employerId) { this.employerId = employerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

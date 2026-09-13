package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V106 — one row per setup activity: the enrollment matrix's own table-wide push lock. Schema
 * and entity layer only this build; no UI, no exporter, no report reads or writes this yet.
 * <p>
 * <b>Scalar foreign key, not a relation</b> — {@code setupId} is a plain column, following
 * {@link SummitPlanTemplateMap}'s and {@link SummitServiceItemFlags}'s own convention in this
 * package rather than a {@code @ManyToOne} graph edge.
 * <p>
 * {@code setupId} points at {@code assignee(id)}, not a {@code setup} table — {@code Setup}
 * extends {@code Activity} extends {@code Assignee} under single-table inheritance (no
 * {@code @Table} override anywhere in that chain), the same reason
 * {@link SummitPlanTemplateMap#getPspId()} and {@link SummitServiceItemFlags#getPspId()} both
 * target {@code assignee(id)} for a {@code PSP} row.
 * <p>
 * <b>One matrix per setup, enforced at the database</b> — {@code setupId} carries a unique
 * constraint, not just an application-level check.
 * <p>
 * <b>The push lock lives here, not on the participant or entry rows</b> — a table-wide lock
 * needs a single owner rather than a flag repeated on every participant. This is a different
 * lock from {@link EnrollmentMatrixParticipant#isEntryLocked()}, which is per participant and
 * soft (confirm-to-unlock); this one is the whole matrix, set once the file is pushed.
 */
@Entity
@Table(name = "enrollment_matrix")
public class EnrollmentMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "setup_id", nullable = false)
    private Long setupId;

    @Column(name = "is_pushed", nullable = false)
    private boolean pushed = false;

    @Column(name = "pushed_by")
    private String pushedBy;

    @Column(name = "pushed_at")
    private LocalDateTime pushedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public EnrollmentMatrix() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSetupId() { return setupId; }
    public void setSetupId(Long setupId) { this.setupId = setupId; }

    public boolean isPushed() { return pushed; }
    public void setPushed(boolean pushed) { this.pushed = pushed; }

    public String getPushedBy() { return pushedBy; }
    public void setPushedBy(String pushedBy) { this.pushedBy = pushedBy; }

    public LocalDateTime getPushedAt() { return pushedAt; }
    public void setPushedAt(LocalDateTime pushedAt) { this.pushedAt = pushedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

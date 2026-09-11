package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V098 — one row per (proposal, step) recording PSP-admin "Mark done" on the Summit setup panel,
 * for the T230 phase-1 steps: {@code employer}, {@code cdhplan}, {@code schedules},
 * {@code demographics}. ({@code enrollment} is priority 2 and is never written here; the
 * whitelist lives in {@code SummitResponseServlet}, not this entity.)
 * <p>
 * <b>Completion is a PSP-admin decision, not a Summit fact.</b> This is why the row is a separate
 * entity from {@link SummitFileExport} rather than an extension of it (T230's S42 decision,
 * reversing that item's earlier "likely an extension" note) — Mark done must work with no export
 * at all, for a group already set up in Summit directly or entered by hand.
 * <p>
 * ⚠️ <b>No response content lives here.</b> A response line can echo personal data — Demographics
 * echoes participant names, and {@link SummitFileExport}'s own class note records a rejection
 * comment that once echoed a full street address. The response is fetched, parsed and rendered
 * in-request by {@code SummitResponseServlet} and discarded; only the fact and basis of
 * completion are stored here.
 * <p>
 * <b>Scalar {@link #pspId}, real FK on {@link #exportId}.</b> {@link #pspId} is nullable and
 * unconstrained-in-spirit the same way {@link SummitFileExport#pspId} is — a recording failure to
 * resolve a PSP must not block Mark done. {@link #exportId}, in contrast, carries a genuine foreign
 * key: a {@code REVIEWED} row's whole reason for existing is "this specific export's response was
 * reviewed", so if that export row were ever removed the review claim should break visibly (FK
 * violation) rather than silently point at nothing.
 */
@Entity
@Table(name = "summit_setup_step")
public class SummitSetupStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** The PSP whose session marked this step. Null when the session resolved none. */
    @Column(name = "psp_id")
    private Long pspId;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    /** employer | cdhplan | schedules | demographics. See the class note on enrollment. */
    @Column(name = "step_key", nullable = false, length = 20)
    private String stepKey;

    /** DONE or OPEN. */
    @Column(name = "state", nullable = false, length = 10)
    private String state;

    /** REVIEWED or MANUAL. Null whenever {@link #state} is OPEN. */
    @Column(name = "basis", length = 10)
    private String basis;

    /** The {@link SummitFileExport} whose response was reviewed. Set only for REVIEWED. */
    @Column(name = "export_id")
    private Long exportId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Display name of the acting user. Null when the session cannot name anyone. */
    @Column(name = "updated_by")
    private String updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }

    public Long getProposalId() { return proposalId; }
    public void setProposalId(Long proposalId) { this.proposalId = proposalId; }

    public String getStepKey() { return stepKey; }
    public void setStepKey(String stepKey) { this.stepKey = stepKey; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }

    public Long getExportId() { return exportId; }
    public void setExportId(Long exportId) { this.exportId = exportId; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

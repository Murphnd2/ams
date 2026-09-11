package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V101 — one row of the PSP-scoped mapping from an elected {@code ServiceItem} to the four Summit
 * Employer Demographic administration flags: Enable CDH Administration, Enable COBRA
 * Administration, Enable Retiree Billing Administration, Enable Direct Bill Administration (T238
 * part 1, D46).
 * <p>
 * <b>Scalar foreign keys, not relations</b> — {@code pspId} and {@code serviceItemId} are plain
 * columns, following {@link SummitPlanTemplateMap}'s own convention in this package rather than
 * {@code @ManyToOne} graph edges. {@link net.superiorstate.ams.data.resolver.SummitEmployerFlagResolver}
 * needs scalars to compute a union across elected items and never navigates from a row to a
 * {@code PSP} or {@code ServiceItem}.
 * <p>
 * ⚠️ {@code serviceItemId} is an {@code Integer}, not a {@code Long} — {@code ServiceItem}'s own
 * primary key is {@code int} ({@code templatepurpose.purpose_id}), matching
 * {@link SummitPlanTemplateMap#getServiceItemId()} exactly.
 * <p>
 * ⚠️ <b>A row's presence is the mapping — there is no {@code isActive} column</b>, unlike
 * {@link SummitPlanTemplateMap}. A flag mapping creates nothing in Summit on its own (unlike a
 * plan template mapping, whose {@code Import Plan ID} becomes an upsert key for a Summit-side
 * plan), so there is no upsert identity to protect by retiring a row instead of deleting it — an
 * admin who no longer wants an item's flags removes the row.
 * <p>
 * <b>Nothing reads this table yet.</b> {@code SummitEmployerFlagResolver} computes the union across
 * elected items; the file 1 emitter change that would use it is a later, separate build, blocked
 * on SDX-27.
 */
@Entity
@Table(name = "summit_service_item_flags")
public class SummitServiceItemFlags {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "psp_id", nullable = false)
    private Long pspId;

    @Column(name = "service_item_id", nullable = false)
    private Integer serviceItemId;

    @Column(name = "enable_cdh", nullable = false)
    private boolean enableCdh;

    @Column(name = "enable_cobra", nullable = false)
    private boolean enableCobra;

    @Column(name = "enable_retiree_billing", nullable = false)
    private boolean enableRetireeBilling;

    @Column(name = "enable_direct_bill", nullable = false)
    private boolean enableDirectBill;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public SummitServiceItemFlags() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }

    public Integer getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Integer serviceItemId) { this.serviceItemId = serviceItemId; }

    public boolean isEnableCdh() { return enableCdh; }
    public void setEnableCdh(boolean enableCdh) { this.enableCdh = enableCdh; }

    public boolean isEnableCobra() { return enableCobra; }
    public void setEnableCobra(boolean enableCobra) { this.enableCobra = enableCobra; }

    public boolean isEnableRetireeBilling() { return enableRetireeBilling; }
    public void setEnableRetireeBilling(boolean enableRetireeBilling) { this.enableRetireeBilling = enableRetireeBilling; }

    public boolean isEnableDirectBill() { return enableDirectBill; }
    public void setEnableDirectBill(boolean enableDirectBill) { this.enableDirectBill = enableDirectBill; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

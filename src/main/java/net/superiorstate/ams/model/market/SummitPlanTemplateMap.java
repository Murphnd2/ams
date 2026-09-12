package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V095 — one row of the PSP-scoped mapping from an elected {@code ServiceItem} to the Summit CDH
 * plan template that should be created for it in file 2 (Employer CDH Plan).
 * <p>
 * This is the table-backed form of the {@code SUMMIT_PLAN_TEMPLATES} property (S28-B).
 * {@link net.superiorstate.ams.data.resolver.SummitPlanTemplateResolver} reads these rows first
 * and falls back to the property when a PSP has none, so <b>an installation that takes V095 and
 * enters no rows behaves exactly as it did before</b>.
 * <p>
 * <b>Scalar foreign keys, not relations</b> — {@code pspId} and {@code serviceItemId} are plain
 * columns, following the sibling {@link EmployerParticipant} in this package rather than
 * introducing {@code @ManyToOne} graph edges. The resolver needs four scalars to build a
 * {@code PlanTemplate} and never navigates from a mapping row to a {@code PSP} or a
 * {@code ServiceItem}; a relation would add fetch behaviour with no reader.
 * <p>
 * ⚠️ {@code serviceItemId} is an {@code Integer}, not a {@code Long} — {@code ServiceItem}'s own
 * primary key is {@code int} ({@code templatepurpose.purpose_id}), and the resolver matches it
 * against the {@code Map<Integer, String>} of elected services the export builds.
 * <p>
 * ⚠️ <b>{@code label} is nullable and is not defaulted here.</b> The fallback to
 * {@code keySegment} for an absent or blank label is the resolver's rule and stays there, so both
 * the table path and the property path apply one implementation of it. Do not materialise the
 * default into this column — a stored copy would drift the first time a key segment is renamed.
 * <p>
 * ⚠️ <b>One active mapping per (PSP, ServiceItem)</b>, enforced by the named unique constraint
 * {@code uq_summit_plan_template_map_psp_service} rather than by the primary key, so the 1:1 rule
 * can be dropped by index name if fan-out is ever wanted. {@code isActive} deliberately does not
 * participate in that constraint: retiring a row does not free the pair for a second one.
 */
@Entity
@Table(name = "summit_plan_template_map")
public class SummitPlanTemplateMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "psp_id", nullable = false)
    private Long pspId;

    @Column(name = "service_item_id", nullable = false)
    private Integer serviceItemId;

    /** The Summit-assigned Plan Template ID. Per-installation; never hardcoded (build rule 4). */
    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    /**
     * The segment placed inside {@code Import Plan ID}. Must be letters and digits only — no pipe,
     * whitespace, or other punctuation — because {@code 125 PI Contributions} rejects any
     * non-alphanumeric character in that field (proven 2026-09-12).
     */
    @Column(name = "key_segment", nullable = false)
    private String keySegment;

    /** Human-facing name for {@code Plan Name} and {@code Plan Description}. Null falls back to {@link #keySegment}. */
    @Column(name = "label")
    private String label;

    /** Emit order, lowest first — the table's equivalent of the property's config order. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public SummitPlanTemplateMap() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }

    public Integer getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Integer serviceItemId) { this.serviceItemId = serviceItemId; }

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }

    public String getKeySegment() { return keySegment; }
    public void setKeySegment(String keySegment) { this.keySegment = keySegment; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

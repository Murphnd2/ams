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
 * ⚠️ <b>One mapping per (PSP, ServiceItem, seq)</b> since V103 (W3) — the unique constraint is
 * {@code uq_summit_plan_template_map_psp_service_seq}, replacing V095's 1:1
 * {@code uq_summit_plan_template_map_psp_service}, so one service item can fan out to several Summit
 * plans distinguished by {@link #seq}. ⚠️ <b>Nothing consumes the fan-out yet:</b>
 * {@code SummitPlanTemplateResolver} still takes the first active row per service item (lowest
 * {@code sortOrder}, then {@code seq}, then {@code id}) and the admin screen still refuses a second
 * row — W4 and W5 respectively. {@code isActive} deliberately does not participate in the
 * constraint: retiring a row does not free its slot for a second one.
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

    /**
     * V103 (W3) — ordinal within one {@code (pspId, serviceItemId)}, so one elected service item can
     * map to several Summit plans. <b>Not a display order</b> ({@link #sortOrder} is) and <b>not an
     * identifier</b> — it exists only so the unique key {@code (psp_id, service_item_id, seq)} can
     * hold more than one row per service item. Every pre-V103 row is {@code 0}. Named {@code seq},
     * not {@code sequence}: {@code SEQUENCE} is reserved on MariaDB and a keyword in several dialects.
     */
    @Column(name = "seq", nullable = false)
    private short seq = 0;

    /**
     * V103 (W3) — how W4 derives this plan's {@code Effective Date} from the sale's plan-year start:
     * {@code PLAN_YEAR_START} or {@code MOST_RECENT_PAST_MONTHDAY}. A string, validated by AMS when
     * W4 evaluates it; nothing evaluates it yet.
     */
    @Column(name = "effective_date_rule", nullable = false)
    private String effectiveDateRule = "PLAN_YEAR_START";

    /** V103 (W3) — signed month offset applied to the plan-year start before the rule is evaluated. */
    @Column(name = "offset_months", nullable = false)
    private int offsetMonths = 0;

    /** V103 (W3) — signed year offset for the plan year this plan is created in; {@code 0} = the sale's own. */
    @Column(name = "plan_year_offset_years", nullable = false)
    private int planYearOffsetYears = 0;

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

    /**
     * V104 — marks the one mapping row per sale that is the card-issuer placeholder plan the $1
     * seed election ({@code type=cardseed}) enrols into. Several rows can share a
     * {@code serviceItemId} (PremiumPath's five-plan fan-out, W3/W4), and nothing else on this row —
     * not {@code templateId} (per tenant), not {@code effectiveDateRule} (a date rule, not a
     * meaning), not {@code label} or {@code keySegment} (both admin-editable free text) — carries
     * that meaning. Exactly one active flagged row among a sale's elected service items is expected;
     * enforced by the {@code cardseed} writer at export time, not by a database constraint, the same
     * way {@code LEGACY_ICHRA_SEGMENT} enforces the ICHRA plan's uniqueness in
     * {@code writeHraEnrollment}.
     */
    @Column(name = "is_card_issuer", nullable = false)
    private boolean cardIssuer = false;

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

    public short getSeq() { return seq; }
    public void setSeq(short seq) { this.seq = seq; }

    public String getEffectiveDateRule() { return effectiveDateRule; }
    public void setEffectiveDateRule(String effectiveDateRule) { this.effectiveDateRule = effectiveDateRule; }

    public int getOffsetMonths() { return offsetMonths; }
    public void setOffsetMonths(int offsetMonths) { this.offsetMonths = offsetMonths; }

    public int getPlanYearOffsetYears() { return planYearOffsetYears; }
    public void setPlanYearOffsetYears(int planYearOffsetYears) { this.planYearOffsetYears = planYearOffsetYears; }

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

    public boolean isCardIssuer() { return cardIssuer; }
    public void setCardIssuer(boolean cardIssuer) { this.cardIssuer = cardIssuer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

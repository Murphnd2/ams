package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * V109 — the AMS-side mirror of DataPath's "Tier Structure 3", the standard four-tier set SSA
 * has adopted for all HRA-type benefit plans (TA-14).
 * <p>
 * Summit stamps these four rows into the benefit plan's Coverage Levels/Tiers grid from a
 * dropdown, so the Summit-side {@code summitTierId} strings are never typed by hand there.
 * Today the enrollment matrix renders the Summit Tier ID as a free-text input with nothing
 * validating it, and the value lands verbatim in column E of the HRA Enrollment import file
 * — under Contribution Schedule funding, the tier determines the dollar amount, so a wrong
 * tier is wrong money with no error. This table is the curated set a future picker will read.
 * <p>
 * <b>No code reads this table yet</b> — the enrollment matrix tier picker is a separate,
 * later task. This entity exists for that later build to read from.
 * <p>
 * <b>{@code summitTierId}</b> must match the Tier ID configured on the Summit benefit plan
 * exactly, character for character (case-sensitive, contains a forward slash on three of the
 * four rows). Nothing in this table or its DAO validates that match.
 */
@Entity
@Table(name = "coverage_tier")
public class CoverageTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "summit_tier_id", nullable = false)
    private String summitTierId;

    @Column(name = "census_code")
    private String censusCode;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public CoverageTier() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSummitTierId() { return summitTierId; }
    public void setSummitTierId(String summitTierId) { this.summitTierId = summitTierId; }

    public String getCensusCode() { return censusCode; }
    public void setCensusCode(String censusCode) { this.censusCode = censusCode; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

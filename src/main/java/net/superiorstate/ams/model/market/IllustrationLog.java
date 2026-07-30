package net.superiorstate.ams.model.market;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One row per A1 rating-illustration run. Holds no PII — result_summary is a short
 * computed descriptor (e.g. "range 421.57-1368.03, 65 plans, 3 carriers"), never a
 * name or employer identifier.
 * <p>
 * agentPersonId references assignee(id), not a "person" table — Person is
 * SINGLE_TABLE inheritance rooted at Assignee (see V075 header comment).
 */
@Entity
@Table(name = "illustration_log")
public class IllustrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "agent_person_id")
    private Long agentPersonId;

    @Column(name = "agency_id")
    private Long agencyId;

    @Column(name = "parent_agency_id")
    private Long parentAgencyId;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "county_fips")
    private String countyFips;

    @Column(name = "state")
    private String state;

    @Column(name = "plan_year")
    private Integer planYear;

    @Column(name = "eligible_headcount")
    private Integer eligibleHeadcount;

    @Column(name = "mode")
    private String mode;

    @Column(name = "cache_hit")
    private Boolean cacheHit;

    @Column(name = "result_summary")
    private String resultSummary;

    public IllustrationLog() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getAgentPersonId() {
        return agentPersonId;
    }

    public void setAgentPersonId(Long agentPersonId) {
        this.agentPersonId = agentPersonId;
    }

    public Long getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(Long agencyId) {
        this.agencyId = agencyId;
    }

    public Long getParentAgencyId() {
        return parentAgencyId;
    }

    public void setParentAgencyId(Long parentAgencyId) {
        this.parentAgencyId = parentAgencyId;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountyFips() {
        return countyFips;
    }

    public void setCountyFips(String countyFips) {
        this.countyFips = countyFips;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getPlanYear() {
        return planYear;
    }

    public void setPlanYear(Integer planYear) {
        this.planYear = planYear;
    }

    public Integer getEligibleHeadcount() {
        return eligibleHeadcount;
    }

    public void setEligibleHeadcount(Integer eligibleHeadcount) {
        this.eligibleHeadcount = eligibleHeadcount;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }
}

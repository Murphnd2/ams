package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;

import java.time.LocalDateTime;

/**
 * T125 — the ZIP + county + headcount an agent enters about a prospect employer when a
 * plus-tier LOS ({@code los.is_plus_tier}, V086) is selected in the Proposal Builder.
 * One row per proposal ({@code proposal_id} is {@code UNIQUE}), or none. Mirrors the
 * shape of the sibling {@link ProposalIchraSnapshot} (V079) but is deliberately a
 * SEPARATE table — this one records INPUT and must persist for a county with no warmed
 * rates at all, so unlike the snapshot it carries no {@code source_env} and no
 * PRODUCTION-only invariant to protect. A proposal may carry either, both, or neither;
 * they are written by independent best-effort paths.
 * <p>
 * Nothing reads this table yet — T126, deliberately out of scope for T125.
 * <p>
 * {@code zip} and {@code countyFips} are the county the agent CHOSE, never auto-picked —
 * see {@code ZipCountyResolver}'s multi-county-is-the-common-case javadoc. {@code planYear}
 * is DERIVED server-side from the {@code RATE_CACHE_PLAN_YEARS} constant, the same source
 * {@code IllustrationServlet.resolvePlanYear} reads, and is never agent-asserted (S10-B
 * HS-1) — it records what year the ZIP/county were interpreted against, not a value
 * collected from the agent. See {@code ProposalBuilder.resolveCurrentPlanYear}.
 */
@Entity
@Table(name = "proposal_ichra_intake")
public class ProposalIchraIntake {

    @Id
    @GeneratedValue
    @Column(name = "intake_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "proposal_id", nullable = false, unique = true)
    private Proposal proposal;

    @Column(name = "zip", columnDefinition = "char(5)", nullable = false)
    private String zip;

    @Column(name = "county_fips", columnDefinition = "char(5)", nullable = false)
    private String countyFips;

    @Column(name = "county_name", columnDefinition = "varchar(100)", nullable = false)
    private String countyName;

    @Column(name = "state", columnDefinition = "char(2)", nullable = false)
    private String state;

    @Column(name = "headcount", nullable = false)
    private Integer headcount;

    @Column(name = "plan_year", nullable = false)
    private Integer planYear;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Person createdBy;

    public ProposalIchraIntake() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCountyFips() {
        return countyFips;
    }

    public void setCountyFips(String countyFips) {
        this.countyFips = countyFips;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getHeadcount() {
        return headcount;
    }

    public void setHeadcount(Integer headcount) {
        this.headcount = headcount;
    }

    public Integer getPlanYear() {
        return planYear;
    }

    public void setPlanYear(Integer planYear) {
        this.planYear = planYear;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public Person getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Person createdBy) {
        this.createdBy = createdBy;
    }
}

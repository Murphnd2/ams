package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;

import java.math.BigDecimal;
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
 * Read by {@code ViewProposal} ({@code resolveMarketPage}, the section-scope filter) since
 * S11-H/S20-B — the "nothing reads this table yet" note from T125 no longer holds.
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

    /**
     * T80 half 1 (V088) — the employer's own stated monthly-per-employee contribution.
     * Optional, unlike every other field on this entity: null means the agent did not
     * enter one, and that must render as an omitted cost block, never an error.
     */
    @Column(name = "monthly_contribution_per_employee", columnDefinition = "decimal(10,2)", nullable = true)
    private BigDecimal monthlyContributionPerEmployee;

    /**
     * V093 — the employer's flat monthly taxable stipend per employee, agent-entered.
     * Unconditional taxable wages, not an ICHRA allowance and not conditioned on any
     * coverage election. Optional, same convention as {@link #monthlyContributionPerEmployee}:
     * null means the agent did not enter one. Not a contribution — must not be summed
     * with {@link #monthlyContributionPerEmployee} anywhere.
     */
    @Column(name = "monthly_stipend_per_employee", columnDefinition = "decimal(10,2)", nullable = true)
    private BigDecimal monthlyStipendPerEmployee;

    /**
     * V093 — agent-entered monthly premium for the alternative coverage an employee
     * could buy instead of an ACA plan. Carries no carrier or product identity — a
     * number the agent types, nothing more. Optional, same convention as every other
     * decimal field on this entity.
     */
    @Column(name = "alternative_coverage_monthly_cost", columnDefinition = "decimal(10,2)", nullable = true)
    private BigDecimal alternativeCoverageMonthlyCost;

    /**
     * S20-B / V091 — the four ICHRA proposal-section selections
     * (docs/analysis/S20A_ichra_sections_spec.md §2/§3). Stored here, not only in
     * {@code payload_json}, because a selection recorded only in the payload is lost
     * entirely for a county with no warmed production rates — the snapshot (and therefore
     * the payload) is never written for that case, but intake always is.
     * <p>
     * {@code sectionMarket} is stored as both submitted AND derived: {@code ProposalBuilder}
     * sets it true whenever any of the other three is true, since market illustration data
     * is the base layer every other section needs, not a peer selection.
     */
    @Column(name = "section_market", nullable = false)
    private boolean sectionMarket;

    @Column(name = "section_contribution", nullable = false)
    private boolean sectionContribution;

    @Column(name = "section_comparison", nullable = false)
    private boolean sectionComparison;

    /** Always false today — build 4 (ICHRA_AFFORDABILITY) is blocked pending an LA-NN entry. */
    @Column(name = "section_affordability", nullable = false)
    private boolean sectionAffordability;

    /**
     * S20-B / V091 — the employer's CURRENT group plan cost, as the agent reports it.
     * Section 3 (ICHRA_COMPARISON) input. Employer-reported, not computed or market-derived,
     * same as {@link #monthlyContributionPerEmployee}. Null until the agent selects section 3.
     */
    @Column(name = "current_total_monthly_premium", columnDefinition = "decimal(10,2)", nullable = true)
    private BigDecimal currentTotalMonthlyPremium;

    /** S20-B / V091 — section 3 input, paired with {@link #currentTotalMonthlyPremium}. */
    @Column(name = "current_employer_monthly_share", columnDefinition = "decimal(10,2)", nullable = true)
    private BigDecimal currentEmployerMonthlyShare;

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

    public BigDecimal getMonthlyContributionPerEmployee() {
        return monthlyContributionPerEmployee;
    }

    public void setMonthlyContributionPerEmployee(BigDecimal monthlyContributionPerEmployee) {
        this.monthlyContributionPerEmployee = monthlyContributionPerEmployee;
    }

    public BigDecimal getMonthlyStipendPerEmployee() {
        return monthlyStipendPerEmployee;
    }

    public void setMonthlyStipendPerEmployee(BigDecimal monthlyStipendPerEmployee) {
        this.monthlyStipendPerEmployee = monthlyStipendPerEmployee;
    }

    public BigDecimal getAlternativeCoverageMonthlyCost() {
        return alternativeCoverageMonthlyCost;
    }

    public void setAlternativeCoverageMonthlyCost(BigDecimal alternativeCoverageMonthlyCost) {
        this.alternativeCoverageMonthlyCost = alternativeCoverageMonthlyCost;
    }

    public boolean isSectionMarket() {
        return sectionMarket;
    }

    public void setSectionMarket(boolean sectionMarket) {
        this.sectionMarket = sectionMarket;
    }

    public boolean isSectionContribution() {
        return sectionContribution;
    }

    public void setSectionContribution(boolean sectionContribution) {
        this.sectionContribution = sectionContribution;
    }

    public boolean isSectionComparison() {
        return sectionComparison;
    }

    public void setSectionComparison(boolean sectionComparison) {
        this.sectionComparison = sectionComparison;
    }

    public boolean isSectionAffordability() {
        return sectionAffordability;
    }

    public void setSectionAffordability(boolean sectionAffordability) {
        this.sectionAffordability = sectionAffordability;
    }

    public BigDecimal getCurrentTotalMonthlyPremium() {
        return currentTotalMonthlyPremium;
    }

    public void setCurrentTotalMonthlyPremium(BigDecimal currentTotalMonthlyPremium) {
        this.currentTotalMonthlyPremium = currentTotalMonthlyPremium;
    }

    public BigDecimal getCurrentEmployerMonthlyShare() {
        return currentEmployerMonthlyShare;
    }

    public void setCurrentEmployerMonthlyShare(BigDecimal currentEmployerMonthlyShare) {
        this.currentEmployerMonthlyShare = currentEmployerMonthlyShare;
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

package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable-by-convention record of an ICHRA illustration attached to a proposal
 * (build-plan item 6, V079). One row per proposal ({@code proposal_id} is
 * {@code UNIQUE}) — not a scenario system; N scenarios later costs dropping that
 * uniqueness and adding an {@code is_selected} flag, not built here.
 * <p>
 * {@link #SECTION_TYPE} is the one shared constant for the {@code ProposalSection}
 * type literal this snapshot renders under — referenced by the writer
 * ({@code ProposalBuilder}) and the admin creation action ({@code ProposalSettings}).
 * The JSP dispatch in {@code viewProposal.jsp} and the scope-card gate in
 * {@code proposalSettings.jsp} cannot reach a Java static field through plain EL (no
 * existing JSP in this codebase does), so both carry the literal {@code "ICHRA_ILLUSTRATION"}
 * directly — the same duplication every other section type (TITLE, CUSTOM, ...)
 * already has between Java and JSP.
 * <p>
 * Carries INPUTS (county, plan year, mode parameters) and OUTPUTS (computed group
 * premiums/net totals) together with provenance ({@code sourceEnv},
 * {@code ratesFetchedAt}), so the document is stable after the cache moves on.
 * <p>
 * {@code /proposal/*} is public and unauthenticated (LA-12), a materially weaker audience
 * guarantee than the illustration page behind {@code IchraAccessResolver}. That guarantee
 * still holds for the public read path: {@link #payloadJson} (T165, V090) does carry an
 * affordability figure in storage, but {@code ViewProposal}'s token set deliberately defines
 * no token that exposes it — the field is written so it exists the moment a compliance
 * decision permits display, not because the public page renders it. See
 * {@code docs/analysis/S19D_ichra_payload_spec.md} §4/§6/§9.
 */
@Entity
@Table(name = "proposal_ichra_snapshot")
public class ProposalIchraSnapshot {

    /** The ProposalSection.sectionType literal this snapshot renders under. */
    public static final String SECTION_TYPE = "ICHRA_ILLUSTRATION";

    public static final String MODE_RANGE = "RANGE";
    public static final String MODE_AGE_BAND = "AGE_BAND";

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Id
    @GeneratedValue
    @Column(name = "snapshot_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "proposal_id", nullable = false, unique = true)
    private Proposal proposal;

    @Column(name = "mode", columnDefinition = "varchar(16)", nullable = false)
    private String mode;

    @Column(name = "county_fips", columnDefinition = "char(5)", nullable = false)
    private String countyFips;

    @Column(name = "state", columnDefinition = "char(2)", nullable = false)
    private String state;

    @Column(name = "county_name", columnDefinition = "varchar(100)", nullable = false)
    private String countyName;

    @Column(name = "plan_year", nullable = false)
    private Integer planYear;

    @Column(name = "contribution")
    private BigDecimal contribution;

    @Column(name = "headcount")
    private Integer headcount;

    @Column(name = "group_monthly_low")
    private BigDecimal groupMonthlyLow;

    @Column(name = "group_monthly_high")
    private BigDecimal groupMonthlyHigh;

    @Column(name = "group_net_total")
    private BigDecimal groupNetTotal;

    @Column(name = "employer_outlay")
    private BigDecimal employerOutlay;

    @Column(name = "source_env", columnDefinition = "varchar(16)", nullable = false)
    private String sourceEnv;

    @Column(name = "rates_fetched_at")
    private LocalDateTime ratesFetchedAt;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Person createdBy;

    /** T165/V090 — schema-versioned JSON payload; see docs/analysis/S19D_ichra_payload_spec.md §3. */
    @Column(name = "payload_json", columnDefinition = "MEDIUMTEXT")
    private String payloadJson;

    public ProposalIchraSnapshot() {}

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public Integer getPlanYear() {
        return planYear;
    }

    public void setPlanYear(Integer planYear) {
        this.planYear = planYear;
    }

    public BigDecimal getContribution() {
        return contribution;
    }

    public void setContribution(BigDecimal contribution) {
        this.contribution = contribution;
    }

    public Integer getHeadcount() {
        return headcount;
    }

    public void setHeadcount(Integer headcount) {
        this.headcount = headcount;
    }

    public BigDecimal getGroupMonthlyLow() {
        return groupMonthlyLow;
    }

    public void setGroupMonthlyLow(BigDecimal groupMonthlyLow) {
        this.groupMonthlyLow = groupMonthlyLow;
    }

    public BigDecimal getGroupMonthlyHigh() {
        return groupMonthlyHigh;
    }

    public void setGroupMonthlyHigh(BigDecimal groupMonthlyHigh) {
        this.groupMonthlyHigh = groupMonthlyHigh;
    }

    public BigDecimal getGroupNetTotal() {
        return groupNetTotal;
    }

    public void setGroupNetTotal(BigDecimal groupNetTotal) {
        this.groupNetTotal = groupNetTotal;
    }

    public BigDecimal getEmployerOutlay() {
        return employerOutlay;
    }

    public void setEmployerOutlay(BigDecimal employerOutlay) {
        this.employerOutlay = employerOutlay;
    }

    public String getSourceEnv() {
        return sourceEnv;
    }

    public void setSourceEnv(String sourceEnv) {
        this.sourceEnv = sourceEnv;
    }

    public LocalDateTime getRatesFetchedAt() {
        return ratesFetchedAt;
    }

    public void setRatesFetchedAt(LocalDateTime ratesFetchedAt) {
        this.ratesFetchedAt = ratesFetchedAt;
    }

    /** Display string for JSP rendering — never format a java.time value in a JSP taglib. */
    public String getRatesFetchedAtDisplay() {
        return ratesFetchedAt == null ? null : ratesFetchedAt.format(DISPLAY_FORMAT);
    }

    public LocalDateTime getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(LocalDateTime snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public Person getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Person createdBy) {
        this.createdBy = createdBy;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}

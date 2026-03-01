package net.superiorstate.ams.model.summit.archive;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Entity
public class Benefit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="benefit_id")
    private int id;

    @Column(name="summit_id", nullable = false)
    private int summitId;

    @Column(name="source_type", nullable = false)
    private String sourceType = "CDH";

    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @ManyToOne
    @JoinColumn(name="plan_type_id")
    private PlanType planType;

    @Column(name="plan_name")
    private String planName;

    @Column(name="plan_description")
    private String planDescription;

    @Column(name="effective_date")
    private Date effectiveDate;

    @Column(name="termination_date")
    private Date terminationDate;

    @Column(name="active")
    private boolean isActive;

    @Column(name="hasCards")
    private boolean hasCards;

    @Column(name="last_renewed")
    private Date lastRenewed;

    @Column(name="next_renewal_due")
    private Date nextRenewalDue;

    @Column(name="renewal_months")
    private int renewalMonths = 12;

    @Column(name="plan_year_start")
    private Date planYearStart;

    @Column(name="plan_year_end")
    private Date planYearEnd;

    @Column(name="benid_pb")
    private int pbBenId;

    @OneToMany(mappedBy = "benefit")
    private List<RenewalItem> renewalItemList;

    public Benefit(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSummitId() {
        return summitId;
    }

    public void setSummitId(int summitId) {
        this.summitId = summitId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanDescription() {
        return planDescription;
    }

    public void setPlanDescription(String planDescription) {
        this.planDescription = planDescription;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(Date terminationDate) {
        this.terminationDate = terminationDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isHasCards() {
        return hasCards;
    }

    public void setHasCards(boolean hasCards) {
        this.hasCards = hasCards;
    }

    public Date getLastRenewed() {
        return lastRenewed;
    }

    public void setLastRenewed(Date lastRenewed) {
        this.lastRenewed = lastRenewed;
    }

    public Date getNextRenewalDue() {
        return nextRenewalDue;
    }

    public void setNextRenewalDue(Date nextRenewalDue) {
        this.nextRenewalDue = nextRenewalDue;
    }

    public int getRenewalMonths() {
        return renewalMonths;
    }

    public void setRenewalMonths(int renewalMonths) {
        this.renewalMonths = renewalMonths;
    }

    public List<RenewalItem> getRenewalItemList() {
        return renewalItemList;
    }

    public void setRenewalItemList(List<RenewalItem> renewalItemList) {
        this.renewalItemList = renewalItemList;
    }

    public Date getPlanYearStart() {
        return planYearStart;
    }

    public void setPlanYearStart(Date planYearStart) {
        this.planYearStart = planYearStart;
    }

    public Date getPlanYearEnd() {
        return planYearEnd;
    }

    public void setPlanYearEnd(Date planYearEnd) {
        this.planYearEnd = planYearEnd;
    }

    public LocalDate getDetectedRenewalDate() {
        if (planYearEnd == null) return null;
        return planYearEnd.toLocalDate().plusDays(1);
    }

    private Date rightNow(){
        return Date.valueOf(LocalDate.ofInstant(Instant.now(),ZoneId.systemDefault()));
    }
    private Date daysOut(Date dateStart, Long daysOut){
        return Date.valueOf(dateStart.toLocalDate().minusDays(daysOut));
    }

    public int getPbBenId() {
        return pbBenId;
    }

    public void setPbBenId(int pbBenId) {
        this.pbBenId = pbBenId;
    }

    public boolean listBenefitForRenewal(){
        return rightNow().after(daysOut(getNextRenewalDue(),90L));
    }

    public boolean flagBenefitForRenewal(){
        return rightNow().after(daysOut(getNextRenewalDue(),30L));
    }
}

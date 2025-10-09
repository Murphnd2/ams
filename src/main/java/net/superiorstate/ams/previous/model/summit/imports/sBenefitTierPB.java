package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.summit.archive.PlanType;

import java.sql.Date;
import java.time.LocalDate;

@Entity
public class
sBenefitTierPB {

    @Column(name="TPA")
    private String tpa;
    @ManyToOne
    @JoinColumn(name="OrganizationID")
    private sEmployer sEmployer;

    @Id
    @Column(name="EmployerID")
    private int employerId;

    @Column(name="Employer")
    private String employer;
    @Column(name="BenefitName")
    private String planDescription;

    @Id
    @Column(name="PBBenefitID")
    private int pbBenefitId;

    @Column(name="Type")
    private String type;

    @Column(name="RemitTo")
    private String remitTo;

    @ManyToOne
    @JoinColumn(name="PlanTypeID")
    private PlanType planType;

    @Column(name="PBType")
    private String pbType;

    @Column(name="PBTypeID")
    private String pbTypeId;
    @Column(name="BenefitID")
    private int benefitId;
    @Column(name="ImportPlanID")
    private String planName;
    @Column(name="EffectiveDate")
    private String effectiveDate;

    @Column(name="Carrier")
    private String carrier;
    @Column(name="LastDayofCoverage")
    private String lastDayOfCoverage;

    @Column(name="Fee")
    private String fee;

    @Id
    @Column(name="PlanYearID")
    private int planYearId;
    @Column(name="StartDate")
    private String startDate;

    @Column(name="EndDate")
    private String endDate;

    @Column(name="TierName")
    private String tierName;

    @Id
    @Column(name="TierID")
    private String tierId;

    @Column(name="TierAge")
    private String tierAge;

    @Column(name="Gender")
    private String gender;

    @Column(name="Smoker")
    private String smoker;

    @Column(name="Amount")
    private String amount;

    public sBenefitTierPB(){}

    public int getEmployerId() {
        return employerId;
    }

    public int getPbBenefitId() {
        return pbBenefitId;
    }

    public int getPlanYearId() {
        return planYearId;
    }

    public String getTierId() {
        return tierId;
    }

    public sEmployer getsEmployer() {
        return sEmployer;
    }

    public PlanType getsPlanType() {
        return planType;
    }

    public int getBenefitId() {
        return benefitId;
    }

    public String getPlanDescription() {
        return planDescription;
    }

    public String getPlanName() {
        return planName;
    }

    private String getEffectiveDate() {
        return effectiveDate;
    }

    public String getLastDayOfCoverage() {
        return lastDayOfCoverage;
    }

    public String getTierName() {
        return tierName;
    }

    private String getStartDate() {
        return startDate;
    }

    private String getEndDate() {
        return endDate;
    }

    public Date getPlanStartDate(){
        return Date.valueOf(LocalDate.of(findYear(getStartDate()),findMonth(getStartDate()),findDay(getStartDate())));
    }

    public Date getPlanEndDate(){
        return Date.valueOf(LocalDate.of(findYear(getEndDate()),findMonth(getEndDate()),findDay(getEndDate())));
    }

    public Date getPlanEffectiveDate(){
        return Date.valueOf(LocalDate.of(findYear(getEffectiveDate()),findMonth(getEffectiveDate()),findDay(getEffectiveDate())));
    }

    private int findMonth(String dateString){
        int firstSlash = dateString.indexOf("/");
        return Integer.parseInt(dateString.substring(0,firstSlash));
    }

    private int findDay(String dateString){
        int firstSlash = dateString.indexOf("/");
        int secondSlash = dateString.indexOf("/",firstSlash+1);
        return Integer.parseInt(dateString.substring(firstSlash+1,secondSlash));
    }

    private int findYear(String dateString){
        int firstSlash = dateString.indexOf("/");
        int secondSlash = dateString.indexOf("/",firstSlash+1);
        return Integer.parseInt(dateString.substring(secondSlash+1,secondSlash+5));
    }
}

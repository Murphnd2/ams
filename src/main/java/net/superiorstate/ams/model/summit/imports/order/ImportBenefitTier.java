package net.superiorstate.ams.model.summit.imports.order;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.sql.Date;
import java.time.LocalDate;

@Entity
public class ImportBenefitTier {
    @Column(name="TPA")
    private String tpa;

    @ManyToOne
    @JoinColumn(name="OrganizationID")
    private ImportEmployer importEmployer;

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

    public ImportBenefitTier(){}

    public String getTpa() {
        return tpa;
    }

    public void setTpa(String tpa) {
        this.tpa = tpa;
    }

    public ImportEmployer getImportEmployer() {
        return importEmployer;
    }

    public void setImportEmployer(ImportEmployer importEmployer) {
        this.importEmployer = importEmployer;
    }

    public int getEmployerId() {
        return employerId;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getPlanDescription() {
        return planDescription;
    }

    public void setPlanDescription(String planDescription) {
        this.planDescription = planDescription;
    }

    public int getPbBenefitId() {
        return pbBenefitId;
    }

    public void setPbBenefitId(int pbBenefitId) {
        this.pbBenefitId = pbBenefitId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRemitTo() {
        return remitTo;
    }

    public void setRemitTo(String remitTo) {
        this.remitTo = remitTo;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public String getPbType() {
        return pbType;
    }

    public void setPbType(String pbType) {
        this.pbType = pbType;
    }

    public String getPbTypeId() {
        return pbTypeId;
    }

    public void setPbTypeId(String pbTypeId) {
        this.pbTypeId = pbTypeId;
    }

    public int getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(int benefitId) {
        this.benefitId = benefitId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getLastDayOfCoverage() {
        return lastDayOfCoverage;
    }

    public void setLastDayOfCoverage(String lastDayOfCoverage) {
        this.lastDayOfCoverage = lastDayOfCoverage;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public int getPlanYearId() {
        return planYearId;
    }

    public void setPlanYearId(int planYearId) {
        this.planYearId = planYearId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public String getTierId() {
        return tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public String getTierAge() {
        return tierAge;
    }

    public void setTierAge(String tierAge) {
        this.tierAge = tierAge;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSmoker() {
        return smoker;
    }

    public void setSmoker(String smoker) {
        this.smoker = smoker;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
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

package net.superiorstate.ams.previous.model.summit.imports.order;

import jakarta.persistence.*;
import net.superiorstate.ams.data.Helper;

import java.sql.Date;
import java.time.LocalDate;

@Entity
@Table(name="import4benefitcdh")
public class ImportBenefitCdh {
    @Column(name="Employer_ID")
    private String employerId;

    @Column(name="EmployerName")
    private String employerName;

    @ManyToOne
    @JoinColumn(name="OrganizationID")
    private ImportEmployer importEmployer;

    @Id
    @Column(name="EmployerPlan_ID")
    private int benefitId;

    @ManyToOne
    @JoinColumn(name="PlanTypeID")
    private net.superiorstate.ams.previous.model.summit.archive.PlanType PlanType;

    @Column(name="PlanName")
    private String planName;

    @Column(name="PlanDescription")
    private String planDescription;

    @Column(name="ImportPlanID")
    private String importPlanId;

    @Column(name="PlanType")
    private String planType;

    @Column(name="EffectiveDate")
    private String effectiveDate;

    @Column(name="TerminationDate")
    private String terminationDate;

    @Column(name="PlanStatus")
    private String planStatus;

    @Column(name="LinkedtoDefaultPlan")
    private String linkedToDefaultPlan;

    @Column(name="CardEnabled")
    private String cardEnabled;

    public ImportBenefitCdh(){}

    public String getEmployerId() {
        return employerId;
    }

    public void setEmployerId(String employerId) {
        this.employerId = employerId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public ImportEmployer getImportEmployer() {
        return importEmployer;
    }

    public void setImportEmployer(ImportEmployer importEmployer) {
        this.importEmployer = importEmployer;
    }

    public int getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(int benefitId) {
        this.benefitId = benefitId;
    }

    public net.superiorstate.ams.previous.model.summit.archive.PlanType getPlanType() {
        return PlanType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(String terminationDate) {
        this.terminationDate = terminationDate;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }

    public String getLinkedToDefaultPlan() {
        return linkedToDefaultPlan;
    }

    public void setLinkedToDefaultPlan(String linkedToDefaultPlan) {
        this.linkedToDefaultPlan = linkedToDefaultPlan;
    }

    public String getCardEnabled() {
        return cardEnabled;
    }

    public void setCardEnabled(String cardEnabled) {
        this.cardEnabled = cardEnabled;
    }

    public void setPlanType(net.superiorstate.ams.previous.model.summit.archive.PlanType planType) {
        PlanType = planType;
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

    public String getImportPlanId() {
        return importPlanId;
    }

    public void setImportPlanId(String importPlanId) {
        this.importPlanId = importPlanId;
    }
    public boolean getCardEnabledBoolean(){
        boolean it = getCardEnabled().trim().equals("True");
        return it;
    }

    public Date getPlanTerminationDate(){
        Date theDate = null;
        try{
            theDate = Date.valueOf(LocalDate.of(findYear(getTerminationDate()),findMonth(getTerminationDate()),findDay(getTerminationDate())));
        } catch (Exception e){
            e.printStackTrace();
        }
        return theDate;
    }

    public Date getPlanEffectiveDate(){
        Date theDate = null;
        try{
            theDate = Date.valueOf(LocalDate.of(findYear(getEffectiveDate()),findMonth(getEffectiveDate()),findDay(getEffectiveDate())));
        } catch (Exception e){
            e.printStackTrace();
        }
        return theDate;
    }

    private int findMonth(String dateString){
        return Helper.parseDateParts(dateString) [0];
    }

    private int findDay(String dateString){
        return Helper.parseDateParts(dateString) [1];
    }

    private int findYear(String dateString){
        return Helper.parseDateParts(dateString) [2];
    }
}

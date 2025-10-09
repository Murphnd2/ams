package net.superiorstate.ams.previous.model.summit.imports.order;

import jakarta.persistence.*;

@Entity
@Table(name="importacoverage")
public class ImportCoverage {
    @Column(name="SSN")
    private String ssn;

    @Column(name="EmployerName")
    private String employerName;

    @Column(name="EmployerCustomID")
    private String employerCustomId;

    @Column(name="TransactionStartDate")
    private String transactionStartDate;

    @Column(name="TransactionEndDate")
    private String transactionEndDate;

    @Column(name="ParticipantLastName")
    private String participantLastName;

    @Column(name="ParticipantFirstName")
    private String participantFirstName;

    @Column(name="ParticipantCustomID")
    private String participantCustomId;

    @ManyToOne
    @JoinColumn(name="Participant_ID")
    private ImportEmployee importEmployee;

    @Column(name="ImportPlanID")
    private String importPlanId;

    @Column(name="BenefitID")
    private int benefitId;

    @Id
    @Column(name="PBCoverageHeaderID")
    private int coverageId;

    @Column(name="CoverageStatus")
    private String coverageStatus;

    @Column(name="PlanType")
    private String planType;

    @Column(name="BenefitName")
    private String benefitName;

    @Column(name="TierName")
    private String tierName;

    @Column(name="EmployerID")
    private int employerId;

    @Column(name="EmployerDivisionID")
    private String employerDivisionId;

    @Column(name="EmployerDivision")
    private String employerDivision;

    @Column(name="EffectiveDate")
    private String effectiveDate;

    public ImportCoverage(){}

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getEmployerCustomId() {
        return employerCustomId;
    }

    public void setEmployerCustomId(String employerCustomId) {
        this.employerCustomId = employerCustomId;
    }

    public String getTransactionStartDate() {
        return transactionStartDate;
    }

    public void setTransactionStartDate(String transactionStartDate) {
        this.transactionStartDate = transactionStartDate;
    }

    public String getTransactionEndDate() {
        return transactionEndDate;
    }

    public void setTransactionEndDate(String transactionEndDate) {
        this.transactionEndDate = transactionEndDate;
    }

    public String getParticipantLastName() {
        return participantLastName;
    }

    public void setParticipantLastName(String participantLastName) {
        this.participantLastName = participantLastName;
    }

    public String getParticipantFirstName() {
        return participantFirstName;
    }

    public void setParticipantFirstName(String participantFirstName) {
        this.participantFirstName = participantFirstName;
    }

    public String getParticipantCustomId() {
        return participantCustomId;
    }

    public void setParticipantCustomId(String participantCustomId) {
        this.participantCustomId = participantCustomId;
    }

    public ImportEmployee getImportEmployee() {
        return importEmployee;
    }

    public void setImportEmployee(ImportEmployee importEmployee) {
        this.importEmployee = importEmployee;
    }

    public String getImportPlanId() {
        return importPlanId;
    }

    public void setImportPlanId(String importPlanId) {
        this.importPlanId = importPlanId;
    }

    public int getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(int benefitId) {
        this.benefitId = benefitId;
    }

    public int getCoverageId() {
        return coverageId;
    }

    public void setCoverageId(int coverageId) {
        this.coverageId = coverageId;
    }

    public String getCoverageStatus() {
        return coverageStatus;
    }

    public void setCoverageStatus(String coverageStatus) {
        this.coverageStatus = coverageStatus;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public int getEmployerId() {
        return employerId;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public String getEmployerDivisionId() {
        return employerDivisionId;
    }

    public void setEmployerDivisionId(String employerDivisionId) {
        this.employerDivisionId = employerDivisionId;
    }

    public String getEmployerDivision() {
        return employerDivision;
    }

    public void setEmployerDivision(String employerDivision) {
        this.employerDivision = employerDivision;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}

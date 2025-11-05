package net.superiorstate.ams.previous.model.summit.imports.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="importBcobraterm")
public class ImportCobTerm {
    @Column(name="TransactionStartDate")
    private String transactionStartDate;
    @Column(name="TransactionEndDate")
    private String transactionEndDate;
    @Id
    @Column(name="ParticipantName")
    private String participantName;
    @Column(name="ID")
    private String id;
    @Column(name="SSN")
    private String ssn;
    @Column(name="EmployerName")
    private String employerName;
    @Column(name="BenefitName")
    private String benefitName;
    @Column(name="Tier")
    private String tier;
    @Column(name="TerminationReason")
    private String terminationReason;
    @Column(name="TermedDate")
    private String termedDate;
    @Column(name="TermedOn")
    private String termedOn;
    @Column(name="PBBillingFrequency")
    private String pbBillingFrequency;
    @Column(name="EmployerOrganizationID")
    private int organizationId;
    public ImportCobTerm(){}

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

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public String getTermedDate() {
        return termedDate;
    }

    public void setTermedDate(String termedDate) {
        this.termedDate = termedDate;
    }

    public String getTermedOn() {
        return termedOn;
    }

    public void setTermedOn(String termedOn) {
        this.termedOn = termedOn;
    }

    public String getPbBillingFrequency() {
        return pbBillingFrequency;
    }

    public void setPbBillingFrequency(String pbBillingFrequency) {
        this.pbBillingFrequency = pbBillingFrequency;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }
}

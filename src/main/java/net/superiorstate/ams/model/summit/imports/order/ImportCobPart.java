package net.superiorstate.ams.model.summit.imports.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="import9cobrapart")
public class ImportCobPart {
    @Column(name="User_ID")
    private String userId;

    @Id
    @Column(name="Participant_id")
    private int participantId;
    @Column(name="AcceptedDate")
    private String acceptedDate;
    @Column(name="DisplayPremium")
    private String displayPremium;
    @Column(name="EmployerName")
    private String employerName;
    @Column(name="ExpirationDate")
    private String expirationDate;
    @Column(name="PaidThroughDate")
    private String paidThroughDate;
    @Column(name="ParticipantName")
    private String participantName;
    @Column(name="CoveredMemberName")
    private String coveredMemberName;
    @Column(name="PBTypeName")
    private String pbTypeName;
    @Column(name="QualifyingEvent")
    private String qualifyingEvent;
    @Column(name="QualifyingEventDate")
    private String qualifyingEventDate;
    @Column(name="StartDate")
    private String startDate;
    @Column(name="TermedDate")
    private String termedDate;
    @Column(name="BenefitName")
    private String benefitName;
    @Column(name="TransactionStartDate")
    private String transactionStartDate;
    @Column(name="TransactionEndDate")
    private String transactionEndDate;
    @Column(name="PBCoverageHeaderId")
    private String pbCoverageHeaderId;
    @Column(name="Tier")
    private String tier;
    @Column(name="CoveredMembersNames")
    private String coveredMembersNames;
    @Column(name="IncludeDependentsCoveredUnderEachPlan")
    private String includeDependentCoveredUnderEachPlan;
    @Column(name="IsDependent")
    private String isDependent;
    @Column(name="SSN")
    private String ssn;
    @Column(name="DateOfBirth")
    private String dateOfBirth;
    @Column(name="ParticipantAddress1")
    private String participantAddress1;
    @Column(name="ParticipantAddress2")
    private String participantAddress2;
    @Column(name="ParticipantCity")
    private String participantCity;
    @Column(name="ParticipantState")
    private String participantState;
    @Column(name="ParticipantZip")
    private String participantZip;
    @Column(name="Carrier")
    private String carrier;
    @Column(name="Gender")
    private String gender;
    @Column(name="CustomID")
    private String customId;
    @Column(name="Relationship")
    private String relationship;
    @Column(name="PBBillingFrequency")
    private String pbBillingFrequency;
    @Column(name="DivisionName")
    private String divisionName;
    @Column(name="DivisionCustomID")
    private String divisionCustomId;
    @Column(name="DivisionID")
    private String divisionId;
    @Column(name="IsByDivision")
    private String isByDivision;
    @Column(name="EmployerOrganizationID")
    private int organizationId;

    public ImportCobPart(){}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public String getAcceptedDate() {
        return acceptedDate;
    }

    public void setAcceptedDate(String acceptedDate) {
        this.acceptedDate = acceptedDate;
    }

    public String getDisplayPremium() {
        return displayPremium;
    }

    public void setDisplayPremium(String displayPremium) {
        this.displayPremium = displayPremium;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getPaidThroughDate() {
        return paidThroughDate;
    }

    public void setPaidThroughDate(String paidThroughDate) {
        this.paidThroughDate = paidThroughDate;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getCoveredMemberName() {
        return coveredMemberName;
    }

    public void setCoveredMemberName(String coveredMemberName) {
        this.coveredMemberName = coveredMemberName;
    }

    public String getPbTypeName() {
        return pbTypeName;
    }

    public void setPbTypeName(String pbTypeName) {
        this.pbTypeName = pbTypeName;
    }

    public String getQualifyingEvent() {
        return qualifyingEvent;
    }

    public void setQualifyingEvent(String qualifyingEvent) {
        this.qualifyingEvent = qualifyingEvent;
    }

    public String getQualifyingEventDate() {
        return qualifyingEventDate;
    }

    public void setQualifyingEventDate(String qualifyingEventDate) {
        this.qualifyingEventDate = qualifyingEventDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getTermedDate() {
        return termedDate;
    }

    public void setTermedDate(String termedDate) {
        this.termedDate = termedDate;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
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

    public String getPbCoverageHeaderId() {
        return pbCoverageHeaderId;
    }

    public void setPbCoverageHeaderId(String pbCoverageHeaderId) {
        this.pbCoverageHeaderId = pbCoverageHeaderId;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getCoveredMembersNames() {
        return coveredMembersNames;
    }

    public void setCoveredMembersNames(String coveredMembersNames) {
        this.coveredMembersNames = coveredMembersNames;
    }

    public String getIncludeDependentCoveredUnderEachPlan() {
        return includeDependentCoveredUnderEachPlan;
    }

    public void setIncludeDependentCoveredUnderEachPlan(String includeDependentCoveredUnderEachPlan) {
        this.includeDependentCoveredUnderEachPlan = includeDependentCoveredUnderEachPlan;
    }

    public String getIsDependent() {
        return isDependent;
    }

    public void setIsDependent(String isDependent) {
        this.isDependent = isDependent;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getParticipantAddress1() {
        return participantAddress1;
    }

    public void setParticipantAddress1(String participantAddress1) {
        this.participantAddress1 = participantAddress1;
    }

    public String getParticipantAddress2() {
        return participantAddress2;
    }

    public void setParticipantAddress2(String participantAddress2) {
        this.participantAddress2 = participantAddress2;
    }

    public String getParticipantCity() {
        return participantCity;
    }

    public void setParticipantCity(String participantCity) {
        this.participantCity = participantCity;
    }

    public String getParticipantState() {
        return participantState;
    }

    public void setParticipantState(String participantState) {
        this.participantState = participantState;
    }

    public String getParticipantZip() {
        return participantZip;
    }

    public void setParticipantZip(String participantZip) {
        this.participantZip = participantZip;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPbBillingFrequency() {
        return pbBillingFrequency;
    }

    public void setPbBillingFrequency(String pbBillingFrequency) {
        this.pbBillingFrequency = pbBillingFrequency;
    }

    public String getDivisionName() {
        return divisionName;
    }

    public void setDivisionName(String divisionName) {
        this.divisionName = divisionName;
    }

    public String getDivisionCustomId() {
        return divisionCustomId;
    }

    public void setDivisionCustomId(String divisionCustomId) {
        this.divisionCustomId = divisionCustomId;
    }

    public String getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(String divisionId) {
        this.divisionId = divisionId;
    }

    public String getIsByDivision() {
        return isByDivision;
    }

    public void setIsByDivision(String isByDivision) {
        this.isByDivision = isByDivision;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }
}

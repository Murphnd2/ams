package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class sCobraQb {
    @Column(name="EmployerSystemId")
    private int employerSystemId;
    @Column(name="EmployerCustomId")
    private String employerCustomId;
    @Column(name="EmployerName")
    private String employerName;
    @Id
    @Column(name="EmployerPlanSystemID")
    private int employerPlanSystemId;
    @Column(name="ImportPlanID")
    private String importPlanId;
    @Column(name="EmployerPlanName")
    private String employerPlanName;
    @Column(name="EmployerDivisionID")
    private String employerDivisionId;
    @Column(name="EmployerDivision")
    private String employerDivision;
    @Column(name="IsIndividuallyRated")
    private String isIndividuallyRated;
    @Column(name="EmployerPlanTierName")
    private String employerPlanTierName;
    @Column(name="UserId")
    private int userId;
    @Column(name="Relationship")
    private String relationship;
    @Id
    @Column(name="ParticipantSystemId")
    private int participantSystemId;

    @Column(name="ParticipantCustomId")
    private String participantCustomId;
    @Column(name="ParticipantName")
    private String participantName;
    @Column(name="ParticipantFirstName")
    private String participantFirstName;
    @Column(name="ParticipantLastName")
    private String participantLastName;
    @Column(name="ParticipantMiddleName")
    private String participantMiddleName;
    @Column(name="IsDependent")
    private String isDependent;
    @Column(name="CoveredMemberName")
    private String coveredMemberName;
    @Column(name="CoveredMemberFirstName")
    private String coveredMemberFirstName;
    @Column(name="CoveredMemberLastName")
    private String coveredMemberLastName;
    @Column(name="CoveredMemberMiddleName")
    private String coveredMemberMiddleName;
    @Column(name="ParticipantAddress1")
    private String participantAddress1;
    @Column(name="ParticipantAddress2")
    private String participantAddress2;
    @Column(name="ParticipantCity")
    private String participantCity;
    @Column(name="ParticipantState")
    private String participantState;
    @Column(name="ParticipantZipCode")
    private String participantZipCode;
    @Column(name="ParticipantEmail")
    private String participantEmail;
    @Column(name="ParticipantPhone")
    private String participantPhone;
    @Column(name="SSN")
    private String ssn;
    @Column(name="DependentSystemID")
    private String dependentSystemId;
    @Column(name="DependentCustomID")
    private String dependentCustomId;
    @Column(name="DependentFirstName")
    private String dependentFirstName;
    @Column(name="DependentLastName")
    private String dependentLastName;
    @Column(name="DependentMiddleName")
    private String dependentMiddleName;
    @Column(name="DependentRelationship")
    private String dependentRelationship;
    @Column(name="DependentParticipantSystemID")
    private String dependentParticipantSystemId;
    @Column(name="QualifyingEventReason")
    private String qualifyingEventReason;
    @Column(name="QualifyingEventDate")
    private String qualifyingEventDate;
    @Column(name="CoverageStatus")
    private String coverageStatus;
    @Column(name="CoverageStatusEffectiveDate")
    private String coverageStatusEffectiveDate;
    @Column(name="LastDayOfCoverage")
    private String lastDayOfCoverage;
    @Column(name="LastDayToAccept")
    private String lastDayToAccept;
    @Column(name="CobraAccepted")
    private String cobraAccepted;
    @Column(name="AcceptedEntered")
    private String acceptedEntered;
    @Column(name="CobraAcceptedDate")
    private String cobraAcceptedDate;
    @Column(name="CobraStartDate")
    private String cobraStartDate;
    @Column(name="CobraTermed")
    private String cobraTermed;
    @Column(name="TermedEntered")
    private String termedEntered;
    @Column(name="Subsidy")
    private String subsidy;
    @Column(name="TransactionStartDate")
    private String transactionStartDate;
    @Column(name="TransactionEndDate")
    private String transactionEndDate;
    @Column(name="IsByDivision")
    private String isByDivision;
    @Column(name="CoveredMembersNames")
    private String coveredMembersName;
    @Column(name="PBBillingFrequency")
    private String pbBillingFrequency;

    public sCobraQb(){}

    public String getParticipantCustomId() {
        return participantCustomId;
    }

    public void setParticipantCustomId(String participantCustomId) {
        this.participantCustomId = participantCustomId;
    }

    public int getEmployerSystemId() {
        return employerSystemId;
    }

    public void setEmployerSystemId(int employerSystemId) {
        this.employerSystemId = employerSystemId;
    }

    public String getEmployerCustomId() {
        return employerCustomId;
    }

    public void setEmployerCustomId(String employerCustomId) {
        this.employerCustomId = employerCustomId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public int getEmployerPlanSystemId() {
        return employerPlanSystemId;
    }

    public void setEmployerPlanSystemId(int employerPlanSystemId) {
        this.employerPlanSystemId = employerPlanSystemId;
    }

    public String getImportPlanId() {
        return importPlanId;
    }

    public void setImportPlanId(String importPlanId) {
        this.importPlanId = importPlanId;
    }

    public String getEmployerPlanName() {
        return employerPlanName;
    }

    public void setEmployerPlanName(String employerPlanName) {
        this.employerPlanName = employerPlanName;
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

    public String getIsIndividuallyRated() {
        return isIndividuallyRated;
    }

    public void setIsIndividuallyRated(String isIndividuallyRated) {
        this.isIndividuallyRated = isIndividuallyRated;
    }

    public String getEmployerPlanTierName() {
        return employerPlanTierName;
    }

    public void setEmployerPlanTierName(String employerPlanTierName) {
        this.employerPlanTierName = employerPlanTierName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public int getParticipantSystemId() {
        return participantSystemId;
    }

    public void setParticipantSystemId(int participantSystemId) {
        this.participantSystemId = participantSystemId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantFirstName() {
        return participantFirstName;
    }

    public void setParticipantFirstName(String participantFirstName) {
        this.participantFirstName = participantFirstName;
    }

    public String getParticipantLastName() {
        return participantLastName;
    }

    public void setParticipantLastName(String participantLastName) {
        this.participantLastName = participantLastName;
    }

    public String getParticipantMiddleName() {
        return participantMiddleName;
    }

    public void setParticipantMiddleName(String participantMiddleName) {
        this.participantMiddleName = participantMiddleName;
    }

    public String getIsDependent() {
        return isDependent;
    }

    public void setIsDependent(String isDependent) {
        this.isDependent = isDependent;
    }

    public String getCoveredMemberName() {
        return coveredMemberName;
    }

    public void setCoveredMemberName(String coveredMemberName) {
        this.coveredMemberName = coveredMemberName;
    }

    public String getCoveredMemberFirstName() {
        return coveredMemberFirstName;
    }

    public void setCoveredMemberFirstName(String coveredMemberFirstName) {
        this.coveredMemberFirstName = coveredMemberFirstName;
    }

    public String getCoveredMemberLastName() {
        return coveredMemberLastName;
    }

    public void setCoveredMemberLastName(String coveredMemberLastName) {
        this.coveredMemberLastName = coveredMemberLastName;
    }

    public String getCoveredMemberMiddleName() {
        return coveredMemberMiddleName;
    }

    public void setCoveredMemberMiddleName(String coveredMemberMiddleName) {
        this.coveredMemberMiddleName = coveredMemberMiddleName;
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

    public String getParticipantZipCode() {
        return participantZipCode;
    }

    public void setParticipantZipCode(String participantZipCode) {
        this.participantZipCode = participantZipCode;
    }

    public String getParticipantEmail() {
        return participantEmail;
    }

    public void setParticipantEmail(String participantEmail) {
        this.participantEmail = participantEmail;
    }

    public String getParticipantPhone() {
        return participantPhone;
    }

    public void setParticipantPhone(String participantPhone) {
        this.participantPhone = participantPhone;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getDependentSystemId() {
        return dependentSystemId;
    }

    public void setDependentSystemId(String dependentSystemId) {
        this.dependentSystemId = dependentSystemId;
    }

    public String getDependentCustomId() {
        return dependentCustomId;
    }

    public void setDependentCustomId(String dependentCustomId) {
        this.dependentCustomId = dependentCustomId;
    }

    public String getDependentFirstName() {
        return dependentFirstName;
    }

    public void setDependentFirstName(String dependentFirstName) {
        this.dependentFirstName = dependentFirstName;
    }

    public String getDependentLastName() {
        return dependentLastName;
    }

    public void setDependentLastName(String dependentLastName) {
        this.dependentLastName = dependentLastName;
    }

    public String getDependentMiddleName() {
        return dependentMiddleName;
    }

    public void setDependentMiddleName(String dependentMiddleName) {
        this.dependentMiddleName = dependentMiddleName;
    }

    public String getDependentRelationship() {
        return dependentRelationship;
    }

    public void setDependentRelationship(String dependentRelationship) {
        this.dependentRelationship = dependentRelationship;
    }

    public String getDependentParticipantSystemId() {
        return dependentParticipantSystemId;
    }

    public void setDependentParticipantSystemId(String dependentParticipantSystemId) {
        this.dependentParticipantSystemId = dependentParticipantSystemId;
    }

    public String getQualifyingEventReason() {
        return qualifyingEventReason;
    }

    public void setQualifyingEventReason(String qualifyingEventReason) {
        this.qualifyingEventReason = qualifyingEventReason;
    }

    public String getQualifyingEventDate() {
        return qualifyingEventDate;
    }

    public void setQualifyingEventDate(String qualifyingEventDate) {
        this.qualifyingEventDate = qualifyingEventDate;
    }

    public String getCoverageStatus() {
        return coverageStatus;
    }

    public void setCoverageStatus(String coverageStatus) {
        this.coverageStatus = coverageStatus;
    }

    public String getCoverageStatusEffectiveDate() {
        return coverageStatusEffectiveDate;
    }

    public void setCoverageStatusEffectiveDate(String coverageStatusEffectiveDate) {
        this.coverageStatusEffectiveDate = coverageStatusEffectiveDate;
    }

    public String getLastDayOfCoverage() {
        return lastDayOfCoverage;
    }

    public void setLastDayOfCoverage(String lastDayOfCoverage) {
        this.lastDayOfCoverage = lastDayOfCoverage;
    }

    public String getLastDayToAccept() {
        return lastDayToAccept;
    }

    public void setLastDayToAccept(String lastDayToAccept) {
        this.lastDayToAccept = lastDayToAccept;
    }

    public String getCobraAccepted() {
        return cobraAccepted;
    }

    public void setCobraAccepted(String cobraAccepted) {
        this.cobraAccepted = cobraAccepted;
    }

    public String getAcceptedEntered() {
        return acceptedEntered;
    }

    public void setAcceptedEntered(String acceptedEntered) {
        this.acceptedEntered = acceptedEntered;
    }

    public String getCobraAcceptedDate() {
        return cobraAcceptedDate;
    }

    public void setCobraAcceptedDate(String cobraAcceptedDate) {
        this.cobraAcceptedDate = cobraAcceptedDate;
    }

    public String getCobraStartDate() {
        return cobraStartDate;
    }

    public void setCobraStartDate(String cobraStartDate) {
        this.cobraStartDate = cobraStartDate;
    }

    public String getCobraTermed() {
        return cobraTermed;
    }

    public void setCobraTermed(String cobraTermed) {
        this.cobraTermed = cobraTermed;
    }

    public String getTermedEntered() {
        return termedEntered;
    }

    public void setTermedEntered(String termedEntered) {
        this.termedEntered = termedEntered;
    }

    public String getSubsidy() {
        return subsidy;
    }

    public void setSubsidy(String subsidy) {
        this.subsidy = subsidy;
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

    public String getIsByDivision() {
        return isByDivision;
    }

    public void setIsByDivision(String isByDivision) {
        this.isByDivision = isByDivision;
    }

    public String getCoveredMembersName() {
        return coveredMembersName;
    }

    public void setCoveredMembersName(String coveredMembersName) {
        this.coveredMembersName = coveredMembersName;
    }

    public String getPbBillingFrequency() {
        return pbBillingFrequency;
    }

    public void setPbBillingFrequency(String pbBillingFrequency) {
        this.pbBillingFrequency = pbBillingFrequency;
    }
}

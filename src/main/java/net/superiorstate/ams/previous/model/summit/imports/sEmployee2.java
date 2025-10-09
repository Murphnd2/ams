package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class sEmployee2 {
    @Id
    @Column(name="Participant_ID")
    private int participantId;

    @Column(name="EffectiveDate")
    private String effectiveDate;

    @Column(name="TerminationDate")
    private String terminationDate;

    @Column(name="UserId")
    private String userId;

    @Column(name="UserStatus")
    private String userStatus;

    @Column(name="CreatedDate")
    private String createdDate;

    @Column(name="HireDate")
    private String hireDate;

    @Column(name="Organization_ID")
    private int organizationId;

    @Column(name="ParticipantStatusId")
    private int participantStatusId;

    @Column(name="Name")
    private String name;

    @Column(name="ERName")
    private String erName;

    @Column(name="ParticipantName")
    private String participantName;

    @Column(name="Participant_Last")
    private String participantLast;

    @Column(name="Participant_First")
    private String participantFirst;

    @Column(name="SSN")
    private String ssn;

    @Column(name="userStatusID")
    private int userStatusId;

    @Column(name="DOB")
    private String dob;

    @Column(name="ReimbursementMethod")
    private String reimbursementMethod;

    @Column(name="EmploymentStatus")
    private String employmentStatus;

    @Column(name="EmploymentStatusID")
    private int employmentStatusId;

    @Column(name="No_of_participants")
    private String noOfParticipants;

    @Column(name="DivisionName")
    private String divisionName;

    @Column(name="Bankname")
    private String bankName;

    @Column(name="RoutingNo")
    private String routingNo;

    @Column(name="AccountType")
    private String accountType;

    @Column(name="AccountNumber")
    private String accountNumber;

    @Column(name="UserBank_ID")
    private String userBankId;

    @Column(name="ReimbursementMethod_ID")
    private String reimbursementMethodId;

    @Column(name="ByDivision")
    private String byDivision;

    public sEmployee2(){}

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }

    public int getParticipantStatusId() {
        return participantStatusId;
    }

    public void setParticipantStatusId(int participantStatusId) {
        this.participantStatusId = participantStatusId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getErName() {
        return erName;
    }

    public void setErName(String erName) {
        this.erName = erName;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantLast() {
        return participantLast;
    }

    public void setParticipantLast(String participantLast) {
        this.participantLast = participantLast;
    }

    public String getParticipantFirst() {
        return participantFirst;
    }

    public void setParticipantFirst(String participantFirst) {
        this.participantFirst = participantFirst;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public int getUserStatusId() {
        return userStatusId;
    }

    public void setUserStatusId(int userStatusId) {
        this.userStatusId = userStatusId;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getReimbursementMethod() {
        return reimbursementMethod;
    }

    public void setReimbursementMethod(String reimbursementMethod) {
        this.reimbursementMethod = reimbursementMethod;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public int getEmploymentStatusId() {
        return employmentStatusId;
    }

    public void setEmploymentStatusId(int employmentStatusId) {
        this.employmentStatusId = employmentStatusId;
    }

    public String getNoOfParticipants() {
        return noOfParticipants;
    }

    public void setNoOfParticipants(String noOfParticipants) {
        this.noOfParticipants = noOfParticipants;
    }

    public String getDivisionName() {
        return divisionName;
    }

    public void setDivisionName(String divisionName) {
        this.divisionName = divisionName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getRoutingNo() {
        return routingNo;
    }

    public void setRoutingNo(String routingNo) {
        this.routingNo = routingNo;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getUserBankId() {
        return userBankId;
    }

    public void setUserBankId(String userBankId) {
        this.userBankId = userBankId;
    }

    public String getReimbursementMethodId() {
        return reimbursementMethodId;
    }

    public void setReimbursementMethodId(String reimbursementMethodId) {
        this.reimbursementMethodId = reimbursementMethodId;
    }

    public String getByDivision() {
        return byDivision;
    }

    public void setByDivision(String byDivision) {
        this.byDivision = byDivision;
    }
}

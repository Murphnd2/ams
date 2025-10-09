package net.superiorstate.ams.previous.model.summit.imports.order;

import jakarta.persistence.*;

@Entity
@Table(name="import2employee")
public class ImportEmployee {
    @Column(name="Employer_ID")
    private String employerId;

    @Column(name="SetupCompletionDate")
    private String setupCompletionDate;

    @Column(name="EmployerCustomID")
    private String employerCustomId;

    @ManyToOne
    @JoinColumn(name="Organization_ID")
    private ImportEmployer importEmployer;

    @Column(name="EmployerName")
    private String employerName;

    @Id
    @Column(name="Participant_ID")
    private int id;

    @Column(name="User_ID")
    private String userId;

    @Column(name="FirstName")
    private String firstName;

    @Column(name="LastName")
    private String lastName;

    @Column(name="ParticipantCustomID")
    private String dpiSuiteMmKey;

    @Column(name="UserStatus")
    private String userStatus;

    @Column(name="Email")
    private String email;

    @Column(name="Address1")
    private String address1;

    @Column(name="Address2")
    private String address2;

    @Column(name="City")
    private String city;

    @Column(name="State")
    private String state;

    @Column(name="ZipCode")
    private String zipCode;

    @Column(name="IsRegisterdToPortal")
    private String isRegisteredToPortal;

    @Column(name="FailedLoginCount")
    private String failedLoginCount;

    @Column(name="LastLoginDate")
    private String lastLoginDate;

    public ImportEmployee(){}

    public String getEmployerId() {
        return employerId;
    }

    public void setEmployerId(String employerId) {
        this.employerId = employerId;
    }

    public String getSetupCompletionDate() {
        return setupCompletionDate;
    }

    public void setSetupCompletionDate(String setupCompletionDate) {
        this.setupCompletionDate = setupCompletionDate;
    }

    public String getEmployerCustomId() {
        return employerCustomId;
    }


    public String getFullNameLastThenFirst(){
        return lastName.trim() + ", " + firstName.trim();
    }
    public void setEmployerCustomId(String employerCustomId) {
        this.employerCustomId = employerCustomId;
    }

    public ImportEmployer getImportEmployer() {
        return importEmployer;
    }

    public void setImportEmployer(ImportEmployer importEmployer) {
        this.importEmployer = importEmployer;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDpiSuiteMmKey() {
        return dpiSuiteMmKey;
    }

    public void setDpiSuiteMmKey(String dpiSuiteMmKey) {
        this.dpiSuiteMmKey = dpiSuiteMmKey;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getIsRegisteredToPortal() {
        return isRegisteredToPortal;
    }

    public void setIsRegisteredToPortal(String isRegisteredToPortal) {
        this.isRegisteredToPortal = isRegisteredToPortal;
    }

    public String getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(String failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
}

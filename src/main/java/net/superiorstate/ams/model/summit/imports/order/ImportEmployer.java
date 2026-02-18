package net.superiorstate.ams.model.summit.imports.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="import1employer")
public class ImportEmployer {
    @Id
    @Column(name="OrganizationID")
    private int organizationId;

    @Column(name="Employer_ID")
    private int employerId;

    @Column(name="EmployerName")
    private String employerName;

    @Column(name="CustomID")
    private String dpiSuiteErKey;

    @Column(name="CreatedDate")
    private String createdDate;

    @Column(name="CreatedByUser")
    private String createdByUser;

    @Column(name="CreatedByUser1")
    private String createdByUser1;

    @Column(name="CreatedUser")
    private String createdUser;

    @Column(name="SetUpCompletionDate")
    private String setUpCompletionDate;

    @Column(name="SetUpComplete")
    private String setUpComplete;

    @Column(name="IsSetUpCompleted")
    private String isSetUpCompleted;

    @Column(name="AgencyOrganizationID")
    private String agencyOrganizationId;

    @Column(name="Agency")
    private String agency;

    @Column(name="TaxID")
    private String taxId;

    @Column(name="ImplementationLeadUserID")
    private String implementationLeadUserId;

    @Column(name="ImplementationLead")
    private String implementationLead;

    @Column(name="OrganizationStatusID")
    private String organizationStatusId;

    @Column(name="Status")
    private String status;

    @Column(name="PrimaryContact")
    private String primaryContact;

    @Column(name="PhoneNumber")
    private String phoneNumber;

    @Column(name="Phone")
    private String phone;

    @Column(name="Email")
    private String email;

    @Column(name="Setup")
    private String setup;

    public ImportEmployer(){}

    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }

    public int getEmployerId() {
        return employerId;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getDpiSuiteErKey() {
        return dpiSuiteErKey;
    }

    public void setDpiSuiteErKey(String dpiSuiteErKey) {
        this.dpiSuiteErKey = dpiSuiteErKey;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getCreatedByUser1() {
        return createdByUser1;
    }

    public void setCreatedByUser1(String createdByUser1) {
        this.createdByUser1 = createdByUser1;
    }

    public String getCreatedUser() {
        return createdUser;
    }

    public void setCreatedUser(String createdUser) {
        this.createdUser = createdUser;
    }

    public String getSetUpCompletionDate() {
        return setUpCompletionDate;
    }

    public void setSetUpCompletionDate(String setUpCompletionDate) {
        this.setUpCompletionDate = setUpCompletionDate;
    }

    public String getSetUpComplete() {
        return setUpComplete;
    }

    public void setSetUpComplete(String setUpComplete) {
        this.setUpComplete = setUpComplete;
    }

    public String getIsSetUpCompleted() {
        return isSetUpCompleted;
    }

    public void setIsSetUpCompleted(String isSetUpCompleted) {
        this.isSetUpCompleted = isSetUpCompleted;
    }

    public String getAgencyOrganizationId() {
        return agencyOrganizationId;
    }

    public void setAgencyOrganizationId(String agencyOrganizationId) {
        this.agencyOrganizationId = agencyOrganizationId;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getImplementationLeadUserId() {
        return implementationLeadUserId;
    }

    public void setImplementationLeadUserId(String implementationLeadUserId) {
        this.implementationLeadUserId = implementationLeadUserId;
    }

    public String getImplementationLead() {
        return implementationLead;
    }

    public void setImplementationLead(String implementationLead) {
        this.implementationLead = implementationLead;
    }

    public String getOrganizationStatusId() {
        return organizationStatusId;
    }

    public void setOrganizationStatusId(String organizationStatusId) {
        this.organizationStatusId = organizationStatusId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(String primaryContact) {
        this.primaryContact = primaryContact;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSetup() {
        return setup;
    }

    public void setSetup(String setup) {
        this.setup = setup;
    }
}

package net.superiorstate.ams.model.summit.imports;

import jakarta.persistence.*;

@Entity
public class sEmployee {

    @Column(name="Employer_ID")
    private String employerId;


    @Column(name="SetupCompletionDate")
    private String setupCompletionDate;

    @Column(name="EmployerCustomID")
    private String employerCustomId;
    @ManyToOne
    @JoinColumn(name="Organization_ID")
    private sEmployer sEmployer;

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

    @Column(name="IsRegisteredToPortal")
    private String isRegisteredToPortal;

    @Column(name="FailedLoginCount")
    private String failedLoginCount;

    @Column(name="LastLoginDate")
    private String lastLoginDate;

    public sEmployee(){}

    public int getId() {
        return id;
    }

    public sEmployer getSummitOrganization() {
        return sEmployer;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return firstName.trim() + " " + lastName.trim();
    }

    public String getFullNameLastThenFirst(){
        return lastName.trim() + ", " + firstName.trim();
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDpiSuiteMmKey() {
        return dpiSuiteMmKey;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getIsRegisteredToPortal() {
        return isRegisteredToPortal;
    }

    public String getFailedLoginCount() {
        return failedLoginCount;
    }

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }
}

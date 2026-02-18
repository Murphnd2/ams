package net.superiorstate.ams.model.summit.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class HsaAccount {
    @Id
    @Column(name="hsaid")
    private int hsaId;

    @Column(name="last_name")
    private String lastName;

    @Column(name="first_name")
    private String firstName;

    @Column(name="employer")
    private String employer;

    @Column(name="address")
    private String address;

    @Column(name="city")
    private String city;

    @Column(name="state")
    private String state;

    @Column(name="zip")
    private String zip;

    @Column(name="phone")
    private String phone;

    @Id
    @Column(name="acct_num")
    private String accountNum;

    @Column(name="active")
    private boolean active;

    public HsaAccount(){}

    public int getHsaId() {
        return hsaId;
    }

    public void setHsaId(int hsaId) {
        this.hsaId = hsaId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAccountNum() {
        return accountNum;
    }

    public void setAccountNum(String accountNum) {
        this.accountNum = accountNum;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

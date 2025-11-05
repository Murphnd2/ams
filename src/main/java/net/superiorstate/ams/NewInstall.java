package net.superiorstate.ams;

import jakarta.persistence.*;

@Entity
@Table(name=".start_here")
public class NewInstall {
    @Id
    @GeneratedValue
    @Column(name="id")
    private int id;

    @Column(name="first_name")
    private String firstName;

    @Column(name="last_name")
    private String lastName;

    @Column(name="company")
    private String companyName;

    @Column(name="phone")
    private String phone;

    @Column(name="email")
    private String email;

    @Column(name="address1")
    private String address1;

    @Column(name="address2")
    private String address2;

    @Column(name="city")
    private String city;

    @Column(name="state_id")
    private String state;

    @Column(name="zip_code")
    private String zipCode;

    @Column(name="tax_id")
    private String taxId;

    @Column(name="cdh")
    private boolean hasCdh;

    @Column(name="fsa")
    private boolean hasFsa;

    @Column(name="hra")
    private boolean hasHra;

    @Column(name="hsa")
    private boolean hasHsa;

    @Column(name="transit")
    private boolean hasTransit;

    @Column(name="lsa")
    private boolean hasLsa;

    @Column(name="pb")
    private boolean hasPb;

    @Column(name="cobra")
    private boolean hasCobra;

    @Column(name="retiree")
    private boolean hasRetireeBilling;

    @Column(name="direct")
    private boolean hasDirectBill;

    @Column(name="setup_completed")
    private boolean setupCompleted;

    public NewInstall() { }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public boolean isHasCdh() {
        return hasCdh;
    }

    public void setHasCdh(boolean hasCdh) {
        this.hasCdh = hasCdh;
    }

    public boolean isHasFsa() {
        return hasFsa;
    }

    public void setHasFsa(boolean hasFsa) {
        this.hasFsa = hasFsa;
    }

    public boolean isHasHra() {
        return hasHra;
    }

    public void setHasHra(boolean hasHra) {
        this.hasHra = hasHra;
    }

    public boolean isHasHsa() {
        return hasHsa;
    }

    public void setHasHsa(boolean hasHsa) {
        this.hasHsa = hasHsa;
    }

    public boolean isHasTransit() {
        return hasTransit;
    }

    public void setHasTransit(boolean hasTransit) {
        this.hasTransit = hasTransit;
    }

    public boolean isHasLsa() {
        return hasLsa;
    }

    public void setHasLsa(boolean hasLsa) {
        this.hasLsa = hasLsa;
    }

    public boolean isHasPb() {
        return hasPb;
    }

    public void setHasPb(boolean hasPb) {
        this.hasPb = hasPb;
    }

    public boolean isHasCobra() {
        return hasCobra;
    }

    public void setHasCobra(boolean hasCobra) {
        this.hasCobra = hasCobra;
    }

    public boolean isHasRetireeBilling() {
        return hasRetireeBilling;
    }

    public void setHasRetireeBilling(boolean hasRetireeBilling) {
        this.hasRetireeBilling = hasRetireeBilling;
    }

    public boolean isHasDirectBill() {
        return hasDirectBill;
    }

    public void setHasDirectBill(boolean hasDirectBill) {
        this.hasDirectBill = hasDirectBill;
    }

    public boolean isSetupCompleted() {
        return setupCompleted;
    }

    public void setSetupCompleted(boolean setupCompleted) {
        this.setupCompleted = setupCompleted;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }
}

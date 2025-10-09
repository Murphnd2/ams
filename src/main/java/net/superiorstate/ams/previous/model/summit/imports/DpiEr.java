package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="dpier")
public class DpiEr {
    @Id
    @Column(name="er_key")
    private int erKey;

    @Column(name="tax_id")
    private String taxId;

    @Column(name="er_company")
    private String erCompany;

    @Column(name="er_first")
    private String erFirst;

    @Column(name="er_last")
    private String erLast;

    @Column(name="er_phone")
    private String phone;

    @Column(name="email")
    private String email;

    public DpiEr(){}

    public int getErKey() {
        return erKey;
    }

    public void setErKey(int erKey) {
        this.erKey = erKey;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getErCompany() {
        return erCompany;
    }

    public void setErCompany(String erCompany) {
        this.erCompany = erCompany;
    }

    public String getErFirst() {
        return erFirst;
    }

    public void setErFirst(String erFirst) {
        this.erFirst = erFirst;
    }

    public String getErLast() {
        return erLast;
    }

    public void setErLast(String erLast) {
        this.erLast = erLast;
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

    public String getFullName(){
        return getErFirst() + " " + getErLast();
    }

    public String getPhoneClean(){
        return getPhone().replaceAll("-","");
    }
}

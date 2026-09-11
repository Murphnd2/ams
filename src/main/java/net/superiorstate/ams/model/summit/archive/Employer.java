package net.superiorstate.ams.model.summit.archive;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Employer {
    @Id
    @Column(name="organization_id")
    private int id;

    @Column(name="employer_id")
    private int altId;

    @Column(name="employer_name")
    private String employerName;

    @Column(name="er_key")
    private int erKey;

    @Column(name="custom_id")
    private String customId;

    @Column(name="phone")
    private String phone;

    @Column(name="email")
    private String email;

    @Column(name="contact_name")
    private String contactName;

    @Column(name="active")
    private boolean isActive;

    @Column(name="billable")
    private boolean isBillable;

    @ManyToMany
    @JoinTable(name="employer_contacts",joinColumns =@JoinColumn(name="employer_id"),inverseJoinColumns = @JoinColumn(name="assignee_id"))
    List<Employee> contactList;

    @Column(name="is_agency")
    private boolean isAgency;

    @Column(name="has_pop")
    private boolean hasPop;

    @Column(name="has_cdh")
    private boolean hasCdh;

    @Column(name="has_pb")
    private boolean hasPb;

    public Employer(){}

    public boolean isPop() {
        return hasPop;
    }

    public void setHasPop(boolean hasPop) {
        this.hasPop = hasPop;
    }

    public boolean isCdh() {
        return hasCdh;
    }

    public void setHasCdh(boolean hasCdh) {
        this.hasCdh = hasCdh;
    }

    public boolean isPb() {
        return hasPb;
    }

    public void setHasPb(boolean hasPb) {
        this.hasPb = hasPb;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAltId() {
        return altId;
    }

    public void setAltId(int altId) {
        this.altId = altId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public int getErKey() {
        return erKey;
    }

    public void setErKey(int erKey) {
        this.erKey = erKey;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
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

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isBillable() {
        return isBillable;
    }

    public void setBillable(boolean billable) {
        isBillable = billable;
    }

    public List<Employee> getContactList() {
        return contactList;
    }

    public boolean isAgency() {
        return isAgency;
    }

    public void setAgency(boolean agency) {
        isAgency = agency;
    }

    public void addContact(Employee assignee){
        this.contactList.add(assignee);
        assignee.getEmployerList().add(this);
    }

    public void removeContact(Employee assignee){
        this.contactList.remove(assignee);
        assignee.getEmployerList().remove(this);
    }
}

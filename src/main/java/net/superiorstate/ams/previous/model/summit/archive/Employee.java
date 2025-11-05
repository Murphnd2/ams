package net.superiorstate.ams.previous.model.summit.archive;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Employee {

    @Id
    @Column(name="employee_id")
    private int id;


    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @Column(name="first_name")
    private String firstName;

    @Column(name="last_name")
    private String lastName;

    @Column(name="mm_key")
    private int mmKey;

    @Column(name="email")
    private String email;

    @Column(name="hr_email")
    private String hrEmail;

    @Column(name="address1")
    private String address1;

    @Column(name="address2")
    private String address2;

    @Column(name="city")
    private String city;

    @Column(name="state")
    private String state;

    @Column(name="zip")
    private String zipCode;

    @Column(name="user_id")
    private String userId;

    @Column(name="is_active")
    private boolean isActive;

    @Column(name="custom_id")
    private String customId;

   @ManyToMany(mappedBy = "contactList")
   private List<Employer> employerList;

   @Column(name="ee_status_id")
   private Integer eeStatusId;

   @Column(name="system_status_id")
   private Integer systemStatusId;

   @Column(name="cobra_status_id")
   private Integer cobraStatusId;

    public Employee(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
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

    public int getMmKey() {
        return mmKey;
    }

    public void setMmKey(int mmKey) {
        this.mmKey = mmKey;
    }

    public String getEmail() {
        if(getHrEmail()==null || getHrEmail().equals(""))
            return email;
        return getHrEmail();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Employer> getEmployerList() {
        return employerList;
    }

    public void setEmployerList(List<Employer> employerList) {
        this.employerList = employerList;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getHrEmail() {
        return hrEmail;
    }

    public void setHrEmail(String hrEmail) {
        this.hrEmail = hrEmail;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public Integer getEeStatusId() {
        return eeStatusId;
    }

    public void setEeStatusId(Integer eeStatusId) {
        this.eeStatusId = eeStatusId;
    }

    public Integer getSystemStatusId() {
        return systemStatusId;
    }

    public void setSystemStatusId(Integer systemStatusId) {
        this.systemStatusId = systemStatusId;
    }

    public Integer getCobraStatusId() {
        return cobraStatusId;
    }

    public void setCobraStatusId(Integer cobraStatusId) {
        this.cobraStatusId = cobraStatusId;
    }
}

package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

@Entity
public class HsaEe {

    @Id
    @Column(name="hsa_id")
    private int hsaId;

    @Column(name="last_name")
    private String lastName;

    @Column(name="first_name")
    private String firstName;

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

    @ManyToOne
    @JoinColumn(name="hsa_er_id")
    private HsaEr hsaEr;

    @OneToOne
    @JoinColumn(name="employee_id")
    private Employee employee;

    public HsaEe (){}

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

    public HsaEr getHsaEr() {
        return hsaEr;
    }

    public void setHsaEr(HsaEr hsaEr) {
        this.hsaEr = hsaEr;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}

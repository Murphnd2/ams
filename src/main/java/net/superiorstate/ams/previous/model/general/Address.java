package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@Entity
public class Address implements Comparable<Address> {
    @Id
    @GeneratedValue
    @Column(name = "address_id")
    private Long id;

    @Column(name = "address1", columnDefinition = "varchar(100)")
    private String address1;
    @Column(name = "address2", columnDefinition = "varchar(100)")
    private String address2;
    @Column(name = "city", columnDefinition = "varchar(50)")
    private String city;
    @Column(name = "state", columnDefinition = "varchar(2)")
    private String state;
    @Column(name = "zip_code", columnDefinition = "varchar(10)")
    private String zipCode;

    @Column(name = "date_created", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Address() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    @Override
    public int compareTo(Address a) {
        if (this.state.compareTo(a.getState()) != 0) {
            return this.state.compareTo(a.getState());
        } else if (this.city.compareTo(a.getCity()) != 0) {
            return this.city.compareTo(a.getCity());
        } else {
            return this.address1.compareTo(a.getAddress1());
        }
    }
}

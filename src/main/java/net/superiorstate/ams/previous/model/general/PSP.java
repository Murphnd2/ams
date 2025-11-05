package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

@Entity
@NamedQuery(
        name = "PSP.getById",
        query = "SELECT p FROM PSP p WHERE p.id = :psp_id"
)
public class PSP extends Assignee {
    @Column(columnDefinition = "varchar(10)")
    private String taxId;

    @OneToOne
    @JoinColumn(name="address_id")
    private Address address;

    @OneToOne
    @JoinColumn(name="contact_id")
    private Person contact;

    @OneToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    public PSP(){}

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Person getContact() {
        return contact;
    }

    public void setContact(Person contact) {
        this.contact = contact;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }
}

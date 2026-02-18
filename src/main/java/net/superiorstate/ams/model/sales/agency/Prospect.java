package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;

import java.util.List;

@Entity
public class Prospect {
    @Id
    @GeneratedValue
    @Column(name="prospect_id")
    private Long id;

    @Column(name="name",columnDefinition = "varchar(200)")
    private String name;

    @OneToOne
    @JoinColumn(name="contact_id")
    private Person contact;

    @OneToOne
    @JoinColumn(name="address_id")
    private Address address;

    @ManyToOne
    @JoinColumn(name="agent_id")
    private Person agent;

    @OneToMany(mappedBy = "prospect")
    List<Proposal> proposalList;

    public Prospect(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person getContact() {
        return contact;
    }

    public void setContact(Person contact) {
        this.contact = contact;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Person getAgent() {
        return agent;
    }

    public void setAgent(Person agent) {
        this.agent = agent;
    }

    public List<Proposal> getProposalList() {
        return proposalList;
    }

    public void setProposalList(List<Proposal> proposalList) {
        this.proposalList = proposalList;
    }
}

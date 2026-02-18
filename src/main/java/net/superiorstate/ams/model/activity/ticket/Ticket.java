package net.superiorstate.ams.model.activity.ticket;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.general.Person;

@Entity
public class Ticket extends Activity {

    @Column(name="description")
    private String description;

    @ManyToOne
    @JoinColumn(name="person_id")
    private Person contact;

    @ManyToOne
    @JoinColumn(name="ticket_category")
    private TicketSubCategory ticketSubCategory;

    @ManyToOne
    @JoinColumn(name="method_id")
    private ContactMethod contactMethod;

    @OneToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    public Ticket(){}

    public Person getContact() {
        return contact;
    }

    public void setContact(Person contact) {
        this.contact = contact;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketSubCategory getTicketSubCategory() {
        return ticketSubCategory;
    }

    public void setTicketSubCategory(TicketSubCategory ticketSubCategory) {
        this.ticketSubCategory = ticketSubCategory;
    }

    public ContactMethod getContactMethod() {
        return contactMethod;
    }

    public void setContactMethod(ContactMethod contactMethod) {
        this.contactMethod = contactMethod;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public void setCheckList(CheckList checkList) {
        this.checkList = checkList;
    }

    @Override
    public String getFullName(){
        if(this.getPrimaryContact() != null)
            return this.getPrimaryContact().getFirstName().trim().toUpperCase() + " " + this.getPrimaryContact().getLastName().trim().toUpperCase();
        else if(this.getContact()!=null)
            return this.getContact().getFirstName().trim().toUpperCase() + " " + this.getContact().getLastName().trim().toUpperCase();
        else return this.getFullName();
    }
}

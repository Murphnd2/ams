package net.superiorstate.ams.model.activity.ticket.setup;

import jakarta.persistence.*;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.general.Person;

import java.util.List;

@Entity
public class Setup extends Activity {

    @OneToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    @OneToOne
    @JoinColumn(name="proposal_id")
    private Application application;

    @OneToOne
    @JoinColumn(name="person_id")
    private Person primaryContactSetup;


    @ManyToMany
    @JoinTable(name="setupcontacts",
            joinColumns = @JoinColumn(name="setup_id"),inverseJoinColumns = @JoinColumn(name="person_id"))
    List<Person> contactList;

    @Column(name="myRsc")
    private String myRsc;


    public Setup(){}

    public CheckList getCheckList() {
        return checkList;
    }

    public void setCheckList(CheckList checkList) {
        this.checkList = checkList;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Person getPrimaryContactSetup() {
        return primaryContactSetup;
    }

    public void setPrimaryContactSetup(Person primaryContactSetup) {
        this.primaryContactSetup = primaryContactSetup;
    }

    public List<Person> getContactList() {
        return contactList;
    }

    public void setContactList(List<Person> contactList) {
        this.contactList = contactList;
    }

    public String getMyRsc() {
        return myRsc;
    }

    public void setMyRsc(String myRsc) {
        this.myRsc = myRsc;
    }

    public void addContact(Person contact){
        this.getContactList().add(contact);
        contact.getSetupList().add(this);
    }
    public void removeContact(Person contact){
        this.getContactList().remove(contact);
        contact.getSetupList().remove(this);
    }
}

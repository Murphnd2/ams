package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.activity.note.Email;

import java.util.List;

@Entity
public class Person extends Assignee implements Comparable<Person>  {

    @Column(name="first_name",columnDefinition = "varchar(50)")
    private String firstName;
    @Column(columnDefinition = "varchar(80)", name="last_name")
    private String lastName;
    @Column(columnDefinition = "char(1)", name="middle_init")
    private String middleInit;
    @Column(columnDefinition = "varchar(100)",name="email")
    private String email;
    @Column(columnDefinition = "varchar(12)",name = "phone")
    private String phone;
    @Column(columnDefinition = "varchar(50)", name="title")
    private String title;

    @ManyToMany(mappedBy = "agentList")
    List<Agency> listOfAgenciesWithThisAgent;
    @OneToOne
    @JoinColumn(name="address_id")
    private Address address;

    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @OneToOne
    @JoinColumn(name="employee_id")
    private Employee employee;

    @ManyToMany
    @JoinTable(name="email_recipents", joinColumns = @JoinColumn(name="recipient_id"),inverseJoinColumns = @JoinColumn(name="email_id"))
    List<Email> emailList;

    @OneToMany(mappedBy = "agent")
    List<Prospect> prospectList;

    @ManyToMany(mappedBy = "contactList")
    private List<Setup> setupList;

    @ManyToMany(mappedBy = "assigneeContactList")
    private List<Assignee> listOfAssigneesWithThePerson;


    public Person(){}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        super.setFullName(firstName + " " + getLastName());
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        super.setFullName(getFirstName()+" "+lastName);
    }

    public String getMiddleInit() {
        return middleInit;
    }

    public void setMiddleInit(String middleInit) {
        this.middleInit = middleInit;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public List<Agency> getListOfAgenciesWithThisAgent() {
        return listOfAgenciesWithThisAgent;
    }

    public void setListOfAgenciesWithThisAgent(List<Agency> listOfAgenciesWithThisAgent) {
        this.listOfAgenciesWithThisAgent = listOfAgenciesWithThisAgent;
    }

    public List<Assignee> getListOfAssigneesWithThePerson() {
        return listOfAssigneesWithThePerson;
    }

    public void setListOfAssigneesWithThePerson(List<Assignee> listOfAssigneesWithThePerson) {
        this.listOfAssigneesWithThePerson = listOfAssigneesWithThePerson;
    }

    public List<Setup> getSetupList() {
        return setupList;
    }

    public void setSetupList(List<Setup> setupList) {
        this.setupList = setupList;
    }

    @Override
    public String toString() {
        return lastName + ", " + firstName + " " + middleInit + "("+super.getId()+")";
    }

    @Override
    public int compareTo(Person p) {
        if(this.lastName.compareTo(p.getLastName()) !=0){
            return this.lastName.compareTo(p.getLastName());
        }
        else if(this.firstName.compareTo(p.getFirstName()) !=0){
            return this.firstName.compareTo(p.getFirstName());
        }
        else if(this.middleInit.compareTo(p.getMiddleInit()) !=0){
            return this.middleInit.compareTo(p.getMiddleInit());
        }
        else{
            return super.getId().compareTo(p.getId());
        }
    }

    public String getFullNameFirstLast(){
        return getFirstName() + " " + getLastName();
    }

    public String getFullNameLastFirst(){
        return getLastName() + ", " + getFirstName();
    }

    public List<Email> getEmailList() {
        return emailList;
    }

    public void setEmailList(List<Email> emailList) {
        this.emailList = emailList;
    }

    public List<Prospect> getProspectList() {
        return prospectList;
    }

    public void setProspectList(List<Prospect> prospectList) {
        this.prospectList = prospectList;
    }

}

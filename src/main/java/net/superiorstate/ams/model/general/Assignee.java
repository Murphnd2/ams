package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Assignee {
    @Id
    @GeneratedValue
    @Column(name="id")
    private Long id;

    @Column(name="full_name")
    private String fullName;

    @ManyToMany
    @JoinTable(name="assignee_links",joinColumns =@JoinColumn(name="assignee_id"),inverseJoinColumns = @JoinColumn(name="link_id"))
    List<WebLink> webLinkList;

    @ManyToMany
    @JoinTable(name="assignee_contacts",joinColumns = @JoinColumn(name="assignee_id"),inverseJoinColumns = @JoinColumn(name="person_id"))
    List<Person> assigneeContactList;



    public Assignee(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<WebLink> getWebLinkList() {
        return webLinkList;
    }

    public void setWebLinkList(List<WebLink> webLinkList) {
        this.webLinkList = webLinkList;
    }
    public void addWebLink(WebLink webLink){
        this.webLinkList.add(webLink);
        webLink.getListOfAssigneesWithThisWebLink().add(this);
    }

    public void removeWebLink(WebLink webLink){
        this.webLinkList.remove(webLink);
        webLink.getListOfAssigneesWithThisWebLink().remove(this);
    }

    public void addAssigneeContact(Person person){
        this.assigneeContactList.add(person);
        person.getListOfAssigneesWithThePerson().add(this);
    }

    public void removeAssigneeContact(Person person){
        this.assigneeContactList.remove(person);
        person.getListOfAssigneesWithThePerson().remove(this);
    }

    public List<Person> getAssigneeContactList() {
        return assigneeContactList;
    }

    public void setAssigneeContactList(List<Person> assigneeContactList) {
        this.assigneeContactList = assigneeContactList;
    }
}

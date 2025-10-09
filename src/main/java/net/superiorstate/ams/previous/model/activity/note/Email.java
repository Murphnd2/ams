package net.superiorstate.ams.previous.model.activity.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.util.List;

@Entity
public class Email extends Note {
    @Column(name="subject")
    private String subject;
    @ManyToMany(mappedBy = "emailList")
    private List<Person> recipientList;

    @OneToMany(mappedBy = "email")
    private List<WebLink> webLinkList;

    public Email(){}

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Person> getRecipientList() {
        return recipientList;
    }

    public void setRecipientList(List<Person> recipientList) {
        this.recipientList = recipientList;
    }

    public List<WebLink> getWebLinkList() {
        return webLinkList;
    }

    public void setWebLinkList(List<WebLink> webLinkList) {
        this.webLinkList = webLinkList;
    }

    public void addRecipient(Person p){
        this.recipientList.add(p);
        p.getEmailList().add(this);
    }

    public void removeRecipient(Person p){
        this.recipientList.remove(p);
        p.getEmailList().remove(this);
    }

}

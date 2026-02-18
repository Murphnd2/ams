package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

@Entity
public class Recipient extends Assignee {
    @Column(name="email_address")
    private String emailAddress;


    public Recipient(){}

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}

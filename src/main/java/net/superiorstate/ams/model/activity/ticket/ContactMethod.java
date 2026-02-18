package net.superiorstate.ams.model.activity.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ContactMethod {
    @Id
    @Column(name="method_id")
    private int id;

    @Column(name="description")
    private String description;

    public ContactMethod(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

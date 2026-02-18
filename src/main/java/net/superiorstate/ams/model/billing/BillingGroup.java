package net.superiorstate.ams.model.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BillingGroup {
    @Id
    @Column(name="billing_group_id")
    private int id;

    @Column(name="description")
    private String description;

    public BillingGroup(){}

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

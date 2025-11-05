package net.superiorstate.ams.previous.model.activity.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class ReasonCreated {

    @Id
    @GeneratedValue
    @Column(name = "use_id")
    private int id;

    @Column(columnDefinition = "varchar(100)")
    private String description;

    @Column(name="outbound")
    private boolean outbound;

    public ReasonCreated(){}

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

    public boolean isOutbound() {
        return outbound;
    }

    public void setOutbound(boolean outbound) {
        this.outbound = outbound;
    }
}

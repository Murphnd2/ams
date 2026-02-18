package net.superiorstate.ams.model.activity.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ActivityStatus {
    @Id
    @Column(name="status_id")
    private int id;

    @Column(name="description")
    private String description;

    public ActivityStatus(){}

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

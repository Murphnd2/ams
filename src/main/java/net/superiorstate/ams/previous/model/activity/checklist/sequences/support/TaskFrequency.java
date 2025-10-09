package net.superiorstate.ams.previous.model.activity.checklist.sequences.support;

import jakarta.persistence.*;

@Entity
public class TaskFrequency {
    @Id
    @Column(name="frequency_id")
    private int id;

    @Column(name="description", columnDefinition = "varchar(100)")
    private String description;

    public TaskFrequency(){}

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

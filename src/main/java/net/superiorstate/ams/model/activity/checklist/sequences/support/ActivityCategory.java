package net.superiorstate.ams.model.activity.checklist.sequences.support;

import jakarta.persistence.*;

@Entity
@Table(name = "templategroup")
public class ActivityCategory {
    @Id
    @GeneratedValue
    @Column(name="group_id")
    private int id;

    @Column(name="description",columnDefinition = "varchar(200)")
    private String description;

    public ActivityCategory(){}

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

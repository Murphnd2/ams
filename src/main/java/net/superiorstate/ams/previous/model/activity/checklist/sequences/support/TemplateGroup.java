package net.superiorstate.ams.previous.model.activity.checklist.sequences.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class TemplateGroup {
    @Id
    @GeneratedValue
    @Column(name="group_id")
    private int id;

    @Column(name="description",columnDefinition = "varchar(200)")
    private String description;

    public TemplateGroup(){}

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

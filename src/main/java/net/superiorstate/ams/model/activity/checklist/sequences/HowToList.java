package net.superiorstate.ams.model.activity.checklist.sequences;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class HowToList extends TaskSequence {

    @Column(name="unique_id")
    private String uniqueId;

    public HowToList(){}

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}

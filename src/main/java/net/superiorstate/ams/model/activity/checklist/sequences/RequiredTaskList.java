package net.superiorstate.ams.model.activity.checklist.sequences;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;

@Entity
public class RequiredTaskList extends TaskSequence {

    @OneToOne
    @JoinColumn(name="purpose_id")
    private TemplatePurpose templatePurpose;

    public RequiredTaskList(){}

    public TemplatePurpose getTemplatePurpose() {
        return templatePurpose;
    }

    public void setTemplatePurpose(TemplatePurpose templatePurpose) {
        this.templatePurpose = templatePurpose;
    }
}

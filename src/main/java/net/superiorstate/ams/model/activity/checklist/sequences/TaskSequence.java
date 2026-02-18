package net.superiorstate.ams.model.activity.checklist.sequences;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.general.PSP;

import java.util.List;

@Entity
public abstract class TaskSequence {
    @Id
    @GeneratedValue
    @Column(name="sequence_id")
    private Long id;

    @Column(name="description",columnDefinition = "varchar(200)")
    private String description;


    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @Column(name="is_inactive")
    private boolean inActive;

    @OneToMany(mappedBy = "taskSequence")
    List<TaskSequenceTable> taskSequenceTableList;

    public TaskSequence(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isInActive() {
        return inActive;
    }

    public void setInActive(boolean inActive) {
        this.inActive = inActive;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public List<TaskSequenceTable> getTaskSequenceTableList() {
        return taskSequenceTableList;
    }

    public void setTaskSequenceTableList(List<TaskSequenceTable> taskSequenceTableList) {
        this.taskSequenceTableList = taskSequenceTableList;
    }
}

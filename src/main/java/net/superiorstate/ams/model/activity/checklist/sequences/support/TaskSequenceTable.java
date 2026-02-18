package net.superiorstate.ams.model.activity.checklist.sequences.support;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;

@Entity
public class TaskSequenceTable {
    @EmbeddedId
    private TaskSequenceID taskSequenceID;

    @Id
    @ManyToOne
    @MapsId("taskId")
    @JoinColumn(name="task_id")
    private Task task;

    @Id
    @ManyToOne
    @MapsId("taskSequenceId")
    @JoinColumn(name="sequence_id")
    private TaskSequence taskSequence;

    @Column(name="sort_order")
    private int sortOrder;

    public TaskSequenceTable(){}

    public TaskSequenceID getTaskSequenceID() {
        return taskSequenceID;
    }

    public void setTaskSequenceID(TaskSequenceID taskSequenceID) {
        this.taskSequenceID = taskSequenceID;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public TaskSequence getTaskSequence() {
        return taskSequence;
    }

    public void setTaskSequence(TaskSequence taskSequence) {
        this.taskSequence = taskSequence;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getCode(){
        return getTaskSequence().getId().toString() + "-" + getTask().getId().toString();
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}

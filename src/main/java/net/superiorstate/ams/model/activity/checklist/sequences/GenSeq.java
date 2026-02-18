package net.superiorstate.ams.model.activity.checklist.sequences;

import net.superiorstate.ams.model.activity.checklist.tasks.Task;

public class GenSeq {
    private Task task;
    private String description;
    private int sequenceNumber;
    private boolean publicTask;

    public GenSeq(){}

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public boolean isPublicTask() {
        return publicTask;
    }

    public void setPublicTask(boolean publicTask) {
        this.publicTask = publicTask;
    }
}

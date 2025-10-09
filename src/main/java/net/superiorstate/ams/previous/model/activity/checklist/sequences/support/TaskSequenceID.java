package net.superiorstate.ams.previous.model.activity.checklist.sequences.support;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskSequenceID implements Serializable {
    private Long taskId;
    private Long taskSequenceId;

    public TaskSequenceID(){}

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTaskSequenceId() {
        return taskSequenceId;
    }

    public void setTaskSequenceId(Long taskSequenceId) {
        this.taskSequenceId = taskSequenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskSequenceID)) return false;
        TaskSequenceID that = (TaskSequenceID) o;
        return getTaskId().equals(that.getTaskId()) && getTaskSequenceId().equals(that.getTaskSequenceId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaskId(), getTaskSequenceId());
    }
}

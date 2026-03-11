package net.superiorstate.ams.model.activity.checklist.sequences.support;

import net.superiorstate.ams.model.activity.checklist.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for displaying a task in the composite ordering view.
 * Carries the task, its composite sort position, and the names of all sequences it belongs to.
 */
public class CompositeTaskView {

    private Task task;
    private int compositeSortOrder;
    private List<String> sequenceNames;
    private boolean reusable;

    public CompositeTaskView(Task task, int compositeSortOrder, boolean reusable) {
        this.task = task;
        this.compositeSortOrder = compositeSortOrder;
        this.reusable = reusable;
        this.sequenceNames = new ArrayList<>();
    }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public int getCompositeSortOrder() { return compositeSortOrder; }
    public void setCompositeSortOrder(int compositeSortOrder) { this.compositeSortOrder = compositeSortOrder; }

    public List<String> getSequenceNames() { return sequenceNames; }
    public void setSequenceNames(List<String> sequenceNames) { this.sequenceNames = sequenceNames; }

    public void addSequenceName(String name) { this.sequenceNames.add(name); }

    public boolean isReusable() { return reusable; }
    public void setReusable(boolean reusable) { this.reusable = reusable; }

    /** Whether this task has a saved composite position (vs newly added/unordered) */
    public boolean isOrdered() { return compositeSortOrder >= 0; }
}

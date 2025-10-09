package net.superiorstate.ams.previous.model.activity.checklist.tasks;

public class SortedTask {
    private Task task;
    private int sortOrder;

    public SortedTask(Task task, int sortOrder){
        this.task = task;
        this.sortOrder = sortOrder;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}

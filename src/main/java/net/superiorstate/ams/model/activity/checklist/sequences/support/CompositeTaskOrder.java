package net.superiorstate.ams.model.activity.checklist.sequences.support;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.general.PSP;

@Entity
@Table(name = "composite_task_order")
public class CompositeTaskOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "psp_id")
    private PSP psp;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private ActivityCategory activityCategory;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "sort_order")
    private int sortOrder;

    public CompositeTaskOrder() {}

    public CompositeTaskOrder(PSP psp, ActivityCategory activityCategory, Task task, int sortOrder) {
        this.psp = psp;
        this.activityCategory = activityCategory;
        this.task = task;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }

    public ActivityCategory getActivityCategory() { return activityCategory; }
    public void setActivityCategory(ActivityCategory activityCategory) { this.activityCategory = activityCategory; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}

package net.superiorstate.ams.model.activity.checklist.tasks;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.general.Person;

import java.sql.Date;
import java.util.UUID;

@Entity
public class ToDo {

    @Id
    @GeneratedValue
    @Column(name="todo_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="task_id")
    private Task task;

    @Column(name="sort_order")
    private int sortOrder;

    @Column(name="is_complete")
    private boolean isComplete;

    @Column(name="date_completed")
    private Date dateCompleted;

    @ManyToOne
    @JoinColumn(name="completed_by_id")
    private Person completedBy;

    @ManyToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    // --- BPO fields ---

    @Column(name="todo_guid", nullable=false, length=36)
    private String todoGuid;

    @Column(name="bpo_completed")
    private boolean bpoCompleted;

    @Column(name="bpo_completed_date")
    private Date bpoCompletedDate;

    @ManyToOne
    @JoinColumn(name="bpo_completed_by_id")
    private Person bpoCompletedBy;

    @Column(name="is_reverted")
    private boolean isReverted;

    @ManyToOne
    @JoinColumn(name="bpo_assigned_to_id")
    private Person bpoAssignedTo;

    public ToDo(){}

    @PrePersist
    private void generateGuid() {
        if (this.todoGuid == null) {
            this.todoGuid = UUID.randomUUID().toString();
        }
    }

    // --- Original getters/setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public Person getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Person completedBy) {
        this.completedBy = completedBy;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public void setCheckList(CheckList checkList) {
        this.checkList = checkList;
    }

    // --- BPO getters/setters ---

    public String getTodoGuid() {
        return todoGuid;
    }

    public void setTodoGuid(String todoGuid) {
        this.todoGuid = todoGuid;
    }

    public boolean isBpoCompleted() {
        return bpoCompleted;
    }

    public void setBpoCompleted(boolean bpoCompleted) {
        this.bpoCompleted = bpoCompleted;
    }

    public Date getBpoCompletedDate() {
        return bpoCompletedDate;
    }

    public void setBpoCompletedDate(Date bpoCompletedDate) {
        this.bpoCompletedDate = bpoCompletedDate;
    }

    public Person getBpoCompletedBy() {
        return bpoCompletedBy;
    }

    public void setBpoCompletedBy(Person bpoCompletedBy) {
        this.bpoCompletedBy = bpoCompletedBy;
    }

    public boolean isReverted() {
        return isReverted;
    }

    public void setReverted(boolean reverted) {
        isReverted = reverted;
    }

    public Person getBpoAssignedTo() {
        return bpoAssignedTo;
    }

    public void setBpoAssignedTo(Person bpoAssignedTo) {
        this.bpoAssignedTo = bpoAssignedTo;
    }
}

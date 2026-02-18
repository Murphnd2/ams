package net.superiorstate.ams.model.activity.checklist.tasks;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.general.Person;

import java.sql.Date;

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

    public ToDo(){}

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


}

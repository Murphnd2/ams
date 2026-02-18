package net.superiorstate.ams.model.activity.checklist.sequences;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.activity.checklist.CheckList;

import java.sql.Date;
import java.util.List;

@Entity
public class RecurringTaskList extends TaskSequence {
    @ManyToOne
    @JoinColumn(name="user_id")
    private Person assignee;

    @ManyToOne
    @JoinColumn(name="frequency_id")
    private TaskFrequency taskFrequency;

    @Column(name="days_in_advance")
    private int daysInAdvance;

    @OneToMany(mappedBy = "recurringTaskList")
    private List<CheckList> checkLists;

    @Column(name="date_start")
    private Date dateStart;

    @ManyToMany
    @JoinTable(name="days_of_week",joinColumns =@JoinColumn(name="item_id"),inverseJoinColumns = @JoinColumn(name="dow_id"))
    List<DoW> doWList;

    public RecurringTaskList(){}

    public Person getAssignee() {
        return assignee;
    }

    public void setAssignee(Person assignee) {
        this.assignee = assignee;
    }

    public TaskFrequency getTaskFrequency() {
        return taskFrequency;
    }

    public void setTaskFrequency(TaskFrequency taskFrequency) {
        this.taskFrequency = taskFrequency;
    }

    public int getDaysInAdvance() {
        return daysInAdvance;
    }

    public void setDaysInAdvance(int daysInAdvance) {
        this.daysInAdvance = daysInAdvance;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public List<DoW> getDoWList() {
        return doWList;
    }

    public void setDoWList(List<DoW> doWList) {
        this.doWList = doWList;
    }

    public List<CheckList> getCheckLists() {
        return checkLists;
    }

    public void setCheckLists(List<CheckList> checkLists) {
        this.checkLists = checkLists;
    }


    public void addDayOfWeek(DoW doW){
        this.getDoWList().add(doW);
        doW.getRecurringTaskListList().add(this);
    }

    public void removeDayOfWeek(DoW doW){
        this.getDoWList().remove(doW);
        doW.getRecurringTaskListList().remove(this);
    }
}

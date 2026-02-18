package net.superiorstate.ams.model.activity.checklist;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.general.Assignee;

import java.sql.Date;
import java.util.List;

@Entity
public class RecurringItems {
    @Id
    @GeneratedValue
    @Column(name="recurring_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="assignee_id")
    private Assignee assignee;

    @ManyToOne
    @JoinColumn(name="sequence_id")
    private TaskSequence taskSequence;

    @ManyToOne
    @JoinColumn(name="frequency_id")
    private TaskFrequency taskFrequency;

    @Column(name="is_inactive")
    private boolean isInactive;

    @Column(name="days_in_advance")
    private int daysInAdvance;

    @Column
    private Date dateOne;

    @Column
    private Date dateTwo;

    @Column(name="date_start")
    private Date dateStart;

    @ManyToMany
    @JoinTable(name="recurring_days",joinColumns =@JoinColumn(name="item_id"),inverseJoinColumns = @JoinColumn(name="dow_id"))
    List<DoW> doWList;

    public RecurringItems (){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Assignee getAssignee() {
        return assignee;
    }

    public void setAssignee(Assignee assignee) {
        this.assignee = assignee;
    }

    public TaskSequence getTaskSequence() {
        return taskSequence;
    }

    public void setTaskSequence(TaskSequence taskSequence) {
        this.taskSequence = taskSequence;
    }

    public TaskFrequency getTaskFrequency() {
        return taskFrequency;
    }

    public void setTaskFrequency(TaskFrequency taskFrequency) {
        this.taskFrequency = taskFrequency;
    }

    public boolean isInactive() {
        return isInactive;
    }

    public void setInactive(boolean inactive) {
        isInactive = inactive;
    }

    public Date getDateOne() {
        return dateOne;
    }

    public void setDateOne(Date dateOne) {
        this.dateOne = dateOne;
    }

    public Date getDateTwo() {
        return dateTwo;
    }

    public void setDateTwo(Date dateTwo) {
        this.dateTwo = dateTwo;
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

    public void addDoW(DoW doW){
        this.doWList.add(doW);
        doW.getRecurringItemsList().add(this);
    }

    public void removeDoW(DoW doW){
        this.doWList.remove(doW);
        doW.getRecurringItemsList().remove(this);
    }
}

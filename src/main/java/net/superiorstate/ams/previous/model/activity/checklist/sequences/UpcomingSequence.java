package net.superiorstate.ams.previous.model.activity.checklist.sequences;

import net.superiorstate.ams.previous.model.general.Person;

import java.sql.Date;

public class UpcomingSequence {
    private RecurringTaskList recurringTaskList;
    private Date lastPerformed;
    private Date nextDue;
    private Date nextBegin;
    private Person assignedTo;

    public UpcomingSequence(RecurringTaskList rtl, Date lastPerformed, Date nextDue, Date nextBegin, Person assignedTo){
        this.recurringTaskList = rtl;
        this.lastPerformed = lastPerformed;
        this.nextBegin = nextBegin;
        this.nextDue = nextDue;
        this.assignedTo = assignedTo;
    }

    public UpcomingSequence(){}

    public RecurringTaskList getRecurringTaskList() {
        return recurringTaskList;
    }

    public void setRecurringTaskList(RecurringTaskList recurringTaskList) {
        this.recurringTaskList = recurringTaskList;
    }

    public Date getLastPerformed() {
        return lastPerformed;
    }

    public void setLastPerformed(Date lastPerformed) {
        this.lastPerformed = lastPerformed;
    }

    public Date getNextDue() {
        return nextDue;
    }

    public void setNextDue(Date nextDue) {
        this.nextDue = nextDue;
    }

    public Date getNextBegin() {
        return nextBegin;
    }

    public void setNextBegin(Date nextBegin) {
        this.nextBegin = nextBegin;
    }

    public Person getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Person assignedTo) {
        this.assignedTo = assignedTo;
    }
}

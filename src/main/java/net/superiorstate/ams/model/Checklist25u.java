package net.superiorstate.ams.model;

import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.general.Person;

import java.sql.Date;

public class Checklist25u {
    private Activity activity;
    private String name;
    private Date dueDate;
    private Person owner;
    private boolean isComplete;
    private boolean singleTasked;
    private int daysAheadAllowed;
    private int sortKey;

    private RecurringTaskList recurringTaskList;

    public Checklist25u(Checklist25 c){
        setActivity(c.getActivity());
        setName(c.getName());
        setDueDate(c.getDueDate());
        setOwner(c.getOwner());
        setComplete(c.isComplete());
        setSingleTasked(c.isSingleTasked());
        setDaysAheadAllowed(c.getDaysAheadAllowed());
        setSortKey(c.getSortKey());
        CheckList c1 = (CheckList) getActivity();
        if(c1.getRecurringTaskList()!=null)
            setRecurringTaskList(c1.getRecurringTaskList());
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public boolean isSingleTasked() {
        return singleTasked;
    }

    public void setSingleTasked(boolean singleTasked) {
        this.singleTasked = singleTasked;
    }

    public int getDaysAheadAllowed() {
        return daysAheadAllowed;
    }

    public void setDaysAheadAllowed(int daysAheadAllowed) {
        this.daysAheadAllowed = daysAheadAllowed;
    }

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    public RecurringTaskList getRecurringTaskList() {
        return recurringTaskList;
    }

    public void setRecurringTaskList(RecurringTaskList recurringTaskList) {
        this.recurringTaskList = recurringTaskList;
    }


}

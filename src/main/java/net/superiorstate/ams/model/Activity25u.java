package net.superiorstate.ams.model;

import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Person;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;
import java.time.LocalDate;

public class Activity25u implements Comparable<Activity25u> {
    private Activity activity;
    private String dType;
    private String name;
    private Person assignedTo;

    private boolean isDelegated;
    private Date dueDate;
    private boolean waitingOnUs;
    private int daysSinceContact;

    public Activity25u(){}

    public Activity25u(Activity25 a){
        setActivity(a.getActivity());
        setdType(a.getDtype());
        setName(a.getName());
        setAssignedTo(a.getAssignedTo());
        setDueDate(a.getDueDate());
        setWaitingOnUs(a.isWaitingOnUs());
        setDaysSinceContact(a.getDaysSinceContact());
    }

    public Activity25u(Activity25u a){
        setActivity(a.getActivity());
        setdType(a.getdType());
        setName(a.getName());
        setAssignedTo(a.getAssignedTo());
        setDueDate(a.getDueDate());
        setWaitingOnUs(a.isWaitingOnUs());
        setDaysSinceContact(a.getDaysSinceContact());
        setDelegated(a.isDelegated());
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public String getdType() {
        return dType;
    }

    public void setdType(String dType) {
        this.dType = dType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Person assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isWaitingOnUs() {
        return waitingOnUs;
    }

    public void setWaitingOnUs(boolean waitingOnUs) {
        this.waitingOnUs = waitingOnUs;
    }

    public int getDaysSinceContact() {
        return daysSinceContact;
    }

    public void setDaysSinceContact(int daysSinceContact) {
        this.daysSinceContact = daysSinceContact;
    }

    public boolean isDelegated() {
        return isDelegated;
    }

    public void setDelegated(boolean delegated) {
        isDelegated = delegated;
    }
    public String getDateHtml(){
        String className;
        LocalDate dueMonth=LocalDate.of(getDueDate().toLocalDate().getYear(),getDueDate().toLocalDate().getMonthValue(),2);
        LocalDate today = LocalDate.now();
        LocalDate curMonth=LocalDate.of(today.getYear(),today.getMonthValue(),1);
        if(dueMonth.isAfter(curMonth.plusMonths(1L)))
            className = "btn btn-outline-secondary text-lowercase fw-lighter pe-none";
        else if(dueMonth.isAfter(curMonth))
            className = "btn btn-outline-dark pe-none";
        else if(dueMonth.isAfter(curMonth.minusMonths(1L)))
            className = "btn btn-warning text-dark pe-none";
        else className = "btn btn-danger fw-bold pe-none";
        return className;
    }

    @Override
    public int compareTo(@NotNull Activity25u o) {
        return this.getActivity().getFullName().compareTo(o.getActivity().getFullName());
    }

    @Override
    public String toString() {
        return getName();
    }
}

package net.superiorstate.ams.model.activity.checklist;

import net.superiorstate.ams.model.general.Person;

import java.sql.Date;

public class CheckListShell {
    private long id;
    private String name;
    private Person owner;
    private Date dueDate;
    private Date showDate;
    private Person delegate;
    private Person bpoUser;
    private int toDoCount;
    private boolean hasDelegate;

    public CheckListShell(){}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getShowDate() {
        return showDate;
    }

    public void setShowDate(Date showDate) {
        this.showDate = showDate;
    }

    public Person getDelegate() {
        return delegate;
    }

    public void setDelegate(Person delegate) {
        this.delegate = delegate;
    }

    public Person getBpoUser() {
        return bpoUser;
    }

    public void setBpoUser(Person bpoUser) {
        this.bpoUser = bpoUser;
    }

    public int getToDoCount() {
        return toDoCount;
    }

    public void setToDoCount(int toDoCount) {
        this.toDoCount = toDoCount;
    }

    public boolean hasDelegate() {
        return  hasDelegate;
    }

    public void setHasDelegate(boolean hasDelegate) {
        this.hasDelegate = hasDelegate;
    }
}

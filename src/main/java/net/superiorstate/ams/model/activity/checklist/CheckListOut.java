package net.superiorstate.ams.model.activity.checklist;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;

import java.sql.Date;

@Entity
@Table(name="ckx_open_checklists")
public class CheckListOut {
    @Id
    @Column(name="UID")
    private String uid;

    @ManyToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    @Column(name="full_name")
    private String name;

    @Column(name="due_date")
    private Date dueDate;

    @ManyToOne
    @JoinColumn(name="assigned_to_id")
    private Person assignedTo;

    @Column(name="days_in_advance")
    private int daysInAdvance;

    @Column(name="has_owner")
    private boolean hasOwner;

    @Column(name="is_sourced")
    private boolean isSourced;
    @ManyToOne
    @JoinColumn(name="owner_id")
    private Person delegatedEmployee;

    @ManyToOne
    @JoinColumn(name="source_owner")
    private Person bpoEmployee;

    @Column(name="show_date")
    private Date showDate;

    @Column(name="todo_count_f")
    private int toDoCount;

    public CheckListOut(){}

    public String getUid() {
        return uid;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public String getName() {
        return name;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Person getAssignedTo() {
        return assignedTo;
    }

    public int getDaysInAdvance() {
        return daysInAdvance;
    }

    public Person getDelegatedEmployee() {
        return delegatedEmployee;
    }

    public Person getBpoEmployee() {
        return bpoEmployee;
    }

    public Date getShowDate(){
        return showDate;
    }

    public boolean hasOwner() {
        return hasOwner;
    }

    public boolean isSourced() {
        return isSourced;
    }

    public int getToDoCount(){ return  toDoCount;}
}

package net.superiorstate.ams.model;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Person;

import java.sql.Date;

@Entity
@Table(name="a25_checklist_full")
public class Checklist25 {
    @Id
    @ManyToOne
    @JoinColumn(name="id")
    private Activity activity;

    @Column(name="full_name")
    private String name;

    @Column(name="due_date")
    private Date dueDate;

    @ManyToOne
    @JoinColumn(name = "assigned_to_id")
    private Person owner;

    @Column(name="is_complete")
    private boolean isComplete;

    @Column(name="single_task")
    private boolean singleTasked;

    @Column(name="days_ahead")
    private int daysAheadAllowed;

    @Column(name="sort_key")
    private int sortKey;

    public Checklist25(){}

    public Activity getActivity() {
        return activity;
    }

    public String getName() {
        return name;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Person getOwner() {
        return owner;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean isSingleTasked() {
        return singleTasked;
    }

    public int getDaysAheadAllowed() {
        return daysAheadAllowed;
    }

    public int getSortKey() {
        return sortKey;
    }
}

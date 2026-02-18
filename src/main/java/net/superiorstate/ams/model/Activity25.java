package net.superiorstate.ams.model;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Person;

import java.sql.Date;

@Entity
@Table(name="a25_activity_list_open")
public class Activity25 {
    @Id
    @ManyToOne
    @JoinColumn(name="id")
    private Activity activity;

    @Column(name="DTYPE")
    private String dType;

    @Column(name="full_name")
    private String name;

    @ManyToOne
    @JoinColumn(name="assigned_to_id")
    private Person assignedTo;

    @Column(name="due_date")
    private Date dueDate;

    @Column(name="on_us")
    private boolean waitingOnUs;

    @Column(name="days_since")
    private int daysSinceContact;

    public Activity25(){}

    public Activity getActivity() {
        return activity;
    }

    public String getDtype() {
        return dType;
    }

    public String getName() {
        return name;
    }

    public Person getAssignedTo() {
        return assignedTo;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public boolean isWaitingOnUs() {
        return waitingOnUs;
    }

    public int getDaysSinceContact() {
        return daysSinceContact;
    }
}

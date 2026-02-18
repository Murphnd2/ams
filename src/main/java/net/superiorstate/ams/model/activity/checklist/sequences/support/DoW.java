package net.superiorstate.ams.model.activity.checklist.sequences.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import net.superiorstate.ams.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.model.activity.checklist.RecurringItems;

import java.util.List;

@Entity
public class DoW {
    @Id
    @Column(name="dow_id")
    private int id;

    @Column(name="name")
    private String name;

    @Column(name="weekday_int")
    private int weekdayId;

    @ManyToMany(mappedBy="doWList")
    List<RecurringItems> recurringItemsList;


    @ManyToMany(mappedBy="doWList")
    List<RecurringTaskList> recurringTaskListList;

    public DoW(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeekdayId() {
        return weekdayId;
    }

    public void setWeekdayId(int weekdayId) {
        this.weekdayId = weekdayId;
    }

    public List<RecurringTaskList> getRecurringTaskListList() {
        return recurringTaskListList;
    }

    public void setRecurringTaskListList(List<RecurringTaskList> recurringTaskListList) {
        this.recurringTaskListList = recurringTaskListList;
    }

    public List<RecurringItems> getRecurringItemsList() {
        return recurringItemsList;
    }

    public void setRecurringItemsList(List<RecurringItems> recurringItemsList) {
        this.recurringItemsList = recurringItemsList;
    }
}

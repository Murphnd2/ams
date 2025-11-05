package net.superiorstate.ams.previous.model.activity.checklist;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;

import java.util.List;

@Entity
public class CheckList extends Activity {
    @OneToMany(mappedBy = "checkList")
    List<ToDo> toDoList;

    @ManyToOne
    @JoinColumn(name="recurring_list_id")
    private RecurringTaskList recurringTaskList;

    @OneToOne(mappedBy = "checkList")
    private Setup setup;

    @OneToOne(mappedBy = "checkList")
    private Renewal renewal;

    @OneToOne(mappedBy="checkList")
    private Ticket ticket;


    public CheckList(){}

    public List<ToDo> getToDoList() {
        return toDoList;
    }

    public void setToDoList(List<ToDo> toDoList) {
        this.toDoList = toDoList;
    }


    public RecurringTaskList getRecurringTaskList() {
        return recurringTaskList;
    }

    public void setRecurringTaskList(RecurringTaskList recurringTaskList) {
        this.recurringTaskList = recurringTaskList;
    }

    public Setup getSetup() {
        return setup;
    }

    public void setSetup(Setup setup) {
        this.setup = setup;
    }

    public Renewal getRenewal() {
        return renewal;
    }

    public void setRenewal(Renewal renewal) {
        this.renewal = renewal;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}

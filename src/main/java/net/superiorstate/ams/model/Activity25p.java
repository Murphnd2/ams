package net.superiorstate.ams.model;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.Person;

@Entity
@Table(name="a25_activity_list_participating")
public class Activity25p {
    @Id
    @ManyToOne
    @JoinColumn(name="id")
    private Activity activity;

    @ManyToOne
    @JoinColumn(name="todo_id")
    private ToDo toDo;

    @ManyToOne
    @JoinColumn(name="act_owner_id")
    private Person activityOwner;

    @ManyToOne
    @JoinColumn(name="tsk_owner_id")
    private Person taskOwner;

    public Activity25p(){}

    public Activity getActivity() {
        return activity;
    }

    public ToDo getToDo() {
        return toDo;
    }

    public Person getActivityOwner() {
        return activityOwner;
    }

    public Person getTaskOwner() {
        return taskOwner;
    }
}

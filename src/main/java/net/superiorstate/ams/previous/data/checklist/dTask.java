package net.superiorstate.ams.previous.data.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.Assignee;
import net.superiorstate.ams.previous.model.general.Person;

import java.util.ArrayList;
import java.util.List;

public abstract class dTask {
    public static List<Task> getMyTasks(EntityManager em, Person p){
        Query q = em.createQuery("SELECT t FROM Task t WHERE t.hasOwner=true AND (t.owner.id = :id OR t.sourceOwner.id = :id)");
        q.setParameter("id",p.getId());
        List<Task> taskList;
        try{
            taskList = (List<Task>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        return taskList;
    }

    public static List<ToDo> getMyToDos(EntityManager em, Person p){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.isComplete=false AND t.task.hasOwner = true AND (t.task.owner.id = :id OR t.task.sourceOwner.id = :id)");
        q.setParameter("id",p.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        return toDoList;
    }

    public static List<CheckList> getCheckListsWithToDos(EntityManager em, Person p){
        List<ToDo> myToDoList = getMyToDos(em,p);
        List<CheckList> checkLists = new ArrayList<>();
        for(ToDo t:myToDoList){
            CheckList c = t.getCheckList();
            if(!checkLists.contains(c))
                checkLists.add(c);
        }
        return checkLists;
    }
    public static List<Activity> getActivitiesWithToDos(EntityManager em, Person p){
        List<CheckList> checkLists = getCheckListsWithToDos(em,p);
        List<Activity> activityList = new ArrayList<>();
        for(CheckList c:checkLists){
            Assignee assignee = c.getAssignedTo();
            if(assignee.getClass().equals("Activity")){
                Activity a = dM.getActivityById(em,c.getAssignedTo().getId());
                if(a!=null && !activityList.contains(a))
                    activityList.add(a);
            }
        }
        return activityList;
    }
}

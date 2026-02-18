package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class ChecklistDAO {

    public static List<TaskFrequency> getTaskFrequencies(EntityManager em){
        Query q = em.createQuery("SELECT tf FROM TaskFrequency tf order by tf.id");
        List<TaskFrequency> taskFrequencyList;
        try{
            taskFrequencyList = (List<TaskFrequency>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskFrequencyList = new ArrayList<>();
        }
        return taskFrequencyList;
    }

    public static List<ToDo> getToDoListByChecklistId(EntityManager em, Long id){
        Query q = em.createQuery("SELECT t FROM ToDo t JOIN FETCH t.task task WHERE t.checkList.id = :id ORDER BY t.isComplete, t.sortOrder");
        q.setParameter("id",id);
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            toDoList = new ArrayList<>();
        }
        return toDoList;
    }
    public static void createToDoList(EntityManager em, Ticket t, CheckList c){
        List<SortedTask> sortedTaskList = TicketQueryDAO.getTasksRequiredForTicket(em,t);
        if(sortedTaskList.size()==0)
            sortedTaskList.add(new SortedTask(EntityLookup.getTaskById(em,129L),1000));
        for(SortedTask st: sortedTaskList){
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(false);
            if(st.getTask().getId()==129L)
                toDo.setComplete(true);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            assert c != null;
            CheckList checkList = EntityLookup.getCheckListById(em,c.getId());
            assert checkList != null;
            checkList.getToDoList().add(toDo);
            em.persist(checkList);
            em.getTransaction().commit();
        }
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public abstract class RequiredTaskDAO {

    public static List<Task> getTasksNotInSequence(EntityManager em, TaskSequence ts){
        List<TaskSequenceTable> taskSequenceTableList;
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id");
        q.setParameter("id",ts.getId());
        try{
            taskSequenceTableList = (List<TaskSequenceTable>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskSequenceTableList = new ArrayList<>();
            System.out.println("No Tasks in Sequence");
        }

        List<Task> tasksInSequence = new ArrayList<>();
        for(TaskSequenceTable tst: taskSequenceTableList)
            tasksInSequence.add(tst.getTask());

        List<Task> fullTaskList;
        Query q2 = em.createQuery("SELECT t FROM Task t WHERE t.psp.id = :id AND t.reUsable=true ORDER BY t.description");
        q2.setParameter("id",ts.getPsp().getId());
        try{
            fullTaskList = (List<Task>) q2.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            fullTaskList = new ArrayList<>();
        }

        List<Task> remainingTaskList = new ArrayList<>();
        for(Task t:fullTaskList){
            if(!tasksInSequence.contains(t))
                remainingTaskList.add(t);
        }
        return remainingTaskList;
    }

    public static RequiredTaskList getRtlForPurpose(EntityManager em, TemplatePurpose tp){
        RequiredTaskList rtl;
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.templatePurpose.id = :id");
        q.setParameter("id",tp.getId());
        try{
            rtl = (RequiredTaskList) q.getSingleResult();
        } catch (NoResultException e){
            rtl = new RequiredTaskList();
            rtl.setId(-1L);
        }
        return rtl;
    }

    public static boolean listExistsForPurpose(EntityManager em, TemplatePurpose tp){
        boolean itExists = getRtlForPurpose(em, tp).getId() != -1L;
        return itExists;
    }

    public static void removeTaskFromSequence(EntityManager em, TaskSequence rtl, Task t){
        TaskSequenceTable taskSequenceTable;
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :seqId and tst.task.id = :taskId");
        q.setParameter("taskId",t.getId());
        q.setParameter("seqId", rtl.getId());
        try{
            taskSequenceTable = (TaskSequenceTable) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            return;
        }
        em.getTransaction().begin();
        em.remove(taskSequenceTable);
        em.getTransaction().commit();
    }

    public static void removeTaskFromSequence(EntityManager em, TaskSequenceTable tst){
        removeTaskFromSequence(em,tst.getTaskSequence(),tst.getTask());
    }

    public static List<TaskSequenceTable> getTasksForSequence(EntityManager em, TaskSequence ts){
        List<TaskSequenceTable> taskList;
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id ORDER BY tst.sortOrder");
        q.setParameter("id",ts.getId());
        try{
            taskList = (List<TaskSequenceTable>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskList = new ArrayList<>();
        }
        return taskList;
    }

    public static TaskSequenceTable getTaskSequenceTableByIds(EntityManager em, TaskSequence ts, Task t){
        return getTaskSequenceTableByIds(em,ts.getId(),t.getId());
    }

    public static TaskSequenceTable getTaskSequenceTableByIds(EntityManager em, Long sequenceId, Long taskId ){
        TaskSequenceTable tst;
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :seqId AND tst.task.id = :taskId");
        q.setParameter("seqId",sequenceId);
        q.setParameter("taskId",taskId);
        try{
            tst = (TaskSequenceTable) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            tst = new TaskSequenceTable();
        }
        return tst;
    }

    public static void moveTaskUp(EntityManager em, TaskSequence ts, Task t){
        List<TaskSequenceTable> taskSequenceTableList = getTasksForSequence(em,ts);
        TaskSequenceTable tst = getTaskSequenceTableByIds(em,ts,t);
        int index = taskSequenceTableList.indexOf(tst);
        if(index!=0){
            int iAbove = index-1;
            TaskSequenceTable tstAbove = taskSequenceTableList.get(iAbove);
            int sortOrderAbove = tstAbove.getSortOrder();
            int currentSortOrder = tst.getSortOrder();
            em.getTransaction().begin();
            TaskSequenceTable ta = getTaskSequenceTableByIds(em,ts,tstAbove.getTask());
            TaskSequenceTable tc = getTaskSequenceTableByIds(em,ts,tst.getTask());
            ta.setSortOrder(currentSortOrder);
            tc.setSortOrder(sortOrderAbove);
            em.persist(ta);
            em.persist(tc);
            em.getTransaction().commit();
        }
    }

    public static void moveTaskDown(EntityManager em, TaskSequence ts, Task t){
        List<TaskSequenceTable> taskSequenceTableList = getTasksForSequence(em,ts);
        TaskSequenceTable tst = getTaskSequenceTableByIds(em,ts,t);
        int index = taskSequenceTableList.indexOf(tst);
        if(index!=taskSequenceTableList.size()-1){
            int iBelow = index+1;
            TaskSequenceTable tstBelow = taskSequenceTableList.get(iBelow);
            int sortOrderBelow = tstBelow.getSortOrder();
            int currentSortOrder = tst.getSortOrder();
            em.getTransaction().begin();
            TaskSequenceTable tb = getTaskSequenceTableByIds(em,ts,tstBelow.getTask());
            TaskSequenceTable tc = getTaskSequenceTableByIds(em,ts,tst.getTask());
            tb.setSortOrder(currentSortOrder);
            tc.setSortOrder(sortOrderBelow);
            em.persist(tb);
            em.persist(tc);
            em.getTransaction().commit();
        }
    }
}

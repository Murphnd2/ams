package net.superiorstate.ams.previous.data.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class dbCheck {
    private static ToDo createToDo(Task task, CheckList c, int so){
        ToDo t = new ToDo();
        t.setTask(task);
        t.setCheckList(c);
        t.setComplete(false);
        t.setSortOrder(so);
        return t;
    }

    public static CheckList createAdminOnlyChecklist(EntityManager em, String name, List<Task> taskList, Person creator){
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setLoggedBy(creator);
        c.setAssignedTo(creator);
        c.setFullName(name);
        c.setComplete(false);
        c.setDueDate(Date.valueOf(LocalDate.now()));
        em.persist(c);
        em.getTransaction().commit();
        int x = 0;
        for(Task t: taskList){
            x+=10;
            em.getTransaction().begin();
            ToDo toDo = createToDo(t,c,x);
            em.persist(toDo);
            em.getTransaction().commit();
            em.getTransaction().begin();
            c.getToDoList().add(toDo);
            em.persist(c);
            em.getTransaction().commit();
        }
        return c;
    }

    // ************************* REQUIRED TASK LIST METHODS ********************************
    public static RequiredTaskList getRequiredTaskListById(EntityManager em, Long id){
        RequiredTaskList rtl;
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.id = :id");
        q.setParameter("id",id);
        try{
            rtl = (RequiredTaskList) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            rtl = new RequiredTaskList();
        }
        return rtl;
    }

    // ********************************* CHECKLIST BUILDER TOOLS ******************************************

    private static List<SortedTask> getTasksForSequence(EntityManager em, TaskSequence taskSequence){
        List<TaskSequenceTable> taskSequenceTableList;
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id");
        q.setParameter("id",taskSequence.getId());
        try{
            taskSequenceTableList = (List<TaskSequenceTable>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskSequenceTableList = new ArrayList<>();
        }
        List<SortedTask> sortedTaskList = new ArrayList<>();
        List<Task> taskList = new ArrayList<>();
        for(TaskSequenceTable tst: taskSequenceTableList){
            if(!taskList.contains(tst.getTask())) {
                taskList.add(tst.getTask());
                SortedTask sortedTask = new SortedTask(tst.getTask(),tst.getSortOrder());
                sortedTaskList.add(sortedTask);
            }
        }
        return sortedTaskList;
    }

    public static List<SortedTask> getTasksForRequiredItem(EntityManager em, RequiredTaskList requiredTaskList){
        return getTasksForSequence(em, requiredTaskList);
    }

    public static List<CheckList> getMyChecklists(EntityManager em, User user){
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :userId AND c.dueDate <= :theDate and c.isComplete=false order by c.isComplete,c.dueDate");
        q.setParameter("userId",user.getPerson().getId());
        q.setParameter("theDate",Date.valueOf(LocalDate.now()));
        List<CheckList> myChecklists;
        try{
            myChecklists = (List<CheckList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            myChecklists = new ArrayList<>();
        }
        Query query = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :userId AND c.isComplete=false AND c.recurringTaskList.id >:tlid AND c.dueDate>:theDate AND c.dueDate < :farDate ORDER BY c.dueDate");
        query.setParameter("userId",user.getPerson().getId());
        query.setParameter("theDate",Date.valueOf(LocalDate.now()));
        query.setParameter("farDate",Date.valueOf(LocalDate.now().plusDays(14L)));
        query.setParameter("tlid",0);
        List<CheckList> onesToCheck;
        try{
            onesToCheck = (List<CheckList>) query.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            onesToCheck = new ArrayList<>();
        }
        for(CheckList c:onesToCheck){
            int daysInAdvance = c.getRecurringTaskList().getDaysInAdvance();
            Date dueDate = c.getDueDate();
            Date today = Date.valueOf(LocalDate.now());
            Date compareDate = Date.valueOf(today.toLocalDate().plusDays(daysInAdvance));
            if(dueDate.compareTo(compareDate)<=0)
                myChecklists.add(c);
        }

        Query q2 = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :id AND c.isComplete = true AND c.dateCompleted = :today order by c.dueDate");
        q2.setParameter("id",user.getPerson().getId());
        q2.setParameter("today",Date.valueOf(LocalDate.now()));
        List<CheckList> closedTodayList;
        try{
            closedTodayList = (List<CheckList>) q2.getResultList();
        }catch (NoResultException e){
            e.printStackTrace();
            closedTodayList = new ArrayList<>();
        }
        for(CheckList cl: closedTodayList)
            if(!myChecklists.contains(cl))
                myChecklists.add(cl);
        for(CheckList cList:myChecklists)
            refreshToDoLists(em,cList);
        return myChecklists;
    }

    private static void refreshToDoLists(EntityManager em, CheckList c){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId order by t.sortOrder, t.task.description");
        q.setParameter("cId",c.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            toDoList = new ArrayList<>();
        }
        em.getTransaction().begin();
        CheckList checkList = dM.getCheckListById(em,c.getId());
        checkList.setToDoList(toDoList);
        em.persist(checkList);
        em.getTransaction().commit();

    }
    public static List<CheckList> getRemainingChecklists(EntityManager em, User user){
        List<CheckList> myOpenChecklists;
        List<CheckList> remainingChecklists = new ArrayList<>();
        List<CheckList> myCurrentChecklists = getMyChecklists(em,user);
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.isComplete = false AND c.assignedTo.id = :id order by c.dueDate");
        q.setParameter("id",user.getPerson().getId());
        try{
            myOpenChecklists = (List<CheckList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            return remainingChecklists;
        }
        for(CheckList c:myOpenChecklists){
            if(!myCurrentChecklists.contains(c))
                remainingChecklists.add(c);
        }
        return remainingChecklists;
    }

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

}

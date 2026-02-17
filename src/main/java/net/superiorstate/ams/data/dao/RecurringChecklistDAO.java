package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.UpcomingSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.general.UserRole;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class RecurringChecklistDAO {

    public static void removeFutureRecurring(EntityManager em, CheckList cl){
        if(cl.getRecurringTaskList()==null || cl.getRecurringTaskList().getId()<=0)
            return;
        RecurringTaskList rtl = getRecurringListById(em,cl.getRecurringTaskList().getId());
        if(rtl==null)
            return;
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.recurringTaskList.id = :id AND c.isComplete = false AND c.id > :cid");
        q.setParameter("id",rtl.getId());
        q.setParameter("cid",cl.getId());
        List<CheckList> listOfFutureCheckLists;
        try{
            listOfFutureCheckLists = (List<CheckList>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(listOfFutureCheckLists == null)
            return;
        for(CheckList c: listOfFutureCheckLists){
            CheckList cUpdate = EntityLookup.getCheckListById(em,c.getId());

            if(cUpdate!=null){
                em.getTransaction().begin();
                cUpdate.setRecurringTaskList(null);
                cUpdate.setComplete(true);
                cUpdate.setFullName(c.getFullName() + "(*ERASED*)");
                cUpdate.setDateCompleted(Date.valueOf(LocalDate.now().minusDays(1)));
                cUpdate.setCompletedBy((Person) c.getAssignedTo());
                em.persist(cUpdate);
                em.getTransaction().commit();
            }

        }
    }
    public static CheckList createNewRecurringChecklist(EntityManager em, UpcomingSequence us, Person currentPerson){
        em.getTransaction().begin();
        RecurringTaskList rtl = RecurringChecklistDAO.getRecurringListById(em,us.getRecurringTaskList().getId());
        CheckList c = new CheckList();
        c.setRecurringTaskList(rtl);
        c.setLoggedBy(currentPerson);
        c.setDueDate(us.getNextDue());
        c.setAssignedTo(us.getAssignedTo());
        c.setFullName(rtl.getDescription());
        c.setComplete(false);
        em.persist(c);
        em.getTransaction().commit();

        List<TaskSequenceTable> rtlTaskList = rtl.getTaskSequenceTableList();
        for(TaskSequenceTable tst:rtlTaskList){
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setComplete(false);
            toDo.setTask(tst.getTask());
            toDo.setSortOrder(tst.getSortOrder());
            toDo.setCheckList(c);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            CheckList c1 = EntityLookup.getCheckListById(em,c.getId());
            if(c1!=null){
                c1.getToDoList().add(toDo);
                em.persist(c1);
            }
            em.getTransaction().commit();
        }
        return c;
    }

    public static UpcomingSequence getUpcomingSequence(EntityManager em, RecurringTaskList rtl){
        UpcomingSequence us = null;
        Date lastDone;
        Date dueNext;
        Date canStart;
        try{
            lastDone = getLastPerformed(em,rtl);
            dueNext = getNextDateDue(em,rtl.getTaskFrequency(),lastDone, rtl.getDoWList());
            canStart = Date.valueOf(dueNext.toLocalDate().minusDays(rtl.getDaysInAdvance()));
            if(!rtlHasActiveChecklist(em,rtl)){
                us = new UpcomingSequence(rtl,lastDone,dueNext,canStart,rtl.getAssignee());
            }
        } catch (Exception e){return null;}
        return us;
    }
    public static List<UpcomingSequence> getMyUpcomingLists(EntityManager em, Person user){
        List<RecurringTaskList> allMyLists = getAllMyLists(em,user);
        List<RecurringTaskList> listsToGenerate = new ArrayList<>();

        List<UpcomingSequence> upcomingLists = new ArrayList<>();
        Date lastDone;
        Date dueNext;
        Date canStart;
        for(RecurringTaskList r: allMyLists){
            lastDone = getLastPerformed(em,r);
            dueNext = getNextDateDue(em,r.getTaskFrequency(),lastDone,r.getDoWList());
            canStart = Date.valueOf(dueNext.toLocalDate().minusDays(r.getDaysInAdvance()));
            if(!rtlHasActiveChecklist(em,r)){
                UpcomingSequence us = new UpcomingSequence(r,lastDone,dueNext,canStart,r.getAssignee());
                upcomingLists.add(us);
            }
        }
        return upcomingLists;
    }

    private static boolean rtlHasActiveChecklist(EntityManager em, RecurringTaskList rtl){
        List<CheckList> checkLists;
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.recurringTaskList.id = :id and c.isComplete=false");
        q.setParameter("id",rtl.getId());
        try{
            checkLists = (List<CheckList>) q.getResultList();
        }catch (NoResultException e){
            e.printStackTrace();
            checkLists = new ArrayList<>();
        }
        boolean hasCheck = false;
        if(checkLists.size()>0){
            for(CheckList c:checkLists){
                System.out.println("Checklist: "+c.getFullName() + " "+c.getId()+ "RTL: "+c.getRecurringTaskList().getId());
            }
            hasCheck = true;
        }
        return hasCheck;
    }



    private static Date getLastPerformed(EntityManager em, RecurringTaskList rtl){
        Date lastDone;
        CheckList checkList;
        List<CheckList> checkLists;
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.recurringTaskList.id = :id AND c.dueDate = (SELECT MAX (ci.dueDate) FROM CheckList ci WHERE ci.recurringTaskList.id=:id)");
        q.setParameter("id",rtl.getId());
        try{
            checkLists = (List<CheckList>) q.getResultList();
            checkList = checkLists.get(0);
        } catch (NoResultException e){
            e.printStackTrace();
            checkList = new CheckList();
            checkList.setDueDate(rtl.getDateStart());
        }
        if(rtl.getDateStart().compareTo(checkList.getDueDate())>0)
            return rtl.getDateStart();
        return checkList.getDueDate();
    }

    public static List<RecurringTaskList> getAllMyLists(EntityManager em, Person user){
        Query q = em.createQuery("SELECT r FROM RecurringTaskList r WHERE r.inActive=false AND r.assignee.id = :id");
        q.setParameter("id",user.getId());
        List<RecurringTaskList> allMyLists;
        try{
            allMyLists = (List<RecurringTaskList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            allMyLists = new ArrayList<>();
        }
        return allMyLists;
    }

    public static RecurringTaskList getRecurringListById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT r FROM RecurringTaskList r WHERE r.id = :id");
        q.setParameter("id",id);
        RecurringTaskList rtl;
        try{
            rtl = (RecurringTaskList) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            rtl = new RecurringTaskList();
        }
        return rtl;
    }
    public static Date getNextDateDue(EntityManager em, TaskFrequency tf, Date dateLast, List<DoW> doWList){
        int freqId = tf.getId();
        return switch (freqId) {
            case 1 -> getNextWeekDay(dateLast);
            case 2 -> getNextWeek(em, dateLast, doWList);
            case 3 -> getNextBiWeek(em, dateLast, doWList);
            case 4 -> get1stOr15th(dateLast);
            case 5 -> get15thOrLast(dateLast);
            case 6 -> getNextMonth(dateLast);
            case 7 -> getNextQuarter(dateLast);
            case 8 -> getNextSemester(dateLast);
            case 9 -> getNextYear(dateLast);
            case 10, 11, 12, 13, 14 -> get1stOfMonth(dateLast, freqId);
            case 15, 16, 17, 18, 19 -> getLastOfMonth(dateLast, freqId);
            default -> dateLast;
        };
    }

    private static Date getNextWeekDay(Date dateLast){
        int dow = dateLast.toLocalDate().getDayOfWeek().getValue();
        if(dow==5)
            return Date.valueOf(dateLast.toLocalDate().plusDays(3));
        else if(dow==6)
            return Date.valueOf(dateLast.toLocalDate().plusDays(2));
        else
            return Date.valueOf(dateLast.toLocalDate().plusDays(1));
    }
    private static Date getNextWeek(EntityManager em, Date dateLast, List<DoW> doWList){
        int dow = dateLast.toLocalDate().getDayOfWeek().getValue();
        DoW dlDow = getDoWByWeekdayInt(em,dow);
        DoW useDow;
        if(doWList.size()==0)
            useDow = getDoWByWeekdayInt(em,1);
        else
            useDow = doWList.get(0);
        int interval = getWeekDayInterval(dlDow,useDow);
        if(doWList.size()>1){
            for(int i = 1; i<doWList.size(); i++){
                if(getWeekDayInterval(dlDow,doWList.get(i))<interval){
                    interval = getWeekDayInterval(dlDow,doWList.get(i));
                    useDow = doWList.get(i);
                }
            }
        }
        return Date.valueOf(dateLast.toLocalDate().plusDays(interval));
    }

    public static List<Person> getPspUserList(EntityManager em, PSP psp){
        Query q1 = em.createQuery("SELECT u FROM User u WHERE u.person.psp.id = :id");
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.psp.id = :id");
        q.setParameter("id",psp.getId());
        q1.setParameter("id",psp.getId());
        List<User> userList = null;
        List<Person> personList = new ArrayList<>();
        try{
            userList = (List<User>) q1.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
        }
        if(userList!=null)
            for(User u:userList){
                if(u != null && u.getUserRoleList()!=null && u.getUserRoleList().size()>0){
                    for(UserRole ur:u.getUserRoleList()){
                        if(ur.getId()==1 || ur.getId()==5 || ur.getId()==9){
                            personList.add(u.getPerson());
                            break;
                        }

                    }
                }
            }

        return personList;
    }

    private static Date getNextBiWeek(EntityManager em, Date dateLast, List<DoW> doWList){
        return Date.valueOf(dateLast.toLocalDate().plusWeeks(2));
    }

    private static Date get1stOr15th(Date dateLast){
        if(dateLast.toLocalDate().getDayOfMonth()<15){
            return Date.valueOf(LocalDate.of(dateLast.toLocalDate().getYear(),dateLast.toLocalDate().getMonthValue(),15));
        } else {
            Date nextMonth = Date.valueOf(dateLast.toLocalDate().plusMonths(1));
            return Date.valueOf(LocalDate.of(nextMonth.toLocalDate().getYear(),nextMonth.toLocalDate().getMonthValue(),1));
        }
    }

    private static Date get15thOrLast(Date dateLast){
        if(dateLast.toLocalDate().getDayOfMonth()>15){
            Date nextMonth = Date.valueOf(dateLast.toLocalDate().plusMonths(1));
            Date firstOfNextMonth = Date.valueOf(LocalDate.of(nextMonth.toLocalDate().getYear(),nextMonth.toLocalDate().getMonthValue(),1));
            return Date.valueOf(firstOfNextMonth.toLocalDate().minusDays(1L));
        } else {
            return Date.valueOf(LocalDate.of(dateLast.toLocalDate().getYear(),dateLast.toLocalDate().getMonthValue(),15));
        }
    }

    private static Date getNextMonth(Date dateLast){
        return Date.valueOf(dateLast.toLocalDate().plusMonths(1));
    }

    private static Date getNextQuarter(Date dateLast){
        return Date.valueOf(dateLast.toLocalDate().plusMonths(3));
    }

    private static Date getNextSemester(Date dateLast){
        return Date.valueOf(dateLast.toLocalDate().plusMonths(6));
    }

    private static Date getNextYear(Date dateLast){
        return Date.valueOf(dateLast.toLocalDate().plusYears(1L));
    }
    private static Date get1stOfMonth(Date dateLast, int freqId){
        int weekDayInt = freqId-9;
        Date nextMonth = Date.valueOf(dateLast.toLocalDate().plusMonths(1));
        Date firstOfNextMonth = Date.valueOf(LocalDate.of(nextMonth.toLocalDate().getYear(),nextMonth.toLocalDate().getMonthValue(),1));
        int wdOfFirstOfMonth = firstOfNextMonth.toLocalDate().getDayOfWeek().getValue();
        int interval;
        if(wdOfFirstOfMonth<weekDayInt){
            interval = weekDayInt - wdOfFirstOfMonth;
        } else {
            interval = weekDayInt + 7 - wdOfFirstOfMonth;
        }
        return Date.valueOf(firstOfNextMonth.toLocalDate().plusDays(interval));
    }
    private static Date getLastOfMonth(Date dateLast, int freqId){
        int weekDayInt = freqId - 14;
        Date twoMonthsFromNow = Date.valueOf(dateLast.toLocalDate().plusMonths(2));
        Date firstOfTwoMonthsFromNow = Date.valueOf(LocalDate.of(twoMonthsFromNow.toLocalDate().getYear(),twoMonthsFromNow.toLocalDate().getMonthValue(),1));
        Date lastOfNextMonth = Date.valueOf(firstOfTwoMonthsFromNow.toLocalDate().minusDays(1L));
        int wdOfLastOfMonth = lastOfNextMonth.toLocalDate().getDayOfWeek().getValue();
        int interval;
        if(wdOfLastOfMonth>weekDayInt){
            interval = wdOfLastOfMonth-weekDayInt;
        } else{
            interval = wdOfLastOfMonth + 7 - weekDayInt;
        }
        return Date.valueOf(lastOfNextMonth.toLocalDate().minusDays(interval));
    }

    private static int getWeekDayInterval(DoW firstDay, DoW secondDay){
        if(firstDay.getWeekdayId()<secondDay.getWeekdayId()){
            return secondDay.getWeekdayId() - firstDay.getWeekdayId();
        } else {
            return secondDay.getWeekdayId() + 7 - firstDay.getWeekdayId();
        }
    }

    public static DoW getDoWByWeekdayInt(EntityManager em, int weekDayInt){
        Query q = em.createQuery("SELECT d FROM DoW d WHERE d.weekdayId = :id");
        q.setParameter("id",weekDayInt);
        DoW d = (DoW) q.getSingleResult();
        return d;
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.TimeLog;
import net.superiorstate.ams.previous.model.general.TimeStretch;
import net.superiorstate.ams.previous.model.general.User;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class TimeTrackingDAO {

    public static List<TimeStretch> getTodaysTimeHistory(EntityManager em, Person user){
        Date today = Date.valueOf(LocalDate.now());
        return getTimeHistoryForRange(em,user,today,today);
    }

    public static List<TimeStretch> getTimeHistoryForRange(EntityManager em,Person user, Date startDate, Date endDate){
        Query q = em.createQuery("SELECT t FROM TimeLog t WHERE t.person.id = :id AND t.punchDate >= :startDate AND t.punchDate <= :endDate ORDER BY t.id");
        q.setParameter("id",user.getId());
        q.setParameter("startDate",startDate);
        q.setParameter("endDate",endDate);
        List<TimeLog> timeLogList;
        try{
            timeLogList = (List<TimeLog>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            timeLogList = new ArrayList<>();
        }
        List<TimeStretch> myStretches = new ArrayList<>();
        TimeStretch timeStretch;

        //Set Starting Index to the first "IN" punch of the date (ignore an out punch - as it will be pulled into previous days log)
        int startIndex = 0;
        if(timeLogList.size()>0)
            if(!timeLogList.get(0).isIn())
                startIndex = 1;
        for(int i = startIndex; i < timeLogList.size(); i=i+2){
            timeStretch = new TimeStretch();
            timeStretch.setId((long) i);
            timeStretch.setUser(user);
            timeStretch.setInDate(timeLogList.get(i).getPunchDate());
            timeStretch.setInTime(timeLogList.get(i).getPunchTime());
            TimeLog forTomorrow = checkTomorrow(em,timeLogList.get(0).getPunchDate());
            // Check to see if we are at there is no final punch out transaction for the date
            if(i+1 <= timeLogList.size()-1){
                timeStretch.setOutDate(timeLogList.get(i+1).getPunchDate());
                timeStretch.setOutTime(timeLogList.get(i+1).getPunchTime());
            }
            // if we are at the last index and we are in a situation where there is not a punch out
            // for the date, check the next day to see if first transaction is a punch out transaction
            // that needs fixing.
            else if(forTomorrow.getId()!=0L && !forTomorrow.isIn()){
                timeStretch.setOutDate(forTomorrow.getPunchDate());
                timeStretch.setOutTime(forTomorrow.getPunchTime());
            }
            myStretches.add(timeStretch);
            System.out.println(timeStretch.getInDate().toString());
        }
        Collections.reverse(myStretches);
        return myStretches;
    }

    private static TimeLog checkTomorrow(EntityManager em, Date today){
        Date tomorrow = Date.valueOf(today.toLocalDate().plusDays(1));
        Query q = em.createQuery("SELECT t FROM TimeLog t WHERE t.punchDate = :tomorrow ORDER BY t.id");
        q.setParameter("tomorrow",tomorrow);
        List<TimeLog> timeLogList;
        TimeLog timeLog;
        try{
            timeLogList = (List<TimeLog>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            timeLogList = new ArrayList<>();
        }
        if(timeLogList.size()>0)
            timeLog = timeLogList.get(0);
        else {
            timeLog = new TimeLog();
            timeLog.setId(0L);
        }
        return timeLog;
    }
    public static TimeLog getMyLastPunch(EntityManager em, User user){
        Query q = em.createQuery("SELECT t FROM TimeLog t WHERE t.person.id = :id order by t.id DESC");
        q.setParameter("id",user.getPerson().getId());
        TimeLog timeLog;
        try{
            List<TimeLog> timeLogList = (List<TimeLog>) q.getResultList();
            timeLog = timeLogList.get(0);
        }catch (NoResultException e){
            e.printStackTrace();
            timeLog = new TimeLog();
            timeLog.setIn(false);
        }
        return timeLog;
    }




}

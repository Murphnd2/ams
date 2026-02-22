package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.general.*;

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
            timeStretch.setInLogId(timeLogList.get(i).getId());       // ← ADD THIS
            TimeLog forTomorrow = checkTomorrow(em,timeLogList.get(0).getPunchDate());
            // Check to see if we are at there is no final punch out transaction for the date
            if(i+1 <= timeLogList.size()-1){
                timeStretch.setOutDate(timeLogList.get(i+1).getPunchDate());
                timeStretch.setOutTime(timeLogList.get(i+1).getPunchTime());
                timeStretch.setOutLogId(timeLogList.get(i+1).getId()); // ← ADD THIS
            }
            // if we are at the last index and we are in a situation where there is not a punch out
            // for the date, check the next day to see if first transaction is a punch out transaction
            // that needs fixing.
            else if(forTomorrow.getId()!=0L && !forTomorrow.isIn()){
                timeStretch.setOutDate(forTomorrow.getPunchDate());
                timeStretch.setOutTime(forTomorrow.getPunchTime());
                timeStretch.setOutLogId(forTomorrow.getId());          // ← ADD THIS
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
// ── Add this method to TimeTrackingDAO ──

    /**
     * Returns a List of 7 DaySummary objects (Mon–Sun) for the week containing the given date.
     * Each DaySummary has that day's stretches and computed totals.
     */
    public static List<DaySummary> getWeeklySummary(EntityManager em, Person user, LocalDate referenceDate) {
        // Find Monday of the week containing referenceDate
        LocalDate monday = referenceDate.with(java.time.DayOfWeek.MONDAY);

        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        LocalDate today = LocalDate.now();

        List<DaySummary> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            DaySummary ds = new DaySummary(
                    Date.valueOf(day),
                    dayLabels[i],
                    day.equals(today)
            );

            // Only fetch stretches for days up to and including today
            if (!day.isAfter(today)) {
                Date sqlDate = Date.valueOf(day);
                List<TimeStretch> stretches = getTimeHistoryForRange(em, user, sqlDate, sqlDate);
                ds.setStretches(stretches);
            }

            week.add(ds);
        }
        return week;
    }

    /**
     * Convenience overload — returns the current week's summary.
     */
    public static List<DaySummary> getWeeklySummary(EntityManager em, Person user) {
        return getWeeklySummary(em, user, LocalDate.now());
    }

    /**
     * Computes total minutes across all days in a weekly summary.
     */
    public static int getWeeklyTotalMinutes(List<DaySummary> week) {
        int total = 0;
        for (DaySummary ds : week) {
            total += ds.getTotalMinutes();
        }
        return total;
    }

    /**
     * Formats total minutes as "Xh Ym".
     */
    public static String formatMinutes(int totalMinutes) {
        if (totalMinutes <= 0) return "—";
        return (totalMinutes / 60) + "h " + String.format("%02d", totalMinutes % 60) + "m";
    }



}

package net.superiorstate.ams.previous.controller.activity.checklist.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbRec;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.UpcomingSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SequenceHome", value = "/SequenceHome")
public class SequenceHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/activity/checklist/sequences/sequenceHome.jsp");
        dispatcher.forward(request,response);
    }

    private void doThisFirst(HttpServletRequest request){
        System.out.println(3);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        getRequiredSequenceList(request,em,psp);
        getRecurringSequenceList(request,em,psp);
        setFrequencyList(request,em);
        getMyDue(request,em);
        em.close();

    }

    private void testDateFunctions(HttpServletRequest request, EntityManager em){
        DoW aMonday = dbRec.getDoWByWeekdayInt(em,1);
        DoW aThursday = dbRec.getDoWByWeekdayInt(em,4);
        Date lastDate = Date.valueOf(LocalDate.of(2023,2,16));
        List<DoW> doWList = new ArrayList<>();
        doWList.add(aMonday);
        doWList.add(aThursday);
        TaskFrequency tf = dM.getTaskFrequencyById(em,17);
        Date nextDate = dbRec.getNextDateDue(em,tf,lastDate,doWList);
        System.out.println(nextDate.toString());
    }

    private void setFrequencyList(HttpServletRequest request, EntityManager em){
        Query q = em.createQuery("SELECT f FROM TaskFrequency f ORDER BY f.id");
        List<TaskFrequency> taskFrequencyList;
        try{
            taskFrequencyList = (List<TaskFrequency>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskFrequencyList = new ArrayList<>();
        }
        request.getSession().setAttribute("taskFrequencyList",taskFrequencyList);
    }

    private void getRequiredSequenceList(HttpServletRequest request, EntityManager em, PSP psp){
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.inActive = false AND rtl.psp.id =:id ORDER BY rtl.description");
        q.setParameter("id",psp.getId());
        List<RequiredTaskList> requiredTaskLists;
        try{
            requiredTaskLists = (List<RequiredTaskList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            requiredTaskLists = new ArrayList<>();
        }
        request.getSession().setAttribute("reqTaskList",requiredTaskLists);
    }

    private void getRecurringSequenceList(HttpServletRequest request, EntityManager em, PSP psp){
        Query q = em.createQuery("SELECT rtl FROM RecurringTaskList rtl WHERE rtl.inActive = false AND rtl.psp.id =:id ORDER BY rtl.description");
        q.setParameter("id",psp.getId());
        List<RecurringTaskList> recurringTaskLists;
        try{
            recurringTaskLists = (List<RecurringTaskList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            recurringTaskLists = new ArrayList<>();
        }
        request.getSession().setAttribute("recTaskList",recurringTaskLists);
    }

    private void getMyDue(HttpServletRequest request, EntityManager em){
        Person user = (Person) request.getSession().getAttribute("currentPerson");
        List<UpcomingSequence> recurringTaskLists = dbRec.getMyUpcomingLists(em,user);
        request.getSession().setAttribute("myUpList",recurringTaskLists);
    }
}

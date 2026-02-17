package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbRec;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AddRecurringSequence", value = "/AddRecurringSequence")
public class AddRecurringSequence extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addSequence(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addSequence(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RecurringSequenceBuilder");
        dispatcher.forward(request,response);
    }

    private void addSequence(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        String sequenceName = request.getParameter("sequenceName");
        Date effectiveDate = Date.valueOf(request.getParameter("startDate"));
        int daysInAdvance = Integer.parseInt(request.getParameter("daysInAdvance"));
        Person assignedTo = dM.getPersonById(em,Long.parseLong(request.getParameter("userList")));
        TaskFrequency taskFrequency = dM.getTaskFrequencyById(em,Integer.parseInt(request.getParameter("frequencyList")));
        List<DoW> dowList = getDaysChecked(request,em);
        em.getTransaction().begin();
        RecurringTaskList rtl = new RecurringTaskList();
        rtl.setDescription(sequenceName);
        rtl.setAssignee(assignedTo);
        rtl.setDateStart(effectiveDate);
        rtl.setDaysInAdvance(daysInAdvance);
        rtl.setTaskFrequency(taskFrequency);
        rtl.setPsp(psp);
        rtl.setInActive(false);
        em.persist(rtl);
        em.getTransaction().commit();

        em.getTransaction().begin();
        DoW d;
        for(DoW dow: dowList){
            d=dbRec.getDoWByWeekdayInt(em,dow.getWeekdayId());
            rtl.addDayOfWeek(d);
        }
        em.persist(rtl);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentReqList",new RequiredTaskList());
        request.getSession().setAttribute("currentRecList", rtl);
        request.getSession().setAttribute("sequenceView",2);

    }

    public static List<DoW> getDaysChecked(HttpServletRequest request,EntityManager em){
        List<DoW> dowList = new ArrayList<>();
        String sMonday = request.getParameter("cbMonday");
        String sTuesday = request.getParameter("cbTuesday");
        String sWednesday = request.getParameter("cbWednesday");
        String sThursday = request.getParameter("cbThursday");
        String sFriday = request.getParameter("cbFriday");
        if(!(sMonday==null || sMonday==""))
            dowList.add(dbRec.getDoWByWeekdayInt(em,1));

        if(!(sTuesday==null || sTuesday==""))
            dowList.add(dbRec.getDoWByWeekdayInt(em,2));

        if(!(sWednesday==null || sWednesday==""))
            dowList.add(dbRec.getDoWByWeekdayInt(em,3));

        if(!(sThursday==null || sThursday==""))
            dowList.add(dbRec.getDoWByWeekdayInt(em,4));

        if(!(sFriday==null || sFriday==""))
            dowList.add(dbRec.getDoWByWeekdayInt(em,5));

        if(dowList.size() == 0)
            dowList.add(dbRec.getDoWByWeekdayInt(em,1));

        return dowList;
    }
}

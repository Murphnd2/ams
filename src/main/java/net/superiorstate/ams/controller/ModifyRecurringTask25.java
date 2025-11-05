package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.checklist.dbRec;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ModifyRecurringTask25", value = "/ModifyRecurringTask25")
public class ModifyRecurringTask25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateSequence(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateSequence(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
    }

    private void updateSequence(HttpServletRequest request){
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = local.getCurrentPerson().getPsp();
        String sequenceName = request.getParameter("sequenceName");
        Date effectiveDate = Date.valueOf(request.getParameter("startDate"));
        int daysInAdvance = Integer.parseInt(request.getParameter("daysInAdvance"));
        Person assignedTo = dM.getPersonById(em,Long.parseLong(request.getParameter("userList")));
        TaskFrequency taskFrequency = dM.getTaskFrequencyById(em,Integer.parseInt(request.getParameter("frequencyList")));
        CheckList c = local.getCurrentChecklist().getCheckList();
        int buttonValue = Integer.parseInt(request.getParameter("btnRecurring"));
        if(buttonValue==0){
            RecurringTaskList rtl = c.getRecurringTaskList();
            em.getTransaction().begin();
            RecurringTaskList r = dbRec.getRecurringListById(em, rtl.getId());
            r.setInActive(true);
            em.persist(r);
            em.getTransaction().commit();
        } else {
            em.getTransaction().begin();
            RecurringTaskList rtl = dbRec.getRecurringListById(em,c.getRecurringTaskList().getId());
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
            rtl.getDoWList().clear();
            em.persist(rtl);
            em.getTransaction().commit();
            if(taskFrequency.getId()==2){
                List<DoW> newDowList = getDaysChecked(request,em);
                DoW d1 = new DoW();
                em.getTransaction().begin();
                if(newDowList.size()==0){
                    rtl.addDayOfWeek(dbRec.getDoWByWeekdayInt(em,1));
                }else{
                    for(DoW dow: newDowList){
                        d1=dbRec.getDoWByWeekdayInt(em,dow.getWeekdayId());
                        rtl.addDayOfWeek(d1);
                    }
                }
                em.persist(rtl);
                em.getTransaction().commit();
            }
        }
        em.close();
        request.getSession().setAttribute("local",local);
    }

    private List<DoW> getDaysChecked(HttpServletRequest request,EntityManager em){
        List<DoW> dowList = new ArrayList<>();
        String sMonday = request.getParameter("cbMonday");
        String sTuesday = request.getParameter("cbTuesday");
        String sWednesday = request.getParameter("cbWednesday");
        String sThursday = request.getParameter("cbThursday");
        String sFriday = request.getParameter("cbFriday");
        if(!(sMonday==null || sMonday.equals("")))
            dowList.add(dbRec.getDoWByWeekdayInt(em,1));

        if(!(sTuesday==null || sTuesday.equals("")))
            dowList.add(dbRec.getDoWByWeekdayInt(em,2));

        if(!(sWednesday==null || sWednesday.equals("")))
            dowList.add(dbRec.getDoWByWeekdayInt(em,3));

        if(!(sThursday==null || sThursday.equals("")))
            dowList.add(dbRec.getDoWByWeekdayInt(em,4));

        if(!(sFriday==null || sFriday.equals("")))
            dowList.add(dbRec.getDoWByWeekdayInt(em,5));

        if(dowList.size() == 0)
            dowList.add(dbRec.getDoWByWeekdayInt(em,1));

        return dowList;
    }
}

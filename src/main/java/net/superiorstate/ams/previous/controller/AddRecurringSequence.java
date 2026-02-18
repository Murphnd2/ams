package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.RecurringChecklistDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
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
        Person assignedTo = EntityLookup.getPersonById(em,Long.parseLong(request.getParameter("userList")));
        TaskFrequency taskFrequency = EntityLookup.getTaskFrequencyById(em,Integer.parseInt(request.getParameter("frequencyList")));
        List<DoW> dowList = RecurringChecklistDAO.getDaysChecked(request,em);
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
            d= RecurringChecklistDAO.getDoWByWeekdayInt(em,dow.getWeekdayId());
            rtl.addDayOfWeek(d);
        }
        em.persist(rtl);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentReqList",new RequiredTaskList());
        request.getSession().setAttribute("currentRecList", rtl);
        request.getSession().setAttribute("sequenceView",2);

    }


}

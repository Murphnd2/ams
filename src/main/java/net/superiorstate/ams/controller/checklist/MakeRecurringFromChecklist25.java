package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.RecurringChecklistDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "MakeRecurringFromChecklist25", value = "/MakeRecurringFromChecklist25")
public class MakeRecurringFromChecklist25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        convertChecklist(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        convertChecklist(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
    }

    private void convertChecklist(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = local.getCurrentPerson().getPsp();
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

        for(DoW d: dowList){
            em.getTransaction().begin();
            RecurringTaskList rt = RecurringChecklistDAO.getRecurringListById(em,rtl.getId());
            rt.addDayOfWeek(d);
            em.persist(rt);
            em.getTransaction().commit();
        }

        CheckList checkList = local.getCurrentChecklist().getCheckList();
        em.getTransaction().begin();
        CheckList c = EntityLookup.getCheckListById(em, checkList.getId());
        assert c != null;
        c.setRecurringTaskList(rtl);
        em.persist(c);
        em.getTransaction().commit();
        local.getCurrentChecklist().setCheckList(c);


        List<Task> checklistTasks = getTasksInChecklist(em,c);
        int index = 10;
        for(Task t: checklistTasks){
            em.getTransaction().begin();
            TaskSequenceTable tst = new TaskSequenceTable();
            tst.setTask(t);
            tst.setTaskSequence(rtl);
            tst.setSortOrder(index);
            index +=10;
            em.persist(tst);
            em.getTransaction().commit();

            em.getTransaction().begin();
            RecurringTaskList r = RecurringChecklistDAO.getRecurringListById(em,rtl.getId());
            r.getTaskSequenceTableList().add(tst);
            em.persist(r);
            em.getTransaction().commit();
        }
        em.close();
        request.getSession().setAttribute("local",local);
    }

    private List<Task> getTasksInChecklist(EntityManager em, CheckList c){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :id order by t.sortOrder");
        q.setParameter("id",c.getId());
        List<ToDo> toDoList = null;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
        }
        List<Task> taskList = new ArrayList<>();
        if(toDoList != null)
            for(ToDo t:toDoList)
                if(!taskList.contains(t.getTask()))
                    taskList.add(t.getTask());
        return taskList;
    }
}

package net.superiorstate.ams.previous.controller.activity.checklist.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.general.PSP;

import java.io.IOException;

@WebServlet(name = "AddAvailableTask", value = "/AddAvailableTask")
public class AddAvailableTask extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addTaskToRequiredTaskList(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addTaskToRequiredTaskList(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RequiredSequenceBuilder");
        dispatcher.forward(request,response);
    }

    private void addTaskToRequiredTaskList(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        RequiredTaskList ts = (RequiredTaskList) request.getSession().getAttribute("currentReqList");
        int sortOrder = Integer.parseInt(request.getParameter("tbSortOrder"));
        Long taskId = Long.parseLong(request.getParameter("ddTaskToAdd"));
        Query q = em.createQuery("SELECT t FROM Task t WHERE t.id = :id");
        q.setParameter("id",taskId);
        Task task;
        try{
            task= (Task) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            return;
        }
        try{
            em.getTransaction().begin();
            TaskSequenceTable tst = new TaskSequenceTable();
            tst.setTask(task);
            tst.setTaskSequence(ts);
            tst.setSortOrder(sortOrder);
            em.persist(tst);
            em.getTransaction().commit();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}

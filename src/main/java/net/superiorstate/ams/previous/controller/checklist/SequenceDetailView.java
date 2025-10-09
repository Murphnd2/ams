package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SequenceDetailView", value = "/SequenceDetailView")
public class SequenceDetailView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getSequenceDetail(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getSequenceDetail(request);
        goToPage(request,response);
    }

    private void getSequenceDetail(HttpServletRequest request){
        request.getSession().setAttribute("checkView",2);
        Long selectedSequenceId = null;
        try{
            selectedSequenceId = Long.parseLong(request.getParameter("sequenceSelectButton"));
        } catch (Exception e){
            e.printStackTrace();
            selectedSequenceId = -1L;
        }
        TaskSequence taskSequence = null;
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        if(selectedSequenceId > 0L){
            taskSequence = ddC.getTaskSequenceById(em,selectedSequenceId);
            request.getSession().setAttribute("hasCurrentTaskSequence",true);
            request.getSession().setAttribute("currentTaskSequence",taskSequence);
        } else{
            taskSequence = (TaskSequence) request.getSession().getAttribute("currentTaskSequence");
        }

        getSequenceTasks(request,em);

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Task task = new Task();
        task.setId(-1L);
        request.getSession().setAttribute("hasCurrentTask",false);
        request.getSession().setAttribute("currentTask",task);
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ChecklistManagerGo");
        dispatcher.forward(request,response);
    }

    private void getSequenceTasks(HttpServletRequest request, EntityManager em){
        TaskSequence taskSequence = (TaskSequence) request.getSession().getAttribute("currentTaskSequence");
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst INNER JOIN FETCH tst.task t WHERE tst.taskSequence.id = :sequence_id ORDER BY tst.sortOrder");
        q.setParameter("sequence_id",taskSequence.getId());
        List<TaskSequenceTable> taskSequenceTableList = null;
        try{
            taskSequenceTableList = (List<TaskSequenceTable>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskSequenceTableList = new ArrayList<>();
        }

        request.getSession().setAttribute("currentSequenceListOfAssignedTasks",taskSequenceTableList);
        setAvailableTasks(request,em, taskSequence);
    }

    private void setAvailableTasks(HttpServletRequest request, EntityManager em,TaskSequence currentTaskSequence){
        PSP psp = dM.getPspById(em,4L);

        Query one = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :sequence_id");
        one.setParameter("sequence_id",currentTaskSequence.getId());
        List<TaskSequenceTable> taskSequenceTableList = (List<TaskSequenceTable>) one.getResultList();

        Query two = em.createQuery("SELECT t FROM Task t WHERE t.psp.id = :psp_id ORDER BY t.description");
        two.setParameter("psp_id",psp.getId());
        List<Task> taskList = (List<Task>) two.getResultList();

        List<Task> useList = new ArrayList<>();
        for (Task task:taskList)
            useList.add(task);

        for (TaskSequenceTable taskSequenceTable: taskSequenceTableList){
            Task taskToRemove = taskSequenceTable.getTask();
            useList.remove(taskToRemove);
        }

        request.getSession().setAttribute("availableTasks",useList);
    }
}

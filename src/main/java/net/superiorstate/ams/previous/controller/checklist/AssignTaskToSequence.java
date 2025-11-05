package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;

import java.io.IOException;

@WebServlet(name = "AssignTaskToSequence", value = "/AssignTaskToSequence")
public class AssignTaskToSequence extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        assignTaskToSequence(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        assignTaskToSequence(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SequenceDetailView");
        dispatcher.forward(request,response);
    }
    private void assignTaskToSequence(HttpServletRequest request){
        TaskSequence currentTaskSequence = (TaskSequence) request.getSession().getAttribute("currentTaskSequence");
        Long taskId = Long.parseLong(request.getParameter("taskListDropDown"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Task task = dM.getTaskById(em,taskId);
        TaskSequence taskSequence = ddC.getTaskSequenceById(em,currentTaskSequence.getId());
        TaskSequenceTable taskSequenceTable = new TaskSequenceTable();
        taskSequenceTable.setTask(task);
        taskSequenceTable.setTaskSequence(taskSequence);
        taskSequenceTable.setSortOrder(task.getId().intValue());
        try{
            em.getTransaction().begin();
            em.persist(taskSequenceTable);
            em.getTransaction().commit();
        } catch (Exception e){
            e.printStackTrace();
        }


        em.close();
        request.getSession().setAttribute("currentTaskSequence",taskSequence);


    }
}

package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;

import java.io.IOException;

@WebServlet(name = "UpdateTaskName", value = "/UpdateTaskName")
public class UpdateTaskName extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String taskName = request.getParameter("task_name");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Task task = (Task) request.getSession().getAttribute("currentTask");
        em.getTransaction().begin();
        Task currentTask = dM.getTaskById(em,task.getId());
        currentTask.setDescription(taskName);
        em.persist(currentTask);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentTask",currentTask);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("TaskDetailView");
        dispatcher.forward(request,response);
    }
}

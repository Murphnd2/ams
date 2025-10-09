package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;

@WebServlet(name = "ReOpenToDo", value = "/ReOpenToDo")
public class ReOpenToDo extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Long id = Long.parseLong(request.getParameter("btnToDo"));
        em.getTransaction().begin();
        ToDo toDo = dM.getToDoById(em,id);
        Task task = dM.getTaskById(em,toDo);
        toDo.setComplete(false);
        toDo.setTask(task);
        toDo.setDateCompleted(null);
        em.persist(toDo);
        em.getTransaction().commit();
        request.getSession().setAttribute("currentToDoList",ViewSelectedChecklist.getToDoListByChecklistId(em,toDo.getCheckList().getId()));
        em.close();
    }

}

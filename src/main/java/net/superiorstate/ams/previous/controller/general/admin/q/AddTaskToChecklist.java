package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedChecklist;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "AddTaskToChecklist", value = "/AddTaskToChecklist")
public class
AddTaskToChecklist extends HttpServlet {
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
        User user = (User) request.getSession().getAttribute("currentUser");
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        String taskIdString = request.getParameter("taskID");
        Long taskId = Long.parseLong(taskIdString);
        CheckList checkList = (CheckList) request.getSession().getAttribute("currentChecklist");
        List<ToDo> toDoList = checkList.getToDoList();
        int maxSort = 0;
        for(ToDo t: toDoList){
            if(t.getSortOrder()>maxSort)
                maxSort = t.getSortOrder();
        }
        Query q = em.createQuery("SELECT t FROM Task t WHERE t.id = :id");
        q.setParameter("id",taskId);
        Task t;
        try{
            t = (Task) q.getSingleResult();
        } catch (NoResultException e){
            em.close();
            return;
        }
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setComplete(false);
        toDo.setTask(t);
        toDo.setCheckList(checkList);
        toDo.setSortOrder(maxSort + 1);
        em.persist(toDo);
        em.getTransaction().commit();
        request.getSession().setAttribute("currentToDoList", ViewSelectedChecklist.getToDoListByChecklistId(em,checkList.getId()));
        em.close();


    }
}

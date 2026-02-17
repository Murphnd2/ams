package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDoOut;

import java.io.IOException;

@WebServlet(name = "ManageTask25", value = "/ManageTask25")
public class ManageTask25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewTaskManager(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewTaskManager(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/taskManager/taskManager25.jsp");
        dispatcher.forward(request,response);
    }

    private void viewTaskManager(HttpServletRequest request){
        String toDoIdString;
        try{
            toDoIdString = request.getParameter("toDoId").toString();
        } catch (Exception e){
            return;
        }
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        long toDoId = Long.parseLong(toDoIdString);
        System.out.println("ToDoId: "+ toDoId);
        Query q = em.createQuery("SELECT t FROM ToDoOut t WHERE t.toDo.id = :id");
        q.setParameter("id",toDoId);
        ToDoOut toDoOut;
        try{
            toDoOut = (ToDoOut) q.getSingleResult();
        } catch (Exception e){
            return;
        }
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        local.setCurrentToDoOut(toDoOut);
        local.setCurrentToDo(EntityLookup.getToDoById(em,toDoOut.getToDo().getId()));
        request.getSession().setAttribute("local",local);
        em.close();

    }
}

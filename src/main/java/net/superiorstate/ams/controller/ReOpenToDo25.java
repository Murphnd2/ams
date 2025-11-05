package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;

@WebServlet(name = "ReOpenToDo25", value = "/ReOpenToDo25")
public class ReOpenToDo25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processToDoClosure(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processToDoClosure(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    private void processToDoClosure(HttpServletRequest request){
        String toDoIdString;
        try{
            toDoIdString = request.getParameter("btnToDo").toString();
        } catch (Exception e){
            return;
        }
        long toDoId = Long.parseLong(toDoIdString);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        // Close the ToDo
        ToDo toDo = dM.getToDoById(em,toDoId);
        if(toDo==null){
            em.close();
            return;
        }
        em.getTransaction().begin();
        toDo.setComplete(false);
        toDo.setDateCompleted(null);
        toDo.setCompletedBy(null);
        em.persist(toDo);
        em.getTransaction().commit();
        em.refresh(toDo);

        // If the task had delegation, refilter the activity list to update status
        if(toDo.getTask().hasOwner() && toDo.getTask().getOwner()!=null && !toDo.getTask().getOwner().getId().equals(local.getCurrentActivity().getActivity().getId())){
            AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
            global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
            local.setActivitiesWithDependencies(global.getActivitiesWithDelegation());
            local.getCurrentActivity().setReFilterOnExit(true);
            request.getServletContext().setAttribute("global",global);
        }

        local.respondToActivityUpdate(em,"TODO_REOPEN",toDoId);

        em.close();

        request.getSession().setAttribute("local",local);
    }
}

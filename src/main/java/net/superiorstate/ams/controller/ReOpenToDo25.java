// File: src/main/java/net/superiorstate/ams/controller/ReOpenToDo25.java
package net.superiorstate.ams.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
        String toKebabIdString;
        try{
            toKebabIdString = request.getParameter("btnToDo").toString();
        } catch (Exception e){
            return;
        }
        long toDoId = Long.parseLong(toKebabIdString);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManager em = getOpenEntityManager(request);

        try {
            // Close the ToDo
            ToDo toDo = dM.getToDoById(em,toDoId);
            if(toDo==null){
                return;
            }
            em.getTransaction().begin();
            toDo.setComplete(false);
            toDo.setDateCompleted(null);
            toDo.setCompletedBy(null);
            em.persist(toDo);
            em.getTransaction().commit();

            // Remove from pending closes (now safe)
            local.getPendingCloseIds().remove(toDoId);

            // Delegation
            if (toDo.getTask().hasOwner() && toDo.getTask().getOwner() != null
                    && !toDo.getTask().getOwner().getId().equals(local.getCurrentActivity().getActivity().getId())) {
                AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
                global.markDelegationDirty();
                local.getCurrentActivity().setReFilterOnExit(true);
            }

            local.respondToActivityUpdate(em,"TODO_REOPEN",toDoId);
            request.getSession().setAttribute("local",local);
        } finally {
            em.close();
        }
    }
    private EntityManager getOpenEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf =
                (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }
}

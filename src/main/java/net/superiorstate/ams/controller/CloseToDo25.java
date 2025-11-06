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
import java.sql.Date;
import java.time.LocalDate;

@WebServlet(name = "CloseToDo25", value = "/CloseToDo25")
public class CloseToDo25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processToDoClosure(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processToDoClosure(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private void processToDoClosure(HttpServletRequest request) {
        String toDoIdParam = request.getParameter("btnToDo");
        if (toDoIdParam == null) return;

        long toDoId;
        try {
            toDoId = Long.parseLong(toDoIdParam);
        } catch (NumberFormatException e) {
            return;
        }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            ToDo toDo = dM.getToDoById(em, toDoId);
            if (toDo == null) return;

            em.getTransaction().begin();
            toDo.setComplete(true);
            toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
            toDo.setCompletedBy(local.getCurrentPerson());
            em.persist(toDo);
            em.getTransaction().commit();
            em.refresh(toDo);

            if (requiresDelegationRefresh(toDo, local)) {
                refreshDelegation(global, local, em);
                request.getServletContext().setAttribute("global", global);
            }

            local.respondToActivityUpdate(em, "TODO_CLOSE", toDoId);
            request.getSession().setAttribute("local", local);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }

    }

    private boolean requiresDelegationRefresh(ToDo toDo, AmsDataLocal local) {
        return toDo.getTask().hasOwner()
                && toDo.getTask().getOwner() != null
                && !toDo.getTask().getOwner().getId().equals(local.getCurrentActivity().getActivity().getId());
    }

    private void refreshDelegation(AmsDataGlobal global, AmsDataLocal local, EntityManager em) {
        global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
        local.setActivitiesWithDependencies(global.getActivitiesWithDelegation());
        local.getCurrentActivity().setReFilterOnExit(true);
    }
}
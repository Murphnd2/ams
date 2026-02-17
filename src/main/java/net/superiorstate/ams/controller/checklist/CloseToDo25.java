package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;

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
        EntityManager em = getOpenEntityManager(request);

        try {
            ToDo toDo = EntityLookup.getToDoById(em, toDoId);
            if (toDo == null) return;

            // === IN-MEMORY UPDATE ===
            // Update ToDoOut25 in session
            local.getCurrentActivity().getToDoList().stream()
                    .filter(out -> out.getToDo().getId() == toDoId)
                    .findFirst()
                    .ifPresent(out -> {
                        out.getToDo().setComplete(true);
                        out.getToDo().setDateCompleted(java.sql.Date.valueOf(java.time.LocalDate.now()));
                        out.getToDo().setCompletedBy(local.getCurrentPerson());
                    });

            // Mark for later DB save
            local.markToDoClosed(toDoId);

            // Delegation flag
            if (requiresDelegationRefresh(toDo, local)) {
                global.markDelegationDirty();
                local.getCurrentActivity().setReFilterOnExit(true);
            }

            local.respondToActivityUpdate(em, "TODO_CLOSE", toDoId);
            request.getSession().setAttribute("local", local);
        } finally {
            em.close();
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

    private EntityManager getOpenEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf =
                (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }
}
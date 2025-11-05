package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "CloseToDo25", value = "/CloseToDo25")
public class CloseToDo25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        boolean newState = processToDoClosure(request);

        if (isAjax) {
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":true,\"complete\":" + newState + "}");
            return;
        }

        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }
    private boolean processToDoClosure(HttpServletRequest request) {
        String toDoIdParam = request.getParameter("btnToDo");
        if (toDoIdParam == null) return false;

        long toDoId;
        try {
            toDoId = Long.parseLong(toDoIdParam);
        } catch (NumberFormatException e) {
            return false;
        }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null) return false;

        List<ToDoOut25> list = local.getCurrentActivity().getToDoList();
        if (list == null || list.isEmpty()) {
            System.out.println("[DEBUG] ToDo list is null or empty");
            return false;
        }

        ToDoOut25 t = null;
        for (ToDoOut25 candidate : list) {
            ToDo toDo = candidate.getToDo();
            if (toDo != null && toDo.getId() != null && toDo.getId() == toDoId) {
                t = candidate;
                break;
            }
        }

        if (t == null) {
            System.out.println("[DEBUG] ToDo not found in list. btnToDo=" + toDoId +
                    " | List size=" + list.size() +
                    " | Sample IDs: " +
                    list.stream()
                            .map(c -> c.getToDo() != null ? String.valueOf(c.getToDo().getId()) : "null")
                            .limit(5)
                            .collect(Collectors.joining(", ")));
            return false;
        }

        boolean newState = !t.isComplete();
        t.setComplete(newState);
        ToDo toDo = t.getToDo();
        toDo.setComplete(newState);
        toDo.setCompletedBy(newState ? local.getCurrentPerson() : null);
        toDo.setDateCompleted(newState ? Date.valueOf(LocalDate.now()) : null);

        local.getCurrentActivity().setReFilterOnExit(true);
        local.respondToActivityUpdate(null, "TODO_TOGGLE", toDoId);

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (requiresDelegationRefresh(toDo, local)) {
            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                refreshDelegation(global, local, em);
                getServletContext().setAttribute("global", global);
            } finally {
                if (em.isOpen()) em.close();
            }
        }

        request.getSession().setAttribute("local", local);
        return newState;
    }

    private boolean requiresDelegationRefresh(ToDo toDo, AmsDataLocal local) {
        return toDo.getTask().hasOwner()
                && toDo.getTask().getOwner() != null
                && !toDo.getTask().getOwner().getId()
                .equals(local.getCurrentActivity().getActivity().getId());
    }

    private void refreshDelegation(AmsDataGlobal global, AmsDataLocal local, EntityManager em) {
        global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
        local.setActivitiesWithDependencies(global.getActivitiesWithDelegation());
        local.getCurrentActivity().setReFilterOnExit(true);
    }
}
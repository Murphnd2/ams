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

@WebServlet(name = "CloseToDo25", value = "/CloseToDo25")
public class CloseToDo25 extends HttpServlet {

    @Override protected void doGet(HttpServletRequest r, HttpServletResponse s)
            throws ServletException, IOException { doPost(r, s); }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        boolean ajax = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
        boolean newState = toggleInMemory(req);               // <-- core logic

        if (ajax) {
            resp.setContentType("application/json");
            resp.getWriter().print("{\"success\":true,\"complete\":" + newState + "}");
            return;                                            // STOP – no forward
        }

        // non-AJAX (old forms) – keep old behaviour
        getServletContext().getNamedDispatcher("ViewActivity25")
                .forward(req, resp);
    }

    /** In-memory toggle – returns the *new* complete flag */
    private boolean toggleInMemory(HttpServletRequest req) {
        String idParam = req.getParameter("btnToDo");
        if (idParam == null) return false;

        long toDoId;
        try { toDoId = Long.parseLong(idParam); }
        catch (NumberFormatException e) { return false; }

        AmsDataLocal local = (AmsDataLocal) req.getSession().getAttribute("local");
        if (local == null) return false;

        List<ToDoOut25> list = local.getCurrentActivity().getToDoList();
        ToDoOut25 wrapper = list.stream()
                .filter(t -> t.getToDo() != null && t.getToDo().getId() == toDoId)
                .findFirst().orElse(null);
        if (wrapper == null) return false;

        boolean newState = !wrapper.isComplete();
        wrapper.setComplete(newState);
        ToDo entity = wrapper.getToDo();
        entity.setComplete(newState);
        entity.setCompletedBy(newState ? local.getCurrentPerson() : null);
        entity.setDateCompleted(newState ? Date.valueOf(LocalDate.now()) : null);

        local.getCurrentActivity().setReFilterOnExit(true);   // <-- batch later
        local.respondToActivityUpdate(null, "TODO_TOGGLE", toDoId);

        // optional delegation refresh (unchanged)
        refreshDelegationIfNeeded(req, local, entity);

        req.getSession().setAttribute("local", local);
        return newState;
    }

    private void refreshDelegationIfNeeded(HttpServletRequest req,
                                           AmsDataLocal local, ToDo toDo) {
        if (!toDo.getTask().hasOwner() || toDo.getTask().getOwner() == null) return;
        if (toDo.getTask().getOwner().getId()
                .equals(local.getCurrentActivity().getActivity().getId())) return;

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
            getServletContext().setAttribute("global", global);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (em != null) {
                try { em.close(); } catch (Exception ignored) {}
            }
        }
    }
}
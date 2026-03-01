package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.PSP;

import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;

@WebServlet(name = "AddToDo25", value = "/AddToDo25")
public class AddToDo25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleAddToDo(request);
        forwardToView(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleAddToDo(request);
        forwardToView(request, response);
    }

    private void forwardToView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private void handleAddToDo(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Activity activity = local.getCurrentActivity().getActivity();
        if (activity == null) return;

        List<ToDoOut25> toDoList = local.getCurrentActivity().getToDoList();
        if (toDoList == null) return;

        CheckList checkList = getCheckList(toDoList, local);
        if (checkList == null) return;

        String toDoDescription;
        long positionId;
        try {
            toDoDescription = request.getParameter("toDoName");
            positionId = Long.parseLong(request.getParameter("insertWhere"));
        } catch (Exception e) {
            return;
        }

        EntityManager em = getOpenEntityManager(request);

        try {
            em.getTransaction().begin();
            Task task = createTask(em, activity.getLoggedBy().getPsp(), toDoDescription);
            em.getTransaction().commit();

            int sortOrder = calculateSortOrder(em, toDoList, positionId);
            em.getTransaction().begin();
            ToDo toDo = createToDo(task, checkList, sortOrder);
            em.persist(toDo);
            em.getTransaction().commit();

            local.respondToActivityUpdate(em, "TD_ADD", toDo);
            request.getSession().setAttribute("local", local);
        } finally {
            em.close();
        }
    }

    private CheckList getCheckList(List<ToDoOut25> toDoList, AmsDataLocal local) {
        if (!toDoList.isEmpty()) {
            return toDoList.get(0).getCheckList();
        } else {
            return local.getCurrentActivity().getCheckList();
        }
    }

    private Task createTask(EntityManager em, PSP psp, String description) {
        Task task = new Task();
        task.setPsp(psp);
        task.setDescription(description);
        task.setReUsable(false);
        task.setAllowEarly(true);
        task.setAllowFuture(true);
        task.setAllowNonOwner(true);
        em.persist(task);
        return task;
    }

    /**
     * Determines the sort_order for a newly inserted todo and ensures
     * there are no collisions with existing todos.
     *
     * positionId == -1  → append after last  (max + 10)
     * positionId ==  0  → insert before first (min - 10)
     * positionId ==  N  → insert AFTER todo N
     *
     * For "insert after", we try to find a gap between the target and
     * the next todo. If there's no gap (consecutive or duplicate
     * sort_orders), we shift all todos below the target down by 10
     * to make room.
     */
    private int calculateSortOrder(EntityManager em, List<ToDoOut25> toDoList, long positionId) {

        if (positionId == -1) {
            // ── Append at bottom: max + 10 ──
            OptionalInt max = toDoList.stream().mapToInt(ToDoOut25::getSortOrder).max();
            return max.orElse(0) + 10;

        } else if (positionId == 0) {
            // ── Insert at top: min - 10 ──
            OptionalInt min = toDoList.stream().mapToInt(ToDoOut25::getSortOrder).min();
            return min.orElse(10) - 10;

        } else {
            // ── Insert AFTER the target todo ──
            ToDo targetToDo = EntityLookup.getToDoById(em, positionId);
            if (targetToDo == null) return 0;

            int targetSort = targetToDo.getSortOrder();
            long checklistId = targetToDo.getCheckList().getId();

            // Find the sort_order of the next todo after the target
            List<?> nextResult = em.createNativeQuery(
                            "SELECT MIN(sort_order) FROM todo " +
                                    "WHERE checklist_id = ?1 AND sort_order > ?2 AND is_complete = 0")
                    .setParameter(1, checklistId)
                    .setParameter(2, targetSort)
                    .getResultList();

            Integer nextSort = null;
            if (!nextResult.isEmpty() && nextResult.get(0) != null) {
                nextSort = ((Number) nextResult.get(0)).intValue();
            }

            if (nextSort == null) {
                // Target is the last todo — just add 10
                return targetSort + 10;
            }

            int gap = nextSort - targetSort;

            if (gap >= 2) {
                // There's room — place in the middle
                return targetSort + (gap / 2);
            } else {
                // No room (gap is 0 or 1) — shift everything after target down by 10
                em.getTransaction().begin();
                em.createNativeQuery(
                                "UPDATE todo SET sort_order = sort_order + 10 " +
                                        "WHERE checklist_id = ?1 AND sort_order > ?2")
                        .setParameter(1, checklistId)
                        .setParameter(2, targetSort)
                        .executeUpdate();
                em.getTransaction().commit();

                // Now there's a gap of at least 10 after the target
                return targetSort + 5;
            }
        }
    }

    private ToDo createToDo(Task task, CheckList checkList, int sortOrder) {
        ToDo toDo = new ToDo();
        toDo.setComplete(false);
        toDo.setTask(task);
        toDo.setCheckList(checkList);
        toDo.setSortOrder(sortOrder);
        return toDo;
    }

    private EntityManager getOpenEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf =
                (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }
}


package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.PSP;

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
            return local.getCurrentChecklist().getCheckList();
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

    private int calculateSortOrder(EntityManager em, List<ToDoOut25> toDoList, long positionId) {
        OptionalInt sortValue;

        if (positionId == -1) {
            sortValue = toDoList.stream().mapToInt(ToDoOut25::getSortOrder).max();
            return sortValue.orElse(0) + 1;
        } else if (positionId != 0) {
            ToDo targetToDo = dM.getToDoById(em, positionId);
            return targetToDo != null ? targetToDo.getSortOrder() : 0;
        } else {
            sortValue = toDoList.stream().mapToInt(ToDoOut25::getSortOrder).min();
            return sortValue.orElse(0) - 1;
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


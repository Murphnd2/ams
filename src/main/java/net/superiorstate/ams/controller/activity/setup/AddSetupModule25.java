package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.data.dao.ActivityDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.sales.application.Application;

import net.superiorstate.ams.data.dao.CompositeOrderDAO;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet(name = "AddSetupModule25", value = "/AddSetupModule25")
public class AddSetupModule25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoActivityDetail25");
        dispatcher.forward(request,response);
    }
    private void doThis(HttpServletRequest request) {
        HttpSession session = request.getSession();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || local.getCurrentActivity() == null)
            return;

        session.setAttribute("vp",1);
        session.setAttribute("pastActivityId", local.getCurrentActivity().getActivity().getId().toString());

        Activity a = local.getCurrentActivity().getActivity();
        if (!(a instanceof Setup))
            return;

        Setup s = (Setup) a;
        Application app = s.getApplication();
        if (app == null)
            return;

        int tpId;
        try {
            tpId = Integer.parseInt(request.getParameter("remainingModList"));
        } catch (Exception e) {
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            ServiceItem tp = EntityLookup.getServiceItemById(em, tpId);
            ActivityDAO.addModule(em, app, tp);

            // Load and update checklist
            CheckList originalChecklist = s.getCheckList();
            CheckList dbChecklist = EntityLookup.getCheckListById(em, originalChecklist.getId());
            if (dbChecklist == null)
                return;

            addMissingTasksFromServiceItem(em, tp, dbChecklist);

            // Refresh updated checklist from database
            CheckList updatedChecklist = em.find(CheckList.class, dbChecklist.getId());

            // Update session-scoped checklist
            ((Setup) local.getCurrentActivity().getActivity()).setCheckList(updatedChecklist);

            // Build and assign ToDoOut25 list
            List<ToDoOut25> toDoOutList = updatedChecklist.getToDoList().stream()
                    .map(ToDoOut25::new)
                    .collect(Collectors.toList());
            local.getCurrentActivity().setToDoList(toDoOutList);

        } finally {
            em.close();
        }
    }




    private void addMissingTasksFromServiceItem(
            EntityManager em,
            ServiceItem tp,
            CheckList checkList) {

        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.serviceItem.id = :id");
        q.setParameter("id", tp.getId());

        RequiredTaskList rtl;
        try {
            rtl = (RequiredTaskList) q.getSingleResult();
        } catch (NoResultException e) {
            return;
        }

        // Check for composite ordering
        int groupId = tp.getActivityCategory().getId();
        Long pspId = rtl.getPsp().getId();
        Map<Long, Integer> compositeMap = CompositeOrderDAO.getCompositeOrderMap(em, pspId, groupId);

        Set<Long> existingTaskIds = checkList.getToDoList().stream()
                .map(t -> t.getTask().getId())
                .collect(Collectors.toSet());

        for (TaskSequenceTable seq : rtl.getTaskSequenceTableList()) {
            Long taskId = seq.getTask().getId();

            if (!existingTaskIds.contains(taskId)) {
                Task task = em.find(Task.class, taskId);
                if (task != null) {
                    ToDo toDo = new ToDo();
                    toDo.setTask(task);
                    // Use composite sort order if available, otherwise per-sequence order
                    Integer compositePos = compositeMap.get(taskId);
                    toDo.setSortOrder(compositePos != null ? compositePos : seq.getSortOrder());
                    toDo.setCheckList(checkList);
                    toDo.setComplete(task.getId() == 153L);

                    em.getTransaction().begin();
                    em.persist(toDo);
                    checkList.getToDoList().add(toDo);
                    em.persist(checkList);
                    em.getTransaction().commit();

                    existingTaskIds.add(taskId); // Prevent re-processing if method is reentered
                }
            }
        }

        // Re-sort entire checklist by sort order (composite or per-sequence)
        checkList.getToDoList().sort(Comparator.comparingInt(ToDo::getSortOrder));
    }





}

package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25p;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.Checklist25;
import net.superiorstate.ams.model.Checklist25u;
import net.superiorstate.ams.data.dao.RecurringChecklistDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.UpcomingSequence;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "CloseActivity25", value = "/CloseActivity25")
public class CloseActivity25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processActivityClosure(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processActivityClosure(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request, response);
    }

    private void processActivityClosure(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");

            Activity activity = EntityLookup.getActivityById(em, local.getCurrentActivity().getActivity().getId());
            if (activity == null) return;

            closeActivityInDatabase(em, activity, local);
            updateInMemoryStructures(activity, local, global, em);

            local.respondToActivityUpdate(em, "CLOSE_ACTIVITY", activity);

            request.getSession().setAttribute("local", local);
            request.getServletContext().setAttribute("global", global);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private void closeActivityInDatabase(EntityManager em, Activity activity, AmsDataLocal local) {
        em.getTransaction().begin();
        activity.setComplete(true);
        activity.setDateCompleted(Date.valueOf(LocalDate.now()));
        activity.setCompletedBy(local.getCurrentPerson());
        em.persist(activity);
        em.getTransaction().commit();
        em.refresh(activity);
    }

    private void updateInMemoryStructures(Activity activity, AmsDataLocal local, AmsDataGlobal global, EntityManager em) {
        if (!activity.getClass().getSimpleName().equals("CheckList")) {
            updateOpenActivityLists(activity, local, global);
        } else {
            processChecklist((CheckList) activity, local, em);
        }
    }

    private void updateOpenActivityLists(Activity activity, AmsDataLocal local, AmsDataGlobal global) {
        List<Activity25u> updatedAllOpen = global.getActivitiesAllOpen().stream()
                .filter(au -> !au.getActivity().getId().equals(activity.getId()))
                .toList();

        List<Activity25p> updatedWithDelegation = global.getActivitiesWithDelegation().stream()
                .filter(ap -> !ap.getActivity().getId().equals(activity.getId()))
                .toList();

        global.setActivitiesAllOpen(updatedAllOpen);
        local.setActivitiesAllOpen(updatedAllOpen);
        global.setActivitiesWithDelegation(updatedWithDelegation);
        local.setActivitiesWithDependencies(updatedWithDelegation);

        local.getCurrentActivity().setReFilterOnExit(true);
        local.setNextView("pspHome");
    }

    private void processChecklist(CheckList checklist, AmsDataLocal local, EntityManager em) {
        handleRecurringChecklist(checklist, local, em);
        updateChecklist25u(checklist, local);
        local.splitChecklists();
    }

    private void handleRecurringChecklist(CheckList checklist, AmsDataLocal local, EntityManager em) {
        if (checklist.getRecurringTaskList() != null && !checklist.getRecurringTaskList().isInActive()) {
            UpcomingSequence us = RecurringChecklistDAO.getUpcomingSequence(em, checklist.getRecurringTaskList());
            if (us != null) {
                RecurringTaskList rtl = checklist.getRecurringTaskList();
                Person owner = (rtl != null && rtl.getAssignee() != null)
                        ? rtl.getAssignee()
                        : local.getCurrentPerson();

                CheckList newChecklist = RecurringChecklistDAO.createNewRecurringChecklist(em, us, owner);
                Checklist25 newChecklist25 = (Checklist25) em.createQuery(
                                "SELECT c FROM Checklist25 c WHERE c.activity.id = :id")
                        .setParameter("id", newChecklist.getId())
                        .getSingleResult();

                local.getChecklistsAll().add(new Checklist25u(newChecklist25));
            }
        }
    }

    private void updateChecklist25u(CheckList checklist, AmsDataLocal local) {
        Checklist25u checklist25u = local.retrieveChecklist25uFromList(local.getChecklistsAll(), checklist.getId());
        if (checklist25u != null) {
            checklist25u.setComplete(true);
            checklist25u.getActivity().setComplete(true);
            checklist25u.getActivity().setCompletedBy(local.getCurrentPerson());
            checklist25u.getActivity().setDateCompleted(Date.valueOf(LocalDate.now()));
            checklist25u.setSortKey(2);
        }
    }
}




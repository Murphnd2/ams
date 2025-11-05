package net.superiorstate.ams.service;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.general.Person;

import java.sql.Date;
import java.time.LocalDate;

public class ChecklistService {

    public static void handleAction(EntityManager em, HttpServletRequest request, String actionCode, long checklistId) {
        CheckList checklist = em.find(CheckList.class, checklistId);
        if (checklist == null) return;

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        switch (actionCode) {
            case "C" -> handleClose(em, local, checklist);
            case "V" -> handleView(local, checklist);
            case "R" -> handleReassign(local, checklist, request);
            case "D" -> handleDueDateChange(em, checklist, request);
            case "U" -> handleUndo(em, checklist);
        }
    }

    private static void handleClose(EntityManager em, AmsDataLocal local, CheckList checklist) {
        checklist.setComplete(true);
        checklist.setDateCompleted(Date.valueOf(LocalDate.now()));
        checklist.setCompletedBy((Person) local.getCurrentPerson());
        em.getTransaction().begin();
        em.persist(checklist);
        em.getTransaction().commit();
    }

    private static void handleView(AmsDataLocal local, CheckList checklist) {
        AmsDataLocal.CurrentChecklist cc = local.new CurrentChecklist();
        cc.setCheckList(checklist);
        local.setCurrentChecklist(cc);
    }

    private static void handleReassign(AmsDataLocal local, CheckList checklist, HttpServletRequest request) {
        AmsDataLocal.CurrentChecklist cc = local.getCurrentChecklist();
        if (cc == null) {
            cc = local.new CurrentChecklist();
            local.setCurrentChecklist(cc);
        }
        cc.setCheckList(checklist);
        cc.setReassignUrl(request.getRequestURI());  // ⬅️ You'll add this in the class
    }

    private static void handleDueDateChange(EntityManager em, CheckList checklist, HttpServletRequest request) {
        String newDate = request.getParameter("dueDate_" + checklist.getId());
        if (newDate != null && !newDate.isBlank()) {
            checklist.setDueDate(Date.valueOf(newDate));
            em.getTransaction().begin();
            em.persist(checklist);
            em.getTransaction().commit();
        }
    }

    private static void handleUndo(EntityManager em, CheckList checklist) {
        checklist.setComplete(false);
        checklist.setDateCompleted(null);
        checklist.setCompletedBy(null);
        em.getTransaction().begin();
        em.persist(checklist);
        em.getTransaction().commit();
    }
}





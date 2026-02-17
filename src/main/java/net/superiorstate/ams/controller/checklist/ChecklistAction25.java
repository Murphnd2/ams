package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.RecurringChecklistDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Objects;

@WebServlet(name = "ChecklistAction25", value = "/ChecklistAction25")
public class ChecklistAction25 extends HttpServlet {
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        takeAction(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        takeAction(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(getPath());
        dispatcher.forward(request, response);
    }

    private void takeAction(HttpServletRequest request) {
        String buttonCode = request.getParameter("btnCheckList");
        if (buttonCode == null || buttonCode.length() < 3) return;

        String actionCode = buttonCode.substring(0, 1);
        long checklistId;

        try {
            checklistId = Long.parseLong(buttonCode.substring(2));
        } catch (NumberFormatException e) {
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = null;

        try {
            em = emf.createEntityManager();
            CheckList c = EntityLookup.getCheckListById(em, checklistId);
            if (c == null) return;

            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

            switch (actionCode) {
                case "C" -> handleClose(em, local, c, request);
                case "V" -> handleView(em, local, c, request);
                case "R" -> handleReassign(em, local, c, request);
                case "D" -> handleDueDateChange(em, local, c, request);
                case "U" -> handleUndo(em, local, c, request);
            }

        } catch (Exception e) {
            e.printStackTrace(); // Optional: replace with logging
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }


    private void handleClose(EntityManager em, AmsDataLocal local, CheckList c, HttpServletRequest request) {
        setPath("ViewHome25");

        em.getTransaction().begin();
        c.setComplete(true);
        c.setDateCompleted(Date.valueOf(LocalDate.now()));
        c.setCompletedBy(local.getCurrentPerson());
        em.persist(c);
        em.getTransaction().commit();
        em.refresh(c);

        local.respondToActivityUpdate(em, "CLOSE_CHECK", c);
        request.getSession().setAttribute("local", local);
    }

    private void handleView(EntityManager em, AmsDataLocal local, CheckList c, HttpServletRequest request) {
        setPath("ViewChecklist25");

        local.respondToActivityUpdate(em, "VIEW_CHECKLIST", c.getId());
        request.getSession().setAttribute("local", local);
    }

    private void handleReassign(EntityManager em, AmsDataLocal local, CheckList c, HttpServletRequest request) {
        setPath("ViewHome25");

        try {
            long userId = Long.parseLong(request.getParameter("userList"));
            Person user = EntityLookup.getPersonById(em, userId);
            if (user == null) return;

            em.getTransaction().begin();
            c.setAssignedTo(user);
            em.persist(c);
            em.getTransaction().commit();
            em.refresh(c);

            if (!Objects.equals(user.getId(), local.getCurrentPerson().getId())) {
                local.respondToActivityUpdate(em, "CHECK_OWNER", c);
            }

            request.getSession().setAttribute("local", local);
        } catch (Exception ignored) {
        }
    }

    private void handleDueDateChange(EntityManager em, AmsDataLocal local, CheckList c, HttpServletRequest request) {
        setPath("ViewHome25");

        try {
            Date newDate = Date.valueOf(request.getParameter("newDueDate"));
            em.getTransaction().begin();
            c.setDueDate(newDate);
            em.persist(c);
            em.getTransaction().commit();
            em.refresh(c);

            local.respondToActivityUpdate(em, "CHECK_DATE", c);
            request.getSession().setAttribute("local", local);
        } catch (Exception ignored) {
        }
    }

    private void handleUndo(EntityManager em, AmsDataLocal local, CheckList c, HttpServletRequest request) {
        setPath("ViewHome25");

        em.getTransaction().begin();
        c.setComplete(false);
        c.setCompletedBy(null);
        c.setDateCompleted(null);
        em.persist(c);
        em.getTransaction().commit();
        em.refresh(c);

        RecurringChecklistDAO.removeFutureRecurring(em, c);
        local.respondToActivityUpdate(em, "CHECK_UNDO", c);

        request.getSession().setAttribute("local", local);
    }
}


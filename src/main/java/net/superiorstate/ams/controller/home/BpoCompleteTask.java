package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

@WebServlet(name = "BpoCompleteTask", value = "/BpoCompleteTask")
public class BpoCompleteTask extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("complete".equals(action)) {
            markComplete(request);
            response.sendRedirect("BpoHome");
        } else if ("addNote".equals(action)) {
            addNote(request);
            response.setStatus(200);
        }
    }

    private void markComplete(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();

            long todoId = Long.parseLong(request.getParameter("todoId"));
            ToDo todo = em.find(ToDo.class, todoId);
            if (todo == null) return;

            em.getTransaction().begin();
            todo.setBpoCompleted(true);
            todo.setBpoCompletedDate(Date.valueOf(LocalDate.now()));
            todo.setBpoCompletedBy(currentUser);
            em.persist(todo);

            // Auto-add a completion note
            ToDoNote note = new ToDoNote();
            note.setToDo(todo);
            note.setCreatedBy(currentUser);
            note.setNoteText("Task marked complete by BPO.");
            note.setSourceType("BPO");
            em.persist(note);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private void addNote(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();

            long todoId = Long.parseLong(request.getParameter("todoId"));
            String noteText = request.getParameter("noteText");
            if (noteText == null || noteText.trim().isEmpty()) return;

            ToDo todo = em.find(ToDo.class, todoId);
            if (todo == null) return;

            em.getTransaction().begin();
            ToDoNote note = new ToDoNote();
            note.setToDo(todo);
            note.setCreatedBy(currentUser);
            note.setNoteText(noteText.trim());
            note.setSourceType("BPO");
            em.persist(note);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}

package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.activity.checklist.tasks.DelegatedToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.PspClient;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

@WebServlet(name = "BpoCompleteTask", value = "/BpoCompleteTask")
public class BpoCompleteTask extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        boolean crossSystem = "true".equals(request.getParameter("crossSystem"));

        if ("complete".equals(action)) {
            if (crossSystem) {
                markCompleteCrossSystem(request);
            } else {
                markComplete(request);
            }
            response.sendRedirect("BpoHome");
        } else if ("addNote".equals(action)) {
            if (crossSystem) {
                addNoteCrossSystem(request);
            } else {
                addNote(request);
            }
            response.setStatus(200);
        } else if ("assign".equals(action)) {
            if (crossSystem) {
                assignTaskCrossSystem(request);
            } else {
                assignTask(request);
            }
            response.setStatus(200);
        }
    }

    // ═══ CO-LOCATED MODE (local ToDo) ═══

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

    private void assignTask(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long todoId = Long.parseLong(request.getParameter("todoId"));
            String assigneeIdStr = request.getParameter("assigneeId");

            ToDo todo = em.find(ToDo.class, todoId);
            if (todo == null) return;

            em.getTransaction().begin();

            if (assigneeIdStr == null || assigneeIdStr.isEmpty() || "0".equals(assigneeIdStr)) {
                todo.setBpoAssignedTo(null);
            } else {
                long assigneeId = Long.parseLong(assigneeIdStr);
                Person assignee = em.find(Person.class, assigneeId);
                todo.setBpoAssignedTo(assignee);
            }

            em.persist(todo);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // ═══ CROSS-SYSTEM MODE (DelegatedToDo) ═══

    private void markCompleteCrossSystem(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();

            long dtId = Long.parseLong(request.getParameter("todoId"));
            DelegatedToDo dt = em.find(DelegatedToDo.class, dtId);
            if (dt == null) return;

            em.getTransaction().begin();
            dt.setCompleted(true);
            dt.setCompletedDate(Date.valueOf(LocalDate.now()));
            dt.setCompletedBy(currentUser);
            em.merge(dt);

            // Add local completion note
            ToDoNote note = new ToDoNote();
            note.setTodoGuid(dt.getTodoGuid());
            note.setCreatedBy(currentUser);
            note.setNoteText("Task marked complete by BPO.");
            note.setSourceType("BPO");
            em.persist(note);

            em.getTransaction().commit();

            // Callback to PSP (non-fatal, AFTER commit)
            callbackTaskCompleted(dt, currentUser);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private void addNoteCrossSystem(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();

            String todoGuid = request.getParameter("todoGuid");
            String noteText = request.getParameter("noteText");
            if (todoGuid == null || noteText == null || noteText.trim().isEmpty()) return;

            em.getTransaction().begin();
            ToDoNote note = new ToDoNote();
            note.setTodoGuid(todoGuid);
            note.setCreatedBy(currentUser);
            note.setNoteText(noteText.trim());
            note.setSourceType("BPO");
            em.persist(note);
            em.getTransaction().commit();

            // Callback to PSP (non-fatal, AFTER commit)
            callbackNoteAdded(em, todoGuid, noteText.trim(), currentUser);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private void assignTaskCrossSystem(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long dtId = Long.parseLong(request.getParameter("todoId"));
            String assigneeIdStr = request.getParameter("assigneeId");

            DelegatedToDo dt = em.find(DelegatedToDo.class, dtId);
            if (dt == null) return;

            em.getTransaction().begin();

            if (assigneeIdStr == null || assigneeIdStr.isEmpty() || "0".equals(assigneeIdStr)) {
                dt.setAssignedTo(null);
            } else {
                long assigneeId = Long.parseLong(assigneeIdStr);
                Person assignee = em.find(Person.class, assigneeId);
                dt.setAssignedTo(assignee);
            }

            em.merge(dt);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // ═══ PSP CALLBACKS (non-fatal) ═══

    private void callbackTaskCompleted(DelegatedToDo dt, Person completedBy) {
        try {
            PspClient psp = dt.getPspClient();
            if (psp == null || psp.getPspUrl() == null || psp.getApiTokenOutbound() == null) return;

            String url = psp.getPspUrl() + "/api/v1/callback/task-completed";
            Map<String, String> payload = Map.of(
                    "todoGuid", dt.getTodoGuid(),
                    "completedByName", completedBy.getFullName()
            );
            ApiClient.postJson(url, payload, psp.getApiTokenOutbound());
        } catch (Exception e) {
            System.err.println("BpoCompleteTask callback (task-completed) failed (non-fatal): " + e.getMessage());
        }
    }

    private void callbackNoteAdded(EntityManager em, String todoGuid, String noteText, Person author) {
        try {
            // Find PspClient from the DelegatedToDo
            var matches = em.createQuery(
                            "SELECT d.pspClient FROM DelegatedToDo d WHERE d.todoGuid = :guid", PspClient.class)
                    .setParameter("guid", todoGuid)
                    .getResultList();
            if (matches.isEmpty()) return;

            PspClient psp = matches.get(0);
            if (psp.getPspUrl() == null || psp.getApiTokenOutbound() == null) return;

            String url = psp.getPspUrl() + "/api/v1/callback/note-added";
            Map<String, String> payload = Map.of(
                    "todoGuid", todoGuid,
                    "noteText", noteText,
                    "authorName", author.getFullName()
            );
            ApiClient.postJson(url, payload, psp.getApiTokenOutbound());
        } catch (Exception e) {
            System.err.println("BpoCompleteTask callback (note-added) failed (non-fatal): " + e.getMessage());
        }
    }
}

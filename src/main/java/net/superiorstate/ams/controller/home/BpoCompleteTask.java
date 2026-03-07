package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.checklist.tasks.DelegatedToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.PspClient;
import net.superiorstate.ams.model.general.WebLink;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@WebServlet(name = "BpoCompleteTask", value = "/BpoCompleteTask")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 1024 * 1024 * 10,        // 10 MB
        maxRequestSize = 1024 * 1024 * 100     // 100 MB
)
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
        } else if ("accept".equals(action)) {
            acceptTasksCrossSystem(request);
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

            // Persist the note
            em.getTransaction().begin();
            ToDoNote note = new ToDoNote();
            note.setToDo(todo);
            note.setCreatedBy(currentUser);
            note.setNoteText(noteText.trim());
            note.setSourceType("BPO");
            em.persist(note);
            em.getTransaction().commit();

            // Handle optional file attachment (separate transaction after note has ID)
            uploadNoteAttachment(request, em, local, note);

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

            // Persist the note
            em.getTransaction().begin();
            ToDoNote note = new ToDoNote();
            note.setTodoGuid(todoGuid);
            note.setCreatedBy(currentUser);
            note.setNoteText(noteText.trim());
            note.setSourceType("BPO");
            em.persist(note);
            em.getTransaction().commit();

            // Handle optional file attachment (separate transaction after note has ID)
            WebLink attachment = uploadNoteAttachment(request, em, local, note);

            // Callback to PSP (non-fatal, AFTER commit)
            callbackNoteAdded(em, todoGuid, noteText.trim(), currentUser, local, attachment);

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

    private void acceptTasksCrossSystem(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            String todoIdsParam = request.getParameter("todoIds");
            String assigneeIdStr = request.getParameter("assigneeId");
            if (todoIdsParam == null || todoIdsParam.isBlank()) return;

            Person assignee = null;
            if (assigneeIdStr != null && !assigneeIdStr.isEmpty() && !"0".equals(assigneeIdStr)) {
                assignee = em.find(Person.class, Long.parseLong(assigneeIdStr));
            }

            em.getTransaction().begin();

            for (String idStr : todoIdsParam.split(",")) {
                long dtId = Long.parseLong(idStr.trim());
                DelegatedToDo dt = em.find(DelegatedToDo.class, dtId);
                if (dt == null || !"PENDING".equals(dt.getStatus())) continue;

                dt.setStatus("ACTIVE");
                if (assignee != null) {
                    dt.setAssignedTo(assignee);
                }
                em.merge(dt);
            }

            em.getTransaction().commit();
            System.out.println("[BPO] Accepted tasks: " + todoIdsParam +
                    (assignee != null ? " assigned to " + assignee.getFullName() : ""));
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // ═══ FILE ATTACHMENT HELPER ═══

    /**
     * Checks for an uploaded file ("noteFile" part) and, if present, uploads it to Wasabi
     * and persists a WebLink attached to the given ToDoNote.
     * Returns the WebLink if created, null otherwise.
     */
    private WebLink uploadNoteAttachment(HttpServletRequest request, EntityManager em,
                                         AmsDataLocal local, ToDoNote note) {
        try {
            Part filePart = request.getPart("noteFile");
            if (filePart == null || filePart.getSize() == 0) return null;

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String extension = Validator.getExtensionByStringHandling(fileName).orElse("bin");
            String objectKey = UUID.randomUUID() + "." + extension;
            String displayName = fileName.replaceAll(" ", "_");
            String pspName = local.getCurrentPerson().getPsp().getFullName();

            try (InputStream is = filePart.getInputStream()) {
                StorageDAO.uploadFile(em, pspName, objectKey, displayName, is,
                        filePart.getSize(), filePart.getContentType());
            }

            // Persist WebLink attached to the note (new transaction)
            em.getTransaction().begin();
            WebLink w = new WebLink();
            w.setPlainText(displayName);
            w.setLinkPath(objectKey);
            LinkType linkType = SequenceDAO.getLinkTypeById(em, 1); // type 1 = file
            w.setLinkType(linkType);
            w.setActive(true);
            w.setToDoNote(note);
            em.persist(w);
            em.getTransaction().commit();
            em.getEntityManagerFactory().getCache().evict(ToDoNote.class, note.getId());

            System.out.println("[BPO] Note attachment uploaded: " + displayName + " → " + objectKey);
            return w;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("[BPO] Note attachment upload failed (non-fatal): " + e.getMessage());
            return null;
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
            ApiClient.ApiResponse resp = ApiClient.postJson(url, payload, psp.getApiTokenOutbound());
            System.out.println("[BPO-API] callbackTaskCompleted: todoGuid=" + dt.getTodoGuid() + " to " + psp.getPspName() + " (" + resp.statusCode + ")");
        } catch (Exception e) {
            System.out.println("[BPO-API] callbackTaskCompleted: failed (non-fatal): " + e.getMessage());
        }
    }

    private void callbackNoteAdded(EntityManager em, String todoGuid, String noteText, Person author,
                                   AmsDataLocal local, WebLink attachment) {
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

            // Build payload with optional attachment metadata
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("todoGuid", todoGuid);
            payload.put("noteText", noteText);
            payload.put("authorName", author.getFullName());

            if (attachment != null) {
                String pspName = local.getCurrentPerson().getPsp().getFullName();
                String downloadUrl = StorageDAO.getDownloadUrl(null, pspName, attachment.getLinkPath(), Duration.ofDays(7));
                List<Map<String, String>> attachments = List.of(Map.of(
                        "displayName", attachment.getPlainText(),
                        "downloadUrl", downloadUrl
                ));
                payload.put("attachments", attachments);
            }

            ApiClient.ApiResponse resp = ApiClient.postJsonObject(url, payload, psp.getApiTokenOutbound());
            System.out.println("[BPO-API] callbackNoteAdded: todoGuid=" + todoGuid + " to " + psp.getPspName() + " (" + resp.statusCode + ")");
        } catch (Exception e) {
            System.out.println("[BPO-API] callbackNoteAdded: failed (non-fatal): " + e.getMessage());
        }
    }
}

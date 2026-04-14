package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.general.BpoRegistration;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

/**
 * PSP-side endpoint for adding notes (with optional file attachment) to a ToDo.
 * If the task is sourced, fires a cross-system callback to the BPO's TaskNotesApi.
 */
@WebServlet(name = "AddNoteToToDo25", value = "/AddNoteToToDo25")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 1024 * 1024 * 10,        // 10 MB
        maxRequestSize = 1024 * 1024 * 100     // 100 MB
)
public class AddNoteToToDo25 extends HttpServlet {

    private static final Logger log = LogManager.getLogger(AddNoteToToDo25.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();

            long todoId = Long.parseLong(request.getParameter("todoId"));
            String noteText = request.getParameter("noteText");
            if (noteText == null || noteText.trim().isEmpty()) {
                response.setStatus(400);
                return;
            }

            ToDo todo = em.find(ToDo.class, todoId);
            if (todo == null) {
                response.setStatus(404);
                return;
            }

            // Persist the note
            em.getTransaction().begin();
            ToDoNote note = new ToDoNote();
            note.setToDo(todo);
            note.setTodoGuid(todo.getTodoGuid());
            note.setCreatedBy(currentUser);
            note.setNoteText(noteText.trim());
            note.setSourceType("PSP");
            em.persist(note);
            em.getTransaction().commit();

            // Handle optional file attachment (separate transaction after note has ID)
            WebLink attachment = uploadNoteAttachment(request, em, local, note);

            // Cross-system callback to BPO (non-fatal, AFTER commit)
            if (todo.getTask().isSourced() && todo.getTask().getBpoRegistration() != null) {
                callbackNoteToBpo(em, todo, noteText.trim(), currentUser, local, attachment);
            }

            response.setStatus(200);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            response.setStatus(500);
        } finally {
            em.close();
        }
    }

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

            log.info("[PSP] Note attachment uploaded: {} -> {}", displayName, objectKey);
            return w;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            log.warn("[PSP] Note attachment upload failed (non-fatal): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fires a cross-system callback to the BPO's TaskNotesApi POST endpoint
     * so the BPO also receives the PSP-authored note.
     */
    private void callbackNoteToBpo(EntityManager em, ToDo todo, String noteText,
                                    Person author, AmsDataLocal local, WebLink attachment) {
        try {
            BpoRegistration reg = todo.getTask().getBpoRegistration();
            if (reg == null || reg.getPartnerUrl() == null || reg.getApiTokenOutbound() == null) return;

            String url = reg.getPartnerUrl() + "/api/v1/tasks/notes";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("todoGuid", todo.getTodoGuid());
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

            ApiClient.ApiResponse resp = ApiClient.postJsonObject(url, payload, reg.getApiTokenOutbound());
            log.info("[BPO-API] callbackNoteToBpo: todoGuid={} ({})", todo.getTodoGuid(), resp.statusCode);
        } catch (Exception e) {
            log.warn("[BPO-API] callbackNoteToBpo: failed (non-fatal): {}", e.getMessage());
        }
    }
}

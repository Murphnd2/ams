package net.superiorstate.ams.controller.api.outlook;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * POST /api/v1/outlook/log-email
 *
 * Logs an Outlook email as a Note on an AMS activity. Any attached files are
 * uploaded to Wasabi (via StorageDAO) and linked to the new Note via WebLink
 * records using the note_id FK added in V060.
 *
 * Multipart form fields:
 *   activityId       (Long)   target activity
 *   statusId         (int)    ActivityStatus id (1=Waiting on Them, 3=Waiting on Us)
 *   subject          (String) email subject line
 *   body             (String) email body (plain or HTML)
 *   senderName       (String) sender's display name
 *   senderEmail      (String) sender's email address
 *   receivedDate     (String) ISO-format received date (free text)
 *   attachmentCount  (int)    number of file_N / fileName_N pairs
 *   file_0..N        (Part)   attachment file content
 *   fileName_0..N    (String) original filename for each attachment
 *
 * ReasonCreated is hard-coded to id=4 ("Received Email") — seeded by
 * DatabaseInitializer.
 */
@WebServlet(name = "OutlookLogEmailApi", urlPatterns = "/api/v1/outlook/log-email")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1 MB
        maxFileSize = 20L * 1024 * 1024,        // 20 MB per file
        maxRequestSize = 50L * 1024 * 1024      // 50 MB total
)
public class OutlookLogEmailApi extends HttpServlet {

    private static final int REASON_RECEIVED_EMAIL = 4;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // --- Auth ---
            Person caller = OutlookApiHelper.validateOutlookToken(em, request);
            if (caller == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or missing Outlook API token");
                return;
            }

            // --- Parse form fields ---
            Long activityId;
            int statusId;
            int attachmentCount;
            try {
                activityId = Long.parseLong(request.getParameter("activityId"));
                statusId = Integer.parseInt(request.getParameter("statusId"));
                String ac = request.getParameter("attachmentCount");
                attachmentCount = (ac != null && !ac.isBlank()) ? Integer.parseInt(ac) : 0;
            } catch (NumberFormatException nfe) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid numeric field: " + nfe.getMessage());
                return;
            }

            String subject = nullToEmpty(request.getParameter("subject"));
            String body = nullToEmpty(request.getParameter("body"));
            String senderName = nullToEmpty(request.getParameter("senderName"));
            String senderEmail = nullToEmpty(request.getParameter("senderEmail"));
            String receivedDate = nullToEmpty(request.getParameter("receivedDate"));

            // --- Resolve target activity and verify PSP scope ---
            Activity activity = EntityLookup.getActivityById(em, activityId);
            if (activity == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Activity not found: " + activityId);
                return;
            }
            if (caller.getPsp() != null
                    && activity.getLoggedBy() != null
                    && activity.getLoggedBy().getPsp() != null
                    && !caller.getPsp().getId().equals(activity.getLoggedBy().getPsp().getId())) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Activity does not belong to your PSP");
                return;
            }

            ActivityStatus status = EntityLookup.getActivityStatusById(em, statusId);
            if (status == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Unknown statusId: " + statusId);
                return;
            }
            ReasonCreated reason = EntityLookup.getReasonById(em, REASON_RECEIVED_EMAIL);
            if (reason == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Reason 'Received Email' (id=4) not seeded in this environment");
                return;
            }

            // --- Build note detail ---
            StringBuilder detail = new StringBuilder();
            detail.append("Email logged from Outlook\n");
            detail.append("From: ").append(senderName);
            if (!senderEmail.isEmpty()) detail.append(" <").append(senderEmail).append(">");
            detail.append("\n");
            if (!receivedDate.isEmpty()) detail.append("Date: ").append(receivedDate).append("\n");
            detail.append("Subject: ").append(subject).append("\n\n");
            detail.append(body);

            String detailStr = detail.toString();
            if (detailStr.length() > 5000) {
                detailStr = detailStr.substring(0, 4995) + "\n...";
            }

            // --- Persist the Note ---
            Note note = new Note();
            note.setActivity(activity);
            note.setDetail(detailStr);
            note.setStatus(status);
            note.setReasonCreated(reason);
            note.setDateGenerated(Date.valueOf(LocalDate.now()));
            note.setCreatedBy(caller);
            note.setResolution(false);

            em.getTransaction().begin();
            em.persist(note);
            em.getTransaction().commit();

            // Link the new note to the activity's noteList (mirrors AddNoteToActivity25)
            em.getTransaction().begin();
            Activity fresh = EntityLookup.getActivityById(em, activityId);
            if (fresh != null && fresh.getNoteList() != null) {
                fresh.getNoteList().add(note);
                em.persist(fresh);
            }
            em.getTransaction().commit();

            // --- Handle attachments ---
            int uploaded = 0;
            String pspName = caller.getPsp() != null ? caller.getPsp().getFullName() : "default";
            LinkType fileLinkType = SequenceDAO.getLinkTypeById(em, 1);
            List<WebLink> webLinks = new ArrayList<>();

            for (int i = 0; i < attachmentCount; i++) {
                Part filePart = request.getPart("file_" + i);
                if (filePart == null) continue;

                String originalName = request.getParameter("fileName_" + i);
                if (originalName == null || originalName.isBlank()) {
                    String submitted = filePart.getSubmittedFileName();
                    originalName = (submitted != null)
                            ? Paths.get(submitted).getFileName().toString()
                            : "attachment_" + i;
                }

                String extension = Validator.getExtensionByStringHandling(originalName).orElse("bin");
                String objectKey = UUID.randomUUID() + "." + extension;
                String displayName = originalName.replaceAll(" ", "_");
                if (!displayName.toLowerCase().endsWith("." + extension.toLowerCase())) {
                    displayName = displayName + "." + extension;
                }

                String contentType = filePart.getContentType();
                long contentLength = filePart.getSize();

                try (InputStream in = filePart.getInputStream()) {
                    StorageDAO.uploadFile(em, pspName, objectKey, displayName, in, contentLength, contentType);
                }

                em.getTransaction().begin();
                WebLink wl = new WebLink();
                wl.setPlainText(displayName);
                wl.setLinkPath(objectKey);
                wl.setLinkType(fileLinkType);
                wl.setActive(true);
                wl.setNote(note);
                em.persist(wl);
                em.getTransaction().commit();

                webLinks.add(wl);
                uploaded++;
            }

            String json = "{"
                    + "\"success\":true,"
                    + "\"noteId\":" + note.getId() + ","
                    + "\"attachmentsUploaded\":" + uploaded
                    + "}";
            OutlookApiHelper.sendJson(response, HttpServletResponse.SC_OK, json);

        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) {
                try { em.getTransaction().rollback(); } catch (Exception ignored) {}
            }
            OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to log email: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

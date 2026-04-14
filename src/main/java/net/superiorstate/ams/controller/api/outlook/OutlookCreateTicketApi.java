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
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.dao.TicketQueryDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.resolver.PersonResolver;
import net.superiorstate.ams.data.service.BpoTaskPushService;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * POST /api/v1/outlook/create-ticket
 *
 * Creates a new Ticket from an Outlook email. The email body becomes the first
 * Note on the ticket (ReasonCreated=4 "Received Email", ActivityStatus=3
 * "Waiting on Us"). Attachments are uploaded to Wasabi and linked via WebLink.
 *
 * Multipart form fields:
 *   serviceItemId    (int)    ServiceItem ID from ticket-categories dropdown;
 *                             0 = custom reason (requires customReason field)
 *   customReason     (String) free-text reason when serviceItemId=0
 *   primaryContactEmail (String) primary contact email (resolved via Employee/Person chain)
 *   primaryContactName  (String) primary contact display name (fallback)
 *   additionalContacts  (String) JSON array of {email, name} — added as assignee contacts
 *   senderEmail      (String) sender's email (for note detail)
 *   senderName       (String) sender's display name (for note detail)
 *   description      (String) ticket description (defaults to email subject in UI)
 *   subject          (String) email subject line (for note detail)
 *   body             (String) email body (for note detail)
 *   receivedDate     (String) ISO-format received date (free text, for note)
 *   attachmentCount  (int)    number of file_N / fileName_N pairs
 *   file_0..N        (Part)   attachment file content
 *   fileName_0..N    (String) original filename for each attachment
 *
 * Returns: { "success": true, "activityId": 123 }
 */
@WebServlet(name = "OutlookCreateTicketApi", urlPatterns = "/api/v1/outlook/create-ticket")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1 MB
        maxFileSize = 20L * 1024 * 1024,        // 20 MB per file
        maxRequestSize = 50L * 1024 * 1024      // 50 MB total
)
public class OutlookCreateTicketApi extends HttpServlet {

    private static final int REASON_RECEIVED_EMAIL = 4;
    private static final int STATUS_WAITING_ON_US = 3;
    private static final int CONTACT_METHOD_EMAIL = 2;

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
            int serviceItemId;
            int attachmentCount;
            try {
                String siParam = request.getParameter("serviceItemId");
                serviceItemId = (siParam != null && !siParam.isBlank()) ? Integer.parseInt(siParam) : 0;
                String ac = request.getParameter("attachmentCount");
                attachmentCount = (ac != null && !ac.isBlank()) ? Integer.parseInt(ac) : 0;
            } catch (NumberFormatException nfe) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid numeric field: " + nfe.getMessage());
                return;
            }

            String customReason = nullToEmpty(request.getParameter("customReason"));
            String senderEmail = nullToEmpty(request.getParameter("senderEmail"));
            String senderName = nullToEmpty(request.getParameter("senderName"));
            String description = nullToEmpty(request.getParameter("description"));
            String subject = nullToEmpty(request.getParameter("subject"));
            String body = nullToEmpty(request.getParameter("body"));
            String receivedDate = nullToEmpty(request.getParameter("receivedDate"));
            String primaryContactEmail = nullToEmpty(request.getParameter("primaryContactEmail"));
            String primaryContactName = nullToEmpty(request.getParameter("primaryContactName"));
            String additionalContactsJson = nullToEmpty(request.getParameter("additionalContacts"));

            if (description.isBlank()) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Description is required");
                return;
            }

            String primaryInput = !primaryContactEmail.isBlank() ? primaryContactEmail : primaryContactName;
            if (primaryInput.isBlank()) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Primary contact email or name is required");
                return;
            }

            // --- Resolve ServiceItem (mirrors CreateTicket25.processTicketType) ---
            ServiceItem serviceItem;
            if (serviceItemId == 0) {
                if (customReason.isBlank()) {
                    OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "customReason is required when serviceItemId is 0");
                    return;
                }
                serviceItem = createCustomServiceItem(em, customReason);
            } else {
                serviceItem = EntityLookup.getServiceItemById(em, serviceItemId);
                if (serviceItem == null) {
                    OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Unknown serviceItemId: " + serviceItemId);
                    return;
                }
            }

            // --- Resolve primary contact (same chain as CreateTicket25) ---
            Person contact = resolveContactFromFreeform(em, primaryInput);
            if (contact == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not resolve or create contact from: " + primaryInput);
                return;
            }

            // --- Resolve additional contacts from checked list ---
            List<String[]> addlRecipients = parseRecipientArray(additionalContactsJson);
            List<Person> additionalContacts = new ArrayList<>();
            for (String[] r : addlRecipients) {
                String input = !r[0].isBlank() ? r[0] : r[1];
                if (!input.isBlank()) {
                    Person p = resolveContactFromFreeform(em, input);
                    if (p != null && !p.getId().equals(contact.getId())) {
                        boolean alreadyAdded = false;
                        for (Person existing : additionalContacts) {
                            if (existing.getId().equals(p.getId())) { alreadyAdded = true; break; }
                        }
                        if (!alreadyAdded) additionalContacts.add(p);
                    }
                }
            }

            // --- Resolve supporting entities ---
            ContactMethod contactMethod = em.find(ContactMethod.class, CONTACT_METHOD_EMAIL);
            ReasonCreated reason = EntityLookup.getReasonById(em, REASON_RECEIVED_EMAIL);
            ActivityStatus status = EntityLookup.getActivityStatusById(em, STATUS_WAITING_ON_US);

            if (reason == null || status == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Required seed data missing (ReasonCreated=4 or ActivityStatus=3)");
                return;
            }

            // --- Create the Ticket ---
            em.getTransaction().begin();
            Ticket ticket = new Ticket();
            ticket.setAssignedTo(caller);
            ticket.setLoggedBy(caller);
            ticket.setDueDate(Date.valueOf(LocalDate.now().plusDays(7)));
            ticket.setDescription(description);
            ticket.setTicketServiceItem(serviceItem);
            ticket.setContact(contact);
            ticket.setPrimaryContact(contact);
            ticket.setComplete(false);
            ticket.setContactMethod(contactMethod);
            ticket.setFullName(contact.getFirstName().trim().toUpperCase() + " "
                    + contact.getLastName().trim().toUpperCase());
            em.persist(ticket);
            em.getTransaction().commit();

            // --- Create CheckList + ToDo tasks (mirrors CreateTicket25) ---
            CheckList checklist = createCheckListForTicket(em, ticket, caller);

            em.getTransaction().begin();
            Ticket freshTicket = em.find(Ticket.class, ticket.getId());
            freshTicket.setCheckList(checklist);
            em.persist(freshTicket);
            em.getTransaction().commit();

            em.getTransaction().begin();
            CheckList freshChecklist = EntityLookup.getCheckListById(em, checklist.getId());
            freshChecklist.setTicket(freshTicket);
            em.persist(freshChecklist);
            em.getTransaction().commit();

            // Push BPO tasks
            BpoTaskPushService.pushDelegatedTasks(em, freshChecklist);

            // Attach questionnaires
            QuestionnaireService.attachMatchingQuestionnaires(em, freshTicket, "TICKET",
                    caller.getPsp().getId());

            // --- Add additional contacts (remaining To + CC) ---
            if (!additionalContacts.isEmpty()) {
                em.getTransaction().begin();
                Ticket ticketForContacts = em.find(Ticket.class, freshTicket.getId());
                for (Person addlContact : additionalContacts) {
                    ticketForContacts.addAssigneeContact(addlContact);
                }
                em.persist(ticketForContacts);
                em.getTransaction().commit();
            }

            // --- Create the first Note from email content ---
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

            em.getTransaction().begin();
            Note note = new Note();
            note.setActivity(freshTicket);
            note.setDetail(detailStr);
            note.setStatus(status);
            note.setReasonCreated(reason);
            note.setDateGenerated(Date.valueOf(LocalDate.now()));
            note.setCreatedBy(caller);
            note.setResolution(false);
            em.persist(note);
            em.getTransaction().commit();

            // Link note to ticket's noteList
            em.getTransaction().begin();
            Ticket ticketForNotes = em.find(Ticket.class, freshTicket.getId());
            if (ticketForNotes.getNoteList() == null) {
                ticketForNotes.setNoteList(new ArrayList<>());
            }
            ticketForNotes.getNoteList().add(note);
            em.persist(ticketForNotes);
            em.getTransaction().commit();

            // --- Handle attachments (same as OutlookLogEmailApi) ---
            int uploaded = 0;
            String pspName = caller.getPsp() != null ? caller.getPsp().getFullName() : "default";
            LinkType fileLinkType = SequenceDAO.getLinkTypeById(em, 1);
            List<String[]> attachmentLinks = new ArrayList<>(); // [displayName, objectKey]

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

                attachmentLinks.add(new String[]{ displayName, objectKey });
                uploaded++;
            }

            // Append attachment links to note detail so they render in activity view
            if (!attachmentLinks.isEmpty()) {
                StringBuilder attachHtml = new StringBuilder();
                attachHtml.append("\n\n--- Attachments ---\n");
                for (String[] link : attachmentLinks) {
                    attachHtml.append("<a href=\"ShowFileUpload?doc=")
                              .append(link[1]).append("\" target=\"_blank\">")
                              .append(link[0]).append("</a>\n");
                }
                em.getTransaction().begin();
                Note noteToUpdate = em.find(Note.class, note.getId());
                noteToUpdate.setDetail(noteToUpdate.getDetail() + attachHtml);
                em.merge(noteToUpdate);
                em.getTransaction().commit();
            }

            // --- Success response ---
            String json = "{"
                    + "\"success\":true,"
                    + "\"activityId\":" + freshTicket.getId() + ","
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
                    "Failed to create ticket: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Resolves a freeform text entry to a Person — same priority chain as
     * CreateTicket25.resolveContactFromFreeform:
     *   1. Email → Employee → Person
     *   2. Email → Person by email
     *   3. Email → create Person from email
     *   4. Name → Employee by name → Person
     *   5. Name → Person by name
     *   6. Name → create new Person
     */
    private Person resolveContactFromFreeform(EntityManager em, String input) {
        String text = input.trim();

        // Email path
        if (EmailDAO.isValidEmail(text)) {
            Employee ee = PersonResolver.getEmployee(em, text);
            if (ee != null) return PersonResolver.getPersonFromEmployee(em, ee);

            Person p = PersonResolver.getBestPersonFromString(em, text);
            if (p != null) return p;

            return PersonResolver.createPersonFromEmail(em, text);
        }

        // Name path
        Employee ee = PersonResolver.getEmployee(em, text);
        if (ee != null) return PersonResolver.getPersonFromEmployee(em, ee);

        Person p = PersonResolver.getBestPersonFromString(em, text);
        if (p != null) return p;

        // Single word — treat as last name
        if (!text.contains(" ") && !text.contains(",")) {
            em.getTransaction().begin();
            Person newP = new Person();
            newP.setPsp(EntityLookup.getPspById(em, 4L));
            newP.setLastName(text.toUpperCase());
            newP.setFirstName("UNKNOWN");
            newP.setFullName("UNKNOWN " + text.toUpperCase());
            em.persist(newP);
            em.getTransaction().commit();
            return newP;
        }
        return PersonResolver.createPersonFromAll(em, text, null, null);
    }

    /**
     * Creates a custom (one-off) ServiceItem for a free-text reason —
     * mirrors CreateTicket25.processTicketType when reasonId==0.
     * Category is auto-detected from keywords, defaulting to General (21).
     */
    private ServiceItem createCustomServiceItem(EntityManager em, String reason) {
        long catId = 21; // default = General
        String t = reason.toLowerCase();
        if (t.contains("claim")) catId = 11;
        else if (t.contains("access") || t.contains("online") || t.contains("log in") || t.contains("portal")) catId = 12;
        else if (t.contains("debit") || t.contains("card")) catId = 13;
        else if (t.contains("cobra")) catId = 14;
        else if (t.contains("hsa")) catId = 15;
        else if (t.contains("enrol") || t.contains("new hire") || t.contains("life event")) catId = 16;
        else if (t.contains("quote") || t.contains("fsa") || t.contains("hra") || t.contains("pop") || t.contains("transit")) catId = 17;
        else if (t.contains("bill") || t.contains("invoice") || t.contains("payment")) catId = 18;

        TicketCategory tc = EntityLookup.getTicketCategoryById(em, catId);
        ActivityCategory tg = em.find(ActivityCategory.class, 3); // group 3 = Ticket

        em.getTransaction().begin();
        ServiceItem si = new ServiceItem();
        si.setDescription(reason);
        si.setActivityCategory(tg);
        si.setPsp(EntityLookup.getPspById(em, 4L));
        si.setSourceType("MANUAL");
        si.setTicketCategory(tc);
        si.setSuppressed(true); // custom one-off reasons start suppressed
        si.setSortOrder(100);
        em.persist(si);
        em.getTransaction().commit();
        return si;
    }

    /**
     * Creates a CheckList for the ticket with required ToDo tasks —
     * mirrors CreateTicket25.createCheckListForTicket + createToDoList.
     */
    private CheckList createCheckListForTicket(EntityManager em, Ticket ticket, Person currentUser) {
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setAssignedTo(ticket);
        c.setComplete(false);
        c.setFullName(ticket.getFullName() + " Checklist");
        c.setDueDate(ticket.getDueDate());
        c.setLoggedBy(currentUser);
        em.persist(c);
        em.getTransaction().commit();

        // Create ToDo tasks
        List<SortedTask> sortedTaskList;
        try {
            sortedTaskList = TicketQueryDAO.getTasksRequiredForTheTicket(em, ticket);
            if (sortedTaskList == null) sortedTaskList = new ArrayList<>();
        } catch (Exception ex) {
            sortedTaskList = new ArrayList<>();
        }

        if (sortedTaskList.isEmpty()) {
            sortedTaskList.add(new SortedTask(EntityLookup.getTaskById(em, 153L), 1000));
        }

        for (SortedTask st : sortedTaskList) {
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(false);
            if (st.getTask().getId() == 153L) toDo.setComplete(true);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            CheckList checkList = EntityLookup.getCheckListById(em, c.getId());
            checkList.getToDoList().add(toDo);
            em.persist(checkList);
            em.getTransaction().commit();
        }

        return EntityLookup.getCheckListById(em, c.getId());
    }

    /**
     * Parses a simple JSON array of recipient objects: [{"email":"...","name":"..."},...]
     * Returns a list of String[2] where [0]=email, [1]=name.
     * Uses basic string parsing to avoid adding a JSON library dependency.
     */
    private static List<String[]> parseRecipientArray(String json) {
        List<String[]> result = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) return result;

        // Split on },{ to get individual objects
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        trimmed = trimmed.trim();
        if (trimmed.isEmpty()) return result;

        // Split objects — handle nested braces by tracking depth
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == ',' && depth == 0) {
                objects.add(trimmed.substring(start, i).trim());
                start = i + 1;
            }
        }
        objects.add(trimmed.substring(start).trim());

        for (String obj : objects) {
            String email = extractJsonValue(obj, "email");
            String name = extractJsonValue(obj, "name");
            result.add(new String[]{ email != null ? email : "", name != null ? name : "" });
        }
        return result;
    }

    /** Extracts a string value for a given key from a simple JSON object string. */
    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            char c = json.charAt(quoteEnd);
            if (c == '\\') { quoteEnd += 2; continue; }
            if (c == '"') break;
            quoteEnd++;
        }
        if (quoteEnd >= json.length()) return null;
        return json.substring(quoteStart + 1, quoteEnd)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

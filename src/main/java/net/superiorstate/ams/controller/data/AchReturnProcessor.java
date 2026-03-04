package net.superiorstate.ams.controller.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.dao.TicketQueryDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.resolver.PersonResolver;
import net.superiorstate.ams.data.service.BpoTaskPushService;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AchReturnProcessor", value = "/AchReturnProcessor")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 100
)
public class AchReturnProcessor extends HttpServlet {

    private static final int SERVICE_ITEM_ID = 122256;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("upload".equals(action)) {
            handleUpload(request, response);
        } else if ("confirm".equals(action)) {
            handleConfirm(request, response);
        } else {
            response.sendRedirect("AchReturnProcessor");
        }
    }

    /**
     * Parse PDF, match persons, store results in session, show preview.
     */
    private void handleUpload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Please select a PDF file to upload.");
            request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);
            return;
        }

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            request.setAttribute("error", "Only PDF files are accepted.");
            request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);
            return;
        }

        byte[] pdfBytes;
        try (InputStream is = filePart.getInputStream()) {
            pdfBytes = is.readAllBytes();
        }

        // Parse the PDF
        AchParseResult result;
        try (InputStream pdfStream = new ByteArrayInputStream(pdfBytes)) {
            result = AchPdfParser.parse(pdfStream);
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);
            return;
        }

        // Match persons
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            for (AchEntry entry : result.getEntries()) {
                matchPerson(em, entry);
            }
        } finally {
            em.close();
        }

        // Store in session for confirm step
        HttpSession session = request.getSession();
        session.setAttribute("achParsedEntries", result.getEntries());
        session.setAttribute("achParseResult", result);
        session.setAttribute("achPdfBytes", pdfBytes);
        session.setAttribute("achPdfFileName", fileName);

        request.setAttribute("parseResult", result);
        request.setAttribute("entries", result.getEntries());
        request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);
    }

    /**
     * Create tickets from session-stored parsed entries, upload PDF, attach to each ticket.
     */
    @SuppressWarnings("unchecked")
    private void handleConfirm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<AchEntry> entries = (List<AchEntry>) session.getAttribute("achParsedEntries");
        AchParseResult parseResult = (AchParseResult) session.getAttribute("achParseResult");
        byte[] pdfBytes = (byte[]) session.getAttribute("achPdfBytes");
        String pdfFileName = (String) session.getAttribute("achPdfFileName");

        if (entries == null || entries.isEmpty() || pdfBytes == null) {
            request.setAttribute("error", "No parsed data found. Please upload a PDF first.");
            request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        Person currentUser = local.getCurrentPerson();

        try {
            // Upload PDF to Wasabi once
            String extension = Validator.getExtensionByStringHandling(pdfFileName).orElse("bin");
            String objectKey = UUID.randomUUID() + "." + extension;
            String pspName = currentUser.getPsp().getFullName();

            try (InputStream pdfStream = new ByteArrayInputStream(pdfBytes)) {
                StorageDAO.uploadFile(em, pspName, objectKey, pdfFileName, pdfStream, (long) pdfBytes.length, "application/pdf");
            }

            ServiceItem serviceItem = EntityLookup.getServiceItemById(em, SERVICE_ITEM_ID);
            LinkType linkType = SequenceDAO.getLinkTypeById(em, 1);
            int ticketsCreated = 0;

            for (AchEntry entry : entries) {
                // Resolve person (may already be matched from preview step, but re-resolve to get managed entity)
                Person contact = resolvePerson(em, entry);
                if (contact == null) continue;

                // Build ticket description
                String description = buildTicketDescription(entry);

                // Create Ticket
                em.getTransaction().begin();
                Ticket t = new Ticket();
                t.setAssignedTo(currentUser);
                t.setDueDate(Date.valueOf(LocalDate.now().plusDays(7)));
                t.setDescription(description);
                t.setTicketServiceItem(serviceItem);
                t.setContact(contact);
                t.setPrimaryContact(contact);
                t.setComplete(false);
                t.setLoggedBy(currentUser);
                t.setFullName(contact.getFirstName().toUpperCase() + " " + contact.getLastName().toUpperCase());
                em.persist(t);
                em.getTransaction().commit();

                // Create CheckList
                CheckList c = createCheckListForTicket(em, t, currentUser);
                CheckList checkList = EntityLookup.getCheckListById(em, c.getId());
                em.getTransaction().begin();
                t.setCheckList(checkList);
                em.persist(t);
                em.getTransaction().commit();

                // Create WebLink pointing to the shared PDF
                em.getTransaction().begin();
                WebLink w = new WebLink();
                w.setPlainText(pdfFileName);
                w.setLinkPath(objectKey);
                w.setLinkType(linkType);
                w.setActive(true);
                em.persist(w);

                Activity managedActivity = EntityLookup.getActivityById(em, t.getId());
                managedActivity.getWebLinkList().add(w);
                em.getTransaction().commit();

                // Update session
                em.refresh(t);
                local.respondToActivityUpdate(em, "ADD_TICKET", t);

                ticketsCreated++;
            }

            session.setAttribute("local", local);

            // Clean up session attributes
            session.removeAttribute("achParsedEntries");
            session.removeAttribute("achParseResult");
            session.removeAttribute("achPdfBytes");
            session.removeAttribute("achPdfFileName");

            request.setAttribute("successMessage", ticketsCreated + " ticket(s) created successfully.");
            request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.setAttribute("error", "Error creating tickets: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/view/a/data/achProcessor25.jsp").forward(request, response);
        } finally {
            em.close();
        }
    }

    /**
     * Match an AchEntry to Employee/Person for preview display.
     */
    private void matchPerson(EntityManager em, AchEntry entry) {
        String name = entry.getIndividualName();

        // Try Employee first
        Employee emp = PersonResolver.getEmployee(em, name);
        if (emp != null) {
            Person p = PersonResolver.getPersonFromEmployee(em, emp);
            if (p != null) {
                entry.setMatchedPersonId(p.getId());
                entry.setMatchedPersonName(p.getFirstName() + " " + p.getLastName());
                entry.setMatchType("EMPLOYEE");
                return;
            }
        }

        // Try Person
        Person person = PersonResolver.getBestPersonFromString(em, name);
        if (person != null) {
            entry.setMatchedPersonId(person.getId());
            entry.setMatchedPersonName(person.getFirstName() + " " + person.getLastName());
            entry.setMatchType("PERSON");
            return;
        }

        // No match — will create new on confirm
        entry.setMatchType("NEW");
        entry.setMatchedPersonName(name);
    }

    /**
     * Resolve a managed Person entity for ticket creation.
     */
    private Person resolvePerson(EntityManager em, AchEntry entry) {
        String name = entry.getIndividualName();

        // Try Employee
        Employee emp = PersonResolver.getEmployee(em, name);
        if (emp != null) {
            Person p = PersonResolver.getPersonFromEmployee(em, emp);
            if (p != null) return p;
        }

        // Try Person
        Person person = PersonResolver.getBestPersonFromString(em, name);
        if (person != null) return person;

        // Create new Person
        if (!name.contains(" ") && !name.contains(",")) {
            em.getTransaction().begin();
            Person newP = new Person();
            newP.setPsp(EntityLookup.getPspById(em, 4L));
            newP.setLastName(name.toUpperCase());
            newP.setFirstName("UNKNOWN");
            newP.setFullName("UNKNOWN " + name.toUpperCase());
            em.persist(newP);
            em.getTransaction().commit();
            return newP;
        }

        return PersonResolver.createPersonFromAll(em, name, null, null);
    }

    /**
     * Build the ticket description from an ACH entry.
     */
    private String buildTicketDescription(AchEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("ACH ");
        if (entry.getReasonCode().startsWith("C")) {
            sb.append("NOC ");
        } else {
            sb.append("Return ");
        }
        sb.append(entry.getReasonCode()).append(" - ").append(entry.getReasonDescription()).append("\n");
        sb.append("Individual: ").append(entry.getIndividualName());
        sb.append(" (ID: ").append(entry.getIndividualId()).append(")\n");
        if (entry.getCorrectedData() != null) {
            sb.append("Corrected Data: ").append(entry.getFormattedCorrectedData()).append("\n");
        }
        sb.append("Effective Date: ").append(entry.getFormattedEffectiveDate()).append("\n");
        if (entry.getOrigSeq() != null) {
            sb.append("Original Seq: ").append(entry.getOrigSeq()).append("\n");
        }
        if (entry.getReceivingBankRT() != null) {
            sb.append("Receiving Bank: ").append(entry.getReceivingBankRT());
        }
        return sb.toString().trim();
    }

    /**
     * Create CheckList for a Ticket, following CreateTicket25 pattern.
     */
    private CheckList createCheckListForTicket(EntityManager em, Ticket t, Person currentUser) {
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setAssignedTo(t);
        c.setComplete(false);
        c.setFullName(t.getFullName() + " Checklist");
        c.setDueDate(t.getDueDate());
        c.setLoggedBy(currentUser);
        em.persist(c);
        em.getTransaction().commit();

        // Create ToDo list
        createToDoList(em, c, t);

        CheckList freshChecklist = EntityLookup.getCheckListById(em, c.getId());
        BpoTaskPushService.pushDelegatedTasks(em, freshChecklist);

        return c;
    }

    /**
     * Create ToDo tasks for the checklist, following CreateTicket25 pattern.
     */
    private void createToDoList(EntityManager em, CheckList c, Ticket t) {
        List<SortedTask> sortedTaskList;
        try {
            sortedTaskList = TicketQueryDAO.getTasksRequiredForTheTicket(em, t);
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
    }
}

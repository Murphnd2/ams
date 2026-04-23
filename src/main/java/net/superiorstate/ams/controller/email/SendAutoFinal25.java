package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.ActivitySessionGuard;
import net.superiorstate.ams.data.util.AutomationHelper;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.summit.archive.Employer;
import net.superiorstate.ams.data.util.AutoSafe;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SendAutoFinal25", value = "/SendAutoFinal25")
public class SendAutoFinal25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendAutoEmail(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendAutoEmail(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAutoEmail(HttpServletRequest request, HttpServletResponse response) throws MessagingException, ServletException, IOException {
        // CSRF PROTECTION — MUST BE FIRST
        String token = (String) request.getSession().getAttribute("csrfToken");
        if (token == null || !token.equals(request.getParameter("csrf"))) {
            response.sendError(403, "CSRF protection failed");
            return;
        }

        // NEW: preview-driven send — content has already been resolved and
        // (possibly) edited by the user in autoPreview25.jsp.
        if ("true".equals(request.getParameter("fromPreview"))) {
            sendFromPreview(request, response);
            return;
        }

        // ─────────────────────────────────────────────────────────────
        // Legacy path — kept as a safety net for any caller that POSTs
        // directly without the preview step. All current UI paths go
        // through autoPreview25.jsp (fromPreview=true), so this branch
        // is not expected to fire under normal use.
        // ─────────────────────────────────────────────────────────────
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Activity a = local.getCurrentActivity().getActivity();
        System.out.println("GOT HERE");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        // Get Session Variables
        String a1autoName = (String) request.getSession().getAttribute("a1autoName");
        boolean shouldClose = (boolean) request.getSession().getAttribute("a1shouldClose");
        Automation automation = (Automation) request.getSession().getAttribute("a1auto");
        int inputCount = Integer.parseInt(request.getSession().getAttribute("a1inputCount").toString());
        String remainingText = request.getSession().getAttribute("a1content").toString();

        //Process Any Inputs Present in the Content
        // ──────────────────────────────────────────────────────
        // Safe input processing – only allow expected indices
        // ──────────────────────────────────────────────────────
        // Extract TO email (if injected by SendAuto25) — goes to recipient, not body
        @SuppressWarnings("unchecked")
        List<String> inputTypes = (List<String>) request.getSession().getAttribute("a1inputTypes");
        String toEmail = null;
        if (inputCount > 0 && inputTypes != null) {
            for (int i = 0; i < inputCount; i++) {
                if ("TO".equals(inputTypes.get(i))) {
                    toEmail = request.getParameter("aInput-" + i);
                    if (toEmail != null) toEmail = toEmail.trim();
                    // Remove placeholder from body — TO email is used as recipient, not content
                    remainingText = remainingText.replace("<[{" + i + "}]>", "");
                } else if ("LINK".equals(inputTypes.get(i))) {
                    String rawValue = request.getParameter("aInput-" + i);
                    String linkValue = (rawValue != null) ? rawValue.trim() : "";
                    if (!linkValue.isBlank()) {
                        linkValue = " <a target=\"_blank\" href=\"" + linkValue + "\">" + linkValue + "</a> ";
                    }
                    remainingText = remainingText.replace("<[{" + i + "}]>", linkValue);
                } else {
                    String rawValue = request.getParameter("aInput-" + i);
                    String safeValue = AutoSafe.getInput(rawValue, inputCount - 1, i);
                    remainingText = remainingText.replace("<[{" + i + "}]>", safeValue);
                }
            }
        }
        // Remove any leftover placeholders (user left blank)
        for (int i = 0; i < inputCount; i++) {
            remainingText = remainingText.replace("<[{" + i + "}]>", "");
        }

        //Process any #erName Hashtags
        if (remainingText.contains("<<#erName>>")) {
            Employer er = AutomationHelper.getEmployerForActivity(em, a);
            if (er != null)
                remainingText = remainingText.replace("<<#erName>>", er.getEmployerName());
            else remainingText = remainingText.replace("<<#erName>>", "");
        }

        //Process any #activityType Hashtags
        if (remainingText.contains("<<#activityType"))
            remainingText = remainingText.replace("<<#activityType>>", local.getCurrentActivity().getActivity().getClass().getSimpleName().toString());

        //Format into proper HTML (Swap <br/> for <p></p> and <nl> for <br/>
        remainingText = AutomationHelper.processBreaksAndNewLines(remainingText);

        //Process any Reference Links (<rf></rf>)
        remainingText = AutomationHelper.processReferenceLinks(remainingText, a, em);

        //Extract any ccList from Inputs
        String ccListString = (String) request.getSession().getAttribute("ccListString");

        //Set Email Items
        List<String> subjectBodyList = AutomationHelper.getSubjectAndBody(remainingText);
        local.getCurrentEmail().setSubject(subjectBodyList.get(0));
        if (subjectBodyList.get(0).equals(""))
            local.getCurrentEmail().setSubject(a1autoName);
        local.getCurrentEmail().setBody(subjectBodyList.get(1));
        local.getCurrentEmail().setAttachments(new ArrayList<>());
        local.getCurrentEmail().setRecipientList(AutomationHelper.getRecipientList(em, local, ccListString));

        // If a TO email was provided (no primary contact scenario), add it as recipient
        if (toEmail != null && !toEmail.isBlank() && Validator.isValidEmail(toEmail)) {
            Person toPerson = EmailDAO.getPersonByEmail(em, toEmail, local.getCurrentPerson().getPsp());
            if (toPerson == null) {
                em.getTransaction().begin();
                toPerson = new Person();
                toPerson.setEmail(toEmail);
                toPerson.setFirstName("NEW");
                toPerson.setLastName("PERSON");
                toPerson.setPsp(local.getCurrentPerson().getPsp());
                em.persist(toPerson);
                em.getTransaction().commit();
            }
            // Add to front of recipient list
            local.getCurrentEmail().getRecipientList().add(0, toPerson);
        }

        String userSignature = buildSignature(local.getCurrentPerson());

        //Create Email Object
        Email email = null;
        try {
            em.getTransaction().begin();
            email = new Email();
            email.setActivity(a);
            email.setSubject(local.getCurrentEmail().getSubject()+" ##ID:"+local.getCurrentActivity().getActivity().getId()+"##");
            email.setDateGenerated(Date.valueOf(LocalDate.now()));
            email.setStatus(EntityLookup.getActivityStatusById(em, 1));
            email.setReasonCreated(EntityLookup.getReasonById(em, 7));
            email.setCreatedBy(local.getCurrentPerson());
            email.setDetail(local.getCurrentEmail().getBody() + userSignature);
            email.setRecipientList(local.getCurrentEmail().getRecipientList());
            em.persist(email);
            em.getTransaction().commit();
        } catch (Exception exception) {
            System.out.println("ERROR: Failed to create email entity");
            exception.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            // email stays null — send/update skipped below
        }

        if (email != null) {
            System.out.println("=========== CREATED EMAIL =========================");
            //Send Email Message
            boolean emailSent = true;
            try {
                EmailDAO.sendEmail(email, em);
                System.out.println("SENT");
                System.out.println(local.getCurrentEmail().getRecipientList().get(0).getEmail());
            } catch (Exception exception) {
                emailSent = false;
                System.out.println("ERROR: Failed to send email");
                exception.printStackTrace();
            }

            if (emailSent) {
                System.out.println("=========== EMAIL SENT =========================");
                // Append email to activity
                try {
                    Activity a1 = EntityLookup.getActivityById(em, a.getId());
                    if (a1 != null) {
                        if (a1.getNoteList() == null)
                            a1.setNoteList(new ArrayList<>());
                        em.getTransaction().begin();
                        a1.getNoteList().add(email);
                        em.persist(a1);
                        em.getTransaction().commit();
                    }

                    if (local.getCurrentActivity().getActivity() != null)
                        local.respondToActivityUpdate(em, "NOTE", email);

                    if (shouldClose) {
                        local.respondToActivityUpdate(em, "AUTO_CLOSE", automation);
                    }
                } catch (Exception exception) {
                    System.out.println("ERROR: Failed to update activity with email");
                    exception.printStackTrace();
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                }
            }
        }

        // Always clean up and redirect — never leave a white screen
        local.getCurrentEmail().setSubject("");
        local.getCurrentEmail().setBody("");
        local.getCurrentEmail().setAttachments(new ArrayList<>());
        local.getCurrentEmail().setRecipientList(new ArrayList<>());
        request.getSession().setAttribute("local",local);
        em.close();

        RequestDispatcher dispatcher =  getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    /**
     * Preview-driven send — uses the subject/body/cc submitted from autoPreview25.jsp
     * (the content the user actually reviewed and possibly edited) rather than
     * re-processing the template. Recipient list was resolved in PrepareAutoPreview25
     * (or SendAuto25 for no-input automations) and is read from session.
     */
    private void sendFromPreview(HttpServletRequest request, HttpServletResponse response)
            throws MessagingException, ServletException, IOException {
        HttpSession session = request.getSession();
        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        if (local == null || local.getCurrentActivity() == null || local.getCurrentActivity().getActivity() == null) {
            response.sendRedirect("ViewActivity25");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        // Multi-tab defense: re-anchor currentActivity if the form carried the expected id.
        ActivitySessionGuard.reanchorIfMismatch(request, em);
        local = (AmsDataLocal) session.getAttribute("local");
        Activity a = local.getCurrentActivity().getActivity();

        // ── Pull user-edited content from the preview form ──
        String subject = request.getParameter("previewSubject");
        if (subject == null) subject = "";
        subject = subject.trim();
        if (subject.isEmpty()) {
            // Fallback to the automation name if the user cleared the subject
            Object autoName = session.getAttribute("a1autoName");
            subject = (autoName != null) ? autoName.toString() : "(No subject)";
        }

        String body = request.getParameter("previewBody");
        if (body == null) body = "";
        // Run the user-edited HTML through the same sanitizer used for template input
        body = AutoSafe.clean(body);

        String ccInput = request.getParameter("previewCc");
        boolean shouldClose = "true".equals(request.getParameter("previewAutoClose"));

        Automation automation = (Automation) session.getAttribute("a1auto");

        // ── Build recipient list: start with resolved list from session ──
        @SuppressWarnings("unchecked")
        List<Person> sessionRecipients = (List<Person>) session.getAttribute("a1recipientList");
        List<Person> recipients = new ArrayList<>();
        List<String> seenEmails = new ArrayList<>();
        if (sessionRecipients != null) {
            for (Person p : sessionRecipients) {
                if (p != null && p.getEmail() != null && Validator.isValidEmail(p.getEmail())) {
                    String key = p.getEmail().trim().toLowerCase();
                    if (!seenEmails.contains(key)) {
                        recipients.add(p);
                        seenEmails.add(key);
                    }
                }
            }
        }

        // ── Append any CC addresses entered in the preview page ──
        if (ccInput != null && !ccInput.isBlank()) {
            for (String raw : ccInput.split(";")) {
                String addr = raw.trim();
                if (addr.isEmpty()) continue;
                if (!Validator.isValidEmail(addr)) continue;
                String key = addr.toLowerCase();
                if (seenEmails.contains(key)) continue;
                Person p = EmailDAO.getPersonByEmail(em, addr, local.getCurrentPerson().getPsp());
                if (p == null) {
                    try {
                        em.getTransaction().begin();
                        p = new Person();
                        p.setEmail(addr);
                        p.setFirstName("NEW");
                        p.setLastName("PERSON");
                        p.setPsp(local.getCurrentPerson().getPsp());
                        em.persist(p);
                        em.getTransaction().commit();
                    } catch (Exception ex) {
                        if (em.getTransaction().isActive()) em.getTransaction().rollback();
                        p = null;
                    }
                }
                if (p != null) {
                    recipients.add(p);
                    seenEmails.add(key);
                }
            }
        }

        // ── Build + persist the Email entity ──
        String signature = buildSignature(local.getCurrentPerson());
        String finalSubject = subject + " ##ID:" + a.getId() + "##";
        String finalBody = body + signature;

        Email email = null;
        try {
            em.getTransaction().begin();
            email = new Email();
            email.setActivity(a);
            email.setSubject(finalSubject);
            email.setDateGenerated(Date.valueOf(LocalDate.now()));
            email.setStatus(EntityLookup.getActivityStatusById(em, 1));
            email.setReasonCreated(EntityLookup.getReasonById(em, 7));
            email.setCreatedBy(local.getCurrentPerson());
            email.setDetail(finalBody);
            email.setRecipientList(recipients);
            em.persist(email);
            em.getTransaction().commit();
        } catch (Exception ex) {
            System.out.println("ERROR: Failed to create email entity (preview path)");
            ex.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            email = null;
        }

        // ── Send + record on activity ──
        if (email != null) {
            boolean emailSent = true;
            try {
                EmailDAO.sendEmail(email, em);
                System.out.println("=========== PREVIEW-SEND EMAIL SENT =========================");
            } catch (Exception ex) {
                emailSent = false;
                System.out.println("ERROR: Failed to send email (preview path)");
                ex.printStackTrace();
            }

            if (emailSent) {
                try {
                    Activity a1 = EntityLookup.getActivityById(em, a.getId());
                    if (a1 != null) {
                        if (a1.getNoteList() == null) a1.setNoteList(new ArrayList<>());
                        em.getTransaction().begin();
                        a1.getNoteList().add(email);
                        em.persist(a1);
                        em.getTransaction().commit();
                    }

                    local.respondToActivityUpdate(em, "NOTE", email);

                    if (shouldClose && automation != null) {
                        local.respondToActivityUpdate(em, "AUTO_CLOSE", automation);
                    }
                } catch (Exception ex) {
                    System.out.println("ERROR: Failed to update activity with email (preview path)");
                    ex.printStackTrace();
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                }
            }
        }

        // Clear preview-related session state so the next automation starts fresh
        clearAutomationSessionState(session);

        // Reset email compose buffer
        if (local.getCurrentEmail() != null) {
            local.getCurrentEmail().setSubject("");
            local.getCurrentEmail().setBody("");
            local.getCurrentEmail().setAttachments(new ArrayList<>());
            local.getCurrentEmail().setRecipientList(new ArrayList<>());
        }
        session.setAttribute("local", local);
        em.close();

        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private static String buildSignature(Person sender) {
        if (sender == null) return "";
        String first = sender.getFirstName() != null ? sender.getFirstName() : "";
        String last = sender.getLastName() != null ? sender.getLastName() : "";
        String pspName = (sender.getPsp() != null && sender.getPsp().getFullName() != null)
                ? sender.getPsp().getFullName() : "";
        return "<p> " + first + " " + last + "<br/>" + pspName + "</p>";
    }

    private static void clearAutomationSessionState(HttpSession session) {
        session.removeAttribute("a1auto");
        session.removeAttribute("a1autoName");
        session.removeAttribute("a1content");
        session.removeAttribute("a1inputLabels");
        session.removeAttribute("a1inputTypes");
        session.removeAttribute("a1inputCount");
        session.removeAttribute("a1shouldClose");
        session.removeAttribute("a1addSignature");
        session.removeAttribute("a1includeCc");
        session.removeAttribute("a1resolvedSubject");
        session.removeAttribute("a1resolvedBody");
        session.removeAttribute("a1recipientList");
        session.removeAttribute("a1previewReady");
        session.removeAttribute("a1expectedActivityId");
    }
}

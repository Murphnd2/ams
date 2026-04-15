package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.util.AutoSafe;
import net.superiorstate.ams.data.util.AutomationHelper;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Step between autoInputScreen25 and autoPreview25.
 * Applies the user-entered inputs to the template, resolves remaining tags,
 * splits into subject/body, and builds the recipient list — then stores the
 * fully-resolved content in session for the preview page.
 *
 * This is the processing half of the old SendAutoFinal25 flow, cleanly
 * separated from the actual send so the user can review and edit first.
 */
@WebServlet(name = "PrepareAutoPreview25", value = "/PrepareAutoPreview25")
public class PrepareAutoPreview25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        action(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        action(request, response);
    }

    private void action(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // CSRF check — autoInputScreen25 submits the token with the form
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("csrfToken");
        if (token == null || !token.equals(request.getParameter("csrf"))) {
            response.sendError(403, "CSRF protection failed");
            return;
        }

        // Must have an automation in session (should have been set by SendAuto25)
        Automation automation = (Automation) session.getAttribute("a1auto");
        String content = (String) session.getAttribute("a1content");
        if (automation == null || content == null) {
            response.sendRedirect("ViewActivity25");
            return;
        }

        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        if (local == null || local.getCurrentActivity() == null) {
            response.sendRedirect("ViewActivity25");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Activity activity = local.getCurrentActivity().getActivity();

        int inputCount = 0;
        try {
            Object raw = session.getAttribute("a1inputCount");
            if (raw != null) inputCount = Integer.parseInt(raw.toString());
        } catch (NumberFormatException ignore) {}

        @SuppressWarnings("unchecked")
        List<String> inputTypes = (List<String>) session.getAttribute("a1inputTypes");

        // ── Apply user-entered inputs to the placeholder slots ──
        String remainingText = content;
        String toEmail = null;
        String ccListFromInputs = null;

        if (inputCount > 0 && inputTypes != null) {
            for (int i = 0; i < inputCount; i++) {
                String type = inputTypes.get(i);
                String rawValue = request.getParameter("aInput-" + i);
                if ("TO".equals(type)) {
                    toEmail = (rawValue != null) ? rawValue.trim() : null;
                    // TO email is a recipient, not body content — strip the placeholder
                    remainingText = remainingText.replace("<[{" + i + "}]>", "");
                } else if ("CC".equals(type)) {
                    ccListFromInputs = (rawValue != null) ? rawValue.trim() : null;
                    remainingText = remainingText.replace("<[{" + i + "}]>", "");
                } else if ("LINK".equals(type)) {
                    String link = (rawValue != null) ? rawValue.trim() : "";
                    String anchor = link.isBlank()
                            ? ""
                            : " <a target=\"_blank\" href=\"" + link + "\">" + link + "</a> ";
                    remainingText = remainingText.replace("<[{" + i + "}]>", anchor);
                } else {
                    String safe = AutoSafe.getInput(rawValue, inputCount - 1, i);
                    remainingText = remainingText.replace("<[{" + i + "}]>", safe);
                }
            }
        }
        // Remove any leftover placeholders (user left blank)
        for (int i = 0; i < inputCount; i++) {
            remainingText = remainingText.replace("<[{" + i + "}]>", "");
        }

        // ── Resolve remaining smart tags (safety net) ──
        if (remainingText.contains("<<#erName>>")) {
            Employer er = AutomationHelper.getEmployerForActivity(em, activity);
            remainingText = remainingText.replace("<<#erName>>", er != null ? er.getEmployerName() : "");
        }
        if (remainingText.contains("<<#activityType")) {
            remainingText = remainingText.replace("<<#activityType>>", activity.getClass().getSimpleName());
        }

        // ── Format paragraphs and reference links ──
        remainingText = AutomationHelper.processBreaksAndNewLines(remainingText);
        remainingText = AutomationHelper.processReferenceLinks(remainingText, activity, em);

        // ── Split subject / body ──
        List<String> subjectBody = AutomationHelper.getSubjectAndBody(remainingText);
        String subject = subjectBody.get(0);
        if (subject == null || subject.isEmpty()) {
            subject = (String) session.getAttribute("a1autoName");
            if (subject == null) subject = automation.getAutomationName();
        }
        String body = subjectBody.get(1);

        // ── Build recipient list ──
        List<Person> recipients = AutomationHelper.getRecipientList(em, local, ccListFromInputs);

        // TO email input (standalone checklist case) — add to front of recipient list
        if (toEmail != null && !toEmail.isBlank() && Validator.isValidEmail(toEmail)) {
            Person toPerson = EmailDAO.getPersonByEmail(em, toEmail, local.getCurrentPerson().getPsp());
            if (toPerson == null) {
                try {
                    em.getTransaction().begin();
                    toPerson = new Person();
                    toPerson.setEmail(toEmail);
                    toPerson.setFirstName("NEW");
                    toPerson.setLastName("PERSON");
                    toPerson.setPsp(local.getCurrentPerson().getPsp());
                    em.persist(toPerson);
                    em.getTransaction().commit();
                } catch (Exception ex) {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                    toPerson = null;
                }
            }
            if (toPerson != null) {
                // Put TO recipient at the front, avoid duplicates
                boolean already = false;
                for (Person p : recipients) {
                    if (p.getEmail() != null && p.getEmail().equalsIgnoreCase(toEmail)) {
                        already = true; break;
                    }
                }
                if (!already) recipients.add(0, toPerson);
            }
        }

        em.close();

        // ── Store preview-ready state ──
        session.setAttribute("a1resolvedSubject", subject);
        session.setAttribute("a1resolvedBody", body);
        session.setAttribute("a1recipientList", recipients);
        session.setAttribute("a1previewReady", Boolean.TRUE);

        request.getRequestDispatcher("/WEB-INF/view/a/taskManager/autoPreview25.jsp")
                .forward(request, response);
    }
}

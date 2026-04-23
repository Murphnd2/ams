package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.ActivitySessionGuard;
import net.superiorstate.ams.data.util.AutomationHelper;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SendAuto25", value = "/SendAuto25")
public class SendAuto25 extends HttpServlet {
    private String currentLabel;

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

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        // Multi-tab defense: re-anchor currentActivity if the caller tells us
        // which activity this automation belongs to. No-op for legacy callers.
        ActivitySessionGuard.reanchorIfMismatch(request, em);

        // Stash the expected id so downstream JSPs can carry it forward in forms.
        String expectedIdParam = request.getParameter("expectedActivityId");
        if (expectedIdParam != null && !expectedIdParam.isBlank()) {
            request.getSession().setAttribute("a1expectedActivityId", expectedIdParam.trim());
        } else {
            request.getSession().removeAttribute("a1expectedActivityId");
        }

        // Get Automation Parameter
        Automation a = null;
        try {
            int autoId = Integer.parseInt(request.getParameter("aeId").toString().trim());
            a = EntityLookup.getAutomationById(em, autoId);
        } catch (Exception e2) {
            em.close();
            return;
        }
        if (a == null) {
            em.close();
            return;
        }

        System.out.println("ID: " + a.getId());

        // Prepare The Automation
        String remainingText = a.getContent();
        StringBuilder processedText = new StringBuilder();

        // Resolve smart tags BEFORE input extraction
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Activity activity = (local != null && local.getCurrentActivity() != null)
                ? local.getCurrentActivity().getActivity() : null;

        // <<#erName>> — resolve employer/prospect name or fall back to input field
        if (remainingText.contains("<<#erName>>")) {
            String erName = (activity != null) ? AutomationHelper.resolveErName(em, activity) : null;
            if (erName != null && !erName.isBlank()) {
                remainingText = remainingText.replace("<<#erName>>", erName);
            } else {
                remainingText = remainingText.replace("<<#erName>>", "<ii>Employer Name</ii>");
            }
        }

        // Recipient check — if no valid primary contact email, inject a "To:" input field.
        // Standalone checklists (personal tasks) have no contacts at all.
        if (local != null && local.getCurrentActivity() != null) {
            boolean hasRecipient = false;
            Person pc = local.getCurrentActivity().getPrimaryContact();
            if (pc != null && pc.getEmail() != null && Validator.isValidEmail(pc.getEmail()))
                hasRecipient = true;
            if (!hasRecipient && activity != null) {
                pc = activity.getPrimaryContact();
                if (pc != null && pc.getEmail() != null && Validator.isValidEmail(pc.getEmail()))
                    hasRecipient = true;
            }
            if (!hasRecipient) {
                // Prepend a TO email input — it will be the first field the user sees
                remainingText = "<ii><to></ii>" + remainingText;
            }
        }

        boolean shouldClose = remainingText.contains("<<close>>");
        remainingText = remainingText.replace("<<close>>", ""); // Strip flag

        boolean addSignature = remainingText.contains("<<sig>>");
        remainingText = remainingText.replace("<<sig>>", ""); // Strip flag

        boolean includeCc = false;

        // Locate and process all inputs required
        int count = 0;
        int iiFlagStart;
        int iiFlagEnd;
        List<String> inputType = new ArrayList<>();
        List<String> inputLabel = new ArrayList<>();

        while (remainingText.contains("<ii>")) {
            count += 1;
            iiFlagStart = remainingText.indexOf("<ii>");
            iiFlagEnd = remainingText.indexOf("</ii>");
            currentLabel = remainingText.substring(iiFlagStart + 4, iiFlagEnd);

            inputType.add(getLabelType(currentLabel));
            inputLabel.add(currentLabel);

            processedText.append(remainingText, 0, iiFlagStart)
                    .append("<[{").append(count - 1).append("}]>");

            if (remainingText.length() > iiFlagEnd + 6)
                remainingText = remainingText.substring(iiFlagEnd + 5);
            else
                remainingText = "";
        }

        processedText.append(remainingText);

        // Set Session Variables (shared by both paths)
        HttpSession session = request.getSession();
        session.setAttribute("a1auto", a);
        session.setAttribute("a1autoName", a.getAutomationName());
        session.setAttribute("a1includeCc", includeCc);
        session.setAttribute("a1shouldClose", shouldClose);
        session.setAttribute("a1addSignature", addSignature);
        session.setAttribute("a1inputLabels", inputLabel);
        session.setAttribute("a1inputTypes", inputType);
        session.setAttribute("a1content", processedText.toString());
        session.setAttribute("a1inputCount", count);

        // Ensure CSRF token exists for this session
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());
        }

        // Clear any stale preview state from a previous run
        session.removeAttribute("a1resolvedSubject");
        session.removeAttribute("a1resolvedBody");
        session.removeAttribute("a1recipientList");
        session.removeAttribute("a1previewReady");

        RequestDispatcher d;
        if (count > 0) {
            // Inputs needed — collect them before previewing
            em.close();
            d = request.getRequestDispatcher("/WEB-INF/view/a/taskManager/autoInputScreen25.jsp");
        } else {
            // No inputs — resolve the rest of the content right now and go straight to preview
            String fullyResolved = resolveNoInputContent(em, processedText.toString(), a, local);
            List<String> subjectBody = AutomationHelper.getSubjectAndBody(fullyResolved);
            String subject = subjectBody.get(0);
            if (subject == null || subject.isEmpty()) {
                subject = a.getAutomationName();
            }
            String body = subjectBody.get(1);

            List<Person> recipients = (local != null && local.getCurrentActivity() != null)
                    ? AutomationHelper.getRecipientList(em, local, null)
                    : new ArrayList<>();

            session.setAttribute("a1resolvedSubject", subject);
            session.setAttribute("a1resolvedBody", body);
            session.setAttribute("a1recipientList", recipients);
            session.setAttribute("a1previewReady", Boolean.TRUE);

            em.close();
            d = request.getRequestDispatcher("/WEB-INF/view/a/taskManager/autoPreview25.jsp");
        }

        d.forward(request, response);
    }

    /**
     * Completes content resolution for automations that have no user inputs.
     * Mirrors the processing chain in SendAutoFinal25 up to the point of
     * subject/body split — so the preview page shows exactly what would be sent.
     */
    private String resolveNoInputContent(EntityManager em, String text, Automation a, AmsDataLocal local) {
        String out = text;

        Activity act = (local != null && local.getCurrentActivity() != null)
                ? local.getCurrentActivity().getActivity() : null;

        // Safety net — <<#erName>> should already be resolved above, but handle legacy templates.
        if (out.contains("<<#erName>>")) {
            Employer er = (act != null) ? AutomationHelper.getEmployerForActivity(em, act) : null;
            out = out.replace("<<#erName>>", er != null ? er.getEmployerName() : "");
        }

        if (out.contains("<<#activityType") && act != null) {
            out = out.replace("<<#activityType>>", act.getClass().getSimpleName());
        }

        out = AutomationHelper.processBreaksAndNewLines(out);
        out = AutomationHelper.processReferenceLinks(out, act, em);
        return out;
    }

    private String getLabelType(String cl) {
        if (cl.contains("<to>")) {
            currentLabel = "To (Email Address)";
            return "TO";
        }
        if (cl.contains("<l>")) {
            currentLabel = cl.substring(3);
            return "LINK";
        }
        if (cl.contains("<cc>")) {
            currentLabel = "CC (Separate with Semi-Colon)";
            return "CC";
        }
        return "INPUT";
    }
}

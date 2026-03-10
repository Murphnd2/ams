package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.AutomationHelper;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.model.general.Person;

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

        em.close();

        // Set Session Variables
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

        // IMPORTANT: don't forward GET directly into SendAutoFinal25 (it requires csrf param)
        RequestDispatcher d;
        if (count > 0) {
            d = request.getRequestDispatcher("/WEB-INF/view/a/taskManager/autoInputScreen25.jsp");
        } else {
            // NEW: confirm/auto-post screen that submits csrf + sendAutoEmail=1 via POST
            d = request.getRequestDispatcher("/WEB-INF/view/a/taskManager/autoConfirmSend25.jsp");
        }

        d.forward(request, response);
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

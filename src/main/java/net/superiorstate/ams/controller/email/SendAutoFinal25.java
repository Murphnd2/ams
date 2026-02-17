package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.AutomationHelper;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.summit.archive.Employer;
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
        // =============================================
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
        if (inputCount > 0) {
            for (int i = 0; i < inputCount; i++) {
                String rawValue = request.getParameter("aInput-" + i);
                String safeValue = AutoSafe.getInput(rawValue, inputCount - 1, i);

                remainingText = remainingText.replace("<[{" + i + "}]>", safeValue);
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
        String userSignature = "<p> " + local.getCurrentPerson().getFirstName() + " " + local.getCurrentPerson().getLastName() + "<br/>";
        userSignature += local.getCurrentPerson().getPsp().getFullName() + "</p>";

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
            return;
        }
        System.out.println("=========== CREATED EMAIL =========================");
        //Send Email Message
        boolean emailSent = true;
        try {
            EmailDAO.sendEmail(email, em);
            System.out.println("SENT");
            System.out.println(local.getCurrentEmail().getRecipientList().get(0).getEmail());
        } catch (Exception exception) {
            emailSent = false;
            exception.printStackTrace();
        }

        if(!emailSent)
            return;
        System.out.println("=========== EMAIL SENT =========================");

        // If Email is Sent, append email to activity
        Activity a1 = EntityLookup.getActivityById(em, a.getId());
        if (a1 == null)
            return;
        if (a1.getNoteList() == null)
            a1.setNoteList(new ArrayList<>());
        em.getTransaction().begin();
        a1.getNoteList().add(email);
        em.persist(a1);
        em.getTransaction().commit();

        if(local.getCurrentActivity().getActivity() != null)
            local.respondToActivityUpdate(em,"NOTE",email);

        if(shouldClose) {
            local.respondToActivityUpdate(em, "AUTO_CLOSE", automation);
        }
        // Clear Cache
        local.getCurrentEmail().setSubject("");
        local.getCurrentEmail().setBody("");
        local.getCurrentEmail().setAttachments(new ArrayList<>());
        local.getCurrentEmail().setRecipientList(new ArrayList<>());
        request.getSession().setAttribute("local",local);
        em.close();

        RequestDispatcher dispatcher =  getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }
}

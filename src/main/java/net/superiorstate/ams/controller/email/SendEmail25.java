package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.data.util.EmailIdentity;
import net.superiorstate.ams.data.util.EmailIdentityResolver;
import net.superiorstate.ams.data.util.EmailTemplate;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.sales.agency.Agency;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SendEmail25", value = "/SendEmail25")
public class SendEmail25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendMessage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendMessage(request, response);
    }

    private void sendMessage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Activity a = getAppropriateActivity(local, em);

        // Get attachments for template rendering
        List<WebLink> attachments = local.getCurrentEmail().getAttachments();

        // V069: originating agency drives both the signature and the From identity.
        Person emailSender = local.getCurrentPerson();
        Agency senderAgency = OriginatingAgencyResolver.resolve(emailSender);

        // Wrap the user's message body in the branded email template (includes attachments at top)
        String wrappedBody = EmailTemplate.wrap(
                local.getCurrentEmail().getBody(),
                emailSender,
                senderAgency,
                attachments,
                em
        );

        boolean emailSent = true;
        Email e1 = null;
        try {
            em.getTransaction().begin();
            Email email = new Email();
            email.setActivity(a);
            email.setSubject(local.getCurrentEmail().getSubject());
            email.setDateGenerated(Date.valueOf(LocalDate.now()));
            email.setStatus(EntityLookup.getActivityStatusById(em, 1));
            email.setReasonCreated(EntityLookup.getReasonById(em, 7));
            email.setCreatedBy(local.getCurrentPerson());
            email.setDetail(wrappedBody);
            em.persist(email);
            em.getTransaction().commit();
            System.out.print("CREATED EMAIL OBJECT -------------");

            for (WebLink webLink : attachments) {
                em.getTransaction().begin();
                WebLink w = SequenceDAO.getWebLinkById(em, webLink.getId());
                w.setEmail(email);
                em.persist(w);
                em.getTransaction().commit();

                em.getTransaction().begin();
                Email e = EntityLookup.getEmailById(em, email.getId());
                if (e.getWebLinkList() == null) e.setWebLinkList(new ArrayList<>());
                e.getWebLinkList().add(w);
                em.persist(e);
                em.getTransaction().commit();
            }
            System.out.println("PROCESSED ATTACHMENTS------------");

            Activity activityToAppend = EntityLookup.getActivityById(em, a.getId());
            em.getTransaction().begin();
            assert activityToAppend != null;
            activityToAppend.addNote(email);
            em.persist(activityToAppend);
            em.getTransaction().commit();

            System.out.println("UPDATED ACTIVITY------");

            List<Person> recipientList = local.getCurrentEmail().getRecipientList();

            for (Person p1 : recipientList) {
                em.getTransaction().begin();
                Person person = EntityLookup.getPersonById(em, p1.getId());
                Email email1 = EntityLookup.getEmailById(em, email.getId());
                email1.addRecipient(person);
                em.persist(email1);
                em.persist(person);
                em.getTransaction().commit();
            }

            System.out.println("PROCESSED RECIPIENTS ");

            e1 = EntityLookup.getEmailById(em, email.getId());

            System.out.println("RETRIEVED UPDATED EMAIL");

            // Send via SMTP with the resolved white-label identity
            EmailIdentity identity = EmailIdentityResolver.resolve(emailSender, senderAgency, emailSender.getPsp(), em);
            EmailDAO.sendEmail(e1, identity, em);
            System.out.println("SENT THE EMAIL");

        } catch (Exception e) {
            e.printStackTrace();
            emailSent = false;
        }

        if (emailSent) {
            if (local.getCurrentActivity().getActivity() != null)
                local.respondToActivityUpdate(em, "NOTE", e1);
            local.getCurrentEmail().setSubject("");
            local.getCurrentEmail().setBody("");
            local.getCurrentEmail().setAttachments(new ArrayList<>());
        }
        request.getSession().setAttribute("local", local);
        RequestDispatcher dispatcher;
        if (!emailSent)
            dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
        else if (local.getCurrentActivity().getActivity() != null)
            dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        else dispatcher = getServletContext().getNamedDispatcher("ViewHome25");

        em.close();
        dispatcher.forward(request, response);
    }

    private Activity getAppropriateActivity(AmsDataLocal local, EntityManager em) {
        if (local.getCurrentActivity().getActivity() != null)
            return local.getCurrentActivity().getActivity();

        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setCompletedBy(local.getCurrentPerson());
        c.setComplete(true);
        c.setDateCompleted(Date.valueOf(LocalDate.now().minusDays(1)));
        c.setFullName("EMAIL SENT TO RECIPIENTS");
        c.setAssignedTo(local.getCurrentPerson());
        c.setDueDate(Date.valueOf(LocalDate.now().minusDays(1)));
        em.persist(c);
        em.getTransaction().commit();
        return c;
    }
}
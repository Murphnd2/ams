package net.superiorstate.ams.controller.emailItems;


import com.microsoft.graph.models.Message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.microsoft.aad.msal4j.*;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

@WebServlet(name = "SendEmail25", value = "/SendEmail25")
public class SendEmail25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendMessage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendMessage(request,response);
    }

    public String getAccessToken() throws Exception {

        return null;
    }

    public void sendEmail(Email e1) throws Exception {
        String accessToken = getAccessToken(); // Make sure this method returns a Graph API token

        // Setup authentication for Graph API
        IAuthenticationProvider authProvider = new IAuthenticationProvider() {
            @NotNull
            @Override
            public CompletableFuture<String> getAuthorizationTokenAsync(@NotNull URL url) {
                // Here, you would return a CompletableFuture that completes with the token.
                // Since you're using a synchronous token acquisition, you might want to wrap it in a CompletableFuture.
                return CompletableFuture.supplyAsync(() -> accessToken);
            }

            // If authenticateRequest is still required, you might need to implement both:

        };

        GraphServiceClient<Request> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        com.microsoft.graph.models.Message message = new Message();
        message.subject = e1.getSubject();
        message.body = new ItemBody();
        message.body.contentType = BodyType.TEXT; // or BodyType.HTML for HTML emails
        message.body.content = e1.getDetail();
        List<Recipient> rList = new ArrayList<>();
        Recipient r;
        EmailAddress ea;
        for(Person rec:e1.getRecipientList()){
            r = new Recipient();
            ea = new EmailAddress();
            ea.address = rec.getEmail();
            r.emailAddress = ea;
            rList.add(r);
        }
        message.toRecipients = rList;
        r = new Recipient();
        ea = new EmailAddress();
        ea.address = e1.getCreatedBy().getEmail();
        r.emailAddress = ea;

        // Set the from field, if required (this might need special permissions or setup)
        message.from = r;

        UserSendMailParameterSet parameterSet = new UserSendMailParameterSet();
        parameterSet.message = message;
        parameterSet.saveToSentItems = true;

        // Send the email
        try {
            graphClient.users(e1.getCreatedBy().getEmail()).sendMail(parameterSet).buildRequest().post();
            System.out.println("Email sent successfully");
        } catch (Exception e) {
            System.out.println("Error sending email: " + e.getMessage());
        }
    }

    private void sendMessage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Activity a = getAppropriateActivity(local, em);

        String userSignature = "<p> " + local.getCurrentPerson().getFirstName() + " " + local.getCurrentPerson().getLastName() + "<br/>";
        userSignature += local.getCurrentPerson().getPsp().getFullName() + "</p>";

        boolean emailSent = true;
        Email e1 = null;
        try{
            em.getTransaction().begin();
            Email email = new Email();
            email.setActivity(a);
            email.setSubject(local.getCurrentEmail().getSubject());
            email.setDateGenerated(Date.valueOf(LocalDate.now()));
            email.setStatus(dM.getActivityStatusById(em,1));
            email.setReasonCreated(dM.getReasonById(em,7));
            email.setCreatedBy(local.getCurrentPerson());
            email.setDetail(local.getCurrentEmail().getBody() + userSignature);
            em.persist(email);
            em.getTransaction().commit();
            System.out.print("CREATED EMAIL OBJECT -------------");

            List<WebLink> attachmentList = local.getCurrentEmail().getAttachments();
            for (WebLink webLink : attachmentList) {
                em.getTransaction().begin();
                WebLink w = ddC.getWebLinkById(em, webLink.getId());
                w.setEmail(email);
                em.persist(w);
                em.getTransaction().commit();

                em.getTransaction().begin();
                Email e = dM.getEmailById(em, email.getId());
                e.getWebLinkList().add(w);
                em.persist(e);
                em.getTransaction().commit();
            }
            System.out.println("PROCESSED ATTACHMENTS------------");

            Activity activityToAppend = dM.getActivityById(em,a.getId());
            em.getTransaction().begin();
            assert activityToAppend != null;
            activityToAppend.addNote(email);
            em.persist(activityToAppend);
            em.getTransaction().commit();

            System.out.println("UPDATED ACTIVITY------");

            List<Person> recipientList = local.getCurrentEmail().getRecipientList();

            for (Person p1 : recipientList) {
                em.getTransaction().begin();
                Person person = dM.getPersonById(em, p1.getId());
                Email email1 = dM.getEmailById(em, email.getId());
                email1.addRecipient(person);
                em.persist(email1);
                em.persist(person);
                em.getTransaction().commit();
            }

            System.out.println("PROCESSED RECIPIENTS ");

            e1 = dM.getEmailById(em,email.getId());

            System.out.println("RETRIEVED UPDATED EMAIL");

            //dbEmail.sendEmail(e1,em);
            sendEmail(e1);
            System.out.println("TOKEN: " + getAccessToken());
            System.out.println("SENT THE EMAIL");

        } catch (Exception e){
            e.printStackTrace();
            emailSent = false;
        }

        if(emailSent) {
            if (local.getCurrentActivity().getActivity() != null)
                local.respondToActivityUpdate(em,"NOTE",e1);
            local.getCurrentEmail().setSubject("");
            local.getCurrentEmail().setBody("");
            local.getCurrentEmail().setAttachments(new ArrayList<>());
        }
        request.getSession().setAttribute("local",local);
        RequestDispatcher dispatcher;
        if(!emailSent)
            dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
        else if(local.getCurrentActivity().getActivity()!=null)
            dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        else dispatcher = getServletContext().getNamedDispatcher("ViewHome25");

        em.close();
        dispatcher.forward(request,response);

    }

    private Activity getAppropriateActivity(AmsDataLocal local, EntityManager em){
        if(local.getCurrentActivity().getActivity()!=null)
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

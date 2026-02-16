package net.superiorstate.ams.controller.email;
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
        String tenant = System.getenv("AZURE_TENANT_ID");
        String clientId = System.getenv("AZURE_CLIENT_ID");
        String clientSecret = System.getenv("AZURE_CLIENT_SECRET");

        String authority = "https://login.microsoftonline.com/" + tenant;

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                        clientId,
                        ClientCredentialFactory.createFromSecret(clientSecret))
                .authority(authority)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters.builder(
                        Collections.singleton("https://graph.microsoft.com/.default"))
                .build();

        IAuthenticationResult result = app.acquireToken(params).get();
        System.out.println("[Auth] ACCESS TOKEN: " + result.accessToken());
        return result.accessToken();
    }

    public void iSendEmail(Email e1) throws Exception{
        // Assumes: String getAccessToken(); Email e1;

        final String accessToken = getAccessToken();

        // Graph auth provider (token we already acquired)
        final IAuthenticationProvider authProvider = url ->
                java.util.concurrent.CompletableFuture.completedFuture(accessToken);

        // Build Graph client
        final GraphServiceClient<okhttp3.Request> graphClient =
                GraphServiceClient.builder()
                        .authenticationProvider(authProvider)
                        .buildClient();

        // 1) Fixed service sender in your tenant (MUST be a licensed mailbox)
        final String senderUpn = "noreply@superiorstate.net"; // TODO: set to your service mailbox UPN

        // 2) Build the message
        final Message message = new Message();
        message.subject = e1.getSubject();

        final ItemBody body = new ItemBody();
        body.contentType = BodyType.HTML;            // HTML is safer for signatures/markup
        body.content = e1.getDetail();
        message.body = body;

        // Collect recipients (skip blanks)
        final java.util.List<Recipient> to = new java.util.ArrayList<>();
        for (Person rec : e1.getRecipientList()) {
            if (rec == null || rec.getEmail() == null) continue;
            final String addr = rec.getEmail().trim();
            if (addr.isEmpty()) continue;

            final Recipient r = new Recipient();
            r.emailAddress = new EmailAddress();
            r.emailAddress.address = addr;
            to.add(r);
        }
        message.toRecipients = to;

        // Optional: Reply-To back to the originator (human)
        if (e1.getCreatedBy() != null && e1.getCreatedBy().getEmail() != null) {
            final String replyToAddr = e1.getCreatedBy().getEmail().trim();
            if (!replyToAddr.isEmpty()) {
                final Recipient replyTo = new Recipient();
                replyTo.emailAddress = new EmailAddress();
                replyTo.emailAddress.address = replyToAddr;
                message.replyTo = java.util.List.of(replyTo);
            }
        }

        // IMPORTANT: do NOT set message.from for sendMail
        // (Graph derives sender from /users/{id} you call)

        // Guard: must have at least one recipient
        if (message.toRecipients == null || message.toRecipients.isEmpty()) {
            throw new IllegalArgumentException("No recipients to send to.");
        }

        // 3) Send (and save to Sent Items)
        final UserSendMailParameterSet parameterSet = new UserSendMailParameterSet();
        parameterSet.message = message;
        parameterSet.saveToSentItems = true;

        try {
            graphClient
                    .users(senderUpn)                 // send AS the service mailbox
                    .sendMail(parameterSet)
                    .buildRequest()
                    .post();

            System.out.println("[SendEmail25] sendMail OK (202)");
        } catch (com.microsoft.graph.http.GraphServiceException gse) {
            System.out.println("[SendEmail25] GraphServiceException");
            try {
                System.out.println("  status: " + gse.getResponseCode());
            } catch (Throwable t) {
                System.out.println("  status: <unknown>");
            }
            System.out.println("  msg:    " + gse.getMessage());
            gse.printStackTrace();
            throw gse; // keep your emailSent=false behavior
        } catch (com.microsoft.graph.core.ClientException ce) {
            System.out.println("[SendEmail25] ClientException: " + ce.getMessage());
            ce.printStackTrace();
            throw ce;
        }

    }
    public void sendEmail(Email e1) throws Exception {
        /**
        System.out.println("[SendEmail25] 0 enter sendEmail");

        final String accessToken = getAccessToken();
        System.out.println("[SendEmail25] 1 token len=" + (accessToken == null ? 0 : accessToken.length()));

        var authProvider = (com.microsoft.graph.authentication.IAuthenticationProvider) url ->
                java.util.concurrent.CompletableFuture.completedFuture(accessToken);

        System.out.println("[SendEmail25] 2 building client");
        com.microsoft.graph.requests.GraphServiceClient<okhttp3.Request> graph =
                com.microsoft.graph.requests.GraphServiceClient.builder()
                        .authenticationProvider(authProvider)
                        .buildClient();

        // IMPORTANT: set this to a REAL, licensed mailbox UPN in YOUR tenant
        final String senderUpn = "kevin@superiorstate.net";
        System.out.println("[SendEmail25] 3 senderUpn=" + senderUpn);

        com.microsoft.graph.models.Message message = new com.microsoft.graph.models.Message();
        message.subject = "[AMS Test] " + e1.getSubject();

        com.microsoft.graph.models.ItemBody body = new com.microsoft.graph.models.ItemBody();
        body.contentType = com.microsoft.graph.models.BodyType.HTML;
        body.content = "<p>Test at " + java.time.OffsetDateTime.now() + "</p>";
        message.body = body;

        java.util.List<com.microsoft.graph.models.Recipient> to = new java.util.ArrayList<>();
        com.microsoft.graph.models.Recipient me = new com.microsoft.graph.models.Recipient();
        me.emailAddress = new com.microsoft.graph.models.EmailAddress();
        me.emailAddress.address = senderUpn; // self-send to prove pipeline works
        to.add(me);
        message.toRecipients = to;

        com.microsoft.graph.models.UserSendMailParameterSet send =
                new com.microsoft.graph.models.UserSendMailParameterSet();
        send.message = message;
        send.saveToSentItems = true;

        try {
            System.out.println("[SendEmail25] 4 about to POST sendMail");
            graph.users(senderUpn).sendMail(send).buildRequest().post();
            System.out.println("[SendEmail25] 5 POST returned (no exception)");
        } catch (com.microsoft.graph.http.GraphServiceException gse) {
            System.out.println("[SendEmail25] X GraphServiceException");
            try { System.out.println("  status: " + gse.getResponseCode()); } catch (Throwable t) { System.out.println("  status:<unknown>"); }
            System.out.println("  msg:    " + gse.getMessage());
            gse.printStackTrace();
            Throwable c = gse.getCause();
            while (c != null) {
                System.out.println("  cause:  " + c.getClass().getName() + ": " + c.getMessage());
                c = c.getCause();
            }
            throw gse; // keeps your emailSent=false behavior
        } catch (com.microsoft.graph.core.ClientException ce) {
            System.out.println("[SendEmail25] X ClientException: " + ce.getMessage());
            ce.printStackTrace();
            throw ce;
        }
        */




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
        message.body.contentType = BodyType.HTML; // or BodyType.HTML for HTML emails
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

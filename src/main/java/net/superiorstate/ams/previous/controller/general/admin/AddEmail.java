package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.activity.checklist.task.AddFileToTask;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.misc.dbA;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.Recipient;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AddEmail", value = "/AddEmail")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    //1 MB
        maxFileSize = 1024 * 1024 * 10,         //10 MB
        maxRequestSize = 1024 * 1024 * 100      //100 MB
)
public class AddEmail extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            changeView(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            changeView(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request) throws ServletException, MessagingException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        logAndSendEmail(request,em);
        em.close();
    }
    private void logAndSendEmail(HttpServletRequest request, EntityManager em) throws ServletException, IOException, MessagingException {
        Person p = (Person) request.getSession().getAttribute("currentPerson");
        int adminView = Integer.parseInt(request.getSession().getAttribute("adminView").toString());

        Email theEmail = createEmail(request,em, adminView);

        switch(adminView){
            case 1,2,3:
                Activity a = (Activity) request.getSession().getAttribute("currentActivity");
                ViewSelectedActivity.setActivityView(request,em,a);
                break;
            default:
                break;
        }
        dbEmail.sendEmail(theEmail,em);
    }
    private Email createEmail(HttpServletRequest request, EntityManager em, int adminView) throws ServletException, IOException {
        String emailList = request.getParameter("toEmail");
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        List<Person> recipientList = getRecipientList(em,emailList);
        Activity a;
        switch (adminView){
            case 1:
                //FIXME: need to setup SETUP here
                request.getSession().setAttribute("nothing",true);
                a = (Setup) request.getSession().getAttribute("currentActivity");
                break;
            case 2:
                Renewal renewal = (Renewal) request.getSession().getAttribute("currentActivity");
                for(Person r:recipientList){
                    Recipient recipient = getRecipientById(em,r.getId());
                    for(Employee employee:renewal.getEmployer().getContactList()){
                        if(r.getEmail().equals(employee.getEmail())){
                            em.getTransaction().begin();
                            Recipient r1 = getRecipientById(em,recipient.getId());
                            r1.setFullName(employee.getFirstName()+ " " + employee.getLastName());
                            em.persist(r1);
                            em.getTransaction().commit();
                        }
                    }
                }
                a = (Renewal) request.getSession().getAttribute("currentActivity");
                break;
            case 3:
                request.getSession().setAttribute("nothing_else",false);
                Ticket t = (Ticket) request.getSession().getAttribute("currentActivity");
                a = t;
                for(Person r:recipientList){
                    Recipient recipient = getRecipientById(em,r.getId());
                    if(recipient.getEmailAddress().equals(t.getContact().getEmail())){
                        em.getTransaction().begin();
                        Recipient r1 = getRecipientById(em,recipient.getId());
                        r1.setFullName(t.getContact().getFirstName()+ " " + t.getContact().getLastName());
                        em.persist(r1);
                        em.getTransaction().commit();
                    }
                }
                break;
            default:
                em.getTransaction().begin();
                a = new CheckList();
                a.setAssignedTo(currentPerson);
                a.setDueDate(Date.valueOf(LocalDate.now().minusDays(1)));
                a.setComplete(true);
                a.setDateCompleted(Date.valueOf(LocalDate.now().minusDays(1)));
                a.setLoggedBy(currentPerson);
                a.setFullName("EMAIL TO: " + recipientList.get(0).getEmail());
                em.persist(a);
                em.getTransaction().commit();
                break;
        }
        String eSubject = request.getParameter("subject");
        String eBody = request.getParameter("emailBody");
        String ccEmail = request.getParameter("ccEmail");
        if(dbEmail.isValidEmail(ccEmail)){
            em.getTransaction().begin();
            Person r2 = new Person();
            r2.setEmail(ccEmail);
            r2.setFullName(ccEmail);
            em.persist(r2);
            em.getTransaction().commit();
            recipientList.add(r2);
        }

        List<WebLink> webLinkList = getWebLinkList(request,em);
        em.getTransaction().begin();
        Email e = new Email();
        e.setCreatedBy(currentPerson);
        e.setRecipientList(recipientList);
        e.setSubject(eSubject);
        e.setActivity(a);
        e.setDateGenerated(Date.valueOf(LocalDate.now()));
        e.setDetail(eBody);
        e.setStatus(dM.getActivityStatusById(em,2));
        e.setReasonCreated(dM.getReasonById(em,7));
        em.persist(e);
        em.getTransaction().commit();
        System.out.println("F:1 Size = "+webLinkList.size());
        int endOfIt = webLinkList.size();
        for (int i = 0; i< endOfIt;i++) {
            System.out.println("F:1:"+i);
            em.getTransaction().begin();
            WebLink webLink = getWebLinkById(em, webLinkList.get(i).getId());
            webLink.setEmail(e);
            em.persist(webLink);
            em.getTransaction().commit();
            webLinkList.add(webLink);
            em.getTransaction().begin();
            e.getWebLinkList().add(webLink);
            em.persist(e);
            em.getTransaction().commit();
        }
        System.out.println("F:2");
        return e;
    }

    private WebLink getWebLinkById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.id = :id");
        q.setParameter("id",id);
        return (WebLink) q.getSingleResult();
    }

    private List<WebLink> getWebLinkList(HttpServletRequest request, EntityManager em) throws ServletException, IOException {

        Part filePart1 = request.getPart("emailFile1");
        WebLink wl1 = null;
        if(filePart1 !=null && filePart1.getSize() !=0)
            wl1 = getWebLinkFromFile(request,em,filePart1);

        Part filePart2 = request.getPart("emailFile2");
        WebLink wl2 = null;
        if(filePart2 !=null && filePart2.getSize()!=0)
            wl2 = getWebLinkFromFile(request,em,filePart2);

        List<WebLink> webLinkList = new ArrayList<>();
            if(wl1!=null)
                webLinkList.add(wl1);
            if(wl2!=null)
                webLinkList.add(wl2);
        return webLinkList;
    }

    private WebLink getWebLinkFromFile(HttpServletRequest request, EntityManager em, Part filePart) throws ServletException, IOException {

        String fileName = filePart.getSubmittedFileName();
        String extension = AddFileToTask.getExtensionByStringHandling(fileName).orElse("fnf");
        String newFileName = UUID.randomUUID() +"."+ extension;
        String thePath= dbA.getSavePath(em) + newFileName;
        String fileDescription = fileName.replaceAll(" ","_");

        WebLink webLink = new WebLink();
        webLink.setLinkPath(newFileName);
        webLink.setPlainText(fileDescription);
        LinkType linkType = ddC.getLinkTypeById(em,1);
        webLink.setLinkType(linkType);
        em.getTransaction().begin();
        em.persist(webLink);
        em.getTransaction().commit();

        for(Part part : request.getParts()){
            part.write(thePath);
        }
        return webLink;
    }



    private Recipient getRecipientById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT r FROM Recipient r WHERE r.id =:id");
        q.setParameter("id",id);
        return(Recipient) q.getSingleResult();
    }

    private List<Person> getRecipientList(EntityManager em, String rawList){
        List<String> emailList = new ArrayList<>();
        String emailFound;
        int location = rawList.indexOf(",");
        if(location==-1)
            if(dbEmail.isValidEmail(rawList))
                emailList.add(rawList);
        else{
            emailFound=rawList.substring(0,location);
            if(dbEmail.isValidEmail(emailFound))
                emailList.add(emailFound);
            int nextLocation;
            while(rawList.indexOf(",",location+1)!=-1){
                nextLocation = rawList.indexOf(",",location+1);
                emailFound = rawList.substring(location+1,nextLocation);
                location = nextLocation;
                if(dbEmail.isValidEmail(emailFound))
                    emailList.add(emailFound);
            }
            emailFound = rawList.substring(location+1);
            if(dbEmail.isValidEmail(emailFound))
                emailList.add(emailFound);
        }
        List<Person> recipientList = new ArrayList<>();
        if(emailList.size()>0){
            for(String e:emailList){
                em.getTransaction().begin();
                Person r = new Person();
                r.setEmail(e);
                r.setFullName(e);
                em.persist(r);
                em.getTransaction().commit();
                recipientList.add(r);
            }
        }
        return recipientList;
    }
}

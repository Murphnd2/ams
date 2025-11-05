package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.activity.checklist.task.AddFileToTask;
import net.superiorstate.ams.previous.data.misc.dbA;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import jakarta.mail.MessagingException;
import java.io.*;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "EmailActions", value = "/EmailActions")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    //1 MB
        maxFileSize = 1024 * 1024 * 10,         //10 MB
        maxRequestSize = 1024 * 1024 * 100      //100 MB
)
public class EmailActions extends HttpServlet {
    private String NEXT_PAGE = "GoEmailHome";
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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(NEXT_PAGE);
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request) throws ServletException, IOException, MessagingException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String btnString = request.getParameter("btnSubmit");
        String code = btnString.substring(0,2);
        switch(code){
            case "Up":
                NEXT_PAGE = "GoEmailHome";
                System.out.println("CHANGE VIEW: Up");
                addFile(request, em);
                break;
            case "AT":
                NEXT_PAGE = "GoEmailHome";
                System.out.println("CHANGE VIEW: AT");
                if(!validEntry(request)){
                    System.out.println("CHANGE VIEW: AT: INVALID");
                } else if(foundPerson(request,em)){
                    System.out.println("CHANGE VIEW: AT: FOUND");
                    addRecipient(request,em);}
                else{
                    System.out.println("CHANGE VIEW: AT: GET");
                    getName(request);
                }
                break;
            case "RA":
                NEXT_PAGE = "GoEmailHome";
                System.out.println("CHANGE VIEW: RA");
                removeAttachment(request,em);
                break;
            case "RT":
                NEXT_PAGE = "GoEmailHome";
                System.out.println("CHANGE VIEW: RT");
                removeRecipient(request,em);
                break;
            case "SE":
                NEXT_PAGE = "GoEmailHome";
                System.out.println("CHANGE VIEW: SE");
                sendMessage(request,em);
                break;
            case "CA":
                NEXT_PAGE = "GoEmailHome";
                request.getSession().setAttribute("personNotFound",false);
                request.getSession().setAttribute("currentEmailString","");
                break;
        }
        em.close();
    }

    private Activity getAppropriateActivity(HttpServletRequest request, EntityManager em, Person currentPerson){
        int adminView = Integer.parseInt(request.getSession().getAttribute("adminView").toString());
        if(adminView == 1 || adminView == 2 || adminView == 3 || adminView == 4)
            return (Activity) request.getSession().getAttribute("currentActivity");
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setCompletedBy(currentPerson);
        c.setComplete(true);
        c.setDateCompleted(Date.valueOf(LocalDate.now().minusDays(1)));
        c.setFullName("EMAIL SENT TO RECIPIENTS");
        c.setAssignedTo(currentPerson);
        c.setDueDate(Date.valueOf(LocalDate.now().minusDays(1)));
        em.persist(c);
        em.getTransaction().commit();
        return c;
    }

    private void sendMessage(HttpServletRequest request, EntityManager em) throws MessagingException {
        List<Person> recipientList = (List<Person>) request.getSession().getAttribute("recipientList");
        if(recipientList.size()<1) {
            saveInterimData(request);
            NEXT_PAGE = "GoEmailHome";
            return;
        }

        String eSubject = request.getParameter("eSubject");
        String messageBody = request.getParameter("messageBody");
        if(eSubject==null || eSubject.equals("") || messageBody==null || messageBody.equals("")){
            saveInterimData(request);
            NEXT_PAGE = "GoEmailHome";
            return;
        }

        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        Activity a = getAppropriateActivity(request,em, currentPerson);

        String userSignature = "<p> " + currentPerson.getFirstName() + " " + currentPerson.getLastName() + "<br/>";
        userSignature += currentPerson.getPsp().getFullName() + "</p>";


        em.getTransaction().begin();
        Email email = new Email();
        email.setActivity(a);
        email.setSubject(eSubject);
        email.setDateGenerated(Date.valueOf(LocalDate.now()));
        email.setStatus(dM.getActivityStatusById(em,1));
        email.setReasonCreated(dM.getReasonById(em,7));
        email.setCreatedBy(currentPerson);
        email.setDetail(messageBody + userSignature);
        em.persist(email);
        em.getTransaction().commit();

        List<WebLink> attachmentList = (List<WebLink>) request.getSession().getAttribute("attachmentList");
        for(int i = 0; i < attachmentList.size();i++){
            em.getTransaction().begin();
            WebLink w = ddC.getWebLinkById(em,attachmentList.get(i).getId());
            w.setEmail(email);
            em.persist(w);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Email e = dM.getEmailById(em,email.getId());
            e.getWebLinkList().add(w);
            em.persist(e);
            em.getTransaction().commit();
        }

        Activity activityToAppend = dM.getActivityById(em,a.getId());
        em.getTransaction().begin();
        assert activityToAppend != null;
        activityToAppend.addNote(email);
        em.persist(activityToAppend);
        em.getTransaction().commit();

        for(int j = 0; j < recipientList.size(); j++){
            System.out.println("Index-" + j + ":PersonID-"+recipientList.get(j).getId());
            em.getTransaction().begin();
            Person person = dM.getPersonById(em,recipientList.get(j).getId());
            Email email1 = dM.getEmailById(em,email.getId());
            email1.addRecipient(person);
            em.persist(email1);
            em.persist(person);
            em.getTransaction().commit();
        }
        Email emailToSend = dM.getEmailById(em,email.getId());

        dbEmail.sendEmail(emailToSend,em);
        NEXT_PAGE = "GoAdminHome";
    }

    private void removeRecipient(HttpServletRequest request, EntityManager em){
        saveInterimData(request);
        String btnString = request.getParameter("btnSubmit");
        Long personId = Long.parseLong(btnString.substring(3));
        List<Person> recipientList = (List<Person>) request.getSession().getAttribute("recipientList");
        Person p = dM.getPersonById(em,personId);
        List<Person> newList = new ArrayList<>();
        for(Person person:recipientList)
            if(person.getId()!=p.getId())
                newList.add(person);
        request.getSession().setAttribute("recipientList",newList);
    }

    private void removeAttachment(HttpServletRequest request, EntityManager em){
        saveInterimData(request);
        String btnString = request.getParameter("btnSubmit");
        Long webLinkId = Long.parseLong(btnString.substring(3));
        List<WebLink> webLinkList = (List<WebLink>) request.getSession().getAttribute("attachmentList");
        WebLink webLink = ddC.getWebLinkById(em,webLinkId);
        webLinkList.remove(webLink);
        request.getSession().setAttribute("attachmentList",webLinkList);
    }

    private void getName(HttpServletRequest request){
        request.getSession().setAttribute("personNotFound",true);
    }

    private boolean validEntry(HttpServletRequest request){
        request.getSession().setAttribute("emailNotFound",false);
        String emailToAdd = request.getParameter("emailName");
        if(emailToAdd == null || emailToAdd.equals("") || !dbEmail.isValidEmail(emailToAdd)){
            System.out.println("VALID ENTRY: INVALID EMAIL");
            request.getSession().setAttribute("emailNotFound",true);
            return false;
        }
        System.out.println("VALID ENTRY: VALID EMAIL");
        return true;

    }
    private boolean foundPerson(HttpServletRequest request, EntityManager em){
        saveInterimData(request);
        request.getSession().setAttribute("personNotFound",false);
        String emailToAdd = request.getParameter("emailName");
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        if(dbEmail.getPersonByEmail(em,emailToAdd,currentPerson.getPsp())!=null) {
            System.out.println("FOUND PERSON: FOUND PERSON");
            return true;
        }
        String fName = request.getParameter("firstName");
        if(fName==null || fName.equals("")) {
            System.out.println("FOUND PERSON: EMPTY FIRST NAME FIELD");
            return false;
        }
        String lName = request.getParameter("lastName");
        if(lName==null || lName.equals("")) {
            System.out.println("FOUND PERSON: EMPTY LAST NAME FIELD");
            return false;
        }
        System.out.println("FOUND PERSON: CREATE PERSON");
        createPerson(request,em,emailToAdd,fName,lName);
        return true;
    }

    private void createPerson(HttpServletRequest request,EntityManager em, String email, String fName, String lName){
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        em.getTransaction().begin();
        Person p = new Person();
        p.setEmail(email);
        p.setLastName(lName);
        p.setFirstName(fName);
        p.setFullName(fName + " " + lName);
        p.setPsp(currentPerson.getPsp());
        em.persist(p);
        em.getTransaction().commit();
        request.getSession().setAttribute("currentEmailString","");
    }

    private void addRecipient(HttpServletRequest request, EntityManager em){
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        saveInterimData(request);

        String emailToAdd = request.getParameter("emailName");
        Person p = dbEmail.getPersonByEmail(em,emailToAdd,currentPerson.getPsp());

        List<Person> recipientList = (List<Person>) request.getSession().getAttribute("recipientList");

        if(recipientList == null)
            recipientList = new ArrayList<>();

        if(!recipientList.contains(p))
            recipientList.add(p);

        request.getSession().setAttribute("currentEmailString","");

        request.getSession().setAttribute("recipientList",recipientList);
    }



    private void addFile(HttpServletRequest request, EntityManager em) throws ServletException, IOException {
        saveInterimData(request);

        Part filePart = request.getPart("fileUpload");
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

        String optionalFileName = request.getParameter("fileUploadText");
        String fileDescription;
        if(optionalFileName!=null && !optionalFileName.equals(""))
            fileDescription = optionalFileName;
        else
            fileDescription = fileName;
        String correctedDescription = fileDescription.replaceAll(" ","_");
        String extension = AddFileToTask.getExtensionByStringHandling(fileName).orElse("fnf");
        String newFileName = UUID.randomUUID() +"."+ extension;


        InputStream fileContent = filePart.getInputStream();
        String uploadPath = dbA.getSavePath(em);
        File uploadDir = new File(uploadPath);
        if(!uploadDir.exists())
            uploadDir.mkdir();
        File file = new File(uploadPath + File.separator + newFileName);



        OutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int length;
        while ((length=fileContent.read(buffer))>0){
            out.write(buffer,0,length);
        }
        out.close();
        fileContent.close();

        em.getTransaction().begin();
        WebLink w = new WebLink();
        w.setPlainText(correctedDescription);
        w.setLinkPath(newFileName);
        LinkType linkType = ddC.getLinkTypeById(em,1);
        w.setLinkType(linkType);
        em.persist(w);
        em.getTransaction().commit();

        List<WebLink> attachmentList = (List<WebLink>) request.getSession().getAttribute("attachmentList");
        List<WebLink> newList = new ArrayList<>();
        newList.add(w);
        for(int n = 0;n<attachmentList.size();n++)
            newList.add(attachmentList.get(n));
        request.getSession().setAttribute("attachmentList",newList);

    }

    private void saveInterimData(HttpServletRequest request){
        String eSubject = request.getParameter("eSubject");
        request.getSession().setAttribute("currentEmailSubject",eSubject);
        String messageBody = request.getParameter("messageBody");
        request.getSession().setAttribute("messageBody",messageBody);
        String emailString = request.getParameter("emailName");
        request.getSession().setAttribute("currentEmailString",emailString);

    }
}

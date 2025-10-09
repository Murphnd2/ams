package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.activity.AddContactToActivity;
import net.superiorstate.ams.previous.controller.general.admin.q.StdAuto;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SendEmployerBillingDetail", value = "/SendEmployerBillingDetail")
public class SendEmployerBillingDetail extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendEmailWithBilling(request);
        goToPage(request,response);
    }
    private void sendEmailWithBilling(HttpServletRequest request){
        List<Person> distributionList = getDistributionList(request);
        if(distributionList.size()==0)
            return;
        String message = getMessage(request);
        Ticket t = createAndGetTicketForMessage(request,distributionList);
        Email email = createEmail(request,distributionList,t, message);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Email e = dM.getEmailById(em,email.getId());
        try {
            dbEmail.sendEmail(e,em);
        } catch (MessagingException ex) {
            throw new RuntimeException(ex);
        }
        em.close();
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ResetBillingView");
        dispatcher.forward(request,response);
    }
    private String getMessage(HttpServletRequest request){
        Person currentUser = (Person) request.getSession().getAttribute("currentPerson");
        String preText = request.getParameter("preLinkText");
        String postText = request.getParameter("postLinkArea");
        String bcLink = request.getSession().getAttribute("bcLink").toString();
        String message = "<p>"+preText+"</p><p><a href = \""+bcLink +"\" target=\"_blank\">BILLING DETAIL LINK</a></p><p>"+postText+"</p>";
        message = message + "<p>" + currentUser.getFullName() + "</p>";
        return message;
    }

    private Email createEmail(HttpServletRequest request,List<Person> dList, Ticket t, String message){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person currentUser =(Person) request.getSession().getAttribute("currentPerson");
        em.getTransaction().begin();
        Email e = new Email();
        e.setActivity(t);
        e.setSubject("Superior State Monthly Billing Detail");
        e.setDateGenerated(Date.valueOf(LocalDate.now()));
        e.setStatus(dM.getActivityStatusById(em,1));
        e.setReasonCreated(dM.getReasonById(em,7));
        e.setCreatedBy(currentUser);
        e.setDetail(message);
        em.persist(e);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Activity a = dM.getActivityById(em,t.getId());
        assert a != null;
        a.getNoteList().add(e);
        em.persist(a);
        em.getTransaction().commit();

        List<Person> fullList = new ArrayList<>(dList);
        if(!fullList.contains(currentUser))
            fullList.add(currentUser);

        StdAuto.addRecipientsToEmail(em,e,fullList);
        em.close();
        return e;
    }
    private Ticket createAndGetTicketForMessage(HttpServletRequest request, List<Person> distroList){
        Person currentUser =(Person) request.getSession().getAttribute("currentPerson");
        Person primary = distroList.get(0);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Task task = dM.getTaskById(em,56401L);

        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setLoggedBy(currentUser);
        c.setDueDate(Date.valueOf(LocalDate.now()));
        c.setComplete(true);
        c.setCompletedBy(currentUser);
        c.setDateCompleted(Date.valueOf(LocalDate.now()));
        c.setFullName(primary.getFullName());
        em.persist(c);
        em.getTransaction().commit();

        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setCheckList(c);
        toDo.setTask(task);
        toDo.setSortOrder(10);
        toDo.setComplete(true);
        toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
        toDo.setCompletedBy(currentUser);
        em.persist(toDo);
        em.getTransaction().commit();

        List<ToDo> toDoList = new ArrayList<>();
        toDoList.add(toDo);

        em.getTransaction().begin();
        Ticket t = new Ticket();
        t.setPrimaryContact(primary);
        t.setContact(primary);
        t.setCheckList(c);
        t.setComplete(true);
        t.setTicketSubCategory(dM.getSubCategoryById(em,56328L));
        t.setDueDate(Date.valueOf(LocalDate.now()));
        t.setDateCompleted(Date.valueOf(LocalDate.now()));
        t.setLoggedBy(currentUser);
        t.setFullName(primary.getFullName().toUpperCase());
        t.setAssignedTo(currentUser);
        t.setDescription("Please send the monthly billing detail.");
        em.persist(t);
        em.getTransaction().commit();

        em.getTransaction().begin();
        c.setToDoList(toDoList);
        c.setTicket(t);
        em.persist(c);
        em.getTransaction().commit();

        if(distroList.size()>1){
            for(int i = 1; i < distroList.size();i++){
                em.getTransaction().begin();
                t.addAssigneeContact(distroList.get(i));
                em.persist(t);
                em.getTransaction().commit();
            }
        }
        em.close();
        return t;
    }

    private List<String> getEmailList(String start){
        if(start==null || start.equals(""))
            return null;
        List<String> emailList = new ArrayList<>();
        if(!start.contains(";")){
            emailList.add(start.toLowerCase());
        } else {
            String remainingText = start.trim();
            if(remainingText.endsWith(";"))
                remainingText = remainingText.substring(0,remainingText.length()-1);

            String email;
            while(remainingText.contains(";")){
                int semiLoc = remainingText.indexOf(";");
                email = remainingText.substring(0,semiLoc).trim().toLowerCase();
                if(!emailList.contains(email))
                    emailList.add(email);
                remainingText = remainingText.substring(semiLoc+1);
            }
            if(!emailList.contains(remainingText))
                emailList.add(remainingText);
        }
        return emailList;
    }

    private List<Person> getDistributionList(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        List<Person> availableList = (List<Person>) request.getSession().getAttribute("employerContactList");
        List<Person> distributionList = new ArrayList<>();
        if(availableList!=null && availableList.size()>0){
            for(int i = 0; i < availableList.size(); i++){
                int j = i+1;
                String pName = "eCheck" + j;
                String value = request.getParameter(pName);
                if(value==null)
                    continue;
                distributionList.add(availableList.get(i));
            }
        }
        String additionalEmails = request.getParameter("additionalEmails");
        if(additionalEmails!=null && !additionalEmails.equals("")) {
            Employer employer = (Employer) request.getSession().getAttribute("currentBillingEmployer");
            List<String> emailList = getEmailList(additionalEmails);
            for(String e: emailList){
                Person p = AddContactToActivity.investigateEmail(em,e,employer);
                if(!distributionList.contains(p))
                    distributionList.add(p);
            }
        }
        em.close();
        return distributionList;
    }
}

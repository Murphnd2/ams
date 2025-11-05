package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.q.StdAuto;
import net.superiorstate.ams.previous.data.misc.dP;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.Person;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@WebServlet(name = "SendQuote1", value = "/SendQuote1")
public class SendQuote1 extends HttpServlet {
    private String agentName;
    private String agentEmail;
    private Person agent;
    private String contactName;
    private String contactEmail;
    private Person contact;
    private String company;
    private String proposal;
    private Activity currentActivity;
    private Ticket currentTicket;
    private String toWho;

    private boolean isFollowUp;

    private Email email;
    private Person user;
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendProposal(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendProposal(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    private boolean isFollowUp(HttpServletRequest request){
        String fu;
        try{
            fu = request.getParameter("fu").toString();
        } catch (Exception ex){
            fu = "N";
        }
        isFollowUp = fu.equals("Y");
        return isFollowUp;
    }
    private void sendProposal(HttpServletRequest request, HttpServletResponse response) throws MessagingException {

        currentActivity = (Activity) request.getSession().getAttribute("currentActivity");
        if(!currentActivity.getClass().getSimpleName().equals("Ticket"))
            return;
        currentTicket = (Ticket) currentActivity;
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        if(isFollowUp(request)){
            user = (Person) request.getSession().getAttribute("currentPerson");
            resetConstants((Ticket) currentActivity,em);
        } else {
            assignConstants(request);
            findOrCreatePersons(em);
            updateTicketText(em);
            closeSendProposalTask(em);
            updateTicketOwner(em);
        }
        getEmailMessage(em);
        assignEmailRecipients(em);
        addEmailToActivityNotes(em);
        dbEmail.sendEmail(email,em);
        changeDueDate(em);
        ViewSelectedActivity.setActivityView(request,em,currentActivity);
        em.close();
    }

    private void resetConstants(Ticket t, EntityManager em){
        String fullText = t.getDescription();
        company = getString("c",fullText);
        System.out.println(company);
        agentName = getString("a",fullText);
        System.out.println(agentName);
        agentEmail = getString("m",fullText);
        System.out.println(agentEmail);
        contactName = getString("n",fullText);
        System.out.println(contactName);
        contactEmail = getString("e",fullText);
        System.out.println(contactEmail);
        proposal = getString("p",fullText);
        System.out.println(proposal);
        toWho = "P";
        if(t.getContact().getEmail().equals(agentEmail))
            toWho = "B";
        agent = dP.getPersonByEmail(em,agentEmail);
        if(toWho.equals("P"))
            contact = dP.getPersonByEmail(em,contactEmail);
    }

    private void changeDueDate(EntityManager em){
        LocalDate localDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        LocalDate localDueDate = localDate.plusDays(7L);
        LocalDate currentDueDate = currentTicket.getDueDate().toLocalDate();
        if(currentDueDate.compareTo(localDueDate)>0)
            return;
        Date dueDate = Date.valueOf(localDueDate);
        em.getTransaction().begin();
        Ticket t = dM.getTicketById(em,currentTicket.getId());
        assert t != null;
        t.setDueDate(dueDate);
        em.persist(t);
        em.getTransaction().commit();
    }
    private void updateTicketOwner(EntityManager em){
        if(contact==null)
            return;
        boolean assignedToAgent = false;
        if(currentTicket.getContact().getEmail().equals(agentEmail))
            assignedToAgent = true;
        if(toWho.equals("B") && assignedToAgent)
            return;
        em.getTransaction().begin();
        Ticket t = dM.getTicketById(em, currentTicket.getId());
        Person correctContact;
        assert t != null;
        if(assignedToAgent) {
            correctContact = dM.getPersonById(em, contact.getId());
            t.setFullName(correctContact.getFullName());
        } else {
            correctContact = dM.getPersonById(em, agent.getId());
            t.setFullName(correctContact.getFullName());
        }
        t.setContact(correctContact);
        em.persist(t);
        em.getTransaction().commit();
    }


    private  String getString(String codeLetter, String fullText){
        String code = "|"+codeLetter+"|";
        int startIndex = fullText.indexOf(code);
        int endIndex = fullText.indexOf(code,startIndex+1);
        return fullText.substring(startIndex+3,endIndex);
    }
    private void updateTicketText(EntityManager em){
        String oldText = currentTicket.getDescription();
        String tt;
        tt = "<p><b>Company:</b> <span style=\"color:blue\"><b>|c|" + company + "|c|</b></span>" +
                "<br/><u>Name:</u> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;;&nbsp;<u>|n|" + contactName + "|n|</u>" +
                "<br/><i>Email:</i> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;;&nbsp;<i>|e|" + contactEmail + "|e|</i>" +
                "<br/><u>Agent:</u> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;;&nbsp;<u>|a|" + agentName + "|a|</u>" +
                "<br/><i>Email:</i> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;;&nbsp;<i>|m|" + agentEmail + "|m|</i>" +
                "<br/><span style=\"color:blue\">Proposal:&nbsp;</span><a href=\"" + proposal + "\" target=\"_blank\"> |p|" + proposal + "|p|</a></p>";
        em.getTransaction().begin();
        Ticket t = dM.getTicketById(em,currentTicket.getId());
        assert t != null;
        t.setDescription(tt);
        em.persist(t);
        em.getTransaction().commit();
        em.getTransaction().begin();
        Note note = new Note();
        note.setStatus(dM.getActivityStatusById(em,2));
        note.setReasonCreated(dM.getReasonById(em,1));
        note.setCreatedBy(user);
        note.setDateGenerated(Date.valueOf(LocalDate.ofInstant(Instant.now(),ZoneId.systemDefault())));
        note.setActivity(currentActivity);
        note.setDetail("<p>** ORIGINAL TEXT **<br/>" + oldText + "</p>");
        em.persist(note);
        em.getTransaction().commit();
    }

    private void closeSendProposalTask(EntityManager em){
        ToDo toDo = getSendProposalToDoForThisActivity(em);
        if(toDo==null)
            return;
        em.getTransaction().begin();
        ToDo t = dM.getToDoById(em,toDo.getId());
        assert t != null;
        t.setComplete(true);
        t.setDateCompleted(StdAuto.getNow());
        t.setCompletedBy(user);
        em.persist(t);
        em.getTransaction().commit();
    }

    private ToDo getSendProposalToDoForThisActivity(EntityManager em){
        Query q = em.createQuery("SELECT t From ToDo t WHERE t.task.id = :taskId AND t.checkList.assignedTo.id = :aId");
        q.setParameter("taskId", 38590L);
        q.setParameter("aId", currentActivity.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
            return toDoList.get(0);
        } catch (NoResultException e){
            return null;
        }
    }
    private void assignEmailRecipients(EntityManager em){
        em.getTransaction().begin();
        Email e = dM.getEmailById(em,email.getId());
        if(e!=null){
            if(dbEmail.isValidEmail(agent.getEmail()))
                e.addRecipient(agent);
            if(toWho.equals("P"))
                if(dbEmail.isValidEmail(contact.getEmail()))
                    e.addRecipient(contact);
            em.persist(e);
        }
        em.getTransaction().commit();
        email = e;
    }
    private void addEmailToActivityNotes(EntityManager em){
        em.getTransaction().begin();
        Activity a = dM.getActivityById(em,currentActivity.getId());
        if(a!=null){
            Email e = dM.getEmailById(em,email.getId());
            a.getNoteList().add(e);
            em.persist(a);
        }
        em.getTransaction().commit();
        currentActivity = a;
    }

    private void getEmailMessage(EntityManager em){
        em.getTransaction().begin();
        Email e = new Email();
        e.setActivity(currentActivity);
        e.setSubject("Service Proposal from Superior State");
        if(isFollowUp)
            e.setSubject("Follow up on Service Proposal from Superior State");
        e.setDateGenerated(StdAuto.getNow());
        e.setStatus(dM.getActivityStatusById(em,1));
        e.setReasonCreated(dM.getReasonById(em,8));
        e.setCreatedBy(user);
        e.setDetail(getEmailText());
        if(isFollowUp)
            e.setDetail(getEmailTextAlt());
        em.persist(e);
        em.getTransaction().commit();
        email = e;
    }

    private String getEmailText(){
        String theMessage= "";
        theMessage += "<p>Please find below a link to the service proposal for " + company + " that you have requested. " +
                "Proposal: <a href=\"" + proposal + "\" target=\"_blank\">" + proposal + "</a>";
        theMessage += StdAuto.userSignature(user);
        return theMessage;
    }
    private String getEmailTextAlt(){
        String theMessage= "";
        theMessage += "<p>Please find below a link to the service proposal for " + company + " that you have requested. " +
                "Proposal: <a href=\"" + proposal + "\" target=\"_blank\">" + proposal + "</a>";
        theMessage += StdAuto.userSignature(user);
        return theMessage;
    }

    private void findOrCreatePersons(EntityManager em){
        agent = dP.getPersonByEmail(em,agentEmail);
        if(agent==null)
            agent = createPersonFromEmail(em,agentEmail);
        contact = dP.getPersonByEmail(em,contactEmail);
        if(contact==null)
            contact = createPersonFromEmail(em,contactEmail);
    }
    private Person createPersonFromEmail(EntityManager em, String email){
        em.getTransaction().begin();
        Person person = new Person();
        person.setEmail(email);
        person.setFirstName(getFirstName(email));
        person.setLastName(getLastName(email));
        person.setPsp(dM.getPspById(em,4));
        person.setAddress(new Address());
        em.persist(person);
        em.getTransaction().commit();
        return person;
    }
    private String getFirstName(String email){
        String fullName = contactName;
        if(agentEmail.equals(email))
            fullName=agentName;
        fullName = fullName.trim();
        if(fullName.contains(","))
            return fullName.substring(fullName.indexOf(",")+1).trim();
        if(fullName.contains(" "))
            return fullName.substring(0,fullName.indexOf(" "));
        return "--unknown--";
    }

    private String getLastName(String email){
        String fullName = contactName;
        if(agentEmail.equals(email))
            fullName=agentName;
        fullName = fullName.trim();
        if(fullName.contains(","))
            return fullName.substring(0, fullName.indexOf(",")).trim();
        if(fullName.contains(" "))
            return fullName.substring(fullName.indexOf(" ")+1).trim();
        return fullName;
    }
    private void assignConstants(HttpServletRequest request){
        toWho = request.getSession().getAttribute("pToWho").toString();
        agentName = request.getSession().getAttribute("pAgentName").toString().trim();
        contactName = request.getSession().getAttribute("pContactName").toString().trim();
        agentEmail = request.getSession().getAttribute("pAgentEmail").toString().trim();
        contactEmail = request.getSession().getAttribute("pContactEmail").toString().trim();
        company = request.getSession().getAttribute("pCompany").toString().trim();
        proposal = request.getSession().getAttribute("pProposal").toString().trim();
        user = (Person) request.getSession().getAttribute("currentPerson");
        currentTicket = (Ticket) currentActivity;
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
}

package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.dao.PersonDAO;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.ActivityStatus;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.activity.ticket.tEmployee;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class StdAuto {
    public static List<Person> getRecipientList(HttpServletRequest request, EntityManager em, Activity a){
        List<Person> recipientList = new ArrayList<>();
        switch (getClassType(a)) {
            case "Renewal" -> {
                List<tEmployee> contactList = (List<tEmployee>) request.getSession().getAttribute("contactList");
                if (contactList != null && contactList.size() > 0) {
                    for (tEmployee te : contactList) {
                        Employee e = EntityLookup.getEmployeeById(em, te.getId());
                        Person person = PersonDAO.getPersonByEe(em, e, getPSP(request));
                        if(person!=null && !recipientList.contains(person))
                            recipientList.add(person);
                    }
                }
                Renewal r = (Renewal) a;
                for (Person p: r.getAssigneeContactList()){
                    if(p!=null && !recipientList.contains(p))
                        recipientList.add(p);
                }
                if(r.getPrimaryContact()!=null && !recipientList.contains(r.getPrimaryContact()))
                    recipientList.add(r.getPrimaryContact());
            }
            case "Setup" -> {
                Setup s = (Setup) a;
                if(s.getPrimaryContact()!=null)
                    recipientList.add(s.getPrimaryContact());
                if(s.getPrimaryContactSetup()!=null && !recipientList.contains(s.getPrimaryContactSetup()));
                    recipientList.add(s.getPrimaryContactSetup());
                for(Person p: s.getContactList()){
                    if(p!=null && !recipientList.contains(p))
                        recipientList.add(p);
                }
                for(Person p: s.getAssigneeContactList()){
                    if(p!=null && !recipientList.contains(p))
                        recipientList.add(p);
                }
            }
            case "Ticket" -> {
                Ticket t = (Ticket) a;
                if(t.getPrimaryContact()!=null)
                    recipientList.add(t.getPrimaryContact());
                if(t.getContact()!=null && !recipientList.contains(t.getContact()))
                    recipientList.add(t.getContact());
                for(Person p: t.getAssigneeContactList()){
                    if(p!=null && !recipientList.contains(p))
                        recipientList.add(p);
                }
            }
        }
        return recipientList;
    }

    private static PSP getPSP(HttpServletRequest request){
        Person p = (Person) request.getSession().getAttribute("currentPerson");
        return p.getPsp();
    }

    public static CheckList getChecklist(Activity a){
        switch (getClassType(a)){
            case "Renewal":
                Renewal r = (Renewal) a;
                return r.getCheckList();
            case "Setup":
                Setup s = (Setup) a;
                return s.getCheckList();
            default: return null;
        }
    }


    private static String getClassType(Activity a){
        return a.getClass().getSimpleName();
    }

    public static void closeThisTask(EntityManager em, Activity a, Long taskId, Person user){
       ToDo toDo = getToDoForTask(em,a,taskId);
       if(toDo==null)
           return;
       em.getTransaction().begin();
       toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
       toDo.setComplete(true);
       toDo.setCompletedBy(user);
       em.persist(toDo);
       em.getTransaction().commit();
    }

    private static ToDo getToDoForTask(EntityManager em, Activity a, Long taskId){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId AND t.task.id = :tId");
        q.setParameter("tId",taskId);
        q.setParameter("cId",getChecklist(a).getId());
        ToDo toDo;
        try{
            toDo = (ToDo) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            toDo = null;
        }
        return toDo;
    }

    public static Person getCurrentPerson(HttpServletRequest request){
        return (Person) request.getSession().getAttribute("currentPerson");
    }
    public static Activity getCurrentActivity(HttpServletRequest request){
        return (Activity) request.getSession().getAttribute("currentActivity");
    }

    public static Email createEmail(HttpServletRequest request, EntityManager em, Activity a, String subject, String message, ActivityStatus status, Person user){
        int hasCc = 0;
        String ccList="";
        try{
            hasCc = Integer.parseInt(request.getSession().getAttribute("useCcList").toString());
            ccList = request.getSession().getAttribute("ccList").toString();
        } catch (Exception e){
            e.printStackTrace();
        }

        Email e = new Email();
        em.getTransaction().begin();
        e.setActivity(a);
        e.setSubject(subject);
        e.setDateGenerated(getNow());
        e.setStatus(status);
        e.setReasonCreated(EntityLookup.getReasonById(em,8));
        e.setCreatedBy(user);
        e.setDetail(message);
        em.persist(e);
        em.getTransaction().commit();
        em.getTransaction().begin();
        Activity activity = EntityLookup.getActivityById(em,a.getId());
        activity.getNoteList().add(e);
        em.persist(activity);
        em.getTransaction().commit();
        List<Person> recipientList = getRecipientList(request,em,a);
        List<Person> finalList;
        if(hasCc==1) {
            finalList = getUpdatedList(em, recipientList, ccList);
            System.out.println("Recognized This");
        }
        else finalList = recipientList;
        addRecipientsToEmail(em,e,finalList);
        return e;
    }

    private static List<Person> getUpdatedList(EntityManager em, List<Person> rl, String ccList){
        List<Person> returnList = new ArrayList<>();
        returnList.addAll(rl);
        System.out.println("GOT TOP---------------------------------------------");
        if(ccList.contains(";")){
            String unprocessed = ccList;
            while(unprocessed.contains(";")){
                int scLoc = unprocessed.indexOf(";");
                String email = unprocessed.substring(0,scLoc);
                if(EmailDAO.isValidEmail(email.trim())){
                    Person p = getPersonByEmail(em, email.trim());
                    if(p!=null)
                        returnList.add(p);
                }
                if(scLoc<unprocessed.length()-1){
                    String holder = unprocessed.substring(scLoc+1);
                    unprocessed = holder;
                } else unprocessed = "";
            }
        } else {
            System.out.println("RECOGNIZED NO SEMI COLON-----------------------------------");
            if(EmailDAO.isValidEmail(ccList.trim())){
                Person p = getPersonByEmail(em,ccList.trim());
                System.out.println("GOT HERE: " + p.getFullName());
                if(p!=null)
                    returnList.add(p);
            }
        }
        return returnList;
    }

    private static Person getPersonByEmail(EntityManager em, String email){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.email = :email order by p.id desc");
        q.setParameter("email",email);
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(personList.size()==0)
            return null;
        return personList.get(0);
    }

    public static Email createEmail(HttpServletRequest request, EntityManager em, Activity a, String subject, String message, int statusId, Person user){
        return createEmail(request,em,a,subject,message, EntityLookup.getActivityStatusById(em,statusId),user);
    }


    public static String userSignature(Person currentPerson){
        String userSignature = "<p> " + currentPerson.getFirstName() + " " + currentPerson.getLastName() + "<br/>";
        userSignature += currentPerson.getEmployee().getEmployer().getEmployerName() + "<br/>";
        if(currentPerson.getPhone() == null || currentPerson.getPhone().equals(""))
            userSignature += currentPerson.getEmployee().getEmployer().getPhone() + "</p>";
        else userSignature += currentPerson.getPhone() + "</p>";
        return userSignature;
    }
    public static Date getNow(){
        return Date.valueOf(LocalDate.now());
    }

    public static void addRecipientsToEmail(EntityManager em, Email e, List<Person> recipientList){
        for(int j = 0; j < recipientList.size(); j++){
            em.getTransaction().begin();

            Person person = EntityLookup.getPersonById(em,recipientList.get(j).getId());
            Email email1 = EntityLookup.getEmailById(em,e.getId());
            if(!email1.getRecipientList().contains(person))
                email1.addRecipient(person);
            em.persist(email1);
            em.persist(person);
            em.getTransaction().commit();
        }
    }
}

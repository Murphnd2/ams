package net.superiorstate.ams.data.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.dao.ChecklistDAO;
import net.superiorstate.ams.data.resolver.EntityFactory;
import net.superiorstate.ams.data.dao.ActivityDAO;
import net.superiorstate.ams.data.dao.RenewalQueryDAO;
import net.superiorstate.ams.data.dao.TicketQueryDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class ActivityViewHelper {
    private static Activity currentActivity;
    private static Person primaryContact;
    private static String classType;

    private static void fillRenewalSpecificLists(HttpServletRequest request, EntityManager em, Renewal r){
        List<Employee> contactList = RenewalQueryDAO.getEmployeesAssignedToRenewal(em,r);
        request.getSession().setAttribute("contactList", TicketQueryDAO.getTicketEmployeeList(em,contactList));
        List<Employee> employeeList = RenewalQueryDAO.getContactsNotAssigned(em,r);
        request.getSession().setAttribute("remainingEmployees", TicketQueryDAO.getTicketEmployeeList(em,employeeList));
        List<Benefit> benefitsNotInRenewal = RenewalQueryDAO.getBenefitsNotInRenewal(em,r);
        request.getSession().setAttribute("benefitsNotInRenewal",benefitsNotInRenewal);

    }
    private static void fillSetupSpecificLists(HttpServletRequest request, EntityManager em, Setup s){
        List<Person> setupContactList = s.getContactList();
        List<ApplicationModule> moduleList = s.getApplication().getApplicationModuleList();
        Person agent = s.getApplication().getProposal().getProspect().getAgent();
        Person setupContact = new Person();
        if(s.getPrimaryContact()==null && s.getPrimaryContactSetup()!=null)
            setupContact = s.getPrimaryContactSetup();
        else if(s.getPrimaryContact()!=null)
            setupContact = s.getPrimaryContact();
        request.getSession().setAttribute("setupContactList",setupContactList);
        request.getSession().setAttribute("setupContact",setupContact);
        request.getSession().setAttribute("moduleList",moduleList);
        request.getSession().setAttribute("setupAgent",agent);
        String propLink = request.getContextPath() + "/serviceProposal?guid=" + s.getApplication().getProposal().getApplicationGUID();
        String appLink = DocumentConstants.ONLINE_APPLICATION_DATA + "&entry=" + s.getApplication().getProposal().getApplicationGUID();
        request.getSession().setAttribute("proposalLink",propLink);
        request.getSession().setAttribute("appLink",appLink);
        List<TemplatePurpose> remainingModules = remainingModules(request,em,moduleList);
        request.getSession().setAttribute("remainingMods",remainingModules);
    }

    public static List<TemplatePurpose> remainingModules(HttpServletRequest request,EntityManager em, List<ApplicationModule> currentMods){
        List<TemplatePurpose> allModulesList = (List<TemplatePurpose>) request.getSession().getAttribute("setupModules");
        List<TemplatePurpose> remainingModules = new ArrayList<>();
        for(TemplatePurpose mod: allModulesList){
            boolean inList = false;
            for(ApplicationModule am: currentMods){
                if(am.getTemplatePurpose().getId()==mod.getId()){
                    inList = true;
                    break;
                }
            }
            if(!inList)
                remainingModules.add(mod);
        }
        return remainingModules;
    }

    public static List<Activity> getPastActivities(HttpServletRequest request, EntityManager em, Activity a){
        if(!a.getClass().getSimpleName().equals("Renewal") && !a.getClass().getSimpleName().equals("Ticket"))
            return new ArrayList<>();
        List<Activity> pastActivities;
        if(a.getClass().getSimpleName().equals("Renewal")){
            Renewal r = (Renewal) a;
            Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.employer.id = :id order by r.id desc");
            q.setParameter("id",r.getEmployer().getId());
            try{
                pastActivities = (List<Activity>) q.getResultList();
            } catch (NoResultException e){
                pastActivities = new ArrayList<>();
            }
        } else {
            Ticket t = (Ticket) a;
            long cId;
            if(t.getPrimaryContact()==null)
                cId = t.getContact().getId();
            else
                cId = t.getPrimaryContact().getId();
            Query q = em.createQuery("SELECT t FROM Ticket t WHERE t.primaryContact.id = :id OR t.contact.id = :id order by t.id desc ");
            q.setParameter("id",cId);
            try{
                pastActivities = (List<Activity>) q.getResultList();
            } catch (NoResultException e){
                pastActivities = new ArrayList<>();
            }
        }
        return pastActivities;

    }
    public static void setActivityView(HttpServletRequest request, EntityManager em, Activity a){
        //vA.changeActivityView(request,em);
        setActivityViewOld(request,em,a);
        request.getSession().setAttribute("pspUserList",getPspUsers(em));
        request.getSession().setAttribute("bpoUserList",getBpoUsers(em));
    }

    private static List<Person> getBpoUsers(EntityManager em){
        return getUsersByRole(em,101);
    }

    private static List<Person> getUsersByRole(EntityManager em, int roleId){
        Query q = em.createQuery("SELECT ur FROM UserRole ur WHERE ur.id = :id");
        q.setParameter("id",roleId);
        UserRole ur = (UserRole) q.getSingleResult();
        List<User> users = ur.getUserList();
        List<Person> personList = new ArrayList<>();
        if(users==null || users.size()==0)
            return personList;
        for(User u:users){
            if(!personList.contains(u.getPerson()))
                personList.add(u.getPerson());
        }
        Collections.sort(personList);
        return personList;    }
    private static List<Person> getPspUsers(EntityManager em){
        return getUsersByRole(em,1);
    }

    private static void setActivityViewOld(HttpServletRequest request, EntityManager em, Activity a){
        Long id = a.getId();
        Activity selectedActivity = EntityLookup.getActivityById(em,id);
        assert selectedActivity != null;
        Person primaryContact = ActivityDAO.getPrimaryContact(em,selectedActivity);
        request.getSession().setAttribute("currentPrimaryContact",primaryContact);
        request.getSession().setAttribute("otherContactList",selectedActivity.getAssigneeContactList());
        request.getSession().setAttribute("activityWebLinkList",selectedActivity.getWebLinkList());
        request.getSession().setAttribute("currentActivityId",id);
        request.getSession().setAttribute("currentActivity",selectedActivity);
        request.getSession().setAttribute("pastActivities",getPastActivities(request,em,selectedActivity));
        request.getSession().setAttribute("rfCodes", ActivityViewHelper.getAutomationInsertLinks(em));
        CheckList c = getCheckListForActivity(em,selectedActivity);
        request.getSession().setAttribute("currentChecklist",c);
        String classType = selectedActivity.getClass().getSimpleName();
        switch (classType){
            case "Renewal":
                request.getSession().setAttribute("adminView",2);
                request.getSession().setAttribute("currentRenewal", EntityLookup.getRenewalById(em,id));
                request.getSession().setAttribute("currentSetup", new Setup());
                request.getSession().setAttribute("currentTicket", new Ticket());
                request.getSession().setAttribute("currentActivityEmployees", ActivityDAO.getEmployeeList(em,selectedActivity));
                fillRenewalSpecificLists(request,em, EntityLookup.getRenewalById(em,id));
                break;
            case "Setup":
                request.getSession().setAttribute("adminView",1);
                request.getSession().setAttribute("currentRenewal", new Renewal());
                request.getSession().setAttribute("currentSetup", EntityLookup.getSetupById(em,id));
                request.getSession().setAttribute("currentTicket", new Ticket());
                fillSetupSpecificLists(request,em, Objects.requireNonNull(EntityLookup.getSetupById(em, id)));
                break;
            case "Ticket":
                request.getSession().setAttribute("adminView",3);
                request.getSession().setAttribute("currentRenewal", new Renewal());
                request.getSession().setAttribute("currentSetup", new Setup());
                Ticket t = EntityLookup.getTicketById(em,id);
                request.getSession().setAttribute("currentTicket", t);
                Employee e = getTicketEmployee(em,t);
                request.getSession().setAttribute("currentEeId","");
                request.getSession().setAttribute("currentEeAltId","");
                request.getSession().setAttribute("currentErId","");
                request.getSession().setAttribute("currentErAltId","");
                request.getSession().setAttribute("tIsEmployee",0);
                request.getSession().setAttribute("currentEmployer3","");
                request.getSession().setAttribute("currentActivityEmployees",new ArrayList<>());
                if(e!=null){
                    request.getSession().setAttribute("tIsEmployee",1);
                    request.getSession().setAttribute("currentActivityEmployees", ActivityDAO.getEmployeeList(em,e.getEmployer()));
                    request.getSession().setAttribute("currentEeId",e.getId());
                    request.getSession().setAttribute("currentEeAltId",e.getMmKey());
                    request.getSession().setAttribute("currentErId",e.getEmployer().getId());
                    request.getSession().setAttribute("currentErAltId",e.getEmployer().getErKey());
                    request.getSession().setAttribute("currentEmployer3",e.getEmployer().getEmployerName());
                }
                break;
            default:
                request.getSession().setAttribute("adminView",999);
                request.getSession().setAttribute("currentRenewal", new Renewal());
                request.getSession().setAttribute("currentSetup", new Setup());
                request.getSession().setAttribute("currentTicket", new Ticket());
                break;
        }
        try{
            List<ToDo> toDoList = ChecklistDAO.getToDoListByChecklistId(em,c.getId());
            request.getSession().setAttribute("currentToDoList",toDoList);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Employee getTicketEmployee(EntityManager em, Ticket ticket){
        if(!isPersonAnEmployee(em,ticket.getContact()))
            return null;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.id = :id");
        q.setParameter("id",ticket.getContact().getEmployee().getId());
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return employeeList.get(0);
    }

    private static boolean isPersonAnEmployee(EntityManager em, Person p){
        Query q = em.createQuery("SELECT p FROM Person p INNER JOIN Employee e ON e.id = p.employee.id");
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            return false;
        }
        if(personList.contains(p))
            return true;
        return false;
    }


    private static void assureChecklistAssignedToActivity(EntityManager em, CheckList c){
        String classType = c.getAssignedTo().getClass().getSimpleName();
        switch (classType) {
            case "Renewal":
                em.getTransaction().begin();
                Renewal r = EntityLookup.getRenewalById(em, c.getAssignedTo().getId());
                r.setCheckList(c);
                em.persist(r);
                em.getTransaction().commit();
                break;
            case "Setup":
                em.getTransaction().begin();
                Setup s = (Setup) EntityLookup.getActivityById(em, c.getAssignedTo().getId());
                s.setCheckList(c);
                em.persist(s);
                em.getTransaction().commit();
                break;
            case "Ticket":
                em.getTransaction().begin();;
                Ticket t = (Ticket) EntityLookup.getActivityById(em,c.getAssignedTo().getId());
                t.setCheckList(c);
                em.persist(t);
                em.getTransaction().commit();
            default:
                break;
        }
    }



    public static CheckList getCheckListForActivity(EntityManager em, Activity a){
        CheckList c;
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :id");
        q.setParameter("id",a.getId());
        try{
            c = (CheckList) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            c = createCheckList(em,a);
        }
        assureChecklistAssignedToActivity(em,c);
        return c;
    }









    private static CheckList createCheckList(EntityManager em, Activity a){
        String classType = a.getClass().getSimpleName();
        CheckList c = new CheckList();
        em.getTransaction().begin();
        c.setAssignedTo(a);
        c.setComplete(false);
        c.setFullName(a.getFullName()+" Checklist");
        c.setDueDate(a.getDueDate());
        c.setLoggedBy((Person) a.getAssignedTo());
        em.persist(c);
        em.getTransaction().commit();
        if(classType.equals("Ticket")){
            ChecklistDAO.createToDoList(em,(Ticket) a,c);
        } else {
            Task t = EntityFactory.createTaskOneTime(em,"Default",((Person) a.getAssignedTo()).getPsp());
            ToDo td = new ToDo();
            em.getTransaction().begin();
            td.setCheckList(c);
            td.setTask(t);
            td.setSortOrder(99999);
            td.setComplete(true);
            em.persist(td);
            em.getTransaction().commit();
        }

        return c;
    }

    public static void changeActivityView(HttpServletRequest request, EntityManager em){
        setTheCurrentActivity(request,em);
        updateSessionAttributes(request,em);
        updateByActivityType(request,em);
    }

    private static void updateByActivityType(HttpServletRequest request,EntityManager em){
        request.getSession().setAttribute("adminView",999);
        request.getSession().setAttribute("currentRenewal", new Renewal());
        request.getSession().setAttribute("currentSetup", new Setup());
        request.getSession().setAttribute("currentTicket", new Ticket());
        switch (getClassType()){
            case "Renewal":
                setRenewalData(request,em);
                break;
            case "Setup":
                setSetupData(request,em);
                break;
            case "Ticket":
                setTicketData(request,em);
                break;
        }
    }

    private static void setTicketData(HttpServletRequest request, EntityManager em){
        Ticket t = (Ticket) getCurrentActivity();
        request.getSession().setAttribute("adminView",3);
        request.getSession().setAttribute("currentTicket", t);

        resetEmployeeSessionData(request);
        Employee e = getTicketEmployee(em,t);
        if(e!=null){
            request.getSession().setAttribute("tIsEmployee",1);
            request.getSession().setAttribute("currentActivityEmployees", ActivityDAO.getEmployeeList(em,e.getEmployer()));
            request.getSession().setAttribute("currentEeId",e.getId());
            request.getSession().setAttribute("currentEeAltId",e.getMmKey());
            request.getSession().setAttribute("currentErId",e.getEmployer().getId());
            request.getSession().setAttribute("currentErAltId",e.getEmployer().getErKey());
            request.getSession().setAttribute("currentEmployer3",e.getEmployer().getEmployerName());
        }

    }

    private static void resetEmployeeSessionData(HttpServletRequest request){
        request.getSession().setAttribute("currentEeId","");
        request.getSession().setAttribute("currentEeAltId","");
        request.getSession().setAttribute("currentErId","");
        request.getSession().setAttribute("currentErAltId","");
        request.getSession().setAttribute("tIsEmployee",0);
        request.getSession().setAttribute("currentEmployer3","");
        request.getSession().setAttribute("currentActivityEmployees",new ArrayList<>());
    }

    public static List<WebLink> getAutomationInsertLinks(EntityManager em){
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.active=true AND w.linkType.id=3 order by w.plainText");
        return (List<WebLink>) q.getResultList();
    }

    private static void setSetupData(HttpServletRequest request,EntityManager em){
        Setup s = (Setup) getCurrentActivity();
        request.getSession().setAttribute("adminView",1);
        request.getSession().setAttribute("currentSetup", s);

        Person agent = s.getApplication().getProposal().getProspect().getAgent();

        Person primaryContact;
        if(s.getPrimaryContact()!=null)
            primaryContact = s.getPrimaryContact();
        else if(s.getPrimaryContactSetup()!=null)
            primaryContact = s.getPrimaryContactSetup();
        else if(s.getAssigneeContactList()!=null && s.getAssigneeContactList().size()>0)
            primaryContact = s.getAssigneeContactList().get(0);
        else if(s.getContactList()!=null && s.getContactList().size()>0)
            primaryContact = s.getContactList().get(0);
        else primaryContact = Objects.requireNonNullElseGet(agent, Person::new);

        List<Person> contacts;
        if(s.getAssigneeContactList()!=null && s.getAssigneeContactList().size()>0)
            contacts = s.getAssigneeContactList();
        else if(s.getContactList()!=null && s.getContactList().size()>0)
            contacts = s.getContactList();
        else
            contacts = new ArrayList<>();

        contacts.remove(primaryContact);

        List<ApplicationModule> moduleList = s.getApplication().getApplicationModuleList();
        request.getSession().setAttribute("setupContactList",contacts);
        request.getSession().setAttribute("setupContact",primaryContact);
        request.getSession().setAttribute("moduleList",moduleList);
        request.getSession().setAttribute("setupAgent",agent);
        String propLink = request.getContextPath() + "/serviceProposal?guid=" + s.getApplication().getProposal().getApplicationGUID();
        String appLink = DocumentConstants.ONLINE_APPLICATION_DATA + "&entry=" + s.getApplication().getProposal().getApplicationGUID();
        request.getSession().setAttribute("proposalLink",propLink);
        request.getSession().setAttribute("appLink",appLink);
        List<TemplatePurpose> remainingModules = remainingModules(request,em,moduleList);
        request.getSession().setAttribute("remainingMods",remainingModules);
    }

    private static void setRenewalData(HttpServletRequest request,EntityManager em){
        Renewal r = (Renewal) getCurrentActivity();
        request.getSession().setAttribute("adminView",2);
        request.getSession().setAttribute("currentRenewal", r);

        request.getSession().setAttribute("currentActivityEmployees", ActivityDAO.getEmployeeList(em,getCurrentActivity()));
        List<Employee> contactList = RenewalQueryDAO.getEmployeesAssignedToRenewal(em,r);
        request.getSession().setAttribute("contactList", TicketQueryDAO.getTicketEmployeeList(em,contactList));
        List<Employee> employeeList = RenewalQueryDAO.getContactsNotAssigned(em,r);
        request.getSession().setAttribute("remainingEmployees", TicketQueryDAO.getTicketEmployeeList(em,employeeList));
        List<Benefit> benefitsNotInRenewal = RenewalQueryDAO.getBenefitsNotInRenewal(em,r);
        request.getSession().setAttribute("benefitsNotInRenewal",benefitsNotInRenewal);
    }

    private static void updateSessionAttributes(HttpServletRequest request,EntityManager em){
        if(getPrimaryContact()!=null)
            request.getSession().setAttribute("currentPrimaryContact",primaryContact);

        if(getCurrentActivity()!=null && getCurrentActivity().getAssigneeContactList()!=null && getCurrentActivity().getAssigneeContactList().size()>0)
            request.getSession().setAttribute("otherContactList",getCurrentActivity().getAssigneeContactList());
        else
            request.getSession().setAttribute("otherContactList",new ArrayList<>());

        if(getCurrentActivity()!=null && getCurrentActivity().getWebLinkList()!=null && getCurrentActivity().getWebLinkList().size()>0)
            request.getSession().setAttribute("activityWebLinkList",getCurrentActivity().getWebLinkList());
        else
            request.getSession().setAttribute("activityWebLinkList",new ArrayList<>());

        if(getCurrentActivity()!=null) {
            request.getSession().setAttribute("currentActivityId", getCurrentActivity().getId());
            request.getSession().setAttribute("currentActivity", getCurrentActivity());
            CheckList c = getCheckListForActivity(em,getCurrentActivity());
            if(c!=null){
                request.getSession().setAttribute("currentChecklist", getCheckListForActivity(em,getCurrentActivity()));
                List<ToDo> toDoList = ChecklistDAO.getToDoListByChecklistId(em,c.getId());
                if(toDoList!=null && toDoList.size()>0)
                    request.getSession().setAttribute("currentToDoList",toDoList);
                else
                    request.getSession().setAttribute("currentToDoList",new ArrayList<>());
            } else
                request.getSession().setAttribute("currentChecklist", new CheckList());
        }

        if(getCurrentActivity()!=null && getPastActivities(request,em)!=null && getPastActivities(request,em).size()>0)
            request.getSession().setAttribute("pastActivities",getPastActivities(request,em));
        else
            request.getSession().setAttribute("pastActivities",new ArrayList<>());
    }

    private static List<Activity> getPastActivities(HttpServletRequest request,EntityManager em){
        List<Activity> pastActivities;
        Query q;
        if(getClassType().equals("Renewal")){
            Renewal r = (Renewal) getCurrentActivity();
            q = em.createQuery("SELECT r FROM Renewal r WHERE r.employer.id = :id order by r.id desc");
            q.setParameter("id",r.getEmployer().getId());
            try{
                pastActivities = (List<Activity>) q.getResultList();
            } catch (NoResultException e){
                pastActivities = null;
            }
        } else if(getClassType().equals("Ticket")) {
            Ticket t = (Ticket) getCurrentActivity();
            Long cId;
            if (t.getPrimaryContact() == null && t.getContact() == null)
                cId = null;
            else if (t.getPrimaryContact() != null)
                cId = t.getPrimaryContact().getId();
            else
                cId = t.getContact().getId();
            if (cId != null) {
                q = em.createQuery("SELECT t FROM Ticket t WHERE (t.primaryContact is not null AND t.primaryContact.id = :id) OR (t.contact.id is not null AND t.contact.id = :id) order by t.id desc");
                q.setParameter("id", cId);
                try {
                    pastActivities = (List<Activity>) q.getResultList();
                } catch (NoResultException e) {
                    pastActivities = null;
                }
            } else {
                pastActivities = null;
            }
        } else
            pastActivities = null;
        return pastActivities;
    }

    private static void setTheCurrentActivity(HttpServletRequest request, EntityManager em){
        Long activityId = Long.parseLong(request.getParameter("btnViewActivity"));
        Activity a = EntityLookup.getActivityById(em,activityId);
        setCurrentActivity(a);
        String className = a.getClass().getSimpleName();
        setClassType(className);
        if(a.getPrimaryContact()!=null)
            setPrimaryContact(a.getPrimaryContact());
    }

   public static void getAndSetFormParameters(HttpServletRequest request,EntityManager em){

   }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static void setCurrentActivity(Activity currentActivity) {
        ActivityViewHelper.currentActivity = currentActivity;
    }

    public static Person getPrimaryContact() {
        return primaryContact;
    }

    public static void setPrimaryContact(Person primaryContact) {
        ActivityViewHelper.primaryContact = primaryContact;
    }

    public static String getClassType() {
        return classType;
    }

    public static void setClassType(String classType) {
        ActivityViewHelper.classType = classType;
    }
}

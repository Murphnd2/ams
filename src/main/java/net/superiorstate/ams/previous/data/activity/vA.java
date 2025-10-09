package net.superiorstate.ams.previous.data.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedChecklist;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.data.misc.dbRenew;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.sales.application.ApplicationModule;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class vA {
    private static Activity currentActivity;
    private static Person primaryContact;
    private static String classType;



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
        Employee e = ViewSelectedActivity.getTicketEmployee(em,t);
        if(e!=null){
            request.getSession().setAttribute("tIsEmployee",1);
            request.getSession().setAttribute("currentActivityEmployees",dActivity.getEmployeeList(em,e.getEmployer()));
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
        String appLink = Q.ONLINE_APPLICATION_DATA + "&entry=" + s.getApplication().getProposal().getApplicationGUID();
        request.getSession().setAttribute("proposalLink",propLink);
        request.getSession().setAttribute("appLink",appLink);
        List<TemplatePurpose> remainingModules = ViewSelectedActivity.remainingModules(request,em,moduleList);
        request.getSession().setAttribute("remainingMods",remainingModules);
    }

    private static void setRenewalData(HttpServletRequest request,EntityManager em){
        Renewal r = (Renewal) getCurrentActivity();
        request.getSession().setAttribute("adminView",2);
        request.getSession().setAttribute("currentRenewal", r);

        request.getSession().setAttribute("currentActivityEmployees", dActivity.getEmployeeList(em,getCurrentActivity()));
        List<Employee> contactList = dbRenew.getEmployeesAssignedToRenewal(em,r);
        request.getSession().setAttribute("contactList", dbTicket.getTicketEmployeeList(em,contactList));
        List<Employee> employeeList = dbRenew.getContactsNotAssigned(em,r);
        request.getSession().setAttribute("remainingEmployees", dbTicket.getTicketEmployeeList(em,employeeList));
        List<Benefit> benefitsNotInRenewal = dbRenew.getBenefitsNotInRenewal(em,r);
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
            CheckList c = ViewSelectedActivity.getCheckListForActivity(em,getCurrentActivity());
            if(c!=null){
                request.getSession().setAttribute("currentChecklist", ViewSelectedActivity.getCheckListForActivity(em,getCurrentActivity()));
                List<ToDo> toDoList = ViewSelectedChecklist.getToDoListByChecklistId(em,c.getId());
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
        Activity a = dM.getActivityById(em,activityId);
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
        vA.currentActivity = currentActivity;
    }

    public static Person getPrimaryContact() {
        return primaryContact;
    }

    public static void setPrimaryContact(Person primaryContact) {
        vA.primaryContact = primaryContact;
    }

    public static String getClassType() {
        return classType;
    }

    public static void setClassType(String classType) {
        vA.classType = classType;
    }
}

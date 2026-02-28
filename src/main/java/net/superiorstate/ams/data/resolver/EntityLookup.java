package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ServiceModule;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;
import net.superiorstate.ams.model.summit.archive.PlanType;
import net.superiorstate.ams.model.summit.imports.order.ImportEmployer;
import net.superiorstate.ams.model.summit.imports.sEmployee;
import net.superiorstate.ams.model.summit.imports.sEmployer;
import org.jetbrains.annotations.NotNull;

public abstract class EntityLookup {

    // File: src/main/java/net/superiorstate/ams/previous/data/model/getByIds/dM.java | Lines 9-18
    public static Activity getActivityById(EntityManager em, Long id) {
        String jpql = """
            SELECT DISTINCT a FROM Activity a
            LEFT JOIN FETCH a.noteList
            LEFT JOIN FETCH a.primaryContact
            LEFT JOIN FETCH a.assigneeContactList
            WHERE a.id = :id
            """;

        Query q = em.createQuery(jpql, Activity.class);
        q.setParameter("id", id);
        System.out.println("*** getActivityById looking for id: " + id);
        try {
            Activity activity = (Activity) q.getSingleResult();

            // Force initialization of the few remaining lazy collections
            // (EclipseLink understands .size() perfectly and it's the standard way)
            if (activity instanceof CheckList cl && cl.getToDoList() != null) {
                cl.getToDoList().size();
            }
            if (activity instanceof Renewal) {
                Renewal r = (Renewal) activity;
                if (r.getRenewalItemList() != null) r.getRenewalItemList().size();
                if (r.getEmployer() != null && r.getEmployer().getContactList() != null) {
                    r.getEmployer().getContactList().size();
                }
            }
            if (activity instanceof Setup) {
                Setup s = (Setup) activity;
                if (s.getApplication() != null && s.getApplication().getApplicationModuleList() != null) {
                    s.getApplication().getApplicationModuleList().size();
                }
            }
            if (activity instanceof Ticket) {
                Ticket t = (Ticket) activity;
                if (t.getContact() != null) {
                    // just touch it
                    t.getContact().getEmail();
                }
            }

            return activity;
        } catch (NoResultException e) {
            System.out.println("*** getActivityById NO RESULT for id: " + id);
            return null;
        } catch (Exception e) {
            System.out.println("*** getActivityById EXCEPTION for id: " + id);
            e.printStackTrace();
            return null;
        }
    }

    public static ActivityStatus getActivityStatusById(EntityManager em, int id){
        Query q= em.createQuery("SELECT s FROM ActivityStatus s WHERE s.id = :id");
        q.setParameter("id",id);
        ActivityStatus as;
        try{
            as = (ActivityStatus) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return as;
    }
    public static Address getAddressById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT a FROM Address a WHERE a.id = :id");
        q.setParameter("id",id);
        Address a;
        try{
            a = (Address) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return a;
    }

    public static Agency getAgencyById(EntityManager em, long id){
        Query q = em.createQuery("SELECT a FROM Agency a WHERE a.id = :id");
        q.setParameter("id",id);
        Agency a;
        try{
            a = (Agency) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return a;
    }

    public static Application getApplicationById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT a FROM Application a WHERE a.proposal.id = :id");
        q.setParameter("id",id);
        Application a;
        try{
            a = (Application) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return a;
    }

    public static Automation getAutomationById(EntityManager em, int id){
        Query q = em.createQuery("SELECT a FROM Automation a WHERE a.id = :id");
        q.setParameter("id",id);
        Automation a;
        try{
            a = (Automation) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return a;
    }
    public static Benefit getBenefitById(EntityManager em, int id){
        Benefit benefit;
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.id = :id");
        q.setParameter("id",id);
        try{
            benefit = (Benefit) q.getSingleResult();
        } catch (NoResultException e){
            benefit = new Benefit();
        }
        return benefit;
    }
    public static Benefit getBenefitById(EntityManager em, int id, boolean returnNull){
        Benefit benefit;
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.id = :id");
        q.setParameter("id",id);
        try{
            benefit = (Benefit) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return benefit;
    }

    /** Lookup Benefit by source-discriminated Summit key (source_type + summit_id). */
    public static Benefit getBenefitBySummitKey(EntityManager em, String sourceType, int summitId) {
        try {
            return em.createQuery(
                    "SELECT b FROM Benefit b WHERE b.sourceType = :src AND b.summitId = :sid", Benefit.class)
                    .setParameter("src", sourceType)
                    .setParameter("sid", summitId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public static BillingGroup getBillingGroupById(EntityManager em, int id){
        BillingGroup billingGroup;
        Query q = em.createQuery("SELECT bg FROM BillingGroup bg WHERE bg.id = :id");
        q.setParameter("id",id);
        try{
            billingGroup = (BillingGroup) q.getSingleResult();
        } catch (NoResultException e){
            billingGroup = new BillingGroup();
        }
        return billingGroup;
    }
    public static BillingGroup getBillingGroupById(EntityManager em, int id,boolean returnNull){
        BillingGroup billingGroup;
        Query q = em.createQuery("SELECT bg FROM BillingGroup bg WHERE bg.id = :id");
        q.setParameter("id",id);
        try{
            billingGroup = (BillingGroup) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return billingGroup;
    }
    public static CheckList getCheckListById(EntityManager em, Long id) {
        String jpql = """
        SELECT c FROM CheckList c
        LEFT JOIN FETCH c.noteList
        LEFT JOIN FETCH c.toDoList
        WHERE c.id = :id
        """;
        Query q = em.createQuery(jpql);
        q.setParameter("id", id);
        try {
            return (CheckList) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    public static CheckList getCheckListByAssignee(EntityManager em, Assignee a){
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :id");
        q.setParameter("id",a.getId());
        CheckList checkList;
        try{
            checkList = (CheckList) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return checkList;
    }
    public static ContactMethod getMethodById(EntityManager em, int id){
        Query q = em.createQuery("SELECT c FROM ContactMethod c WHERE c.id = :id");
        q.setParameter("id",id);
        ContactMethod cm;
        try{
            cm = (ContactMethod) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return cm;
    }

    public static Email getEmailById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT e FROM Email e WHERE e.id = :id");
        q.setParameter("id",id);
        Email email;
        try{
            email = (Email) q.getSingleResult();
        } catch (NoResultException e){
            email = null;
        }
        return email;
    }


    public static Employee getEmployeeById(EntityManager em, int id){
        Employee employee;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.id = :id");
        q.setParameter("id",id);
        try{
            employee = (Employee) q.getSingleResult();
        } catch (NoResultException e){
            employee = new Employee();
            employee.setLastName("NULL");
        }
        return employee;
    }
    public static Employee getEmployeeById(EntityManager em, int id,boolean returnNull){
        Employee employee;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.id = :id");
        q.setParameter("id",id);
        try{
            employee = (Employee) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return employee;
    }


    public static Employer getEmployerById(EntityManager em, int id){
        Employer employer;
        Query q = em.createQuery("SELECT e FROM Employer e WHERE e.id = :id");
        q.setParameter("id",id);
        try{
            employer = (Employer) q.getSingleResult();
        } catch (NoResultException e){
            employer = new Employer();
        }
        return employer;
    }
    public static Employer getEmployerById(EntityManager em, int id,boolean returnNull){
        Employer employer;
        Query q = em.createQuery("SELECT e FROM Employer e WHERE e.id = :id");
        q.setParameter("id",id);
        try{
            employer = (Employer) q.getSingleResult();
        } catch (NoResultException e){
            return  null;
        }
        return employer;
    }
    public static ImportEmployer getImportEmployerById(EntityManager em, int id){
        Query q = em.createQuery("SELECT ie FROM ImportEmployer ie WHERE ie.organizationId = :id");
        q.setParameter("id",id);
        ImportEmployer importEmployer;
        try{
            importEmployer = (ImportEmployer) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return importEmployer;
    }
    public static LinkType getLinkTypeById(EntityManager em, int id){
        LinkType linkType;
        Query q = em.createQuery("SELECT l FROM LinkType l WHERE l.id = :id");
        q.setParameter("id",id);
        try{
            linkType = (LinkType) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return linkType;
    }

    public static LOS getLosById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT l FROM LOS l WHERE l.id = :id");
        q.setParameter("id",id);
        LOS l;
        try{
            l = (LOS) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return l;
    }
    public static Opportunity getOpportunityById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT o FROM Opportunity o WHERE o.id = :id");
        q.setParameter("id",id);
        Opportunity o;
        try{
            o = (Opportunity) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return o;
    }

    public static Person getPersonById(EntityManager em, int id){
        Long lId = intToLong(id);
        return getPersonById(em,lId);
    }

    public static Person getPersonById(EntityManager em, long id){
        Long lId = Long.valueOf(id);
        return getPersonById(em,lId);
    }

    public static Person getPersonById(EntityManager em, Long id){
        Person person;
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.id = :id");
        q.setParameter("id",id);
        try{
            person = (Person) q.getSingleResult();
        } catch (NoResultException e){
            person = new Person();
        }
        return person;
    }
    public static Person getPersonById(EntityManager em, Long id,boolean returnNull){
        Person person;
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.id = :id");
        q.setParameter("id",id);
        try{
            person = (Person) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return person;
    }
    public static PlanType getPlanTypeById(EntityManager em, int id){
        Query q = em.createQuery("SELECT pt FROM PlanType pt WHERE pt.planTypeId = :id");
        q.setParameter("id",id);
        PlanType planType;
        try{
            planType = (PlanType) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return planType;
    }
    public static PriceItem getPriceItemById(EntityManager em, long id){
        Query q = em.createQuery("SELECT pi FROM PriceItem pi WHERE pi.id = :id");
        q.setParameter("id",id);
        PriceItem p;
        try{
            p = (PriceItem) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return p;
    }
    public static Proposal getProposalById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT p FROM Proposal p WHERE p.id = :id");
        q.setParameter("id",id);
        return (Proposal) q.getSingleResult();
    }
    public static Prospect getProspectById(EntityManager em, long id){
        Query q = em.createQuery("SELECT p FROM Prospect p INNER JOIN FETCH p.contact a where p.id = :prospect_id");
        q.setParameter("prospect_id",id);
        Prospect p;
        try{
            p = (Prospect) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return p;
    }
    public static PSP getPspById(EntityManager em, long id){
        Query q = em.createQuery("SELECT p FROM PSP p WHERE p.id = :id");
        q.setParameter("id",id);
        PSP p;
        try{
            p = (PSP) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return p;
    }
    public static Rate getRateById(EntityManager em, long id){
        Query q = em.createQuery("SELECT r FROM Rate r WHERE r.id = :id");
        q.setParameter("id",id);
        Rate r;
        try{
            r = (Rate) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            r = new Rate();
        }
        return r;
    }
    public static Rate getRateById(EntityManager em, long id,boolean returnNull){
        Query q = em.createQuery("SELECT r FROM Rate r WHERE r.id = :id");
        q.setParameter("id",id);
        Rate r;
        try{
            r = (Rate) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return r;
    }
    public static ReasonCreated getReasonById(EntityManager em, int id){
        Query q= em.createQuery("SELECT r FROM ReasonCreated r WHERE r.id = :id");
        q.setParameter("id",id);
        ReasonCreated r;
        try{
            r = (ReasonCreated) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return r;
    }
    public static Renewal getRenewalById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.id = :id");
        q.setParameter("id",id);
        Renewal r;
        try{
            r = (Renewal) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return r;
    }
    public static RenewalItem getRenewalItemById(EntityManager em, Long id){
        RenewalItem renewalItem;
        try{
            Query q = em.createQuery("SELECT r FROM RenewalItem r WHERE r.id = :id");
            q.setParameter("id",id);
            renewalItem = (RenewalItem) q.getSingleResult();
        } catch (NoResultException e){
            renewalItem = new RenewalItem();
        }
        return renewalItem;
    }
    public static RenewalItem getRenewalItemById(EntityManager em, Long id,boolean returnNull){
        RenewalItem renewalItem;
        try{
            Query q = em.createQuery("SELECT r FROM RenewalItem r WHERE r.id = :id");
            q.setParameter("id",id);
            renewalItem = (RenewalItem) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return renewalItem;
    }

    public static ServiceModule getServiceModuleById(@NotNull EntityManager em, int id){
        Query q = em.createQuery("SELECT sm FROM ServiceModule sm WHERE sm.id = :id");
        q.setParameter("id",id);
        ServiceModule sm;
        try{
            sm = (ServiceModule) q.getSingleResult();
        }catch (NoResultException e){
            sm = new ServiceModule();
        }
        return sm;
    }
    public static ServiceModule getServiceModuleById(@NotNull EntityManager em, int id,boolean returnNull){
        Query q = em.createQuery("SELECT sm FROM ServiceModule sm WHERE sm.id = :id");
        q.setParameter("id",id);
        ServiceModule sm;
        try{
            sm = (ServiceModule) q.getSingleResult();
        }catch (NoResultException e){
            return null;
        }
        return sm;
    }

    public static Setup getSetupById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT s FROM Setup s WHERE s.id = :id");
        q.setParameter("id",id);
        Setup s;
        try{
            s = (Setup) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return s;
    }

    public static sEmployer getSummitEmployerById(EntityManager em, int id){
        sEmployer sEmployer;
        Query q = em.createQuery("SELECT se FROM sEmployer se WHERE se.organizationId = :id");
        q.setParameter("id",id);
        try{
            sEmployer = (sEmployer) q.getSingleResult();
        } catch (NoResultException e){
            sEmployer = new sEmployer();
        }
        return sEmployer;
    }
    public static sEmployer getSummitEmployerById(EntityManager em, int id,boolean returnNull){
        sEmployer sEmployer;
        Query q = em.createQuery("SELECT se FROM sEmployer se WHERE se.organizationId = :id");
        q.setParameter("id",id);
        try{
            sEmployer = (sEmployer) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return sEmployer;
    }
    public static sEmployee getSummitEmployeeById(EntityManager em, int id){
        Query q = em.createQuery("SELECT s FROM sEmployee s where s.id = :id");
        q.setParameter("id",id);
        sEmployee s;
        try{
            s = (sEmployee) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return s;
    }

    public static Task getTaskById(EntityManager em, Long id){
        Task task = null;
        try{
            Query q = em.createQuery("SELECT t FROM Task t WHERE t.id = :id");
            q.setParameter("id",id);
            task = (Task) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return task;
    }

    public static Task getTaskById(EntityManager em, ToDo toDo){
        Query q = em.createQuery("SELECT t FROM Task t WHERE t.id = :id");
        q.setParameter("id",toDo.getTask().getId());
        Task t;
        try {
            t = (Task) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return t;
    }

    public static ActivityCategory getTemplateGroupById(EntityManager em, int id){
        Query q = em.createQuery("SELECT tg FROM ActivityCategory tg WHERE tg.id = :id");
        q.setParameter("id",id);
        ActivityCategory t;
        try{
            t = (ActivityCategory) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return t;
    }
    public static ServiceItem getServiceItemById(EntityManager em, int id){
        ServiceItem serviceItem = null;
        try{
            Query q = em.createQuery("SELECT tp FROM ServiceItem tp WHERE tp.id = :id");
            q.setParameter("id",id);
            serviceItem = (ServiceItem) q.getSingleResult();
        } catch (NoResultException e){
            serviceItem = new ServiceItem();
        } finally {
            return serviceItem;
        }
    }
    public static ServiceItem getServiceItemById(EntityManager em, int id, boolean returnNull){
        Query q = em.createQuery("SELECT tp FROM ServiceItem tp WHERE tp.id = :id");
        q.setParameter("id",id);
        ServiceItem t;
        try{
            t = (ServiceItem) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return t;
    }
    public static Ticket getTicketById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT t FROM Ticket t WHERE t.id = :id");
        q.setParameter("id",id);
        Ticket t;
        try{
            t = (Ticket) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return t;
    }
    public static TicketCategory getTicketCategoryById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT tc FROM TicketCategory tc WHERE tc.id = :id");
        q.setParameter("id",id);
        TicketCategory t;
        try{
            t=(TicketCategory) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return t;
    }

    public static ToDo getToDoById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.id = :id");
        q.setParameter("id",id);
        ToDo toDo;
        try{
            toDo = (ToDo) q.getSingleResult();
        }catch (NoResultException e){
            return null;
        }
        return toDo;
    }
    public static User getUserById(EntityManager em, String email){
        Query q = em.createQuery("SELECT u FROM User u WHERE u.userName = :email");
        q.setParameter("email",email);
        User u;
        try{
            u = (User) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return u;
    }


    public static UserRole getUserRoleById(EntityManager em, int id){
        Query q = em.createQuery("SELECT u FROM UserRole u WHERE u.id = :id");
        q.setParameter("id",id);
        UserRole r;
        try{
            r = (UserRole) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return r;
    }
    private static Long intToLong(int theInteger){
        String stringFromInt = String.valueOf(theInteger);
        return Long.parseLong(stringFromInt);
    }

    public static TaskFrequency getTaskFrequencyById(EntityManager em, int id){
        TaskFrequency tf;
        Query q = em.createQuery("SELECT t FROM TaskFrequency t WHERE t.id = :id");
        q.setParameter("id",id);
        try{
            tf = (TaskFrequency) q.getSingleResult();
        } catch (NoResultException e){
            tf = new TaskFrequency();
        }
        return tf;
    }
    public static TaskFrequency getTaskFrequencyById(EntityManager em, int id,boolean returnNull){
        TaskFrequency tf;
        Query q = em.createQuery("SELECT t FROM TaskFrequency t WHERE t.id = :id");
        q.setParameter("id",id);
        try{
            tf = (TaskFrequency) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return tf;
    }


    public static RequiredTaskList getReqListById(EntityManager em, Long id){
        RequiredTaskList requiredTaskList;
        Query q = em.createQuery("SELECT r FROM RequiredTaskList r WHERE r.id = :id");
        q.setParameter("id",id);
        try{
            requiredTaskList = (RequiredTaskList) q.getSingleResult();
        } catch (NoResultException e){
            requiredTaskList = new RequiredTaskList();
        }
        return requiredTaskList;
    }
    public static RequiredTaskList getReqListById(EntityManager em, Long id,boolean returnNull){
        RequiredTaskList requiredTaskList;
        Query q = em.createQuery("SELECT r FROM RequiredTaskList r WHERE r.id = :id");
        q.setParameter("id",id);
        try{
            requiredTaskList = (RequiredTaskList) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return requiredTaskList;
    }

}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.List;

public abstract class ActivityDAO {

    public static boolean moduleExists(EntityManager em, Application a, ServiceItem tp){
        Query q = em.createQuery("SELECT am FROM ApplicationModule am WHERE am.application.proposal.id = :aId AND am.serviceItem.id = :tpId");
        q.setParameter("aId",a.getProposal().getId());
        q.setParameter("tpId",tp.getId());
        ApplicationModule applicationModule;
        try{
            applicationModule = (ApplicationModule) q.getSingleResult();
        }catch (NoResultException e){
            e.printStackTrace();
            System.out.println("Application Module for TP ID: " + tp.getId()+ " not found in application.");
            return false;
        }

        System.out.println("Application Module for TP ID: " + tp.getId()+ " ALREADY in application.");
        return true;
    }
    public static void addModule(EntityManager em, Application a, ServiceItem tp){
        if(moduleExists(em,a,tp))
            return;

        em.getTransaction().begin();
        ApplicationModule am = new ApplicationModule();
        am.setServiceItem(tp);
        am.setApplication(a);
        em.persist(am);
        em.getTransaction().commit();
        System.out.println("Application Module Created: TP ID="+ tp.getId() + " and App="+a.getProposal().getId());

        em.getTransaction().begin();
        Application application = EntityLookup.getApplicationById(em,a.getProposal().getId());
        application.getApplicationModuleList().add(am);
        em.persist(application);
        em.getTransaction().commit();

        em.getTransaction().begin();
        tp.getApplicationModuleList().add(am);
        em.persist(tp);
        em.getTransaction().commit();
    }


    public static void setPrimaryContact(EntityManager em, Activity a){
        if(a.getPrimaryContact().equals(getPrimaryContact(em,a)))
            return;
        Person contact = getPrimaryContact(em,a);
        if(contact==null)
            return;
        Activity activity = EntityLookup.getActivityById(em,a.getId());
        em.getTransaction().begin();
        assert activity != null;
        activity.setPrimaryContact(contact);
        em.persist(activity);
        em.getTransaction().commit();
    }
    public static Person getPrimaryContact(EntityManager em, Activity a){
        if(a.getPrimaryContact()!=null)
            return a.getPrimaryContact();
        String activityType = a.getClass().getSimpleName();
        switch(activityType){
            case "Renewal":
                Renewal pr = getPreviousRenewal(em, a);
                if(pr!=null && pr.getPrimaryContact()!=null)
                    return pr.getPrimaryContact();
                List<Employee> contactList = getEmployerContactList(em, a);
                if(contactList!=null && contactList.size()>0)
                    return getEmployeePerson(em,contactList.get(0));
                List<Employee> employeeList = getEmployeeList(em,a);
                if(employeeList!=null && employeeList.size()>0)
                    return getEmployeePerson(em,employeeList.get(0));
                return null;
            case "Setup":
                Setup cs = (Setup) a;
                if(cs.getPrimaryContact()!=null)
                    return cs.getPrimaryContact();
                if(cs.getPrimaryContactSetup()!=null)
                    return cs.getPrimaryContactSetup();
                if(cs.getContactList()!=null && cs.getContactList().size()>0)
                    return cs.getContactList().get(0);
                Prospect p = cs.getApplication().getProposal().getProspect();
                if(p.getContact()!=null)
                    return p.getContact();
                if(p.getAgent()!=null)
                    return p.getAgent();
                return null;
            case "Ticket":
                Ticket t = (Ticket) a;
                if(t.getContact()!=null)
                    return t.getContact();
                return null;
            default:
                return null;
        }
    }

    public static List<Employee> getEmployeeList(EntityManager em, Employer er){
        Query q = em.createQuery("SELECT ee FROM Employee ee WHERE ee.employer.id = :id order by ee.lastName,ee.firstName");
        q.setParameter("id",er.getId());
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(employeeList.size()==0)
            return null;
        return employeeList;
    }

    public static List<Employee> getEmployeeList(EntityManager em, Activity a){
        Renewal r = (Renewal) a;
        return getEmployeeList(em,r.getEmployer());
    }
    public static Person getEmployeePerson(EntityManager em, Employee ee){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.employee.id = :id order by p.id desc");
        q.setParameter("id",ee.getId());
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            personList = null;
        }
        if(personList==null || personList.size()==0)
            return createPersonForThisEmployee(em,ee);
        Person p = personList.get(0);
        if(ee.getHrEmail()!=null  && EmailDAO.isValidEmail(ee.getHrEmail()) && !p.getEmail().equalsIgnoreCase(ee.getHrEmail())){
            Person person = EntityLookup.getPersonById(em,p.getId());
            em.getTransaction().begin();
            person.setEmail(ee.getHrEmail());
            em.persist(person);
            em.getTransaction().commit();
            return person;
        }
        return p;
    }

    private static Person createPersonForThisEmployee(EntityManager em, Employee ee){
        em.getTransaction().begin();
        Address address = new Address();
        if(ee.getState()!=null)
            address.setState(ee.getState().substring(0,2));
        if(ee.getCity()!=null)
            address.setCity(ee.getCity());
        if(ee.getAddress2()!=null)
            address.setAddress2(ee.getAddress2());
        if(ee.getAddress1()!=null)
            address.setAddress1(ee.getAddress1());
        if(ee.getZipCode()!=null)
            address.setZipCode(ee.getZipCode());
        em.persist(address);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Person p = new Person();
        p.setEmployee(ee);
        p.setPsp(EntityLookup.getPspById(em,4));
        p.setAddress(address);
        if(ee.getHrEmail()!=null && EmailDAO.isValidEmail(ee.getHrEmail()))
            p.setEmail(ee.getHrEmail().toLowerCase());
        else if(ee.getEmail()!=null && EmailDAO.isValidEmail(ee.getEmail()))
            p.setEmail(ee.getEmail().toLowerCase());
        p.setFirstName(ee.getFirstName().trim().toUpperCase());
        p.setLastName(ee.getLastName().trim().toUpperCase());
        p.setFullName(ee.getFirstName().trim().toUpperCase() + " " + ee.getLastName().trim().toUpperCase());
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }
    private static List<Employee> getEmployerContactList(EntityManager em, Activity a){
        Renewal r = (Renewal) a;
        Employer er = r.getEmployer();
        if(er.getContactList()==null || er.getContactList().size()==0)
            return null;
        return er.getContactList();
    }

    private static Renewal getPreviousRenewal(EntityManager em, Activity a){
        Renewal r = (Renewal) a;
        Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.employer.id = :id order by r.id desc ");
        q.setParameter("id", r.getEmployer().getId());
        List<Renewal> renewalList;
        try{
            renewalList = (List<Renewal>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(renewalList.size()==0)
            return null;
        return renewalList.get(0);
    }


}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.ArrayList;
import java.util.List;

public abstract class PersonDAO {

    public static Person getPersonByEmployee(EntityManager em, Employee e) {
        try {
            List<Person> results = em.createQuery(
                            "SELECT p FROM Person p WHERE p.employee.id = :id", Person.class)
                    .setParameter("id", e.getId())
                    .setMaxResults(1)
                    .getResultList();

            if (results.isEmpty()) {
                Person p = new Person();
                p.setEmail(null);
                return p;
            }
            return results.get(0);
        } catch (Exception ex) {
            System.err.println("❌ getPersonByEmployee: " + ex.getMessage());
            return null;
        }
    }

    public static Person getPersonByEmployee1(EntityManager em, Employee e) {
        try {
            List<Person> results = em.createQuery(
                            "SELECT p FROM Person p WHERE p.employee.id = :id", Person.class)
                    .setParameter("id", e.getId())
                    .setMaxResults(1)
                    .getResultList();

            return results.isEmpty() ? null : results.get(0);
        } catch (Exception ex) {
            System.err.println("❌ getPersonByEmployee1: " + ex.getMessage());
            return null;
        }
    }

    public static Person getPersonByEe(EntityManager em, Employee e, PSP psp) {
        try {
            List<Person> results = em.createQuery(
                            "SELECT p FROM Person p WHERE p.employee.id = :id", Person.class)
                    .setParameter("id", e.getId())
                    .setMaxResults(1)
                    .getResultList();

            return results.isEmpty() ? AuthDAO.createPersonFromEmployee(em, e, psp) : results.get(0);
        } catch (Exception ex) {
            System.err.println("❌ getPersonByEe: " + ex.getMessage());
            return AuthDAO.createPersonFromEmployee(em, e, psp);
        }
    }

    public static Person getPersonByEmail(EntityManager em, String email) {
        try {
            Employee ee = getEmployeeByEmail(em, email);
            if (ee != null) {
                return getPersonByEe(em, ee, EntityLookup.getPspById(em, 4L));
            }

            List<Person> personList = em.createQuery(
                            "SELECT p FROM Person p WHERE p.email = :email", Person.class)
                    .setParameter("email", email)
                    .setMaxResults(1)
                    .getResultList();

            return personList.isEmpty() ? null : personList.get(0);
        } catch (Exception ex) {
            System.err.println("❌ getPersonByEmail: " + ex.getMessage());
            return null;
        }
    }

    public static Employee getEmployeeByEmail(EntityManager em, String email) {
        try {
            List<Employee> employeeList = em.createQuery(
                            "SELECT e FROM Employee e WHERE e.email = :email", Employee.class)
                    .setParameter("email", email)
                    .setMaxResults(1)
                    .getResultList();

            return employeeList.isEmpty() ? null : employeeList.get(0);
        } catch (Exception ex) {
            System.err.println("❌ getEmployeeByEmail: " + ex.getMessage());
            return null;
        }
    }


    public static void refreshPersonDataFromEmployeeData(EntityManager em, PSP psp){
        Query q = em.createQuery("SELECT e FROM Employee e");
        List<Employee> employeeList;
        try {
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            return;
        }
        for(Employee e: employeeList){
            Query query = em.createQuery("SELECT p FROM Person p WHERE p.employee.id = :id");
            query.setParameter("id",e.getId());
            Person p;
            try{
                p = (Person) query.getSingleResult();
                System.out.println("Ee:"+e.getId()+"-P:"+p.getId());
            } catch (Exception e1){
                e1.printStackTrace();
                p = null;
            }
            //try to find by email next
            if(p==null){
             List<Person> maybes;
             Query qMaybes = em.createQuery("SELECT p FROM Person p WHERE p.email = :email");
             qMaybes.setParameter("email",e.getEmail());
             try{
                 maybes = (List<Person>) qMaybes.getResultList();
             } catch (NoResultException e2){
                 e2.printStackTrace();
                 maybes = null;
             }
             if(maybes!=null){
                 for(Person pMaybe: maybes){
                     if(pMaybe.getEmail().equals(e.getEmail()) && pMaybe.getEmployee()==null){
                         p=pMaybe;
                         System.out.println("Ee:"+e.getId()+"-M:"+p.getId());
                         break;
                     }
                 }
             }
            }
            Long pid;
            if(p!=null){
                pid = p.getId();
                em.getTransaction().begin();
                p = EntityLookup.getPersonById(em,pid);
                if(!p.getEmail().equals(e.getEmail()) && e.getEmail()!=null && !e.getEmail().equals(""))
                    p.setEmail(e.getEmail());
                if(!p.getFirstName().equals(e.getFirstName()) && e.getFirstName()!=null && !e.getFirstName().equals(""))
                    p.setFirstName(e.getFirstName());
                if(!p.getLastName().equals(e.getLastName()) && e.getLastName()!=null && !e.getLastName().equals(""))
                    p.setLastName(e.getLastName());
                p.setEmployee(e);
                em.persist(p);
                em.getTransaction().commit();
                Address a = p.getAddress();
                em.getTransaction().begin();
                Address address = EntityLookup.getAddressById(em,a.getId());
                if(address!=null){
                    if(!address.getAddress1().equals(e.getAddress1()) && e.getAddress1()!=null && !e.getAddress1().equals(""))
                        address.setAddress1(e.getAddress1());
                    if(!address.getAddress2().equals(e.getAddress2()) && e.getAddress2()!=null && !e.getAddress2().equals(""))
                        address.setAddress2(e.getAddress2());
                    if(!address.getCity().equals(e.getCity()) && e.getCity()!=null && !e.getCity().equals(""))
                        address.setCity(e.getCity());
                    if(!address.getState().equals(e.getState()))
                        address.setAddress1(e.getAddress1().substring(0,2));
                    if(!address.getZipCode().equals(e.getZipCode()) && e.getZipCode()!=null && !e.getZipCode().equals(""))
                        address.setZipCode(e.getZipCode());
                    em.persist(address);
                }
                em.getTransaction().commit();
            } else {
                AuthDAO.createPersonFromEmployee(em,e,psp);
            }
        }
    }

    public static Employee getEmployeeByPerson(EntityManager em, Person p) {
        if (p == null || p.getEmployee() == null || p.getEmployee().getId() < 0) {
            return null;
        }

        try {
            List<Employee> results = em.createQuery(
                            "SELECT e FROM Employee e WHERE e.id = :id", Employee.class)
                    .setParameter("id", p.getEmployee().getId())
                    .setMaxResults(1)
                    .getResultList();

            return results.isEmpty() ? null : results.get(0);
        } catch (Exception ex) {
            System.err.println("❌ getEmployeeByPerson: " + ex.getMessage());
            return null;
        }
    }



    public static void processDuplicateEmails(EntityManager em){
        List<String> duplicateEmailList = getDuplicateEmailList(em);
        List<Person> personsWithSameEmail;
        Query q;
        for(String email:duplicateEmailList){
            q = em.createQuery("SELECT p FROM Person p WHERE p.email = :email");
            q.setParameter("email",email);
            try{
                personsWithSameEmail = (List<Person>) q.getResultList();
            } catch (NoResultException e) {
                continue;
            }
            if(personsWithSameEmail.size()>1)
                processThisPersonList(em,personsWithSameEmail);
        }
    }

    private static void processThisPersonList(EntityManager em, List<Person> personList){
        Person aPerson;
        Person bPerson;
        int fileSize = personList.size();
        for(int i = 1; i < fileSize; i++){
            aPerson = personList.get(i-1);
            bPerson = personList.get(i);
            int atLoc = aPerson.getEmail().indexOf("@");
            String domain = aPerson.getEmail().substring(atLoc+1).trim().toLowerCase();
            if(isEe(aPerson) && !isEe(bPerson)){
                mergePeople(em,aPerson,bPerson);
            } else if (isEe(bPerson) && !isEe(aPerson)){
                mergePeople(em,bPerson,aPerson);
            } else if (!isEe(aPerson) && !isEe(bPerson)){
                bestOfBothWorlds(em,aPerson,bPerson);
                mergePeople(em,aPerson,bPerson);
            } else if(aPerson.getEmployee().getEmployer()==bPerson.getEmployee().getEmployer() && !ignoreList().contains(domain)){
                mergePeople(em,aPerson,bPerson);
            }
        }
    }

    private static List<String> ignoreList(){
        List<String> theList = new ArrayList<>();
        theList.add("mail.com");
        theList.add("email.com");
        theList.add("superiorstate.net");
        theList.add("wgu.edu");
        return theList;
    }

    private static void bestOfBothWorlds(EntityManager em, Person a, Person b){
        //First Name
        String firstName;
        if(a.getFirstName()==null&&b.getFirstName()!=null)
            firstName=b.getFirstName().trim().toUpperCase();
        else if(a.getFirstName()!=null&&b.getFirstName()==null)
            firstName=a.getFirstName().trim().toUpperCase();
        else if(a.getFirstName()==null&&b.getFirstName()==null) {
            int atLoc = a.getEmail().indexOf("@");
            firstName = a.getEmail().substring(0,atLoc).toUpperCase();
        } else if(a.getFirstName().trim().equalsIgnoreCase("EMPLOYER"))
            firstName = b.getFirstName().trim().toUpperCase();
        else
            firstName = a.getFirstName().trim().toUpperCase();
        //Last Name
        String lastName;
        if(a.getLastName()==null&&b.getLastName()!=null)
            lastName=b.getLastName().trim().toUpperCase();
        else if(a.getLastName()!=null&&b.getLastName()==null)
            lastName=a.getLastName().trim().toUpperCase();
        else if(a.getLastName()==null&&b.getLastName()==null){
            int atLoc = a.getEmail().indexOf("@");
            int dotLoc = a.getEmail().indexOf(".",atLoc);
            lastName=a.getEmail().substring(atLoc,dotLoc).trim().toUpperCase();
        } else if(a.getLastName().trim().equalsIgnoreCase("CONTACT"))
            lastName = b.getLastName().trim().toUpperCase();
        else
            lastName = a.getLastName().trim().toUpperCase();
        //Phone
        String phone = null;
        if(a.getPhone()==null && b.getPhone()!=null)
            phone = b.getPhone().trim();
        else if(a.getPhone()!=null && b.getPhone()!=null){
            if(a.getPhone().trim().length()>8)
                phone = a.getPhone().trim();
            else if(b.getPhone().trim().length()>8)
                phone = b.getPhone().trim();
        }
        else if(a.getPhone()!=null)
            phone = a.getPhone().trim();


        Person master = EntityLookup.getPersonById(em,a.getId());
        em.getTransaction().begin();
        master.setFirstName(firstName);
        master.setLastName(lastName);
        master.setPhone(phone);
        master.setFullName(firstName + " " + lastName);
        em.persist(master);
        em.getTransaction().commit();
    }

    private static void mergePeople(EntityManager em, Person keep, Person replace){
        updateTickets(em,keep,replace);
        updateSetupContacts(em,keep,replace);
        updateEmailReference(em,keep,replace);
        updateProspects(em,keep,replace);
        //if(!isEe(keep,true))
            //assignToGenericEmployer(em,keep);
    }

    private static void assignToGenericEmployer(EntityManager em, Person person){
        Employer er = EntityLookup.getEmployerById(em,0);
        Person p = EntityLookup.getPersonById(em,person.getId());
        em.getTransaction().begin();
        Employee ee = new Employee();
        ee.setEmail(p.getEmail().trim().toLowerCase());
        ee.setEmployer(er);
        ee.setFirstName(p.getFirstName().trim().toUpperCase());
        ee.setLastName(p.getLastName().trim().toUpperCase());
        ee.setActive(true);
        em.persist(ee);
        em.getTransaction().commit();
        em.getTransaction().begin();
        p.setEmployee(ee);
        em.persist(p);
        em.getTransaction().commit();
    }

    private static void updateEmailReference(EntityManager em, Person keep, Person replace){
        List<Email> emails = replace.getEmailList();
        for(Email email: emails){

            Person r = EntityLookup.getPersonById(em,replace.getId());
            Person k = EntityLookup.getPersonById(em,keep.getId());
            em.getTransaction().begin();
            email.addRecipient(k);
            email.removeRecipient(r);
            em.persist(email);
            em.getTransaction().commit();
        }
    }

    private static void updateSetupContacts(EntityManager em, Person keep, Person replace){
        List<Setup> setupList = replace.getSetupList();
        for(Setup setup: setupList){
            Query q = em.createQuery("SELECT s FROM Setup s WHERE s.id = :id");
            q.setParameter("id",setup.getId());
            Setup s;
            try{
                s = (Setup) q.getSingleResult();
            } catch (NoResultException e){
                continue;
            }
            Person r = EntityLookup.getPersonById(em,replace.getId());
            Person k = EntityLookup.getPersonById(em,keep.getId());
            em.getTransaction().begin();
            s.addContact(k);
            s.removeContact(r);
            em.persist(s);
            em.getTransaction().commit();
        }
    }

    private static void updateProspects(EntityManager em, Person keep, Person replace){
        Query q = em.createQuery("SELECT p FROM Prospect p WHERE p.contact = :id");
        q.setParameter("id",replace);
        List<Prospect> prospects;
        try{
            prospects = (List<Prospect>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(prospects.size()==0)
            return;
        for(Prospect p : prospects){
            em.getTransaction().begin();
            p.setContact(keep);
            em.persist(p);
            em.getTransaction().commit();
        }
    }
    private static void updateTickets(EntityManager em, Person keep, Person replace){
        Query q = em.createQuery("SELECT t FROM Ticket t WHERE t.assignedTo = :assignee");
        q.setParameter("assignee",replace);
        List<Ticket> ticketList;
        try{
            ticketList = (List<Ticket>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(ticketList.size()==0)
            return;
        for(Ticket t: ticketList){
            em.getTransaction().begin();
            t.setAssignedTo(keep);
            em.persist(t);
            em.getTransaction().commit();
        }
    }

    private static boolean isEe(Person p){
        try{
            if(p.getEmployee().getEmployer().getId()==0)
                return false;
            return true;
        } catch (Exception e){
            return false;
        }
    }
    private static boolean isEe(Person p, boolean skipZero){
        try{
            int i = p.getEmployee().getId();
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public static List<String> getDuplicateEmailList(EntityManager em){
        List<String> emailList = new ArrayList<>();
        Query q = em.createQuery("SELECT p FROM Person p WHERE (p.email is not null AND p.email <> :tString)");
        String tString = "";
        q.setParameter("tString",tString);
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            return emailList;
        }
        if(personList.size()==0)
            return emailList;
        String lastEmail = "";
        String thisEmail;
        for(int i = 0; i < personList.size() ; i++){
            thisEmail = personList.get(i).getEmail().trim().toLowerCase();
            if(thisEmail.equals(lastEmail) && !emailList.contains(thisEmail)){
                emailList.add(thisEmail);
            }
            em.getTransaction().begin();
            Person p = EntityLookup.getPersonById(em,personList.get(i).getId());
            p.setEmail(thisEmail);
            em.persist(p);
            em.getTransaction().commit();
            lastEmail = thisEmail;
        }
        return emailList;
    }

}
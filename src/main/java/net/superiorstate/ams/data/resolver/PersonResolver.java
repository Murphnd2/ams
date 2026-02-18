package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.PersonV;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.EmployeeV;

import java.util.List;
import java.util.Objects;

public abstract class PersonResolver {

    private static String firstName;
    private static String lastName;
    private static Long personId;
    private static Integer employeeId;

    public static Person getBestPersonFromString(EntityManager em, String textBox){
        Person p;

        if(getEmployee(em,textBox)!=null) {
            System.out.println("EE ID: " + getEmployee(em,textBox).getId());
            return getPersonFromEmployee(em, Objects.requireNonNull(getEmployee(em, textBox)));
        } else
            System.out.println("EE NULL");
        return getPerson(em,textBox);
    }

    public static Person getPersonFromEmployee(EntityManager em, Employee ee){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.employee.id = :id");
        q.setParameter("id",ee.getId());
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            personList = null;
        }
        if(personList!=null && personList.size()>0)
            return personList.get(0);
        // CREATE PERSON AS NONE EXISTS
        em.getTransaction().begin();
        Address a = new Address();
        if(ee.getZipCode()!=null)
            a.setZipCode(ee.getZipCode());
        if(ee.getCity()!=null)
            a.setCity(ee.getCity());
        if(ee.getAddress1()!=null)
            a.setAddress1(ee.getAddress1());
        if(ee.getAddress2()!=null)
            a.setAddress2(ee.getAddress2());
        if(ee.getState()!=null && ee.getState().length()>=2)
            a.setState(ee.getState().substring(0,2));
        em.persist(a);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Person p = new Person();
        p.setAddress(a);
        if(ee.getHrEmail()!=null && EmailDAO.isValidEmail(ee.getHrEmail()))
            p.setEmail(ee.getHrEmail().toLowerCase());
        else if(ee.getEmail()!=null && EmailDAO.isValidEmail(ee.getEmail()))
            p.setEmail(ee.getEmail().toLowerCase());
        if(ee.getFirstName()!=null)
            p.setFirstName(ee.getFirstName().trim().toUpperCase());
        if(ee.getLastName()!=null)
            p.setLastName(ee.getLastName().trim().toUpperCase());
        p.setPsp(EntityLookup.getPspById(em,4L));
        p.setEmployee(ee);
        em.persist(p);
        em.getTransaction().commit();
        return p;

    }

    public static Employee getEmployee(EntityManager em, String textBox){
        if(hasEmployeeId(em,textBox))
            return EntityLookup.getEmployeeById(em,employeeId);
        if(hasPersonId(em,textBox) && personIsEmployee(em, EntityLookup.getPersonById(em,personId)))
            return EntityLookup.getPersonById(em,personId).getEmployee();
        if(EmailDAO.isValidEmail(textBox) && emailIsEmployee(em,textBox))
            return getEmployeeByEmail(em,textBox);
        if(textIsFullName(textBox) && nameIsEmployee(em,textBox))
            return getEmployeeByFullName(em,textBox);
        return null;
    }



    private static boolean personIsEmployee(EntityManager em, Person p){
        if(p.getEmployee()!=null)
            return true;
        return false;
    }

    private static boolean hasEmployeeId(EntityManager em, String textBox){
        if(!textBox.contains("("))
            return false;
        int firstBracketLoc = textBox.indexOf("(");
        String remainingText = textBox.substring(firstBracketLoc+1);
        if(!remainingText.contains(")"))
            return false;
        String employeeIdString = remainingText.substring(0,remainingText.indexOf(")"));

        try{
            employeeId = Integer.parseInt(employeeIdString);
            Employee ee = EntityLookup.getEmployeeById(em,employeeId);
            if(ee==null) {
                employeeId = null;
                return false;
            }
        } catch (Exception e){
            employeeId = null;
            return false;
        }
        return true;
    }

    private static boolean hasPersonId(EntityManager em, String textBox){
        if(!textBox.contains("{"))
            return false;
        int firstBracketLoc = textBox.indexOf("{");
        String remainingText = textBox.substring(firstBracketLoc+1);
        if(!remainingText.contains("}"))
            return false;
        String personIdString = remainingText.substring(0,remainingText.indexOf("}"));
        System.out.println("personIdString = " + personIdString);
        try{
            personId = Long.parseLong(personIdString);
            Person p = EntityLookup.getPersonById(em,personId);
            if(p==null || p.getId()==null) {
                personId = null;
                return false;
            }
        } catch (Exception e){
            personId = null;
            return false;
        }
        return true;
    }

    private static Person getPersonByEmail(EntityManager em, String email){
        Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.email is not null AND p.email = :email order by p.id desc");
        String emailScrubbed = email.trim().toLowerCase();
        q.setParameter("email",emailScrubbed);
        List<PersonV> personVList;
        try{
            personVList = (List<PersonV>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(personVList==null || personVList.size()==0)
            return null;
        return EntityLookup.getPersonById(em,personVList.get(0).getId());
    }

    private static Person getPerson(EntityManager em, String textBox){
        if(hasPersonId(em,textBox))
            return EntityLookup.getPersonById(em,personId);
        if(EmailDAO.isValidEmail(textBox) && isPerson(em,textBox))
            return getPersonByEmail(em,textBox);
        if(EmailDAO.isValidEmail(textBox))
            return createPersonFromEmail(em,textBox);
        if(textIsFullName(textBox))
            return getPersonByFullName(em,textBox);
        return null;
    }

    public static Person createPersonFromEmail(EntityManager em, String email){
        return createPersonFromEmail(em,email,null);
    }
    public static Person createPersonFromFullName(EntityManager em, String fullName, String email){
        return createPersonFromAll(em,fullName,email,null);
    }

    public static void cleanseContactList(EntityManager em){
        Query q = em.createQuery("SELECT p FROM PersonV p WHERE (p.firstName is null OR p.lastName is null OR p.email is null) AND p.employee is not null");
        List<PersonV> personVList;
        try{
            personVList = (List<PersonV>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(personVList==null || personVList.size()==0)
            return;
        for(PersonV pv: personVList){
            Employee ee = EntityLookup.getEmployeeById(em,pv.getEmployee().getId());
            em.getTransaction().begin();
            Person p = EntityLookup.getPersonById(em,pv.getId());
            p.setLastName(ee.getLastName().trim().toUpperCase());
            p.setFirstName(ee.getFirstName().trim().toUpperCase());
            if(p.getEmail()==null || !EmailDAO.isValidEmail(p.getEmail())){
                if(ee.getHrEmail()!=null && EmailDAO.isValidEmail(ee.getHrEmail()))
                    p.setEmail(ee.getHrEmail().trim().toLowerCase());
                else if(ee.getEmail()!=null && EmailDAO.isValidEmail(ee.getEmail()))
                    p.setEmail(ee.getEmail().trim().toLowerCase());
            }
            em.persist(p);
            em.getTransaction().commit();
        }
    }

    public static Person createPersonFromAll(EntityManager em, String fullName, String email, String phone){

        em.getTransaction().begin();
        Person p = new Person();
        p.setPsp(EntityLookup.getPspById(em,4L));
        if(email!=null)
            p.setEmail(email.toLowerCase().trim());
        if((fullName == null || fullName.equals("")) && (email!=null && !email.equals(""))){
            String domain = email.substring(email.indexOf("@")+1);
            String name = email.substring(0,email.indexOf("@"));
            if(name.contains("."))
                p.setFirstName(name.substring(0,name.indexOf(".")).toUpperCase());
            else
                p.setFirstName(name.toUpperCase());
            if(name.contains(".") && name.length()>name.indexOf(".")+1)
                p.setLastName(name.substring(name.indexOf(".")+1).toUpperCase());
            else
                p.setLastName(domain.substring(0,domain.indexOf(".")).toUpperCase());
        } else if(fullName!=null) {
            if(fullName.contains(",")){
                p.setLastName(fullName.substring(0,fullName.indexOf(",")).trim().toUpperCase());
                p.setFirstName(fullName.substring(fullName.indexOf(",")+1).trim().toUpperCase());
            } else {
                p.setFirstName(fullName.substring(0,fullName.indexOf(" ")).trim().toUpperCase());
                p.setLastName(fullName.substring(fullName.indexOf(" ")+1).trim().toUpperCase());
            }
        }
        if(phone!=null)
            p.setPhone(phone);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }
    public static Person createPersonFromEmail(EntityManager em, String email, String phone){
        return createPersonFromAll(em,null,email,phone);
    }
    private static Person getPersonByFullName(EntityManager em, String fullName){
        setFirstAndLastName(fullName);
        if(firstName!=null && firstName.length() >= 1 && lastName!=null && lastName.length() >= 1){
            Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.firstName = :fName AND p.lastName = :lName order by p.id desc");
            q.setParameter("fName",firstName);
            q.setParameter("lName",lastName);
            List<PersonV> personVList;
            try{
                personVList = (List<PersonV>) q.getResultList();
            } catch (NoResultException e){
                return null;
            }
            if(personVList!=null && personVList.size()>=1)
                return EntityLookup.getPersonById(em,personVList.get(0).getId());
        }
        return null;
    }

    private static boolean isPerson(EntityManager em, String email){
        if(getPersonByEmail(em,email)==null)
            return false;
        return true;
    }

    private static boolean emailIsEmployee(EntityManager em, String email){
        if(getEmployeeByEmail(em,email)==null)
            return false;
        return true;
    }



    public static boolean textIsFullName(String name){
        if(!name.contains(" ") && !name.contains(","))
            return false;
        return true;
    }

    private static boolean nameIsEmployee(EntityManager em, String fullName){
        if(getEmployeeByFullName(em,fullName)==null)
            return false;
        return true;
    }

    private static Employee getEmployeeByFullName(EntityManager em, String fullName){
        setFirstAndLastName(fullName);
        if(firstName!=null && firstName.length() >= 1 && lastName!=null && lastName.length() >= 1){
            Query q = em.createQuery("SELECT ee FROM EmployeeV ee WHERE ee.firstName = :fName AND ee.lastName = :lName order by ee.id desc");
            q.setParameter("fName",firstName);
            q.setParameter("lName",lastName);
            List<EmployeeV> employeeVList;
            try{
                employeeVList = (List<EmployeeV>) q.getResultList();
            } catch (NoResultException e){
                return null;
            }
            if(employeeVList!=null && employeeVList.size()>=1)
                return EntityLookup.getEmployeeById(em,employeeVList.get(0).getId());
        }
        return null;
    }
    private static void setFirstAndLastName(String fullName){
        if(fullName.contains(",")){
            lastName = fullName.substring(0,fullName.indexOf(",")).trim().toUpperCase();
            if(fullName.trim().length()==lastName.length()+1) {
                firstName = null;
                lastName = null;
                return;
            }
            firstName = fullName.substring(fullName.indexOf(",")+1).trim().toUpperCase();
        } else {
            firstName = fullName.substring(0,fullName.indexOf(" ")).trim().toUpperCase();
            if(fullName.trim().length()==firstName.length()+1) {
                firstName = null;
                lastName = null;
                return;
            }
            lastName = fullName.substring(fullName.indexOf(" ")+1).trim().toUpperCase();
        }
    }
    private static Employee getEmployeeByEmail(EntityManager em, String email){
        Query q = em.createQuery("SELECT e FROM EmployeeV e WHERE (e.emailSystem is not null AND e.emailSystem = :email) OR (e.emailSummit is not null AND e.emailSummit = :email) order by e.id desc");
        String emailScrubbed = email.trim().toLowerCase();
        q.setParameter("email",emailScrubbed);
        List<EmployeeV> employeeVList;
        try{
            employeeVList = (List<EmployeeV>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(employeeVList==null || employeeVList.size()==0)
            return null;
        return EntityLookup.getEmployeeById(em,employeeVList.get(0).getId());
    }

}

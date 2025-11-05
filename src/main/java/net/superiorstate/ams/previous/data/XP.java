package net.superiorstate.ams.previous.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.PersonV;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.EmployeeV;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class XP {
    public static Person getBestPerson(EntityManager em, String text){
        if(hasEmployeeCodeInString(Objects.requireNonNull(text)))
            return getPersonFromEmployee(em,getEmployeeFromCode(em,text));
        else if(dbEmail.isValidEmail(text) && emailBelongsToEmployee(em,text))
            return getPersonFromEmployee(em, Objects.requireNonNull(getEmployeeFromEmail(em, text)));
        else if(dbEmail.isValidEmail(text) && emailBelongsToPerson(em,text))
            return getPersonFromEmail(em,text);
        else if(dbEmail.isValidEmail(text) && emailNameMatchesExistingPerson(em,text))
            return getPersonByEmailName(em,text);
        else if(dbEmail.isValidEmail(text))
            return eV.createPersonFromEmail(em, text);
        else if(textMatchesExistingPerson(em,text))
            return getPersonFromText(em,text);
        else return createNewPerson(em,text);
    }

    private static Person createNewPerson(EntityManager em, String text){
        List<String> nameList = getLastFirstStrings(text);
        String first = text.toUpperCase().trim();
        String last;
        if(nameList==null)
            last = "UNKNOWN";
        else{
            last = nameList.get(0).toUpperCase();
            first = nameList.get(1).toUpperCase();
        }
        PSP psp = dM.getPspById(em,4L);
        em.getTransaction().begin();
        Person p = new Person();
        p.setPsp(psp);
        p.setFirstName(first);
        p.setLastName(last);
        p.setFullName(first +" "+ last);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }
    private static boolean emailNameMatchesExistingPerson(EntityManager em, String email){
        return getPersonByEmailName(em,email)!=null;
    }

    private static Person getPersonByEmailName(EntityManager em, String text){
        String left = text.substring(0,text.indexOf("@"));
        if(left.contains(".")){
            String first = left.substring(0,left.indexOf(".")).toUpperCase().trim();
            String last = left.substring(left.indexOf(".")+1).toUpperCase().trim();
            Person p = getPersonFromText(em,first+" "+last);
            if(p!=null && p.getEmail()==null){
                em.getTransaction().begin();
                p.setEmail(text);
                em.persist(p);
                em.getTransaction().commit();
                return p;
            }
        }
        return null;
    }

    private static boolean textMatchesExistingPerson(EntityManager em, String text){
        return getPersonFromText(em,text)!=null;
    }
    private static List<String> getLastFirstStrings(String text){
        boolean hasAtLeastTwoNames = text.contains(" ") || text.contains(",");
        if(!hasAtLeastTwoNames)
            return null;
        String first;
        String last;
        if(text.contains(",")) {
            last = text.substring(0, text.indexOf(",")).toUpperCase().trim();
            first = text.substring(text.indexOf(",")+1).trim().toUpperCase();
        } else {
            first = text.substring(0, text.indexOf(" ")).toUpperCase().trim();
            last = text.substring(text.indexOf(" ")+1).trim().toUpperCase();
        }
        List<String> nameList = new ArrayList<>();
        nameList.add(last);
        nameList.add(first);
        return nameList;
    }
    private static Person getPersonFromText(EntityManager em, String text){
        List<String> nameList = getLastFirstStrings(text);
        if(nameList==null)
            return null;
        String last = nameList.get(0);
        String first = nameList.get(1);
        if(getPersonFromNames(em,first,last)!=null)
            return getPersonFromNames(em,first,last);
        else if(getPersonFromNames(em,last,first)!=null)
            return getPersonFromNames(em,last,first);
        else return null;
    }

    private static Person getPersonFromNames(EntityManager em, String first, String last){
        Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.firstName = :first AND p.lastName = :last order by p.id DESC");
        q.setParameter("first",first);
        q.setParameter("last",last);
        List<PersonV> personVList;
        try{
            personVList = (List<PersonV>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(personVList.size()==0)
            return null;
        return dM.getPersonById(em,personVList.get(0).getId());
    }
    private static boolean emailBelongsToPerson(EntityManager em, String email){
        boolean answer = getPersonFromEmail(em,email)!=null;
        System.out.println("###### Email belongs to Person is "+answer);
        return answer;
    }

    private static Person getPersonFromEmail(EntityManager em, String email){
        System.out.println("Checking Person for Email: " + email);
        Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.email = :email order by p.id DESC");
        q.setParameter("email",email.toLowerCase().trim());
        List<PersonV> personVList;
        try {
            personVList = (List<PersonV>) q.getResultList();
        } catch (NoResultException e1){
            System.out.println("PERSON BY EMAIL NULL");
            return null;
        }
        System.out.println("PERSON BY EMAIL SIZE: "+personVList.size());
        if(personVList.size()==0)
            return null;
        System.out.println("PERSON BY EMAIL FOUND");
        return dM.getPersonById(em,personVList.get(0).getId());
    }

    private static boolean emailBelongsToEmployee(EntityManager em, String text) {
        boolean answer = getEmployeeFromEmail(em, text) != null;
        System.out.print("-----Email belongs to Employee is "+answer);
        return answer;
    }
    private static Employee getEmployeeFromEmail(EntityManager em, String text) {
        Query q = em.createQuery("SELECT e FROM EmployeeV e WHERE e.emailSummit =:email order by e.id DESC");
        q.setParameter("email",text.toLowerCase());
        List<EmployeeV> employeeVList;
        try {
            employeeVList = (List<EmployeeV>) q.getResultList();
        } catch (NoResultException e) {
            Query q1 = em.createQuery("SELECT e FROM EmployeeV e WHERE e.emailSystem = :email order by e.id DESC");
            q1.setParameter("email",text.toLowerCase());
            try {
                employeeVList = (List<EmployeeV>) q1.getResultList();
            } catch (NoResultException e1) {
                return null;
            }
        }
        if(employeeVList.size()==0)
            return null;
        return dM.getEmployeeById(em,employeeVList.get(0).getId());
    }

    private static String getEmployeeIdString(String text){
        if(text.contains("{[") && text.contains("]}") && text.indexOf("]}")>text.indexOf("{[")){
            int firstBracket = text.indexOf("{[");
            int secondBracket = text.indexOf("]}");
            String idString = text.substring(firstBracket+2,secondBracket);
            if(idString.length() == 0) {
                return null;
            }
            else{
                int i;
                try{
                     i = Integer.parseInt(idString);
                } catch (NumberFormatException nfe){
                    return null;
                }
            }
            return idString;
        } else return null;
    }
    private static boolean hasEmployeeCodeInString(String text){
        return getEmployeeIdString(text) != null;
    }
    public static Employee getEmployeeFromCode(EntityManager em, String text){
        int id = Integer.parseInt(Objects.requireNonNull(getEmployeeIdString(text)));
        return dM.getEmployeeById(em,id);
    }
    public static Person getPersonFromEmployee(EntityManager em, Employee ee){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.employee.id = :id");
        q.setParameter("id",ee.getId());
        List<Person> personList;
        PSP psp = dM.getPspById(em,4L);
        Person p;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
           personList = null;
        }
        if(personList==null || personList.size()==0){
            em.getTransaction().begin();
            Address a = new Address();
            a.setAddress1(ee.getAddress1());
            a.setAddress2(ee.getAddress2());
            a.setCity(ee.getCity());
            if(ee.getState()!=null && ee.getState().length()>=2)
                a.setState(ee.getState().substring(0,2));
            a.setZipCode(ee.getZipCode());
            em.persist(a);
            em.getTransaction().commit();
            em.getTransaction().begin();
            p = new Person();
            p.setEmployee(ee);
            p.setLastName(ee.getLastName());
            p.setFirstName(ee.getFirstName());
            p.setFullName(ee.getFirstName() + " " + ee.getLastName());
            p.setAddress(a);
            p.setPsp(psp);
            em.persist(p);
            em.getTransaction().commit();
            return p;
        }
        return personList.get(0);
    }
}

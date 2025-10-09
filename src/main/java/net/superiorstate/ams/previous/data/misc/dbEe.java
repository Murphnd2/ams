package net.superiorstate.ams.previous.data.misc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.util.List;

public abstract class dbEe {


    public static Employee getEmployeeByEmail(EntityManager em, String email){
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.email = :emailString");
        q.setParameter("emailString",email);
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
        if(employeeList.size()>0)
            return employeeList.get(0);
        return null;
    }

    public static void addEmployeeContact(EntityManager em, Employee ee, Employer er){
        em.getTransaction().begin();
        Employer employer = dM.getEmployerById(em,er.getId());
        Employee employee = dM.getEmployeeById(em,ee.getId());
        List<Employee> employeeList = employer.getContactList();
        if(!employeeList.contains(employee)) {
            employer.addContact(employee);
            em.persist(employer);
            em.persist(employee);
        }
        em.getTransaction().commit();
    }

    public static void removeEmployeeContact(EntityManager em, Employee ee, Employer er){
        em.getTransaction().begin();
        Employer employer = dM.getEmployerById(em,er.getId());
        Employee employee = dM.getEmployeeById(em,ee.getId());
        List<Employee> employeeList = employer.getContactList();
        if(employeeList.contains(employee)) {
            employer.removeContact(employee);
            em.persist(employer);
            em.persist(employee);
        }
        em.getTransaction().commit();
    }

}

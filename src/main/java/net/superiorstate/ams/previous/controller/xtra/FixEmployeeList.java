package net.superiorstate.ams.previous.controller.xtra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "FixEmployeeList", value = "/FixEmployeeList")
public class FixEmployeeList extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void deleteEmployees(EntityManager em){
        List<Employee> employees = employeesToDelete(em);
        for(Employee e:employees){
            Employee keeper;
            Query q = em.createQuery("SELECT e FROM Employee e WHERE e.email = :email order by e.email, e.employer.id, e.id desc");
            q.setParameter("email",e.getEmail());
            List<Employee> temp;
            try{
                temp = (List<Employee>) q.getResultList();
            } catch (NoResultException ex){
                continue;
            }
            if(temp.size()>=0)
                continue;
            keeper = temp.get(0);
            //Find Persons with this Employee and Replace
            List<Person> personList = getPersonFromEmployee(em,e);
            if(personList!=null && personList.size()>0){
                for(Person p: personList){
                    em.getTransaction().begin();
                    p.setEmployee(keeper);
                    em.persist(p);
                    em.getTransaction().commit();
                }
            }
        }
    }

    private List<Person> getPersonFromEmployee(EntityManager em, Employee ee){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.employee.id = :id");
        q.setParameter("id",ee.getId());
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return personList;
    }

    private List<Employee> employeesToDelete(EntityManager em){
        Query q = em.createQuery("SELECT e FROM Employee e where e.email is not null order by e.email,e.employer.id,e.id desc ");
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        }catch (NoResultException e){
            return null;
        }
        List<Employee> deleteList = new ArrayList<>();
        String currentEmail = employeeList.get(0).getEmail();
        int currentEmployerId = employeeList.get(0).getEmployer().getId();
        for(int i = 1; i < employeeList.size(); i++){
            Employee ee = employeeList.get(i);
            String email = ee.getEmail();
            int employerId = ee.getEmployer().getId();
            if(email.equalsIgnoreCase(currentEmail)&&employerId==currentEmployerId){
                deleteList.add(ee);
            } else {
                currentEmail = email;
                currentEmployerId = employerId;
            }
        }
        return deleteList;
    }
}

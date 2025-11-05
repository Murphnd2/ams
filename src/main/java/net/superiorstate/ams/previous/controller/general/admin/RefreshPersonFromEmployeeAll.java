package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RefreshPersonFromEmployeeAll", value = "/RefreshPersonFromEmployeeAll")
public class RefreshPersonFromEmployeeAll extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/adminHome.jsp");
        dispatcher.forward(request,response);
    }

    private void doThisFirst(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        refreshAllPersons(em);
        //Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        //dP.refreshPersonDataFromEmployeeData(em,currentPerson.getPsp());
        em.close();

    }

    private void refreshAllPersons(EntityManager em){
        Query getPersonsWithEmployees = em.createQuery("SELECT e FROM Employee e INNER JOIN Person p ON e.id = p.employee.id");
        List<Employee> eesWithPersons = (List<Employee>) getPersonsWithEmployees.getResultList();
        Query getAllEmployees = em.createQuery("SELECT e FROM Employee e");
        List<Employee> employeeList = (List<Employee>) getAllEmployees.getResultList();
        employeeList.removeAll(eesWithPersons);
        for(Employee ee:employeeList){
            if(ee.getEmail()!=null){
                boolean foundPersonExisted = false;
                Query q= em.createQuery("SELECT p FROM Person p WHERE p.email = :email");
                q.setParameter("email",ee.getEmail());
                List<Person> personList;
                try{
                    personList = (List<Person>) q.getResultList();
                } catch (NoResultException e){
                    personList = null;
                }
                if(personList.size()==0)
                    continue;
                for(Person p:personList){
                    if(p.getEmployee() == null){
                        em.getTransaction().begin();
                        Query query = em.createQuery("SELECT p FROM Person p WHERE p.id = :id");
                        query.setParameter("id",p.getId());
                        Person person = (Person) query.getSingleResult();
                        person.setEmployee(ee);
                        em.persist(person);
                        em.getTransaction().commit();
                        foundPersonExisted = true;
                        break;
                    }
                }
                if(foundPersonExisted)
                    break;
                Address a = createAddress(em,ee.getAddress1(),ee.getAddress2(),ee.getCity(),ee.getState(),ee.getZipCode());
                PSP psp = dM.getPspById(em,4L);
                em.getTransaction().begin();
                Person p1 = new Person();
                p1.setEmployee(ee);
                p1.setEmail(ee.getEmail());
                p1.setLastName(ee.getLastName());
                p1.setPsp(psp);
                p1.setAddress(a);
                p1.setFullName(ee.getFirstName() + " " + ee.getLastName());
                em.persist(p1);
                em.getTransaction().commit();
            }
        }
    }

    private Address createAddress(EntityManager em, String ad1, String ad2, String cit, String st, String zip){
        em.getTransaction().begin();
        Address a = new Address();
        a.setAddress1(ad1);
        a.setZipCode(zip);
        a.setAddress2(ad2);
        a.setCity(cit);
        a.setState(st.substring(0,2));
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }
}

package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.Starter;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.general.UserRole;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@WebServlet(name = "CreatePspUser25", value = "/CreatePspUser25")
public class CreatePspUser25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User u;
        try {
            u = generateUser(request);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        goToHome(request,response);

    }

    private void goToHome(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
    }

    private User generateUser(HttpServletRequest request) throws NoSuchAlgorithmException {
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("userEmail");
        String password = request.getParameter("tempPassword");
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Person admin = local.getCurrentPerson();
        if(!dbEmail.isValidEmail(email) || dM.getUserById(em,email)!=null)
            return null;
        //Get or Create Employee Record
        Employee ee = getOrCreateEmployeeForUser(em,email,lastName,firstName,admin);
        Person newPerson = getOrCreatePersonFromEmployee(em,ee,admin);
        global.addUser(newPerson);
        //Create User
        User u = Starter.createUser(em,newPerson,email,password);
        UserRole ur = dM.getUserRoleById(em,1);
        em.getTransaction().begin();
        u.addUserToRole(ur);
        em.persist(u);
        em.getTransaction().commit();

        //Make Administrator?
        String makeAdmin = request.getParameter("makeAdmin");
        if(makeAdmin!=null && makeAdmin.equals("1")){
            ur = dM.getUserRoleById(em,5);
            em.getTransaction().begin();
            u.addUserToRole(ur);
            em.persist(u);
            em.getTransaction().commit();
        }
        Starter.createTimeEntry(em,newPerson,null,null);
        em.close();
        request.getServletContext().setAttribute("global",global);
        return u;
    }
    private Person getOrCreatePersonFromEmployee(EntityManager em, Employee ee, Person admin){
        Person p;
        Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.employee is null AND p.email = :email");
        q.setParameter("email",ee.getEmail().trim().toLowerCase());
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            personList = null;
        }

        if(personList==null || personList.size()==0){
            PSP psp = dM.getPspById(em,4L);
            em.getTransaction().begin();
            p = new Person();
            p.setPsp(psp);
            p.setEmployee(ee);
            p.setEmail(ee.getEmail());
            p.setAddress(admin.getAddress());
            p.setLastName(ee.getLastName().toUpperCase());
            p.setFullName(ee.getFirstName().toUpperCase()+" "+ee.getLastName().toUpperCase());
            p.setFirstName(ee.getFirstName().toUpperCase());
        } else {
            p = personList.get(0);
            em.getTransaction().begin();
            p.setEmployee(ee);
            p.setFirstName(ee.getFirstName().toUpperCase());
            p.setLastName(ee.getLastName().toUpperCase());
            p.setFullName(p.getFirstName()+" "+p.getLastName());
        }
        em.persist(p);
        em.getTransaction().commit();
        return p;

    }
    private Employee getOrCreateEmployeeForUser(EntityManager em, String email, String lastName, String firstName, Person admin){
        //Does Employee Already Exist
        Employee ee = dbEmail.getEmployeeByEmail(em,email,admin);
        if(ee!=null)
            return ee;
        Query q = em.createQuery("SELECT e FROM EmployeeV e WHERE e.lastName=:lName and e.firstName=:fName");
        q.setParameter("lName",lastName.trim().toUpperCase());
        q.setParameter("fName",firstName.trim().toUpperCase());
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            employeeList = null;
        }
        if(employeeList!=null && employeeList.size()>0){
            for(Employee employee:employeeList){
                if(employee.getEmployer().getId()==admin.getEmployee().getEmployer().getId())
                    return employee;
            }
        }
        // Create a New Employee
        int eeId = getNewId(em);
        em.getTransaction().begin();
        ee = new Employee();
        ee.setEmployer(admin.getEmployee().getEmployer());
        ee.setLastName(lastName.toUpperCase());
        ee.setFirstName(firstName.toUpperCase());
        ee.setEmail(email);
        ee.setAddress1(admin.getAddress().getAddress1());
        ee.setAddress2(admin.getAddress().getAddress2());
        ee.setCity(admin.getAddress().getCity());
        ee.setState(admin.getAddress().getState());
        ee.setZipCode(admin.getAddress().getZipCode());
        ee.setId(eeId);
        ee.setActive(true);
        em.persist(ee);
        em.getTransaction().commit();
        return ee;
    }

    private int getNewId(EntityManager em){
        Query q = em.createQuery("SELECT e FROM Employee e order by e.id");
        List<Employee> employeeList = (List<Employee>) q.getResultList();
        return employeeList.get(0).getId() -1;
    }
}


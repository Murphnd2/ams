package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.ActivityDAO;
import net.superiorstate.ams.data.resolver.PersonResolver;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.ActivityViewHelper;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "AddContactToActivity", value = "/AddContactToActivity")
public class AddContactToActivity extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addContact(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
    private void addContact(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        request.getSession().setAttribute("lastTab",1);
        String buttonClicked = request.getParameter("btnAddContact");
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        boolean makePrimary  = false;
        try{
            makePrimary = request.getParameter("makePrimaryCheck").equals("1");
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Make Primary is: " + makePrimary);
        Person p = null;
        if(buttonClicked.equals("2")){
            int eeId = Integer.parseInt(request.getParameter("addEmployeeList"));
            Employee ee = EntityLookup.getEmployeeById(em,eeId);
            p = ActivityDAO.getEmployeePerson(em,ee);
            Activity activity = EntityLookup.getActivityById(em,a.getId());
            em.getTransaction().begin();
            assert activity != null;
            activity.addAssigneeContact(p);
            em.persist(activity);
            em.getTransaction().commit();
        } else if(buttonClicked.equals("1")){
            String email = request.getParameter("contactEmail");
            if(EmailDAO.isValidEmail(email)){
                p = PersonResolver.getBestPersonFromString(em,email);

                if(p==null)
                    p = PersonResolver.createPersonFromEmail(em,email);

                Activity activity = EntityLookup.getActivityById(em,a.getId());
                em.getTransaction().begin();
                assert activity != null;
                activity.addAssigneeContact(p);
                em.persist(activity);
                em.getTransaction().commit();
            }
        }
        if(p!=null && p.getEmail()!=null && EmailDAO.isValidEmail(p.getEmail()) && makePrimary)
            makeContactPrimary(em,p,a);
        ActivityViewHelper.setActivityView(request,em,a);
        em.close();
    }

    public static Person investigateEmail(EntityManager em, String email, Employer er){
        if(!EmailDAO.isValidEmail(email))
            return null;
        Person p;
        p = checkEmployeeList(em,er,email);
        if(p==null)
            p = checkPersonList(em,email);
        if(p==null){
            em.getTransaction().begin();
            p = new Person();
            p.setEmail(email);
            p.setPsp(EntityLookup.getPspById(em,4));
            p.setFirstName(findFirstName(email));
            p.setLastName(findLastName(email));
            p.setFullName(findFirstName(email) + " " + findLastName(email));
            em.persist(p);
            em.getTransaction().commit();
        }
        return p;
    }

    private void makeContactPrimary(EntityManager em, Person np, Activity a){
        Person cp = ActivityDAO.getPrimaryContact(em,a);
        Activity activity = EntityLookup.getActivityById(em,a.getId());
        em.getTransaction().begin();
        assert activity != null;
        activity.setPrimaryContact(np);
        em.persist(activity);
        em.getTransaction().commit();

        em.getTransaction().begin();
        activity.addAssigneeContact(cp);
        activity.removeAssigneeContact(np);
        em.persist(activity);
        em.getTransaction().commit();

    }

    private static String findFirstName(String email){
        int atLoc = email.indexOf("@");
        int dotLoc = email.indexOf(".");
        int underLoc = email.indexOf("_");
        int dashLoc = email.indexOf("-");
        if(dotLoc>0 && dotLoc<atLoc)
            return email.substring(0,dotLoc);
        else if(underLoc>0 && underLoc<atLoc)
            return email.substring(0,underLoc);
        else if(dashLoc>0 && dashLoc<atLoc)
            return email.substring(0,dashLoc);
        else
            return email.substring(0,atLoc);
    }

    private static String findLastName(String email){
        int atLoc = email.indexOf("@");
        int dotLoc = email.indexOf(".");
        int underLoc = email.indexOf("_");
        int dashLoc = email.indexOf("-");
        if(dotLoc>0 && dotLoc<atLoc)
            return email.substring(dotLoc+1,atLoc);
        else if(underLoc>0 && underLoc<atLoc)
            return email.substring(underLoc+1,atLoc);
        else if(dashLoc>0 && dashLoc<atLoc)
            return email.substring(dashLoc+1,atLoc);
        else
            return email.substring(atLoc+1,email.indexOf(".",atLoc));
    }
    private static Person checkPersonList(EntityManager em, String email){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.email is not null");
        List<Person> personList = (List<Person>) q.getResultList();
        for(Person p: personList){
            if(p.getEmail().equalsIgnoreCase(email))
                return p;
        }
        return null;
    }

    private static Person checkEmployeeList(EntityManager em, Employer er, String email){
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.employer.id = :id order by e.id desc");
        q.setParameter("id",er.getId());
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(employeeList.size()==0)
            return null;
        for(Employee e: employeeList){
            if(e.getHrEmail()!=null && e.getHrEmail().equalsIgnoreCase(email))
                return ActivityDAO.getEmployeePerson(em,e);
            else if(e.getEmail()!=null && e.getEmail().equalsIgnoreCase(email))
                return ActivityDAO.getEmployeePerson(em,e);
        }
        return null;
    }
    private static Person checkEmployeeList(EntityManager em, Activity a, String email){
        Employer er;
        if(a.getClass().getSimpleName().equals("Renewal")){
            Renewal r = (Renewal) a;
            er = r.getEmployer();
        } else if (a.getClass().getSimpleName().equals("Ticket") && isTiedToEmployer(em,a)){
            er = a.getPrimaryContact().getEmployee().getEmployer();
        } else {
            return getAnyEmployeeMatchingEmail(em,email);
        }
        return checkEmployeeList(em,er,email);
    }

    private static Person getAnyEmployeeMatchingEmail(EntityManager em, String email){
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.email is not null or e.hrEmail is not null");
        List<Employee> employees = (List<Employee>) q.getResultList();
        for(Employee e:employees){
            if(e.getHrEmail()!=null && e.getHrEmail().equalsIgnoreCase(email))
                return ActivityDAO.getEmployeePerson(em,e);
            else if(e.getEmail()!=null && e.getEmail().equals(email))
                return ActivityDAO.getEmployeePerson(em,e);
        }
        return null;
    }

    private static boolean isTiedToEmployer(EntityManager em, Activity a){
        Person p = a.getPrimaryContact();
        if(p.getEmployee()!=null)
            return true;
        return false;
    }
}

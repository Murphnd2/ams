package net.superiorstate.ams.previous.archive.activityDetail.actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.eV;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "addContactToActivities", value = "/addContactToActivities")
public class addContactToActivities extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addContact(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("goActivityDetail");
        dispatcher.forward(request,response);
    }
    private void addContact(HttpServletRequest request){
        String buttonClicked;
        boolean makePrimary;
        try{
            buttonClicked = request.getParameter("btnAddContact").toString();
            makePrimary = "1".equals(request.getParameter("makePrimaryCheck"));
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Didn't Retrieve");
            return;
        }

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        if(sVar==null)
            return;

        System.out.println("bc" + buttonClicked);
        System.out.println("mp: " + makePrimary);
        Activity a = sVar.getCurrentActivity();

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Person p = null;
        if(buttonClicked.equals("2")){
            int eeId = Integer.parseInt(request.getParameter("addEmployeeList"));
            Employee ee = dM.getEmployeeById(em,eeId);
            p = dActivity.getEmployeePerson(em,ee);
            Activity activity = dM.getActivityById(em,a.getId());
            em.getTransaction().begin();
            assert activity != null;
            activity.addAssigneeContact(p);
            em.persist(activity);
            em.getTransaction().commit();
            em.refresh(activity);
        } else if(buttonClicked.equals("1")){
            String email = request.getParameter("contactEmail");
            if(dbEmail.isValidEmail(email)){
                p = eV.getBestPersonFromString(em,email);

                if(p==null)
                    p = eV.createPersonFromEmail(em,email);

                Activity activity = dM.getActivityById(em,a.getId());
                em.getTransaction().begin();
                if(activity!=null)
                    activity.addAssigneeContact(p);
                em.persist(activity);
                em.getTransaction().commit();
                em.refresh(activity);
            }
        }
        if(makePrimary && p!=null){
            String email = null;
            if(p.getEmployee()!=null && p.getEmployee().getEmail()!=null && V.isValidEmail(p.getEmployee().getEmail()))
                email = p.getEmployee().getEmail();
            else if(p.getEmail()!=null && V.isValidEmail(p.getEmail()))
                email = p.getEmail();
            if(email!=null){
                System.out.println("DID THIS *****************");
                Person cp = null;
                if(sVar.getPrimaryContactForActivity()!=null)
                    cp = sVar.getPrimaryContactForActivity();
                else if(sVar.getCurrentActivity().getPrimaryContact()!=null)
                    cp = sVar.getCurrentActivity().getPrimaryContact();
                if(cp!=null){
                    Activity a1 = dM.getActivityById(em,a.getId());
                    if(a1!=null){
                        em.getTransaction().begin();
                        a1.setPrimaryContact(p);
                        em.persist(a1);
                        em.getTransaction().commit();

                        em.getTransaction().begin();
                        a1.addAssigneeContact(cp);
                        a1.removeAssigneeContact(p);
                        em.persist(a1);
                        em.getTransaction().commit();
                        em.refresh(a1);
                        sVar.setCurrentActivity(a1);
                        sVar.setPrimaryContactForActivity(p);
                    }
                }
            }
        }


        sVar.refreshContactListFromCurrentActivity(em);
        request.getSession().setAttribute("sVar",sVar);

        em.close();
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
                return dActivity.getEmployeePerson(em,e);
            else if(e.getEmail()!=null && e.getEmail().equalsIgnoreCase(email))
                return dActivity.getEmployeePerson(em,e);
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
                return dActivity.getEmployeePerson(em,e);
            else if(e.getEmail()!=null && e.getEmail().equals(email))
                return dActivity.getEmployeePerson(em,e);
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

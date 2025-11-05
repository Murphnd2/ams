package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dP;
import net.superiorstate.ams.previous.data.misc.dbEe;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;

@WebServlet(name = "AddSetupContact", value = "/AddSetupContact")
public class AddSetupContact extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        assignPersonToSetup(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        assignPersonToSetup(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void assignPersonToSetup(HttpServletRequest request){
        Setup setup = (Setup) request.getSession().getAttribute("currentActivity");
        System.out.println("Add Setup ID 1: " + setup.getId());
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person p = retrievePerson(request,em);
        em.getTransaction().begin();
        Person person = dM.getPersonById(em,p.getId());
        Setup s = dM.getSetupById(em,setup.getId());
        System.out.println("Add Setup ID 2: "+ setup.getId());
        assert s != null;
        s.getContactList().add(person);
        em.persist(s);
        em.getTransaction().commit();
        ViewSelectedActivity.setActivityView(request,em,s);
        em.close();
    }

    private Person retrievePerson(HttpServletRequest request,EntityManager em){
        String theEmail = request.getParameter("newEmail");
        Person p = getPersonByEmail(request,em,theEmail);
        if(p!=null)
            return p;
        String fName = request.getParameter("newFirst");
        String lName = request.getParameter("newLast");
        Setup s = (Setup) request.getSession().getAttribute("currentActivity");
        Person currentUser = (Person) request.getSession().getAttribute("currentPerson");
        em.getTransaction().begin();
        p = new Person();
        p.setEmail(theEmail);
        p.setFirstName(fName);
        p.setLastName(lName);
        p.setFullName(fName + " " + lName);
        p.setPsp(currentUser.getPsp());
        em.persist(p);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Setup setup = dM.getSetupById(em,s.getId());
        setup.addContact(p);
        em.persist(setup);
        em.persist(p);
        em.getTransaction().commit();

        return p;
    }

    private Employee getEmployeeByEmail(HttpServletRequest request, EntityManager em, String theEmail){
        if(!dbEmail.isValidEmail(theEmail))
            return null;
        Employee e = dbEe.getEmployeeByEmail(em,theEmail);
        return null;
    }

    private Person getPersonByEmail(HttpServletRequest request, EntityManager em, String theEmail){

        Employee e = getEmployeeByEmail(request,em,theEmail);
        Person p;
        if(e!=null){
            p = dP.getPersonByEmployee1(em,e);
        } else {
            p = dP.getPersonByEmail(em,theEmail);
        }
        return p;
    }
}

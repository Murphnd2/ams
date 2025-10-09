package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dP;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.ticket.tEmployee;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "UpdateRenewalContact", value = "/UpdateRenewalContact")
public class UpdateRenewalContact extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ResetAdminView");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        String theEmail = request.getParameter("newEmail");
        String theFirst = request.getParameter("newFirst");
        String theLast = request.getParameter("newLast");
        List<tEmployee> employeeList = (List<tEmployee>) request.getSession().getAttribute("contactList");
        String oldEmail;
        try{
            oldEmail = employeeList.get(0).getEmail();
            if(oldEmail==null)
                oldEmail="";
        } catch (Exception e1){
            oldEmail = "";
        }
        String oldFirst;
        try{
            oldFirst = employeeList.get(0).getFirstName();
            if(oldFirst==null)
                oldFirst="";
        } catch (Exception e2){
            oldFirst="";
        }
        String oldLast;
        try{
            oldLast = employeeList.get(0).getLastName();
            if(oldLast==null)
                oldLast="";
        } catch (Exception e3){
            oldLast = "";
        }
        String reminderText = "Update " + oldFirst + " " + oldLast + " whose email is:" + oldEmail + " to " + theFirst + " " + theLast + " with email of: " + theEmail;
        if(theEmail.compareTo(oldEmail)==0 && theFirst.compareTo(oldFirst)==0 && theLast.compareTo(oldLast)==0)
            return;
        processChanges(request,employeeList.get(0),theEmail,theFirst,theLast, reminderText);

    }

    private void processChanges(HttpServletRequest request, tEmployee ee, String email, String first, String last, String reminder){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Employee e1 = dM.getEmployeeById(em,ee.getId());
        User user = (User) request.getSession().getAttribute("currentUser");
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        Employee employee = updateEmployee(em,e1,email,first,last);
        updatePerson(em,employee,psp);
        createReminder(em,user,reminder);
        em.close();
    }

    private Employee updateEmployee(EntityManager em, Employee ee, String email, String first, String last){
        em.getTransaction().begin();
        Employee employee = dM.getEmployeeById(em,ee.getId());
        employee.setHrEmail(email);
        employee.setFirstName(first);
        employee.setLastName(last);
        em.persist(employee);
        em.getTransaction().commit();
        return employee;
    }

    private void updatePerson(EntityManager em, Employee ee,PSP psp){
        Person person = null;
        try{
            person = dP.getPersonByEe(em,ee,psp);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        if(person == null)
            return;
        em.getTransaction().begin();
        Person p = dM.getPersonById(em,person.getId());
        p.setEmail(ee.getEmail());
        p.setFirstName(ee.getFirstName());
        p.setLastName(ee.getLastName());
        em.persist(p);
        em.getTransaction().commit();
    }

    private void createReminder(EntityManager em, User user, String reminder){
        Date dateDue = Date.valueOf(LocalDate.now());
        dC.createReminder(em,reminder,user,dateDue);
    }
}

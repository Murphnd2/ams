package net.superiorstate.ams.previous.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ModifyContactForm", value = "/ModifyContactForm")
public class ModifyContactForm extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            modifyContact(request);
            goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
    private void modifyContact(HttpServletRequest request){
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        String firstName;
        String lastName;
        String email;
        long contactId;
        try{
            firstName = request.getParameter("contactFirst");
            lastName = request.getParameter("contactLast");
            email = request.getParameter("contactEmail");
            contactId = Long.parseLong(request.getParameter("pcId"));
        } catch (Exception e){
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person contact;
        try{
            contact = dM.getPersonById(em,contactId);
        } catch (Exception ex){
            em.close();
            return;
        }

        em.getTransaction().begin();
        String changes = "";
        String was;
        if(firstName !=null && !firstName.equals("") && (contact.getFirstName()==null || !contact.getFirstName().equalsIgnoreCase(firstName))){
            if(contact.getFirstName()==null)
                was = "";
            else
                was = contact.getFirstName();
            changes += "Changed first name from " + was.toUpperCase() + " to " + firstName.toUpperCase() + ". ";
            contact.setFirstName(firstName.toUpperCase());
            contact.setFullName(firstName.toUpperCase() + " " + contact.getLastName().toUpperCase());
        }
        if(lastName !=null && !lastName.equals("") && (contact.getLastName()==null || !contact.getLastName().equalsIgnoreCase(lastName))){
            if(contact.getLastName()==null)
                was = "";
            else
                was = contact.getLastName();
            changes += "Changed last name from " + was.toUpperCase() + " to " + lastName.toUpperCase() + ". ";
            contact.setLastName(lastName.toUpperCase());
            contact.setFullName(contact.getFirstName().toUpperCase() + " " + lastName.toUpperCase());
        }
        boolean emailChange = false;
        if(email !=null && dbEmail.isValidEmail(email) && (contact.getEmail()==null || !contact.getEmail().equalsIgnoreCase(email))){
            if(contact.getEmail()==null)
                was = "";
            else
                was = contact.getEmail();
            changes += "Changed email from " + was.toLowerCase() + " to " + email.toLowerCase() + ". ";
            contact.setEmail(email.toLowerCase());
            emailChange = true;
        }
        boolean addSummitTask = false;
        String theChange = "";
        if(!changes.equals("")){
            theChange = contact.getFullName() + ": " + changes;
            if(contact.getEmployee()!=null)
                addSummitTask = true;
            em.persist(contact);
        }
        em.getTransaction().commit();
        if(addSummitTask) {
            addSummitTasks(request, em, contact, theChange);
            if(emailChange)
                updateEmployeeEmailField(em,contact,contact.getEmail());
        }
        ViewSelectedActivity.setActivityView(request,em,a);
        em.close();
    }

    private void updateEmployeeEmailField(EntityManager em, Person p, String email){
        Employee e = dM.getEmployeeById(em,p.getEmployee().getId());
        em.getTransaction().begin();
        e.setHrEmail(email);
        em.persist(e);
        em.getTransaction().commit();
    }

    private void addSummitTasks(HttpServletRequest request,EntityManager em, Person contact, String changes){
        Person user = (Person) request.getSession().getAttribute("currentPerson");
        PSP psp = dM.getPspById(em,4L);

        em.getTransaction().begin();
        Task task = new Task();
        task.setPsp(psp);
        task.setHasAutomation(false);
        task.setReUsable(false);
        task.setDescription(changes);
        em.persist(task);
        em.getTransaction().commit();

        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setTask(task);
        toDo.setComplete(false);
        toDo.setSortOrder(10);
        em.persist(toDo);
        em.getTransaction().commit();

        List<ToDo> toDoList = new ArrayList<>();
        toDoList.add(toDo);

        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setAssignedTo(user);
        c.setPrimaryContact(user);
        c.setLoggedBy(user);
        c.setFullName(changes);
        c.setToDoList(toDoList);
        c.setComplete(false);
        c.setDueDate(Date.valueOf(LocalDate.now()));
        em.persist(c);
        em.getTransaction().commit();

        em.getTransaction().begin();
        toDo.setCheckList(c);
        em.persist(toDo);
        em.getTransaction().commit();
    }
}

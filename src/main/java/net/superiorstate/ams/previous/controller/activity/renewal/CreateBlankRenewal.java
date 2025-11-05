package net.superiorstate.ams.previous.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

@WebServlet(name = "CreateBlankRenewal", value = "/CreateBlankRenewal")
public class CreateBlankRenewal extends HttpServlet {
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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        int erId = Integer.parseInt(request.getParameter("employerId"));
        if(erId==0)
            return;

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Employer employer = dM.getEmployerById(em,erId);
        Person p = (Person) request.getSession().getAttribute("currentPerson");

        //Create a New Renewal
        em.getTransaction().begin();
        Renewal r = new Renewal();
        r.setComplete(false);
        r.setDueDate(Date.valueOf(LocalDate.now().plusDays(21)));
        r.setEmployer(employer);
        r.setLoggedBy(p);
        r.setAssignedTo(p);
        r.setFullName(employer.getEmployerName());
        em.persist(r);
        em.getTransaction().commit();

        //Create CheckList
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setRenewal(r);
        c.setDueDate(Date.valueOf(LocalDate.now().plusDays(21)));
        c.setFullName(employer.getEmployerName() + " Renewal Checklist");
        c.setAssignedTo(r);
        c.setComplete(false);
        c.setLoggedBy(p);
        em.persist(c);
        em.getTransaction().commit();

        //Update Renewal
        em.getTransaction().begin();
        r.setCheckList(c);
        em.persist(r);
        em.getTransaction().commit();

        //Create Task
        em.getTransaction().begin();
        Task task = new Task();
        task.setDescription("Default");
        task.setReUsable(false);
        task.setHasAutomation(false);
        task.setPsp(dM.getPspById(em,4L));
        em.persist(task);
        em.getTransaction().commit();

        //Create Todo
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setTask(task);
        toDo.setCheckList(c);
        toDo.setSortOrder(100);
        toDo.setComplete(true);
        em.persist(toDo);
        em.getTransaction().commit();

        em.getTransaction().begin();
        c.getToDoList().add(toDo);
        em.persist(c);
        em.getTransaction().commit();

        ViewSelectedActivity.setActivityView(request,em,r);

        em.close();
    }
}

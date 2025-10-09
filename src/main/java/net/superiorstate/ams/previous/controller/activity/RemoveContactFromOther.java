package net.superiorstate.ams.previous.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "RemoveContactFromOther", value = "/RemoveContactFromOther")
public class RemoveContactFromOther extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeContact(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
    private void removeContact(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        request.getSession().setAttribute("lastTab",1);
        long personId = Long.parseLong(request.getParameter("contactIdToRemove"));
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        Person personToRemove = dM.getPersonById(em,personId);
        Activity activity = dM.getActivityById(em,a.getId());
        em.getTransaction().begin();
        assert activity != null;
        activity.removeAssigneeContact(personToRemove);
        em.persist(activity);
        em.getTransaction().commit();
        ViewSelectedActivity.setActivityView(request,em,activity);
        em.close();
    }
}

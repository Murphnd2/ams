package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "RemoveSetupContact", value = "/RemoveSetupContact")
public class RemoveSetupContact extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeContact(request,response);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeContact(request,response);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void removeContact(HttpServletRequest request,HttpServletResponse response){
        Setup setup = (Setup) request.getSession().getAttribute("currentActivity");
        String idString = request.getParameter("setupContactList");
        System.out.println("String: " + idString);
        Long id = Long.parseLong(idString);
        System.out.println("ID: "+id);
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Person p = dM.getPersonById(em,id);
        System.out.println("Person ID: "+p.getId());
        Setup s = dM.getSetupById(em,setup.getId());
        System.out.println("Setup ID: "+s.getId());
        s.removeContact(p);
        em.persist(s);
        em.persist(p);
        em.getTransaction().commit();
        ViewSelectedActivity.setActivityView(request,em,s);
        em.close();

    }
}

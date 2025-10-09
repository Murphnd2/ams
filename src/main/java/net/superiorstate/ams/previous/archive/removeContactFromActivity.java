package net.superiorstate.ams.previous.archive;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "removeContactFromActivity", value = "/removeContactFromActivity")
public class removeContactFromActivity extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeContact(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("goActivityDetail");
        dispatcher.forward(request,response);
    }
    private void removeContact(HttpServletRequest request){
        long personId;
        try{
            personId = Long.parseLong(request.getParameter("contactIdToRemove"));
        } catch (Exception e){
            return;
        }

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        if(sVar==null)
            return;

        Activity a = sVar.getCurrentActivity();
        if(a==null)
            return;

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Person personToRemove = dM.getPersonById(em,personId);
        Activity activity = dM.getActivityById(em,a.getId());

        if(activity!=null && personToRemove!=null) {
            em.getTransaction().begin();
            activity.removeAssigneeContact(personToRemove);
            em.persist(activity);
            em.getTransaction().commit();
            em.refresh(activity);
        }

        sVar.refreshContactListFromCurrentActivity(em);
        request.getSession().setAttribute("sVar",sVar);

        em.close();
    }
}

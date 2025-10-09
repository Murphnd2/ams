package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "RemoveContact25", value = "/RemoveContact25")
public class RemoveContact25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeContact(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }
    private void removeContact(HttpServletRequest request){
        long personId;
        try{
            personId = Long.parseLong(request.getParameter("contactIdToRemove"));
        } catch (Exception e){
            return;
        }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Person personToRemove = dM.getPersonById(em,personId);
        Activity activity = dM.getActivityById(em,local.getCurrentActivity().getActivity().getId());

        if(activity!=null && personToRemove!=null) {
            em.getTransaction().begin();
            activity.removeAssigneeContact(personToRemove);
            em.persist(activity);
            em.getTransaction().commit();
            em.refresh(activity);
        }
        local.respondToActivityUpdate(em,"REMOVE_CONTACT",personToRemove);
        request.getSession().setAttribute("local",local);

        em.close();
    }
}

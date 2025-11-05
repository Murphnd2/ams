package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.util.ArrayList;

@WebServlet(name = "ViewHome25", value = "/ViewHome25")
public class ViewHome25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    private void processData(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if(local.getCurrentActivity().isReFilterOnExit()){
            local.setFilteredActivityList(local.filterActivityListing());
            System.out.println("-----I REFILTERED !!!!------------------------------------------------------------");
        }
        if (local.getCurrentActivity()!=null && local.getCurrentActivity().isReFilterOnExit()) {
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(null);
        } else if(local.getCurrentActivity()!=null && local.getCurrentActivity().getActivity()!=null){
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(new ArrayList<>());
        }
        local.getCurrentEmail().setRecipientList(new ArrayList<>());
        local.getCurrentEmail().setAttachments(new ArrayList<>());
        local.getCurrentEmail().setSubject("");
        local.getCurrentEmail().setBody("");

        // File: src/main/java/net/superiorstate/ams/controller/ViewHome25.java
        if (local.getCurrentActivity().isReFilterOnExit()) {
            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                for (ToDoOut25 t : local.getCurrentActivity().getToDoList()) {
                    if (t.isComplete() != t.wasComplete() && t.getToDo().getId() != null) {
                        ToDo managed = em.find(ToDo.class, t.getToDo().getId());
                        if (managed != null) {
                            managed.setComplete(t.isComplete());
                            managed.setCompletedBy(t.getToDo().getCompletedBy());
                            managed.setDateCompleted(t.getToDo().getDateCompleted());
                        }
                    }
                }
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                e.printStackTrace();
            } finally {
                if (em.isOpen()) em.close();
            }
        }
        request.getSession().setAttribute("local", local);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome25.jsp");
        dispatcher.forward(request,response);
    }
}

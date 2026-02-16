package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "ChangeDueDate25", value = "/ChangeDueDate25")
public class ChangeDueDate25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToView(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToView(request, response);
    }

    private void forwardToView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private void handleRequest(HttpServletRequest request) {
        Date newDueDate = parseDate(request.getParameter("newDueDate"));
        if (newDueDate == null) return;

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Activity currentActivity = local.getCurrentActivity().getActivity();

            if (newDueDate.equals(currentActivity.getDueDate())) return;

            Activity activity = dM.getActivityById(em, currentActivity.getId());
            if (activity == null) return;

            em.getTransaction().begin();
            activity.setDueDate(newDueDate);
            em.persist(activity);
            em.getTransaction().commit();

            em.refresh(activity);
            local.respondToActivityUpdate(em, "DATE", newDueDate);
            request.getSession().setAttribute("local", local);

        } finally {
            em.close();
        }
    }

    private Date parseDate(String dateString) {
        try {
            return Date.valueOf(dateString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}


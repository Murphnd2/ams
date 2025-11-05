package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "ChangeActivityDueDate", value = "/ChangeActivityDueDate")
public class ChangeActivityDueDate extends HttpServlet {
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

    private void changeView(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Activity selectedActivity = (Activity) request.getSession().getAttribute("currentActivity");
        Date newDate = Date.valueOf(request.getParameter("newDueDate"));
        em.getTransaction().begin();
        Activity a = dM.getActivityById(em,selectedActivity.getId());
        assert a != null;
        a.setDueDate(newDate);
        em.persist(a);
        em.getTransaction().commit();
        request.getSession().setAttribute("currentActivity", a);
        ViewSelectedActivity.setActivityView(request,em,a);
        em.close();
    }
}

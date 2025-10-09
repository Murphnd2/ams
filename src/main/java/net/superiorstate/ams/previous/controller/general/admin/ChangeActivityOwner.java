package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "ChangeActivityOwner", value = "/ChangeActivityOwner")
public class ChangeActivityOwner extends HttpServlet {
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
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        request.getSession().setAttribute("lastTab",4);
        Activity activity = (Activity) request.getSession().getAttribute("currentActivity");
        Long id = Long.parseLong(request.getParameter("userList"));
        Person assignTo = dM.getPersonById(em,id);
        em.getTransaction().begin();
        Activity a = dM.getActivityById(em, activity.getId());
        assert a != null;
        a.setAssignedTo(assignTo);
        em.persist(a);
        em.getTransaction().commit();
        em.close();
    }
}

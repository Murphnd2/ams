package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Automation;

import java.io.IOException;

@WebServlet(name = "CreateAutoEmail", value = "/CreateAutoEmail")
public class CreateAutoEmail extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void generateAutomation(HttpServletRequest request){
        int automationId = Integer.parseInt(request.getParameter("aeId").toString().trim());
        Activity activity = (Activity) request.getSession().getAttribute("currentActivity");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Automation a = new Automation();
        a.setId(automationId);
        a.setContent(request.getParameter("autoContent"));
        a.setAutomationName(request.getParameter("autoName"));
        em.persist(a);
        em.getTransaction().commit();
        em.close();
    }
}

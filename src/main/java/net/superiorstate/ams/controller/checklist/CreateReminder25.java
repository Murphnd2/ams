package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityFactory;
import net.superiorstate.ams.model.activity.checklist.CheckList;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "CreateReminder25", value = "/CreateReminder25")
public class CreateReminder25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addReminder(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isBpo = Boolean.TRUE.equals(request.getSession().getAttribute("isBpo"));
        boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));
        boolean isBpoUser = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoUser"));
        if (isBpo || isBpoAdmin || isBpoUser) {
            response.sendRedirect("BpoHome");
        } else {
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
            dispatcher.forward(request, response);
        }
    }

    private void addReminder(HttpServletRequest request){
        String reminderNames = request.getParameter("reminderName");
        Date dueDate = Date.valueOf(request.getParameter("reminderDate"));
        if(reminderNames==null || dueDate==null)
            return;

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        CheckList c = EntityFactory.createReminder(em,reminderNames,local.getCurrentUser(),dueDate);
        em.refresh(c);
        local.respondToActivityUpdate(em,"CHECK_REMINDER",c);

        request.getSession().setAttribute("local",local);

        em.close();
    }
}

package net.superiorstate.ams.previous.controller.activity.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "AddReminder", value = "/AddReminder")
public class AddReminder extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addReminder(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ResetAdminView");
        dispatcher.forward(request,response);
    }

    private void addReminder(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        User user = (User) request.getSession().getAttribute("currentUser");
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        String reminderName = request.getParameter("reminderName");
        Date dateDue = Date.valueOf(request.getParameter("reminderDate"));
        dC.createReminder(em,reminderName,user,dateDue);
        em.close();
    }
}

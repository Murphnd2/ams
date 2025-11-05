package net.superiorstate.ams.previous.archive.checklistDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "createReminder", value = "/createReminder")
public class createReminder extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addReminder(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("goPspHome");
        dispatcher.forward(request,response);
    }

    private void addReminder(HttpServletRequest request){
        String reminderName = null;
        Date dueDate = null;
        try{
            reminderName = request.getParameter("reminderName");
            dueDate = Date.valueOf(request.getParameter("reminderDate"));
        } catch (Exception e){
            return;
        }
        if(reminderName==null || dueDate==null)
            return;

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        if(sVar==null || sVar.getCurrentUser()==null)
            return;

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        CheckList c = dC.createReminder(em,reminderName,sVar.getCurrentUser(),dueDate);
        sVar.setCurrentActivity(null);
        sVar.setCurrentCheckList(null);
        sVar.refreshOpenChecklists(em);

        request.getSession().setAttribute("sVar",sVar);

        em.close();
    }
}

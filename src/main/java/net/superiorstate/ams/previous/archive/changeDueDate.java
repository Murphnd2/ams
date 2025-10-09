package net.superiorstate.ams.previous.archive;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "changeDueDate", value = "/changeDueDate")
public class changeDueDate extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeActivityDueDate(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeActivityDueDate(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("goPspHome");
        dispatcher.forward(request,response);
    }

    private void changeActivityDueDate(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        Date newDate = Date.valueOf(request.getParameter("newDueDate"));
        if(sVar.getCurrentActivity().getDueDate().equals(newDate)) {
            em.close();
            return;
        }
        Activity a = dM.getActivityById(em,sVar.getCurrentActivity().getId());
        if(a==null) {
            em.close();
            return;
        }
        em.getTransaction().begin();
        a.setDueDate(newDate);
        em.persist(a);
        em.getTransaction().commit();
        em.refresh(a);

        sVar.refreshThisActivity(em,a,sVar);

        request.getSession().setAttribute("sVar",sVar);
        em.close();
    }

}

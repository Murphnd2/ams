package net.superiorstate.ams.previous.archive;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "changeOwner", value = "/changeOwner")
public class changeOwner extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeOwnership(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeOwnership(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("goPspHome");
        dispatcher.forward(request,response);
    }

    private void changeOwnership(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        Person newOwner = dM.getPersonById(em,Long.parseLong(request.getParameter("userList")));
        if(sVar.getCurrentActivity().getAssignedTo().getId().equals(newOwner.getId())) {
            em.close();
            return;
        }
        Activity a = dM.getActivityById(em,sVar.getCurrentActivity().getId());
        if(a==null) {
            em.close();
            return;
        }
        em.getTransaction().begin();
        a.setAssignedTo(newOwner);
        em.persist(a);
        em.getTransaction().commit();
        em.refresh(a);

        sVar.refreshThisActivity(em,a,sVar);

        request.getSession().setAttribute("sVar",sVar);
        em.close();
    }
}

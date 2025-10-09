package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

@WebServlet(name = "AddHsaToPop", value = "/AddHsaToPop")
public class AddHsaToPop extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendQuickAction(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendQuickAction(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void sendQuickAction(HttpServletRequest request) throws MessagingException {
        addNextTask(request,2158L,225);
        addNextTask(request,1465L,230);
        addNextTask(request,1651L,235);
        addNextTask(request,1652L,240);
        addNextTask(request,14671L,245);
    }

    private void addNextTask(HttpServletRequest request, Long tid, int sort){
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Renewal r = (Renewal) local.getCurrentActivity().getActivity();
        Person p = local.getCurrentPerson();
        ToDo t= Q.addNextTask1(em,r,p,tid,sort);
        local.respondToActivityUpdate(em,"TD_ADD",t);
        em.close();
    }

    private void closeThisTask(EntityManager em, Renewal r, Person p, Long taskId){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId AND t.task.id = :tId");
        q.setParameter("tId",taskId);
        q.setParameter("cId",r.getCheckList().getId());
        ToDo test;
        try{
            test =(ToDo) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            test = null;
        }
        if(test==null)
            return;
        em.getTransaction().begin();
        test.setDateCompleted(Date.valueOf(LocalDate.now()));
        test.setComplete(true);
        test.setCompletedBy(p);
        em.persist(test);
        em.getTransaction().commit();
    }

}

package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;

import jakarta.mail.MessagingException;
import java.io.IOException;

@WebServlet(name = "ConfirmAddHSA", value = "/ConfirmAddHSA")
public class ConfirmAddHSA extends HttpServlet {
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
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        Person p = (Person) request.getSession().getAttribute("currentPerson");

        String eSubject = "Decision Needed: Add HSA to 125 Plan?";
        String eMessage = Q.FEEDBACK_HSA_SINCE_NEW_WANT_TO_EXPAND  + StdAuto.userSignature(p);
        Email email = StdAuto.createEmail(request,em,a,eSubject,eMessage,1,p);
        Email emailToSend = dM.getEmailById(em,email.getId());
        dbEmail.sendEmail(emailToSend,em);

        Renewal r = (Renewal)a;

        addNextTask(request,em,r,p,1464L);
        ViewSelectedActivity.setActivityView(request,em,r);
        em.close();
    }

    private void addNextTask(HttpServletRequest request, EntityManager em, Renewal r, Person p, long taskId){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.task.id = :tId AND t.checkList.id = :cId");
        q.setParameter("tId",taskId);
        q.setParameter("cId",r.getCheckList().getId());
        ToDo test;
        try{
            test =(ToDo) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            test = null;
        }

        em.getTransaction().begin();
        if(test!=null){
            ToDo t1 = dM.getToDoById(em,test.getId());
            t1.setComplete(false);
            t1.setDateCompleted(null);
            em.persist(t1);
        } else {
            Task t = dM.getTaskById(em,taskId);
            CheckList c = dM.getCheckListById(em,r.getCheckList().getId());
            ToDo toDo = new ToDo();
            toDo.setTask(t);
            toDo.setComplete(false);
            toDo.setDateCompleted(null);
            toDo.setCheckList(c);
            toDo.setSortOrder(220);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            CheckList checkList = dM.getCheckListById(em,c.getId());
            ToDo t1 = dM.getToDoById(em,toDo.getId());
            checkList.getToDoList().add(t1);
            em.persist(checkList);
        }
        em.getTransaction().commit();

    }
}

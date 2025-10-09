package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "DeleteSingleItemRenewal", value = "/DeleteSingleItemRenewal")
public class DeleteSingleItemRenewal extends HttpServlet {
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
        Renewal renewal = (Renewal)request.getSession().getAttribute("currentActivity");
        Person person = (Person) request.getSession().getAttribute("currentPerson");
        Query q = em.createQuery("SELECT ri FROM RenewalItem ri WHERE ri.renewal.id = :id");
        q.setParameter("id",renewal.getId());
        List<RenewalItem> renewalItemList = null;
        try{
            renewalItemList = (List<RenewalItem>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
        }
        if(renewalItemList!=null){
            String riName = renewalItemList.get(0).getBenefit().getPlanName();
            for(RenewalItem ri: renewalItemList){
                //REVERT the next renewal date for this benefit
                em.getTransaction().begin();
                Benefit b = dM.getBenefitById(em,ri.getBenefit().getId());
                b.setNextRenewalDue(ri.getDateFor());
                b.setLastRenewed(null);
                em.persist(b);
                em.getTransaction().commit();

                em.getTransaction().begin();
                Renewal r = dM.getRenewalById(em, renewal.getId());
                r.getRenewalItemList().remove(ri);
                em.persist(r);
                em.getTransaction().commit();

                em.getTransaction().begin();
                RenewalItem renewalItem = dM.getRenewalItemById(em,ri.getId());
                em.remove(ri);
                em.getTransaction().commit();
            }
            em.getTransaction().begin();
            CheckList c = dM.getCheckListById(em,renewal.getCheckList().getId());
            c.setComplete(true);
            c.setCompletedBy(person);
            c.setDateCompleted(Date.valueOf(LocalDate.now()));
            em.persist(c);
            em.getTransaction().commit();

            //Log a note in the renewal of the benefit being removed
            em.getTransaction().begin();
            Note n = new Note();
            n.setDateGenerated(Date.valueOf(LocalDate.now()));
            n.setActivity(renewal);
            n.setCreatedBy(person);
            n.setReasonCreated(dM.getReasonById(em,1));
            n.setStatus(dM.getActivityStatusById(em,2));
            String whatHappened = riName + " was removed from this renewal by " + person.getFullNameFirstLast();
            n.setDetail(whatHappened);
            em.persist(n);
            em.getTransaction().commit();
            //Close Activity
            em.getTransaction().begin();
            Renewal r1 = dM.getRenewalById(em, renewal.getId());
            r1.setComplete(true);
            r1.setDateCompleted(Date.valueOf(LocalDate.now()));
            r1.setCompletedBy(person);
            em.persist(r1);
            em.getTransaction().commit();
        }
        em.close();
    }
}

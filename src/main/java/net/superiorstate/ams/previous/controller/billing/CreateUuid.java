package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.billing.BillingGrid;
import net.superiorstate.ams.previous.model.billing.BillingLink;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "CreateUuid", value = "/CreateUuid")
public class CreateUuid extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        generateGuidItems();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        generateGuidItems();
    }

    private void generateGuidItems(){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        List<BillingGrid> billingGridList = getBillingGrid(em);
        for(BillingGrid bg:billingGridList){
            if(billingLinkExists(em,bg))
                continue;
            String billingId = bg.getBillingMonth().getFullDate().toString() + "-" + bg.getEmployer().getId();
            em.getTransaction().begin();
            BillingLink bl = new BillingLink();
            bl.setBillingId(billingId);
            bl.setUniqueId(UUID.randomUUID().toString());
            bl.setEmployer(bg.getEmployer());
            bl.setBillingMonth(bg.getBillingMonth());
            em.persist(bl);
            em.getTransaction().commit();
        }
        em.close();
    }

    private boolean billingLinkExists(EntityManager em, BillingGrid bg){
        Query q = em.createQuery("SELECT bl FROM BillingLink bl WHERE bl.employer.id = :eId AND bl.billingMonth.monthId = :mId");
        q.setParameter("eId",bg.getEmployer().getId());
        q.setParameter("mId",bg.getBillingMonth().getMonthId());
        BillingLink bl;
        try{
            bl = (BillingLink) q.getSingleResult();
        } catch (NoResultException e){
            return false;
        }
        return bl.getUniqueId() != null && !bl.getUniqueId().equals("");

    }

    private List<BillingGrid> getBillingGrid(EntityManager em){
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg order by bg.billingMonth.monthId, bg.employer.id");
        return (List<BillingGrid>) q.getResultList();
    }
}

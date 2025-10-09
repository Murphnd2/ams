package net.superiorstate.ams.previous.data.summit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.model.billing.BillingGrid;
import net.superiorstate.ams.previous.model.billing.BillingLink;
import net.superiorstate.ams.previous.model.billing.BillingMonth;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.util.List;

public abstract class bill {

    public static String getLastGuid(EntityManager em, Employer er){
        Query q = em.createQuery("SELECT bl FROM BillingLink bl WHERE bl.employer.id = :id order by bl.billingMonth.monthId desc ");
        q.setParameter("id",er.getId());
        List<BillingLink> billingLinkList;
        try{
            billingLinkList = (List<BillingLink>) q.getResultList();
        } catch (NoResultException e){
            return "";
        }
        if(billingLinkList.size()==0)
            return "";
        return billingLinkList.get(0).getUniqueId().toString();
    }

    public static BillingLink getBillingLinkByGuid(EntityManager em, String guid){
        Query q = em.createQuery("SELECT bl FROM BillingLink bl WHERE bl.uniqueId = :uid");
        q.setParameter("uid",guid);
        BillingLink bl;
        try{
            bl = (BillingLink) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return bl;
    }

    public static Employer getEmployerByGuid(EntityManager em, String guid){
        BillingLink bl = getBillingLinkByGuid(em,guid);
        if(bl==null)
            return null;
        return bl.getEmployer();
    }


    public static BillingMonth getBillingMonthByGuid(EntityManager em, String guid){
        BillingLink bl = getBillingLinkByGuid(em,guid);
        if(bl==null)
            return null;
        return bl.getBillingMonth();
    }

    public static List<BillingGrid> getEmployerMonthlyBilling(EntityManager em, Employer er, BillingMonth bm){
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.billingMonth.monthId = :mId AND bg.employer.id = :eId order by bg.employee.lastName,bg.employee.firstName");
        q.setParameter("mId",bm.getMonthId());
        q.setParameter("eId",er.getId());
        List<BillingGrid> billingGridList;
        try{
            billingGridList= (List<BillingGrid>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return billingGridList;
    }

    public static List<BillingGrid> getEmployerMonthlyBilling(EntityManager em, String guid){
        Employer er = getEmployerByGuid(em,guid);
        BillingMonth bm = getBillingMonthByGuid(em,guid);
        if(er==null || bm==null)
            return null;
        return getEmployerMonthlyBilling(em,er,bm);
    }

    public static List<BillingMonth> getAllBillingMonths(EntityManager em){
        Query q = em.createQuery("SELECT bm FROM BillingMonth bm order by bm.monthId desc ");
        return (List<BillingMonth>) q.getResultList();
    }

    public static List<BillingGrid> getEmployerBillings(EntityManager em, Employer er){
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.employer.id = :id order by bg.gridId, bg.employee.lastName, bg.employee.firstName");
        q.setParameter("id",er.getId());
        List<BillingGrid> billingGridList;
        try{
            billingGridList = (List<BillingGrid>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(billingGridList.size()==0)
            return null;
        return billingGridList;
    }
}

package net.superiorstate.ams.previous.data.misc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.model.billing.BillingItem;
import net.superiorstate.ams.previous.model.billing.BillingLink;
import net.superiorstate.ams.previous.model.summit.imports.sEmployer;
import net.superiorstate.ams.previous.model.summit.archive.PlanType;

import java.util.ArrayList;
import java.util.List;

public abstract class dS1 {

    public static BillingLink getBillingLinkByGUID(EntityManager em, String guid){
        BillingLink billingLink;
        Query q = em.createQuery("SELECT bl FROM BillingLink bl INNER JOIN FETCH bl.sEmployer so WHERE bl.uniqueId = :guid");
        q.setParameter("guid",guid);
        try{
            billingLink = (BillingLink) q.getSingleResult();
        } catch (NoResultException e) {
            e.printStackTrace();
            billingLink = new BillingLink();
        }
        return billingLink;
    }

    public static List<BillingLink> getBillingMonthsByOrganizationId(EntityManager em, int organizationId){
        List<BillingLink> billingLinkList;
        Query q = em.createQuery("SELECT bl FROM BillingLink bl WHERE bl.sEmployer.organizationId = :id");
        q.setParameter("id",organizationId);
        try{
            billingLinkList = (List<BillingLink>) q.getResultList();
        } catch (NoResultException e){
            billingLinkList = new ArrayList<>();
        }
        return billingLinkList;
    }

    public static List<BillingItem> getBillingDetailForMonth(EntityManager em, String guid){
        return getBillingDetailForMonth(em,getBillingLinkByGUID(em,guid).getSummitOrganization().getOrganizationId());
    }

    public static List<BillingItem> getBillingDetailForMonth(EntityManager em, int organizationId){
        List<BillingItem> monthlyBillingDetail;
        Query q = em.createQuery("SELECT bi FROM BillingItem bi WHERE bi.employerId = :id");
        q.setParameter("id",organizationId);
        try{
            monthlyBillingDetail = q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            monthlyBillingDetail = new ArrayList<>();
        }
        return monthlyBillingDetail;
    }


    public static List<BillingItem> getBillingDetailForMonth(EntityManager em, sEmployer sEmployer){
        return getBillingDetailForMonth(em, sEmployer.getOrganizationId());
    }

    public static sEmployer getOrganizationByEmployerId(EntityManager em, int id){
        sEmployer sEmployer;
        Query q = em.createQuery("SELECT o FROM sEmployer o WHERE o.employerId = :id");
        q.setParameter("id",id);
        try{
            sEmployer = (sEmployer) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            sEmployer = new sEmployer();
        }
        return sEmployer;
    }

    public static PlanType getPlanTypeByPlanTypeName(EntityManager em, String name){
        PlanType PlanType;
        Query q = em.createQuery("SELECT pt FROM PlanType pt WHERE pt.planTypeName = :name");
        q.setParameter("name",name);
        try{
            PlanType = (PlanType) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            PlanType = new PlanType();
        }
        return PlanType;
    }

}

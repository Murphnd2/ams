package net.superiorstate.ams.previous.data.summit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.model.billing.BillingGrid;
import net.superiorstate.ams.previous.model.billing.BillingMonth;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.imports.HsaAccount;
import net.superiorstate.ams.previous.model.summit.imports.HsaEe;
import net.superiorstate.ams.previous.model.summit.imports.HsaEr;

import java.util.List;

public abstract class dH {

    public static void fillHsaBillingGrid(EntityManager em, BillingMonth bm){
        List<HsaAccount> activeAccounts = getActiveHsas(em);
        assert activeAccounts != null;
        for(HsaAccount a:activeAccounts){
            HsaEe hsaEe = getHsaEeByAccount(em,a);
            assert hsaEe != null;
            if(hsaEe.getEmployee()==null)
                return;
            if(!hsaEe.getHsaEr().isBilledDirect()){
                BillingGrid billingGrid = getBillingGridForEe(em,hsaEe,bm);
                if(billingGrid==null){
                    createBillingGridForEe(em,hsaEe,bm);
                } else {
                    em.getTransaction().begin();
                    billingGrid.setHsa(true);
                    em.persist(billingGrid);
                    em.getTransaction().commit();
                }
            }

        }
    }
    private static void createBillingGridForEe(EntityManager em, HsaEe hsaEe, BillingMonth bm){
        String eeIdString;
        int eeId;
        if(hsaEe.getEmployee().getId()<0){
            eeId = -1*hsaEe.getEmployee().getId();
            eeIdString = "N"+eeId;
        } else {
            eeId = hsaEe.getEmployee().getId();
            eeIdString = "" + eeId;
        }
        String gridId = bm.getFullDate().toString()+"-"+eeIdString;
        em.getTransaction().begin();
        BillingGrid bg = new BillingGrid();
        bg.setGridId(gridId);
        bg.setCobra(false);
        bg.setCurrentStatus("TBD");
        bg.setDirect(false);
        bg.setDualPlan(false);
        bg.setFlexSpend(false);
        bg.setHealthReimb(false);
        bg.setHsa(true);
        bg.setLsa(false);
        bg.setRetiree(false);
        bg.setTransit(false);
        bg.setBillingMonth(bm);
        bg.setEmployee(hsaEe.getEmployee());
        bg.setEmployer(hsaEe.getHsaEr().getEmployer());
        em.persist(bg);
        em.getTransaction().commit();
    }
    private static BillingGrid getBillingGridForEe(EntityManager em, HsaEe hsaEe, BillingMonth bm){
        String eeIdString;
        int eeId;
        if(hsaEe.getEmployee().getId()<0){
            eeId = -1*hsaEe.getEmployee().getId();
            eeIdString = "N"+eeId;
        } else {
            eeId = hsaEe.getEmployee().getId();
            eeIdString = "" + eeId;
        }
        String gridId = bm.getFullDate().toString()+"-"+eeIdString;
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.gridId = :gridId");
        q.setParameter("gridId",gridId);
        BillingGrid billingGrid;
        try{
            billingGrid = (BillingGrid) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return billingGrid;
    }
    private static List<HsaAccount> getActiveHsas(EntityManager em){
        Query q = em.createQuery("SELECT h FROM HsaAccount h WHERE h.active = true");
        List<HsaAccount> hsaAccountList;
        try{
            hsaAccountList = (List<HsaAccount>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return hsaAccountList;
    }
    public static int getNextEeId(EntityManager em){
        Query q = em.createQuery("SELECT ee FROM Employee ee WHERE ee.id < 0 order by ee.id");
        List<Employee> employees = (List<Employee>) q.getResultList();
        return employees.get(0).getId() - 1;
    }
    public static HsaEr getHsaErByAccount(EntityManager em, HsaAccount a){
        Query q = em.createQuery("SELECT h FROM HsaEr h WHERE h.name = :name");
        q.setParameter("name",a.getEmployer());
        HsaEr hsaEr;
        try{
            hsaEr = (HsaEr) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return hsaEr;
    }
    public static HsaEe getHsaEeByAccount(EntityManager em, HsaAccount h){
        Query q= em.createQuery("SELECT h FROM HsaEe h WHERE h.hsaId = :id");
        q.setParameter("id",h.getHsaId());
        HsaEe hsaEe;
        try{
            hsaEe = (HsaEe) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return hsaEe;
    }
}

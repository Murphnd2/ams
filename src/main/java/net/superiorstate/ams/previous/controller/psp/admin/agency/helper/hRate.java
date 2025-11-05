package net.superiorstate.ams.previous.controller.psp.admin.agency.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.StarterData;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.agency.RateTable;

import java.util.ArrayList;
import java.util.List;

public abstract class hRate {
    public static Rate unlockRate(EntityManager em, Rate rate){
        //Copy and Create New Rate
        Rate newRate = new Rate();
        newRate.setDescription(rate.getDescription());
        newRate.setPsp(rate.getPsp());
        newRate.setSuppressed(false);
        em.getTransaction().begin();
        em.persist(newRate);
        em.getTransaction().commit();

        //Assign New Rate to the Same Agencies
        assignNewRateToSameAgencies(em,rate,newRate);
        //Copy RateTable from Old to New
        copyRateTable(em,rate,newRate);
        //Suppress Old Rate
        suppressRate(em,rate);

        return newRate;
    }
    public static boolean wasUsed(EntityManager em, Rate rate){
        boolean wasUsed = false;
        Query q = em.createQuery("SELECT p FROM Proposal p WHERE p.rate.id = :rate_id");
        q.setParameter("rate_id",rate.getId());
        List<Proposal> proposalList = (List<Proposal>) q.getResultList();
        if(proposalList.size()>0)
            wasUsed=true;
        return wasUsed;
    }
    public static void suppressRate(EntityManager em, Rate rate){
        Query q = em.createQuery("SELECT r FROM Rate r WHERE r.id = :rate_id");
        q.setParameter("rate_id",rate.getId());
        Rate theRate = (Rate) q.getSingleResult();
        theRate.setSuppressed(true);
        em.getTransaction().begin();
        em.persist(theRate);
        em.getTransaction().commit();
    }
    public static void copyRateTable(EntityManager em, Rate oldRate, Rate newRate){
        deleteExistingRates(em,newRate);
        Query q = em.createQuery("SELECT rt FROM RateTable rt WHERE rt.rate.id = :rate_id");
        q.setParameter("rate_id",oldRate.getId());
        List<RateTable> rateTableList = new ArrayList<>();
        rateTableList = (List<RateTable>) q.getResultList();
        for (RateTable rt: rateTableList){
            RateTable newRateTable = new RateTable();
            newRateTable.setRate(newRate);
            newRateTable.setModule(rt.getModule());
            newRateTable.setPriceItem(rt.getPriceItem());
            newRateTable.setPrice(rt.getPrice());
            em.getTransaction().begin();
            em.persist(newRateTable);
            em.getTransaction().commit();
        }
    }

    public static void fillStandardRates(EntityManager em, Rate rate){
        deleteExistingRates(em,rate);
        StarterData.developRateTable(em,rate);
    }

    public static void deleteExistingRates(EntityManager em, Rate rate){
        Query q = em.createQuery("SELECT rt FROM RateTable rt WHERE rt.rate.id = :rate_id");
        q.setParameter("rate_id",rate.getId());
        List<RateTable> existingRates = (List<RateTable>) q.getResultList();
        for(RateTable rt: existingRates){
            em.getTransaction().begin();
            em.remove(rt);
            em.getTransaction().commit();
        }
    }
    private static void assignNewRateToSameAgencies(EntityManager em, Rate oldRate, Rate newRate){
        List<Agency> agencyList = oldRate.getListOfAgenciesWithThisRate();
        for (Agency a:agencyList) {
            Agency agency = dG.getAgencyFull(em,a.getId());
            em.getTransaction().begin();
            agency.addRate(newRate);
            em.persist(agency);
            em.getTransaction().commit();
        }
    }
}

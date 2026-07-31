package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.util.BillingHelper;
import net.superiorstate.ams.model.billing.BillingMonth;

import java.util.Date;

public abstract class Biller {

    protected final EntityManager em;

    public Biller(EntityManager em) {
        this.em = em;
    }

    public abstract void run();
    public void clearMonthlyBilling() {
        em.getTransaction().begin();

        int monthId = resolveMonthId();
        if (monthId != -1) {
            clearBillingGrid(monthId);
            clearCoverageStatus();
        }

        em.getTransaction().commit();
    }

    protected int resolveMonthId() {
        // Runtime type here is java.sql.Date despite the java.util.Date declaration (BillingHelper.getMonthFor()
        // returns java.sql.Date). Calling .toInstant() on it throws UnsupportedOperationException at runtime
        // even though it compiles cleanly against java.util.Date — see T40. Use .toLocalDate() instead.
        Date monthFor = BillingHelper.getMonthFor();
        try {
            BillingMonth bm = em.createQuery("SELECT bm FROM BillingMonth bm WHERE bm.fullDate = :fullDate", BillingMonth.class)
                    .setParameter("fullDate", monthFor)
                    .getSingleResult();
            return bm.getMonthId();
        } catch (NoResultException e) {
            return -1;
        }
    }

    protected void clearBillingGrid(int monthId) {
        Query q = em.createQuery("DELETE FROM BillingGrid bg WHERE bg.billingMonth.monthId = :mId");
        q.setParameter("mId", monthId);
        q.executeUpdate();
    }

    protected void clearCoverageStatus() {
        Query q = em.createQuery("DELETE FROM CoverageStatus cs WHERE cs.monthFor = :mf");
        q.setParameter("mf", BillingHelper.getMonthFor());
        q.executeUpdate();
    }
}


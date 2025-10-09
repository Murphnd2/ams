package net.superiorstate.ams.previous.data.misc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import net.superiorstate.ams.previous.data.QueryPair;

import java.util.List;

public abstract class dGen {
    private static final String PERSISTENCE_UNIT_NAME = "default";
    private static void assignParameter(Query q, QueryPair qp){
        if(qp.getStringValue()!=""){
            q.setParameter(qp.getParameterName(),qp.getStringValue());
        } else if (qp.getIntValue()!=0) {
            q.setParameter(qp.getParameterName(),qp.getIntValue());
        } else {
            q.setParameter(qp.getParameterName(),qp.getLongValue());
        }
    }

    public static List<?> getList(String queryName, QueryPair qp){
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = emf.createEntityManager();
        Query q = em.createNamedQuery(queryName);
        assignParameter(q,qp);
        List<?> genericTypedList = q.getResultList();
        em.close();
        emf.close();
        return genericTypedList;
    }

    public static List<?> getList(String queryName, QueryPair qp1, QueryPair qp2){
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = emf.createEntityManager();
        Query q = em.createNamedQuery(queryName);
        assignParameter(q,qp1);
        assignParameter(q,qp2);
        List<?> genericTypedList = q.getResultList();
        em.close();
        emf.close();
        return genericTypedList;
    }
    public static Object getObject(String queryName, QueryPair qp1){
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = emf.createEntityManager();
        Query q = em.createNamedQuery(queryName);
        assignParameter(q,qp1);
        Object genericObject = q.getSingleResult();
        em.close();
        emf.close();
        return genericObject;
    }
    public static Object getObject(String queryName, QueryPair qp1, QueryPair qp2){
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = emf.createEntityManager();
        Query q = em.createNamedQuery(queryName);
        assignParameter(q,qp1);
        assignParameter(q,qp2);
        Object genericObject = q.getSingleResult();
        em.close();
        emf.close();
        return genericObject;
    }
}

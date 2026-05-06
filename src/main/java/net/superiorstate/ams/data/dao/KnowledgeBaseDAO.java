package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.general.KnowledgeBase;

import java.util.List;

public abstract class KnowledgeBaseDAO {

    public static List<KnowledgeBase> getAllActive(EntityManager em) {
        return em.createQuery(
                "SELECT kb FROM KnowledgeBase kb WHERE kb.active = true ORDER BY kb.sortOrder ASC",
                KnowledgeBase.class)
                .getResultList();
    }

    public static List<KnowledgeBase> getAll(EntityManager em) {
        return em.createQuery(
                "SELECT kb FROM KnowledgeBase kb ORDER BY kb.sortOrder ASC",
                KnowledgeBase.class)
                .getResultList();
    }

    public static KnowledgeBase getByKey(EntityManager em, String kbKey) {
        List<KnowledgeBase> results = em.createQuery(
                "SELECT kb FROM KnowledgeBase kb WHERE kb.kbKey = :kbKey",
                KnowledgeBase.class)
                .setParameter("kbKey", kbKey)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public static KnowledgeBase getById(EntityManager em, Long id) {
        return em.find(KnowledgeBase.class, id);
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.general.*;

import java.util.List;

public abstract class KnowledgeChunkDAO {

    // ── Read ──────────────────────────────────────────────────────

    public static List<KnowledgeChunk> getActiveByKb(EntityManager em, Long kbId) {
        return em.createQuery(
                "SELECT c FROM KnowledgeChunk c WHERE c.knowledgeBase.id = :kbId AND c.active = true " +
                "ORDER BY c.section ASC, c.title ASC",
                KnowledgeChunk.class)
                .setParameter("kbId", kbId)
                .getResultList();
    }

    public static List<KnowledgeChunk> getAllByKb(EntityManager em, Long kbId) {
        return em.createQuery(
                "SELECT c FROM KnowledgeChunk c WHERE c.knowledgeBase.id = :kbId " +
                "ORDER BY c.section ASC, c.title ASC",
                KnowledgeChunk.class)
                .setParameter("kbId", kbId)
                .getResultList();
    }

    public static KnowledgeChunk getById(EntityManager em, Long id) {
        return em.find(KnowledgeChunk.class, id);
    }

    public static List<KnowledgeChunk> getByChunkType(EntityManager em, KnowledgeChunkType chunkType) {
        return em.createQuery(
                "SELECT c FROM KnowledgeChunk c WHERE c.chunkType = :chunkType AND c.active = true",
                KnowledgeChunk.class)
                .setParameter("chunkType", chunkType)
                .getResultList();
    }

    public static List<KnowledgeChunk> getActiveAlwaysLoadChunks(EntityManager em) {
        return em.createQuery(
                "SELECT c FROM KnowledgeChunk c " +
                "WHERE c.knowledgeBase.reloadStrategy = :strategy AND c.active = true",
                KnowledgeChunk.class)
                .setParameter("strategy", KnowledgeBaseReloadStrategy.ALWAYS_LOAD)
                .getResultList();
    }

    // ── Write (each method manages its own transaction) ───────────

    public static KnowledgeChunk create(EntityManager em, KnowledgeChunk chunk, Person modifiedBy) {
        if (modifiedBy == null) throw new IllegalArgumentException("modifiedBy must not be null");
        try {
            chunk.setModifiedBy(modifiedBy);
            em.getTransaction().begin();
            em.persist(chunk);
            em.getTransaction().commit();
            return chunk;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    /**
     * Persists a new KnowledgeChunk WITHOUT opening or committing a transaction.
     * The caller must manage begin/commit/rollback.
     * Use this inside a caller-owned outer transaction (e.g. bulk import).
     */
    public static KnowledgeChunk createNoTx(EntityManager em, KnowledgeChunk chunk, Person modifiedBy) {
        if (modifiedBy == null) throw new IllegalArgumentException("modifiedBy must not be null");
        chunk.setModifiedBy(modifiedBy);
        em.persist(chunk);
        return chunk;
    }

    public static KnowledgeChunk update(EntityManager em, KnowledgeChunk updatedChunk,
                                        Person modifiedBy, String changeNote) {
        if (modifiedBy == null) throw new IllegalArgumentException("modifiedBy must not be null");
        try {
            em.getTransaction().begin();

            KnowledgeChunk existing = em.find(KnowledgeChunk.class, updatedChunk.getId());
            if (existing == null)
                throw new IllegalArgumentException("KnowledgeChunk not found: " + updatedChunk.getId());

            em.persist(buildHistory(existing, KnowledgeChunkChangeType.UPDATE, changeNote, modifiedBy));

            existing.setTitle(updatedChunk.getTitle());
            existing.setSection(updatedChunk.getSection());
            existing.setContent(updatedChunk.getContent());
            existing.setKeywords(updatedChunk.getKeywords());
            existing.setAccountType(updatedChunk.getAccountType());
            existing.setChunkType(updatedChunk.getChunkType());
            existing.setEffectiveStart(updatedChunk.getEffectiveStart());
            existing.setEffectiveEnd(updatedChunk.getEffectiveEnd());
            existing.setSourceCitation(updatedChunk.getSourceCitation());
            existing.setVisibility(updatedChunk.getVisibility());
            existing.setActive(updatedChunk.isActive());
            existing.setModifiedBy(modifiedBy);

            em.getTransaction().commit();
            return existing;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    public static void deactivate(EntityManager em, Long chunkId, Person modifiedBy, String changeNote) {
        if (modifiedBy == null) throw new IllegalArgumentException("modifiedBy must not be null");
        try {
            em.getTransaction().begin();

            KnowledgeChunk chunk = em.find(KnowledgeChunk.class, chunkId);
            if (chunk == null)
                throw new IllegalArgumentException("KnowledgeChunk not found: " + chunkId);

            em.persist(buildHistory(chunk, KnowledgeChunkChangeType.DEACTIVATE, changeNote, modifiedBy));

            chunk.setActive(false);
            chunk.setModifiedBy(modifiedBy);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    public static void activate(EntityManager em, Long chunkId, Person modifiedBy, String changeNote) {
        if (modifiedBy == null) throw new IllegalArgumentException("modifiedBy must not be null");
        try {
            em.getTransaction().begin();

            KnowledgeChunk chunk = em.find(KnowledgeChunk.class, chunkId);
            if (chunk == null)
                throw new IllegalArgumentException("KnowledgeChunk not found: " + chunkId);

            String note = (changeNote != null && !changeNote.isBlank()) ? changeNote : "Reactivated";
            em.persist(buildHistory(chunk, KnowledgeChunkChangeType.UPDATE, note, modifiedBy));

            chunk.setActive(true);
            chunk.setModifiedBy(modifiedBy);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    public static void hardDelete(EntityManager em, Long chunkId, Person modifiedBy, String changeNote) {
        if (modifiedBy == null) throw new IllegalArgumentException("modifiedBy must not be null");
        try {
            em.getTransaction().begin();

            KnowledgeChunk chunk = em.find(KnowledgeChunk.class, chunkId);
            if (chunk == null)
                throw new IllegalArgumentException("KnowledgeChunk not found: " + chunkId);

            em.persist(buildHistory(chunk, KnowledgeChunkChangeType.DELETE, changeNote, modifiedBy));
            em.remove(chunk);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    // ── History ───────────────────────────────────────────────────

    /**
     * Returns up to 10 history entries for the given chunk, newest first.
     * Uses LEFT JOIN FETCH on modifiedBy so the Person is eagerly loaded
     * before the EntityManager closes.
     */
    public static List<KnowledgeChunkHistory> getHistoryByChunkId(EntityManager em, Long chunkId) {
        return em.createQuery(
                "SELECT h FROM KnowledgeChunkHistory h LEFT JOIN FETCH h.modifiedBy " +
                "WHERE h.chunkId = :chunkId ORDER BY h.modifiedOn DESC",
                KnowledgeChunkHistory.class)
                .setParameter("chunkId", chunkId)
                .setMaxResults(10)
                .getResultList();
    }

    private static KnowledgeChunkHistory buildHistory(KnowledgeChunk chunk,
                                                       KnowledgeChunkChangeType changeType,
                                                       String changeNote,
                                                       Person modifiedBy) {
        KnowledgeChunkHistory h = new KnowledgeChunkHistory();
        h.setChunkId(chunk.getId());
        h.setTitleBefore(chunk.getTitle());
        h.setContentBefore(chunk.getContent());
        h.setKeywordsBefore(chunk.getKeywords());
        h.setActiveBefore(chunk.isActive());
        h.setChangeType(changeType);
        h.setChangeNote(changeNote);
        h.setModifiedBy(modifiedBy);
        return h;
    }
}

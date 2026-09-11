package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.Collections;
import java.util.List;

/**
 * T241 -- finds {@code Employer} rows by the Summit {@code CustomID}, stored string-faithful in
 * {@code employer.custom_id} (V102), for the Summit setup panel's employer link. Distinct from
 * {@code er_key}, which is int-typed and lossy for a composed key (T242) and is never queried here.
 */
public final class SummitEmployerLookupDAO {

    private SummitEmployerLookupDAO() {}

    /**
     * @return every {@code Employer} whose {@code customId} equals {@code customId} exactly, or an
     * empty list for a null/blank input or no match. More than one row means the same custom id
     * was assigned to more than one Summit employer -- the caller renders that as ambiguous rather
     * than picking one.
     */
    public static List<Employer> findByCustomId(EntityManager em, String customId) {
        if (customId == null || customId.isBlank()) return Collections.emptyList();

        TypedQuery<Employer> query = em.createQuery(
                "SELECT e FROM Employer e WHERE e.customId = :cid", Employer.class);
        query.setParameter("cid", customId);
        return query.getResultList();
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.CoverageTier;

import java.util.List;

/**
 * DAO for {@link CoverageTier} (V109). All methods are static; the class is abstract (not
 * instantiable), following {@link PayrollFrequencyDAO}'s convention.
 * <p>
 * <b>Not an authorization boundary.</b> This registry is employer-independent and
 * installation-wide, unconditional read like {@link PayrollFrequencyDAO}.
 * <p>
 * <b>No hardcoded row ids.</b> Callers ask by {@code code} — never by primary key literal.
 * <p>
 * No CRUD screen exists for this table yet; only the two lookups a future picker needs are
 * provided here.
 */
public abstract class CoverageTierDAO {

    private static final String JPQL_ACTIVE =
            "SELECT c FROM CoverageTier c WHERE c.active = true ORDER BY c.sortOrder, c.label";

    private static final String JPQL_BY_CODE =
            "SELECT c FROM CoverageTier c WHERE c.code = :code";

    /** The active rows, in display order — what a future picker offers. */
    public static List<CoverageTier> findActive(EntityManager em) {
        return em.createQuery(JPQL_ACTIVE, CoverageTier.class).getResultList();
    }

    /** One row by its unique {@code code}, or null. */
    public static CoverageTier findByCode(EntityManager em, String code) {
        if (code == null || code.isBlank()) return null;
        List<CoverageTier> found = em.createQuery(JPQL_BY_CODE, CoverageTier.class)
                .setParameter("code", code)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }
}

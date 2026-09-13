package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.PaycycleFrequencyAlias;

import java.util.List;

/**
 * DAO for {@link PaycycleFrequencyAlias} (V110). All methods are static; the class is abstract
 * (not instantiable), following {@link PayrollFrequencyDAO}'s convention.
 * <p>
 * <b>No hardcoded row ids.</b> Callers ask by {@code aliasKey} — never by primary key literal.
 * <p>
 * No CRUD screen exists for this table yet; only the one lookup a future filter needs is
 * provided here, plus the shared normalisation helper so a caller's key and the seed
 * migration's key are computed identically.
 */
public abstract class PaycycleFrequencyAliasDAO {

    private static final String JPQL_BY_ALIAS_KEY =
            "SELECT a FROM PaycycleFrequencyAlias a WHERE a.active = true AND a.aliasKey = :aliasKey";

    /**
     * The active alias whose {@code aliasKey} equals {@code normalize(rawText)}, or null when
     * nothing matches. A caller with no match must apply no filtering at all — never a partial
     * or wrong one — per this table's own documented contract.
     */
    public static PaycycleFrequencyAlias findByNormalizedKey(EntityManager em, String rawText) {
        String key = normalize(rawText);
        if (key == null) return null;
        List<PaycycleFrequencyAlias> found = em.createQuery(JPQL_BY_ALIAS_KEY, PaycycleFrequencyAlias.class)
                .setParameter("aliasKey", key)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * The one normalisation rule for {@code alias_key}: uppercase, then strip every character
     * that is not a letter or digit. Used here and by the V110 seed migration so both compute
     * the same key from the same text.
     *
     * @return the normalised key, or null when {@code rawText} is null or normalises to empty.
     */
    public static String normalize(String rawText) {
        if (rawText == null) return null;
        String normalized = rawText.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}

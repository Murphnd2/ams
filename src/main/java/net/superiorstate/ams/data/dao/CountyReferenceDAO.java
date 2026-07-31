package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.market.CountyReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DAO for {@link CountyReference}. All methods are static; the class is
 * abstract (not instantiable) following the convention of RateCacheDAO.
 */
public abstract class CountyReferenceDAO {

    private static final String JPQL_FIND_BY_FIPS =
            "SELECT c FROM CountyReference c WHERE c.countyFips = :countyFips";

    private static final String JPQL_LIST_BY_STATE =
            "SELECT c FROM CountyReference c WHERE c.state = :state ORDER BY c.countyName";

    private static final String JPQL_FIND_BY_FIPS_IN =
            "SELECT c FROM CountyReference c WHERE c.countyFips IN :countyFipsCodes ORDER BY c.countyName";

    /** @return the county, or null if no row exists for this FIPS. */
    public static CountyReference findByFips(EntityManager em, String countyFips) {
        if (countyFips == null || countyFips.isBlank()) {
            return null;
        }
        try {
            return em.createQuery(JPQL_FIND_BY_FIPS, CountyReference.class)
                    .setParameter("countyFips", countyFips)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** @return counties in the state, ordered by county name. Empty list if none. */
    public static List<CountyReference> listByState(EntityManager em, String state) {
        return em.createQuery(JPQL_LIST_BY_STATE, CountyReference.class)
                .setParameter("state", state)
                .getResultList();
    }

    /** @return counties matching any of the given FIPS codes, ordered by county name. Empty list if none. */
    public static List<CountyReference> findByFipsIn(EntityManager em, Collection<String> countyFipsCodes) {
        if (countyFipsCodes == null || countyFipsCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return em.createQuery(JPQL_FIND_BY_FIPS_IN, CountyReference.class)
                .setParameter("countyFipsCodes", countyFipsCodes)
                .getResultList()
                .stream()
                .collect(Collectors.toList());
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.ZipCounty;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO for {@link ZipCounty}. All methods are static; the class is abstract
 * (not instantiable) following the convention of {@code CountyReferenceDAO}
 * and {@code RateCacheDAO}.
 */
public abstract class ZipCountyDAO {

    /**
     * Ordered most-land-first so a crossing ZIP's candidates arrive in a stable,
     * useful order. {@code land_area_ratio} is nullable, and a null must sort
     * <i>last</i> rather than first — MySQL orders NULLs first on {@code DESC}
     * without the explicit {@code IS NULL} key, which would put the least-known
     * candidate at the top of a list an agent picks from. County FIPS breaks
     * remaining ties so the order is deterministic.
     */
    private static final String JPQL_FIND_BY_ZIP =
            "SELECT z FROM ZipCounty z WHERE z.zip = :zip "
                    + "ORDER BY CASE WHEN z.landAreaRatio IS NULL THEN 1 ELSE 0 END, "
                    + "z.landAreaRatio DESC, z.countyFips";

    /**
     * @return every county this ZIP touches, most-land-first; an empty list if the
     * ZIP is absent. <b>Never null, and an empty list is a legitimate answer</b> —
     * the crosswalk is ZCTA-derived and does not cover every USPS ZIP.
     */
    public static List<ZipCounty> findByZip(EntityManager em, String zip) {
        if (zip == null || zip.isBlank()) {
            return new ArrayList<>();
        }
        return em.createQuery(JPQL_FIND_BY_ZIP, ZipCounty.class)
                .setParameter("zip", zip.trim())
                .getResultList();
    }
}

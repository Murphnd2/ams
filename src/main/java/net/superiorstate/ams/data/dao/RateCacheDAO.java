package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.market.RatingAreaRateCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for {@link RatingAreaRateCache}. All methods are static; the class is
 * abstract (not instantiable) following the convention of EmployerInventoryDAO
 * and ChatbotSkillDAO.
 */
public abstract class RateCacheDAO {

    private static final Logger log = LogManager.getLogger(RateCacheDAO.class);

    private static final String JPQL_GET_RATE =
            "SELECT r FROM RatingAreaRateCache r " +
            "WHERE r.planYear = :planYear AND r.countyFips = :countyFips " +
            "AND r.age = :age AND r.usesTobacco = :usesTobacco";

    private static final String JPQL_GET_COUNTY_RATES =
            "SELECT r FROM RatingAreaRateCache r " +
            "WHERE r.planYear = :planYear AND r.countyFips = :countyFips " +
            "ORDER BY r.age";

    private static final String JPQL_COUNTY_SUMMARIES =
            "SELECT r.countyFips, COUNT(r), MIN(r.fetchedAt), MAX(r.fetchedAt), MAX(r.sourceEnv) " +
            "FROM RatingAreaRateCache r WHERE r.planYear = :planYear GROUP BY r.countyFips";

    private static final String JPQL_DELETE_COUNTY_RATES =
            "DELETE FROM RatingAreaRateCache r " +
            "WHERE r.planYear = :planYear AND r.countyFips = :countyFips";

    /** Looks up a single cache row by its unique key. Returns null if not cached. */
    public static RatingAreaRateCache getRate(EntityManager em, int planYear, String countyFips, int age, boolean usesTobacco) {
        try {
            return em.createQuery(JPQL_GET_RATE, RatingAreaRateCache.class)
                    .setParameter("planYear", planYear)
                    .setParameter("countyFips", countyFips)
                    .setParameter("age", age)
                    .setParameter("usesTobacco", usesTobacco)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** All cached rows (every age) for one county and plan year, ordered by age. */
    public static List<RatingAreaRateCache> getRatesForCounty(EntityManager em, int planYear, String countyFips) {
        return em.createQuery(JPQL_GET_COUNTY_RATES, RatingAreaRateCache.class)
                .setParameter("planYear", planYear)
                .setParameter("countyFips", countyFips)
                .getResultList();
    }

    /**
     * Per-county summary for the admin page: row count, oldest/newest fetchedAt,
     * and source_env. A county's rows are always written together in one
     * {@link #replaceCountyRates} transaction, so MAX(sourceEnv) and MAX/MIN(fetchedAt)
     * collapsing to a single value per county is the expected case, not a fallback.
     */
    public static List<CountySummary> getCountySummaries(EntityManager em, int planYear) {
        List<Object[]> rows = em.createQuery(JPQL_COUNTY_SUMMARIES, Object[].class)
                .setParameter("planYear", planYear)
                .getResultList();

        List<CountySummary> summaries = new ArrayList<>();
        for (Object[] row : rows) {
            summaries.add(new CountySummary(
                    (String) row[0],
                    ((Long) row[1]).intValue(),
                    (LocalDateTime) row[2],
                    (LocalDateTime) row[3],
                    (String) row[4]));
        }
        return summaries;
    }

    /**
     * Bulk-replaces every row for one (planYear, countyFips): deletes existing rows,
     * then persists the supplied replacement rows, in a single transaction.
     */
    public static void replaceCountyRates(EntityManager em, int planYear, String countyFips, List<RatingAreaRateCache> newRows) {
        em.getTransaction().begin();
        try {
            int deleted = em.createQuery(JPQL_DELETE_COUNTY_RATES)
                    .setParameter("planYear", planYear)
                    .setParameter("countyFips", countyFips)
                    .executeUpdate();
            for (RatingAreaRateCache row : newRows) {
                em.persist(row);
            }
            em.getTransaction().commit();
            log.info("Replaced rate cache for county {} plan year {}: {} deleted, {} inserted",
                    countyFips, planYear, deleted, newRows.size());
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /** Lightweight read-only summary row for the admin page — not a JPA entity. */
    public static class CountySummary {
        private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final String countyFips;
        private final int rowCount;
        private final LocalDateTime oldestFetchedAt;
        private final LocalDateTime newestFetchedAt;
        private final String sourceEnv;

        public CountySummary(String countyFips, int rowCount, LocalDateTime oldestFetchedAt,
                              LocalDateTime newestFetchedAt, String sourceEnv) {
            this.countyFips = countyFips;
            this.rowCount = rowCount;
            this.oldestFetchedAt = oldestFetchedAt;
            this.newestFetchedAt = newestFetchedAt;
            this.sourceEnv = sourceEnv;
        }

        public String getCountyFips() { return countyFips; }
        public int getRowCount() { return rowCount; }
        public LocalDateTime getOldestFetchedAt() { return oldestFetchedAt; }
        public LocalDateTime getNewestFetchedAt() { return newestFetchedAt; }
        public String getSourceEnv() { return sourceEnv; }

        /** Display string for JSP rendering — never format a java.time value in a JSP taglib. */
        public String getOldestFetchedAtDisplay() {
            return oldestFetchedAt == null ? "—" : oldestFetchedAt.format(DISPLAY_FORMAT);
        }

        /** Display string for JSP rendering — never format a java.time value in a JSP taglib. */
        public String getNewestFetchedAtDisplay() {
            return newestFetchedAt == null ? "—" : newestFetchedAt.format(DISPLAY_FORMAT);
        }
    }
}

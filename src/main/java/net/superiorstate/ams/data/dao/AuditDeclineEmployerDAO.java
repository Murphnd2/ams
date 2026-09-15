package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.AuditDeclineEmployer;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for {@link AuditDeclineEmployer} (V114). All methods are static; the class is abstract (not
 * instantiable), following {@link SummitServiceItemFlagsDAO}'s convention.
 * <p>
 * <b>Not an authorization boundary.</b> The reads answer "which employers has this PSP designated
 * for card-decline monitoring", nothing more — callers establish that the caller may act for that
 * PSP before calling, exactly as {@link SummitServiceItemFlagsDAO} documents for its own reads.
 */
public abstract class AuditDeclineEmployerDAO {

    /**
     * Every designation for one PSP, ordered by the employer's name (the admin screen's own
     * listing order). The join to {@code Employer} is on the AMS primary key the row stores
     * ({@code employer.organization_id}); both sides are small.
     */
    private static final String JPQL_ALL_BY_PSP =
            "SELECT d FROM AuditDeclineEmployer d, Employer e " +
            "WHERE d.pspId = :pspId AND e.id = d.employerId " +
            "ORDER BY e.employerName, e.id";

    /** Falls back to id order when the join above can't be used (e.g. a stale employer id). */
    private static final String JPQL_ALL_BY_PSP_FALLBACK =
            "SELECT d FROM AuditDeclineEmployer d WHERE d.pspId = :pspId ORDER BY d.id";

    /** The designated employers themselves, joined — the check's own read. */
    private static final String JPQL_EMPLOYERS_BY_PSP =
            "SELECT e FROM AuditDeclineEmployer d, Employer e " +
            "WHERE d.pspId = :pspId AND e.id = d.employerId " +
            "ORDER BY e.employerName, e.id";

    private static final String JPQL_BY_PSP_AND_EMPLOYER =
            "SELECT d FROM AuditDeclineEmployer d " +
            "WHERE d.pspId = :pspId AND d.employerId = :employerId";

    /**
     * Every designation for one PSP, ordered by employer name (falling back to id if an employer
     * id no longer resolves). Never null; empty when {@code pspId} is null or the PSP has
     * designated nothing.
     */
    public static List<AuditDeclineEmployer> findAllByPspId(EntityManager em, Long pspId) {
        if (pspId == null) return List.of();
        try {
            return em.createQuery(JPQL_ALL_BY_PSP, AuditDeclineEmployer.class)
                    .setParameter("pspId", pspId)
                    .getResultList();
        } catch (RuntimeException e) {
            return em.createQuery(JPQL_ALL_BY_PSP_FALLBACK, AuditDeclineEmployer.class)
                    .setParameter("pspId", pspId)
                    .getResultList();
        }
    }

    /**
     * The designated employers' Summit ids and names for one PSP — what {@code CardDeclineCheck}
     * consumes. Keyed by {@code Employer.altId} (Summit {@code EmployerID}, the value the export's
     * {@code Employer SystemID} carries), value {@code Employer.employerName}. A designated
     * employer whose {@code altId} is {@code 0} (imported from a J1 file without the
     * {@code EmployerID} column, T265) is <b>omitted</b> — it can never match a decline row, and
     * keying it would let two such employers collide. Never null; empty when {@code pspId} is
     * null or nothing is designated.
     * <p>
     * Insertion order is the employer-name order of {@link #JPQL_EMPLOYERS_BY_PSP}.
     */
    public static Map<Integer, String> findDesignatedEmployersByAltId(EntityManager em, Long pspId) {
        Map<Integer, String> byAltId = new LinkedHashMap<>();
        if (pspId == null) return byAltId;
        List<Employer> employers = em.createQuery(JPQL_EMPLOYERS_BY_PSP, Employer.class)
                .setParameter("pspId", pspId)
                .getResultList();
        for (Employer employer : employers) {
            if (employer.getAltId() <= 0) continue;
            byAltId.putIfAbsent(employer.getAltId(), employer.getEmployerName());
        }
        return byAltId;
    }

    /** One designation by primary key, or null. */
    public static AuditDeclineEmployer findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(AuditDeclineEmployer.class, id);
    }

    /**
     * The row already occupying a (PSP, employer) pair, or null — the admin screen's duplicate
     * pre-check, the same role {@link SummitServiceItemFlagsDAO#findByPspAndServiceItem} plays for
     * its own table.
     */
    public static AuditDeclineEmployer findByPspAndEmployer(EntityManager em, Long pspId, Integer employerId) {
        if (pspId == null || employerId == null) return null;
        List<AuditDeclineEmployer> found = em.createQuery(JPQL_BY_PSP_AND_EMPLOYER, AuditDeclineEmployer.class)
                .setParameter("pspId", pspId)
                .setParameter("employerId", employerId)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Persists a new designation in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back. A unique-constraint
     * violation arrives here when two admins race the same (PSP, employer) pair; the caller
     * reports it rather than swallowing it, the same contract {@link SummitServiceItemFlagsDAO#insert}
     * documents.
     */
    public static void insert(EntityManager em, AuditDeclineEmployer designation) {
        try {
            em.getTransaction().begin();
            em.persist(designation);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to designate employer: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one designation, in its own transaction. There is no Summit-side artifact keyed on
     * this row to worry about orphaning — a designation is pure AMS-side data, so deletion is
     * unconditional.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Long id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            AuditDeclineEmployer designation = em.find(AuditDeclineEmployer.class, id);
            if (designation != null) em.remove(designation);
            em.getTransaction().commit();
            return designation != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove employer designation: " + e.getMessage(), e);
        }
    }
}

package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.market.PayrollFrequency;

import java.util.List;

/**
 * DAO for {@link PayrollFrequency} (V107). All methods are static; the class is abstract
 * (not instantiable), following {@link SummitPlanTemplateMapDAO}'s and
 * {@link EnrollmentMatrixDAO}'s convention.
 * <p>
 * <b>Not an authorization boundary.</b> This registry is employer-independent and
 * installation-wide, not PSP-scoped — every read here is unconditional, unlike
 * {@code SummitPlanTemplateMapDAO}'s PSP-filtered reads. Callers establish that the caller
 * may administer or read this table before calling (PSP-admin gate on
 * {@code PayrollFrequencyAdmin}).
 * <p>
 * <b>No hardcoded row ids.</b> Callers ask by code, by application value, or by the
 * enrollment-approved flag — never by id literal, since the table ships empty and every row
 * is Kevin's to create.
 */
public abstract class PayrollFrequencyDAO {

    private static final String JPQL_ALL =
            "SELECT p FROM PayrollFrequency p ORDER BY p.sortOrder, p.label";

    /**
     * The curated set the enrollment matrix dropdown offers: enrollment-approved and active,
     * in display order. This is the one read the matrix UI calls.
     */
    private static final String JPQL_ENROLLMENT_APPROVED =
            "SELECT p FROM PayrollFrequency p " +
            "WHERE p.enrollmentApproved = true AND p.active = true " +
            "ORDER BY p.sortOrder, p.label";

    private static final String JPQL_BY_CODE =
            "SELECT p FROM PayrollFrequency p WHERE p.code = :code";

    private static final String JPQL_BY_APPLICATION_VALUE =
            "SELECT p FROM PayrollFrequency p " +
            "WHERE p.active = true AND p.applicationValue = :applicationValue " +
            "ORDER BY p.sortOrder, p.label";

    /** Every row, active and inactive alike — the admin screen's listing. */
    public static List<PayrollFrequency> findAll(EntityManager em) {
        return em.createQuery(JPQL_ALL, PayrollFrequency.class).getResultList();
    }

    /**
     * The curated, enrollment-approved rows the matrix dropdown offers.
     *
     * @return the matching rows, or an empty list when nothing is approved yet. Never null —
     * an empty result is the ordinary state until Kevin approves at least one row.
     */
    public static List<PayrollFrequency> findEnrollmentApproved(EntityManager em) {
        return em.createQuery(JPQL_ENROLLMENT_APPROVED, PayrollFrequency.class).getResultList();
    }

    /** One row by primary key, or null. */
    public static PayrollFrequency findById(EntityManager em, Integer id) {
        if (id == null) return null;
        return em.find(PayrollFrequency.class, id);
    }

    /** One row by its unique {@code code}, or null. */
    public static PayrollFrequency findByCode(EntityManager em, String code) {
        if (code == null || code.isBlank()) return null;
        List<PayrollFrequency> found = em.createQuery(JPQL_BY_CODE, PayrollFrequency.class)
                .setParameter("code", code)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * For defaulting the matrix dropdown from the application's {@code paycycle_frequency}
     * answer. Returns the first active match in display order, or null when nothing maps to
     * that answer string.
     */
    public static PayrollFrequency findByApplicationValue(EntityManager em, String applicationValue) {
        if (applicationValue == null || applicationValue.isBlank()) return null;
        List<PayrollFrequency> found = em.createQuery(JPQL_BY_APPLICATION_VALUE, PayrollFrequency.class)
                .setParameter("applicationValue", applicationValue)
                .getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Persists a new row in its own transaction.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back — same contract
     * {@link EnrollmentMatrixDAO#insert} documents. A unique-constraint violation on
     * {@code code} arrives here.
     */
    public static void save(EntityManager em, PayrollFrequency payrollFrequency) {
        try {
            em.getTransaction().begin();
            if (payrollFrequency.getId() == null) {
                em.persist(payrollFrequency);
            } else {
                em.merge(payrollFrequency);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to save payroll frequency: " + e.getMessage(), e);
        }
    }

    /**
     * Removes one row, in its own transaction.
     *
     * @return true if a row was removed, false if the id matched nothing.
     */
    public static boolean delete(EntityManager em, Integer id) {
        if (id == null) return false;
        try {
            em.getTransaction().begin();
            PayrollFrequency found = em.find(PayrollFrequency.class, id);
            if (found != null) em.remove(found);
            em.getTransaction().commit();
            return found != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove payroll frequency: " + e.getMessage(), e);
        }
    }
}

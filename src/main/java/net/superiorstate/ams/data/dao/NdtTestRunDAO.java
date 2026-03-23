package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.activity.ndt.NdtAccessLog;
import net.superiorstate.ams.model.activity.ndt.NdtDocumentUpload;
import net.superiorstate.ams.model.activity.ndt.NdtTestRun;
import net.superiorstate.ams.model.general.Person;

import java.util.List;

public abstract class NdtTestRunDAO {

    /**
     * Find the test run linked to a given activity (Renewal).
     */
    public static NdtTestRun findByActivity(EntityManager em, Long activityId) {
        try {
            return em.createQuery(
                    "SELECT t FROM NdtTestRun t WHERE t.activity.id = :activityId",
                    NdtTestRun.class)
                    .setParameter("activityId", activityId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find a test run by its ID.
     */
    public static NdtTestRun findById(EntityManager em, Long testRunId) {
        return em.find(NdtTestRun.class, testRunId);
    }

    /**
     * Find a test run with its documents eagerly loaded.
     */
    public static NdtTestRun findByIdWithDocuments(EntityManager em, Long testRunId) {
        try {
            return em.createQuery(
                    "SELECT t FROM NdtTestRun t LEFT JOIN FETCH t.documents WHERE t.id = :id",
                    NdtTestRun.class)
                    .setParameter("id", testRunId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Get all documents for a test run, ordered by upload date descending.
     */
    public static List<NdtDocumentUpload> getDocuments(EntityManager em, Long testRunId) {
        return em.createQuery(
                "SELECT d FROM NdtDocumentUpload d WHERE d.testRun.id = :testRunId ORDER BY d.uploadedAt DESC",
                NdtDocumentUpload.class)
                .setParameter("testRunId", testRunId)
                .getResultList();
    }

    /**
     * Find a document upload by its ID.
     */
    public static NdtDocumentUpload findUploadById(EntityManager em, Long uploadId) {
        return em.find(NdtDocumentUpload.class, uploadId);
    }

    /**
     * Log an access event. Runs in its own transaction so audit logging
     * never fails the main operation.
     */
    public static void logAccess(EntityManager em, NdtTestRun testRun, Person person,
                                 String action, String detail, String ipAddress) {
        try {
            em.getTransaction().begin();
            NdtAccessLog log = new NdtAccessLog(testRun, person, action, detail, ipAddress);
            em.persist(log);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("[NdtDAO] Failed to log access: " + e.getMessage());
        }
    }

    /**
     * Get the access log for a test run.
     */
    public static List<NdtAccessLog> getAccessLog(EntityManager em, Long testRunId) {
        return em.createQuery(
                "SELECT l FROM NdtAccessLog l WHERE l.testRun.id = :testRunId ORDER BY l.accessedAt DESC",
                NdtAccessLog.class)
                .setParameter("testRunId", testRunId)
                .getResultList();
    }
}

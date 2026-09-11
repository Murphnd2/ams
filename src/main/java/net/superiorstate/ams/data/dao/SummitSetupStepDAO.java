package net.superiorstate.ams.data.dao;

import net.superiorstate.ams.model.market.SummitFileExport;
import net.superiorstate.ams.model.market.SummitSetupStep;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO for {@link SummitSetupStep} (V098). All methods are static; the class is abstract (not
 * instantiable), following {@link SummitFileExportDAO}'s own shape.
 * <p>
 * <b>Not an authorization boundary</b> for the same reason {@link SummitFileExportDAO} states for
 * its own reads — these methods answer "what is this step's state" / "what was last pushed for
 * this proposal and file type", nothing more. Callers establish that the caller may act for the
 * PSP before calling, exactly as {@code SummitResponseServlet} does with its PSP-admin and
 * {@code IchraAccessResolver} gates (mirrored from {@code SummitExportServlet}).
 * <p>
 * Two read methods here ({@link #findLatestPushed} and {@link #findLatestDeliveryAttempt}) query
 * {@link SummitFileExport} rather than {@link SummitSetupStep}. They live here, not on
 * {@link SummitFileExportDAO}, so that class stays untouched — the response-check flow this DAO
 * supports is new surface, not a change to the export-recording path.
 */
public abstract class SummitSetupStepDAO {

    /**
     * ⚠️ <b>Deliberately not filtered by {@code pspId}.</b> {@code summit_setup_step}'s unique key
     * is {@code (proposal_id, step_key)} — it does not include {@code psp_id}, because
     * {@link SummitSetupStep#pspId} is nullable and unconstrained for the same reason
     * {@link SummitFileExport#pspId} is (a recording failure to resolve a PSP must not block Mark
     * done). Filtering this lookup by {@code pspId} could miss an existing row whose {@code psp_id}
     * came back null or from a different session, and {@code markDone}/{@code reopen} would then
     * attempt to insert a second row for the same {@code (proposal_id, step_key)} and violate the
     * unique key. The caller establishes PSP authorization before calling, exactly as
     * {@link SummitFileExportDAO#findById} documents for its own not-an-authorization-boundary read.
     */
    private static final String JPQL_STEP_BY_PROPOSAL =
            "SELECT s FROM SummitSetupStep s " +
            "WHERE s.proposalId = :proposalId AND s.stepKey = :stepKey";

    /** Latest successfully pushed export for one proposal and file type. */
    private static final String JPQL_LATEST_PUSHED =
            "SELECT e FROM SummitFileExport e " +
            "WHERE e.pspId = :pspId AND e.proposalId = :proposalId AND e.fileType = :fileType " +
            "AND e.deliveryStatus = 'PUSHED' " +
            "ORDER BY e.deliveredAt DESC, e.id DESC";

    /** Latest delivery attempt of any outcome (PUSHING/PUSHED/PUSH_FAILED) for one proposal and file type. */
    private static final String JPQL_LATEST_DELIVERY_ATTEMPT =
            "SELECT e FROM SummitFileExport e " +
            "WHERE e.pspId = :pspId AND e.proposalId = :proposalId AND e.fileType = :fileType " +
            "AND e.deliveryStatus IS NOT NULL " +
            "ORDER BY e.deliveredAt DESC, e.id DESC";

    /**
     * The step-state row for one proposal and step, or {@code null} if it has never been marked.
     * See the class-level note on {@link #JPQL_STEP_BY_PROPOSAL} for why {@code pspId} is accepted
     * but not used to filter.
     */
    public static SummitSetupStep findByProposalAndStep(EntityManager em, Long pspId, Long proposalId, String stepKey) {
        if (proposalId == null || stepKey == null) return null;
        List<SummitSetupStep> results = em.createQuery(JPQL_STEP_BY_PROPOSAL, SummitSetupStep.class)
                .setParameter("proposalId", proposalId)
                .setParameter("stepKey", stepKey)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Upserts one step to {@code DONE}, in its own transaction. {@code basis} is {@code REVIEWED}
     * (with a non-null {@code exportId}) or {@code MANUAL} (with a null {@code exportId}) — the
     * caller decides which, exactly as {@code SummitExportServlet.recordExport} decides push-vs-
     * download before calling {@code SummitFileExportDAO.insert}.
     * <p>
     * Same transaction pattern as {@link SummitFileExportDAO#insert} / {@code updateDelivery}: find
     * inside the transaction, mutate the managed entity (or persist a new one), commit; rollback and
     * rethrow on failure.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void markDone(EntityManager em, Long pspId, Long proposalId, String stepKey,
                                 String basis, Long exportId, String user) {
        try {
            em.getTransaction().begin();
            List<SummitSetupStep> existing = em.createQuery(JPQL_STEP_BY_PROPOSAL, SummitSetupStep.class)
                    .setParameter("proposalId", proposalId)
                    .setParameter("stepKey", stepKey)
                    .setMaxResults(1)
                    .getResultList();
            SummitSetupStep row = existing.isEmpty() ? null : existing.get(0);
            if (row == null) {
                row = new SummitSetupStep();
                row.setProposalId(proposalId);
                row.setStepKey(stepKey);
                em.persist(row);
            }
            row.setPspId(pspId);
            row.setState("DONE");
            row.setBasis(basis);
            row.setExportId(exportId);
            row.setUpdatedAt(LocalDateTime.now());
            row.setUpdatedBy(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to mark Summit setup step done: " + e.getMessage(), e);
        }
    }

    /**
     * Sets one step back to {@code OPEN}, nulling {@code basis} and {@code exportId}. A no-op (not
     * an error) when no row exists for this {@code (proposalId, stepKey)} yet — there is nothing to
     * reopen. Same transaction pattern as {@link #markDone}.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void reopen(EntityManager em, Long pspId, Long proposalId, String stepKey, String user) {
        try {
            em.getTransaction().begin();
            List<SummitSetupStep> existing = em.createQuery(JPQL_STEP_BY_PROPOSAL, SummitSetupStep.class)
                    .setParameter("proposalId", proposalId)
                    .setParameter("stepKey", stepKey)
                    .setMaxResults(1)
                    .getResultList();
            if (!existing.isEmpty()) {
                SummitSetupStep row = existing.get(0);
                row.setPspId(pspId);
                row.setState("OPEN");
                row.setBasis(null);
                row.setExportId(null);
                row.setUpdatedAt(LocalDateTime.now());
                row.setUpdatedBy(user);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to reopen Summit setup step: " + e.getMessage(), e);
        }
    }

    /**
     * The most recently successfully pushed ({@code deliveryStatus = 'PUSHED'}) export for this
     * proposal and file type, or {@code null}. Filtered by {@code pspId} the same way
     * {@link SummitFileExportDAO#findByPspId} filters its own listing — returns {@code null} rather
     * than an unscoped result when {@code pspId} is null.
     */
    public static SummitFileExport findLatestPushed(EntityManager em, Long pspId, Long proposalId, String fileType) {
        if (pspId == null || proposalId == null || fileType == null) return null;
        List<SummitFileExport> results = em.createQuery(JPQL_LATEST_PUSHED, SummitFileExport.class)
                .setParameter("pspId", pspId)
                .setParameter("proposalId", proposalId)
                .setParameter("fileType", fileType)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * The most recent delivery attempt of any outcome (PUSHING, PUSHED or PUSH_FAILED) for this
     * proposal and file type, or {@code null}. Same {@code pspId} filtering as
     * {@link #findLatestPushed}. Used to surface a later failed or in-flight attempt alongside the
     * latest successful push, if the two differ.
     */
    public static SummitFileExport findLatestDeliveryAttempt(EntityManager em, Long pspId, Long proposalId, String fileType) {
        if (pspId == null || proposalId == null || fileType == null) return null;
        List<SummitFileExport> results = em.createQuery(JPQL_LATEST_DELIVERY_ATTEMPT, SummitFileExport.class)
                .setParameter("pspId", pspId)
                .setParameter("proposalId", proposalId)
                .setParameter("fileType", fileType)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}

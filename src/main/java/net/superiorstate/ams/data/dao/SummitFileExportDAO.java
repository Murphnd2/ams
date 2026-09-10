package net.superiorstate.ams.data.dao;

import net.superiorstate.ams.model.market.SummitFileExport;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * DAO for {@link SummitFileExport} (V096). All methods are static; the class is abstract
 * (not instantiable), following {@link SummitPlanTemplateMapDAO} and {@link EmployerParticipantDAO}.
 * <p>
 * <b>Not an authorization boundary.</b> These methods answer "what export files were generated",
 * nothing more — they do not know who is asking. Callers establish that the caller may act for the
 * PSP before calling, exactly as {@code SummitExportServlet} does with its PSP-admin and
 * {@code IchraAccessResolver} gates.
 */
public abstract class SummitFileExportDAO {

    /** The listing read, newest first, matching the (psp_id, file_type, generated_at) index. */
    private static final String JPQL_BY_PSP =
            "SELECT e FROM SummitFileExport e " +
            "WHERE e.pspId = :pspId " +
            "ORDER BY e.generatedAt DESC, e.id DESC";

    /** Same, narrowed to one proposal — the per-proposal "what has been sent for this employer" read. */
    private static final String JPQL_BY_PROPOSAL =
            "SELECT e FROM SummitFileExport e " +
            "WHERE e.proposalId = :proposalId " +
            "ORDER BY e.generatedAt DESC, e.id DESC";

    /**
     * T194 — prior exports of the same file type carrying the same content hash.
     * <p>
     * ⚠️ <b>Summit dedupes a re-sent file on content</b>, so a non-empty answer here means
     * re-sending these bytes would be a silent no-op: no error, no results row, nothing. The hash
     * is matched together with {@code file_type} because two different file types could in
     * principle share bytes (a zero-row file of either type is the degenerate case) and only a
     * same-type match tells you anything about what Summit will do.
     */
    private static final String JPQL_BY_HASH =
            "SELECT e FROM SummitFileExport e " +
            "WHERE e.contentSha256 = :sha AND e.fileType = :fileType " +
            "ORDER BY e.generatedAt DESC, e.id DESC";

    /**
     * S42-D — T227/T229: has a PUSHING or PUSHED row already used this exact filename? Summit
     * rejects a repeated filename outright regardless of content (SDX-17), and the push filename
     * (S42-D) is now the same shape a download already uses, carrying no per-push uniqueness of
     * its own -- this is the check that supplies it instead.
     * <p>
     * No PSP filter, deliberately: one installation has one Summit {@code ImportFiles} folder, so
     * filenames collide across PSPs, not only within one.
     */
    private static final String JPQL_COUNT_BY_PUSHED_FILENAME =
            "SELECT COUNT(e) FROM SummitFileExport e " +
            "WHERE e.fileName = :fileName AND e.deliveryStatus IN ('PUSHING','PUSHED')";

    /**
     * Persists one export record in its own transaction.
     * <p>
     * ⚠️ <b>This method throws; the caller must not let that reach the user.</b> The governing rule
     * for the recording path is that <b>a recording failure must not fail the export</b> — a person
     * waiting on a file is not blocked by a bookkeeping failure. That decision lives at the call
     * site ({@code SummitExportServlet.writeFile}, which records only after the response has been
     * written and flushed, and logs at ERROR on failure). This method stays loud rather than
     * swallowing, so the call site's choice is visible where it is made rather than hidden here
     * where a later caller with different needs could not see it.
     *
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void insert(EntityManager em, SummitFileExport export) {
        try {
            em.getTransaction().begin();
            em.persist(export);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to record Summit file export: " + e.getMessage(), e);
        }
    }

    /**
     * V097 — updates one row's delivery outcome after a push attempt (PUSHING/PUSHED/PUSH_FAILED).
     * <p>
     * Same transaction pattern as {@link #insert}, including rollback and throwing after it; the
     * caller decides whether a failure here may reach the user, exactly as {@code insert}'s
     * javadoc describes for the recording path.
     *
     * @param error the failure message, truncated to 500 characters when non-null; null clears it.
     * @throws RuntimeException wrapping whatever failed, after rolling back.
     */
    public static void updateDelivery(EntityManager em, Long id, String status, String error) {
        try {
            em.getTransaction().begin();
            SummitFileExport export = em.find(SummitFileExport.class, id);
            if (export != null) {
                export.setDeliveryStatus(status);
                export.setDeliveryError(error == null ? null
                        : (error.length() > 500 ? error.substring(0, 500) : error));
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Failed to update Summit file export delivery: " + e.getMessage(), e);
        }
    }

    /**
     * Every recorded export for one PSP, newest first.
     *
     * @return the matching rows, or an empty list when {@code pspId} is null. Never null.
     */
    public static List<SummitFileExport> findByPspId(EntityManager em, Long pspId) {
        if (pspId == null) return List.of();
        return em.createQuery(JPQL_BY_PSP, SummitFileExport.class)
                .setParameter("pspId", pspId)
                .getResultList();
    }

    /**
     * Every recorded export for one proposal, newest first.
     *
     * @return the matching rows, or an empty list when {@code proposalId} is null. Never null.
     */
    public static List<SummitFileExport> findByProposalId(EntityManager em, Long proposalId) {
        if (proposalId == null) return List.of();
        return em.createQuery(JPQL_BY_PROPOSAL, SummitFileExport.class)
                .setParameter("proposalId", proposalId)
                .getResultList();
    }

    /**
     * T194 — prior exports of {@code fileType} whose content hashes to {@code sha}, newest first.
     * A non-empty result means these exact bytes have already been generated, and Summit would
     * dedupe them on a re-send rather than importing them.
     *
     * @return the matching rows, or an empty list when either argument is null. Never null.
     */
    public static List<SummitFileExport> findByContentHash(EntityManager em, String sha, String fileType) {
        if (sha == null || fileType == null) return List.of();
        return em.createQuery(JPQL_BY_HASH, SummitFileExport.class)
                .setParameter("sha", sha)
                .setParameter("fileType", fileType)
                .getResultList();
    }

    /**
     * S42-D — true when a PUSHING or PUSHED row already carries {@code fileName}. See
     * {@link #JPQL_COUNT_BY_PUSHED_FILENAME}.
     *
     * @return false when {@code fileName} is null. Never null.
     */
    public static boolean existsPushedFileName(EntityManager em, String fileName) {
        if (fileName == null) return false;
        Long count = em.createQuery(JPQL_COUNT_BY_PUSHED_FILENAME, Long.class)
                .setParameter("fileName", fileName)
                .getSingleResult();
        return count != null && count > 0;
    }

    /**
     * S32-G — one export record by primary key, or null.
     * <p>
     * Added for the retained-export listing screen's download action, which needs a single row and
     * had no read to reach it with. ⚠️ <b>This does not scope by PSP</b> — it answers "the row with
     * this id" and nothing more. The caller must compare the returned row's {@code pspId} against
     * the acting session's before serving anything, exactly as {@code SummitFileExportAdmin} does;
     * without that check an id in a query string would read another PSP's participant names and
     * addresses. Same not-an-authorization-boundary contract the class note states for every other
     * read here.
     */
    public static SummitFileExport findById(EntityManager em, Long id) {
        if (id == null) return null;
        return em.find(SummitFileExport.class, id);
    }
}

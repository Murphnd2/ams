package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V096 — one row per Summit export file that {@code SummitExportServlet} actually generated,
 * carrying the exact bytes that were sent.
 * <p>
 * <b>This is a record, not a transport.</b> Nothing here connects anywhere; the row exists so
 * that two things become possible which are impossible without it:
 * <ul>
 *   <li><b>Response-file verification.</b> Summit's results file for file 2 carries no row
 *       number, so correlation falls back to {@code Plan Name}; Demographics correlates on row
 *       number. Both need the bytes that were <i>sent</i>. A regeneration is not equivalent —
 *       plan-template mappings (V095), {@code SUMMIT_BRANCH_CODE}, {@code SUMMIT_TPA_ID_PREFIX}
 *       and the participant roster can all change underneath an export, so {@link #content} is
 *       stored rather than recomputed.</li>
 *   <li><b>T194 — Summit dedupes a re-sent file on content.</b> Re-sending unchanged bytes is a
 *       silent no-op. {@link #contentSha256} is indexed so that is detectable before sending.</li>
 * </ul>
 * <p>
 * ⚠️ <b>{@link #content} holds PII</b> — participant names and addresses on the Demographics
 * file. It carries no SSN: {@link EmployerParticipant} (V094) has no SSN, DOB or compensation
 * column and the census parser drops those headers, so no SSN enters the process at all.
 * <p>
 * <b>Scalar foreign keys, not relations</b> — following the siblings {@link EmployerParticipant}
 * and {@link SummitPlanTemplateMap} in this package. Nothing navigates from an export record to a
 * {@code PSP}, {@code Proposal} or {@code Prospect}; a relation would add fetch behaviour with no
 * reader, and worse, would couple an audit row's readability to the continued existence of the
 * things it describes.
 * <p>
 * ⚠️ <b>{@link #pspId} is nullable here, unlike {@code SummitPlanTemplateMap.pspId}.</b> That row
 * is read <i>by</i> PSP so a null could never be read back; this row is <i>written</i> from a live
 * request whose session may not resolve one, and the governing rule is that a recording failure
 * must never fail an export. {@link #proposalId} and {@link #prospectId} are nullable and
 * unconstrained for the same reason.
 */
@Entity
@Table(name = "summit_file_export")
public class SummitFileExport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** The PSP whose session generated the file. Null when the session resolved none — see the class note. */
    @Column(name = "psp_id")
    private Long pspId;

    /** The servlet's own {@code type} discriminator, verbatim: employer | cdhplan | demographics | enrollment. */
    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "proposal_id")
    private Long proposalId;

    @Column(name = "prospect_id")
    private Long prospectId;

    /** The resolved download filename, exactly as the {@code Content-Disposition} header carried it. */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /** Display name of the acting user. Null when the session cannot name anyone. */
    @Column(name = "generated_by")
    private String generatedBy;

    /**
     * Emitted line count. ⚠️ <b>Zero is a legitimate recorded value</b> — S31-J's empty-file guard
     * covers {@code cdhplan} only, so an empty participant roster still produces a zero-row
     * {@code demographics} or {@code enrollment} file, and that send is worth a record.
     */
    @Column(name = "row_count", nullable = false)
    private int rowCount;

    /** UTF-8 byte length of {@link #content}. */
    @Column(name = "byte_count", nullable = false)
    private int byteCount;

    /** Lowercase hex SHA-256 of the UTF-8 bytes. Indexed — this is what makes T194 detectable. */
    @Column(name = "content_sha256", nullable = false)
    private String contentSha256;

    /** V097 — null means download only; else PUSHING, PUSHED or PUSH_FAILED. */
    @Column(name = "delivery_status", length = 20)
    private String deliveryStatus;

    /** V097 — when the push was attempted, set alongside {@link #deliveryStatus} PUSHING. */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /** V097 — the remote directory the file was (or would be) pushed to, i.e. ImportFiles. */
    @Column(name = "delivery_dir", length = 255)
    private String deliveryDir;

    /** V097 — the scrubbed failure message when {@link #deliveryStatus} is PUSH_FAILED. */
    @Column(name = "delivery_error", length = 500)
    private String deliveryError;

    /** The file's bytes as text. MEDIUMTEXT; files are kilobytes. See the PII note on the class. */
    @Lob
    @Column(name = "content")
    private String content;

    public SummitFileExport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getProposalId() { return proposalId; }
    public void setProposalId(Long proposalId) { this.proposalId = proposalId; }

    public Long getProspectId() { return prospectId; }
    public void setProspectId(Long prospectId) { this.prospectId = prospectId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public int getByteCount() { return byteCount; }
    public void setByteCount(int byteCount) { this.byteCount = byteCount; }

    public String getContentSha256() { return contentSha256; }
    public void setContentSha256(String contentSha256) { this.contentSha256 = contentSha256; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getDeliveryDir() { return deliveryDir; }
    public void setDeliveryDir(String deliveryDir) { this.deliveryDir = deliveryDir; }

    public String getDeliveryError() { return deliveryError; }
    public void setDeliveryError(String deliveryError) { this.deliveryError = deliveryError; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

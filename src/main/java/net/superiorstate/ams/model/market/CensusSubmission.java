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
 * V100 — one client upload through a {@link CensusRequest} link, as parsed rows (T231 build 1,
 * D45 decision a). <b>The uploaded file is never stored.</b> It is read from the multipart stream,
 * parsed leniently ({@code CensusParseService.parseLenient}) and discarded; only the eight
 * whitelisted fields (name, address, email) reach {@link #rowsJson}, so SSN, date of birth and pay
 * can never be here (LA-35, LA-41). {@link #mappingJson} holds header names and field matches only,
 * never a cell value.
 * <p>
 * State: {@code PENDING} (readable, awaiting PSP review) | {@code UNREADABLE} (a required column
 * was missing — header names kept, no rows) | {@code SUPERSEDED} (a newer upload arrived) |
 * {@code LOADED} | {@code REJECTED} (the last two written by build 2). <b>{@link #rowsJson} is
 * NULLed whenever a row leaves {@code PENDING}</b> ({@code CensusSubmissionDAO.supersedeOpen}), so
 * staged personal data lives only while a review is actually pending.
 * <p>
 * {@link #reviewedBy}/{@link #reviewedAt}/{@link #reviewNote} exist now and are written by build 2's
 * review page; build 1 never sets them.
 */
@Entity
@Table(name = "census_submission")
public class CensusSubmission {

    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_UNREADABLE = "UNREADABLE";
    public static final String STATE_SUPERSEDED = "SUPERSEDED";
    public static final String STATE_LOADED = "LOADED";
    public static final String STATE_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /** The client's file name only — never a path. Display only. */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /** PENDING | UNREADABLE | SUPERSEDED | LOADED | REJECTED. */
    @Column(name = "state", nullable = false, length = 16)
    private String state;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "issue_count", nullable = false)
    private int issueCount;

    /** Header names and field matches only. TEXT. */
    @Lob
    @Column(name = "mapping_json")
    private String mappingJson;

    /** Whitelisted fields only. MEDIUMTEXT. NULL unless {@link #state} is PENDING. */
    @Lob
    @Column(name = "rows_json")
    private String rowsJson;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public int getIssueCount() { return issueCount; }
    public void setIssueCount(int issueCount) { this.issueCount = issueCount; }

    public String getMappingJson() { return mappingJson; }
    public void setMappingJson(String mappingJson) { this.mappingJson = mappingJson; }

    public String getRowsJson() { return rowsJson; }
    public void setRowsJson(String rowsJson) { this.rowsJson = rowsJson; }

    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}

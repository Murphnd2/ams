package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@Entity
@Table(name = "time_correction_request")
public class TimeCorrectionRequest {

    @Id
    @GeneratedValue
    @Column(name = "request_id")
    private Long id;

    // ── Who is requesting ──
    @ManyToOne
    @JoinColumn(name = "requestor_id")
    private Person requestor;

    // ── Original punch pair ──
    @ManyToOne
    @JoinColumn(name = "in_log_id")
    private TimeLog inLog;

    @ManyToOne
    @JoinColumn(name = "out_log_id")
    private TimeLog outLog;

    // ── Original values (snapshot for display even if logs change) ──
    @Column(name = "original_date")
    private Date originalDate;

    @Column(name = "original_in_time")
    private Time originalInTime;

    @Column(name = "original_out_time")
    private Time originalOutTime;

    // ── Requested changes (null means "no change requested") ──
    @Column(name = "requested_in_time")
    private Time requestedInTime;

    @Column(name = "requested_out_time")
    private Time requestedOutTime;

    // ── Employee note ──
    @Column(name = "request_note", length = 500)
    private String requestNote;

    // ── Status: PENDING, APPROVED, DENIED ──
    @Column(name = "status", length = 20)
    private String status;

    // ── Timestamps ──
    @Column(name = "date_requested")
    private Timestamp dateRequested;

    @Column(name = "date_reviewed")
    private Timestamp dateReviewed;

    // ── Reviewer ──
    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private Person reviewer;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    public TimeCorrectionRequest() {
        this.status = "PENDING";
    }

    // ── Getters / Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getRequestor() { return requestor; }
    public void setRequestor(Person requestor) { this.requestor = requestor; }

    public TimeLog getInLog() { return inLog; }
    public void setInLog(TimeLog inLog) { this.inLog = inLog; }

    public TimeLog getOutLog() { return outLog; }
    public void setOutLog(TimeLog outLog) { this.outLog = outLog; }

    public Date getOriginalDate() { return originalDate; }
    public void setOriginalDate(Date originalDate) { this.originalDate = originalDate; }

    public Time getOriginalInTime() { return originalInTime; }
    public void setOriginalInTime(Time originalInTime) { this.originalInTime = originalInTime; }

    public Time getOriginalOutTime() { return originalOutTime; }
    public void setOriginalOutTime(Time originalOutTime) { this.originalOutTime = originalOutTime; }

    public Time getRequestedInTime() { return requestedInTime; }
    public void setRequestedInTime(Time requestedInTime) { this.requestedInTime = requestedInTime; }

    public Time getRequestedOutTime() { return requestedOutTime; }
    public void setRequestedOutTime(Time requestedOutTime) { this.requestedOutTime = requestedOutTime; }

    public String getRequestNote() { return requestNote; }
    public void setRequestNote(String requestNote) { this.requestNote = requestNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getDateRequested() { return dateRequested; }
    public void setDateRequested(Timestamp dateRequested) { this.dateRequested = dateRequested; }

    public Timestamp getDateReviewed() { return dateReviewed; }
    public void setDateReviewed(Timestamp dateReviewed) { this.dateReviewed = dateReviewed; }

    public Person getReviewer() { return reviewer; }
    public void setReviewer(Person reviewer) { this.reviewer = reviewer; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    /** Helper for JSP display */
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isApproved() { return "APPROVED".equals(status); }
    public boolean isDenied() { return "DENIED".equals(status); }

    /** True if the in-time is being changed */
    public boolean isInTimeChanged() { return requestedInTime != null; }

    /** True if the out-time is being changed */
    public boolean isOutTimeChanged() { return requestedOutTime != null; }
}

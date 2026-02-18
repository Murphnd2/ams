package net.superiorstate.ams.model.activity.note;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.activity.Activity;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
public class Note {
    @Id
    @GeneratedValue
    @Column(name="note_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="created_by_id")
    private Person createdBy;

    @Column(name="date_created",nullable = false,updatable = false,insertable = false,columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    @Column(name="date_generated")
    private Date dateGenerated;

    @Column(columnDefinition = "varchar(5000)")
    private String detail;

    @ManyToOne
    @JoinColumn(name="activity_id")
    private Activity activity;

    @ManyToOne
    @JoinColumn(name="reason_id")
    private ReasonCreated reasonCreated;

    @ManyToOne
    @JoinColumn(name="status_id")
    private ActivityStatus status;

    public Note(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Person createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public ReasonCreated getReasonCreated() {
        return reasonCreated;
    }

    public void setReasonCreated(ReasonCreated reasonCreated) {
        this.reasonCreated = reasonCreated;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(Date dateGenerated) {
        this.dateGenerated = dateGenerated;
    }
}

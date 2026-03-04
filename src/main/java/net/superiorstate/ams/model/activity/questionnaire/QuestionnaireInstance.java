package net.superiorstate.ams.model.activity.questionnaire;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Assignee;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questionnaire_instance")
public class QuestionnaireInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instance_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "questionnaire_id", nullable = false)
    private Questionnaire questionnaire;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Assignee activity;

    @ManyToOne
    @JoinColumn(name = "todo_id")
    private ToDo todo;

    @Column(name = "instance_guid", columnDefinition = "varchar(36)", nullable = false, unique = true)
    private String instanceGuid;

    @Column(name = "status", columnDefinition = "varchar(20) DEFAULT 'NOT_STARTED'", nullable = false)
    private String status = "NOT_STARTED";

    @Column(name = "submitted_by_name", columnDefinition = "varchar(100)")
    private String submittedByName;

    @Column(name = "submitted_by_email", columnDefinition = "varchar(200)")
    private String submittedByEmail;

    @Column(name = "date_created", insertable = false, updatable = false)
    private Timestamp dateCreated;

    @Column(name = "date_submitted")
    private Timestamp dateSubmitted;

    @Column(name = "date_reviewed")
    private Timestamp dateReviewed;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_id")
    private Assignee reviewedBy;

    @Column(name = "date_reopened")
    private Timestamp dateReopened;

    @ManyToOne
    @JoinColumn(name = "reopened_by_id")
    private Assignee reopenedBy;

    @OneToMany(mappedBy = "instance")
    private List<QuestionnaireFieldValue> fieldValues;

    public QuestionnaireInstance() {}

    @PrePersist
    private void ensureGuid() {
        if (instanceGuid == null || instanceGuid.isBlank()) {
            instanceGuid = UUID.randomUUID().toString();
        }
    }

    /** Convenience: is this instance's questionnaire external mode? */
    public boolean isExternal() {
        return questionnaire != null && questionnaire.isExternal();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Questionnaire getQuestionnaire() { return questionnaire; }
    public void setQuestionnaire(Questionnaire questionnaire) { this.questionnaire = questionnaire; }

    public Assignee getActivity() { return activity; }
    public void setActivity(Assignee activity) { this.activity = activity; }

    public ToDo getTodo() { return todo; }
    public void setTodo(ToDo todo) { this.todo = todo; }

    public String getInstanceGuid() { return instanceGuid; }
    public void setInstanceGuid(String instanceGuid) { this.instanceGuid = instanceGuid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubmittedByName() { return submittedByName; }
    public void setSubmittedByName(String submittedByName) { this.submittedByName = submittedByName; }

    public String getSubmittedByEmail() { return submittedByEmail; }
    public void setSubmittedByEmail(String submittedByEmail) { this.submittedByEmail = submittedByEmail; }

    public Timestamp getDateCreated() { return dateCreated; }

    public Timestamp getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(Timestamp dateSubmitted) { this.dateSubmitted = dateSubmitted; }

    public Timestamp getDateReviewed() { return dateReviewed; }
    public void setDateReviewed(Timestamp dateReviewed) { this.dateReviewed = dateReviewed; }

    public Assignee getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Assignee reviewedBy) { this.reviewedBy = reviewedBy; }

    public Timestamp getDateReopened() { return dateReopened; }
    public void setDateReopened(Timestamp dateReopened) { this.dateReopened = dateReopened; }

    public Assignee getReopenedBy() { return reopenedBy; }
    public void setReopenedBy(Assignee reopenedBy) { this.reopenedBy = reopenedBy; }

    public List<QuestionnaireFieldValue> getFieldValues() { return fieldValues; }
    public void setFieldValues(List<QuestionnaireFieldValue> fieldValues) { this.fieldValues = fieldValues; }
}

package net.superiorstate.ams.model.activity.checklist.tasks;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.PspClient;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name="delegated_todo")
public class DelegatedToDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="delegated_id")
    private Long id;

    @Column(name="todo_guid", nullable=false, length=36)
    private String todoGuid;

    @ManyToOne
    @JoinColumn(name="psp_client_id", nullable=false)
    private PspClient pspClient;

    @Column(name="task_name", nullable=false, length=255)
    private String taskName;

    @Column(name="task_description", columnDefinition="TEXT")
    private String taskDescription;

    @Column(name="due_date")
    private Date dueDate;

    @Column(name="goto_link", length=500)
    private String gotoLink;

    @Column(name="info_link", length=500)
    private String infoLink;

    @Column(name="activity_type", length=30)
    private String activityType;

    @Column(name="activity_name", length=255)
    private String activityName;

    @Column(name="employer_name", length=255)
    private String employerName;

    @ManyToOne
    @JoinColumn(name="assigned_to_id")
    private Person assignedTo;

    @Column(name="is_completed", nullable=false)
    private boolean isCompleted;

    @ManyToOne
    @JoinColumn(name="completed_by_id")
    private Person completedBy;

    @Column(name="completed_date")
    private Date completedDate;

    @Column(name="is_reverted", nullable=false)
    private boolean isReverted;

    @Column(name="last_sync_timestamp")
    private Timestamp lastSyncTimestamp;

    @Column(name="status", nullable=false, length=30)
    private String status = "ACTIVE";

    @Column(name="date_received", nullable=false)
    private Timestamp dateReceived;

    @Column(name="sort_order")
    private int sortOrder;

    @Column(name = "recurring_series_id", length = 50)
    private String recurringSeriesId;

    public DelegatedToDo() {}

    @PrePersist
    private void setDefaults() {
        if (dateReceived == null) dateReceived = new Timestamp(System.currentTimeMillis());
    }

    // --- Getters/Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTodoGuid() {
        return todoGuid;
    }

    public void setTodoGuid(String todoGuid) {
        this.todoGuid = todoGuid;
    }

    public PspClient getPspClient() {
        return pspClient;
    }

    public void setPspClient(PspClient pspClient) {
        this.pspClient = pspClient;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getGotoLink() {
        return gotoLink;
    }

    public void setGotoLink(String gotoLink) {
        this.gotoLink = gotoLink;
    }

    public String getInfoLink() {
        return infoLink;
    }

    public void setInfoLink(String infoLink) {
        this.infoLink = infoLink;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public Person getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Person assignedTo) {
        this.assignedTo = assignedTo;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public Person getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Person completedBy) {
        this.completedBy = completedBy;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public boolean isReverted() {
        return isReverted;
    }

    public void setReverted(boolean reverted) {
        isReverted = reverted;
    }

    public Timestamp getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }

    public void setLastSyncTimestamp(Timestamp lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(Timestamp dateReceived) {
        this.dateReceived = dateReceived;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRecurringSeriesId() {
        return recurringSeriesId;
    }

    public void setRecurringSeriesId(String recurringSeriesId) {
        this.recurringSeriesId = recurringSeriesId;
    }
}

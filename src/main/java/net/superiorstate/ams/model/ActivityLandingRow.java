package net.superiorstate.ams.model;

import java.sql.Date;

public class ActivityLandingRow {

    private final long activityId;
    private final String dtype;
    private final String fullName;
    private final Long assignedToId;
    private final Date dueDate;
    private final boolean waitingOnUs;
    private final int daysSinceContact;
    private final boolean delegatedToMe;
    private final int dueBucket;
    private final String ticketEmployerNameLc;

    public ActivityLandingRow(
            long activityId,
            String dtype,
            String fullName,
            Long assignedToId,
            Date dueDate,
            boolean waitingOnUs,
            int daysSinceContact,
            boolean delegatedToMe,
            int dueBucket,
            String ticketEmployerNameLc
    ) {
        this.activityId = activityId;
        this.dtype = dtype;
        this.fullName = fullName;
        this.assignedToId = assignedToId;
        this.dueDate = dueDate;
        this.waitingOnUs = waitingOnUs;
        this.daysSinceContact = daysSinceContact;
        this.delegatedToMe = delegatedToMe;
        this.dueBucket = dueBucket;
        this.ticketEmployerNameLc = ticketEmployerNameLc;
    }

    public long getActivityId() {
        return activityId;
    }

    public String getDtype() {
        return dtype;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getAssignedToId() {
        return assignedToId;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public boolean isWaitingOnUs() {
        return waitingOnUs;
    }

    public int getDaysSinceContact() {
        return daysSinceContact;
    }

    public boolean isDelegatedToMe() {
        return delegatedToMe;
    }

    public int getDueBucket() {
        return dueBucket;
    }

    public String getTicketEmployerNameLc() {
        return ticketEmployerNameLc;
    }
}

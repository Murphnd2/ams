package net.superiorstate.ams.model.general;

import java.sql.Date;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;

public class TimeStretch {
    private Long id;
    private Person user;
    private Date inDate;
    private Time inTime;
    private Date outDate;
    private Time outTime;

    // ── NEW: actual TimeLog IDs for correction requests ──
    private Long inLogId;
    private Long outLogId;

    public TimeStretch(){}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getUser() { return user; }
    public void setUser(Person user) { this.user = user; }

    public Date getInDate() { return inDate; }
    public void setInDate(Date inDate) { this.inDate = inDate; }

    public Time getInTime() { return inTime; }
    public void setInTime(Time inTime) { this.inTime = inTime; }

    public Date getOutDate() { return outDate; }
    public void setOutDate(Date outDate) { this.outDate = outDate; }

    public Time getOutTime() { return outTime; }
    public void setOutTime(Time outTime) { this.outTime = outTime; }

    public Long getInLogId() { return inLogId; }
    public void setInLogId(Long inLogId) { this.inLogId = inLogId; }

    public Long getOutLogId() { return outLogId; }
    public void setOutLogId(Long outLogId) { this.outLogId = outLogId; }

    /** True if this stretch is complete (has both in and out) */
    public boolean isComplete() {
        return inTime != null && outTime != null;
    }

    /** Minutes worked — uses Duration.between (fixes old compareTo bug) */
    public int getMinutesWorked() {
        if (inDate == null || inTime == null) return 0;
        LocalDateTime timeIn = LocalDateTime.of(inDate.toLocalDate(), inTime.toLocalTime());
        LocalDateTime timeOut;
        try {
            if (outDate != null && outTime != null) {
                timeOut = LocalDateTime.of(outDate.toLocalDate(), outTime.toLocalTime());
            } else {
                // Still clocked in — count up to now
                timeOut = LocalDateTime.now();
            }
        } catch (Exception e) {
            return 0;
        }
        return (int) Duration.between(timeIn, timeOut).toMinutes();
    }

    /** Formatted as "Xh Ym" */
    public String getMinutesFormatted() {
        int mins = getMinutesWorked();
        if (mins <= 0) return "";
        return (mins / 60) + "h " + String.format("%02d", mins % 60) + "m";
    }
}

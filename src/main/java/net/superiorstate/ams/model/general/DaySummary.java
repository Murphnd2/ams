package net.superiorstate.ams.model.general;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DaySummary {
    private Date date;
    private String dayLabel;       // "Mon", "Tue", etc.
    private List<TimeStretch> stretches;
    private boolean isToday;

    public DaySummary() {
        this.stretches = new ArrayList<>();
    }

    public DaySummary(Date date, String dayLabel, boolean isToday) {
        this.date = date;
        this.dayLabel = dayLabel;
        this.isToday = isToday;
        this.stretches = new ArrayList<>();
    }

    public int getTotalMinutes() {
        int total = 0;
        for (TimeStretch ts : stretches) {
            if (ts.getInDate() != null && ts.getInTime() != null) {
                LocalDateTime timeIn = LocalDateTime.of(
                        ts.getInDate().toLocalDate(), ts.getInTime().toLocalTime());
                LocalDateTime timeOut;
                if (ts.getOutDate() != null && ts.getOutTime() != null) {
                    timeOut = LocalDateTime.of(
                            ts.getOutDate().toLocalDate(), ts.getOutTime().toLocalTime());
                } else {
                    // Still clocked in — count up to now
                    timeOut = LocalDateTime.now();
                }
                total += (int) Duration.between(timeIn, timeOut).toMinutes();
            }
        }
        return total;
    }

    /** Formatted as "Xh Ym" */
    public String getTotalFormatted() {
        int mins = getTotalMinutes();
        if (mins <= 0) return "—";
        return (mins / 60) + "h " + String.format("%02d", mins % 60) + "m";
    }

    /** Percentage of an 8-hour day (capped at 137% for display = 11h max scale) */
    public double getPercentOfDay() {
        return Math.min((getTotalMinutes() / 480.0) * 100.0, 137.5);
    }

    /** True if over 8 hours */
    public boolean isOvertime() {
        return getTotalMinutes() > 480;
    }

    // ── Getters / Setters ──

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getDayLabel() { return dayLabel; }
    public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }

    public List<TimeStretch> getStretches() { return stretches; }
    public void setStretches(List<TimeStretch> stretches) { this.stretches = stretches; }

    public boolean isToday() { return isToday; }
    public void setToday(boolean today) { isToday = today; }
}

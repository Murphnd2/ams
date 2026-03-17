package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "training_video")
public class TrainingVideo {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "filename", columnDefinition = "varchar(255)", nullable = false)
    private String filename;

    @Column(name = "title", columnDefinition = "varchar(255)", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public TrainingVideo() {}

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFormattedDuration() {
        if (durationSeconds == null || durationSeconds == 0) return "";
        int mins = durationSeconds / 60;
        int secs = durationSeconds % 60;
        return mins + ":" + String.format("%02d", secs);
    }
}

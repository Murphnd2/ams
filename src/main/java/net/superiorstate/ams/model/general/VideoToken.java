package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "video_token")
public class VideoToken {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "token", columnDefinition = "varchar(36)", nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "video_id", nullable = false)
    private TrainingVideo video;

    @Column(name = "recipient_name", columnDefinition = "varchar(200)")
    private String recipientName;

    @Column(name = "recipient_email", columnDefinition = "varchar(255)")
    private String recipientEmail;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Person createdBy;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "first_viewed_at")
    private Timestamp firstViewedAt;

    @Column(name = "last_viewed_at")
    private Timestamp lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "max_views", nullable = false)
    private int maxViews = 1;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @Column(name = "ip_address", columnDefinition = "varchar(45)")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "varchar(500)")
    private String userAgent;

    public VideoToken() {}

    @PrePersist
    private void prePersist() {
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }
    }

    /** Check if this token is still valid for viewing */
    public boolean isValid() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        // Expired by date?
        if (expiresAt != null && expiresAt.before(now)) return false;
        // Under max views?
        if (viewCount < maxViews) return true;
        // Within 2-hour grace period after first view?
        if (firstViewedAt != null) {
            long gracePeriodMs = 2 * 60 * 60 * 1000L; // 2 hours
            return now.getTime() - firstViewedAt.getTime() < gracePeriodMs;
        }
        return false;
    }

    /** Record a view (call only on landing page, not on stream requests) */
    public void recordView(String ipAddress, String userAgent) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (firstViewedAt == null) {
            this.firstViewedAt = now;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
        }
        this.lastViewedAt = now;
        this.viewCount++;
    }

    public String getStatus() {
        if (expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()))) return "Expired";
        if (viewCount >= maxViews) {
            if (firstViewedAt != null) {
                long gracePeriodMs = 2 * 60 * 60 * 1000L;
                if (System.currentTimeMillis() - firstViewedAt.getTime() < gracePeriodMs) return "Viewed (grace)";
            }
            return "Consumed";
        }
        if (viewCount > 0) return "Viewed";
        return "Unused";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public TrainingVideo getVideo() { return video; }
    public void setVideo(TrainingVideo video) { this.video = video; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public Person getCreatedBy() { return createdBy; }
    public void setCreatedBy(Person createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getFirstViewedAt() { return firstViewedAt; }
    public void setFirstViewedAt(Timestamp firstViewedAt) { this.firstViewedAt = firstViewedAt; }

    public Timestamp getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(Timestamp lastViewedAt) { this.lastViewedAt = lastViewedAt; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getMaxViews() { return maxViews; }
    public void setMaxViews(int maxViews) { this.maxViews = maxViews; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}

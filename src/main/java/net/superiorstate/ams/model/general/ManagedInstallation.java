package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "managed_installation")
public class ManagedInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "installation_name", nullable = false, length = 100)
    private String installationName;

    @Column(name = "installation_url", nullable = false, length = 255)
    private String installationUrl;

    @Column(name = "system_type", nullable = false, length = 10)
    private String systemType;

    @Column(name = "api_token_outbound", length = 64)
    private String apiTokenOutbound;

    @Column(name = "api_token_inbound", length = 64)
    private String apiTokenInbound;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "last_schema_version", length = 20)
    private String lastSchemaVersion;

    @Column(name = "last_app_version", length = 20)
    private String lastAppVersion;

    @Column(name = "last_user_count")
    private Integer lastUserCount;

    @Column(name = "date_registered", nullable = false)
    private Date dateRegistered;

    @Column(name = "date_approved")
    private Date dateApproved;

    @Column(name = "date_disconnected")
    private Date dateDisconnected;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    public ManagedInstallation() {}

    @PrePersist
    private void setDefaults() {
        if (this.dateRegistered == null) {
            this.dateRegistered = Date.valueOf(LocalDate.now());
        }
        this.isActive = true;
    }

    public boolean isConnected() {
        return "ACTIVE".equals(status) && isActive;
    }

    // --- Getters/Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstallationName() { return installationName; }
    public void setInstallationName(String installationName) { this.installationName = installationName; }

    public String getInstallationUrl() { return installationUrl; }
    public void setInstallationUrl(String installationUrl) { this.installationUrl = installationUrl; }

    public String getSystemType() { return systemType; }
    public void setSystemType(String systemType) { this.systemType = systemType; }

    public String getApiTokenOutbound() { return apiTokenOutbound; }
    public void setApiTokenOutbound(String apiTokenOutbound) { this.apiTokenOutbound = apiTokenOutbound; }

    public String getApiTokenInbound() { return apiTokenInbound; }
    public void setApiTokenInbound(String apiTokenInbound) { this.apiTokenInbound = apiTokenInbound; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    /** Formatted heartbeat for JSP display (fmt:formatDate doesn't support LocalDateTime) */
    public String getLastHeartbeatFormatted() {
        if (lastHeartbeat == null) return null;
        return lastHeartbeat.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
    }

    public String getLastSchemaVersion() { return lastSchemaVersion; }
    public void setLastSchemaVersion(String lastSchemaVersion) { this.lastSchemaVersion = lastSchemaVersion; }

    public String getLastAppVersion() { return lastAppVersion; }
    public void setLastAppVersion(String lastAppVersion) { this.lastAppVersion = lastAppVersion; }

    public Integer getLastUserCount() { return lastUserCount; }
    public void setLastUserCount(Integer lastUserCount) { this.lastUserCount = lastUserCount; }

    public Date getDateRegistered() { return dateRegistered; }
    public void setDateRegistered(Date dateRegistered) { this.dateRegistered = dateRegistered; }

    public Date getDateApproved() { return dateApproved; }
    public void setDateApproved(Date dateApproved) { this.dateApproved = dateApproved; }

    public Date getDateDisconnected() { return dateDisconnected; }
    public void setDateDisconnected(Date dateDisconnected) { this.dateDisconnected = dateDisconnected; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

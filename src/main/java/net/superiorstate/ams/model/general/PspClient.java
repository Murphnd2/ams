package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.sql.Date;

@Entity
@Table(name="psp_clients")
public class PspClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="client_id")
    private Long id;

    @Column(name="psp_name", nullable=false, length=100)
    private String pspName;

    @Column(name="psp_url", nullable=false, length=255)
    private String pspUrl;

    @Column(name="api_token_outbound", length=64)
    private String apiTokenOutbound;

    @Column(name="api_token_inbound", length=64)
    private String apiTokenInbound;

    @Column(name="status", nullable=false, length=20)
    private String status = "PENDING";

    @Column(name="auto_accept_tasks", nullable=false)
    private boolean autoAcceptTasks;

    @Column(name="date_requested")
    private Date dateRequested;

    @Column(name="date_approved")
    private Date dateApproved;

    @Column(name="date_disconnected")
    private Date dateDisconnected;

    @Column(name="is_active", nullable=false)
    private boolean isActive = true;

    public PspClient() {}

    @PrePersist
    private void setDefaults() {
        if (dateRequested == null) dateRequested = Date.valueOf(java.time.LocalDate.now());
        isActive = true;
    }

    // --- Getters/Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPspName() {
        return pspName;
    }

    public void setPspName(String pspName) {
        this.pspName = pspName;
    }

    public String getPspUrl() {
        return pspUrl;
    }

    public void setPspUrl(String pspUrl) {
        this.pspUrl = pspUrl;
    }

    public String getApiTokenOutbound() {
        return apiTokenOutbound;
    }

    public void setApiTokenOutbound(String apiTokenOutbound) {
        this.apiTokenOutbound = apiTokenOutbound;
    }

    public String getApiTokenInbound() {
        return apiTokenInbound;
    }

    public void setApiTokenInbound(String apiTokenInbound) {
        this.apiTokenInbound = apiTokenInbound;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAutoAcceptTasks() {
        return autoAcceptTasks;
    }

    public void setAutoAcceptTasks(boolean autoAcceptTasks) {
        this.autoAcceptTasks = autoAcceptTasks;
    }

    public Date getDateRequested() {
        return dateRequested;
    }

    public void setDateRequested(Date dateRequested) {
        this.dateRequested = dateRequested;
    }

    public Date getDateApproved() {
        return dateApproved;
    }

    public void setDateApproved(Date dateApproved) {
        this.dateApproved = dateApproved;
    }

    public Date getDateDisconnected() {
        return dateDisconnected;
    }

    public void setDateDisconnected(Date dateDisconnected) {
        this.dateDisconnected = dateDisconnected;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

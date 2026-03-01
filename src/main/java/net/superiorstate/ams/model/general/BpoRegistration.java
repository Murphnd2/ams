package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.sql.Date;

@Entity
@Table(name="bpo_registration")
public class BpoRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="bpo_reg_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="psp_id", nullable=false)
    private PSP psp;

    @Column(name="bpo_name", nullable=false, length=100)
    private String bpoName;

    @Column(name="bpo_url", length=255)
    private String bpoUrl;

    @Column(name="is_active", nullable=false, columnDefinition = "boolean default true")
    private boolean isActive;

    @Column(name="is_approved", nullable=false, columnDefinition = "boolean default false")
    private boolean isApproved;

    @Column(name="is_requested", nullable=false, columnDefinition = "boolean default false")
    private boolean isRequested;

    @Column(name="is_accepted", nullable=false, columnDefinition = "boolean default false")
    private boolean isAccepted;

    @Column(name="date_registered", nullable=false)
    private Date dateRegistered;

    public BpoRegistration() {}

    @PrePersist
    private void setDefaults() {
        if (this.dateRegistered == null) {
            this.dateRegistered = Date.valueOf(java.time.LocalDate.now());
        }
        this.isActive = true;
    }

    /** A BPO is available for task sourcing when all four flags are true */
    public boolean isAvailable() {
        return isActive && isApproved && isRequested && isAccepted;
    }

    // --- Getters/Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public String getBpoName() {
        return bpoName;
    }

    public void setBpoName(String bpoName) {
        this.bpoName = bpoName;
    }

    public String getBpoUrl() {
        return bpoUrl;
    }

    public void setBpoUrl(String bpoUrl) {
        this.bpoUrl = bpoUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public boolean isRequested() {
        return isRequested;
    }

    public void setRequested(boolean requested) {
        isRequested = requested;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean accepted) {
        isAccepted = accepted;
    }

    public Date getDateRegistered() {
        return dateRegistered;
    }

    public void setDateRegistered(Date dateRegistered) {
        this.dateRegistered = dateRegistered;
    }
}

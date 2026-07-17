package net.superiorstate.ams.model.billing;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_run")
public class BillingRun {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String MODE_FULL = "FULL";
    public static final String MODE_BILLING_ONLY = "BILLING_ONLY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "mode", nullable = false)
    private String mode;

    @Column(name = "plan_type_supplied", nullable = false)
    private boolean planTypeSupplied;

    @Column(name = "renewals_refreshed", nullable = false)
    private boolean renewalsRefreshed;

    @Column(name = "error_text")
    private String errorText;

    @Column(name = "launched_by")
    private Long launchedById;

    public BillingRun() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isPlanTypeSupplied() {
        return planTypeSupplied;
    }

    public void setPlanTypeSupplied(boolean planTypeSupplied) {
        this.planTypeSupplied = planTypeSupplied;
    }

    public boolean isRenewalsRefreshed() {
        return renewalsRefreshed;
    }

    public void setRenewalsRefreshed(boolean renewalsRefreshed) {
        this.renewalsRefreshed = renewalsRefreshed;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public Long getLaunchedById() {
        return launchedById;
    }

    public void setLaunchedById(Long launchedById) {
        this.launchedById = launchedById;
    }
}

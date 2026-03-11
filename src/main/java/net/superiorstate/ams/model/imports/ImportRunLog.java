package net.superiorstate.ams.model.imports;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "import_run_log")
public class ImportRunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ImportProvider provider;

    @Column(name = "run_by", nullable = false)
    private long runBy;

    @Column(name = "started_on", insertable = false, updatable = false)
    private Timestamp startedOn;

    @Column(name = "completed_on")
    private Timestamp completedOn;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "RUNNING";

    @Column(name = "plan_types_inserted")
    private int planTypesInserted = 0;

    @Column(name = "plan_types_updated")
    private int planTypesUpdated = 0;

    @Column(name = "plan_types_skipped")
    private int planTypesSkipped = 0;

    @Column(name = "employers_inserted")
    private int employersInserted = 0;

    @Column(name = "employers_updated")
    private int employersUpdated = 0;

    @Column(name = "employers_skipped")
    private int employersSkipped = 0;

    @Column(name = "employees_inserted")
    private int employeesInserted = 0;

    @Column(name = "employees_updated")
    private int employeesUpdated = 0;

    @Column(name = "employees_skipped")
    private int employeesSkipped = 0;

    @Column(name = "benefits_inserted")
    private int benefitsInserted = 0;

    @Column(name = "benefits_updated")
    private int benefitsUpdated = 0;

    @Column(name = "benefits_skipped")
    private int benefitsSkipped = 0;

    @Column(name = "service_items_created")
    private int serviceItemsCreated = 0;

    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "errors", columnDefinition = "TEXT")
    private String errors;

    public ImportRunLog() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ImportProvider getProvider() {
        return provider;
    }

    public void setProvider(ImportProvider provider) {
        this.provider = provider;
    }

    public long getRunBy() {
        return runBy;
    }

    public void setRunBy(long runBy) {
        this.runBy = runBy;
    }

    public Timestamp getStartedOn() {
        return startedOn;
    }

    public Timestamp getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(Timestamp completedOn) {
        this.completedOn = completedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPlanTypesInserted() {
        return planTypesInserted;
    }

    public void setPlanTypesInserted(int planTypesInserted) {
        this.planTypesInserted = planTypesInserted;
    }

    public int getPlanTypesUpdated() {
        return planTypesUpdated;
    }

    public void setPlanTypesUpdated(int planTypesUpdated) {
        this.planTypesUpdated = planTypesUpdated;
    }

    public int getPlanTypesSkipped() {
        return planTypesSkipped;
    }

    public void setPlanTypesSkipped(int planTypesSkipped) {
        this.planTypesSkipped = planTypesSkipped;
    }

    public int getEmployersInserted() {
        return employersInserted;
    }

    public void setEmployersInserted(int employersInserted) {
        this.employersInserted = employersInserted;
    }

    public int getEmployersUpdated() {
        return employersUpdated;
    }

    public void setEmployersUpdated(int employersUpdated) {
        this.employersUpdated = employersUpdated;
    }

    public int getEmployersSkipped() {
        return employersSkipped;
    }

    public void setEmployersSkipped(int employersSkipped) {
        this.employersSkipped = employersSkipped;
    }

    public int getEmployeesInserted() {
        return employeesInserted;
    }

    public void setEmployeesInserted(int employeesInserted) {
        this.employeesInserted = employeesInserted;
    }

    public int getEmployeesUpdated() {
        return employeesUpdated;
    }

    public void setEmployeesUpdated(int employeesUpdated) {
        this.employeesUpdated = employeesUpdated;
    }

    public int getEmployeesSkipped() {
        return employeesSkipped;
    }

    public void setEmployeesSkipped(int employeesSkipped) {
        this.employeesSkipped = employeesSkipped;
    }

    public int getBenefitsInserted() {
        return benefitsInserted;
    }

    public void setBenefitsInserted(int benefitsInserted) {
        this.benefitsInserted = benefitsInserted;
    }

    public int getBenefitsUpdated() {
        return benefitsUpdated;
    }

    public void setBenefitsUpdated(int benefitsUpdated) {
        this.benefitsUpdated = benefitsUpdated;
    }

    public int getBenefitsSkipped() {
        return benefitsSkipped;
    }

    public void setBenefitsSkipped(int benefitsSkipped) {
        this.benefitsSkipped = benefitsSkipped;
    }

    public int getServiceItemsCreated() {
        return serviceItemsCreated;
    }

    public void setServiceItemsCreated(int serviceItemsCreated) {
        this.serviceItemsCreated = serviceItemsCreated;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }
}

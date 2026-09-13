package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * V107 — the curated payroll-frequency reference registry the enrollment matrix dropdown
 * ({@code EnrollmentMatrixParticipant#getPayrollFrequency()}) reads from.
 * <p>
 * s53b (read-only investigation) found nothing in AMS to hang this on: the only existing
 * representation is {@code applicationfield.paycycle_frequency}, a pipe-delimited string in
 * one column that a JSON reseed overwrites, holding neither the Summit-flagged
 * first-4-of-5 / first-2-of-3 variants nor a Summit schedule name. This table is the
 * config-mapped payroll-frequency → schedule-name registry
 * {@code docs/analysis/summit_import_spec.md} build rule 11 already calls for, rather than
 * literal strings in code.
 * <p>
 * <b>Name note:</b> a dead, never-committed {@code PayFrequency} class exists only as
 * commented-out code in {@code ReferenceDataSeeder.java:337-356}. This entity is deliberately
 * named {@code PayrollFrequency}, not {@code PayFrequency} — that commented-out code is left
 * exactly as it is, not uncommented, not referenced.
 * <p>
 * <b>{@code code} is what {@code enrollment_matrix_participant.payroll_frequency} stores</b>
 * — a plain string column there (V106), not a foreign key, following that column's own
 * documented rationale (code-validated later, not a database {@code ENUM}). This table
 * supplies the curated set of values that column may hold, plus the two sentinel members
 * below, which are never rows here.
 * <p>
 * <b>{@code enrollmentApproved}</b> is the curated flag: only rows with
 * {@code enrollment_approved = 1 AND active = 1} are eligible for the matrix dropdown
 * ({@link net.superiorstate.ams.data.dao.PayrollFrequencyDAO#findEnrollmentApproved}).
 * A new row defaults to not-approved, so entering test data cannot silently open the matrix
 * to a value Kevin has not designated. Approval and the Summit-flagged first-4-of-5 /
 * first-2-of-3 distinction are administered here, not built or guessed by this run.
 * <p>
 * <b>{@code applicationValue}</b> is a plain string, deliberately not a foreign key to
 * {@code applicationfield} — it maps this row from a {@code paycycle_frequency} answer
 * string, for defaulting the matrix dropdown from the application, and is looked up by
 * value ({@link net.superiorstate.ams.data.dao.PayrollFrequencyDAO#findByApplicationValue}),
 * never by id.
 * <p>
 * <b>The table ships empty.</b> No row is seeded by V107, {@code DatabaseInitializer}, or
 * {@code ReferenceDataSeeder} — rows are Kevin's, created through
 * {@code PayrollFrequencyAdmin} when testing requires them.
 */
@Entity
@Table(name = "payroll_frequency")
public class PayrollFrequency {

    /**
     * Servlet-level sentinel codes for {@code EnrollmentMatrixParticipant.payrollFrequency}
     * — never rows in this table. {@code OTHER_CUSTOM} reveals a custom Summit schedule name
     * on the participant row; {@code OTHER_NOT_IMPORTABLE} excludes that participant's
     * entries from the FTP export. Declared here so a caller validating a {@code code} against
     * this table's rows has the two exceptions in the same place as the table they are
     * exceptions to.
     */
    public static final String OTHER_CUSTOM = "OTHER_CUSTOM";
    public static final String OTHER_NOT_IMPORTABLE = "OTHER_NOT_IMPORTABLE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "periods_per_year")
    private Integer periodsPerYear;

    @Column(name = "summit_schedule_name")
    private String summitScheduleName;

    @Column(name = "application_value")
    private String applicationValue;

    @Column(name = "enrollment_approved", nullable = false)
    private boolean enrollmentApproved = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public PayrollFrequency() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Integer getPeriodsPerYear() { return periodsPerYear; }
    public void setPeriodsPerYear(Integer periodsPerYear) { this.periodsPerYear = periodsPerYear; }

    public String getSummitScheduleName() { return summitScheduleName; }
    public void setSummitScheduleName(String summitScheduleName) { this.summitScheduleName = summitScheduleName; }

    public String getApplicationValue() { return applicationValue; }
    public void setApplicationValue(String applicationValue) { this.applicationValue = applicationValue; }

    public boolean isEnrollmentApproved() { return enrollmentApproved; }
    public void setEnrollmentApproved(boolean enrollmentApproved) { this.enrollmentApproved = enrollmentApproved; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}

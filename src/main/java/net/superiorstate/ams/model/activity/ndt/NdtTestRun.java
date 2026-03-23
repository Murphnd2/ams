package net.superiorstate.ams.model.activity.ndt;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Assignee;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ndt_test_run")
public class NdtTestRun {

    // Status constants
    public static final String STATUS_DATA_COLLECTION = "data_collection";
    public static final String STATUS_PARSING = "parsing";
    public static final String STATUS_GAP_ANALYSIS = "gap_analysis";
    public static final String STATUS_READY_TO_TEST = "ready_to_test";
    public static final String STATUS_TESTING = "testing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_ERROR = "error";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_run_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Assignee activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psp_id", nullable = false)
    private PSP psp;

    @Column(name = "employer_name", nullable = false, length = 200)
    private String employerName;

    @Column(name = "plan_year_end", nullable = false)
    private Date planYearEnd;

    @Column(name = "status", nullable = false, length = 30)
    private String status = STATUS_DATA_COLLECTION;

    // JSON data columns
    @Lob
    @Column(name = "plan_data", columnDefinition = "LONGTEXT")
    private String planData;

    @Lob
    @Column(name = "census_data", columnDefinition = "LONGTEXT")
    private String censusData;

    @Lob
    @Column(name = "test_results", columnDefinition = "LONGTEXT")
    private String testResults;

    @Lob
    @Column(name = "gap_analysis", columnDefinition = "LONGTEXT")
    private String gapAnalysis;

    @Lob
    @Column(name = "parse_log", columnDefinition = "LONGTEXT")
    private String parseLog;

    // Metadata
    @Column(name = "employee_count", nullable = false)
    private int employeeCount = 0;

    @Column(name = "document_count", nullable = false)
    private int documentCount = 0;

    @Column(name = "data_quality_score", precision = 5, scale = 2)
    private BigDecimal dataQualityScore;

    // Audit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Person createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;

    @Column(name = "submitted_at")
    private Timestamp submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private Person submittedBy;

    // Child relationships
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt DESC")
    private List<NdtDocumentUpload> documents = new ArrayList<>();

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Assignee getActivity() { return activity; }
    public void setActivity(Assignee activity) { this.activity = activity; }

    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }

    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }

    public Date getPlanYearEnd() { return planYearEnd; }
    public void setPlanYearEnd(Date planYearEnd) { this.planYearEnd = planYearEnd; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPlanData() { return planData; }
    public void setPlanData(String planData) { this.planData = planData; }

    public String getCensusData() { return censusData; }
    public void setCensusData(String censusData) { this.censusData = censusData; }

    public String getTestResults() { return testResults; }
    public void setTestResults(String testResults) { this.testResults = testResults; }

    public String getGapAnalysis() { return gapAnalysis; }
    public void setGapAnalysis(String gapAnalysis) { this.gapAnalysis = gapAnalysis; }

    public String getParseLog() { return parseLog; }
    public void setParseLog(String parseLog) { this.parseLog = parseLog; }

    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }

    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

    public BigDecimal getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(BigDecimal dataQualityScore) { this.dataQualityScore = dataQualityScore; }

    public Person getCreatedBy() { return createdBy; }
    public void setCreatedBy(Person createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }

    public Person getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(Person submittedBy) { this.submittedBy = submittedBy; }

    public List<NdtDocumentUpload> getDocuments() { return documents; }
    public void setDocuments(List<NdtDocumentUpload> documents) { this.documents = documents; }

    // =========================================================================
    // Convenience
    // =========================================================================

    public String getStatusLabel() {
        if (status == null) return "Unknown";
        return switch (status) {
            case STATUS_DATA_COLLECTION -> "Data Collection";
            case STATUS_PARSING -> "Parsing";
            case STATUS_GAP_ANALYSIS -> "Gap Analysis";
            case STATUS_READY_TO_TEST -> "Ready to Test";
            case STATUS_TESTING -> "Testing";
            case STATUS_COMPLETED -> "Completed";
            case STATUS_ERROR -> "Error";
            default -> status;
        };
    }

    public String getStatusBadgeClass() {
        if (status == null) return "bg-secondary";
        return switch (status) {
            case STATUS_DATA_COLLECTION -> "bg-info";
            case STATUS_PARSING -> "bg-warning text-dark";
            case STATUS_GAP_ANALYSIS -> "bg-warning text-dark";
            case STATUS_READY_TO_TEST -> "bg-primary";
            case STATUS_TESTING -> "bg-primary";
            case STATUS_COMPLETED -> "bg-success";
            case STATUS_ERROR -> "bg-danger";
            default -> "bg-secondary";
        };
    }
}

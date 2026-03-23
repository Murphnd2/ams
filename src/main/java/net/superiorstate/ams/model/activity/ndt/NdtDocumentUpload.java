package net.superiorstate.ams.model.activity.ndt;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "ndt_document_upload")
public class NdtDocumentUpload {

    // Document type constants
    public static final String TYPE_CENSUS = "census";
    public static final String TYPE_PAYROLL = "payroll";
    public static final String TYPE_OWNERSHIP = "ownership";
    public static final String TYPE_BILLING = "billing";
    public static final String TYPE_ENROLLMENT = "enrollment";
    public static final String TYPE_OTHER = "other";

    // Parse status constants
    public static final String PARSE_PENDING = "pending";
    public static final String PARSE_PARSING = "parsing";
    public static final String PARSE_PARSED = "parsed";
    public static final String PARSE_FAILED = "failed";
    public static final String PARSE_REJECTED = "rejected";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private NdtTestRun testRun;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType = TYPE_OTHER;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes = 0;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // Parse tracking
    @Column(name = "parse_status", nullable = false, length = 20)
    private String parseStatus = PARSE_PENDING;

    @Column(name = "parse_confidence", precision = 5, scale = 2)
    private BigDecimal parseConfidence;

    @Lob
    @Column(name = "column_mapping", columnDefinition = "LONGTEXT")
    private String columnMapping;

    @Lob
    @Column(name = "parse_errors", columnDefinition = "LONGTEXT")
    private String parseErrors;

    @Column(name = "records_extracted")
    private Integer recordsExtracted;

    // Security
    @Column(name = "virus_scan_status", nullable = false, length = 20)
    private String virusScanStatus = "skipped";

    @Column(name = "ssn_detected", nullable = false)
    private boolean ssnDetected = false;

    @Column(name = "ssn_scrubbed", nullable = false)
    private boolean ssnScrubbed = false;

    @Lob
    @Column(name = "pii_scrub_log", columnDefinition = "LONGTEXT")
    private String piiScrubLog;

    // Audit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private Person uploadedBy;

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private Timestamp uploadedAt;

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NdtTestRun getTestRun() { return testRun; }
    public void setTestRun(NdtTestRun testRun) { this.testRun = testRun; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getParseStatus() { return parseStatus; }
    public void setParseStatus(String parseStatus) { this.parseStatus = parseStatus; }

    public BigDecimal getParseConfidence() { return parseConfidence; }
    public void setParseConfidence(BigDecimal parseConfidence) { this.parseConfidence = parseConfidence; }

    public String getColumnMapping() { return columnMapping; }
    public void setColumnMapping(String columnMapping) { this.columnMapping = columnMapping; }

    public String getParseErrors() { return parseErrors; }
    public void setParseErrors(String parseErrors) { this.parseErrors = parseErrors; }

    public Integer getRecordsExtracted() { return recordsExtracted; }
    public void setRecordsExtracted(Integer recordsExtracted) { this.recordsExtracted = recordsExtracted; }

    public String getVirusScanStatus() { return virusScanStatus; }
    public void setVirusScanStatus(String virusScanStatus) { this.virusScanStatus = virusScanStatus; }

    public boolean isSsnDetected() { return ssnDetected; }
    public void setSsnDetected(boolean ssnDetected) { this.ssnDetected = ssnDetected; }

    public boolean isSsnScrubbed() { return ssnScrubbed; }
    public void setSsnScrubbed(boolean ssnScrubbed) { this.ssnScrubbed = ssnScrubbed; }

    public String getPiiScrubLog() { return piiScrubLog; }
    public void setPiiScrubLog(String piiScrubLog) { this.piiScrubLog = piiScrubLog; }

    public Person getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Person uploadedBy) { this.uploadedBy = uploadedBy; }

    public Timestamp getUploadedAt() { return uploadedAt; }

    // =========================================================================
    // Convenience
    // =========================================================================

    public String getDocumentTypeLabel() {
        if (documentType == null) return "Other";
        return switch (documentType) {
            case TYPE_CENSUS -> "Employee Census";
            case TYPE_PAYROLL -> "Payroll Report";
            case TYPE_OWNERSHIP -> "Ownership Declaration";
            case TYPE_BILLING -> "Insurance Billing";
            case TYPE_ENROLLMENT -> "Benefits Enrollment";
            case TYPE_OTHER -> "Other";
            default -> documentType;
        };
    }

    public String getFileSizeFormatted() {
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        if (fileSizeBytes < 1024 * 1024) return String.format("%.1f KB", fileSizeBytes / 1024.0);
        return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
    }

    public String getParseStatusLabel() {
        if (parseStatus == null) return "Unknown";
        return switch (parseStatus) {
            case PARSE_PENDING -> "Pending";
            case PARSE_PARSING -> "Parsing...";
            case PARSE_PARSED -> "Parsed";
            case PARSE_FAILED -> "Failed";
            case PARSE_REJECTED -> "Rejected";
            default -> parseStatus;
        };
    }

    public String getParseStatusBadgeClass() {
        if (parseStatus == null) return "bg-secondary";
        return switch (parseStatus) {
            case PARSE_PENDING -> "bg-secondary";
            case PARSE_PARSING -> "bg-warning text-dark";
            case PARSE_PARSED -> "bg-success";
            case PARSE_FAILED -> "bg-danger";
            case PARSE_REJECTED -> "bg-danger";
            default -> "bg-secondary";
        };
    }
}

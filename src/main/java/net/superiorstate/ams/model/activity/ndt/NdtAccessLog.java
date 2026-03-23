package net.superiorstate.ams.model.activity.ndt;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;

import java.sql.Timestamp;

@Entity
@Table(name = "ndt_access_log")
public class NdtAccessLog {

    // Action constants
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPLOAD = "upload";
    public static final String ACTION_DELETE_UPLOAD = "delete_upload";
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_RUN_TEST = "run_test";
    public static final String ACTION_SUBMIT = "submit";
    public static final String ACTION_EXPORT = "export";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private NdtTestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "accessed_at", insertable = false, updatable = false)
    private Timestamp accessedAt;

    // =========================================================================
    // Constructors
    // =========================================================================

    public NdtAccessLog() {}

    public NdtAccessLog(NdtTestRun testRun, Person person, String action, String detail, String ipAddress) {
        this.testRun = testRun;
        this.person = person;
        this.action = action;
        this.detail = detail;
        this.ipAddress = ipAddress;
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Long getId() { return id; }

    public NdtTestRun getTestRun() { return testRun; }
    public void setTestRun(NdtTestRun testRun) { this.testRun = testRun; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Timestamp getAccessedAt() { return accessedAt; }
}

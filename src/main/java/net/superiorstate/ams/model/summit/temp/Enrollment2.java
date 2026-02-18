package net.superiorstate.ams.model.summit.temp;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.imports.order.ImportEmployee;
import net.superiorstate.ams.model.summit.imports.order.ImportEmployer;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.summit.imports.order.ImportBenefitCdh;
import net.superiorstate.ams.model.summit.imports.order.ImportBenefitYear;

import java.sql.Date;

@Entity
public class Enrollment2 {
    @Id
    @Column(name="enrollment_id")
    private int enrollmentId;

    @ManyToOne
    @JoinColumn(name="participant_id")
    private ImportEmployee importEmployee;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private ImportEmployer importEmployer;

    @ManyToOne
    @JoinColumn(name="benefit_id")
    private ImportBenefitCdh importBenefitCdh;

    @ManyToOne
    @JoinColumn(name="benefit_year_id")
    private ImportBenefitYear importBenefitYear;

    @ManyToOne
    @JoinColumn(name="billing_group_id")
    private BillingGroup billingGroup;

    @Column(name="current_month")
    private Date currentMonth;

    @Column(name="term_date")
    private Date termDate;

    @Column(name="py_start")
    private Date startDate;

    @Column(name="py_end")
    private Date endDate;

    @Column(name="billable")
    private boolean billable;

    @Column(name="employer_name")
    private String employerName;

    @Column(name="participant_name")
    private String participantName;

    @Column(name="benefit_name")
    private String benefitName;

    @Column(name="benefit_group")
    private String benefitGroup;

    @Column(name="card_enabled")
    private boolean cardEnabled;

    public Enrollment2(){}

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public ImportEmployee getImportEmployee() {
        return importEmployee;
    }

    public void setImportEmployee(ImportEmployee importEmployee) {
        this.importEmployee = importEmployee;
    }

    public ImportEmployer getImportEmployer() {
        return importEmployer;
    }

    public void setImportEmployer(ImportEmployer importEmployer) {
        this.importEmployer = importEmployer;
    }

    public ImportBenefitCdh getImportBenefitCdh() {
        return importBenefitCdh;
    }

    public void setImportBenefitCdh(ImportBenefitCdh importBenefitCdh) {
        this.importBenefitCdh = importBenefitCdh;
    }

    public ImportBenefitYear getImportBenefitYear() {
        return importBenefitYear;
    }

    public void setImportBenefitYear(ImportBenefitYear importBenefitYear) {
        this.importBenefitYear = importBenefitYear;
    }

    public BillingGroup getBillingGroup() {
        return billingGroup;
    }

    public void setBillingGroup(BillingGroup billingGroup) {
        this.billingGroup = billingGroup;
    }

    public Date getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(Date currentMonth) {
        this.currentMonth = currentMonth;
    }

    public Date getTermDate() {
        return termDate;
    }

    public void setTermDate(Date termDate) {
        this.termDate = termDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(boolean billable) {
        this.billable = billable;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public String getBenefitGroup() {
        return benefitGroup;
    }

    public void setBenefitGroup(String benefitGroup) {
        this.benefitGroup = benefitGroup;
    }

    public boolean isCardEnabled() {
        return cardEnabled;
    }

    public void setCardEnabled(boolean cardEnabled) {
        this.cardEnabled = cardEnabled;
    }
}

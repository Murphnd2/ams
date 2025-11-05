package net.superiorstate.ams.previous.model.summit.temp;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.billing.BillingGroup;
import net.superiorstate.ams.previous.model.summit.imports.sBenefitYear;
import net.superiorstate.ams.previous.model.summit.imports.sEmployer;
import net.superiorstate.ams.previous.model.summit.imports.sBenefit;
import net.superiorstate.ams.previous.model.summit.imports.sEmployee;

import java.sql.Date;

@Entity
public class Enrollment {
    @Id
    @Column(name="enrollment_id")
    private int enrollmentId;
    @ManyToOne
    @JoinColumn(name="participant_id")
    private sEmployee sEmployee;

    @ManyToOne
    @JoinColumn(name="organization_id")
    private net.superiorstate.ams.previous.model.summit.imports.sEmployer sEmployer;

    @ManyToOne
    @JoinColumn(name="benefit_id")
    private sBenefit sBenefit;
    @ManyToOne
    @JoinColumn(name="benefit_year_id")
    private net.superiorstate.ams.previous.model.summit.imports.sBenefitYear sBenefitYear;

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

    public Enrollment(){}

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public sEmployee getSummitEmployee() {
        return sEmployee;
    }

    public void setSummitEmployee(sEmployee sEmployee) {
        this.sEmployee = sEmployee;
    }

    public sEmployer getSummitOrganization() {
        return sEmployer;
    }

    public void setSummitOrganization(sEmployer sEmployer) {
        this.sEmployer = sEmployer;
    }

    public sBenefit getSummitBenefit() {
        return sBenefit;
    }

    public void setSummitBenefit(sBenefit sBenefit) {
        this.sBenefit = sBenefit;
    }

    public sBenefitYear getSummitBenefitYear() {
        return sBenefitYear;
    }

    public void setSummitBenefitYear(sBenefitYear sBenefitYear) {
        this.sBenefitYear = sBenefitYear;
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

    public String getBillingId(){
        return getCurrentMonth().toString() + "-" + getBillingGroup().getId() + "-" + getSummitEmployee().getId();
    }
}

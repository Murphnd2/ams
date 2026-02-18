package net.superiorstate.ams.model.summit.temp;

import jakarta.persistence.*;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.summit.archive.PlanType;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.sql.Date;

@Entity
public class Coverage {
    @Id
    @Column(name="coverage_id")
    private int coverageId;

    @ManyToOne
    @JoinColumn(name="organization_id")
    private Employer employer;

    @ManyToOne
    @JoinColumn(name="participant_id")
    private Employee employee;

    @Column(name="employer_id")
    private int employerId;

    @ManyToOne
    @JoinColumn(name="plan_type_id")
    private net.superiorstate.ams.model.summit.archive.PlanType PlanType;

    @Column(name="status")
    private String status;

    @Column(name="is_billable")
    private boolean isBillable;

    @Column(name="current_month")
    private Date currentMonth;

    @Column(name="pb_benefit_id")
    private int pbBenefitId;

    @Column(name="benefit_name")
    private String benefitName;

    @ManyToOne
    @JoinColumn(name="billing_group_id")
    private BillingGroup billingGroup;

    @Column(name="tier_name")
    private String tierName;

    public Coverage(){}

    public int getCoverageId() {
        return coverageId;
    }

    public void setCoverageId(int coverageId) {
        this.coverageId = coverageId;
    }

    public Employer getSummitOrganization() {
        return employer;
    }

    public void setSummitOrganization(Employer employer) {
        this.employer = employer;
    }
    public Employee getSummitEmployee() {
        return employee;
    }

    public void setSummitEmployee(Employee employee) {
        this.employee = employee;
    }

    public int getEmployerId() {
        return employerId;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public PlanType getSummitPlanType() {
        return PlanType;
    }

    public void setSummitPlanType(PlanType PlanType) {
        this.PlanType = PlanType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBillable() {
        return isBillable;
    }

    public void setBillable(boolean billable) {
        isBillable = billable;
    }

    public Date getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(Date currentMonth) {
        this.currentMonth = currentMonth;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public BillingGroup getBillingGroup() {
        return billingGroup;
    }

    public void setBillingGroup(BillingGroup billingGroup) {
        this.billingGroup = billingGroup;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }


    public int getPbBenefitId() {
        return pbBenefitId;
    }

    public void setPbBenefitId(int pbBenefitId) {
        this.pbBenefitId = pbBenefitId;
    }

    public String getBillingId(){
        return getCurrentMonth().toString() + "-" + getBillingGroup().getId() + "-" + getSummitEmployee().getId();
    }
}

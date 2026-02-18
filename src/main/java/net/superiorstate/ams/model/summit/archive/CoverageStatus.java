package net.superiorstate.ams.model.summit.archive;

import jakarta.persistence.*;
import net.superiorstate.ams.model.billing.BillingGroup;

import java.sql.Date;

@Entity
public class CoverageStatus {

    @Id
    @Column(name="coverage_status_id")
    private String id;

    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @ManyToOne
    @JoinColumn(name="employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name="benefit_id")
    private Benefit benefit;

    @ManyToOne
    @JoinColumn(name="billing_group_id")
    private BillingGroup billingGroup;

    @Column(name="month_for")
    private Date monthFor;

    @Column(name="active")
    private boolean isActive;

    @Column(name="cards")
    private boolean hasCards;

    public CoverageStatus (){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public void setBenefit(Benefit benefit) {
        this.benefit = benefit;
    }

    public Date getMonthFor() {
        return monthFor;
    }

    public void setMonthFor(Date monthFor) {
        this.monthFor = monthFor;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isHasCards() {
        return hasCards;
    }

    public void setHasCards(boolean hasCards) {
        this.hasCards = hasCards;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    public BillingGroup getBillingGroup() {
        return billingGroup;
    }

    public void setBillingGroup(BillingGroup billingGroup) {
        this.billingGroup = billingGroup;
    }
}

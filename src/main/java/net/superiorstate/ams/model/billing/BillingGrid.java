package net.superiorstate.ams.model.billing;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

@Entity
public class BillingGrid {
    @Id
    @Column(name="grid_id")
    private String gridId;

    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @ManyToOne
    @JoinColumn(name="employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name="month_id")
    private BillingMonth billingMonth;

    @Column(name="current_status")
    private String currentStatus;

    @Column(name="fsa")
    private boolean flexSpend;

    @Column(name="hra")
    private boolean healthReimb;

    @Column(name="dual_plan")
    private boolean dualPlan;

    @Column(name="cobra",columnDefinition = "boolean default false")
    private boolean cobra;

    @Column(name="transit",columnDefinition = "boolean default false")
    private boolean transit;

    @Column(name="hsa",columnDefinition = "boolean default false")
    private boolean hsa;

    @Column(name="lsa",columnDefinition = "boolean default false")
    private boolean lsa;

    @Column(name="retiree",columnDefinition = "boolean default false")
    private boolean retiree;

    @Column(name="direct",columnDefinition = "boolean default false")
    private boolean direct;

    public BillingGrid(){}

    public String getGridId() {
        return gridId;
    }

    public void setGridId(String gridId) {
        this.gridId = gridId;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public BillingMonth getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(BillingMonth billingMonth) {
        this.billingMonth = billingMonth;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public boolean isCobra() {
        return cobra;
    }

    public void setCobra(boolean cobra) {
        this.cobra = cobra;
    }

    public boolean isTransit() {
        return transit;
    }

    public void setTransit(boolean transit) {
        this.transit = transit;
    }

    public boolean isHsa() {
        return hsa;
    }

    public void setHsa(boolean hsa) {
        this.hsa = hsa;
    }

    public boolean isLsa() {
        return lsa;
    }

    public void setLsa(boolean lsa) {
        this.lsa = lsa;
    }

    public boolean isRetiree() {
        return retiree;
    }

    public void setRetiree(boolean retiree) {
        this.retiree = retiree;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public boolean isFlexSpend() {
        return flexSpend;
    }

    public void setFlexSpend(boolean flexSpend) {
        this.flexSpend = flexSpend;
    }

    public boolean isHealthReimb() {
        return healthReimb;
    }

    public void setHealthReimb(boolean healthReimb) {
        this.healthReimb = healthReimb;
    }

    public boolean isDualPlan() {
        return dualPlan;
    }

    public void setDualPlan(boolean dualPlan) {
        this.dualPlan = dualPlan;
    }
}

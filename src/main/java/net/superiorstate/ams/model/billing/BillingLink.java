package net.superiorstate.ams.model.billing;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.imports.sEmployer;
import net.superiorstate.ams.model.summit.archive.Employer;

@Entity
public class BillingLink {
    @Id
    @Column(name="billing_id")
    private String billingId;

    @ManyToOne
    @JoinColumn(name="month_id")
    private BillingMonth billingMonth;

    @ManyToOne
    @JoinColumn(name="organization_id")
    private net.superiorstate.ams.model.summit.imports.sEmployer sEmployer;

    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @Column(name="unique_id")
    private String uniqueId;

    public BillingLink(){}

    public String getBillingId() {
        return billingId;
    }

    public void setBillingId(String billingId) {
        this.billingId = billingId;
    }

    public BillingMonth getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(BillingMonth billingMonth) {
        this.billingMonth = billingMonth;
    }

    public net.superiorstate.ams.model.summit.imports.sEmployer getsEmployer() {
        return sEmployer;
    }

    public void setsEmployer(net.superiorstate.ams.model.summit.imports.sEmployer sEmployer) {
        this.sEmployer = sEmployer;
    }


    public sEmployer getSummitOrganization() {
        return sEmployer;
    }

    public void setSummitOrganization(sEmployer sEmployer) {
        this.sEmployer = sEmployer;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }
}

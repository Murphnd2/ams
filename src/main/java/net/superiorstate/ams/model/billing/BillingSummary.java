package net.superiorstate.ams.model.billing;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.archive.Employer;
import org.eclipse.persistence.annotations.ReadOnly;

@Entity
@ReadOnly
@Table(name="billing_summary")
public class BillingSummary {
    @Id
    @Column(name="uuid")
    private String uuid;

    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @ManyToOne
    @JoinColumn(name="month_id")
    private BillingMonth billingMonth;

    @Column(name="cobra_tot")
    private int cobraTotal;

    @Column(name="direct_tot")
    private int directTotal;

    @Column(name="dual_plan_tot")
    private int dualPlanTotal;

    @Column(name="fsa_tot")
    private int fsaTotal;

    @Column(name="hra_tot")
    private int hraTotal;

    @Column(name="lsa_tot")
    private int lsaTotal;

    @Column(name="hsa_tot")
    private int hsaTotal;

    @Column(name="retiree_tot")
    private int retireeTotal;

    @Column(name="transit_tot")
    private int transitTotal;

    public BillingSummary(){}

    public String getUuid() {
        return uuid;
    }

    public Employer getEmployer() {
        return employer;
    }

    public BillingMonth getBillingMonth() {
        return billingMonth;
    }

    public int getCobraTotal() {
        return cobraTotal;
    }

    public int getDirectTotal() {
        return directTotal;
    }

    public int getDualPlanTotal() {
        return dualPlanTotal;
    }

    public int getFsaTotal() {
        return fsaTotal;
    }

    public int getHraTotal() {
        return hraTotal;
    }

    public int getLsaTotal() {
        return lsaTotal;
    }

    public int getHsaTotal() {
        return hsaTotal;
    }

    public int getRetireeTotal() {
        return retireeTotal;
    }

    public int getTransitTotal() {
        return transitTotal;
    }
}

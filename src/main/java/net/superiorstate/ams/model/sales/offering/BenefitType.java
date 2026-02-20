package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

@Entity
@Table(name = "benefittype")
public class BenefitType {
    @Id
    @GeneratedValue
    @Column(name = "benefittype_id")
    private Long id;

    @Column(name = "name", columnDefinition = "varchar(100)", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "default_billingtype_id")
    private BillingType defaultBillingType;

    @ManyToOne
    @JoinColumn(name = "psp_id", nullable = false)
    private PSP psp;

    @Column(name = "sort_order")
    private int sortOrder;

    public BenefitType() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BillingType getDefaultBillingType() { return defaultBillingType; }
    public void setDefaultBillingType(BillingType defaultBillingType) { this.defaultBillingType = defaultBillingType; }
    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

@Entity
@Table(name = "billingtype")
public class BillingType {
    @Id
    @GeneratedValue
    @Column(name = "billingtype_id")
    private Long id;

    @Column(name = "name", columnDefinition = "varchar(50)", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "psp_id", nullable = false)
    private PSP psp;

    @Column(name = "sort_order")
    private int sortOrder;

    public BillingType() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

@Entity
@Table(name = "resourcecategory")
public class ResourceCategory implements Comparable<ResourceCategory> {
    @Id
    @GeneratedValue
    @Column(name = "category_id")
    private Long id;

    @Column(name = "name", columnDefinition = "varchar(50)", nullable = false)
    private String name;

    @Column(name = "icon_class", columnDefinition = "varchar(50)")
    private String iconClass;

    @Column(name = "sort_order")
    private int sortOrder;

    @ManyToOne
    @JoinColumn(name = "psp_id", nullable = false)
    private PSP psp;

    public ResourceCategory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }

    @Override
    public int compareTo(ResourceCategory o) {
        return Integer.compare(this.sortOrder, o.sortOrder);
    }
}
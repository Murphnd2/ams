package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

import java.util.List;

@Entity
public class ModuleDetail {
    @Id
    @GeneratedValue
    @Column(name="service_item_id")
    private Long id;

    @Column(name="description",columnDefinition = "varchar(1000)")
    private String description;

    @Column(name="bullet_point",columnDefinition = "varchar(200)")
    private String bulletPoint;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column
    private boolean suppressed;

    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @ManyToMany(mappedBy = "moduleDetailList")
    List<ServiceModule> listOfModulesWithThisServiceItem;

    public ModuleDetail(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBulletPoint() {
        return bulletPoint;
    }

    public void setBulletPoint(String bulletPoint) {
        this.bulletPoint = bulletPoint;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public List<ServiceModule> getListOfModulesWithThisServiceItem() {
        return listOfModulesWithThisServiceItem;
    }

    public void setListOfModulesWithThisServiceItem(List<ServiceModule> listOfModulesWithThisServiceItem) {
        this.listOfModulesWithThisServiceItem = listOfModulesWithThisServiceItem;
    }
}

package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

import java.util.List;

@Entity
@Table(name="marketingmaterial")
public class MarketingMaterial {
    @Id
    @GeneratedValue
    @Column(name="material_id")
    private Long id;

    @Column(name="title",columnDefinition = "varchar(200)",nullable = false)
    private String title;

    @Column(name="description",columnDefinition = "varchar(500)")
    private String description;

    @Column(name="material_type",columnDefinition = "varchar(20)",nullable = false)
    private String materialType;

    @Column(name="url",columnDefinition = "varchar(500)")
    private String url;

    @Column(name="storage_guid",columnDefinition = "varchar(36)")
    private String storageGuid;

    @Column(name="audience",columnDefinition = "varchar(20)")
    private String audience;

    @Column(name="sort_order")
    private int sortOrder;

    @ManyToOne
    @JoinColumn(name="psp_id",nullable = false)
    private PSP psp;

    @ManyToMany
    @JoinTable(name="materialmodule",
            joinColumns = @JoinColumn(name="material_id"),
            inverseJoinColumns = @JoinColumn(name="module_id"))
    private List<ServiceModule> serviceModuleList;

    public MarketingMaterial(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStorageGuid() {
        return storageGuid;
    }

    public void setStorageGuid(String storageGuid) {
        this.storageGuid = storageGuid;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public List<ServiceModule> getServiceModuleList() {
        return serviceModuleList;
    }

    public void setServiceModuleList(List<ServiceModule> serviceModuleList) {
        this.serviceModuleList = serviceModuleList;
    }
}
package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

@Entity
public class Feature {
    @Id
    @GeneratedValue
    @Column(name="feature_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="module_id",nullable = false)
    private ServiceModule serviceModule;

    @Column(name="description",columnDefinition = "varchar(500)",nullable = false)
    private String description;

    @Column(name="sort_order")
    private int sortOrder;

    @ManyToOne
    @JoinColumn(name="psp_id",nullable = false)
    private PSP psp;

    @ManyToOne
    @JoinColumn(name="library_resource_id")
    private MarketingMaterial libraryResource;

    public Feature(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceModule getServiceModule() {
        return serviceModule;
    }

    public void setServiceModule(ServiceModule serviceModule) {
        this.serviceModule = serviceModule;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public MarketingMaterial getLibraryResource() {
        return libraryResource;
    }

    public void setLibraryResource(MarketingMaterial libraryResource) {
        this.libraryResource = libraryResource;
    }
}

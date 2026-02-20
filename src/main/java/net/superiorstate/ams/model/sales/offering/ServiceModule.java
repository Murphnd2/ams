package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

import java.util.List;

@Entity
public class ServiceModule implements Comparable<ServiceModule> {
    @Id
    @GeneratedValue
    @Column(name="module_id")
    private Long id;

    @Column(name="short_text",columnDefinition = "varchar(20)")
    private String shortText;
    @Column(name="description",columnDefinition = "varchar(100)")
    private String description;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name="app_select")
    private boolean applicationSelectionItem;

    @Column
    private boolean suppressed;

    @ManyToOne
    @JoinColumn(name = "psp_id")
    private PSP psp;

    @ManyToOne
    @JoinColumn(name = "los_id")
    private LOS los;

    @ManyToOne
    @JoinColumn(name = "enhancement_id")
    private Enhancement enhancement;

    @ManyToMany
    @JoinTable(name="moduleitems",
            joinColumns = @JoinColumn(name="module_id"),inverseJoinColumns = @JoinColumn(name="item_id"))
    List<ServiceItem> serviceItemList;

    @ManyToMany(mappedBy = "serviceModuleList")
    List<LOS> listOfLosWithThisModule;

    public ServiceModule(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
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

    public boolean isApplicationSelectionItem() {
        return applicationSelectionItem;
    }

    public void setApplicationSelectionItem(boolean applicationSelectionItem) {
        this.applicationSelectionItem = applicationSelectionItem;
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

    public LOS getLos() {
        return los;
    }

    public void setLos(LOS los) {
        this.los = los;
    }

    public Enhancement getEnhancement() {
        return enhancement;
    }

    public void setEnhancement(Enhancement enhancement) {
        this.enhancement = enhancement;
    }

    public List<ServiceItem> getServiceItemList() {
        return serviceItemList;
    }

    public void setServiceItemList(List<ServiceItem> serviceItemList) {
        this.serviceItemList = serviceItemList;
    }

    public List<LOS> getListOfLosWithThisModule() {
        return listOfLosWithThisModule;
    }

    public void setListOfLosWithThisModule(List<LOS> listOfLosWithThisModule) {
        this.listOfLosWithThisModule = listOfLosWithThisModule;
    }

    public void addServiceItem(ServiceItem serviceItem){
        this.serviceItemList.add(serviceItem);
        serviceItem.getListOfModulesWithThisServiceItem().add(this);
    }

    public void removeServiceItem(ServiceItem serviceItem){
        this.serviceItemList.remove(serviceItem);
        serviceItem.getListOfModulesWithThisServiceItem().remove(this);
    }

    @Override
    public int compareTo(ServiceModule o) {
        if(this.getSortOrder() < o.getSortOrder()) {
            return -1;
        } else if (this.getSortOrder() > o.getSortOrder()){
            return 1;
        } else if (this.getDescription().compareTo(o.getDescription()) !=0) {
            return this.getDescription().compareTo(o.getDescription());
        } else {
            return this.getId().compareTo(o.getId());
        }
    }
}

package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.agency.Proposal;

import java.util.List;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "LOS.getByPsp",
                query = "SELECT los FROM LOS los WHERE los.psp.id = :psp_id ORDER BY los.sortOrder"
        ),
        @NamedQuery(
                name = "LOS.getById",
                query = "SELECT l FROM LOS l WHERE l.id = :los_id"
        )
})
public class LOS implements Comparable<LOS> {
    @Id
    @GeneratedValue
    @Column(name="los_id")
    private Long id;

    @Column(name="description",columnDefinition = "varchar(100)")
    private String description;

    @Column(name="short_text", columnDefinition = "varchar(10)")
    private String shortText;

    @Column(name="sort_order")
    private int sortOrder;

    @Column
    private boolean suppressed;

    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @ManyToOne
    @JoinColumn(name="service_item_id")
    private ServiceItem serviceItem;

    @ManyToMany
    @JoinTable(name="losmodules",
            joinColumns = @JoinColumn(name="los_id"),inverseJoinColumns = @JoinColumn(name="module_id"))
    List<ServiceModule> serviceModuleList;

    @ManyToMany(mappedBy = "losList")
    List<Proposal> listOfProposalsThatIncludeThisLOS;

    @ManyToMany(mappedBy = "losList")
    List<Enhancement> enhancementList;

    public LOS(){}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getShortText() { return shortText; }
    public void setShortText(String shortText) { this.shortText = shortText; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isSuppressed() { return suppressed; }
    public void setSuppressed(boolean suppressed) { this.suppressed = suppressed; }
    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }
    public ServiceItem getServiceItem() { return serviceItem; }
    public void setServiceItem(ServiceItem serviceItem) { this.serviceItem = serviceItem; }

    public List<ServiceModule> getServiceModuleList() { return serviceModuleList; }
    public void setServiceModuleList(List<ServiceModule> serviceModuleList) { this.serviceModuleList = serviceModuleList; }
    public List<Proposal> getListOfProposalsThatIncludeThisLOS() { return listOfProposalsThatIncludeThisLOS; }
    public void setListOfProposalsThatIncludeThisLOS(List<Proposal> listOfProposalsThatIncludeThisLOS) { this.listOfProposalsThatIncludeThisLOS = listOfProposalsThatIncludeThisLOS; }
    public List<Enhancement> getEnhancementList() { return enhancementList; }
    public void setEnhancementList(List<Enhancement> enhancementList) { this.enhancementList = enhancementList; }

    public void addServiceModule(ServiceModule serviceModule){
        this.serviceModuleList.add(serviceModule);
        serviceModule.getListOfLosWithThisModule().remove(this);
    }
    public void removeServiceModule(ServiceModule serviceModule){
        this.serviceModuleList.remove(serviceModule);
        serviceModule.getListOfLosWithThisModule().remove(this);
    }

    @Override
    public int compareTo(LOS o) {
        if (this.sortOrder != o.sortOrder) {
            return Integer.compare(this.sortOrder, o.sortOrder);
        } else if (!this.description.equals(o.description)) {
            return this.description.compareTo(o.description);
        } else {
            return this.id.compareTo(o.id);
        }
    }
}

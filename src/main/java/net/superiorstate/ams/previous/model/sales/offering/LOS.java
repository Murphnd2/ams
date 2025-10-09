package net.superiorstate.ams.previous.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;

import java.util.List;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "LOS.getByPsp",
                query = "SELECT los FROM LOS los WHERE los.psp.id = :psp_id"
        ),
        @NamedQuery(
                name = "LOS.getById",
                query = "SELECT l FROM LOS l WHERE l.id = :los_id"
        )
})
public class LOS {
    @Id
    @GeneratedValue
    @Column(name="los_id")
    private Long id;

    @Column(name="description",columnDefinition = "varchar(100)")
    private String description;

    @Column(name="short_text", columnDefinition = "varchar(10)")
    private String shortText;

    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @ManyToMany
    @JoinTable(name="losmodules",
            joinColumns = @JoinColumn(name="los_id"),inverseJoinColumns = @JoinColumn(name="module_id"))
    List<ServiceModule> serviceModuleList;

    @ManyToMany(mappedBy = "losList")
    List<Proposal> listOfProposalsThatIncludeThisLOS;

    public LOS(){}

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

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
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

    public List<Proposal> getListOfProposalsThatIncludeThisLOS() {
        return listOfProposalsThatIncludeThisLOS;
    }

    public void setListOfProposalsThatIncludeThisLOS(List<Proposal> listOfProposalsThatIncludeThisLOS) {
        this.listOfProposalsThatIncludeThisLOS = listOfProposalsThatIncludeThisLOS;
    }
    public void addServiceModule(ServiceModule serviceModule){
        this.serviceModuleList.add(serviceModule);
        serviceModule.getListOfLosWithThisModule().remove(this);
    }

    public void removeServiceModule(ServiceModule serviceModule){
        this.serviceModuleList.remove(serviceModule);
        serviceModule.getListOfLosWithThisModule().remove(this);
    }
}

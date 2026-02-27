package net.superiorstate.ams.model.activity.checklist.sequences.support;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.application.ApplicationModule;

import java.util.List;

@Entity
@Table(name = "templatepurpose")
public class ServiceItem {

    @Id
    @GeneratedValue
    @Column(name = "purpose_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private ActivityCategory activityCategory;

    @OneToMany(mappedBy = "serviceItem")
    private List<ApplicationModule> applicationModuleList;

    @Column(name = "description", columnDefinition = "varchar(200)")
    private String description;

    @Column(name = "code", columnDefinition = "varchar(20)")
    private String code;

    @Column(name = "sort_order")
    private int sortOrder;

    @ManyToOne
    @JoinColumn(name = "psp_id")
    private PSP psp;

    @Column(name = "is_suppressed")
    private boolean suppressed;

    @Column(name = "provider_ref", columnDefinition = "varchar(100)")
    private String providerRef;

    @Column(name = "source_type", columnDefinition = "varchar(20)")
    private String sourceType;

    @Column(name = "default_renewal_months")
    private Integer defaultRenewalMonths;

    @Column(name = "has_required_tasks")
    private boolean hasRequiredTasks = true;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private TicketCategory ticketCategory;

    public ServiceItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ActivityCategory getActivityCategory() {
        return activityCategory;
    }

    public void setActivityCategory(ActivityCategory activityCategory) {
        this.activityCategory = activityCategory;
    }

    public List<ApplicationModule> getApplicationModuleList() {
        return applicationModuleList;
    }

    public void setApplicationModuleList(List<ApplicationModule> applicationModuleList) {
        this.applicationModuleList = applicationModuleList;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public void setProviderRef(String providerRef) {
        this.providerRef = providerRef;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getDefaultRenewalMonths() {
        return defaultRenewalMonths;
    }

    public void setDefaultRenewalMonths(Integer defaultRenewalMonths) {
        this.defaultRenewalMonths = defaultRenewalMonths;
    }

    public boolean isHasRequiredTasks() {
        return hasRequiredTasks;
    }

    public void setHasRequiredTasks(boolean hasRequiredTasks) {
        this.hasRequiredTasks = hasRequiredTasks;
    }

    public TicketCategory getTicketCategory() {
        return ticketCategory;
    }

    public void setTicketCategory(TicketCategory ticketCategory) {
        this.ticketCategory = ticketCategory;
    }
}

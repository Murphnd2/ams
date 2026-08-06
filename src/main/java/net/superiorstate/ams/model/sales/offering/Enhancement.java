package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.application.ApplicationSection;

import java.util.List;

@Entity
public class Enhancement implements Comparable<Enhancement> {
    @Id
    @GeneratedValue
    @Column(name = "enhancement_id")
    private Long id;

    @Column(name = "description", columnDefinition = "varchar(100)")
    private String description;

    @Column(name = "short_text", columnDefinition = "varchar(20)")
    private String shortText;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column
    private boolean suppressed;

    @Column(name = "system_managed", nullable = false)
    private boolean systemManaged;

    /**
     * S20-B / V091 — which of the four system-managed proposal sections this enhancement
     * drives ({@code ICHRA_MARKET}, {@code ICHRA_CONTRIBUTION}, {@code ICHRA_COMPARISON},
     * {@code ICHRA_AFFORDABILITY} — see {@code docs/analysis/S20A_ichra_sections_spec.md}
     * §1.8/§5.1). The Rule-4-compliant discriminator {@link
     * net.superiorstate.ams.data.resolver.FlaggedEnhancementResolver} needs to identify
     * which payload sub-block a given flagged enhancement's section corresponds to, without
     * ever hardcoding an enhancement id — this column is PSP-scoped by construction, the
     * same reasoning {@code systemManaged} itself follows. Byte-identical to the payload's
     * own {@code sections} key; no mapping layer. Null/blank means flagged but unclassified,
     * which the resolver treats as withheld, same as unflagged-but-unset.
     */
    @Column(name = "system_section_key", columnDefinition = "varchar(32)")
    private String systemSectionKey;

    @ManyToOne
    @JoinColumn(name = "psp_id")
    private PSP psp;

    @ManyToOne
    @JoinColumn(name = "service_item_id")
    private ServiceItem serviceItem;

    @ManyToMany
    @JoinTable(name = "enhancement_los",
            joinColumns = @JoinColumn(name = "enhancement_id"),
            inverseJoinColumns = @JoinColumn(name = "los_id"))
    private List<LOS> losList;

    @ManyToMany(mappedBy = "enhancementList")
    private List<ApplicationSection> applicationSectionList;

    public Enhancement() {}

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

    public boolean isSystemManaged() { return systemManaged; }
    public void setSystemManaged(boolean systemManaged) { this.systemManaged = systemManaged; }

    public String getSystemSectionKey() { return systemSectionKey; }
    public void setSystemSectionKey(String systemSectionKey) { this.systemSectionKey = systemSectionKey; }

    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }

    public ServiceItem getServiceItem() { return serviceItem; }
    public void setServiceItem(ServiceItem serviceItem) { this.serviceItem = serviceItem; }

    public List<LOS> getLosList() { return losList; }
    public void setLosList(List<LOS> losList) { this.losList = losList; }

    public List<ApplicationSection> getApplicationSectionList() { return applicationSectionList; }
    public void setApplicationSectionList(List<ApplicationSection> applicationSectionList) { this.applicationSectionList = applicationSectionList; }

    public void addLos(LOS los) {
        this.losList.add(los);
        los.getEnhancementList().add(this);
    }

    public void removeLos(LOS los) {
        this.losList.remove(los);
        los.getEnhancementList().remove(this);
    }

    @Override
    public int compareTo(Enhancement o) {
        if (this.sortOrder != o.sortOrder) {
            return Integer.compare(this.sortOrder, o.sortOrder);
        } else if (!this.description.equals(o.description)) {
            return this.description.compareTo(o.description);
        } else {
            return this.id.compareTo(o.id);
        }
    }
}

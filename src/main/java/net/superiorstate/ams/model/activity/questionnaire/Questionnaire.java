package net.superiorstate.ams.model.activity.questionnaire;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.general.PSP;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questionnaire")
public class Questionnaire implements Comparable<Questionnaire> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "questionnaire_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psp_id", nullable = false)
    private PSP psp;

    @Column(name = "name", columnDefinition = "varchar(100)", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "varchar(500)")
    private String description;

    @Column(name = "activity_type", columnDefinition = "varchar(20) DEFAULT 'ALL'", nullable = false)
    private String activityType = "ALL";

    @Column(name = "external_url", columnDefinition = "varchar(500)")
    private String externalUrl;

    @Column(name = "renderer", columnDefinition = "varchar(30)")
    private String renderer;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "suppressed", columnDefinition = "TINYINT")
    private boolean suppressed;

    @Column(name = "template_key", columnDefinition = "varchar(50)")
    private String templateKey;

    @ManyToMany
    @JoinTable(name = "questionnaire_serviceitem",
            joinColumns = @JoinColumn(name = "questionnaire_id"),
            inverseJoinColumns = @JoinColumn(name = "purpose_id"))
    private List<ServiceItem> serviceItemList = new ArrayList<>();

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuestionnaireField> fieldList;

    public Questionnaire() {}

    /** True if this questionnaire points to an external form (Jotform, etc.) */
    public boolean isExternal() {
        return externalUrl != null && !externalUrl.isBlank();
    }

    /** True if this questionnaire uses native AMS fields (no external URL). */
    public boolean isNative() {
        return externalUrl == null || externalUrl.isBlank();
    }

    /** True if any ServiceItem scoping is defined. */
    public boolean isScopedToServices() {
        return !serviceItemList.isEmpty();
    }

    /** Resolves merge tokens in the external URL. Returns null if not external. */
    public String resolveExternalUrl(String erName, Long activityId, String instanceGuid) {
        if (!isExternal()) return null;
        String resolved = externalUrl;
        if (erName != null) resolved = resolved.replace("{erName}", erName);
        if (activityId != null) resolved = resolved.replace("{activityId}", String.valueOf(activityId));
        if (instanceGuid != null) resolved = resolved.replace("{instanceGuid}", instanceGuid);
        return resolved;
    }

    @Override
    public int compareTo(Questionnaire o) {
        return Integer.compare(this.sortOrder, o.sortOrder);
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isSuppressed() { return suppressed; }
    public void setSuppressed(boolean suppressed) { this.suppressed = suppressed; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public List<ServiceItem> getServiceItemList() { return serviceItemList; }
    public void setServiceItemList(List<ServiceItem> serviceItemList) { this.serviceItemList = serviceItemList; }

    public String getRenderer() { return renderer; }
    public void setRenderer(String renderer) { this.renderer = renderer; }

    /** Returns renderer key, defaulting to "standard" when null/blank. */
    public String getRendererOrDefault() {
        return (renderer != null && !renderer.isBlank()) ? renderer : "standard";
    }

    public List<QuestionnaireField> getFieldList() { return fieldList; }
    public void setFieldList(List<QuestionnaireField> fieldList) { this.fieldList = fieldList; }
}

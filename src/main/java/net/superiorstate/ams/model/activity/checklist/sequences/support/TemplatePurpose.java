package net.superiorstate.ams.model.activity.checklist.sequences.support;

import jakarta.persistence.*;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import net.superiorstate.ams.model.sales.application.DataKey;

import java.util.List;

@Entity
public class TemplatePurpose {

    @Id
    @GeneratedValue
    @Column(name="purpose_id")
    private int id;

    @ManyToOne
    @JoinColumn(name="group_id")
    private TemplateGroup templateGroup;

    @OneToMany(mappedBy = "templatePurpose")
    private List<ApplicationModule> applicationModuleList;

    @Column(name="description",columnDefinition = "varchar(200)")
    private String description;

    @Column(name="sort_order")
    private int sortOrder;

    @OneToMany(mappedBy = "templatePurpose")
    List<DataKey> dataKeyList;

    public TemplatePurpose(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TemplateGroup getTemplateGroup() {
        return templateGroup;
    }

    public void setTemplateGroup(TemplateGroup templateGroup) {
        this.templateGroup = templateGroup;
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

    public List<ApplicationModule> getApplicationModuleList() {
        return applicationModuleList;
    }

    public void setApplicationModuleList(List<ApplicationModule> applicationModuleList) {
        this.applicationModuleList = applicationModuleList;
    }

    public List<DataKey> getDataKeyList() {
        return dataKeyList;
    }

    public void setDataKeyList(List<DataKey> dataKeyList) {
        this.dataKeyList = dataKeyList;
    }
}

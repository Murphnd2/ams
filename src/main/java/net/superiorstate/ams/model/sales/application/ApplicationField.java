package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;

@Entity
@Table(name="applicationfield")
public class ApplicationField {
    @Id
    @Column(name="field_key",columnDefinition = "varchar(100)")
    private String fieldKey;

    @Column(name="label",columnDefinition = "varchar(200)")
    private String label;

    @ManyToOne
    @JoinColumn(name="template_purpose_id")
    private TemplatePurpose templatePurpose;

    @Column(name="field_type",columnDefinition = "varchar(20) DEFAULT 'TEXT'")
    private String fieldType;

    @Column(name="is_required",columnDefinition = "TINYINT")
    private boolean isRequired;

    @Column(name="sort_order")
    private int sortOrder;

    @Column(name="select_options",columnDefinition = "varchar(500)")
    private String selectOptions;

    public ApplicationField(){}

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getLabel() {
        if(label == null)
            return fieldKey;
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public TemplatePurpose getTemplatePurpose() {
        return templatePurpose;
    }

    public void setTemplatePurpose(TemplatePurpose templatePurpose) {
        this.templatePurpose = templatePurpose;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSelectOptions() {
        return selectOptions;
    }

    public void setSelectOptions(String selectOptions) {
        this.selectOptions = selectOptions;
    }
}
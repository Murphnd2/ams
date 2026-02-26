package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="applicationfield")
public class ApplicationField {
    @Id
    @Column(name="field_key",columnDefinition = "varchar(100)")
    private String fieldKey;

    @Column(name="label",columnDefinition = "varchar(200)")
    private String label;

    @ManyToOne
    @JoinColumn(name="section_id")
    private ApplicationSection applicationSection;

    @Column(name="help_text",columnDefinition = "varchar(500)")
    private String helpText;

    @Column(name="field_type",columnDefinition = "varchar(20) DEFAULT 'TEXT'")
    private String fieldType;

    @Column(name="is_required",columnDefinition = "TINYINT")
    private boolean isRequired;

    @Column(name="sort_order")
    private int sortOrder;

    @Column(name="select_options",columnDefinition = "varchar(500)")
    private String selectOptions;

    @Column(columnDefinition = "TINYINT")
    private boolean suppressed;

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

    public ApplicationSection getApplicationSection() {
        return applicationSection;
    }

    public void setApplicationSection(ApplicationSection applicationSection) {
        this.applicationSection = applicationSection;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
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

    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public List<String> getSelectOptionsList() {
        if (selectOptions == null || selectOptions.isEmpty()) return new java.util.ArrayList<>();
        return java.util.Arrays.asList(selectOptions.split("\\|"));
    }
}

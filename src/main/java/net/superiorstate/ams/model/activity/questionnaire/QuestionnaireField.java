package net.superiorstate.ams.model.activity.questionnaire;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "questionnaire_field")
public class QuestionnaireField implements Comparable<QuestionnaireField> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id", nullable = false)
    private Questionnaire questionnaire;

    @Column(name = "field_key", columnDefinition = "varchar(100)", nullable = false)
    private String fieldKey;

    @Column(name = "label", columnDefinition = "varchar(200)")
    private String label;

    @Column(name = "field_type", columnDefinition = "varchar(20) DEFAULT 'TEXT'")
    private String fieldType = "TEXT";

    @Column(name = "select_options", columnDefinition = "varchar(500)")
    private String selectOptions;

    @Column(name = "help_text", columnDefinition = "varchar(500)")
    private String helpText;

    @Column(name = "section_name", columnDefinition = "varchar(100)")
    private String sectionName;

    @Column(name = "is_required", columnDefinition = "TINYINT")
    private boolean isRequired;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "suppressed", columnDefinition = "TINYINT")
    private boolean suppressed;

    public QuestionnaireField() {}

    /** Splits pipe-delimited select_options into a list. Same pattern as ApplicationField. */
    public List<String> getSelectOptionsList() {
        if (selectOptions == null || selectOptions.isEmpty()) return new ArrayList<>();
        return Arrays.asList(selectOptions.split("\\|"));
    }

    @Override
    public int compareTo(QuestionnaireField o) {
        return Integer.compare(this.sortOrder, o.sortOrder);
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Questionnaire getQuestionnaire() { return questionnaire; }
    public void setQuestionnaire(Questionnaire questionnaire) { this.questionnaire = questionnaire; }

    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }

    public String getLabel() {
        if (label == null) return fieldKey;
        return label;
    }
    public void setLabel(String label) { this.label = label; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public String getSelectOptions() { return selectOptions; }
    public void setSelectOptions(String selectOptions) { this.selectOptions = selectOptions; }

    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { isRequired = required; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isSuppressed() { return suppressed; }
    public void setSuppressed(boolean suppressed) { this.suppressed = suppressed; }
}

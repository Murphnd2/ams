package net.superiorstate.ams.model.activity.questionnaire;

import jakarta.persistence.*;

@Entity
@Table(name = "questionnaire_field_value")
public class QuestionnaireFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_value_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private QuestionnaireInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private QuestionnaireField field;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    public QuestionnaireFieldValue() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QuestionnaireInstance getInstance() { return instance; }
    public void setInstance(QuestionnaireInstance instance) { this.instance = instance; }

    public QuestionnaireField getField() { return field; }
    public void setField(QuestionnaireField field) { this.field = field; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }
}

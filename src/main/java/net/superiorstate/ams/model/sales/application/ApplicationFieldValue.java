package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;

@Entity
@Table(name="applicationfieldvalue")
public class ApplicationFieldValue {
    @Id
    @GeneratedValue
    @Column(name="field_value_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="application_id",nullable = false)
    private Application application;

    @ManyToOne
    @JoinColumn(name="field_key",nullable = false)
    private ApplicationField applicationField;

    @Column(name="field_value",columnDefinition = "TEXT")
    private String fieldValue;

    public ApplicationFieldValue(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public ApplicationField getApplicationField() {
        return applicationField;
    }

    public void setApplicationField(ApplicationField applicationField) {
        this.applicationField = applicationField;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ApplicationModuleID implements Serializable {
    private Long applicationId;
    private int templatePurposeId;
    public ApplicationModuleID(){}

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public int getTemplatePurposeId() {
        return templatePurposeId;
    }

    public void setTemplatePurposeId(int templatePurposeId) {
        this.templatePurposeId = templatePurposeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationModuleID)) return false;
        ApplicationModuleID that = (ApplicationModuleID) o;
        return Objects.equals(getApplicationId(), that.getApplicationId()) && Objects.equals(getTemplatePurposeId(), that.getTemplatePurposeId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApplicationId(), getTemplatePurposeId());
    }
}

package net.superiorstate.ams.previous.model.sales.application;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ApplicationDataID implements Serializable {
    private Long applicationId;
    private Long dataPairId;

    public ApplicationDataID(){}

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getDataPairId() {
        return dataPairId;
    }

    public void setDataPairId(Long dataPairId) {
        this.dataPairId = dataPairId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationDataID)) return false;
        ApplicationDataID that = (ApplicationDataID) o;
        return Objects.equals(getApplicationId(), that.getApplicationId()) && Objects.equals(getDataPairId(), that.getDataPairId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApplicationId(), getDataPairId());
    }
}

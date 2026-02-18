package net.superiorstate.ams.model.sales.agency;


import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RateTableID implements Serializable {
    private Long moduleId;
    private Long priceItemId;
    private Long rateId;

    public RateTableID(){}

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public Long getPriceItemId() {
        return priceItemId;
    }

    public void setPriceItemId(Long priceItemId) {
        this.priceItemId = priceItemId;
    }

    public Long getRateId() {
        return rateId;
    }

    public void setRateId(Long rateId) {
        this.rateId = rateId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RateTableID)) return false;
        RateTableID that = (RateTableID) o;
        return getModuleId().equals(that.getModuleId()) && getPriceItemId().equals(that.getPriceItemId()) && getRateId().equals(that.getRateId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getModuleId(), getPriceItemId(), getRateId());
    }
}

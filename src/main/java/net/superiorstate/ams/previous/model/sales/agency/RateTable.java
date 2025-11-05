package net.superiorstate.ams.previous.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

@Entity
public class RateTable {

    @EmbeddedId
    private RateTableID rateTableID;
    @Id
    @ManyToOne
    @MapsId("rateId")
    @JoinColumn(name="rate_id")
    private Rate rate;

    @Id
    @ManyToOne
    @MapsId("priceItemId")
    @JoinColumn(name="price_item_id")
    private PriceItem priceItem;

    @Id
    @ManyToOne
    @MapsId("moduleId")
    @JoinColumn(name="module_id")
    private ServiceModule module;

    @Column(name="price")
    private double price;

    public RateTable(){}

    public RateTableID getRateTableID() {
        return rateTableID;
    }

    public void setRateTableID(RateTableID rateTableID) {
        this.rateTableID = rateTableID;
    }

    public Rate getRate() {
        return rate;
    }

    public void setRate(Rate rate) {
        this.rate = rate;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public void setPriceItem(PriceItem priceItem) {
        this.priceItem = priceItem;
    }

    public ServiceModule getModule() {
        return module;
    }

    public void setModule(ServiceModule module) {
        this.module = module;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}

package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.util.List;

@Entity
@Table(name="ratediscount")
public class RateDiscount {
    @Id
    @GeneratedValue
    @Column(name="ratediscount_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="rate_id",nullable = false)
    private Rate rate;

    @Column(name="description",columnDefinition = "varchar(200)",nullable = false)
    private String description;

    @Column(name="discount_amount",nullable = false)
    private double discountAmount;

    @ManyToOne
    @JoinColumn(name="price_item_id",nullable = false)
    private PriceItem priceItem;

    @ManyToMany
    @JoinTable(name="ratediscountlos",
            joinColumns = @JoinColumn(name="ratediscount_id"),
            inverseJoinColumns = @JoinColumn(name="los_id"))
    private List<LOS> requiredLosList;

    public RateDiscount(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Rate getRate() {
        return rate;
    }

    public void setRate(Rate rate) {
        this.rate = rate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public void setPriceItem(PriceItem priceItem) {
        this.priceItem = priceItem;
    }

    public List<LOS> getRequiredLosList() {
        return requiredLosList;
    }

    public void setRequiredLosList(List<LOS> requiredLosList) {
        this.requiredLosList = requiredLosList;
    }
}
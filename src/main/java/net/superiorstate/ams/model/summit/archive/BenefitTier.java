package net.superiorstate.ams.model.summit.archive;

import jakarta.persistence.*;

import java.sql.Date;

@Entity
public class BenefitTier {
    @Id
    @Column(name="benefit_tier_id")
    private String id;

    @ManyToOne
    @JoinColumn(name="benefit_id")
    private Benefit benefit;

    @Column(name="tier_name")
    private String tierName;

    @Column(name="tier_description")
    private String tierDescription;

    @Column(name="start_date")
    private Date startDate;

    @Column(name="end_date")
    private Date endDate;

    public BenefitTier(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public void setBenefit(Benefit benefit) {
        this.benefit = benefit;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public String getTierDescription() {
        return tierDescription;
    }

    public void setTierDescription(String tierDescription) {
        this.tierDescription = tierDescription;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}

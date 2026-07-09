package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.sql.Timestamp;

@Entity
@Table(name = "proposal_price_adjustment")
public class ProposalPriceAdjustment {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne
    @JoinColumn(name = "module_id", nullable = false)
    private ServiceModule module;

    @ManyToOne
    @JoinColumn(name = "price_item_id", nullable = false)
    private PriceItem priceItem;

    @Column(name = "markup_amount", nullable = false)
    private double markupAmount;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Person createdBy;

    @Column(name = "date_created", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    public ProposalPriceAdjustment(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public ServiceModule getModule() {
        return module;
    }

    public void setModule(ServiceModule module) {
        this.module = module;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public void setPriceItem(PriceItem priceItem) {
        this.priceItem = priceItem;
    }

    public double getMarkupAmount() {
        return markupAmount;
    }

    public void setMarkupAmount(double markupAmount) {
        this.markupAmount = markupAmount;
    }

    public Person getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Person createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }
}

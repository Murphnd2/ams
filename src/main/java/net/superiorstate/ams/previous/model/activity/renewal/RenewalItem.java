package net.superiorstate.ams.previous.model.activity.renewal;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;

import java.sql.Date;

@Entity
public class RenewalItem {

    @Id
    @GeneratedValue
    @Column(name="renewal_item_id")
    private Long id;

    @Column(name="date_for")
    private Date dateFor;

    @ManyToOne
    @JoinColumn(name="benefit_id")
    private Benefit benefit;

    @ManyToOne
    @JoinColumn(name="renewal_id")
    private Renewal renewal;

    public RenewalItem(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDateFor() {
        return dateFor;
    }

    public void setDateFor(Date dateFor) {
        this.dateFor = dateFor;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public void setBenefit(Benefit benefit) {
        this.benefit = benefit;
    }

    public Renewal getRenewal() {
        return renewal;
    }

    public void setRenewal(Renewal renewal) {
        this.renewal = renewal;
    }
}

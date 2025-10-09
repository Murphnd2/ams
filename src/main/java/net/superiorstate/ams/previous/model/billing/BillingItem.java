package net.superiorstate.ams.previous.model.billing;

import jakarta.persistence.*;

@Entity
public class BillingItem {

    @Id
    @Column(name="billing_id")
    private String billingId;

    @ManyToOne
    @JoinColumn(name="month_id")
    private BillingMonth billingMonth;

    @Column(name="employer_id")
    private int employerId;

    @Column(name="participant_id")
    private int participantId;

    @Column(name="benefit_group_id")
    private int benefitGroupId;

    @Column(name="employer")
    private String employerName;

    @Column(name="participant")
    private String participantName;

    @Column(name="benefit")
    private String benefitGroup;

    @Column(name="card")
    private boolean haveCards;

    public BillingItem(){}

    public String getBillingId() {
        return billingId;
    }

    public void setBillingId(String billingId) {
        this.billingId = billingId;
    }

    public BillingMonth getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(BillingMonth billingMonth) {
        this.billingMonth = billingMonth;
    }

    public int getEmployerId() {
        return employerId;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public int getBenefitGroupId() {
        return benefitGroupId;
    }

    public void setBenefitGroupId(int benefitGroupId) {
        this.benefitGroupId = benefitGroupId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getBenefitGroup() {
        return benefitGroup;
    }

    public void setBenefitGroup(String benefitGroup) {
        this.benefitGroup = benefitGroup;
    }

    public boolean isHaveCards() {
        return haveCards;
    }

    public void setHaveCards(boolean haveCards) {
        this.haveCards = haveCards;
    }
}

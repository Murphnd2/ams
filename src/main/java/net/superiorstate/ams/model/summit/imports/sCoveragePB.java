package net.superiorstate.ams.model.summit.imports;

import jakarta.persistence.*;

@Entity
public class sCoveragePB {

    @Column(name="SSN")
    private String ssn;

    @Column(name="EmployerName")
    private String employerName;

    @Column(name="EmployerCustomID")
    private String employerCustomId;

    @Column(name="TransactionStartDate")
    private String transactionStartDate;

    @Column(name="TransactionEndDate")
    private String transactionEndDate;

    @Column(name="ParticipantLastName")
    private String participantLastName;

    @Column(name="ParticipantFirstName")
    private String participantFirstName;

    @Column(name="ParticipantCustomID")
    private String participantCustomId;

    @ManyToOne
    @JoinColumn(name="Participant_ID")
    private sEmployee2 sEmployee2;

    @Column(name="ImportPlanID")
    private String importPlanId;

    @Column(name="BenefitID")
    private int benefitId;

    @Id
    @Column(name="PBCoverageHeaderID")
    private int coverageId;

    @Column(name="CoverageStatus")
    private String coverageStatus;

    @Column(name="PlanType")
    private String planType;

    @Column(name="BenefitName")
    private String benefitName;

    @Column(name="TierName")
    private String tierName;

    @Column(name="EmployerID")
    private int employerId;

    @Column(name="EmployerDivisionID")
    private String employerDivisionId;

    @Column(name="EmployerDivision")
    private String employerDivision;

    @Column(name="EffectiveDate")
    private String effectiveDate;

    public sCoveragePB(){}

    public sEmployee2 getSummitEmployee() {
        return sEmployee2;
    }

    public int getBenefitId() {
        return benefitId;
    }

    public int getCoverageId() {
        return coverageId;
    }

    public String getCoverageStatus() {
        return coverageStatus;
    }

    public String getPlanType() {
        return planType;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public String getTierName() {
        return tierName;
    }

    public int getEmployerId() {
        return employerId;
    }
}

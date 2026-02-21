package net.superiorstate.ams.model.activity;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;

import java.sql.Date;

@Entity
public class Opportunity extends Activity {

    @ManyToOne
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @ManyToOne
    @JoinColumn(name = "agency_id_opp")
    private Agency agency;

    @OneToOne
    @JoinColumn(name = "checklist_id")
    private CheckList checkList;

    @Column(name = "opportunity_stage", columnDefinition = "varchar(30) DEFAULT 'NEW'")
    private String stage;

    @Column(name = "estimated_employees")
    private Integer estimatedEmployees;

    @Column(name = "estimated_value")
    private Double estimatedValue;

    @Column(name = "expected_close_date")
    private Date expectedCloseDate;

    public Opportunity() {}

    public Prospect getProspect() {
        return prospect;
    }

    public void setProspect(Prospect prospect) {
        this.prospect = prospect;
    }

    public Agency getAgency() {
        return agency;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public void setCheckList(CheckList checkList) {
        this.checkList = checkList;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getEstimatedEmployees() {
        return estimatedEmployees;
    }

    public void setEstimatedEmployees(Integer estimatedEmployees) {
        this.estimatedEmployees = estimatedEmployees;
    }

    public Double getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(Double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public Date getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(Date expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    @Override
    public String getFullName() {
        if (this.getProspect() != null)
            return this.getProspect().getName().trim().toUpperCase();
        else if (this.getPrimaryContact() != null)
            return this.getPrimaryContact().getFirstName().trim().toUpperCase() + " " + this.getPrimaryContact().getLastName().trim().toUpperCase();
        else return "OPPORTUNITY";
    }
}

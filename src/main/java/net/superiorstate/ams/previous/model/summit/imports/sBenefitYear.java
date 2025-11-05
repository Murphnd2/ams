package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class sBenefitYear {

    @Column(name="Organization_ID")
    private String organizationID;

    @Column(name="Employer")
    private String employer;

    @Column(name="PlanYear")
    private String planYear;

    @Column(name="PlanYear_ID")
    private int planYearId;

    @ManyToOne
    @JoinColumn(name="EmployerPlan_ID")
    private sBenefit sBenefit;
    @Id
    @Column(name="EmployerPlanDetailForPlanYear_ID")
    private int benefitYearId;

    @Column(name="PlanName")
    private String planName;

    @Column(name="PlanDescription")
    private String planDescription;

    @Column(name="ContributionSchedule")
    private String contributionSchedule;

    @Column(name="ContributionScheduleTemplate_ID")
    private String contributionScheduleTemplateId;

    @Column(name="PlanStatus")
    private String planStatus;

    public sBenefitYear(){}

    public int getBenefitYearId() {
        return benefitYearId;
    }

    public int getPlanYearId() {
        return planYearId;
    }

   public sBenefit getSummitBenefit() {
        return sBenefit;
    }

    public String getContributionSchedule() {
        return contributionSchedule;
    }


    public LocalDate getPlanYearStart() {
        int month = Integer.parseInt(planYear.substring(0,2));
        int day = Integer.parseInt(planYear.substring(3,5));
        int year = Integer.parseInt(planYear.substring(6,10));
        LocalDate localDate = LocalDate.of(year,month,day);
        return localDate;
    }
    public LocalDate getPlanYearEnd(){
        String planYear1 = planYear.substring(11,21);
        int month1 = Integer.parseInt(planYear1.substring(0,2));
        int day1 = Integer.parseInt(planYear1.substring(3,5));
        int year1 = Integer.parseInt(planYear1.substring(6,10));
        LocalDate localDate = LocalDate.of(year1,month1,day1);
        return localDate;
    }

    public boolean isActive(){
        boolean isActive = planStatus.trim() == "Active";
        return isActive;
    }


}

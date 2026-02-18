package net.superiorstate.ams.model.summit.imports.order;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name="import5benefityear")
public class ImportBenefitYear {
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
    private ImportBenefitCdh importBenefitCdh;

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

    public ImportBenefitYear(){}

    public String getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(String organizationID) {
        this.organizationID = organizationID;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getPlanYear() {
        return planYear;
    }

    public void setPlanYear(String planYear) {
        this.planYear = planYear;
    }

    public int getPlanYearId() {
        return planYearId;
    }

    public void setPlanYearId(int planYearId) {
        this.planYearId = planYearId;
    }

    public ImportBenefitCdh getImportBenefitCdh() {
        return importBenefitCdh;
    }

    public void setImportBenefitCdh(ImportBenefitCdh importBenefitCdh) {
        this.importBenefitCdh = importBenefitCdh;
    }

    public int getBenefitYearId() {
        return benefitYearId;
    }

    public void setBenefitYearId(int benefitYearId) {
        this.benefitYearId = benefitYearId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanDescription() {
        return planDescription;
    }

    public void setPlanDescription(String planDescription) {
        this.planDescription = planDescription;
    }

    public String getContributionSchedule() {
        return contributionSchedule;
    }

    public void setContributionSchedule(String contributionSchedule) {
        this.contributionSchedule = contributionSchedule;
    }

    public String getContributionScheduleTemplateId() {
        return contributionScheduleTemplateId;
    }

    public void setContributionScheduleTemplateId(String contributionScheduleTemplateId) {
        this.contributionScheduleTemplateId = contributionScheduleTemplateId;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
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
}

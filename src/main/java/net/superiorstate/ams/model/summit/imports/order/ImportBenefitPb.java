package net.superiorstate.ams.model.summit.imports.order;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.sql.Date;
import java.time.LocalDate;

@Entity
@Table(name="import7benefitpb")
public class ImportBenefitPb {
    @Id
    @Column(name="BenefitID")
    private int benefitId;

    @Column(name="PBBenefitID")
    private int pbBenefitId;

    @Id
    @Column(name="PlanYearID")
    private int planYearId;

    @Column(name="BenefitName")
    private String benefitName;

    @ManyToOne
    @JoinColumn(name="OrganizationID")
    private ImportEmployer importEmployer;

    @ManyToOne
    @JoinColumn(name="PlanTypeID")
    private PlanType planType;

    @Column(name="EmployerID")
    private int employerIdAlt;

    @Column(name="EffectiveDate")
    private String effectiveDate;

    @Column(name="StartDate")
    private String startDate;

    @Column(name="EndDate")
    private String endDate;

    public ImportBenefitPb(){}

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public int getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(int benefitId) {
        this.benefitId = benefitId;
    }

    public int getPbBenefitId() {
        return pbBenefitId;
    }

    public void setPbBenefitId(int pbBenefitId) {
        this.pbBenefitId = pbBenefitId;
    }

    public int getPlanYearId() {
        return planYearId;
    }

    public void setPlanYearId(int planYearId) {
        this.planYearId = planYearId;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public ImportEmployer getImportEmployer() {
        return importEmployer;
    }

    public void setImportEmployer(ImportEmployer importEmployer) {
        this.importEmployer = importEmployer;
    }

    public int getEmployerIdAlt() {
        return employerIdAlt;
    }

    public void setEmployerIdAlt(int employerIdAlt) {
        this.employerIdAlt = employerIdAlt;
    }

    public Date getEffectiveDate() {
        return getDateValue(effectiveDate);
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getStartDate() {
        return getDateValue(startDate);
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return getDateValue(endDate);
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    private Date getDateValue(String dateString){

        int space = dateString.indexOf(" ");
        if(space==-1)
            return Date.valueOf(LocalDate.of(2000,1,1));
        int fs = dateString.indexOf("/");
        int ss = dateString.indexOf("/",fs+1);
        String theMonth = dateString.substring(0,fs);
        String theDay = dateString.substring(fs+1,ss);
        String theYear = dateString.substring(ss+1,space);
        LocalDate ld = LocalDate.of(Integer.parseInt(theYear.trim()),Integer.parseInt(theMonth.trim()),Integer.parseInt(theDay.trim()));
        return Date.valueOf(ld);

    }

}

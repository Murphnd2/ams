package net.superiorstate.ams.model.summit.imports;

import jakarta.persistence.*;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.sql.Date;
import java.time.LocalDate;

@Entity
public class sBenefit {

    @Column(name="Employer_ID")
    private String employerId;

    @Column(name="EmployerName")
    private String employerName;

    @ManyToOne
    @JoinColumn(name="OrganizationID")
    private sEmployer sEmployer;
    @Id
    @Column(name="EmployerPlan_ID")
    private int benefitId;

    @ManyToOne
    @JoinColumn(name="PlanTypeID")
    private net.superiorstate.ams.model.summit.archive.PlanType PlanType;

    @Column(name="PlanName")
    private String planName;

    @Column(name="PlanDescription")
    private String planDescription;

    @Column(name="ImportPlanID")
    private String importPlanId;

    @Column(name="PlanType")
    private String planType;

    @Column(name="EffectiveDate")
    private String effectiveDate;

    @Column(name="TerminationDate")
    private String terminationDate;

    @Column(name="PlanStatus")
    private String planStatus;

    @Column(name="LinkedtoDefaultPlan")
    private String linkedToDefaultPlan;

    @Column(name="CardEnabled")
    private String cardEnabled;


    public sBenefit(){}

    public sEmployer getSummitEmployer() {
        return sEmployer;
    }

    public int getBenefitId() {
        return benefitId;
    }

    public PlanType getSummitPlanType() {
        return PlanType;
    }

    public String getPlanName() {
        return planName;
    }

    public String getPlanDescription() {
        return planDescription;
    }

    private String getEffectiveDate() {
        return effectiveDate;
    }

    private String getTerminationDate() {
        return terminationDate;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public String getCardEnabled() {
        return cardEnabled;
    }

    public boolean getCardEnabledBoolean(){
        boolean it = getCardEnabled().trim().equals("True");
        return it;
    }

    public Date getPlanTerminationDate(){
        Date theDate = null;
        try{
            theDate = Date.valueOf(LocalDate.of(findYear(getTerminationDate()),findMonth(getTerminationDate()),findDay(getTerminationDate())));
        } catch (Exception e){
            e.printStackTrace();
        }
        return theDate;
    }

    public Date getPlanEffectiveDate(){
        Date theDate = null;
        try{
            theDate = Date.valueOf(LocalDate.of(findYear(getEffectiveDate()),findMonth(getEffectiveDate()),findDay(getEffectiveDate())));
        } catch (Exception e){
            e.printStackTrace();
        }
        return theDate;
    }

    private int findMonth(String dateString){
        int firstSlash = dateString.indexOf("/");
        return Integer.parseInt(dateString.substring(0,firstSlash));
    }

    private int findDay(String dateString){
        int firstSlash = dateString.indexOf("/");
        int secondSlash = dateString.indexOf("/",firstSlash+1);
        return Integer.parseInt(dateString.substring(firstSlash+1,secondSlash));
    }

    private int findYear(String dateString){
        int firstSlash = dateString.indexOf("/");
        int secondSlash = dateString.indexOf("/",firstSlash+1);
        return Integer.parseInt(dateString.substring(secondSlash+1,secondSlash+5));
    }
}

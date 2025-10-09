package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
public class sEnrollment {
    @ManyToOne
    @JoinColumn(name="Participant_ID")
    private sEmployee sEmployee;

    @Column(name="OrganizationID")
    private String organizationId;

    @Column(name="DivisionOrganizationID")
    private String divisionOrganizationId;

    @Column(name="DivisionOrganizationName")
    private String divisionOrganizationName;

    @Column(name="DivisionCustomID")
    private String divisionCustomId;

    @Column(name="EmployerID")
    private String employerId;

    @Column(name="EmployerName")
    private String employerName;

    @Column(name="EmployerPlan_ID")
    private String employerPlanId;

    @ManyToOne
    @JoinColumn(name="EmployerPlanDetailForPlanYearID")
    private sBenefitYear sBenefitYear;

    @Id
    @Column(name="ParticipantPlan_ID")
    private int enrollmentId;

    @Column(name="PlanName")
    private String planName;

    @Column(name="PlanDescription")
    private String planDescription;

    @Column(name="ElectionAmount")
    private String electionAmount;

    @Column(name="PlanType")
    private String planType;

    @Column(name="ParticipantYTDContribution")
    private String participantYTDContribution;

    @Column(name="EmployerYTDContribution")
    private String employerYTDContribution;

    @Column(name="PlanYear")
    private String planYear;

    @Column(name="IsCarryOverEnable")
    private String isCarryOverEnable;

    @Column(name="CarryoverAmount")
    private String carryOverAmount;

    @Column(name="PendingCardTransaction")
    private String pendingCardTransaction;

    @Column(name="AccountBalance")
    private String accountBalance;

    @Column(name="DisbursableBalance")
    private String disbursableBalance;

    @Column(name="AvailableBalance")
    private String availableBalance;

    @Column(name="CoverageEndDate")
    private String coverageEndDate;

    @Column(name = "Eff_TermDate")
    private String termDate;

    @Column(name="YtdDCReturn")
    private String ytdDcReturn;

    @Column(name="YtdReturn")
    private String ytdReturn;

    @Column(name="YTDClaim")
    private String ytdClaim;

    @Column(name="YTDPayments")
    private String ytdPayments;

    @Column(name="FirstName")
    private String firstName;

    @Column(name="LastName")
    private String lastName;

    @Column(name="CustomID")
    private String customId;

    @Column(name="SSN")
    private String ssn;

    @Column(name="PlanStatus")
    private String planStatus;

    @Column(name="ActiveParticipantCount")
    private String activeParticipantCount;

    @Column(name="ParticipantCount")
    private String participantCount;

    @Column(name="IsByDivision")
    private String isByDivision;

    public sEnrollment(){}

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public sEmployee getSummitEmployee() {
        return sEmployee;
    }

    public sBenefitYear getSummitBenefitYear() {
        return sBenefitYear;
    }

    public double getElectionAmount() {
        double eAmount=0;
        try{
            eAmount = Double.parseDouble(electionAmount);
        } catch (Exception e) {
            e.printStackTrace();
            eAmount = 0;
        } finally {
            return eAmount;
        }
    }

    public LocalDate getCoverageEndDate() {
        LocalDate ced = getSummitBenefitYear().getPlanYearEnd();
        if(coverageEndDate!= null && !coverageEndDate.equals("")){
            try{
                int firstSlash = coverageEndDate.indexOf("/");
                int secondSlash = coverageEndDate.indexOf("/",firstSlash+1);
                int month = Integer.parseInt(coverageEndDate.substring(0,firstSlash));
                int day = Integer.parseInt(coverageEndDate.substring(firstSlash+1,secondSlash));
                int year = Integer.parseInt(coverageEndDate.substring(secondSlash+1));
                ced = LocalDate.of(year,month,day);
            } catch (Exception e){
                e.printStackTrace();
                ced = getSummitBenefitYear().getPlanYearEnd();
            }
        }
        return ced;
    }

    public LocalDate getTermDate() {
        LocalDate td = getCoverageEndDate();
        if(termDate!= null && !termDate.equals("")){
            try{
                int firstSlash = termDate.indexOf("/");
                int secondSlash = termDate.indexOf("/",firstSlash+1);
                int month = Integer.parseInt(termDate.substring(0,firstSlash));
                int day = Integer.parseInt(termDate.substring(firstSlash+1,secondSlash));
                int year = Integer.parseInt(termDate.substring(secondSlash+1));
                td = LocalDate.of(year,month,day);
            } catch (Exception e){
                e.printStackTrace();
                td = getSummitBenefitYear().getPlanYearEnd();
            }
        }
        return td;
    }

    public boolean isActive(){
        boolean isActive = planStatus.trim() == "Active";
        return isActive;
    }

    public boolean isBillableThisMonth(){
        boolean isBillable = false;
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        LocalDate billingDate = LocalDate.of(currentDate.getYear(),currentDate.getMonthValue(),1);
        LocalDate checkStart = getSummitBenefitYear().getPlanYearStart();
        LocalDate checkEnd = getSummitBenefitYear().getPlanYearEnd();
        if(getTermDate().compareTo(checkEnd)<0)
            checkEnd = getTermDate();
        if(getCoverageEndDate().compareTo(checkEnd)<0)
            checkEnd = getCoverageEndDate();
        if(checkStart.compareTo(billingDate)<=0 && billingDate.compareTo(checkEnd)<=0)
            isBillable = true;
        return  isBillable;
    }

    public void basicEnrollmentDetail(){
        System.out.println("Employer: " + getSummitEmployee().getSummitOrganization().getEmployerName());
        System.out.println("Participant: " + getSummitEmployee().getFullName());
        System.out.println("Benefit: " + getSummitBenefitYear().getSummitBenefit().getPlanName());
        System.out.println("Plan Year: " + getSummitBenefitYear().getPlanYearStart() + "-" + getSummitBenefitYear().getPlanYearEnd());
        System.out.println("Coverage Ends: " + getCoverageEndDate());
        System.out.println("Is Billable?: " + isBillableThisMonth());
        System.out.println("**********************************************");
    }
}

package net.superiorstate.ams.model.summit.imports.order;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Table(name="import6enrollment")
public class ImportEnrollment {
    @ManyToOne
    @JoinColumn(name="Participant_ID")
    private ImportEmployee importEmployee;

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
    private ImportBenefitYear importBenefitYear;

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

    public ImportEnrollment(){}

    public ImportEmployee getImportEmployee() {
        return importEmployee;
    }

    public void setImportEmployee(ImportEmployee importEmployee) {
        this.importEmployee = importEmployee;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getDivisionOrganizationId() {
        return divisionOrganizationId;
    }

    public void setDivisionOrganizationId(String divisionOrganizationId) {
        this.divisionOrganizationId = divisionOrganizationId;
    }

    public String getDivisionOrganizationName() {
        return divisionOrganizationName;
    }

    public void setDivisionOrganizationName(String divisionOrganizationName) {
        this.divisionOrganizationName = divisionOrganizationName;
    }

    public String getDivisionCustomId() {
        return divisionCustomId;
    }

    public void setDivisionCustomId(String divisionCustomId) {
        this.divisionCustomId = divisionCustomId;
    }

    public String getEmployerId() {
        return employerId;
    }

    public void setEmployerId(String employerId) {
        this.employerId = employerId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getEmployerPlanId() {
        return employerPlanId;
    }

    public void setEmployerPlanId(String employerPlanId) {
        this.employerPlanId = employerPlanId;
    }

    public ImportBenefitYear getImportBenefitYear() {
        return importBenefitYear;
    }

    public void setImportBenefitYear(ImportBenefitYear importBenefitYear) {
        this.importBenefitYear = importBenefitYear;
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
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

    public String getElectionAmount() {
        return electionAmount;
    }

    public void setElectionAmount(String electionAmount) {
        this.electionAmount = electionAmount;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getParticipantYTDContribution() {
        return participantYTDContribution;
    }

    public void setParticipantYTDContribution(String participantYTDContribution) {
        this.participantYTDContribution = participantYTDContribution;
    }

    public String getEmployerYTDContribution() {
        return employerYTDContribution;
    }

    public void setEmployerYTDContribution(String employerYTDContribution) {
        this.employerYTDContribution = employerYTDContribution;
    }

    public String getPlanYear() {
        return planYear;
    }

    public void setPlanYear(String planYear) {
        this.planYear = planYear;
    }

    public String getIsCarryOverEnable() {
        return isCarryOverEnable;
    }

    public void setIsCarryOverEnable(String isCarryOverEnable) {
        this.isCarryOverEnable = isCarryOverEnable;
    }

    public String getCarryOverAmount() {
        return carryOverAmount;
    }

    public void setCarryOverAmount(String carryOverAmount) {
        this.carryOverAmount = carryOverAmount;
    }

    public String getPendingCardTransaction() {
        return pendingCardTransaction;
    }

    public void setPendingCardTransaction(String pendingCardTransaction) {
        this.pendingCardTransaction = pendingCardTransaction;
    }

    public String getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(String accountBalance) {
        this.accountBalance = accountBalance;
    }

    public String getDisbursableBalance() {
        return disbursableBalance;
    }

    public void setDisbursableBalance(String disbursableBalance) {
        this.disbursableBalance = disbursableBalance;
    }

    public String getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(String availableBalance) {
        this.availableBalance = availableBalance;
    }


    public void setCoverageEndDate(String coverageEndDate) {
        this.coverageEndDate = coverageEndDate;
    }

    public void setTermDate(String termDate) {
        this.termDate = termDate;
    }

    public String getYtdDcReturn() {
        return ytdDcReturn;
    }

    public void setYtdDcReturn(String ytdDcReturn) {
        this.ytdDcReturn = ytdDcReturn;
    }

    public String getYtdReturn() {
        return ytdReturn;
    }

    public void setYtdReturn(String ytdReturn) {
        this.ytdReturn = ytdReturn;
    }

    public String getYtdClaim() {
        return ytdClaim;
    }

    public void setYtdClaim(String ytdClaim) {
        this.ytdClaim = ytdClaim;
    }

    public String getYtdPayments() {
        return ytdPayments;
    }

    public void setYtdPayments(String ytdPayments) {
        this.ytdPayments = ytdPayments;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }

    public String getActiveParticipantCount() {
        return activeParticipantCount;
    }

    public void setActiveParticipantCount(String activeParticipantCount) {
        this.activeParticipantCount = activeParticipantCount;
    }

    public String getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(String participantCount) {
        this.participantCount = participantCount;
    }

    public String getIsByDivision() {
        return isByDivision;
    }

    public void setIsByDivision(String isByDivision) {
        this.isByDivision = isByDivision;
    }

    public LocalDate getCoverageEndDate() {
        LocalDate ced = getImportBenefitYear().getPlanYearEnd();
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
                ced = getImportBenefitYear().getPlanYearEnd();
            }
        }
        return ced;
    }

    public LocalDate getTermDate() {
        if (termDate == null || termDate.trim().isEmpty()) {
            return fallbackTermDate();
        }

        List<DateTimeFormatter> knownFormats = List.of(
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy h:mm:ss a") // handles "2025 12:00:00 AM"
        );

        for (DateTimeFormatter fmt : knownFormats) {
            try {
                return LocalDate.parse(termDate.trim(), fmt);
            } catch (Exception ignored) {
            }
        }

        // If none of the formats work, fallback
        return fallbackTermDate();
    }

    private LocalDate fallbackTermDate() {
        try {
            return getImportBenefitYear().getPlanYearEnd();
        } catch (Exception e) {
            return LocalDate.of(LocalDate.now().getYear(), 12, 31);
        }
    }


    public boolean isActive(){
        boolean isActive = planStatus.trim() == "Active";
        return isActive;
    }

    public boolean isBillableThisMonth(){
        boolean isBillable = false;
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        LocalDate billingDate = LocalDate.of(currentDate.getYear(),currentDate.getMonthValue(),1);
        LocalDate checkStart = getImportBenefitYear().getPlanYearStart();
        LocalDate checkEnd = getImportBenefitYear().getPlanYearEnd();
        if(getTermDate().compareTo(checkEnd)<0)
            checkEnd = getTermDate();
        if(getCoverageEndDate().compareTo(checkEnd)<0)
            checkEnd = getCoverageEndDate();
        if(checkStart.compareTo(billingDate)<=0 && billingDate.compareTo(checkEnd)<=0)
            isBillable = true;
        return  isBillable;
    }

}

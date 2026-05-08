package net.superiorstate.ams.model.dto.inventory;

/**
 * Read-only DTO representing a single Benefit row, with resolved
 * PlanType and BillingGroup fields flattened in.
 *
 * All date fields are ISO-8601 strings (yyyy-MM-dd) or null.
 * Serialized to JSON via Gson.
 */
public class BenefitDTO {

    private int benefitId;
    private String sourceType;       // "CDH" | "COBRA"
    private String code;             // PlanType.Code
    private String planTypeName;     // PlanType.PlanTypeName
    private String planName;
    private String planDescription;
    private Integer billingGroupId;  // null when PlanType has no BillingGroup
    private String billingGroup;     // BillingGroup.description, null when absent
    private boolean active;
    private boolean hasCards;
    private String effectiveDate;
    private String terminationDate;
    private String nextRenewalDue;
    private String lastRenewed;
    private String planYearStart;
    private String planYearEnd;
    private Integer summitId;
    private Integer benIdPb;         // Benefit.pbBenId → column benid_pb

    public BenefitDTO() {}

    // ── getters ──────────────────────────────────────────────────────────────

    public int getBenefitId()         { return benefitId; }
    public String getSourceType()     { return sourceType; }
    public String getCode()           { return code; }
    public String getPlanTypeName()   { return planTypeName; }
    public String getPlanName()       { return planName; }
    public String getPlanDescription(){ return planDescription; }
    public Integer getBillingGroupId(){ return billingGroupId; }
    public String getBillingGroup()   { return billingGroup; }
    public boolean isActive()         { return active; }
    public boolean isHasCards()       { return hasCards; }
    public String getEffectiveDate()  { return effectiveDate; }
    public String getTerminationDate(){ return terminationDate; }
    public String getNextRenewalDue() { return nextRenewalDue; }
    public String getLastRenewed()    { return lastRenewed; }
    public String getPlanYearStart()  { return planYearStart; }
    public String getPlanYearEnd()    { return planYearEnd; }
    public Integer getSummitId()      { return summitId; }
    public Integer getBenIdPb()       { return benIdPb; }

    // ── setters ──────────────────────────────────────────────────────────────

    public void setBenefitId(int benefitId)               { this.benefitId = benefitId; }
    public void setSourceType(String sourceType)           { this.sourceType = sourceType; }
    public void setCode(String code)                       { this.code = code; }
    public void setPlanTypeName(String planTypeName)       { this.planTypeName = planTypeName; }
    public void setPlanName(String planName)               { this.planName = planName; }
    public void setPlanDescription(String planDescription) { this.planDescription = planDescription; }
    public void setBillingGroupId(Integer billingGroupId)  { this.billingGroupId = billingGroupId; }
    public void setBillingGroup(String billingGroup)       { this.billingGroup = billingGroup; }
    public void setActive(boolean active)                  { this.active = active; }
    public void setHasCards(boolean hasCards)              { this.hasCards = hasCards; }
    public void setEffectiveDate(String effectiveDate)     { this.effectiveDate = effectiveDate; }
    public void setTerminationDate(String terminationDate) { this.terminationDate = terminationDate; }
    public void setNextRenewalDue(String nextRenewalDue)   { this.nextRenewalDue = nextRenewalDue; }
    public void setLastRenewed(String lastRenewed)         { this.lastRenewed = lastRenewed; }
    public void setPlanYearStart(String planYearStart)     { this.planYearStart = planYearStart; }
    public void setPlanYearEnd(String planYearEnd)         { this.planYearEnd = planYearEnd; }
    public void setSummitId(Integer summitId)              { this.summitId = summitId; }
    public void setBenIdPb(Integer benIdPb)                { this.benIdPb = benIdPb; }
}

package net.superiorstate.ams.model.dto.inventory;

import java.util.List;

/**
 * Read-only DTO representing one employer's full service-scope snapshot.
 *
 * Serialized to JSON via Gson by EmployerInventoryApi.
 * No JPA annotations — this is a pure data transfer object.
 *
 * Date fields are ISO-8601 strings (yyyy-MM-dd) or null.
 */
public class EmployerInventoryDTO {

    /** Employer.id → organization_id */
    private Long organizationId;

    /** Employer.employerName → employer_name */
    private String employerName;

    /** Employer.isActive → active */
    private boolean active;

    /** Employer.isBillable → billable */
    private boolean billable;

    /**
     * Feature flags from the employer record and HsaEr presence.
     * Values: "CDH", "PB", "POP", "HSA"
     */
    private List<String> scopeFlags;

    /**
     * Distinct service lines derived from active benefits and employer flags.
     * CDH benefits → PlanType.Code (e.g. "FSA", "DCA", "LFSA", "MERP", "Med125")
     * COBRA benefits → "COBRA-" + PlanType.Code (e.g. "COBRA-Medical")
     * Employer.hasPop → "POP"
     * HsaEr with billedDirect=false → "HSA"
     * HsaEr with billedDirect=true  → "HSA-Direct"
     */
    private List<String> serviceLines;

    /** Distinct PlanType.Code values across active benefits */
    private List<String> planCodes;

    /** Distinct BillingGroup.description values across active benefits (nulls excluded) */
    private List<String> billingGroups;

    private int activeBenefitCount;

    /** Count of active benefits where sourceType = "CDH" */
    private int cdhCount;

    /** Count of active benefits where sourceType = "COBRA" */
    private int cobraCount;

    /** True if any active benefit has hasCards = true */
    private boolean hasDebitCards;

    /** Earliest effectiveDate across active benefits; null if no benefits */
    private String earliestBenefitEffective;

    /** Minimum nextRenewalDue across active benefits; null if none set */
    private String nextRenewalMin;

    /** Maximum nextRenewalDue across active benefits; null if none set */
    private String nextRenewalMax;

    /** Nested HSA account information derived from HsaEr rows */
    private HsaInfo hsa;

    /** Full detail for each active benefit */
    private List<BenefitDTO> benefits;

    public EmployerInventoryDTO() {}

    // ── nested: HsaInfo ──────────────────────────────────────────────────────

    /**
     * Summary of HsaEr rows for this employer.
     */
    public static class HsaInfo {

        /** True if any HsaEr row exists for this employer */
        private boolean active;

        /** True if any HsaEr row has billedDirect = false (invoiced through us) */
        private boolean invoiced;

        /** True if any HsaEr row has billedDirect = true (employer pays HSA vendor directly) */
        private boolean billedDirect;

        /** HsaEr.name values (string PK) for this employer */
        private List<String> hsaerNames;

        public HsaInfo() {}

        public boolean isActive()             { return active; }
        public boolean isInvoiced()           { return invoiced; }
        public boolean isBilledDirect()       { return billedDirect; }
        public List<String> getHsaerNames()   { return hsaerNames; }

        public void setActive(boolean active)               { this.active = active; }
        public void setInvoiced(boolean invoiced)           { this.invoiced = invoiced; }
        public void setBilledDirect(boolean billedDirect)   { this.billedDirect = billedDirect; }
        public void setHsaerNames(List<String> hsaerNames) { this.hsaerNames = hsaerNames; }
    }

    // ── getters ──────────────────────────────────────────────────────────────

    public Long getOrganizationId()                  { return organizationId; }
    public String getEmployerName()                  { return employerName; }
    public boolean isActive()                        { return active; }
    public boolean isBillable()                      { return billable; }
    public List<String> getScopeFlags()              { return scopeFlags; }
    public List<String> getServiceLines()            { return serviceLines; }
    public List<String> getPlanCodes()               { return planCodes; }
    public List<String> getBillingGroups()           { return billingGroups; }
    public int getActiveBenefitCount()               { return activeBenefitCount; }
    public int getCdhCount()                         { return cdhCount; }
    public int getCobraCount()                       { return cobraCount; }
    public boolean isHasDebitCards()                 { return hasDebitCards; }
    public String getEarliestBenefitEffective()      { return earliestBenefitEffective; }
    public String getNextRenewalMin()                { return nextRenewalMin; }
    public String getNextRenewalMax()                { return nextRenewalMax; }
    public HsaInfo getHsa()                          { return hsa; }
    public List<BenefitDTO> getBenefits()            { return benefits; }

    // ── setters ──────────────────────────────────────────────────────────────

    public void setOrganizationId(Long organizationId)               { this.organizationId = organizationId; }
    public void setEmployerName(String employerName)                  { this.employerName = employerName; }
    public void setActive(boolean active)                             { this.active = active; }
    public void setBillable(boolean billable)                         { this.billable = billable; }
    public void setScopeFlags(List<String> scopeFlags)               { this.scopeFlags = scopeFlags; }
    public void setServiceLines(List<String> serviceLines)           { this.serviceLines = serviceLines; }
    public void setPlanCodes(List<String> planCodes)                 { this.planCodes = planCodes; }
    public void setBillingGroups(List<String> billingGroups)         { this.billingGroups = billingGroups; }
    public void setActiveBenefitCount(int activeBenefitCount)        { this.activeBenefitCount = activeBenefitCount; }
    public void setCdhCount(int cdhCount)                            { this.cdhCount = cdhCount; }
    public void setCobraCount(int cobraCount)                        { this.cobraCount = cobraCount; }
    public void setHasDebitCards(boolean hasDebitCards)              { this.hasDebitCards = hasDebitCards; }
    public void setEarliestBenefitEffective(String d)               { this.earliestBenefitEffective = d; }
    public void setNextRenewalMin(String nextRenewalMin)             { this.nextRenewalMin = nextRenewalMin; }
    public void setNextRenewalMax(String nextRenewalMax)             { this.nextRenewalMax = nextRenewalMax; }
    public void setHsa(HsaInfo hsa)                                  { this.hsa = hsa; }
    public void setBenefits(List<BenefitDTO> benefits)               { this.benefits = benefits; }
}

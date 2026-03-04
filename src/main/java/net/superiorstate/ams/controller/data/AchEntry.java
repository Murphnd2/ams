package net.superiorstate.ams.controller.data;

import java.io.Serializable;

/**
 * DTO representing a single entry from a Wells Fargo ACH Return/NOC report.
 */
public class AchEntry implements Serializable {

    private String individualId;
    private String individualName;
    private String reasonCode;
    private String reasonDescription;
    private String correctedData;
    private String effectiveDate;
    private String amount;
    private String receivingBankRT;
    private String accountNumber;
    private String origSeq;

    // Set during person-matching step
    private Long matchedPersonId;
    private String matchedPersonName;
    private String matchType; // "EMPLOYEE", "PERSON", "NEW"

    public AchEntry() {}

    public String getIndividualId() { return individualId; }
    public void setIndividualId(String individualId) { this.individualId = individualId; }

    public String getIndividualName() { return individualName; }
    public void setIndividualName(String individualName) { this.individualName = individualName; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }

    public String getCorrectedData() { return correctedData; }
    public void setCorrectedData(String correctedData) { this.correctedData = correctedData; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getReceivingBankRT() { return receivingBankRT; }
    public void setReceivingBankRT(String receivingBankRT) { this.receivingBankRT = receivingBankRT; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getOrigSeq() { return origSeq; }
    public void setOrigSeq(String origSeq) { this.origSeq = origSeq; }

    public Long getMatchedPersonId() { return matchedPersonId; }
    public void setMatchedPersonId(Long matchedPersonId) { this.matchedPersonId = matchedPersonId; }

    public String getMatchedPersonName() { return matchedPersonName; }
    public void setMatchedPersonName(String matchedPersonName) { this.matchedPersonName = matchedPersonName; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    /**
     * Returns a human-readable version of the corrected data field.
     */
    public String getFormattedCorrectedData() {
        if (correctedData == null) return "";
        // Handle multiple corrections on one line or joined with "; "
        StringBuilder sb = new StringBuilder();
        for (String part : correctedData.split("(?=\\*)|;\\s*")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append("; ");
            if (part.startsWith("*ACCT S/B:")) sb.append("Account should be ").append(part.substring(10).trim());
            else if (part.startsWith("*\u0410\u0421\u0421\u0422 S/B:")) sb.append("Account should be ").append(part.substring(10).trim()); // Cyrillic OCR
            else if (part.startsWith("*T/R S/B:")) sb.append("Routing should be ").append(part.substring(9).trim());
            else if (part.startsWith("*TC S/B:")) sb.append("Transaction code should be ").append(part.substring(8).trim());
            else sb.append(part);
        }
        return sb.toString();
    }

    /**
     * Returns formatted effective date (MMYYYY → MM/YYYY).
     */
    public String getFormattedEffectiveDate() {
        if (effectiveDate == null || effectiveDate.length() != 6) return effectiveDate;
        return effectiveDate.substring(0, 2) + "/" + effectiveDate.substring(2);
    }
}

package net.superiorstate.ams.controller.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO containing the full parse result from a Wells Fargo ACH Return/NOC PDF report.
 */
public class AchParseResult implements Serializable {

    private String reportType;   // "NOC" or "RETURN"
    private String reportDate;   // from SYS DATE header
    private String companyName;  // e.g., "SUPERIOR STATE ADMINISTRATORS"
    private List<AchEntry> entries = new ArrayList<>();

    public AchParseResult() {}

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public List<AchEntry> getEntries() { return entries; }
    public void setEntries(List<AchEntry> entries) { this.entries = entries; }

    public void addEntry(AchEntry entry) { this.entries.add(entry); }

    public boolean hasEntries() { return !entries.isEmpty(); }
}

package net.superiorstate.ams.controller.data;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static utility class for parsing Wells Fargo ACH Return/NOC PDF reports.
 * Uses PDFBox with setSortByPosition(true) to get proper column ordering from faxed PDFs.
 */
public class AchPdfParser {

    private AchPdfParser() {} // utility class

    // Entry line 1: "23866 Melanie McDonald 022026 DDA .00"
    // or: "DavenportEnergy Davenport Energy 022526 DDA .00"
    // individualId (alphanumeric) + name + 6-digit effDate + 2-3 char type + amount
    private static final Pattern ENTRY_LINE1 = Pattern.compile(
            "^(\\S+)\\s+(.+?)\\s+(\\d{6})\\s+\\w{2,3}\\s+([\\.\\d]+)\\s*$"
    );

    // Entry line 2: "051400549 0077395 1053054793704 ON-US CR 0527411"
    // or: "051401027 0000411 0101758601 DB 1544109"
    // bankRT + seq + account + disposition/type + origSeq
    private static final Pattern ENTRY_LINE2 = Pattern.compile(
            "^(\\d{9})\\s+(\\d+)\\s+(\\S+)\\s+.+?\\s+(\\d{7})\\s*$"
    );

    // NOC reason: "NOC RETURN REASON: C01 - ACCOUNT NO" or "NOC RETURN REASON: C03 R/T & ACCT"
    private static final Pattern NOC_REASON = Pattern.compile(
            "NOC RETURN REASON:\\s+([A-Z]\\d{2})\\s+(?:-\\s+)?(.+)"
    );

    // Return reason: "RETURN REASON: R01 - INSUFFICIENT FUNDS" (without NOC prefix)
    private static final Pattern RETURN_REASON = Pattern.compile(
            "^\\s+RETURN REASON:\\s+([A-Z]\\d{2})\\s+-\\s+(.+)"
    );

    // Corrected data: "*ACCT S/B: 3968052096"
    // Also matches Cyrillic OCR artifacts: *АССТ (U+0410 U+0421 U+0421 U+0422)
    private static final Pattern CORRECTED_DATA = Pattern.compile(
            "^\\s*(\\*(?:ACCT|\u0410\u0421\u0421\u0422|T/R|TC)\\s+S/B:.+)$"
    );

    // SYS DATE in header
    private static final Pattern SYS_DATE = Pattern.compile("SYS DATE:\\s+(\\d{2}/\\d{2}/\\d{2})");

    // Company name
    private static final Pattern COMPANY_NAME = Pattern.compile("COMPANY NAME:\\s+(.+?)(?:\\s{2,}|$)");

    /**
     * Parse a Wells Fargo ACH Return/NOC PDF report.
     */
    public static AchParseResult parse(InputStream pdfStream) throws IOException {
        String fullText;
        try (PDDocument doc = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            fullText = stripper.getText(doc);
        }

        AchParseResult result = new AchParseResult();

        boolean hasNoc = fullText.contains("NOTIFICATION OF CHANGE");
        boolean hasReturns = fullText.contains("RETURNS REPORT COVER PAGE");

        if (!hasNoc && !hasReturns) {
            throw new IllegalArgumentException("Not a recognized Wells Fargo ACH Return/NOC report.");
        }

        // Extract metadata
        Matcher dateMatcher = SYS_DATE.matcher(fullText);
        if (dateMatcher.find()) {
            result.setReportDate(dateMatcher.group(1));
        }
        Matcher compMatcher = COMPANY_NAME.matcher(fullText);
        if (compMatcher.find()) {
            result.setCompanyName(compMatcher.group(1).trim());
        }

        if (hasNoc) {
            result.setReportType(hasReturns ? "NOC+RETURN" : "NOC");
        } else {
            result.setReportType("RETURN");
        }

        // Parse entries
        String[] lines = fullText.split("\\r?\\n");
        parseEntries(lines, result);

        if (!result.hasEntries()) {
            throw new IllegalArgumentException("No ACH entries found in the report.");
        }

        return result;
    }

    /**
     * Parse entry blocks from the sorted extracted text.
     * With setSortByPosition(true), each entry appears as:
     *   Line 1: ID  Name  EffDate Type Amount
     *   Line 2: BankRT Seq Account  Disposition Type OrigSeq
     *   NOC RETURN REASON: Cxx - description
     *   *corrected data
     *   ADDENDA: ...
     */
    private static void parseEntries(String[] lines, AchParseResult result) {
        // Find the data section start (after column header separator "====")
        // that follows "INDIVIDUAL ID/" header
        int dataStart = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("INDIVIDUAL ID/")) {
                // Find the separator line after this header
                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    if (lines[j].contains("====")) {
                        dataStart = j + 1;
                        break;
                    }
                }
                break;
            }
        }

        for (int i = dataStart; i < lines.length; i++) {
            String line = lines[i].trim();

            // Stop at SUBTOTAL
            if (line.startsWith("SUBTOTAL") || (line.contains("====") && i > dataStart)) break;

            // Try to match entry line 1
            Matcher m1 = ENTRY_LINE1.matcher(line);
            if (!m1.matches()) continue;

            AchEntry entry = new AchEntry();
            entry.setIndividualId(m1.group(1));
            entry.setIndividualName(m1.group(2).trim().toUpperCase());
            entry.setEffectiveDate(m1.group(3));
            entry.setAmount(m1.group(4));

            // Try to match entry line 2 (next line)
            if (i + 1 < lines.length) {
                Matcher m2 = ENTRY_LINE2.matcher(lines[i + 1].trim());
                if (m2.matches()) {
                    entry.setReceivingBankRT(m2.group(1));
                    entry.setAccountNumber(m2.group(3));
                    entry.setOrigSeq(m2.group(4));
                    i++;
                }
            }

            // Scan ahead for reason code and corrected data (within next 6 lines)
            List<String> correctedParts = new ArrayList<>();
            for (int j = i + 1; j < Math.min(i + 7, lines.length); j++) {
                String ahead = lines[j].trim();

                // Check for NOC reason
                Matcher nocM = NOC_REASON.matcher(ahead);
                if (nocM.find()) {
                    entry.setReasonCode(nocM.group(1));
                    entry.setReasonDescription(nocM.group(2).trim());
                    continue;
                }

                // Check for Return reason (without NOC prefix)
                Matcher retM = RETURN_REASON.matcher(lines[j]); // use original line for leading space check
                if (retM.find()) {
                    entry.setReasonCode(retM.group(1));
                    entry.setReasonDescription(retM.group(2).trim());
                    continue;
                }

                // Check for corrected data
                Matcher cdM = CORRECTED_DATA.matcher(ahead);
                if (cdM.matches()) {
                    correctedParts.add(cdM.group(1).trim());
                    continue;
                }

                // ADDENDA lines — skip
                if (ahead.startsWith("ADDENDA:")) continue;

                // If we hit another entry line or SUBTOTAL, stop
                if (ENTRY_LINE1.matcher(ahead).matches() || ahead.startsWith("SUBTOTAL")
                        || ahead.contains("=====")) {
                    break;
                }
            }

            if (!correctedParts.isEmpty()) {
                entry.setCorrectedData(String.join("; ", correctedParts));
            }

            // Only add entries that have a reason code
            if (entry.getReasonCode() != null) {
                result.addEntry(entry);
            }
        }
    }
}

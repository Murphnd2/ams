package net.superiorstate.ams.controller.data;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;

public class AchPdfDebug {
    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "C:/Users/kevinmurphy/Downloads/Wells Fargo ACH 12.30.25.pdf";
        File pdfFile = new File(path);

        System.out.println("=== SORTED TEXT ===");
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            String[] lines = text.split("\\r?\\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                System.out.printf("%4d | %s%n", i + 1, line);
                // Show hex for lines with corrected data to detect Cyrillic
                if (line.contains("S/B:")) {
                    System.out.print("     HEX: ");
                    for (int c = 0; c < Math.min(line.length(), 40); c++) {
                        System.out.printf("%04X ", (int) line.charAt(c));
                    }
                    System.out.println();
                }
            }
        }

        System.out.println("\n=== PARSER OUTPUT ===");
        try (FileInputStream fis = new FileInputStream(pdfFile)) {
            AchParseResult result = AchPdfParser.parse(fis);
            System.out.println("Type: " + result.getReportType() + "  Date: " + result.getReportDate());
            System.out.println("Entries: " + result.getEntries().size());
            for (int i = 0; i < result.getEntries().size(); i++) {
                AchEntry e = result.getEntries().get(i);
                System.out.printf("Entry %d: ID=%s  Name=%s  Code=%s  Data=%s  Eff=%s  Bank=%s  Acct=%s  Seq=%s%n",
                    i+1, e.getIndividualId(), e.getIndividualName(),
                    e.getReasonCode() + "-" + e.getReasonDescription(),
                    e.getFormattedCorrectedData(), e.getEffectiveDate(),
                    e.getReceivingBankRT(), e.getAccountNumber(), e.getOrigSeq());
            }
        } catch (Exception ex) {
            System.out.println("PARSE ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

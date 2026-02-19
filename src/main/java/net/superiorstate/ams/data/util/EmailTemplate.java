package net.superiorstate.ams.data.util;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

import java.time.Duration;
import java.util.List;

/**
 * Builds a branded HTML email wrapper using PSP colors from DB constants.
 * Constants used: EMAIL_COLOR_PRIMARY, EMAIL_COLOR_ACCENT
 *
 * Spam reduction measures:
 * - Table-based layout (maximum email client compatibility)
 * - role="presentation" on all layout tables
 * - No JavaScript, forms, or hidden text
 * - Clean text-to-HTML ratio
 * - Proper charset declaration
 * - Plain readable content structure
 */
public abstract class EmailTemplate {

    /**
     * Wraps the user's email body in a professional branded HTML template.
     *
     * @param body        the HTML body content (from CKEditor)
     * @param sender      the Person sending the email (for signature)
     * @param attachments list of WebLink attachments (may be null or empty)
     * @param em          EntityManager for reading constants and generating pre-signed URLs
     * @return fully wrapped HTML email string
     */
    public static String wrap(String body, Person sender, List<WebLink> attachments, EntityManager em) {

        String primary = safe(AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY"), "#2B5F8A");
        String accent  = safe(AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT"), "#7AB648");

        String pspName = "";
        String senderName = "";
        String senderEmail = "";

        if (sender != null) {
            senderName = nullSafe(sender.getFirstName()) + " " + nullSafe(sender.getLastName());
            senderEmail = nullSafe(sender.getEmail());
            if (sender.getPsp() != null) {
                pspName = nullSafe(sender.getPsp().getFullName());
            }
        }

        // Get PSP name for Wasabi storage prefix
        String pspNameForStorage = pspName.isEmpty() ? "default" : pspName;

        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>");
        sb.append("<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">");

        // Outer wrapper table
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f4;\">");
        sb.append("<tr><td align=\"center\" style=\"padding:20px 10px;\">");

        // Inner content table (max 600px)
        sb.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);\">");

        // ── Header bar ──
        sb.append("<tr>");
        sb.append("<td style=\"background-color:").append(primary).append(";padding:24px 32px;\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
        sb.append("<tr>");
        sb.append("<td width=\"4\" style=\"background-color:").append(accent).append(";\">&nbsp;</td>");
        sb.append("<td style=\"padding-left:16px;\">");
        sb.append("<span style=\"font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;\">").append(pspName).append("</span>");
        sb.append("</td>");
        sb.append("</tr>");
        sb.append("</table>");
        sb.append("</td>");
        sb.append("</tr>");

        // ── Accent divider ──
        sb.append("<tr>");
        sb.append("<td style=\"background-color:").append(accent).append(";height:4px;font-size:0;line-height:0;\">&nbsp;</td>");
        sb.append("</tr>");

        // ── Attachments section (only if attachments exist) ──
        if (attachments != null && !attachments.isEmpty()) {
            sb.append("<tr>");
            sb.append("<td style=\"padding:16px 32px;background-color:#f0f5fa;border-bottom:1px solid #e0e0e0;\">");
            sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
            sb.append("<tr>");
            sb.append("<td style=\"font-size:13px;font-weight:bold;color:").append(primary).append(";padding-bottom:8px;\">&#128206; Attachments</td>");
            sb.append("</tr>");
            sb.append("<tr><td>");

            for (WebLink w : attachments) {
                if (w.getLinkType() != null && w.getLinkType().getId() == 1) {
                    // Type 1 = uploaded file → pre-signed Wasabi URL (7 days)
                    try {
                        String url = StorageDAO.getDownloadUrl(em, pspNameForStorage, w.getLinkPath(), Duration.ofDays(7));
                        sb.append("<a href=\"").append(url).append("\" target=\"_blank\" style=\"display:inline-block;padding:6px 14px;margin:0 8px 4px 0;background-color:#ffffff;border:1px solid #d0d0d0;border-radius:4px;font-size:13px;color:")
                                .append(primary).append(";text-decoration:none;\">&#128196; ")
                                .append(nullSafe(w.getPlainText())).append("</a>");
                    } catch (Exception e) {
                        System.out.println("[EmailTemplate] Failed to generate pre-signed URL for: " + w.getLinkPath());
                        sb.append("<span style=\"font-size:13px;color:#999999;\">").append(nullSafe(w.getPlainText())).append(" (unavailable)</span> ");
                    }
                } else if (w.getLinkType() != null && w.getLinkType().getId() == 2) {
                    // Type 2 = external URL
                    sb.append("<a href=\"").append(nullSafe(w.getLinkPath())).append("\" target=\"_blank\" style=\"display:inline-block;padding:6px 14px;margin:0 8px 4px 0;background-color:#ffffff;border:1px solid #d0d0d0;border-radius:4px;font-size:13px;color:")
                            .append(primary).append(";text-decoration:none;\">&#128279; ")
                            .append(nullSafe(w.getPlainText())).append("</a>");
                }
            }

            sb.append("</td></tr>");
            sb.append("</table>");
            sb.append("</td>");
            sb.append("</tr>");
        }

        // ── Body content ──
        sb.append("<tr>");
        sb.append("<td style=\"padding:32px;color:#333333;font-size:15px;line-height:1.6;\">");
        sb.append(nullSafe(body));
        sb.append("</td>");
        sb.append("</tr>");

        // ── Signature block ──
        sb.append("<tr>");
        sb.append("<td style=\"padding:0 32px 24px 32px;\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
        sb.append("<tr><td style=\"border-top:1px solid #e0e0e0;padding-top:16px;\">");
        sb.append("<span style=\"font-size:15px;font-weight:bold;color:").append(primary).append(";\">").append(senderName.trim()).append("</span><br/>");
        if (!senderEmail.isBlank()) {
            sb.append("<span style=\"font-size:13px;color:#666666;\">").append(senderEmail).append("</span><br/>");
        }
        sb.append("<span style=\"font-size:13px;color:").append(accent).append(";font-weight:bold;\">").append(pspName).append("</span>");
        sb.append("</td></tr>");
        sb.append("</table>");
        sb.append("</td>");
        sb.append("</tr>");

        // ── Footer ──
        sb.append("<tr>");
        sb.append("<td style=\"background-color:#f9f9f9;padding:16px 32px;border-top:1px solid #e8e8e8;\">");
        sb.append("<span style=\"font-size:11px;color:#999999;\">This message was sent on behalf of ").append(pspName).append(".</span>");
        sb.append("</td>");
        sb.append("</tr>");

        // Close inner table
        sb.append("</table>");

        // Close outer table
        sb.append("</td></tr>");
        sb.append("</table>");

        sb.append("</body></html>");

        return sb.toString();
    }

    /**
     * Generates a plain-text version of the email for multipart/alternative.
     * Helps spam score by providing a text alternative to the HTML.
     */
    public static String plainText(String body, Person sender, List<WebLink> attachments) {
        StringBuilder sb = new StringBuilder();

        // Strip HTML tags for plain text version
        String textBody = (body == null) ? "" : body
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("<p[^>]*>", "\n")
                .replaceAll("</p>", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        sb.append(textBody);
        sb.append("\n\n");

        if (attachments != null && !attachments.isEmpty()) {
            sb.append("---\nAttachments:\n");
            for (WebLink w : attachments) {
                sb.append("- ").append(w.getPlainText() != null ? w.getPlainText() : w.getLinkPath()).append("\n");
            }
            sb.append("\n");
        }

        if (sender != null) {
            sb.append("---\n");
            sb.append(nullSafe(sender.getFirstName())).append(" ").append(nullSafe(sender.getLastName())).append("\n");
            if (sender.getEmail() != null && !sender.getEmail().isBlank()) {
                sb.append(sender.getEmail()).append("\n");
            }
            if (sender.getPsp() != null) {
                sb.append(nullSafe(sender.getPsp().getFullName())).append("\n");
            }
        }

        return sb.toString();
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
    public static String wrapBodyOnly(String body, String pspName, EntityManager em) {
        String primary = safe(AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY"), "#2B5F8A");
        String accent  = safe(AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT"), "#7AB648");
        if (pspName == null) pspName = "";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>");
        sb.append("<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f4;\">");
        sb.append("<tr><td align=\"center\" style=\"padding:20px 10px;\">");
        sb.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);\">");
        sb.append("<tr><td style=\"background-color:").append(primary).append(";padding:24px 32px;\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
        sb.append("<td width=\"4\" style=\"background-color:").append(accent).append(";\">&nbsp;</td>");
        sb.append("<td style=\"padding-left:16px;\"><span style=\"font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;\">").append(pspName).append("</span></td>");
        sb.append("</tr></table></td></tr>");
        sb.append("<tr><td style=\"padding:32px;color:#333333;font-size:15px;line-height:1.6;\">");
        sb.append(nullSafe(body));
        sb.append("</td></tr>");
        sb.append("<tr><td style=\"background-color:#f9f9f9;padding:16px 32px;border-top:1px solid #e8e8e8;\">");
        sb.append("<span style=\"font-size:11px;color:#999999;\">This message was sent on behalf of ").append(pspName).append(".</span>");
        sb.append("</td></tr>");
        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
    }
}
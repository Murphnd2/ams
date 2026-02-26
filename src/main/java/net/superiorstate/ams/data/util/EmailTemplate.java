package net.superiorstate.ams.data.util;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

import java.time.Duration;
import java.util.List;

/**
 * Builds a clean, professional HTML email wrapper.
 *
 * Design: minimal white card on light background, system font stack,
 * subtle border, refined signature block, attachment pills below signature.
 * No bold branded header bar — lets the content speak for itself.
 *
 * Constants used: EMAIL_COLOR_PRIMARY (attachment link color),
 *                 EMAIL_FOOTER_TEXT (customizable footer per PSP)
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

    private static final String FONT_STACK = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif";

    /**
     * Wraps the user's email body in a clean HTML template.
     *
     * @param body        the HTML body content (from CKEditor)
     * @param sender      the Person sending the email (for signature)
     * @param attachments list of WebLink attachments (may be null or empty)
     * @param em          EntityManager for reading constants and generating pre-signed URLs
     * @return fully wrapped HTML email string
     */
    public static String wrap(String body, Person sender, List<WebLink> attachments, EntityManager em) {

        String linkColor = safe(AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY"), "#1a56db");
        String footerText = safe(AppConstantDAO.getConstantValue(em, "EMAIL_FOOTER_TEXT"), "");

        String pspName = "";
        String senderName = "";
        String senderEmail = "";

        if (sender != null) {
            senderName = (capitalCase(sender.getFirstName()) + " " + capitalCase(sender.getLastName())).trim();
            senderEmail = nullSafe(sender.getEmail());
            if (sender.getPsp() != null) {
                pspName = nullSafe(sender.getPsp().getFullName());
            }
        }

        // PSP slug for Wasabi storage prefix
        String pspNameForStorage = pspName.isEmpty() ? "default" : pspName;

        StringBuilder sb = new StringBuilder();

        // ── DOCTYPE + HEAD ──
        sb.append("<!DOCTYPE html>");
        sb.append("<html><head>");
        sb.append("<meta charset=\"UTF-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">");
        sb.append("<title>Email</title>");
        sb.append("</head>");
        sb.append("<body style=\"margin:0;padding:0;background-color:#f9f9f9;font-family:").append(FONT_STACK).append(";\">");

        // ── Outer wrapper table ──
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f9f9f9;\">");
        sb.append("<tr><td align=\"center\" style=\"padding:48px 16px;\">");

        // ── Inner card table ──
        sb.append("<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;width:100%;background-color:#ffffff;border:1px solid #e5e5e5;border-radius:4px;\">");

        // ── Body content ──
        sb.append("<tr>");
        sb.append("<td style=\"padding:28px 44px 32px 44px;\">");

        // Body text — CKEditor content
        sb.append("<div style=\"font-size:15px;line-height:1.7;color:#333333;\">");
        sb.append(nullSafe(body));
        sb.append("</div>");

        // ── Signature block ──
        if (!senderName.isBlank()) {
            sb.append("<div style=\"margin-top:28px;padding-top:20px;border-top:1px solid #eeeeee;\">");
            sb.append("<p style=\"font-size:13.5px;color:#888888;margin:0 0 3px;line-height:1.5;\"><strong style=\"color:#333333;\">").append(senderName).append("</strong></p>");
            if (!senderEmail.isBlank()) {
                sb.append("<p style=\"font-size:13.5px;color:#888888;margin:0 0 3px;line-height:1.5;\">").append(senderEmail).append("</p>");
            }
            if (!pspName.isBlank()) {
                sb.append("<p style=\"font-size:13.5px;color:#888888;margin:0 0 3px;line-height:1.5;\">").append(pspName).append("</p>");
            }
            sb.append("</div>");
        }

        // ── Attachments (after signature, above footer) ──
        if (attachments != null && !attachments.isEmpty()) {
            sb.append("<div style=\"margin-top:20px;padding-top:16px;border-top:1px solid #eeeeee;\">");
            sb.append("<p style=\"font-size:12px;color:#888888;margin:0 0 8px;\">Attachments</p>");

            for (WebLink w : attachments) {
                if (w.getLinkType() != null && w.getLinkType().getId() == 1) {
                    // Type 1 = uploaded file → pre-signed Wasabi URL (7 days)
                    try {
                        String url = StorageDAO.getDownloadUrl(em, pspNameForStorage, w.getLinkPath(), Duration.ofDays(7));
                        sb.append("<a href=\"").append(url).append("\" target=\"_blank\" style=\"display:inline-block;padding:5px 12px;margin:0 6px 6px 0;background-color:#f5f5f5;border:1px solid #ddd;border-radius:3px;font-size:12px;color:")
                                .append(linkColor).append(";text-decoration:none;\">")
                                .append(nullSafe(w.getPlainText())).append("</a>");
                    } catch (Exception e) {
                        System.out.println("[EmailTemplate] Failed to generate pre-signed URL for: " + w.getLinkPath());
                        sb.append("<span style=\"font-size:12px;color:#999999;\">").append(nullSafe(w.getPlainText())).append(" (unavailable)</span> ");
                    }
                } else if (w.getLinkType() != null && w.getLinkType().getId() == 2) {
                    // Type 2 = external URL
                    sb.append("<a href=\"").append(nullSafe(w.getLinkPath())).append("\" target=\"_blank\" style=\"display:inline-block;padding:5px 12px;margin:0 6px 6px 0;background-color:#f5f5f5;border:1px solid #ddd;border-radius:3px;font-size:12px;color:")
                            .append(linkColor).append(";text-decoration:none;\">")
                            .append(nullSafe(w.getPlainText())).append("</a>");
                }
            }

            sb.append("</div>");
        }

        sb.append("</td>");
        sb.append("</tr>");

        // ── Footer ──
        sb.append("<tr>");
        sb.append("<td style=\"padding:16px 44px 20px;text-align:center;border-top:1px solid #f0f0f0;\">");
        sb.append("<p style=\"font-size:11px;color:#bbbbbb;margin:0;line-height:1.5;\">");
        sb.append(nullSafe(footerText));
        sb.append("</p>");
        sb.append("</td>");
        sb.append("</tr>");

        // ── Close inner card table ──
        sb.append("</table>");

        // ── Close outer wrapper table ──
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

        if (sender != null) {
            sb.append("--\n");
            String name = (capitalCase(sender.getFirstName()) + " " + capitalCase(sender.getLastName())).trim();
            if (!name.isBlank()) sb.append(name).append("\n");
            if (sender.getEmail() != null && !sender.getEmail().isBlank()) {
                sb.append(sender.getEmail()).append("\n");
            }
            if (sender.getPsp() != null && sender.getPsp().getFullName() != null) {
                sb.append(sender.getPsp().getFullName()).append("\n");
            }
        }

        if (attachments != null && !attachments.isEmpty()) {
            sb.append("\n---\nAttachments:\n");
            for (WebLink w : attachments) {
                sb.append("- ").append(w.getPlainText() != null ? w.getPlainText() : "File").append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Lightweight wrapper for system-generated emails (password resets, one-time logins, etc.)
     * Uses the same clean design but no signature block or attachments.
     *
     * @param body    the HTML body content
     * @param pspName the PSP name for the footer
     * @param em      EntityManager for reading constants
     * @return fully wrapped HTML email string
     */
    public static String wrapBodyOnly(String body, String pspName, EntityManager em) {
        if (pspName == null) pspName = "";
        String footerText = safe(AppConstantDAO.getConstantValue(em, "EMAIL_FOOTER_TEXT"), "");

        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>");
        sb.append("<body style=\"margin:0;padding:0;background-color:#f9f9f9;font-family:").append(FONT_STACK).append(";\">");

        // Outer wrapper
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f9f9f9;\">");
        sb.append("<tr><td align=\"center\" style=\"padding:48px 16px;\">");

        // Inner card
        sb.append("<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;width:100%;background-color:#ffffff;border:1px solid #e5e5e5;border-radius:4px;\">");

        // Body
        sb.append("<tr><td style=\"padding:28px 44px 32px 44px;\">");
        sb.append("<div style=\"font-size:15px;line-height:1.7;color:#333333;\">");
        sb.append(nullSafe(body));
        sb.append("</div>");
        sb.append("</td></tr>");

        // Footer
        sb.append("<tr><td style=\"padding:16px 44px 20px;text-align:center;border-top:1px solid #f0f0f0;\">");
        sb.append("<p style=\"font-size:11px;color:#bbbbbb;margin:0;line-height:1.5;\">");
        sb.append(nullSafe(footerText));
        sb.append("</p>");
        sb.append("</td></tr>");

        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Normalizes a name to Capital Case (e.g., "JOHN" → "John", "mcdonald" → "Mcdonald").
     * Handles multiple words separated by spaces or hyphens.
     */
    private static String capitalCase(String s) {
        if (s == null || s.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : s.trim().toCharArray()) {
            if (c == ' ' || c == '-') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}

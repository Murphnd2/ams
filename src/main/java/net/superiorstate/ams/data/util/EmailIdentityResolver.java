package net.superiorstate.ams.data.util;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves the outbound {@link EmailIdentity} for a message from a human sender (V069).
 * <p>
 * Four tiers, checked in order:
 * <ol>
 *   <li><b>Tier 0 — PSP sender:</b> the sender's email domain is in
 *       {@code VERIFIED_PSP_DOMAINS} (ssa.properties, default
 *       {@code superiorstate.net,superiorstate.biz}). From = the sender's own address
 *       with full-name display; Reply-To = the sender's own address.</li>
 *   <li><b>Tier 1 — verified agency:</b> the resolved agency has a non-blank
 *       {@code email_domain} and {@code email_verified = true}. From =
 *       {@code <sender-localpart>@<email_domain>} with full-name display; Reply-To =
 *       the sender's <b>real</b> address. If the sender's real address is malformed or
 *       the grafted address is invalid, drop to Tier 2 rather than emit a broken From.</li>
 *   <li><b>Tier 2 — unverified fallback:</b> From = {@code FALLBACK_FROM} (DB constant,
 *       default {@code notifications@superiorstate.net}) with the sender's full-name
 *       display; Reply-To = the sender's real address.</li>
 *   <li><b>System:</b> {@link #systemIdentity()} — a fixed {@code noreply@superiorstate.net},
 *       no display name, no Reply-To. Used for auth/security mail only.</li>
 * </ol>
 * Invariant: every human tier sets Reply-To to the sender's real inbox, so replies always
 * route to the person regardless of what From became.
 */
public final class EmailIdentityResolver {

    private EmailIdentityResolver() {}

    static final String DEFAULT_FALLBACK_FROM = "notifications@superiorstate.net";
    static final String SYSTEM_FROM = "noreply@superiorstate.net";
    private static final String DEFAULT_VERIFIED_PSP_DOMAINS = "superiorstate.net,superiorstate.biz";

    private static volatile Set<String> verifiedPspDomains;

    /**
     * Resolve the sender identity for a human-originated message.
     *
     * @param sender       the Person sending (real inbox = sender.getEmail())
     * @param agencyOrNull the originating agency, or null if none resolves
     * @param psp          the sender's PSP (reserved; not required by the tier logic today)
     * @param em           EntityManager for reading the FALLBACK_FROM constant
     * @return the resolved {@link EmailIdentity}; never null
     */
    public static EmailIdentity resolve(Person sender, Agency agencyOrNull, PSP psp, EntityManager em) {
        String senderEmail = (sender != null && sender.getEmail() != null) ? sender.getEmail().trim() : "";
        String displayName = fullName(sender);

        // Tier 0 — PSP sender (own domain is a verified PSP domain).
        if (EmailDAO.isValidEmail(senderEmail) && isPspDomain(domainOf(senderEmail))) {
            return new EmailIdentity(senderEmail, displayName, senderEmail, null);
        }

        // Tier 1 — verified agency: graft the sender's localpart onto the agency domain.
        if (agencyOrNull != null
                && agencyOrNull.getEmailDomain() != null
                && !agencyOrNull.getEmailDomain().isBlank()
                && agencyOrNull.isEmailVerified()
                && EmailDAO.isValidEmail(senderEmail)) {
            String localPart = localPartOf(senderEmail);
            String domain = agencyOrNull.getEmailDomain().trim().toLowerCase(Locale.ROOT);
            if (!localPart.isEmpty()) {
                String grafted = localPart + "@" + domain;
                if (EmailDAO.isValidEmail(grafted)) {
                    // Reply-To = sender's REAL address, so replies reach the person.
                    return new EmailIdentity(grafted, displayName, senderEmail, null);
                }
            }
            // malformed localpart / invalid grafted address -> fall through to Tier 2
        }

        // Tier 2 — unverified fallback. Reply-To = sender's real address when valid.
        String replyTo = EmailDAO.isValidEmail(senderEmail) ? senderEmail : "";
        return new EmailIdentity(fallbackFrom(em), displayName, replyTo, null);
    }

    /** Fixed identity for system/security mail (new-user, password/help): no white-label, no Reply-To. */
    public static EmailIdentity systemIdentity() {
        return new EmailIdentity(SYSTEM_FROM, "", "", null);
    }

    /* ----------------------------- helpers ----------------------------- */

    private static String fallbackFrom(EntityManager em) {
        try {
            String v = AppConstantDAO.getConstantValue(em, "FALLBACK_FROM");
            if (v != null && EmailDAO.isValidEmail(v.trim())) return v.trim();
        } catch (Exception ignore) {}
        return DEFAULT_FALLBACK_FROM;
    }

    private static boolean isPspDomain(String domain) {
        if (domain == null || domain.isEmpty()) return false;
        return verifiedPspDomains().contains(domain);
    }

    private static Set<String> verifiedPspDomains() {
        Set<String> cached = verifiedPspDomains;
        if (cached == null) {
            Set<String> built = new HashSet<>();
            String raw = AppConfig.get("VERIFIED_PSP_DOMAINS", DEFAULT_VERIFIED_PSP_DOMAINS);
            if (raw != null) {
                for (String part : raw.split(",")) {
                    String d = part.strip().toLowerCase(Locale.ROOT);
                    if (!d.isEmpty()) built.add(d);
                }
            }
            cached = built;
            verifiedPspDomains = built;
        }
        return cached;
    }

    /** Lowercased domain part of an email, or "" if none. */
    static String domainOf(String email) {
        if (email == null) return "";
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return "";
        return email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
    }

    /** Local part of an email (before the last '@'), or "" if none. */
    static String localPartOf(String email) {
        if (email == null) return "";
        int at = email.lastIndexOf('@');
        if (at <= 0) return "";
        return email.substring(0, at).trim();
    }

    /** Capital-cased "First Last" display name for the sender, or "" if unavailable. */
    private static String fullName(Person p) {
        if (p == null) return "";
        String name = (capitalCase(p.getFirstName()) + " " + capitalCase(p.getLastName())).trim();
        return name;
    }

    private static String capitalCase(String s) {
        if (s == null || s.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        boolean capNext = true;
        for (char c : s.trim().toCharArray()) {
            if (c == ' ' || c == '-') {
                result.append(c);
                capNext = true;
            } else if (capNext) {
                result.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}

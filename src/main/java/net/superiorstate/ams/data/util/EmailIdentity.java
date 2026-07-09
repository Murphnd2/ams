package net.superiorstate.ams.data.util;

/**
 * Immutable resolved sender identity for a single outbound message (V069).
 * <p>
 * Produced by {@link EmailIdentityResolver} and consumed by the identity-aware
 * {@code EmailDAO.sendEmail(...)} overload. Separates the four deliverability-relevant
 * decisions so the send layer never has to re-derive them:
 * <ul>
 *   <li>{@link #fromAddress()} — the {@code From:} header address (must be a domain the
 *       relay is authorized to send for: a PSP domain, a verified agency domain, or the
 *       fallback address).</li>
 *   <li>{@link #displayName()} — the friendly {@code From:} display name (the human
 *       sender's full name), or blank for none.</li>
 *   <li>{@link #replyTo()} — where replies route; for every human tier this is the
 *       sender's <b>real</b> inbox (the safety invariant), or blank for system mail.</li>
 *   <li>{@link #envelopeFrom()} — optional SMTP envelope sender / return-path. Null means
 *       "let the relay's own VERP own the return-path" (the v1 default for all tiers);
 *       a non-null value would force {@code mail.smtp.from}.</li>
 * </ul>
 */
public final class EmailIdentity {

    private final String fromAddress;
    private final String displayName;
    private final String replyTo;
    private final String envelopeFrom;

    public EmailIdentity(String fromAddress, String displayName, String replyTo, String envelopeFrom) {
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.displayName = displayName == null ? "" : displayName.trim();
        this.replyTo = replyTo == null ? "" : replyTo.trim();
        this.envelopeFrom = (envelopeFrom == null || envelopeFrom.isBlank()) ? null : envelopeFrom.trim();
    }

    /** The {@code From:} header address. */
    public String fromAddress() { return fromAddress; }

    /** The friendly {@code From:} display name, or "" for none. */
    public String displayName() { return displayName; }

    /** Reply-To address (sender's real inbox for human tiers), or "" for none. */
    public String replyTo() { return replyTo; }

    /** Optional envelope sender / return-path; null = let the relay's VERP own it. */
    public String envelopeFrom() { return envelopeFrom; }

    public boolean hasDisplayName() { return !displayName.isEmpty(); }
    public boolean hasReplyTo() { return !replyTo.isEmpty(); }
    public boolean hasEnvelopeFrom() { return envelopeFrom != null; }

    @Override
    public String toString() {
        // Never includes secrets; safe for debug logging.
        return "EmailIdentity{from=" + fromAddress
                + ", display='" + displayName + "'"
                + ", replyTo=" + (replyTo.isEmpty() ? "(none)" : replyTo)
                + ", envelopeFrom=" + (envelopeFrom == null ? "(relay VERP)" : envelopeFrom)
                + "}";
    }
}

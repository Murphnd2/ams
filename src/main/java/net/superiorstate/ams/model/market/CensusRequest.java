package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * V100 — one census-request link per send, keyed to a proposal (T231 build 1, D45 decision c).
 * A PSP admin sends the employer's contact a link carrying {@link #token}; the employer uploads
 * through it (unauthenticated, {@code /census-drop/{token}}) any number of times until the link
 * closes. State is {@code OPEN}, {@code LOADED} (build 2's review page loaded a submission into the
 * roster) or {@code REVOKED} (PSP admin revoked it, or it expired and a new one was issued).
 * <p>
 * {@link #expiresAt} is 30 days from the last send ({@code CensusIntakeService.EXPIRY_DAYS});
 * re-sending an open, unexpired request renews the expiry and keeps the same token. A request
 * is active only while {@code OPEN} and unexpired — {@code CensusIntakeService.resolveActive} is the
 * only reader the public page uses, and it never distinguishes unknown from expired or closed.
 * <p>
 * Same scalar conventions as {@link SummitSetupStep}: {@link #proposalId} is a plain {@code BIGINT}
 * with no FK to {@code proposal}; {@link #requestedBy}/{@link #closedBy} are the acting user's
 * {@code assignee.id}, nullable when the session could not name anyone.
 */
@Entity
@Table(name = "census_request")
public class CensusRequest {

    public static final String STATE_OPEN = "OPEN";
    public static final String STATE_LOADED = "LOADED";
    public static final String STATE_REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    /** UUID string, 36 chars. Unique. Appears only in the link and the token lookup. */
    @Column(name = "token", nullable = false, length = 36)
    private String token;

    /** OPEN | LOADED | REVOKED. */
    @Column(name = "state", nullable = false, length = 16)
    private String state;

    @Column(name = "sent_to", length = 320)
    private String sentTo;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProposalId() { return proposalId; }
    public void setProposalId(Long proposalId) { this.proposalId = proposalId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getSentTo() { return sentTo; }
    public void setSentTo(String sentTo) { this.sentTo = sentTo; }

    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public Long getClosedBy() { return closedBy; }
    public void setClosedBy(Long closedBy) { this.closedBy = closedBy; }

    /** True when {@code OPEN} and {@link #expiresAt} is in the future. */
    public boolean isActive() {
        return STATE_OPEN.equals(state) && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }

    /** True when {@code OPEN} but past {@link #expiresAt}. */
    public boolean isExpired() {
        return STATE_OPEN.equals(state) && expiresAt != null && !expiresAt.isAfter(LocalDateTime.now());
    }
}

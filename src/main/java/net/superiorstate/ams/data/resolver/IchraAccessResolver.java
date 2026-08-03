package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * Single source of truth for "is the ICHRA capability available in this context?" —
 * one question, one boolean answer. Backs both the {@code IllustrationServlet} guard
 * and the {@code IchraHome} hub guard, and the top-level ICHRA nav entry, so all three
 * agree by construction.
 * <p>
 * Resolution order: a session flagged PSP admin is always available; otherwise the
 * caller's primary agency (via {@link AgencyScopeResolver}) is checked first for
 * {@code agency.ichra_enabled} — the common-case fast path, one {@code find}; if that
 * agency is not entitled (or there is no primary agency), every agency in the caller's
 * full membership set ({@code AgencyScope.detailAgencyIds()}) is checked instead, since
 * {@code primaryAgencyId} is an arbitrary tie-break among a person's agencies and not
 * itself an authorization boundary. Anything else — no session, no agency anywhere in
 * scope with the flag set, or any exception encountered while resolving — is not
 * available. This method fails closed in every case; it never throws.
 * <p>
 * Callers receive only the boolean, never a reason. No {@code constant} row, LOS,
 * {@code ServiceItem}, {@code PlanType}, {@code ServiceModule}, or {@code RateTable}
 * reference may ever appear in this class — this resolver answers entitlement, not
 * catalog state, and a literal reference-row ID breaks on the second installation.
 */
public final class IchraAccessResolver {

    private static final Logger log = LogManager.getLogger(IchraAccessResolver.class);

    private static final String NAV_VISIBLE_ATTR = "ichraNavVisible";

    private IchraAccessResolver() {}

    public static boolean isAvailable(EntityManager em, HttpServletRequest request) {
        try {
            if (request == null) {
                return false;
            }
            HttpSession session = request.getSession(false);
            if (session == null) {
                return false;
            }
            if (Boolean.TRUE.equals(session.getAttribute("isPspAdmin"))) {
                return true;
            }
            if (em == null) {
                return false;
            }

            AgencyScope scope = AgencyScopeResolver.resolve(em, request);
            Long agencyId = scope.primaryAgencyId();
            Set<Long> membership = scope.detailAgencyIds();

            if (agencyId != null) {
                Agency agency = em.find(Agency.class, agencyId);
                if (agency != null && agency.isIchraEnabled()) {
                    log.info("[ICHRA] Access resolved: primaryAgencyId={}, membership={}, matched={}, available=true",
                            agencyId, membership, agencyId);
                    return true;
                }
            }

            // Fast path missed (no primary agency, or the primary tie-break isn't the
            // entitled one) — fall back to the caller's full membership set. Single query
            // over the whole set rather than N em.find calls; an empty set short-circuits
            // before the query so an empty "IN ()" is never issued.
            if (membership.isEmpty()) {
                log.info("[ICHRA] Access resolved: primaryAgencyId={}, membership={}, matched=none, available=false",
                        agencyId == null ? "none" : agencyId, membership);
                return false;
            }

            List<Long> matches = em.createQuery(
                            "SELECT a.id FROM Agency a WHERE a.id IN :ids AND a.ichraEnabled = true", Long.class)
                    .setParameter("ids", membership)
                    .setMaxResults(1)
                    .getResultList();

            boolean available = !matches.isEmpty();
            log.info("[ICHRA] Access resolved: primaryAgencyId={}, membership={}, matched={}, available={}",
                    agencyId == null ? "none" : agencyId, membership, available ? matches.get(0) : "none", available);
            return available;
        } catch (Exception e) {
            log.debug("[ICHRA] Access resolution failed; defaulting to not available", e);
            return false;
        }
    }

    /**
     * Is ICHRA content permitted on <em>this proposal</em>? The session-free counterpart to
     * {@link #isAvailable(EntityManager, HttpServletRequest)}, for the public, unauthenticated
     * proposal view (<code>/proposal/*</code>), which has no {@code HttpSession} to resolve against —
     * {@code ViewProposal} is exempted from {@code LoginFilter} and reads no session anywhere in its
     * render path. Entitlement is therefore resolved from the {@code Proposal} instance alone.
     * <p>
     * <b>This is a compliance control, not a display preference.</b> Under <b>LA-17</b> the entitled
     * agency behind a proposal is the machine-checkable proxy for "a licensed agent composed and sent
     * this document" — the fact LA-17's whole assumption rests on. Over-reporting entitlement here does
     * not merely show a section to the wrong audience; it renders market data into an employer-facing
     * document in a case LA-17 does not cover, which is a state producer-licensing question rather
     * than a UI bug. Review changes to this method accordingly. See <b>T116</b>.
     * <p>
     * <b>There is deliberately no {@code isPspAdmin} bypass, and none may be added.</b>
     * {@link #isAvailable(EntityManager, HttpServletRequest)} short-circuits on the session's PSP-admin
     * attribute, which is correct on an authenticated agent surface and <b>wrong here</b>: a PSP admin
     * who happens to be logged in and opens a public proposal link must not thereby cause
     * employer-facing market data to render in a document sent to a prospect. The audience of this page
     * is the employer, never the viewer's own session. This method reads no session state of any kind.
     * <p>
     * <b>Multi-membership rule — strictly the originating agency.</b> Entitlement follows
     * {@link OriginatingAgencyResolver#resolve(Proposal)}: the same resolver, and therefore necessarily
     * the same agency, that {@code ViewProposal} already uses to pick the {@code TITLE}/{@code CLOSING}
     * agency override and the {@code AGENCY_NAME} merge token. <b>The gate and the branding cannot
     * disagree</b> — whichever agency's name is on the document is the agency whose flag governs it.
     * The rejected alternative was "any entitled agency in the originating agent's membership," which
     * would let a document branded agency Y render ICHRA content because unrelated agency X is
     * entitled — precisely the case LA-17's constraints do not cover.
     * <p>
     * ⚠️ This does <b>not</b> fix {@code OriginatingAgencyResolver.agencyOf}'s unordered
     * {@code list.get(0)} pick over a {@code @ManyToMany}. It makes that nondeterminism <i>harmless for
     * gating</i>, because brand and gate now go wrong together or not at all. The underlying branding
     * nondeterminism is a separate pre-existing defect and is tracked separately — do not "fix" it here.
     * <p>
     * Fails closed on every path and never throws: a null argument, an unresolvable agency, or any
     * exception encountered while resolving yields {@code false}. Consistent with the class-level
     * prohibition, this method references no {@code LOS}, {@code ServiceItem}, {@code PlanType},
     * {@code ServiceModule} or {@code RateTable} — it answers entitlement, never catalog state.
     *
     * @param em       an open {@code EntityManager}; must still be open, since resolution lazily walks
     *                 the originating agent's agency membership
     * @param proposal the proposal being rendered
     * @return {@code true} only when the proposal's originating agency exists and carries
     *         {@code agency.ichra_enabled}
     */
    public static boolean isAvailableForProposal(EntityManager em, Proposal proposal) {
        try {
            if (em == null || proposal == null) {
                return false;
            }

            Agency agency = OriginatingAgencyResolver.resolve(proposal);
            if (agency == null) {
                log.info("[ICHRA] Proposal access resolved: proposalId={}, agency=none, available=false",
                        proposal.getId());
                return false;
            }

            boolean available = agency.isIchraEnabled();
            log.info("[ICHRA] Proposal access resolved: proposalId={}, agencyId={}, available={}",
                    proposal.getId(), agency.getId(), available);
            return available;
        } catch (Exception e) {
            log.debug("[ICHRA] Proposal access resolution failed; defaulting to not available", e);
            return false;
        }
    }

    /**
     * Cached navigation-visibility hint for the ICHRA nav entry — a per-session cache
     * of {@link #isAvailable(EntityManager, HttpServletRequest)}, computed at most once
     * per session. ICHRA entitlement changes only when {@code agency.ichra_enabled} is
     * deliberately flipped, not within a session, so caching it here turns a database
     * round trip on nearly every page render into a session-attribute read on all but
     * the first.
     * <p>
     * <b>This is not an authorization check.</b> It exists only to decide whether to
     * draw a link. Callers that enforce actual access — {@code IllustrationServlet},
     * {@code IchraHome} — MUST continue to call
     * {@link #isAvailable(EntityManager, HttpServletRequest)} directly, live, per
     * request; they must never call this method for that purpose. A stale cached
     * {@code true} draws a link that the live check then denies (fail-safe); a stale
     * cached {@code false} only hides a link that an entitled user could still reach
     * directly by URL.
     * <p>
     * Fails closed and never throws: a null session, or any failure acquiring
     * persistence or resolving the answer, caches and returns {@code false} so a
     * failure does not retry on every page.
     */
    public static boolean isAvailableForNav(HttpServletRequest request) {
        try {
            if (request == null) {
                return false;
            }
            HttpSession session = request.getSession(false);
            if (session == null) {
                return false;
            }

            Object cached = session.getAttribute(NAV_VISIBLE_ATTR);
            if (cached instanceof Boolean) {
                return (Boolean) cached;
            }

            boolean available;
            try {
                EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
                EntityManager em = emf.createEntityManager();
                try {
                    available = isAvailable(em, request);
                } finally {
                    if (em.isOpen()) em.close();
                }
            } catch (Exception e) {
                log.debug("[ICHRA] Nav visibility resolution failed; caching not-available", e);
                available = false;
            }

            session.setAttribute(NAV_VISIBLE_ATTR, available);
            return available;
        } catch (Exception e) {
            log.debug("[ICHRA] Nav visibility resolution failed", e);
            return false;
        }
    }
}

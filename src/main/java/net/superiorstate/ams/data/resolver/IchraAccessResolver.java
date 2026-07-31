package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.model.sales.agency.Agency;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Single source of truth for "is the ICHRA capability available in this context?" —
 * one question, one boolean answer. Backs both the {@code IllustrationServlet} guard
 * and the {@code IchraHome} hub guard, and the top-level ICHRA nav entry, so all three
 * agree by construction.
 * <p>
 * Resolution order: a session flagged PSP admin is always available; otherwise the
 * caller's primary agency (via {@link AgencyScopeResolver}) must have
 * {@code agency.ichra_enabled} set; anything else — no session, no agency, no flag, or
 * any exception encountered while resolving — is not available. This method fails
 * closed in every case; it never throws.
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
            if (agencyId == null) {
                return false;
            }

            Agency agency = em.find(Agency.class, agencyId);
            return agency != null && agency.isIchraEnabled();
        } catch (Exception e) {
            log.debug("[ICHRA] Access resolution failed; defaulting to not available", e);
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

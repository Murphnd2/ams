package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.model.market.RatingAreaRateCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * S21-L — single source of truth for "which {@code RatingAreaRateCache.sourceEnv} is
 * authoritative for this installation." Backs every read-time provenance check that used
 * to hardcode {@link RatingAreaRateCache#SOURCE_ENV_PRODUCTION} directly:
 * {@code RateCacheDAO.check}, {@code ViewProposal.putIchraMarketTokens}, and
 * {@code ProposalBuilder}'s snapshot-attach checks. Callers ask this one question and
 * never learn how it is answered — no caller reads the backing constant directly.
 * <p>
 * <b>This changes only which env is treated as authoritative on read.</b> It does not
 * change what env is stamped on a cache row when rate data is fetched
 * ({@code RateCacheWarmService}), and it does not change what env is recorded on a
 * {@code ProposalIchraSnapshot} at build time — both remain honest records of what
 * actually happened, independent of which env this resolver currently favors.
 * <p>
 * <b>Kevin's decision, recorded in {@code docs/analysis/legal_assumptions.md} LA-18.</b>
 * This installation treats staging rate data as fully authoritative — with no
 * distinguishing marker anywhere a customer or entitled user can see — until production
 * HealthSherpa access is live. The control is entitlement ({@code IchraAccessResolver} /
 * {@code agency.ichra_enabled}), not a data-provenance check: nobody outside PSP admin is
 * granted ICHRA access while this reads {@code STAGING}. Today it reads {@code STAGING};
 * flipping to {@code PRODUCTION} once real data exists is a one-row change, never a code
 * change, and staging remains meaningful afterward for dev/test installations.
 * <p>
 * <b>Never cached across requests.</b> Backed by {@link AppConstantDAO#getConstantValue},
 * itself an uncached, per-call query — flipping the constant takes effect on the very next
 * call, no application restart required.
 * <p>
 * <b>Fails closed to {@code STAGING}, not {@code PRODUCTION}, on a missing or unreadable
 * constant — deliberately the less obviously "safe"-sounding direction.</b> Defaulting to
 * {@code PRODUCTION} would make every currently-visible ICHRA market token across the whole
 * installation silently go blank the instant the constant broke — a large, disruptive
 * regression from today's working state, and a failure mode nobody would immediately trace
 * back to a missing constant row. Defaulting to {@code STAGING} instead just continues
 * today's already-accepted state through the error: it exposes nothing beyond what
 * {@code IchraAccessResolver} already permits, because entitlement — not this resolver — is
 * the actual control on who sees ICHRA content at all. The failure itself is never silent —
 * see the {@code log.error} calls below — it just does not compound a broken constant with
 * a broken render for a caller who was already entitled and already seeing staging data
 * treated as authoritative.
 */
public final class RateSourceEnvResolver {

    private static final Logger log = LogManager.getLogger(RateSourceEnvResolver.class);

    /** The backing {@code constant} row name, seeded by {@code DatabaseInitializer}. */
    public static final String CONSTANT_NAME = "ICHRA_RATE_SOURCE_ENV";

    private RateSourceEnvResolver() {}

    /**
     * The {@code RatingAreaRateCache.sourceEnv} value currently treated as authoritative.
     * Always one of {@link RatingAreaRateCache#SOURCE_ENV_STAGING} or
     * {@link RatingAreaRateCache#SOURCE_ENV_PRODUCTION} — never null, never any other
     * string, so every caller can compare it directly with {@code .equals()} exactly as it
     * already compared against the old hardcoded constant.
     */
    public static String authoritativeSourceEnv(EntityManager em) {
        try {
            String value = AppConstantDAO.getConstantValue(em, CONSTANT_NAME);
            if (RatingAreaRateCache.SOURCE_ENV_STAGING.equals(value)
                    || RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(value)) {
                return value;
            }
            log.error("[RATE-SOURCE-ENV] Constant '{}' missing or unrecognized (read: '{}') -- " +
                            "defaulting to STAGING, today's intended value. This does not, by " +
                            "itself, expose ICHRA data to anyone not already entitled.",
                    CONSTANT_NAME, value);
            return RatingAreaRateCache.SOURCE_ENV_STAGING;
        } catch (Exception e) {
            log.error("[RATE-SOURCE-ENV] Failed to resolve constant '{}' -- defaulting to " +
                    "STAGING, today's intended value.", CONSTANT_NAME, e);
            return RatingAreaRateCache.SOURCE_ENV_STAGING;
        }
    }
}

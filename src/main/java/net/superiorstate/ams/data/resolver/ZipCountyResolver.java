package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.CountyReferenceDAO;
import net.superiorstate.ams.data.dao.ZipCountyDAO;
import net.superiorstate.ams.model.market.CountyReference;
import net.superiorstate.ams.model.market.ZipCounty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Answers exactly one question: <b>given a ZIP, which counties?</b>
 * <p>
 * It does not read the rate cache, does not warm anything (that is T76), does not
 * know what a rating area is, and — most importantly — <b>does not choose.</b> A ZIP
 * that touches three counties returns three counties; picking one is the caller's
 * job, and per {@code ichra_flow_and_handoffs.md} §5 the caller must ask the agent
 * rather than decide. In Texas 686 of 1,992 ZCTAs (34%) cross a county line, so the
 * multi-county answer is the common case, not an edge case, and any caller that
 * silently takes the first element is wrong roughly one time in three.
 * <p>
 * <b>Why silently choosing is the failure that matters.</b> A wrong county resolves
 * cleanly, returns that county's cached rates, and puts a wrong premium in front of
 * a client. Nothing downstream detects it — there is no second source to disagree
 * with. A ZIP that fails to resolve, by contrast, is visible immediately and costs
 * nothing but a click.
 * <p>
 * Three outcomes, all first-class and none of them exceptional:
 * <ul>
 *     <li><b>One county</b> — {@link Resolution#isUnique()}; the caller may proceed.</li>
 *     <li><b>Several</b> — {@link Resolution#isAmbiguous()}; the caller must ask.</li>
 *     <li><b>None</b> — {@link Resolution#isEmpty()}. <b>Not an error.</b> The
 *     crosswalk is ZCTA-derived (V085) and ZCTAs omit PO-box-only and
 *     single-building ZIPs, so a perfectly valid USPS ZIP can land here. It is also
 *     what every out-of-state ZIP returns while the crosswalk is Texas-only. The
 *     caller says "we don't have that ZIP, choose a county" — never "invalid ZIP".</li>
 * </ul>
 * Fails closed and never throws: any exception resolving is logged and returned as
 * the empty resolution, which degrades to the county dropdown rather than to a
 * stack trace on an agent's screen.
 */
public final class ZipCountyResolver {

    private static final Logger log = LogManager.getLogger(ZipCountyResolver.class);

    /** USPS ZIPs are five digits. Anything else is not a ZIP and is not looked up. */
    private static final int ZIP_LENGTH = 5;

    private ZipCountyResolver() {}

    /**
     * @param zip a five-digit ZIP as typed by an agent; leading and trailing space is
     *            tolerated, a ZIP+4 suffix is not (callers should send the five-digit
     *            part). Null, blank, wrong length or non-numeric all resolve empty
     *            rather than throwing — a typo is not an exceptional condition.
     * @return the resolution; never null.
     */
    public static Resolution resolve(EntityManager em, String zip) {
        try {
            if (em == null) {
                return Resolution.empty(zip);
            }
            String normalized = normalize(zip);
            if (normalized == null) {
                return Resolution.empty(zip);
            }

            List<ZipCounty> matches = ZipCountyDAO.findByZip(em, normalized);
            if (matches.isEmpty()) {
                log.debug("[ZIP] {} -> no crosswalk row", normalized);
                return Resolution.empty(normalized);
            }

            // Hydrate the county names so a caller can show what it resolved to.
            // A crosswalk row whose county is absent from county_reference is
            // dropped rather than shown as a bare FIPS code: county_reference is
            // Texas-only today (V076), so an unnamed county is one this
            // installation cannot price or label, and offering it as a choice
            // would be offering a dead end.
            List<Candidate> candidates = new ArrayList<>();
            for (ZipCounty match : matches) {
                CountyReference county = CountyReferenceDAO.findByFips(em, match.getCountyFips());
                if (county != null) {
                    candidates.add(new Candidate(county));
                }
            }

            if (candidates.isEmpty()) {
                log.debug("[ZIP] {} -> {} crosswalk row(s), none in county_reference", normalized, matches.size());
                return Resolution.empty(normalized);
            }

            log.debug("[ZIP] {} -> {} county candidate(s)", normalized, candidates.size());
            return new Resolution(normalized, candidates);
        } catch (Exception e) {
            log.debug("[ZIP] Resolution failed; returning empty", e);
            return Resolution.empty(zip);
        }
    }

    /** @return the trimmed five-digit ZIP, or null if it is not one. */
    private static String normalize(String zip) {
        if (zip == null) {
            return null;
        }
        String trimmed = zip.trim();
        if (trimmed.length() != ZIP_LENGTH) {
            return null;
        }
        for (int i = 0; i < ZIP_LENGTH; i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return null;
            }
        }
        return trimmed;
    }

    /**
     * One county a ZIP may resolve to. Carries the {@link CountyReference} rather
     * than a bare FIPS code so a caller can label the choice without a second
     * lookup. The land-area ratio is deliberately <b>not</b> exposed — it ordered
     * the list and has no business being shown or reasoned about further.
     */
    public static final class Candidate {
        private final CountyReference county;

        Candidate(CountyReference county) {
            this.county = county;
        }

        public CountyReference getCounty() { return county; }
        public String getCountyFips() { return county.getCountyFips(); }
        public String getCountyName() { return county.getCountyName(); }
        public String getState() { return county.getState(); }
    }

    /** The answer. Immutable; {@code candidates} is never null and never modifiable. */
    public static final class Resolution {
        private final String zip;
        private final List<Candidate> candidates;

        Resolution(String zip, List<Candidate> candidates) {
            this.zip = zip;
            this.candidates = Collections.unmodifiableList(candidates);
        }

        static Resolution empty(String zip) {
            return new Resolution(zip, new ArrayList<>());
        }

        /** The normalized ZIP, or whatever was passed in when it could not be normalized. */
        public String getZip() { return zip; }

        public List<Candidate> getCandidates() { return candidates; }

        /** No crosswalk coverage for this ZIP. <b>A legitimate answer, not an error.</b> */
        public boolean isEmpty() { return candidates.isEmpty(); }

        /** Exactly one county — the caller may proceed without asking. */
        public boolean isUnique() { return candidates.size() == 1; }

        /** More than one county — <b>the caller must ask the agent.</b> */
        public boolean isAmbiguous() { return candidates.size() > 1; }

        /**
         * @return the single county when {@link #isUnique()}, otherwise null.
         * <b>Deliberately null on an ambiguous resolution</b> rather than returning
         * the first candidate: a caller that wants "the" county for a ZIP that has
         * three must be forced to confront that, not handed a plausible guess.
         */
        public Candidate getUnique() {
            return isUnique() ? candidates.get(0) : null;
        }
    }
}

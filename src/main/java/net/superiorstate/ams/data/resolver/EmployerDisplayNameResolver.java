package net.superiorstate.ams.data.resolver;

import net.superiorstate.ams.model.sales.agency.Prospect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * T239 -- D47(a) / N1' (Kevin, s48b3): the employer display name prefers the application's
 * {@code company_legal_name} answer; a blank answer falls back to {@code Prospect.name} with a
 * WARN, never a refusal. Extracted from {@code SummitExportServlet.writeEmployerDemographic},
 * where the rule originated for file 1 (Employer Demographic); this is now the single place the
 * rule lives, shared by file 1 and the Setup activity's stored {@code full_name}
 * ({@code ReviewApplication}).
 * <p>
 * ⚠️ <b>Option A on drift (Kevin, T239).</b> A caller that stores this value (the Setup activity)
 * stores it once, at creation. A {@code company_legal_name} edited after that has no effect on the
 * stored name -- there is no refresh path and no backfill. A caller that resolves this value fresh
 * on every read (file 1) always sees the current answer.
 */
public final class EmployerDisplayNameResolver {

    private static final Logger log = LogManager.getLogger(EmployerDisplayNameResolver.class);

    /** The application field key this rule reads. Mirrors {@code SummitExportServlet}'s own
     *  {@code FIELD_COMPANY_LEGAL_NAME} constant; both files carry the literal (accepted, T239). */
    public static final String FIELD_COMPANY_LEGAL_NAME = "company_legal_name";

    private EmployerDisplayNameResolver() {}

    /**
     * @return the trimmed {@code company_legal_name} answer when present and non-blank; otherwise
     * {@code prospect.getName()} (unchanged, not trimmed, matching the rule's original shape) with
     * a WARN logged; otherwise {@code null}. Never throws on a null {@code answers} or a null
     * {@code prospect}.
     */
    public static String resolve(Map<String, String> answers, Prospect prospect) {
        String legalNameAnswer = answers == null ? null : answers.get(FIELD_COMPANY_LEGAL_NAME);
        if (legalNameAnswer != null && !legalNameAnswer.trim().isEmpty()) {
            return legalNameAnswer.trim();
        }

        String prospectName = prospect == null ? null : prospect.getName();
        if (prospectName != null && !prospectName.trim().isEmpty()) {
            log.warn("[SUMMIT-EXPORT] Employer Demographic for prospect {}: Employer Name fell back"
                            + " to the prospect name because 'company_legal_name' is blank",
                    prospect.getId());
            return prospectName;
        }

        return null;
    }
}

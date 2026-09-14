package net.superiorstate.ams.data.resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * S60-P2 -- in-code catalog of Summit pages reachable from {@code /SummitLink}. Page paths are a
 * property of the Summit product itself, identical on every installation; the only
 * installation-specific values ({@code SUMMIT_PATH}, {@code SUMMIT_TPA_GUID}) are already DB
 * constants read elsewhere. A page key that needs adding is a code change here, not a data entry.
 */
public final class SummitPageCatalog {

    /**
     * {@code DIRECT} -- the page is a tab on {@code EditEmployer.aspx}; one hop, one redirect,
     * built by {@code SummitEmployerLinkResolver.buildEditEmployerUrl}.
     * {@code CONTEXT_THEN_PATH} -- the page lives elsewhere in Summit and takes no employer
     * identifier of its own. Hop 1 sets Summit's server-side employer context via {@code
     * EditEmployer.aspx}; hop 2 is a literal path+query that relies on that context already being
     * set, rendered as a two-step interstitial rather than a single redirect.
     * {@code DIRECT_PATH} (S60-P5) -- the page lives elsewhere in Summit but takes the employer id
     * on its own query string, so it needs no context hop: one literal path+query, one redirect,
     * built by substituting the resolved {@code altId} into a {@code {employerId}} placeholder.
     */
    public enum Mode { DIRECT, CONTEXT_THEN_PATH, DIRECT_PATH }

    /**
     * @param tab          {@code DIRECT} only -- optional. {@code null} means no {@code tab}
     *                     parameter at all (e.g. {@code employer}, S60-P3); when present it must
     *                     satisfy {@code ^[A-Za-z]+$}, matching
     *                     {@code SummitEmployerLinkResolver.buildEditEmployerUrl}'s own validator.
     *                     Must be {@code null} for every other mode.
     * @param pathAndQuery {@code CONTEXT_THEN_PATH} and {@code DIRECT_PATH} -- a literal beginning
     *                     with {@code /}, used verbatim (not re-encoded) by the hop-2 / one-hop URL
     *                     builder. For {@code DIRECT_PATH} it must additionally contain the literal
     *                     placeholder token {@code {employerId}} exactly once, substituted with the
     *                     resolved {@code altId} at request time.
     */
    public record SummitPage(String key, String label, Mode mode, String tab, String pathAndQuery) {
        public SummitPage {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("SummitPage key is required");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("SummitPage \"" + key + "\": label is required");
            }
            if (mode == Mode.DIRECT) {
                if (tab != null && !tab.matches("^[A-Za-z]+$")) {
                    throw new IllegalArgumentException(
                            "SummitPage \"" + key + "\": DIRECT mode's tab, when present, must be alphabetic");
                }
            } else if (mode == Mode.DIRECT_PATH) {
                if (tab != null) {
                    throw new IllegalArgumentException(
                            "SummitPage \"" + key + "\": DIRECT_PATH mode does not take a tab");
                }
                if (pathAndQuery == null || pathAndQuery.isBlank() || !pathAndQuery.startsWith("/")) {
                    throw new IllegalArgumentException(
                            "SummitPage \"" + key + "\": DIRECT_PATH mode requires a pathAndQuery starting with '/'");
                }
                int firstToken = pathAndQuery.indexOf("{employerId}");
                int lastToken = pathAndQuery.lastIndexOf("{employerId}");
                if (firstToken < 0 || firstToken != lastToken) {
                    throw new IllegalArgumentException(
                            "SummitPage \"" + key + "\": DIRECT_PATH mode requires pathAndQuery to contain "
                                    + "\"{employerId}\" exactly once");
                }
            } else {
                if (pathAndQuery == null || !pathAndQuery.startsWith("/")) {
                    throw new IllegalArgumentException(
                            "SummitPage \"" + key + "\": CONTEXT_THEN_PATH mode requires a pathAndQuery starting with '/'");
                }
            }
        }
    }

    private static final Map<String, SummitPage> PAGES = buildCatalog();

    private static Map<String, SummitPage> buildCatalog() {
        Map<String, SummitPage> pages = new LinkedHashMap<>();

        // DIRECT -- one hop, a real 302 to a tab on EditEmployer.aspx.
        // S60-P3 -- "employer" carries no tab parameter at all: null, not "", so no ^[A-Za-z]+$
        // validation runs and buildEditEmployerUrl appends no bare "&tab=".
        register(pages, new SummitPage("employer", "Employer", Mode.DIRECT, null, null));
        register(pages, new SummitPage("benefit-plans", "Benefit Plans", Mode.DIRECT, "BenefitPlans", null));
        register(pages, new SummitPage("cards", "Cards", Mode.DIRECT, "Cards", null));
        register(pages, new SummitPage("pay-schedules", "Pay Schedules", Mode.DIRECT, "Schedules", null));

        // CONTEXT_THEN_PATH -- hop 1 EditEmployer.aspx, hop 2 a literal path+query, no identifier.
        // ⚠️ bank-accounts (S60-P3): the bare path with no query, per Kevin's URL. Whether this
        // path honours Summit's session employer context with no parameter at all is NOT yet
        // runtime-verified -- the one entry here resting on inference rather than a confirmed hop.
        register(pages, new SummitPage("bank-accounts", "Bank Accounts", Mode.CONTEXT_THEN_PATH,
                null, "/Area/Employer/BankAccounts"));
        // S60-P3 -- renamed from "banking-checking" (S60-P2); same pathAndQuery, Kevin's naming.
        // The old key is dropped, not aliased -- nothing persisted referenced it yet.
        register(pages, new SummitPage("reimbursement-accounts", "Reimbursement Accounts", Mode.CONTEXT_THEN_PATH,
                null, "/Reimbursement/ReimbursementDefaults.aspx?isEmpTab=true&tab=Banking/Checking"));
        // S60-P5 -- Summit's participant list takes the employer on its own query string, so it
        // needs no context hop: DIRECT_PATH, not CONTEXT_THEN_PATH. ⚠️ Unverified assumption: that
        // this employerId is Summit's EmployerID (Employer.altId), the same value EditEmployer.
        // aspx?employerId= takes -- not Employer.id/organization_id. Preserved here, not resolved.
        register(pages, new SummitPage("participants", "Participants", Mode.DIRECT_PATH,
                null, "/Area/Participant/ParticipantList?employerId={employerId}"));

        // S61-P1 -- CONTEXT_THEN_PATH, no employer identifier of their own.
        register(pages, new SummitPage("on-demand-processing", "On-Demand Processing", Mode.CONTEXT_THEN_PATH,
                null, "/Area/OnDemandProcessing?processType=Events"));
        // Note: singular "ReceiptManagementSetting.aspx" -- correct as given by Kevin.
        register(pages, new SummitPage("receipt-management-settings", "Receipt Management Settings", Mode.CONTEXT_THEN_PATH,
                null, "/ReceiptManagementModule/ReceiptManagementSetting.aspx?isEmpTab=true"));
        register(pages, new SummitPage("receipt-management-request-settings", "Receipt Management Request Settings", Mode.CONTEXT_THEN_PATH,
                null, "/ReceiptManagementModule/ReceiptManagementRequest.aspx?isEmpTab=true"));
        register(pages, new SummitPage("receipt-management-processing", "Receipt Management Processing", Mode.CONTEXT_THEN_PATH,
                null, "/ReceiptManagementModule/ReceiptManagementProcessing.aspx?isEmpTab=true"));

        return Map.copyOf(pages);
    }

    private static void register(Map<String, SummitPage> pages, SummitPage page) {
        pages.put(page.key().toLowerCase(), page);
    }

    private SummitPageCatalog() {}

    /** Case-insensitive, trims. Empty/blank/unknown input yields an empty Optional. */
    public static Optional<SummitPage> find(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(PAGES.get(key.trim().toLowerCase()));
    }

    public static List<SummitPage> all() {
        return List.copyOf(PAGES.values());
    }
}

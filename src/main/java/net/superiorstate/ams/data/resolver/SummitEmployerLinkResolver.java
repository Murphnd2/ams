package net.superiorstate.ams.data.resolver;

import jakarta.servlet.ServletContext;
import net.superiorstate.ams.data.AmsDataGlobal;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * T241 -- builds the Summit "Edit Employer" URL for the setup panel's employer link, the same
 * shape {@code SummitEditEmployer.buildSummitUrl} already builds for the renewal/ticket redirect:
 * {@code {SUMMIT_PATH}/EmployerModule/EditEmployer.aspx?tpaGuid={SUMMIT_TPA_GUID}&employerId={altId}}.
 * <p>
 * {@code SUMMIT_PATH} and {@code SUMMIT_TPA_GUID} are DB constants read through
 * {@code AmsDataGlobal}, obtained from the {@code ServletContext} attribute {@code "global"} --
 * the same route {@code AbstractSummitEmployerRedirect.doGet} uses. No tenant host and no GUID
 * literal appear here.
 */
public final class SummitEmployerLinkResolver {

    private SummitEmployerLinkResolver() {}

    /** @return the Edit Employer URL, or null if the id is non-positive or either config value is unset. */
    public static String buildEditEmployerUrl(ServletContext context, int employerAltId) {
        return buildEditEmployerUrl(context, employerAltId, null);
    }

    /**
     * T241b -- overload appending {@code &tab=} for a Summit product tab, e.g. {@code
     * BenefitPlans} (Kevin, 2026-09-11; only tab name confirmed). A null/blank {@code tab} behaves
     * exactly like the two-argument form. {@code tab} is validated against {@code ^[A-Za-z]+$} --
     * Summit's own tab names are bare words -- and any other value refuses (returns null) rather
     * than emit an unvalidated query parameter.
     *
     * @return the Edit Employer URL (optionally with {@code &tab=}), or null if the id is
     * non-positive, either config value is unset, or {@code tab} is non-blank and not alphabetic.
     */
    public static String buildEditEmployerUrl(ServletContext context, int employerAltId, String tab) {
        if (employerAltId <= 0) return null;

        AmsDataGlobal global = (AmsDataGlobal) context.getAttribute("global");
        if (global == null) return null;

        String summitPath = global.getSummitPath();
        String tpaGuid = global.getSummitTpaGuid();
        if (summitPath == null || summitPath.isBlank() || tpaGuid == null || tpaGuid.isBlank()) {
            return null;
        }

        String tabParam = "";
        if (tab != null && !tab.isBlank()) {
            if (!tab.matches("^[A-Za-z]+$")) return null;
            tabParam = "&tab=" + URLEncoder.encode(tab, StandardCharsets.UTF_8);
        }

        String path = summitPath.endsWith("/")
                ? summitPath.substring(0, summitPath.length() - 1)
                : summitPath;
        String encodedGuid = URLEncoder.encode(tpaGuid, StandardCharsets.UTF_8);

        return path + "/EmployerModule/EditEmployer.aspx?tpaGuid=" + encodedGuid
                + "&employerId=" + employerAltId + tabParam;
    }
}

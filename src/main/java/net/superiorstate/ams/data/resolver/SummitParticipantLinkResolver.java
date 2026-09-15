package net.superiorstate.ams.data.resolver;

import jakarta.servlet.ServletContext;
import net.superiorstate.ams.data.AmsDataGlobal;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds the Summit "Edit Participant" URL, the same pattern {@link SummitEmployerLinkResolver}
 * uses for {@code EditEmployer.aspx}: {@code {SUMMIT_PATH}/ParticipantModule/EditParticipant.aspx
 * ?isGlobalSearch=true&tpaGuid={SUMMIT_TPA_GUID}&participantId={participantSystemId}}.
 * <p>
 * {@code SUMMIT_PATH} and {@code SUMMIT_TPA_GUID} are DB constants read through
 * {@code AmsDataGlobal}, obtained from the {@code ServletContext} attribute {@code "global"} —
 * the same route {@link SummitEmployerLinkResolver} uses. No tenant host and no GUID literal
 * appear here.
 * <p>
 * {@code participantSystemId} is a Summit-native id already present on the export row this is
 * built from (e.g. the Transaction export's {@code Participant System ID}) — unlike
 * {@code employerAltId}, there is no AMS-side lookup to resolve it first.
 */
public final class SummitParticipantLinkResolver {

    private SummitParticipantLinkResolver() {}

    /**
     * @return the Edit Participant URL, or {@code null} if {@code participantSystemId} is blank
     * or either config value is unset.
     */
    public static String buildEditParticipantUrl(ServletContext context, String participantSystemId) {
        if (participantSystemId == null || participantSystemId.isBlank()) return null;

        AmsDataGlobal global = (AmsDataGlobal) context.getAttribute("global");
        if (global == null) return null;

        String summitPath = global.getSummitPath();
        String tpaGuid = global.getSummitTpaGuid();
        if (summitPath == null || summitPath.isBlank() || tpaGuid == null || tpaGuid.isBlank()) {
            return null;
        }

        String path = summitPath.endsWith("/")
                ? summitPath.substring(0, summitPath.length() - 1)
                : summitPath;
        String encodedGuid = URLEncoder.encode(tpaGuid, StandardCharsets.UTF_8);

        return path + "/ParticipantModule/EditParticipant.aspx?isGlobalSearch=true&tpaGuid=" + encodedGuid
                + "&participantId=" + participantSystemId.trim();
    }
}

package net.superiorstate.ams.data.util;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.Activity;

/**
 * Multi-tab defense for flows that read AmsDataLocal.currentActivity.
 *
 * AmsDataLocal.currentActivity is a single session slot shared across all
 * open tabs. Opening a cross-link (ViewById?id=...) in a new tab silently
 * rebinds that slot, so a later POST from an older tab can land on the
 * wrong activity.
 *
 * When a request carries an explicit expectedActivityId, this guard
 * re-anchors currentActivity to the expected target before the calling
 * servlet reads any session state. No-op if the parameter is absent
 * (legacy callers are unaffected).
 */
public final class ActivitySessionGuard {

    private ActivitySessionGuard() {}

    public static void reanchorIfMismatch(HttpServletRequest request, EntityManager em) {
        String raw = request.getParameter("expectedActivityId");
        if (raw == null || raw.isBlank()) return;

        long expectedId;
        try {
            expectedId = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return;
        }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || local.getCurrentActivity() == null) return;

        Activity cur = local.getCurrentActivity().getActivity();
        Long curId = (cur != null) ? cur.getId() : null;
        if (curId != null && curId == expectedId) return;

        try {
            local.getCurrentActivity().intializeActivity(em, expectedId);
            request.getSession().setAttribute("local", local);
            System.out.println("[ActivitySessionGuard] Re-anchored currentActivity from "
                    + curId + " to " + expectedId);
        } catch (Exception e) {
            System.out.println("[ActivitySessionGuard] Re-anchor failed for id="
                    + expectedId + ": " + e.getMessage());
        }
    }
}

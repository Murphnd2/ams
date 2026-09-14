package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.dao.SummitEmployerLookupDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.SummitEmployerLinkResolver;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.summit.archive.Employer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * T241 -- an include-only fragment for the Summit setup panel ({@code detailSummitSetup25.jsp}),
 * dispatched via {@code jsp:include} on the Employer step. Reads only -- it composes the setup's
 * expected Employer TPA Custom ID and looks up {@code Employer} rows by {@code customId}
 * (V102), then renders one muted line: a working "Open in Summit" link when exactly one Summit
 * employer id matches, or a status line explaining why there is none. Writes nothing to any table.
 * <p>
 * ⚠️ <b>Fails silent, always,</b> matching {@code SummitSetupStatusServlet}: not PSP admin, not
 * ICHRA-available, no proposal/prospect, or any exception all write nothing rather than an error
 * page, because this fragment is embedded inside the activity detail page and must never be the
 * reason that page breaks. Exceptions are logged at WARN with no response content in the message.
 */
@WebServlet(name = "SummitEmployerLinkServlet", value = "/SummitEmployerLink")
public class SummitEmployerLinkServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitEmployerLinkServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Matches SummitSetupStatusServlet:S45c -- jsp:include preserves the including request's
        // method, and the Setup panel is reached via GoActivityDetail25's doPost as well as its
        // doGet. Without this override, HttpServlet's default doPost returns 405 here, and the
        // container silently discards that inside an include -- the line renders as nothing, with
        // no log line.
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
            if (!isPspAdmin) return;

            String proposalIdParam = request.getParameter("proposalId");
            if (proposalIdParam == null || proposalIdParam.isBlank()) return;

            long proposalId;
            try {
                proposalId = Long.parseLong(proposalIdParam.trim());
            } catch (NumberFormatException e) {
                return;
            }

            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                if (!IchraAccessResolver.isAvailable(em, request)) return;

                Proposal proposal = em.find(Proposal.class, proposalId);
                Prospect prospect = proposal != null ? proposal.getProspect() : null;
                if (prospect == null) return;

                String key = SummitExportServlet.resolveEmployerTpaCustomId(prospect);
                String tab = request.getParameter("tab");
                String label = request.getParameter("label");
                String mode = request.getParameter("mode");

                String html = renderLine(key, em, tab, label, mode);
                if (html == null) return;

                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(html);
            } finally {
                if (em.isOpen()) em.close();
            }
        } catch (Exception e) {
            log.warn("[SUMMIT-EMPLOYER-LINK] fragment failed for proposalId={}: {}",
                    request.getParameter("proposalId"), e.getMessage());
        }
    }

    /**
     * T241b -- {@code tab} selects between two shapes. Absent/blank (step 1's call): every one of
     * the six render states from T241, unchanged byte-for-byte from before this build. Present
     * (step 2's call, e.g. {@code BenefitPlans}): only the success state renders, as a bare anchor
     * with no wrapping status line and no "· employer N" suffix -- step 1 already reports match
     * status, so step 2 repeating it would be noise. Every other state renders null (nothing).
     * <p>
     * S59-P2 -- {@code mode} selects an alternate output shape from the same resolution, evaluated
     * exactly once regardless of mode. {@code mode} absent/anything other than the two values below
     * is byte-for-byte the pre-S59-P2 behaviour just described.
     * <ul>
     *   <li>{@code mode=url} -- the resolved Summit URL alone, HTML-attribute-escaped the same way
     *   the success anchor below already escapes it, no wrapper, no label. Null (nothing written)
     *   on every failure path, including the ones that already return null in tabMode.</li>
     *   <li>{@code mode=status} -- identical to absent, except the non-tab success case emits the
     *   identity text with the anchor removed (same wrapper, same font size); tabMode emits nothing,
     *   matching tabMode's existing null-on-non-primary-purpose shape.</li>
     * </ul>
     */
    private String renderLine(String key, EntityManager em, String tab, String label, String mode) {
        boolean tabMode = tab != null && !tab.isBlank();
        boolean urlMode = "url".equals(mode);
        boolean statusMode = "status".equals(mode);
        String linkLabel = (label != null && !label.isBlank()) ? label : "Open in Summit ↗";

        if (key == null) {
            if (urlMode) return null;
            return tabMode ? null : line("Summit link unavailable — SUMMIT_TPA_ID_PREFIX not configured.");
        }

        List<Employer> matches = SummitEmployerLookupDAO.findByCustomId(em, key);
        if (matches.isEmpty()) {
            if (urlMode) return null;
            return tabMode ? null : line("Not yet in Summit employer data (" + escape(key) + ").");
        }

        Set<Integer> distinctAltIds = new LinkedHashSet<>();
        for (Employer employer : matches) {
            if (employer.getAltId() > 0) distinctAltIds.add(employer.getAltId());
        }

        if (distinctAltIds.isEmpty()) {
            if (urlMode) return null;
            return tabMode ? null : line("Found " + escape(key) + " but no Summit employer id.");
        }

        if (distinctAltIds.size() > 1) {
            if (urlMode) return null;
            if (tabMode) return null;
            StringBuilder ids = new StringBuilder();
            for (Integer id : distinctAltIds) {
                if (ids.length() > 0) ids.append(", ");
                ids.append(id);
            }
            return line("Ambiguous — Summit employer ids " + escape(ids.toString())
                    + " share " + escape(key) + "; no link.");
        }

        int altId = distinctAltIds.iterator().next();
        String url = SummitEmployerLinkResolver.buildEditEmployerUrl(getServletContext(), altId, tab);
        if (url == null) {
            if (urlMode) return null;
            return tabMode ? null : line("Summit employer " + altId + " — link not configured (SUMMIT_PATH / SUMMIT_TPA_GUID).");
        }

        if (urlMode) return escape(url);

        if (statusMode) {
            return tabMode ? null : line("employer " + altId);
        }

        String anchor = "<a href=\"" + escape(url) + "\" target=\"_blank\" rel=\"noopener\">" + escape(linkLabel) + "</a>";
        return tabMode ? anchor : line(anchor + " · employer " + altId);
    }

    private static String line(String content) {
        return "<div style=\"font-size: 0.72rem;\" class=\"text-muted\">" + content + "</div>";
    }

    /** Minimal HTML-attribute/text escaping for the handful of trusted-shape values this fragment renders. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

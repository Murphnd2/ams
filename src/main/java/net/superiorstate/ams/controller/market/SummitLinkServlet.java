package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.SummitEmployerLinkResolver;
import net.superiorstate.ams.data.resolver.SummitPageCatalog;
import net.superiorstate.ams.data.resolver.SummitPageCatalog.SummitPage;
import net.superiorstate.ams.data.dao.SummitEmployerLookupDAO;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.summit.archive.Employer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * S60-P2 -- turns a generic task GoTo URL ({@code /SummitLink?page=cards}) into a redirect (or, for
 * a page that lives outside {@code EditEmployer.aspx}, a small interstitial) to an employer-specific
 * Summit page, resolving the employer at click time from the setup open in session. No employer id,
 * no proposal id, and no organization id ever appear in the URL a task stores -- everything is
 * re-derived here the same way {@code SummitEmployerLinkServlet} already does for the setup panel.
 * <p>
 * Unlike {@code SummitEmployerLinkServlet} (an include-only fragment that fails silent), this is a
 * navigation target opened in its own tab: every refusal renders a visible page explaining what went
 * wrong, never a blank response and never a stack trace.
 */
@WebServlet(name = "SummitLinkServlet", value = "/SummitLink")
public class SummitLinkServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitLinkServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession();

            boolean isPspAdmin = Boolean.TRUE.equals(session.getAttribute("isPspAdmin"));
            if (!isPspAdmin) {
                renderFailure(response, "Access restricted", "This page is available to PSP admins only.");
                return;
            }

            String pageKeyParam = request.getParameter("page");
            SummitPage page = SummitPageCatalog.find(pageKeyParam).orElse(null);
            if (page == null) {
                renderUnknownPage(response, pageKeyParam);
                return;
            }

            AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
            Activity activity = (local != null && local.getCurrentActivity() != null)
                    ? local.getCurrentActivity().getActivity() : null;
            if (activity == null) {
                renderFailure(response, "No setup open",
                        "There is no setup open in this session. Open the setup you want to link to, "
                                + "then click this link again from its Summit setup panel.");
                return;
            }

            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                if (!IchraAccessResolver.isAvailable(em, request)) {
                    renderFailure(response, "Access restricted",
                            "ICHRA access is not available for your PSP.");
                    return;
                }

                Prospect prospect = resolveProspect(activity);
                String key = prospect != null ? SummitExportServlet.resolveEmployerTpaCustomId(prospect) : null;
                if (key == null) {
                    renderFailure(response, "Summit link unavailable",
                            "Summit link unavailable — SUMMIT_TPA_ID_PREFIX not configured.");
                    return;
                }

                List<Employer> matches = SummitEmployerLookupDAO.findByCustomId(em, key);
                if (matches.isEmpty()) {
                    renderFailure(response, "Summit link unavailable",
                            "Not yet in Summit employer data (" + escape(key) + ").");
                    return;
                }

                Set<Integer> distinctAltIds = new LinkedHashSet<>();
                for (Employer employer : matches) {
                    if (employer.getAltId() > 0) distinctAltIds.add(employer.getAltId());
                }

                if (distinctAltIds.isEmpty()) {
                    renderFailure(response, "Summit link unavailable",
                            "Found " + escape(key) + " but no Summit employer id.");
                    return;
                }

                if (distinctAltIds.size() > 1) {
                    StringBuilder ids = new StringBuilder();
                    for (Integer id : distinctAltIds) {
                        if (ids.length() > 0) ids.append(", ");
                        ids.append(id);
                    }
                    renderFailure(response, "Summit link unavailable",
                            "Ambiguous — Summit employer ids " + escape(ids.toString())
                                    + " share " + escape(key) + "; no link.");
                    return;
                }

                int altId = distinctAltIds.iterator().next();

                if (page.mode() == SummitPageCatalog.Mode.DIRECT) {
                    String url = SummitEmployerLinkResolver.buildEditEmployerUrl(getServletContext(), altId, page.tab());
                    if (url == null) {
                        renderFailure(response, "Summit link unavailable",
                                "Summit employer " + altId + " — link not configured (SUMMIT_PATH / SUMMIT_TPA_GUID).");
                        return;
                    }
                    response.sendRedirect(url);
                    return;
                }

                if (page.mode() == SummitPageCatalog.Mode.DIRECT_PATH) {
                    // S60-P5 -- takes the employer on its own query string; no context hop needed.
                    String pathAndQuery = page.pathAndQuery().replace("{employerId}", String.valueOf(altId));
                    String url = SummitEmployerLinkResolver.buildContextPageUrl(getServletContext(), pathAndQuery);
                    if (url == null) {
                        renderFailure(response, "Summit link unavailable",
                                "Summit employer " + altId + " — link not configured (SUMMIT_PATH / SUMMIT_TPA_GUID).");
                        return;
                    }
                    response.sendRedirect(url);
                    return;
                }

                // CONTEXT_THEN_PATH -- two hops, never a single 302 (hop 2 alone carries no identifier).
                String hop1 = SummitEmployerLinkResolver.buildEditEmployerUrl(getServletContext(), altId);
                String hop2 = SummitEmployerLinkResolver.buildContextPageUrl(getServletContext(), page.pathAndQuery());
                if (hop1 == null || hop2 == null) {
                    renderFailure(response, "Summit link unavailable",
                            "Summit employer " + altId + " — link not configured (SUMMIT_PATH / SUMMIT_TPA_GUID).");
                    return;
                }
                renderInterstitial(response, page, altId, hop1, hop2);
            } finally {
                if (em.isOpen()) em.close();
            }
        } catch (Exception e) {
            log.warn("[SUMMIT-LINK] failed for page={}: {}", request.getParameter("page"), e.getMessage());
            renderFailure(response, "Summit link unavailable",
                    "Something went wrong building this link. Please try again from the setup's Summit setup panel.");
        }
    }

    /** Setup → Application → Proposal → Prospect, the same traversal detailSummitSetup25.jsp uses. */
    private static Prospect resolveProspect(Activity activity) {
        if (!(activity instanceof Setup setup)) return null;
        Application application = setup.getApplication();
        if (application == null) return null;
        Proposal proposal = application.getProposal();
        if (proposal == null) return null;
        return proposal.getProspect();
    }

    // ═══════════════════════════════════════════════════════════════
    //  RENDERING
    // ═══════════════════════════════════════════════════════════════

    private static final String PAGE_CSS =
            "body{font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;" +
            "max-width:640px;margin:3rem auto;padding:0 1.5rem;color:#212529}" +
            "h1{font-size:1.15rem;margin-bottom:0.75rem}" +
            "p{line-height:1.5}" +
            ".steps{padding-left:1.25rem}" +
            ".steps li{margin-bottom:0.5rem}" +
            ".steps a{word-break:break-all}" +
            ".notice{background:#fff3cd;border:1px solid #ffe69c;border-radius:0.4rem;" +
            "padding:0.75rem 1rem;font-weight:600;margin-bottom:1rem}" +
            ".step-disabled{color:#adb5bd;cursor:not-allowed;pointer-events:none}" +
            ".step-hint{color:#dc3545;font-size:0.85rem;margin-left:0.35rem}";

    private static void renderPage(HttpServletResponse response, String title, String bodyHtml) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
                "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + escape(title)
                        + "</title><style>" + PAGE_CSS + "</style></head><body>"
                        + bodyHtml + "</body></html>");
    }

    private static void renderFailure(HttpServletResponse response, String title, String message) throws IOException {
        String body = "<h1>" + escape(title) + "</h1><p>" + escape(message) + "</p>";
        renderPage(response, title, body);
    }

    private static void renderUnknownPage(HttpServletResponse response, String pageKeyParam) throws IOException {
        StringBuilder list = new StringBuilder("<ul class=\"steps\">");
        for (SummitPage p : SummitPageCatalog.all()) {
            list.append("<li><code>").append(escape(p.key())).append("</code> — ")
                    .append(escape(p.label())).append("</li>");
        }
        list.append("</ul>");

        String heading = (pageKeyParam == null || pageKeyParam.isBlank())
                ? "No Summit page specified"
                : "Unknown Summit page \"" + escape(pageKeyParam) + "\"";

        String body = "<h1>" + heading + "</h1>"
                + "<p>Valid <code>page</code> keys:</p>" + list;
        renderPage(response, "Unknown Summit page", body);
    }

    /**
     * CONTEXT_THEN_PATH success. There is no way to observe cross-origin load completion from the
     * opener, so no script can safely know hop 1 has finished before firing hop 2 -- an earlier
     * one-click design tried a fixed delay and it landed on the wrong employer's page (Summit's
     * employer context is server-side and shared across tabs, so hop 2 arriving early still
     * renders a correct-looking page, just for the wrong employer). Removed rather than tuned: a
     * longer delay only lowers the frequency of that silent misfire, it doesn't remove it.
     * <p>
     * The ordering is instead an explicit user action the UI enforces: step 2 is a live, working
     * link in the HTML (so with JS disabled it still works, alongside the warning) that an inline
     * script disables on load and step 1's click handler re-enables. No script here ever
     * navigates anywhere -- only step 1's own default anchor-click behavior does, and only step
     * 2's href attribute is toggled.
     */
    private static void renderInterstitial(HttpServletResponse response, SummitPage page, int altId,
                                            String hop1Url, String hop2Url) throws IOException {
        String hop1Escaped = escape(hop1Url);
        String hop2Escaped = escape(hop2Url);
        String labelEscaped = escape(page.label());

        String body =
                "<h1>Open " + labelEscaped + " for Summit employer " + altId + "</h1>"
                + "<p class=\"notice\">This Summit page carries no employer identifier of its own — "
                + "the employer is selected in Summit by step 1, and step 2 opens whichever employer "
                + "Summit last selected. Step 1 must finish loading in Summit before you click step 2.</p>"
                + "<ol class=\"steps\">"
                + "<li><a id=\"summitLinkStep1\" href=\"" + hop1Escaped
                + "\" target=\"summitEmployerContext\" rel=\"noopener\">Select employer " + altId
                + " in Summit</a></li>"
                + "<li><a id=\"summitLinkStep2\" href=\"" + hop2Escaped
                + "\" target=\"summitEmployerContext\" rel=\"noopener\">Open " + labelEscaped + "</a>"
                + "<span id=\"summitLinkStep2Hint\" class=\"step-hint\" style=\"display:none\">"
                + " — finish step 1 first</span></li>"
                + "</ol>"
                + "<script>"
                + "(function(){"
                + "var step1=document.getElementById('summitLinkStep1');"
                + "var step2=document.getElementById('summitLinkStep2');"
                + "var hint=document.getElementById('summitLinkStep2Hint');"
                + "if(!step1||!step2)return;"
                + "var step2Href=step2.getAttribute('href');"
                + "step2.removeAttribute('href');"
                + "step2.setAttribute('aria-disabled','true');"
                + "step2.classList.add('step-disabled');"
                + "if(hint)hint.style.display='inline';"
                + "step1.addEventListener('click',function(){"
                + "step2.setAttribute('href',step2Href);"
                + "step2.removeAttribute('aria-disabled');"
                + "step2.classList.remove('step-disabled');"
                + "if(hint)hint.style.display='none';"
                + "});"
                + "})();"
                + "</script>";

        renderPage(response, "Open " + page.label(), body);
    }

    /** Minimal HTML escaping for the handful of trusted-shape values this servlet renders. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

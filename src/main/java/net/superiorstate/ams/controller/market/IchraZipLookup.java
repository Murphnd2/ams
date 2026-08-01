package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.ZipCountyResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * JSON ZIP → county lookup for the illustration page's ZIP field (T74 / R2).
 * <p>
 * <b>Why this exists.</b> Before it, a ZIP resolved only on Enter — and Enter submits
 * the form, so an agent who typed a ZIP with the headcount still empty was answered
 * with <i>"Enter a valid number of eligible employees"</i>. Resolving a ZIP had become
 * entangled with computing an illustration. This separates them: the field resolves on
 * blur, with no submit, no validation and no page reload.
 * <p>
 * <b>It resolves and nothing else.</b> No rates, no cache read, no warming (that is
 * T76), no computation of any kind. One question in, a county list out.
 * <p>
 * <b>Nothing is persisted.</b> No ZIP reaches {@code illustration_log} or any other
 * row — the illustration's own logging is untouched and happens only when an
 * illustration is actually run.
 * <p>
 * <b>Gated identically to every other ICHRA surface</b> — a live, per-request
 * {@link IchraAccessResolver#isAvailable} call, never the session-cached nav hint. A
 * caller without ICHRA entitlement gets the same empty answer as a caller with an
 * unknown ZIP, because distinguishing them would itself disclose something.
 * <p>
 * <b>Never throws into the page.</b> Any failure is logged and answered with an empty
 * county list, which the field renders as "we don't have that ZIP" — the same
 * fail-closed shape {@code IchraOpportunityAnalyses} uses, and it degrades to the
 * county dropdown rather than to a broken field.
 */
@WebServlet(name = "IchraZipLookup", value = "/IchraZipLookup")
public class IchraZipLookup extends HttpServlet {

    private static final Logger log = LogManager.getLogger(IchraZipLookup.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // A ZIP lookup is stable reference data, but caching it per-URL would let a
        // stale answer outlive a crosswalk update. Cheap query; do not cache.
        response.setHeader("Cache-Control", "no-store");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            writeEmpty(response, request.getParameter("zip"));
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            if (!IchraAccessResolver.isAvailable(em, request)) {
                writeEmpty(response, request.getParameter("zip"));
                return;
            }

            String zip = request.getParameter("zip");
            ZipCountyResolver.Resolution resolution = ZipCountyResolver.resolve(em, zip);
            writeResolution(response, resolution, pricedCountyFips(em, request.getParameter("planYear")));
        } catch (Exception e) {
            log.error("[ZIP-LOOKUP] Failed to resolve; returning empty", e);
            writeEmpty(response, request.getParameter("zip"));
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Writes {@code {"zip":"...","counties":[{"fips":..,"name":..,"state":..}]}}.
     * Counties arrive in the resolver's order — most land area first — and that order
     * is preserved rather than re-sorted. It is a stability convenience, <b>not a
     * ranking</b>, and the page must not render it as one.
     */
    private void writeResolution(HttpServletResponse response, ZipCountyResolver.Resolution resolution,
                                  Set<String> priced) throws IOException {
        StringBuilder json = new StringBuilder(128);
        json.append("{\"zip\":\"").append(escapeJson(resolution.getZip())).append("\",\"counties\":[");

        boolean first = true;
        for (ZipCountyResolver.Candidate candidate : resolution.getCandidates()) {
            if (!first) json.append(',');
            first = false;
            json.append("{\"fips\":\"").append(escapeJson(candidate.getCountyFips()))
                    .append("\",\"name\":\"").append(escapeJson(candidate.getCountyName()))
                    .append("\",\"state\":\"").append(escapeJson(candidate.getState()))
                    .append("\",\"priced\":").append(priced != null && priced.contains(candidate.getCountyFips()))
                    .append('}');
        }

        json.append("]}");

        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    /**
     * County FIPS codes with cached rates for the requested plan year — the same set the
     * illustration's county dropdown is built from.
     * <p>
     * Exists because the crosswalk knows 254 Texas counties (V085) while the illustration
     * can price only the handful that have been warmed, so a chooser that did not say
     * which is which would hand an agent a county and then reject it. The flag lets the
     * page label that honestly <b>before</b> the click.
     * <p>
     * ⚠️ <b>A label, never an ordering.</b> Candidates keep the resolver's land-area order;
     * an unpriced county is not demoted and nothing is pre-selected.
     * <p>
     * A missing or unparseable {@code planYear} yields an empty set, so every county
     * reports {@code priced:false} and the page simply shows the caveat on all of them —
     * the cautious direction, and never an exception.
     */
    private Set<String> pricedCountyFips(EntityManager em, String planYearParam) {
        Set<String> priced = new HashSet<>();
        if (planYearParam == null || planYearParam.isBlank()) {
            return priced;
        }
        try {
            int planYear = Integer.parseInt(planYearParam.trim());
            for (RateCacheDAO.CountySummary summary : RateCacheDAO.getCountySummaries(em, planYear)) {
                priced.add(summary.getCountyFips());
            }
        } catch (Exception e) {
            log.debug("[ZIP-LOOKUP] Could not resolve priced counties for plan year {}", planYearParam, e);
        }
        return priced;
    }

    private void writeEmpty(HttpServletResponse response, String zip) throws IOException {
        PrintWriter out = response.getWriter();
        out.print("{\"zip\":\"" + escapeJson(zip) + "\",\"counties\":[]}");
        out.flush();
    }

    /** Mirrors {@code IchraOpportunityAnalyses.escapeJson} — the codebase's hand-built-JSON convention. */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

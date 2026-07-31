package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.dao.IllustrationLogDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.util.OpportunityAuthz;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.market.IllustrationLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON feed of the ICHRA illustrations and group-to-ICHRA conversion analyses logged
 * against one opportunity (build-plan item 13, the console read). Consumed by the
 * opportunity drawer in {@code agentHome25.jsp}; not a page and not linked from anywhere.
 * <p>
 * <b>What it returns, and what it deliberately does not.</b> One object per row carrying
 * three fields: the display-formatted date, a friendly label for the run kind, and the
 * agent's name. No {@code result_summary}, no premium, no county, no headcount, no plan
 * year, no figure of any kind. An agent looking at a deal needs to know that analysis
 * work happened, when, and by whom — the numbers live on the tool that produced them,
 * behind that tool's own gate and its own agent-only framing (D24). Widening this
 * response is a decision, not a convenience.
 * <p>
 * <b>Three checks, every path, no exceptions.</b> This endpoint takes a record id
 * straight off the query string, so the order is: ICHRA entitlement
 * ({@link IchraAccessResolver#isAvailable}), then parse, then per-record authorization
 * ({@link OpportunityAuthz#canAccessOpportunity}). Every failure returns the same empty
 * array — a caller cannot tell "no such opportunity" from "not yours" from "ICHRA is off
 * for you", because that distinction is itself a disclosure.
 * <p>
 * <b>Never throws into the drawer.</b> Any unexpected failure is logged and answered with
 * an empty array, which the drawer renders as nothing at all. A broken ICHRA feature must
 * not degrade the shared sales pipeline for agents who never asked for ICHRA (build
 * rule 1).
 */
@WebServlet(name = "IchraOpportunityAnalyses", value = "/IchraOpportunityAnalyses")
public class IchraOpportunityAnalyses extends HttpServlet {

    private static final Logger log = LogManager.getLogger(IchraOpportunityAnalyses.class);

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String EMPTY_ARRAY = "[]";

    /**
     * Friendly labels for {@code illustration_log.mode}. An unrecognised value renders as
     * itself rather than as "Unknown" — a mode this build has not heard of is more useful
     * shown raw than hidden behind a placeholder.
     */
    private static final Map<String, String> MODE_LABELS = Map.of(
            "RANGE", "Rate range",
            "AGE_BAND", "Age-band net cost",
            "CONVERSION", "Group conversion"
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            writeEmpty(response);
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            // 1. ICHRA entitlement. Same live, per-request call every other ICHRA surface
            //    makes — never the session-cached nav hint.
            if (!IchraAccessResolver.isAvailable(em, request)) {
                writeEmpty(response);
                return;
            }

            // 2. Parse. Absent, blank or unparseable is indistinguishable from not found.
            Long opportunityId = parseOpportunityId(request.getParameter("opportunityId"));
            if (opportunityId == null) {
                writeEmpty(response);
                return;
            }

            // 3. Per-record authorization. The id came off the query string, so this is
            //    the check that stops one agency reading another's analysis history.
            //    Covers "no such opportunity" and "not permitted" with one identical answer.
            if (!OpportunityAuthz.canAccessOpportunity(em, request, opportunityId)) {
                writeEmpty(response);
                return;
            }

            List<IllustrationLog> rows = IllustrationLogDAO.findByOpportunityId(em, opportunityId);
            writeRows(response, em, rows);
        } catch (Exception e) {
            log.error("[ICHRA-ANALYSES] Failed to build opportunity analysis list; returning empty", e);
            writeEmpty(response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /** @return the parsed id, or null if missing, blank or non-numeric. */
    private Long parseOpportunityId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Writes the JSON array. Agent names are resolved once per distinct person id — a
     * drawer opening on an opportunity with 25 runs by the same agent must not issue 25
     * identical lookups.
     */
    private void writeRows(HttpServletResponse response, EntityManager em, List<IllustrationLog> rows) throws IOException {
        Map<Long, String> nameCache = new HashMap<>();
        StringBuilder json = new StringBuilder(256);
        json.append('[');

        boolean first = true;
        for (IllustrationLog row : rows) {
            if (!first) json.append(',');
            first = false;

            json.append("{\"createdAt\":\"")
                    .append(row.getCreatedAt() != null ? escapeJson(row.getCreatedAt().format(DISPLAY_FORMAT)) : "")
                    .append("\",\"kind\":\"")
                    .append(escapeJson(modeLabel(row.getMode())))
                    .append("\",\"agent\":\"")
                    .append(escapeJson(agentName(em, nameCache, row.getAgentPersonId())))
                    .append("\"}");
        }

        json.append(']');

        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    private void writeEmpty(HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(EMPTY_ARRAY);
        out.flush();
    }

    /** Friendly label, or the raw value when this build does not recognise the mode. */
    private String modeLabel(String mode) {
        if (mode == null || mode.isBlank()) {
            return "";
        }
        return MODE_LABELS.getOrDefault(mode, mode);
    }

    /**
     * Display name for a logged agent person id, in the same "first last" shape
     * {@code agentHome25.jsp}'s own OPPS map already uses. A missing or unresolvable
     * person yields an empty string rather than a placeholder — the drawer simply shows
     * the date and kind without an agent.
     */
    private String agentName(EntityManager em, Map<Long, String> cache, Long personId) {
        if (personId == null) {
            return "";
        }
        String cached = cache.get(personId);
        if (cached != null) {
            return cached;
        }
        String name = "";
        try {
            Person person = em.find(Person.class, personId);
            if (person != null) {
                String first = person.getFirstName() != null ? person.getFirstName() : "";
                String last = person.getLastName() != null ? person.getLastName() : "";
                name = (first + " " + last).trim();
            }
        } catch (Exception e) {
            log.debug("[ICHRA-ANALYSES] Could not resolve agent name for person {}", personId, e);
        }
        cache.put(personId, name);
        return name;
    }

    /** Mirrors SetupModalData.escapeJson — the codebase's hand-built-JSON convention. */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

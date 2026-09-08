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
import net.superiorstate.ams.data.dao.SummitFileExportDAO;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.SummitFileExport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * S32-G — the retained Summit export listing (T212).
 * <p>
 * V096 gave {@code summit_file_export} a row per generated file, and until this screen existed
 * <b>nothing in the running application could see any of it</b> — all three
 * {@link SummitFileExportDAO} reads had no caller. The record was built to make two things
 * possible, and both need a screen:
 * <ul>
 *   <li><b>Re-download the bytes that were actually sent.</b> Summit's results file for file 2
 *       carries no row number, so correlating a result back to what was sent falls back to
 *       {@code Plan Name}; Demographics correlates on row number. Either way the comparison needs
 *       the sent bytes, and a regeneration is not a substitute — plan-template mappings (V095),
 *       {@code SUMMIT_BRANCH_CODE}, {@code SUMMIT_TPA_ID_PREFIX} and the participant roster can all
 *       have moved underneath an export since it ran.</li>
 *   <li><b>T194 — Summit dedupes a re-sent file on content.</b> Re-sending identical bytes is a
 *       silent no-op: no error, no results row, nothing. This screen marks every row whose content
 *       hash it shares with another, so that is visible <i>before</i> a send rather than after a
 *       confusing non-result.</li>
 * </ul>
 * <p>
 * ⚠️ <b>This screen writes nothing, ever.</b> It has no {@code doPost}, it opens no transaction, and
 * a download does <b>not</b> record a new export row — the download serves stored bytes and is not
 * a generation event. {@code SummitExportServlet} remains the only writer of this table.
 * <p>
 * ⚠️ <b>Never regenerate.</b> The download action serves {@link SummitFileExport#getContent()}
 * verbatim. Regenerating would defeat the entire purpose of the record.
 * <p>
 * ⚠️ <b>The listed content holds PII</b> — participant names, addresses and emails ride in the
 * Demographics file's rows. That is why the list never selects or previews {@code content}, why the
 * gate is PSP-admin, and why {@link #downloadOne} re-checks the row's PSP against the session's
 * rather than trusting an id from a query string.
 * <p>
 * ⚠️ <b>No timezone arithmetic anywhere in this class or its JSP.</b> T216 established by a page
 * view on 2026-09-08 that the EclipseLink round trip is symmetric — {@code /RateCacheAdmin} showed
 * {@code 16:04} against a stored {@code 21:04:32} — so {@code generated_at} reads back as local
 * time and is displayed as-is. Adding a conversion here would introduce the very five-hour skew
 * T216 was filed about.
 */
@WebServlet(name = "SummitFileExportAdmin", value = "/SummitFileExportAdmin")
public class SummitFileExportAdmin extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitFileExportAdmin.class);

    private static final String VIEW = "/WEB-INF/view/market/summitFileExportAdmin25.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = resolveCurrentPspId(request);

            if ("download".equals(request.getParameter("action"))) {
                downloadOne(request, response, em, pspId);
                return;
            }

            // An unresolvable PSP lists nothing rather than listing everything. The DAO already
            // returns an empty list for a null pspId; this keeps that explicit at the call site.
            List<SummitFileExport> exports = SummitFileExportDAO.findByPspId(em, pspId);

            request.setAttribute("exports", exports);
            request.setAttribute("duplicateHash", duplicateHashMap(exports));
            request.setAttribute("pspResolved", pspId != null);

            request.getRequestDispatcher(VIEW).forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Serves one stored export's bytes back, byte for byte, under its original filename.
     * <p>
     * ⚠️ <b>The content type, character encoding and {@code Content-Disposition} shape are copied
     * from {@code SummitExportServlet.writeFile}</b> so a re-download is indistinguishable from the
     * original download to whatever consumes it. They were read from that file, not invented, and
     * that file was not edited.
     * <p>
     * ⚠️ <b>The PSP check is the access control, not the link.</b> {@code findById} is deliberately
     * unscoped, so an id typed into the query string would otherwise read another PSP's
     * participants. A row belonging to a different PSP is treated exactly as a missing one — the
     * response is the same 404 either way, so the endpoint does not confirm that an id exists.
     */
    private void downloadOne(HttpServletRequest request, HttpServletResponse response,
                             EntityManager em, Long pspId) throws IOException {

        Long id = parseLong(request.getParameter("id"));
        SummitFileExport row = SummitFileExportDAO.findById(em, id);

        if (row == null || pspId == null || !pspId.equals(row.getPspId())) {
            log.warn("[SUMMIT-EXPORT-ADMIN] refused download of export id {} for psp {}",
                    id, pspId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // The stored bytes, verbatim. Never regenerated -- see the class note.
        String content = row.getContent() == null ? "" : row.getContent();

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + row.getFileName() + "\"");
        PrintWriter out = response.getWriter();
        out.print(content);
        out.flush();
    }

    /**
     * Maps a content hash to {@code TRUE} when two or more listed rows carry it, and omits every
     * hash that appears once.
     * <p>
     * Computed in memory from the list already loaded rather than through
     * {@link SummitFileExportDAO#findByContentHash}, which would cost one query per row to answer
     * the same question. A map rather than a set because JSTL EL reads
     * {@code ${duplicateHash[e.contentSha256]}} on any container, while set membership needs EL
     * method invocation.
     */
    private static Map<String, Boolean> duplicateHashMap(List<SummitFileExport> exports) {
        Map<String, Boolean> result = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (SummitFileExport e : exports) {
            String sha = e.getContentSha256();
            if (sha == null) continue;
            if (!seen.add(sha)) result.put(sha, Boolean.TRUE);
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Verbatim from {@code SummitPlanTemplateAdmin.isAuthorized} — the gate is copied, not invented. */
    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    /**
     * The session's PSP id, or null. Same walk {@code SummitExportServlet} and
     * {@code SummitPlanTemplateAdmin} use; null is handled by the caller rather than thrown.
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    private static Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

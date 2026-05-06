package net.superiorstate.ams.controller.assistant;

import com.google.gson.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.KnowledgeBaseDAO;
import net.superiorstate.ams.data.dao.KnowledgeChunkDAO;
import net.superiorstate.ams.data.service.KnowledgeSearchService;
import net.superiorstate.ams.model.general.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Admin CRUD servlet for the Knowledge Manager — browse, create, edit,
 * activate/deactivate, and hard-delete knowledge chunks.
 *
 * PSP Admin only.
 *
 * URL: /KnowledgeManager
 */
@WebServlet(name = "KnowledgeManager", value = "/KnowledgeManager")
public class KnowledgeManager extends HttpServlet {

    private static final Logger log = LogManager.getLogger(KnowledgeManager.class);
    // Default Gson keeps HTML escaping ON — safe to embed in <script> without </script> breakage.
    private static final Gson gson = new Gson();

    // ── GET — list view ───────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        AmsDataLocal local = (session != null) ? (AmsDataLocal) session.getAttribute("local") : null;
        if (local == null || !local.isPspAdmin()) {
            response.sendRedirect("ViewHome25");
            return;
        }

        // Consume flash messages set by POST-redirect
        String kmMessage = (String) session.getAttribute("kmMessage");
        String kmError   = (String) session.getAttribute("kmError");
        if (kmMessage != null) { request.setAttribute("kmMessage", kmMessage); session.removeAttribute("kmMessage"); }
        if (kmError   != null) { request.setAttribute("kmError",   kmError);   session.removeAttribute("kmError");   }

        // Parse filter params
        String selectedKbKey     = emptyToNull(request.getParameter("kbKey"));
        String chunkTypeFilter   = emptyToNull(request.getParameter("chunkType"));
        String accountTypeFilter = emptyToNull(request.getParameter("accountType"));
        boolean activeOnly       = !"false".equals(request.getParameter("activeOnly")); // default true
        String search            = emptyToNull(request.getParameter("search"));

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Load all KBs (ordered by sort_order)
            List<KnowledgeBase> allKbs = KnowledgeBaseDAO.getAll(em);

            // Default selectedKbKey to first KB
            if (selectedKbKey == null && !allKbs.isEmpty()) {
                selectedKbKey = allKbs.get(0).getKbKey();
            }

            // Find the selected KB entity
            KnowledgeBase selectedKb = null;
            for (KnowledgeBase kb : allKbs) {
                if (kb.getKbKey().equals(selectedKbKey)) { selectedKb = kb; break; }
            }

            // Load chunks for selected KB, then apply Java-side filters
            List<KnowledgeChunk> chunks = Collections.emptyList();
            Map<Long, List<KnowledgeChunkHistory>> historyMap = new LinkedHashMap<>();
            JsonObject chunkDataJson  = new JsonObject();
            JsonObject historyDataJson = new JsonObject();

            if (selectedKb != null) {
                chunks = activeOnly
                        ? KnowledgeChunkDAO.getActiveByKb(em, selectedKb.getId())
                        : KnowledgeChunkDAO.getAllByKb(em, selectedKb.getId());

                chunks = applyFilters(chunks, chunkTypeFilter, accountTypeFilter, search);

                // Load history and build JSON maps while EM is open (lazy Person)
                for (KnowledgeChunk chunk : chunks) {
                    // Build chunk data JSON (safe embedding: Gson html-escapes < > &)
                    chunkDataJson.add(String.valueOf(chunk.getId()),
                            chunkToJson(chunk));

                    // Load and embed history (JOIN FETCH loads Person eagerly)
                    List<KnowledgeChunkHistory> hist =
                            KnowledgeChunkDAO.getHistoryByChunkId(em, chunk.getId());
                    historyMap.put(chunk.getId(), hist);
                    historyDataJson.add(String.valueOf(chunk.getId()),
                            historyToJsonArray(hist));
                }
            }

            // Per-KB stats: active chunk count + last modified
            // Single aggregate query: Object[] = [kbId (Long), count (Long), maxDate (Timestamp)]
            List<Object[]> statsRows = em.createQuery(
                    "SELECT c.knowledgeBase.id, COUNT(c), MAX(c.dateModified) " +
                    "FROM KnowledgeChunk c WHERE c.active = true GROUP BY c.knowledgeBase.id",
                    Object[].class)
                    .getResultList();

            Map<Long, Long>      kbActiveCount   = new HashMap<>();
            Map<Long, Timestamp> kbLastModified  = new HashMap<>();
            for (Object[] row : statsRows) {
                Long kbId   = (Long)      row[0];
                Long cnt    = (Long)      row[1];
                Timestamp t = (Timestamp) row[2];
                kbActiveCount.put(kbId, cnt);
                kbLastModified.put(kbId, t);
            }

            // Request attributes
            request.setAttribute("allKbs",           allKbs);
            request.setAttribute("selectedKbKey",    selectedKbKey);
            request.setAttribute("selectedKb",       selectedKb);
            request.setAttribute("chunks",           chunks);
            request.setAttribute("kbActiveCount",    kbActiveCount);
            request.setAttribute("kbLastModified",   kbLastModified);
            request.setAttribute("chunkTypeFilter",  chunkTypeFilter);
            request.setAttribute("accountTypeFilter",accountTypeFilter);
            request.setAttribute("activeOnly",       activeOnly);
            request.setAttribute("search",           search);
            request.setAttribute("chunkTypes",       KnowledgeChunkType.values());
            request.setAttribute("visibilities",     KnowledgeChunkVisibility.values());
            request.setAttribute("chunkDataJson",    gson.toJson(chunkDataJson));
            request.setAttribute("historyDataJson",  gson.toJson(historyDataJson));

        } finally {
            em.close();
        }

        request.getRequestDispatcher("/WEB-INF/view/a/assistant/knowledgeManager25.jsp")
                .forward(request, response);
    }

    // ── POST — mutations ─────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        AmsDataLocal local = (session != null) ? (AmsDataLocal) session.getAttribute("local") : null;
        if (local == null || !local.isPspAdmin()) {
            response.sendRedirect("ViewHome25");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) { response.sendRedirect("KnowledgeManager"); return; }

        // Preserve the selected KB in the redirect URL
        String kbKey = emptyToNull(request.getParameter("kbKey"));
        String redirectUrl = "KnowledgeManager" + (kbKey != null ? "?kbKey=" + kbKey : "");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person currentPerson = local.getCurrentPerson();

        try {
            switch (action) {
                case "createChunk" -> {
                    KnowledgeChunk chunk = buildChunkFromRequest(request, em);
                    KnowledgeChunkDAO.create(em, chunk, currentPerson);
                    triggerReload(request);
                    flashOk(session, "Chunk \"" + chunk.getTitle() + "\" created.");
                    log.info("KnowledgeManager: created chunk '{}' in KB '{}'", chunk.getTitle(), kbKey);
                }
                case "updateChunk" -> {
                    Long chunkId = parseLong(request.getParameter("chunkId"));
                    KnowledgeChunk updated = buildChunkFromRequest(request, em);
                    updated.setId(chunkId);
                    String changeNote = emptyToNull(request.getParameter("changeNote"));
                    KnowledgeChunkDAO.update(em, updated, currentPerson, changeNote);
                    triggerReload(request);
                    flashOk(session, "Chunk updated.");
                    log.info("KnowledgeManager: updated chunk ID {}", chunkId);
                }
                case "activateChunk" -> {
                    Long chunkId = parseLong(request.getParameter("chunkId"));
                    KnowledgeChunkDAO.activate(em, chunkId, currentPerson, null);
                    triggerReload(request);
                    flashOk(session, "Chunk activated.");
                    log.info("KnowledgeManager: activated chunk ID {}", chunkId);
                }
                case "deactivateChunk" -> {
                    Long chunkId = parseLong(request.getParameter("chunkId"));
                    String changeNote = emptyToNull(request.getParameter("changeNote"));
                    KnowledgeChunkDAO.deactivate(em, chunkId, currentPerson, changeNote);
                    triggerReload(request);
                    flashOk(session, "Chunk deactivated.");
                    log.info("KnowledgeManager: deactivated chunk ID {}", chunkId);
                }
                case "deleteChunk" -> {
                    Long chunkId = parseLong(request.getParameter("chunkId"));
                    KnowledgeChunkDAO.hardDelete(em, chunkId, currentPerson, "Hard-deleted via Knowledge Manager");
                    triggerReload(request);
                    flashOk(session, "Chunk permanently deleted.");
                    log.info("KnowledgeManager: hard-deleted chunk ID {}", chunkId);
                }
                case "bulkImportChunks" -> {
                    String json   = request.getParameter("importJson");
                    String importKbKey = emptyToNull(request.getParameter("importKbKey"));
                    int count = handleBulkImport(json, importKbKey, em, currentPerson);
                    triggerReload(request);
                    flashOk(session, count + " chunk(s) imported successfully.");
                    log.info("KnowledgeManager: bulk-imported {} chunks into KB '{}'", count, importKbKey);
                    // Redirect to the imported-into KB
                    if (importKbKey != null) redirectUrl = "KnowledgeManager?kbKey=" + importKbKey;
                }
                case "reloadCache" -> {
                    triggerReload(request);
                    flashOk(session, "Knowledge cache reloaded.");
                    log.info("KnowledgeManager: manual cache reload triggered");
                }
                default -> log.warn("KnowledgeManager: unknown action '{}'", action);
            }
        } catch (Exception e) {
            log.error("KnowledgeManager error for action {}", action, e);
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            flashErr(session, "Error: " + e.getMessage());
        } finally {
            em.close();
        }

        response.sendRedirect(redirectUrl);
    }

    // ── Bulk import ──────────────────────────────────────────────────

    /**
     * All-or-nothing bulk import of knowledge chunks from a JSON array.
     *
     * <p>Two-pass design:
     * <ol>
     *   <li><b>Pass 1 — validate everything</b> without any DB writes. Collects all
     *       validation errors with 1-based entry indexes. If any errors are found,
     *       throws {@link IllegalArgumentException} with a formatted message listing
     *       up to 5 specifics plus a total count. Zero rows are written.</li>
     *   <li><b>Pass 2 — insert</b> all chunks inside a single outer transaction.
     *       Because pass 1 guarantees correctness, pass 2 should never fail on
     *       validation grounds. If a runtime error does occur (e.g. DB connection
     *       drop), the outer transaction rolls back every insert in the batch.</li>
     * </ol>
     *
     * <p>Expected JSON format:
     * <pre>
     * [
     *   {
     *     "kbKey": "style_voice",        // optional if default KB selected
     *     "title": "...",                // required, max 200 chars
     *     "content": "...",              // required
     *     "chunkType": "STYLE_RULE",     // required, must be a valid enum value
     *     "section": "...",              // optional
     *     "keywords": "...",             // optional, comma-separated
     *     "accountType": "...",          // optional
     *     "visibility": "INTERNAL",      // optional (PUBLIC/INTERNAL/ADMIN_ONLY)
     *     "sourceCitation": "...",       // optional
     *     "effectiveStart": "2025-01-01",// optional, YYYY-MM-DD
     *     "effectiveEnd":   "2026-01-01" // optional, YYYY-MM-DD
     *   }
     * ]
     * </pre>
     */
    private int handleBulkImport(String json, String defaultKbKey,
                                  EntityManager em, Person currentPerson) {

        // ── Parse ─────────────────────────────────────────────────────
        if (json == null || json.isBlank())
            throw new IllegalArgumentException("Import JSON is empty.");

        JsonArray arr;
        try {
            arr = gson.fromJson(json, JsonArray.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage());
        }
        if (arr == null || arr.size() == 0)
            throw new IllegalArgumentException("JSON array is empty.");

        // ── Pass 1: validate all entries — no DB writes ───────────────
        List<String> errors = new ArrayList<>();
        // Cache KB lookups so we don't hit the DB once per entry for the same key.
        Map<String, KnowledgeBase> kbCache = new HashMap<>();

        for (int i = 0; i < arr.size(); i++) {
            int entryNum = i + 1;
            JsonObject obj = arr.get(i).getAsJsonObject();

            // ── kbKey ──
            String resolvedKbKey = obj.has("kbKey") && !obj.get("kbKey").isJsonNull()
                    ? obj.get("kbKey").getAsString().trim() : defaultKbKey;

            if (resolvedKbKey == null || resolvedKbKey.isBlank()) {
                errors.add("Entry " + entryNum + ": missing 'kbKey' and no default KB selected.");
            } else {
                if (!kbCache.containsKey(resolvedKbKey)) {
                    kbCache.put(resolvedKbKey, KnowledgeBaseDAO.getByKey(em, resolvedKbKey));
                }
                if (kbCache.get(resolvedKbKey) == null) {
                    errors.add("Entry " + entryNum + ": unknown kbKey '" + resolvedKbKey + "'.");
                }
            }

            // ── title (required, max 200) ──
            String title = getStr(obj, "title");
            if (title == null || title.isBlank()) {
                errors.add("Entry " + entryNum + ": missing required field 'title'.");
            } else if (title.length() > 200) {
                errors.add("Entry " + entryNum + ": 'title' exceeds 200 characters (" + title.length() + ").");
            }

            // ── content (required) ──
            String content = getStr(obj, "content");
            if (content == null || content.isBlank()) {
                errors.add("Entry " + entryNum + ": missing required field 'content'.");
            }

            // ── chunkType (required, must be a valid enum value) ──
            String typeName = getStr(obj, "chunkType");
            if (typeName == null || typeName.isBlank()) {
                errors.add("Entry " + entryNum + ": missing required field 'chunkType'.");
            } else {
                try {
                    KnowledgeChunkType.valueOf(typeName.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    errors.add("Entry " + entryNum + ": invalid chunkType '" + typeName + "'.");
                }
            }

            // ── visibility (optional, but must be valid if supplied) ──
            String visName = getStr(obj, "visibility");
            if (visName != null && !visName.isBlank()) {
                try {
                    KnowledgeChunkVisibility.valueOf(visName.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    errors.add("Entry " + entryNum + ": invalid visibility '" + visName + "'. "
                            + "Valid values: PUBLIC, INTERNAL, ADMIN_ONLY.");
                }
            }

            // ── effectiveStart (optional, must be ISO date if supplied) ──
            String startStr = getStr(obj, "effectiveStart");
            if (startStr != null && !startStr.isBlank() && parseDate(startStr) == null) {
                errors.add("Entry " + entryNum + ": 'effectiveStart' is not a valid date ('"
                        + startStr + "'). Use YYYY-MM-DD.");
            }

            // ── effectiveEnd (optional, must be ISO date if supplied) ──
            String endStr = getStr(obj, "effectiveEnd");
            if (endStr != null && !endStr.isBlank() && parseDate(endStr) == null) {
                errors.add("Entry " + entryNum + ": 'effectiveEnd' is not a valid date ('"
                        + endStr + "'). Use YYYY-MM-DD.");
            }
        }

        // If any validation errors, abort with a detailed message — zero rows written.
        if (!errors.isEmpty()) {
            int total = errors.size();
            StringBuilder msg = new StringBuilder();
            msg.append("Import failed: ").append(total)
               .append(total == 1 ? " error" : " errors")
               .append(" found, no chunks created. ");
            int show = Math.min(total, 5);
            for (int i = 0; i < show; i++) {
                msg.append(errors.get(i));
                if (i < show - 1) msg.append(" ");
            }
            if (total > 5) {
                int remainder = total - 5;
                msg.append(" ...and ").append(remainder)
                   .append(remainder == 1 ? " more error." : " more errors.");
            }
            throw new IllegalArgumentException(msg.toString());
        }

        // ── Pass 2: all valid — insert in a single outer transaction ──
        // Uses createNoTx so EclipseLink doesn't try to nest a second transaction
        // inside our outer one (RESOURCE_LOCAL doesn't support nested transactions).
        try {
            em.getTransaction().begin();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();

                String resolvedKbKey = obj.has("kbKey") && !obj.get("kbKey").isJsonNull()
                        ? obj.get("kbKey").getAsString().trim() : defaultKbKey;
                KnowledgeBase kb = kbCache.get(resolvedKbKey);

                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setKnowledgeBase(kb);
                chunk.setTitle(getStr(obj, "title"));
                chunk.setSection(getStr(obj, "section"));
                chunk.setContent(getStr(obj, "content"));
                chunk.setKeywords(getStr(obj, "keywords"));
                chunk.setAccountType(getStr(obj, "accountType"));
                chunk.setSourceCitation(getStr(obj, "sourceCitation"));
                chunk.setActive(true);
                chunk.setChunkType(KnowledgeChunkType.valueOf(getStr(obj, "chunkType").toUpperCase()));

                String visName = getStr(obj, "visibility");
                chunk.setVisibility(
                        (visName != null && !visName.isBlank())
                        ? KnowledgeChunkVisibility.valueOf(visName.toUpperCase())
                        : KnowledgeChunkVisibility.INTERNAL);

                String startStr = getStr(obj, "effectiveStart");
                String endStr   = getStr(obj, "effectiveEnd");
                if (startStr != null && !startStr.isBlank()) chunk.setEffectiveStart(parseDate(startStr));
                if (endStr   != null && !endStr.isBlank())   chunk.setEffectiveEnd(parseDate(endStr));

                KnowledgeChunkDAO.createNoTx(em, chunk, currentPerson);
            }
            em.getTransaction().commit();
            return arr.size();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    // ── Builder helpers ──────────────────────────────────────────────

    /** Builds a KnowledgeChunk from POST parameters. Does NOT set id or knowledgeBase.id. */
    private KnowledgeChunk buildChunkFromRequest(HttpServletRequest request, EntityManager em) {
        KnowledgeChunk chunk = new KnowledgeChunk();

        // KB lookup — kbKey param determines which KB
        String kbKey = request.getParameter("chunkKbKey");
        if (kbKey != null && !kbKey.isBlank()) {
            KnowledgeBase kb = KnowledgeBaseDAO.getByKey(em, kbKey.trim());
            if (kb == null) throw new IllegalArgumentException("Unknown kbKey: " + kbKey);
            chunk.setKnowledgeBase(kb);
        }

        chunk.setTitle(request.getParameter("title"));
        chunk.setSection(emptyToNull(request.getParameter("section")));
        chunk.setContent(request.getParameter("content"));
        chunk.setKeywords(emptyToNull(request.getParameter("keywords")));
        chunk.setAccountType(emptyToNull(request.getParameter("accountType")));
        chunk.setSourceCitation(emptyToNull(request.getParameter("sourceCitation")));

        String typeName = request.getParameter("chunkType");
        try {
            chunk.setChunkType(KnowledgeChunkType.valueOf(typeName));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid chunkType: " + typeName);
        }

        String visName = request.getParameter("visibility");
        try {
            chunk.setVisibility(KnowledgeChunkVisibility.valueOf(
                    visName != null && !visName.isBlank() ? visName : "INTERNAL"));
        } catch (IllegalArgumentException e) {
            chunk.setVisibility(KnowledgeChunkVisibility.INTERNAL);
        }

        String startStr = emptyToNull(request.getParameter("effectiveStart"));
        String endStr   = emptyToNull(request.getParameter("effectiveEnd"));
        if (startStr != null) chunk.setEffectiveStart(parseDate(startStr));
        if (endStr   != null) chunk.setEffectiveEnd(parseDate(endStr));

        // active checkbox — present means true; absent in POST body means false
        chunk.setActive("true".equals(request.getParameter("active"))
                     || "on".equals(request.getParameter("active")));

        return chunk;
    }

    // ── JSON helpers ─────────────────────────────────────────────────

    private JsonObject chunkToJson(KnowledgeChunk c) {
        JsonObject o = new JsonObject();
        o.addProperty("id",             c.getId());
        o.addProperty("kbKey",          c.getKnowledgeBase() != null ? c.getKnowledgeBase().getKbKey() : "");
        o.addProperty("title",          nvl(c.getTitle()));
        o.addProperty("section",        nvl(c.getSection()));
        o.addProperty("content",        nvl(c.getContent()));
        o.addProperty("keywords",       nvl(c.getKeywords()));
        o.addProperty("accountType",    nvl(c.getAccountType()));
        o.addProperty("chunkType",      c.getChunkType() != null ? c.getChunkType().name() : "");
        o.addProperty("visibility",     c.getVisibility() != null ? c.getVisibility().name() : "INTERNAL");
        o.addProperty("sourceCitation", nvl(c.getSourceCitation()));
        o.addProperty("effectiveStart", c.getEffectiveStart() != null ? c.getEffectiveStart().toString() : "");
        o.addProperty("effectiveEnd",   c.getEffectiveEnd()   != null ? c.getEffectiveEnd().toString()   : "");
        o.addProperty("active",         c.isActive());
        return o;
    }

    private JsonArray historyToJsonArray(List<KnowledgeChunkHistory> hist) {
        JsonArray arr = new JsonArray();
        for (KnowledgeChunkHistory h : hist) {
            JsonObject o = new JsonObject();
            o.addProperty("changeType",  h.getChangeType()  != null ? h.getChangeType().name() : "");
            o.addProperty("changeNote",  nvl(h.getChangeNote()));
            o.addProperty("titleBefore", nvl(h.getTitleBefore()));
            o.addProperty("activeBefore", h.getActiveBefore() != null ? h.getActiveBefore() : false);
            String who = "System";
            if (h.getModifiedBy() != null) {
                who = nvl(h.getModifiedBy().getFirstName()) + " " + nvl(h.getModifiedBy().getLastName());
                who = who.trim();
            }
            o.addProperty("modifiedBy",  who.isEmpty() ? "System" : who);
            o.addProperty("modifiedOn",  h.getModifiedOn() != null ? h.getModifiedOn().toString() : "");
            arr.add(o);
        }
        return arr;
    }

    // ── Filter ───────────────────────────────────────────────────────

    private List<KnowledgeChunk> applyFilters(List<KnowledgeChunk> chunks,
                                               String chunkTypeFilter,
                                               String accountTypeFilter,
                                               String search) {
        List<KnowledgeChunk> result = chunks;

        if (chunkTypeFilter != null && !chunkTypeFilter.isBlank()) {
            try {
                KnowledgeChunkType type = KnowledgeChunkType.valueOf(chunkTypeFilter);
                result = result.stream()
                        .filter(c -> type.equals(c.getChunkType()))
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        if (accountTypeFilter != null && !accountTypeFilter.isBlank()) {
            String af = accountTypeFilter.trim().toLowerCase();
            result = result.stream()
                    .filter(c -> c.getAccountType() != null
                              && c.getAccountType().toLowerCase().contains(af))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String s = search.trim().toLowerCase();
            result = result.stream()
                    .filter(c -> matches(c.getTitle(),   s)
                              || matches(c.getSection(), s)
                              || matches(c.getContent(), s)
                              || matches(c.getKeywords(), s))
                    .collect(java.util.stream.Collectors.toList());
        }

        return result;
    }

    private boolean matches(String field, String lower) {
        return field != null && field.toLowerCase().contains(lower);
    }

    // ── Misc helpers ─────────────────────────────────────────────────

    private void triggerReload(HttpServletRequest request) {
        KnowledgeSearchService ks =
                (KnowledgeSearchService) request.getServletContext().getAttribute("knowledgeService");
        if (ks != null && ks.isInitialized()) {
            ks.reload();
        }
    }

    private void flashOk(HttpSession session, String msg) {
        session.setAttribute("kmMessage", msg);
    }

    private void flashErr(HttpSession session, String msg) {
        session.setAttribute("kmError", msg);
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String getStr(JsonObject obj, String field) {
        return (obj.has(field) && !obj.get(field).isJsonNull())
                ? obj.get(field).getAsString().trim() : "";
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("Missing numeric ID parameter");
        return Long.parseLong(s.trim());
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

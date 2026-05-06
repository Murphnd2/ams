package net.superiorstate.ams.controller.assistant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.ChatbotSkillDAO;
import net.superiorstate.ams.data.dao.PersonDAO;
import net.superiorstate.ams.data.service.ClaudeApiService;
import net.superiorstate.ams.data.service.KnowledgeSearchService;
import net.superiorstate.ams.model.general.ChatbotSkill;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * AJAX endpoint that drafts structured email replies grounded in knowledge bases.
 *
 * Loads the EMAIL_DRAFT_ASSISTANT chatbot skill from the DB (system prompt is
 * editable via Skill Manager without redeploying). Looks up AMS context for the
 * sender email, retrieves always-loaded style/voice chunks + searched chunks from
 * federal_rules / ssa_business / summit_supplemental KBs, assembles a prompt, and
 * calls Claude Sonnet. Returns a structured JSON response with subject, body,
 * escalation, encryption, completeness, and metadata fields.
 *
 * PSP Admin only. POST only.
 *
 * URL: /EmailAssistantDraft
 */
@WebServlet(name = "EmailAssistantDraft", value = "/EmailAssistantDraft")
public class EmailAssistantDraft extends HttpServlet {

    private static final Logger log = LogManager.getLogger(EmailAssistantDraft.class);
    private static final Gson gson = new Gson();

    /** Fallback model if the skill row is missing its model field. */
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final int DEFAULT_MAX_TOKENS = 4096;

    /** Well-known skill name seeded by V065. */
    private static final String SKILL_NAME = "EMAIL_DRAFT_ASSISTANT";

    /** Hardcoded PSP entity ID — the per-installation PSP convention. */
    private static final long SKILL_PSP_ID = 4L;

    /** Cached service reference — populated lazily on first request, same pattern as ChatAssistant. */
    private KnowledgeSearchService knowledgeService;

    // ── KB routing constants ──────────────────────────────────────────────────

    private static final Set<String> ALWAYS_LOAD_KBS = Set.of("style_voice");
    private static final Set<String> SEARCH_KBS =
            Set.of("federal_rules", "ssa_business", "summit_supplemental");

    /** Per-KB retrieval caps for the retrieval-split logic. */
    private static final int MAX_FEDERAL = 6;
    private static final int MAX_SSA     = 3;
    private static final int MAX_SUMMIT  = 2;

    // ── Request / response DTOs (Gson-deserializable) ─────────────────────────

    private static class DraftRequest {
        String inboundSubject;
        String inboundBody;
        String inboundSender;
        String inboundDate;
        String userNotes;
        String draftMode;         // FRESH | REFINE
        JsonObject previousDraft; // for REFINE mode
    }

    private static class Completeness {
        boolean planNamed;
        boolean dateCited;
        boolean nextStepsStated;
        String toneCalibrated;
    }

    private static class DraftResponse {
        String subject;
        String body;
        String draftType;
        Completeness completeness;
        String encryption;
        String encryptionReason;
        String escalation;
        String escalationReason;
        String escalationQuestionForKevin;
    }

    private static class SenderContext {
        boolean resolved   = false;
        String  name;
        String  senderType = "OTHER";
        String  employerName;
        String  planYearRange;
    }

    // ── Servlet handler ───────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Auth — PSP Admin only
        HttpSession session = request.getSession(false);
        AmsDataLocal local  = (session != null)
                ? (AmsDataLocal) session.getAttribute("local")
                : null;
        if (local == null || !local.isPspAdmin()) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN,
                      "unauthorized", "PSP Admin role required", null);
            return;
        }

        // Parse request body
        DraftRequest req;
        try {
            req = gson.fromJson(request.getReader(), DraftRequest.class);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                      "invalid_request", "Invalid JSON body", e.getMessage());
            return;
        }

        if (req == null || isBlank(req.inboundSubject)
                || isBlank(req.inboundBody)
                || isBlank(req.inboundSender)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                      "missing_fields",
                      "inboundSubject, inboundBody, and inboundSender are required", null);
            return;
        }

        if (req.draftMode == null) req.draftMode = "FRESH";

        log.info("EmailAssistantDraft: sender={}, mode={}, subject.len={}, body.len={}",
                req.inboundSender, req.draftMode,
                req.inboundSubject.length(), req.inboundBody.length());

        // Knowledge service — lazy-init on first request, same pattern as ChatAssistant
        ensureKnowledgeService(request);
        if (knowledgeService == null || !knowledgeService.isInitialized()) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      "service_unavailable", "Knowledge service not initialized", null);
            return;
        }

        EntityManagerFactory emf =
                (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Load skill (system prompt + model config)
            ChatbotSkill skill = ChatbotSkillDAO.getBySkillName(em, SKILL_NAME, SKILL_PSP_ID);
            if (skill == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          "skill_missing",
                          "EMAIL_DRAFT_ASSISTANT skill not configured — apply V065 migration", null);
                return;
            }
            String systemPrompt = skill.getSystemPrompt();
            String model = (skill.getModel() != null && !skill.getModel().isBlank())
                    ? skill.getModel() : DEFAULT_MODEL;
            int maxTokens = skill.getMaxTokens() > 0 ? skill.getMaxTokens() : DEFAULT_MAX_TOKENS;

            // Resolve sender context (graceful degradation — never blocks the draft)
            SenderContext senderCtx = resolveSenderContext(em, req.inboundSender);

            // KB retrieval
            List<KnowledgeSearchService.Chunk> alwaysLoaded =
                    knowledgeService.getAlwaysLoadChunks(ALWAYS_LOAD_KBS);

            String searchQuery = req.inboundSubject + "\n" + req.inboundBody
                    + (req.userNotes != null ? "\n" + req.userNotes : "");
            List<KnowledgeSearchService.ScoredChunk> searchResults =
                    knowledgeService.search(searchQuery, SEARCH_KBS);

            List<KnowledgeSearchService.ScoredChunk> selectedChunks =
                    applyRetrievalSplit(searchResults, MAX_FEDERAL, MAX_SSA, MAX_SUMMIT);

            if (selectedChunks.isEmpty()) {
                log.warn("EmailAssistantDraft: KB search returned no results for subject='{}'",
                        req.inboundSubject);
            }

            // Assemble prompt
            String assembledPrompt = buildPrompt(req, senderCtx, alwaysLoaded, selectedChunks);

            // Claude API call
            List<Map<String, String>> messages =
                    List.of(Map.of("role", "user", "content", assembledPrompt));
            String rawResponse = ClaudeApiService.ask(systemPrompt, messages, model, maxTokens);

            // Parse response — retry once on malformed JSON
            DraftResponse draft = parseDraftResponse(rawResponse);
            if (draft == null) {
                log.warn("EmailAssistantDraft: malformed JSON on first attempt, retrying");
                rawResponse = ClaudeApiService.ask(systemPrompt, messages, model, maxTokens);
                draft = parseDraftResponse(rawResponse);
                if (draft == null) {
                    JsonObject err = new JsonObject();
                    err.addProperty("error",   "parse_error");
                    err.addProperty("message", "Model returned malformed JSON after retry");
                    err.addProperty("details", rawResponse);
                    response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                    response.getWriter().write(gson.toJson(err));
                    return;
                }
            }

            // Apply defensive defaults for any null fields
            if (draft.subject  == null)    draft.subject  = "";
            if (draft.body     == null)    draft.body     = "";
            if (draft.draftType == null)   draft.draftType = "FULL_DRAFT";
            if (draft.encryption == null)  draft.encryption = "NONE";
            if (draft.escalation == null)  draft.escalation = "NONE";
            if (draft.completeness == null) {
                draft.completeness = new Completeness();
                draft.completeness.toneCalibrated = "not_calibrated";
            }

            // Build final response with metadata
            JsonObject finalResponse = gson.toJsonTree(draft).getAsJsonObject();

            JsonObject metadata = new JsonObject();
            metadata.addProperty("modelUsed", model);
            JsonArray kbsArr = new JsonArray();
            kbsArr.add("style_voice");
            kbsArr.add("federal_rules");
            kbsArr.add("ssa_business");
            kbsArr.add("summit_supplemental");
            metadata.add("kbsConsulted", kbsArr);
            metadata.addProperty("alwaysLoadChunkCount", alwaysLoaded.size());
            metadata.addProperty("searchChunkCount",     selectedChunks.size());
            metadata.addProperty("amsContextResolved",   senderCtx.resolved);
            finalResponse.add("metadata", metadata);

            response.getWriter().write(gson.toJson(finalResponse));

        } finally {
            em.close();
        }
    }

    // ── Sender context resolution ─────────────────────────────────────────────

    /**
     * Looks up the sender's Person record and extracts name, type, and employer.
     * All failures are logged as warnings and produce an unresolved context —
     * never blocks the draft.
     */
    private SenderContext resolveSenderContext(EntityManager em, String email) {
        SenderContext ctx = new SenderContext();
        try {
            Person person = PersonDAO.getPersonByEmail(em, email);
            if (person == null) {
                log.warn("EmailAssistantDraft: sender '{}' not found in AMS", email);
                return ctx;
            }
            ctx.resolved = true;
            ctx.name     = buildDisplayName(person);

            // Determine type and employer via Employee chain
            Employee employee = person.getEmployee();
            if (employee != null) {
                ctx.senderType = "PARTICIPANT";
                try {
                    Employer employer = employee.getEmployer();
                    if (employer != null) {
                        ctx.employerName = employer.getEmployerName();
                    }
                } catch (Exception e) {
                    log.warn("EmailAssistantDraft: could not resolve employer for '{}': {}",
                            email, e.getMessage());
                }
            } else {
                ctx.senderType = "OTHER";
            }
        } catch (Exception e) {
            log.warn("EmailAssistantDraft: error resolving sender context for '{}': {}",
                    email, e.getMessage());
        }
        return ctx;
    }

    private String buildDisplayName(Person p) {
        StringBuilder sb = new StringBuilder();
        if (p.getFirstName() != null) sb.append(p.getFirstName().trim());
        if (p.getLastName()  != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(p.getLastName().trim());
        }
        return sb.toString().isBlank() ? p.getEmail() : sb.toString();
    }

    // ── KB retrieval helpers ──────────────────────────────────────────────────

    /**
     * Walks the ranked scored-chunk list and caps results per KB key:
     * up to maxFederal from federal_rules, maxSsa from ssa_business,
     * maxSummit from summit_supplemental.
     */
    private List<KnowledgeSearchService.ScoredChunk> applyRetrievalSplit(
            List<KnowledgeSearchService.ScoredChunk> ranked,
            int maxFederal, int maxSsa, int maxSummit) {

        Map<String, Integer> limits = new HashMap<>();
        limits.put("federal_rules",       maxFederal);
        limits.put("ssa_business",        maxSsa);
        limits.put("summit_supplemental", maxSummit);

        Map<String, Integer> counts = new HashMap<>();
        List<KnowledgeSearchService.ScoredChunk> result = new ArrayList<>();

        for (KnowledgeSearchService.ScoredChunk sc : ranked) {
            String kbId = sc.getChunk().getKbId();
            int limit   = limits.getOrDefault(kbId, 0);
            int current = counts.getOrDefault(kbId, 0);
            if (current < limit) {
                result.add(sc);
                counts.put(kbId, current + 1);
            }
        }
        return result;
    }

    // ── Prompt assembly ───────────────────────────────────────────────────────

    private String buildPrompt(DraftRequest req, SenderContext senderCtx,
                                List<KnowledgeSearchService.Chunk> alwaysLoaded,
                                List<KnowledgeSearchService.ScoredChunk> selectedChunks) {
        StringBuilder sb = new StringBuilder();

        // Always-loaded style/voice rules
        sb.append("== ALWAYS-LOADED STYLE & VOICE RULES ==\n\n");
        if (alwaysLoaded.isEmpty()) {
            sb.append("(No style/voice rules loaded.)\n\n");
        } else {
            for (KnowledgeSearchService.Chunk c : alwaysLoaded) {
                sb.append("--- ").append(c.getTitle()).append(" ---\n");
                sb.append(c.getContent()).append("\n\n");
            }
        }

        // KB search results
        sb.append("== KNOWLEDGE BASE SEARCH RESULTS ==\n\n");
        if (selectedChunks.isEmpty()) {
            sb.append("(No relevant knowledge base results found.)\n\n");
        } else {
            for (KnowledgeSearchService.ScoredChunk sc : selectedChunks) {
                KnowledgeSearchService.Chunk c = sc.getChunk();
                sb.append("[").append(c.getKbLabel()).append("] ")
                  .append(c.getTitle()).append("\n");
                sb.append(c.getContent()).append("\n\n");
            }
        }

        // Sender context
        sb.append("== SENDER CONTEXT ==\n\n");
        if (senderCtx.resolved) {
            sb.append("SENDER CONTEXT FROM AMS:\n");
            sb.append("Name: ").append(senderCtx.name).append("\n");
            sb.append("Type: ").append(senderCtx.senderType).append("\n");
            if (senderCtx.employerName != null) {
                sb.append("Employer: ").append(senderCtx.employerName).append("\n");
            }
            if (senderCtx.planYearRange != null) {
                sb.append("Employer plan year: ").append(senderCtx.planYearRange).append("\n");
            }
        } else {
            sb.append("SENDER CONTEXT FROM AMS: ")
              .append("Sender email not found in AMS database. No internal context available.\n");
        }
        sb.append("\n");

        // Inbound email
        sb.append("== INBOUND EMAIL ==\n\n");
        sb.append("From: ").append(req.inboundSender).append("\n");
        sb.append("Date: ")
          .append(req.inboundDate != null ? req.inboundDate : "not provided").append("\n");
        sb.append("Subject: ").append(req.inboundSubject).append("\n\n");
        sb.append(req.inboundBody).append("\n\n");

        // Staff notes
        sb.append("== STAFF NOTES ==\n\n");
        sb.append((req.userNotes != null && !req.userNotes.isBlank())
                ? req.userNotes
                : "(none provided)").append("\n\n");

        // REFINE mode — append previous draft
        if ("REFINE".equals(req.draftMode) && req.previousDraft != null) {
            sb.append("== PREVIOUS DRAFT FOR REFINEMENT ==\n\n");
            sb.append("The staff member previously generated this draft and wants it ")
              .append("refined per the staff notes above:\n\n");
            sb.append(req.previousDraft.toString()).append("\n\n");
        }

        // Task instruction
        sb.append("== TASK ==\n\n");
        sb.append("Draft a reply following all the rules and constraints in the system prompt ")
          .append("and style/voice rules. Output the JSON object exactly per the schema.");

        return sb.toString();
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    /**
     * Parses the Claude response string into a DraftResponse.
     * Strips markdown code fences if present. Returns null if the JSON is malformed.
     */
    private DraftResponse parseDraftResponse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();

        // Strip markdown code fences (```json ... ``` or ``` ... ```)
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence    = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }

        try {
            DraftResponse dr = gson.fromJson(trimmed, DraftResponse.class);
            // Minimal sanity — need at least a non-null subject
            return (dr != null) ? dr : null;
        } catch (Exception e) {
            log.error("EmailAssistantDraft: JSON parse failure — raw response follows:\n{}", raw);
            return null;
        }
    }

    // ── Knowledge service lazy init ───────────────────────────────────────────

    /**
     * Ensures the KnowledgeSearchService is initialized and cached in application scope.
     * Matches ChatAssistant's pattern exactly: double-checked locking on the servlet context,
     * new KnowledgeSearchService() + initialize(emf), attribute stored under "knowledgeService".
     */
    private void ensureKnowledgeService(HttpServletRequest request) {
        if (knowledgeService != null && knowledgeService.isInitialized()) return;

        synchronized (request.getServletContext()) {
            knowledgeService = (KnowledgeSearchService)
                    request.getServletContext().getAttribute("knowledgeService");
            if (knowledgeService != null && knowledgeService.isInitialized()) return;

            try {
                EntityManagerFactory emf =
                        (EntityManagerFactory) request.getServletContext().getAttribute("emf");
                knowledgeService = new KnowledgeSearchService();
                knowledgeService.initialize(emf);
                request.getServletContext().setAttribute("knowledgeService", knowledgeService);
                log.info("KnowledgeSearchService initialized lazily by EmailAssistantDraft");
            } catch (Exception e) {
                log.error("Failed to initialize KnowledgeSearchService", e);
                knowledgeService = null;
            }
        }
    }

    // ── Error response helper ─────────────────────────────────────────────────

    private void sendError(HttpServletResponse response, int status,
                            String error, String message, String details) throws IOException {
        response.setStatus(status);
        JsonObject obj = new JsonObject();
        obj.addProperty("error",   error);
        obj.addProperty("message", message);
        if (details != null) obj.addProperty("details", details);
        response.getWriter().write(gson.toJson(obj));
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

package net.superiorstate.ams.data.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.ChatbotSkillDAO;
import net.superiorstate.ams.data.dao.EmployerInventoryDAO;
import net.superiorstate.ams.data.dao.PersonDAO;
import net.superiorstate.ams.model.dto.inventory.EmployerInventoryDTO;
import net.superiorstate.ams.model.general.ChatbotSkill;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Stateless service that drafts structured email replies grounded in knowledge bases.
 *
 * Encapsulates all drafting logic extracted from EmailAssistantDraft:
 *   - Skill loading (system prompt, model, max_tokens)
 *   - Sender context resolution (Person → Employee → Employer)
 *   - Employer service-scope injection (EmployerContextResolver, primary + fallback)
 *   - KB retrieval and per-KB retrieval split
 *   - Prompt assembly
 *   - Claude API call with one retry on malformed JSON
 *   - Response parsing and default application
 *
 * Callers provide a KnowledgeSearchService instance and an open EntityManager.
 * All exceptions are caught — the caller never sees a thrown exception from draft().
 */
public class EmailDraftService {

    private static final Logger log = LogManager.getLogger(EmailDraftService.class);
    private static final Gson   gson = new Gson();

    private static final String SKILL_NAME     = "EMAIL_DRAFT_ASSISTANT";
    // S20-D — was the retired "claude-sonnet-4-20250514" snapshot. Fallback only: the live
    // chatbot_skill.model DB row for EMAIL_DRAFT_ASSISTANT wins whenever it is non-blank
    // (line ~145), so this constant is reached only if that row is ever cleared.
    private static final String DEFAULT_MODEL  = "claude-sonnet-5";
    private static final int    DEFAULT_MAX_TOKENS = 4096;

    private static final Set<String> ALWAYS_LOAD_KBS = Set.of("style_voice");
    private static final Set<String> SEARCH_KBS =
            Set.of("federal_rules", "ssa_business", "summit_supplemental");

    private static final int MAX_FEDERAL = 6;
    private static final int MAX_SSA     = 3;
    private static final int MAX_SUMMIT  = 2;

    // ── Public API types ──────────────────────────────────────────────────────

    /** Caller-supplied draft parameters. */
    public static class Request {
        public String     senderEmail;      // required — used for sender-context lookup
        public String     inboundSubject;
        public String     inboundBody;
        public String     inboundDate;      // ISO string or whatever the client sends
        public String     userNotes;        // nullable
        public String     draftMode;        // "FRESH" (default) or "REFINE"
        public JsonObject previousDraft;    // nullable; only relevant when draftMode = "REFINE"
        public Long       pspId;            // required — used to load the EMAIL_DRAFT_ASSISTANT skill row
    }

    /** Draft result returned to callers. Gson-serializable to the response envelope. */
    public static class Result {

        // ── Core draft fields (populated from Claude's JSON response) ─────────
        public String subject;
        public String body;
        public String draftType;
        public Completeness completeness;
        public String encryption;
        public String encryptionReason;
        public String escalation;
        public String escalationReason;
        public String escalationQuestionForKevin;

        // ── Metadata (populated by the service after a successful draft) ──────
        public Metadata metadata;

        // ── Internal signals — transient so Gson skips them in serialization ──
        /** Non-null when Claude returned malformed JSON after one retry.
         *  Value is the raw Claude response. Caller should write a 502 with details. */
        public transient String parseError;
        /** Non-null on any other hard failure inside draft().
         *  Caller should write a 500. */
        public transient String error;

        public static class Completeness {
            public boolean planNamed;
            public boolean dateCited;
            public boolean nextStepsStated;
            public String  toneCalibrated;
        }

        public static class Metadata {
            public String       modelUsed;
            public List<String> kbsConsulted;
            public int          alwaysLoadChunkCount;
            public int          searchChunkCount;
            public boolean      amsContextResolved;
        }
    }

    // ── Private context DTO (not exposed to callers) ──────────────────────────

    private static class SenderContext {
        boolean resolved   = false;
        String  name;
        String  senderType = "OTHER";
        String  employerName;
        Long    organizationId;   // Employer.id; null when sender is not a PARTICIPANT
        String  planYearRange;
    }

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Drafts an email reply and returns a populated Result.
     * Never throws — all exceptions are caught and surfaced via Result.error or Result.parseError.
     *
     * @param em              open EntityManager (caller owns lifecycle)
     * @param knowledgeService initialized KnowledgeSearchService (caller ensures not null)
     * @param req             populated Request (senderEmail and pspId are required)
     */
    public static Result draft(EntityManager em,
                               KnowledgeSearchService knowledgeService,
                               Request req) {
        Result result = new Result();
        try {
            // Guard
            if (knowledgeService == null || !knowledgeService.isInitialized()) {
                result.error = "Knowledge service not initialized";
                return result;
            }

            // Load skill (system prompt, model, max_tokens)
            ChatbotSkill skill = ChatbotSkillDAO.getBySkillName(em, SKILL_NAME, req.pspId);
            if (skill == null) {
                result.error = "EMAIL_DRAFT_ASSISTANT skill not configured — apply V065 migration";
                return result;
            }
            String systemPrompt = skill.getSystemPrompt();
            String model = (skill.getModel() != null && !skill.getModel().isBlank())
                    ? skill.getModel() : DEFAULT_MODEL;
            int maxTokens = skill.getMaxTokens() > 0 ? skill.getMaxTokens() : DEFAULT_MAX_TOKENS;

            // Resolve sender context (graceful degradation — never blocks the draft)
            SenderContext senderCtx = resolveSenderContext(em, req.senderEmail);

            // KB retrieval
            List<KnowledgeSearchService.Chunk> alwaysLoaded =
                    knowledgeService.getAlwaysLoadChunks(ALWAYS_LOAD_KBS);

            String searchQuery = (req.inboundSubject != null ? req.inboundSubject : "") + "\n"
                    + (req.inboundBody   != null ? req.inboundBody   : "")
                    + (req.userNotes     != null ? "\n" + req.userNotes : "");
            List<KnowledgeSearchService.ScoredChunk> searchResults =
                    knowledgeService.search(searchQuery, SEARCH_KBS);
            List<KnowledgeSearchService.ScoredChunk> selectedChunks =
                    applyRetrievalSplit(searchResults, MAX_FEDERAL, MAX_SSA, MAX_SUMMIT);

            if (selectedChunks.isEmpty()) {
                log.warn("EmailDraftService: KB search returned no results for subject='{}'",
                        req.inboundSubject);
            }

            // Resolve employer service-scope inventory (best-effort, never blocks)
            String employerContext = null;
            try {
                if (senderCtx.organizationId != null) {
                    EmployerInventoryDTO inv =
                            EmployerInventoryDAO.getByOrganizationId(em, senderCtx.organizationId);
                    if (inv != null) {
                        employerContext = EmployerContextResolver.formatContextBlock(List.of(inv));
                        log.info("EmailDraftService: employer inventory resolved via PARTICIPANT link for '{}'",
                                inv.getEmployerName());
                    }
                }
                if (employerContext == null) {
                    String searchText = (req.inboundSubject != null ? req.inboundSubject : "") + " "
                            + (req.inboundBody   != null ? req.inboundBody   : "")
                            + (req.userNotes     != null ? " " + req.userNotes : "");
                    employerContext = EmployerContextResolver.resolveContext(em, searchText);
                    if (employerContext != null) {
                        log.info("EmailDraftService: employer context resolved via name-matching");
                    }
                }
            } catch (Exception e) {
                log.warn("EmailDraftService: employer context resolution failed; proceeding without it", e);
            }

            // Assemble prompt
            String assembledPrompt = buildPrompt(req, senderCtx, alwaysLoaded, selectedChunks, employerContext);

            // Claude API call
            List<Map<String, String>> messages =
                    List.of(Map.of("role", "user", "content", assembledPrompt));
            String rawResponse = ClaudeApiService.ask(systemPrompt, messages, model, maxTokens);

            // Parse response — retry once on malformed JSON
            Result parsed = parseDraftResponse(rawResponse);
            if (parsed == null) {
                log.warn("EmailDraftService: malformed JSON on first attempt, retrying");
                rawResponse = ClaudeApiService.ask(systemPrompt, messages, model, maxTokens);
                parsed = parseDraftResponse(rawResponse);
                if (parsed == null) {
                    result.parseError = rawResponse;
                    return result;
                }
            }

            // Apply defensive defaults
            if (parsed.subject    == null)  parsed.subject    = "";
            if (parsed.body       == null)  parsed.body       = "";
            if (parsed.draftType  == null)  parsed.draftType  = "FULL_DRAFT";
            if (parsed.encryption == null)  parsed.encryption = "NONE";
            if (parsed.escalation == null)  parsed.escalation = "NONE";
            if (parsed.completeness == null) {
                parsed.completeness = new Result.Completeness();
                parsed.completeness.toneCalibrated = "not_calibrated";
            }

            // Attach metadata
            Result.Metadata meta = new Result.Metadata();
            meta.modelUsed          = model;
            meta.kbsConsulted       = List.of("style_voice", "federal_rules",
                                               "ssa_business", "summit_supplemental");
            meta.alwaysLoadChunkCount = alwaysLoaded.size();
            meta.searchChunkCount     = selectedChunks.size();
            meta.amsContextResolved   = senderCtx.resolved;
            parsed.metadata = meta;

            return parsed;

        } catch (Exception e) {
            log.error("EmailDraftService: unexpected error in draft()", e);
            result.error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return result;
        }
    }

    // ── Private helpers (byte-equivalent to extracted EmailAssistantDraft methods) ──

    private static SenderContext resolveSenderContext(EntityManager em, String email) {
        SenderContext ctx = new SenderContext();
        try {
            Person person = PersonDAO.getPersonByEmail(em, email);
            if (person == null) {
                log.warn("EmailDraftService: sender '{}' not found in AMS", email);
                return ctx;
            }
            ctx.resolved = true;
            ctx.name     = buildDisplayName(person);

            Employee employee = person.getEmployee();
            if (employee != null) {
                ctx.senderType = "PARTICIPANT";
                try {
                    Employer employer = employee.getEmployer();
                    if (employer != null) {
                        ctx.employerName   = employer.getEmployerName();
                        ctx.organizationId = (long) employer.getId();
                    }
                } catch (Exception e) {
                    log.warn("EmailDraftService: could not resolve employer for '{}': {}",
                            email, e.getMessage());
                }
            } else {
                ctx.senderType = "OTHER";
            }
        } catch (Exception e) {
            log.warn("EmailDraftService: error resolving sender context for '{}': {}",
                    email, e.getMessage());
        }
        return ctx;
    }

    private static String buildDisplayName(Person p) {
        StringBuilder sb = new StringBuilder();
        if (p.getFirstName() != null) sb.append(p.getFirstName().trim());
        if (p.getLastName()  != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(p.getLastName().trim());
        }
        return sb.toString().isBlank() ? p.getEmail() : sb.toString();
    }

    private static List<KnowledgeSearchService.ScoredChunk> applyRetrievalSplit(
            List<KnowledgeSearchService.ScoredChunk> ranked,
            int maxFederal, int maxSsa, int maxSummit) {

        Map<String, Integer> limits = new HashMap<>();
        limits.put("federal_rules",       maxFederal);
        limits.put("ssa_business",        maxSsa);
        limits.put("summit_supplemental", maxSummit);

        Map<String, Integer> counts = new HashMap<>();
        List<KnowledgeSearchService.ScoredChunk> result = new ArrayList<>();

        for (KnowledgeSearchService.ScoredChunk sc : ranked) {
            String kbId  = sc.getChunk().getKbId();
            int    limit = limits.getOrDefault(kbId, 0);
            int    current = counts.getOrDefault(kbId, 0);
            if (current < limit) {
                result.add(sc);
                counts.put(kbId, current + 1);
            }
        }
        return result;
    }

    private static String buildPrompt(Request req, SenderContext senderCtx,
                                      List<KnowledgeSearchService.Chunk> alwaysLoaded,
                                      List<KnowledgeSearchService.ScoredChunk> selectedChunks,
                                      String employerContext) {
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

        // Employer service scope (resolved from EmployerInventoryDAO)
        if (employerContext != null && !employerContext.isBlank()) {
            sb.append(employerContext);
        }

        // Inbound email
        sb.append("== INBOUND EMAIL ==\n\n");
        sb.append("From: ").append(req.senderEmail).append("\n");
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

    /**
     * Parses a Claude response into a Result, stripping markdown code fences if present.
     * Returns null if the JSON is missing or malformed.
     */
    private static Result parseDraftResponse(String raw) {
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
            Result r = gson.fromJson(trimmed, Result.class);
            return r;
        } catch (Exception e) {
            log.error("EmailDraftService: JSON parse failure — raw response follows:\n{}", raw);
            return null;
        }
    }
}

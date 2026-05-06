package net.superiorstate.ams.controller.assistant;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.service.ClaudeApiService;
import net.superiorstate.ams.data.service.KnowledgeSearchService;
import net.superiorstate.ams.data.service.KnowledgeSearchService.ScoredChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * AJAX endpoint for the AI Proposal Page Builder panel in proposalSettings.jsp.
 * Uses a specialized system prompt for generating styled HTML proposal page blocks
 * and only searches the proposal_page_builder knowledge base.
 *
 * Maintains conversation history in the session for multi-turn interaction.
 * Uses Claude Sonnet for higher-quality HTML/CSS generation.
 *
 * URL: /ProposalAiBuilder
 */
@WebServlet("/ProposalAiBuilder")
public class ProposalAiBuilder extends HttpServlet {

    private static final Logger log = LogManager.getLogger(ProposalAiBuilder.class);
    private static final Gson gson = new Gson();
    private static final int MAX_HISTORY_TURNS = 10;
    private static final String SESSION_KEY = "proposalAiBuilderHistory";

    /** Use Sonnet for complex HTML/CSS generation */
    private static final String MODEL = "claude-sonnet-4-20250514";
    private static final int MAX_TOKENS = 4096;

    private static final Set<String> TARGET_KB = Set.of("proposal_page_builder");

    private static final String SYSTEM_PROMPT =
            "You are an AI assistant that creates styled HTML content blocks for a benefits administration proposal system.\n\n" +
            "You generate self-contained HTML fragments that render inside a proposal viewer. The proposal viewer loads Bootstrap 5 CSS, has a light gray (#f8f9fa) background, and a 900px max-width container. Your output is injected unescaped into a <div class=\"proposal-section\">.\n\n" +
            "CRITICAL STRUCTURAL RULES:\n" +
            "1. Output is an HTML FRAGMENT — no <html>, <head>, <body> wrappers\n" +
            "2. Start with a <style> block containing ALL CSS, scoped under a unique class prefix (e.g., .about1, .cobra2)\n" +
            "3. After the <style> block, the HTML MUST be wrapped in a <div class=\"PREFIX\"> element using your prefix class.\n" +
            "   This wrapper is REQUIRED — without it, none of your CSS selectors will match!\n" +
            "   Correct:  <style>.about1 .card-inset { ... }</style>  <div class=\"about1\"><div class=\"card-inset\">...</div></div>\n" +
            "   WRONG:    <style>.about1 .card-inset { ... }</style>  <div class=\"card-inset\">...</div>  (missing wrapper!)\n" +
            "4. Use @import inside <style> for any Google Fonts — never <link> tags\n" +
            "5. Every CSS selector must start with your prefix class to avoid collisions\n" +
            "6. No <script> tags, onclick/onload handlers, or javascript: links — they are stripped by the server\n" +
            "7. No Bootstrap class names — the viewer loads Bootstrap 5, so avoid .card, .btn, .container, .row, .col, etc.\n" +
            "   Use your own class names like .info-card, .tile, .panel, .block, etc.\n\n" +
            "THE CARD-INSET PATTERN (required HTML structure):\n" +
            "<style>\n" +
            ".PREFIX { --s: 1; /* CSS variables here */ }\n" +
            ".PREFIX .card-inset { border-radius: 12px; min-height: calc(11in - 1.5in); display: flex; flex-direction: column; }\n" +
            "/* all other rules scoped under .PREFIX */\n" +
            "</style>\n" +
            "<div class=\"PREFIX\">\n" +
            "  <div class=\"card-inset\">\n" +
            "    <!-- page content here, one child gets flex:1 -->\n" +
            "  </div>\n" +
            "</div>\n\n" +
            "Include @media print { .PREFIX .card-inset { min-height:auto; } } and a mobile @media (max-width:700px) rule.\n\n" +
            "SCALE FACTOR:\n" +
            "Define --s:1 on your prefix class. Use calc(value * var(--s)) for all font-sizes, padding, margins, gaps. This lets the user scale the entire page by changing one number.\n\n" +
            "STYLE ADAPTATION (VERY IMPORTANT):\n" +
            "When the user describes a style, provides reference material, mentions colors, or shares existing content:\n" +
            "- ANALYZE the visual language they want BEFORE writing CSS\n" +
            "- ADAPT the card-inset's background, colors, fonts, and accent treatment to match\n" +
            "- The default dark navy (#1B2A4A) palette is a FALLBACK, not mandatory\n" +
            "- If they want light/white, use white. Corporate blue, use blue. Warm/organic, adjust accordingly.\n" +
            "- When they provide existing content or text to include, preserve its meaning and structure while styling it to fit the card-inset pattern\n\n" +
            "MERGE TOKENS:\n" +
            "These are replaced server-side with real values:\n" +
            "- {{PROSPECT_NAME}} — company/prospect name\n" +
            "- {{AGENT_NAME}} — sales agent full name\n" +
            "- {{AGENT_EMAIL}} — agent email\n" +
            "- {{AGENCY_NAME}} — agency name\n" +
            "- {{PSP_NAME}} — PSP company name\n" +
            "- {{DATE_CREATED}} — proposal creation date\n" +
            "- {{PRIMARY_COLOR}} — PSP brand primary color (hex)\n" +
            "- {{ACCENT_COLOR}} — PSP brand accent color (hex)\n" +
            "- {{APPLY_BUTTON}} — renders a styled Apply Now button\n" +
            "- {{PROPOSAL_ID}} — proposal ID\n\n" +
            "Using {{PRIMARY_COLOR}} and {{ACCENT_COLOR}} in CSS vars (e.g., --primary: {{PRIMARY_COLOR}}) makes pages auto-adapt to any PSP's branding.\n\n" +
            "WORKFLOW:\n" +
            "1. Ask what the page is about and what section type (TITLE, CLOSING, or CUSTOM)\n" +
            "2. Ask about desired style/colors (or analyze provided reference material)\n" +
            "3. Ask what content to include\n" +
            "4. Generate the HTML block in a code block (triple backticks)\n" +
            "5. Explain what you built and how to customize it\n\n" +
            "Always output the final HTML in a single code block. The user will click \"Insert into Editor\" to paste it into the section textarea.";

    private KnowledgeSearchService knowledgeService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Authentication check
        HttpSession session = request.getSession(false);
        if (session == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }
        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        if (local == null || !local.isAuthenticated() || !local.isPspAdmin()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authorized");
            return;
        }

        // Parse request body
        String question;
        String action = null;
        String sectionId = null;
        String sectionType = null;
        try {
            JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
            question = body.has("question") ? body.get("question").getAsString().trim() : "";
            if (body.has("action")) action = body.get("action").getAsString();
            if (body.has("sectionId")) sectionId = body.get("sectionId").getAsString();
            if (body.has("sectionType")) sectionType = body.get("sectionType").getAsString();
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request format");
            return;
        }

        // Handle reset action
        if ("reset".equals(action)) {
            session.removeAttribute(SESSION_KEY);
            JsonObject result = new JsonObject();
            result.addProperty("answer", "Conversation cleared. Describe the proposal page you want to build.");
            response.getWriter().write(gson.toJson(result));
            return;
        }

        if (question.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Please enter a message");
            return;
        }

        // Initialize knowledge service
        ensureKnowledgeService(request);

        // Get or create conversation history
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute(SESSION_KEY);
        if (history == null) {
            history = new ArrayList<>();
        }

        // Search KB for relevant context
        String kbContext = "";
        if (knowledgeService != null && knowledgeService.isInitialized()) {
            List<ScoredChunk> results = knowledgeService.search(question, TARGET_KB);
            if (!results.isEmpty()) {
                kbContext = knowledgeService.buildContext(results);
            }
        }

        // Add section context to the question if provided
        String enrichedQuestion = question;
        if (sectionType != null && !sectionType.isEmpty()) {
            enrichedQuestion = "[Section type: " + sectionType + "] " + question;
        }

        // Build messages array for multi-turn
        List<Map<String, String>> messages = new ArrayList<>();

        if (!kbContext.isEmpty() && history.isEmpty()) {
            // First turn: include KB context with the question
            Map<String, String> firstMsg = new HashMap<>();
            firstMsg.put("role", "user");
            firstMsg.put("content", kbContext + "\n\n=== USER REQUEST ===\n" + enrichedQuestion);
            messages.add(firstMsg);
        } else {
            // Add conversation history
            messages.addAll(history);

            // Add current question (with KB context if new topics come up)
            Map<String, String> currentMsg = new HashMap<>();
            currentMsg.put("role", "user");
            if (!kbContext.isEmpty()) {
                currentMsg.put("content", "Additional reference:\n" + kbContext + "\n\n" + enrichedQuestion);
            } else {
                currentMsg.put("content", enrichedQuestion);
            }
            messages.add(currentMsg);
        }

        // Call Claude with multi-turn messages — use Sonnet for higher-quality HTML generation
        String answer = ClaudeApiService.ask(SYSTEM_PROMPT, messages, MODEL, MAX_TOKENS);

        // Update conversation history
        Map<String, String> userEntry = new HashMap<>();
        userEntry.put("role", "user");
        userEntry.put("content", question);
        history.add(userEntry);

        Map<String, String> assistantEntry = new HashMap<>();
        assistantEntry.put("role", "assistant");
        assistantEntry.put("content", answer);
        history.add(assistantEntry);

        // Truncate if too many turns
        while (history.size() > MAX_HISTORY_TURNS * 2) {
            history.remove(0);
            history.remove(0);
        }

        session.setAttribute(SESSION_KEY, history);

        // Return response
        JsonObject result = new JsonObject();
        result.addProperty("answer", answer);
        response.getWriter().write(gson.toJson(result));
    }

    private void ensureKnowledgeService(HttpServletRequest request) {
        if (knowledgeService != null && knowledgeService.isInitialized()) return;

        synchronized (request.getServletContext()) {
            knowledgeService = (KnowledgeSearchService) request.getServletContext().getAttribute("knowledgeService");
            if (knowledgeService != null && knowledgeService.isInitialized()) return;

            try {
                EntityManagerFactory emf =
                        (EntityManagerFactory) request.getServletContext().getAttribute("emf");
                knowledgeService = new KnowledgeSearchService();
                knowledgeService.initialize(emf);
                request.getServletContext().setAttribute("knowledgeService", knowledgeService);
                log.info("KnowledgeSearchService initialized by ProposalAiBuilder");
            } catch (Exception e) {
                log.error("Failed to initialize KnowledgeSearchService", e);
                knowledgeService = null;
            }
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        response.getWriter().write(gson.toJson(error));
    }
}

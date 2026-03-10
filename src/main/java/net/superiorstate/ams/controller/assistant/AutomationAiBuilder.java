package net.superiorstate.ams.controller.assistant;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
 * AJAX endpoint for the AI Email Builder panel in taskManager25.jsp.
 * Uses a specialized system prompt for automation email template building
 * and only searches the automation_email_builder knowledge base.
 *
 * Maintains conversation history in the session for multi-turn interaction.
 *
 * URL: /AutomationAiBuilder
 */
@WebServlet("/AutomationAiBuilder")
public class AutomationAiBuilder extends HttpServlet {

    private static final Logger log = LogManager.getLogger(AutomationAiBuilder.class);
    private static final Gson gson = new Gson();
    private static final int MAX_HISTORY_TURNS = 10;
    private static final String SESSION_KEY = "aiBuilderHistory";

    private static final Set<String> TARGET_KB = Set.of("automation_email_builder");

    private static final String SYSTEM_PROMPT =
            "You are an AI assistant that helps build automation email templates for a benefits administration system.\n\n" +
            "You create template code using a specific tag language. Here are the available tags:\n\n" +
            "INPUT TAGS (create fields the user fills in at send time):\n" +
            "- <ii>Label</ii> — text input field, label between tags\n" +
            "- <ii><l>Label</ii> — link/URL input, auto-wrapped in clickable <a> tag\n" +
            "- <ii><cc></ii> — CC recipient field, semicolon-separated, one per template\n\n" +
            "VARIABLE TAGS (auto-insert system data):\n" +
            "- <<#erName>> — employer/prospect name. Auto-fills when available (Renewal, Setup, Ticket with employee, Opportunity). If the system cannot resolve the name, it automatically becomes an input field so the user can type it manually. Safe to use in any template.\n" +
            "- <<#activityType>> — activity type (Renewal, Setup, Ticket, etc.)\n" +
            "- <<sig>> — sender's email signature block\n" +
            "- <<close>> — auto-closes the activity after sending (invisible flag)\n\n" +
            "SUBJECT TAG:\n" +
            "- <sbj>Subject Text</sbj> — email subject line (first line of template, variable tags OK inside, NO input tags)\n\n" +
            "TEMPLATE STRUCTURE ORDER:\n" +
            "1. <sbj>...</sbj> (optional, first line)\n" +
            "2. Email body with input tags and variable tags\n" +
            "3. <<sig>> (near end)\n" +
            "4. <ii><cc></ii> (if needed, after sig)\n" +
            "5. <<close>> (last, if desired)\n\n" +
            "RULES:\n" +
            "- Always output the final template code in a code block (triple backticks)\n" +
            "- Ask clarifying questions if the user's request is ambiguous\n" +
            "- When generating, explain what each part does in plain English BEFORE showing the code\n" +
            "- One <<sig>> per template, one <ii><cc></ii> per template\n" +
            "- Do NOT put <ii> input tags inside <sbj> subject tags\n" +
            "- <<close>> produces no visible output — it's a behavioral flag\n\n" +
            "When the user describes what they want, walk through these questions (but skip ones that are obvious from their description):\n" +
            "1. What is the email about?\n" +
            "2. Who gets it? Need to CC anyone?\n" +
            "3. What information changes each time? (these become input tags)\n" +
            "4. Should the activity auto-close after sending?\n" +
            "5. Include your signature?\n\n" +
            "Then generate the template and explain it.";

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
        try {
            JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
            question = body.has("question") ? body.get("question").getAsString().trim() : "";
            if (body.has("action")) action = body.get("action").getAsString();
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request format");
            return;
        }

        // Handle reset action
        if ("reset".equals(action)) {
            session.removeAttribute(SESSION_KEY);
            JsonObject result = new JsonObject();
            result.addProperty("answer", "Conversation cleared. Describe the email you want to build.");
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

        // Build messages array for multi-turn
        List<Map<String, String>> messages = new ArrayList<>();

        // First message includes KB context if available
        if (!kbContext.isEmpty() && history.isEmpty()) {
            // First turn: include KB context with the question
            Map<String, String> firstMsg = new HashMap<>();
            firstMsg.put("role", "user");
            firstMsg.put("content", kbContext + "\n\n=== USER REQUEST ===\n" + question);
            messages.add(firstMsg);
        } else {
            // Add conversation history
            messages.addAll(history);

            // Add current question (with KB context if new topics come up)
            Map<String, String> currentMsg = new HashMap<>();
            currentMsg.put("role", "user");
            if (!kbContext.isEmpty()) {
                currentMsg.put("content", "Additional reference:\n" + kbContext + "\n\n" + question);
            } else {
                currentMsg.put("content", question);
            }
            messages.add(currentMsg);
        }

        // Call Claude with multi-turn messages
        String answer = ClaudeApiService.ask(SYSTEM_PROMPT, messages);

        // Update conversation history
        Map<String, String> userEntry = new HashMap<>();
        userEntry.put("role", "user");
        userEntry.put("content", question);
        history.add(userEntry);

        Map<String, String> assistantEntry = new HashMap<>();
        assistantEntry.put("role", "assistant");
        assistantEntry.put("content", answer);
        history.add(assistantEntry);

        // Truncate if too many turns (each turn = 2 entries)
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
                knowledgeService = new KnowledgeSearchService();
                knowledgeService.initialize();
                request.getServletContext().setAttribute("knowledgeService", knowledgeService);
                log.info("KnowledgeSearchService initialized by AutomationAiBuilder");
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

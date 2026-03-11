package net.superiorstate.ams.controller.assistant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.ChatbotSkillDAO;
import net.superiorstate.ams.data.dao.TicketKnowledgeDAO;
import net.superiorstate.ams.data.service.ClaudeApiService;
import net.superiorstate.ams.data.service.KnowledgeSearchService;
import net.superiorstate.ams.data.service.KnowledgeSearchService.ScoredChunk;
import net.superiorstate.ams.model.general.ChatbotSkill;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * AJAX endpoint for the AI Knowledge Assistant chatbox.
 * Accepts both JSON text questions and multipart file uploads.
 * Routes to matched skills when available, falls through to KB search otherwise.
 *
 * URL: /ChatAssistant
 */
@WebServlet("/ChatAssistant")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024)
public class ChatAssistant extends HttpServlet {

    private static final Logger log = LogManager.getLogger(ChatAssistant.class);
    private static final Gson gson = new Gson();

    private static final String SYSTEM_PROMPT =
            "You are an AI assistant for employees of a benefits administration company. " +
                    "Answer questions using ONLY the provided context from our knowledge bases. " +
                    "If the context doesn't contain enough information to answer, say so clearly. " +
                    "When citing information, mention the source document name. " +
                    "If a training video link is included in the context, include it in your response. " +
                    "Keep answers concise and practical. " +
                    "Do not make up information that isn't in the provided context.";

    /** Application-scoped KnowledgeSearchService — initialized once on first request */
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
        if (local == null || !local.isAuthenticated()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        // Determine request type: multipart (file upload) vs JSON (text only)
        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.startsWith("multipart/");

        String question;
        byte[] fileBytes = null;
        String fileMimeType = null;
        String fileName = null;

        if (isMultipart) {
            try {
                Part filePart = request.getPart("chatFile");
                if (filePart != null && filePart.getSize() > 0) {
                    fileBytes = filePart.getInputStream().readAllBytes();
                    fileMimeType = filePart.getContentType();
                    fileName = filePart.getSubmittedFileName();
                    if (fileName != null) {
                        fileName = java.nio.file.Paths.get(fileName).getFileName().toString();
                    }
                }
                Part textPart = request.getPart("question");
                question = textPart != null ? new String(textPart.getInputStream().readAllBytes()).trim() : "";
            } catch (Exception e) {
                log.error("Error reading multipart upload", e);
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Error reading upload");
                return;
            }
        } else {
            // Existing JSON path
            try {
                JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
                question = body.has("question") ? body.get("question").getAsString().trim() : "";
            } catch (Exception e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request format");
                return;
            }
        }

        if (question.isEmpty() && fileBytes == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Please enter a question or upload a file");
            return;
        }

        // Initialize knowledge service on first use
        ensureKnowledgeService(request);

        // Skill matching + response
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String answer;

        try {
            // Load active skills for this PSP
            Long pspId = local.getCurrentPerson().getPsp().getId();
            List<ChatbotSkill> activeSkills = new ArrayList<>(ChatbotSkillDAO.getActiveSkills(em, pspId));

            // Filter by role: remove admin-only skills if user is not admin
            if (!local.isPspAdmin()) {
                activeSkills.removeIf(ChatbotSkill::isAdminOnly);
            }

            // Try to match a skill
            ChatbotSkill matchedSkill = ChatbotSkillDAO.findMatchingSkill(activeSkills, question, fileMimeType);

            if (matchedSkill != null) {
                answer = executeSkill(matchedSkill, question, fileBytes, fileMimeType, fileName);
            } else if (fileBytes != null) {
                // File uploaded but no skill matched
                answer = "No skill is configured to analyze this file type. Please contact an administrator.";
            } else {
                // Default KB search path (existing behavior)
                answer = executeKBSearch(em, local, question);
            }
        } finally {
            em.close();
        }

        // Send response
        JsonObject result = new JsonObject();
        result.addProperty("answer", answer);
        response.getWriter().write(gson.toJson(result));
    }

    /**
     * Executes a matched chatbot skill. If the skill accepts files and a file is attached,
     * sends the file as a base64 document block to Claude alongside the text question.
     */
    private String executeSkill(ChatbotSkill skill, String question, byte[] fileBytes,
                                String fileMimeType, String fileName) {
        if (skill.isAcceptsFileUpload() && fileBytes != null) {
            String base64 = Base64.getEncoder().encodeToString(fileBytes);

            JsonArray contentBlocks = new JsonArray();

            // Document block
            JsonObject docBlock = new JsonObject();
            docBlock.addProperty("type", "document");
            JsonObject source = new JsonObject();
            source.addProperty("type", "base64");
            source.addProperty("media_type", fileMimeType);
            source.addProperty("data", base64);
            docBlock.add("source", source);
            contentBlocks.add(docBlock);

            // Text block
            JsonObject textBlock = new JsonObject();
            textBlock.addProperty("type", "text");
            textBlock.addProperty("text",
                    (question != null && !question.isBlank()) ? question : "Please analyze this document.");
            contentBlocks.add(textBlock);

            log.info("Executing skill '{}' with file: {} ({} bytes)",
                    skill.getSkillName(), fileName, fileBytes.length);

            return ClaudeApiService.askWithContent(
                    skill.getSystemPrompt(), contentBlocks, skill.getModel(), skill.getMaxTokens());
        } else {
            // Text-only skill (specialized system prompt, no file)
            log.info("Executing skill '{}' (text-only)", skill.getSkillName());
            return ClaudeApiService.ask(null, skill.getSystemPrompt(), question);
        }
    }

    /**
     * Existing KB search behavior — searches knowledge bases and resolved tickets,
     * builds context, and sends to Claude with the default system prompt.
     */
    private String executeKBSearch(EntityManager em, AmsDataLocal local, String question) {
        if (knowledgeService == null || !knowledgeService.isInitialized()) {
            return "Knowledge base is not available. Please try again later.";
        }

        boolean isAdmin = local.isPspAdmin();
        Set<String> eligibleKBs = knowledgeService.getEligibleKBs(isAdmin);
        List<ScoredChunk> results = knowledgeService.search(question, eligibleKBs);
        String context = knowledgeService.buildContext(results);

        // Search resolved tickets
        String[] words = question.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        List<String> searchTerms = new ArrayList<>();
        for (String w : words) {
            if (w.length() > 2) searchTerms.add(w);
        }
        List<String> ticketResults = TicketKnowledgeDAO.searchResolvedTickets(em, searchTerms, 5);

        StringBuilder fullContext = new StringBuilder(context);
        if (!ticketResults.isEmpty()) {
            fullContext.append("\n=== RESOLVED TICKET HISTORY ===\n\n");
            for (String t : ticketResults) {
                fullContext.append(t).append("\n");
            }
        }

        String userMessage = fullContext.toString() + "\n\n=== QUESTION ===\n" + question;
        return ClaudeApiService.ask(null, SYSTEM_PROMPT, userMessage);
    }

    /**
     * Initializes the KnowledgeSearchService once and stores it in application scope.
     * Thread-safe via synchronization on the servlet context.
     */
    private void ensureKnowledgeService(HttpServletRequest request) {
        if (knowledgeService != null && knowledgeService.isInitialized()) return;

        synchronized (request.getServletContext()) {
            knowledgeService = (KnowledgeSearchService) request.getServletContext().getAttribute("knowledgeService");
            if (knowledgeService != null && knowledgeService.isInitialized()) return;

            try {
                knowledgeService = new KnowledgeSearchService();
                knowledgeService.initialize();
                request.getServletContext().setAttribute("knowledgeService", knowledgeService);
                log.info("KnowledgeSearchService initialized and stored in application scope");
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

package net.superiorstate.ams.controller.assistant;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.TicketKnowledgeDAO;
import net.superiorstate.ams.data.service.ClaudeApiService;
import net.superiorstate.ams.data.service.KnowledgeSearchService;
import net.superiorstate.ams.data.service.KnowledgeSearchService.ScoredChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * AJAX endpoint for the AI Knowledge Assistant chatbox.
 * Accepts a question via POST, searches relevant knowledge bases,
 * sends context + question to Claude, and returns the response as JSON.
 *
 * URL: /ChatAssistant
 */
@WebServlet("/ChatAssistant")
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

        // Parse question from request body
        String question;
        try {
            JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
            question = body.has("question") ? body.get("question").getAsString().trim() : "";
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request format");
            return;
        }

        if (question.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Please enter a question");
            return;
        }

        // Initialize knowledge service on first use
        ensureKnowledgeService(request);
        if (knowledgeService == null || !knowledgeService.isInitialized()) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Knowledge base is not available. Please try again later.");
            return;
        }

        // Determine eligible KBs based on user role
        boolean isAdmin = local.isPspAdmin();
        Set<String> eligibleKBs = knowledgeService.getEligibleKBs(isAdmin);

        // Search for relevant chunks
        List<ScoredChunk> results = knowledgeService.search(question, eligibleKBs);
        String context = knowledgeService.buildContext(results);

        // Search resolved tickets from database
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String answer;
        try {
            // Build ticket search terms from the question
            String[] words = question.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
            List<String> searchTerms = new java.util.ArrayList<>();
            for (String w : words) {
                if (w.length() > 2) searchTerms.add(w);
            }
            List<String> ticketResults = TicketKnowledgeDAO.searchResolvedTickets(em, searchTerms, 5);

            // Combine KB context + ticket context
            StringBuilder fullContext = new StringBuilder(context);
            if (!ticketResults.isEmpty()) {
                fullContext.append("\n=== RESOLVED TICKET HISTORY ===\n\n");
                for (String t : ticketResults) {
                    fullContext.append(t).append("\n");
                }
            }

            // Build the user message with combined context
            String userMessage = fullContext.toString() + "\n\n=== QUESTION ===\n" + question;

            answer = ClaudeApiService.ask(em, SYSTEM_PROMPT, userMessage);
        } finally {
            em.close();
        }

        // Send response
        JsonObject result = new JsonObject();
        result.addProperty("answer", answer);
        result.addProperty("chunksUsed", results.size());
        response.getWriter().write(gson.toJson(result));
    }

    /**
     * Initializes the KnowledgeSearchService once and stores it in application scope.
     * Thread-safe via synchronization on the servlet context.
     */
    private void ensureKnowledgeService(HttpServletRequest request) {
        if (knowledgeService != null && knowledgeService.isInitialized()) return;

        synchronized (request.getServletContext()) {
            // Double-check after acquiring lock
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

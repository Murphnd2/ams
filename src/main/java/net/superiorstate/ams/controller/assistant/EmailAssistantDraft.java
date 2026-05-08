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
import net.superiorstate.ams.data.service.EmailDraftService;
import net.superiorstate.ams.data.service.KnowledgeSearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * AJAX endpoint that drafts structured email replies grounded in knowledge bases.
 *
 * Delegates all drafting logic to EmailDraftService. This servlet handles only:
 * session auth, HTTP body parsing, knowledge-service lazy-init, and response writing.
 *
 * PSP Admin only. POST only.
 *
 * URL: /EmailAssistantDraft
 */
@WebServlet(name = "EmailAssistantDraft", value = "/EmailAssistantDraft")
public class EmailAssistantDraft extends HttpServlet {

    private static final Logger log = LogManager.getLogger(EmailAssistantDraft.class);
    private static final Gson   gson = new Gson();

    private KnowledgeSearchService knowledgeService;

    // ── HTTP body shape (Gson-deserializable from client JSON) ────────────────

    private static class DraftRequest {
        String     inboundSubject;
        String     inboundBody;
        String     inboundSender;
        String     inboundDate;
        String     userNotes;
        String     draftMode;        // FRESH | REFINE
        JsonObject previousDraft;    // for REFINE mode
    }

    // ── Servlet handler ───────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Auth — PSP Admin only
        HttpSession  session = request.getSession(false);
        AmsDataLocal local   = (session != null)
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

        // Knowledge service — lazy-init, shared in application scope
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
            EmailDraftService.Request serviceReq = new EmailDraftService.Request();
            serviceReq.senderEmail    = req.inboundSender;
            serviceReq.inboundSubject = req.inboundSubject;
            serviceReq.inboundBody    = req.inboundBody;
            serviceReq.inboundDate    = req.inboundDate;
            serviceReq.userNotes      = req.userNotes;
            serviceReq.draftMode      = req.draftMode;
            serviceReq.previousDraft  = req.previousDraft;
            serviceReq.pspId          = local.getCurrentPerson().getPsp().getId();

            EmailDraftService.Result result = EmailDraftService.draft(em, knowledgeService, serviceReq);

            if (result.parseError != null) {
                JsonObject err = new JsonObject();
                err.addProperty("error",   "parse_error");
                err.addProperty("message", "Model returned malformed JSON after retry");
                err.addProperty("details", result.parseError);
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                response.getWriter().write(gson.toJson(err));
                return;
            }

            if (result.error != null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          "draft_error", result.error, null);
                return;
            }

            response.getWriter().write(gson.toJson(result));

        } finally {
            em.close();
        }
    }

    // ── Knowledge service lazy init ───────────────────────────────────────────

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

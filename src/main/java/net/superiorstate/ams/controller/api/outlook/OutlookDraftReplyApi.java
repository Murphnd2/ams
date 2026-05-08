package net.superiorstate.ams.controller.api.outlook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.service.EmailDraftService;
import net.superiorstate.ams.data.service.KnowledgeSearchService;
import net.superiorstate.ams.model.general.Person;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;

/**
 * Outlook add-in endpoint that drafts a structured email reply.
 *
 * The add-in POSTs the inbound email details; this endpoint validates the Bearer
 * token, builds an EmailDraftService.Request, delegates to the service, and
 * returns the same JSON shape as EmailAssistantDraft.
 *
 * Auth: Authorization: Bearer {outlook_user_link.api_token}
 * URL: POST /api/v1/outlook/draft-reply
 */
@WebServlet(name = "OutlookDraftReplyApi", urlPatterns = "/api/v1/outlook/draft-reply")
public class OutlookDraftReplyApi extends HttpServlet {

    private static final Logger log = LogManager.getLogger(OutlookDraftReplyApi.class);
    private static final Gson   gson = new Gson();

    private KnowledgeSearchService knowledgeService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        EntityManagerFactory emf =
                (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Bearer-token auth
            Person person = OutlookApiHelper.validateOutlookToken(em, request);
            if (person == null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or missing Bearer token");
                return;
            }

            // Parse JSON body
            JsonObject body;
            try {
                Reader reader = request.getReader();
                body = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid JSON body");
                return;
            }

            String senderEmail    = jsonString(body, "senderEmail");
            String inboundSubject = jsonString(body, "inboundSubject");
            String inboundBody    = jsonString(body, "inboundBody");

            if (isBlank(senderEmail) || isBlank(inboundSubject) || isBlank(inboundBody)) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "senderEmail, inboundSubject, and inboundBody are required");
                return;
            }

            log.info("OutlookDraftReplyApi: person={}, sender={}, subject.len={}",
                    person.getId(), senderEmail, inboundSubject.length());

            // Knowledge service — lazy-init, shared in application scope
            ensureKnowledgeService(request, emf);
            if (knowledgeService == null || !knowledgeService.isInitialized()) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Knowledge service not initialized");
                return;
            }

            // Build service request
            EmailDraftService.Request serviceReq = new EmailDraftService.Request();
            serviceReq.senderEmail    = senderEmail;
            serviceReq.inboundSubject = inboundSubject;
            serviceReq.inboundBody    = inboundBody;
            serviceReq.inboundDate    = jsonString(body, "inboundDate");
            serviceReq.userNotes      = jsonString(body, "userNotes");
            serviceReq.draftMode      = body.has("draftMode") && !body.get("draftMode").isJsonNull()
                    ? body.get("draftMode").getAsString() : "FRESH";
            serviceReq.previousDraft  = body.has("previousDraft") && body.get("previousDraft").isJsonObject()
                    ? body.get("previousDraft").getAsJsonObject() : null;
            serviceReq.pspId          = person.getPsp().getId();

            EmailDraftService.Result result = EmailDraftService.draft(em, knowledgeService, serviceReq);

            if (result.parseError != null) {
                JsonObject err = new JsonObject();
                err.addProperty("error",   "parse_error");
                err.addProperty("message", "Model returned malformed JSON after retry");
                err.addProperty("details", result.parseError);
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(gson.toJson(err));
                return;
            }

            if (result.error != null) {
                OutlookApiHelper.sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        result.error);
                return;
            }

            OutlookApiHelper.sendJson(response, HttpServletResponse.SC_OK, gson.toJson(result));

        } finally {
            em.close();
        }
    }

    // ── Knowledge service lazy init ───────────────────────────────────────────

    private void ensureKnowledgeService(HttpServletRequest request, EntityManagerFactory emf) {
        if (knowledgeService != null && knowledgeService.isInitialized()) return;

        synchronized (request.getServletContext()) {
            knowledgeService = (KnowledgeSearchService)
                    request.getServletContext().getAttribute("knowledgeService");
            if (knowledgeService != null && knowledgeService.isInitialized()) return;

            try {
                knowledgeService = new KnowledgeSearchService();
                knowledgeService.initialize(emf);
                request.getServletContext().setAttribute("knowledgeService", knowledgeService);
                log.info("KnowledgeSearchService initialized lazily by OutlookDraftReplyApi");
            } catch (Exception e) {
                log.error("Failed to initialize KnowledgeSearchService", e);
                knowledgeService = null;
            }
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private String jsonString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

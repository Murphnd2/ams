package net.superiorstate.ams.controller.api;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.Constant;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Receives registration requests from the master management node.
 * POST /api/v1/system/register
 * Public endpoint (no token required — this IS the handshake).
 *
 * Master sends: { masterUrl, masterName, callbackToken }
 * This installation stores the callbackToken as MASTER_API_TOKEN_INBOUND,
 * generates its own confirmToken, and returns it so the master can call our APIs.
 */
@WebServlet("/api/v1/system/register")
public class SystemRegisterApi extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        Map<String, String> body = ApiClient.readJsonBody(request);
        String masterUrl = body.get("masterUrl");
        String masterName = body.get("masterName");
        String callbackToken = body.get("callbackToken");
        String deploymentKey = body.get("deploymentKey");

        if (callbackToken == null || callbackToken.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"callbackToken is required\"}");
            return;
        }
        if (masterUrl == null) masterUrl = "";

        // Validate deployment key for security
        String expectedKey = net.superiorstate.ams.AppConfig.get("DEPLOYMENT_KEY");
        if (expectedKey == null || expectedKey.isBlank() || !expectedKey.equals(deploymentKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Invalid deployment key\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"System not initialized\"}");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Store/update master token as a constant
            Constant masterToken = em.find(Constant.class, "MASTER_API_TOKEN_INBOUND");
            if (masterToken == null) {
                masterToken = new Constant();
                masterToken.setName("MASTER_API_TOKEN_INBOUND");
                masterToken.setValue(callbackToken);
                masterToken.setNote("Token for master management node authentication");
                em.persist(masterToken);
            } else {
                masterToken.setValue(callbackToken);
                em.merge(masterToken);
            }

            // Store master URL
            Constant masterUrlConst = em.find(Constant.class, "MASTER_URL");
            if (masterUrlConst == null) {
                masterUrlConst = new Constant();
                masterUrlConst.setName("MASTER_URL");
                masterUrlConst.setValue(masterUrl);
                masterUrlConst.setNote("URL of the master management node");
                em.persist(masterUrlConst);
            } else {
                masterUrlConst.setValue(masterUrl);
                em.merge(masterUrlConst);
            }

            em.getTransaction().commit();

            // Generate a confirm token for master to use when calling our APIs
            String confirmToken = UUID.randomUUID().toString();

            // Store the confirm token as the inbound master token (this is what master will send us)
            em.getTransaction().begin();
            masterToken.setValue(confirmToken);
            em.merge(masterToken);
            em.getTransaction().commit();

            Map<String, String> result = new LinkedHashMap<>();
            result.put("status", "REGISTERED");
            result.put("confirmToken", confirmToken);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(gson.toJson(result));
            System.out.println("[SYSTEM-API] Registration from master accepted: " + masterUrl);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Registration failed\"}");
            System.out.println("[SYSTEM-API] SystemRegisterApi error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }
}

package net.superiorstate.ams.controller.api;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.general.PspClient;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * BPO endpoint — receives partnership requests from PSP deployments.
 * POST /api/v1/partnership/request
 */
@WebServlet("/api/v1/partnership/request")
public class PartnershipRequestApi extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        if (!AppConfig.isBpo()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Not found\"}");
            return;
        }

        Map<String, String> body = ApiClient.readJsonBody(request);
        String pspName = body.get("pspName");
        String pspUrl = body.get("pspUrl");
        String callbackToken = body.get("callbackToken");

        if (pspName == null || pspName.isBlank() || pspUrl == null || !pspUrl.startsWith("http")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"pspName and valid pspUrl are required\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Check for existing active partnership
            List<PspClient> existing = em.createQuery(
                            "SELECT p FROM PspClient p WHERE p.pspUrl = :url AND p.isActive = true", PspClient.class)
                    .setParameter("url", pspUrl)
                    .getResultList();

            for (PspClient pc : existing) {
                if ("APPROVED".equals(pc.getStatus())) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.getWriter().write("{\"error\": \"Partnership already exists\"}");
                    return;
                }
            }

            // Create new PspClient record
            PspClient client = new PspClient();
            client.setPspName(pspName);
            client.setPspUrl(pspUrl);
            client.setApiTokenInbound(callbackToken);
            client.setStatus("PENDING");
            client.setDateRequested(Date.valueOf(LocalDate.now()));

            em.getTransaction().begin();
            em.persist(client);
            em.getTransaction().commit();

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"status\": \"PENDING\", \"message\": \"Partnership request received. Awaiting BPO admin approval.\"}");
            System.out.println("[BPO-API] PartnershipRequestApi: partnership request received");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.out.println("[BPO-API] PartnershipRequestApi error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }
}

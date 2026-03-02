package net.superiorstate.ams.controller.api;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.general.BpoRegistration;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * PSP endpoint — receives partnership approval callbacks from BPO deployments.
 * POST /api/v1/partnership/approve
 */
@WebServlet("/api/v1/partnership/approve")
public class PartnershipApproveApi extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        if (!AppConfig.isPsp()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Not found\"}");
            return;
        }

        Map<String, String> body = ApiClient.readJsonBody(request);
        String bpoUrl = body.get("bpoUrl");
        String bpoToken = body.get("bpoToken");
        String pspToken = body.get("pspToken");

        if (bpoUrl == null || bpoToken == null || pspToken == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"bpoUrl, bpoToken, and pspToken are required\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Find matching BpoRegistration by partner URL and outbound token
            List<BpoRegistration> matches = em.createQuery(
                            "SELECT b FROM BpoRegistration b WHERE b.partnerUrl = :url " +
                                    "AND b.apiTokenOutbound = :token AND b.isActive = true", BpoRegistration.class)
                    .setParameter("url", bpoUrl)
                    .setParameter("token", pspToken)
                    .getResultList();

            if (matches.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"No matching partnership request found\"}");
                return;
            }

            BpoRegistration reg = matches.get(0);

            em.getTransaction().begin();
            reg.setApiTokenInbound(bpoToken);
            reg.setApproved(true);
            reg.setAccepted(true);
            reg.setDateApproved(Date.valueOf(LocalDate.now()));
            em.merge(reg);
            em.getTransaction().commit();

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\": \"APPROVED\", \"message\": \"Partnership approved and tokens exchanged.\"}");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
            System.err.println("PartnershipApproveApi error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }
}

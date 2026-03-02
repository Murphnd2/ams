package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.general.BpoRegistration;
import net.superiorstate.ams.model.general.PSP;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PSP Admin — Vendor Management page.
 * Request BPO connections, view registered vendors, disconnect.
 */
@WebServlet(name = "VendorManager25", value = "/VendorManager")
public class VendorManager25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            Long pspId = global.getPsp().getId();

            List<BpoRegistration> vendors = em.createQuery(
                            "SELECT b FROM BpoRegistration b WHERE b.psp.id = :pspId ORDER BY b.bpoName",
                            BpoRegistration.class)
                    .setParameter("pspId", pspId)
                    .getResultList();

            request.setAttribute("vendors", vendors);
            request.setAttribute("pageTitle", "Vendor Management");
            request.setAttribute("pageIcon", "bi-diagram-3");
            request.getRequestDispatcher("/WEB-INF/view/a/admin/vendorManager25.jsp").forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String action = request.getParameter("action");
        if ("requestConnection".equals(action)) {
            handleRequestConnection(request, response);
        } else if ("disconnect".equals(action)) {
            handleDisconnect(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/VendorManager");
        }
    }

    private void handleRequestConnection(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String bpoName = request.getParameter("bpoName");
        String bpoUrl = request.getParameter("bpoUrl");

        if (bpoName == null || bpoName.isBlank() || bpoUrl == null || !bpoUrl.startsWith("https://")) {
            request.getSession().setAttribute("vendorError", "BPO Name and a valid HTTPS URL are required.");
            response.sendRedirect(request.getContextPath() + "/VendorManager");
            return;
        }

        // Normalize: strip trailing slash
        if (bpoUrl.endsWith("/")) bpoUrl = bpoUrl.substring(0, bpoUrl.length() - 1);

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // Check for existing active registration with this URL
            List<BpoRegistration> existing = em.createQuery(
                            "SELECT b FROM BpoRegistration b WHERE b.partnerUrl = :url AND b.isActive = true",
                            BpoRegistration.class)
                    .setParameter("url", bpoUrl)
                    .getResultList();

            if (!existing.isEmpty()) {
                request.getSession().setAttribute("vendorError", "A vendor with this URL is already registered.");
                response.sendRedirect(request.getContextPath() + "/VendorManager");
                return;
            }

            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            PSP psp = global.getPsp();
            String outboundToken = UUID.randomUUID().toString().replace("-", "");

            // Resolve this system's URL for the callback
            // SYSTEM_URL should be set in ssa.properties for production (e.g. https://superiorstate.biz)
            // Fallback includes context path (e.g. http://localhost:8080/ams)
            String systemUrl = AppConfig.get("SYSTEM_URL");
            if (systemUrl == null || systemUrl.isBlank()) {
                systemUrl = request.getScheme() + "://" + request.getServerName();
                if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                    systemUrl += ":" + request.getServerPort();
                }
                String ctx = request.getContextPath();
                if (ctx != null && !ctx.isEmpty()) {
                    systemUrl += ctx;
                }
            }

            BpoRegistration reg = new BpoRegistration();
            reg.setPsp(psp);
            reg.setBpoName(bpoName);
            reg.setBpoUrl(bpoUrl);
            reg.setPartnerUrl(bpoUrl);
            reg.setApiTokenOutbound(outboundToken);
            reg.setRequested(true);
            reg.setDateRegistered(Date.valueOf(LocalDate.now()));
            reg.setDateRequested(Date.valueOf(LocalDate.now()));

            em.getTransaction().begin();
            em.persist(reg);
            em.getTransaction().commit();

            // POST partnership request to BPO
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("pspName", psp.getFullName());
            payload.put("pspUrl", systemUrl);
            payload.put("callbackToken", outboundToken);

            try {
                ApiClient.ApiResponse apiResponse = ApiClient.postJson(
                        bpoUrl + "/api/v1/partnership/request", payload);

                if (apiResponse.isSuccess()) {
                    request.getSession().setAttribute("vendorMessage",
                            "Connection requested successfully. Awaiting BPO approval.");
                } else {
                    // Rollback: delete the registration
                    em.getTransaction().begin();
                    BpoRegistration toRemove = em.find(BpoRegistration.class, reg.getId());
                    if (toRemove != null) em.remove(toRemove);
                    em.getTransaction().commit();
                    request.getSession().setAttribute("vendorError",
                            "BPO rejected the request (HTTP " + apiResponse.statusCode + "): " + apiResponse.body);
                }
            } catch (RuntimeException e) {
                // Rollback: delete the registration
                em.getTransaction().begin();
                BpoRegistration toRemove = em.find(BpoRegistration.class, reg.getId());
                if (toRemove != null) em.remove(toRemove);
                em.getTransaction().commit();
                request.getSession().setAttribute("vendorError",
                        "Could not reach BPO at " + bpoUrl + ": " + e.getMessage());
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("vendorError", "Error: " + e.getMessage());
            System.err.println("VendorManager25 requestConnection error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/VendorManager");
    }

    private void handleDisconnect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String vendorIdStr = request.getParameter("vendorId");
        if (vendorIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/VendorManager");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long vendorId = Long.parseLong(vendorIdStr);
            BpoRegistration reg = em.find(BpoRegistration.class, vendorId);
            if (reg != null) {
                em.getTransaction().begin();
                reg.setActive(false);
                reg.setDateDisconnected(Date.valueOf(LocalDate.now()));
                em.merge(reg);
                em.getTransaction().commit();
                request.getSession().setAttribute("vendorMessage", "Vendor disconnected.");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("vendorError", "Error disconnecting: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/VendorManager");
    }

    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }
}

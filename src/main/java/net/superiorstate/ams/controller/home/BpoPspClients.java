package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.general.PspClient;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BPO Admin — PSP Clients page.
 * View pending requests, approve/reject, manage active clients, disconnect.
 */
@WebServlet(name = "BpoPspClients", value = "/BpoPspClients")
public class BpoPspClients extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/BpoHome");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<PspClient> clients = em.createQuery(
                            "SELECT p FROM PspClient p WHERE p.isActive = true ORDER BY p.pspName", PspClient.class)
                    .getResultList();

            request.setAttribute("clients", clients);
            request.setAttribute("pageTitle", "PSP Clients");
            request.setAttribute("pageIcon", "bi-building");
            request.getRequestDispatcher("/WEB-INF/view/bpo/pspClients25.jsp").forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/BpoHome");
            return;
        }

        String action = request.getParameter("action");
        switch (action != null ? action : "") {
            case "approve" -> handleApprove(request, response);
            case "reject" -> handleReject(request, response);
            case "disconnect" -> handleDisconnect(request, response);
            case "toggleAutoAccept" -> handleToggleAutoAccept(request, response);
            default -> response.sendRedirect(request.getContextPath() + "/BpoPspClients");
        }
    }

    private void handleApprove(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIdStr = request.getParameter("clientId");
        if (clientIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/BpoPspClients");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long clientId = Long.parseLong(clientIdStr);
            PspClient client = em.find(PspClient.class, clientId);
            if (client == null) {
                response.sendRedirect(request.getContextPath() + "/BpoPspClients");
                return;
            }

            String outboundToken = UUID.randomUUID().toString().replace("-", "");

            em.getTransaction().begin();
            client.setApiTokenOutbound(outboundToken);
            client.setStatus("APPROVED");
            client.setDateApproved(Date.valueOf(LocalDate.now()));
            em.merge(client);
            em.getTransaction().commit();

            // Resolve this system's URL
            // SYSTEM_URL should be set in ssa.properties for production (e.g. https://bpo.example.com)
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

            // POST approval callback to PSP
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("bpoUrl", systemUrl);
            payload.put("bpoToken", outboundToken);
            payload.put("pspToken", client.getApiTokenInbound());

            try {
                ApiClient.ApiResponse apiResponse = ApiClient.postJson(
                        client.getPspUrl() + "/api/v1/partnership/approve", payload);

                if (apiResponse.isSuccess()) {
                    request.getSession().setAttribute("clientMessage",
                            "Partnership with " + client.getPspName() + " approved.");
                } else {
                    request.getSession().setAttribute("clientError",
                            "Approved locally but PSP callback failed (HTTP " + apiResponse.statusCode +
                                    "). The PSP may need to re-sync.");
                }
            } catch (RuntimeException e) {
                request.getSession().setAttribute("clientError",
                        "Approved locally but could not reach PSP at " + client.getPspUrl() +
                                ". The PSP may need to re-sync.");
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("clientError", "Error: " + e.getMessage());
            System.out.println("[BPO-API] BpoPspClients approve error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/BpoPspClients");
    }

    private void handleReject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIdStr = request.getParameter("clientId");
        if (clientIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/BpoPspClients");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long clientId = Long.parseLong(clientIdStr);
            PspClient client = em.find(PspClient.class, clientId);
            if (client != null) {
                em.getTransaction().begin();
                client.setStatus("REJECTED");
                client.setActive(false);
                em.merge(client);
                em.getTransaction().commit();
                request.getSession().setAttribute("clientMessage", "Request from " + client.getPspName() + " rejected.");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("clientError", "Error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/BpoPspClients");
    }

    private void handleDisconnect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIdStr = request.getParameter("clientId");
        if (clientIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/BpoPspClients");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long clientId = Long.parseLong(clientIdStr);
            PspClient client = em.find(PspClient.class, clientId);
            if (client != null) {
                em.getTransaction().begin();
                client.setStatus("DISCONNECTED");
                client.setActive(false);
                client.setDateDisconnected(Date.valueOf(LocalDate.now()));
                em.merge(client);
                em.getTransaction().commit();
                request.getSession().setAttribute("clientMessage", client.getPspName() + " disconnected.");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.getSession().setAttribute("clientError", "Error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/BpoPspClients");
    }

    private void handleToggleAutoAccept(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIdStr = request.getParameter("clientId");
        if (clientIdStr == null) {
            response.sendRedirect(request.getContextPath() + "/BpoPspClients");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long clientId = Long.parseLong(clientIdStr);
            PspClient client = em.find(PspClient.class, clientId);
            if (client != null) {
                em.getTransaction().begin();
                client.setAutoAcceptTasks(!client.isAutoAcceptTasks());
                em.merge(client);
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/BpoPspClients");
    }

    private boolean isAuthorized(HttpSession session) {
        Object isBpoAdmin = session.getAttribute("isBpoAdmin");
        return Boolean.TRUE.equals(isBpoAdmin);
    }
}

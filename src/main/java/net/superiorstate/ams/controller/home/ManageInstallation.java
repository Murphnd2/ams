package net.superiorstate.ams.controller.home;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.general.ManagedInstallation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Detail view + actions for a single managed installation.
 * GET  → fetch health + constants from remote → forward to detail JSP
 * POST → push a constant to the remote installation
 */
@WebServlet(name = "ManageInstallation", value = "/ManageInstallation")
public class ManageInstallation extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request, response)) return;

        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.sendRedirect("SuperDashboard");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            ManagedInstallation mi = em.find(ManagedInstallation.class, Long.parseLong(idStr));
            if (mi == null) {
                response.sendRedirect("SuperDashboard?error=Installation+not+found");
                return;
            }

            request.setAttribute("installation", mi);

            // Fetch health if connected
            if (mi.isConnected() && mi.getApiTokenInbound() != null) {
                try {
                    ApiClient.ApiResponse healthResp = ApiClient.getJson(
                            mi.getInstallationUrl() + "/api/v1/system/health", mi.getApiTokenInbound());
                    if (healthResp.isSuccess()) {
                        request.setAttribute("healthJson", healthResp.body);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> health = gson.fromJson(healthResp.body, Map.class);
                        // Fix Gson double rendering: convert totalActiveUsers to int for display
                        if (health.get("totalActiveUsers") instanceof Number n) {
                            health.put("totalActiveUsers", n.intValue());
                        }
                        request.setAttribute("health", health);
                    } else {
                        request.setAttribute("healthError", "HTTP " + healthResp.statusCode);
                    }
                } catch (Exception e) {
                    request.setAttribute("healthError", e.getMessage());
                }

                // Fetch constants
                try {
                    ApiClient.ApiResponse constResp = ApiClient.getJson(
                            mi.getInstallationUrl() + "/api/v1/system/constants", mi.getApiTokenInbound());
                    if (constResp.isSuccess()) {
                        List<Map<String, String>> constants = gson.fromJson(constResp.body,
                                new TypeToken<List<Map<String, String>>>(){}.getType());
                        request.setAttribute("remoteConstants", constants);
                    } else {
                        request.setAttribute("constantsError", "HTTP " + constResp.statusCode);
                    }
                } catch (Exception e) {
                    request.setAttribute("constantsError", e.getMessage());
                }
            }

            request.setAttribute("pageTitle", mi.getInstallationName());
            request.setAttribute("pageIcon", "bi-hdd-network");

            request.getRequestDispatcher("/WEB-INF/view/a/superDashboard/installationDetail25.jsp")
                    .forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request, response)) return;

        String action = request.getParameter("action");
        String idStr = request.getParameter("id");

        if (idStr == null || action == null) {
            response.sendRedirect("SuperDashboard");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            ManagedInstallation mi = em.find(ManagedInstallation.class, Long.parseLong(idStr));
            if (mi == null || !mi.isConnected()) {
                response.sendRedirect("SuperDashboard?error=Installation+not+connected");
                return;
            }

            if ("pushConstant".equals(action)) {
                String constName = request.getParameter("constName");
                String constValue = request.getParameter("constValue");

                if (constName == null || constName.isBlank()) {
                    response.sendRedirect("ManageInstallation?id=" + idStr + "&error=Name+required");
                    return;
                }

                // PUT to remote constants API
                Map<String, String> payload = Map.of("name", constName, "value", constValue != null ? constValue : "");
                ApiClient.ApiResponse apiResp = ApiClient.postJson(
                        mi.getInstallationUrl() + "/api/v1/system/constants",
                        payload, mi.getApiTokenInbound());

                // Note: postJson sends POST, but we can add a PUT method later.
                // For now, the constants API also handles POST with the same logic.

                if (apiResp.isSuccess()) {
                    response.sendRedirect("ManageInstallation?id=" + idStr + "&success=Constant+pushed");
                } else {
                    response.sendRedirect("ManageInstallation?id=" + idStr + "&error=Push+failed:+" + apiResp.statusCode);
                }
            } else {
                response.sendRedirect("ManageInstallation?id=" + idStr);
            }
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!AppConfig.isMaster()) {
            response.sendRedirect("ViewHome25");
            return false;
        }
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        if (isPspAdmin == null || !isPspAdmin) {
            response.sendRedirect("ViewHome25");
            return false;
        }
        return true;
    }
}

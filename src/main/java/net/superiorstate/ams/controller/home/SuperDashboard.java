package net.superiorstate.ams.controller.home;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.general.ManagedInstallation;
import net.superiorstate.ams.service.InstallationHealthScheduler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Super User Dashboard — centralized management of all AMS installations.
 * Only available when IS_MASTER=true on this installation.
 * GET  → load installations + compute alerts → forward to JSP
 * POST → handle actions (add, register, refresh, refreshAll, disconnect)
 */
@WebServlet(name = "SuperDashboard", value = "/SuperDashboard")
public class SuperDashboard extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request, response)) return;

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        List<ManagedInstallation> installations = global.getManagedInstallations();
        request.setAttribute("installations", installations);

        // Compute alert data
        String masterSchema = global.getSchemaVersion();
        String masterAppVersion = AppConfig.getAppVersion();
        request.setAttribute("masterSchema", masterSchema);
        request.setAttribute("masterAppVersion", masterAppVersion);

        int alertCount = 0;
        List<String> alerts = new ArrayList<>();
        if (installations != null) {
            for (ManagedInstallation mi : installations) {
                if (!"ACTIVE".equals(mi.getStatus())) continue;

                // Unreachable: no heartbeat or > 24 hours stale
                if (mi.getLastHeartbeat() == null) {
                    alerts.add(mi.getInstallationName() + ": never contacted");
                    alertCount++;
                } else if (ChronoUnit.HOURS.between(mi.getLastHeartbeat(), LocalDateTime.now()) > 24) {
                    alerts.add(mi.getInstallationName() + ": last seen > 24h ago");
                    alertCount++;
                }

                // Schema behind
                if (mi.getLastSchemaVersion() != null && masterSchema != null
                        && !"Unknown".equals(masterSchema)
                        && !masterSchema.equals(mi.getLastSchemaVersion())) {
                    alerts.add(mi.getInstallationName() + ": schema " + mi.getLastSchemaVersion()
                            + " (master: " + masterSchema + ")");
                    alertCount++;
                }

                // App version behind
                if (mi.getLastAppVersion() != null && masterAppVersion != null
                        && !"Unknown".equals(masterAppVersion)
                        && !masterAppVersion.equals(mi.getLastAppVersion())) {
                    alerts.add(mi.getInstallationName() + ": WAR " + mi.getLastAppVersion()
                            + " (master: " + masterAppVersion + ")");
                    alertCount++;
                }
            }
        }
        request.setAttribute("alerts", alerts);
        request.setAttribute("alertCount", alertCount);

        // Auto-refresh scheduler status
        request.setAttribute("lastAutoRefresh", InstallationHealthScheduler.getLastRefreshFormatted());
        request.setAttribute("lastAutoRefreshSummary", InstallationHealthScheduler.getLastRefreshSummary());

        request.setAttribute("pageTitle", "Super Dashboard");
        request.setAttribute("pageIcon", "bi-hdd-network");

        request.getRequestDispatcher("/WEB-INF/view/a/superDashboard/superDashboard25.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request, response)) return;

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect("SuperDashboard");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");

        switch (action) {
            case "add" -> handleAdd(request, response, emf, global);
            case "register" -> handleRegister(request, response, emf, global);
            case "refresh" -> handleRefresh(request, response, emf, global);
            case "refreshAll" -> handleRefreshAll(request, response, emf, global);
            case "disconnect" -> handleDisconnect(request, response, emf, global);
            default -> response.sendRedirect("SuperDashboard");
        }
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response,
                           EntityManagerFactory emf, AmsDataGlobal global) throws IOException {
        String name = request.getParameter("installationName");
        String url = request.getParameter("installationUrl");
        String type = request.getParameter("systemType");

        if (name == null || name.isBlank() || url == null || !url.startsWith("http")) {
            response.sendRedirect("SuperDashboard?error=Invalid+name+or+URL");
            return;
        }

        // Normalize URL: strip trailing slash
        url = url.replaceAll("/+$", "");

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            ManagedInstallation mi = new ManagedInstallation();
            mi.setInstallationName(name);
            mi.setInstallationUrl(url);
            mi.setSystemType(type != null ? type : "PSP");
            mi.setStatus("PENDING");
            mi.setApiTokenOutbound(UUID.randomUUID().toString());
            mi.setDateRegistered(Date.valueOf(LocalDate.now()));

            em.persist(mi);
            em.getTransaction().commit();

            reloadInstallations(em, global);

            response.sendRedirect("SuperDashboard?success=Installation+added");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.sendRedirect("SuperDashboard?error=Failed+to+add+installation");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response,
                                EntityManagerFactory emf, AmsDataGlobal global) throws IOException {
        String idStr = request.getParameter("id");
        String deploymentKey = request.getParameter("deploymentKey");

        if (idStr == null || deploymentKey == null || deploymentKey.isBlank()) {
            response.sendRedirect("SuperDashboard?error=Installation+ID+and+deployment+key+required");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            ManagedInstallation mi = em.find(ManagedInstallation.class, Long.parseLong(idStr));
            if (mi == null) {
                response.sendRedirect("SuperDashboard?error=Installation+not+found");
                return;
            }

            // POST registration request to the installation
            String registerUrl = mi.getInstallationUrl() + "/api/v1/system/register";
            String masterUrl = AppConfig.get("SYSTEM_URL", "");
            if (masterUrl.isBlank() && global != null && global.getWebPath() != null) {
                masterUrl = global.getWebPath();
            }

            Map<String, String> payload = Map.of(
                    "masterUrl", masterUrl,
                    "masterName", "SSA Master",
                    "callbackToken", mi.getApiTokenOutbound(),
                    "deploymentKey", deploymentKey
            );

            ApiClient.ApiResponse apiResponse = ApiClient.postJson(registerUrl, payload);

            if (apiResponse.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, String> result = gson.fromJson(apiResponse.body, Map.class);
                String confirmToken = result.get("confirmToken");

                em.getTransaction().begin();
                mi.setApiTokenInbound(confirmToken);
                mi.setStatus("ACTIVE");
                mi.setDateApproved(Date.valueOf(LocalDate.now()));
                em.merge(mi);
                em.getTransaction().commit();

                reloadInstallations(em, global);

                response.sendRedirect("SuperDashboard?success=Registration+successful");
            } else {
                String msg = URLEncoder.encode("Registration failed: HTTP " + apiResponse.statusCode, StandardCharsets.UTF_8);
                response.sendRedirect("SuperDashboard?error=" + msg);
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            String msg = URLEncoder.encode("Registration failed: " + e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect("SuperDashboard?error=" + msg);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private void handleRefresh(HttpServletRequest request, HttpServletResponse response,
                               EntityManagerFactory emf, AmsDataGlobal global) throws IOException {
        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.sendRedirect("SuperDashboard");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            ManagedInstallation mi = em.find(ManagedInstallation.class, Long.parseLong(idStr));
            if (mi == null || mi.getApiTokenInbound() == null) {
                response.sendRedirect("SuperDashboard?error=Not+connected");
                return;
            }

            refreshSingleInstallation(mi, em);
            reloadInstallations(em, global);

            response.sendRedirect("SuperDashboard?success=Health+refreshed");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("[SUPER-DASH] Refresh failed for id=" + idStr + ": " + e.getMessage());
            String msg = URLEncoder.encode("Refresh failed: " + e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect("SuperDashboard?error=" + msg);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private void handleRefreshAll(HttpServletRequest request, HttpServletResponse response,
                                  EntityManagerFactory emf, AmsDataGlobal global) throws IOException {
        EntityManager em = emf.createEntityManager();
        try {
            List<ManagedInstallation> active = em.createQuery(
                    "SELECT m FROM ManagedInstallation m WHERE m.status = 'ACTIVE' AND m.isActive = true",
                    ManagedInstallation.class).getResultList();

            int success = 0, failed = 0;
            for (ManagedInstallation mi : active) {
                try {
                    refreshSingleInstallation(mi, em);
                    success++;
                } catch (Exception e) {
                    System.out.println("[SUPER-DASH] Refresh failed for " + mi.getInstallationName() + ": " + e.getMessage());
                    failed++;
                }
            }

            reloadInstallations(em, global);

            String msg = success + "+refreshed";
            if (failed > 0) msg += ",+" + failed + "+failed";
            response.sendRedirect("SuperDashboard?success=" + msg);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.sendRedirect("SuperDashboard?error=Refresh+all+failed");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Pulls health from a single installation and updates the entity in the DB.
     * Caller must manage the EntityManager lifecycle.
     */
    private void refreshSingleInstallation(ManagedInstallation mi, EntityManager em) {
        String healthUrl = mi.getInstallationUrl() + "/api/v1/system/health";
        ApiClient.ApiResponse apiResponse = ApiClient.getJson(healthUrl, mi.getApiTokenInbound());

        if (!apiResponse.isSuccess()) {
            throw new RuntimeException("HTTP " + apiResponse.statusCode + " from " + mi.getInstallationUrl());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> health = gson.fromJson(apiResponse.body, Map.class);

        em.getTransaction().begin();
        mi.setLastHeartbeat(LocalDateTime.now());

        if (health.get("schemaVersion") != null)
            mi.setLastSchemaVersion(health.get("schemaVersion").toString());

        if (health.get("appVersion") != null)
            mi.setLastAppVersion(health.get("appVersion").toString());

        if (health.get("totalActiveUsers") != null) {
            int userCount = ((Number) health.get("totalActiveUsers")).intValue();
            mi.setLastUserCount(userCount >= 0 ? userCount : null);
        }

        em.merge(mi);
        em.getTransaction().commit();
    }

    private void handleDisconnect(HttpServletRequest request, HttpServletResponse response,
                                  EntityManagerFactory emf, AmsDataGlobal global) throws IOException {
        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.sendRedirect("SuperDashboard");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ManagedInstallation mi = em.find(ManagedInstallation.class, Long.parseLong(idStr));
            if (mi != null) {
                mi.setStatus("DISCONNECTED");
                mi.setActive(false);
                mi.setDateDisconnected(Date.valueOf(LocalDate.now()));
                em.merge(mi);
            }
            em.getTransaction().commit();
            reloadInstallations(em, global);
            response.sendRedirect("SuperDashboard?success=Installation+disconnected");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.sendRedirect("SuperDashboard?error=Disconnect+failed");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private void reloadInstallations(EntityManager em, AmsDataGlobal global) {
        try {
            List<ManagedInstallation> fresh = em.createQuery(
                    "SELECT m FROM ManagedInstallation m WHERE m.isActive = true ORDER BY m.installationName",
                    ManagedInstallation.class).getResultList();
            global.setManagedInstallations(fresh);
        } catch (Exception ignored) {}
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

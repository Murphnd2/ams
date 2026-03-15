package net.superiorstate.ams.service;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.general.ManagedInstallation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background scheduler that periodically polls all active managed installations
 * for health data. Only runs on the master installation.
 *
 * Lifecycle: started from EmfListener.contextInitialized(), stopped from contextDestroyed().
 */
public class InstallationHealthScheduler {

    private static final Gson gson = new Gson();
    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long INTERVAL_HOURS = 4;
    private static final long INITIAL_DELAY_MINUTES = 2; // small delay after startup

    private static volatile String lastRefreshFormatted;
    private static volatile String lastRefreshSummary;

    private final ScheduledExecutorService executor;
    private final EntityManagerFactory emf;
    private final AmsDataGlobal global;

    public InstallationHealthScheduler(EntityManagerFactory emf, AmsDataGlobal global) {
        this.emf = emf;
        this.global = global;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "installation-health-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the periodic health refresh. Runs every INTERVAL_HOURS after an initial delay.
     */
    public void start() {
        executor.scheduleAtFixedRate(this::refreshAll, INITIAL_DELAY_MINUTES, INTERVAL_HOURS * 60, TimeUnit.MINUTES);
        System.out.println("[HEALTH-SCHEDULER] Started — refreshing every " + INTERVAL_HOURS
                + "h (first run in " + INITIAL_DELAY_MINUTES + "m)");
    }

    /**
     * Shuts down the scheduler gracefully.
     */
    public void stop() {
        executor.shutdownNow();
        System.out.println("[HEALTH-SCHEDULER] Stopped");
    }

    /**
     * Refreshes health for all active, connected installations.
     * Each installation is polled independently; failures are logged but don't stop others.
     */
    private void refreshAll() {
        String timestamp = LocalDateTime.now().format(LOG_FMT);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();

            List<ManagedInstallation> active = em.createQuery(
                    "SELECT m FROM ManagedInstallation m WHERE m.status = 'ACTIVE' AND m.isActive = true",
                    ManagedInstallation.class).getResultList();

            if (active.isEmpty()) {
                return; // nothing to do
            }

            int success = 0, failed = 0;
            for (ManagedInstallation mi : active) {
                if (mi.getApiTokenInbound() == null) {
                    continue; // not yet registered
                }
                try {
                    refreshSingle(mi, em);
                    success++;
                } catch (Exception e) {
                    System.out.println("[HEALTH-SCHEDULER] " + timestamp + " FAILED "
                            + mi.getInstallationName() + ": " + e.getMessage());
                    failed++;
                }
            }

            // Reload the cached list
            reloadInstallations(em);

            // Update static status for dashboard display
            lastRefreshFormatted = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
            lastRefreshSummary = success + " ok" + (failed > 0 ? ", " + failed + " failed" : "");

            System.out.println("[HEALTH-SCHEDULER] " + timestamp + " Complete: "
                    + success + " ok, " + failed + " failed");

        } catch (Exception e) {
            System.out.println("[HEALTH-SCHEDULER] " + timestamp + " ERROR: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private void refreshSingle(ManagedInstallation mi, EntityManager em) {
        String healthUrl = mi.getInstallationUrl() + "/api/v1/system/health";
        ApiClient.ApiResponse apiResponse = ApiClient.getJson(healthUrl, mi.getApiTokenInbound());

        if (!apiResponse.isSuccess()) {
            throw new RuntimeException("HTTP " + apiResponse.statusCode);
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

    /** Last auto-refresh timestamp formatted for display, or null if never run. */
    public static String getLastRefreshFormatted() { return lastRefreshFormatted; }

    /** Summary of last auto-refresh result, e.g. "2 ok" or "1 ok, 1 failed". */
    public static String getLastRefreshSummary() { return lastRefreshSummary; }

    private void reloadInstallations(EntityManager em) {
        try {
            List<ManagedInstallation> fresh = em.createQuery(
                    "SELECT m FROM ManagedInstallation m WHERE m.isActive = true ORDER BY m.installationName",
                    ManagedInstallation.class).getResultList();
            global.setManagedInstallations(fresh);
        } catch (Exception ignored) {}
    }
}

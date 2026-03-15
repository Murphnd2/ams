package net.superiorstate.ams.controller.api;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns system health information for this installation.
 * GET /api/v1/system/health
 * Authenticated via ApiTokenFilter (master token required).
 */
@WebServlet("/api/v1/system/health")
public class SystemHealthApi extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"System not initialized\"}");
            return;
        }

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("systemType", AppConfig.getSystemType());
        health.put("schemaVersion", global != null ? global.getSchemaVersion() : "Unknown");
        health.put("appVersion", AppConfig.getAppVersion());
        health.put("buildTimestamp", AppConfig.getBuildTimestamp());

        // PSP name
        if (global != null && global.getPsp() != null) {
            health.put("pspName", global.getPsp().getFullName());
        }

        // JVM uptime in days
        try {
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            double uptimeDays = uptimeMs / (1000.0 * 60 * 60 * 24);
            health.put("uptimeDays", Math.round(uptimeDays * 10.0) / 10.0);
        } catch (Exception ignored) {}

        // Java version
        health.put("javaVersion", System.getProperty("java.version", "Unknown"));

        // User counts by role
        EntityManager em = emf.createEntityManager();
        try {
            Map<String, Integer> usersByRole = new LinkedHashMap<>();
            int totalActive = 0;

            @SuppressWarnings("unchecked")
            List<Object[]> roleCounts = em.createNativeQuery(
                    "SELECT ur.description, COUNT(DISTINCT u.person_id) AS cnt " +
                    "FROM userinroles uir " +
                    "JOIN user u ON u.person_id = uir.person_id " +
                    "JOIN userrole ur ON ur.role_id = uir.role_id " +
                    "WHERE u.is_active = 1 " +
                    "GROUP BY ur.description ORDER BY ur.description")
                    .getResultList();

            for (Object[] row : roleCounts) {
                String roleName = (String) row[0];
                int count = ((Number) row[1]).intValue();
                usersByRole.put(roleName, count);
                totalActive += count;
            }

            health.put("totalActiveUsers", totalActive);
            health.put("usersByRole", usersByRole);

        } catch (Exception e) {
            health.put("totalActiveUsers", -1);
            health.put("usersByRole", Map.of());
            health.put("userCountError", e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(health));
    }
}

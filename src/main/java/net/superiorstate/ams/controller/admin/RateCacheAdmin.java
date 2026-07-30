package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.service.RateCacheWarmService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PSP Admin — A1 rate-cache status page. Displays whether the warm job is enabled
 * on this instance, per-county cache status, and a manual refresh trigger.
 */
@WebServlet(name = "RateCacheAdmin", value = "/RateCacheAdmin")
public class RateCacheAdmin extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        RateCacheWarmService warmService = (RateCacheWarmService) getServletContext().getAttribute("rateCacheWarmService");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");

        EntityManager em = emf.createEntityManager();
        try {
            String warmEnabledConstant = AppConstantDAO.getConstantValue(em, "RATE_CACHE_WARM_ENABLED");
            String planYearsConstant = AppConstantDAO.getConstantValue(em, "RATE_CACHE_PLAN_YEARS");
            List<Integer> planYears = parsePlanYears(planYearsConstant);

            // Plan years are configuration (RATE_CACHE_PLAN_YEARS, D-84), never derived from
            // the current date — see RateCacheWarmService's class Javadoc for why. This page
            // displays whatever is actually configured, which may be zero, one, or several years
            // (e.g. "2026,2027" during open enrollment).
            Map<Integer, List<RateCacheDAO.CountySummary>> countySummariesByYear = new LinkedHashMap<>();
            for (Integer year : planYears) {
                countySummariesByYear.put(year, RateCacheDAO.getCountySummaries(em, year));
            }

            request.setAttribute("warmEnabled", warmService != null);
            request.setAttribute("warmEnabledConstant", warmEnabledConstant);
            request.setAttribute("sourceEnv", RateCacheWarmService.currentSourceEnv());
            request.setAttribute("planYearsConstant", planYearsConstant);
            request.setAttribute("configuredPlanYears", planYears);
            request.setAttribute("runInProgress", warmService != null && warmService.isRunInProgress());
            request.setAttribute("lastRunSummary", warmService != null ? warmService.getLastRunSummary() : null);
            request.setAttribute("lastRunAt", warmService != null ? warmService.getLastRunAt() : null);
            request.setAttribute("countySummariesByYear", countySummariesByYear);
            request.setAttribute("pageTitle", "Rate Cache");
            request.setAttribute("pageIcon", "bi-graph-up");
            request.getRequestDispatcher("/WEB-INF/view/a/admin/rateCacheAdmin25.jsp").forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /** Tolerant parse matching RateCacheWarmService's own RATE_CACHE_PLAN_YEARS handling — display only. */
    private List<Integer> parsePlanYears(String raw) {
        List<Integer> years = new ArrayList<>();
        if (raw == null || raw.isBlank()) return years;
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            try {
                years.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                // malformed entries are reported by RateCacheWarmService's own run logs; this
                // display-only parse just skips them
            }
        }
        return years;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String action = request.getParameter("action");
        if ("refresh".equals(action)) {
            RateCacheWarmService warmService = (RateCacheWarmService) getServletContext().getAttribute("rateCacheWarmService");
            if (warmService == null) {
                session.setAttribute("rateCacheError", "Rate-cache warming is not enabled on this instance.");
            } else {
                RateCacheWarmService.WarmTriggerResult result = warmService.triggerManualWarm();
                if (result == RateCacheWarmService.WarmTriggerResult.STARTED) {
                    session.setAttribute("rateCacheMessage", "Warm run started.");
                } else {
                    session.setAttribute("rateCacheMessage", "A warm run is already in progress.");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/RateCacheAdmin");
    }

    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }
}

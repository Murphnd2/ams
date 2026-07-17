package net.superiorstate.ams.controller.monthly;

import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.service.BillingPipelineRunner;
import net.superiorstate.ams.data.service.BillingRunService;
import net.superiorstate.ams.model.billing.BillingRun;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Launches the Monthly Billing Launcher's background pipeline run.
 * POST-only. Admin-gated. Single-run-guarded via BillingRunService.isRunActive.
 */
@WebServlet(name = "LaunchMonthlyBilling", value = "/LaunchMonthlyBilling")
public class LaunchMonthlyBilling extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String modeParam = request.getParameter("mode");
        String mode = BillingRun.MODE_BILLING_ONLY.equals(modeParam)
                ? BillingRun.MODE_BILLING_ONLY
                : BillingRun.MODE_FULL;

        boolean planTypeSupplied = parseBoolean(request.getParameter("planTypeSupplied"));

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        ExecutorService billingExecutor = (ExecutorService) getServletContext().getAttribute("billingExecutor");

        if (billingExecutor == null) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Billing worker is not available (system not fully initialized).");
            return;
        }

        BillingRunService.reapStaleRuns(emf, 30);
        if (BillingRunService.isRunActive(emf)) {
            writeError(response, HttpServletResponse.SC_CONFLICT,
                    "A billing run is already in progress.");
            return;
        }

        long launchedBy = getPersonId(request);
        long runId = BillingRunService.createRun(emf, mode, launchedBy, planTypeSupplied);

        billingExecutor.submit(new BillingPipelineRunner(emf, global, runId, mode));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runId", runId);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(body));
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        response.getWriter().write(gson.toJson(body));
    }

    private boolean isAdmin(HttpServletRequest request) {
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return isPspAdmin != null && isPspAdmin;
    }

    private long getPersonId(HttpServletRequest request) {
        Object personId = request.getSession().getAttribute("personId");
        if (personId instanceof Long) return (Long) personId;
        if (personId instanceof Number) return ((Number) personId).longValue();
        return 104L;
    }
}

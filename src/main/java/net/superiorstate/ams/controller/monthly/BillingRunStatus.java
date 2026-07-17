package net.superiorstate.ams.controller.monthly;

import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.service.BillingRunService;
import net.superiorstate.ams.model.billing.BillingRun;
import net.superiorstate.ams.model.billing.BillingRunStep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only poll endpoint for the Monthly Billing Launcher page. Reads via fresh
 * EntityManagers against BillingRunService, which evicts the L2 cache after every
 * write, so polling always reflects live worker progress.
 */
@WebServlet(name = "BillingRunStatus", value = "/BillingRunStatus")
public class BillingRunStatus extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long runId = parseLong(request.getParameter("runId"));
        if (runId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");

        BillingRun run = BillingRunService.findRun(emf, runId);
        if (run == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        List<BillingRunStep> steps = BillingRunService.findSteps(emf, runId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runId", run.getId());
        body.put("status", run.getStatus());
        body.put("mode", run.getMode());
        body.put("currentStep", run.getCurrentStep());
        body.put("startedAt", run.getStartedAt() != null ? run.getStartedAt().toString() : null);
        body.put("completedAt", run.getCompletedAt() != null ? run.getCompletedAt().toString() : null);
        body.put("planTypeSupplied", run.isPlanTypeSupplied());
        body.put("renewalsRefreshed", run.isRenewalsRefreshed());
        body.put("errorText", run.getErrorText());

        List<Map<String, Object>> stepList = new ArrayList<>();
        for (BillingRunStep step : steps) {
            Map<String, Object> stepBody = new LinkedHashMap<>();
            stepBody.put("stepName", step.getStepName());
            stepBody.put("status", step.getStatus());
            stepBody.put("startedAt", step.getStartedAt() != null ? step.getStartedAt().toString() : null);
            stepBody.put("completedAt", step.getCompletedAt() != null ? step.getCompletedAt().toString() : null);
            stepBody.put("detail", step.getDetail());
            stepBody.put("errorText", step.getErrorText());
            stepList.add(stepBody);
        }
        body.put("steps", stepList);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(body));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return isPspAdmin != null && isPspAdmin;
    }
}

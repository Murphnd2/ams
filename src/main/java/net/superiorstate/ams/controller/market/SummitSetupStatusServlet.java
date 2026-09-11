package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SummitSetupStepDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.SummitFileExport;
import net.superiorstate.ams.model.market.SummitSetupStep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * S45-B -- an include-only status fragment for the Summit setup panel
 * ({@code detailSummitSetup25.jsp}), dispatched via {@code jsp:include} once per file step. Writes
 * one small {@code <div>} naming the step's current state, or nothing.
 * <p>
 * ⚠️ <b>Fails silent, always.</b> Not PSP admin, not ICHRA-available, an unrecognized step, or any
 * exception -- every one of those writes nothing rather than an error page, because this fragment
 * is embedded inside the activity detail page and must never be the reason that page breaks.
 * Exceptions are logged at WARN with no response content in the message.
 */
@WebServlet(name = "SummitSetupStatusServlet", value = "/SummitSetupStatus")
public class SummitSetupStatusServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitSetupStatusServlet.class);

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> STEP_FILE_TYPES = Map.of(
            "employer", "employer",
            "cdhplan", "cdhplan",
            "demographics", "demographics"
            // "schedules" deliberately absent -- no pushed file, so no delivery-attempt fallback line.
    );

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // S45c -- jsp:include preserves the including request's method, and the Setup panel
        // (detailSummitSetup25.jsp, included from detailDetail25.jsp/detailSetup25.jsp) is
        // reached via GoActivityDetail25's doPost, not only its doGet. Without this override,
        // HttpServlet's default doPost returns 405 here, and the container silently discards
        // that inside an include -- the status line renders as nothing, with no log line.
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
            if (!isPspAdmin) return;

            String proposalIdParam = request.getParameter("proposalId");
            String step = request.getParameter("step");
            if (proposalIdParam == null || proposalIdParam.isBlank() || step == null) return;

            long proposalId;
            try {
                proposalId = Long.parseLong(proposalIdParam.trim());
            } catch (NumberFormatException e) {
                return;
            }

            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                if (!IchraAccessResolver.isAvailable(em, request)) return;

                Long pspId = resolveCurrentPspId(request);
                SummitSetupStep stepState = SummitSetupStepDAO.findByProposalAndStep(em, pspId, proposalId, step);

                String html;
                if (stepState != null && "DONE".equals(stepState.getState())) {
                    html = "<div style=\"font-size: 0.72rem;\" class=\"text-success\">Done · "
                            + DISPLAY_FORMAT.format(stepState.getUpdatedAt()) + " · "
                            + escape(stepState.getBasis()) + "</div>";
                } else {
                    String fileType = STEP_FILE_TYPES.get(step);
                    SummitFileExport attempt = fileType == null ? null
                            : SummitSetupStepDAO.findLatestDeliveryAttempt(em, pspId, proposalId, fileType);
                    if (attempt != null && attempt.getDeliveredAt() != null) {
                        html = "<div style=\"font-size: 0.72rem;\" class=\"text-muted\">"
                                + escape(attempt.getDeliveryStatus()) + " · "
                                + DISPLAY_FORMAT.format(attempt.getDeliveredAt()) + "</div>";
                    } else {
                        return;
                    }
                }
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(html);
            } finally {
                if (em.isOpen()) em.close();
            }
        } catch (Exception e) {
            log.warn("[SUMMIT-SETUP-STATUS] fragment failed for proposalId={} step={}: {}",
                    request.getParameter("proposalId"), request.getParameter("step"), e.getMessage());
        }
    }

    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** Minimal HTML-attribute/text escaping for the handful of trusted-shape values this fragment renders. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

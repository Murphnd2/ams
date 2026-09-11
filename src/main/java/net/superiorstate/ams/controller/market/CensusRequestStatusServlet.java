package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.service.CensusIntakeService;
import net.superiorstate.ams.model.market.CensusSubmission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * S47-C -- an include-only status fragment for the Summit setup panel's step 3, "Request census"
 * ({@code detailSummitSetup25.jsp}), dispatched via {@code jsp:include}. Copies
 * {@link SummitSetupStatusServlet}'s pattern exactly: writes one small muted line from
 * {@code CensusIntakeService.statusFor}, or nothing.
 * <p>
 * ⚠️ <b>Fails silent, always.</b> Not PSP admin, not ICHRA-available, a bad proposal id, or any
 * exception -- every one of those writes nothing rather than an error page, because this fragment
 * is embedded inside the activity detail page and must never be the reason that page breaks.
 * Exceptions are logged at WARN with no response content in the message.
 */
@WebServlet(name = "CensusRequestStatusServlet", value = "/CensusRequestStatus")
public class CensusRequestStatusServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(CensusRequestStatusServlet.class);

    private static final String VIEW = "/WEB-INF/view/market/censusRequestStatus25.jsp";
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // S45c -- jsp:include preserves the including request's method, and the Setup panel is
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
            if (proposalIdParam == null || proposalIdParam.isBlank()) return;

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

                CensusIntakeService.Status status = CensusIntakeService.statusFor(em, proposalId);

                String line;
                if (!status.hasRequest()) {
                    line = "Not requested";
                } else if (status.isActive()) {
                    line = "Requested " + format(status.getRequestedAt())
                            + " · link expires " + format(status.getExpiresAt());
                } else if (status.isExpired()) {
                    line = "Link expired " + format(status.getExpiresAt());
                } else if ("LOADED".equals(status.getRequestState())) {
                    line = "Loaded " + format(status.getClosedAt());
                } else {
                    line = "Revoked " + format(status.getClosedAt());
                }
                if (status.hasSubmission()) {
                    CensusSubmission s = status.getLatestSubmission();
                    if (CensusSubmission.STATE_UNREADABLE.equals(s.getState())) {
                        line += " · Upload " + format(s.getSubmittedAt()) + ": unreadable";
                    } else {
                        line += " · Upload " + format(s.getSubmittedAt()) + ": " + s.getRowCount()
                                + " rows, " + s.getIssueCount() + " issues";
                    }
                }

                // S47-F -- a Review link appears only while there is something to review: the
                // request is OPEN and its latest submission is still PENDING/UNREADABLE (the
                // same "reviewable" definition CensusIntakeService.reviewable uses).
                if (status.isOpen()
                        && CensusIntakeService.reviewable(em, proposalId).isPresent()) {
                    request.setAttribute("censusReviewUrl",
                            request.getContextPath() + "/CensusReview?proposalId=" + proposalId);
                }

                request.setAttribute("censusStatusLine", line);
                // The JSP is the one place the line is rendered; forwarding from inside an include
                // is not allowed, so this is an include of the fragment view.
                request.getRequestDispatcher(VIEW).include(request, response);
            } finally {
                if (em.isOpen()) em.close();
            }
        } catch (Exception e) {
            log.warn("[CENSUS-REQUEST-STATUS] fragment failed for proposalId={}: {}",
                    request.getParameter("proposalId"), e.getMessage());
        }
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : DISPLAY_FORMAT.format(dateTime);
    }
}

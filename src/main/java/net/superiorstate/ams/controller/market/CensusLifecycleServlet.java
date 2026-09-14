package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.service.CensusLifecycleService;
import net.superiorstate.ams.model.general.PSP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * S59-P6rev -- an include-only state-token fragment for the Summit setup panel's merged `Census`
 * row ({@code detailSummitSetup25.jsp}), dispatched via {@code c:import} once per render. Writes
 * {@link CensusLifecycleService.State}'s name (e.g. {@code "NOT_REQUESTED"}, {@code
 * "AWAITING_CLIENT"}) as its entire body; the JSP's own {@code <c:choose>} maps the token to which
 * action buttons render.
 * <p>
 * ⚠️ <b>Always returns 200, on every path, including a bad/missing {@code proposalId} and any
 * internal failure.</b> {@code <c:import>} aborts the whole including page on a non-2xx response,
 * and this fragment must never be the reason the Setup detail view breaks -- matching {@code
 * CardIssuerAvailabilityServlet}'s contract exactly, except here a missing/failed determination
 * still writes a body ({@code "INDETERMINATE"}), because the JSP has no "nothing rendered" fallback
 * shape the way a hidden row does -- the button group must always contain something.
 */
@WebServlet(name = "CensusLifecycleServlet", value = "/CensusLifecycle")
public class CensusLifecycleServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(CensusLifecycleServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Matches the sibling fragment servlets: jsp:include / c:import preserve the including
        // request's method, and the Setup panel is reached via GoActivityDetail25's doPost as
        // well as its doGet. Without this override, HttpServlet's default doPost returns 405
        // here, which is not the 200 <c:import> requires.
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
            if (!isPspAdmin) {
                // Not indeterminate -- an unauthorized caller sees nothing, matching the sibling
                // fragment servlets' isPspAdmin gate. The panel that includes this fragment is
                // itself isPspAdmin-gated, so this path is unreachable through the page; it exists
                // only to fail safe against a direct request.
                return;
            }

            String proposalIdParam = request.getParameter("proposalId");
            if (proposalIdParam == null || proposalIdParam.isBlank()) {
                writeState(response, CensusLifecycleService.State.INDETERMINATE);
                return;
            }

            long proposalId;
            try {
                proposalId = Long.parseLong(proposalIdParam.trim());
            } catch (NumberFormatException e) {
                writeState(response, CensusLifecycleService.State.INDETERMINATE);
                return;
            }

            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                Long pspId = resolveCurrentPspId(request);
                CensusLifecycleService.State state = CensusLifecycleService.resolve(em, pspId, proposalId);
                writeState(response, state);
            } finally {
                if (em.isOpen()) em.close();
            }
        } catch (Exception e) {
            log.warn("[CENSUS-LIFECYCLE] fragment failed for proposalId={}: {}",
                    request.getParameter("proposalId"), e.getMessage());
            // Any other failure -- indeterminate, fail open. Still 200: <c:import> must not abort
            // the page.
            writeState(response, CensusLifecycleService.State.INDETERMINATE);
        }
    }

    /**
     * Same PSP-resolution route {@code SummitSetupStatusServlet.resolveCurrentPspId} uses, already
     * copied once into {@code CardIssuerAvailabilityServlet}; copied again here rather than
     * extracted into a shared helper, per this run's scope.
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    private void writeState(HttpServletResponse response, CensusLifecycleService.State state) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(state.name());
    }
}

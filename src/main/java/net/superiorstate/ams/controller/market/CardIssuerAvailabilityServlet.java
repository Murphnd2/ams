package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.service.CardIssuerAvailabilityService;
import net.superiorstate.ams.model.general.PSP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * S59-P3rev -- an include-only marker fragment for the Summit setup panel's card-issuer row
 * ({@code detailSummitSetup25.jsp}), dispatched via {@code c:import} once per render. Answers
 * "should the $1 card-issuer seed election row render" using
 * {@link CardIssuerAvailabilityService}'s three-valued determination, collapsed here to a
 * present/absent marker: {@code AVAILABLE} and {@code INDETERMINATE} both write a short non-blank
 * marker (fail open -- see the service's javadoc for why); {@code NOT_AVAILABLE}, the one definite
 * negative, writes nothing.
 * <p>
 * ⚠️ <b>Always returns 200, on every path, including a bad/missing {@code proposalId} and any
 * internal failure.</b> {@code <c:import>} aborts the whole including page on a non-2xx response,
 * and this fragment must never be the reason the Setup detail view breaks -- matching the fail-safe
 * contract the three sibling fragment servlets ({@code SummitSetupStatusServlet},
 * {@code SummitEmployerLinkServlet}, {@code CensusRequestStatusServlet}) already carry, though
 * those three simply write nothing on any failure; this one must write the fail-open marker
 * instead, because here "write nothing" means "hide a control", not "omit a decoration".
 */
@WebServlet(name = "CardIssuerAvailabilityServlet", value = "/CardIssuerAvailability")
public class CardIssuerAvailabilityServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(CardIssuerAvailabilityServlet.class);

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
        boolean shouldRender;
        try {
            shouldRender = shouldRenderCardIssuerRow(request);
        } catch (Exception e) {
            log.warn("[CARD-ISSUER-AVAILABILITY] fragment failed for proposalId={}: {}",
                    request.getParameter("proposalId"), e.getMessage());
            shouldRender = true; // any other failure -- indeterminate, fail open.
        }

        if (shouldRender) {
            writeMarker(response);
        } else {
            writeNothing(response);
        }
    }

    /**
     * @return {@code true} when the row should render ({@code AVAILABLE} or
     * {@code INDETERMINATE} -- fail open), {@code false} only for the one definite negative
     * ({@code NOT_AVAILABLE}) or an unauthorized caller. Never throws to its caller -- every
     * checked failure inside is itself indeterminate and returns {@code true}; only an
     * unauthorized session or a malformed/missing {@code proposalId} return {@code false}/{@code true}
     * directly without reaching the service.
     */
    private boolean shouldRenderCardIssuerRow(HttpServletRequest request) {
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            // Not indeterminate -- an unauthorized caller sees nothing, matching the three
            // sibling fragment servlets' isPspAdmin gate. The panel that includes this fragment
            // is itself isPspAdmin-gated, so this path is unreachable through the page; it exists
            // only to fail safe against a direct request.
            return false;
        }

        String proposalIdParam = request.getParameter("proposalId");
        if (proposalIdParam == null || proposalIdParam.isBlank()) {
            // "No proposal" -- indeterminate, fail open.
            return true;
        }

        long proposalId;
        try {
            proposalId = Long.parseLong(proposalIdParam.trim());
        } catch (NumberFormatException e) {
            // Unparseable proposalId -- indeterminate, fail open, same reasoning.
            return true;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = resolveCurrentPspId(request);
            CardIssuerAvailabilityService.Result result =
                    CardIssuerAvailabilityService.resolve(em, pspId, proposalId);
            return result != CardIssuerAvailabilityService.Result.NOT_AVAILABLE;
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Same PSP-resolution route {@code SummitSetupStatusServlet.resolveCurrentPspId} uses, copied
     * verbatim rather than shared, matching that class's own precedent (it is {@code private} in
     * its class, so this is a copy of the route, not a call to it).
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** The non-blank marker the JSP's {@code c:import} captures and tests for blankness. */
    private void writeMarker(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("1");
    }

    /** Deliberately empty body -- what the JSP's blankness test after trimming reads as "hide". */
    private void writeNothing(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}

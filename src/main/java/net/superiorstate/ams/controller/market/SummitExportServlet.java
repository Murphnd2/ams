package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.ProposalIchraIntakeDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.ProposalIchraIntake;
import net.superiorstate.ams.model.sales.agency.Prospect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * S24-E, stage 1 — generates the two Summit import files AMS can source today from a
 * proposal's own data: Employer Demographic and Employer CDH Plan. Participants and
 * enrollment are out of scope; they need an employee roster AMS does not have. See
 * {@code docs/business/summit_data_exchange.md} for the file spec this implements.
 * <p>
 * PSP-admin-only, reachable only by URL — no nav entry, no menu link. A user clicks the
 * link Kevin gives them, a file downloads, and the file is uploaded to Summit by hand.
 * No FTP, no automation, no scheduling.
 */
@WebServlet(name = "SummitExportServlet", value = "/SummitExport")
public class SummitExportServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SummitExportServlet.class);

    private static final DateTimeFormatter SUMMIT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String proposalIdParam = request.getParameter("proposalId");
        String type = request.getParameter("type");
        if (proposalIdParam == null || proposalIdParam.isBlank()
                || type == null || !(type.equals("employer") || type.equals("cdhplan"))) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "proposalId and type (employer|cdhplan) are required.");
            return;
        }

        long proposalId;
        try {
            proposalId = Long.parseLong(proposalIdParam.trim());
        } catch (NumberFormatException e) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid proposalId.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            // PSP admin, then ICHRA entitlement — same order and same resolver every other
            // ICHRA surface uses, so this gate can never disagree with the rest of the feature.
            if (!IchraAccessResolver.isAvailable(em, request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            Proposal proposal = em.find(Proposal.class, proposalId);
            Prospect prospect = proposal != null ? proposal.getProspect() : null;
            if (prospect == null) {
                writePlainError(response, HttpServletResponse.SC_NOT_FOUND, "Proposal not found.");
                return;
            }

            if (type.equals("employer")) {
                writeEmployerDemographic(response, prospect);
            } else {
                writeEmployerCdhPlan(em, response, proposal, prospect);
            }
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    /**
     * Employer Demographic — six columns, {@code Employer TPA Custom ID} = {@code Prospect.id}.
     * The upsert key must never change for a given employer, so it is read straight off the
     * AMS-owned, immutable primary key rather than any Summit- or user-editable value.
     */
    private void writeEmployerDemographic(HttpServletResponse response, Prospect prospect) throws IOException {
        Address address = prospect.getAddress();
        String address1 = address != null ? address.getAddress1() : null;
        String city = address != null ? address.getCity() : null;
        String state = address != null ? address.getState() : null;
        String zip = address != null ? address.getZipCode() : null;

        String missingField = firstBlank(
                "Mailing Address", address1,
                "Mailing City", city,
                "Mailing State", state,
                "Mailing Zip", zip);
        if (missingField != null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer Demographic file: prospect " + prospect.getId()
                            + " is missing " + missingField + ". All four address fields are hard"
                            + " requirements in Summit despite being labelled Optional.");
            return;
        }

        String line = String.join("|",
                sanitize(prospect.getName()),
                String.valueOf(prospect.getId()),
                sanitize(address1),
                sanitize(city),
                sanitize(state),
                sanitize(zip));

        String filename = "employer-demographic-" + sanitizeFilename(prospect.getName())
                + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt";
        writeFile(response, filename, line);
    }

    /**
     * Employer CDH Plan — eight columns. Plan year comes from the proposal's ICHRA intake
     * (calendar-year assumption, LA-NN — see close-out); the plan template ID comes from
     * config, resolved here at request time so it is never baked into the WAR.
     */
    private void writeEmployerCdhPlan(EntityManager em, HttpServletResponse response,
                                       Proposal proposal, Prospect prospect) throws IOException {
        ProposalIchraIntake intake = ProposalIchraIntakeDAO.findByProposalId(em, proposal.getId());
        Integer planYear = intake != null ? intake.getPlanYear() : null;
        if (planYear == null) {
            writePlainError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot generate Employer CDH Plan file: proposal " + proposal.getId()
                            + " has no plan year on its ICHRA intake.");
            return;
        }

        Integer templateId = parsePositiveInt(AppConfig.get("SUMMIT_ICHRA_PLAN_TEMPLATE_ID"));
        if (templateId == null) {
            log.error("[SUMMIT-EXPORT] SUMMIT_ICHRA_PLAN_TEMPLATE_ID is absent or non-numeric on this installation");
            writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot generate Employer CDH Plan file: this installation has no valid"
                            + " SUMMIT_ICHRA_PLAN_TEMPLATE_ID configured. Set it in ssa.properties"
                            + " to the Summit-assigned Plan Template ID before generating this file.");
            return;
        }

        String planYearBegin = LocalDate.of(planYear, 1, 1).format(SUMMIT_DATE);
        String planYearEnd = LocalDate.of(planYear, 12, 31).format(SUMMIT_DATE);
        String importPlanId = prospect.getId() + "-ICHRA-" + planYear;

        String line = String.join("|",
                String.valueOf(templateId),
                sanitize("ICHRA " + planYear),
                sanitize(importPlanId),
                sanitize("ICHRA Plan " + planYear + " for " + prospect.getName()),
                planYearBegin,
                String.valueOf(prospect.getId()),
                planYearBegin,
                planYearEnd);

        String filename = "employer-cdh-plan-" + sanitizeFilename(prospect.getName())
                + "-" + prospect.getId() + "-" + LocalDate.now().format(SUMMIT_DATE) + ".txt";
        writeFile(response, filename, line);
    }

    private void writeFile(HttpServletResponse response, String filename, String line) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        PrintWriter out = response.getWriter();
        out.print(line);
        out.print("\n");
        out.flush();
    }

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(status);
        response.getWriter().write(message);
    }

    /**
     * Pipe is the file delimiter, so no emitted value may carry one — and none may carry an
     * embedded newline or carriage return either, since that would silently fabricate an
     * extra record. One shared helper for every free-text field rather than a per-field rule.
     */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("|", "").replace("\r", "").replace("\n", "");
    }

    private static String sanitizeFilename(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }

    private static Integer parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Returns the label of the first null-or-blank value in the label/value pairs, or null if none. */
    private static String firstBlank(String... labelsAndValues) {
        for (int i = 0; i < labelsAndValues.length; i += 2) {
            String label = labelsAndValues[i];
            String value = labelsAndValues[i + 1];
            if (value == null || value.isBlank()) {
                return label;
            }
        }
        return null;
    }
}

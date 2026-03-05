package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationFieldValue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/ExportApplicationCsv")
public class ExportApplicationCsv extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || !local.isAuthenticated()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String proposalIdParam = request.getParameter("proposalId");
        String opportunityIdParam = request.getParameter("opportunityId");

        if ((proposalIdParam == null || proposalIdParam.isBlank())
                && (opportunityIdParam == null || opportunityIdParam.isBlank())) {
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Either proposalId or opportunityId is required.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long pspId = local.getCurrentPerson().getPsp().getId();

            List<Proposal> proposals;
            String filename;
            Opportunity opp = null;

            if (proposalIdParam != null && !proposalIdParam.isBlank()) {
                long proposalId = Long.parseLong(proposalIdParam);
                Proposal p = loadProposalWithDetails(em, proposalId);
                if (p == null) {
                    response.setContentType("text/plain");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("Proposal not found.");
                    return;
                }
                proposals = List.of(p);
                // Try to load opportunity for contact/agency info
                if (p.getSourceActivity() != null) {
                    try { opp = em.find(Opportunity.class, p.getSourceActivity().getId()); } catch (Exception ignored) {}
                }
                String prospectName = p.getProspect() != null ? sanitizeFilename(p.getProspect().getName()) : "unknown";
                filename = "application_export_" + prospectName + "_" + proposalId + ".csv";
            } else {
                long opportunityId = Long.parseLong(opportunityIdParam);
                opp = em.find(Opportunity.class, opportunityId);
                if (opp == null) {
                    response.setContentType("text/plain");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("Opportunity not found.");
                    return;
                }
                proposals = loadProposalsByOpportunity(em, opportunityId);
                if (proposals.isEmpty()) {
                    response.setContentType("text/plain");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("No proposals found for this opportunity.");
                    return;
                }
                String prospectName = opp.getProspect() != null ? sanitizeFilename(opp.getProspect().getName()) : "unknown";
                filename = "application_export_" + prospectName + "_opp" + opportunityId + ".csv";
            }

            // Load master field list for column headers
            List<ApplicationField> masterFields = loadMasterFields(em, pspId);

            // Load field values for each application
            Map<Long, Map<String, String>> fieldValueMaps = new LinkedHashMap<>();
            for (Proposal p : proposals) {
                if (p.getApplication() != null) {
                    List<ApplicationFieldValue> values = loadFieldValues(em, p.getApplication());
                    Map<String, String> valueMap = new LinkedHashMap<>();
                    for (ApplicationFieldValue fv : values) {
                        if (fv.getApplicationField() != null) {
                            valueMap.put(fv.getApplicationField().getFieldKey(), fv.getFieldValue());
                        }
                    }
                    fieldValueMaps.put(p.getId(), valueMap);
                }
            }

            // Write CSV response
            response.setContentType("text/csv");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            PrintWriter out = response.getWriter();

            // Header row
            List<String> headers = new ArrayList<>();
            headers.add("Prospect Name");
            headers.add("Contact First Name");
            headers.add("Contact Last Name");
            headers.add("Contact Email");
            headers.add("Contact Phone");
            headers.add("Agency Name");
            headers.add("Agent Name");
            headers.add("Proposal ID");
            headers.add("Proposal Status");
            headers.add("Proposal Created Date");
            headers.add("Application Status");
            headers.add("Application Submitted Date");
            headers.add("Services");
            headers.add("Rate Name");
            for (ApplicationField f : masterFields) {
                headers.add(f.getLabel());
            }
            out.println(headers.stream().map(this::escapeCsv).collect(Collectors.joining(",")));

            // Data rows
            for (Proposal p : proposals) {
                List<String> row = new ArrayList<>();

                // Prospect name
                row.add(p.getProspect() != null ? p.getProspect().getName() : "");

                // Contact info — from prospect's contact person
                String contactFirst = "";
                String contactLast = "";
                String contactEmail = "";
                String contactPhone = "";
                Person contact = null;
                if (p.getProspect() != null && p.getProspect().getContact() != null) {
                    contact = p.getProspect().getContact();
                } else if (opp != null && opp.getPrimaryContact() != null) {
                    contact = opp.getPrimaryContact();
                }
                if (contact != null) {
                    contactFirst = contact.getFirstName() != null ? contact.getFirstName() : "";
                    contactLast = contact.getLastName() != null ? contact.getLastName() : "";
                    contactEmail = contact.getEmail() != null ? contact.getEmail() : "";
                    contactPhone = contact.getPhone() != null ? contact.getPhone() : "";
                }
                row.add(contactFirst);
                row.add(contactLast);
                row.add(contactEmail);
                row.add(contactPhone);

                // Agency name — from opportunity if available
                String agencyName = "";
                if (opp != null && opp.getAgency() != null) {
                    agencyName = opp.getAgency().getName() != null ? opp.getAgency().getName() : "";
                }
                row.add(agencyName);

                // Agent name
                String agentName = "";
                if (p.getProspect() != null && p.getProspect().getAgent() != null) {
                    agentName = p.getProspect().getAgent().getFullName() != null ? p.getProspect().getAgent().getFullName() : "";
                }
                row.add(agentName);

                // Proposal metadata
                row.add(String.valueOf(p.getId()));
                row.add(p.getStatus() != null ? p.getStatus() : "");
                row.add(p.getDateCreated() != null ? p.getDateCreated().toString() : "");

                // Application metadata
                if (p.getApplication() != null) {
                    row.add(p.getApplication().getStatus() != null ? p.getApplication().getStatus() : "");
                    row.add(p.getApplication().getDateSubmitted() != null ? p.getApplication().getDateSubmitted().toString() : "");
                } else {
                    row.add("");
                    row.add("");
                }

                // Services (LOS names, pipe-delimited)
                String services = "";
                if (p.getLosList() != null) {
                    services = p.getLosList().stream()
                            .map(los -> los.getShortText() != null ? los.getShortText() : "")
                            .collect(Collectors.joining("|"));
                }
                row.add(services);

                // Rate name
                row.add(p.getRate() != null && p.getRate().getDescription() != null ? p.getRate().getDescription() : "");

                // Field values
                Map<String, String> valueMap = fieldValueMaps.getOrDefault(p.getId(), Collections.emptyMap());
                for (ApplicationField f : masterFields) {
                    row.add(valueMap.getOrDefault(f.getFieldKey(), ""));
                }

                out.println(row.stream().map(this::escapeCsv).collect(Collectors.joining(",")));
            }

            out.flush();

        } catch (NumberFormatException e) {
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid ID parameter.");
        } finally {
            em.close();
        }
    }

    private Proposal loadProposalWithDetails(EntityManager em, long proposalId) {
        try {
            Query q = em.createQuery(
                    "SELECT p FROM Proposal p " +
                    "LEFT JOIN FETCH p.application " +
                    "LEFT JOIN FETCH p.prospect pr " +
                    "WHERE p.id = :proposalId");
            q.setParameter("proposalId", proposalId);
            Proposal p = (Proposal) q.getSingleResult();
            // Touch lazy fields
            if (p.getLosList() != null) p.getLosList().size();
            if (p.getProspect() != null) {
                if (p.getProspect().getAgent() != null) p.getProspect().getAgent().getFullName();
                if (p.getProspect().getContact() != null) p.getProspect().getContact().getFirstName();
            }
            if (p.getRate() != null) p.getRate().getDescription();
            return p;
        } catch (NoResultException e) {
            return null;
        }
    }

    private List<Proposal> loadProposalsByOpportunity(EntityManager em, long opportunityId) {
        Query q = em.createQuery(
                "SELECT p FROM Proposal p " +
                "LEFT JOIN FETCH p.application " +
                "LEFT JOIN FETCH p.prospect pr " +
                "WHERE p.sourceActivity.id = :activityId AND p.isInactive = false");
        q.setParameter("activityId", opportunityId);
        List<Proposal> proposals = (List<Proposal>) q.getResultList();

        // Touch lazy fields to avoid issues outside EM scope
        for (Proposal p : proposals) {
            if (p.getLosList() != null) p.getLosList().size();
            if (p.getProspect() != null) {
                if (p.getProspect().getAgent() != null) p.getProspect().getAgent().getFullName();
                if (p.getProspect().getContact() != null) p.getProspect().getContact().getFirstName();
            }
            if (p.getRate() != null) p.getRate().getDescription();
        }

        return proposals;
    }

    private List<ApplicationField> loadMasterFields(EntityManager em, long pspId) {
        Query q = em.createQuery(
                "SELECT f FROM ApplicationField f " +
                "JOIN f.applicationSection s " +
                "WHERE s.psp.id = :pspId AND f.suppressed = false AND s.suppressed = false " +
                "ORDER BY s.sortOrder, f.sortOrder");
        q.setParameter("pspId", pspId);
        return (List<ApplicationField>) q.getResultList();
    }

    private List<ApplicationFieldValue> loadFieldValues(EntityManager em, Application app) {
        Query q = em.createQuery(
                "SELECT fv FROM ApplicationFieldValue fv " +
                "JOIN FETCH fv.applicationField " +
                "WHERE fv.application = :app");
        q.setParameter("app", app);
        return (List<ApplicationFieldValue>) q.getResultList();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}

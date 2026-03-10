package net.superiorstate.ams.controller.activity.setup;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.application.*;
import net.superiorstate.ams.model.sales.offering.BenefitType;
import net.superiorstate.ams.model.sales.offering.BillingType;
import net.superiorstate.ams.model.sales.offering.LOS;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "ApplyForProposal", value = "/apply/*")
public class ApplyForProposal extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = extractGuid(request, response);
        if (guid == null) return;

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Proposal proposal = loadProposal(em, guid);
            if (proposal == null || proposal.isInactive()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Get LOS IDs from the proposal
            List<Long> losIds = proposal.getLosList().stream()
                    .map(LOS::getId).collect(Collectors.toList());

            // Load matching sections: scope=ALL, or scope=LOS with overlapping LOSs
            // Exclude suppressed sections and fields
            Query sq = em.createQuery(
                    "SELECT DISTINCT s FROM ApplicationSection s " +
                            "LEFT JOIN FETCH s.fieldList " +
                            "LEFT JOIN s.losList los " +
                            "WHERE s.suppressed = false AND (s.scope = 'ALL' OR los.id IN :losIds) " +
                            "ORDER BY s.sortOrder");
            sq.setParameter("losIds", losIds);
            List<ApplicationSection> sections = sq.getResultList();

            // EclipseLink DISTINCT + JOIN FETCH can scramble @OrderBy — re-sort and remove suppressed fields
            for (ApplicationSection sec : sections) {
                if (sec.getFieldList() != null) {
                    sec.getFieldList().removeIf(ApplicationField::isSuppressed);
                    sec.getFieldList().sort(java.util.Comparator.comparingInt(ApplicationField::getSortOrder));
                }
            }

            // PSP branding
            String primaryColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY");
            String accentColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT");
            if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#2B5F8A";
            if (accentColor == null || accentColor.isEmpty()) accentColor = "#7AB648";

            String pspName = "";
            if (proposal.getProspect().getContact().getPsp() != null) {
                pspName = proposal.getProspect().getContact().getPsp().getFullName();
            }

            request.setAttribute("proposal", proposal);
            request.setAttribute("sections", sections);
            request.setAttribute("primaryColor", primaryColor);
            request.setAttribute("accentColor", accentColor);
            request.setAttribute("pspName", pspName);
            Map<String, net.superiorstate.ams.model.general.IrsLimit> irsLimits = SalesDAO.getIrsLimits(em);
            request.setAttribute("irsLimits", irsLimits);
            // Build default values map from prospect data
            Map<String, String> defaults = new java.util.HashMap<>();
            Person contact = proposal.getProspect().getContact();
            if (contact != null) {
                if (contact.getFirstName() != null) defaults.put("contact_first_name", contact.getFirstName());
                if (contact.getLastName() != null) defaults.put("contact_last_name", contact.getLastName());
                if (contact.getEmail() != null) defaults.put("contact_email", contact.getEmail());
                if (contact.getPhone() != null) defaults.put("contact_phone", contact.getPhone());
                if (contact.getTitle() != null) defaults.put("contact_title", contact.getTitle());
                if (contact.getAddress() != null) {
                    Address addr = contact.getAddress();
                    if (addr.getAddress1() != null) defaults.put("address_street1", addr.getAddress1());
                    if (addr.getAddress2() != null) defaults.put("address_street2", addr.getAddress2());
                    if (addr.getCity() != null) defaults.put("address_city", addr.getCity());
                    if (addr.getState() != null) defaults.put("address_state", addr.getState());
                    if (addr.getZipCode() != null) defaults.put("address_zip", addr.getZipCode());
                }
            }
            if (proposal.getProspect().getName() != null) {
                defaults.put("company_name", proposal.getProspect().getName());
            }
            request.setAttribute("defaults", defaults);

            // Load previously saved field values (if application exists)
            List<ApplicationFieldValue> savedValues = new ArrayList<>();
            try {
                Query appQ = em.createQuery(
                        "SELECT fv FROM ApplicationFieldValue fv " +
                                "JOIN FETCH fv.applicationField " +
                                "WHERE fv.application.proposal.id = :proposalId");
                appQ.setParameter("proposalId", proposal.getId());
                savedValues = appQ.getResultList();
            } catch (Exception e) {
                // No saved values yet — that's fine
            }
            if (!savedValues.isEmpty()) {
                Map<String, String> saved = new java.util.HashMap<>();
                for (ApplicationFieldValue fv : savedValues) {
                    saved.put(fv.getApplicationField().getFieldKey(), fv.getFieldValue());
                }
                for (Map.Entry<String, String> entry : saved.entrySet()) {
                    defaults.put(entry.getKey(), entry.getValue());
                }
                if (saved.containsKey("bill_benefit_plans")) {
                    defaults.put("bill_benefit_plans_escaped",
                            saved.get("bill_benefit_plans").replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;"));
                }
            }
            List<BenefitType> benefitTypes = em.createQuery(
                            "SELECT bt FROM BenefitType bt WHERE bt.psp.id = :pspId ORDER BY bt.sortOrder", BenefitType.class)
                    .setParameter("pspId", (long) proposal.getProspect().getContact().getPsp().getId())
                    .getResultList();
            List<BillingType> billingTypes = em.createQuery(
                            "SELECT bt FROM BillingType bt WHERE bt.psp.id = :pspId ORDER BY bt.sortOrder", BillingType.class)
                    .setParameter("pspId", (long) proposal.getProspect().getContact().getPsp().getId())
                    .getResultList();
            request.setAttribute("benefitTypes", benefitTypes);
            request.setAttribute("billingTypes", billingTypes);
        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/applyForProposal.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = extractGuid(request, response);
        if (guid == null) return;

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Proposal proposal = loadProposal(em, guid);
            if (proposal == null || proposal.isInactive()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Create or get existing Application
            Application application = proposal.getApplication();
            if (application == null) {
                em.getTransaction().begin();
                application = new Application();
                application.setProposal(proposal);
                application.setStatus("IN_PROGRESS");
                application.setDateStarted(Timestamp.from(Instant.now()));
                em.persist(application);
                em.getTransaction().commit();
            }

            // Get all field keys for sections that apply to this proposal
            List<Long> losIds = proposal.getLosList().stream()
                    .map(LOS::getId).collect(Collectors.toList());

            Query fq = em.createQuery(
                    "SELECT DISTINCT f FROM ApplicationField f " +
                            "JOIN f.applicationSection s " +
                            "LEFT JOIN s.losList los " +
                            "WHERE f.suppressed = false AND s.suppressed = false " +
                            "AND (s.scope = 'ALL' OR los.id IN :losIds)");
            fq.setParameter("losIds", losIds);
            List<ApplicationField> fields = fq.getResultList();

            // Save field values
            em.getTransaction().begin();
            for (ApplicationField field : fields) {
                String paramValue = request.getParameter(field.getFieldKey());

                // Handle CHECKBOX — multiple values
                if ("CHECKBOX".equals(field.getFieldType())) {
                    String[] values = request.getParameterValues(field.getFieldKey());
                    paramValue = (values != null) ? String.join("|", values) : null;
                }

                if (paramValue != null && !paramValue.trim().isEmpty()) {
                    // Check if value already exists (re-submission)
                    Query existQ = em.createQuery(
                            "SELECT v FROM ApplicationFieldValue v " +
                                    "WHERE v.application = :app AND v.applicationField = :field");
                    existQ.setParameter("app", application);
                    existQ.setParameter("field", field);
                    List<ApplicationFieldValue> existing = existQ.getResultList();

                    if (!existing.isEmpty()) {
                        existing.get(0).setFieldValue(paramValue.trim());
                        em.persist(existing.get(0));
                    } else {
                        ApplicationFieldValue fv = new ApplicationFieldValue();
                        fv.setApplication(application);
                        fv.setApplicationField(field);
                        fv.setFieldValue(paramValue.trim());
                        em.persist(fv);
                    }
                }
            }

            // Update statuses
            application.setStatus("SUBMITTED");
            application.setDateSubmitted(Timestamp.from(Instant.now()));
            em.persist(application);

            proposal.setStatus("APPLIED");
            proposal.setDateApplied(Timestamp.from(Instant.now()));
            em.persist(proposal);

            em.getTransaction().commit();

            // Redirect to confirmation
            request.setAttribute("pspName", proposal.getProspect().getContact().getPsp() != null
                    ? proposal.getProspect().getContact().getPsp().getFullName() : "");
            request.setAttribute("prospectName", proposal.getProspect().getName());
            String primaryColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY");
            String accentColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT");
            if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#2B5F8A";
            if (accentColor == null || accentColor.isEmpty()) accentColor = "#7AB648";
            request.setAttribute("primaryColor", primaryColor);
            request.setAttribute("accentColor", accentColor);

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/applicationConfirmation.jsp");
        dispatcher.forward(request, response);
    }

    private String extractGuid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return pathInfo.substring(1);
    }

    private Proposal loadProposal(EntityManager em, String guid) {
        Query q = em.createQuery("SELECT DISTINCT p FROM Proposal p LEFT JOIN FETCH p.losList LEFT JOIN FETCH p.application WHERE p.applicationGUID = :guid");
        q.setParameter("guid", guid);
        List<Proposal> results = q.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
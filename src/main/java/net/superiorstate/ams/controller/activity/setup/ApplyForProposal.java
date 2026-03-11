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
import net.superiorstate.ams.model.sales.offering.Enhancement;
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

            // Full proposal LOS list
            List<LOS> proposalLosList = proposal.getLosList();
            List<Long> proposalLosIds = proposalLosList.stream()
                    .map(LOS::getId).collect(Collectors.toList());

            // Load enhancements linked to any of the proposal's LOSs
            List<Enhancement> proposalEnhancements = new ArrayList<>();
            if (!proposalLosIds.isEmpty()) {
                proposalEnhancements = em.createQuery(
                        "SELECT DISTINCT e FROM Enhancement e LEFT JOIN FETCH e.losList " +
                                "JOIN e.losList l WHERE l.id IN :losIds AND e.suppressed = false ORDER BY e.sortOrder",
                        Enhancement.class)
                        .setParameter("losIds", proposalLosIds)
                        .getResultList();
            }

            // Check if Application exists and has service selections (use em.find to bypass stale cache)
            Application application = em.find(Application.class, proposal.getId());
            boolean showServiceSelection = false;
            List<Long> effectiveLosIds;
            List<Long> effectiveEnhIds;

            if (application != null && application.hasServiceSelections()) {
                // User already made selections — use them
                effectiveLosIds = application.getSelectedLosIdList();
                effectiveEnhIds = application.getSelectedEnhancementIdList();
            } else if (proposalLosList.size() <= 1 && proposalEnhancements.isEmpty()) {
                // Single LOS, no enhancements — auto-select, no selection step needed
                effectiveLosIds = proposalLosIds;
                effectiveEnhIds = Collections.emptyList();

                // Persist auto-selection so it's recorded
                if (application == null) {
                    em.getTransaction().begin();
                    application = new Application();
                    application.setProposal(proposal);
                    application.setStatus("IN_PROGRESS");
                    application.setDateStarted(Timestamp.from(Instant.now()));
                    application.setSelectedLosIds(proposalLosIds.stream()
                            .map(String::valueOf).collect(Collectors.joining(",")));
                    application.setSelectedEnhancementIds("");
                    em.persist(application);
                    em.getTransaction().commit();
                } else if (!application.hasServiceSelections()) {
                    em.getTransaction().begin();
                    application.setSelectedLosIds(proposalLosIds.stream()
                            .map(String::valueOf).collect(Collectors.joining(",")));
                    application.setSelectedEnhancementIds("");
                    em.merge(application);
                    em.getTransaction().commit();
                }
            } else if (application == null || !application.hasServiceSelections()) {
                // Multiple LOSs or enhancements — show selection step
                showServiceSelection = true;
                effectiveLosIds = proposalLosIds; // won't be used for section query since selection is shown
                effectiveEnhIds = Collections.emptyList();
            } else {
                // Fallback — use full proposal LOS list (backward compat for pre-V045 apps)
                effectiveLosIds = proposalLosIds;
                effectiveEnhIds = Collections.emptyList();
            }

            // Pass selection-related attributes to JSP
            request.setAttribute("proposalLos", proposalLosList);
            request.setAttribute("proposalEnhancements", proposalEnhancements);
            request.setAttribute("showServiceSelection", showServiceSelection);
            request.setAttribute("selectedLosIds", application != null ? application.getSelectedLosIds() : "");
            request.setAttribute("selectedEnhancementIds", application != null ? application.getSelectedEnhancementIds() : "");

            // Build enhancementLosMap JSON for cascade logic
            StringBuilder enhLosJson = new StringBuilder("{");
            boolean first = true;
            for (Enhancement enh : proposalEnhancements) {
                if (!first) enhLosJson.append(",");
                first = false;
                enhLosJson.append("\"").append(enh.getId()).append("\":[");
                if (enh.getLosList() != null) {
                    enhLosJson.append(enh.getLosList().stream()
                            .map(l -> String.valueOf(l.getId()))
                            .collect(Collectors.joining(",")));
                }
                enhLosJson.append("]");
            }
            enhLosJson.append("}");
            request.setAttribute("enhancementLosMapJson", enhLosJson.toString());

            // Load sections (only if not showing selection step)
            List<ApplicationSection> sections = Collections.emptyList();
            if (!showServiceSelection) {
                sections = new ArrayList<>(loadSections(em, effectiveLosIds, effectiveEnhIds));

                // Filter out enhancement-gated sections:
                // If a section is linked to any enhancement (via applicationsectionenhancement),
                // only show it if at least one of those enhancements was selected.
                if (!proposalEnhancements.isEmpty()) {
                    Set<Long> proposalEnhIds = proposalEnhancements.stream()
                            .map(Enhancement::getId).collect(Collectors.toSet());
                    Set<Long> selectedEnhSet = new HashSet<>(effectiveEnhIds);

                    sections.removeIf(sec -> {
                        if ("ALL".equals(sec.getScope())) return false;

                        // Get this section's direct enhancement links
                        List<Long> secEnhIds;
                        try {
                            secEnhIds = em.createQuery(
                                            "SELECT e.id FROM ApplicationSection s JOIN s.enhancementList e WHERE s.id = :sId", Long.class)
                                    .setParameter("sId", sec.getId())
                                    .getResultList();
                        } catch (Exception e) {
                            return false; // keep section on error
                        }

                        // If section has no enhancement links, keep it (plain LOS-scoped section)
                        if (secEnhIds.isEmpty()) return false;

                        // If section is linked to a proposal enhancement, require it to be selected
                        boolean linkedToProposalEnh = secEnhIds.stream().anyMatch(proposalEnhIds::contains);
                        if (!linkedToProposalEnh) return false; // linked to other enhancements, not relevant — keep

                        // Section IS linked to a proposal enhancement — only keep if that enhancement was selected
                        return secEnhIds.stream().noneMatch(selectedEnhSet::contains);
                    });
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

            // ── Service Selection action ──
            String action = request.getParameter("action");
            if ("selectServices".equals(action)) {
                // Create or get existing Application (use em.find to bypass stale cache)
                Application application = em.find(Application.class, proposal.getId());
                if (application == null) {
                    em.getTransaction().begin();
                    application = new Application();
                    application.setProposal(proposal);
                    application.setStatus("IN_PROGRESS");
                    application.setDateStarted(Timestamp.from(Instant.now()));
                    em.persist(application);
                    em.getTransaction().commit();
                }

                // Parse selected LOS IDs from checkboxes
                String[] losIdParams = request.getParameterValues("selectedLos");
                String losIdsCsv = (losIdParams != null)
                        ? String.join(",", losIdParams) : "";

                // Parse selected Enhancement IDs from checkboxes
                String[] enhIdParams = request.getParameterValues("selectedEnh");
                String enhIdsCsv = (enhIdParams != null)
                        ? String.join(",", enhIdParams) : "";

                // Validate: at least one LOS must be selected
                if (losIdsCsv.isBlank()) {
                    request.setAttribute("selectionError", "Please select at least one service to continue.");
                    doGet(request, response);
                    return;
                }

                // Persist selections
                em.getTransaction().begin();
                application.setSelectedLosIds(losIdsCsv);
                application.setSelectedEnhancementIds(enhIdsCsv);
                em.merge(application);
                em.getTransaction().commit();

                // PRG — redirect back to GET so form renders with selected sections
                response.sendRedirect(request.getRequestURI());
                return;
            }

            // ── Application Submit action ──

            // Create or get existing Application (use em.find to bypass stale cache)
            Application application = em.find(Application.class, proposal.getId());
            if (application == null) {
                em.getTransaction().begin();
                application = new Application();
                application.setProposal(proposal);
                application.setStatus("IN_PROGRESS");
                application.setDateStarted(Timestamp.from(Instant.now()));
                em.persist(application);
                em.getTransaction().commit();
            }

            // Use selected LOS/Enhancement IDs if available, fall back to full proposal LOS list
            List<Long> losIds;
            List<Long> enhIds;
            if (application.hasServiceSelections()) {
                losIds = application.getSelectedLosIdList();
                enhIds = application.getSelectedEnhancementIdList();
            } else {
                losIds = proposal.getLosList().stream()
                        .map(LOS::getId).collect(Collectors.toList());
                enhIds = Collections.emptyList();
            }

            List<ApplicationField> fields = loadFields(em, losIds, enhIds);

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

    // ======================== Shared Helpers ========================

    /** Load application sections matching the given LOS and Enhancement IDs. */
    private List<ApplicationSection> loadSections(EntityManager em, List<Long> losIds, List<Long> enhIds) {
        List<Long> safeLosIds = (losIds == null || losIds.isEmpty()) ? List.of(-1L) : losIds;
        List<Long> safeEnhIds = (enhIds == null || enhIds.isEmpty()) ? List.of(-1L) : enhIds;

        Query sq = em.createQuery(
                "SELECT DISTINCT s FROM ApplicationSection s " +
                        "LEFT JOIN FETCH s.fieldList " +
                        "LEFT JOIN s.losList los " +
                        "LEFT JOIN s.enhancementList enh " +
                        "WHERE s.suppressed = false AND (s.scope = 'ALL' OR los.id IN :losIds OR enh.id IN :enhIds) " +
                        "ORDER BY s.sortOrder");
        sq.setParameter("losIds", safeLosIds);
        sq.setParameter("enhIds", safeEnhIds);
        List<ApplicationSection> sections = sq.getResultList();

        // EclipseLink DISTINCT + JOIN FETCH can scramble @OrderBy — re-sort and remove suppressed fields
        for (ApplicationSection sec : sections) {
            if (sec.getFieldList() != null) {
                sec.getFieldList().removeIf(ApplicationField::isSuppressed);
                sec.getFieldList().sort(java.util.Comparator.comparingInt(ApplicationField::getSortOrder));
            }
        }
        return sections;
    }

    /** Load application fields matching the given LOS and Enhancement IDs. */
    private List<ApplicationField> loadFields(EntityManager em, List<Long> losIds, List<Long> enhIds) {
        List<Long> safeLosIds = (losIds == null || losIds.isEmpty()) ? List.of(-1L) : losIds;
        List<Long> safeEnhIds = (enhIds == null || enhIds.isEmpty()) ? List.of(-1L) : enhIds;

        Query fq = em.createQuery(
                "SELECT DISTINCT f FROM ApplicationField f " +
                        "JOIN f.applicationSection s " +
                        "LEFT JOIN s.losList los " +
                        "LEFT JOIN s.enhancementList enh " +
                        "WHERE f.suppressed = false AND s.suppressed = false " +
                        "AND (s.scope = 'ALL' OR los.id IN :losIds OR enh.id IN :enhIds)");
        fq.setParameter("losIds", safeLosIds);
        fq.setParameter("enhIds", safeEnhIds);
        return fq.getResultList();
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

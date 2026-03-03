package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.RateTable;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.Feature;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.MarketingMaterial;
import net.superiorstate.ams.model.sales.offering.ProposalSection;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "ViewProposal", value = "/proposal/*")
public class ViewProposal extends HttpServlet {

    /** Matches [link text](resourceId) markers in feature descriptions */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\((\\d+)\\)");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String guid = pathInfo.substring(1);

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Load proposal with LOSs
            Query q = em.createQuery("SELECT DISTINCT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.applicationGUID = :guid");
            q.setParameter("guid", guid);
            List<Proposal> results = q.getResultList();
            if (results.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Proposal proposal = results.get(0);

            if (proposal.isInactive()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Update status to VIEWED on first access
            if ("SENT".equals(proposal.getStatus()) && proposal.getDateViewed() == null) {
                em.getTransaction().begin();
                proposal.setStatus("VIEWED");
                proposal.setDateViewed(Timestamp.from(Instant.now()));
                em.persist(proposal);
                em.getTransaction().commit();
            }

            // Load pricing
            List<RateTable> pricing = SalesDAO.getPricing(em, proposal);

            // Load features with library resources eagerly fetched
            List<Long> moduleIds = pricing.stream()
                    .map(rt -> rt.getModule().getId())
                    .distinct()
                    .toList();

            List<Feature> features = List.of();
            if (!moduleIds.isEmpty()) {
                Query fq = em.createQuery(
                        "SELECT f FROM Feature f LEFT JOIN FETCH f.libraryResource WHERE f.serviceModule.id IN :moduleIds ORDER BY f.serviceModule.sortOrder, f.sortOrder");
                fq.setParameter("moduleIds", moduleIds);
                features = fq.getResultList();
            }

            // Get PSP name for storage URLs
            String pspName = proposal.getProspect().getContact().getPsp() != null
                    ? proposal.getProspect().getContact().getPsp().getFullName() : "default";

            // Build a map of resourceId → download URL for all referenced resources
            // Collect all resource IDs from inline links and libraryResource FKs
            Set<Long> resourceIds = new HashSet<>();
            for (Feature f : features) {
                // From inline link markers
                Matcher m = LINK_PATTERN.matcher(f.getDescription());
                while (m.find()) {
                    try { resourceIds.add(Long.parseLong(m.group(2))); } catch (NumberFormatException ignored) {}
                }
                // From end-icon libraryResource
                if (f.getLibraryResource() != null) {
                    resourceIds.add(f.getLibraryResource().getId());
                }
            }

            // Load referenced resources and build URL map
            Map<Long, String> resourceUrlMap = new HashMap<>();
            Map<Long, String> resourceTypeMap = new HashMap<>();
            for (Long resId : resourceIds) {
                MarketingMaterial mat = em.find(MarketingMaterial.class, resId);
                if (mat != null) {
                    String url = getResourceUrl(mat, pspName, em);
                    if (url != null) {
                        resourceUrlMap.put(resId, url);
                    }
                    resourceTypeMap.put(resId, mat.getMaterialType());
                }
            }

            // Build rendered feature HTML map: featureId → rendered HTML string
            Map<Long, String> renderedFeatures = new LinkedHashMap<>();
            for (Feature f : features) {
                String html = renderFeatureHtml(f, resourceUrlMap, resourceTypeMap);
                renderedFeatures.put(f.getId(), html);
            }

            // Get PSP branding colors
            String primaryColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY");
            String accentColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT");
            if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#2B5F8A";
            if (accentColor == null || accentColor.isEmpty()) accentColor = "#7AB648";

            request.setAttribute("proposal", proposal);
            request.setAttribute("pricing", pricing);
            request.setAttribute("features", features);
            request.setAttribute("renderedFeatures", renderedFeatures);
            request.setAttribute("primaryColor", primaryColor);
            request.setAttribute("accentColor", accentColor);
            request.setAttribute("pspName", pspName);

            // Section-based rendering: load active ProposalSections for this PSP
            PSP psp = proposal.getProspect().getContact().getPsp();
            if (psp != null) {
                List<ProposalSection> sections = em.createQuery(
                                "SELECT s FROM ProposalSection s WHERE s.psp.id = :pspId AND s.active = true ORDER BY s.sortOrder",
                                ProposalSection.class)
                        .setParameter("pspId", psp.getId())
                        .getResultList();

                // Force-init M:N collections for scope filtering
                for (ProposalSection s : sections) {
                    if (s.getLosList() != null) s.getLosList().size();
                    if (s.getEnhancementList() != null) s.getEnhancementList().size();
                }

                // Scope filtering — remove SCOPED sections that don't match the proposal
                Set<Long> proposalLosIds = new HashSet<>();
                if (proposal.getLosList() != null) {
                    for (LOS los : proposal.getLosList()) {
                        proposalLosIds.add(los.getId());
                    }
                }

                Set<Long> proposalEnhIds = new HashSet<>();
                for (RateTable rt : pricing) {
                    if (rt.getModule() != null && rt.getModule().getEnhancement() != null) {
                        proposalEnhIds.add(rt.getModule().getEnhancement().getId());
                    }
                }

                List<ProposalSection> filteredSections = new ArrayList<>();
                for (ProposalSection section : sections) {
                    if (!"SCOPED".equals(section.getScope())) {
                        filteredSections.add(section);
                        continue;
                    }
                    // SCOPED — check if any linked LOS or Enhancement matches
                    boolean matches = false;
                    if (section.getLosList() != null) {
                        for (LOS los : section.getLosList()) {
                            if (proposalLosIds.contains(los.getId())) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (!matches && section.getEnhancementList() != null) {
                        for (Enhancement enh : section.getEnhancementList()) {
                            if (proposalEnhIds.contains(enh.getId())) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (matches) {
                        filteredSections.add(section);
                    }
                }
                sections = filteredSections;

                if (!sections.isEmpty()) {
                    // Build token replacement map
                    Map<String, String> tokens = buildTokenMap(proposal, psp, primaryColor, accentColor, request);

                    // Build rendered HTML map for sections with content
                    Map<Long, String> sectionHtml = new LinkedHashMap<>();
                    for (ProposalSection section : sections) {
                        String type = section.getSectionType();
                        if (("TITLE".equals(type) || "CLOSING".equals(type) || "CUSTOM".equals(type))
                                && section.getHtmlContent() != null) {
                            sectionHtml.put(section.getId(), replaceTokens(section.getHtmlContent(), tokens));
                        }
                    }

                    request.setAttribute("proposalSections", sections);
                    request.setAttribute("sectionHtml", sectionHtml);
                }
            }

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/viewProposal.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * Renders a feature description to HTML:
     * - Converts [text](resourceId) markers to <a> tags
     * - Appends an end-icon link if libraryResource is set
     */
    private String renderFeatureHtml(Feature feature, Map<Long, String> urlMap, Map<Long, String> typeMap) {
        String desc = escapeHtml(feature.getDescription());

        // Replace inline link markers: [text](id) → <a href="url" target="_blank">text</a>
        Matcher m = LINK_PATTERN.matcher(feature.getDescription());
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String linkText = escapeHtml(m.group(1));
            long resId = Long.parseLong(m.group(2));
            String url = urlMap.get(resId);
            if (url != null) {
                m.appendReplacement(sb, "<a href=\"" + escapeHtml(url) + "\" target=\"_blank\" style=\"color: inherit; text-decoration: underline;\">" + linkText + "</a>");
            } else {
                m.appendReplacement(sb, linkText);
            }
        }
        m.appendTail(sb);
        desc = sb.toString();

        // Append end-icon if libraryResource is set
        if (feature.getLibraryResource() != null) {
            Long resId = feature.getLibraryResource().getId();
            String url = urlMap.get(resId);
            String type = typeMap.getOrDefault(resId, "DOCUMENT");
            if (url != null) {
                String icon = getIconForType(type, feature.getLibraryResource().getStorageGuid());
                desc += " <a href=\"" + escapeHtml(url) + "\" target=\"_blank\" title=\"" +
                        escapeHtml(feature.getLibraryResource().getTitle()) +
                        "\" style=\"color: inherit; font-size: 0.9em;\">" + icon + "</a>";
            }
        }

        return desc;
    }

    /**
     * Returns the appropriate URL for a MarketingMaterial:
     * - DOCUMENT: ShowFileUpload?doc=storageGuid
     * - VIDEO/LINK: the url field directly
     */
    private String getResourceUrl(MarketingMaterial mat, String pspName, EntityManager em) {
        if ("DOCUMENT".equals(mat.getMaterialType()) && mat.getStorageGuid() != null) {
            return "ShowFileUpload?doc=" + mat.getStorageGuid();
        } else if (mat.getUrl() != null && !mat.getUrl().isBlank()) {
            return mat.getUrl();
        }
        return null;
    }

    /** Returns a Bootstrap icon HTML snippet based on material type / file extension */
    private String getIconForType(String materialType, String storageGuid) {
        if ("VIDEO".equals(materialType)) {
            return "<i class=\"bi bi-camera-video-fill\"></i>";
        } else if ("LINK".equals(materialType)) {
            return "<i class=\"bi bi-box-arrow-up-right\"></i>";
        } else if (storageGuid != null) {
            String ext = storageGuid.contains(".") ? storageGuid.substring(storageGuid.lastIndexOf('.') + 1).toLowerCase() : "";
            return switch (ext) {
                case "pdf" -> "<i class=\"bi bi-file-earmark-pdf-fill\"></i>";
                case "xlsx" -> "<i class=\"bi bi-file-earmark-spreadsheet-fill\"></i>";
                case "docx" -> "<i class=\"bi bi-file-earmark-word-fill\"></i>";
                case "csv" -> "<i class=\"bi bi-file-earmark-spreadsheet\"></i>";
                default -> "<i class=\"bi bi-file-earmark-fill\"></i>";
            };
        }
        return "<i class=\"bi bi-file-earmark-fill\"></i>";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Builds the merge token map from proposal data */
    private Map<String, String> buildTokenMap(Proposal proposal, PSP psp, String primaryColor, String accentColor, HttpServletRequest request) {
        Map<String, String> tokens = new HashMap<>();

        // Prospect
        tokens.put("PROSPECT_NAME", proposal.getProspect().getName() != null ? proposal.getProspect().getName() : "");

        // Agent (createdBy)
        Person agent = proposal.getCreatedBy();
        if (agent != null) {
            String agentName = (agent.getFirstName() != null ? agent.getFirstName() : "") +
                    " " + (agent.getLastName() != null ? agent.getLastName() : "");
            tokens.put("AGENT_NAME", agentName.trim());
            tokens.put("AGENT_EMAIL", agent.getEmail() != null ? agent.getEmail() : "");
        } else {
            tokens.put("AGENT_NAME", "");
            tokens.put("AGENT_EMAIL", "");
        }

        // Agency/PSP
        tokens.put("AGENCY_NAME", psp.getFullName() != null ? psp.getFullName() : "");
        tokens.put("PSP_NAME", psp.getFullName() != null ? psp.getFullName() : "");

        // Date
        if (proposal.getDateCreated() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy");
            tokens.put("DATE_CREATED", sdf.format(proposal.getDateCreated()));
        } else {
            tokens.put("DATE_CREATED", "");
        }

        // Colors
        tokens.put("PRIMARY_COLOR", primaryColor);
        tokens.put("ACCENT_COLOR", accentColor);

        // Proposal ID
        tokens.put("PROPOSAL_ID", proposal.getId() != null ? proposal.getId().toString() : "");

        // Apply Now button
        String contextPath = request.getContextPath();
        String applyUrl = contextPath + "/apply/" + proposal.getApplicationGUID();
        tokens.put("APPLY_BUTTON",
                "<a href=\"" + applyUrl + "\" style=\"display:inline-block; padding:0.75rem 3rem; background:" +
                        accentColor + "; color:white; text-decoration:none; font-size:1.15rem; font-weight:600; border-radius:6px;\">" +
                        "<i class=\"bi bi-pencil-square\" style=\"margin-right:0.5rem;\"></i>Apply Now</a>");

        return tokens;
    }

    /** Replaces {{TOKEN_NAME}} placeholders in HTML content (case-insensitive) */
    private String replaceTokens(String html, Map<String, String> tokens) {
        if (html == null) return "";
        String result = html;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            // Case-insensitive replacement of {{TOKEN_NAME}}
            String pattern = "(?i)\\{\\{" + Pattern.quote(entry.getKey()) + "\\}\\}";
            result = result.replaceAll(pattern, Matcher.quoteReplacement(entry.getValue()));
        }
        return result;
    }
}

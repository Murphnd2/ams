package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.ProposalIchraIntakeDAO;
import net.superiorstate.ams.data.dao.ProposalIchraSnapshotDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.model.market.RatingAreaRateCache;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.ProposalIchraIntake;
import net.superiorstate.ams.model.sales.agency.ProposalIchraSnapshot;
import net.superiorstate.ams.model.sales.agency.ProposalIchraSnapshotBand;
import net.superiorstate.ams.model.sales.agency.ProposalPriceLine;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.Feature;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.MarketingMaterial;
import net.superiorstate.ams.model.sales.offering.ProposalSection;
import net.superiorstate.ams.model.sales.offering.ServiceModule;
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

    /**
     * Section types withheld unless the proposal's originating agency is ICHRA-entitled
     * (LA-17 / T116). Gated by {@link IchraAccessResolver#isAvailableForProposal}, which is a
     * compliance control on this page and not a display preference — see its javadoc.
     * <p>
     * The single element is {@link ProposalIchraSnapshot#SECTION_TYPE}, i.e. the literal
     * {@code "ICHRA_ILLUSTRATION"}, referenced through the constant so the gate cannot drift
     * from the type it is gating.
     */
    private static final Set<String> ICHRA_GATED_SECTION_TYPES = Set.of(ProposalIchraSnapshot.SECTION_TYPE);

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

            // Load pricing (sell price only — base/markup breakdown is internal-only, see proposalDetail.jsp)
            List<ProposalPriceLine> pricing = SalesDAO.getPricingWithAdjustments(em, proposal);

            // Build-plan item 6: ICHRA illustration snapshot — two flat queries, never
            // a nested JOIN FETCH (EclipseLink silently drops a 2-level nested fetch in
            // this codebase; see AgentHome's Opportunity->prospect->proposalList bug,
            // fixed v0.71.08). Fails closed on provenance: a snapshot not sourced from
            // PRODUCTION never sets these attributes, so this public, unauthenticated
            // page renders neither staging figures nor a staging warning for it — it
            // simply does not appear.
            // LA-17 / T116 — resolved ONCE per request, here, while the EM is still open (it closes in
            // the finally below, before the JSP forward) and before either half of the gate needs it.
            // Fails closed internally, so no try/catch is needed at the call site. Deliberately NOT the
            // session-based isAvailable(): this page is public and unauthenticated, and a PSP admin who
            // happens to be logged in must not cause market data to render in a document sent to a
            // prospect. Half one of the gate is immediately below; half two follows section filtering.
            boolean ichraEntitled = IchraAccessResolver.isAvailableForProposal(em, proposal);

            ProposalIchraSnapshot ichraSnapshot = ichraEntitled
                    ? ProposalIchraSnapshotDAO.findByProposalId(em, proposal.getId())
                    : null;
            if (ichraSnapshot != null && RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(ichraSnapshot.getSourceEnv())) {
                request.setAttribute("ichraSnapshot", ichraSnapshot);
                if (ProposalIchraSnapshot.MODE_AGE_BAND.equals(ichraSnapshot.getMode())) {
                    List<ProposalIchraSnapshotBand> ichraBands = ProposalIchraSnapshotDAO.findBandsBySnapshotId(em, ichraSnapshot.getId());
                    request.setAttribute("ichraBands", ichraBands);
                }
            }

            // Collect direct-FK module IDs for all proposed LOSs and all Enhancements with pricing
            Set<Long> featureModuleIds = new LinkedHashSet<>();

            // LOS modules (direct FK: servicemodule.los_id)
            for (LOS los : proposal.getLosList()) {
                try {
                    ServiceModule sm = em.createQuery(
                            "SELECT sm FROM ServiceModule sm WHERE sm.los.id = :losId", ServiceModule.class)
                            .setParameter("losId", los.getId())
                            .getSingleResult();
                    featureModuleIds.add(sm.getId());
                } catch (NoResultException ignored) {}
            }

            // Enhancement modules (direct FK: servicemodule.enhancement_id) — only for enhancements with pricing
            Set<Long> enhancementIdsWithPricing = new HashSet<>();
            for (ProposalPriceLine line : pricing) {
                if (line.getModule() != null && line.getModule().getEnhancement() != null) {
                    enhancementIdsWithPricing.add(line.getModule().getEnhancement().getId());
                }
            }
            for (Long enhId : enhancementIdsWithPricing) {
                try {
                    ServiceModule sm = em.createQuery(
                            "SELECT sm FROM ServiceModule sm WHERE sm.enhancement.id = :enhId", ServiceModule.class)
                            .setParameter("enhId", enhId)
                            .getSingleResult();
                    featureModuleIds.add(sm.getId());
                } catch (NoResultException ignored) {}
            }

            // Load features for all collected modules (JOIN FETCH los + enhancement for lazy-load safety)
            List<Feature> features = new ArrayList<>();
            if (!featureModuleIds.isEmpty()) {
                features = em.createQuery(
                        "SELECT f FROM Feature f LEFT JOIN FETCH f.libraryResource LEFT JOIN FETCH f.serviceModule sm LEFT JOIN FETCH sm.los LEFT JOIN FETCH sm.enhancement WHERE f.serviceModule.id IN :moduleIds ORDER BY sm.sortOrder, f.sortOrder",
                        Feature.class)
                        .setParameter("moduleIds", new ArrayList<>(featureModuleIds))
                        .getResultList();
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

                // Force-init M:N collections and agency for scope/override filtering
                for (ProposalSection s : sections) {
                    if (s.getLosList() != null) s.getLosList().size();
                    if (s.getEnhancementList() != null) s.getEnhancementList().size();
                    if (s.getAgency() != null) s.getAgency().getId();
                }

                // Scope filtering — remove SCOPED sections that don't match the proposal
                Set<Long> proposalLosIds = new HashSet<>();
                if (proposal.getLosList() != null) {
                    for (LOS los : proposal.getLosList()) {
                        proposalLosIds.add(los.getId());
                    }
                }

                Set<Long> proposalEnhIds = new HashSet<>();
                for (ProposalPriceLine line : pricing) {
                    if (line.getModule() != null && line.getModule().getEnhancement() != null) {
                        proposalEnhIds.add(line.getModule().getEnhancement().getId());
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

                // LA-17 / T116 — entitlement gate, half two. A SEPARATE pass after scope filtering,
                // deliberately not folded into it: scope answers "does this proposal include the
                // service", entitlement answers "may this audience be shown market data at all", and
                // conflating them would make one silently stand in for the other.
                //
                // ⚠️ Half one already suppresses all visible output on its own TODAY, because
                // viewProposal.jsp wraps the entire ICHRA <div> in <c:if test="${not empty
                // ichraSnapshot}"> — verified by runtime walk 2026-08-02, not assumed. This pass is
                // therefore defence in depth, and deliberately so: it does not depend on that JSP
                // detail holding. Move the <c:if> inside the div, add a header outside it, or render
                // a section index from proposalSections, and half one alone would start leaking
                // chrome for an unentitled agency. The section must not be in the list at all.
                //
                // No-op for every proposal carrying no ICHRA-typed section, which today is all of
                // them: the stream rebuilds an equal list and nothing downstream sees a difference.
                //
                // T129 — the second filter closes the hole the first one leaves. Gating by section
                // TYPE cannot reach plus-tier content configured as a CUSTOM section, and CUSTOM
                // must NOT be added to ICHRA_GATED_SECTION_TYPES: non-ICHRA CUSTOM sections are in
                // live use across other lines of service, and gating the type would strip them from
                // those proposals — a customer-facing regression on content unrelated to ICHRA.
                // So the discriminator is the section's plus-tier LOS association, not its type.
                if (!ichraEntitled) {
                    sections = sections.stream()
                            .filter(s -> !ICHRA_GATED_SECTION_TYPES.contains(s.getSectionType()))
                            .filter(s -> !isPlusTierScoped(s))
                            .collect(java.util.stream.Collectors.toList());
                }

                // Agency override for TITLE and CLOSING
                Agency proposalAgency = OriginatingAgencyResolver.resolve(proposal);

                if (proposalAgency != null) {
                    // Separate agency-scoped and default TITLE/CLOSING
                    Map<String, ProposalSection> agencyScopedByType = new HashMap<>();
                    for (ProposalSection s : sections) {
                        String t = s.getSectionType();
                        if (("TITLE".equals(t) || "CLOSING".equals(t))
                                && s.getAgency() != null && s.getAgency().getId().equals(proposalAgency.getId())) {
                            agencyScopedByType.put(t, s);
                        }
                    }

                    // Replace defaults with agency-scoped versions where they exist
                    List<ProposalSection> finalSections = new ArrayList<>();
                    for (ProposalSection s : sections) {
                        String t = s.getSectionType();
                        if ("TITLE".equals(t) || "CLOSING".equals(t)) {
                            if (s.getAgency() == null && agencyScopedByType.containsKey(t)) {
                                continue; // skip default — agency override will be used
                            }
                            if (s.getAgency() != null && !s.getAgency().getId().equals(proposalAgency.getId())) {
                                continue; // skip agency-scoped sections for OTHER agencies
                            }
                        }
                        finalSections.add(s);
                    }
                    sections = finalSections;
                } else {
                    // No agency resolved — strip all agency-scoped TITLE/CLOSING, keep only defaults
                    sections = sections.stream()
                            .filter(s -> {
                                String t = s.getSectionType();
                                return !("TITLE".equals(t) || "CLOSING".equals(t)) || s.getAgency() == null;
                            })
                            .collect(java.util.stream.Collectors.toList());
                }

                if (!sections.isEmpty()) {
                    // Build token replacement map
                    Map<String, String> tokens = buildTokenMap(em, proposal, psp, primaryColor, accentColor, request);

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

                request.setAttribute("agencyName", proposalAgency != null ? proposalAgency.getName() : null);
            }

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/viewProposal.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * T129 — is this section reached through an LOS carrying {@code los.is_plus_tier} (V086)?
     * <p>
     * The discriminator for the entitlement gate's plus-tier half. <b>Deliberately keyed on the LOS
     * association rather than on {@code section_type}</b>: plus-tier content is configured as a
     * {@code CUSTOM} section, and {@code CUSTOM} cannot be added to {@link #ICHRA_GATED_SECTION_TYPES}
     * because non-ICHRA {@code CUSTOM} sections are in live use on other lines of service and would
     * be stripped from those proposals along with it.
     * <p>
     * <b>Reads no id literal.</b> Plus-tier is whatever {@code los.is_plus_tier} says, per PSP, which
     * is the whole point of V086 having been a column on {@code los} rather than a {@code constant}
     * row naming LOS ids.
     * <p>
     * <b>Costs no query.</b> {@code losList} is force-initialised for every section before scope
     * filtering runs (see {@code doGet}), and {@code plusTier} is a basic mapped column on the
     * already-loaded {@link LOS}, so this walks objects that are in the persistence context already.
     * <p>
     * <b>Fails closed.</b> A section with no LOS association is a determinate <i>not</i> plus-tier and
     * returns {@code false} — that is what keeps every existing non-ICHRA {@code CUSTOM} section
     * rendering byte-identically. But genuine <i>uncertainty</i> — a lazy-load failure, a detached
     * collection, any exception at all — returns {@code true}, so the caller omits the section. Never
     * render on uncertainty: this gate exists because the content behind it is about to become
     * incomplete market data, and incomplete market data must not reach an unentitled audience.
     * Never throws, so the proposal cannot fail to render because this check failed.
     */
    private boolean isPlusTierScoped(ProposalSection section) {
        try {
            List<LOS> sectionLos = section.getLosList();
            if (sectionLos == null || sectionLos.isEmpty()) {
                return false;
            }
            for (LOS los : sectionLos) {
                if (los != null && los.isPlusTier()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("T129 plus-tier scope check failed for section #"
                    + (section != null ? section.getId() : "null")
                    + " — treating as plus-tier and omitting it: " + e.getMessage());
            return true;
        }
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
    private Map<String, String> buildTokenMap(EntityManager em, Proposal proposal, PSP psp, String primaryColor, String accentColor, HttpServletRequest request) {
        Map<String, String> tokens = new HashMap<>();

        // Prospect
        tokens.put("PROSPECT_NAME", proposal.getProspect().getName() != null ? proposal.getProspect().getName() : "");

        // Agent — resolve via OriginatingAgencyResolver
        // (walks sourceActivity.assignedTo → prospect.agent → createdBy)
        Person agent = OriginatingAgencyResolver.resolveAgent(proposal);
        if (agent != null) {
            String agentName = (agent.getFirstName() != null ? agent.getFirstName() : "") +
                    " " + (agent.getLastName() != null ? agent.getLastName() : "");
            tokens.put("AGENT_NAME", agentName.trim());
            tokens.put("AGENT_EMAIL", agent.getEmail() != null ? agent.getEmail() : "");
        } else {
            tokens.put("AGENT_NAME", "");
            tokens.put("AGENT_EMAIL", "");
        }

        // Agency — resolved agency name; fall back to PSP name only if none resolves
        Agency resolvedAgency = OriginatingAgencyResolver.resolve(proposal);
        String agencyName = (resolvedAgency != null && resolvedAgency.getName() != null)
                ? resolvedAgency.getName()
                : (psp.getFullName() != null ? psp.getFullName() : "");
        tokens.put("AGENCY_NAME", agencyName);
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

        // T128 — ICHRA plus-tier intake (T125's proposal_ichra_intake row), for CUSTOM
        // sections. No entitlement check here: this method only resolves values, it does
        // not gate — what holds a tier-1 CUSTOM section closed today is reference data
        // (rate assignment), not this map (see T129). Absent for every proposal with no
        // intake row — every existing line of service, and every proposal created before
        // T125 shipped — so these four keys are ALWAYS put into the map, with empty-string
        // values in that case: an unresolved key would render as the literal "{{ICHRA_COUNTY}}"
        // on a customer-facing page, and empty is the fail-closed choice. The lookup must
        // never stop the proposal from rendering, so a DAO failure falls through the same way.
        ProposalIchraIntake ichraIntake = null;
        try {
            ichraIntake = ProposalIchraIntakeDAO.findByProposalId(em, proposal.getId());
        } catch (Exception e) {
            System.out.println("ICHRA intake lookup failed for proposal #" + proposal.getId() + ": " + e.getMessage());
        }
        tokens.put("ICHRA_COUNTY", ichraIntake != null && ichraIntake.getCountyName() != null ? ichraIntake.getCountyName() : "");
        tokens.put("ICHRA_COUNTY_FIPS", ichraIntake != null && ichraIntake.getCountyFips() != null ? ichraIntake.getCountyFips() : "");
        tokens.put("ICHRA_HEADCOUNT", ichraIntake != null && ichraIntake.getHeadcount() != null ? ichraIntake.getHeadcount().toString() : "");
        tokens.put("ICHRA_PLAN_YEAR", ichraIntake != null && ichraIntake.getPlanYear() != null ? ichraIntake.getPlanYear().toString() : "");

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

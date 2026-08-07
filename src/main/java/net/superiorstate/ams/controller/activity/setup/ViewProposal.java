package net.superiorstate.ams.controller.activity.setup;

import com.google.gson.*;
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
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.resolver.FlaggedEnhancementResolver;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.data.resolver.RateSourceEnvResolver;
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
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "ViewProposal", value = "/proposal/*")
public class ViewProposal extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ViewProposal.class);

    /** Matches [link text](resourceId) markers in feature descriptions */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\((\\d+)\\)");

    /**
     * S11-H — the {@code proposal_section.section_type} discriminator for the conditional Market
     * page. Free-text {@code varchar(20)} column, so this is data rather than schema and needed no
     * migration. Declared here rather than on an entity because, unlike
     * {@code ICHRA_ILLUSTRATION}/{@link ProposalIchraSnapshot#SECTION_TYPE}, no entity owns this
     * page — it renders from the live rate cache, not from a persisted row of its own.
     * {@code ProposalSettings} references this constant so the admin CRUD and the render gate
     * cannot drift apart.
     */
    public static final String MARKET_SECTION_TYPE = "MARKET";

    /**
     * Section types withheld unless the proposal's originating agency is ICHRA-entitled
     * (LA-17 / T116). Gated by {@link IchraAccessResolver#isAvailableForProposal}, which is a
     * compliance control on this page and not a display preference — see its javadoc.
     * <p>
     * The first element is {@link ProposalIchraSnapshot#SECTION_TYPE}, i.e. the literal
     * {@code "ICHRA_ILLUSTRATION"}, referenced through the constant so the gate cannot drift
     * from the type it is gating.
     * <p>
     * S11-H added {@link #MARKET_SECTION_TYPE}. ⚠️ <b>Adding a type here is safe only for a
     * type no existing row carries.</b> {@code MARKET} is brand new, so gating it strips
     * nothing that renders today. {@code CUSTOM} must never be added for exactly the opposite
     * reason — see the T129 note at the filter site.
     */
    private static final Set<String> ICHRA_GATED_SECTION_TYPES =
            Set.of(ProposalIchraSnapshot.SECTION_TYPE, MARKET_SECTION_TYPE);

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
            if (ichraSnapshot != null && RateSourceEnvResolver.authoritativeSourceEnv(em).equals(ichraSnapshot.getSourceEnv())) {
                request.setAttribute("ichraSnapshot", ichraSnapshot);
                if (ProposalIchraSnapshot.MODE_AGE_BAND.equals(ichraSnapshot.getMode())) {
                    List<ProposalIchraSnapshotBand> ichraBands = ProposalIchraSnapshotDAO.findBandsBySnapshotId(em, ichraSnapshot.getId());
                    request.setAttribute("ichraBands", ichraBands);
                }
            }

            // S11-H — the conditional Market page. Resolved here, while the EM is still open (it
            // closes in the finally below, before the JSP forward), for the same reason the
            // snapshot above is: the fragment renders from request attributes and can issue no
            // query of its own.
            resolveMarketPage(request, em, proposal, ichraEntitled);

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
                // S20-B/V091 — a system_managed enhancement drives no pricing by design, so it
                // never enters proposalEnhIds via the loop above. Widen membership directly from
                // the proposal's LOS list so its section can be reached at all. See
                // docs/analysis/S20A_ichra_sections_spec.md §1.4b/§5.2. Bit-identical when no
                // enhancement on this PSP is flagged, which is every PSP until an admin flags one.
                proposalEnhIds.addAll(FlaggedEnhancementResolver.systemManagedIdsForProposal(em, proposal));

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
                            if (proposalEnhIds.contains(enh.getId()) && FlaggedEnhancementResolver.isSectionEnabled(em, proposal, enh)) {
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
                    Map<String, String> tokens = buildTokenMap(em, proposal, psp, primaryColor, accentColor, request, ichraEntitled);

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
     * S11-H — resolves the conditional Market page, setting {@code marketPageVisible} plus the
     * figures the fragment renders. The page appears only when <b>all three</b> hold:
     * <ol>
     *   <li><b>The originating agency is ICHRA-entitled</b> — {@code ichraEntitled}, already
     *       resolved once per request by {@link IchraAccessResolver#isAvailableForProposal}. Not
     *       re-derived here; a second resolution could disagree with the one the section filter
     *       used.</li>
     *   <li><b>The proposal is quoting a plus-tier LOS</b> — {@code los.isPlusTier()} (V086) over
     *       {@code proposal.getLosList()}. ⚠️ <b>Deliberately the proposal-level question, not
     *       {@link #isPlusTierScoped}'s section-level one.</b> That method asks whether a given
     *       <i>section</i> is scoped to a plus-tier LOS, which is the right question for T129's
     *       blanket filter and the wrong one here: this page is about what the proposal quotes,
     *       and a MARKET section is deliberately not LOS-scoped. Mirrors
     *       {@code ProposalBuilder.attachIchraIntakeIfPresent}'s own {@code anyPlusTier} loop, so
     *       the page can only appear on a proposal the intake row was allowed to be written for.
     *       Reads no LOS id literal — plus-tier is whatever the column says, per PSP.</li>
     *   <li><b>Production-sourced rates exist for the intake row's county and plan year</b> —
     *       one call to {@link RateCacheDAO#check} (S11-G), the same question the agent-facing
     *       advisory asks, so builder and proposal cannot disagree. The provenance condition is
     *       not restated here; restating it is how the two drift.</li>
     * </ol>
     * <b>Fails closed at every step.</b> No intake row, a null plan year or county, anything short
     * of {@link RateCacheDAO.MarketDataAvailability#PRODUCTION_OK}, or any exception at all leaves
     * {@code marketPageVisible} false and every figure attribute unset — the JSP's {@code <c:if>}
     * then renders nothing, exactly as the features page does with no features. Never throws: a
     * proposal must not fail to render because a rate lookup failed.
     * <p>
     * <b>Byte-identical for every proposal with no MARKET section row</b>, which today is all of
     * them — the attributes are simply never read.
     */
    private void resolveMarketPage(HttpServletRequest request, EntityManager em,
                                   Proposal proposal, boolean ichraEntitled) {
        request.setAttribute("marketPageVisible", false);
        try {
            if (!ichraEntitled) {
                return;
            }

            boolean anyPlusTier = false;
            if (proposal.getLosList() != null) {
                for (LOS los : proposal.getLosList()) {
                    if (los != null && los.isPlusTier()) {
                        anyPlusTier = true;
                        break;
                    }
                }
            }
            if (!anyPlusTier) {
                return;
            }

            ProposalIchraIntake intake = ProposalIchraIntakeDAO.findByProposalId(em, proposal.getId());
            if (intake == null || intake.getPlanYear() == null || intake.getCountyFips() == null) {
                return;
            }

            if (RateCacheDAO.check(em, intake.getPlanYear(), intake.getCountyFips())
                    != RateCacheDAO.MarketDataAvailability.PRODUCTION_OK) {
                return;
            }

            // Provenance is settled by check() above, so these rows are known PRODUCTION-sourced.
            // Tobacco rows are a separate rating basis and are excluded, matching
            // putIchraMarketTokens and IllustrationServlet.handleRangeMode.
            List<RatingAreaRateCache> nonTobacco = new ArrayList<>();
            for (RatingAreaRateCache r : RateCacheDAO.getRatesForCounty(em, intake.getPlanYear(), intake.getCountyFips())) {
                if (!r.isUsesTobacco()) nonTobacco.add(r);
            }
            if (nonTobacco.isEmpty()) {
                return;
            }

            Map<Integer, RatingAreaRateCache> byAge = new HashMap<>();
            LocalDateTime newestFetchedAt = null;
            for (RatingAreaRateCache r : nonTobacco) {
                byAge.putIfAbsent(r.getAge(), r);
                if (r.getFetchedAt() != null
                        && (newestFetchedAt == null || r.getFetchedAt().isAfter(newestFetchedAt))) {
                    newestFetchedAt = r.getFetchedAt();
                }
            }

            // Counts come from the AGE-40 row specifically, matching putIchraMarketTokens and
            // IllustrationServlet.handleRangeMode's deterministic choice, and for its stated reason:
            // carrier/plan counts are age-specific, because catastrophic plans are under-30 only.
            RatingAreaRateCache countRow = byAge.get(40) != null ? byAge.get(40) : nonTobacco.get(0);

            request.setAttribute("marketPlanCount", countRow.getPlanCount());
            request.setAttribute("marketCarrierCount", countRow.getCarrierCount());
            request.setAttribute("marketFloor21", premiumOrNull(byAge.get(21)));
            request.setAttribute("marketFloor40", premiumOrNull(byAge.get(40)));
            request.setAttribute("marketFloor64", premiumOrNull(byAge.get(64)));
            request.setAttribute("marketCountyName", intake.getCountyName());
            request.setAttribute("marketState", intake.getState());
            request.setAttribute("marketPlanYear", intake.getPlanYear());
            request.setAttribute("marketRatesAsOf",
                    newestFetchedAt == null ? null : newestFetchedAt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
            request.setAttribute("marketPageVisible", true);

        } catch (Exception e) {
            request.setAttribute("marketPageVisible", false);
            System.out.println("S11-H Market page resolution failed for proposal #"
                    + (proposal != null ? proposal.getId() : "null") + "; omitting the page: " + e.getMessage());
        }
    }

    /** The bronze-floor premium on a cache row, or null when the row or the figure is absent. */
    private BigDecimal premiumOrNull(RatingAreaRateCache row) {
        return row == null ? null : row.getLowestBronzePremium();
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
    private Map<String, String> buildTokenMap(EntityManager em, Proposal proposal, PSP psp, String primaryColor, String accentColor, HttpServletRequest request, boolean ichraEntitled) {
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

        // T80 half 1 (V088) — the employer's own stated monthly-per-employee contribution,
        // an intake token exactly like the four above: not entitlement-gated, not
        // provenance-gated, resolved from the agent's own input, never from market data.
        // All-or-nothing as a group, matching the pattern above — a null contribution, or a
        // null/zero headcount, means every one of these four resolves to "", never a literal
        // brace and never $0.00.
        BigDecimal monthlyContribution = ichraIntake != null ? ichraIntake.getMonthlyContributionPerEmployee() : null;
        Integer contributionHeadcount = ichraIntake != null ? ichraIntake.getHeadcount() : null;
        if (monthlyContribution != null && contributionHeadcount != null && contributionHeadcount != 0) {
            BigDecimal annual = monthlyContribution.multiply(BigDecimal.valueOf(12));
            BigDecimal totalMonthly = monthlyContribution.multiply(BigDecimal.valueOf(contributionHeadcount));
            BigDecimal totalAnnual = totalMonthly.multiply(BigDecimal.valueOf(12));
            tokens.put("ICHRA_CONTRIBUTION_MONTHLY", formatCurrency(monthlyContribution));
            tokens.put("ICHRA_CONTRIBUTION_ANNUAL", formatCurrency(annual));
            tokens.put("ICHRA_CONTRIBUTION_TOTAL_MONTHLY", formatCurrency(totalMonthly));
            tokens.put("ICHRA_CONTRIBUTION_TOTAL_ANNUAL", formatCurrency(totalAnnual));
        } else {
            tokens.put("ICHRA_CONTRIBUTION_MONTHLY", "");
            tokens.put("ICHRA_CONTRIBUTION_ANNUAL", "");
            tokens.put("ICHRA_CONTRIBUTION_TOTAL_MONTHLY", "");
            tokens.put("ICHRA_CONTRIBUTION_TOTAL_ANNUAL", "");
        }

        putIchraMarketTokens(em, tokens, ichraIntake, ichraEntitled, proposal);
        putIchraPayloadTokens(em, tokens, proposal);

        return tokens;
    }

    /**
     * T130 — market-data tokens (plan/carrier counts, premium floors, as-of date) for a plus-tier
     * {@code CUSTOM} section, resolved from the warm rate cache for the intake row's county and plan year.
     * <p>
     * <b>⚠️ Cache reads only. This method must never be able to reach the HealthSherpa API.</b> It goes
     * through {@link RateCacheDAO#getRatesForCounty}, which is pure JPQL and references no HTTP client of
     * any kind; {@code HealthSherpaService} is reachable only from {@code RateCacheWarmService} (the
     * scheduled warm job) and {@code RateCacheAdmin}. A county with no warm cache yields empty tokens —
     * <b>it does not lazily warm, and nothing here may be changed to</b>. This page is public and
     * unauthenticated: an outbound call from it would put a credential on an anonymous request and add
     * third-party latency to a document an employer is reading.
     * <p>
     * <b>Every rate value is accompanied by {@code ICHRA_RATES_AS_OF}, taken from the cache rows' own
     * {@code fetchedAt}</b> — never today's date and never the proposal date. A cached premium on a PDF an
     * employer keeps for months ages silently otherwise.
     * <p>
     * <b>Fails closed on four independent conditions</b>, any of which yields empty strings for every token
     * in this group: not ICHRA-entitled (defence in depth — T129 should already have omitted the section,
     * but its discriminator has proven sensitive to configuration); no intake row; no cached rows for that
     * county/year; or any row not sourced from {@code PRODUCTION}. The last mirrors what this servlet
     * already does for {@code ProposalIchraSnapshot} — staging figures must never render to an employer.
     * <p>
     * <b>Blank, never zero.</b> A cache miss renders gaps; {@code 0 plans} would be a false market claim.
     * <p>
     * <b>Compliance.</b> Counts and floors are precedented by the shipped illustration and are named
     * explicitly in LA-17. No plan is named, no carrier is named, nothing is ordered, ranked, defaulted or
     * recommended, and no per-employee affordability figure is produced — this method is structurally
     * incapable of it, since it reads only aggregate counts and the {@code lowestBronzePremium} floor.
     * Never throws: a proposal must not fail to render because a rate lookup failed.
     */
    private void putIchraMarketTokens(EntityManager em, Map<String, String> tokens,
                                      ProposalIchraIntake intake, boolean ichraEntitled, Proposal proposal) {
        String planCount = "", carrierCount = "";
        String floor21 = "", floor40 = "", floor64 = "";
        String ratesAsOf = "", ratesScope = "";

        try {
            if (ichraEntitled && intake != null
                    && intake.getCountyFips() != null && intake.getPlanYear() != null) {

                List<RatingAreaRateCache> rows =
                        RateCacheDAO.getRatesForCounty(em, intake.getPlanYear(), intake.getCountyFips());

                // Tobacco rows are a separate rating basis and are excluded, matching
                // IllustrationServlet.handleRangeMode's own filter.
                List<RatingAreaRateCache> nonTobacco = new ArrayList<>();
                for (RatingAreaRateCache r : rows) {
                    if (!r.isUsesTobacco()) nonTobacco.add(r);
                }

                // Provenance gate — EVERY row must match the currently-authoritative env
                // (S21-L, RateSourceEnvResolver), not merely the first.
                String authoritativeEnv = RateSourceEnvResolver.authoritativeSourceEnv(em);
                boolean allAuthoritative = !nonTobacco.isEmpty();
                for (RatingAreaRateCache r : nonTobacco) {
                    if (!authoritativeEnv.equals(r.getSourceEnv())) {
                        allAuthoritative = false;
                        break;
                    }
                }

                if (allAuthoritative) {
                    Map<Integer, RatingAreaRateCache> byAge = new HashMap<>();
                    LocalDateTime newestFetchedAt = null;
                    for (RatingAreaRateCache r : nonTobacco) {
                        byAge.putIfAbsent(r.getAge(), r);
                        if (r.getFetchedAt() != null
                                && (newestFetchedAt == null || r.getFetchedAt().isAfter(newestFetchedAt))) {
                            newestFetchedAt = r.getFetchedAt();
                        }
                    }

                    floor21 = formatPremium(byAge.get(21));
                    floor40 = formatPremium(byAge.get(40));
                    floor64 = formatPremium(byAge.get(64));

                    // Counts come from the AGE-40 row specifically, matching
                    // IllustrationServlet.handleRangeMode's deterministic choice and for its stated
                    // reason: carrier/plan counts are age-specific, because catastrophic plans are
                    // under-30 only — so age 21 reports a different plan count than age 40.
                    RatingAreaRateCache countRow = byAge.get(40) != null ? byAge.get(40) : nonTobacco.get(0);
                    if (countRow.getPlanCount() != null) planCount = countRow.getPlanCount().toString();
                    if (countRow.getCarrierCount() != null) carrierCount = countRow.getCarrierCount().toString();

                    if (newestFetchedAt != null) {
                        // Date only, and formatted to match this map's own DATE_CREATED convention
                        // rather than the illustration's admin-facing "yyyy-MM-dd HH:mm" timestamp —
                        // the audience here is an employer reading a document, not an operator
                        // inspecting a cache.
                        ratesAsOf = newestFetchedAt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

                        // Disclosure line. Off-exchange-only makes any plan display definitionally
                        // incomplete; this exists so the HTML can say so plainly instead of the
                        // author having to remember to. Emitted ONLY alongside real figures — an
                        // empty-token proposal must not carry a caveat about data it never showed.
                        ratesScope = "Figures reflect off-exchange individual plans available in "
                                + (intake.getCountyName() != null ? intake.getCountyName() : "the selected county")
                                + " as of " + ratesAsOf + ". They are not a quote and not a complete"
                                + " view of the market.";
                    }
                }
            }
        } catch (Exception e) {
            // Empty strings already hold; a failed lookup must never break the render.
            System.out.println("T130 ICHRA market-data token lookup failed for proposal #"
                    + (proposal != null ? proposal.getId() : "null") + ": " + e.getMessage());
        }

        // Always present, per T128's mechanism: replaceTokens leaves an unmatched key as the literal
        // "{{ICHRA_PLAN_COUNT}}" in the output, which on an employer's proposal is a visible defect.
        tokens.put("ICHRA_PLAN_COUNT", planCount);
        tokens.put("ICHRA_CARRIER_COUNT", carrierCount);
        tokens.put("ICHRA_FLOOR_AGE_21", floor21);
        tokens.put("ICHRA_FLOOR_AGE_40", floor40);
        tokens.put("ICHRA_FLOOR_AGE_64", floor64);
        tokens.put("ICHRA_RATES_AS_OF", ratesAsOf);
        tokens.put("ICHRA_RATES_SCOPE", ratesScope);
    }

    /**
     * T165/V090 — three read-only tokens off {@code ProposalIchraSnapshot.payloadJson}:
     * {@code ICHRA_AGE_BAND_TABLE}, {@code ICHRA_PLAN_LANDSCAPE_TABLE}, {@code ICHRA_PAYLOAD_AS_OF}.
     * See {@code docs/analysis/S19D_ichra_payload_spec.md} §4 (Read). <b>Deliberately defines no
     * token for the payload's {@code affordability} block</b> — the spec's BLOCKED item 2; this
     * method must never quietly grow one.
     * <p>
     * The plan-landscape table withholds plan {@code name} and {@code issuerName} pending O25,
     * even though both are present in storage (spec §3). Never throws — a proposal must not fail
     * to render because the payload is absent, unparseable, or an unrecognized schema version;
     * mirrors {@link #putIchraMarketTokens}'s own try/catch discipline exactly.
     */
    private void putIchraPayloadTokens(EntityManager em, Map<String, String> tokens, Proposal proposal) {
        String ageBandTable = "";
        String planLandscapeTable = "";
        String payloadAsOf = "";
        try {
            ProposalIchraSnapshot snapshot = ProposalIchraSnapshotDAO.findByProposalId(em, proposal.getId());
            String payloadJson = snapshot != null ? snapshot.getPayloadJson() : null;
            if (payloadJson != null) {
                JsonObject payload = new Gson().fromJson(payloadJson, JsonObject.class);

                if (payload.has("provenance") && payload.get("provenance").isJsonObject()) {
                    JsonObject provenance = payload.getAsJsonObject("provenance");
                    if (provenance.has("capturedAt") && !provenance.get("capturedAt").isJsonNull()) {
                        // S21-K — capturedAt is stored machine-readable (ProposalBuilder's
                        // ICHRA_PAYLOAD_TIMESTAMP, "yyyy-MM-dd'T'HH:mm:ss") for a customer-facing
                        // read here, not for display as-is. Re-parsed with that same pattern and
                        // reformatted for a human reader; a parse failure falls to this method's
                        // existing outer catch, same as any other malformed payload field.
                        LocalDateTime capturedAt = LocalDateTime.parse(
                                provenance.get("capturedAt").getAsString(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                        payloadAsOf = "Figures reflect an ICHRA data snapshot captured on "
                                + capturedAt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a"))
                                + ". They are not a live quote.";
                    }
                }

                if (payload.has("ageBands") && payload.get("ageBands").isJsonArray()) {
                    JsonArray ageBands = payload.getAsJsonArray("ageBands");
                    if (ageBands.size() > 0) {
                        StringBuilder sb = new StringBuilder("<table class=\"ichra-age-band-table\"><thead><tr>"
                                + "<th>Age</th><th>Lives</th><th>Premium</th></tr></thead><tbody>");
                        for (JsonElement el : ageBands) {
                            JsonObject band = el.getAsJsonObject();
                            String age = band.has("age") && !band.get("age").isJsonNull() ? band.get("age").getAsString() : "";
                            String lives = band.has("lives") && !band.get("lives").isJsonNull() ? band.get("lives").getAsString() : "";
                            String premium = band.has("premium") && !band.get("premium").isJsonNull()
                                    ? formatCurrency(band.get("premium").getAsBigDecimal()) : "";
                            sb.append("<tr><td>").append(escapeHtml(age)).append("</td><td>")
                                    .append(escapeHtml(lives)).append("</td><td>")
                                    .append(escapeHtml(premium)).append("</td></tr>");
                        }
                        sb.append("</tbody></table>");
                        ageBandTable = sb.toString();
                    }
                }

                if (payload.has("planLandscape") && payload.get("planLandscape").isJsonObject()) {
                    JsonObject planLandscape = payload.getAsJsonObject("planLandscape");
                    if (planLandscape.has("plans") && planLandscape.get("plans").isJsonArray()) {
                        JsonArray plans = planLandscape.getAsJsonArray("plans");
                        if (plans.size() > 0) {
                            // name/issuerName withheld pending O25 (spec §4/§9), even though both are in storage.
                            StringBuilder sb = new StringBuilder("<table class=\"ichra-plan-landscape-table\"><thead><tr>"
                                    + "<th>Metal Level</th><th>Premium</th><th>HSA Eligible</th><th>ICHRA Only</th></tr></thead><tbody>");
                            for (JsonElement el : plans) {
                                JsonObject plan = el.getAsJsonObject();
                                String metalLevel = plan.has("metalLevel") && !plan.get("metalLevel").isJsonNull() ? plan.get("metalLevel").getAsString() : "";
                                String premium = plan.has("premium") && !plan.get("premium").isJsonNull()
                                        ? formatCurrency(plan.get("premium").getAsBigDecimal()) : "";
                                String hsaEligible = plan.has("hsaEligible") && !plan.get("hsaEligible").isJsonNull()
                                        ? (plan.get("hsaEligible").getAsBoolean() ? "Yes" : "No") : "";
                                String ichraOnly = plan.has("ichraOnly") && !plan.get("ichraOnly").isJsonNull()
                                        ? (plan.get("ichraOnly").getAsBoolean() ? "Yes" : "No") : "";
                                sb.append("<tr><td>").append(escapeHtml(metalLevel)).append("</td><td>")
                                        .append(escapeHtml(premium)).append("</td><td>")
                                        .append(escapeHtml(hsaEligible)).append("</td><td>")
                                        .append(escapeHtml(ichraOnly)).append("</td></tr>");
                            }
                            sb.append("</tbody></table>");
                            planLandscapeTable = sb.toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Empty strings already hold; an absent/unparseable/unrecognized-schema payload must
            // never break the render.
            log.warn("T165 ICHRA payload token lookup failed for proposal #{}: {}",
                    proposal != null ? proposal.getId() : "null", e.getMessage());
            ageBandTable = "";
            planLandscapeTable = "";
            payloadAsOf = "";
        }

        tokens.put("ICHRA_AGE_BAND_TABLE", ageBandTable);
        tokens.put("ICHRA_PLAN_LANDSCAPE_TABLE", planLandscapeTable);
        tokens.put("ICHRA_PAYLOAD_AS_OF", payloadAsOf);

        putIchraContributionScenarioToken(em, tokens, proposal);
        putIchraGroupComparisonToken(em, tokens, proposal);
    }

    /**
     * S21-C/T171 — {@code ICHRA_CONTRIBUTION_SCENARIO_TABLE}, build 2 of the four ICHRA
     * sections (docs/analysis/S20A_ichra_sections_spec.md §7). What the entered employer
     * contribution produces against the frozen snapshot. Read-only off {@code
     * ProposalIchraSnapshot} — never re-fetches {@code rating_area_rate_cache} and never
     * calls HealthSherpa; the snapshot taken at proposal build time is the source of truth,
     * same discipline as {@link #putIchraPayloadTokens}.
     * <p>
     * Per-band net (age, lives, premium, contribution, net) when the snapshot carries age
     * bands — {@code payload.ageBands} plus {@code snapshot.getContribution()}, both frozen
     * at build time. <b>Finding: {@code attachRangeSnapshot} never calls {@code
     * setContribution} on the snapshot</b> (only {@code attachAgeBandSnapshot} does) — so a
     * RANGE-mode (headcount-only) proposal has no frozen per-employee contribution figure to
     * net against, even though the sections-block gate can mark {@code ICHRA_CONTRIBUTION}
     * complete for that mode. Falling back to {@code proposal_ichra_intake}'s live,
     * independently-editable row would break the point-in-time guarantee this whole payload
     * design exists for (spec §3), so that mode instead renders the gross group premium range
     * ({@code snapshot.getGroupMonthlyLow()}/{@code getGroupMonthlyHigh()}) with no net
     * column, rather than fabricate one. Age bands are a fidelity upgrade, never a gate — see
     * spec §2.
     * <p>
     * No ranking, no recommendation, no highlighted row — arithmetic on employer-supplied
     * inputs only. Degrades to "" on any missing/unparseable input; never throws, mirroring
     * {@link #putIchraPayloadTokens}'s own try/catch discipline exactly.
     */
    private void putIchraContributionScenarioToken(EntityManager em, Map<String, String> tokens, Proposal proposal) {
        String scenarioTable = "";
        try {
            ProposalIchraSnapshot snapshot = ProposalIchraSnapshotDAO.findByProposalId(em, proposal.getId());
            String payloadJson = snapshot != null ? snapshot.getPayloadJson() : null;
            if (snapshot != null && payloadJson != null) {
                JsonObject payload = new Gson().fromJson(payloadJson, JsonObject.class);
                BigDecimal contribution = snapshot.getContribution();

                if (payload.has("ageBands") && payload.get("ageBands").isJsonArray()
                        && payload.getAsJsonArray("ageBands").size() > 0 && contribution != null) {
                    JsonArray ageBands = payload.getAsJsonArray("ageBands");
                    StringBuilder sb = new StringBuilder("<table class=\"ichra-contribution-scenario-table\"><thead><tr>"
                            + "<th>Age</th><th>Lives</th><th>Premium</th><th>Contribution</th><th>Net</th></tr></thead><tbody>");
                    for (JsonElement el : ageBands) {
                        JsonObject band = el.getAsJsonObject();
                        String age = band.has("age") && !band.get("age").isJsonNull() ? band.get("age").getAsString() : "";
                        String lives = band.has("lives") && !band.get("lives").isJsonNull() ? band.get("lives").getAsString() : "";
                        String premiumStr = "", contributionStr = "", netStr = "";
                        if (band.has("premium") && !band.get("premium").isJsonNull()) {
                            BigDecimal premium = band.get("premium").getAsBigDecimal();
                            BigDecimal net = premium.subtract(contribution).max(BigDecimal.ZERO);
                            premiumStr = formatCurrency(premium);
                            contributionStr = formatCurrency(contribution);
                            netStr = formatCurrency(net);
                        }
                        sb.append("<tr><td>").append(escapeHtml(age)).append("</td><td>")
                                .append(escapeHtml(lives)).append("</td><td>")
                                .append(escapeHtml(premiumStr)).append("</td><td>")
                                .append(escapeHtml(contributionStr)).append("</td><td>")
                                .append(escapeHtml(netStr)).append("</td></tr>");
                    }
                    sb.append("</tbody></table>");
                    scenarioTable = sb.toString();
                } else if (snapshot.getGroupMonthlyLow() != null && snapshot.getGroupMonthlyHigh() != null) {
                    // RANGE mode. S21-F — attachRangeSnapshot (fixed S21-D, 468cf7c) now
                    // persists the same per-employee monthly figure attachAgeBandSnapshot
                    // always did: proposalBuilder.jsp:266's own label ("Monthly employer
                    // contribution per employee") and the identical shared request parameter
                    // (ichraParam(request, "contribution", "intakeContribution")) confirm the
                    // unit -- per employee, never a group total. snapshot.getHeadcount()
                    // (also set by attachRangeSnapshot) converts it to a group total so it is
                    // comparable to groupMonthlyLow/High, which are themselves group totals.
                    if (snapshot.getContribution() != null && snapshot.getHeadcount() != null) {
                        BigDecimal totalContribution = snapshot.getContribution()
                                .multiply(BigDecimal.valueOf(snapshot.getHeadcount()));
                        // Not clamped at zero, unlike the per-band branch above: a contribution
                        // that exceeds the low end of the range is a real, honest negative net,
                        // not an error to hide or floor away.
                        BigDecimal netLow = snapshot.getGroupMonthlyLow().subtract(totalContribution);
                        BigDecimal netHigh = snapshot.getGroupMonthlyHigh().subtract(totalContribution);
                        StringBuilder sb = new StringBuilder("<table class=\"ichra-contribution-scenario-table\"><thead><tr>"
                                + "<th>Scenario</th><th>Group Monthly Premium</th>"
                                + "<th>Total Monthly Contribution</th><th>Net Monthly</th></tr></thead><tbody>");
                        sb.append("<tr><td>Low</td><td>").append(escapeHtml(formatCurrency(snapshot.getGroupMonthlyLow())))
                                .append("</td><td>").append(escapeHtml(formatCurrency(totalContribution)))
                                .append("</td><td>").append(escapeHtml(formatCurrency(netLow))).append("</td></tr>");
                        sb.append("<tr><td>High</td><td>").append(escapeHtml(formatCurrency(snapshot.getGroupMonthlyHigh())))
                                .append("</td><td>").append(escapeHtml(formatCurrency(totalContribution)))
                                .append("</td><td>").append(escapeHtml(formatCurrency(netHigh))).append("</td></tr>");
                        sb.append("</tbody></table>");
                        scenarioTable = sb.toString();
                    } else {
                        // No frozen contribution -- predates S21-D (468cf7c), or genuinely never
                        // entered. Exactly today's degraded output: the gross range, never a
                        // fabricated contribution or net column.
                        StringBuilder sb = new StringBuilder("<table class=\"ichra-contribution-scenario-table\"><thead><tr>"
                                + "<th>Group Monthly Premium (Low)</th><th>Group Monthly Premium (High)</th></tr></thead><tbody>");
                        sb.append("<tr><td>").append(escapeHtml(formatCurrency(snapshot.getGroupMonthlyLow())))
                                .append("</td><td>").append(escapeHtml(formatCurrency(snapshot.getGroupMonthlyHigh())))
                                .append("</td></tr>");
                        sb.append("</tbody></table>");
                        scenarioTable = sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Empty string already holds; an absent/unparseable payload must never break the render.
            log.warn("T171 ICHRA contribution scenario token lookup failed for proposal #{}: {}",
                    proposal != null ? proposal.getId() : "null", e.getMessage());
            scenarioTable = "";
        }
        tokens.put("ICHRA_CONTRIBUTION_SCENARIO_TABLE", scenarioTable);
    }

    /**
     * S21-E/T172 — {@code ICHRA_GROUP_COMPARISON_TABLE}, build 3 of the four ICHRA sections
     * (docs/analysis/S20A_ichra_sections_spec.md §7). What the employer's current group plan
     * costs against the planned ICHRA contribution. Read-only off {@code
     * ProposalIchraSnapshot.getPayloadJson()}'s {@code groupComparison} sub-block (S21-E,
     * frozen at build time in {@code ProposalBuilder.buildIchraPayload}) — never re-fetches
     * {@code rating_area_rate_cache}, never calls HealthSherpa, and never reads {@code
     * proposal_ichra_intake} at render; the frozen payload is the source of truth, same
     * discipline as {@link #putIchraPayloadTokens} and {@link
     * #putIchraContributionScenarioToken}.
     * <p>
     * {@code groupComparison} is absent on every payload written before this build, and on
     * any proposal where the employer's current-coverage figures were never entered — both
     * degrade to "" here, never a literal {{TOKEN}}, never a fabricated figure, never a zero
     * standing in for "not entered".
     * <p>
     * Presents figures only: current total premium, current employer share, planned
     * contribution, and the employer delta already computed at freeze time — never
     * recomputed here. No "savings" framing, no highlighted column, no sort implying
     * preference, no implied verdict; the employer draws the conclusion. No affordability
     * determination — section 4 stays blocked and this method makes no such claim.
     */
    private void putIchraGroupComparisonToken(EntityManager em, Map<String, String> tokens, Proposal proposal) {
        String comparisonTable = "";
        try {
            ProposalIchraSnapshot snapshot = ProposalIchraSnapshotDAO.findByProposalId(em, proposal.getId());
            String payloadJson = snapshot != null ? snapshot.getPayloadJson() : null;
            if (payloadJson != null) {
                JsonObject payload = new Gson().fromJson(payloadJson, JsonObject.class);
                if (payload.has("groupComparison") && payload.get("groupComparison").isJsonObject()) {
                    JsonObject gc = payload.getAsJsonObject("groupComparison");
                    if (gc.has("currentTotalMonthlyPremium") && !gc.get("currentTotalMonthlyPremium").isJsonNull()
                            && gc.has("currentEmployerMonthlyShare") && !gc.get("currentEmployerMonthlyShare").isJsonNull()
                            && gc.has("plannedContributionTotal") && !gc.get("plannedContributionTotal").isJsonNull()
                            && gc.has("employerDelta") && !gc.get("employerDelta").isJsonNull()) {
                        // S21-G — reads plannedContributionTotal (group total, corrected), not
                        // the pre-S21-G plannedContribution key (the raw per-employee figure
                        // that produced a materially wrong delta). A payload frozen before this
                        // fix carries the old key name only, so it has no plannedContributionTotal
                        // and this whole block degrades to "" for it — never a re-derived or
                        // partially-corrected figure for old data, per the same withhold-rather-
                        // than-fabricate rule as everywhere else in this method.
                        String currentTotal = formatCurrency(gc.get("currentTotalMonthlyPremium").getAsBigDecimal());
                        String currentShare = formatCurrency(gc.get("currentEmployerMonthlyShare").getAsBigDecimal());
                        String planned = formatCurrency(gc.get("plannedContributionTotal").getAsBigDecimal());
                        String delta = formatCurrency(gc.get("employerDelta").getAsBigDecimal());

                        // Headings state their unit explicitly, following S21-F's own
                        // precedent ("Total Monthly Contribution", not bare "Contribution") --
                        // every figure here is a whole-group monthly total, and the difference
                        // column names its own sign convention so a negative number never
                        // requires the reader to guess the direction.
                        StringBuilder sb = new StringBuilder("<table class=\"ichra-group-comparison-table\"><thead><tr>"
                                + "<th>Current Total Monthly Premium (Group)</th><th>Current Employer Monthly Share (Group)</th>"
                                + "<th>Planned ICHRA Contribution (Group Total)</th><th>Employer Monthly Difference (Planned − Current)</th></tr></thead><tbody>");
                        sb.append("<tr><td>").append(escapeHtml(currentTotal)).append("</td><td>")
                                .append(escapeHtml(currentShare)).append("</td><td>")
                                .append(escapeHtml(planned)).append("</td><td>")
                                .append(escapeHtml(delta)).append("</td></tr>");
                        sb.append("</tbody></table>");
                        comparisonTable = sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Empty string already holds; an absent/unparseable payload must never break the render.
            log.warn("T172 ICHRA group comparison token lookup failed for proposal #{}: {}",
                    proposal != null ? proposal.getId() : "null", e.getMessage());
            comparisonTable = "";
        }
        tokens.put("ICHRA_GROUP_COMPARISON_TABLE", comparisonTable);
    }

    /**
     * The lowest available monthly premium on a cache row, as displayed currency, or "" when the row or
     * the figure is absent. Reads {@code lowestBronzePremium} — the same field the illustration and
     * {@code ProposalBuilder}'s snapshot both use as the floor.
     */
    private String formatPremium(RatingAreaRateCache row) {
        if (row == null || row.getLowestBronzePremium() == null) return "";
        BigDecimal premium = row.getLowestBronzePremium();
        return NumberFormat.getCurrencyInstance(Locale.US).format(premium);
    }

    /** Same formatter as {@link #formatPremium}, for a plain BigDecimal dollar figure. */
    private String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
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
        // T133 — the loop above iterates the map's keys, so a token present in the content
        // but absent from the map is never visited and survives into the rendered output and
        // onto a customer-facing document. Strip any residual token, naming it first.
        // Token-shaped only: {{a:1}} (a nested JS object literal) must never match, and the
        // character class mirrors exactly the shape the substitution above recognises, so a
        // spaced variant like "{{ FOO }}" is left alone here just as it is left alone there.
        // No proposal or section identifier is in scope in this method and the signature is
        // fixed, so the token names are the whole message — they are enough to grep for.
        String residualPattern = "\\{\\{[A-Za-z0-9_]+\\}\\}";
        Matcher residual = Pattern.compile(residualPattern).matcher(result);
        if (residual.find()) {
            StringBuilder unmatched = new StringBuilder(residual.group());
            while (residual.find()) {
                unmatched.append(", ").append(residual.group());
            }
            log.warn("T133 stripped unmatched proposal token(s) before render: {}", unmatched);
            result = result.replaceAll(residualPattern, "");
        }
        return result;
    }
}

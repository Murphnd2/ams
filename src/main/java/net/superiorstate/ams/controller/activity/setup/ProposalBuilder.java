package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.CountyReferenceDAO;
import net.superiorstate.ams.data.dao.ProposalIchraIntakeDAO;
import net.superiorstate.ams.data.dao.ProposalIchraSnapshotDAO;
import net.superiorstate.ams.data.dao.RateCacheDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.AgencyScope;
import net.superiorstate.ams.data.resolver.AgencyScopeResolver;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.market.CountyReference;
import net.superiorstate.ams.model.market.RatingAreaRateCache;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "ProposalBuilder", value = "/ProposalBuilder")
public class ProposalBuilder extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        EntityManager em = getEntityManager(request);
        em.clear();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        int pspId = local.getCurrentPerson().getPsp().getId().intValue();
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));

        try {
            // ── Pass role flags to JSP ──
            request.setAttribute("isPspAdmin", isPspAdmin);
            request.setAttribute("isAgent", isAgent);
            request.setAttribute("isAgencyAdmin", isAgencyAdmin);

            // ── Load active agencies for this PSP (needed for PSP admin new-prospect modal) ──
            List<Agency> agencyList = SalesDAO.getActiveAgencyList(em, pspId);
            request.setAttribute("agencyList", agencyList);

            // ── Resolve the current user's agency (if they belong to one) ──
            Agency userAgency = null;
            if (isAgent || isAgencyAdmin) {
                AgencyScope userScope = AgencyScopeResolver.resolve(em, request);
                userAgency = AgencyScopeResolver.primaryAgencyEntity(em, userScope);
            }
            request.setAttribute("userAgency", userAgency);

            // ── Load rates — filter by agency for agent/agencyAdmin roles ──
            List<Rate> allRates;
            if (isPspAdmin) {
                // PSP Admin sees all non-suppressed rates
                allRates = SalesDAO.getRateList(em, pspId);
                allRates.removeIf(Rate::isSuppressed);
            } else if (userAgency != null) {
                // Agent or Agency Manager sees only their agency's assigned rates
                Agency fullAgency = SalesDAO.getAgencyFull(em, userAgency.getId());
                allRates = fullAgency.getAgencyRateList() != null
                        ? new ArrayList<>(fullAgency.getAgencyRateList())
                        : new ArrayList<>();
                allRates.removeIf(Rate::isSuppressed);
            } else {
                allRates = new ArrayList<>();
            }
            request.setAttribute("allRates", allRates);

            // If only one rate, auto-select it
            if (allRates.size() == 1) {
                request.setAttribute("autoSelectedRateId", allRates.get(0).getId());
            }

            // ── Load all LOS for this PSP — filter out suppressed ──
            List<LOS> losList = em.createNamedQuery("LOS.getByPsp", LOS.class)
                    .setParameter("psp_id", (long) pspId)
                    .getResultList();
            losList.removeIf(LOS::isSuppressed);
            request.setAttribute("losList", losList);

            // ── T125: resolve ICHRA entitlement once — the plus-tier intake panel and its
            // data-plus-tier LOS markers are both gated on this single flag (rule 2,
            // invisible by default). Live, per-request, fails closed; never cached. ──
            boolean ichraAvailable = IchraAccessResolver.isAvailable(em, request);
            request.setAttribute("ichraAvailable", ichraAvailable);
            if (ichraAvailable) {
                // Plan year is DERIVED, never agent-asserted (S10-B HS-1) — the same source
                // IllustrationServlet.resolvePlanYear() reads. Only resolved for an entitled
                // session since it is only ever used by the intake panel. If unconfigured,
                // the panel itself stays hidden below rather than collect data that
                // attachIchraIntakeIfPresent will refuse to persist at submit time.
                request.setAttribute("ichraPlanYear", resolveCurrentPlanYear(em));
            }

            // ── Build rate → LOS availability map ──
            // Only include LOSs that have at least one fee line item (RateTable row) in the rate
            Map<Long, Set<Long>> rateLosMap = new HashMap<>();
            for (Rate rate : allRates) {
                List<RateTable> rtRows = SalesDAO.getRateTableList(em, rate.getId());
                Set<Long> availableLosIds = new HashSet<>();
                for (RateTable rt : rtRows) {
                    ServiceModule mod = rt.getModule();
                    if (mod != null && mod.getLos() != null) {
                        availableLosIds.add(mod.getLos().getId());
                    }
                }
                rateLosMap.put(rate.getId(), availableLosIds);
            }

            // Serialize to JSON string for the JSP
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<Long, Set<Long>> entry : rateLosMap.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":[");
                json.append(entry.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")));
                json.append("]");
                first = false;
            }
            json.append("}");
            request.setAttribute("rateLosMapJson", json.toString());

            // Support both parameter names for pre-selection (read early for auto-expand logic)
            String selectedProspect = request.getParameter("prospectId");
            if (selectedProspect == null) selectedProspect = request.getParameter("selectedProspect");

            // ── Load prospects — role-based scoping ──
            List<Prospect> prospectList;
            boolean canExpand = false;  // whether the user can toggle to see more prospects

            if (isPspAdmin) {
                // PSP Admin default: all prospects from the PSP admin's own agency (if they have one)
                // with a button to expand to ALL prospects across all agencies
                AgencyScope pspUserScope = AgencyScopeResolver.resolve(em, request);
                Agency pspUserAgency = AgencyScopeResolver.primaryAgencyEntity(em, pspUserScope);
                if (pspUserAgency != null && pspUserAgency.getAgentList() != null) {
                    List<Long> agentIds = pspUserAgency.getAgentList().stream()
                            .map(Person::getId).collect(Collectors.toList());
                    prospectList = em.createQuery(
                                    "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id IN :agentIds ORDER BY p.name",
                                    Prospect.class)
                            .setParameter("pspId", (long) pspId)
                            .setParameter("agentIds", agentIds)
                            .getResultList();
                    request.setAttribute("defaultAgencyId", pspUserAgency.getId());
                } else {
                    // PSP admin not in any agency — just show all by default
                    prospectList = SalesDAO.getProspectsByPsp(em, pspId);
                }
                // Always load the full list for the "Show All" expansion — exclude prospects from suppressed agencies
                List<Prospect> allProspects = SalesDAO.getProspectsByPsp(em, pspId);
                // Build set of agent IDs that belong to at least one active agency
                Set<Long> activeAgentIds = new HashSet<>();
                for (Agency ag : agencyList) {
                    Agency fullAg = SalesDAO.getAgencyFull(em, ag.getId());
                    if (fullAg.getAgentList() != null) {
                        fullAg.getAgentList().forEach(a -> activeAgentIds.add(a.getId()));
                    }
                }
                allProspects.removeIf(p -> p.getAgent() == null || !activeAgentIds.contains(p.getAgent().getId()));
                request.setAttribute("allProspects", allProspects);
                canExpand = true;

            } else if (isAgencyAdmin) {
                // Agency Manager default: their own prospects
                prospectList = em.createQuery(
                                "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id = :agentId ORDER BY p.name",
                                Prospect.class)
                        .setParameter("pspId", (long) pspId)
                        .setParameter("agentId", local.getCurrentPerson().getId())
                        .getResultList();

                // Load all agency prospects for "Show All Agency" expansion
                if (userAgency != null && userAgency.getAgentList() != null) {
                    List<Long> agentIds = userAgency.getAgentList().stream()
                            .map(Person::getId).collect(Collectors.toList());
                    List<Prospect> agencyProspects = em.createQuery(
                                    "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id IN :agentIds ORDER BY p.name",
                                    Prospect.class)
                            .setParameter("pspId", (long) pspId)
                            .setParameter("agentIds", agentIds)
                            .getResultList();
                    request.setAttribute("allProspects", agencyProspects);
                    canExpand = agencyProspects.size() > prospectList.size();
                }

            } else if (isAgent) {
                // Agent sees only their own prospects — no expansion
                prospectList = em.createQuery(
                                "SELECT p FROM Prospect p WHERE p.contact.psp.id = :pspId AND p.agent.id = :agentId ORDER BY p.name",
                                Prospect.class)
                        .setParameter("pspId", (long) pspId)
                        .setParameter("agentId", local.getCurrentPerson().getId())
                        .getResultList();

            } else {
                prospectList = new ArrayList<>();
            }

            // If a prospect is pre-selected but not in the default list, auto-expand
            boolean autoExpand = false;
            if (selectedProspect != null && !selectedProspect.isEmpty()) {
                long selId;
                try { selId = Long.parseLong(selectedProspect); } catch (NumberFormatException e) { selId = -1; }
                boolean found = false;
                for (Prospect p : prospectList) {
                    if (p.getId().equals(selId)) { found = true; break; }
                }
                if (!found && canExpand) {
                    // Switch to the expanded list so the selected prospect is visible
                    List<Prospect> allProspects = (List<Prospect>) request.getAttribute("allProspects");
                    if (allProspects != null) {
                        prospectList = allProspects;
                        autoExpand = true;
                    }
                }
            }

            request.setAttribute("prospectList", prospectList);
            request.setAttribute("canExpand", canExpand);
            request.setAttribute("autoExpand", autoExpand);

            // ── Load agent list for New Prospect modal ──
            // PSP Admin: needs agency dropdown + agent sub-dropdown (agents loaded per agency via JS, but seed with first agency)
            // Agency Manager: agent list from their agency
            // Agent: no dropdown needed (auto-assigned to self)
            if (isPspAdmin) {
                // Pass all agencies (already set above as agencyList)
                // Build a JSON map of agencyId → [{id, name}, ...] for JS-driven agent sub-dropdown
                StringBuilder agentMapJson = new StringBuilder("{");
                boolean agFirst = true;
                for (Agency agency : agencyList) {
                    if (!agFirst) agentMapJson.append(",");
                    agentMapJson.append("\"").append(agency.getId()).append("\":[");
                    Agency fullAg = SalesDAO.getAgencyFull(em, agency.getId());
                    if (fullAg.getAgentList() != null) {
                        boolean pFirst = true;
                        for (Person agent : fullAg.getAgentList()) {
                            if (!pFirst) agentMapJson.append(",");
                            agentMapJson.append("{\"id\":").append(agent.getId())
                                    .append(",\"name\":\"")
                                    .append(agent.getFirstName().replace("\"", "\\\""))
                                    .append(" ")
                                    .append(agent.getLastName().replace("\"", "\\\""))
                                    .append("\"}");
                            pFirst = false;
                        }
                    }
                    agentMapJson.append("]");
                    agFirst = false;
                }
                agentMapJson.append("}");
                request.setAttribute("agentMapJson", agentMapJson.toString());

            } else if (isAgencyAdmin && userAgency != null) {
                // Agency Manager: pass agent list for their agency
                Agency fullAgency = SalesDAO.getAgencyFull(em, userAgency.getId());
                List<Person> agencyAgents = fullAgency.getAgentList() != null
                        ? new ArrayList<>(fullAgency.getAgentList())
                        : new ArrayList<>();
                Collections.sort(agencyAgents);
                request.setAttribute("agencyAgents", agencyAgents);
            }
            // Agent: no agent list needed — CreateProspect will use the current user

            // Pass current user ID for default selections
            request.setAttribute("currentUserId", local.getCurrentPerson().getId());

            request.setAttribute("selectedProspect", selectedProspect);

            // ── Build prospect→agencyIds and agency→rateIds JSON for client-side rate filtering ──
            // Query DB directly (not global cache) to ensure newly-created prospects are included
            List<Object[]> prospectAgencyRows = SalesDAO.getProspectAgencyData(em);
            Map<Long, String> pam = new HashMap<>();
            Map<Long, List<Long>> tempPam = new HashMap<>();
            for (Object[] row : prospectAgencyRows) {
                tempPam.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add((Long) row[1]);
            }
            for (Map.Entry<Long, List<Long>> entry : tempPam.entrySet()) {
                pam.put(entry.getKey(), entry.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
            Map<Long, List<Long>> arm = SalesDAO.getAgencyRateMap(em);

            // prospectAgencyMap JSON: { "prospectId": "agencyId,agencyId", ... }
            StringBuilder pamJson = new StringBuilder("{");
            if (pam != null) {
                boolean pFirst = true;
                for (Map.Entry<Long, String> e : pam.entrySet()) {
                    if (!pFirst) pamJson.append(",");
                    pamJson.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
                    pFirst = false;
                }
            }
            pamJson.append("}");
            request.setAttribute("prospectAgencyMapJson", pamJson.toString());

            // agencyRateMap JSON: { "agencyId": [rateId, rateId], ... }
            StringBuilder armJson = new StringBuilder("{");
            if (arm != null) {
                boolean aFirst = true;
                for (Map.Entry<Long, List<Long>> e : arm.entrySet()) {
                    if (!aFirst) armJson.append(",");
                    armJson.append("\"").append(e.getKey()).append("\":[");
                    armJson.append(e.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")));
                    armJson.append("]");
                    aFirst = false;
                }
            }
            armJson.append("}");
            request.setAttribute("agencyRateMapJson", armJson.toString());

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/proposalBuilder.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManager em = getEntityManager(request);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        try {
            String action = request.getParameter("action");

            if ("createProposal".equals(action)) {
                Proposal proposal = createProposal(request, em, local);
                response.sendRedirect("ProposalDetail?id=" + proposal.getId());
                return;
            }

        } finally {
            em.close();
        }

        // Redirect back to builder to show updated state
    }

    private Proposal createProposal(HttpServletRequest request, EntityManager em, AmsDataLocal local) {
        // Get prospect
        long prospectId = Long.parseLong(request.getParameter("prospectId"));
        Prospect prospect = em.find(Prospect.class, prospectId);

        // Get rate
        long rateId = Long.parseLong(request.getParameter("rateId"));
        Rate rate = em.find(Rate.class, rateId);

        // Get current user as creator
        Person createdBy = local.getCurrentPerson();

        // Generate GUID
        String guid = UUID.randomUUID().toString();

        // Create proposal
        em.getTransaction().begin();
        Proposal proposal = new Proposal();
        proposal.setProspect(prospect);
        proposal.setRate(rate);
        proposal.setApplicationGUID(guid);
        proposal.setStatus("CREATED");
        proposal.setCreatedBy(createdBy);
        proposal.setInactive(false);
        proposal.setLosList(new ArrayList<>());

        // Link to source opportunity if creating from an opportunity context
        String sourceActivityIdStr = request.getParameter("sourceActivityId");
        if (sourceActivityIdStr != null && !sourceActivityIdStr.isEmpty()) {
            try {
                long sourceActivityId = Long.parseLong(sourceActivityIdStr);
                Activity sourceActivity = em.find(Activity.class, sourceActivityId);
                if (sourceActivity != null) {
                    proposal.setSourceActivity(sourceActivity);
                }
            } catch (NumberFormatException ignored) {}
        }

        em.persist(proposal);
        em.getTransaction().commit();

        // Build-plan item 6: ICHRA illustration snapshot. Only when the hand-off's
        // inputs (countyFips + mode) are present — item 7's "Use This in a Proposal"
        // button is the only sender; AgentHome, detailOpportunity25.jsp,
        // CreateOpportunity and direct entry all omit them, so this is a byte-identical
        // no-op for every existing caller and every existing LOS. Best-effort: any
        // failure here must never break proposal creation itself.
        try {
            attachIchraSnapshotIfPresent(request, em, proposal, createdBy);
        } catch (Exception e) {
            System.out.println("ICHRA snapshot attach failed for proposal #" + proposal.getId() + ": " + e.getMessage());
        }

        // Add selected LOSs
        String[] losIds = request.getParameterValues("losIds");
        if (losIds != null) {
            for (String losIdStr : losIds) {
                long losId = Long.parseLong(losIdStr);
                LOS los = SalesDAO.getLosFull(em, losId);
                em.getTransaction().begin();
                proposal.getLosList().add(los);
                los.getListOfProposalsThatIncludeThisLOS().add(proposal);
                em.persist(proposal);
                em.persist(los);
                em.getTransaction().commit();
            }
        }

        // T125 — plus-tier intake (ZIP/county/headcount). Best-effort, exactly like the
        // ICHRA illustration snapshot above: any failure here must never break proposal
        // creation itself.
        try {
            attachIchraIntakeIfPresent(request, em, proposal, createdBy);
        } catch (Exception e) {
            System.out.println("ICHRA intake attach failed for proposal #" + proposal.getId() + ": " + e.getMessage());
        }

        System.out.println("Proposal created: #" + proposal.getId() + " GUID=" + guid);
        return proposal;
    }

    /**
     * T125 — Proposal Builder plus-tier interjection. Writes a {@link ProposalIchraIntake}
     * only when both are true: this session is ICHRA-entitled (re-checked here, live and
     * server-side, since the panel's visibility and the LOS cards' {@code data-plus-tier}
     * markers are both client-controlled and must be treated as an assertion, not a fact),
     * and at least one of the LOS actually attached to this proposal carries
     * {@code los.isPlusTier()} — re-derived from the database rather than trusted from the
     * form, because a caller can POST {@code intakeZip} against a non-plus-tier
     * {@code losIds} selection. Fails closed at every step: a missing or invalid required
     * field, or an unconfigured plan year, means no row is written at all — never a partial
     * one. The one exception is {@code intakeContribution} (T80 half 1, V088), which is
     * optional and simply persists null when absent, invalid, or negative.
     */
    private void attachIchraIntakeIfPresent(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy) {
        if (!IchraAccessResolver.isAvailable(em, request)) {
            return;
        }

        boolean anyPlusTier = false;
        for (LOS los : proposal.getLosList()) {
            if (los.isPlusTier()) {
                anyPlusTier = true;
                break;
            }
        }
        if (!anyPlusTier) {
            return;
        }

        String zip = normalizeFiveDigitCode(request.getParameter("intakeZip"));
        if (zip == null) return;

        String countyFips = normalizeFiveDigitCode(request.getParameter("intakeCountyFips"));
        if (countyFips == null) return;

        String stateRaw = request.getParameter("intakeState");
        if (stateRaw == null || stateRaw.trim().length() != 2) return;
        String state = stateRaw.trim().toUpperCase();

        String countyNameRaw = request.getParameter("intakeCountyName");
        if (countyNameRaw == null || countyNameRaw.isBlank()) return;

        Integer headcount = parseIntOrNull(request.getParameter("intakeHeadcount"));
        if (headcount == null || headcount < 1 || headcount > 10000) return;

        // T80 half 1 — optional, unlike every field above: a missing or unparseable value
        // means the agent didn't answer, and the row is still written without it. A typed
        // negative is also treated as unanswered rather than failing the whole intake write,
        // since this field alone is optional (the browser control already enforces min="0";
        // this is defense against a direct POST).
        BigDecimal monthlyContribution = parseDecimalOrNull(request.getParameter("intakeContribution"));
        if (monthlyContribution != null && monthlyContribution.signum() < 0) {
            monthlyContribution = null;
        }

        // Derived, not agent-asserted (S10-B HS-1) — re-resolved here rather than trusting
        // whatever doGet rendered, since the constant could change between GET and POST.
        Integer planYear = resolveCurrentPlanYear(em);
        if (planYear == null) return;

        ProposalIchraIntake intake = new ProposalIchraIntake();
        intake.setProposal(proposal);
        intake.setZip(zip);
        intake.setCountyFips(countyFips);
        intake.setCountyName(countyNameRaw.trim());
        intake.setState(state);
        intake.setHeadcount(headcount);
        intake.setMonthlyContributionPerEmployee(monthlyContribution);
        intake.setPlanYear(planYear);
        intake.setCollectedAt(LocalDateTime.now());
        intake.setCreatedBy(createdBy);

        ProposalIchraIntakeDAO.save(em, intake);
    }

    /**
     * "Current" plan year, derived rather than agent-asserted (S10-B HS-1) — the same
     * source {@code IllustrationServlet.resolvePlanYear} reads: the first entry of the
     * {@code RATE_CACHE_PLAN_YEARS} constant, tolerant CSV, malformed entries skipped. No
     * second mechanism, no hardcoded year. Null when the constant is missing, blank, or
     * carries no parseable entry — callers treat that as "no year to write against" and
     * fail closed rather than invent one.
     */
    private Integer resolveCurrentPlanYear(EntityManager em) {
        String raw = AppConstantDAO.getConstantValue(em, "RATE_CACHE_PLAN_YEARS");
        if (raw == null || raw.isBlank()) return null;
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException ignored) {
                // malformed entry skipped, same tolerance as IllustrationServlet.parsePlanYears
            }
        }
        return null;
    }

    /** Exactly five digits after trimming, or null. Used for intakeZip/intakeCountyFips. */
    private String normalizeFiveDigitCode(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.length() != 5) return null;
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) return null;
        }
        return trimmed;
    }

    /**
     * Build-plan item 6. Writes a {@link ProposalIchraSnapshot} when the request
     * carries the illustration hand-off's inputs. Fails closed at every step: a
     * missing county, an unparseable plan year, a missing or non-PRODUCTION cache
     * row means no snapshot is written at all — never a partial one, and never one
     * built from the off-exchange-derived figures the market illustration itself
     * uses for display (this snapshot reads the exact same {@code lowestBronzePremium}
     * field {@code IllustrationServlet} does, for the exact same reason: it is
     * recording what the agent saw, not computing affordability).
     */
    private void attachIchraSnapshotIfPresent(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy) {
        String countyFips = request.getParameter("countyFips");
        String mode = request.getParameter("mode");
        if (countyFips == null || countyFips.isBlank() || mode == null || mode.isBlank()) {
            return;
        }

        Integer planYear = parseIntOrNull(request.getParameter("planYear"));
        if (planYear == null) return;

        CountyReference county = CountyReferenceDAO.findByFips(em, countyFips);
        if (county == null) return;

        if (ProposalIchraSnapshot.MODE_AGE_BAND.equals(mode)) {
            attachAgeBandSnapshot(request, em, proposal, createdBy, county, planYear, countyFips);
        } else if (ProposalIchraSnapshot.MODE_RANGE.equals(mode)) {
            attachRangeSnapshot(request, em, proposal, createdBy, county, planYear, countyFips);
        }
        // Any other mode value: not recognized, no snapshot written.
    }

    /**
     * T150 — the step-6 demo override: {@code ICHRA_DEMO_ALLOW_STAGING_PROPOSAL=true} in
     * ssa.properties <b>AND</b> a PSP-admin session. Both, always; either alone is false.
     * <p>
     * Re-evaluated here rather than threaded down from the caller precisely so that no
     * method signature in this class changes. {@code ProposalBuilder} is on every
     * proposal-creation path for every line of service, and both snapshot writers already
     * receive the {@code request} they need.
     * <p>
     * When true, the two provenance guards below admit staging-sourced rates so the snapshot
     * write path can be exercised before production rate data exists. ⚠️ <b>The snapshot is
     * still stamped with its ACTUAL {@code sourceEnv}</b> — see the setters — so
     * {@code ViewProposal}'s public-path gate refuses it permanently and the artifact stays
     * self-identifying. Enabling the demo never produces a client-facing figure.
     * <p>
     * {@code getSession(false)} deliberately — a feature check must never create a session.
     */
    private boolean isIchraDemoOverride(HttpServletRequest request) {
        if (!AppConfig.isIchraDemoStagingAllowed()) {
            return false;
        }
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("isPspAdmin"));
    }

    /** RANGE snapshot — group premium range at ages 21/64 times headcount, mirroring IllustrationServlet.handleRangeMode's own arithmetic. */
    private void attachRangeSnapshot(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy,
                                      CountyReference county, int planYear, String countyFips) {
        Integer headcount = parseIntOrNull(request.getParameter("headcount"));
        if (headcount == null || headcount < 1) return;

        List<RatingAreaRateCache> rows = RateCacheDAO.getRatesForCounty(em, planYear, countyFips);
        Map<Integer, RatingAreaRateCache> byAge = new HashMap<>();
        String sourceEnv = null;
        LocalDateTime fetchedAt = null;
        for (RatingAreaRateCache row : rows) {
            if (row.isUsesTobacco()) continue;
            byAge.putIfAbsent(row.getAge(), row);
            if (row.getSourceEnv() != null) sourceEnv = row.getSourceEnv();
            if (row.getFetchedAt() != null && (fetchedAt == null || row.getFetchedAt().isAfter(fetchedAt))) {
                fetchedAt = row.getFetchedAt();
            }
        }

        // Fail closed — no PRODUCTION-sourced data backing this range, no snapshot.
        // T150: unless the demo override is on (properties flag AND PSP-admin session), in
        // which case staging-sourced rates are admitted so this write path can be exercised.
        // The stamp below stays honest — setSourceEnv gets the real value, not PRODUCTION.
        if (!isIchraDemoOverride(request)
                && !RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(sourceEnv)) return;

        RatingAreaRateCache age21Row = byAge.get(21);
        RatingAreaRateCache age64Row = byAge.get(64);
        if (age21Row == null || age64Row == null
                || age21Row.getLowestBronzePremium() == null || age64Row.getLowestBronzePremium() == null) {
            return;
        }

        BigDecimal groupMonthlyLow = age21Row.getLowestBronzePremium().multiply(BigDecimal.valueOf(headcount));
        BigDecimal groupMonthlyHigh = age64Row.getLowestBronzePremium().multiply(BigDecimal.valueOf(headcount));

        ProposalIchraSnapshot snapshot = new ProposalIchraSnapshot();
        snapshot.setProposal(proposal);
        snapshot.setMode(ProposalIchraSnapshot.MODE_RANGE);
        snapshot.setCountyFips(countyFips);
        snapshot.setState(county.getState());
        snapshot.setCountyName(county.getCountyName());
        snapshot.setPlanYear(planYear);
        snapshot.setHeadcount(headcount);
        snapshot.setGroupMonthlyLow(groupMonthlyLow);
        snapshot.setGroupMonthlyHigh(groupMonthlyHigh);
        snapshot.setSourceEnv(sourceEnv);
        snapshot.setRatesFetchedAt(fetchedAt);
        snapshot.setSnapshotAt(LocalDateTime.now());
        snapshot.setCreatedBy(createdBy);

        ProposalIchraSnapshotDAO.save(em, snapshot, null);
    }

    /** AGE_BAND snapshot — per-row net cost and group total, mirroring IllustrationServlet.handleAgeBandMode's own arithmetic. */
    private void attachAgeBandSnapshot(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy,
                                        CountyReference county, int planYear, String countyFips) {
        BigDecimal contribution = parseDecimalOrNull(request.getParameter("contribution"));
        if (contribution == null || contribution.signum() < 0) return;

        List<int[]> ageCountPairs = new ArrayList<>(); // {age, count}
        for (int i = 1; i <= 6; i++) {
            String ageRaw = request.getParameter("age" + i);
            if (ageRaw == null || ageRaw.isBlank()) continue; // blank age = ignored row, same as the illustration
            Integer age = parseIntOrNull(ageRaw);
            if (age == null || age < 21 || age > 64) continue;
            Integer count = parseIntOrNull(request.getParameter("count" + i));
            if (count == null || count < 1) count = 1;
            ageCountPairs.add(new int[]{age, count});
        }
        if (ageCountPairs.isEmpty()) return;

        List<ProposalIchraSnapshotBand> bands = new ArrayList<>();
        BigDecimal groupNetTotal = BigDecimal.ZERO;
        int totalLives = 0;
        String sourceEnv = null;
        LocalDateTime fetchedAt = null;
        int sortOrder = 1;

        // T150 — hoisted out of the loop: the answer cannot change between rows, and this
        // keeps the per-row cost identical to before.
        boolean demoOverride = isIchraDemoOverride(request);

        for (int[] pair : ageCountPairs) {
            int age = pair[0], count = pair[1];
            RatingAreaRateCache row = RateCacheDAO.getRate(em, planYear, countyFips, age, false);
            // Fail closed — missing cache row: no snapshot. This is a data-completeness
            // check, NOT a provenance check, and the demo override deliberately does not
            // touch it: a missing row means there is no figure to record at all.
            if (row == null || row.getLowestBronzePremium() == null) return;
            // Provenance. T150: staging admitted only under the two-condition override.
            // The stamp below stays honest — setSourceEnv gets the real value.
            if (!demoOverride
                    && !RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(row.getSourceEnv())) return;

            BigDecimal floorPremium = row.getLowestBronzePremium();
            BigDecimal netPerEmployee = floorPremium.subtract(contribution).max(BigDecimal.ZERO);
            BigDecimal bandNet = netPerEmployee.multiply(BigDecimal.valueOf(count));

            ProposalIchraSnapshotBand band = new ProposalIchraSnapshotBand();
            band.setAge(age);
            band.setLives(count);
            band.setFloorPremium(floorPremium);
            band.setNetPerEmployee(netPerEmployee);
            band.setBandNet(bandNet);
            band.setSortOrder(sortOrder++);
            bands.add(band);

            groupNetTotal = groupNetTotal.add(bandNet);
            totalLives += count;
            sourceEnv = row.getSourceEnv();
            if (row.getFetchedAt() != null && (fetchedAt == null || row.getFetchedAt().isAfter(fetchedAt))) {
                fetchedAt = row.getFetchedAt();
            }
        }

        BigDecimal employerOutlay = contribution.multiply(BigDecimal.valueOf(totalLives));

        ProposalIchraSnapshot snapshot = new ProposalIchraSnapshot();
        snapshot.setProposal(proposal);
        snapshot.setMode(ProposalIchraSnapshot.MODE_AGE_BAND);
        snapshot.setCountyFips(countyFips);
        snapshot.setState(county.getState());
        snapshot.setCountyName(county.getCountyName());
        snapshot.setPlanYear(planYear);
        snapshot.setContribution(contribution);
        snapshot.setGroupNetTotal(groupNetTotal);
        snapshot.setEmployerOutlay(employerOutlay);
        snapshot.setSourceEnv(sourceEnv);
        snapshot.setRatesFetchedAt(fetchedAt);
        snapshot.setSnapshotAt(LocalDateTime.now());
        snapshot.setCreatedBy(createdBy);

        ProposalIchraSnapshotDAO.save(em, snapshot, bands);
    }

    private Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseDecimalOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private EntityManager getEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }

}

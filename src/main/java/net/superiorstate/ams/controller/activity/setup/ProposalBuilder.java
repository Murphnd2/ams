package net.superiorstate.ams.controller.activity.setup;

import com.google.gson.*;
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
import net.superiorstate.ams.data.util.AffordabilityCalculator;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "ProposalBuilder", value = "/ProposalBuilder")
public class ProposalBuilder extends HttpServlet {

    /** T165/V090 — machine-readable timestamp format for the ICHRA JSON payload's provenance block. */
    private static final DateTimeFormatter ICHRA_PAYLOAD_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * S19-O — {@code serializeNulls()} deliberately, unlike the plain {@code new Gson()} this
     * replaces. Without it, a default {@code Gson} instance silently omits any object member
     * whose value is {@code JsonNull.INSTANCE} when writing a {@code JsonElement} tree, which
     * defeated {@code buildIchraPayload}'s own intent of a stable top-level key set (six as of
     * S20-B/V091: {@code schemaVersion}/{@code provenance}/{@code affordability}/
     * {@code ageBands}/{@code planLandscape}/{@code sections}) with absent sub-blocks explicit
     * as {@code null}: "this proposal had no affordability data" and "this
     * payload predates the affordability block" were indistinguishable as shipped — exactly
     * the distinction {@code schemaVersion} and a stable key set exist to preserve. Confirmed
     * safe against {@code ViewProposal.putIchraPayloadTokens}, the payload's one reader: every
     * check there is {@code payload.has(key) && payload.get(key).isJsonObject()/.isJsonArray()}
     * — a {@code JsonNull} value fails the type check exactly like an absent key does, so a
     * present-but-null member and an absent member already behaved identically to that reader.
     */
    private static final Gson ICHRA_PAYLOAD_GSON = new GsonBuilder().serializeNulls().create();

    /**
     * S19-I — age/count pairs this servlet reads, emits and echoes. Must stay equal to
     * {@code IllustrationServlet.AGE_BAND_ROWS} (6) and to the number of age/count hidden
     * field pairs in {@code proposalBuilder.jsp}: the illustration's own cap comment warns
     * that raising one without the others silently drops the extra rows from every proposal
     * snapshot. A form-field cap, not a reference-row id.
     */
    private static final int ICHRA_AGE_BAND_ROWS = 6;

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

                // S19-G — prefill only, for the plus-tier intake panel. The illustration
                // hand-off posts UN-prefixed countyFips/headcount/zip (see the hand-off's own
                // comment above the c:url in illustration25.jsp); attachIchraIntakeIfPresent
                // reads intake*-prefixed params at submit time and is untouched by this. Every
                // value is validated with the same helpers the POST-time writers already use
                // (normalizeFiveDigitCode / parseIntOrNull) so only a vetted 5-digit code or a
                // vetted positive integer is ever echoed into the page — never a raw,
                // attacker-controllable query-string value. Missing or malformed input yields
                // a null attribute, which the JSP renders as today's empty field; a direct
                // visit to ProposalBuilder with no ICHRA params carries none of these
                // parameters at all and is unaffected.
                request.setAttribute("handoffZip", normalizeFiveDigitCode(request.getParameter("zip")));
                request.setAttribute("handoffCountyFips", normalizeFiveDigitCode(request.getParameter("countyFips")));
                Integer handoffHeadcount = parseIntOrNull(request.getParameter("headcount"));
                request.setAttribute("handoffHeadcount",
                        (handoffHeadcount != null && handoffHeadcount >= 1 && handoffHeadcount <= 10000) ? handoffHeadcount : null);

                // S19-I — the AGE_BAND hand-off's age{i}/count{i} pairs, same validated-only
                // discipline as the three values above. Bounds mirror the illustration's own
                // controls exactly (age 21-64; a blank/invalid count defaults to 1, matching
                // attachAgeBandSnapshot's own parse and the illustration's W15 real-default
                // convention). Emitted as a JSON array literal built from parsed ints only —
                // no request text is ever concatenated in, so this cannot carry markup or a
                // quote out of the query string; same hand-built-JSON convention as
                // rateLosMapJson below. An absent/malformed pair is skipped, so a page with no
                // band parameters simply receives [].
                request.setAttribute("handoffBandsJson", buildHandoffBandsJson(request));
                request.setAttribute("ichraAgeBandMaxRows", ICHRA_AGE_BAND_ROWS);
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
        if (headcount == null) {
            // S19-O — band-only intake. S19-I's UI hides #intakeHeadcount and shows only a
            // read-only derived total when bands are in use, so the real input submits blank.
            // Falls back to the sum of the entered band counts — derived arithmetic on values
            // the agent typed, never a placeholder. sumIntakeBandCounts mirrors the JSP's own
            // ichraBandTotalLives() field-for-field (same intakeCount{i} names, same "count
            // >= 1" rule, no age check in either), so this can never disagree with the number
            // the agent actually saw on screen before clicking Create.
            headcount = sumIntakeBandCounts(request);
        }
        if (headcount == null || headcount < 1 || headcount > 10000) return;

        // T80 half 1 — optional, unlike every field above: a missing or unparseable value
        // means the agent didn't answer, and the row is still written without it. A typed
        // negative is also treated as unanswered rather than failing the whole intake write,
        // since this field alone is optional (the browser control already enforces min="0";
        // this is defense against a direct POST).
        BigDecimal monthlyContribution = parseOptionalNonNegativeDecimal(request.getParameter("intakeContribution"));

        // S20-B/V091 — the four section selections, via the one place that derives them
        // (§8.4's server-side rule) so this write and buildSectionsBlock's payload copy can
        // never disagree. None of these may block the intake write (spec §7 edit 6): a
        // section left unselected, or selected with incomplete inputs, is a normal outcome
        // recorded in payload_json.sections, not a reason to fail closed here.
        IchraSectionSelections sections = resolveSectionSelections(request);

        // Section 3 (ICHRA_COMPARISON) — the employer's current group plan cost, as reported
        // by the agent. Same "negative collapses to unanswered" rule as monthlyContribution
        // above, and for the same reason: optional at the database level, and a direct POST
        // must not be trusted to respect the browser's min="0".
        BigDecimal currentTotalPremium = parseOptionalNonNegativeDecimal(request.getParameter("intakeCurrentTotalPremium"));
        BigDecimal currentEmployerShare = parseOptionalNonNegativeDecimal(request.getParameter("intakeCurrentEmployerShare"));

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
        intake.setSectionMarket(sections.market());
        intake.setSectionContribution(sections.contribution());
        intake.setSectionComparison(sections.comparison());
        intake.setSectionAffordability(sections.affordability());
        intake.setCurrentTotalMonthlyPremium(currentTotalPremium);
        intake.setCurrentEmployerMonthlyShare(currentEmployerShare);
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

    /**
     * S19-I — the hand-off's validated age/count pairs as a JSON array literal for the intake
     * block's repeater. Only parsed ints reach the output; request text never does. See the
     * call site in {@code doGet} for the validation rationale.
     */
    private String buildHandoffBandsJson(HttpServletRequest request) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (int i = 1; i <= ICHRA_AGE_BAND_ROWS; i++) {
            Integer age = parseIntOrNull(request.getParameter("age" + i));
            if (age == null || age < 21 || age > 64) continue;
            Integer count = parseIntOrNull(request.getParameter("count" + i));
            if (count == null || count < 1) count = 1;
            if (count > 10000) continue;
            if (!first) json.append(',');
            json.append("{\"age\":").append(age).append(",\"count\":").append(count).append('}');
            first = false;
        }
        return json.append(']').toString();
    }

    /**
     * S19-I — one ICHRA input, resolved across the two namespaces this form carries.
     * <p>
     * The illustration hand-off echoes UN-prefixed {@code countyFips/headcount/contribution/
     * age{i}/count{i}} hidden fields; the plus-tier intake block posts its own
     * {@code intake*}-prefixed fields (deliberately distinct — see the intake panel's own
     * comment in {@code proposalBuilder.jsp}, reusing a name would silently collide). The
     * hand-off wins when present so an untouched hand-off behaves exactly as it did before
     * this run; the intake value is the fallback, which is what makes a standalone intake —
     * no illustration, no URL parameters — able to produce a snapshot at all.
     */
    private String ichraParam(HttpServletRequest request, String handoffName, String intakeName) {
        String handoffValue = request.getParameter(handoffName);
        if (handoffValue != null && !handoffValue.isBlank()) return handoffValue;
        return request.getParameter(intakeName);
    }

    /**
     * S19-I — the snapshot mode a standalone intake implies, or null when the intake block
     * submitted nothing usable (every field blank, or the panel never rendered).
     * <p>
     * Mirrors the illustration's own rule that mode is DERIVED from whether any age band is
     * present, rather than asserted separately — see {@code illustration25.jsp}'s K3-b note.
     * Only ever consulted when the URL carries no explicit {@code mode}, so a hand-off's
     * declared mode is never overridden by it.
     */
    private String deriveIntakeMode(HttpServletRequest request) {
        for (int i = 1; i <= ICHRA_AGE_BAND_ROWS; i++) {
            String age = request.getParameter("intakeAge" + i);
            if (age != null && !age.isBlank()) {
                return ProposalIchraSnapshot.MODE_AGE_BAND;
            }
        }
        String headcount = request.getParameter("intakeHeadcount");
        if (headcount != null && !headcount.isBlank()) {
            return ProposalIchraSnapshot.MODE_RANGE;
        }
        return null;
    }

    /**
     * S19-O — sum of {@code intakeCount1..N}, or null if none is present. Deliberately mirrors
     * {@code proposalBuilder.jsp}'s {@code ichraBandTotalLives()} exactly: same field names,
     * same "count >= 1" rule, no age check in either — so this can never derive a different
     * total than the one already displayed to the agent as "N from bands".
     */
    private Integer sumIntakeBandCounts(HttpServletRequest request) {
        int total = 0;
        boolean any = false;
        for (int i = 1; i <= ICHRA_AGE_BAND_ROWS; i++) {
            Integer count = parseIntOrNull(request.getParameter("intakeCount" + i));
            if (count != null && count >= 1) {
                total += count;
                any = true;
            }
        }
        return any ? total : null;
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
        // S19-I — resolved across both namespaces (see ichraParam). Before this run all three
        // came from the illustration hand-off's URL parameters only, so a plus-tier intake
        // filled in by hand — with no illustration behind it — wrote an intake row and never
        // a snapshot, which meant payload_json.ageBands could never be non-null on that path.
        // A hand-off still wins every field it supplies, so an untouched hand-off is
        // bit-identical to its pre-S19-I behaviour.
        String countyFips = ichraParam(request, "countyFips", "intakeCountyFips");
        String mode = request.getParameter("mode");
        if (mode == null || mode.isBlank()) {
            mode = deriveIntakeMode(request);
        }
        if (countyFips == null || countyFips.isBlank() || mode == null || mode.isBlank()) {
            return;
        }

        // Derived rather than agent-asserted when the hand-off does not carry one — the same
        // source and the same fail-closed posture attachIchraIntakeIfPresent already uses.
        Integer planYear = parseIntOrNull(request.getParameter("planYear"));
        if (planYear == null) planYear = resolveCurrentPlanYear(em);
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
        // S19-I — hand-off value first, intake block's field as fallback (see ichraParam).
        Integer headcount = parseIntOrNull(ichraParam(request, "headcount", "intakeHeadcount"));
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

        // S20-B/V091 — sections 2/3 (contribution scenarios, group comparison) are a fidelity
        // upgrade over RANGE mode's group-level figures, never gated on AGE_BAND — spec §2.
        // Parsed here purely for sections-block completeness (§4.3); RANGE mode's own
        // structured columns (groupNetTotal/employerOutlay are AGE_BAND-only) are unchanged.
        BigDecimal contribution = parseOptionalNonNegativeDecimal(ichraParam(request, "contribution", "intakeContribution"));
        BigDecimal currentTotalPremium = parseOptionalNonNegativeDecimal(request.getParameter("intakeCurrentTotalPremium"));
        BigDecimal currentEmployerShare = parseOptionalNonNegativeDecimal(request.getParameter("intakeCurrentEmployerShare"));

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
        snapshot.setContribution(contribution);
        snapshot.setSourceEnv(sourceEnv);
        snapshot.setRatesFetchedAt(fetchedAt);
        snapshot.setSnapshotAt(LocalDateTime.now());
        snapshot.setCreatedBy(createdBy);
        // T165/V090 — RANGE mode has no per-age loop (S19-D open question 1, decided
        // S19-E §2): ageBands stays null rather than inventing per-age data this mode
        // never collected.
        snapshot.setPayloadJson(buildIchraPayload(request, em, planYear, countyFips, county.getCountyName(),
                sourceEnv, fetchedAt, null, contribution, currentTotalPremium, currentEmployerShare));

        ProposalIchraSnapshotDAO.save(em, snapshot, null);
    }

    /** AGE_BAND snapshot — per-row net cost and group total, mirroring IllustrationServlet.handleAgeBandMode's own arithmetic. */
    private void attachAgeBandSnapshot(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy,
                                        CountyReference county, int planYear, String countyFips) {
        // S19-I — hand-off value first, intake block's field as fallback (see ichraParam).
        // S19-J — contribution is now OPTIONAL. The illustration itself treats it as an
        // optional fidelity step (adding one unlocks net-cost math; it does not gate the
        // tool), and since S19-E this snapshot is also the carrier for payload_json —
        // provenance/ageBands/planLandscape — none of which need a contribution. A negative
        // entered value collapses to "unanswered" (null) rather than blocking the whole
        // write, mirroring attachIchraIntakeIfPresent's own established convention for this
        // exact field's intake-side counterpart (T80 half 1): a typed negative is treated as
        // no answer, not as a reason to refuse everything else the agent supplied.
        BigDecimal contribution = parseOptionalNonNegativeDecimal(ichraParam(request, "contribution", "intakeContribution"));

        // S20-B/V091 — section 3 (ICHRA_COMPARISON) inputs, parsed here purely for
        // sections-block completeness (§4.3); no structured column on this snapshot mode
        // stores either figure — spec §6 puts both on proposal_ichra_intake only.
        BigDecimal currentTotalPremium = parseOptionalNonNegativeDecimal(request.getParameter("intakeCurrentTotalPremium"));
        BigDecimal currentEmployerShare = parseOptionalNonNegativeDecimal(request.getParameter("intakeCurrentEmployerShare"));

        List<int[]> ageCountPairs = new ArrayList<>(); // {age, count}
        for (int i = 1; i <= ICHRA_AGE_BAND_ROWS; i++) {
            String ageRaw = ichraParam(request, "age" + i, "intakeAge" + i);
            if (ageRaw == null || ageRaw.isBlank()) continue; // blank age = ignored row, same as the illustration
            Integer age = parseIntOrNull(ageRaw);
            if (age == null || age < 21 || age > 64) continue;
            Integer count = parseIntOrNull(ichraParam(request, "count" + i, "intakeCount" + i));
            if (count == null || count < 1) count = 1;
            ageCountPairs.add(new int[]{age, count});
        }
        if (ageCountPairs.isEmpty()) return;

        List<ProposalIchraSnapshotBand> bands = new ArrayList<>();
        // S19-J — null, not BigDecimal.ZERO, until a real contribution proves a real net
        // figure. ProposalIchraSnapshot.groupNetTotal/employerOutlay are nullable columns;
        // rendering a computed "$0.00" here would claim a net cost of zero that was never
        // actually derived, which is exactly the fabrication this run must not commit.
        BigDecimal groupNetTotal = (contribution != null) ? BigDecimal.ZERO : null;
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

            ProposalIchraSnapshotBand band = new ProposalIchraSnapshotBand();
            band.setAge(age);
            band.setLives(count);
            band.setFloorPremium(floorPremium);
            band.setSortOrder(sortOrder++);

            // S19-J — netPerEmployee/bandNet are NOT NULL columns on
            // ProposalIchraSnapshotBand (schema unchanged by this run, no migration written).
            // Both are net-of-contribution figures by definition, so without a contribution
            // there is nothing honest to put in either — left unset here (this band object is
            // then never persisted; see the save() call below) rather than filled with a zero
            // or the raw floor premium standing in for "net". The payload's ageBands array
            // still gets this band — it reads only age/lives/floorPremium, never these two.
            if (contribution != null) {
                BigDecimal netPerEmployee = floorPremium.subtract(contribution).max(BigDecimal.ZERO);
                BigDecimal bandNet = netPerEmployee.multiply(BigDecimal.valueOf(count));
                band.setNetPerEmployee(netPerEmployee);
                band.setBandNet(bandNet);
                groupNetTotal = groupNetTotal.add(bandNet);
            }
            bands.add(band);

            totalLives += count;
            sourceEnv = row.getSourceEnv();
            if (row.getFetchedAt() != null && (fetchedAt == null || row.getFetchedAt().isAfter(fetchedAt))) {
                fetchedAt = row.getFetchedAt();
            }
        }

        BigDecimal employerOutlay = (contribution != null)
                ? contribution.multiply(BigDecimal.valueOf(totalLives)) : null;

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
        // T165/V090 — bands (age/lives/floorPremium only, never netPerEmployee/bandNet) feeds
        // the payload's ageBands array regardless of whether contribution is present; the
        // payload's ageBands per S19-F's schema was never contribution-dependent in the first
        // place. See S19D_ichra_payload_spec.md §3.
        snapshot.setPayloadJson(buildIchraPayload(request, em, planYear, countyFips, county.getCountyName(),
                sourceEnv, fetchedAt, bands, contribution, currentTotalPremium, currentEmployerShare));

        // S20-B/V091 — bands now persist unconditionally. net_per_employee/band_net were
        // widened off NOT NULL (spec §6 item 4) precisely because sections 1 and 3 are both
        // available with no contribution at all; before this the structured table silently
        // dropped every contribution-less AGE_BAND proposal's band rows (spec §1.6). Net
        // figures on each band stay null when no contribution was entered — set above, never
        // backfilled with a zero standing in for "not computed".
        ProposalIchraSnapshotDAO.save(em, snapshot, bands);
    }

    /**
     * T165/V090, schemaVersion 2 since S20-B/V091 — serializes
     * {@link ProposalIchraSnapshot#getPayloadJson()}'s schema-versioned JSON payload. See
     * {@code docs/analysis/S19D_ichra_payload_spec.md} §3/§4 and
     * {@code docs/analysis/S20A_ichra_sections_spec.md} §4.
     * <p>
     * {@code provenance} is always populated — every input it needs is already required by the
     * caller to reach this point at all. {@code ageBands} is null when {@code bands} is null or
     * empty (RANGE mode, per S19-E §2's decision — never synthesized). {@code affordability} is
     * null unless {@link #buildAffordabilityBlock} resolves every one of its own inputs — as of
     * S19-F it is a per-band structure keyed the same way {@code ageBands} is, so it aligns with
     * {@code ageBands} entry-for-entry rather than collapsing the whole group into one age (see
     * {@code docs/analysis/S19D_ichra_payload_spec.md}'s dated correction note). {@code
     * planLandscape} is always null — nothing in this build calls HealthSherpa; that is T166's
     * plan-fetch, not this one's. {@code sections} (S20-B/V091, new) is always present with all
     * four keys — see {@link #buildSectionsBlock}.
     * <p>
     * {@code contribution}/{@code currentTotalPremium}/{@code currentEmployerShare} are the
     * section 2/3 inputs, resolved by the caller and passed in purely for the sections block's
     * completeness computation (§4.3 of the S20-A spec) — this method persists none of them to
     * a structured column itself.
     */
    private String buildIchraPayload(HttpServletRequest request, EntityManager em, int planYear, String countyFips,
                                      String countyName, String sourceEnv, LocalDateTime fetchedAt,
                                      List<ProposalIchraSnapshotBand> bands, BigDecimal contribution,
                                      BigDecimal currentTotalPremium, BigDecimal currentEmployerShare) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 2);

        JsonObject provenance = new JsonObject();
        provenance.addProperty("sourceEnv", sourceEnv);
        provenance.addProperty("capturedAt", ICHRA_PAYLOAD_TIMESTAMP.format(LocalDateTime.now()));
        provenance.addProperty("ratesFetchedAt", fetchedAt != null ? ICHRA_PAYLOAD_TIMESTAMP.format(fetchedAt) : null);
        provenance.addProperty("planYear", planYear);
        provenance.addProperty("countyFips", countyFips);
        provenance.addProperty("countyName", countyName);
        root.add("provenance", provenance);

        if (bands != null && !bands.isEmpty()) {
            JsonArray ageBandsArr = new JsonArray();
            for (ProposalIchraSnapshotBand band : bands) {
                JsonObject b = new JsonObject();
                b.addProperty("age", band.getAge());
                b.addProperty("lives", band.getLives());
                b.addProperty("premium", band.getFloorPremium());
                ageBandsArr.add(b);
            }
            root.add("ageBands", ageBandsArr);
        } else {
            root.add("ageBands", JsonNull.INSTANCE);
        }

        JsonObject affordability = buildAffordabilityBlock(request, em, planYear, countyFips, bands);
        root.add("affordability", affordability != null ? affordability : JsonNull.INSTANCE);

        // T166's plan-fetch is not built by this run — nothing supplies plans, so this stays
        // null unconditionally. See S19D_ichra_payload_spec.md §4 (Read), "Out of scope".
        root.add("planLandscape", JsonNull.INSTANCE);

        // S20-B/V091 — always present, all four keys, per docs/analysis/S20A_ichra_sections_spec.md §4.
        root.add("sections", buildSectionsBlock(request, contribution, bands, currentTotalPremium,
                currentEmployerShare, affordability));

        return ICHRA_PAYLOAD_GSON.toJson(root);
    }

    /**
     * S20-B/V091 — the payload's {@code sections} block. See
     * {@code docs/analysis/S20A_ichra_sections_spec.md} §4. {@code selected} is derived via
     * {@link #resolveSectionSelections} — the same derivation the intake write uses, so the
     * two can never disagree. {@code complete} restates §4.3's rules exactly:
     * <ul>
     *   <li>{@code ICHRA_MARKET} — always complete. Reaching this method at all is the proof:
     *       every caller already refused to write a snapshot without a resolvable county,
     *       plan year, and (headcount or ≥1 band).</li>
     *   <li>{@code ICHRA_CONTRIBUTION} — complete iff {@code contribution} is non-null.</li>
     *   <li>{@code ICHRA_COMPARISON} — complete iff {@code contribution},
     *       {@code currentTotalPremium} and {@code currentEmployerShare} are all non-null. No
     *       cross-check that the share doesn't exceed the total — record what was collected,
     *       do not adjudicate it (spec §4.3).</li>
     *   <li>{@code ICHRA_AFFORDABILITY} — complete iff {@code affordability} (this method's own
     *       {@link #buildAffordabilityBlock} result) is non-null, reusing that method's outcome
     *       rather than duplicating its bands/basis/income conditions so the two can never
     *       drift. Always {@code selected: false} in this build — section 4 renders no control
     *       that could select it (build 4 is blocked pending an LA-NN entry).</li>
     * </ul>
     */
    private JsonObject buildSectionsBlock(HttpServletRequest request, BigDecimal contribution,
                                           List<ProposalIchraSnapshotBand> bands, BigDecimal currentTotalPremium,
                                           BigDecimal currentEmployerShare, JsonObject affordability) {
        IchraSectionSelections selections = resolveSectionSelections(request);
        JsonObject sections = new JsonObject();

        sections.add("ICHRA_MARKET", sectionEntry(selections.market(), true, new JsonArray()));

        JsonArray contributionMissing = new JsonArray();
        if (contribution == null) contributionMissing.add("contribution");
        sections.add("ICHRA_CONTRIBUTION",
                sectionEntry(selections.contribution(), contribution != null, contributionMissing));

        JsonArray comparisonMissing = new JsonArray();
        if (contribution == null) comparisonMissing.add("contribution");
        if (currentTotalPremium == null) comparisonMissing.add("currentTotalMonthlyPremium");
        if (currentEmployerShare == null) comparisonMissing.add("currentEmployerMonthlyShare");
        boolean comparisonComplete = contribution != null && currentTotalPremium != null && currentEmployerShare != null;
        sections.add("ICHRA_COMPARISON", sectionEntry(selections.comparison(), comparisonComplete, comparisonMissing));

        // Best-effort diagnostic detail only — section 4 is never selectable in this build, so
        // precision beyond "incomplete" carries no render-path consequence.
        JsonArray affordabilityMissing = new JsonArray();
        if (bands == null || bands.isEmpty()) affordabilityMissing.add("ageBands");
        if (contribution == null) affordabilityMissing.add("contribution");
        String basis = request.getParameter("affordabilityBasis");
        if (!"FPL".equals(basis) && !"INCOME".equals(basis)) {
            affordabilityMissing.add("affordabilityBasis");
        } else if ("INCOME".equals(basis)) {
            String income = request.getParameter("annualIncome");
            if (income == null || income.isBlank()) affordabilityMissing.add("annualIncome");
        }
        sections.add("ICHRA_AFFORDABILITY",
                sectionEntry(selections.affordability(), affordability != null, affordabilityMissing));

        return sections;
    }

    private JsonObject sectionEntry(boolean selected, boolean complete, JsonArray missing) {
        JsonObject entry = new JsonObject();
        entry.addProperty("selected", selected);
        entry.addProperty("complete", complete);
        entry.add("missing", missing);
        return entry;
    }

    /**
     * T165/V090, reshaped S19-F. The {@code affordability} sub-block — per-band, keyed the same
     * way {@code ageBands} is (by {@code age}), so the two align entry-for-entry. Reverses S19-E's
     * age-40 collapse: {@code IllustrationServlet.computeAffordability} (uncalled, unmodified —
     * consulted only as the precedent for its constant lookups and fail-closed shape) computes
     * affordability per age band, never one group-level figure, because on-exchange premiums are
     * age-rated on roughly a 3:1 spread — an offer computed affordable at one age can be
     * unaffordable at another, and that is employer exposure, not a rounding error.
     * <p>
     * {@code applicablePercentage} and {@code incomeBasis} stay block-level: both are genuinely
     * single group-level inputs today — one {@code ICHRA_AFFORDABILITY_PCT_<planYear>} constant,
     * and one {@code annualIncome} request parameter (no JSP submits a per-band income; if one
     * ever does, {@code incomeBasis} would need to move into each band entry, not before). Each
     * band entry carries {@code onExchangeLcspPremium} (the threshold's raw input) and
     * {@code subsidyPreservingCeiling} (the computed flip-contribution) as two separate fields,
     * per LA-15 — never collapsed into one number. A band lacking a cached on-exchange LCSP still
     * gets an entry, with both figures {@code null}, so positional alignment with {@code ageBands}
     * never silently drops an age — mirrors {@code IllustrationServlet}'s own per-row
     * "not cached" disposition rather than failing the whole block closed over one missing row.
     * <p>
     * Returns null (no affordability block at all, {@code bands} array included) when
     * {@code bands} itself is null/empty (RANGE mode, or nothing collected), when no
     * {@code affordabilityBasis} parameter is present, or when the applicable-percentage/income
     * inputs are missing. No default, ever, for any of these.
     */
    private JsonObject buildAffordabilityBlock(HttpServletRequest request, EntityManager em, int planYear, String countyFips,
                                                List<ProposalIchraSnapshotBand> bands) {
        if (bands == null || bands.isEmpty()) return null;

        String basis = request.getParameter("affordabilityBasis");
        if (!"FPL".equals(basis) && !"INCOME".equals(basis)) return null;

        BigDecimal applicablePct = parseDecimalOrNull(AppConstantDAO.getConstantValue(em, "ICHRA_AFFORDABILITY_PCT_" + planYear));
        if (applicablePct == null) return null;

        BigDecimal annualIncome;
        String incomeBasisType;
        if ("FPL".equals(basis)) {
            annualIncome = parseDecimalOrNull(AppConstantDAO.getConstantValue(em, "FPL_ANNUAL_" + planYear));
            incomeBasisType = "FPL_SAFE_HARBOR";
        } else {
            annualIncome = parseDecimalOrNull(request.getParameter("annualIncome"));
            incomeBasisType = "ENTERED";
        }
        if (annualIncome == null) return null;

        JsonArray bandsArr = new JsonArray();
        for (ProposalIchraSnapshotBand band : bands) {
            RatingAreaRateCache referenceRow = RateCacheDAO.getRate(em, planYear, countyFips, band.getAge(), false);
            BigDecimal onexLcsp = referenceRow != null ? referenceRow.getOnexLcspPremium() : null;

            JsonObject bandEntry = new JsonObject();
            bandEntry.addProperty("age", band.getAge());
            if (onexLcsp != null) {
                BigDecimal ceiling = AffordabilityCalculator.flipContribution(onexLcsp, applicablePct, annualIncome);
                bandEntry.addProperty("onExchangeLcspPremium", onexLcsp);
                bandEntry.addProperty("subsidyPreservingCeiling", ceiling);
            } else {
                bandEntry.add("onExchangeLcspPremium", JsonNull.INSTANCE);
                bandEntry.add("subsidyPreservingCeiling", JsonNull.INSTANCE);
            }
            bandsArr.add(bandEntry);
        }

        JsonObject affordability = new JsonObject();
        affordability.addProperty("applicablePercentage", applicablePct);
        JsonObject incomeBasisObj = new JsonObject();
        incomeBasisObj.addProperty("type", incomeBasisType);
        incomeBasisObj.addProperty("annualIncome", annualIncome);
        affordability.add("incomeBasis", incomeBasisObj);
        affordability.add("bands", bandsArr);
        return affordability;
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

    /**
     * S20-B/V091 — parses an optional employer-reported decimal, collapsing a typed negative
     * to "unanswered" rather than treating it as invalid input. This feature's established
     * convention (T80 half 1's {@code intakeContribution}), now shared by every optional
     * decimal field this build adds — a typed negative on any of them means the agent didn't
     * answer, not that the whole write should fail.
     */
    private BigDecimal parseOptionalNonNegativeDecimal(String raw) {
        BigDecimal value = parseDecimalOrNull(raw);
        return (value != null && value.signum() < 0) ? null : value;
    }

    /**
     * S20-B/V091 — the four ICHRA proposal-section selections
     * (docs/analysis/S20A_ichra_sections_spec.md §2/§8.4). {@code market} is derived, not
     * read: it is true whenever any of the other three is true, since market illustration
     * data is the base layer every other section needs, not a peer selection. {@code
     * affordability} is always false — build 4 is blocked pending an LA-NN entry (spec §7)
     * and this build renders no control that could set it.
     */
    private record IchraSectionSelections(boolean market, boolean contribution, boolean comparison, boolean affordability) {}

    /**
     * The one place these four booleans are derived from the request, so
     * {@code attachIchraIntakeIfPresent}'s write and {@code buildSectionsBlock}'s payload
     * copy of the same facts can never disagree.
     */
    private IchraSectionSelections resolveSectionSelections(HttpServletRequest request) {
        boolean secContribution = "on".equals(request.getParameter("sectionContribution"));
        boolean secComparison = "on".equals(request.getParameter("sectionComparison"));
        boolean secAffordability = false; // build 4 blocked -- never read from the request
        boolean secMarket = "on".equals(request.getParameter("sectionMarket"))
                || secContribution || secComparison || secAffordability;
        return new IchraSectionSelections(secMarket, secContribution, secComparison, secAffordability);
    }

    private EntityManager getEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }

}

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Proposal Builder</title>
    <style>
        .los-card { cursor: pointer; transition: all 0.2s ease; border: 2px solid #dee2e6; }
        .los-card:hover { border-color: var(--ssa); box-shadow: 0 2px 8px rgba(13,86,129,0.15); }
        .los-card.selected { border-color: var(--ssa); background-color: #e8f1f8; }
        .los-card.selected .los-check { color: var(--ssa); }
        .los-card.unavailable { display: none !important; }
        .los-check { font-size: 1.25rem; color: #dee2e6; }
        .step-badge { width: 32px; height: 32px; border-radius: 50%; display: inline-flex;
            align-items: center; justify-content: center; font-weight: 600; font-size: 0.875rem; }
        .step-active { background-color: var(--ssa); color: white; }
        .step-complete { background-color: #198754; color: white; }
        .step-pending { background-color: #e9ecef; color: #6c757d; }
        .rate-option { cursor: pointer; transition: all 0.15s ease; }
        .rate-option:hover { background-color: #f8f9fa; }
        .rate-option.selected { background-color: #e8f1f8; border-color: var(--ssa) !important; }
        .los-none-msg { display: none; color: #6c757d; font-style: italic; }
        .intake-msg { display: none; font-size: 0.85rem; }
        .pb-wrap { max-width: 960px; margin: 0 auto; padding: 0 1rem; }
        @media (max-width: 767.98px) { .pb-wrap { padding: 0 0.5rem; } }
        .ghost-back { background: none; border: none; color: white; font-size: 0.85rem;
            text-decoration: none; display: inline-flex; align-items: center; gap: 4px;
            padding: 2px 8px; border-radius: 4px; }
        .ghost-back:hover { background: rgba(255,255,255,0.12); color: white; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Proposal Builder" scope="request"/>
    <c:set var="pageIcon" value="bi-file-earmark-plus" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

    <div class="pb-wrap mt-3">
    <%-- Page Header --%>
    <div class="hdr-bar d-flex align-items-center justify-content-between mb-3">
        <span><i class="bi bi-file-earmark-plus me-2"></i>Proposal Builder</span>
        <div>
            <c:choose>
                <c:when test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
                    <a href="AgentHome" class="ghost-back"><i class="bi bi-arrow-left me-1"></i>Back to Pipeline</a>
                </c:when>
                <c:otherwise>
                    <a href="ViewHome25" class="ghost-back"><i class="bi bi-arrow-left me-1"></i>Back to Dashboard</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <form method="post" action="ProposalBuilder" id="proposalForm">
        <input type="hidden" name="action" value="createProposal">
        <input type="hidden" name="sourceActivityId" value="${param.sourceActivityId}">

        <%-- Build-plan item 6: echo the ICHRA illustration hand-off's inputs (item 7's
             "Use This in a Proposal" button) through to doPost so createProposal can
             attach a snapshot. Absent for every existing entry point — additive only. --%>
        <input type="hidden" name="countyFips" value="${param.countyFips}">
        <input type="hidden" name="mode" value="${param.mode}">
        <input type="hidden" name="planYear" value="${param.planYear}">
        <input type="hidden" name="headcount" value="${param.headcount}">
        <input type="hidden" name="contribution" value="${param.contribution}">
        <input type="hidden" name="age1" value="${param.age1}">
        <input type="hidden" name="count1" value="${param.count1}">
        <input type="hidden" name="age2" value="${param.age2}">
        <input type="hidden" name="count2" value="${param.count2}">
        <input type="hidden" name="age3" value="${param.age3}">
        <input type="hidden" name="count3" value="${param.count3}">
        <input type="hidden" name="age4" value="${param.age4}">
        <input type="hidden" name="count4" value="${param.count4}">
        <input type="hidden" name="age5" value="${param.age5}">
        <input type="hidden" name="count5" value="${param.count5}">
        <input type="hidden" name="age6" value="${param.age6}">
        <input type="hidden" name="count6" value="${param.count6}">

        <%-- STEP 1: Select Prospect --%>
        <div class="card mb-3">
            <div class="card-header bg-white py-3">
                <div class="d-flex align-items-center">
                    <span class="step-badge step-active me-3" id="stepBadge1">1</span>
                    <h5 class="mb-0 fw-semibold">Select Prospect</h5>
                </div>
            </div>
            <div class="card-body">
                <div class="row g-3 align-items-end">
                    <div class="col-md-6">
                        <label for="prospectId" class="form-label">Existing Prospect</label>
                        <select class="form-select" name="prospectId" id="prospectId" onchange="filterRatesByProspect(); updateSteps()">
                            <option value="" selected>-- Choose a prospect --</option>
                            <c:forEach var="prospect" items="${prospectList}">
                                <option value="${prospect.getId()}" ${prospect.getId().toString().equals(selectedProspect) ? 'selected' : ''}>${prospect.getName()}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <%-- Expand button: PSP Admin → "All Agencies", Agency Manager → "All Agency Prospects" --%>
                    <c:if test="${canExpand}">
                        <div class="col-auto">
                            <button type="button" class="btn btn-outline-secondary btn-sm" id="btnExpandProspects" onclick="expandProspects()" title="${isPspAdmin ? 'Show prospects from all agencies' : 'Show all agency prospects'}">
                                <i class="bi bi-people me-1"></i>
                                <c:choose>
                                    <c:when test="${isPspAdmin}">All Agencies</c:when>
                                    <c:otherwise>All Agency</c:otherwise>
                                </c:choose>
                            </button>
                        </div>
                    </c:if>

                    <div class="col-auto">
                        <span class="text-muted">or</span>
                    </div>
                    <div class="col-auto">
                        <button type="button" class="btn btn-outline-primary" data-bs-toggle="modal" data-bs-target="#newProspectModal">
                            <i class="bi bi-plus-lg me-1"></i>New Prospect
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <%-- STEP 2: Select Rate Package --%>
        <div class="card mb-3">
            <div class="card-header bg-white py-3">
                <div class="d-flex align-items-center">
                    <span class="step-badge step-pending me-3" id="stepBadge2">2</span>
                    <h5 class="mb-0 fw-semibold">Select Rate Package</h5>
                </div>
            </div>
            <div class="card-body" id="rateSection">
                <div class="row g-2" id="rateCardGrid" style="display: none;">
                    <c:forEach var="rate" items="${allRates}">
                        <div class="col-md-4 col-sm-6 rate-wrapper" data-rate-id="${rate.getId()}">
                            <div class="card rate-option p-3" onclick="selectRate(this, ${rate.getId()})">
                                <div class="d-flex align-items-center">
                                    <i class="bi bi-circle me-2 rate-icon" style="font-size: 1.1rem;"></i>
                                    <span class="fw-medium">${rate.getDescription()}</span>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
                <p class="text-muted" id="rateSelectProspectMsg"><i class="bi bi-arrow-up-circle me-1"></i>Select a prospect above to see available rate packages.</p>
                <input type="hidden" name="rateId" id="rateId" value="${autoSelectedRateId != null ? autoSelectedRateId : ''}">
            </div>
        </div>

        <%-- STEP 3: Select Lines of Service --%>
        <div class="card mb-3">
            <div class="card-header bg-white py-3">
                <div class="d-flex align-items-center">
                    <span class="step-badge step-pending me-3" id="stepBadge3">3</span>
                    <h5 class="mb-0 fw-semibold">Select Lines of Service</h5>
                </div>
            </div>
            <div class="card-body" id="losSection">
                <p class="text-muted small mb-3">Select the services to include in this proposal.
                    All associated modules and enhancements will be included automatically.</p>
                <div class="row g-2" id="losCardGrid" style="display: none;">
                    <c:forEach var="los" items="${losList}">
                        <div class="col-md-4 col-sm-6 los-card-wrapper" data-los-id="${los.getId()}"<c:if test="${ichraAvailable}"> data-plus-tier="${los.isPlusTier()}"</c:if>>
                            <div class="card los-card p-3" onclick="toggleLos(this, ${los.getId()})">
                                <div class="d-flex align-items-center">
                                    <i class="bi bi-square los-check me-2"></i>
                                    <div>
                                        <div class="fw-medium">${los.getDescription()}</div>
                                        <small class="text-muted">${los.getShortText()}</small>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
                <p class="text-muted" id="losSelectRateMsg"><i class="bi bi-arrow-up-circle me-1"></i>Select a rate package above to see available lines of service.</p>
                <p class="los-none-msg mt-3" id="losNoneMsg">
                    <i class="bi bi-info-circle me-1"></i>No lines of service have pricing configured for the selected rate.
                </p>
                <div id="losInputs"></div>
            </div>
        </div>

        <%-- T125: plus-tier ICHRA intake interjection. Present in the DOM ONLY when this
             session is ICHRA-entitled (rule 2, invisible by default) -- for every other
             agent, or when the RATE_CACHE_PLAN_YEARS constant that plan_year is derived
             from is unconfigured, this <c:if> block emits nothing at all, not a hidden
             container. Shown by JS only when a selected LOS carries data-plus-tier="true"
             (updateIntakePanel()). Field names are intake*-prefixed -- the form already
             posts unprefixed headcount/countyFips/planYear/mode/contribution/age1..6/
             count1..6 above for the illustration hand-off, and reusing one of those names
             would silently collide (request.getParameter returns the first). --%>
        <c:if test="${ichraAvailable and not empty ichraPlanYear}">
        <div class="card mb-3" id="ichraIntakePanel" data-plan-year="${ichraPlanYear}" style="display:none;">
            <div class="card-header bg-white py-3">
                <div class="d-flex align-items-center">
                    <i class="bi bi-clipboard2-pulse me-3" style="font-size:1.25rem; color:var(--ssa);"></i>
                    <h5 class="mb-0 fw-semibold">Additional Info for This Line of Service</h5>
                </div>
            </div>
            <div class="card-body">
                <p class="text-muted small mb-3">This line of service needs the employer's ZIP and eligible employee count before the proposal is created.</p>

                <%-- S20-B/V091 — the four ICHRA proposal-section selections
                     (docs/analysis/S20A_ichra_sections_spec.md §2/§8.6). Placed above the
                     ZIP/county row, since selecting any of these determines what that row
                     requires (Rule B). No ICHRA_AFFORDABILITY control -- build 4 is blocked
                     pending an LA-NN entry (spec §7); rendering a control for it would invite
                     the question every time an agent sees it. The server never trusts these
                     checkboxes (ProposalBuilder resolves selection itself, §8.4) -- this JS is
                     for agent guidance only. --%>
                <div class="mb-3">
                    <label class="form-label mb-1 d-block">What should this proposal include?</label>
                    <div class="form-check">
                        <input type="checkbox" class="form-check-input" id="sectionMarket" name="sectionMarket">
                        <label class="form-check-label" for="sectionMarket">Market illustration data</label>
                    </div>
                    <div class="form-check">
                        <input type="checkbox" class="form-check-input" id="sectionContribution" name="sectionContribution">
                        <label class="form-check-label" for="sectionContribution">Contribution scenarios</label>
                    </div>
                    <div class="form-check">
                        <input type="checkbox" class="form-check-input" id="sectionComparison" name="sectionComparison">
                        <label class="form-check-label" for="sectionComparison">Comparison against their current group plan</label>
                    </div>
                    <p class="text-muted mb-0 mt-1" id="sectionMarketAutoNote" style="font-size:0.78rem; display:none;">
                        <i class="bi bi-info-circle me-1"></i>Market illustration data is included automatically — the other sections are built from it.
                    </p>
                </div>

                <div class="row g-3 align-items-end">
                    <div class="col-auto">
                        <label class="form-label mb-1" for="intakeZip">Employer ZIP</label>
                        <input type="text" class="form-control form-control-sm" id="intakeZip" name="intakeZip"
                               inputmode="numeric" pattern="[0-9]{5}" maxlength="5" placeholder="#####"
                               style="max-width:110px;" autocomplete="off">
                    </div>
                    <div class="col-auto">
                        <label class="form-label mb-1" for="intakeCountyFips">County</label>
                        <select class="form-select form-select-sm" id="intakeCountyFips" name="intakeCountyFips" style="min-width:220px;" disabled>
                            <option value="">-- Enter a ZIP first --</option>
                        </select>
                        <input type="hidden" id="intakeCountyName" name="intakeCountyName" value="">
                        <input type="hidden" id="intakeState" name="intakeState" value="">
                    </div>
                    <%-- S19-I: wrapper id so the band repeater can hide this the moment a band
                         exists -- the illustration's own convention (its #headcountField), hidden
                         rather than removed so nothing is lost switching back, and the servlet
                         ignores headcount in AGE_BAND regardless. --%>
                    <div class="col-auto" id="intakeHeadcountField">
                        <label class="form-label mb-1" for="intakeHeadcount">Eligible Employees</label>
                        <input type="number" class="form-control form-control-sm" id="intakeHeadcount" name="intakeHeadcount"
                               min="1" max="10000" step="1" style="max-width:130px;">
                    </div>
                    <%-- S19-I: derived total shown in place of the input once bands carry the
                         counts, so the figure never simply disappears from the panel. --%>
                    <div class="col-auto" id="intakeDerivedHeadcountField" style="display:none;">
                        <label class="form-label mb-1">Eligible Employees</label>
                        <div class="form-control form-control-sm bg-light" style="max-width:130px;">
                            <span id="intakeDerivedHeadcount">0</span>
                            <span class="text-muted" style="font-size:0.75rem;">from bands</span>
                        </div>
                    </div>
                    <div class="col-auto">
                        <label class="form-label mb-1" for="intakeContribution">Monthly employer contribution per employee</label>
                        <input type="number" class="form-control form-control-sm" id="intakeContribution" name="intakeContribution"
                               min="0" step="0.01" style="max-width:150px;" placeholder="Optional">
                    </div>
                </div>

                <%-- S19-I: the age-band repeater, mirroring the illustration's own (its W7 block)
                     rather than inventing a second convention -- same template-clone structure,
                     same renumber-to-contiguous-1..N parameter contract, same max cap, same
                     "adding one replaces Eligible Employees" behaviour, same age 21-64 bounds and
                     same REAL count default of 1 (its W15: entered and assumed must never be
                     indistinguishable).

                     ⚠️ Names are intakeAge{i}/intakeCount{i} -- intake*-prefixed like every other
                     field in this panel, because the form already posts un-prefixed age{i}/count{i}
                     hidden fields for the illustration hand-off and reusing those names would
                     silently collide (request.getParameter returns the first).

                     ⚠️ The cap is ${ichraAgeBandMaxRows}, matching ProposalBuilder.ICHRA_AGE_BAND_ROWS,
                     IllustrationServlet.AGE_BAND_ROWS, and the six age/count hidden pairs at the top
                     of this form. Raising one without the others silently drops the extra rows. --%>
                <div class="mt-3">
                    <label class="form-label mb-1 d-block">Ages and Headcounts
                        <span class="text-muted fw-normal" style="font-size:0.78rem;">&mdash; optional; adding one replaces Eligible Employees</span>
                    </label>
                    <%-- Unnamed inputs, so the template itself can never submit anything. Its
                         class is swapped on clone, which is what makes a clone count as a row. --%>
                    <div class="intake-band-template align-items-end gap-2 mb-2" id="intakeBandTemplate" style="display:none;" aria-hidden="true">
                        <div>
                            <label class="form-label mb-1" style="font-size:0.7rem;">Age</label>
                            <input type="number" class="form-control form-control-sm intake-band-age" min="21" max="64" style="max-width:100px;">
                        </div>
                        <div>
                            <label class="form-label mb-1" style="font-size:0.7rem;">Count</label>
                            <input type="number" class="form-control form-control-sm intake-band-count" min="1" max="10000" value="1" style="max-width:100px;">
                        </div>
                        <button type="button" class="btn btn-sm btn-outline-secondary intake-band-remove"
                                aria-label="Remove this age band">&times;</button>
                    </div>
                    <div id="intakeBandRows"></div>
                    <button type="button" class="btn btn-sm btn-outline-primary fw-semibold" id="intakeBandAdd"
                            data-max="${ichraAgeBandMaxRows}"><i class="bi bi-plus-lg me-1"></i>Add age band</button>
                    <span class="ms-2 text-muted" id="intakeBandMaxNote" style="display:none; font-size:0.78rem;">
                        Maximum ${ichraAgeBandMaxRows} age bands.
                    </span>
                </div>

                <%-- S20-B/V091 — section 3 (ICHRA_COMPARISON) inputs. Hidden, not removed,
                     following #intakeHeadcountField's own established convention -- and the
                     readiness gate must skip required checks on anything currently hidden
                     (spec §8.7). intake*-prefixed: no hand-off equivalent exists for either
                     field, but the prefix is kept for consistency with every other field in
                     this panel. --%>
                <div class="row g-3 align-items-end mt-1" id="intakeComparisonFields" style="display:none;">
                    <div class="col-auto">
                        <label class="form-label mb-1" for="intakeCurrentTotalPremium">Current total monthly premium (whole group)</label>
                        <input type="number" class="form-control form-control-sm" id="intakeCurrentTotalPremium" name="intakeCurrentTotalPremium"
                               min="0" step="0.01" style="max-width:170px;">
                    </div>
                    <div class="col-auto">
                        <label class="form-label mb-1" for="intakeCurrentEmployerShare">Current employer monthly share (whole group)</label>
                        <input type="number" class="form-control form-control-sm" id="intakeCurrentEmployerShare" name="intakeCurrentEmployerShare"
                               min="0" step="0.01" style="max-width:170px;">
                    </div>
                </div>

                <%-- Never "invalid ZIP" -- the crosswalk is Texas-only and ZCTA-derived, so a
                     perfectly valid USPS ZIP can land here (ZipCountyResolver javadoc). --%>
                <p class="intake-msg mt-2 mb-0" id="intakeNoMatchMsg">
                    <i class="bi bi-info-circle me-1"></i>We don't have ZIP <span id="intakeNoMatchZip"></span> in our county lookup. Try a nearby ZIP, or confirm the county with the employer directly.
                </p>
                <p class="intake-msg mt-2 mb-0 text-muted" id="intakeUnpricedMsg">
                    <i class="bi bi-info-circle me-1"></i>No rate data is cached yet for the selected county -- the proposal will still be created.
                </p>
                <%-- S19-G: shown when the illustration hand-off's countyFips is not among this
                     ZIP's resolved candidates -- never resolved silently either direction; the
                     agent must pick explicitly from what the ZIP actually returns. --%>
                <p class="intake-msg mt-2 mb-0" id="intakeCountyMismatchMsg">
                    <i class="bi bi-exclamation-triangle me-1"></i>This ZIP does not match the county the illustration was built for. Please confirm the correct county below.
                </p>
            </div>
        </div>
        </c:if>

        <%-- Submit --%>
        <div class="row mb-5">
            <div class="col">
                <button type="submit" class="btn btn-primary btn-lg px-5" id="btnCreate" disabled>
                    <i class="bi bi-file-earmark-plus me-2"></i>Create Proposal
                </button>
            </div>
        </div>
    </form>
    </div><%-- /.pb-wrap --%>
</div>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- NEW PROSPECT MODAL — role-conditional fields                       --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="modal fade" id="newProspectModal" tabindex="-1" aria-labelledby="newProspectLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                <h6 class="modal-title fw-semibold" id="newProspectLabel"><i class="bi bi-plus-circle me-2"></i>New Prospect</h6>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form method="post" action="CreateProspect">
                <div class="modal-body">

                    <%-- ── PSP Admin: Agency + Agent dropdowns ── --%>
                    <c:if test="${isPspAdmin}">
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Agency</label>
                            <select class="form-select" name="agencyId" id="modalAgencyId" required onchange="updateAgentDropdown()">
                                <c:forEach var="agency" items="${agencyList}">
                                    <option value="${agency.getId()}" ${agency.getId() == defaultAgencyId ? 'selected' : ''}>${agency.getName()}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Assign to Agent</label>
                            <select class="form-select" name="agentId" id="modalAgentId">
                                <%-- Populated by JS based on agency selection --%>
                            </select>
                        </div>
                    </c:if>

                    <%-- ── Agency Manager: hidden agency, agent dropdown ── --%>
                    <c:if test="${isAgencyAdmin && !isPspAdmin}">
                        <input type="hidden" name="agencyId" value="${userAgency.getId()}">
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Assign to Agent</label>
                            <select class="form-select" name="agentId" id="modalAgentId">
                                <c:forEach var="agt" items="${agencyAgents}">
                                    <option value="${agt.getId()}" ${agt.getId() == currentUserId ? 'selected' : ''}>${agt.getFirstName()} ${agt.getLastName()}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </c:if>

                    <%-- ── Agent: hidden agency, no agent dropdown (auto-assigned to self) ── --%>
                    <c:if test="${isAgent && !isAgencyAdmin && !isPspAdmin}">
                        <input type="hidden" name="agencyId" value="${userAgency != null ? userAgency.getId() : ''}">
                        <%-- agentId not sent — servlet defaults to current user --%>
                    </c:if>

                    <%-- ── Common fields ── --%>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Company Name</label>
                        <input type="text" name="prospectName" class="form-control" required>
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label fw-semibold">Contact First Name</label>
                            <input type="text" name="contactFirst" class="form-control" required>
                        </div>
                        <div class="col-6">
                            <label class="form-label fw-semibold">Contact Last Name</label>
                            <input type="text" name="contactLast" class="form-control" required>
                        </div>
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label fw-semibold">Email</label>
                            <input type="email" name="contactEmail" class="form-control" required>
                        </div>
                        <div class="col-6">
                            <label class="form-label fw-semibold">Phone</label>
                            <input type="text" name="contactPhone" class="form-control">
                        </div>
                    </div>
                </div>
                <div class="modal-footer justify-content-center border-0">
                    <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create Prospect</button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    // ── Rate → available LOS IDs map (built server-side) ──
    const rateLosMap = ${rateLosMapJson};

    // ── Prospect → agency IDs and agency → rate IDs (for rate filtering by prospect) ──
    const prospectAgencyMap = ${prospectAgencyMapJson};
    const agencyRateMap = ${agencyRateMapJson};

    let selectedRateId = null;
    let selectedLosIds = new Set();
    let isExpanded = ${autoExpand != null && autoExpand ? 'true' : 'false'};

    // ── Expanded prospect data (for "Show All" toggle) ──
    <c:if test="${canExpand}">
    const expandedProspects = [
        <c:forEach var="p" items="${allProspects}" varStatus="st">
        {id: '${p.getId()}', name: '${p.getName().replace("'", "\\'")}'${p.getAgent() != null ? ", agent: '".concat(p.getAgent().getFirstName().replace("'", "\\\\'")).concat(" ").concat(p.getAgent().getLastName().replace("'", "\\\\'")).concat("'") : ""}}<c:if test="${!st.last}">,</c:if>
        </c:forEach>
    ];
    const defaultProspects = [
        <c:forEach var="p" items="${prospectList}" varStatus="st">
        {id: '${p.getId()}', name: '${p.getName().replace("'", "\\'")}'}<c:if test="${!st.last}">,</c:if>
        </c:forEach>
    ];
    </c:if>

    function expandProspects() {
        <c:if test="${canExpand}">
        var sel = document.getElementById('prospectId');
        var btn = document.getElementById('btnExpandProspects');
        var currentVal = sel.value;

        isExpanded = !isExpanded;
        var list = isExpanded ? expandedProspects : defaultProspects;

        // Clear and rebuild
        sel.innerHTML = '<option value="">-- Choose a prospect --</option>';
        list.forEach(function(p) {
            var opt = document.createElement('option');
            opt.value = p.id;
            var label = p.name;
            if (isExpanded && p.agent) label += ' (' + p.agent + ')';
            opt.textContent = label;
            if (p.id === currentVal) opt.selected = true;
            sel.appendChild(opt);
        });

        btn.classList.toggle('btn-outline-secondary', !isExpanded);
        btn.classList.toggle('btn-secondary', isExpanded);
        btn.innerHTML = isExpanded
            ? '<i class="bi bi-person me-1"></i>${isPspAdmin ? "My Agency" : "My Prospects"}'
            : '<i class="bi bi-people me-1"></i>${isPspAdmin ? "All Agencies" : "All Agency"}';
        </c:if>
    }

    // ── PSP Admin: agent dropdown driven by agency selection ──
    <c:if test="${isPspAdmin}">
    const agentMap = ${agentMapJson};
    const currentUserId = ${currentUserId};

    function updateAgentDropdown() {
        var agencyId = document.getElementById('modalAgencyId').value;
        var agentSel = document.getElementById('modalAgentId');
        agentSel.innerHTML = '';
        var agents = agentMap[agencyId] || [];
        agents.forEach(function(a) {
            var opt = document.createElement('option');
            opt.value = a.id;
            opt.textContent = a.name;
            if (a.id === currentUserId) opt.selected = true;
            agentSel.appendChild(opt);
        });
    }
    // Initialize on page load
    document.addEventListener('DOMContentLoaded', function() {
        updateAgentDropdown();
    });
    </c:if>

    function filterRatesByProspect() {
        var prospectId = document.getElementById('prospectId').value;
        var wrappers = document.querySelectorAll('.rate-wrapper');
        var rateGrid = document.getElementById('rateCardGrid');
        var rateMsg = document.getElementById('rateSelectProspectMsg');

        if (!prospectId) {
            // No prospect selected — hide rate grid, show message
            rateGrid.style.display = 'none';
            rateMsg.style.display = '';
            return;
        }

        // Prospect selected — show rate grid, hide message
        rateGrid.style.display = '';
        rateMsg.style.display = 'none';

        // Resolve prospect → agency IDs → allowed rate IDs
        var agencyIds = prospectAgencyMap[prospectId];
        var allowedRates = new Set();
        if (agencyIds) {
            agencyIds.split(',').forEach(function(aid) {
                var rates = agencyRateMap[aid.trim()];
                if (rates) rates.forEach(function(rid) { allowedRates.add(rid); });
            });
        }

        if (allowedRates.size === 0) {
            // No agency mapping found — show all rates (fallback for home agency prospects etc.)
            wrappers.forEach(function(w) { w.style.display = ''; });
            return;
        }

        // Show/hide rate cards
        wrappers.forEach(function(w) {
            var rateId = parseInt(w.dataset.rateId);
            w.style.display = allowedRates.has(rateId) ? '' : 'none';
        });

        // If current selection is now hidden, deselect it
        if (selectedRateId !== null && !allowedRates.has(selectedRateId)) {
            selectedRateId = null;
            document.getElementById('rateId').value = '';
            document.querySelectorAll('.rate-option').forEach(function(card) {
                card.classList.remove('selected');
                card.querySelector('.rate-icon').className = 'bi bi-circle me-2 rate-icon';
            });
            // Reset LOS
            selectedLosIds.clear();
            document.querySelectorAll('.los-card').forEach(function(c) {
                c.classList.remove('selected');
                c.querySelector('.los-check').className = 'bi bi-square los-check me-2';
            });
            rebuildLosInputs();
        }

        // Auto-select if only one visible rate
        var visibleWrappers = Array.from(wrappers).filter(function(w) { return w.style.display !== 'none'; });
        if (visibleWrappers.length === 1) {
            var card = visibleWrappers[0].querySelector('.rate-option');
            var rid = parseInt(visibleWrappers[0].dataset.rateId);
            selectRate(card, rid);
        }
    }

    function selectRate(el, rateId) {
        document.querySelectorAll('.rate-option').forEach(function(card) {
            card.classList.remove('selected');
            card.querySelector('.rate-icon').className = 'bi bi-circle me-2 rate-icon';
        });
        el.classList.add('selected');
        el.querySelector('.rate-icon').className = 'bi bi-check-circle-fill me-2 rate-icon text-primary';
        selectedRateId = rateId;
        document.getElementById('rateId').value = rateId;
        filterLosCards(rateId);
        updateSteps();
    }

    function filterLosCards(rateId) {
        var availableIds = rateLosMap[rateId] || [];
        var visibleCount = 0;

        document.getElementById('losSelectRateMsg').style.display = 'none';
        document.getElementById('losCardGrid').style.display = '';

        document.querySelectorAll('.los-card-wrapper').forEach(function(wrapper) {
            var losId = parseInt(wrapper.dataset.losId);
            var card = wrapper.querySelector('.los-card');
            if (availableIds.includes(losId)) {
                wrapper.style.display = '';
                card.classList.remove('unavailable');
                visibleCount++;
            } else {
                wrapper.style.display = 'none';
                card.classList.add('unavailable');
                if (selectedLosIds.has(losId)) {
                    selectedLosIds.delete(losId);
                    card.classList.remove('selected');
                    card.querySelector('.los-check').className = 'bi bi-square los-check me-2';
                }
            }
        });

        document.getElementById('losNoneMsg').style.display = visibleCount === 0 ? '' : 'none';
        rebuildLosInputs();
        updateIntakePanel();
        updateSteps();
    }

    function toggleLos(el, losId) {
        if (el.classList.contains('unavailable')) return;
        if (selectedLosIds.has(losId)) {
            selectedLosIds.delete(losId);
            el.classList.remove('selected');
            el.querySelector('.los-check').className = 'bi bi-square los-check me-2';
        } else {
            selectedLosIds.add(losId);
            el.classList.add('selected');
            el.querySelector('.los-check').className = 'bi bi-check-square-fill los-check me-2';
        }
        rebuildLosInputs();
        updateIntakePanel();
        updateSteps();
    }

    function rebuildLosInputs() {
        var container = document.getElementById('losInputs');
        container.innerHTML = '';
        selectedLosIds.forEach(function(id) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'losIds';
            input.value = id;
            container.appendChild(input);
        });
    }

    function updateSteps() {
        var prospectOk = document.getElementById('prospectId').value !== '';
        var rateOk = selectedRateId !== null;
        var losOk = selectedLosIds.size > 0;

        document.getElementById('stepBadge1').className = 'step-badge ' + (prospectOk ? 'step-complete' : 'step-active') + ' me-3';
        document.getElementById('stepBadge2').className = 'step-badge ' + (rateOk ? 'step-complete' : (prospectOk ? 'step-active' : 'step-pending')) + ' me-3';
        document.getElementById('stepBadge3').className = 'step-badge ' + (losOk ? 'step-complete' : (rateOk ? 'step-active' : 'step-pending')) + ' me-3';

        // T125 -- when the plus-tier intake panel is visible, it gates Create Proposal too.
        document.getElementById('btnCreate').disabled = !(prospectOk && rateOk && losOk && ichraIntakeComplete());
    }

    // ── T125: ICHRA plus-tier intake panel ──────────────────────────────────────────
    // Every function below is a no-op when ichraIntakePanel is absent from the DOM (an
    // unentitled session, or an unconfigured RATE_CACHE_PLAN_YEARS) -- each starts with a
    // null-element guard so nothing here can throw on a page that never rendered the panel.
    var ichraPlanYear = <c:choose><c:when test="${ichraAvailable and not empty ichraPlanYear}">${ichraPlanYear}</c:when><c:otherwise>null</c:otherwise></c:choose>;
    var ichraLastLookedUpZip = '';

    // S19-G — illustration hand-off prefill. Server-validated (normalizeFiveDigitCode /
    // parseIntOrNull in ProposalBuilder.doGet) before reaching here, so these are either
    // null or a vetted 5-digit code / positive integer -- never a raw query-string value.
    var ichraHandoffZip = <c:choose><c:when test="${not empty handoffZip}">'${handoffZip}'</c:when><c:otherwise>null</c:otherwise></c:choose>;
    var ichraHandoffCountyFips = <c:choose><c:when test="${not empty handoffCountyFips}">'${handoffCountyFips}'</c:when><c:otherwise>null</c:otherwise></c:choose>;
    var ichraHandoffHeadcount = <c:choose><c:when test="${handoffHeadcount != null}">${handoffHeadcount}</c:when><c:otherwise>null</c:otherwise></c:choose>;
    // S19-I — validated {age,count} pairs from the AGE_BAND hand-off; [] when none. Built
    // server-side from parsed ints only (buildHandoffBandsJson), never from request text.
    var ichraHandoffBands = <c:choose><c:when test="${not empty handoffBandsJson}">${handoffBandsJson}</c:when><c:otherwise>[]</c:otherwise></c:choose>;
    var ichraBandMaxRows = <c:choose><c:when test="${ichraAgeBandMaxRows != null}">${ichraAgeBandMaxRows}</c:when><c:otherwise>6</c:otherwise></c:choose>;
    // S19-H — apply-once latch. filterLosCards()/toggleLos() call updateIntakePanel() on
    // every rate/LOS change, and its hide branch calls clearIntakeFields() whenever no
    // plus-tier LOS is currently selected -- routine during the normal prospect-then-rate
    // walk, before the agent ever reaches a plus-tier LOS. Prefilling once at page load was
    // wiped by that clear before the panel ever became visible. This latch is checked inside
    // ichraApplyHandoffPrefill() itself so it is true after the FIRST time the panel becomes
    // visible and stays true regardless of how many times the panel is later hidden/reshown.
    var ichraHandoffPrefillApplied = false;

    function anySelectedLosIsPlusTier() {
        var found = false;
        selectedLosIds.forEach(function(id) {
            var wrapper = document.querySelector('.los-card-wrapper[data-los-id="' + id + '"]');
            if (wrapper && wrapper.dataset.plusTier === 'true') found = true;
        });
        return found;
    }

    function hideIntakeMessages() {
        var noMatch = document.getElementById('intakeNoMatchMsg');
        var unpriced = document.getElementById('intakeUnpricedMsg');
        var mismatch = document.getElementById('intakeCountyMismatchMsg');
        if (noMatch) noMatch.style.display = 'none';
        if (unpriced) unpriced.style.display = 'none';
        if (mismatch) mismatch.style.display = 'none';
    }

    function clearIntakeFields() {
        var zipEl = document.getElementById('intakeZip');
        var countyEl = document.getElementById('intakeCountyFips');
        var countyNameEl = document.getElementById('intakeCountyName');
        var stateEl = document.getElementById('intakeState');
        var headcountEl = document.getElementById('intakeHeadcount');
        var contributionEl = document.getElementById('intakeContribution');
        if (zipEl) zipEl.value = '';
        if (countyEl) { countyEl.innerHTML = '<option value="">-- Enter a ZIP first --</option>'; countyEl.disabled = true; }
        if (countyNameEl) countyNameEl.value = '';
        if (stateEl) stateEl.value = '';
        if (headcountEl) headcountEl.value = '';
        if (contributionEl) contributionEl.value = '';
        // S19-I — bands are fields too, and a hidden input still submits: leaving rows behind
        // on a deselect would post intakeAge{i} for a proposal whose plus-tier LOS is gone,
        // and deriveIntakeMode would read them as an AGE_BAND intent. Remove, then resync.
        ichraRemoveAllBandRows();
        // S20-B/V091 — a stale section selection must not survive a deselect either, same
        // reasoning as every other field above.
        var sectionMarketEl = document.getElementById('sectionMarket');
        var sectionContributionEl = document.getElementById('sectionContribution');
        var sectionComparisonEl = document.getElementById('sectionComparison');
        var totalPremiumEl = document.getElementById('intakeCurrentTotalPremium');
        var employerShareEl = document.getElementById('intakeCurrentEmployerShare');
        if (sectionMarketEl) { sectionMarketEl.checked = false; sectionMarketEl.disabled = false; }
        if (sectionContributionEl) sectionContributionEl.checked = false;
        if (sectionComparisonEl) sectionComparisonEl.checked = false;
        if (totalPremiumEl) totalPremiumEl.value = '';
        if (employerShareEl) employerShareEl.value = '';
        ichraSyncSectionSelections();
        ichraLastLookedUpZip = '';
        hideIntakeMessages();
    }

    // S20-B/V091 — Rule A: ticking #2 or #3 ticks #1 and marks it read-only (cosmetic only;
    // ProposalBuilder's own server-side derivation, §8.4, is authoritative regardless of what
    // this checkbox shows). Rule C: #intakeContribution required while #2 or #3 is selected.
    // Rule D: #intakeComparisonFields shown and required only while #3 is selected, and #3
    // also requires the contribution (the planned ICHRA/QSEHRA contribution is its third
    // input). See docs/analysis/S20A_ichra_sections_spec.md §8.7.
    function ichraSyncSectionSelections() {
        var marketEl = document.getElementById('sectionMarket');
        if (!marketEl) return; // panel absent from the DOM -- no-op, same as every function here
        var contributionEl = document.getElementById('sectionContribution');
        var comparisonEl = document.getElementById('sectionComparison');
        var autoNote = document.getElementById('sectionMarketAutoNote');

        var forced = (contributionEl && contributionEl.checked) || (comparisonEl && comparisonEl.checked);
        marketEl.disabled = forced;
        if (forced) marketEl.checked = true;
        if (autoNote) autoNote.style.display = forced ? '' : 'none';

        var contributionRequired = (contributionEl && contributionEl.checked) || (comparisonEl && comparisonEl.checked);
        var contributionField = document.getElementById('intakeContribution');
        if (contributionField) {
            contributionField.placeholder = contributionRequired ? '' : 'Optional';
        }

        var comparisonSelected = comparisonEl && comparisonEl.checked;
        var comparisonFields = document.getElementById('intakeComparisonFields');
        if (comparisonFields) comparisonFields.style.display = comparisonSelected ? '' : 'none';

        updateSteps();
    }

    // ── S19-I: age-band repeater ────────────────────────────────────────────────────
    // Mirrors the illustration's own repeater (illustration25.jsp, W7) rather than inventing
    // a second convention: template-clone structure, renumber-to-contiguous-1..N as the
    // parameter contract, a remove control hidden on a lone row, an add button hidden at the
    // cap, and Eligible Employees replaced the moment a band exists. Every function no-ops
    // when the panel is absent from the DOM, exactly like the rest of this block.

    function ichraBandRowList() {
        var rows = document.getElementById('intakeBandRows');
        if (!rows) return [];
        return Array.prototype.slice.call(rows.querySelectorAll('.intake-band-row'));
    }

    // Rewrites id/name/for on every row so the submitted set is always 1..N with no gaps.
    // attachAgeBandSnapshot scans intakeAge1..N contiguously, so a gap would silently
    // truncate the bands at the hole.
    function ichraRenumberBands() {
        ichraBandRowList().forEach(function(row, idx) {
            var n = idx + 1;
            [['intakeAge', '.intake-band-age'], ['intakeCount', '.intake-band-count']].forEach(function(pair) {
                var input = row.querySelector(pair[1]);
                if (!input) return;
                input.id = pair[0] + n;
                input.name = pair[0] + n;
            });
        });
        ichraSyncBandControls();
        ichraSyncBandTier();
        updateSteps();
    }

    // The tier transition: zero bands -- Eligible Employees is the input; one or more -- the
    // bands carry the counts and their sum IS the headcount. Hidden, never removed, so
    // nothing is lost switching back.
    function ichraSyncBandTier() {
        var hasBands = ichraBandRowList().length > 0;
        var headcountField = document.getElementById('intakeHeadcountField');
        var derivedField = document.getElementById('intakeDerivedHeadcountField');
        var derivedValue = document.getElementById('intakeDerivedHeadcount');
        if (headcountField) headcountField.style.display = hasBands ? 'none' : '';
        if (derivedField) derivedField.style.display = hasBands ? '' : 'none';
        if (derivedValue) derivedValue.textContent = ichraBandTotalLives();
    }

    function ichraBandTotalLives() {
        var total = 0;
        ichraBandRowList().forEach(function(row) {
            var countEl = row.querySelector('.intake-band-count');
            var n = countEl ? parseInt(countEl.value, 10) : NaN;
            if (!isNaN(n) && n >= 1) total += n;
        });
        return total;
    }

    // At least one row carrying a valid age -- the same "a blank band is not a band" rule
    // attachAgeBandSnapshot applies server-side when it skips blank rows.
    function ichraHasValidBand() {
        return ichraBandRowList().some(function(row) {
            var ageEl = row.querySelector('.intake-band-age');
            var age = ageEl ? parseInt(ageEl.value, 10) : NaN;
            return !isNaN(age) && age >= 21 && age <= 64;
        });
    }

    function ichraSyncBandControls() {
        var list = ichraBandRowList();
        var addBtn = document.getElementById('intakeBandAdd');
        var maxNote = document.getElementById('intakeBandMaxNote');
        // Removing the only row must stay possible here -- unlike the illustration, this
        // panel's zero-band state is fully valid (it falls back to Eligible Employees), so
        // the control is always offered.
        list.forEach(function(row) {
            var btn = row.querySelector('.intake-band-remove');
            if (btn) btn.style.display = '';
        });
        var atMax = list.length >= ichraBandMaxRows;
        if (addBtn) addBtn.style.display = atMax ? 'none' : '';
        if (maxNote) maxNote.style.display = atMax ? '' : 'none';
    }

    function ichraAddBandRow(age, count) {
        var rows = document.getElementById('intakeBandRows');
        var template = document.getElementById('intakeBandTemplate');
        if (!rows || !template) return null;
        if (ichraBandRowList().length >= ichraBandMaxRows) return null;

        var clone = template.cloneNode(true);
        clone.removeAttribute('id');
        clone.removeAttribute('aria-hidden');
        clone.style.display = '';
        clone.className = 'intake-band-row d-flex align-items-end gap-2 mb-2';

        var ageEl = clone.querySelector('.intake-band-age');
        var countEl = clone.querySelector('.intake-band-count');
        if (ageEl) ageEl.value = (age != null) ? age : '';
        // A real default of 1, never a placeholder -- entered and assumed must not look alike.
        if (countEl) countEl.value = (count != null) ? count : '1';

        rows.appendChild(clone);
        ichraRenumberBands();
        return clone;
    }

    function ichraRemoveAllBandRows() {
        var rows = document.getElementById('intakeBandRows');
        if (!rows) return;
        ichraBandRowList().forEach(function(row) { row.parentNode.removeChild(row); });
        ichraSyncBandControls();
        ichraSyncBandTier();
    }

    // Gate link 4: the panel appears iff a selected LOS is plus-tier, and a stale value
    // must not survive a deselect -- so hiding always clears every field.
    function updateIntakePanel() {
        var panel = document.getElementById('ichraIntakePanel');
        if (!panel) return;

        if (anySelectedLosIsPlusTier()) {
            panel.style.display = '';
            // S19-H — applied here, not at page load, so it survives every clearIntakeFields()
            // call that happens before this LOS was selected. Latched inside the function
            // itself; safe to call on every reveal.
            ichraApplyHandoffPrefill();
            // S20-B/V091 — syncs the market-forced/comparison-visible/contribution-placeholder
            // state to whatever the checkboxes currently hold (they may still carry a value the
            // agent set before toggling the plus-tier LOS off and back on).
            ichraSyncSectionSelections();
        } else {
            panel.style.display = 'none';
            clearIntakeFields();
        }
    }

    function ichraIntakeComplete() {
        var panel = document.getElementById('ichraIntakePanel');
        if (!panel || panel.style.display === 'none') return true; // panel not showing -- nothing required

        // S20-B/V091 — Rule B: only required at all when at least one section is selected. An
        // agent who wants no ICHRA content on this proposal is not blocked by the panel.
        var marketEl = document.getElementById('sectionMarket');
        var contributionEl = document.getElementById('sectionContribution');
        var comparisonEl = document.getElementById('sectionComparison');
        var anySectionSelected = (marketEl && marketEl.checked) || (contributionEl && contributionEl.checked)
                || (comparisonEl && comparisonEl.checked);
        if (!anySectionSelected) return true;

        var zip = (document.getElementById('intakeZip').value || '').trim();
        var county = document.getElementById('intakeCountyFips').value;
        var headcount = parseInt(document.getElementById('intakeHeadcount').value, 10);
        // S19-I — bands satisfy the headcount requirement in their own right: when any band
        // carries a valid age, the counts ARE the headcount and the Eligible Employees input
        // is hidden, so requiring it too would leave btnCreate permanently disabled.
        var headcountOk = ichraHasValidBand() || headcount >= 1;
        var ok = /^[0-9]{5}$/.test(zip) && county !== '' && headcountOk;

        // Rule C — #2 selected requires a contribution.
        var contributionRaw = document.getElementById('intakeContribution').value;
        var contributionOk = contributionRaw !== '' && !isNaN(parseFloat(contributionRaw));
        if (contributionEl && contributionEl.checked) {
            ok = ok && contributionOk;
        }

        // Rule D — #3 selected requires both comparison fields AND the contribution (the
        // planned ICHRA/QSEHRA contribution is #3's third input, spec §2).
        if (comparisonEl && comparisonEl.checked) {
            var totalRaw = document.getElementById('intakeCurrentTotalPremium').value;
            var shareRaw = document.getElementById('intakeCurrentEmployerShare').value;
            var totalOk = totalRaw !== '' && !isNaN(parseFloat(totalRaw));
            var shareOk = shareRaw !== '' && !isNaN(parseFloat(shareRaw));
            ok = ok && totalOk && shareOk && contributionOk;
        }

        return ok;
    }

    function ichraSelectCounty(county) {
        var countyEl = document.getElementById('intakeCountyFips');
        var nameEl = document.getElementById('intakeCountyName');
        var stateEl = document.getElementById('intakeState');
        if (!countyEl) return;

        countyEl.innerHTML = '';
        var placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = '-- Select a county --';
        countyEl.appendChild(placeholder);

        var opt = document.createElement('option');
        opt.value = county.fips;
        opt.textContent = county.name + ', ' + county.state + (county.priced === false ? ' (no rates cached yet)' : '');
        opt.selected = true;
        countyEl.appendChild(opt);
        countyEl.disabled = false;

        if (nameEl) nameEl.value = county.name;
        if (stateEl) stateEl.value = county.state;
    }

    // Several counties: the agent chooses. NEVER auto-select counties[0] -- 34% of TX
    // ZCTAs cross a county line (ZipCountyResolver javadoc); a silently wrong county
    // returns clean rates with nothing downstream to disagree.
    function ichraRenderChooser(counties) {
        var countyEl = document.getElementById('intakeCountyFips');
        var nameEl = document.getElementById('intakeCountyName');
        var stateEl = document.getElementById('intakeState');
        if (!countyEl) return;

        countyEl.innerHTML = '';
        var placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = '-- Select a county --';
        countyEl.appendChild(placeholder);

        counties.forEach(function(county) {
            var opt = document.createElement('option');
            opt.value = county.fips;
            opt.textContent = county.name + ', ' + county.state + (county.priced === false ? ' (no rates cached yet)' : '');
            opt.dataset.name = county.name;
            opt.dataset.state = county.state;
            countyEl.appendChild(opt);
        });
        countyEl.disabled = false;

        // Nothing is pre-selected -- the agent must choose (see the NEVER-auto-select note
        // above). The hidden name/state fields are filled by the change listener below once
        // they do.
        if (nameEl) nameEl.value = '';
        if (stateEl) stateEl.value = '';
    }

    function ichraZipLookup() {
        var zipEl = document.getElementById('intakeZip');
        if (!zipEl) return;
        var raw = (zipEl.value || '').trim();
        if (raw === ichraLastLookedUpZip) return;
        ichraLastLookedUpZip = raw;

        hideIntakeMessages();
        var countyEl = document.getElementById('intakeCountyFips');
        var nameEl = document.getElementById('intakeCountyName');
        var stateEl = document.getElementById('intakeState');
        if (countyEl) { countyEl.innerHTML = '<option value="">-- Enter a ZIP first --</option>'; countyEl.disabled = true; }
        if (nameEl) nameEl.value = '';
        if (stateEl) stateEl.value = '';

        // Half-typed input is not an error -- say nothing and wait for five digits.
        if (!/^[0-9]{5}$/.test(raw)) { updateSteps(); return; }

        var url = 'IchraZipLookup?zip=' + encodeURIComponent(raw)
                + (ichraPlanYear ? '&planYear=' + encodeURIComponent(ichraPlanYear) : '');
        fetch(url, { headers: { 'Accept': 'application/json' } })
            .then(function(res) { return res.ok ? res.json() : { counties: [] }; })
            .then(function(data) {
                if ((zipEl.value || '').trim() !== raw) return; // field moved on -- drop the answer
                var counties = (data && Array.isArray(data.counties)) ? data.counties : [];
                if (counties.length === 0) {
                    var noMatchZipEl = document.getElementById('intakeNoMatchZip');
                    var noMatchMsg = document.getElementById('intakeNoMatchMsg');
                    if (noMatchZipEl) noMatchZipEl.textContent = raw;
                    if (noMatchMsg) noMatchMsg.style.display = '';
                    if (countyEl) { countyEl.innerHTML = '<option value="">-- No county found for this ZIP --</option>'; countyEl.disabled = false; }
                } else if (counties.length === 1 && ichraCountyAgreesWithHandoff(counties[0].fips)) {
                    ichraSelectCounty(counties[0]);
                    if (counties[0].priced === false) {
                        var unpricedMsg = document.getElementById('intakeUnpricedMsg');
                        if (unpricedMsg) unpricedMsg.style.display = '';
                    }
                } else {
                    // Either genuinely ambiguous (multiple counties -- unchanged, agent must
                    // choose) or a single county that disagrees with the illustration
                    // hand-off's countyFips. Neither is auto-selected; ichraRenderChooser
                    // never auto-selects even for a one-item list.
                    ichraRenderChooser(counties);
                    if (!ichraCountyAmongCandidates(counties)) {
                        var mismatchMsg = document.getElementById('intakeCountyMismatchMsg');
                        if (mismatchMsg) mismatchMsg.style.display = '';
                    }
                }
                updateSteps();
            })
            .catch(function() {
                // Leave it to the submit path; nothing here blocks the form.
            });
    }

    // S19-G — the known-trap guard (sec 3): the ZIP-driven lookup stays the sole authority
    // for what is selectable; ichraHandoffCountyFips is only ever an expected-agreement
    // check, never a substitute. Both return true (nothing to check / nothing to flag) when
    // no handoff countyFips is present -- a hand-typed ZIP with no hand-off behaves exactly
    // as it always has.
    function ichraCountyAgreesWithHandoff(fips) {
        return !ichraHandoffCountyFips || ichraHandoffCountyFips === fips;
    }

    function ichraCountyAmongCandidates(counties) {
        if (!ichraHandoffCountyFips) return true;
        return counties.some(function(c) { return c.fips === ichraHandoffCountyFips; });
    }

    // Fires when the agent manually picks from the multi-county chooser -- fills the
    // hidden name/state fields from the chosen <option>'s data-* attributes (the single-
    // match auto-select path in ichraSelectCounty fills them directly and never needs this).
    function ichraCountySelected() {
        var countyEl = document.getElementById('intakeCountyFips');
        var nameEl = document.getElementById('intakeCountyName');
        var stateEl = document.getElementById('intakeState');
        if (!countyEl) return;
        var opt = countyEl.options[countyEl.selectedIndex];
        if (nameEl) nameEl.value = (opt && opt.dataset.name) ? opt.dataset.name : '';
        if (stateEl) stateEl.value = (opt && opt.dataset.state) ? opt.dataset.state : '';
        updateSteps();
    }

    // On load: filter rates by pre-selected prospect (if any), then auto-select rate
    document.addEventListener('DOMContentLoaded', function() {
        // If server auto-expanded the prospect list, update toggle button appearance
        if (isExpanded) {
            var btn = document.getElementById('btnExpandProspects');
            if (btn) {
                btn.classList.remove('btn-outline-secondary');
                btn.classList.add('btn-secondary');
                btn.innerHTML = '<i class="bi bi-person me-1"></i>${isPspAdmin ? "My Agency" : "My Prospects"}';
            }
        }
        filterRatesByProspect();
        <c:if test="${autoSelectedRateId != null}">
        var autoRate = document.querySelector('.rate-option');
        if (autoRate) selectRate(autoRate, ${autoSelectedRateId});
        </c:if>
        var ichraZipEl = document.getElementById('intakeZip');
        if (ichraZipEl) {
            ichraZipEl.addEventListener('change', ichraZipLookup);
            ichraZipEl.addEventListener('blur', ichraZipLookup);
        }
        var ichraCountyEl = document.getElementById('intakeCountyFips');
        if (ichraCountyEl) {
            ichraCountyEl.addEventListener('change', ichraCountySelected);
        }
        var ichraHeadcountEl = document.getElementById('intakeHeadcount');
        if (ichraHeadcountEl) {
            ichraHeadcountEl.addEventListener('input', updateSteps);
        }
        // S20-B/V091 — section checkboxes drive Rule A/C/D's show/hide and required state;
        // the three optional decimal fields only need btnCreate re-evaluated as they're typed.
        ['sectionMarket', 'sectionContribution', 'sectionComparison'].forEach(function(id) {
            var el = document.getElementById(id);
            if (el) el.addEventListener('change', ichraSyncSectionSelections);
        });
        ['intakeContribution', 'intakeCurrentTotalPremium', 'intakeCurrentEmployerShare'].forEach(function(id) {
            var el = document.getElementById(id);
            if (el) el.addEventListener('input', updateSteps);
        });
        // S19-I — band repeater wiring. Delegated on the rows container so it covers every
        // row, including ones added later by the button or by the hand-off prefill.
        var ichraBandAddBtn = document.getElementById('intakeBandAdd');
        if (ichraBandAddBtn) {
            ichraBandAddBtn.addEventListener('click', function() {
                var row = ichraAddBandRow(null, null);
                var firstInput = row ? row.querySelector('.intake-band-age') : null;
                if (firstInput) firstInput.focus();
            });
        }
        var ichraBandRowsEl = document.getElementById('intakeBandRows');
        if (ichraBandRowsEl) {
            ichraBandRowsEl.addEventListener('click', function(e) {
                var btn = e.target.closest ? e.target.closest('.intake-band-remove') : null;
                if (!btn) return;
                var row = btn.closest('.intake-band-row');
                if (row) row.parentNode.removeChild(row);
                ichraRenumberBands();
            });
            // Keeps the derived Eligible Employees total and btnCreate honest as the agent types.
            ichraBandRowsEl.addEventListener('input', function(e) {
                if (!e.target.closest) return;
                if (e.target.closest('.intake-band-age') || e.target.closest('.intake-band-count')) {
                    ichraSyncBandTier();
                    updateSteps();
                }
            });
        }
        updateSteps();
    });

    // S19-G/S19-H — prefill only, from the illustration hand-off. Invoked from
    // updateIntakePanel()'s visible branch, not at page load, so it runs at the moment the
    // panel actually appears rather than being wiped by an intervening clearIntakeFields()
    // call (S19-H). Latched: no-ops on every call after the first, so toggling a plus-tier
    // LOS off and back on never overwrites what the agent has since typed. No-op (both
    // guarded fields simply stay empty) when ichraIntakePanel is absent from the DOM, exactly
    // like every other function in this block. Sets ZIP synchronously, then calls
    // ichraZipLookup() -- that function is the ONLY code path that ever populates or selects
    // County, so County is never set here directly and there is nothing to race: the
    // dropdown reflects whatever that lookup's own (unchanged) promise chain decides once it
    // resolves. Headcount is independent and set synchronously alongside ZIP.
    function ichraApplyHandoffPrefill() {
        if (ichraHandoffPrefillApplied) return;
        ichraHandoffPrefillApplied = true;

        var headcountEl = document.getElementById('intakeHeadcount');
        if (headcountEl && ichraHandoffHeadcount != null && !headcountEl.value) {
            headcountEl.value = ichraHandoffHeadcount;
        }

        var zipEl = document.getElementById('intakeZip');
        if (zipEl && ichraHandoffZip != null && !zipEl.value) {
            zipEl.value = ichraHandoffZip;
            ichraZipLookup();
        }

        // S19-I — bands from an AGE_BAND hand-off. Only when the panel currently has none, so
        // this can never duplicate or clobber rows an agent has already built. Each row is
        // added through the same ichraAddBandRow() the "+ Add age band" button uses, so the
        // renumber/tier/gating sync is identical on both paths. ichraRenumberBands() (called
        // inside) hides Eligible Employees and publishes the derived total once rows exist.
        if (ichraHandoffBands && ichraHandoffBands.length && ichraBandRowList().length === 0) {
            ichraHandoffBands.forEach(function(band) {
                ichraAddBandRow(band.age, band.count);
            });
        }
    }
</script>
</body>
</html>

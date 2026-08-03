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
                    <div class="col-auto">
                        <label class="form-label mb-1" for="intakeHeadcount">Eligible Employees</label>
                        <input type="number" class="form-control form-control-sm" id="intakeHeadcount" name="intakeHeadcount"
                               min="1" max="10000" step="1" style="max-width:130px;">
                    </div>
                    <div class="col-auto">
                        <label class="form-label mb-1" for="intakeContribution">Monthly employer contribution per employee</label>
                        <input type="number" class="form-control form-control-sm" id="intakeContribution" name="intakeContribution"
                               min="0" step="0.01" style="max-width:150px;" placeholder="Optional">
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
        if (noMatch) noMatch.style.display = 'none';
        if (unpriced) unpriced.style.display = 'none';
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
        ichraLastLookedUpZip = '';
        hideIntakeMessages();
    }

    // Gate link 4: the panel appears iff a selected LOS is plus-tier, and a stale value
    // must not survive a deselect -- so hiding always clears every field.
    function updateIntakePanel() {
        var panel = document.getElementById('ichraIntakePanel');
        if (!panel) return;

        if (anySelectedLosIsPlusTier()) {
            panel.style.display = '';
        } else {
            panel.style.display = 'none';
            clearIntakeFields();
        }
    }

    function ichraIntakeComplete() {
        var panel = document.getElementById('ichraIntakePanel');
        if (!panel || panel.style.display === 'none') return true; // panel not showing -- nothing required
        var zip = (document.getElementById('intakeZip').value || '').trim();
        var county = document.getElementById('intakeCountyFips').value;
        var headcount = parseInt(document.getElementById('intakeHeadcount').value, 10);
        return /^[0-9]{5}$/.test(zip) && county !== '' && headcount >= 1;
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
                } else if (counties.length === 1) {
                    ichraSelectCounty(counties[0]);
                    if (counties[0].priced === false) {
                        var unpricedMsg = document.getElementById('intakeUnpricedMsg');
                        if (unpricedMsg) unpricedMsg.style.display = '';
                    }
                } else {
                    ichraRenderChooser(counties);
                }
                updateSteps();
            })
            .catch(function() {
                // Leave it to the submit path; nothing here blocks the form.
            });
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
        updateSteps();
    });
</script>
</body>
</html>

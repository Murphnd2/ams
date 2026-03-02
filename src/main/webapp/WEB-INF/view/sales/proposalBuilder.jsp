<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Proposal Builder</title>
    <style>
        .los-card { cursor: pointer; transition: all 0.2s ease; border: 2px solid #dee2e6; }
        .los-card:hover { border-color: #0d6efd; box-shadow: 0 2px 8px rgba(13,110,253,0.15); }
        .los-card.selected { border-color: #198754; background-color: #f0fdf4; }
        .los-card.selected .los-check { color: #198754; }
        .los-card.unavailable { display: none !important; }
        .los-check { font-size: 1.25rem; color: #dee2e6; }
        .step-badge { width: 32px; height: 32px; border-radius: 50%; display: inline-flex;
            align-items: center; justify-content: center; font-weight: 600; font-size: 0.875rem; }
        .step-active { background-color: #0d6efd; color: white; }
        .step-complete { background-color: #198754; color: white; }
        .step-pending { background-color: #e9ecef; color: #6c757d; }
        .rate-option { cursor: pointer; transition: all 0.15s ease; }
        .rate-option:hover { background-color: #f8f9fa; }
        .rate-option.selected { background-color: #e7f1ff; border-color: #0d6efd !important; }
        .los-none-msg { display: none; color: #6c757d; font-style: italic; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Proposal Builder" scope="request"/>
    <c:set var="pageIcon" value="bi-file-earmark-plus" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

    <%-- Page Header --%>
    <div class="row mt-3 mb-4">
        <div class="col">
            <h4 class="fw-bold"><i class="bi bi-file-earmark-text me-2"></i>Proposal Builder</h4>
            <p class="text-muted mb-0">Create a new service proposal for a prospect</p>
        </div>
        <div class="col-auto">
            <c:choose>
                <c:when test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
                    <a href="AgentHome" class="btn btn-outline-secondary btn-sm">
                        <i class="bi bi-arrow-left me-1"></i>Back to Pipeline
                    </a>
                </c:when>
                <c:otherwise>
                    <a href="ViewHome25" class="btn btn-outline-secondary btn-sm">
                        <i class="bi bi-arrow-left me-1"></i>Back to Dashboard
                    </a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <form method="post" action="ProposalBuilder" id="proposalForm">
        <input type="hidden" name="action" value="createProposal">
        <input type="hidden" name="sourceActivityId" value="${param.sourceActivityId}">

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
                        <select class="form-select" name="prospectId" id="prospectId" onchange="updateSteps()">
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
                <div class="row g-2">
                    <c:forEach var="rate" items="${allRates}">
                        <div class="col-md-4 col-sm-6">
                            <div class="card rate-option p-3" onclick="selectRate(this, ${rate.getId()})">
                                <div class="d-flex align-items-center">
                                    <i class="bi bi-circle me-2 rate-icon" style="font-size: 1.1rem;"></i>
                                    <span class="fw-medium">${rate.getDescription()}</span>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
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
                        <div class="col-md-4 col-sm-6 los-card-wrapper" data-los-id="${los.getId()}">
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

        <%-- Submit --%>
        <div class="row mb-5">
            <div class="col">
                <button type="submit" class="btn btn-primary btn-lg px-5" id="btnCreate" disabled>
                    <i class="bi bi-file-earmark-plus me-2"></i>Create Proposal
                </button>
            </div>
        </div>
    </form>
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

    let selectedRateId = null;
    let selectedLosIds = new Set();
    let isExpanded = false;

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

        document.getElementById('btnCreate').disabled = !(prospectOk && rateOk && losOk);
    }

    // Auto-select rate if only one
    document.addEventListener('DOMContentLoaded', function() {
        <c:if test="${autoSelectedRateId != null}">
        var autoRate = document.querySelector('.rate-option');
        if (autoRate) selectRate(autoRate, ${autoSelectedRateId});
        </c:if>
        updateSteps();
    });
</script>
</body>
</html>

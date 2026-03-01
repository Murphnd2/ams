<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  Add Activity Modal — Create a new Renewal, Opportunity, or Setup from the PSP home page.
  Opened from the "+" button in activityHeader25.jsp.
  All element IDs prefixed aa_ to avoid collisions with other modals.
--%>
<div class="modal fade" id="addActivityModal" tabindex="-1" aria-labelledby="addActivityLabel" aria-hidden="true">
  <div class="modal-dialog modal-fullscreen-sm-down" role="document" id="aa_dialog">
    <div class="modal-content">
      <div class="modal-header py-2" style="background: linear-gradient(135deg, #0d5681, #0a4468); color: white;">
        <h6 class="modal-title m-0" id="addActivityLabel">
          <i class="bi bi-plus-circle me-1"></i>New Activity
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">

        <%-- ═══ ACTIVITY TYPE TOGGLE ═══ --%>
        <div class="btn-group w-100 mb-3" role="group">
          <input type="radio" class="btn-check" name="aa_typeToggle" id="aa_togRenewal" autocomplete="off" checked
                 onclick="aa_showType('renewal')">
          <label class="btn btn-outline-primary btn-sm" for="aa_togRenewal">
            <i class="bi bi-recycle me-1"></i>Renewal
          </label>
          <input type="radio" class="btn-check" name="aa_typeToggle" id="aa_togOpportunity" autocomplete="off"
                 onclick="aa_showType('opportunity')">
          <label class="btn btn-outline-primary btn-sm" for="aa_togOpportunity">
            <i class="bi bi-graph-up-arrow me-1"></i>Opportunity
          </label>
          <input type="radio" class="btn-check" name="aa_typeToggle" id="aa_togSetup" autocomplete="off"
                 onclick="aa_showType('setup')">
          <label class="btn btn-outline-primary btn-sm" for="aa_togSetup">
            <i class="bi bi-building-add me-1"></i>Setup
          </label>
        </div>

        <%-- ═══ RENEWAL SECTION ═══ --%>
        <div id="aa_renewalSection">
          <form method="post" action="CreateBlankRenewal25" id="aa_renewalForm">
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-building me-1 text-ssa"></i>Employer
            </label>
            <select class="form-select form-select-sm mb-3" name="employerId" id="aa_employerId" required>
              <option value="" selected disabled>Select an employer...</option>
              <c:forEach var="employer" items="${applicationScope.global.getEmployers()}">
                <option value="${employer.getId()}">${fn:escapeXml(employer.getEmployerName().toUpperCase().trim())}</option>
              </c:forEach>
            </select>
            <button type="submit" class="btn btn-ssa btn-sm w-100" id="aa_renewalBtn" disabled>
              <i class="bi bi-recycle me-1"></i>Create Renewal
            </button>
          </form>
        </div>

        <%-- ═══ OPPORTUNITY SECTION ═══ --%>
        <div id="aa_opportunitySection" style="display:none;">
          <form method="post" action="CreateOpportunity" id="aa_oppForm">
            <input type="hidden" name="returnTo" value="home">
            <input type="hidden" name="prospectMode" id="aa_prospectMode" value="existing">

            <%-- Prospect mode toggle --%>
            <div class="btn-group w-100 mb-3" role="group">
              <input type="radio" class="btn-check" name="aa_prospectToggle" id="aa_togExisting" autocomplete="off" checked
                     onclick="aa_showProspectMode('existing')">
              <label class="btn btn-outline-secondary btn-sm" for="aa_togExisting">
                <i class="bi bi-search me-1"></i>Existing Prospect
              </label>
              <input type="radio" class="btn-check" name="aa_prospectToggle" id="aa_togNew" autocomplete="off"
                     onclick="aa_showProspectMode('new')">
              <label class="btn btn-outline-secondary btn-sm" for="aa_togNew">
                <i class="bi bi-plus-circle me-1"></i>New Prospect
              </label>
            </div>

            <%-- Existing prospect dropdown --%>
            <div id="aa_existingProspectFields">
              <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                <i class="bi bi-briefcase me-1 text-ssa"></i>Prospect
              </label>
              <select class="form-select form-select-sm mb-3" name="prospectId" id="aa_prospectId">
                <option value="" selected disabled>Select a prospect...</option>
                <c:forEach var="p" items="${applicationScope.global.getProspects()}">
                  <option value="${p.getId()}">${fn:escapeXml(p.getName())}</option>
                </c:forEach>
              </select>
            </div>

            <%-- New prospect fields --%>
            <div id="aa_newProspectFields" style="display:none;">
              <div class="mb-2">
                <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                  Company Name <span class="text-danger">*</span>
                </label>
                <input type="text" name="companyName" id="aa_companyName" class="form-control form-control-sm"
                       placeholder="e.g. Acme Corp">
              </div>
              <div class="row mb-2">
                <div class="col-6">
                  <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">Contact First</label>
                  <input type="text" name="contactFirst" class="form-control form-control-sm" placeholder="First">
                </div>
                <div class="col-6">
                  <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">Contact Last</label>
                  <input type="text" name="contactLast" class="form-control form-control-sm" placeholder="Last">
                </div>
              </div>
              <div class="mb-2">
                <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">Contact Email</label>
                <input type="email" name="contactEmail" class="form-control form-control-sm" placeholder="email@example.com">
              </div>
            </div>

            <%-- Agency (always needed for opportunity) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-people me-1 text-ssa"></i>Agency
            </label>
            <select class="form-select form-select-sm mb-3" name="agencyId" id="aa_agencyId" required>
              <c:choose>
                <c:when test="${applicationScope.global.getAgencies().size() == 1}">
                  <option value="${applicationScope.global.getAgencies().get(0).getId()}" selected>
                    ${fn:escapeXml(applicationScope.global.getAgencies().get(0).getName())}
                  </option>
                </c:when>
                <c:otherwise>
                  <option value="" selected disabled>Select agency...</option>
                  <c:forEach var="a" items="${applicationScope.global.getAgencies()}">
                    <option value="${a.getId()}">${fn:escapeXml(a.getName())}</option>
                  </c:forEach>
                </c:otherwise>
              </c:choose>
            </select>

            <button type="submit" class="btn btn-ssa btn-sm w-100" id="aa_oppBtn" disabled>
              <i class="bi bi-graph-up-arrow me-1"></i>Create Opportunity
            </button>
          </form>
        </div>

        <%-- ═══ SETUP SECTION ═══ --%>
        <div id="aa_setupSection" style="display:none;">
          <form method="post" action="CreateSetup25" id="aa_setupForm">
            <input type="hidden" name="prospectMode" id="aa_sProspectMode" value="existing">
            <input type="hidden" id="aa_currentPersonId" value="${sessionScope.currentPerson.getId()}">
            <input type="hidden" id="aa_currentPersonAgencies"
                   value="${applicationScope.global.getPersonAgencyIds(sessionScope.currentPerson.getId())}">

            <%-- Agency (first — drives agent, prospect, rate cascades) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-people me-1 text-ssa"></i>Agency
            </label>
            <select class="form-select form-select-sm mb-3" name="agencyId" id="aa_sAgencyId" required>
              <option value="" disabled>Select agency...</option>
              <c:forEach var="a" items="${applicationScope.global.getAgencies()}">
                <c:if test="${applicationScope.global.hasAgents(a.getId())}">
                  <option value="${a.getId()}"
                          data-rates="${applicationScope.global.getAgencyRateIds(a.getId())}"
                          data-manager="${applicationScope.global.getAgencyManagerId(a.getId())}">
                    ${fn:escapeXml(a.getName())}
                  </option>
                </c:if>
              </c:forEach>
            </select>

            <%-- Agent (filtered by agency, shown for new prospect or when multiple agents) --%>
            <div id="aa_sAgentRow" style="display:none;">
              <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                <i class="bi bi-person-badge me-1 text-ssa"></i>Agent
              </label>
              <select class="form-select form-select-sm mb-3" name="agentId" id="aa_sAgentId">
                <option value="" selected disabled>Select agent...</option>
                <c:forEach var="ag" items="${applicationScope.global.getSetupAgents()}">
                  <option value="${ag.getId()}" class="aa-agent-option" hidden
                          data-agencies="${ag.getAgencyIds()}">
                    ${fn:escapeXml(ag.getName())}
                  </option>
                </c:forEach>
              </select>
            </div>

            <%-- Prospect mode toggle --%>
            <div class="btn-group w-100 mb-3" role="group">
              <input type="radio" class="btn-check" name="aa_sProspectToggle" id="aa_sTogExisting" autocomplete="off" checked
                     onclick="aa_showSetupProspectMode('existing')">
              <label class="btn btn-outline-secondary btn-sm" for="aa_sTogExisting">
                <i class="bi bi-search me-1"></i>Existing Prospect
              </label>
              <input type="radio" class="btn-check" name="aa_sProspectToggle" id="aa_sTogNew" autocomplete="off"
                     onclick="aa_showSetupProspectMode('new')">
              <label class="btn btn-outline-secondary btn-sm" for="aa_sTogNew">
                <i class="bi bi-plus-circle me-1"></i>New Prospect
              </label>
            </div>

            <%-- Existing prospect dropdown (filtered by agency) --%>
            <div id="aa_sExistingProspectFields">
              <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                <i class="bi bi-briefcase me-1 text-ssa"></i>Prospect
              </label>
              <select class="form-select form-select-sm mb-3" name="prospectId" id="aa_sProspectId">
                <option value="" selected disabled>Select a prospect...</option>
                <c:forEach var="p" items="${applicationScope.global.getProspects()}">
                  <option value="${p.getId()}" class="aa-prospect-option" hidden
                          data-agencies="${applicationScope.global.getProspectAgencyIds(p.getId())}"
                          data-agent-id="${p.getAgent().getId()}">
                    ${fn:escapeXml(p.getName())}
                  </option>
                </c:forEach>
              </select>
            </div>

            <%-- New prospect fields --%>
            <div id="aa_sNewProspectFields" style="display:none;">
              <div class="mb-2">
                <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                  Company Name <span class="text-danger">*</span>
                </label>
                <input type="text" name="companyName" id="aa_sCompanyName" class="form-control form-control-sm"
                       placeholder="e.g. Acme Corp">
              </div>
              <div class="row mb-2">
                <div class="col-6">
                  <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">Contact First</label>
                  <input type="text" name="contactFirst" id="aa_sContactFirst" class="form-control form-control-sm" placeholder="First">
                </div>
                <div class="col-6">
                  <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">Contact Last</label>
                  <input type="text" name="contactLast" id="aa_sContactLast" class="form-control form-control-sm" placeholder="Last">
                </div>
              </div>
              <div class="mb-2">
                <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">Contact Email</label>
                <input type="email" name="contactEmail" id="aa_sContactEmail" class="form-control form-control-sm" placeholder="email@example.com">
              </div>
            </div>

            <%-- Rate (filtered by agency selection) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-cash-coin me-1 text-ssa"></i>Rate Package
            </label>
            <select class="form-select form-select-sm mb-3" name="rateId" id="aa_sRateId" required>
              <option value="" selected disabled>Select rate...</option>
              <c:forEach var="r" items="${applicationScope.global.getRateList()}">
                <option value="${r.getId()}" class="aa-rate-option" hidden
                        data-los="${applicationScope.global.getRateLosIds(r.getId())}"
                        data-extras="${applicationScope.global.getRateExtraIds(r.getId())}">
                  ${fn:escapeXml(r.getDescription())}
                </option>
              </c:forEach>
            </select>

            <%-- Lines of Service --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-list-check me-1 text-ssa"></i>Lines of Service
            </label>
            <div class="border rounded p-2 mb-3" id="aa_sLosContainer" style="max-height:180px; overflow-y:auto;">
              <div id="aa_sLosHint" class="text-muted small fst-italic">Select a rate package first</div>
              <c:forEach var="los" items="${applicationScope.global.getLosList()}">
                <div class="form-check form-switch mb-1 aa-los-item" data-los-id="${los.getId()}" style="display:none;">
                  <input class="form-check-input aa-los-switch" type="checkbox"
                         id="aa_sLos_${los.getId()}" value="${los.getId()}" onchange="aa_validateSetup()">
                  <label class="form-check-label" for="aa_sLos_${los.getId()}" style="font-size:0.85rem;">
                    ${fn:escapeXml(los.getDescription())}
                  </label>
                </div>
              </c:forEach>
            </div>

            <%-- Additional Services (enhancements) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-puzzle me-1 text-ssa"></i>Additional Services
            </label>
            <div class="border rounded p-2 mb-3" id="aa_sExtraContainer" style="max-height:180px; overflow-y:auto;">
              <c:forEach var="enh" items="${applicationScope.global.getEnhancementList()}">
                <div class="form-check form-switch mb-1 aa-extra-item" data-extra-id="${enh.getServiceItem().getId()}" style="display:none;">
                  <input class="form-check-input aa-extra-switch" type="checkbox"
                         id="aa_sExtra_${enh.getServiceItem().getId()}" value="${enh.getServiceItem().getId()}" onchange="aa_validateSetup()">
                  <label class="form-check-label" for="aa_sExtra_${enh.getServiceItem().getId()}" style="font-size:0.85rem;">
                    ${fn:escapeXml(enh.getDescription())}
                  </label>
                </div>
              </c:forEach>
              <div id="aa_sExtraHint" class="text-muted small fst-italic">Select a rate package first</div>
            </div>

            <%-- Hidden inputs populated by JS --%>
            <div id="aa_sLosInputs"></div>
            <div id="aa_sExtraInputs"></div>

            <button type="submit" class="btn btn-ssa btn-sm w-100" id="aa_setupBtn" disabled
                    onclick="aa_prepareSetupSubmit()">
              <i class="bi bi-building-add me-1"></i>Create Setup
            </button>
          </form>
        </div>

      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var modal = document.getElementById('addActivityModal');
  var dialog = document.getElementById('aa_dialog');

  /* ═══ Activity type toggle ═══ */
  window.aa_showType = function(type) {
    document.getElementById('aa_renewalSection').style.display = (type === 'renewal') ? '' : 'none';
    document.getElementById('aa_opportunitySection').style.display = (type === 'opportunity') ? '' : 'none';
    document.getElementById('aa_setupSection').style.display = (type === 'setup') ? '' : 'none';
    // Widen dialog for setup (more fields)
    if (type === 'setup') {
      dialog.classList.add('modal-lg');
    } else {
      dialog.classList.remove('modal-lg');
    }
  };

  /* ═══ Prospect mode toggle (Opportunity) ═══ */
  window.aa_showProspectMode = function(mode) {
    document.getElementById('aa_prospectMode').value = mode;
    var isNew = (mode === 'new');
    document.getElementById('aa_existingProspectFields').style.display = isNew ? 'none' : '';
    document.getElementById('aa_newProspectFields').style.display = isNew ? '' : 'none';

    // Toggle required fields
    document.getElementById('aa_prospectId').required = !isNew;
    document.getElementById('aa_companyName').required = isNew;

    aa_validateOpp();
  };

  /* ═══ Prospect mode toggle (Setup) ═══ */
  window.aa_showSetupProspectMode = function(mode) {
    document.getElementById('aa_sProspectMode').value = mode;
    var isNew = (mode === 'new');
    document.getElementById('aa_sExistingProspectFields').style.display = isNew ? 'none' : '';
    document.getElementById('aa_sNewProspectFields').style.display = isNew ? '' : 'none';
    // Show agent dropdown for new prospect mode (agent must be assigned)
    aa_updateAgentVisibility();
    aa_validateSetup();
  };

  /* ═══ Enable/disable submit buttons based on selection ═══ */
  var employerSelect = document.getElementById('aa_employerId');
  var renewalBtn = document.getElementById('aa_renewalBtn');
  employerSelect.addEventListener('change', function() {
    renewalBtn.disabled = !employerSelect.value;
  });

  /* ═══ Opportunity validation ═══ */
  function aa_validateOpp() {
    var mode = document.getElementById('aa_prospectMode').value;
    var agencyOk = !!document.getElementById('aa_agencyId').value;
    var prospectOk;
    if (mode === 'existing') {
      prospectOk = !!document.getElementById('aa_prospectId').value;
    } else {
      prospectOk = document.getElementById('aa_companyName').value.trim().length > 0;
    }
    document.getElementById('aa_oppBtn').disabled = !(agencyOk && prospectOk);
  }
  window.aa_validateOpp = aa_validateOpp;

  document.getElementById('aa_prospectId').addEventListener('change', aa_validateOpp);
  document.getElementById('aa_agencyId').addEventListener('change', aa_validateOpp);
  document.getElementById('aa_companyName').addEventListener('input', aa_validateOpp);

  /* ═══ Agency change → filter agents, prospects, and rates ═══ */
  window.aa_onAgencyChange = function() {
    var agSel = document.getElementById('aa_sAgencyId');
    var agVal = agSel.value;
    var agOpt = agSel.options[agSel.selectedIndex];
    var ratesStr = (agOpt && agOpt.dataset && agOpt.dataset.rates) || '';
    var rateIds = ratesStr ? ratesStr.split(',').map(Number) : [];
    var currentPersonId = document.getElementById('aa_currentPersonId').value;
    var managerId = (agOpt && agOpt.dataset && agOpt.dataset.manager) || '';

    // ── Filter agent options by selected agency ──
    var agentSel = document.getElementById('aa_sAgentId');
    var agentCount = 0;
    var lastAgentIdx = -1;
    var currentPersonIdx = -1;
    var managerIdx = -1;
    for (var i = 0; i < agentSel.options.length; i++) {
      var aOpt = agentSel.options[i];
      if (!aOpt.classList.contains('aa-agent-option')) continue;
      var agencies = (aOpt.dataset.agencies || '').split(',');
      var show = agVal && agencies.indexOf(agVal) >= 0;
      aOpt.hidden = !show;
      aOpt.disabled = !show;
      if (show) {
        agentCount++;
        lastAgentIdx = i;
        if (aOpt.value === currentPersonId) currentPersonIdx = i;
        if (aOpt.value === managerId) managerIdx = i;
      }
    }

    // Determine if current user is an agent of the selected agency
    window.aa_userIsAgent = (currentPersonIdx >= 0);

    // Auto-select agent: current user > agency manager > single agent
    agentSel.selectedIndex = 0;
    if (currentPersonIdx >= 0) {
      agentSel.selectedIndex = currentPersonIdx;
    } else if (managerIdx >= 0) {
      agentSel.selectedIndex = managerIdx;
    } else if (agentCount === 1) {
      agentSel.selectedIndex = lastAgentIdx;
    }
    aa_updateAgentVisibility();

    // ── Filter prospect options by selected agency ──
    var prospSel = document.getElementById('aa_sProspectId');
    prospSel.selectedIndex = 0;
    for (var i = 0; i < prospSel.options.length; i++) {
      var pOpt = prospSel.options[i];
      if (!pOpt.classList.contains('aa-prospect-option')) continue;
      var pAgencies = (pOpt.dataset.agencies || '').split(',');
      var pShow = agVal && pAgencies.indexOf(agVal) >= 0;
      pOpt.hidden = !pShow;
      pOpt.disabled = !pShow;
    }

    // ── Filter rate options by agency ──
    var rateSel = document.getElementById('aa_sRateId');
    var visibleCount = 0;
    var lastVisibleIdx = -1;
    for (var i = 0; i < rateSel.options.length; i++) {
      var opt = rateSel.options[i];
      if (!opt.classList.contains('aa-rate-option')) continue;
      var rId = parseInt(opt.value);
      var show = rateIds.indexOf(rId) >= 0;
      opt.hidden = !show;
      opt.disabled = !show;
      if (show) { visibleCount++; lastVisibleIdx = i; }
    }
    rateSel.selectedIndex = 0;
    if (visibleCount === 1) {
      rateSel.selectedIndex = lastVisibleIdx;
    }

    // Cascade to rate → LOS/extras
    aa_onRateChange();
  };

  /* ═══ Show/hide agent dropdown based on context ═══ */
  /*  Logic tree:
   *  - Existing prospect: agent already assigned, no dropdown needed
   *  - New prospect + user IS agent of agency: auto-assign user, no dropdown
   *  - New prospect + user NOT agent: show dropdown (defaulted to agency manager)
   */
  window.aa_updateAgentVisibility = function() {
    var mode = document.getElementById('aa_sProspectMode').value;
    var agentRow = document.getElementById('aa_sAgentRow');

    if (mode === 'existing') {
      // Existing prospect — agent is already on the prospect, no dropdown
      agentRow.style.display = 'none';
    } else {
      // New prospect — show dropdown only if current user is NOT an agent of selected agency
      var showAgent = !window.aa_userIsAgent;
      agentRow.style.display = showAgent ? '' : 'none';
    }
  };

  /* ═══ Rate change → filter LOS & extras ═══ */
  window.aa_onRateChange = function() {
    var sel = document.getElementById('aa_sRateId');
    var opt = sel.options[sel.selectedIndex];
    var losStr = (opt && opt.dataset && opt.dataset.los) || '';
    var extraStr = (opt && opt.dataset && opt.dataset.extras) || '';
    var losIds = losStr ? losStr.split(',').map(Number) : [];
    var extraIds = extraStr ? extraStr.split(',').map(Number) : [];

    // Show/hide LOS switches
    var anyLos = false;
    document.querySelectorAll('.aa-los-item').forEach(function(div) {
      var id = parseInt(div.dataset.losId);
      var show = losIds.indexOf(id) >= 0;
      div.style.display = show ? '' : 'none';
      if (!show) div.querySelector('input').checked = false;
      if (show) anyLos = true;
    });
    var losHint = document.getElementById('aa_sLosHint');
    losHint.style.display = (sel.value && !anyLos) ? '' : 'none';
    if (sel.value && !anyLos) losHint.textContent = 'No lines of service configured for this rate';
    if (!sel.value) { losHint.style.display = ''; losHint.textContent = 'Select a rate package first'; }

    // Show/hide extra switches
    var anyExtra = false;
    document.querySelectorAll('.aa-extra-item').forEach(function(div) {
      var id = parseInt(div.dataset.extraId);
      var show = extraIds.indexOf(id) >= 0;
      div.style.display = show ? '' : 'none';
      if (!show) div.querySelector('input').checked = false;
      if (show) anyExtra = true;
    });
    var extraHint = document.getElementById('aa_sExtraHint');
    extraHint.style.display = anyExtra ? 'none' : '';
    if (!sel.value) { extraHint.textContent = 'Select a rate package first'; }
    else if (!anyExtra) { extraHint.textContent = 'No additional services for this rate'; }

    aa_validateSetup();
  };

  /* ═══ Setup validation ═══ */
  window.aa_validateSetup = function() {
    var mode = document.getElementById('aa_sProspectMode').value;
    var agencyOk = !!document.getElementById('aa_sAgencyId').value;
    var rateOk = !!document.getElementById('aa_sRateId').value;
    var prospectOk;
    if (mode === 'existing') {
      prospectOk = !!document.getElementById('aa_sProspectId').value;
    } else {
      prospectOk = document.getElementById('aa_sCompanyName').value.trim().length > 0;
    }
    // Agent must be selected if agent row is visible
    var agentRow = document.getElementById('aa_sAgentRow');
    var agentOk = (agentRow.style.display === 'none') || !!document.getElementById('aa_sAgentId').value;
    // At least one visible LOS selected
    var losOk = false;
    document.querySelectorAll('.aa-los-item').forEach(function(div) {
      if (div.style.display !== 'none' && div.querySelector('input').checked) losOk = true;
    });

    document.getElementById('aa_setupBtn').disabled = !(agencyOk && rateOk && prospectOk && losOk && agentOk);
  };

  document.getElementById('aa_sProspectId').addEventListener('change', aa_validateSetup);
  document.getElementById('aa_sAgencyId').addEventListener('change', function() { aa_onAgencyChange(); });
  document.getElementById('aa_sAgentId').addEventListener('change', aa_validateSetup);
  document.getElementById('aa_sRateId').addEventListener('change', function() { aa_onRateChange(); });
  document.getElementById('aa_sCompanyName').addEventListener('input', aa_validateSetup);

  // Default agency to the user's home agency (first agency they belong to)
  (function() {
    var agSel = document.getElementById('aa_sAgencyId');
    var personAgencies = (document.getElementById('aa_currentPersonAgencies').value || '').split(',');
    var homeAgencyId = personAgencies.length > 0 ? personAgencies[0] : '';

    // Try to select the user's home agency
    var found = false;
    if (homeAgencyId) {
      for (var i = 0; i < agSel.options.length; i++) {
        if (agSel.options[i].value === homeAgencyId) {
          agSel.selectedIndex = i;
          found = true;
          break;
        }
      }
    }
    // Fallback: if only one agency option (besides placeholder), select it
    if (!found) {
      var realOpts = 0; var lastReal = -1;
      for (var i = 0; i < agSel.options.length; i++) {
        if (!agSel.options[i].disabled) { realOpts++; lastReal = i; }
      }
      if (realOpts === 1) { agSel.selectedIndex = lastReal; found = true; }
    }

    if (agSel.value) aa_onAgencyChange();
  })();

  /* ═══ Prepare hidden inputs before submit ═══ */
  window.aa_prepareSetupSubmit = function() {
    // Build losIds hidden inputs
    var losContainer = document.getElementById('aa_sLosInputs');
    losContainer.innerHTML = '';
    document.querySelectorAll('.aa-los-switch:checked').forEach(function(cb) {
      var inp = document.createElement('input');
      inp.type = 'hidden'; inp.name = 'losIds'; inp.value = cb.value;
      losContainer.appendChild(inp);
    });

    // Build extraModuleIds hidden inputs
    var extraContainer = document.getElementById('aa_sExtraInputs');
    extraContainer.innerHTML = '';
    document.querySelectorAll('.aa-extra-switch:checked').forEach(function(cb) {
      var inp = document.createElement('input');
      inp.type = 'hidden'; inp.name = 'extraModuleIds'; inp.value = cb.value;
      extraContainer.appendChild(inp);
    });

    // Disable button to prevent double-submit
    setTimeout(function() {
      var btn = document.getElementById('aa_setupBtn');
      btn.disabled = true;
      btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Creating...';
    }, 50);
  };

  /* ═══ Reset on modal open/close ═══ */
  function resetModal() {
    // Reset to renewal type
    document.getElementById('aa_togRenewal').checked = true;
    aa_showType('renewal');

    // Reset renewal form
    document.getElementById('aa_employerId').selectedIndex = 0;
    renewalBtn.disabled = true;

    // Reset opportunity form
    document.getElementById('aa_togExisting').checked = true;
    aa_showProspectMode('existing');
    document.getElementById('aa_prospectId').selectedIndex = 0;
    document.getElementById('aa_companyName').value = '';
    document.querySelectorAll('#aa_newProspectFields input').forEach(function(el) { el.value = ''; });
    var agencySel = document.getElementById('aa_agencyId');
    if (agencySel.options.length > 1) agencySel.selectedIndex = 0;
    document.getElementById('aa_oppBtn').disabled = true;

    // Reset setup form
    document.getElementById('aa_sTogExisting').checked = true;
    document.getElementById('aa_sProspectMode').value = 'existing';
    document.getElementById('aa_sExistingProspectFields').style.display = '';
    document.getElementById('aa_sNewProspectFields').style.display = 'none';
    document.getElementById('aa_sProspectId').selectedIndex = 0;
    document.getElementById('aa_sCompanyName').value = '';
    document.querySelectorAll('#aa_sNewProspectFields input').forEach(function(el) { el.value = ''; });
    // Re-default to user's home agency
    var sAgencySel = document.getElementById('aa_sAgencyId');
    var personAgencies = (document.getElementById('aa_currentPersonAgencies').value || '').split(',');
    var homeId = personAgencies.length > 0 ? personAgencies[0] : '';
    var homeFound = false;
    if (homeId) {
      for (var k = 0; k < sAgencySel.options.length; k++) {
        if (sAgencySel.options[k].value === homeId) { sAgencySel.selectedIndex = k; homeFound = true; break; }
      }
    }
    if (!homeFound && sAgencySel.options.length > 1) sAgencySel.selectedIndex = 0;
    document.getElementById('aa_sAgentId').selectedIndex = 0;
    document.getElementById('aa_sAgentRow').style.display = 'none';
    document.querySelectorAll('.aa-los-switch, .aa-extra-switch').forEach(function(cb) { cb.checked = false; });
    document.getElementById('aa_sLosInputs').innerHTML = '';
    document.getElementById('aa_sExtraInputs').innerHTML = '';
    // Re-apply agency→rate→LOS cascade
    aa_onAgencyChange();
    var setupBtn = document.getElementById('aa_setupBtn');
    setupBtn.disabled = true;
    setupBtn.innerHTML = '<i class="bi bi-building-add me-1"></i>Create Setup';
  }

  modal.addEventListener('show.bs.modal', resetModal);
  modal.addEventListener('hidden.bs.modal', resetModal);
})();
</script>

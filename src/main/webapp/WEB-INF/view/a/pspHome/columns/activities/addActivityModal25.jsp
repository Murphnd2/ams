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
            <input type="hidden" name="losIds" id="aa_oppLosInput" value="">

            <%-- Agency (first — drives prospect cascade) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-people me-1 text-ssa"></i>Agency
            </label>
            <select class="form-select form-select-sm mb-3" name="agencyId" id="aa_agencyId" required
                    onchange="aa_onOppAgencyChange()">
              <option value="" selected disabled>Select agency...</option>
              <%-- Options populated by JavaScript via SetupModalData --%>
            </select>

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

            <%-- Existing prospect dropdown (filtered by agency) --%>
            <div id="aa_existingProspectFields">
              <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                <i class="bi bi-briefcase me-1 text-ssa"></i>Prospect
              </label>
              <select class="form-select form-select-sm mb-3" name="prospectId" id="aa_prospectId">
                <option value="" selected disabled>Select a prospect...</option>
                <%-- Options populated by JavaScript via SetupModalData, filtered by agency --%>
              </select>
            </div>

            <%-- New prospect fields --%>
            <div id="aa_newProspectFields" style="display:none;">
              <%-- Agent selector (visible only when user is NOT an agent of the selected agency) --%>
              <div id="aa_oppAgentRow" style="display:none;">
                <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                  <i class="bi bi-person-badge me-1 text-ssa"></i>Agent
                </label>
                <select class="form-select form-select-sm mb-2" name="agentId" id="aa_oppAgentId">
                  <option value="" selected disabled>Select agent...</option>
                  <%-- Options populated by JavaScript from SetupModalData agents array --%>
                </select>
              </div>
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

            <%-- Rate selector (filtered by agency) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-cash-coin me-1 text-ssa"></i>Rate Package
            </label>
            <select class="form-select form-select-sm mb-3" name="rateId" id="aa_oppRateId" required
                    onchange="aa_onOppRateChange()">
              <option value="" selected disabled>Select rate...</option>
              <%-- Options populated by JavaScript via SetupModalData --%>
            </select>

            <%-- LOS checkboxes (filtered by rate) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-list-check me-1 text-ssa"></i>Lines of Service
            </label>
            <div class="border rounded p-2 mb-3" id="aa_oppLosContainer" style="max-height:180px; overflow-y:auto;">
              <div id="aa_oppLosHint" class="text-muted small fst-italic">Select a rate package first</div>
              <%-- LOS switches populated by JavaScript via SetupModalData --%>
            </div>

            <button type="submit" class="btn btn-ssa btn-sm w-100" id="aa_oppBtn" disabled
                    onclick="return aa_prepareOppSubmit()">
              <i class="bi bi-graph-up-arrow me-1"></i>Create Opportunity
            </button>
          </form>
        </div>

        <%-- ═══ SETUP SECTION ═══ --%>
        <div id="aa_setupSection" style="display:none;">
          <form method="post" action="CreateSetup25" id="aa_setupForm">
            <input type="hidden" name="prospectMode" id="aa_sProspectMode" value="existing">
            <input type="hidden" name="returnTo" value="home">
            <input type="hidden" name="losIds" id="aa_sLosInput" value="">
            <input type="hidden" name="enhancementIds" id="aa_sEnhInput" value="">
            <input type="hidden" id="aa_currentPersonId" value="">
            <input type="hidden" id="aa_pspHomeAgencyId" value="">

            <%-- 1. Agency selector (drives all cascades) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-people me-1 text-ssa"></i>Agency
            </label>
            <select class="form-select form-select-sm mb-3" name="agencyId" id="aa_sAgencyId" required>
              <option value="" disabled>Select agency...</option>
              <%-- Options populated by JavaScript via SetupModalData --%>
            </select>

            <%-- 2. Prospect mode toggle --%>
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
                <%-- Options populated by JavaScript via SetupModalData --%>
              </select>
            </div>

            <%-- New prospect fields + agent selector --%>
            <div id="aa_sNewProspectFields" style="display:none;">
              <%-- Agent selector (visible only when user is NOT an agent of the selected agency) --%>
              <div id="aa_sAgentRow" style="display:none;">
                <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
                  <i class="bi bi-person-badge me-1 text-ssa"></i>Agent
                </label>
                <select class="form-select form-select-sm mb-2" name="agentId" id="aa_sAgentId">
                  <option value="" selected disabled>Select agent...</option>
                  <%-- Options populated by JavaScript via SetupModalData --%>
                </select>
              </div>
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

            <%-- 3. Rate selector (filtered by agency) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-cash-coin me-1 text-ssa"></i>Rate Package
            </label>
            <select class="form-select form-select-sm mb-3" name="rateId" id="aa_sRateId" required>
              <option value="" selected disabled>Select rate...</option>
              <%-- Options populated by JavaScript via SetupModalData --%>
            </select>

            <%-- 4. LOS checkboxes (filtered by rate) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-list-check me-1 text-ssa"></i>Lines of Service
            </label>
            <div class="border rounded p-2 mb-3" id="aa_sLosContainer" style="max-height:180px; overflow-y:auto;">
              <div id="aa_sLosHint" class="text-muted small fst-italic">Loading...</div>
              <%-- LOS switches populated by JavaScript via SetupModalData --%>
            </div>

            <%-- 5. Enhancement checkboxes (filtered by rate) --%>
            <label class="form-label fw-semibold mb-1" style="font-size:0.85rem;">
              <i class="bi bi-puzzle me-1 text-ssa"></i>Additional Services
            </label>
            <div class="border rounded p-2 mb-3" id="aa_sExtraContainer" style="max-height:180px; overflow-y:auto;">
              <%-- Enhancement switches populated by JavaScript via SetupModalData --%>
              <div id="aa_sExtraHint" class="text-muted small fst-italic">Loading...</div>
            </div>

            <%-- 6. Submit --%>
            <button type="submit" class="btn btn-ssa btn-sm w-100" id="aa_setupBtn" disabled
                    onclick="return aa_prepareSetupSubmit()">
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
  var aa_setupData = null; // holds the fetched setup data
  window.aa_oppUserIsAgent = false;
  window.aa_oppSingleAgent = false;

  /* ═══ Activity type toggle ═══ */
  window.aa_showType = function(type) {
    document.getElementById('aa_renewalSection').style.display = (type === 'renewal') ? '' : 'none';
    document.getElementById('aa_opportunitySection').style.display = (type === 'opportunity') ? '' : 'none';
    document.getElementById('aa_setupSection').style.display = (type === 'setup') ? '' : 'none';
    if (type === 'setup') {
      dialog.classList.add('modal-lg');
    } else {
      dialog.classList.remove('modal-lg');
    }
    if (type === 'opportunity' || type === 'setup') {
      aa_loadModalData(type);
    }
  };

  /* ═══ Prospect mode toggle (Opportunity) ═══ */
  window.aa_showProspectMode = function(mode) {
    document.getElementById('aa_prospectMode').value = mode;
    var isNew = (mode === 'new');
    document.getElementById('aa_existingProspectFields').style.display = isNew ? 'none' : '';
    document.getElementById('aa_newProspectFields').style.display = isNew ? '' : 'none';
    document.getElementById('aa_prospectId').required = !isNew;
    document.getElementById('aa_companyName').required = isNew;
    aa_updateOppAgentVisibility();
    aa_validateOpp();
  };

  /* ═══ Prospect mode toggle (Setup) ═══ */
  window.aa_showSetupProspectMode = function(mode) {
    document.getElementById('aa_sProspectMode').value = mode;
    var isNew = (mode === 'new');
    document.getElementById('aa_sExistingProspectFields').style.display = isNew ? 'none' : '';
    document.getElementById('aa_sNewProspectFields').style.display = isNew ? '' : 'none';
    aa_updateAgentVisibility();
    aa_validateSetup();
  };

  /* ═══ Renewal validation ═══ */
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
    // Agent must be selected if agent row is visible
    var agentRow = document.getElementById('aa_oppAgentRow');
    var agentOk = (agentRow.style.display === 'none') || !!document.getElementById('aa_oppAgentId').value;
    // Rate must be selected
    var rateOk = !!document.getElementById('aa_oppRateId').value;
    // At least one visible LOS must be checked
    var losOk = false;
    document.querySelectorAll('.aa-opp-los-item').forEach(function(div) {
      if (div.style.display !== 'none' && div.querySelector('input').checked) losOk = true;
    });
    document.getElementById('aa_oppBtn').disabled = !(agencyOk && prospectOk && agentOk && rateOk && losOk);
  }
  window.aa_validateOpp = aa_validateOpp;
  document.getElementById('aa_prospectId').addEventListener('change', aa_validateOpp);
  document.getElementById('aa_agencyId').addEventListener('change', aa_validateOpp);
  document.getElementById('aa_companyName').addEventListener('input', aa_validateOpp);
  document.getElementById('aa_oppAgentId').addEventListener('change', aa_validateOpp);

  /* ═══════════════════════════════════════════════════════════════════
     SETUP — AJAX DATA LOADING
     Fetches fresh data from SetupModalData and rebuilds all dropdowns
     ═══════════════════════════════════════════════════════════════════ */

  window.aa_loadModalData = function(type) {
    // If we already fetched this modal open, just rebuild from cache
    if (aa_setupData) {
      if (type === 'setup') aa_rebuildSetupOptions();
      if (type === 'opportunity') aa_rebuildOppOptions();
      return;
    }

    // Disable submit buttons while loading
    document.getElementById('aa_oppBtn').disabled = true;
    document.getElementById('aa_setupBtn').disabled = true;

    fetch('SetupModalData')
      .then(function(r) { return r.json(); })
      .then(function(data) {
        aa_setupData = data;
        document.getElementById('aa_currentPersonId').value = data.currentPersonId;
        document.getElementById('aa_pspHomeAgencyId').value = data.homeAgencyId || '';
        aa_rebuildSetupOptions();
        aa_rebuildOppOptions();
      })
      .catch(function(err) {
        console.error('Failed to load modal data:', err);
      });
  };

  function aa_rebuildSetupOptions() {
    var data = aa_setupData;
    if (!data) return;

    // ── Build agency options ──
    var agSel = document.getElementById('aa_sAgencyId');
    agSel.length = 1; // keep placeholder
    for (var i = 0; i < data.agencies.length; i++) {
      var a = data.agencies[i];
      var opt = document.createElement('option');
      opt.value = a.id;
      opt.textContent = a.name;
      opt.dataset.rates = a.rateIds; // comma-separated string from server
      agSel.appendChild(opt);
    }

    // ── Build prospect options ──
    var prospSel = document.getElementById('aa_sProspectId');
    prospSel.length = 1;
    for (var i = 0; i < data.prospects.length; i++) {
      var p = data.prospects[i];
      var opt = document.createElement('option');
      opt.value = p.id;
      opt.textContent = p.name;
      opt.className = 'aa-prospect-option';
      opt.dataset.agencies = p.agencyIds;
      opt.hidden = true;
      prospSel.appendChild(opt);
    }

    // ── Build agent options ──
    var agentSel = document.getElementById('aa_sAgentId');
    agentSel.length = 1;
    for (var i = 0; i < data.agents.length; i++) {
      var ag = data.agents[i];
      var opt = document.createElement('option');
      opt.value = ag.id;
      opt.textContent = ag.name;
      opt.className = 'aa-agent-option';
      opt.dataset.agencies = ag.agencyIds;
      opt.hidden = true;
      agentSel.appendChild(opt);
    }

    // ── Build rate options ──
    var rateSel = document.getElementById('aa_sRateId');
    rateSel.length = 1;
    for (var i = 0; i < data.rates.length; i++) {
      var r = data.rates[i];
      var opt = document.createElement('option');
      opt.value = r.id;
      opt.textContent = r.description;
      opt.className = 'aa-rate-option';
      opt.hidden = true;
      rateSel.appendChild(opt);
    }

    // ── Build LOS switches ──
    var losContainer = document.getElementById('aa_sLosContainer');
    var losHint = document.getElementById('aa_sLosHint');
    // Remove old dynamic switches (keep hint)
    var oldLos = losContainer.querySelectorAll('.aa-los-item');
    for (var i = 0; i < oldLos.length; i++) oldLos[i].remove();
    losHint.textContent = 'Select a rate package first';
    losHint.style.display = '';

    for (var i = 0; i < data.losList.length; i++) {
      var los = data.losList[i];
      var div = document.createElement('div');
      div.className = 'form-check form-switch mb-1 aa-los-item';
      div.dataset.losId = los.id;
      div.style.display = 'none';
      div.innerHTML =
        '<input class="form-check-input aa-los-switch" type="checkbox" ' +
        'id="aa_sLos_' + los.id + '" value="' + los.id + '" onchange="aa_filterEnhancements(); aa_validateSetup()">' +
        '<label class="form-check-label" for="aa_sLos_' + los.id + '" style="font-size:0.85rem;">' +
        aa_escapeHtml(los.description) + '</label>';
      losContainer.insertBefore(div, losHint);
    }

    // ── Build enhancement switches ──
    var extraContainer = document.getElementById('aa_sExtraContainer');
    var extraHint = document.getElementById('aa_sExtraHint');
    var oldExtra = extraContainer.querySelectorAll('.aa-extra-item');
    for (var i = 0; i < oldExtra.length; i++) oldExtra[i].remove();
    extraHint.textContent = 'Select a rate package first';
    extraHint.style.display = '';

    for (var i = 0; i < data.enhancements.length; i++) {
      var enh = data.enhancements[i];
      var div = document.createElement('div');
      div.className = 'form-check form-switch mb-1 aa-extra-item';
      div.dataset.extraId = enh.serviceItemId;
      div.style.display = 'none';
      div.innerHTML =
        '<input class="form-check-input aa-extra-switch" type="checkbox" ' +
        'id="aa_sExtra_' + enh.id + '" value="' + enh.id + '" onchange="aa_validateSetup()">' +
        '<label class="form-check-label" for="aa_sExtra_' + enh.id + '" style="font-size:0.85rem;">' +
        aa_escapeHtml(enh.description) + '</label>';
      extraContainer.insertBefore(div, extraHint);
    }

    // ── Set default agency to home agency and trigger cascade ──
    var homeId = String(data.homeAgencyId || '');
    var found = false;
    if (homeId) {
      for (var i = 0; i < agSel.options.length; i++) {
        if (agSel.options[i].value === homeId) { agSel.selectedIndex = i; found = true; break; }
      }
    }
    if (!found) {
      for (var i = 0; i < agSel.options.length; i++) {
        if (!agSel.options[i].disabled && agSel.options[i].value) { agSel.selectedIndex = i; break; }
      }
    }
    if (agSel.value) aa_onAgencyChange();
  }

  /** Simple HTML escaper for dynamic content */
  function aa_escapeHtml(s) {
    if (!s) return '';
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  /* ═══════════════════════════════════════════════════════════════════
     OPPORTUNITY — AJAX POPULATION
     Builds agency dropdown, cascades to prospect list
     ═══════════════════════════════════════════════════════════════════ */

  function aa_rebuildOppOptions() {
    var data = aa_setupData;
    if (!data) return;

    // Build agency options
    var agSel = document.getElementById('aa_agencyId');
    agSel.length = 1; // keep "Select agency..." placeholder

    for (var i = 0; i < data.agencies.length; i++) {
      var a = data.agencies[i];
      var opt = document.createElement('option');
      opt.value = a.id;
      opt.textContent = a.name;
      opt.dataset.rates = a.rateIds || '';
      agSel.appendChild(opt);
    }

    // Build rate options
    var rateSel = document.getElementById('aa_oppRateId');
    rateSel.length = 1;
    for (var i = 0; i < data.rates.length; i++) {
      var r = data.rates[i];
      var opt = document.createElement('option');
      opt.value = r.id;
      opt.textContent = r.description;
      rateSel.appendChild(opt);
    }

    // Build LOS switches
    var losContainer = document.getElementById('aa_oppLosContainer');
    var losHint = document.getElementById('aa_oppLosHint');
    var oldLos = losContainer.querySelectorAll('.aa-opp-los-item');
    for (var i = 0; i < oldLos.length; i++) oldLos[i].remove();
    losHint.textContent = 'Select a rate package first';
    losHint.style.display = '';

    for (var i = 0; i < data.losList.length; i++) {
      var los = data.losList[i];
      var div = document.createElement('div');
      div.className = 'form-check form-switch mb-1 aa-opp-los-item';
      div.dataset.losId = los.id;
      div.style.display = 'none';
      div.innerHTML =
        '<input class="form-check-input aa-opp-los-switch" type="checkbox" ' +
        'id="aa_oppLos_' + los.id + '" value="' + los.id + '" onchange="aa_validateOpp()">' +
        '<label class="form-check-label" for="aa_oppLos_' + los.id + '" style="font-size:0.85rem;">' +
        aa_escapeHtml(los.description) + '</label>';
      losContainer.insertBefore(div, losHint);
    }

    // Auto-select if only one agency
    if (data.agencies.length === 1) {
      agSel.selectedIndex = 1;
      aa_onOppAgencyChange();
    }
    // Or auto-select homeAgencyId if present
    else if (data.homeAgencyId) {
      for (var i = 0; i < agSel.options.length; i++) {
        if (agSel.options[i].value == data.homeAgencyId) {
          agSel.selectedIndex = i;
          aa_onOppAgencyChange();
          break;
        }
      }
    }

    aa_validateOpp();
  }

  window.aa_onOppAgencyChange = function() {
    var agVal = document.getElementById('aa_agencyId').value;
    var data = aa_setupData;
    if (!data) return;

    // ── Rebuild prospect dropdown with only matching prospects ──
    var prospSel = document.getElementById('aa_prospectId');
    prospSel.length = 1; // keep "Select a prospect..." placeholder

    if (agVal && data.prospects) {
      for (var i = 0; i < data.prospects.length; i++) {
        var p = data.prospects[i];
        var pAgencies = (p.agencyIds || '').split(',');
        if (pAgencies.indexOf(String(agVal)) >= 0) {
          var opt = document.createElement('option');
          opt.value = p.id;
          opt.textContent = p.name;
          prospSel.appendChild(opt);
        }
      }
    }

    // ── Rebuild agent dropdown for new-prospect mode ──
    var agentSel = document.getElementById('aa_oppAgentId');
    agentSel.length = 1; // keep placeholder
    var agentCount = 0;
    var lastAgentIdx = -1;
    var currentPersonIdx = -1;
    var currentPersonId = data.currentPersonId;

    if (agVal && data.agents) {
      for (var i = 0; i < data.agents.length; i++) {
        var ag = data.agents[i];
        var agAgencies = (ag.agencyIds || '').split(',');
        if (agAgencies.indexOf(String(agVal)) >= 0) {
          var opt = document.createElement('option');
          opt.value = ag.id;
          opt.textContent = ag.name;
          agentSel.appendChild(opt);
          agentCount++;
          lastAgentIdx = agentSel.options.length - 1;
          if (ag.id == currentPersonId) {
            currentPersonIdx = agentSel.options.length - 1;
          }
        }
      }
    }

    // Track state for visibility logic
    window.aa_oppUserIsAgent = (currentPersonIdx >= 0);
    window.aa_oppSingleAgent = (agentCount === 1);

    // Auto-select agent: current user > single agent > placeholder
    agentSel.selectedIndex = 0;
    if (currentPersonIdx >= 0) {
      agentSel.selectedIndex = currentPersonIdx;
    } else if (agentCount === 1) {
      agentSel.selectedIndex = lastAgentIdx;
    }

    // ── Filter rate dropdown by agency's rates ──
    var agOpt = document.getElementById('aa_agencyId').options[document.getElementById('aa_agencyId').selectedIndex];
    var ratesStr = (agOpt && agOpt.dataset && agOpt.dataset.rates) || '';
    var agencyRateIds = ratesStr ? ratesStr.split(',').map(Number) : [];

    var rateSel = document.getElementById('aa_oppRateId');
    rateSel.selectedIndex = 0;
    for (var i = 1; i < rateSel.options.length; i++) {
      var show = agencyRateIds.indexOf(parseInt(rateSel.options[i].value)) >= 0;
      rateSel.options[i].style.display = show ? '' : 'none';
      rateSel.options[i].disabled = !show;
    }
    // Auto-select if only one rate visible
    var visibleRates = [];
    for (var i = 1; i < rateSel.options.length; i++) {
      if (!rateSel.options[i].disabled) visibleRates.push(i);
    }
    if (visibleRates.length === 1) rateSel.selectedIndex = visibleRates[0];

    aa_onOppRateChange();
    aa_updateOppAgentVisibility();
    aa_validateOpp();
  };

  /* ═══ Opportunity Rate → LOS cascade ═══ */
  window.aa_onOppRateChange = function() {
    var rateId = document.getElementById('aa_oppRateId').value;
    var losIds = [];
    if (rateId && aa_setupData && aa_setupData.rateLosMap[rateId]) {
      losIds = aa_setupData.rateLosMap[rateId];
    }

    var anyLos = false;
    document.querySelectorAll('.aa-opp-los-item').forEach(function(div) {
      var id = parseInt(div.dataset.losId);
      var show = losIds.indexOf(id) >= 0;
      div.style.display = show ? '' : 'none';
      if (!show) div.querySelector('input').checked = false;
      if (show) anyLos = true;
    });
    var losHint = document.getElementById('aa_oppLosHint');
    losHint.style.display = (rateId && !anyLos) ? '' : 'none';
    if (rateId && !anyLos) losHint.textContent = 'No lines of service configured for this rate';
    if (!rateId) { losHint.style.display = ''; losHint.textContent = 'Select a rate package first'; }

    aa_validateOpp();
  };

  /* ═══ Show/hide agent dropdown (Opportunity) ═══ */
  window.aa_updateOppAgentVisibility = function() {
    var mode = document.getElementById('aa_prospectMode').value;
    var agentRow = document.getElementById('aa_oppAgentRow');
    if (mode === 'existing') {
      agentRow.style.display = 'none';
    } else {
      var showAgent = !window.aa_oppUserIsAgent && !window.aa_oppSingleAgent;
      agentRow.style.display = showAgent ? '' : 'none';
    }
  };

  /* ═══════════════════════════════════════════════════════════════════
     SETUP CASCADE LOGIC
     Agency → prospects, agents, rates → LOS, enhancements → validate
     ═══════════════════════════════════════════════════════════════════ */

  window.aa_onAgencyChange = function() {
    var agSel = document.getElementById('aa_sAgencyId');
    var agVal = agSel.value;
    var agOpt = agSel.options[agSel.selectedIndex];
    var ratesStr = (agOpt && agOpt.dataset && agOpt.dataset.rates) || '';
    var rateIds = ratesStr ? ratesStr.split(',').map(Number) : [];
    var currentPersonId = document.getElementById('aa_currentPersonId').value;

    // ── Filter agent options by selected agency ──
    var agentSel = document.getElementById('aa_sAgentId');
    var agentCount = 0;
    var lastAgentIdx = -1;
    var currentPersonIdx = -1;
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
      }
    }

    // Is current user an agent of this agency?
    window.aa_userIsAgent = (currentPersonIdx >= 0);
    // Is there only one agent? If so auto-select and hide
    window.aa_singleAgent = (agentCount === 1);

    // Auto-select agent: current user > single agent > first placeholder
    agentSel.selectedIndex = 0;
    if (currentPersonIdx >= 0) {
      agentSel.selectedIndex = currentPersonIdx;
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

  /* ═══ Show/hide agent dropdown ═══
   *  Existing prospect: agent already assigned → hide
   *  New prospect + user IS agent: auto-assign user → hide
   *  New prospect + only one agent in agency: auto-selected → hide
   *  New prospect + multiple agents + user NOT agent: show dropdown
   */
  window.aa_updateAgentVisibility = function() {
    var mode = document.getElementById('aa_sProspectMode').value;
    var agentRow = document.getElementById('aa_sAgentRow');
    if (mode === 'existing') {
      agentRow.style.display = 'none';
    } else {
      var showAgent = !window.aa_userIsAgent && !window.aa_singleAgent;
      agentRow.style.display = showAgent ? '' : 'none';
    }
  };

  /* ═══ Rate change → filter LOS & extras ═══ */
  window.aa_onRateChange = function() {
    var sel = document.getElementById('aa_sRateId');
    var rateId = sel.value;

    // Look up LOS and extra IDs from the fetched data maps
    var losIds = [];
    var extraIds = [];
    if (rateId && aa_setupData) {
      var losArr = aa_setupData.rateLosMap[rateId];
      if (losArr) losIds = losArr;
      var extraArr = aa_setupData.rateExtraMap[rateId];
      if (extraArr) extraIds = extraArr;
    }

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
    losHint.style.display = (rateId && !anyLos) ? '' : 'none';
    if (rateId && !anyLos) losHint.textContent = 'No lines of service configured for this rate';
    if (!rateId) { losHint.style.display = ''; losHint.textContent = 'Select a rate package first'; }

    // Filter enhancements based on rate AND checked LOSs
    aa_filterEnhancements();

    aa_validateSetup();
  };

  /* ═══ LOS → Enhancement cascade ═══ */
  window.aa_filterEnhancements = function() {
    var rateId = document.getElementById('aa_sRateId').value;

    // Get checked LOS IDs (only visible ones)
    var checkedLosIds = [];
    document.querySelectorAll('.aa-los-item').forEach(function(div) {
      if (div.style.display !== 'none' && div.querySelector('input').checked) {
        checkedLosIds.push(parseInt(div.dataset.losId));
      }
    });

    // Enhancement is visible if at least one of its parent LOSs is checked
    var enhLosMap = (aa_setupData && aa_setupData.enhLosMap) ? aa_setupData.enhLosMap : {};
    var anyExtra = false;
    document.querySelectorAll('.aa-extra-item').forEach(function(div) {
      var enhId = div.querySelector('input').value;
      var parentLosIds = enhLosMap[enhId] || [];
      var losChecked = false;
      for (var i = 0; i < parentLosIds.length; i++) {
        if (checkedLosIds.indexOf(parentLosIds[i]) >= 0) { losChecked = true; break; }
      }

      div.style.display = losChecked ? '' : 'none';
      if (!losChecked) div.querySelector('input').checked = false;
      if (losChecked) anyExtra = true;
    });

    var extraHint = document.getElementById('aa_sExtraHint');
    if (!rateId) {
      extraHint.style.display = ''; extraHint.textContent = 'Select a rate package first';
    } else if (!anyExtra && checkedLosIds.length > 0) {
      extraHint.style.display = ''; extraHint.textContent = 'No additional services for selected lines';
    } else {
      extraHint.style.display = 'none';
    }
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

  /* ═══ Wire up change/input listeners ═══ */
  document.getElementById('aa_sProspectId').addEventListener('change', aa_validateSetup);
  document.getElementById('aa_sAgencyId').addEventListener('change', function() { aa_onAgencyChange(); });
  document.getElementById('aa_sAgentId').addEventListener('change', aa_validateSetup);
  document.getElementById('aa_sRateId').addEventListener('change', function() { aa_onRateChange(); });
  document.getElementById('aa_sCompanyName').addEventListener('input', aa_validateSetup);

  /* ═══ Prepare hidden inputs before submit ═══ */
  window.aa_prepareOppSubmit = function() {
    // Build comma-separated losIds from opportunity LOS switches
    var losVals = [];
    document.querySelectorAll('.aa-opp-los-switch:checked').forEach(function(cb) {
      losVals.push(cb.value);
    });
    document.getElementById('aa_oppLosInput').value = losVals.join(',');

    // Disable button to prevent double-submit
    setTimeout(function() {
      var btn = document.getElementById('aa_oppBtn');
      btn.disabled = true;
      btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Creating...';
    }, 50);

    return true;
  };

  window.aa_prepareSetupSubmit = function() {
    // Build comma-separated losIds
    var losVals = [];
    document.querySelectorAll('.aa-los-switch:checked').forEach(function(cb) {
      losVals.push(cb.value);
    });
    document.getElementById('aa_sLosInput').value = losVals.join(',');

    // Build comma-separated enhancementIds
    var enhVals = [];
    document.querySelectorAll('.aa-extra-switch:checked').forEach(function(cb) {
      enhVals.push(cb.value);
    });
    document.getElementById('aa_sEnhInput').value = enhVals.join(',');

    // Disable button to prevent double-submit
    setTimeout(function() {
      var btn = document.getElementById('aa_setupBtn');
      btn.disabled = true;
      btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Creating...';
    }, 50);

    return true; // allow form submission
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
    document.getElementById('aa_agencyId').length = 1; // clear AJAX options, keep placeholder
    document.getElementById('aa_prospectId').length = 1; // clear AJAX options, keep placeholder
    document.getElementById('aa_oppAgentId').length = 1; // clear AJAX agent options, keep placeholder
    document.getElementById('aa_oppAgentRow').style.display = 'none';
    window.aa_oppUserIsAgent = false;
    window.aa_oppSingleAgent = false;
    document.getElementById('aa_companyName').value = '';
    document.querySelectorAll('#aa_newProspectFields input').forEach(function(el) { if (el.type !== 'hidden') el.value = ''; });
    document.getElementById('aa_oppBtn').disabled = true;
    document.getElementById('aa_oppBtn').innerHTML = '<i class="bi bi-graph-up-arrow me-1"></i>Create Opportunity';
    document.getElementById('aa_oppRateId').selectedIndex = 0;
    document.getElementById('aa_oppLosInput').value = '';
    document.querySelectorAll('.aa-opp-los-item input').forEach(function(cb) { cb.checked = false; });
    document.querySelectorAll('.aa-opp-los-item').forEach(function(div) { div.style.display = 'none'; });
    var oppLosHint = document.getElementById('aa_oppLosHint');
    if (oppLosHint) { oppLosHint.style.display = ''; oppLosHint.textContent = 'Select a rate package first'; }

    // Reset setup form
    document.getElementById('aa_sTogExisting').checked = true;
    document.getElementById('aa_sProspectMode').value = 'existing';
    document.getElementById('aa_sExistingProspectFields').style.display = '';
    document.getElementById('aa_sNewProspectFields').style.display = 'none';
    document.getElementById('aa_sProspectId').selectedIndex = 0;
    document.getElementById('aa_sCompanyName').value = '';
    document.querySelectorAll('#aa_sNewProspectFields input').forEach(function(el) { el.value = ''; });
    document.getElementById('aa_sLosInput').value = '';
    document.getElementById('aa_sEnhInput').value = '';
    document.querySelectorAll('.aa-los-switch, .aa-extra-switch').forEach(function(cb) { cb.checked = false; });
    document.getElementById('aa_sAgentId').selectedIndex = 0;
    document.getElementById('aa_sAgentRow').style.display = 'none';

    var setupBtn = document.getElementById('aa_setupBtn');
    setupBtn.disabled = true;
    setupBtn.innerHTML = '<i class="bi bi-building-add me-1"></i>Create Setup';
  }

  modal.addEventListener('show.bs.modal', resetModal);
  modal.addEventListener('hidden.bs.modal', function() {
    resetModal();
    aa_setupData = null; // clear cache so next open fetches fresh data
  });
})();
</script>

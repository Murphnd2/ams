<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  Add Activity Modal — Create a new Renewal or Opportunity from the PSP home page.
  Opened from the "+" button in activityHeader25.jsp.
  All element IDs prefixed aa_ to avoid collisions with other modals.
--%>
<div class="modal fade" id="addActivityModal" tabindex="-1" aria-labelledby="addActivityLabel" aria-hidden="true">
  <div class="modal-dialog modal-fullscreen-sm-down" role="document">
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

      </div>
    </div>
  </div>
</div>

<script>
(function(){
  var modal = document.getElementById('addActivityModal');

  /* ═══ Activity type toggle ═══ */
  window.aa_showType = function(type) {
    document.getElementById('aa_renewalSection').style.display = (type === 'renewal') ? '' : 'none';
    document.getElementById('aa_opportunitySection').style.display = (type === 'opportunity') ? '' : 'none';
  };

  /* ═══ Prospect mode toggle ═══ */
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

  /* ═══ Enable/disable submit buttons based on selection ═══ */
  var employerSelect = document.getElementById('aa_employerId');
  var renewalBtn = document.getElementById('aa_renewalBtn');
  employerSelect.addEventListener('change', function() {
    renewalBtn.disabled = !employerSelect.value;
  });

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
  }

  modal.addEventListener('show.bs.modal', resetModal);
  modal.addEventListener('hidden.bs.modal', resetModal);
})();
</script>

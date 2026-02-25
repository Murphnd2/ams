<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Unified Create User Modal (v3 — Sales Capability checkbox)
  Role-aware:
    PSP Admin  → role checkboxes (PSP User, PSP Admin) + standalone Sales Capability toggle
    Agency Admin → agent-only form (agency auto-resolved server-side)
    BPO Admin → BPO user form with optional admin checkbox
--%>
<c:if test="${sessionScope.isPspAdmin || sessionScope.isAgencyAdmin || sessionScope.isBpoAdmin}">
<div class="modal fade" id="createUserModal" role="dialog" tabindex="-1" aria-labelledby="createUserLabel" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header" style="background:#0d5681; color:white; padding:0.6rem 1rem;">
        <h6 class="modal-title fw-bold m-0" id="createUserLabel">
          <i class="bi bi-person-plus-fill me-1"></i>Create User
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">

        <%-- Success/Error messages --%>
        <c:if test="${not empty sessionScope.createUserError}">
          <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
            <i class="bi bi-exclamation-triangle me-1"></i>${sessionScope.createUserError}
            <button type="button" class="btn-close" data-bs-dismiss="alert" style="padding:0.5rem;"></button>
          </div>
          <c:remove var="createUserError" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.createUserSuccess}">
          <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
            <i class="bi bi-check-circle me-1"></i>${sessionScope.createUserSuccess}
            <button type="button" class="btn-close" data-bs-dismiss="alert" style="padding:0.5rem;"></button>
          </div>
          <c:remove var="createUserSuccess" scope="session"/>
        </c:if>

        <form method="post" action="CreateUser25" id="createUserForm">

          <%-- Hidden creatorType field — determines which servlet path runs --%>
          <c:choose>
            <c:when test="${sessionScope.isPspAdmin}">
              <input type="hidden" name="creatorType" value="pspAdmin">
            </c:when>
            <c:when test="${sessionScope.isBpoAdmin}">
              <input type="hidden" name="creatorType" value="bpoAdmin">
            </c:when>
            <c:when test="${sessionScope.isAgencyAdmin}">
              <input type="hidden" name="creatorType" value="agencyAdmin">
            </c:when>
          </c:choose>

          <%-- ═══ COMMON FIELDS ═══ --%>
          <div class="row mb-2">
            <div class="col-sm-6">
              <label class="form-label fw-semibold mb-0" style="font-size:0.85rem;">First Name</label>
              <input class="form-control form-control-sm" required type="text" name="firstName" placeholder="First Name">
            </div>
            <div class="col-sm-6">
              <label class="form-label fw-semibold mb-0" style="font-size:0.85rem;">Last Name</label>
              <input class="form-control form-control-sm" required type="text" name="lastName" placeholder="Last Name">
            </div>
          </div>

          <div class="mb-2">
            <label class="form-label fw-semibold mb-0" style="font-size:0.85rem;">Email</label>
            <input class="form-control form-control-sm" required type="email" name="userEmail" placeholder="Enter user's email">
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold mb-0" style="font-size:0.85rem;">Temporary Password</label>
            <input class="form-control form-control-sm" type="text" name="tempPassword" placeholder="If left blank, one will be generated">
          </div>

          <%-- ═══ PSP ADMIN: ROLE CHECKBOXES + SALES CAPABILITY ═══ --%>
          <c:if test="${sessionScope.isPspAdmin}">
            <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em; margin-bottom:0.4rem;">
              Assign Roles
            </div>
            <div class="border rounded p-2 mb-2" style="background:#f8f9fa;">
              <c:forEach var="role" items="${applicationScope.global.assignableRoles}">
                <div class="form-check">
                  <input type="checkbox" class="form-check-input role-cb"
                         name="roleIds" value="${role.id}" id="role_${role.id}">
                  <label for="role_${role.id}" class="form-check-label" style="font-size:0.85rem;">
                    ${role.description}
                  </label>
                </div>
              </c:forEach>

              <%-- Standalone Sales Capability checkbox (not from DB query) --%>
              <hr class="my-2" style="border-color:#dee2e6;">
              <div class="form-check">
                <input type="checkbox" class="form-check-input" id="salesCapability"
                       name="salesCapability" value="1" onchange="onSalesCapabilityChange()">
                <label for="salesCapability" class="form-check-label" style="font-size:0.85rem;">
                  <i class="bi bi-briefcase me-1"></i>Will Require Sales Capability
                </label>
              </div>
            </div>

            <%-- Agency section — shown when Sales Capability is checked --%>
            <div id="agencySection" style="display:none;">
              <div class="mb-2">
                <label class="form-label fw-semibold mb-0" style="font-size:0.85rem;">Agency</label>
                <select id="agencySelect" name="agencyId" class="form-select form-select-sm">
                  <option value="">-- Select Agency --</option>
                  <c:forEach var="agency" items="${applicationScope.global.agencies}">
                    <option value="${agency.id}"
                            data-manager-name="${agency.manager != null ? agency.manager.firstName.concat(' ').concat(agency.manager.lastName) : ''}"
                            data-manager-id="${agency.manager != null ? agency.manager.id : '0'}">
                        ${agency.name}
                    </option>
                  </c:forEach>
                </select>
              </div>

              <div class="form-check mb-2">
                <input type="checkbox" id="makeManager" name="makeManager" value="1" class="form-check-input" onchange="checkManagerWarning()">
                <label for="makeManager" class="form-check-label" style="font-size:0.85rem;">
                  <i class="bi bi-star me-1"></i>Designate as Agency Manager
                </label>
              </div>

              <%-- Manager replacement warning --%>
              <div id="managerWarning" class="alert alert-warning py-2 d-none" style="font-size:0.8rem;">
                <i class="bi bi-exclamation-triangle-fill me-1"></i>
                <strong>Warning:</strong> This will replace <span id="currentManagerName"></span> as manager of the selected agency.
              </div>
            </div>
          </c:if>

          <%-- ═══ AGENCY ADMIN: AGENT-ONLY (no role selection needed) ═══ --%>
          <c:if test="${sessionScope.isAgencyAdmin && !sessionScope.isPspAdmin}">
            <div class="text-muted mb-2" style="font-size:0.8rem;">
              <i class="bi bi-info-circle me-1"></i>This user will be created as an Agent in your agency.
            </div>
          </c:if>

          <%-- ═══ BPO ADMIN: BPO OPTIONS ═══ --%>
          <c:if test="${sessionScope.isBpoAdmin && !sessionScope.isPspAdmin}">
            <div class="form-check mb-2">
              <input type="checkbox" id="makeBpoAdmin" name="makeBpoAdmin" value="1" class="form-check-input">
              <label for="makeBpoAdmin" class="form-check-label" style="font-size:0.85rem;">
                <i class="bi bi-shield-lock me-1"></i>Make BPO Administrator
              </label>
            </div>
          </c:if>

          <hr class="my-2">

          <div class="d-flex justify-content-end gap-2">
            <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-ssa btn-sm">
              <i class="bi bi-person-plus me-1"></i>Create User
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<script>
  function onSalesCapabilityChange() {
    const salesCb = document.getElementById('salesCapability');
    const agencySection = document.getElementById('agencySection');
    if (!salesCb || !agencySection) return;

    if (salesCb.checked) {
      agencySection.style.display = '';
    } else {
      agencySection.style.display = 'none';
      // Reset agency fields when hiding
      const agencySelect = document.getElementById('agencySelect');
      if (agencySelect) agencySelect.value = '';
      const mgr = document.getElementById('makeManager');
      if (mgr) mgr.checked = false;
      const warn = document.getElementById('managerWarning');
      if (warn) warn.classList.add('d-none');
    }
  }

  function checkManagerWarning() {
    const managerCb = document.getElementById('makeManager');
    const warn = document.getElementById('managerWarning');
    const agencySelect = document.getElementById('agencySelect');
    if (!managerCb || !warn || !agencySelect) return;

    if (managerCb.checked && agencySelect.value) {
      const selected = agencySelect.options[agencySelect.selectedIndex];
      const currentManagerName = selected.dataset.managerName || '';
      const currentManagerId = selected.dataset.managerId || '0';

      if (currentManagerId !== '0' && currentManagerName.trim() !== '') {
        document.getElementById('currentManagerName').textContent = currentManagerName;
        warn.classList.remove('d-none');
      } else {
        warn.classList.add('d-none');
      }
    } else {
      warn.classList.add('d-none');
    }
  }

  // Also check warning when agency selection changes
  const agencySel = document.getElementById('agencySelect');
  if (agencySel) {
    agencySel.addEventListener('change', function() {
      checkManagerWarning();
    });
  }
</script>
</c:if>

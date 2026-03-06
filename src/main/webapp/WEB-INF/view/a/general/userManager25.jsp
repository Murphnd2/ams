<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- User Manager Modal — tabbed: Create User | Manage Users. Included in navbar25.jsp (PSP Admin only) --%>
<style>
  .um-action {
    background: none; border: none; color: var(--ssa, #0d5681);
    font-size: 0.75rem; padding: 2px 6px; cursor: pointer;
    border-radius: 4px; text-decoration: none;
    display: inline-flex; align-items: center; gap: 3px;
    white-space: nowrap;
  }
  .um-action:hover { background: rgba(13,86,129,0.08); color: var(--ssa, #0d5681); }
  .um-action-danger { color: #dc3545; }
  .um-action-danger:hover { background: rgba(220,53,69,0.08); color: #dc3545; }
  .um-action-success { color: #198754; }
  .um-action-success:hover { background: rgba(25,135,84,0.08); color: #198754; }
</style>

<div class="modal fade" id="userManagerModal" role="dialog" tabindex="-1" aria-labelledby="userManagerLabel" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background: linear-gradient(135deg, #0d5681, #0a4468); color: white;">
        <h6 class="modal-title m-0" id="userManagerLabel">
          <i class="bi bi-people-fill me-1"></i>User Manager
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">

        <%-- ═══ TABS ═══ --%>
        <ul class="nav nav-tabs nav-fill mb-3" role="tablist" style="font-size:0.82rem;">
          <li class="nav-item" role="presentation">
            <button class="nav-link active" id="um-tab-create" data-bs-toggle="tab" data-bs-target="#um-pane-create"
                    type="button" role="tab" aria-controls="um-pane-create" aria-selected="true">
              <i class="bi bi-person-plus me-1"></i>Create User
            </button>
          </li>
          <li class="nav-item" role="presentation">
            <button class="nav-link" id="um-tab-manage" data-bs-toggle="tab" data-bs-target="#um-pane-manage"
                    type="button" role="tab" aria-controls="um-pane-manage" aria-selected="false">
              <i class="bi bi-people me-1"></i>Manage Users
            </button>
          </li>
        </ul>

        <div class="tab-content">

          <%-- ═══ TAB 1: CREATE USER ═══ --%>
          <div class="tab-pane fade show active" id="um-pane-create" role="tabpanel" aria-labelledby="um-tab-create">

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

            <form method="post" action="CreateUser25" id="umCreateUserForm">
              <input type="hidden" name="creatorType" value="pspAdmin">

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

              <%-- Role checkboxes --%>
              <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em; margin-bottom:0.4rem;">
                Assign Roles
              </div>
              <div class="border rounded p-2 mb-2" style="background:#f8f9fa;">
                <c:forEach var="role" items="${applicationScope.global.assignableRoles}">
                  <div class="form-check">
                    <input type="checkbox" class="form-check-input"
                           name="roleIds" value="${role.id}" id="umRole_${role.id}">
                    <label for="umRole_${role.id}" class="form-check-label" style="font-size:0.85rem;">
                      ${role.description}
                    </label>
                  </div>
                </c:forEach>

                <hr class="my-2" style="border-color:#dee2e6;">
                <div class="form-check">
                  <input type="checkbox" class="form-check-input" id="umSalesCapability"
                         name="salesCapability" value="1" onchange="umOnSalesCapabilityChange()">
                  <label for="umSalesCapability" class="form-check-label" style="font-size:0.85rem;">
                    <i class="bi bi-briefcase me-1"></i>Will Require Sales Capability
                  </label>
                </div>
              </div>

              <%-- Agency section — shown when Sales Capability is checked --%>
              <div id="umAgencySection" style="display:none;">
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size:0.85rem;">Agency</label>
                  <select id="umAgencySelect" name="agencyId" class="form-select form-select-sm">
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
                  <input type="checkbox" id="umMakeManager" name="makeManager" value="1" class="form-check-input" onchange="umCheckManagerWarning()">
                  <label for="umMakeManager" class="form-check-label" style="font-size:0.85rem;">
                    <i class="bi bi-star me-1"></i>Designate as Agency Manager
                  </label>
                </div>

                <div id="umManagerWarning" class="alert alert-warning py-2 d-none" style="font-size:0.8rem;">
                  <i class="bi bi-exclamation-triangle-fill me-1"></i>
                  <strong>Warning:</strong> This will replace <span id="umCurrentManagerName"></span> as manager of the selected agency.
                </div>
              </div>

              <hr class="my-2">
              <div class="d-flex justify-content-end gap-2">
                <button type="submit" class="btn btn-ssa btn-sm">
                  <i class="bi bi-person-plus me-1"></i>Create User
                </button>
              </div>
            </form>
          </div>

          <%-- ═══ TAB 2: MANAGE USERS ═══ --%>
          <div class="tab-pane fade" id="um-pane-manage" role="tabpanel" aria-labelledby="um-tab-manage">
            <div id="umManageLoading" class="text-center py-3">
              <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
              <span class="ms-2 text-muted" style="font-size:0.82rem;">Loading users...</span>
            </div>
            <div id="umManageError" class="alert alert-danger py-2 mb-0" style="display:none; font-size:0.82rem;">
              <i class="bi bi-exclamation-triangle me-1"></i>Failed to load users.
            </div>
            <div id="umManageContent" style="display:none;">
              <div class="d-flex align-items-center mb-2">
                <input type="text" id="umUserSearch" class="form-control form-control-sm" placeholder="Search users..."
                       style="width:240px;" autocomplete="off">
              </div>
              <div style="max-height:400px; overflow-y:auto;">
                <table class="table table-sm table-hover mb-0" style="font-size:0.84rem;">
                  <thead>
                    <tr style="font-size:0.78rem;">
                      <th style="position:sticky; top:0; background:#f8f9fa; z-index:1;">Name</th>
                      <th style="position:sticky; top:0; background:#f8f9fa; z-index:1;">Email</th>
                      <th style="position:sticky; top:0; background:#f8f9fa; z-index:1;">Roles</th>
                      <th style="position:sticky; top:0; background:#f8f9fa; z-index:1;">Status</th>
                      <th style="position:sticky; top:0; background:#f8f9fa; z-index:1;">Actions</th>
                    </tr>
                  </thead>
                  <tbody id="umUserTableBody">
                  </tbody>
                </table>
              </div>
            </div>
          </div>

        </div><%-- /tab-content --%>
      </div><%-- /modal-body --%>
    </div>
  </div>
</div>

<%-- ═══ DEACTIVATE CONFIRMATION MODAL (stacked on top) ═══ --%>
<div class="modal fade" id="umDeactivateModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-md">
    <div class="modal-content">
      <div class="modal-header py-2" style="background:#dc3545; color:white;">
        <h6 class="modal-title m-0"><i class="bi bi-exclamation-triangle me-1"></i>Deactivate User</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body" style="font-size:0.85rem;">
        <div id="umDeacLoading" class="text-center py-3">
          <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
          <span class="ms-2 text-muted">Loading counts...</span>
        </div>
        <div id="umDeacContent" style="display:none;">
          <p>Deactivating <strong id="umDeacName"></strong> will reassign:</p>
          <ul class="mb-3">
            <li><span id="umDeacActivities" class="fw-bold">0</span> open activities</li>
            <li><span id="umDeacOpportunities" class="fw-bold">0</span> managed opportunities</li>
            <li><span id="umDeacTodos" class="fw-bold">0</span> delegated tasks</li>
            <li><span id="umDeacTasks" class="fw-bold">0</span> owned task templates</li>
          </ul>
          <div class="mb-2">
            <label class="form-label fw-semibold mb-1">Reassign all items to:</label>
            <select id="umDeacTarget" class="form-select form-select-sm"></select>
          </div>
        </div>
      </div>
      <div class="modal-footer py-2">
        <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Cancel</button>
        <button type="button" class="btn btn-sm btn-danger" id="umConfirmDeactivateBtn" disabled
                onclick="umDoDeactivate()">
          <i class="bi bi-person-dash me-1"></i>Deactivate
        </button>
      </div>
    </div>
  </div>
</div>

<%-- ═══ REMOVE AGENT ROLE MODAL (stacked on top) ═══ --%>
<div class="modal fade" id="umRemoveAgentModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-md">
    <div class="modal-content">
      <div class="modal-header py-2" style="background:#0d5681; color:white;">
        <h6 class="modal-title m-0"><i class="bi bi-briefcase me-1"></i>Remove Agent Role</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body" style="font-size:0.85rem;">
        <div id="umRaLoading" class="text-center py-3">
          <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
          <span class="ms-2 text-muted">Loading counts...</span>
        </div>
        <div id="umRaContent" style="display:none;">
          <p>Removing Agent role from <strong id="umRaName"></strong>.</p>
          <p id="umRaOppCount" style="display:none;">
            <span id="umRaOppNum" class="fw-bold">0</span> open managed opportunities will be reassigned.
          </p>
          <div class="mb-2" id="umRaTargetDiv">
            <label class="form-label fw-semibold mb-1">Reassign opportunities to:</label>
            <select id="umRaTarget" class="form-select form-select-sm"></select>
          </div>
        </div>
      </div>
      <div class="modal-footer py-2">
        <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Cancel</button>
        <button type="button" class="btn btn-sm btn-danger" id="umConfirmRemoveAgentBtn" disabled
                onclick="umDoRemoveAgent()">
          <i class="bi bi-briefcase me-1"></i>Remove Agent Role
        </button>
      </div>
    </div>
  </div>
</div>

<script>
// ══════════════════════════════════════════════════════════════════════════
// User Manager Modal — JavaScript
// ══════════════════════════════════════════════════════════════════════════

var umUserData = null;        // Cached user list from AJAX
var umCurrentPersonId = ${sessionScope.currentPerson.id};  // Current logged-in user
var umDeacPersonId = null;    // Person being deactivated
var umRaPersonId = null;      // Person losing Agent role

// ── Fix stacked modal z-index (Bootstrap 5) ──
document.addEventListener('show.bs.modal', function(e) {
  var openModals = document.querySelectorAll('.modal.show').length;
  if (openModals > 0) {
    var zIdx = 1055 + (10 * openModals);
    e.target.style.zIndex = zIdx;
    setTimeout(function() {
      var backdrops = document.querySelectorAll('.modal-backdrop');
      if (backdrops.length > 0) {
        backdrops[backdrops.length - 1].style.zIndex = zIdx - 1;
      }
    }, 10);
  }
});

// ── Load user data when modal opens ──
document.getElementById('userManagerModal').addEventListener('show.bs.modal', function() {
  umLoadUsers();
});

function umLoadUsers() {
  document.getElementById('umManageLoading').style.display = '';
  document.getElementById('umManageContent').style.display = 'none';
  document.getElementById('umManageError').style.display = 'none';

  fetch('UserManager')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      umUserData = data;
      umBuildUserTable(data);
      document.getElementById('umManageLoading').style.display = 'none';
      document.getElementById('umManageContent').style.display = '';
    })
    .catch(function() {
      document.getElementById('umManageLoading').style.display = 'none';
      document.getElementById('umManageError').style.display = '';
    });
}

// ── Build user table from JSON ──
function umBuildUserTable(data) {
  var tbody = document.getElementById('umUserTableBody');
  tbody.innerHTML = '';

  var users = data.users || [];
  for (var i = 0; i < users.length; i++) {
    var u = users[i];
    var tr = document.createElement('tr');
    tr.setAttribute('data-name', u.name.toLowerCase());
    if (!u.active) tr.className = 'table-secondary';

    // Name
    var tdName = document.createElement('td');
    tdName.textContent = u.name;
    tr.appendChild(tdName);

    // Email
    var tdEmail = document.createElement('td');
    tdEmail.style.fontSize = '0.82rem';
    tdEmail.textContent = u.email;
    tr.appendChild(tdEmail);

    // Roles
    var tdRoles = document.createElement('td');
    for (var j = 0; j < u.roles.length; j++) {
      var badge = document.createElement('span');
      badge.className = 'badge';
      badge.style.cssText = 'background:#6c757d; color:#fff; font-size:0.68rem; margin-right:2px;';
      badge.textContent = u.roles[j].desc;
      tdRoles.appendChild(badge);
    }
    tr.appendChild(tdRoles);

    // Status
    var tdStatus = document.createElement('td');
    var statusBadge = document.createElement('span');
    statusBadge.className = 'badge';
    if (u.active) {
      statusBadge.style.cssText = 'background:#198754; color:#fff; font-size:0.7rem;';
      statusBadge.textContent = 'Active';
    } else {
      statusBadge.style.cssText = 'background:#dc3545; color:#fff; font-size:0.7rem;';
      statusBadge.textContent = 'Inactive';
    }
    tdStatus.appendChild(statusBadge);
    tr.appendChild(tdStatus);

    // Actions
    var tdActions = document.createElement('td');
    tdActions.innerHTML = umBuildActions(u);
    tr.appendChild(tdActions);

    tbody.appendChild(tr);
  }
}

function umBuildActions(u) {
  var hasRole1 = false, hasRole2 = false;
  for (var j = 0; j < u.roles.length; j++) {
    if (u.roles[j].id === 1) hasRole1 = true;
    if (u.roles[j].id === 2) hasRole2 = true;
  }

  if (!u.active) {
    return '<button type="button" class="um-action um-action-success" onclick="umConfirmReactivate(' + u.personId + ',\'' + umEsc(u.name) + '\')">' +
           '<i class="bi bi-person-check"></i>Reactivate</button>';
  }

  var html = '';

  // Deactivate
  html += '<button type="button" class="um-action um-action-danger" onclick="umOpenDeactivateModal(' + u.personId + ',\'' + umEsc(u.name) + '\')">' +
          '<i class="bi bi-person-dash"></i>Deactivate</button>';

  // + Agent (only if no role 2)
  if (!hasRole2) {
    html += '<button type="button" class="um-action" onclick="umConfirmAddAgent(' + u.personId + ',\'' + umEsc(u.name) + '\')">' +
            '<i class="bi bi-briefcase-fill"></i>+ Agent</button>';
  }

  // - Agent (only if has role 2 AND is home agency agent)
  if (hasRole2 && u.homeAgent) {
    html += '<button type="button" class="um-action um-action-danger" onclick="umOpenRemoveAgentModal(' + u.personId + ',\'' + umEsc(u.name) + '\')">' +
            '<i class="bi bi-briefcase"></i>- Agent</button>';
  }

  // + PSP User (only if has role 2 but NOT role 1, home agency only)
  if (hasRole2 && !hasRole1 && u.homeAgent) {
    html += '<button type="button" class="um-action um-action-success" onclick="umConfirmAddPspUser(' + u.personId + ',\'' + umEsc(u.name) + '\')">' +
            '<i class="bi bi-person-badge"></i>+ PSP User</button>';
  }

  return html;
}

function umEsc(s) {
  return s.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

// ── User Search ──
document.getElementById('umUserSearch').addEventListener('input', function() {
  var term = this.value.trim().toLowerCase();
  var rows = document.querySelectorAll('#umUserTableBody tr');
  for (var i = 0; i < rows.length; i++) {
    var name = rows[i].getAttribute('data-name') || '';
    rows[i].style.display = name.indexOf(term) >= 0 ? '' : 'none';
  }
});

// ── Build active user dropdown for reassignment ──
function umBuildTargetDropdown(selectId, excludePersonId) {
  var sel = document.getElementById(selectId);
  sel.innerHTML = '';
  if (!umUserData) return;
  var users = umUserData.users || [];
  for (var i = 0; i < users.length; i++) {
    var u = users[i];
    if (!u.active) continue;
    if (u.personId === excludePersonId) continue;
    var opt = document.createElement('option');
    opt.value = u.personId;
    opt.textContent = u.name;
    sel.appendChild(opt);
  }
}

// ══════════════════════════════════════════════════════════════════════
// DEACTIVATE
// ══════════════════════════════════════════════════════════════════════

function umOpenDeactivateModal(personId, name) {
  umDeacPersonId = personId;
  document.getElementById('umDeacName').textContent = name;
  document.getElementById('umDeacLoading').style.display = '';
  document.getElementById('umDeacContent').style.display = 'none';
  document.getElementById('umConfirmDeactivateBtn').disabled = true;

  // Build reassignment dropdown (exclude target user and self)
  umBuildTargetDropdown('umDeacTarget', personId);

  new bootstrap.Modal(document.getElementById('umDeactivateModal')).show();

  fetch('UserManager?action=getCounts&personId=' + personId)
    .then(function(r) { return r.json(); })
    .then(function(data) {
      document.getElementById('umDeacActivities').textContent = data.openActivities;
      document.getElementById('umDeacOpportunities').textContent = data.managedOpportunities;
      document.getElementById('umDeacTodos').textContent = data.delegatedTodos;
      document.getElementById('umDeacTasks').textContent = data.ownedTasks;
      document.getElementById('umDeacLoading').style.display = 'none';
      document.getElementById('umDeacContent').style.display = '';
      document.getElementById('umConfirmDeactivateBtn').disabled = false;
    })
    .catch(function() {
      document.getElementById('umDeacLoading').innerHTML =
        '<span class="text-danger"><i class="bi bi-exclamation-triangle me-1"></i>Failed to load counts.</span>';
    });
}

function umDoDeactivate() {
  document.getElementById('umConfirmDeactivateBtn').disabled = true;
  var params = new URLSearchParams();
  params.append('action', 'deactivate');
  params.append('personId', umDeacPersonId);
  params.append('targetPersonId', document.getElementById('umDeacTarget').value);
  fetch('UserManager', { method: 'POST', body: params })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.success) {
        location.reload();
      } else {
        alert(data.error || 'Failed to deactivate user.');
        document.getElementById('umConfirmDeactivateBtn').disabled = false;
      }
    })
    .catch(function() {
      alert('Request failed.');
      document.getElementById('umConfirmDeactivateBtn').disabled = false;
    });
}

// ══════════════════════════════════════════════════════════════════════
// REACTIVATE
// ══════════════════════════════════════════════════════════════════════

function umConfirmReactivate(personId, name) {
  if (!confirm('Reactivate ' + name + '?')) return;
  var params = new URLSearchParams();
  params.append('action', 'reactivate');
  params.append('personId', personId);
  fetch('UserManager', { method: 'POST', body: params })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.success) location.reload();
      else alert(data.error || 'Failed to reactivate user.');
    })
    .catch(function() { alert('Request failed.'); });
}

// ══════════════════════════════════════════════════════════════════════
// ADD AGENT ROLE
// ══════════════════════════════════════════════════════════════════════

function umConfirmAddAgent(personId, name) {
  if (!confirm('Add Agent role to ' + name + '?\n\nThey will be added to the PSP home agency.')) return;
  var params = new URLSearchParams();
  params.append('action', 'addAgentRole');
  params.append('personId', personId);
  fetch('UserManager', { method: 'POST', body: params })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.success) location.reload();
      else alert(data.error || 'Failed to add Agent role.');
    })
    .catch(function() { alert('Request failed.'); });
}

// ══════════════════════════════════════════════════════════════════════
// REMOVE AGENT ROLE
// ══════════════════════════════════════════════════════════════════════

function umOpenRemoveAgentModal(personId, name) {
  umRaPersonId = personId;
  document.getElementById('umRaName').textContent = name;
  document.getElementById('umRaLoading').style.display = '';
  document.getElementById('umRaContent').style.display = 'none';
  document.getElementById('umConfirmRemoveAgentBtn').disabled = true;

  // Build reassignment dropdown
  umBuildTargetDropdown('umRaTarget', personId);

  new bootstrap.Modal(document.getElementById('umRemoveAgentModal')).show();

  fetch('UserManager?action=getCounts&personId=' + personId)
    .then(function(r) { return r.json(); })
    .then(function(data) {
      var oppCount = data.managedOpportunities;
      document.getElementById('umRaOppNum').textContent = oppCount;
      document.getElementById('umRaOppCount').style.display = oppCount > 0 ? '' : 'none';
      document.getElementById('umRaTargetDiv').style.display = oppCount > 0 ? '' : 'none';
      document.getElementById('umRaLoading').style.display = 'none';
      document.getElementById('umRaContent').style.display = '';
      document.getElementById('umConfirmRemoveAgentBtn').disabled = false;
    })
    .catch(function() {
      document.getElementById('umRaLoading').innerHTML =
        '<span class="text-danger"><i class="bi bi-exclamation-triangle me-1"></i>Failed to load counts.</span>';
    });
}

function umDoRemoveAgent() {
  document.getElementById('umConfirmRemoveAgentBtn').disabled = true;
  var params = new URLSearchParams();
  params.append('action', 'removeAgentRole');
  params.append('personId', umRaPersonId);
  params.append('targetPersonId', document.getElementById('umRaTarget').value);
  fetch('UserManager', { method: 'POST', body: params })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.success) {
        location.reload();
      } else {
        alert(data.error || 'Failed to remove Agent role.');
        document.getElementById('umConfirmRemoveAgentBtn').disabled = false;
      }
    })
    .catch(function() {
      alert('Request failed.');
      document.getElementById('umConfirmRemoveAgentBtn').disabled = false;
    });
}

// ══════════════════════════════════════════════════════════════════════
// ADD PSP USER ROLE
// ══════════════════════════════════════════════════════════════════════

function umConfirmAddPspUser(personId, name) {
  if (!confirm('Add PSP User role to ' + name + '?')) return;
  var params = new URLSearchParams();
  params.append('action', 'addPspUserRole');
  params.append('personId', personId);
  fetch('UserManager', { method: 'POST', body: params })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.success) location.reload();
      else alert(data.error || 'Failed to add PSP User role.');
    })
    .catch(function() { alert('Request failed.'); });
}

// ══════════════════════════════════════════════════════════════════════
// CREATE USER FORM HELPERS
// ══════════════════════════════════════════════════════════════════════

function umOnSalesCapabilityChange() {
  var salesCb = document.getElementById('umSalesCapability');
  var agencySection = document.getElementById('umAgencySection');
  if (!salesCb || !agencySection) return;

  if (salesCb.checked) {
    agencySection.style.display = '';
  } else {
    agencySection.style.display = 'none';
    var agencySelect = document.getElementById('umAgencySelect');
    if (agencySelect) agencySelect.value = '';
    var mgr = document.getElementById('umMakeManager');
    if (mgr) mgr.checked = false;
    var warn = document.getElementById('umManagerWarning');
    if (warn) warn.classList.add('d-none');
  }
}

function umCheckManagerWarning() {
  var managerCb = document.getElementById('umMakeManager');
  var warn = document.getElementById('umManagerWarning');
  var agencySelect = document.getElementById('umAgencySelect');
  if (!managerCb || !warn || !agencySelect) return;

  if (managerCb.checked && agencySelect.value) {
    var selected = agencySelect.options[agencySelect.selectedIndex];
    var currentManagerName = selected.dataset.managerName || '';
    var currentManagerId = selected.dataset.managerId || '0';

    if (currentManagerId !== '0' && currentManagerName.trim() !== '') {
      document.getElementById('umCurrentManagerName').textContent = currentManagerName;
      warn.classList.remove('d-none');
    } else {
      warn.classList.add('d-none');
    }
  } else {
    warn.classList.add('d-none');
  }
}

var umAgencySel = document.getElementById('umAgencySelect');
if (umAgencySel) {
  umAgencySel.addEventListener('change', function() {
    umCheckManagerWarning();
  });
}

// ── Auto-open modal if there's a create user result ──
<c:if test="${not empty requestScope.autoOpenUserManager}">
document.addEventListener('DOMContentLoaded', function() {
  new bootstrap.Modal(document.getElementById('userManagerModal')).show();
});
</c:if>
</script>

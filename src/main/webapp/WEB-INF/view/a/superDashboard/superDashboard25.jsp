<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"/>
  <title>Super Dashboard</title>
  <style>
    .install-card {
      border: 1px solid #dee2e6;
      border-radius: 10px;
      padding: 1.25rem;
      background: #fff;
      transition: box-shadow 0.15s;
    }
    .install-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
    .status-dot {
      display: inline-block;
      width: 10px; height: 10px;
      border-radius: 50%;
      margin-right: 6px;
    }
    .status-green  { background: #198754; }
    .status-yellow { background: #ffc107; }
    .status-red    { background: #dc3545; }
    .status-gray   { background: #6c757d; }
    .type-badge {
      font-size: 0.65rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      padding: 2px 8px;
      border-radius: 4px;
    }
    .type-psp { background: rgba(13,86,129,0.1); color: #0d5681; }
    .type-bpo { background: rgba(135,169,72,0.15); color: #5c7a20; }
    .meta-label {
      font-size: 0.7rem;
      color: #6c757d;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      font-weight: 600;
    }
    .meta-value {
      font-size: 0.88rem;
      font-weight: 500;
    }
    .alert-panel {
      background: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 8px;
      padding: 0.75rem 1rem;
      font-size: 0.82rem;
    }
    .alert-panel .alert-item {
      padding: 2px 0;
    }
    .ghost-action {
      background: none; border: none; color: var(--ssa, #0d5681);
      font-size: 0.82rem; padding: 4px 10px; cursor: pointer;
      border-radius: 4px; text-decoration: none;
      display: inline-flex; align-items: center; gap: 4px;
      line-height: 1.4; vertical-align: middle;
    }
    .ghost-action:hover { background: rgba(13,86,129,0.08); color: var(--ssa, #0d5681); text-decoration: none; }
    .card-actions {
      border-top: 1px solid #dee2e6;
      padding-top: 0.75rem;
      margin-top: 0.25rem;
      display: flex; align-items: center; gap: 4px; flex-wrap: wrap;
    }
    .card-actions form,
    .toolbar-actions form { display: inline-flex; align-items: center; margin: 0; padding: 0; }
  </style>
</head>
<body>
<div class="audit-wrap">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  <%-- Toolbar --%>
  <div class="d-flex align-items-center justify-content-between px-3 py-2" style="background: #f8f9fa; border-bottom: 1px solid #dee2e6;">
    <div class="d-flex align-items-center gap-2">
      <span class="text-muted" style="font-size: 0.8rem;">
        <strong>${installations.size()}</strong> installation<c:if test="${installations.size() != 1}">s</c:if>
      </span>
      <c:if test="${not empty masterSchema}">
        <span class="text-muted" style="font-size: 0.75rem;">|</span>
        <span class="text-muted" style="font-size: 0.75rem;">Master: <code>${masterSchema}</code></span>
      </c:if>
      <c:if test="${not empty lastAutoRefresh}">
        <span class="text-muted" style="font-size: 0.75rem;">|</span>
        <span class="text-muted" style="font-size: 0.72rem;" title="Auto-refresh: ${lastAutoRefreshSummary}">
          <i class="bi bi-clock-history me-1"></i>Auto: ${lastAutoRefresh}
        </span>
      </c:if>
    </div>
    <div class="d-flex align-items-center gap-2 toolbar-actions">
      <c:if test="${installations.size() > 0}">
        <form method="post" action="SuperDashboard">
          <input type="hidden" name="action" value="refreshAll">
          <button type="submit" class="ghost-action">
            <i class="bi bi-arrow-repeat me-1"></i>Refresh All
          </button>
        </form>
      </c:if>
      <button class="ghost-action" type="button" data-bs-toggle="modal" data-bs-target="#addInstallationModal">
        <i class="bi bi-plus-lg me-1"></i>Add Installation
      </button>
    </div>
  </div>

  <%-- Success/Error Messages --%>
  <c:if test="${not empty param.success}">
    <div class="alert alert-success alert-dismissible fade show mx-3 mt-2 mb-0" role="alert" style="font-size:0.85rem;">
      <i class="bi bi-check-circle me-1"></i>${param.success}
      <button type="button" class="btn-close" data-bs-dismiss="alert" style="font-size:0.65rem;"></button>
    </div>
  </c:if>
  <c:if test="${not empty param.error}">
    <div class="alert alert-danger alert-dismissible fade show mx-3 mt-2 mb-0" role="alert" style="font-size:0.85rem;">
      <i class="bi bi-exclamation-triangle me-1"></i>${param.error}
      <button type="button" class="btn-close" data-bs-dismiss="alert" style="font-size:0.65rem;"></button>
    </div>
  </c:if>

  <%-- Alert Banner --%>
  <c:if test="${alertCount > 0}">
    <div class="mx-3 mt-2">
      <div class="alert-panel">
        <div class="d-flex align-items-center mb-1">
          <i class="bi bi-exclamation-triangle-fill text-warning me-2"></i>
          <strong>${alertCount} attention item<c:if test="${alertCount != 1}">s</c:if></strong>
        </div>
        <c:forEach var="a" items="${alerts}">
          <div class="alert-item text-muted"><i class="bi bi-dot"></i>${a}</div>
        </c:forEach>
      </div>
    </div>
  </c:if>

  <%-- Installation Cards --%>
  <div class="flex-grow-1 overflow-auto p-3">
    <c:choose>
      <c:when test="${empty installations}">
        <div class="text-center text-muted py-5">
          <i class="bi bi-hdd-network" style="font-size: 3rem; opacity: 0.3;"></i>
          <p class="mt-3">No installations registered yet.</p>
          <button class="ghost-action" data-bs-toggle="modal" data-bs-target="#addInstallationModal">
            <i class="bi bi-plus-lg me-1"></i>Add Your First Installation
          </button>
        </div>
      </c:when>
      <c:otherwise>
        <div class="row g-3">
          <c:forEach var="inst" items="${installations}">
            <div class="col-12 col-md-6 col-xl-4">
              <div class="install-card">
                <%-- Header: name + type badge --%>
                <div class="d-flex align-items-center justify-content-between mb-2">
                  <div class="d-flex align-items-center">
                    <c:choose>
                      <c:when test="${inst.status == 'ACTIVE'}">
                        <c:choose>
                          <c:when test="${not empty inst.lastHeartbeat}">
                            <span class="status-dot status-green" title="Active"></span>
                          </c:when>
                          <c:otherwise>
                            <span class="status-dot status-yellow" title="Active - never checked"></span>
                          </c:otherwise>
                        </c:choose>
                      </c:when>
                      <c:when test="${inst.status == 'PENDING'}">
                        <span class="status-dot status-yellow" title="Pending registration"></span>
                      </c:when>
                      <c:otherwise>
                        <span class="status-dot status-gray" title="${inst.status}"></span>
                      </c:otherwise>
                    </c:choose>
                    <strong style="font-size: 1rem;">${inst.installationName}</strong>
                  </div>
                  <span class="type-badge type-${inst.systemType == 'BPO' ? 'bpo' : 'psp'}">${inst.systemType}</span>
                </div>

                <%-- URL --%>
                <div class="mb-2">
                  <a href="${inst.installationUrl}" target="_blank" class="text-muted" style="font-size: 0.78rem; word-break: break-all;">
                    ${inst.installationUrl} <i class="bi bi-box-arrow-up-right" style="font-size: 0.65rem;"></i>
                  </a>
                </div>

                <%-- Metrics --%>
                <div class="row g-2 mb-3">
                  <div class="col-3">
                    <div class="meta-label">Schema</div>
                    <div class="meta-value">${not empty inst.lastSchemaVersion ? inst.lastSchemaVersion : '—'}</div>
                  </div>
                  <div class="col-3">
                    <div class="meta-label">Users</div>
                    <div class="meta-value">${not empty inst.lastUserCount ? inst.lastUserCount : '—'}</div>
                  </div>
                  <div class="col-3">
                    <div class="meta-label">WAR</div>
                    <div class="meta-value" style="font-size:0.78rem;">${not empty inst.lastAppVersion ? inst.lastAppVersion : '—'}</div>
                  </div>
                  <div class="col-3">
                    <div class="meta-label">Status</div>
                    <div class="meta-value">
                      <c:choose>
                        <c:when test="${inst.status == 'ACTIVE'}"><span class="text-success">${inst.status}</span></c:when>
                        <c:when test="${inst.status == 'PENDING'}"><span class="text-warning">${inst.status}</span></c:when>
                        <c:otherwise><span class="text-muted">${inst.status}</span></c:otherwise>
                      </c:choose>
                    </div>
                  </div>
                </div>

                <%-- Last Seen --%>
                <c:if test="${not empty inst.lastHeartbeatFormatted}">
                  <div class="text-muted mb-2" style="font-size: 0.72rem;">
                    <i class="bi bi-clock me-1"></i>Last seen: ${inst.lastHeartbeatFormatted}
                  </div>
                </c:if>

                <%-- Actions --%>
                <div class="card-actions">
                  <c:if test="${inst.status == 'ACTIVE'}">
                    <form method="post" action="SuperDashboard">
                      <input type="hidden" name="action" value="refresh">
                      <input type="hidden" name="id" value="${inst.id}">
                      <button type="submit" class="ghost-action"><i class="bi bi-arrow-clockwise me-1"></i>Refresh</button>
                    </form>
                    <a href="ManageInstallation?id=${inst.id}" class="ghost-action"><i class="bi bi-gear me-1"></i>Details</a>
                    <form method="post" action="SuperDashboard"
                          onsubmit="return confirm('Disconnect this installation?');">
                      <input type="hidden" name="action" value="disconnect">
                      <input type="hidden" name="id" value="${inst.id}">
                      <button type="submit" class="ghost-action" style="color:#dc3545;"><i class="bi bi-x-circle me-1"></i>Disconnect</button>
                    </form>
                  </c:if>
                  <c:if test="${inst.status == 'PENDING'}">
                    <button type="button" class="ghost-action" style="color:#0d5681; font-weight:600;"
                            data-bs-toggle="modal" data-bs-target="#registerModal${inst.id}">
                      <i class="bi bi-link-45deg me-1"></i>Register
                    </button>
                  </c:if>
                </div>
              </div>
            </div>

            <%-- Register Modal (per installation) --%>
            <c:if test="${inst.status == 'PENDING'}">
            <div class="modal fade" id="registerModal${inst.id}" tabindex="-1">
              <div class="modal-dialog">
                <div class="modal-content">
                  <form method="post" action="SuperDashboard">
                    <input type="hidden" name="action" value="register">
                    <input type="hidden" name="id" value="${inst.id}">
                    <div class="modal-header">
                      <h6 class="modal-title">Register: ${inst.installationName}</h6>
                      <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                      <p class="text-muted" style="font-size:0.85rem;">
                        Enter the deployment key configured in this installation's <code>ssa.properties</code>.
                        This will establish a secure token exchange.
                      </p>
                      <div class="mb-3">
                        <label class="form-label" style="font-size:0.82rem;">Deployment Key</label>
                        <input type="password" name="deploymentKey" class="form-control form-control-sm" required
                               placeholder="From ssa.properties DEPLOYMENT_KEY">
                      </div>
                    </div>
                    <div class="modal-footer">
                      <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                      <button type="submit" class="btn btn-sm btn-primary"><i class="bi bi-link-45deg me-1"></i>Register</button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
            </c:if>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%-- Add Installation Modal --%>
<div class="modal fade" id="addInstallationModal" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <form method="post" action="SuperDashboard">
        <input type="hidden" name="action" value="add">
        <div class="modal-header">
          <h6 class="modal-title"><i class="bi bi-plus-lg me-2"></i>Add Installation</h6>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label" style="font-size:0.82rem;">Installation Name</label>
            <input type="text" name="installationName" class="form-control form-control-sm" required
                   placeholder="e.g., Acme Benefits">
          </div>
          <div class="mb-3">
            <label class="form-label" style="font-size:0.82rem;">Installation URL</label>
            <input type="url" name="installationUrl" class="form-control form-control-sm" required
                   placeholder="https://acme.superiorstate.biz">
          </div>
          <div class="mb-3">
            <label class="form-label" style="font-size:0.82rem;">System Type</label>
            <select name="systemType" class="form-select form-select-sm">
              <option value="PSP" selected>PSP</option>
              <option value="BPO">BPO</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="submit" class="btn btn-sm btn-primary"><i class="bi bi-plus-lg me-1"></i>Add</button>
        </div>
      </form>
    </div>
  </div>
</div>

</body>
</html>

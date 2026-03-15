<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"/>
  <title>${installation.installationName} — Detail</title>
  <style>
    .ghost-action {
      background: none; border: none; color: var(--ssa, #0d5681);
      font-size: 0.82rem; padding: 4px 10px; cursor: pointer;
      border-radius: 4px; text-decoration: none;
      display: inline-flex; align-items: center; gap: 4px;
      line-height: 1.4;
    }
    .ghost-action:hover { background: rgba(13,86,129,0.08); color: var(--ssa, #0d5681); text-decoration: none; }
  </style>
</head>
<body>
<div class="audit-wrap">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  <%-- Toolbar --%>
  <div class="d-flex align-items-center justify-content-between px-3 py-2" style="background: #f8f9fa; border-bottom: 1px solid #dee2e6;">
    <div class="d-flex align-items-center gap-2">
      <a href="SuperDashboard" class="ghost-action"><i class="bi bi-arrow-left me-1"></i>Back</a>
      <span class="text-muted">|</span>
      <span style="font-size:0.85rem;">
        <strong>${installation.installationName}</strong>
        <span class="badge ${installation.systemType == 'BPO' ? 'bg-success' : 'bg-primary'} ms-1" style="font-size:0.6rem;">${installation.systemType}</span>
        <c:choose>
          <c:when test="${installation.status == 'ACTIVE'}"><span class="badge bg-success ms-1" style="font-size:0.6rem;">ACTIVE</span></c:when>
          <c:when test="${installation.status == 'PENDING'}"><span class="badge bg-warning text-dark ms-1" style="font-size:0.6rem;">PENDING</span></c:when>
          <c:otherwise><span class="badge bg-secondary ms-1" style="font-size:0.6rem;">${installation.status}</span></c:otherwise>
        </c:choose>
      </span>
    </div>
    <div>
      <a href="${installation.installationUrl}" target="_blank" class="ghost-action" style="font-size:0.78rem;">
        ${installation.installationUrl} <i class="bi bi-box-arrow-up-right ms-1"></i>
      </a>
    </div>
  </div>

  <%-- Alerts --%>
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

  <div class="flex-grow-1 overflow-auto p-3">
    <div class="row g-3">

      <%-- Health Panel --%>
      <div class="col-12 col-lg-5">
        <div class="card">
          <div class="card-header d-flex align-items-center justify-content-between" style="font-size:0.85rem; font-weight:600;">
            <span><i class="bi bi-heart-pulse me-2"></i>Health</span>
            <c:if test="${not empty healthError}">
              <span class="badge bg-danger" style="font-size:0.65rem;">Error: ${healthError}</span>
            </c:if>
          </div>
          <div class="card-body" style="font-size:0.85rem;">
            <c:choose>
              <c:when test="${not empty health}">
                <table class="table table-sm mb-0" style="font-size:0.82rem;">
                  <tr><td class="text-muted" style="width:40%;">System Type</td><td><strong>${health.systemType}</strong></td></tr>
                  <c:if test="${not empty health.pspName}">
                    <tr><td class="text-muted">PSP Name</td><td>${health.pspName}</td></tr>
                  </c:if>
                  <tr><td class="text-muted">Schema Version</td><td><code>${health.schemaVersion}</code></td></tr>
                  <tr><td class="text-muted">App Version</td><td><code>${health.appVersion}</code></td></tr>
                  <c:if test="${not empty health.buildTimestamp}">
                    <tr><td class="text-muted">Built</td><td>${health.buildTimestamp}</td></tr>
                  </c:if>
                  <tr><td class="text-muted">Active Users</td><td><strong>${health.totalActiveUsers}</strong></td></tr>
                  <c:if test="${not empty health.uptimeDays}">
                    <tr><td class="text-muted">Uptime</td><td>${health.uptimeDays} days</td></tr>
                  </c:if>
                  <c:if test="${not empty health.javaVersion}">
                    <tr><td class="text-muted">Java</td><td>${health.javaVersion}</td></tr>
                  </c:if>
                </table>

                <%-- Users by Role --%>
                <c:if test="${not empty health.usersByRole}">
                  <h6 class="mt-3 mb-2" style="font-size:0.78rem; text-transform:uppercase; color:#6c757d; letter-spacing:0.03em;">Users by Role</h6>
                  <table class="table table-sm mb-0" style="font-size:0.82rem;">
                    <c:forEach var="entry" items="${health.usersByRole}">
                      <tr><td class="text-muted">${entry.key}</td><td><strong>${entry.value}</strong></td></tr>
                    </c:forEach>
                  </table>
                </c:if>
              </c:when>
              <c:when test="${not empty healthError}">
                <p class="text-danger mb-0"><i class="bi bi-exclamation-triangle me-1"></i>${healthError}</p>
              </c:when>
              <c:otherwise>
                <p class="text-muted mb-0">Not connected — register to enable health checks.</p>
              </c:otherwise>
            </c:choose>
          </div>
        </div>

        <%-- Push Constant Form --%>
        <c:if test="${installation.connected}">
        <div class="card mt-3">
          <div class="card-header" style="font-size:0.85rem; font-weight:600;">
            <i class="bi bi-send me-2"></i>Push Constant
          </div>
          <div class="card-body">
            <form method="post" action="ManageInstallation">
              <input type="hidden" name="action" value="pushConstant">
              <input type="hidden" name="id" value="${installation.id}">
              <div class="mb-2">
                <label class="form-label" style="font-size:0.78rem;">Name</label>
                <input type="text" name="constName" class="form-control form-control-sm" required placeholder="e.g., CHATBOT_ALL_USERS">
              </div>
              <div class="mb-2">
                <label class="form-label" style="font-size:0.78rem;">Value</label>
                <input type="text" name="constValue" class="form-control form-control-sm" placeholder="e.g., true">
              </div>
              <button type="submit" class="btn btn-primary btn-sm"><i class="bi bi-send me-1"></i>Push</button>
            </form>
          </div>
        </div>
        </c:if>
      </div>

      <%-- Constants Table --%>
      <div class="col-12 col-lg-7">
        <div class="card">
          <div class="card-header d-flex align-items-center justify-content-between" style="font-size:0.85rem; font-weight:600;">
            <span><i class="bi bi-sliders me-2"></i>Constants</span>
            <c:if test="${not empty remoteConstants}">
              <span class="badge bg-secondary" style="font-size:0.65rem;">${remoteConstants.size()}</span>
            </c:if>
          </div>
          <div class="card-body p-0">
            <c:choose>
              <c:when test="${not empty remoteConstants}">
                <div class="table-responsive" style="max-height: calc(100vh - 220px); overflow-y: auto;">
                  <table class="table table-sm table-hover mb-0" style="font-size:0.8rem;">
                    <thead style="position:sticky; top:0; background:#f8f9fa; z-index:1;">
                      <tr>
                        <th style="width:35%;">Name</th>
                        <th style="width:40%;">Value</th>
                        <th style="width:25%;">Note</th>
                      </tr>
                    </thead>
                    <tbody>
                      <c:forEach var="c" items="${remoteConstants}">
                        <tr>
                          <td><code style="font-size:0.78rem;">${c.name}</code></td>
                          <td style="word-break:break-all;">${c.value}</td>
                          <td class="text-muted">${c.note}</td>
                        </tr>
                      </c:forEach>
                    </tbody>
                  </table>
                </div>
              </c:when>
              <c:when test="${not empty constantsError}">
                <div class="p-3"><p class="text-danger mb-0"><i class="bi bi-exclamation-triangle me-1"></i>${constantsError}</p></div>
              </c:when>
              <c:otherwise>
                <div class="p-3"><p class="text-muted mb-0">Not connected — register to view constants.</p></div>
              </c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>

    </div>
  </div>
</div>
</body>
</html>

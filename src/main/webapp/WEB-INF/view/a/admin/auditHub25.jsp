<%--
  T237 -- audit framework hub. Lists every registered check with its latest stored result.
  Nothing here evaluates a check on render -- every value comes from AuditHub's request
  attributes, themselves read from AuditService's in-memory map (last-run results only).
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Audit Hub</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .audit-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
        }
        .toolbar {
            padding: 0.65rem 1rem;
            background: #fff; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: center; gap: 0.75rem;
        }
        .toolbar .t-title {
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; margin: 0;
        }
        .ah-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .status-card dl { display: grid; grid-template-columns: 180px 1fr; row-gap: 0.4rem; margin: 0; }
        .status-card dt { font-weight: 600; color: #495057; }
        .status-card dd { margin: 0; }
        .badge-on { background: #198754; color: #fff; }
        .badge-off { background: #adb5bd; color: #fff; }
        .badge-running { background: #fd7e14; color: #fff; }
        .badge-ok { background: #198754; color: #fff; }
        .badge-action { background: #dc3545; color: #fff; }
        .badge-error { background: #ffc107; color: #212529; }
        .badge-not-configured { background: #adb5bd; color: #fff; }
        .badge-never { background: #adb5bd; color: #fff; }
        .check-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .check-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .check-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: top; }
        .check-table tbody tr:hover { background: #f8f9fa; }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-bell me-1"></i>Audit Hub
        </h1>
    </div>

    <div class="ah-body">

        <c:if test="${not empty sessionScope.auditMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i>${sessionScope.auditMessage}
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="auditMessage" scope="session"/>
        </c:if>

        <div class="status-card">
            <dl>
                <dt>Scheduler</dt>
                <dd>
                    <c:choose>
                        <c:when test="${schedulerEnabled}"><span class="badge badge-on">Enabled — daily</span></c:when>
                        <c:otherwise><span class="badge badge-off">Disabled</span></c:otherwise>
                    </c:choose>
                </dd>
                <dt>Run status</dt>
                <dd>
                    <c:choose>
                        <c:when test="${runInProgress}"><span class="badge badge-running">Running…</span></c:when>
                        <c:otherwise><span class="text-muted">Idle</span></c:otherwise>
                    </c:choose>
                </dd>
            </dl>
            <form method="POST" action="AuditHub" class="mt-3">
                <input type="hidden" name="action" value="runnow">
                <button type="submit" class="btn btn-sm btn-outline-primary" ${runInProgress ? 'disabled' : ''}>
                    <i class="bi bi-play-fill me-1"></i>Run now
                </button>
                <a href="AuditHub" class="btn btn-sm btn-outline-secondary ms-1">
                    <i class="bi bi-arrow-clockwise me-1"></i>Refresh
                </a>
            </form>
        </div>

        <c:choose>
            <c:when test="${empty checkRows}">
                <div class="empty-state">
                    <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                    No checks are registered on this installation.
                </div>
            </c:when>
            <c:otherwise>
                <div style="overflow-x:auto;">
                    <table class="check-table">
                        <thead>
                        <tr>
                            <th>Check</th>
                            <th>Last run</th>
                            <th>Status</th>
                            <th>Count</th>
                            <th>Summary / error</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="row" items="${checkRows}">
                            <tr>
                                <td>${row.check.label()}</td>
                                <td>
                                    ${row.lastRunDisplay}
                                    <c:if test="${row.everRun}">
                                        <span class="text-muted">(${row.latestRun.runTrigger})</span>
                                    </c:if>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${!row.everRun}"><span class="badge badge-never">Never run</span></c:when>
                                        <c:when test="${row.latestRun.status == 'OK'}"><span class="badge badge-ok">OK</span></c:when>
                                        <c:when test="${row.latestRun.status == 'ACTION'}"><span class="badge badge-action">Action</span></c:when>
                                        <c:when test="${row.latestRun.status == 'ERROR'}"><span class="badge badge-error">Error</span></c:when>
                                        <c:when test="${row.latestRun.status == 'NOT_CONFIGURED'}"><span class="badge badge-not-configured">Not configured</span></c:when>
                                        <c:otherwise><span class="badge badge-not-configured">${row.latestRun.status}</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${row.everRun ? row.latestRun.findingCount : '—'}</td>
                                <td>
                                    <c:if test="${row.everRun && not empty row.latestRun.summary}">${row.latestRun.summary}</c:if>
                                    <c:if test="${row.everRun && not empty row.latestRun.error}"><span class="text-danger">${row.latestRun.error}</span></c:if>
                                </td>
                                <td>
                                    <a href="${pageContext.request.contextPath}${row.check.detailPath()}" class="btn btn-sm btn-outline-primary">Details</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>

    </div>
</div>

</body>
</html>

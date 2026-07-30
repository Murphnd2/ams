<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Rate Cache</title>
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
        .rc-body {
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
        .county-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .county-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .county-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; }
        .county-table tbody tr:hover { background: #f8f9fa; }
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
        <h1 class="t-title m-0"><i class="bi bi-graph-up me-1"></i>Rate Cache — Plan Year ${planYear}</h1>
    </div>

    <div class="rc-body">

        <c:if test="${not empty sessionScope.rateCacheMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i>${sessionScope.rateCacheMessage}
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="rateCacheMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.rateCacheError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i>${sessionScope.rateCacheError}
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="rateCacheError" scope="session"/>
        </c:if>

        <div class="status-card">
            <dl>
                <dt>Warming enabled</dt>
                <dd>
                    <c:choose>
                        <c:when test="${warmEnabled}">
                            <span class="badge badge-on"><i class="bi bi-check-lg"></i> Enabled</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge badge-off"><i class="bi bi-dash-lg"></i> Disabled</span>
                            <span class="text-muted ms-2" style="font-size:0.78rem;">
                                RATE_CACHE_WARM_ENABLED = <c:out value="${empty warmEnabledConstant ? '(not set)' : warmEnabledConstant}"/>
                            </span>
                        </c:otherwise>
                    </c:choose>
                </dd>

                <dt>Source environment</dt>
                <dd><c:out value="${sourceEnv}"/></dd>

                <dt>Run status</dt>
                <dd>
                    <c:choose>
                        <c:when test="${runInProgress}">
                            <span class="badge badge-running"><i class="bi bi-arrow-repeat"></i> Running</span>
                        </c:when>
                        <c:otherwise>
                            <span class="text-muted">Idle</span>
                        </c:otherwise>
                    </c:choose>
                </dd>

                <dt>Last run</dt>
                <dd>
                    <c:choose>
                        <c:when test="${not empty lastRunAt}">
                            <fmt:formatDate value="${lastRunAt}" pattern="yyyy-MM-dd HH:mm"/> —
                            <c:out value="${lastRunSummary}"/>
                        </c:when>
                        <c:otherwise><span class="text-muted">Never run this instance-lifetime.</span></c:otherwise>
                    </c:choose>
                </dd>
            </dl>

            <form method="post" action="RateCacheAdmin" class="mt-3">
                <input type="hidden" name="action" value="refresh">
                <button type="submit" class="ssa-action save" ${!warmEnabled ? 'disabled' : ''}>
                    <i class="bi bi-arrow-clockwise me-1"></i>Manual Refresh
                </button>
            </form>
        </div>

        <div class="card">
            <div class="hdr-bar d-flex align-items-center">
                <i class="bi bi-map me-2"></i>Configured Counties
            </div>
            <c:choose>
                <c:when test="${not empty countySummaries}">
                    <table class="county-table">
                        <thead>
                        <tr>
                            <th>County FIPS</th>
                            <th>Row Count</th>
                            <th>Oldest Fetched</th>
                            <th>Newest Fetched</th>
                            <th>Source</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="cs" items="${countySummaries}">
                            <tr>
                                <td><c:out value="${cs.countyFips}"/></td>
                                <td>${cs.rowCount}</td>
                                <td>
                                    <c:if test="${not empty cs.oldestFetchedAt}">
                                        <fmt:formatDate value="${cs.oldestFetchedAt}" pattern="yyyy-MM-dd HH:mm"/>
                                    </c:if>
                                </td>
                                <td>
                                    <c:if test="${not empty cs.newestFetchedAt}">
                                        <fmt:formatDate value="${cs.newestFetchedAt}" pattern="yyyy-MM-dd HH:mm"/>
                                    </c:if>
                                </td>
                                <td><c:out value="${cs.sourceEnv}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <i class="bi bi-map"></i>
                        <div style="font-size:0.85rem; margin-top:0.5rem;">No cached counties for plan year ${planYear} yet.</div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</div>
</body>
</html>

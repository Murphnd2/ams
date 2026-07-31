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
        <h1 class="t-title m-0">
            <i class="bi bi-graph-up me-1"></i>Rate Cache
            <c:if test="${not empty configuredPlanYears}"> — Plan Years:
                <c:forEach var="y" items="${configuredPlanYears}" varStatus="ys">${y}<c:if test="${!ys.last}">, </c:if></c:forEach>
            </c:if>
        </h1>
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
                <dd>
                    <c:choose>
                        <c:when test="${empty sourceEnv}">
                            <span class="text-muted">Not configured</span>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${sourceEnv}"/>
                        </c:otherwise>
                    </c:choose>
                </dd>

                <dt>Configured plan years</dt>
                <dd>
                    <c:choose>
                        <c:when test="${not empty configuredPlanYears}">
                            <c:forEach var="y" items="${configuredPlanYears}" varStatus="ys">${y}<c:if test="${!ys.last}">, </c:if></c:forEach>
                        </c:when>
                        <c:otherwise>
                            <span class="text-muted">
                                None — RATE_CACHE_PLAN_YEARS = <c:out value="${empty planYearsConstant ? '(not set)' : planYearsConstant}"/>.
                                The warm job skips its entire run until this is configured (D-84).
                            </span>
                        </c:otherwise>
                    </c:choose>
                </dd>

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
                            <c:out value="${lastRunAtDisplay}"/> —
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

        <div class="card mb-3">
            <div class="hdr-bar d-flex align-items-center">
                <i class="bi bi-search me-2"></i>Rate Cache Diagnostic
            </div>
            <div class="status-card" style="border-radius:0; border-top:none; margin-bottom:0;">
                <p class="text-muted mb-2" style="font-size:0.8rem;">
                    Issues two live HealthSherpa quotes, identical except <code>off_ex</code>, and shows both side by side.
                    Writes nothing — no cache row, no <code>illustration_log</code> row, no file.
                    <strong>Each click costs two live API calls</strong>; no rate limit is documented for this endpoint.
                </p>

                <c:if test="${not empty diagnosticError}">
                    <div class="alert alert-danger py-2" style="font-size:0.85rem;" role="alert">
                        <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${diagnosticError}"/>
                    </div>
                </c:if>

                <form method="get" action="RateCacheAdmin" class="row gy-2 gx-3 align-items-end">
                    <input type="hidden" name="action" value="diagnostic">
                    <div class="col-auto">
                        <label class="form-label mb-1" for="diagZip">ZIP</label>
                        <input type="text" class="form-control form-control-sm" id="diagZip" name="zip"
                               value="${diagnosticZip}" style="width:90px;">
                    </div>
                    <div class="col-auto">
                        <label class="form-label mb-1" for="diagFips">County FIPS</label>
                        <input type="text" class="form-control form-control-sm" id="diagFips" name="fips"
                               value="${diagnosticFips}" style="width:90px;">
                    </div>
                    <div class="col-auto">
                        <label class="form-label mb-1" for="diagAge">Age</label>
                        <input type="number" class="form-control form-control-sm" id="diagAge" name="age"
                               min="21" max="64" value="${diagnosticAge}" style="width:80px;">
                    </div>
                    <div class="col-auto">
                        <label class="form-label mb-1" for="diagPlanYear">Plan Year</label>
                        <input type="number" class="form-control form-control-sm" id="diagPlanYear" name="planYear"
                               value="${diagnosticPlanYear}" style="width:100px;">
                    </div>
                    <div class="col-auto">
                        <button type="submit" class="ssa-action save">
                            <i class="bi bi-play-fill me-1"></i>Run Diagnostic
                        </button>
                    </div>
                </form>

                <c:if test="${not empty diagnosticOffExchange or not empty diagnosticOnExchange}">
                    <div class="row mt-3 gy-3">
                        <div class="col-md-6">
                            <div class="status-card">
                                <strong><i class="bi bi-shop me-1"></i>Off-Exchange <span class="text-muted fw-normal">(off_ex = true)</span></strong>
                                <c:choose>
                                    <c:when test="${diagnosticOffExchange.success}">
                                        <dl class="mt-2">
                                            <dt>Base URL</dt>
                                            <dd><c:out value="${diagnosticOffExchange.baseUrl}"/></dd>
                                            <dt>Accumulated plan count</dt>
                                            <dd>${diagnosticOffExchange.accumulatedPlanCount}</dd>
                                            <dt>Result count (response)</dt>
                                            <dd>${diagnosticOffExchange.resultCount}</dd>
                                            <dt>Metal breakdown</dt>
                                            <dd>
                                                <c:forEach var="m" items="${diagnosticOffExchange.metalBreakdown}">
                                                    <c:out value="${m.key}"/>: ${m.value}<br>
                                                </c:forEach>
                                            </dd>
                                            <dt>Lowest silver</dt>
                                            <dd>
                                                <c:choose>
                                                    <c:when test="${not empty diagnosticOffExchange.lowestSilver}"><fmt:formatNumber value="${diagnosticOffExchange.lowestSilver}" type="currency"/></c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </dd>
                                            <dt>Second-lowest silver</dt>
                                            <dd>
                                                <c:choose>
                                                    <c:when test="${not empty diagnosticOffExchange.secondLowestSilver}"><fmt:formatNumber value="${diagnosticOffExchange.secondLowestSilver}" type="currency"/></c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </dd>
                                            <dt>Distinct issuers</dt>
                                            <dd>${diagnosticOffExchange.distinctIssuerCount}</dd>
                                        </dl>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="alert alert-danger py-2 mt-2" style="font-size:0.85rem;">
                                            <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${diagnosticOffExchange.errorMessage}"/>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="status-card">
                                <strong><i class="bi bi-shop-window me-1"></i>On-Exchange <span class="text-muted fw-normal">(off_ex = false)</span></strong>
                                <c:choose>
                                    <c:when test="${diagnosticOnExchange.success}">
                                        <dl class="mt-2">
                                            <dt>Base URL</dt>
                                            <dd><c:out value="${diagnosticOnExchange.baseUrl}"/></dd>
                                            <dt>Accumulated plan count</dt>
                                            <dd>${diagnosticOnExchange.accumulatedPlanCount}</dd>
                                            <dt>Result count (response)</dt>
                                            <dd>${diagnosticOnExchange.resultCount}</dd>
                                            <dt>Metal breakdown</dt>
                                            <dd>
                                                <c:forEach var="m" items="${diagnosticOnExchange.metalBreakdown}">
                                                    <c:out value="${m.key}"/>: ${m.value}<br>
                                                </c:forEach>
                                            </dd>
                                            <dt>Lowest silver</dt>
                                            <dd>
                                                <c:choose>
                                                    <c:when test="${not empty diagnosticOnExchange.lowestSilver}"><fmt:formatNumber value="${diagnosticOnExchange.lowestSilver}" type="currency"/></c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </dd>
                                            <dt>Second-lowest silver</dt>
                                            <dd>
                                                <c:choose>
                                                    <c:when test="${not empty diagnosticOnExchange.secondLowestSilver}"><fmt:formatNumber value="${diagnosticOnExchange.secondLowestSilver}" type="currency"/></c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </dd>
                                            <dt>Distinct issuers</dt>
                                            <dd>${diagnosticOnExchange.distinctIssuerCount}</dd>
                                        </dl>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="alert alert-danger py-2 mt-2" style="font-size:0.85rem;">
                                            <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${diagnosticOnExchange.errorMessage}"/>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>

                    <div class="quiet-note mt-2">
                        Neither panel can show a per-plan on/off-exchange indicator or the raw HealthSherpa error
                        code/envelope — the currently parsed plan and error fields don't carry them. Exchange status
                        here is only knowable from which call produced which panel. "Result count (response)" and
                        "Accumulated plan count" are always identical on success under the current API contract —
                        see the servlet for why.
                    </div>
                </c:if>
            </div>
        </div>

        <c:choose>
            <c:when test="${not empty countySummariesByYear}">
                <c:forEach var="yearEntry" items="${countySummariesByYear}">
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex align-items-center">
                            <i class="bi bi-map me-2"></i>Plan Year ${yearEntry.key} — Configured Counties
                        </div>
                        <c:choose>
                            <c:when test="${not empty yearEntry.value}">
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
                                    <c:forEach var="cs" items="${yearEntry.value}">
                                        <tr>
                                            <td><c:out value="${cs.countyFips}"/></td>
                                            <td>${cs.rowCount}</td>
                                            <td><c:out value="${cs.oldestFetchedAtDisplay}"/></td>
                                            <td><c:out value="${cs.newestFetchedAtDisplay}"/></td>
                                            <td><c:out value="${cs.sourceEnv}"/></td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </c:when>
                            <c:otherwise>
                                <div class="empty-state">
                                    <i class="bi bi-map"></i>
                                    <div style="font-size:0.85rem; margin-top:0.5rem;">No cached counties for plan year ${yearEntry.key} yet.</div>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <i class="bi bi-map"></i>
                    <div style="font-size:0.85rem; margin-top:0.5rem;">No plan years configured (RATE_CACHE_PLAN_YEARS) — nothing to show.</div>
                </div>
            </c:otherwise>
        </c:choose>

    </div>
</div>
</body>
</html>

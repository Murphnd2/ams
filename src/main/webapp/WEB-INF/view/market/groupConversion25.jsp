<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Group-to-ICHRA conversion analysis (build-plan item 11, A4a).

     D24, non-negotiable: this output goes to the agent and never to the employer. There is
     deliberately NO print, PDF, export, email, share-link, download, or "use this in a
     proposal" affordance on this page, and none may be added "for convenience".

     Also deliberately absent: any plan name, carrier name, plan list, ranking, badge or
     default selection (aggregate market cost only), and any affordability output —
     affordability is item 9's, on /Illustration?mode=AGE_BAND. --%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Group-to-ICHRA Conversion</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .illustration-wrap {
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
        .illustration-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
        .disclaimer {
            background: #fff3cd; border: 1px solid #ffe69c; border-radius: 6px;
            padding: 0.75rem 1rem; margin-bottom: 0.9rem; font-size: 0.85rem; color: #664d03;
        }
        .agent-only {
            background: #e2e3e5; border: 1px solid #c4c8cb; border-radius: 6px;
            padding: 0.6rem 1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem; font-weight: 700; color: #41464b;
        }
        .results-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; background: #fff; }
        .results-table th {
            background: #f8f9fa; text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .results-table td { padding: 0.55rem 0.75rem; border-bottom: 1px solid #eee; }
        .results-table tr.headline td { font-weight: 700; color: #0d5681; font-size: 0.95rem; }
        .net-ahead { color: #0f5132; font-weight: 700; }
        .net-behind { color: #842029; font-weight: 700; }
        .footnote { font-size: 0.75rem; color: #6c757d; margin-top: 0.35rem; }
        .quiet-note { font-size: 0.78rem; color: #6c757d; margin-top: 0.5rem; }
        .meta-line { font-size: 0.8rem; color: #495057; margin-top: 0.75rem; }
        .summary-line { font-size: 0.95rem; font-weight: 700; color: #0d5681; line-height: 1.45; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="illustration-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0"><i class="bi bi-arrow-left-right me-1"></i>Group-to-ICHRA Conversion</h1>
    </div>

    <div class="illustration-body">

        <c:choose>
            <c:when test="${empty configuredPlanYears}">
                <div class="empty-state">
                    <i class="bi bi-graph-up"></i>
                    <div style="font-size:0.85rem; margin-top:0.5rem;">Rate cache is not configured on this installation.</div>
                </div>
            </c:when>
            <c:otherwise>

                <c:if test="${not empty inputError}">
                    <div class="alert alert-danger py-2" style="font-size:0.85rem;" role="alert">
                        <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${inputError}"/>
                    </div>
                </c:if>

                <div class="status-card">
                    <form method="post" action="GroupConversion" class="row gy-2 gx-3 align-items-end">
                        <%-- Item 13: carry the opportunity attribution across this form's own
                             re-submissions. Already resolved and scope-checked server-side;
                             absent entirely when there is none. Not a picker — no UI. --%>
                        <c:if test="${not empty opportunityId}">
                            <input type="hidden" name="opportunityId" value="${opportunityId}">
                        </c:if>
                        <div class="col-auto">
                            <label class="form-label mb-1" for="countyFips">County</label>
                            <select class="form-select form-select-sm" id="countyFips" name="countyFips" ${empty availableCounties ? 'disabled' : ''}>
                                <option value="">-- Select a county --</option>
                                <c:forEach var="county" items="${availableCounties}">
                                    <option value="${county.countyFips}" ${county.countyFips == submittedCountyFips ? 'selected' : ''}>
                                        <c:out value="${county.countyName}"/>, <c:out value="${county.state}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <c:choose>
                            <c:when test="${fn:length(configuredPlanYears) > 1}">
                                <div class="col-auto">
                                    <label class="form-label mb-1" for="planYear">Plan Year</label>
                                    <select class="form-select form-select-sm" id="planYear" name="planYear">
                                        <c:forEach var="y" items="${configuredPlanYears}">
                                            <option value="${y}" ${y == selectedPlanYear ? 'selected' : ''}>${y}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="planYear" value="${selectedPlanYear}">
                            </c:otherwise>
                        </c:choose>

                        <div class="col-12">
                            <label class="form-label mb-1 d-block">Census <span class="text-muted fw-normal">(blank age = skip row; deduction is optional)</span></label>
                            <div class="d-flex flex-wrap gap-2">
                                <c:forEach begin="1" end="6" var="i">
                                    <div class="d-flex align-items-end gap-1">
                                        <div>
                                            <label class="form-label mb-1" style="font-size:0.7rem;" for="age${i}">Age</label>
                                            <input type="number" class="form-control form-control-sm" id="age${i}" name="age${i}"
                                                   min="21" max="64" value="${submittedAges[i-1]}" style="width:75px;">
                                        </div>
                                        <div>
                                            <label class="form-label mb-1" style="font-size:0.7rem;" for="count${i}">Count</label>
                                            <input type="number" class="form-control form-control-sm" id="count${i}" name="count${i}"
                                                   min="1" placeholder="1" value="${submittedCounts[i-1]}" style="width:65px;">
                                        </div>
                                        <div>
                                            <label class="form-label mb-1" style="font-size:0.7rem;" for="deduction${i}">Deduction</label>
                                            <input type="number" step="0.01" class="form-control form-control-sm" id="deduction${i}" name="deduction${i}"
                                                   min="0" placeholder="Monthly" value="${submittedDeductions[i-1]}" style="width:100px;">
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>

                        <div class="col-auto">
                            <label class="form-label mb-1" for="currentTotalPremium">Current Total Monthly Premium</label>
                            <input type="number" step="0.01" class="form-control form-control-sm" id="currentTotalPremium" name="currentTotalPremium"
                                   min="0" value="${submittedCurrentTotalPremium}" style="width:190px;">
                            <div class="footnote">Employer + employee combined.</div>
                        </div>
                        <div class="col-auto">
                            <label class="form-label mb-1" for="currentEmployerShare">Current Monthly Employer Share</label>
                            <input type="number" step="0.01" class="form-control form-control-sm" id="currentEmployerShare" name="currentEmployerShare"
                                   min="0" value="${submittedCurrentEmployerShare}" style="width:190px;">
                            <div class="footnote">Employee share is derived as total &minus; employer share.</div>
                        </div>
                        <div class="col-auto">
                            <label class="form-label mb-1" for="proposedContribution">Proposed ICHRA Contribution</label>
                            <input type="number" step="0.01" class="form-control form-control-sm" id="proposedContribution" name="proposedContribution"
                                   min="0" value="${submittedProposedContribution}" style="width:190px;">
                            <div class="footnote">Flat, per employee, per month.</div>
                        </div>

                        <div class="col-auto">
                            <button type="submit" class="ssa-action save" ${empty availableCounties ? 'disabled' : ''}>
                                <i class="bi bi-arrow-left-right me-1"></i>Compare
                            </button>
                        </div>
                    </form>

                    <c:if test="${missingReferenceCount > 0}">
                        <div class="quiet-note">
                            <c:out value="${missingReferenceCount}"/> cached county reference${missingReferenceCount == 1 ? '' : 'es'} not shown above — no matching county_reference row.
                        </div>
                    </c:if>
                </div>

                <c:if test="${empty availableCounties}">
                    <div class="empty-state">
                        <i class="bi bi-map"></i>
                        <div style="font-size:0.85rem; margin-top:0.5rem;">No counties have cached rate data yet. Rates are loaded by the nightly rate-cache warm job.</div>
                    </div>
                </c:if>

                <c:if test="${not empty selectedCounty}">
                    <c:choose>
                        <c:when test="${not hasRates}">
                            <div class="empty-state">
                                <i class="bi bi-exclamation-circle"></i>
                                <div style="font-size:0.85rem; margin-top:0.5rem;">
                                    No cached rate data for age(s)
                                    <c:forEach var="a" items="${missingAges}" varStatus="as">${a}<c:if test="${!as.last}">, </c:if></c:forEach>
                                    in this county — cache-completeness gap, not computed.
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>

                            <div class="agent-only">
                                <i class="bi bi-eye-slash me-1"></i>For agent use only. Not for distribution to the employer or to employees.
                            </div>

                            <c:if test="${not empty sourceEnv and sourceEnv != 'PRODUCTION'}">
                                <div class="disclaimer" style="background:#f8d7da; border-color:#f5c2c7; color:#842029;">
                                    <i class="bi bi-exclamation-triangle-fill me-1"></i>
                                    <strong>Test-environment rates.</strong> These figures came from the
                                    <c:out value="${sourceEnv}"/> environment, not production market data. Do not present this to a client.
                                </div>
                            </c:if>

                            <div class="disclaimer">
                                <i class="bi bi-info-circle me-1"></i>
                                This is an illustration based on cached market rates, not a quote and not a compliance determination.
                                Actual premiums depend on individual enrollee details, and ICHRA affordability must be determined separately.
                            </div>

                            <div class="disclaimer">
                                <i class="bi bi-info-circle me-1"></i>
                                <strong>Off-exchange plans only.</strong> These figures cover the off-exchange individual market.
                                On-exchange plans are not included in the premium figures shown.
                            </div>

                            <div class="status-card">
                                <div class="summary-line"><c:out value="${summaryLine}"/></div>
                            </div>

                            <div class="status-card">
                                <strong>Employer Monthly Cost</strong>
                                <table class="results-table mt-2">
                                    <tbody>
                                    <tr>
                                        <td>Current employer monthly cost</td>
                                        <td><fmt:formatNumber value="${currentEmployerCost}" type="currency"/></td>
                                    </tr>
                                    <tr>
                                        <td>Proposed ICHRA monthly cost <span class="text-muted fw-normal">(contribution &times; ${submittedTotalLives})</span></td>
                                        <td><fmt:formatNumber value="${proposedEmployerCost}" type="currency"/></td>
                                    </tr>
                                    <tr class="headline">
                                        <td>Monthly change</td>
                                        <td class="${monthlyDelta > 0 ? 'net-behind' : 'net-ahead'}">
                                            ${monthlyDelta >= 0 ? '+' : '-'}<fmt:formatNumber value="${monthlyDelta < 0 ? -monthlyDelta : monthlyDelta}" type="currency"/>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>Annualised change <span class="text-muted fw-normal">(&times; 12)</span></td>
                                        <td class="${annualDelta > 0 ? 'net-behind' : 'net-ahead'}">
                                            ${annualDelta >= 0 ? '+' : '-'}<fmt:formatNumber value="${annualDelta < 0 ? -annualDelta : annualDelta}" type="currency"/>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>Percentage change</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty pctChange}">
                                                    ${pctChange >= 0 ? '+' : '-'}<fmt:formatNumber value="${pctChange < 0 ? -pctChange : pctChange}" maxFractionDigits="1" minFractionDigits="1"/>%
                                                </c:when>
                                                <c:otherwise>&mdash; <span class="text-muted">not available against a $0.00 current employer cost</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>

                            <table class="results-table">
                                <thead>
                                <tr>
                                    <th>Age</th>
                                    <th>Count</th>
                                    <th>Lowest Bronze <span class="text-muted fw-normal">(per employee)</span></th>
                                    <th>Proposed Employee Cost <span class="text-muted fw-normal">(after contribution)</span></th>
                                    <th>Current Employee Cost</th>
                                    <th>Net Position</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="row" items="${conversionResultRows}">
                                    <tr>
                                        <td>${row.age}</td>
                                        <td>${row.count}</td>
                                        <td><fmt:formatNumber value="${row.marketPremium}" type="currency"/></td>
                                        <td><fmt:formatNumber value="${row.proposedEmployeeCost}" type="currency"/></td>
                                        <td>
                                            <fmt:formatNumber value="${row.currentEmployeeCost}" type="currency"/>
                                            <c:if test="${not row.deductionSupplied}">
                                                <span class="text-muted" style="font-size:0.75rem;">(derived average)</span>
                                            </c:if>
                                        </td>
                                        <td class="${row.netPosition > 0 ? 'net-ahead' : (row.netPosition < 0 ? 'net-behind' : '')}">
                                            ${row.netPosition >= 0 ? '+' : '-'}<fmt:formatNumber value="${row.netPosition < 0 ? -row.netPosition : row.netPosition}" type="currency"/>
                                            <c:choose>
                                                <c:when test="${row.netPosition > 0}"> ahead</c:when>
                                                <c:when test="${row.netPosition < 0}"> behind</c:when>
                                                <c:otherwise> even</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                            <div class="footnote">
                                Lowest Bronze is the cached lowest-bronze premium at that age — the practical market floor.
                                Net Position is current employee cost minus proposed employee cost; a positive figure means the employee comes out ahead.
                                Rows without an entered payroll deduction use the derived average employee share
                                (<fmt:formatNumber value="${totalEmployeeShare}" type="currency"/> &divide; ${submittedTotalLives} =
                                <fmt:formatNumber value="${derivedEmployeeCost}" type="currency"/>).
                            </div>

                            <div class="meta-line">
                                <c:out value="${selectedCounty.countyName}"/>, <c:out value="${selectedCounty.state}"/> &middot;
                                Plan Year ${selectedPlanYear} &middot;
                                <c:choose>
                                    <c:when test="${not empty fetchedAtDisplay}">
                                        Rates as of <c:out value="${fetchedAtDisplay}"/>
                                    </c:when>
                                    <c:otherwise>Cache freshness unavailable</c:otherwise>
                                </c:choose>
                                <c:choose>
                                    <c:when test="${sourceEnv == 'PRODUCTION'}"> &middot; Source: production</c:when>
                                    <c:when test="${empty sourceEnv}"> &middot; Source: not recorded</c:when>
                                </c:choose>
                            </div>

                        </c:otherwise>
                    </c:choose>
                </c:if>

            </c:otherwise>
        </c:choose>

    </div>
</div>
</body>
</html>

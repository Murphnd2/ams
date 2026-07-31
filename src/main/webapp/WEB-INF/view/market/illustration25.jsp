<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ICHRA Illustration</title>
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
        .results-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; background: #fff; }
        .results-table th {
            background: #f8f9fa; text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .results-table td { padding: 0.55rem 0.75rem; border-bottom: 1px solid #eee; }
        .results-table tr.headline td { font-weight: 700; color: #0d5681; font-size: 0.95rem; }
        .footnote { font-size: 0.75rem; color: #6c757d; margin-top: 0.35rem; }
        .quiet-note { font-size: 0.78rem; color: #6c757d; margin-top: 0.5rem; }
        .meta-line { font-size: 0.8rem; color: #495057; margin-top: 0.75rem; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="illustration-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0"><i class="bi bi-calculator me-1"></i>ICHRA Illustration</h1>
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
                    <form method="get" action="Illustration" class="row gy-2 gx-3 align-items-end">
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

                        <div class="col-auto">
                            <label class="form-label mb-1" for="headcount">Eligible Employees</label>
                            <input type="number" class="form-control form-control-sm" id="headcount" name="headcount"
                                   min="1" max="10000" value="${submittedHeadcount}" style="width:140px;">
                        </div>

                        <div class="col-auto">
                            <button type="submit" class="ssa-action save" ${empty availableCounties ? 'disabled' : ''}>
                                <i class="bi bi-calculator me-1"></i>Illustrate
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
                                <div style="font-size:0.85rem; margin-top:0.5rem;">No rate data for this county yet.</div>
                            </div>
                        </c:when>
                        <c:otherwise>

                            <div class="disclaimer">
                                <i class="bi bi-info-circle me-1"></i>
                                This is an illustration based on cached market rates, not a quote and not a compliance determination.
                                Actual premiums depend on individual enrollee details, and ICHRA affordability must be determined separately.
                            </div>

                            <table class="results-table">
                                <thead>
                                <tr>
                                    <th></th>
                                    <th>Age 21</th>
                                    <th>Age 40</th>
                                    <th>Age 64</th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr class="headline">
                                    <td>Lowest bronze <span class="text-muted fw-normal">(practical floor)</span></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age21Row.lowestBronzePremium}"><fmt:formatNumber value="${age21Row.lowestBronzePremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age40Row.lowestBronzePremium}"><fmt:formatNumber value="${age40Row.lowestBronzePremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age64Row.lowestBronzePremium}"><fmt:formatNumber value="${age64Row.lowestBronzePremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Benchmark silver</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age21Row.benchmarkSilverPremium}"><fmt:formatNumber value="${age21Row.benchmarkSilverPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age40Row.benchmarkSilverPremium}"><fmt:formatNumber value="${age40Row.benchmarkSilverPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age64Row.benchmarkSilverPremium}"><fmt:formatNumber value="${age64Row.benchmarkSilverPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Market low</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age21Row.marketLowPremium}"><fmt:formatNumber value="${age21Row.marketLowPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age40Row.marketLowPremium}"><fmt:formatNumber value="${age40Row.marketLowPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age64Row.marketLowPremium}"><fmt:formatNumber value="${age64Row.marketLowPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                            <div class="footnote">
                                <i class="bi bi-exclamation-triangle me-1"></i>
                                Market low at Age 21 may reflect a catastrophic plan, available only to enrollees under 30 — not available at ages 30 and up.
                                Lowest bronze is the practical floor for this illustration.
                            </div>

                            <div class="status-card mt-3">
                                <strong>Estimated monthly group premium at the bronze floor</strong>
                                <div style="font-size:1.05rem; font-weight:700; color:#0d5681; margin-top:0.35rem;">
                                    <c:choose>
                                        <c:when test="${not empty groupMonthlyLow and not empty groupMonthlyHigh}">
                                            <fmt:formatNumber value="${groupMonthlyLow}" type="currency"/> &ndash; <fmt:formatNumber value="${groupMonthlyHigh}" type="currency"/>
                                        </c:when>
                                        <c:when test="${not empty groupMonthlyLow}">
                                            From <fmt:formatNumber value="${groupMonthlyLow}" type="currency"/>
                                        </c:when>
                                        <c:when test="${not empty groupMonthlyHigh}">
                                            Up to <fmt:formatNumber value="${groupMonthlyHigh}" type="currency"/>
                                        </c:when>
                                        <c:otherwise>&mdash;</c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="footnote">For ${submittedHeadcount} eligible employees. This spread reflects age mix across the group, not plan choice.</div>
                            </div>

                            <div class="meta-line">
                                <c:out value="${selectedCounty.countyName}"/>, <c:out value="${selectedCounty.state}"/> &middot;
                                Plan Year ${selectedPlanYear} &middot;
                                <c:if test="${not empty carrierCount}">${carrierCount} carriers &middot; </c:if>
                                <c:if test="${not empty planCount}">${planCount} plans &middot; </c:if>
                                <c:choose>
                                    <c:when test="${not empty fetchedAt}">
                                        Rates as of <fmt:formatDate value="${fetchedAt}" pattern="yyyy-MM-dd HH:mm"/>
                                    </c:when>
                                    <c:otherwise>Cache freshness unavailable</c:otherwise>
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

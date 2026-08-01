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
        <c:if test="${not empty configuredPlanYears}">
            <%-- G5: the toggle carried county and plan year but dropped the headcount, so an
                 agent switching modes retyped a number they had entered one screen earlier.
                 Going AGE_BAND -> RANGE the carry is exact: submittedTotalLives is the sum of
                 the census row counts, which is precisely what RANGE means by headcount. It is
                 set only on a successful AGE_BAND compute, so it is empty on a bare form or a
                 validation bounce and the parameter is then simply omitted.

                 The reverse carry (RANGE -> AGE_BAND) is deliberately NOT done: a flat total
                 has no age to sit against, and seeding count1 with it would be right only for
                 a group whose members share one age -- wrong for the Sandoval demo case
                 (3 lives, 3 different ages) and wrong quietly, which is worse than blank. --%>
            <div class="ms-auto d-flex gap-1">
                <a class="btn btn-sm ${mode == 'RANGE' ? 'btn-primary' : 'btn-outline-secondary'}"
                   href="Illustration?mode=RANGE&countyFips=${submittedCountyFips}&planYear=${selectedPlanYear}${not empty submittedTotalLives ? '&headcount='.concat(submittedTotalLives) : ''}${not empty opportunityId ? '&opportunityId='.concat(opportunityId) : ''}">Range</a>
                <a class="btn btn-sm ${mode == 'AGE_BAND' ? 'btn-primary' : 'btn-outline-secondary'}"
                   href="Illustration?mode=AGE_BAND&countyFips=${submittedCountyFips}&planYear=${selectedPlanYear}${not empty opportunityId ? '&opportunityId='.concat(opportunityId) : ''}">Age Band</a>
            </div>
        </c:if>
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
                        <input type="hidden" name="mode" value="${mode}">
                        <%-- Item 13: carry the opportunity attribution across this form's own
                             re-submissions. Already resolved and scope-checked server-side;
                             absent entirely when there is none. Not a picker — no UI. --%>
                        <c:if test="${not empty opportunityId}">
                            <input type="hidden" name="opportunityId" value="${opportunityId}">
                        </c:if>
                        <%-- T74 ZIP intake. The agent has the employer's ZIP, not its county FIPS.

                             ⚠️ The note that stood here was wrong and caused R1. It read "the
                             servlet consults this ONLY when countyFips is absent, so the county
                             selector below still wins" — which also meant a stale selection beat
                             a freshly typed ZIP, and produced another county's rates with no
                             warning. The rule now: a blank ZIP leaves the county contract
                             untouched, and a present ZIP is always resolved, with a county it
                             contradicts never computed from. See IllustrationServlet.

                             The county selector deliberately STAYS. The crosswalk is
                             ZCTA-derived and Texas-only, so some valid ZIPs do not resolve
                             and the agent needs a way through; it is also what every
                             existing link uses; and keeping it makes this whole change
                             reversible by deleting the ZIP block. --%>
                        <div class="col-auto">
                            <label class="form-label mb-1" for="zip">ZIP</label>
                            <input type="text" class="form-control form-control-sm" id="zip" name="zip"
                                   inputmode="numeric" pattern="[0-9]{5}" maxlength="5" placeholder="75482"
                                   value="${submittedZip}" style="width:100px;">
                            <div class="quiet-note" id="zipResolvedNote" style="display:none; margin-top:0.2rem;"></div>
                        </div>

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

                        <c:choose>
                            <c:when test="${mode == 'AGE_BAND'}">
                                <div class="col-12">
                                    <label class="form-label mb-1 d-block">Ages and Headcounts <span class="text-muted fw-normal">(blank age = skip row)</span></label>
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
                                                    <label class="form-label mb-1" style="font-size:0.7rem;" for="income${i}">Income</label>
                                                    <input type="number" step="1" class="form-control form-control-sm" id="income${i}" name="income${i}"
                                                           min="1" placeholder="Annual" value="${submittedIncomes[i-1]}" style="width:100px;">
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </div>
                                <div class="col-auto">
                                    <label class="form-label mb-1" for="contribution">Employer Monthly Contribution</label>
                                    <input type="number" step="0.01" class="form-control form-control-sm" id="contribution" name="contribution"
                                           min="0" value="${submittedContribution}" style="width:160px;">
                                </div>
                                <div class="col-auto">
                                    <label class="form-label mb-1" for="affordabilityBasis">Affordability Basis</label>
                                    <select class="form-select form-select-sm" id="affordabilityBasis" name="affordabilityBasis" style="width:180px;">
                                        <option value="" ${empty affordabilityBasis ? 'selected' : ''}>None</option>
                                        <option value="FPL" ${affordabilityBasis == 'FPL' ? 'selected' : ''}>FPL Safe Harbor</option>
                                        <option value="INCOME" ${affordabilityBasis == 'INCOME' ? 'selected' : ''}>Entered Income</option>
                                    </select>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="col-auto">
                                    <label class="form-label mb-1" for="headcount">Eligible Employees</label>
                                    <input type="number" class="form-control form-control-sm" id="headcount" name="headcount"
                                           min="1" max="10000" value="${submittedHeadcount}" style="width:140px;">
                                </div>
                            </c:otherwise>
                        </c:choose>

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

                <%-- T74: crossing ZIP. 34% of Texas ZIPs touch more than one county, so this
                     is a normal step, not an error — hence a status-card and neutral wording
                     rather than an alert.

                     Nothing is pre-selected and nothing is marked likely. The list arrives
                     ordered by land-area share purely so it is stable and the bigger slice
                     is not buried, and that ordering must NOT read as a recommendation —
                     the no-steering boundary applies to counties exactly as it does to
                     plans. Every entry is rendered identically.

                     Each choice is a link to the ordinary ?countyFips= URL, so the result
                     the agent lands on is linkable and shareable like any other. --%>
                <%-- R2/R4: rendered always, hidden when unused, so the blur lookup reuses
                     THIS markup and THIS wording rather than carrying a second copy in
                     JavaScript. One source of truth for the copy; the script only toggles
                     visibility and swaps the ZIP and the list items. --%>
                <div class="status-card" id="zipChooserPanel" ${empty zipCandidates ? 'style="display:none;"' : ''}>
                    <strong><i class="bi bi-signpost-2 me-1"></i>ZIP <span id="zipChooserZip"><c:out value="${submittedZip}"/></span> is in more than one county</strong>
                    <div class="footnote" style="margin-bottom:0.6rem;">
                        Rates differ by county, so pick the one this employer is in.
                    </div>
                    <ul id="zipChooserList" style="list-style:none; padding-left:0; margin-bottom:0;">
                        <c:forEach var="cand" items="${zipCandidates}">
                            <c:url value="Illustration" var="candUrl">
                                <c:param name="mode" value="${mode}"/>
                                <c:param name="countyFips" value="${cand.countyFips}"/>
                                <c:param name="planYear" value="${selectedPlanYear}"/>
                                <c:if test="${not empty opportunityId}">
                                    <c:param name="opportunityId" value="${opportunityId}"/>
                                </c:if>
                            </c:url>
                            <li style="padding:0.25rem 0;">
                                <a href="${candUrl}"><c:out value="${cand.countyName}"/>, <c:out value="${cand.state}"/></a>
                            </li>
                        </c:forEach>
                    </ul>
                </div>

                <%-- T74: the ZIP is not in the crosswalk.

                     ⚠️ Wording is load-bearing. This is almost certainly a real ZIP — our
                     data is ZCTA-derived (so PO-box-only ZIPs are absent entirely) and
                     Texas-only. The gap is ours. An agent who thinks he mistyped will
                     retype it three times; an agent told the data is missing uses the
                     county selector and moves on. Never "invalid ZIP".

                     Kept distinct from the unwarmed-county case, which the servlet reports
                     separately through inputError and which is T76's to fix. --%>
                <%-- Same always-render-hidden treatment as the chooser above, for the same
                     reason: the blur lookup must not carry a second copy of this wording. --%>
                <div class="status-card" id="zipNoMatchPanel" ${zipNoMatch ? '' : 'style="display:none;"'}>
                    <strong><i class="bi bi-info-circle me-1"></i>We don't have ZIP <span id="zipNoMatchZip"><c:out value="${submittedZip}"/></span> in our county lookup</strong>
                    <div class="footnote" style="margin-top:0.4rem;">
                        ZIP coverage is incomplete — the lookup is built from Census tabulation areas,
                        which omit some valid ZIPs, and currently covers Texas only. This is a gap in
                        our data, not a problem with the ZIP.
                        <strong>Select the county above instead</strong> — everything else works the same.
                    </div>
                </div>

                <c:if test="${empty availableCounties}">
                    <div class="empty-state">
                        <i class="bi bi-map"></i>
                        <div style="font-size:0.85rem; margin-top:0.5rem;">No counties have cached rate data yet. Rates are loaded by the nightly rate-cache warm job.</div>
                    </div>
                </c:if>

                <%-- R3: `empty inputError` guards the whole result panel. A validation
                     failure returns from the mode handler BEFORE hasRates is set, so
                     `not hasRates` was true and this branch printed "No cached rate
                     data..." for a county that demonstrably has rates — collapsing the
                     unwarmed-county state (T76's) into a plain validation error, which is
                     precisely the pair prompt F required kept apart. On a validation
                     error, render no result panel at all. --%>
                <c:if test="${not empty selectedCounty and mode == 'AGE_BAND' and empty inputError}">
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
                                On-exchange plans are not included in the plan counts or the premium figures shown.
                            </div>

                            <%-- G9 / build-plan §1 step 5. The slider recomputes CLIENT-SIDE from
                                 figures already on the page — no POST per tick, no AJAX, no new
                                 endpoint, no second servlet. /Illustration is GET-only by design
                                 (IllustrationServlet:58-60), and that stays true: the slider does
                                 arithmetic on rendered data and never talks to the server.

                                 It never recomputes the flip point. flip = onexLCSP − pct × (income
                                 ÷ 12) does not depend on the contribution, so the server's figure is
                                 already final and is simply read back out of the row. That is
                                 deliberate: the regulated computation stays in
                                 AffordabilityCalculator, in one place, and there is no second
                                 implementation in JavaScript that could drift from it. The two
                                 constants are consequently NOT needed client-side.

                                 Nothing here is persisted. Dragging the slider writes no row, no
                                 log line, no column — the illustration_log row for this run was
                                 already written server-side, and it carries no contribution figure
                                 at all (IllustrationServlet:608-618). --%>
                            <div class="status-card mt-3" id="contribSliderCard">
                                <strong><i class="bi bi-sliders me-1"></i>Employer Monthly Contribution</strong>
                                <span class="text-muted" style="font-size:0.8rem;">&mdash; drag to see the effect; nothing is saved</span>
                                <div class="d-flex align-items-center gap-3 mt-2">
                                    <%-- data-submitted carries the figure the server actually
                                         computed with. It is read from here rather than from the
                                         input's own value, because a range input snaps its value to
                                         the step and would misreport what was submitted. --%>
                                    <input type="range" class="form-range flex-grow-1" id="contribSlider"
                                           min="0" step="5" value="${submittedContribution}"
                                           data-submitted="${submittedContribution}"
                                           aria-label="Employer monthly contribution">
                                    <div style="font-size:1.05rem; font-weight:700; color:#0d5681; min-width:7rem; text-align:right;"
                                         id="contribReadout"></div>
                                </div>
                                <div class="footnote" id="contribRevertNote" style="display:none;">
                                    Showing <span id="contribShown"></span>; the figures were calculated at
                                    <span id="contribSubmitted"></span>.
                                    <a href="#" id="contribReset">Reset</a>
                                </div>
                                <c:if test="${empty affordabilityBasis}">
                                    <%-- Without a basis there is no flip point to show, so say why
                                         rather than leaving the agent to discover the selector. Not
                                         a recommendation to turn it on and not a default. --%>
                                    <div class="footnote">
                                        Net cost only. Choose an <strong>Affordability Basis</strong> above and re-run to see
                                        the contribution at which each employee crosses the affordability threshold.
                                    </div>
                                </c:if>
                            </div>

                            <table class="results-table">
                                <thead>
                                <tr>
                                    <th>Age</th>
                                    <th>Count</th>
                                    <th>Lowest Bronze <span class="text-muted fw-normal">(per employee)</span></th>
                                    <th>Net / Employee <span class="text-muted fw-normal">(after contribution)</span></th>
                                    <th>Band Net Total</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="row" items="${ageBandResultRows}">
                                    <tr class="net-row" data-count="${row.count}" data-floor="${row.floorPremium}">
                                        <td>${row.age}</td>
                                        <td>${row.count}</td>
                                        <td><fmt:formatNumber value="${row.floorPremium}" type="currency"/></td>
                                        <td class="net-per-emp"><fmt:formatNumber value="${row.netPerEmployee}" type="currency"/></td>
                                        <td class="net-band"><fmt:formatNumber value="${row.bandNet}" type="currency"/></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>

                            <div class="status-card mt-3">
                                <strong>Group Monthly Net Cost</strong>
                                <div style="font-size:1.05rem; font-weight:700; color:#0d5681; margin-top:0.35rem;" id="groupNetTotalOut">
                                    <fmt:formatNumber value="${groupNetTotal}" type="currency"/>
                                </div>
                                <div class="footnote">For ${submittedTotalLives} eligible employees, after employer contribution. Sum of the Band Net Total column.</div>
                            </div>

                            <div class="status-card mt-3">
                                <strong>Employer Total Monthly Outlay</strong>
                                <div style="font-size:1.05rem; font-weight:700; color:#0d5681; margin-top:0.35rem;" id="employerOutlayOut">
                                    <fmt:formatNumber value="${employerOutlay}" type="currency"/>
                                </div>
                                <div class="footnote">Contribution &times; total eligible employees. Shown separately from net cost above.</div>
                            </div>

                            <c:if test="${not empty affordabilityBasis}">
                                <div class="status-card mt-3">
                                    <strong><i class="bi bi-shield-check me-1"></i>Affordability Threshold</strong>
                                    <span class="text-muted">
                                        &mdash; <c:choose><c:when test="${affordabilityBasis == 'FPL'}">FPL safe harbor</c:when><c:otherwise>entered income</c:otherwise></c:choose> basis
                                    </span>

                                    <c:choose>
                                        <c:when test="${not empty affordabilityUnavailableReason}">
                                            <div class="alert alert-warning py-2 mt-2" style="font-size:0.85rem;">
                                                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${affordabilityUnavailableReason}"/>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <p class="text-muted mt-2 mb-2" style="font-size:0.78rem;">
                                                This is an analysis for the employer, not a determination, and not advice to any employee.
                                                <c:if test="${affordabilityBasis == 'INCOME'}"> Entered-income results rest on an assumed income the employer cannot verify.</c:if>
                                                <c:if test="${affordabilityBasis == 'FPL'}"> FPL safe-harbor results depend on the employer electing that safe harbor.</c:if>
                                                Threshold figures are derived from cached rates and can differ from a live quote by a cent or two &mdash; treat as an estimate, not an exact figure.
                                            </p>

                                            <table class="results-table">
                                                <thead>
                                                <tr>
                                                    <th>Age</th>
                                                    <th>Count</th>
                                                    <th>On-Exchange LCSP</th>
                                                    <th>Flip Contribution</th>
                                                    <th>At Entered Contribution</th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                <c:forEach var="row" items="${affordabilityRows}">
                                                    <%-- data-flip is the server's own figure, read back
                                                         unchanged. The slider compares against it; it
                                                         never recomputes it. --%>
                                                    <tr class="afford-row" data-flip="${row.available ? row.flipContribution : ''}">
                                                        <td>${row.age}</td>
                                                        <td>${row.count}</td>
                                                        <c:choose>
                                                            <c:when test="${row.available}">
                                                                <td><fmt:formatNumber value="${row.onexLcspPremium}" type="currency"/></td>
                                                                <td><fmt:formatNumber value="${row.flipContribution}" type="currency"/></td>
                                                                <td class="afford-verdict">
                                                                    <c:choose>
                                                                        <c:when test="${row.affordable}">Affordable &mdash; employee loses PTC eligibility</c:when>
                                                                        <c:otherwise>Unaffordable &mdash; employee keeps PTC eligibility</c:otherwise>
                                                                    </c:choose>
                                                                </td>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <td colspan="3" class="text-muted"><c:out value="${row.unavailableReason}"/></td>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </tr>
                                                </c:forEach>
                                                </tbody>
                                            </table>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </c:if>

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

                            <%-- Build-plan item 7 hand-off. URL contract is provisional — item 6 decides
                                 whether ProposalBuilder re-derives the illustration from these inputs or
                                 reads a snapshot row; if a snapshot is needed, this becomes one param
                                 (a snapshot id) instead of the set below. Carries INPUTS only (county,
                                 plan year, mode, ages/counts, contribution) — never computed outputs,
                                 never affordability/income (those are scenario inputs on an assumed
                                 income and have no business in a proposal URL), never a prospect id (no
                                 drop-in prospect picker exists on this page — ProposalBuilder prompts for
                                 the prospect as it always does). --%>
                            <c:url value="ProposalBuilder" var="proposalHandoffUrl">
                                <c:param name="mode" value="AGE_BAND"/>
                                <c:param name="countyFips" value="${submittedCountyFips}"/>
                                <c:param name="planYear" value="${selectedPlanYear}"/>
                                <c:param name="contribution" value="${submittedContribution}"/>
                                <c:forEach begin="1" end="6" var="i">
                                    <c:if test="${not empty submittedAges[i-1]}">
                                        <c:param name="age${i}" value="${submittedAges[i-1]}"/>
                                        <c:param name="count${i}" value="${submittedCounts[i-1]}"/>
                                    </c:if>
                                </c:forEach>
                            </c:url>
                            <c:choose>
                                <c:when test="${sourceEnv == 'PRODUCTION'}">
                                    <a href="${proposalHandoffUrl}" class="ssa-action save" id="ichraProposalLink">
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <button type="button" class="ssa-action save" disabled>
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </button>
                                    <div class="quiet-note">Available once production rates are configured.</div>
                                </c:otherwise>
                            </c:choose>

                            <%-- G9 slider behaviour. Pure display arithmetic over data already in the
                                 DOM; no fetch, no form submit, no storage of any kind. Deliberately
                                 NOT here: any marking of a contribution as recommended, optimal or
                                 best, any default the slider snaps to, and any ranking — the control
                                 reports the flip point as a fact and leaves the choice with the
                                 agent. Deleting this script block restores the previous page
                                 exactly; every figure it touches is already rendered correctly by
                                 the server for the submitted contribution. --%>
                            <script>
                            (function () {
                                var slider = document.getElementById('contribSlider');
                                if (!slider) return;

                                var submitted = parseFloat(slider.getAttribute('data-submitted'));
                                if (isNaN(submitted)) submitted = 0;
                                // Set on first drag. The "you have moved it" note keys off this
                                // rather than off a value comparison, so a submitted figure that
                                // is not on a step boundary cannot make the note appear on load.
                                var userMoved = false;

                                var money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
                                var netRows = Array.prototype.slice.call(document.querySelectorAll('tr.net-row'));
                                var affordRows = Array.prototype.slice.call(document.querySelectorAll('tr.afford-row'));

                                // Headroom to the highest premium on the page, so the agent can always
                                // drag past the point where net cost reaches zero. Rounded up to a
                                // sane step; never below a floor, so a cheap county still gets range.
                                var highestFloor = 0;
                                netRows.forEach(function (tr) {
                                    var f = parseFloat(tr.getAttribute('data-floor'));
                                    if (!isNaN(f) && f > highestFloor) highestFloor = f;
                                });
                                var max = Math.max(1000, Math.ceil((highestFloor * 1.1) / 50) * 50);
                                if (submitted > max) max = Math.ceil(submitted / 50) * 50;
                                slider.max = max;
                                slider.value = submitted;

                                var readout = document.getElementById('contribReadout');
                                var revertNote = document.getElementById('contribRevertNote');
                                var shownEl = document.getElementById('contribShown');
                                var submittedEl = document.getElementById('contribSubmitted');
                                var formInput = document.getElementById('contribution');
                                var proposalLink = document.getElementById('ichraProposalLink');
                                var groupOut = document.getElementById('groupNetTotalOut');
                                var outlayOut = document.getElementById('employerOutlayOut');

                                function setContributionParam(href, value) {
                                    // Rewrites only the contribution parameter so the proposal
                                    // snapshot cannot disagree with the figure on screen. Without
                                    // this, dragging to 350 and clicking through would snapshot the
                                    // originally submitted 400 -- silently.
                                    if (!href) return href;
                                    var parts = href.split('?');
                                    if (parts.length < 2) return href;
                                    var pairs = parts[1].split('&').filter(function (p) {
                                        return p.indexOf('contribution=') !== 0;
                                    });
                                    pairs.push('contribution=' + encodeURIComponent(value));
                                    return parts[0] + '?' + pairs.join('&');
                                }

                                var baseHref = proposalLink ? proposalLink.getAttribute('href') : null;

                                function render() {
                                    var c = parseFloat(slider.value);
                                    if (isNaN(c) || c < 0) c = 0;

                                    readout.textContent = money.format(c);

                                    var groupNet = 0;
                                    var lives = 0;
                                    netRows.forEach(function (tr) {
                                        var floor = parseFloat(tr.getAttribute('data-floor'));
                                        var count = parseInt(tr.getAttribute('data-count'), 10);
                                        if (isNaN(floor) || isNaN(count)) return;
                                        var net = Math.max(0, floor - c);
                                        var band = net * count;
                                        groupNet += band;
                                        lives += count;
                                        tr.querySelector('.net-per-emp').textContent = money.format(net);
                                        tr.querySelector('.net-band').textContent = money.format(band);
                                    });

                                    if (groupOut) groupOut.textContent = money.format(groupNet);
                                    if (outlayOut) outlayOut.textContent = money.format(c * lives);

                                    // The flip point does not move with the contribution -- it is the
                                    // server's figure. Only which side of it we are on changes, and
                                    // the two strings are the ones already on the page.
                                    affordRows.forEach(function (tr) {
                                        var cell = tr.querySelector('.afford-verdict');
                                        if (!cell) return;
                                        var flip = parseFloat(tr.getAttribute('data-flip'));
                                        if (isNaN(flip)) return;
                                        // Dash built from its code point so this block stays pure
                                        // ASCII and cannot be mangled by an encoding step between
                                        // here and the browser. The resulting wording is identical
                                        // to what the server renders above: the slider flips
                                        // between two already-approved strings and introduces no
                                        // new phrasing about any employee (boundary 1).
                                        var DASH = String.fromCharCode(0x2014);
                                        cell.textContent = (c >= flip)
                                            ? 'Affordable ' + DASH + ' employee loses PTC eligibility'
                                            : 'Unaffordable ' + DASH + ' employee keeps PTC eligibility';
                                    });

                                    if (formInput) formInput.value = c;
                                    if (proposalLink && baseHref) {
                                        proposalLink.setAttribute('href', setContributionParam(baseHref, c));
                                    }

                                    revertNote.style.display = userMoved ? '' : 'none';
                                    if (userMoved) {
                                        shownEl.textContent = money.format(c);
                                        submittedEl.textContent = money.format(submitted);
                                    }
                                }

                                slider.addEventListener('input', function () {
                                    userMoved = true;
                                    render();
                                });
                                document.getElementById('contribReset').addEventListener('click', function (e) {
                                    e.preventDefault();
                                    slider.value = submitted;
                                    userMoved = false;
                                    render();
                                });
                                render();
                            })();
                            </script>

                        </c:otherwise>
                    </c:choose>
                </c:if>

                <%-- R3, RANGE side. Same reasoning as the AGE_BAND guard above: this is
                     the branch actually observed printing "No rate data for this county
                     yet." for Hopkins while the real failure was a blank headcount. --%>
                <c:if test="${not empty selectedCounty and mode != 'AGE_BAND' and empty inputError}">
                    <c:choose>
                        <c:when test="${not hasRates}">
                            <div class="empty-state">
                                <i class="bi bi-exclamation-circle"></i>
                                <div style="font-size:0.85rem; margin-top:0.5rem;">No rate data for this county yet.</div>
                            </div>
                        </c:when>
                        <c:otherwise>

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
                                On-exchange plans are not included in the plan counts or the premium figures shown.
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
                                    <td>Second-lowest silver <span class="text-muted fw-normal">(off-exchange)</span></td>
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
                                <c:if test="${not empty carrierCount}">${carrierCount} carriers (age ${countRowAge}) &middot; </c:if>
                                <c:if test="${not empty planCount}">${planCount} plans (age ${countRowAge}) &middot; </c:if>
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

                            <%-- Build-plan item 7 hand-off. URL contract is provisional — item 6 decides
                                 whether ProposalBuilder re-derives the illustration from these inputs or
                                 reads a snapshot row; if a snapshot is needed, this becomes one param
                                 (a snapshot id) instead of the set below. Carries INPUTS only (county,
                                 plan year, mode, headcount) — never computed outputs, never a prospect id
                                 (no drop-in prospect picker exists on this page — ProposalBuilder prompts
                                 for the prospect as it always does). --%>
                            <c:url value="ProposalBuilder" var="proposalHandoffUrl">
                                <c:param name="mode" value="RANGE"/>
                                <c:param name="countyFips" value="${submittedCountyFips}"/>
                                <c:param name="planYear" value="${selectedPlanYear}"/>
                                <c:param name="headcount" value="${submittedHeadcount}"/>
                            </c:url>
                            <c:choose>
                                <c:when test="${sourceEnv == 'PRODUCTION'}">
                                    <a href="${proposalHandoffUrl}" class="ssa-action save">
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <button type="button" class="ssa-action save" disabled>
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </button>
                                    <div class="quiet-note">Available once production rates are configured.</div>
                                </c:otherwise>
                            </c:choose>

                        </c:otherwise>
                    </c:choose>
                </c:if>

            </c:otherwise>
        </c:choose>

    </div>
</div>
<%-- R2/R4: resolve the ZIP on blur, without submitting anything.

     Before this, the only trigger was Enter -- which submits the form, so typing a
     ZIP with the headcount still empty answered with "Enter a valid number of
     eligible employees". Resolving a ZIP had become entangled with computing an
     illustration; this separates them.

     Deliberately NOT here: any copy of its own. The chooser and no-match panels are
     server-rendered above and merely hidden, and this script toggles them and swaps
     their ZIP and list items -- so the wording has one source and cannot drift.

     R1's other half: changing the ZIP clears the county selection immediately, before
     any lookup returns. A stale selection must not survive a new ZIP. The server
     enforces the same precedence independently, because this script may not run.

     Progressive enhancement throughout -- with JavaScript off, the field still posts
     as ?zip= and the servlet resolves it exactly as it does today. Nothing here is the
     only path to anything. --%>
<script>
(function () {
    var zipInput = document.getElementById('zip');
    if (!zipInput) return;

    var countySelect  = document.getElementById('countyFips');
    var chooser       = document.getElementById('zipChooserPanel');
    var chooserZip    = document.getElementById('zipChooserZip');
    var chooserList   = document.getElementById('zipChooserList');
    var noMatch       = document.getElementById('zipNoMatchPanel');
    var noMatchZip    = document.getElementById('zipNoMatchZip');
    var resolvedNote  = document.getElementById('zipResolvedNote');
    var form          = zipInput.form;

    // What the server already rendered for. Re-looking-up the same value on every
    // blur would flicker the panels the server just drew.
    var lastLookedUp = (zipInput.value || '').trim();

    function hidePanels() {
        if (chooser) chooser.style.display = 'none';
        if (noMatch) noMatch.style.display = 'none';
        if (resolvedNote) {
            resolvedNote.style.display = 'none';
            resolvedNote.textContent = '';
        }
    }

    // R1 + R4. Any edit to the ZIP invalidates both the county selection and whatever
    // panel is on screen, immediately -- not when the lookup returns.
    function invalidate() {
        hidePanels();
        if (countySelect) countySelect.value = '';
    }

    function currentParam(name, fallback) {
        if (!form) return fallback;
        var el = form.elements[name];
        return (el && el.value) ? el.value : fallback;
    }

    function selectCounty(county) {
        if (!countySelect) return;
        var found = false;
        for (var i = 0; i < countySelect.options.length; i++) {
            if (countySelect.options[i].value === county.fips) {
                countySelect.selectedIndex = i;
                found = true;
                break;
            }
        }
        if (resolvedNote) {
            // Mirrors the server's own wording for each case rather than inventing one.
            resolvedNote.textContent = found
                ? (county.name + ', ' + county.state)
                : ('That ZIP is in ' + county.name + ', ' + county.state + ', which has no cached rates yet.');
            resolvedNote.style.display = '';
        }
    }

    function renderChooser(zip, counties) {
        if (!chooser || !chooserList) return;
        if (chooserZip) chooserZip.textContent = zip;

        var mode = currentParam('mode', 'RANGE');
        var planYear = currentParam('planYear', '');

        // Rebuilt with createElement/textContent, never innerHTML: every value here
        // came off an HTTP response, and a response is data, not markup.
        chooserList.textContent = '';
        counties.forEach(function (county) {
            var href = 'Illustration?mode=' + encodeURIComponent(mode)
                     + '&countyFips=' + encodeURIComponent(county.fips)
                     + (planYear ? '&planYear=' + encodeURIComponent(planYear) : '');

            var a = document.createElement('a');
            a.setAttribute('href', href);
            a.textContent = county.name + ', ' + county.state;

            var li = document.createElement('li');
            li.style.padding = '0.25rem 0';
            li.appendChild(a);
            chooserList.appendChild(li);
        });

        chooser.style.display = '';
    }

    function lookup() {
        var raw = (zipInput.value || '').trim();
        if (raw === lastLookedUp) return;
        lastLookedUp = raw;

        invalidate();

        // Not five digits yet: say nothing at all. Half-typed input is not an error,
        // and calling it one is what makes an agent retype a ZIP three times.
        if (!/^[0-9]{5}$/.test(raw)) return;

        fetch('IchraZipLookup?zip=' + encodeURIComponent(raw), {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (res) { return res.ok ? res.json() : { counties: [] }; })
            .then(function (data) {
                // The field moved on while this was in flight -- drop the answer.
                if ((zipInput.value || '').trim() !== raw) return;

                var counties = (data && Array.isArray(data.counties)) ? data.counties : [];
                if (counties.length === 0) {
                    if (noMatchZip) noMatchZip.textContent = raw;
                    if (noMatch) noMatch.style.display = '';
                } else if (counties.length === 1) {
                    selectCounty(counties[0]);
                } else {
                    // Nothing auto-selects. The agent picks, exactly as on the server path.
                    renderChooser(raw, counties);
                }
            })
            .catch(function () {
                // Leave it to the submit path, which resolves server-side regardless.
            });
    }

    zipInput.addEventListener('change', lookup);
    zipInput.addEventListener('blur', lookup);
    zipInput.addEventListener('input', function () {
        if ((zipInput.value || '').trim() !== lastLookedUp) invalidate();
    });
})();
</script>
</body>
</html>

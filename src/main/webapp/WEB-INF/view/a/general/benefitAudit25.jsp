<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Benefit Renewal Audit" scope="request"/>
<c:set var="pageIcon" value="bi-calendar-check" scope="request"/>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Benefit Renewal Audit</title>
    <style>
        .audit-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
            overflow: hidden;
        }
        .audit-toolbar {
            display: flex; align-items: center; justify-content: space-between;
            padding: 10px 16px 8px; flex-shrink: 0;
        }
        .audit-scroll {
            flex: 1; overflow-y: auto; padding: 0 16px 16px;
        }
        .audit-table th {
            font-size: 0.78rem; white-space: nowrap;
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
        }
        .audit-table td { font-size: 0.84rem; vertical-align: middle; }
        .badge-mismatch { background: #dc3545; color: #fff; font-size: 0.7rem; }
        .badge-ok { background: #198754; color: #fff; font-size: 0.7rem; }
        .edit-date {
            width: 105px; font-size: 0.82rem; padding: 2px 6px;
            text-align: center;
        }
        .edit-months {
            width: 60px; font-size: 0.82rem; padding: 2px 6px;
            text-align: center;
        }
        .save-btn { padding: 2px 8px; font-size: 0.75rem; }
        .ghost-action {
            background: none; border: none; color: var(--ssa);
            font-size: 0.82rem; padding: 4px 10px; cursor: pointer;
            border-radius: 4px; text-decoration: none;
            display: inline-flex; align-items: center; gap: 4px;
            line-height: 1.4;
        }
        .ghost-action:hover { background: rgba(13,86,129,0.08); color: var(--ssa); }
        .ghost-action-form {
            display: inline-flex; align-items: center; margin: 0; padding: 0;
        }
        .employer-search {
            width: 220px; font-size: 0.82rem; padding: 4px 8px;
            border: 1px solid #dee2e6; border-radius: 4px;
        }
        .employer-search:focus {
            border-color: var(--ssa); outline: none;
            box-shadow: 0 0 0 2px rgba(13,86,129,0.15);
        }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="audit-wrap">
    <%-- Toolbar --%>
    <div class="audit-toolbar">
        <input type="text" id="employerSearch" class="employer-search" placeholder="Search employer..."
               autocomplete="off">
        <div class="d-flex align-items-center gap-1">
            <c:if test="${filter eq 'all' or filter eq 'flagged'}">
                <a href="BenefitAudit${filter eq 'flagged' ? '' : '?filter=flagged'}" class="ghost-action">
                    <i class="bi bi-funnel"></i> ${filter eq 'flagged' ? 'Show All' : 'Flagged Only'}
                </a>
            </c:if>
            <c:if test="${filter eq 'all' or filter eq 'noRenewal'}">
                <a href="BenefitAudit${filter eq 'noRenewal' ? '' : '?filter=noRenewal'}" class="ghost-action">
                    <i class="bi bi-calendar-x"></i> ${filter eq 'noRenewal' ? 'Show All' : 'No Renewal'}
                </a>
            </c:if>
            <c:if test="${filter eq 'noRenewal'}">
                <form method="POST" action="BenefitAudit" class="ghost-action-form">
                    <input type="hidden" name="action" value="acceptAll">
                    <input type="hidden" name="filter" value="noRenewal">
                    <button type="submit" class="ghost-action"
                            onclick="return confirm('Accept all detected renewal dates? This will update nextRenewalDue for all benefits with plan year data.');">
                        <i class="bi bi-check-all"></i> Accept All Detected
                    </button>
                </form>
            </c:if>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger mx-3 mb-2">${error}</div>
    </c:if>

    <%-- Scrollable table --%>
    <div class="audit-scroll">
        <table class="table table-sm table-hover audit-table mb-0">
            <thead>
                <tr>
                    <th>Employer</th>
                    <th>Plan Name</th>
                    <th>Type</th>
                    <th>Source</th>
                    <th>Effective Date</th>
                    <th>Plan Year End</th>
                    <th>Detected Renewal</th>
                    <th>Next Renewal Due</th>
                    <th>Mo.</th>
                    <th>Status</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="b" items="${benefits}">
                    <c:set var="detectedRenewal" value="${b.detectedRenewalDate}"/>
                    <c:set var="hasPlanYear" value="${b.planYearEnd ne null}"/>
                    <c:set var="isMismatch" value="false"/>
                    <c:if test="${hasPlanYear and b.effectiveDate ne null}">
                        <fmt:formatDate var="effMD" value="${b.effectiveDate}" pattern="MM/dd"/>
                        <c:set var="formattedDetectedMD"><fmt:formatNumber value="${detectedRenewal.monthValue}" minIntegerDigits="2" pattern="00"/>/<fmt:formatNumber value="${detectedRenewal.dayOfMonth}" minIntegerDigits="2" pattern="00"/></c:set>
                        <c:if test="${effMD ne formattedDetectedMD}">
                            <c:set var="isMismatch" value="true"/>
                        </c:if>
                    </c:if>

                    <c:if test="${filter eq 'all' or (filter eq 'flagged' and isMismatch) or (filter eq 'noRenewal' and b.nextRenewalDue eq null)}">
                        <tr>
                            <td>${b.employer.employerName}</td>
                            <td>${b.planName}</td>
                            <td>${b.planType.planTypeName}</td>
                            <td><span class="badge bg-secondary" style="font-size:0.7rem;">${b.sourceType}</span></td>
                            <td><fmt:formatDate value="${b.effectiveDate}" pattern="MM/dd/yyyy"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${hasPlanYear}"><fmt:formatDate value="${b.planYearEnd}" pattern="MM/dd/yyyy"/></c:when>
                                    <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${detectedRenewal ne null}">
                                        <fmt:formatNumber value="${detectedRenewal.monthValue}" minIntegerDigits="2" pattern="00"/>/<fmt:formatNumber value="${detectedRenewal.dayOfMonth}" minIntegerDigits="2" pattern="00"/>/<c:out value="${detectedRenewal.year}"/>
                                    </c:when>
                                    <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <form method="POST" action="BenefitAudit" class="d-flex gap-1 align-items-center mb-0">
                                    <input type="hidden" name="action" value="save">
                                    <input type="hidden" name="benefitId" value="${b.id}">
                                    <c:if test="${filter ne 'all'}">
                                        <input type="hidden" name="filter" value="${filter}">
                                    </c:if>
                                    <input type="text" name="nextRenewalDue" class="form-control form-control-sm edit-date"
                                           value="<fmt:formatDate value='${b.nextRenewalDue}' pattern='MM/dd/yyyy'/>"
                                           placeholder="MM/DD/YYYY">
                            </td>
                            <td>
                                    <input type="number" name="renewalMonths" class="form-control form-control-sm edit-months"
                                           value="${b.renewalMonths}" min="1" max="60">
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${isMismatch}"><span class="badge badge-mismatch">Flagged</span></c:when>
                                    <c:when test="${hasPlanYear}"><span class="badge badge-ok">OK</span></c:when>
                                    <c:otherwise><span class="text-muted" style="font-size:0.75rem;">No data</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                    <button type="submit" class="btn btn-outline-primary save-btn">Save</button>
                                </form>
                            </td>
                        </tr>
                    </c:if>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
<script>
document.getElementById('employerSearch').addEventListener('input', function() {
    var term = this.value.trim().toLowerCase();
    if (!term) return;
    var rows = document.querySelectorAll('.audit-table tbody tr');
    for (var i = 0; i < rows.length; i++) {
        var employer = rows[i].querySelector('td');
        if (employer && employer.textContent.trim().toLowerCase().indexOf(term) === 0) {
            rows[i].scrollIntoView({behavior: 'smooth', block: 'center'});
            return;
        }
    }
});
</script>
</body>
</html>

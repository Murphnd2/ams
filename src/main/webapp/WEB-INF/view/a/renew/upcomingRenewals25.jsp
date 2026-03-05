<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>Upcoming Renewals</title>
  <style>
    .renewal-card {
      background: #fff;
      border: 1px solid #dee2e6;
      border-radius: 8px;
      padding: 0.85rem 1rem;
      margin-bottom: 0.5rem;
      transition: box-shadow 0.15s;
    }
    .renewal-card:hover {
      box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    }
    .renewal-card.stage-overdue {
      border-left: 4px solid #dc3545;
    }
    .renewal-card.stage-this-month {
      border-left: 4px solid #fd7e14;
    }
    .renewal-card.stage-future {
      border-left: 4px solid #0d5681;
    }
    .month-header {
      color: #fff;
      font-weight: 600;
      font-size: 0.95rem;
      padding: 0.55rem 1rem;
      border-radius: 8px;
      margin-bottom: 0.6rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .month-header-overdue {
      background: #dc3545;
    }
    .month-header-normal {
      background: linear-gradient(135deg, #0d5681 0%, #0a4468 100%);
    }
    .benefit-row {
      display: flex;
      align-items: center;
      padding: 0.4rem 0.6rem;
      border-bottom: 1px solid #f0f0f0;
      font-size: 0.88rem;
    }
    .benefit-row:last-child {
      border-bottom: none;
    }
    .benefit-row.flag-urgent {
      color: #dc3545;
      font-weight: 600;
    }
    .benefit-code {
      font-weight: 600;
      color: #0d5681;
      min-width: 50px;
    }
    .benefit-row.flag-urgent .benefit-code {
      color: #dc3545;
    }
    .btn-ssa {
      background-color: #0d5681;
      border-color: #0d5681;
      color: #fff;
      font-weight: 500;
      font-size: 0.85rem;
      border-radius: 6px;
      padding: 0.4rem 1rem;
    }
    .btn-ssa:hover {
      background-color: #0a4468;
      border-color: #0a4468;
      color: #fff;
    }
    .btn-ghost {
      background: none;
      border: 1px solid #0d5681;
      color: #0d5681;
      font-weight: 500;
      font-size: 0.82rem;
      border-radius: 6px;
      padding: 0.35rem 0.85rem;
    }
    .btn-ghost:hover {
      background: rgba(13,86,129,0.08);
      color: #0d5681;
    }
    .empty-state {
      text-align: center;
      padding: 4rem 2rem;
      color: #6c757d;
    }
    .empty-state i {
      font-size: 3rem;
      color: #87a948;
      margin-bottom: 1rem;
    }
    .employer-name {
      font-weight: 600;
      font-size: 0.95rem;
    }
    .employer-meta {
      font-size: 0.82rem;
      color: #6c757d;
    }
    .ur-wrap { max-width: 1100px; margin: 0 auto; padding: 0 1rem; }
    @media (max-width: 767.98px) { .ur-wrap { padding: 0 0.5rem; } }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

  <div class="ur-wrap mt-3">
  <%-- Header Bar --%>
  <div class="hdr-bar mb-3">
    <i class="bi bi-calendar-check me-2"></i>Upcoming Renewals
  </div>

  <c:choose>
    <c:when test="${empty renewalsByMonth}">
      <%-- Empty State --%>
      <div class="empty-state">
        <i class="bi bi-check-circle d-block"></i>
        <h5>No Upcoming Renewals</h5>
        <p class="text-muted">All benefits are current — no renewals due within the next 3 months.</p>
      </div>
    </c:when>
    <c:otherwise>
      <c:forEach var="monthEntry" items="${renewalsByMonth}">
        <c:set var="monthLabel" value="${monthEntry.key}" />
        <c:set var="employers" value="${monthEntry.value}" />
        <c:set var="isOverdue" value="${monthLabel == 'OVERDUE'}" />

        <%-- Month Group Header --%>
        <div class="month-header ${isOverdue ? 'month-header-overdue' : 'month-header-normal'}">
          <span>
            <c:choose>
              <c:when test="${isOverdue}">
                <i class="bi bi-exclamation-triangle-fill me-1"></i>OVERDUE
              </c:when>
              <c:otherwise>
                <i class="bi bi-calendar3 me-1"></i>${monthLabel}
              </c:otherwise>
            </c:choose>
          </span>
          <span style="font-size: 0.82rem; font-weight: 400;">
            ${fn:length(employers)} employer<c:if test="${fn:length(employers) != 1}">s</c:if>
          </span>
        </div>

        <%-- Employer Cards --%>
        <c:forEach var="re" items="${employers}">
          <c:set var="emp" value="${re.employer}" />
          <c:set var="isExpanded" value="${not empty expandedEmployerId && expandedEmployerId == emp.id}" />
          <c:set var="stageClass" value="stage-future" />
          <c:if test="${re.stage == 0}"><c:set var="stageClass" value="stage-overdue" /></c:if>
          <c:if test="${re.stage == 1}"><c:set var="stageClass" value="stage-this-month" /></c:if>

          <div class="renewal-card ${stageClass}" id="employer-${emp.id}">
            <c:choose>
              <c:when test="${isExpanded}">
                <%-- ═══ EXPANDED STATE ═══ --%>
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <div>
                    <span class="employer-name">${emp.employerName}</span>
                    <span class="employer-meta ms-2">
                      <c:if test="${not empty re.lastRenewed}">
                        Last renewed: <fmt:formatDate value="${re.lastRenewed}" pattern="MM/dd/yyyy" />
                      </c:if>
                    </span>
                  </div>
                  <form method="post" action="UpcomingRenewals" class="d-inline">
                    <input type="hidden" name="action" value="collapse" />
                    <a href="UpcomingRenewals" class="btn-ghost">
                      <i class="bi bi-chevron-up me-1"></i>Collapse
                    </a>
                  </form>
                </div>

                <%-- Benefit Checkboxes --%>
                <form method="post" action="UpcomingRenewals">
                  <input type="hidden" name="action" value="startRenewal" />
                  <div class="border rounded" style="border-color: #dee2e6 !important;">
                    <c:forEach var="benefit" items="${benefitsForDisplay}">
                      <c:set var="isUrgent" value="${benefit.flagBenefitForRenewal()}" />
                      <div class="benefit-row ${isUrgent ? 'flag-urgent' : ''}">
                        <div class="form-check me-3">
                          <input class="form-check-input" type="checkbox"
                                 name="btnBen${benefit.id}" value="on"
                                 id="chk-${benefit.id}"
                                 ${isUrgent ? 'checked' : ''} />
                        </div>
                        <span class="benefit-code me-2">(${benefit.planType.code})</span>
                        <span class="flex-grow-1">${benefit.planDescription}</span>
                        <span class="text-end" style="min-width: 100px; font-size: 0.82rem;">
                          <c:if test="${not empty benefit.nextRenewalDue}">
                            <fmt:formatDate value="${benefit.nextRenewalDue}" pattern="MM/dd/yyyy" />
                          </c:if>
                        </span>
                      </div>
                    </c:forEach>
                    <c:if test="${empty benefitsForDisplay}">
                      <div class="text-muted text-center py-3" style="font-size: 0.88rem;">
                        No benefits available for renewal.
                      </div>
                    </c:if>
                  </div>
                  <c:if test="${not empty benefitsForDisplay}">
                    <div class="mt-2 d-flex justify-content-end">
                      <button type="submit" class="btn btn-ssa">
                        <i class="bi bi-arrow-repeat me-1"></i>Start Renewal for Selected Benefits
                      </button>
                    </div>
                  </c:if>
                </form>
              </c:when>
              <c:otherwise>
                <%-- ═══ COLLAPSED STATE ═══ --%>
                <div class="d-flex justify-content-between align-items-center">
                  <div>
                    <span class="employer-name">${emp.employerName}</span>
                    <span class="employer-meta ms-2">
                      <c:if test="${not empty re.lastRenewed}">
                        Last: <fmt:formatDate value="${re.lastRenewed}" pattern="MM/dd/yyyy" />
                      </c:if>
                    </span>
                  </div>
                  <form method="post" action="UpcomingRenewals" class="d-inline">
                    <input type="hidden" name="action" value="selectEmployer" />
                    <input type="hidden" name="employerId" value="${emp.id}" />
                    <button type="submit" class="btn-ssa" style="padding: 0.3rem 0.75rem; font-size: 0.82rem;">
                      <i class="bi bi-chevron-down me-1"></i>View
                    </button>
                  </form>
                </div>
              </c:otherwise>
            </c:choose>
          </div>
        </c:forEach>

        <div class="mb-3"></div>
      </c:forEach>
    </c:otherwise>
  </c:choose>
  </div><%-- /.ur-wrap --%>
</div>

<%-- Auto-scroll to expanded employer --%>
<c:if test="${not empty expandedEmployerId}">
<script>
  document.addEventListener('DOMContentLoaded', function() {
    var el = document.getElementById('employer-${expandedEmployerId}');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  });
</script>
</c:if>
</body>
</html>

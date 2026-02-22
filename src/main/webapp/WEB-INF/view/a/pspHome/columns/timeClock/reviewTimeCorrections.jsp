<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>Review Time Corrections</title>
  <c:set var="pageTitle" value="Time Corrections" scope="request"/>
  <c:set var="pageIcon" value="bi-clock-history" scope="request"/>
  <style>
    .tcr-card {
      background: #fff; border: 1px solid #e2e6ea; border-radius: 10px;
      margin-bottom: 0.75rem; overflow: hidden;
      transition: box-shadow 0.15s ease;
    }
    .tcr-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .tcr-card-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 0.7rem 1rem; background: #f8f9fb;
      border-bottom: 1px solid #e2e6ea;
    }
    .tcr-person { font-weight: 600; font-size: 0.92rem; color: #212529; }
    .tcr-date-requested { font-size: 0.76rem; color: #6c757d; }
    .tcr-body { padding: 0.85rem 1rem; }

    .tcr-time-row {
      display: flex; align-items: center; gap: 0.5rem;
      padding: 0.3rem 0; font-size: 0.88rem;
    }
    .tcr-label { font-weight: 600; color: #495057; width: 65px; }
    .tcr-original { color: #6c757d; }
    .tcr-arrow { color: #198754; font-weight: 700; margin: 0 0.25rem; }
    .tcr-requested { font-weight: 600; color: #198754; }
    .tcr-no-change { color: #adb5bd; font-style: italic; font-size: 0.82rem; }

    .tcr-note {
      margin-top: 0.5rem; padding: 0.5rem 0.7rem;
      background: #f8f9fb; border-radius: 6px; border-left: 3px solid #0d5681;
      font-size: 0.84rem; color: #495057;
    }
    .tcr-note-label { font-weight: 600; font-size: 0.74rem; text-transform: uppercase; color: #6c757d; margin-bottom: 0.15rem; }

    .tcr-actions {
      padding: 0.6rem 1rem; border-top: 1px solid #e2e6ea;
      display: flex; gap: 0.5rem; align-items: flex-start;
    }
    .tcr-comment-input {
      flex: 1; font-size: 0.82rem; resize: none; min-height: 34px;
    }

    .tcr-status-badge {
      font-size: 0.72rem; font-weight: 600; padding: 0.2rem 0.55rem;
      border-radius: 4px; text-transform: uppercase; letter-spacing: 0.03em;
    }
    .tcr-badge-pending  { background: #fff3e0; color: #e65100; }
    .tcr-badge-approved { background: #e8f5e9; color: #2e7d32; }
    .tcr-badge-denied   { background: #fce4ec; color: #c62828; }

    .tcr-review-info {
      font-size: 0.78rem; color: #6c757d; margin-top: 0.5rem;
      padding: 0.4rem 0.7rem; background: #f8f9fb; border-radius: 6px;
    }
    .tcr-review-info strong { color: #495057; }

    .tcr-filter-bar { margin-bottom: 1rem; }
    .tcr-count-badge {
      font-size: 0.7rem; font-weight: 700; background: #e65100; color: #fff;
      border-radius: 10px; padding: 0.1rem 0.45rem; margin-left: 0.3rem;
    }
    .tcr-empty { text-align: center; color: #adb5bd; padding: 2rem 0; font-size: 0.92rem; }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  <div class="row justify-content-center mt-3">
    <div class="col-12 col-lg-8 col-xl-6">

      <%-- ── Filter bar ── --%>
      <div class="tcr-filter-bar d-flex gap-2">
        <a href="ReviewTimeCorrections?statusFilter=PENDING"
           class="btn btn-sm ${statusFilter == 'PENDING' ? 'btn-primary' : 'btn-outline-secondary'}">
          Pending
          <c:if test="${pendingCount > 0}">
            <span class="tcr-count-badge">${pendingCount}</span>
          </c:if>
        </a>
        <a href="ReviewTimeCorrections?statusFilter=APPROVED"
           class="btn btn-sm ${statusFilter == 'APPROVED' ? 'btn-success' : 'btn-outline-secondary'}">
          Approved
        </a>
        <a href="ReviewTimeCorrections?statusFilter=DENIED"
           class="btn btn-sm ${statusFilter == 'DENIED' ? 'btn-danger' : 'btn-outline-secondary'}">
          Denied
        </a>
        <a href="ReviewTimeCorrections?statusFilter=ALL"
           class="btn btn-sm ${statusFilter == 'ALL' ? 'btn-dark' : 'btn-outline-secondary'}">
          All
        </a>
      </div>

      <%-- ── Request cards ── --%>
      <c:choose>
        <c:when test="${not empty correctionRequests}">
          <c:forEach var="tcr" items="${correctionRequests}">
            <div class="tcr-card">

              <%-- Card header --%>
              <div class="tcr-card-header">
                <div>
                  <span class="tcr-person">
                    ${tcr.getRequestor().getFirstName()} ${tcr.getRequestor().getLastName()}
                  </span>
                  <span class="ms-2 tcr-status-badge ${tcr.isPending() ? 'tcr-badge-pending' : (tcr.isApproved() ? 'tcr-badge-approved' : 'tcr-badge-denied')}">
                    ${tcr.getStatus()}
                  </span>
                </div>
                <span class="tcr-date-requested">
                  <fmt:formatDate value="${tcr.getOriginalDate()}" pattern="EEE, MMM d" />
                </span>
              </div>

              <%-- Card body --%>
              <div class="tcr-body">

                <%-- Clock In row --%>
                <div class="tcr-time-row">
                  <span class="tcr-label">Clock In:</span>
                  <span class="tcr-original">
                    <fmt:formatDate value="${tcr.getOriginalInTime()}" pattern="h:mm a" />
                  </span>
                  <c:choose>
                    <c:when test="${tcr.isInTimeChanged()}">
                      <span class="tcr-arrow">→</span>
                      <span class="tcr-requested">
                        <fmt:formatDate value="${tcr.getRequestedInTime()}" pattern="h:mm a" />
                      </span>
                    </c:when>
                    <c:otherwise>
                      <span class="tcr-no-change">no change</span>
                    </c:otherwise>
                  </c:choose>
                </div>

                <%-- Clock Out row --%>
                <div class="tcr-time-row">
                  <span class="tcr-label">Clock Out:</span>
                  <c:choose>
                    <c:when test="${tcr.getOriginalOutTime() != null}">
                      <span class="tcr-original">
                        <fmt:formatDate value="${tcr.getOriginalOutTime()}" pattern="h:mm a" />
                      </span>
                    </c:when>
                    <c:otherwise>
                      <span class="tcr-no-change">none</span>
                    </c:otherwise>
                  </c:choose>
                  <c:choose>
                    <c:when test="${tcr.isOutTimeChanged()}">
                      <span class="tcr-arrow">→</span>
                      <span class="tcr-requested">
                        <fmt:formatDate value="${tcr.getRequestedOutTime()}" pattern="h:mm a" />
                      </span>
                    </c:when>
                    <c:when test="${tcr.getOriginalOutTime() != null}">
                      <span class="tcr-no-change">no change</span>
                    </c:when>
                  </c:choose>
                </div>

                <%-- Employee note --%>
                <c:if test="${not empty tcr.getRequestNote()}">
                  <div class="tcr-note">
                    <div class="tcr-note-label">Employee Note</div>
                    ${tcr.getRequestNote()}
                  </div>
                </c:if>

                <%-- Review info (for resolved requests) --%>
                <c:if test="${!tcr.isPending() && tcr.getReviewer() != null}">
                  <div class="tcr-review-info">
                    <strong>${tcr.isApproved() ? 'Approved' : 'Denied'}</strong> by
                    ${tcr.getReviewer().getFirstName()} ${tcr.getReviewer().getLastName()}
                    on <fmt:formatDate value="${tcr.getDateReviewed()}" pattern="M/d/yyyy h:mm a" />
                    <c:if test="${not empty tcr.getReviewComment()}">
                      <br><em>"${tcr.getReviewComment()}"</em>
                    </c:if>
                  </div>
                </c:if>
              </div>

              <%-- Action buttons (pending only) --%>
              <c:if test="${tcr.isPending()}">
                <form method="post" action="ReviewTimeCorrections">
                  <input type="hidden" name="requestId" value="${tcr.getId()}">
                  <input type="hidden" name="statusFilter" value="${statusFilter}">
                  <div class="tcr-actions">
                    <textarea class="form-control form-control-sm tcr-comment-input"
                              name="reviewComment" placeholder="Comment (optional)..." rows="1"></textarea>
                    <button type="submit" name="action" value="APPROVE" class="btn btn-sm btn-success"
                            title="Approve and apply correction">
                      <i class="bi bi-check-lg"></i> Approve
                    </button>
                    <button type="submit" name="action" value="DENY" class="btn btn-sm btn-outline-danger"
                            title="Deny this request">
                      <i class="bi bi-x-lg"></i> Deny
                    </button>
                  </div>
                </form>
              </c:if>

            </div>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <div class="tcr-empty">
            <i class="bi bi-check-circle" style="font-size:1.5rem;"></i><br>
            No ${statusFilter == 'ALL' ? '' : statusFilter.toLowerCase()} correction requests
          </div>
        </c:otherwise>
      </c:choose>

    </div>
  </div>
</div>
</body>
</html>

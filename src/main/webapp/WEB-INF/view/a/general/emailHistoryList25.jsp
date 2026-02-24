<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"/>
  <title>Email History — ${requestScope.emailHistoryAddress}</title>
  <style>
    .eh-card { max-width: 900px; margin: 0 auto; }
    .eh-row {
      display: flex;
      align-items: center;
      padding: 0.5rem 0.75rem;
      border-bottom: 1px solid #eee;
      font-size: 0.82rem;
      transition: background 0.12s;
    }
    .eh-row:hover { background: #f8f9fb; }
    .eh-date { width: 100px; flex-shrink: 0; color: #6c757d; }
    .eh-from { width: 140px; flex-shrink: 0; font-weight: 600; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .eh-subject { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #0d5681; }
    .eh-action { flex-shrink: 0; margin-left: 0.5rem; }
    .eh-empty { text-align: center; padding: 3rem 1rem; color: #6c757d; font-style: italic; font-size: 0.88rem; }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:set var="pageTitle" value="Email History" scope="request"/>
  <c:set var="pageIcon" value="bi-envelope-paper" scope="request"/>
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  <div class="eh-card mt-3 mb-4">
    <div class="card border-0 shadow-sm">

      <%-- Header --%>
      <div class="hdr-bar d-flex align-items-center justify-content-between">
        <span>
          <i class="bi bi-envelope-paper me-2"></i>${requestScope.emailHistoryAddress}
        </span>
        <span style="font-size: 0.75rem; opacity: 0.8;">
          ${requestScope.emailHistoryCount} email<c:if test="${requestScope.emailHistoryCount != 1}">s</c:if>
        </span>
      </div>

      <%-- Column headers --%>
      <c:if test="${not empty requestScope.emailHistoryList}">
        <div class="d-flex px-3 py-1 border-bottom" style="font-size: 0.72rem; color: #999; font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em;">
          <div style="width: 100px;">Date</div>
          <div style="width: 140px;">From</div>
          <div style="flex: 1;">Subject</div>
          <div style="width: 50px;"></div>
        </div>
      </c:if>

      <%-- Email rows --%>
      <div class="overflow-auto" style="max-height: 600px;">
        <c:choose>
          <c:when test="${empty requestScope.emailHistoryList}">
            <div class="eh-empty">
              <i class="bi bi-inbox me-1"></i>No emails found for this address.
            </div>
          </c:when>
          <c:otherwise>
            <c:forEach var="email" items="${requestScope.emailHistoryList}">
              <a class="eh-row text-decoration-none" href="ViewEmail?id=${email.getId()}" target="_blank">
                <div class="eh-date">
                  <fmt:formatDate value="${email.getDateGenerated()}" pattern="MMM dd, yyyy"/>
                </div>
                <div class="eh-from">
                  <c:choose>
                    <c:when test="${email.getCreatedBy() != null}">
                      ${email.getCreatedBy().getFirstName()} ${email.getCreatedBy().getLastName()}
                    </c:when>
                    <c:otherwise>
                      <span class="text-muted">Unknown</span>
                    </c:otherwise>
                  </c:choose>
                </div>
                <div class="eh-subject">
                  <c:choose>
                    <c:when test="${not empty email.getSubject()}">${email.getSubject()}</c:when>
                    <c:otherwise><span class="text-muted fst-italic">(no subject)</span></c:otherwise>
                  </c:choose>
                </div>
                <div class="eh-action">
                  <i class="bi bi-box-arrow-up-right" style="font-size: 0.72rem; color: #adb5bd;"></i>
                </div>
              </a>
            </c:forEach>
          </c:otherwise>
        </c:choose>
      </div>

    </div>
  </div>
</div>
</body>
</html>

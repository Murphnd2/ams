<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"/>
  <title>View Email</title>
  <style>
    .email-card {
      max-width: 900px;
      margin: 0 auto;
    }
    .email-meta {
      font-size: 0.82rem;
      padding: 0.45rem 0.75rem;
      border-bottom: 1px solid #eee;
    }
    .email-meta-label {
      color: #6c757d;
      font-weight: 600;
      min-width: 90px;
      display: inline-block;
    }
    .email-chip {
      display: inline-flex;
      align-items: center;
      background: #e8f4fd;
      border-radius: 12px;
      padding: 0.15rem 0.55rem;
      font-size: 0.78rem;
      color: #0d5681;
      margin: 0.1rem 0.15rem;
    }
    .email-chip i { margin-right: 0.3rem; font-size: 0.7rem; }
    .attach-chip {
      display: inline-flex;
      align-items: center;
      background: #f0f0f0;
      border-radius: 12px;
      padding: 0.15rem 0.55rem;
      font-size: 0.78rem;
      color: #333;
      margin: 0.1rem 0.15rem;
      text-decoration: none;
      transition: background 0.15s;
    }
    .attach-chip:hover { background: #e0e0e0; color: #333; text-decoration: none; }
    .attach-chip i { margin-right: 0.3rem; font-size: 0.7rem; }
    .email-body {
      padding: 1rem;
      font-size: 0.88rem;
      line-height: 1.6;
      min-height: 200px;
    }
    .email-body img { max-width: 100%; height: auto; }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:set var="pageTitle" value="View Email" scope="request"/>
  <c:set var="pageIcon" value="bi-envelope-open" scope="request"/>
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  <c:choose>
    <c:when test="${empty requestScope.viewEmail}">
      <div class="text-center text-muted fst-italic py-5" style="font-size: 0.9rem;">
        <i class="bi bi-exclamation-circle me-1"></i>Email not found.
      </div>
    </c:when>
    <c:otherwise>
      <c:set var="em" value="${requestScope.viewEmail}"/>

      <div class="email-card mt-3 mb-4">
        <div class="card border-0 shadow-sm">

          <%-- Header --%>
          <div class="hdr-bar d-flex align-items-center justify-content-between">
            <span><i class="bi bi-envelope-open me-2"></i>${em.getSubject()}</span>
            <span style="font-size: 0.75rem; opacity: 0.8;">
              <fmt:formatDate value="${em.getDateCreated()}" pattern="MMM dd, yyyy @ hh:mm aa"/>
            </span>
          </div>

          <%-- From --%>
          <div class="email-meta">
            <span class="email-meta-label"><i class="bi bi-send me-1"></i>From</span>
            <span class="fw-semibold">${em.getCreatedBy().getFullName()}</span>
          </div>

          <%-- Recipients --%>
          <div class="email-meta">
            <span class="email-meta-label"><i class="bi bi-people me-1"></i>To</span>
            <c:choose>
              <c:when test="${empty em.getRecipientList()}">
                <span class="text-muted fst-italic">No recipients</span>
              </c:when>
              <c:otherwise>
                <c:forEach var="r" items="${em.getRecipientList()}">
                  <span class="email-chip">
                    <i class="bi bi-person-fill"></i>${r.getFirstName()} ${r.getLastName()}
                    <c:if test="${r.getEmail() != null}">
                      <span class="ms-1 text-muted" style="font-size: 0.72rem;">${r.getEmail().toLowerCase()}</span>
                    </c:if>
                  </span>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </div>

          <%-- Attachments (only if present) --%>
          <c:if test="${not empty em.getWebLinkList()}">
            <div class="email-meta">
              <span class="email-meta-label"><i class="bi bi-paperclip me-1"></i>Files</span>
              <c:forEach var="att" items="${em.getWebLinkList()}">
                <c:choose>
                  <c:when test="${att.getLinkType().getId() == 1}">
                    <a class="attach-chip" href="${pageContext.request.contextPath}/ShowFileUpload?doc=${att.getLinkPath()}" target="_blank">
                      <i class="bi bi-file-earmark-arrow-down"></i>${att.getPlainText()}
                    </a>
                  </c:when>
                  <c:when test="${att.getLinkType().getId() == 2}">
                    <a class="attach-chip" href="${att.getLinkPath()}" target="_blank">
                      <i class="bi bi-link-45deg"></i>${att.getPlainText()}
                    </a>
                  </c:when>
                </c:choose>
              </c:forEach>
            </div>
          </c:if>

          <%-- Body --%>
          <div class="email-body">
            ${em.getDetail()}
          </div>

        </div>
      </div>

    </c:otherwise>
  </c:choose>
</div>
</body>
</html>

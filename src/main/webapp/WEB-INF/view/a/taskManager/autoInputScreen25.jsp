<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${applicationScope.global.getPsp().getFullName()} — Automation Inputs</title>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

  <div class="row mt-3 mb-4">
    <div class="col-xl-8 offset-xl-2 col-lg-10 offset-lg-1">

      <div class="card border-0 shadow-sm">
        <div class="hdr-bar d-flex justify-content-between align-items-center">
          <span><i class="bi bi-lightning-charge-fill me-2" style="color:#fd7e14;"></i>${sessionScope.a1autoName}</span>
          <span class="small opacity-75"><i class="bi bi-1-circle me-1"></i>Step 1 of 2 — Fill inputs</span>
        </div>
        <div class="card-body">
          <p class="text-muted mb-3" style="font-size:0.85rem;">
            <i class="bi bi-info-circle me-1"></i>Complete the fields below. You'll review and edit the final email before it's sent.
          </p>

          <form method="post" action="PrepareAutoPreview25" id="autoInputForm">
            <input type="hidden" name="csrf" value="${sessionScope.csrfToken}" />

            <c:forEach var="input" items="${sessionScope.a1inputLabels}" varStatus="loop">
              <div class="mb-3">
                <label class="form-label text-ssa fw-bold" for="aInput-${loop.index}" style="font-size:0.85rem;">
                  <c:choose>
                    <c:when test="${sessionScope.a1inputTypes[loop.index] == 'TO'}">
                      <i class="bi bi-envelope-at me-1"></i>
                    </c:when>
                    <c:when test="${sessionScope.a1inputTypes[loop.index] == 'LINK'}">
                      <i class="bi bi-link-45deg me-1"></i>
                    </c:when>
                    <c:when test="${sessionScope.a1inputTypes[loop.index] == 'CC'}">
                      <i class="bi bi-people me-1"></i>
                    </c:when>
                    <c:otherwise>
                      <i class="bi bi-pencil me-1"></i>
                    </c:otherwise>
                  </c:choose>
                  ${fn:escapeXml(input.replaceAll('<[^>]*>', ''))}
                </label>
                <c:choose>
                  <c:when test="${sessionScope.a1inputTypes[loop.index] == 'TO'}">
                    <input type="email"
                           class="form-control"
                           name="aInput-${loop.index}"
                           id="aInput-${loop.index}"
                           placeholder="recipient@example.com"
                           required>
                  </c:when>
                  <c:when test="${sessionScope.a1inputTypes[loop.index] == 'LINK'}">
                    <input type="url"
                           class="form-control"
                           name="aInput-${loop.index}"
                           id="aInput-${loop.index}"
                           placeholder="https://..."
                           required>
                  </c:when>
                  <c:otherwise>
                    <input type="text"
                           class="form-control"
                           name="aInput-${loop.index}"
                           id="aInput-${loop.index}"
                           required>
                  </c:otherwise>
                </c:choose>
              </div>
            </c:forEach>

            <div class="d-flex justify-content-end gap-2 pt-2">
              <a class="btn btn-outline-ssa" href="ViewActivity25">
                <i class="bi bi-x-lg me-1"></i>Cancel
              </a>
              <button type="submit" class="btn btn-ssa">
                <i class="bi bi-eye me-1"></i>Preview Email
              </button>
            </div>
          </form>
        </div>
      </div>

    </div>
  </div>
</div>
</body>
</html>

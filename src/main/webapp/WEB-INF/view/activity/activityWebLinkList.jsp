<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row">
  <div class="col">
    <c:forEach var="webLink" items="${sessionScope.activityWebLinkList}" varStatus="wlStat">
      <c:choose>
        <c:when test="${webLink.getLinkType().getId()==1 || webLink.getLinkType().getId()==2}">
          <div class="row mb-1">
            <div class="col-auto text-primary">
              <c:choose>
                <c:when test="${webLink.getLinkType().getId()==1}">
                  <i class="bi bi-download"></i>
                </c:when>
                <c:otherwise>
                  <i class="bi bi-link"></i>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="col">
              <a class="link-primary text-capitalize" href="${webLink.getActivityHref()}" target="_blank">
                ${webLink.getPlainText().toLowerCase()}
              </a>
            </div>
          </div>
        </c:when>
        <c:otherwise>&nbsp;</c:otherwise>
      </c:choose>
    </c:forEach>
  </div>
</div>
<div class="row">
  <div class="col"></div>
  <div class="col-auto">
    <div class="row">
      <div class="col">
        <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#addUrlAct">
          <i class="bi bi-link"></i> Add Link</button>
      </div>
      <div class="col-auto">
        <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#addDocAct">
          <i class="bi bi-upload"></i> Add Document</button>
      </div>
    </div>
  </div>
</div>


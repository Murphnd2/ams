<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="container-fluid overflow-auto" style="max-height: 700px;">
<c:forEach var="note" items="${sessionScope.activityHistory}">
  <div class="row border-top border-2 border-dark bg-light pt-1 pb-0 mb-0">
    <div class="col form-label text-primary fw-bold" style="font-size: small">
      <c:choose>
        <c:when test="${note.getClass().getSimpleName()==\"Email\"}">
          <a href="ViewEmail?id=${note.getId()}" target="_blank">
              ${note.getReasonCreated().getDescription()} (View)
          </a>
        </c:when>
        <c:otherwise>
          ${note.getReasonCreated().getDescription()}
        </c:otherwise>
      </c:choose>
    </div>
    <div class="col-auto form-label" style="font-size: small">
        ${note.getStatus().getDescription()}
    </div>
  </div>
  <div class="row border-bottom bg-light mt-0 pt-0">
    <div class="col">
      &nbsp;
    </div>
    <div class="col-auto text-muted fst-italic form-label" style="font-size: xx-small">
        ${note.getCreatedBy().getFullName()} on <fmt:formatDate value="${note.getDateCreated()}" pattern="MM/dd/yy @ hh:mm aa"></fmt:formatDate>
    </div>
  </div>
  <div class="row pb-2">
    <div class="col" style="font-size: small">
          ${note.getDetail()}
    </div>
  </div>
</c:forEach>
</div>
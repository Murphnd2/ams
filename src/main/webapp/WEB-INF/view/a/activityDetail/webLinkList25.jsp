<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="penone" value=""></c:set>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==true}">
  <c:set var="penone" value="pe-none"></c:set>
</c:if>
<c:forEach var="webLink" items="${sessionScope.local.getCurrentActivity().getActivity().getWebLinkList()}" varStatus="wlStat">
  <div class="row mb-1">
    <div class="col">
      <c:choose>
        <c:when test="${webLink.linkType.id == 1}">
          <a class="btn btn-sm btn-outline-secondary w-100"
             href="${pageContext.request.contextPath}/ShowFileUpload?doc=${webLink.linkPath}"
             target="_blank">
            <i class="bi bi-download"></i> ${webLink.plainText.toUpperCase()}
          </a>
        </c:when>
        <c:when test="${webLink.linkType.id == 2}">
          <a class="btn btn-sm btn-outline-dark w-100"
             href="${webLink.linkPath}"
             target="_blank">
            <i class="bi bi-link"></i> ${webLink.plainText.toLowerCase()}
          </a>
        </c:when>
        <c:otherwise>
          &nbsp;
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</c:forEach>

<div class="row">
  <div class="col">
    <button type="button" class="btn btn-sm btn-dark w-100 ${penone}" data-bs-toggle="modal" data-bs-target="#addUrlAct">
      <i class="bi bi-link"></i> Add Link
    </button>
  </div>
  <div class="col">
    <button type="button" class="btn btn-sm btn-secondary w-100 ${penone}" data-bs-toggle="modal" data-bs-target="#addDocAct">
      <i class="bi bi-upload"></i> Add Document
    </button>
  </div>
</div>



<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row mb-1 mt-4">
  <div class="col">
    <div class="btn btn-warning btn-sm pe-none w-100">Future Items</div>
  </div>
</div>
<form method="post" action="ChecklistAction25">
  <div class="row mb-1">
    <div class="col w-100">
      <div class="input-group input-group-sm">
        <c:choose>
          <c:when test="${sessionScope.local.getChecklistsFuture().size()==0}">
            <button type="button" disabled class="btn btn-warning"><i class="bi bi-emoji-dizzy"></i></button>
          </c:when>
          <c:otherwise>
            <button type="submit" class="btn btn-outline-secondary">
              <i class="bi bi-arrows-fullscreen"></i>
            </button>
          </c:otherwise>
        </c:choose>
        <select class="form-select" aria-label="recurring freq type drop down" name="btnCheckList" id="btnCheckList">
          <c:choose>
            <c:when test="${sessionScope.local.getChecklistsFuture().size()==0}">
              <option value="-1">NO FUTURE TODOS RIGHT NOW</option>
            </c:when>
            <c:otherwise>
              <c:forEach var="checklist" items="${sessionScope.local.getChecklistsFuture()}">
                <option value="V-${checklist.getActivity().getId()}">${checklist.getName()} (<fmt:formatDate value="${checklist.getDueDate()}" pattern="MM/dd/yy"></fmt:formatDate>)</option></c:forEach>
            </c:otherwise>
          </c:choose>
        </select>
      </div>
    </div>
  </div>
</form>

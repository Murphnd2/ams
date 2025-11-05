<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-1 mt-4">
  <div class="col">
    <div class="btn btn-warning btn-sm pe-none w-100">Future Items</div>
  </div>
</div>
<form method="post" action="ViewSelectedChecklist">
  <div class="row mb-1">
    <div class="col w-100">
      <div class="input-group input-group-sm">
        <c:choose>
          <c:when test="${sessionScope.remainingChecklists.size()==0}">
            <button type="button" disabled class="btn btn-warning"><i class="bi bi-emoji-dizzy"></i></button>
          </c:when>
          <c:otherwise>
            <button type="submit" class="btn btn-outline-secondary">
              <i class="bi bi-arrows-fullscreen"></i>
            </button>
          </c:otherwise>
        </c:choose>
        <c:import url="/WEB-INF/view/activity/checklist/component/ddRemainingChecklistsV1.jsp"></c:import>
      </div>
    </div>
  </div>
</form>

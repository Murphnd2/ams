<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AssignBenefitToRenewal">
  <div class="input-group input-group-sm mb-1">
    <span class="input-group-text">Assign Benefit</span>
    <c:import url="/WEB-INF/view/activity/renew/components/ddBensNotInRenewal.jsp"></c:import>
    <c:choose>
      <c:when test="${sessionScope.benefitsNotInRenewal.size()==0}">
        <button type="button" class="btn btn-secondary pe-none"><i class="bi bi-plus-square"></i></button>
      </c:when>
      <c:otherwise>
        <button type="submit" class="btn btn-secondary">
          <i class="bi bi-plus"></i>
        </button>
      </c:otherwise>
    </c:choose>
  </div>
</form>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddRenewalContact">
  <div class="input-group input-group-sm mb-1">
    <span class="input-group-text">Assign Employee</span>
    <c:import url="/WEB-INF/view/activity/renew/components/ddContactsNotAssigned.jsp"></c:import>
    <c:choose>
      <c:when test="${sessionScope.remainingEmployees.size()==0}">
        <button type="button" disabled class="btn btn-outline-danger">
          <i class="bi bi-plus"></i>
        </button>
      </c:when>
      <c:otherwise>
        <button type="submit" class="btn btn-primary">
          <i class="bi bi-plus"></i>
        </button>
      </c:otherwise>
    </c:choose>
  </div>
</form>

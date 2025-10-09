<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row">
  <div class="col">
    <c:choose>
      <c:when test="${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName().equals(\"Setup\")}">
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailSetup.jsp"></c:import>
      </c:when>
      <c:when test="${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName().equals(\"Renewal\")}">
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailRenewal.jsp"></c:import>
      </c:when>
      <c:otherwise>
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailTicket.jsp"></c:import>
      </c:otherwise>
    </c:choose>
  </div>
</div>

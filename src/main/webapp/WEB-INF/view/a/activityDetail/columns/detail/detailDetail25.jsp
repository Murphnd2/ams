<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row">
  <div class="col">
    <c:choose>
      <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Setup\")}">
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailSetup25.jsp"></c:import>
      </c:when>
      <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Renewal\")}">
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailRenewal25.jsp"></c:import>
      </c:when>
      <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">

      </c:when>
      <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Opportunity\")}">
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailOpportunity25.jsp"></c:import>
      </c:when>
      <c:otherwise>
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailTicket25.jsp"></c:import>
      </c:otherwise>
    </c:choose>
  </div>
</div>

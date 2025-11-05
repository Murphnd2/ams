<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
  <c:when test="${sessionScope.aFilter.equals(\"Renewal\")}">
    <c:set var="aColor" value="btn-outline-warning pe-auto"></c:set>
    <c:set var="rColor" value="btn-primary pe-none"></c:set>
    <c:set var="sColor" value="btn-outline-secondary pe-auto"></c:set>
    <c:set var="tColor" value="btn-outline-info pe-auto"></c:set>
    <c:set var="onUsColor" value="btn-outline-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-outline-danger pe-auto"></c:set>
  </c:when>
  <c:when test="${sessionScope.aFilter.equals(\"Setup\")}">
    <c:set var="aColor" value="btn-outline-warning pe-auto"></c:set>
    <c:set var="rColor" value="btn-outline-primary pe-auto"></c:set>
    <c:set var="sColor" value="btn-secondary pe-none"></c:set>
    <c:set var="tColor" value="btn-outline-info pe-auto"></c:set>
    <c:set var="onUsColor" value="btn-outline-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-outline-danger pe-auto"></c:set>
  </c:when>
  <c:when test="${sessionScope.aFilter.equals(\"Ticket\")}">
    <c:set var="aColor" value="btn-outline-warning pe-auto"></c:set>
    <c:set var="rColor" value="btn-outline-primary pe-auto"></c:set>
    <c:set var="sColor" value="btn-outline-secondary pe-auto"></c:set>
    <c:set var="tColor" value="btn-info pe-none"></c:set>
    <c:set var="onUsColor" value="btn-outline-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-outline-danger pe-auto"></c:set>
  </c:when>
  <c:when test="${sessionScope.aFilter.equals(\"OnUs\")}">
    <c:set var="aColor" value="btn-outline-warning pe-auto"></c:set>
    <c:set var="rColor" value="btn-outline-primary pe-auto"></c:set>
    <c:set var="sColor" value="btn-outline-secondary pe-auto"></c:set>
    <c:set var="tColor" value="btn-outline-info pe-auto"></c:set>
    <c:set var="onUsColor" value="btn-light pe-none"></c:set>
    <c:set var="fColor" value="btn-outline-danger pe-auto"></c:set>
  </c:when>
  <c:when test="${sessionScope.aFilter.equals(\"FollowUp\")}">
    <c:set var="aColor" value="btn-outline-warning pe-auto"></c:set>
    <c:set var="rColor" value="btn-outline-primary pe-auto"></c:set>
    <c:set var="sColor" value="btn-outline-secondary pe-auto"></c:set>
    <c:set var="tColor" value="btn-outline-info pe-auto"></c:set>
    <c:set var="onUsColor" value="btn-outline-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-danger pe-none"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="aColor" value="btn-warning pe-none"></c:set>
    <c:set var="rColor" value="btn-outline-primary pe-auto"></c:set>
    <c:set var="sColor" value="btn-outline-secondary pe-auto"></c:set>
    <c:set var="tColor" value="btn-outline-info pe-auto"></c:set>
    <c:set var="onUsColor" value="btn-outline-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-outline-danger pe-auto"></c:set>
  </c:otherwise>
</c:choose>
<div class="p-2 pt-1 border border-dark rounded w-100 mt-2 mb-2 text-light bg-dark fw-bold fs-3 align-items-center text-center">
  <div class="row m-0 p-0 align-items-center align-middle">
    <div class="col-auto m-0 p-0 me-2 align-items-center text-center">
      <a class="btn btn-sm fs-6 ${aColor} " href="ShowAllActivities">
        ALL
      </a>
    </div>
    <div class="col-auto m-0 me-2 p-0">
      <a class="btn btn-sm ${rColor} fs-6 " href="ShowRenewalActivities">
        <i class="bi bi-repeat"></i> R
      </a>
    </div>
    <div class="col-auto m-0 me-2 p-0">
      <a class="btn btn-sm ${sColor} fs-6 " href="ShowSetupActivities">
        <i class="bi bi-building"></i> S
      </a>
    </div>
    <div class="col-auto m-0 me-2 p-0">
      <a class="btn btn-sm ${tColor} fs-6 " href="ShowTicketActivities">
        <i class="bi bi-ticket-detailed"></i> T
      </a>
    </div>
      <c:if test="${sessionScope.onUsCount >0}">
        <div class="col-auto m-0 me-2 p-0">
          <a class="btn btn-sm ${onUsColor} fs-6 " href="ShowOnUsActivities">
            <i class="bi bi-stack-overflow"></i> ${sessionScope.onUsCount}
          </a>
        </div>
      </c:if>
    <c:if test="${sessionScope.followCount > 0}">
      <div class="col-auto m-0 p-0">
        <a class="btn btn-sm ${fColor} fs-6 " href="ShowFollowUps">
          <i class="bi bi-phone"></i> ${sessionScope.followCount}
        </a>
      </div>
    </c:if>
    <div class="col"></div>
    <div class="col-auto m-0 me-1 p-0">
      ${sessionScope.currentPerson.getFirstName()}
    </div>

  </div>
</div>
<div class="row overflow-auto" style="height: 700px">
  <div class="col">
    <c:import url="/WEB-INF/view/activity/activityList.jsp"></c:import>
  </div>
</div>

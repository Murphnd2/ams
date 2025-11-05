<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
  <c:when test="${sessionScope.onUs==1}">
    <c:set var="onUsColor" value="btn-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-outline-danger pe-auto"></c:set>
  </c:when>
  <c:when test="${sessionScope.followUp==1}">
    <c:set var="onUsColor" value="btn-outline-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-danger pe-auto"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="onUsColor" value="btn-outline-light pe-auto"></c:set>
    <c:set var="fColor" value="btn-outline-danger pe-auto"></c:set>
  </c:otherwise>
</c:choose>
<c:choose>
  <c:when test="${sessionScope.vR.equals(\"ON\")}">
    <c:set var="rColor" value="btn-primary pe-auto"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="rColor" value="btn-outline-primary pe-auto"></c:set>
  </c:otherwise>
</c:choose>
<c:choose>
  <c:when test="${sessionScope.vS.equals(\"ON\")}">
    <c:set var="sColor" value="btn-secondary pe-auto"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="sColor" value="btn-outline-secondary pe-auto"></c:set>
  </c:otherwise>
</c:choose>
<c:choose>
  <c:when test="${sessionScope.vT.equals(\"ON\")}">
    <c:set var="tColor" value="btn-info pe-auto"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="tColor" value="btn-outline-info pe-auto"></c:set>
  </c:otherwise>
</c:choose>
<c:choose>
  <c:when test="${sessionScope.vA.equals(\"ALL\")}">
    <c:set var="aColor1" value="btn-warning pe-none"></c:set>
    <c:set var="aColor2" value="btn-outline-warning pe-auto"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="aColor2" value="btn-warning pe-none"></c:set>
    <c:set var="aColor1" value="btn-outline-warning pe-auto"></c:set>
  </c:otherwise>
</c:choose>
<div class="p-2 pt-1 border border-dark rounded w-100 mt-2 mb-2 text-light bg-dark fw-bold fs-3 align-items-center text-center">
  <div class="row m-0 p-0 align-items-center align-middle">
    <div class="col-auto m-0 me-3 p-0 align-items-center text-center">
      <div class="btn-group" role="group" aria-label="AllOrMine">
        <a class="btn btn-sm fs-6 ${aColor1} " href="ShowAllActivities">
          <i class="bi bi-people"></i>
        </a>
        <a class="btn btn-sm fs-6 ${aColor2} " href="ShowAllActivities">
          <i class="bi bi-person"></i>
        </a>
      </div>
    </div>
    <div class="col-auto m-0 me-3 p-0">
      <div class="btn-group" role="group">
        <a class="btn btn-sm ${rColor} fs-6 " href="ShowRenewalActivities">
          <i class="bi bi-repeat"></i> R
        </a>
        <a class="btn btn-sm ${sColor} fs-6 " href="ShowSetupActivities">
          <i class="bi bi-building"></i> S
        </a>
        <a class="btn btn-sm ${tColor} fs-6 " href="ShowTicketActivities">
          <i class="bi bi-ticket-detailed"></i> T
        </a>
        <%--<c:if test="${sessionScope.isPspAdmin && sessionScope.onlyPast.equals(\"Y\")}">
          <a class="btn btn-sm fs-6 btn-light" href="OnlyPastDue">
            <i class="bi bi-calendar3-event"></i>
          </a>
        </c:if>
        <c:if test="${sessionScope.isPspAdmin && !sessionScope.onlyPast.equals(\"Y\")}">
          <a class="btn btn-sm fs-6 btn-outline-light" href="OnlyPastDue">
            <i class="bi bi-calendar3-event"></i>
          </a>
        </c:if>--%>
      </div>
    </div>
    <c:if test="${sessionScope.vR==\"ON\" && sessionScope.vS==\"OFF\" && sessionScope.vT==\"OFF\"}">
      <div class="col-auto m-0 me-3 p-0">
        <div class="btn-group" role="group">
          <a class="btn btn-sm btn-outline-light fs-6 " href="ShowCobraRenewals">
            C
          </a>
          <a class="btn btn-sm btn-outline-light fs-6 " href="ShowFsaRenewals">
            F
          </a>
          <a class="btn btn-sm btn-outline-light fs-6 " href="ShowHraRenewals">
            H
          </a>
          <a class="btn btn-sm btn-outline-light fs-6 " href="ShowPopRenewals">
            P
          </a>
        </div>
      </div>
    </c:if>
    <div class="col"></div>
    <div class="col-auto m-0 me-1 p-0">
      <div class="btn-group" role="group">
        <c:if test="${sessionScope.onUsCount1 >0}">
            <a class="btn btn-sm ${onUsColor} fs-6 " href="ShowOnUsActivities">
              <i class="bi bi-stack-overflow"></i> ${sessionScope.onUsCount1}
            </a>
        </c:if>
        <c:if test="${sessionScope.followCount1 > 0}">
            <a class="btn btn-sm ${fColor} fs-6 " href="ShowFollowUps">
              <i class="bi bi-phone"></i> ${sessionScope.followCount1}
            </a>
        </c:if>
        <c:choose>
          <c:when test="${sessionScope.sortHow.equals(\"DATE\")}">
              <a class="btn btn-sm fs-6 btn-outline-success" href="ShowSortedByDate">
                <i class="bi bi-sort-alpha-down"></i>
                <i class="bi bi-person"></i>
              </a>
          </c:when>
          <c:otherwise>
              <a class="btn btn-sm fs-6 btn-outline-success" href="ShowSortedByDate">
                <i class="bi bi-sort-down"></i>
                <i class="bi bi-calendar2"></i>
              </a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>




  </div>
</div>
<div class="row overflow-auto" style="height: 700px">
  <div class="col">
    <c:import url="/WEB-INF/view/activity/activityList.jsp"></c:import>
  </div>
</div>

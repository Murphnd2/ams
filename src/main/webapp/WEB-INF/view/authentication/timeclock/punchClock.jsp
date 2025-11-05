<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="PunchClock">
  <div class="d-none" id="codeBehindClock">
    <c:set var="oClass" value="btn-danger pe-none"> </c:set>
    <c:set var="iClass" value="btn-outline-success"> </c:set>
    <c:if test="${sessionScope.userStatus==true}">
      <c:set var="oClass" value="btn-outline-danger"> </c:set>
      <c:set var="iClass" value="btn-success pe-none"> </c:set>
    </c:if>
  </div>
  <div class="row">
    <div class="col">
      <div class="btn btn-outline-dark border-0 w-100 pe-none">
        <c:choose>
          <c:when test="${sessionScope.userStatus==true && sessionScope.myTimeList.size()==0}">
            IN Since Yesterday
          </c:when>
          <c:when test="${sessionScope.userStatus==true}">
            IN Since <fmt:formatDate value="${sessionScope.myTimeList.get(0).getInTime()}" pattern="hh:mm aa"></fmt:formatDate>
          </c:when>
          <c:when test="${sessionScope.userStatus==false && sessionScope.myTimeList.size()==0}">
            OUT Since Yesterday
          </c:when>
          <c:otherwise>
            OUT Since <fmt:formatDate value="${sessionScope.myTimeList.get(0).getOutTime()}" pattern="hh:mm aa"></fmt:formatDate>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
  <div class="row mt-1">
    <div class="col-1"></div>
    <div class="col-5">
      <button type="submit" class="btn ${oClass} w-100" name="btnPunch" value="0">
        <i class="bi bi-moon-stars"></i>&nbsp;
        Out
      </button>
    </div>
    <div class="col-5">
      <button type="submit" class="btn ${iClass} w-100" name="btnPunch" value="1">
        <i class="bi bi-sun"></i>&nbsp;
        In
      </button>
    </div>
    <div class="col-1"></div>
  </div>
</form>
<hr>
<div class="row mt-3">
  <div class="col-5 text-center">
    Date
  </div>
  <div class="col-7 text-center">
    <div class="row m-0 p-0">
      <div class="col-6 m-0 p-0 text-center">
        Time In
      </div>
      <div class="col-6 m-0 p-0 text-center">
        Time Out
      </div>
    </div>
  </div>
</div>

<c:forEach var="timeStretch" items="${sessionScope.myTimeList}" varStatus="loop">
  <c:set var="rowInd" value="${(loop.count + 2) % 2}"></c:set>
  <c:set var="bgColor" value="text-primary bg-light"></c:set>
  <c:if test="${rowInd==1}">
    <c:set var="bgColor" value="text-secondary"></c:set>
  </c:if>
  <div class="row ${bgColor}">
    <div class="col-5 text-center">
      <fmt:formatDate value="${timeStretch.getInDate()}" pattern="E, MMM-dd"></fmt:formatDate>
    </div>
    <div class="col-7 text-center">
      <div class="row m-0 p-0">
        <div class="col-6 m-0 p-0 text-center">
          <fmt:formatDate value="${timeStretch.getInTime()}" pattern="hh:mm aa"></fmt:formatDate>
        </div>
        <div class="col-6 m-0 p-0 text-center">
          <fmt:formatDate value="${timeStretch.getOutTime()}" pattern="hh:mm aa"></fmt:formatDate>
        </div>
      </div>
    </div>
  </div>
</c:forEach>


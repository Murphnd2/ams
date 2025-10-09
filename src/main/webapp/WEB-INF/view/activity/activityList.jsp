
<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="ViewSelectedActivity">
  <c:set var="tMonth" value="${sessionScope.thisMonth}"></c:set>
  <c:set var="lMonth" value="${sessionScope.lastMonth}"></c:set>
  <c:set var="twoMonth" value="${sessionScope.twoMonth}"></c:set>
  <c:forEach var="activity" items="${sessionScope.activityList2}">
    <c:set var="cName" value="${activity.getClass().getSimpleName()}"></c:set>
    <c:set var="dueDate" value="${activity.getDueDate()}"></c:set>
    <c:set var="dueMonth" value="${activity.getDueMonth()}"></c:set>

    <c:choose>
      <c:when test="${activity.getLastDate() < Date.valueOf(LocalDate.now().minusDays(14))}">
        <c:set var="nColor" value="fw-bold text-danger"></c:set>
      </c:when>
      <c:when test="${activity.getLastDate() < Date.valueOf(LocalDate.now().minusDays(7))}">
        <c:set var="nColor" value="fw-bold"></c:set>
      </c:when>
      <c:otherwise>
        <c:set var="nColor" value="text-muted"></c:set>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${activity.getOnUs()}">
        <c:set var="nCaps" value="text-uppercase"></c:set>
        <c:set var="outline" value=""></c:set>
      </c:when>
      <c:otherwise>
        <c:set var="nCaps" value="text-lowercase"></c:set>
        <c:set var="outline" value="-outline"></c:set>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${cName.equals(\"Renewal\")}">
        <c:set var="bType" value="-primary"></c:set>
        <c:set var="iClass" value="repeat"></c:set>
      </c:when>
      <c:when test="${cName.equals(\"Setup\")}">
        <c:set var="bType" value="-secondary"></c:set>
        <c:set var="iClass" value="building"></c:set>
      </c:when>
      <c:when test="${cName.equals(\"Ticket\")}">
        <c:set var="bType" value="-info"></c:set>
        <c:set var="iClass" value="ticket-detailed"></c:set>
      </c:when>
      <c:otherwise>
        <c:set var="bType" value="-dark"></c:set>
      </c:otherwise>
    </c:choose>
    <c:choose>
      <c:when test="${dueMonth <= twoMonth}">
        <c:set var="bgFormat" value="bg-danger text-white fw-bold pe-none"></c:set>
        <c:set var="dFormat" value=""></c:set>
      </c:when>
      <c:when test="${dueMonth <= lMonth}">
        <c:set var="bgFormat" value="btn-warning text-dark pe-none"></c:set>
        <c:set var="dFormat" value=""></c:set>
      </c:when>
      <c:when test="${dueMonth <= tMonth}">
        <c:set var="bgFormat" value="btn-outline-dark pe-none"></c:set>
        <c:set var="dFormat" value=""></c:set>
      </c:when>
      <c:otherwise>
        <c:set var="bgFormat" value="btn-outline${bType} pe-none"></c:set>
        <c:set var="dFormat" value="text-secondary fw-lighter text-lowercase"></c:set>
      </c:otherwise>
    </c:choose>
    <div class="row mb-1">
      <div class="col">
        <div class="input-group input-group-sm">
          <button type="submit" class="btn btn${outline}${bType} btn-sm" id="${activity.getId()}" name="btnViewActivity" value="${activity.getId()}" style="width:21%">
            <i class="bi bi-${iClass}"></i>
              ${activity.getClass().getSimpleName()}
          </button>
          <div class="form-control pe-none">
            <div class="row m-0 p-0">
              <c:choose>
                <c:when test="${activity.getClass().getSimpleName().equals(\"Ticket\") && activity.getContact().getEmployee().getId()!=null &&
                      activity.getContact().getEmployee().getId()>-3}">
                  <div class="col m-0 p-0 ${nCaps} ${nColor} text-truncate">
                      ${activity.getContact().getEmployee().getEmployer().getEmployerName()} (${activity.getFullName()})
                  </div>
                </c:when>
                <c:when test="${activity.getClass().getSimpleName().equals(\"Ticket\")}">
                  <div class="col m-0 p-0 ${nCaps} ${nColor} text-truncate">
                    <c:choose>
                      <c:when test="${activity.getPrimaryContact().getLastName()==null || activity.getPrimaryContact().getLastName().equals(\"\")}">
                        ${activity.getContact().getFirstName().toUpperCase()} ${activity.getContact().getLastName().toUpperCase()}
                      </c:when>
                      <c:otherwise>
                        ${activity.getContact().getFirstName().toUpperCase()} ${activity.getContact().getLastName().toUpperCase()}
                      </c:otherwise>
                    </c:choose>
                  </div>
                </c:when>
                <c:otherwise>
                  <div class="col m-0 p-0 ${nCaps} ${nColor} text-truncate">
                      ${activity.getFullName()}
                  </div>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
          <div class="btn ${bgFormat} d-none d-md-grid text-truncate" style="width:20%"><fmt:formatDate value="${activity.getDueDate()}" pattern="MMM dd, yyyy"></fmt:formatDate>
          </div>
        </div>
      </div>
    </div>
  </c:forEach>
</form>




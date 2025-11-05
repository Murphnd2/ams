<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<c:set var="aColor" value="primary"></c:set>
<c:set var="aIcon" value="repeat"></c:set>
<c:set var="bColor" value="bg-white"></c:set>
<c:set var="cName" value="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()}"></c:set>
<c:if test="${cName.equals(\"Ticket\")}">
  <c:set var="aColor" value="info"></c:set>
  <c:set var="aIcon" value="ticket-detailed"></c:set>
</c:if>
<c:if test="${cName.equals(\"Setup\")}">
  <c:set var="aColor" value="secondary"></c:set>
  <c:set var="aIcon" value="building"></c:set>
</c:if>
<c:if test="${cName.equals(\"CheckList\")}">
  <c:set var="aColor" value="warning"></c:set>
  <c:set var="aIcon" value="check"></c:set>
  <c:set var="bColor" value="bg-dark"></c:set>
</c:if><%----%>
<div class="p-2 pt-0 border border-${aColor} ${bColor} rounded w-100 mt-2 mb-2 fw-bold fs-3 align-items-center text-center position-relative">
  <div class="row m-0 p-0 align-items-center align-middle ">
    <div class="col-auto m-0 p-0">
      <a class="btn btn-${aColor} btn-sm btn-sm pe-auto position-absolute start-0 top-50 translate-middle" href="ViewHome25">
        <i class="bi bi-arrow-return-left"></i>
      </a>
    </div>
    <div class="col m-0 p-0 pt-1 ms-4 fw-bold fs-3 text-${aColor} text-truncate">
      <i class="bi bi-${aIcon}"></i>
      <c:choose>
        <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Ticket\")}">
          <c:choose>
            <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact()!=null && sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee()!=null}">
              ${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getEmployer().getEmployerName()}
            </c:when>
            <c:otherwise>
              ${sessionScope.local.getCurrentActivity().getPrimaryContact().getFirstName().toUpperCase()} ${sessionScope.local.getCurrentActivity().getPrimaryContact().getLastName().toUpperCase()}
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:otherwise>
          ${sessionScope.local.getCurrentActivity().getActivity().getFullName()}
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</div>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==true}">
  <div class="row border rounded bg-warning text-dark-emphasis fw-bold">
    <div class="col text-center">
      Created ${sessionScope.local.getCurrentActivity().getActivity().getDateCreated()} - Closed ${sessionScope.local.getCurrentActivity().getActivity().getDateCompleted()}
    </div>
  </div>
</c:if>
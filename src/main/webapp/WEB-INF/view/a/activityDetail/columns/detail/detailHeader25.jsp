<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<c:set var="cName" value="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()}"/>

<%-- Type-specific icon and badge color --%>
<c:choose>
  <c:when test="${cName == 'Ticket'}">
    <c:set var="aIcon" value="ticket-detailed"/>
    <c:set var="badgeColor" value="#17a2b8"/>
    <c:set var="badgeLabel" value="Ticket"/>
  </c:when>
  <c:when test="${cName == 'Renewal'}">
    <c:set var="aIcon" value="repeat"/>
    <c:set var="badgeColor" value="#0d6efd"/>
    <c:set var="badgeLabel" value="Renewal"/>
  </c:when>
  <c:when test="${cName == 'Setup'}">
    <c:set var="aIcon" value="building"/>
    <c:set var="badgeColor" value="#6c757d"/>
    <c:set var="badgeLabel" value="Setup"/>
  </c:when>
  <c:when test="${cName == 'Opportunity'}">
    <c:set var="aIcon" value="bullseye"/>
    <c:set var="badgeColor" value="#0d6efd"/>
    <c:set var="badgeLabel" value="Opportunity"/>
  </c:when>
  <c:when test="${cName == 'CheckList'}">
    <c:set var="aIcon" value="check2-square"/>
    <c:set var="badgeColor" value="#e5a100"/>
    <c:set var="badgeLabel" value="Checklist"/>
  </c:when>
</c:choose>

<%-- Header bar --%>
<div class="hdr-bar mt-2 mb-0 d-flex align-items-center">
  <a class="btn btn-outline-light btn-sm me-2" href="${sessionScope.isAgent || sessionScope.isAgencyAdmin ? 'AgentHome' : 'ViewHome25'}">
    <i class="bi bi-arrow-return-left"></i>
  </a>
  <i class="bi bi-${aIcon} me-2"></i>
  <span class="text-truncate flex-grow-1">
    <c:choose>
      <c:when test="${cName == 'Ticket'}">
        <c:choose>
          <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact() != null && sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee() != null}">
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
  </span>
  <span class="ms-2 badge rounded-pill" style="background-color: ${badgeColor}; font-size: 0.7rem;">
    ${badgeLabel}
  </span>
</div>

<%-- Driver/category subtitle --%>
<div class="d-flex align-items-center px-2 py-1 bg-light border-start border-end" style="font-size: 0.82rem; border-color: #dee2e6 !important;">
  <c:choose>
    <c:when test="${cName == 'Ticket' && sessionScope.local.getCurrentActivity().getActivity().getTicketSubCategory() != null}">
      <span class="text-muted me-1">Issue:</span>
      <span class="text-capitalize fw-semibold" style="color: var(--ssa);">
          ${sessionScope.local.getCurrentActivity().getActivity().getTicketSubCategory().getDescription().toLowerCase()}
      </span>
    </c:when>
    <c:when test="${cName == 'Renewal'}">
      <span class="text-muted me-1">Due:</span>
      <span class="fw-semibold" style="color: var(--ssa);">
        <fmt:formatDate value="${sessionScope.local.getCurrentActivity().getActivity().getDueDate()}" pattern="MMMM dd, yyyy"/>
      </span>
    </c:when>
    <c:when test="${cName == 'Setup' && sessionScope.local.getCurrentActivity().getActivity().getApplication() != null}">
      <span class="text-muted me-1">Setup for:</span>
      <span class="fw-semibold" style="color: var(--ssa);">
          ${sessionScope.local.getCurrentActivity().getActivity().getFullName()}
      </span>
    </c:when>
    <c:when test="${cName == 'Opportunity'}">
      <span class="text-muted me-1">Prospect:</span>
      <span class="fw-semibold" style="color: var(--ssa);">
          ${sessionScope.local.getCurrentActivity().getActivity().getFullName()}
      </span>
    </c:when>
    <c:otherwise>
      <span class="text-muted">&nbsp;</span>
    </c:otherwise>
  </c:choose>
</div>

<%-- Closed activity banner --%>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()}">
  <div class="d-flex align-items-center justify-content-center px-2 py-1 fw-semibold" style="background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 0 0 6px 6px; font-size: 0.82rem; color: #664d03;">
    <i class="bi bi-lock-fill me-1"></i>
    Created <fmt:formatDate value="${sessionScope.local.getCurrentActivity().getActivity().getDateCreated()}" pattern="MM/dd/yyyy"/>
    &mdash; Closed <fmt:formatDate value="${sessionScope.local.getCurrentActivity().getActivity().getDateCompleted()}" pattern="MM/dd/yyyy"/>
  </div>
</c:if>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="isPast" value="pe-none"/>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete() == false}">
  <c:set var="isPast" value=""/>
</c:if>
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: var(--ssa) !important;">
  <div class="card-body py-2 px-3">
    <div class="d-flex align-items-center justify-content-between mb-1">
      <span class="fw-semibold" style="color: var(--ssa); font-size: 0.85rem;">
        <i class="bi bi-person-fill me-1"></i>Primary Contact
      </span>
      <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 ${isPast}" type="button"
              data-bs-target="#modContactModal1" data-bs-toggle="modal" title="Edit contact">
        <i class="bi bi-pencil-square" style="font-size: 0.8rem;"></i>
      </button>
    </div>
    <div class="d-flex align-items-center flex-wrap" style="font-size: 0.9rem;">
      <span class="fw-bold text-dark text-capitalize me-2">
        <c:choose>
          <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact() != null && sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee() != null}">
            ${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getFirstName().toLowerCase()}
            ${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getLastName().toLowerCase()}
          </c:when>
          <c:otherwise>
            ${sessionScope.local.getCurrentActivity().getPrimaryContact().getFullName().toLowerCase()}
          </c:otherwise>
        </c:choose>
      </span>
      <c:choose>
        <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee() != null && sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getEmail() != null}">
          <a class="text-muted text-decoration-none" style="font-size: 0.82rem;"
             href="ViewEmailHistory?em=${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getEmail()}" target="_blank">
            <i class="bi bi-envelope me-1"></i>${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getEmail().toLowerCase()}
          </a>
        </c:when>
        <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmail() != null}">
          <a class="text-muted text-decoration-none" style="font-size: 0.82rem;"
             href="ViewEmailHistory?em=${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmail()}" target="_blank">
            <i class="bi bi-envelope me-1"></i>${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmail().toLowerCase()}
          </a>
        </c:when>
      </c:choose>
    </div>
  </div>
</div>
<c:import url="/WEB-INF/view/a/activityDetail/columns/detail/modals/modContact25.jsp"></c:import>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="isPast" value="pe-none"></c:set>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
  <c:set var="isPast" value=""></c:set>
</c:if>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col-auto me-0 pe-0">

        <span class="text-ssa fw-bold fs-5">Primary Contact</span>
      </div>
      <div class="col ms-0 ps-0">
        <button class="btn btn-sm ${isPast}" name="showModConForm1" id="btnShowModConForm12" type="button" data-bs-target="#modContactModal1" data-bs-toggle="modal">
          [edit]
        </button>
      </div>
      <div class="col-auto">

      </div>
    </div><%----%>
    <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/modals/modContact25.jsp"></c:import>
    <div class="row">
      <div class="col-auto">
        <i class="bi bi-arrow-right"></i>
      </div>
      <div class="col">
         <c:choose>
           <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact()!=null && sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee()!=null}">
             <span class="text-altSsa fw-bolder text-uppercase">
                 ${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getFirstName().toLowerCase()}&nbsp;
                 ${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getLastName().toLowerCase()}
             </span>
           </c:when>
          <c:otherwise>
              <span class="text-altSsa fw-bolder text-uppercase">${sessionScope.local.getCurrentActivity().getPrimaryContact().getFullName().toLowerCase()}</span>
          </c:otherwise>
        </c:choose>
      </div><%----%>
      <div class="col-auto">
        <c:choose>
          <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee()!=null && sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getEmail()!=null}">
            <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getEmail()}" target="_blank">
                ${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmployee().getEmail().toLowerCase()}
            </a>
          </c:when>
          <c:when test="${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmail()==null}">
            &nbsp;
          </c:when>
          <c:otherwise>
            <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmail()}" target="_blank">
                ${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmail().toLowerCase()}
            </a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

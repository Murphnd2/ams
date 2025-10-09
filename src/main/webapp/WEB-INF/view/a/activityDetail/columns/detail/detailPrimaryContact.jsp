<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col-auto me-0 pe-0">
        <span class="text-ssa fw-bold fs-5">Primary Contact</span>
      </div>
      <div class="col ms-0 ps-0">
        <button class="btn btn-sm" name="showModConForm1" id="btnShowModConForm12" type="button" data-bs-target="#modContactModal1" data-bs-toggle="modal">
          [edit]
        </button>
      </div>
      <div class="col-auto"></div>
    </div>
    <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/modals/modContactModal.jsp"></c:import>
    <div class="row">
      <div class="col-auto">
        <i class="bi bi-arrow-right"></i>
      </div>
      <div class="col">
        <c:choose>
          <c:when test="${sessionScope.sVar.getPrimaryContactForActivity().getEmployee()!=null}">
            <span class="text-altSsa fw-bolder text-uppercase">
                ${sessionScope.sVar.getPrimaryContactForActivity().getEmployee().getFirstName().toLowerCase()}&nbsp;
                ${sessionScope.sVar.getPrimaryContactForActivity().getEmployee().getLastName().toLowerCase()}
            </span>
          </c:when>
          <c:otherwise>
            <span class="text-altSsa fw-bolder text-uppercase">${sessionScope.sVar.getPrimaryContactForActivity().getFullName().toLowerCase()}</span>
          </c:otherwise>
        </c:choose>
      </div>
      <div class="col-auto">
        <c:choose>
          <c:when test="${sessionScope.sVar.getPrimaryContactForActivity().getEmployee()!=null && sessionScope.sVar.getPrimaryContactForActivity().getEmployee().getEmail()!=null}">
            <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${sessionScope.sVar.getPrimaryContactForActivity().getEmployee().getEmail()}" target="_blank">
                ${sessionScope.sVar.getPrimaryContactForActivity().getEmployee().getEmail().toLowerCase()}
            </a>
          </c:when>
          <c:when test="${sessionScope.sVar.getPrimaryContactForActivity().getEmail()==null}">
            &nbsp;
          </c:when>
          <c:otherwise>
            <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${sessionScope.sVar.getPrimaryContactForActivity().getEmail()}" target="_blank">
                ${sessionScope.sVar.getPrimaryContactForActivity().getEmail().toLowerCase()}
            </a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>
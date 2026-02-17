<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="superUser" value="N"></c:set>
<c:forEach var="role" items="${sessionScope.currentUser.getUserRoleList()}">
  <c:if test="${role.getId()==19}">
    <c:set var="superUser" value="Y"></c:set>
  </c:if>
</c:forEach>
<div class="offcanvas offcanvas-end" tabindex="-1" id="ocAdminMenu" aria-labelledby="offcanvasExampleLabel">
  <div class="offcanvas-header">
    <div class="row m-0 p-0 w-100">
      <div class="col w-100 m-0 p-0">
        <div class="btn btn-ssa pe-none w-100 fw-bold fs-4">ADMIN MENU</div>
      </div>
    </div>
  </div>
  <div class="offcanvas-body">
    <div class="btn btn-altSsa w-100 pe-none text-uppercase fw-bold">Manual Activity Options</div>
    <button class="btn btn-outline-ssa btn-sm w-100 mt-1"  type="button" data-bs-toggle="modal" data-bs-target="#createSetupForm">
      <i class="bi bi-building"></i>
      Create A Setup</button>
    <button class="btn btn-outline-ssa btn-sm w-100 mt-1"  type="button" data-bs-toggle="modal" data-bs-target="#createBlankRenewal">
      <i class="bi bi-repeat"></i>
      Create Empty Renewal</button>
    <button class="btn btn-outline-ssa btn-sm w-100 mt-1"  type="button" data-bs-toggle="modal" data-bs-target="#addRenewalModal">
      <i class="bi bi-repeat"></i>
      Upcoming Renewals</button>
    <a class="btn btn-outline-ssa btn-sm w-100 mt-1"  href="GoTicketTemplate25">
      <i class="bi bi-list-task"></i>
      Manage Task Templates
    </a>
    <%--
    <button class="btn btn-outline-ssa btn-sm w-100  mt-1"  type="button" data-bs-dismiss="offcanvas" data-bs-toggle="modal" data-bs-target="#addInsertLinkModal">
      <i class="bi bi-link"></i>
      Create Insert Link</button>
      --%>
    <button class="btn btn-outline-ssa btn-sm w-100  mt-1"  type="button" data-bs-dismiss="offcanvas" data-bs-toggle="modal" data-bs-target="#createUserModal">
      <i class="bi bi-person-plus-fill"></i>&nbsp;Create&nbsp;User
    </button>





    <div class="btn btn-altSsa w-100  mt-2 pe-none text-uppercase fw-bold">
      <i class="bi bi-cash"></i>&nbsp;
      Sales Processes</div>
    <a class="btn btn-outline-ssa btn-sm w-100 mt-1" href="PspAdminHome">
      <i class="bi bi-cash-coin"></i>
      Manage Rates and Other
    </a>
    <a class="btn btn-outline-ssa btn-sm w-100 mt-1"  href="PspAgencyHome">
      <i class="bi bi-people-fill"></i>
      Manage Agencies and Agents
    </a>
    <c:if test="${sessionScope.currentPerson.getId()==104}">
      <div class="btn btn-altSsa w-100 mt-2 pe-none text-uppercase fw-bold">
        <i class="bi bi-cash"></i>&nbsp;
        Billing</div>
      <a class="btn btn-sm btn-outline-ssa w-100 mt-1" href="ResetBillingView">
        <i class="bi bi-currency-dollar"></i> Billing
      </a>
    </c:if>
    <div class="btn btn-ssa w-100 mt-1 mb-1 pe-none text-uppercase fw-bold">
      <i class="bi bi-calendar"></i> Monthly Processes</div>
    <a class="btn btn-outline-ssa btn-sm w-100 mb-1"  href="CreateBillingChecklist">
          <i class="bi bi-1-square"></i>
          Generate Monthly Checklist
    </a>
    <button type="button" class="btn btn-danger w-100 mt-2" data-bs-toggle="modal" data-bs-target="#updatePspMod" >
      <i class="bi bi-exclamation"></i> Update PSP Detail
    </button>
    <a class="btn btn-sm btn-outline-ssa w-100 mt-1 d-none" href="ResetAdminView">
      <i class="bi bi-emoji-neutral"></i> New Main
    </a>
  </div>
</div>


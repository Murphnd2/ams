<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="modal fade" id="addContactToActivity" role="dialog" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-person-plus me-2"></i>Add Contact</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body py-3">
        <form action="AddActivityContact25" method="post">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">Email Address</label>
          <input type="email" class="form-control form-control-sm" name="contactEmail" placeholder="name@company.com">
          <button type="submit" value="1" name="btnAddContact" class="btn btn-sm btn-ssa w-100 mt-2">
            <i class="bi bi-plus-lg me-1"></i>Add Contact
          </button>

          <c:set var="showEmployees" value="false"/>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals('Renewal') || sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals('Ticket')}">
            <c:if test="${sessionScope.local.getCurrentActivity().getEmployees() != null && sessionScope.local.getCurrentActivity().getEmployees().size() > 0}">
              <c:set var="showEmployees" value="true"/>
            </c:if>
          </c:if>

          <c:if test="${showEmployees == 'true'}">
            <hr class="my-3">
            <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">Or Select Employee</label>
            <select class="form-select form-select-sm" name="addEmployeeList">
              <c:forEach var="employee" items="${sessionScope.local.getCurrentActivity().getEmployees()}">
                <option value="${employee.getId()}">${employee.getLastName()}, ${employee.getFirstName()}</option>
              </c:forEach>
            </select>
            <button type="submit" value="2" name="btnAddContact" class="btn btn-sm btn-outline-ssa w-100 mt-2">
              <i class="bi bi-plus-lg me-1"></i>Add Employee
            </button>
          </c:if>
        </form>
      </div>
    </div>
  </div>
</div>
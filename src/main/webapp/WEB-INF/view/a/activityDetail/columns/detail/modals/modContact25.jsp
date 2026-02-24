<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pc" value="${sessionScope.local.getCurrentActivity().getPrimaryContact()}"/>
<div class="modal fade" id="modContactModal1" role="dialog" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-pencil-square me-2"></i>Edit Contact</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body py-3">
        <form action="ModifyContact25" method="post">
          <input type="hidden" name="pcId" value="${pc.getId()}">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">First Name</label>
          <input type="text" class="form-control form-control-sm mb-2" name="contactFirst" value="${pc.getFirstName()}">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">Last Name</label>
          <input type="text" class="form-control form-control-sm mb-2" name="contactLast" value="${pc.getLastName()}">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">Email</label>
          <c:choose>
            <c:when test="${pc.getEmployee() != null && pc.getEmployee().getHrEmail() != null && !pc.getEmployee().getHrEmail().equals('')}">
              <input type="email" class="form-control form-control-sm mb-2" name="contactEmail" value="${pc.getEmployee().getHrEmail().toLowerCase()}">
            </c:when>
            <c:when test="${pc.getEmployee() != null && pc.getEmployee().getEmail() != null && !pc.getEmployee().getEmail().equals('')}">
              <input type="email" class="form-control form-control-sm mb-2" name="contactEmail" value="${pc.getEmployee().getEmail().toLowerCase()}">
            </c:when>
            <c:otherwise>
              <input type="email" class="form-control form-control-sm mb-2" name="contactEmail" value="${pc.getEmail() != null ? pc.getEmail().toLowerCase() : ''}">
            </c:otherwise>
          </c:choose>
          <button type="submit" value="1" name="btnModContact" class="btn btn-sm btn-ssa w-100 mt-1">
            <i class="bi bi-check-lg me-1"></i>Update
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
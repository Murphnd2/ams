<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- Change Owner Modal --%>
<div class="modal fade" id="ownershipModal" role="dialog" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-key me-2"></i>Change Owner</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body py-3">
        <div class="text-muted mb-2" style="font-size: 0.82rem;">
          Currently assigned to <strong>${sessionScope.local.getCurrentActivity().getActivity().getAssignedTo().getFullName()}</strong>
        </div>
        <form method="post" action="ChangeOwner25">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">New Owner</label>
          <c:import url="/WEB-INF/view/a/general/ddUserList25.jsp"></c:import>
          <button type="submit" class="btn btn-sm btn-ssa w-100 mt-2">
            <i class="bi bi-arrow-repeat me-1"></i>Change Owner
          </button>
        </form>
      </div>
    </div>
  </div>
</div>

<%-- Change Due Date Modal --%>
<div class="modal fade" id="dueDateModal" role="dialog" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-calendar-event me-2"></i>Change Due Date</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body py-3">
        <div class="text-muted mb-2" style="font-size: 0.82rem;">
          Currently due <strong><fmt:formatDate value="${sessionScope.local.getCurrentActivity().getActivity().getDueDate()}" pattern="MMMM d, yyyy"/></strong>
        </div>
        <form method="post" action="ChangeDueDate25">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">New Due Date</label>
          <input type="date" name="newDueDate" class="form-control form-control-sm"
                 value="${sessionScope.local.getCurrentActivity().getActivity().getDueDate()}">
          <button type="submit" class="btn btn-sm btn-ssa w-100 mt-2">
            <i class="bi bi-arrow-repeat me-1"></i>Change Date
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
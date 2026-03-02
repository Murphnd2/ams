<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addReminderModal" role="dialog" tabindex="-1" aria-labelledby="addReminderModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold" id="loginLabel"><i class="bi bi-bell me-2"></i>Create a Reminder</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/a/checklistDetail/addReminderForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

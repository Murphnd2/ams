<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="createTicketModal" data-bs-backdrop="static" data-bs-keyboard="false" role="dialog" tabindex="-1" aria-labelledby="createTicketModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold">
          <i class="bi bi-ticket-detailed me-2"></i>Create Ticket</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close" tabindex="-1"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/activity/ticket/createTicketFormNew.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

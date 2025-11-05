<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="createTicketModal" data-bs-backdrop="static" data-bs-keyboard="false" role="dialog" tabindex="-1" aria-labelledby="createTicketModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title text-info fw-bold" id="loginLabel">
          <i class="bi bi-ticket-detailed"></i> Create Ticket</h3>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close" tabindex="-1"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/activity/ticket/createTicketFormNew.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

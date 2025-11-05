<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addNewRateModal" tabindex="-1" aria-labelledby="addNewRateModal" aria-hidden="true">
  <div class="modal-dialog modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="addNewRateModalLabel">Add a New Rate</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/psp/admin/forms/addNewRateForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

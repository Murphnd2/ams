<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addSimpleChecklistModal" role="dialog" tabindex="-1" aria-labelledby="addSimpleChecklistModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Create a Checklist</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/a/checklistDetail/addChecklistForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

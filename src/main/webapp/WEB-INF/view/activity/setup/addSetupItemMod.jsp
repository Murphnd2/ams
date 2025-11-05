<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addSetupItem" role="dialog" tabindex="-1" aria-labelledby="addSetupItem" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Add Setup Item</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/activity/setup/components/addModuleToSetupForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addRenewalItem" role="dialog" tabindex="-1" aria-labelledby="addRenewalItem" aria-hidden="true">
  <div class="modal-dialog modal-md modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Add Benefit To Renewal</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/activity/renew/components/ddBensNotInRenewalForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

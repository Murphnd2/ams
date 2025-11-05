<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="ownershipModal" role="dialog" tabindex="-1" aria-labelledby="ownershipModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Manage Owner and Due Date</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/a/general/activityToolbar/ownerAndDateChange/changeOwnerForm25.jsp"></c:import>
        <c:import url="/WEB-INF/view/a/general/activityToolbar/ownerAndDateChange/dateModal25.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addRenewalItem" role="dialog" tabindex="-1" aria-labelledby="addRenewalItemLabel" aria-hidden="true">
  <div class="modal-dialog modal-md modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold" id="addRenewalItemLabel">
          <i class="bi bi-shield-plus me-2"></i>Add Benefit To Renewal
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/modals/ddBensNotInRenewalForm25.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

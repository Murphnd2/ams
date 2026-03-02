<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="closeActivity" role="dialog" tabindex="-1" aria-labelledby="closeActivity" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Close Activity?</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <div class="row mb-2">
          <div class="col">
            All tasks have been completed.  Are you sure you want to close this ${sessionScope.sVar.getClassName()}?
          </div>
        </div>
        <div class="row">
          <div class="col">
            <button type="submit" class="ssa-action danger w-100" name="btnCheckList" value="${sessionScope.sVar.getCurrentActivity().getId()}">
              <i class="bi bi-door-open me-1"></i>Close ${sessionScope.sVar.getClassName()}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

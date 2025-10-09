<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="docLinksModal" role="dialog" tabindex="-1" aria-labelledby="docLinksModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title text-ssa fw-bold fs-5" id="loginLabel">
          <i class="bi bi-files"></i> Related Documents and Links</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/activity/activityWebLinkList.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

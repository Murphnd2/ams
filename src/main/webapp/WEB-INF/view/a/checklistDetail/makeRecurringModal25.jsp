<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="makeRecurringSequence" role="dialog" tabindex="-1" aria-labelledby="makeRecurringSequence" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header" style="background:#0d5681; color:white; padding:0.6rem 1rem;">
        <h6 class="modal-title fw-bold m-0">
          <i class="bi bi-arrow-repeat me-1"></i>Convert Checklist to Recurring
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/a/checklistDetail/MakeRecurringForm25.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

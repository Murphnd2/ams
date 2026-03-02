<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addInsertLinkModal" role="dialog" tabindex="-1" aria-labelledby="addInsertLinkModal" aria-hidden="true">
  <div class="modal-dialog " role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-link-45deg me-2"></i>Create Insert Link</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/a/general/addInsertLinkForm25.jsp"></c:import>
      </div>
    </div>
  </div>
</div>

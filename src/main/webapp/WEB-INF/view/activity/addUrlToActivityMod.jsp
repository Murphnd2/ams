<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addUrlAct" role="dialog" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-link-45deg me-2"></i>Add Link</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body py-3">
        <form method="post" action="AddUrlToActivity">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">URL</label>
          <input type="url" class="form-control form-control-sm mb-2" name="urlUpload" placeholder="https://..." required>
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">Display Name <span class="fw-normal text-muted">(optional)</span></label>
          <input type="text" class="form-control form-control-sm mb-2" name="urlName" placeholder="Describe this link">
          <button type="submit" class="btn btn-sm btn-ssa w-100 mt-1">
            <i class="bi bi-plus-lg me-1"></i>Add Link
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
